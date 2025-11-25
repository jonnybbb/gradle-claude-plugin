///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//REPOS mavencentral,gradle=https://repo.gradle.org/gradle/libs-releases
//DEPS org.gradle:gradle-tooling-api:9.2.1
//DEPS org.slf4j:slf4j-simple:2.0.9
//DEPS com.google.code.gson:gson:2.10.1

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * Gradle Migration Fixer - Phase 4 of Active Automation
 *
 * Analyzes build files for deprecated APIs and generates migration fixes
 * for upgrading between Gradle versions (7→8, 8→9).
 *
 * Usage: migration-fixer.java <project-dir> [--target VERSION] [--json]
 *
 * Output: Structured migration plan with:
 *   - Current version detection
 *   - Deprecated API usage
 *   - Exact replacement code
 *   - Migration path recommendations
 */
class migration_fixer {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static boolean jsonOutput = false;
    private static String targetVersion = "9.0";

    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");

        if (args.length < 1) {
            System.err.println("Usage: migration-fixer.java <project-dir> [--target VERSION] [--json]");
            System.err.println("  project-dir: Path to Gradle project root");
            System.err.println("  --target VERSION: Target Gradle version (default: 9.0)");
            System.err.println("  --json: Output structured JSON migration plan");
            System.exit(1);
        }

        File projectDir = new File(args[0]);

        for (int i = 1; i < args.length; i++) {
            if ("--json".equals(args[i])) {
                jsonOutput = true;
            } else if ("--target".equals(args[i]) && i + 1 < args.length) {
                targetVersion = args[++i];
            }
        }

        if (!projectDir.exists() || !projectDir.isDirectory()) {
            System.err.println("Error: Project directory does not exist: " + projectDir);
            System.exit(1);
        }

