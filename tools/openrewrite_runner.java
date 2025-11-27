///usr/bin/env jbang "$0" "$@" ; exit $?
//REPOS mavencentral,gradle=https://repo.gradle.org/gradle/libs-releases
//DEPS org.gradle:gradle-tooling-api:9.2.1
//DEPS info.picocli:picocli:4.7.6
//DEPS com.google.code.gson:gson:2.11.0
//DEPS org.slf4j:slf4j-simple:2.0.9

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import org.gradle.tooling.*;
import org.gradle.tooling.model.build.BuildEnvironment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.*;

/**
 * OpenRewrite Runner - Hybrid approach using Gradle Tooling API + Init Script
 *
 * This tool:
 * 1. Connects to Gradle project via Tooling API
 * 2. Detects Gradle version
 * 3. Generates appropriate init script (Groovy for <8, Kotlin for >=8)
 * 4. Executes OpenRewrite recipes via init script injection
 * 5. Parses and reports results
 */
@Command(name = "openrewrite-runner",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Run OpenRewrite recipes on Gradle projects")
public class openrewrite_runner implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to Gradle project")
    private Path projectPath;

    @Option(names = {"-r", "--recipe"}, description = "Recipe(s) to run (comma-separated)")
    private String recipes;

    @Option(names = {"--dry-run"}, description = "Preview changes without applying")
    private boolean dryRun = false;

    @Option(names = {"--list"}, description = "List available recipes (optionally filter by keyword)")
    private String listFilter;

    @Option(names = {"--suggest"}, description = "Suggest recipes based on project analysis")
    private boolean suggest = false;

    @Option(names = {"--generate-recipe"}, description = "Generate custom recipe for project-specific patterns")
    private boolean generateRecipe = false;

    @Option(names = {"--analyze"}, description = "Analyze project complexity and recommend migration strategy")
    private boolean analyze = false;

    @Option(names = {"--output-dir"}, description = "Output directory for generated recipes (default: .rewrite)")
    private String outputDir = ".rewrite";

    @Option(names = {"--json"}, description = "Output results as JSON")
    private boolean jsonOutput = false;

    @Option(names = {"--verbose"}, description = "Verbose output")
    private boolean verbose = false;

    @Option(names = {"--fail-on-dry-run"}, description = "Exit with error if dry-run finds changes")
    private boolean failOnDryRun = false;

    @Option(names = {"--additional-deps"}, description = "Additional recipe dependencies (comma-separated)")
    private String additionalDeps;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        int exitCode = new CommandLine(new openrewrite_runner()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        if (listFilter != null || (recipes == null && !suggest && !generateRecipe && !analyze)) {
            return listRecipes(listFilter);
        }

        if (!Files.exists(projectPath)) {
            error("Project path does not exist: " + projectPath);
            return 1;
        }

        if (analyze) {
            return analyzeProject();
        }

        if (generateRecipe) {
            return generateCustomRecipe();
        }

        if (suggest) {
            return suggestRecipes();
        }

        return runRecipes();
    }

    private int runRecipes() throws Exception {
        Result result = new Result();
        result.projectPath = projectPath.toAbsolutePath().toString();
        result.recipes = Arrays.asList(recipes.split(","));
        result.dryRun = dryRun;

        Path initScript = null;

        try (ProjectConnection connection = connect()) {
            // 1. Get Gradle version
            BuildEnvironment env = connection.getModel(BuildEnvironment.class);
            String gradleVersion = env.getGradle().getGradleVersion();
            result.gradleVersion = gradleVersion;

            log("Gradle version: " + gradleVersion);
            log("Recipes: " + recipes);
            log("Mode: " + (dryRun ? "dry-run" : "apply"));

            // 2. Generate init script
            initScript = generateInitScript(gradleVersion, recipes);
            log("Generated init script: " + initScript);

            // 3. Execute
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();

            String task = dryRun ? "rewriteDryRun" : "rewriteRun";

            List<String> args = new ArrayList<>();
            args.add("--init-script");
            args.add(initScript.toString());
            args.add("--no-configuration-cache"); // OpenRewrite doesn't support CC yet
            if (verbose) {
                args.add("--info");
            }

            log("Running: ./gradlew " + task + " " + String.join(" ", args));

            long startTime = System.currentTimeMillis();

            try {
                connection.newBuild()
                        .forTasks(task)
                        .withArguments(args.toArray(new String[0]))
                        .setStandardOutput(stdout)
                        .setStandardError(stderr)
                        .run();

                result.success = true;
            } catch (BuildException e) {
                result.success = false;
                result.error = e.getMessage();
            }

            result.durationMs = System.currentTimeMillis() - startTime;
            result.stdout = stdout.toString();
            result.stderr = stderr.toString();

            // 4. Parse results
            parseResults(result);

            // 5. Output
            if (jsonOutput) {
                System.out.println(gson.toJson(result));
            } else {
                printHumanReadable(result);
            }

            if (dryRun && failOnDryRun && result.changesDetected > 0) {
                return 1;
            }

            return result.success ? 0 : 1;

        } finally {
            // Cleanup
            if (initScript != null) {
                try {
                    Files.deleteIfExists(initScript);
                } catch (IOException e) {
                    // Log but don't fail - cleanup is best-effort
                    System.err.println("WARNING: Failed to delete temporary init script: "
                        + initScript + " - " + e.getMessage());
                }
            }
        }
    }

    // ==================== Phase 4: Project Analysis for Orchestrator ====================

    private int analyzeProject() throws Exception {
        ProjectAnalysis analysis = new ProjectAnalysis();
        analysis.projectPath = projectPath.toAbsolutePath().toString();

        try (ProjectConnection connection = connect()) {
            BuildEnvironment env = connection.getModel(BuildEnvironment.class);
            analysis.gradleVersion = env.getGradle().getGradleVersion();
        } catch (Exception e) {
            // Fallback if Tooling API fails - log the issue so users know
            System.err.println("WARNING: Could not connect to Gradle Tooling API: " + e.getMessage());
            System.err.println("Falling back to wrapper properties detection (less reliable)");
            analysis.gradleVersion = detectGradleVersionFromWrapper();
        }

        log("Analyzing project complexity...");

        // Count build files
        List<Path> gradleFiles = Files.walk(projectPath)
                .filter(Files::isRegularFile)
                .filter(p -> (p.toString().endsWith(".gradle") || p.toString().endsWith(".gradle.kts")) &&
                        !p.toString().contains("/build/") && !p.toString().contains("/.gradle/"))
                .toList();

        List<Path> groovyFiles = gradleFiles.stream()
                .filter(p -> p.toString().endsWith(".gradle") && !p.toString().endsWith(".gradle.kts"))
                .toList();

        List<Path> kotlinFiles = gradleFiles.stream()
                .filter(p -> p.toString().endsWith(".gradle.kts"))
                .toList();

        analysis.totalBuildFiles = gradleFiles.size();
        analysis.groovyBuildFiles = groovyFiles.size();
        analysis.kotlinBuildFiles = kotlinFiles.size();

        // Count modules (from settings file)
        analysis.moduleCount = countModules();

        // Count lines of build code
        long totalLines = 0;
        for (Path file : gradleFiles) {
            totalLines += Files.lines(file).count();
        }
        analysis.totalBuildLines = totalLines;

        // Scan for issues
        analysis.issues = scanAllIssues(gradleFiles);

        // Determine complexity
        analysis.complexity = determineComplexity(analysis);

        // Recommend strategy
        analysis.recommendedStrategy = recommendStrategy(analysis);

        // Generate migration plan
        analysis.migrationPlan = generateMigrationPlan(analysis);

        if (jsonOutput) {
            System.out.println(gson.toJson(analysis));
        } else {
            printProjectAnalysis(analysis);
        }

        return 0;
    }

    private String detectGradleVersionFromWrapper() {
        Path wrapperProps = projectPath.resolve("gradle/wrapper/gradle-wrapper.properties");

        if (!Files.exists(wrapperProps)) {
            log("No gradle-wrapper.properties found at: " + wrapperProps);
            return "unknown";
        }

        try {
            String content = Files.readString(wrapperProps);
            Pattern p = Pattern.compile("gradle-(\\d+\\.\\d+(?:\\.\\d+)?)-");
            Matcher m = p.matcher(content);
            if (m.find()) {
                return m.group(1);
            } else {
                System.err.println("WARNING: Could not parse Gradle version from wrapper properties");
                return "unknown";
            }
        } catch (IOException e) {
            System.err.println("WARNING: Failed to read wrapper properties: " + e.getMessage());
            return "unknown";
        }
    }

    private int countModules() throws IOException {
        // Look for settings.gradle.kts or settings.gradle
        Path settingsKts = projectPath.resolve("settings.gradle.kts");
        Path settingsGroovy = projectPath.resolve("settings.gradle");

        Path settingsFile = Files.exists(settingsKts) ? settingsKts : settingsGroovy;
        if (!Files.exists(settingsFile)) {
            return 1; // Single project
        }

        String content = Files.readString(settingsFile);
        Pattern includePattern = Pattern.compile("include\\s*\\(([^)]+)\\)|include\\s+['\"]([^'\"]+)['\"]");
        Matcher matcher = includePattern.matcher(content);

        int count = 1; // Root project
        while (matcher.find()) {
            String match = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            // Count comma-separated values
            count += match.split(",").length;
        }

        return count;
    }

    private Map<String, Integer> scanAllIssues(List<Path> buildFiles) throws IOException {
        Map<String, Integer> issues = new LinkedHashMap<>();

        Pattern[] patterns = {
                Pattern.compile("tasks\\.create\\("),
                Pattern.compile("tasks\\.getByName\\("),
                Pattern.compile("\\$buildDir|project\\.buildDir"),
                Pattern.compile("System\\.getProperty\\("),
                Pattern.compile("System\\.getenv\\("),
                Pattern.compile("apply\\s+plugin:"),
                Pattern.compile("org\\.gradle\\.[\\w.]*\\.internal\\."),
                Pattern.compile("convention\\.(getPlugin|findPlugin)"),
                Pattern.compile("(compile|testCompile|runtime)\\s*['\"]")
        };

        String[] issueNames = {
                "eager_task_create",
                "eager_task_getByName",
                "deprecated_buildDir",
                "system_getProperty",
                "system_getenv",
                "legacy_apply_plugin",
                "internal_api_usage",
                "deprecated_convention",
                "deprecated_configurations"
        };

        for (int i = 0; i < patterns.length; i++) {
            issues.put(issueNames[i], 0);
        }

        for (Path file : buildFiles) {
            String content = Files.readString(file);
            for (int i = 0; i < patterns.length; i++) {
                int count = countMatches(patterns[i], content);
                issues.merge(issueNames[i], count, Integer::sum);
            }
        }

        // Remove zero counts
        issues.entrySet().removeIf(e -> e.getValue() == 0);

        return issues;
    }

    private String determineComplexity(ProjectAnalysis analysis) {
        if (analysis.moduleCount > 50 || analysis.totalBuildFiles > 100) {
            return "LARGE";
        } else if (analysis.moduleCount > 10 || analysis.totalBuildFiles > 15) {
            return "MEDIUM";
        } else {
            return "SMALL";
        }
    }

    private String recommendStrategy(ProjectAnalysis analysis) {
        int totalIssues = analysis.issues.values().stream().mapToInt(Integer::intValue).sum();

        if ("LARGE".equals(analysis.complexity)) {
            return "OPENREWRITE_PRIMARY";
        } else if ("SMALL".equals(analysis.complexity) && totalIssues < 20) {
            return "CLAUDE_PRIMARY";
        } else {
            return "HYBRID";
        }
    }

    private List<MigrationStep> generateMigrationPlan(ProjectAnalysis analysis) {
        List<MigrationStep> steps = new ArrayList<>();

        steps.add(new MigrationStep(1, "Create git checkpoint", "GIT", "< 1 min"));

        if (isVersionLessThan(analysis.gradleVersion, "8.0")) {
            steps.add(new MigrationStep(2, "Update Gradle wrapper", "OPENREWRITE", "< 1 min"));
            steps.add(new MigrationStep(3, "Migrate deprecated APIs (Gradle 7→8)", "OPENREWRITE", "2-5 min"));
            steps.add(new MigrationStep(4, "Migrate deprecated APIs (Gradle 8→9)", "OPENREWRITE", "2-5 min"));
        } else if (isVersionLessThan(analysis.gradleVersion, "9.0")) {
            steps.add(new MigrationStep(2, "Update Gradle wrapper", "OPENREWRITE", "< 1 min"));
            steps.add(new MigrationStep(3, "Migrate deprecated APIs (Gradle 8→9)", "OPENREWRITE", "2-5 min"));
        }

        if (analysis.issues.containsKey("legacy_apply_plugin")) {
            steps.add(new MigrationStep(steps.size() + 1, "Migrate to plugins block", "OPENREWRITE", "1-2 min"));
        }

        if (analysis.groovyBuildFiles > 0) {
            steps.add(new MigrationStep(steps.size() + 1, "Migrate to Kotlin DSL (optional)", "OPENREWRITE", "varies"));
        }

        int manualIssues = analysis.issues.getOrDefault("internal_api_usage", 0) +
                analysis.issues.getOrDefault("deprecated_convention", 0);

        if (manualIssues > 0) {
            steps.add(new MigrationStep(steps.size() + 1, "Fix " + manualIssues + " edge cases", "CLAUDE", "5-10 min"));
        }

        steps.add(new MigrationStep(steps.size() + 1, "Verify build", "GRADLE", "2-5 min"));
        steps.add(new MigrationStep(steps.size() + 1, "Run tests", "GRADLE", "varies"));

        return steps;
    }

    private void printProjectAnalysis(ProjectAnalysis analysis) {
        System.out.println();
        System.out.println("═".repeat(65));
        System.out.println("                    PROJECT ANALYSIS");
        System.out.println("═".repeat(65));
        System.out.println();
        System.out.println("Project: " + analysis.projectPath);
        System.out.println("Gradle Version: " + analysis.gradleVersion);
        System.out.println();
        System.out.println("Metrics:");
        System.out.println("  Modules: " + analysis.moduleCount);
        System.out.println("  Build files: " + analysis.totalBuildFiles +
                " (" + analysis.kotlinBuildFiles + " Kotlin, " + analysis.groovyBuildFiles + " Groovy)");
        System.out.println("  Lines of build code: " + analysis.totalBuildLines);
        System.out.println();
        System.out.println("Complexity: " + analysis.complexity);
        System.out.println("Recommended Strategy: " + formatStrategy(analysis.recommendedStrategy));
        System.out.println();

        if (!analysis.issues.isEmpty()) {
            int total = analysis.issues.values().stream().mapToInt(Integer::intValue).sum();
            System.out.println("Issues Detected: " + total);
            for (Map.Entry<String, Integer> entry : analysis.issues.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            }
            System.out.println();
        }

        System.out.println("Migration Plan:");
        System.out.println("┌────┬──────────────────────────────────────┬────────────┬───────────┐");
        System.out.println("│ #  │ Step                                 │ Engine     │ Est. Time │");
        System.out.println("├────┼──────────────────────────────────────┼────────────┼───────────┤");
        for (MigrationStep step : analysis.migrationPlan) {
            System.out.printf("│ %-2d │ %-36s │ %-10s │ %-9s │%n",
                    step.order, truncate(step.description, 36), step.engine, step.estimatedTime);
        }
        System.out.println("└────┴──────────────────────────────────────┴────────────┴───────────┘");
        System.out.println();
        System.out.println("Run: /migrate <target-version> to execute this plan");
        System.out.println("     /migrate <target-version> --dry-run to preview only");
        System.out.println();
    }

    private String formatStrategy(String strategy) {
        return switch (strategy) {
            case "OPENREWRITE_PRIMARY" -> "OpenRewrite (primary) + Claude (fallback)";
            case "CLAUDE_PRIMARY" -> "Claude (primary) + OpenRewrite (optional)";
            case "HYBRID" -> "Hybrid: OpenRewrite bulk + Claude edge cases";
            default -> strategy;
        };
    }

    // Supporting classes for analysis
    static class ProjectAnalysis {
        String projectPath;
        String gradleVersion;
        int moduleCount;
        int totalBuildFiles;
        int groovyBuildFiles;
        int kotlinBuildFiles;
        long totalBuildLines;
        String complexity;
        String recommendedStrategy;
        Map<String, Integer> issues;
        List<MigrationStep> migrationPlan;
    }

    static class MigrationStep {
        int order;
        String description;
        String engine;
        String estimatedTime;

        MigrationStep(int order, String description, String engine, String estimatedTime) {
            this.order = order;
            this.description = description;
            this.engine = engine;
            this.estimatedTime = estimatedTime;
        }
    }

    // ==================== End Phase 4 ====================

    private int suggestRecipes() throws Exception {
        SuggestionResult result = new SuggestionResult();
        result.projectPath = projectPath.toAbsolutePath().toString();

        try (ProjectConnection connection = connect()) {
            BuildEnvironment env = connection.getModel(BuildEnvironment.class);
            result.gradleVersion = env.getGradle().getGradleVersion();

            // Check Gradle version
            if (isVersionLessThan(result.gradleVersion, "8.0")) {
                result.suggestions.add(new Suggestion(
                        "org.openrewrite.gradle.UpdateGradleWrapper",
                        "Update Gradle wrapper to 9.2.1",
                        0.95,
                        "version=9.2.1"
                ));
                result.suggestions.add(new Suggestion(
                        "org.openrewrite.gradle.MigrateToGradle8",
                        "Migrate deprecated APIs to Gradle 8",
                        0.85,
                        null
                ));
                result.suggestions.add(new Suggestion(
                        "org.openrewrite.gradle.MigrateToGradle9",
                        "Migrate deprecated APIs to Gradle 9",
                        0.85,
                        null
                ));
            } else if (isVersionLessThan(result.gradleVersion, "9.0")) {
                result.suggestions.add(new Suggestion(
                        "org.openrewrite.gradle.UpdateGradleWrapper",
                        "Update Gradle wrapper to 9.2.1",
                        0.95,
                        "version=9.2.1"
                ));
                result.suggestions.add(new Suggestion(
                        "org.openrewrite.gradle.MigrateToGradle9",
                        "Migrate deprecated APIs to Gradle 9",
                        0.85,
                        null
                ));
            }

            // Scan for patterns
            scanForPatterns(result);
        }

        if (jsonOutput) {
            System.out.println(gson.toJson(result));
        } else {
            printSuggestions(result);
        }

        return 0;
    }

    private void scanForPatterns(SuggestionResult result) throws IOException {
        // Check for Groovy build files
        long groovyFiles = Files.walk(projectPath)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".gradle") && !p.toString().endsWith(".gradle.kts"))
                .count();

        if (groovyFiles > 0) {
            result.suggestions.add(new Suggestion(
                    "org.openrewrite.kotlin.gradle.MigrateToKotlinDsl",
                    "Migrate " + groovyFiles + " Groovy build files to Kotlin DSL",
                    0.85,
                    null
            ));
        }

        // Check for legacy patterns
        Pattern applyPlugin = Pattern.compile("apply\\s+plugin:");
        Pattern tasksCreate = Pattern.compile("tasks\\.create\\(");
        Pattern buildDir = Pattern.compile("\\$buildDir|project\\.buildDir");

        int applyPluginCount = 0;
        int tasksCreateCount = 0;
        int buildDirCount = 0;

        List<Path> buildFiles = Files.walk(projectPath)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".gradle") || p.toString().endsWith(".gradle.kts"))
                .filter(p -> !p.toString().contains("/build/"))
                .toList();

        for (Path file : buildFiles) {
            String content = Files.readString(file);
            applyPluginCount += countMatches(applyPlugin, content);
            tasksCreateCount += countMatches(tasksCreate, content);
            buildDirCount += countMatches(buildDir, content);
        }

        if (applyPluginCount > 0) {
            result.suggestions.add(new Suggestion(
                    "org.openrewrite.gradle.plugins.MigrateToPluginsBlock",
                    "Migrate " + applyPluginCount + " legacy plugin applications",
                    0.9,
                    null
            ));
        }

        if (tasksCreateCount > 0 || buildDirCount > 0) {
            // Suggest appropriate migration based on current version
            String recipe = isVersionLessThan(result.gradleVersion, "8.0")
                    ? "org.openrewrite.gradle.MigrateToGradle8"
                    : "org.openrewrite.gradle.MigrateToGradle9";
            String targetVersion = isVersionLessThan(result.gradleVersion, "8.0") ? "8" : "9";
            result.suggestions.add(new Suggestion(
                    recipe,
                    "Fix " + (tasksCreateCount + buildDirCount) + " deprecated patterns (Gradle " + targetVersion + ")",
                    0.75,
                    null
            ));
        }

        // Check for version catalog
        Path versionCatalog = projectPath.resolve("gradle/libs.versions.toml");
        if (!Files.exists(versionCatalog)) {
            result.suggestions.add(new Suggestion(
                    "org.openrewrite.gradle.MigrateToVersionCatalog",
                    "Create version catalog for centralized dependency management",
                    0.7,
                    null
            ));
        }
    }

    private int countMatches(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    // ==================== Phase 3: Custom Recipe Generation ====================

    private int generateCustomRecipe() throws Exception {
        GeneratedRecipeResult result = new GeneratedRecipeResult();
        result.projectPath = projectPath.toAbsolutePath().toString();

        log("Scanning project for patterns not covered by standard recipes...");

        // Scan for project-specific patterns
        List<DetectedPattern> patterns = scanForCustomPatterns();
        result.detectedPatterns = patterns;

        if (patterns.isEmpty()) {
            if (jsonOutput) {
                result.message = "No project-specific patterns detected that require custom recipes";
                System.out.println(gson.toJson(result));
            } else {
                System.out.println();
                System.out.println("No project-specific patterns detected.");
                System.out.println("Standard OpenRewrite recipes should cover your migration needs.");
                System.out.println();
                System.out.println("Run: /openrewrite suggest");
            }
            return 0;
        }

        // Generate the recipe YAML
        String recipeYaml = generateRecipeYaml(patterns);
        result.generatedRecipe = recipeYaml;

        // Write to output directory
        Path outputPath = projectPath.resolve(outputDir);
        Files.createDirectories(outputPath);

        Path recipeFile = outputPath.resolve("generated-migrations.yml");
        Files.writeString(recipeFile, recipeYaml);
        result.outputFile = recipeFile.toString();

        if (jsonOutput) {
            result.message = "Generated custom recipe with " + patterns.size() + " transformations";
            System.out.println(gson.toJson(result));
        } else {
            printGeneratedRecipeResult(result, patterns);
        }

        return 0;
    }

    private List<DetectedPattern> scanForCustomPatterns() throws IOException {
        List<DetectedPattern> patterns = new ArrayList<>();

        // Pattern definitions for project-specific issues
        List<PatternDefinition> definitions = getPatternDefinitions();

        // Scan all relevant files
        List<Path> sourceFiles = Files.walk(projectPath)
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String path = p.toString();
                    return (path.endsWith(".gradle") || path.endsWith(".gradle.kts") ||
                            path.endsWith(".java") || path.endsWith(".kt")) &&
                            !path.contains("/build/") && !path.contains("/.gradle/");
                })
                .toList();

        for (Path file : sourceFiles) {
            String content = Files.readString(file);
            String relativePath = projectPath.relativize(file).toString();

            for (PatternDefinition def : definitions) {
                Matcher matcher = def.pattern.matcher(content);
                while (matcher.find()) {
                    DetectedPattern detected = new DetectedPattern();
                    detected.file = relativePath;
                    detected.line = getLineNumber(content, matcher.start());
                    detected.patternType = def.type;
                    detected.matchedText = matcher.group();
                    detected.description = def.description;
                    detected.suggestedFix = def.suggestedFix;
                    detected.recipeType = def.recipeType;
                    detected.recipeConfig = def.generateConfig(matcher);
                    patterns.add(detected);
                }
            }
        }

        // Deduplicate similar patterns (keep unique transformations)
        return deduplicatePatterns(patterns);
    }

    private List<PatternDefinition> getPatternDefinitions() {
        List<PatternDefinition> defs = new ArrayList<>();

        // Custom method renames (project-specific APIs)
        defs.add(new PatternDefinition(
                "custom-method-call",
                Pattern.compile("(\\w+)\\.getBuildDir\\(\\)"),
                "Direct getBuildDir() call",
                "Use layout.buildDirectory instead",
                "ChangeMethodName",
                m -> Map.of(
                        "methodPattern", m.group(1) + " getBuildDir()",
                        "newMethodName", "getLayoutBuildDirectory"
                )
        ));

        // System property in configuration
        defs.add(new PatternDefinition(
                "system-property-config",
                Pattern.compile("System\\.getProperty\\([\"']([^\"']+)[\"']\\)"),
                "System.getProperty in configuration phase",
                "Use providers.systemProperty()",
                "ChangeMethodInvocation",
                m -> Map.of(
                        "pattern", "System.getProperty(\"" + m.group(1) + "\")",
                        "replacement", "providers.systemProperty(\"" + m.group(1) + "\").getOrNull()"
                )
        ));

        // System.getenv in configuration
        defs.add(new PatternDefinition(
                "system-env-config",
                Pattern.compile("System\\.getenv\\([\"']([^\"']+)[\"']\\)"),
                "System.getenv in configuration phase",
                "Use providers.environmentVariable()",
                "ChangeMethodInvocation",
                m -> Map.of(
                        "pattern", "System.getenv(\"" + m.group(1) + "\")",
                        "replacement", "providers.environmentVariable(\"" + m.group(1) + "\").getOrNull()"
                )
        ));

        // File operations in task action
        defs.add(new PatternDefinition(
                "project-file-operation",
                Pattern.compile("project\\.(copy|delete|sync|exec|javaexec)\\s*\\{"),
                "Project file operation in task action",
                "Inject FileSystemOperations or ExecOperations service",
                "Manual",
                m -> Map.of(
                        "operation", m.group(1),
                        "note", "Requires manual migration to injected services"
                )
        ));

        // Convention access
        defs.add(new PatternDefinition(
                "convention-access",
                Pattern.compile("(project\\.)?convention\\.(getPlugin|findPlugin|plugins)"),
                "Deprecated Convention API",
                "Use extensions instead",
                "Manual",
                m -> Map.of(
                        "note", "Replace convention with extensions API"
                )
        ));

        // Eager task configuration
        defs.add(new PatternDefinition(
                "eager-task-getByName",
                Pattern.compile("tasks\\.getByName\\([\"']([^\"']+)[\"']\\)"),
                "Eager task lookup with getByName",
                "Use tasks.named() for lazy configuration",
                "ReplaceMethodCall",
                m -> Map.of(
                        "oldMethod", "tasks.getByName(\"" + m.group(1) + "\")",
                        "newMethod", "tasks.named(\"" + m.group(1) + "\")"
                )
        ));

        // Internal API usage
        defs.add(new PatternDefinition(
                "internal-api",
                Pattern.compile("import\\s+org\\.gradle\\.[\\w.]*\\.internal\\.(\\w+)"),
                "Internal Gradle API import",
                "Use public API equivalent",
                "Manual",
                m -> Map.of(
                        "internalClass", m.group(1),
                        "note", "Replace with public API - no automated fix available"
                )
        ));

        // buildDir direct access in Kotlin DSL
        defs.add(new PatternDefinition(
                "buildDir-kotlin",
                Pattern.compile("(project\\.)?buildDir(?!ectory)"),
                "Direct buildDir property access",
                "Use layout.buildDirectory",
                "ReplaceText",
                m -> Map.of(
                        "find", m.group(),
                        "replace", "layout.buildDirectory.asFile.get()"
                )
        ));

        return defs;
    }

    private int getLineNumber(String content, int charIndex) {
        int line = 1;
        for (int i = 0; i < charIndex && i < content.length(); i++) {
            if (content.charAt(i) == '\n') line++;
        }
        return line;
    }

    private List<DetectedPattern> deduplicatePatterns(List<DetectedPattern> patterns) {
        // Group by recipe config to avoid duplicate transformations
        Map<String, DetectedPattern> unique = new LinkedHashMap<>();
        for (DetectedPattern p : patterns) {
            String key = p.recipeType + ":" + p.recipeConfig.toString();
            if (!unique.containsKey(key)) {
                unique.put(key, p);
            } else {
                // Increment occurrence count on existing
                unique.get(key).occurrences++;
            }
        }
        return new ArrayList<>(unique.values());
    }

    private String generateRecipeYaml(List<DetectedPattern> patterns) {
        StringBuilder yaml = new StringBuilder();

        yaml.append("# Auto-generated by gradle-claude-plugin\n");
        yaml.append("# Generated: ").append(java.time.LocalDateTime.now()).append("\n");
        yaml.append("# Review before applying - some transformations may need manual adjustment\n");
        yaml.append("#\n");
        yaml.append("# Usage:\n");
        yaml.append("#   /openrewrite run com.generated.ProjectMigrations\n");
        yaml.append("#   /openrewrite dry-run com.generated.ProjectMigrations\n");
        yaml.append("\n");
        yaml.append("---\n");
        yaml.append("type: specs.openrewrite.org/v1beta/recipe\n");
        yaml.append("name: com.generated.ProjectMigrations\n");
        yaml.append("displayName: Project-Specific Migrations\n");
        yaml.append("description: |\n");
        yaml.append("  Auto-generated recipe for project-specific patterns.\n");
        yaml.append("  Detected ").append(patterns.size()).append(" unique transformation(s).\n");
        yaml.append("recipeList:\n");

        // Group patterns by type for better organization
        Map<String, List<DetectedPattern>> byType = new LinkedHashMap<>();
        for (DetectedPattern p : patterns) {
            byType.computeIfAbsent(p.recipeType, k -> new ArrayList<>()).add(p);
        }

        for (Map.Entry<String, List<DetectedPattern>> entry : byType.entrySet()) {
            String recipeType = entry.getKey();
            List<DetectedPattern> typePatterns = entry.getValue();

            yaml.append("\n  # ").append(recipeType).append(" transformations\n");

            for (DetectedPattern p : typePatterns) {
                if ("Manual".equals(recipeType)) {
                    // Add as comment for manual review
                    yaml.append("  # MANUAL: ").append(p.description).append("\n");
                    yaml.append("  #   File: ").append(p.file).append(":").append(p.line).append("\n");
                    yaml.append("  #   Match: ").append(truncate(p.matchedText, 60)).append("\n");
                    yaml.append("  #   Fix: ").append(p.suggestedFix).append("\n");
                } else {
                    yaml.append("  - org.openrewrite.");
                    yaml.append(getRecipeClass(recipeType)).append(":\n");
                    for (Map.Entry<String, String> config : p.recipeConfig.entrySet()) {
                        yaml.append("      ").append(config.getKey()).append(": \"");
                        yaml.append(escapeYaml(config.getValue())).append("\"\n");
                    }
                    if (p.occurrences > 1) {
                        yaml.append("      # Found ").append(p.occurrences).append(" occurrences\n");
                    }
                }
            }
        }

        // Add standard recipes that complement the custom ones
        yaml.append("\n  # Standard recipes to run alongside custom transformations\n");
        yaml.append("  - org.openrewrite.gradle.MigrateToGradle8\n");
        yaml.append("  - org.openrewrite.gradle.MigrateToGradle9\n");

        return yaml.toString();
    }

    private String getRecipeClass(String recipeType) {
        return switch (recipeType) {
            case "ChangeMethodName" -> "java.ChangeMethodName";
            case "ChangeMethodInvocation" -> "java.ChangeMethodInvocation";
            case "ReplaceMethodCall" -> "java.ReplaceMethodInvocation";
            case "ReplaceText" -> "text.FindAndReplace";
            default -> "java." + recipeType;
        };
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        s = s.replace("\n", " ").replace("\r", "");
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 3) + "...";
    }

    private String escapeYaml(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void printGeneratedRecipeResult(GeneratedRecipeResult result, List<DetectedPattern> patterns) {
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("Generated Custom OpenRewrite Recipe");
        System.out.println("=".repeat(60));
        System.out.println();
        System.out.println("Output: " + result.outputFile);
        System.out.println("Patterns detected: " + patterns.size());
        System.out.println();

        // Group by type
        Map<String, Long> byType = patterns.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        p -> p.recipeType,
                        java.util.stream.Collectors.counting()
                ));

        System.out.println("Transformations by type:");
        for (Map.Entry<String, Long> entry : byType.entrySet()) {
            String marker = "Manual".equals(entry.getKey()) ? "(manual)" : "(auto)";
            System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " " + marker);
        }

        long manualCount = byType.getOrDefault("Manual", 0L);
        long autoCount = patterns.size() - manualCount;

        System.out.println();
        System.out.println("Summary:");
        System.out.println("  Automatable: " + autoCount);
        System.out.println("  Manual review: " + manualCount);

        System.out.println();
        System.out.println("Next steps:");
        System.out.println("  1. Review generated recipe: " + result.outputFile);
        System.out.println("  2. Preview changes: /openrewrite dry-run com.generated.ProjectMigrations");
        System.out.println("  3. Apply changes: /openrewrite run com.generated.ProjectMigrations");

        if (manualCount > 0) {
            System.out.println();
            System.out.println("Note: " + manualCount + " patterns require manual migration (marked in recipe)");
        }
        System.out.println();
    }

    // Supporting classes for recipe generation
    static class PatternDefinition {
        String type;
        Pattern pattern;
        String description;
        String suggestedFix;
        String recipeType;
        java.util.function.Function<Matcher, Map<String, String>> configGenerator;

        PatternDefinition(String type, Pattern pattern, String description,
                         String suggestedFix, String recipeType,
                         java.util.function.Function<Matcher, Map<String, String>> configGenerator) {
            this.type = type;
            this.pattern = pattern;
            this.description = description;
            this.suggestedFix = suggestedFix;
            this.recipeType = recipeType;
            this.configGenerator = configGenerator;
        }

        Map<String, String> generateConfig(Matcher m) {
            return configGenerator.apply(m);
        }
    }

    static class DetectedPattern {
        String file;
        int line;
        String patternType;
        String matchedText;
        String description;
        String suggestedFix;
        String recipeType;
        Map<String, String> recipeConfig;
        int occurrences = 1;
    }

    static class GeneratedRecipeResult {
        String projectPath;
        String message;
        List<DetectedPattern> detectedPatterns;
        String generatedRecipe;
        String outputFile;
    }

    // ==================== End Phase 3 ====================

    private int listRecipes(String filter) {
        List<RecipeInfo> recipes = getKnownRecipes();

        if (filter != null && !filter.isEmpty()) {
            String lowerFilter = filter.toLowerCase();
            recipes = recipes.stream()
                    .filter(r -> r.name.toLowerCase().contains(lowerFilter) ||
                            r.description.toLowerCase().contains(lowerFilter))
                    .toList();
        }

        if (jsonOutput) {
            System.out.println(gson.toJson(recipes));
        } else {
            System.out.println("Available Gradle Recipes:");
            System.out.println("=".repeat(60));
            for (RecipeInfo recipe : recipes) {
                System.out.println();
                System.out.println("  " + recipe.name);
                System.out.println("    " + recipe.description);
                if (recipe.options != null && !recipe.options.isEmpty()) {
                    System.out.println("    Options: " + String.join(", ", recipe.options));
                }
            }
            System.out.println();
            System.out.println("Run with: /openrewrite run <recipe-name>");
            System.out.println("Preview:  /openrewrite dry-run <recipe-name>");
        }

        return 0;
    }

    private List<RecipeInfo> getKnownRecipes() {
        List<RecipeInfo> recipes = new ArrayList<>();

        // Migration recipes
        recipes.add(new RecipeInfo(
                "org.openrewrite.gradle.MigrateToGradle9",
                "Migrate deprecated APIs from Gradle 8 to Gradle 9",
                List.of()
        ));
        recipes.add(new RecipeInfo(
                "org.openrewrite.gradle.MigrateToGradle8",
                "Migrate deprecated APIs from Gradle 7 to Gradle 8",
                List.of()
        ));
        recipes.add(new RecipeInfo(
                "org.openrewrite.gradle.MigrateToGradle7",
                "Migrate deprecated APIs from Gradle 6 to Gradle 7",
                List.of()
        ));
        recipes.add(new RecipeInfo(
                "org.openrewrite.gradle.UpdateGradleWrapper",
                "Update Gradle wrapper to a specific version",
                List.of("version", "distribution")
        ));

        // DSL recipes
        recipes.add(new RecipeInfo(
                "org.openrewrite.kotlin.gradle.MigrateToKotlinDsl",
                "Migrate Groovy DSL to Kotlin DSL",
                List.of()
        ));
        recipes.add(new RecipeInfo(
                "org.openrewrite.gradle.plugins.MigrateToPluginsBlock",
                "Migrate apply plugin to plugins block",
                List.of()
        ));

        // Dependency recipes
        recipes.add(new RecipeInfo(
                "org.openrewrite.gradle.MigrateToVersionCatalog",
                "Migrate dependencies to version catalog",
                List.of("catalogName")
        ));
        recipes.add(new RecipeInfo(
                "org.openrewrite.gradle.ChangeDependencyVersion",
                "Change a dependency version",
                List.of("groupId", "artifactId", "newVersion")
        ));
        recipes.add(new RecipeInfo(
                "org.openrewrite.gradle.UpgradeDependencyVersion",
                "Upgrade a dependency to a newer version",
                List.of("groupId", "artifactId", "newVersion")
        ));
        recipes.add(new RecipeInfo(
                "org.openrewrite.gradle.RemoveDependency",
                "Remove a dependency",
                List.of("groupId", "artifactId")
        ));

        // Plugin recipes
        recipes.add(new RecipeInfo(
                "org.openrewrite.gradle.plugins.ChangePlugin",
                "Change a plugin ID",
                List.of("pluginIdOld", "pluginIdNew", "newVersion")
        ));
        recipes.add(new RecipeInfo(
                "org.openrewrite.gradle.plugins.UpgradePluginVersion",
                "Upgrade a plugin version",
                List.of("pluginId", "newVersion")
        ));

        return recipes;
    }

    private ProjectConnection connect() {
        return GradleConnector.newConnector()
                .forProjectDirectory(projectPath.toFile())
                .connect();
    }

    private Path generateInitScript(String gradleVersion, String recipes) throws IOException {
        boolean useKotlin = !isVersionLessThan(gradleVersion, "8.0");
        String extension = useKotlin ? ".gradle.kts" : ".gradle";
        Path initScript = Files.createTempFile("openrewrite-init-", extension);

        String content;
        if (useKotlin) {
            content = generateKotlinInitScript(recipes);
        } else {
            content = generateGroovyInitScript(recipes);
        }

        Files.writeString(initScript, content);
        return initScript;
    }

    private String generateKotlinInitScript(String recipes) {
        StringBuilder sb = new StringBuilder();
        sb.append("// Generated by gradle-claude-plugin openrewrite-runner\n");
        sb.append("initscript {\n");
        sb.append("    repositories {\n");
        sb.append("        mavenCentral()\n");
        sb.append("        gradlePluginPortal()\n");
        sb.append("    }\n");
        sb.append("    dependencies {\n");
        sb.append("        classpath(\"org.openrewrite:plugin:latest.release\")\n");

        // Add additional dependencies
        if (additionalDeps != null && !additionalDeps.isEmpty()) {
            for (String dep : additionalDeps.split(",")) {
                sb.append("        classpath(\"").append(dep.trim()).append("\")\n");
            }
        }

        sb.append("    }\n");
        sb.append("}\n\n");

        sb.append("allprojects {\n");
        sb.append("    apply(plugin = org.openrewrite.gradle.RewritePlugin::class.java)\n\n");
        sb.append("    configure<org.openrewrite.gradle.RewriteExtension> {\n");

        for (String recipe : recipes.split(",")) {
            sb.append("        activeRecipe(\"").append(recipe.trim()).append("\")\n");
        }

        if (failOnDryRun) {
            sb.append("        setFailOnDryRunResults(true)\n");
        }

        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    private String generateGroovyInitScript(String recipes) {
        StringBuilder sb = new StringBuilder();
        sb.append("// Generated by gradle-claude-plugin openrewrite-runner\n");
        sb.append("initscript {\n");
        sb.append("    repositories {\n");
        sb.append("        mavenCentral()\n");
        sb.append("        gradlePluginPortal()\n");
        sb.append("    }\n");
        sb.append("    dependencies {\n");
        sb.append("        classpath 'org.openrewrite:plugin:latest.release'\n");

        // Add additional dependencies
        if (additionalDeps != null && !additionalDeps.isEmpty()) {
            for (String dep : additionalDeps.split(",")) {
                sb.append("        classpath '").append(dep.trim()).append("'\n");
            }
        }

        sb.append("    }\n");
        sb.append("}\n\n");

        sb.append("allprojects {\n");
        sb.append("    apply plugin: org.openrewrite.gradle.RewritePlugin\n\n");
        sb.append("    rewrite {\n");

        for (String recipe : recipes.split(",")) {
            sb.append("        activeRecipe '").append(recipe.trim()).append("'\n");
        }

        if (failOnDryRun) {
            sb.append("        failOnDryRunResults = true\n");
        }

        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    private void parseResults(Result result) {
        String output = result.stdout;

        // Count files changed
        Pattern filesChanged = Pattern.compile("(\\d+) files? (?:changed|would be changed)");
        Matcher matcher = filesChanged.matcher(output);
        if (matcher.find()) {
            result.changesDetected = Integer.parseInt(matcher.group(1));
        }

        // Extract changed files
        Pattern filePattern = Pattern.compile("(?:Changed|Would change): (.+\\.(?:gradle|gradle\\.kts|java|kt))");
        matcher = filePattern.matcher(output);
        while (matcher.find()) {
            result.changedFiles.add(matcher.group(1));
        }

        // Check for recipe execution
        if (output.contains("Running recipe")) {
            result.recipesExecuted = true;
        }
    }

    private void printHumanReadable(Result result) {
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("OpenRewrite " + (result.dryRun ? "Dry Run" : "Execution") + " Results");
        System.out.println("=".repeat(60));
        System.out.println();
        System.out.println("Project:  " + result.projectPath);
        System.out.println("Gradle:   " + result.gradleVersion);
        System.out.println("Recipes:  " + String.join(", ", result.recipes));
        System.out.println("Duration: " + result.durationMs + "ms");
        System.out.println();

        if (result.success) {
            System.out.println("Status: " + (result.dryRun ? "PREVIEW" : "SUCCESS"));
            System.out.println();

            if (result.changesDetected > 0) {
                System.out.println(result.dryRun ? "Would change:" : "Changed:");
                System.out.println("  " + result.changesDetected + " file(s)");

                if (!result.changedFiles.isEmpty()) {
                    System.out.println();
                    System.out.println("Files:");
                    for (String file : result.changedFiles) {
                        System.out.println("  - " + file);
                    }
                }
            } else {
                System.out.println("No changes " + (result.dryRun ? "would be made" : "made"));
            }
        } else {
            System.out.println("Status: FAILED");
            System.out.println("Error: " + result.error);
        }

        if (verbose && !result.stderr.isEmpty()) {
            System.out.println();
            System.out.println("Stderr:");
            System.out.println(result.stderr);
        }

        System.out.println();
    }

    private void printSuggestions(SuggestionResult result) {
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("OpenRewrite Recipe Suggestions");
        System.out.println("=".repeat(60));
        System.out.println();
        System.out.println("Project: " + result.projectPath);
        System.out.println("Gradle:  " + result.gradleVersion);
        System.out.println();

        if (result.suggestions.isEmpty()) {
            System.out.println("No suggestions - project follows best practices!");
        } else {
            System.out.println("Suggested recipes:");
            System.out.println();

            int i = 1;
            for (Suggestion s : result.suggestions) {
                System.out.printf("%d. %s%n", i++, s.recipe);
                System.out.println("   " + s.description);
                System.out.printf("   Confidence: %.0f%%%n", s.confidence * 100);
                if (s.options != null) {
                    System.out.println("   Options: " + s.options);
                }
                System.out.println();
            }

            System.out.println("Run with:");
            System.out.println("  /openrewrite dry-run <recipe>  # Preview changes");
            System.out.println("  /openrewrite run <recipe>      # Apply changes");
        }
        System.out.println();
    }

    private boolean isVersionLessThan(String version, String target) {
        if ("unknown".equals(version)) {
            // Can't compare unknown versions - assume older for safety (conservative approach)
            log("Cannot determine if version is less than " + target + " - version is unknown, assuming older");
            return true;
        }

        try {
            String[] vParts = version.split("\\.");
            String[] tParts = target.split("\\.");

            int major1 = Integer.parseInt(vParts[0]);
            int major2 = Integer.parseInt(tParts[0]);

            if (major1 != major2) return major1 < major2;

            int minor1 = vParts.length > 1 ? Integer.parseInt(vParts[1].split("-")[0]) : 0;
            int minor2 = tParts.length > 1 ? Integer.parseInt(tParts[1].split("-")[0]) : 0;

            return minor1 < minor2;
        } catch (NumberFormatException e) {
            // Invalid version format - assume older for safety
            System.err.println("WARNING: Invalid version format '" + version + "', cannot compare with " + target);
            return true;
        }
    }

    private void log(String message) {
        if (verbose) {
            System.err.println("[openrewrite-runner] " + message);
        }
    }

    private void error(String message) {
        System.err.println("ERROR: " + message);
    }

    // Result classes
    static class Result {
        String projectPath;
        String gradleVersion;
        List<String> recipes;
        boolean dryRun;
        boolean success;
        String error;
        long durationMs;
        int changesDetected;
        List<String> changedFiles = new ArrayList<>();
        boolean recipesExecuted;
        String stdout;
        String stderr;
    }

    static class SuggestionResult {
        String projectPath;
        String gradleVersion;
        List<Suggestion> suggestions = new ArrayList<>();
    }

    static class Suggestion {
        String recipe;
        String description;
        double confidence;
        String options;

        Suggestion(String recipe, String description, double confidence, String options) {
            this.recipe = recipe;
            this.description = description;
            this.confidence = confidence;
            this.options = options;
        }
    }

    static class RecipeInfo {
        String name;
        String description;
        List<String> options;

        RecipeInfo(String name, String description, List<String> options) {
            this.name = name;
            this.description = description;
            this.options = options;
        }
    }
}