        try {
            MigrationPlan plan = analyzeProject(projectDir);

            if (jsonOutput) {
                System.out.println(gson.toJson(plan));
            } else {
                printHumanReadable(plan);
            }

        } catch (Exception e) {
            System.err.println("Error analyzing project: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static MigrationPlan analyzeProject(File projectDir) throws IOException {
        MigrationPlan plan = new MigrationPlan();
        plan.project = projectDir.getAbsolutePath();
        plan.targetVersion = targetVersion;
        plan.analyzedFiles = new ArrayList<>();
        plan.fixes = new ArrayList<>();

        // Detect current Gradle version
        plan.currentVersion = detectGradleVersion(projectDir);
        plan.migrationPath = determineMigrationPath(plan.currentVersion, targetVersion);

        // Find all build files (Groovy and Kotlin)
        List<Path> buildFiles = Files.walk(projectDir.toPath())
            .filter(p -> {
                String name = p.getFileName().toString();
                String pathStr = p.toString();
                boolean isBuildFile = name.equals("build.gradle") || name.equals("build.gradle.kts") ||
                                      name.equals("settings.gradle") || name.equals("settings.gradle.kts");
                boolean isInGradleDir = pathStr.contains("/.gradle/") || pathStr.contains(File.separator + ".gradle" + File.separator);
                boolean isInBuildDir = pathStr.contains("/build/") || pathStr.contains(File.separator + "build" + File.separator);
                return isBuildFile && !isInGradleDir && !isInBuildDir;
            })
            .collect(Collectors.toList());

        int fixId = 1;
        for (Path buildFile : buildFiles) {
            String relativePath = projectDir.toPath().relativize(buildFile).toString();
            plan.analyzedFiles.add(relativePath);
            boolean isGroovy = !buildFile.toString().endsWith(".kts");
            fixId = analyzeFile(buildFile, relativePath, plan.fixes, fixId, isGroovy, plan.migrationPath);
        }

        // Also check gradle.properties
        Path gradleProps = projectDir.toPath().resolve("gradle.properties");
        if (Files.exists(gradleProps)) {
            plan.analyzedFiles.add("gradle.properties");
            fixId = analyzeGradleProperties(gradleProps, plan.fixes, fixId);
        }

        // Calculate summary
        plan.summary = new Summary();
        plan.summary.total = plan.fixes.size();
        plan.summary.highConfidence = (int) plan.fixes.stream()
            .filter(f -> "HIGH".equals(f.confidence)).count();
        plan.summary.mediumConfidence = (int) plan.fixes.stream()
            .filter(f -> "MEDIUM".equals(f.confidence)).count();
        plan.summary.lowConfidence = (int) plan.fixes.stream()
            .filter(f -> "LOW".equals(f.confidence)).count();
        plan.summary.autoFixable = (int) plan.fixes.stream()
            .filter(f -> f.autoFixable).count();
        plan.summary.byCategory = plan.fixes.stream()
            .collect(Collectors.groupingBy(f -> f.category, Collectors.counting()));

        return plan;
    }

    private static String detectGradleVersion(File projectDir) throws IOException {
        Path wrapperProps = projectDir.toPath().resolve("gradle/wrapper/gradle-wrapper.properties");
        if (Files.exists(wrapperProps)) {
            String content = Files.readString(wrapperProps);
            Pattern versionPattern = Pattern.compile("gradle-(\\d+\\.\\d+(?:\\.\\d+)?)-");
            Matcher matcher = versionPattern.matcher(content);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "unknown";
    }

    private static List<String> determineMigrationPath(String current, String target) {
        List<String> path = new ArrayList<>();
        if ("unknown".equals(current)) {
            path.add("7→8");
            path.add("8→9");
            return path;
        }

        int currentMajor = Integer.parseInt(current.split("\\.")[0]);
        int targetMajor = Integer.parseInt(target.split("\\.")[0]);

        for (int v = currentMajor; v < targetMajor; v++) {
            path.add(v + "→" + (v + 1));
        }
        return path;
    }

    private static int analyzeFile(Path file, String relativePath, List<Fix> fixes, int fixId,
            boolean isGroovy, List<String> migrationPath) throws IOException {
        String content = Files.readString(file);
        String[] lines = content.split("\n", -1);

        // Gradle 7→8 deprecations
        if (migrationPath.contains("7→8") || migrationPath.contains("8→9")) {
            fixId = findArchivesBaseName(content, lines, relativePath, fixes, fixId, isGroovy);
            fixId = findMainClassName(content, lines, relativePath, fixes, fixId, isGroovy);
            fixId = findArchiveName(content, lines, relativePath, fixes, fixId, isGroovy);
            fixId = findDestinationDir(content, lines, relativePath, fixes, fixId, isGroovy);
            fixId = findEagerTaskDefinition(content, lines, relativePath, fixes, fixId, isGroovy);
            fixId = findBuildDirUsage(content, lines, relativePath, fixes, fixId, isGroovy);
            fixId = findSourceTargetCompatibility(content, lines, relativePath, fixes, fixId, isGroovy);
            fixId = findEagerTaskDependency(content, lines, relativePath, fixes, fixId, isGroovy);
            fixId = findGetByName(content, lines, relativePath, fixes, fixId, isGroovy);
        }

        // Gradle 8→9 specific
        if (migrationPath.contains("8→9")) {
            fixId = findProjectConventions(content, lines, relativePath, fixes, fixId, isGroovy);
        }

        return fixId;
    }

    // ========== Pattern Detection Methods ==========

    private static int findArchivesBaseName(String content, String[] lines, String file,
            List<Fix> fixes, int fixId, boolean isGroovy) {
        Pattern pattern = Pattern.compile("archivesBaseName\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            String value = matcher.group(1);
            String original = matcher.group(0);

            String replacement = isGroovy
                ? "base {\n    archivesName = '" + value + "'\n}"
                : "base {\n    archivesName.set(\"" + value + "\")\n}";

            fixes.add(new Fix(
                "MIG-" + String.format("%03d", fixId++),
                file,
                lineNum,
                1,
                "Deprecated archivesBaseName property",
                "DEPRECATED_PROPERTY",
                "7→8",
                "HIGH",
                true,
                original,
                replacement,
                "archivesBaseName is deprecated. Use base { archivesName } extension instead."
            ));
        }
        return fixId;
    }

    private static int findMainClassName(String content, String[] lines, String file,
            List<Fix> fixes, int fixId, boolean isGroovy) {
        Pattern pattern = Pattern.compile("mainClassName\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            String value = matcher.group(1);
            String original = matcher.group(0);

            String replacement = isGroovy
                ? "mainClass = '" + value + "'"
                : "mainClass.set(\"" + value + "\")";

            fixes.add(new Fix(
                "MIG-" + String.format("%03d", fixId++),
                file,
                lineNum,
                1,
                "Deprecated mainClassName property",
                "DEPRECATED_PROPERTY",
                "7→8",
                "HIGH",
                true,
                original,
                replacement,
                "mainClassName is deprecated. Use mainClass property instead."
            ));
        }
        return fixId;
    }

    private static int findArchiveName(String content, String[] lines, String file,
            List<Fix> fixes, int fixId, boolean isGroovy) {
        Pattern pattern = Pattern.compile("archiveName\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            String value = matcher.group(1);
            String original = matcher.group(0);

            String replacement = isGroovy
                ? "archiveFileName = '" + value + "'"
                : "archiveFileName.set(\"" + value + "\")";

            fixes.add(new Fix(
                "MIG-" + String.format("%03d", fixId++),
                file,
                lineNum,
                1,
                "Deprecated archiveName property",
                "DEPRECATED_PROPERTY",
                "7→8",
                "HIGH",
                true,
                original,
                replacement,
                "archiveName is deprecated. Use archiveFileName property instead."
            ));
        }
        return fixId;
    }

    private static int findDestinationDir(String content, String[] lines, String file,
            List<Fix> fixes, int fixId, boolean isGroovy) {
        Pattern pattern = Pattern.compile("destinationDir\\s*=\\s*file\\(['\"]?([^'\"\\)]+)['\"]?\\)");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            String path = matcher.group(1);
            String original = matcher.group(0);

            // Convert $buildDir to layout.buildDirectory
            String newPath = path.replace("$buildDir", "${layout.buildDirectory.get().asFile}");
            String replacement = isGroovy
                ? "destinationDirectory = file('" + newPath + "')"
                : "destinationDirectory.set(file(\"" + newPath + "\"))";

            fixes.add(new Fix(
                "MIG-" + String.format("%03d", fixId++),
                file,
                lineNum,
                1,
                "Deprecated destinationDir property",
                "DEPRECATED_PROPERTY",
                "7→8",
                "HIGH",
                true,
                original,
                replacement,
                "destinationDir is deprecated. Use destinationDirectory property instead."
            ));
        }
        return fixId;
    }

    private static int findEagerTaskDefinition(String content, String[] lines, String file,
            List<Fix> fixes, int fixId, boolean isGroovy) {
        // Match Groovy-style task definition: task taskName { } or task taskName(type: X) { }
        Pattern pattern = Pattern.compile("^task\\s+(\\w+)(?:\\s*\\(type:\\s*(\\w+)\\))?\\s*\\{",
            Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            String taskName = matcher.group(1);
            String taskType = matcher.group(2);
            String original = matcher.group(0);

            String replacement;
            if (taskType != null) {
                replacement = isGroovy
                    ? "tasks.register('" + taskName + "', " + taskType + ") {"
                    : "tasks.register<" + taskType + ">(\"" + taskName + "\") {";
            } else {
                replacement = isGroovy
                    ? "tasks.register('" + taskName + "') {"
                    : "tasks.register(\"" + taskName + "\") {";
            }

            fixes.add(new Fix(
                "MIG-" + String.format("%03d", fixId++),
                file,
                lineNum,
                1,
                "Eager task definition",
                "TASK_AVOIDANCE",
                "7→8",
                "HIGH",
                true,
                original,
                replacement,
                "Eager task definitions create tasks immediately. Use tasks.register() for lazy registration."
            ));
        }
        return fixId;
    }

    private static int findBuildDirUsage(String content, String[] lines, String file,
            List<Fix> fixes, int fixId, boolean isGroovy) {
        // Find $buildDir or "${buildDir}" usage in strings
        Pattern pattern = Pattern.compile("\"\\$\\{?buildDir\\}?(/[^\"]*)?\"");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            String suffix = matcher.group(1) != null ? matcher.group(1) : "";
            String original = matcher.group(0);

            String replacement = "\"${layout.buildDirectory.get().asFile}" + suffix + "\"";

            fixes.add(new Fix(
                "MIG-" + String.format("%03d", fixId++),
                file,
                lineNum,
                1,
                "Direct $buildDir access",
                "DEPRECATED_API",
                "7→8",
                "HIGH",
                true,
                original,
                replacement,
                "Direct buildDir access is deprecated. Use layout.buildDirectory instead."
            ));
        }
        return fixId;
    }

    private static int findSourceTargetCompatibility(String content, String[] lines, String file,
            List<Fix> fixes, int fixId, boolean isGroovy) {
        // sourceCompatibility = 'X' (top-level, not in java block)
        Pattern pattern = Pattern.compile("^(sourceCompatibility|targetCompatibility)\\s*=\\s*['\"]?(\\d+)['\"]?",
            Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);

        Set<Integer> reportedLines = new HashSet<>();
        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            if (reportedLines.contains(lineNum)) continue;
            reportedLines.add(lineNum);

            String prop = matcher.group(1);
            String version = matcher.group(2);
            String original = matcher.group(0);

            String replacement = isGroovy
                ? "java {\n    " + prop + " = JavaVersion.VERSION_" + version + "\n}"
                : "java {\n    " + prop + ".set(JavaVersion.VERSION_" + version + ")\n}";

            fixes.add(new Fix(
                "MIG-" + String.format("%03d", fixId++),
                file,
                lineNum,
                1,
                "Top-level " + prop + " convention",
                "CONVENTION_DEPRECATION",
                "8→9",
                "MEDIUM",
                false,
                original,
                replacement,
                "Top-level source/targetCompatibility is deprecated. Configure in java { } block or use toolchain."
            ));
        }
        return fixId;
    }

    private static int findEagerTaskDependency(String content, String[] lines, String file,
            List<Fix> fixes, int fixId, boolean isGroovy) {
        // taskName.dependsOn otherTask
        Pattern pattern = Pattern.compile("^(\\w+)\\.dependsOn\\s+(\\w+)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            String task = matcher.group(1);
            String dependency = matcher.group(2);
            String original = matcher.group(0);

            String replacement = isGroovy
                ? "tasks.named('" + task + "') { dependsOn '" + dependency + "' }"
                : "tasks.named(\"" + task + "\") { dependsOn(\"" + dependency + "\") }";

            fixes.add(new Fix(
                "MIG-" + String.format("%03d", fixId++),
                file,
                lineNum,
                1,
                "Eager task dependency declaration",
                "TASK_AVOIDANCE",
                "7→8",
                "HIGH",
                true,
                original,
                replacement,
                "Direct task.dependsOn eagerly resolves tasks. Use tasks.named() for lazy configuration."
            ));
        }
        return fixId;
    }

    private static int findGetByName(String content, String[] lines, String file,
            List<Fix> fixes, int fixId, boolean isGroovy) {
        Pattern pattern = Pattern.compile("tasks\\.getByName\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            String taskName = matcher.group(1);
            String original = matcher.group(0);

            String replacement = isGroovy
                ? "tasks.named('" + taskName + "')"
                : "tasks.named(\"" + taskName + "\")";

            fixes.add(new Fix(
                "MIG-" + String.format("%03d", fixId++),
                file,
                lineNum,
                1,
                "Eager tasks.getByName() access",
                "TASK_AVOIDANCE",
                "7→8",
                "HIGH",
                true,
                original,
                replacement,
                "tasks.getByName() eagerly resolves tasks. Use tasks.named() for lazy access."
            ));
        }
        return fixId;
    }

    private static int findProjectConventions(String content, String[] lines, String file,
            List<Fix> fixes, int fixId, boolean isGroovy) {
        // Project convention patterns removed in Gradle 9
        // This is a placeholder for 8→9 specific patterns
        return fixId;
    }

    private static int analyzeGradleProperties(Path propsFile, List<Fix> fixes, int fixId) throws IOException {
        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(propsFile)) {
            props.load(is);
        }

        // Check for missing recommended properties
        if (!props.containsKey("org.gradle.parallel")) {
            fixes.add(new Fix(
                "MIG-" + String.format("%03d", fixId++),
                "gradle.properties",
                0,
                1,
                "Missing parallel execution setting",
                "PERFORMANCE",
                "general",
                "HIGH",
                true,
                "# (missing)",
                "org.gradle.parallel=true",
                "Enable parallel execution for faster builds."
            ));
        }

        if (!props.containsKey("org.gradle.caching")) {
            fixes.add(new Fix(
                "MIG-" + String.format("%03d", fixId++),
                "gradle.properties",
                0,
                1,
                "Missing build cache setting",
                "PERFORMANCE",
                "general",
                "HIGH",
                true,
                "# (missing)",
                "org.gradle.caching=true",
                "Enable build cache for faster incremental builds."
            ));
        }

        if (!props.containsKey("org.gradle.configuration-cache")) {
            fixes.add(new Fix(
                "MIG-" + String.format("%03d", fixId++),
                "gradle.properties",
                0,
                1,
                "Missing configuration cache setting",
                "PERFORMANCE",
                "8→9",
                "MEDIUM",
                true,
                "# (missing)",
                "org.gradle.configuration-cache=true",
                "Enable configuration cache for faster configuration phase (requires Gradle 8.1+)."
            ));
        }

        return fixId;
    }

    // ========== Utility Methods ==========

    private static int getLineNumber(String content, int position) {
        int line = 1;
        for (int i = 0; i < position && i < content.length(); i++) {
            if (content.charAt(i) == '\n') line++;
        }
        return line;
    }

    // ========== Output Methods ==========

    private static void printHumanReadable(MigrationPlan plan) {
        System.out.println("=== Gradle Migration Plan ===\n");

        System.out.println("Project: " + plan.project);
        System.out.println("Current Version: " + plan.currentVersion);
        System.out.println("Target Version: " + plan.targetVersion);
        System.out.println("Migration Path: " + String.join(" → ", plan.migrationPath));
        System.out.println();

        System.out.println("Files analyzed: " + plan.analyzedFiles.size());
        for (String f : plan.analyzedFiles) {
            System.out.println("  - " + f);
        }
        System.out.println();

        System.out.println("Summary:");
        System.out.println("  Total fixes:      " + plan.summary.total);
        System.out.println("  HIGH confidence:  " + plan.summary.highConfidence);
        System.out.println("  MEDIUM confidence:" + plan.summary.mediumConfidence);
        System.out.println("  LOW confidence:   " + plan.summary.lowConfidence);
        System.out.println("  Auto-fixable:     " + plan.summary.autoFixable);
        System.out.println();

        if (plan.summary.byCategory != null && !plan.summary.byCategory.isEmpty()) {
            System.out.println("By Category:");
            for (Map.Entry<String, Long> entry : plan.summary.byCategory.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            }
            System.out.println();
        }

        if (!plan.fixes.isEmpty()) {
            System.out.println("Fixes:\n");

            // Group by migration step
            Map<String, List<Fix>> byStep = plan.fixes.stream()
                .collect(Collectors.groupingBy(f -> f.migrationStep));

            for (String step : byStep.keySet().stream().sorted().toList()) {
                System.out.println("── " + step + " ──\n");

                for (Fix fix : byStep.get(step)) {
                    String icon = switch (fix.confidence) {
                        case "HIGH" -> "●";
                        case "MEDIUM" -> "◐";
                        case "LOW" -> "○";
                        default -> "?";
                    };
                    String autoIcon = fix.autoFixable ? "✓" : "✗";

                    System.out.println(fix.id + " [" + icon + " " + fix.confidence + "] [" + autoIcon + " auto]");
                    System.out.println("  File: " + fix.file + (fix.line > 0 ? ":" + fix.line : ""));
                    System.out.println("  Issue: " + fix.issue);
                    System.out.println("  Category: " + fix.category);
                    System.out.println();
                    System.out.println("  Original:");
                    System.out.println("    " + fix.original);
                    System.out.println();
                    System.out.println("  Replacement:");
                    for (String line : fix.replacement.split("\n")) {
                        System.out.println("    " + line);
                    }
                    System.out.println();
                    System.out.println("  " + fix.explanation);
                    System.out.println();
                    System.out.println("  " + "─".repeat(60));
                    System.out.println();
                }
            }
        } else {
            System.out.println("✓ No migration issues found!");
        }

        // Wrapper update recommendation
        if (!"unknown".equals(plan.currentVersion) && !plan.currentVersion.startsWith(plan.targetVersion.split("\\.")[0])) {
            System.out.println("\n═══════════════════════════════════════════════════════════════");
            System.out.println("WRAPPER UPDATE REQUIRED");
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println();
            System.out.println("After applying fixes, update the Gradle wrapper:");
            System.out.println("  ./gradlew wrapper --gradle-version " + plan.targetVersion);
            System.out.println();
        }
    }

    // ========== Data Classes ==========

    static class MigrationPlan {
        String project;
        String currentVersion;
        String targetVersion;
        List<String> migrationPath;
        List<String> analyzedFiles;
        List<Fix> fixes;
        Summary summary;
    }

    static class Fix {
        String id;
        String file;
        int line;
        int column;
        String issue;
        String category;
        String migrationStep;
        String confidence;
        boolean autoFixable;
        String original;
        String replacement;
        String explanation;

        Fix(String id, String file, int line, int column, String issue, String category,
                String migrationStep, String confidence, boolean autoFixable,
                String original, String replacement, String explanation) {
            this.id = id;
            this.file = file;
            this.line = line;
            this.column = column;
            this.issue = issue;
            this.category = category;
            this.migrationStep = migrationStep;
            this.confidence = confidence;
            this.autoFixable = autoFixable;
            this.original = original;
            this.replacement = replacement;
            this.explanation = explanation;
        }
    }

    static class Summary {
        int total;
        int highConfidence;
        int mediumConfidence;
        int lowConfidence;
        int autoFixable;
        Map<String, Long> byCategory;
    }
}
