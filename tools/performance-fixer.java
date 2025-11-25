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
 * Gradle Performance Fixer - Phase 5 of Active Automation
 *
 * Analyzes Gradle projects for performance issues and generates
 * optimization fixes for gradle.properties and build scripts.
 *
 * Usage: performance-fixer.java <project-dir> [--json] [--benchmark]
 *
 * Output: Structured optimization plan with:
 *   - Missing gradle.properties settings
 *   - Build script anti-patterns
 *   - JVM tuning recommendations
 *   - Expected impact estimates
 */
class performance_fixer {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static boolean jsonOutput = false;
    private static boolean benchmark = false;

    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");

        if (args.length < 1) {
            System.err.println("Usage: performance-fixer.java <project-dir> [--json] [--benchmark]");
            System.err.println("  project-dir: Path to Gradle project root");
            System.err.println("  --json: Output structured JSON optimization plan");
            System.err.println("  --benchmark: Include benchmark commands");
            System.exit(1);
        }

        File projectDir = new File(args[0]);

        for (int i = 1; i < args.length; i++) {
            if ("--json".equals(args[i])) jsonOutput = true;
            if ("--benchmark".equals(args[i])) benchmark = true;
        }

        if (!projectDir.exists() || !projectDir.isDirectory()) {
            System.err.println("Error: Project directory does not exist: " + projectDir);
            System.exit(1);
        }

        try {
            OptimizationPlan plan = analyzeProject(projectDir);

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

    private static OptimizationPlan analyzeProject(File projectDir) throws IOException {
        OptimizationPlan plan = new OptimizationPlan();
        plan.project = projectDir.getAbsolutePath();
        plan.analyzedFiles = new ArrayList<>();
        plan.fixes = new ArrayList<>();

        // Analyze gradle.properties
        int fixId = 1;
        fixId = analyzeGradleProperties(projectDir, plan, fixId);

        // Analyze build scripts
        List<Path> buildFiles = Files.walk(projectDir.toPath())
            .filter(p -> {
                String name = p.getFileName().toString();
                String pathStr = p.toString();
                boolean isBuildFile = name.equals("build.gradle") || name.equals("build.gradle.kts");
                boolean isInGradleDir = pathStr.contains("/.gradle/");
                boolean isInBuildDir = pathStr.contains("/build/");
                return isBuildFile && !isInGradleDir && !isInBuildDir;
            })
            .collect(Collectors.toList());

        for (Path buildFile : buildFiles) {
            String relativePath = projectDir.toPath().relativize(buildFile).toString();
            plan.analyzedFiles.add(relativePath);
            boolean isKotlin = buildFile.toString().endsWith(".kts");
            fixId = analyzeBuildScript(buildFile, relativePath, plan.fixes, fixId, isKotlin);
        }

        // Calculate summary
        plan.summary = new Summary();
        plan.summary.total = plan.fixes.size();
        plan.summary.highImpact = (int) plan.fixes.stream()
            .filter(f -> "HIGH".equals(f.impact)).count();
        plan.summary.mediumImpact = (int) plan.fixes.stream()
            .filter(f -> "MEDIUM".equals(f.impact)).count();
        plan.summary.lowImpact = (int) plan.fixes.stream()
            .filter(f -> "LOW".equals(f.impact)).count();
        plan.summary.autoFixable = (int) plan.fixes.stream()
            .filter(f -> f.autoFixable).count();
        plan.summary.byCategory = plan.fixes.stream()
            .collect(Collectors.groupingBy(f -> f.category, Collectors.counting()));

        // Estimate total impact
        plan.summary.estimatedImprovement = estimateImprovement(plan.fixes);

        // Add benchmark commands
        if (benchmark) {
            plan.benchmarkCommands = generateBenchmarkCommands();
        }

        return plan;
    }

    private static int analyzeGradleProperties(File projectDir, OptimizationPlan plan, int fixId)
            throws IOException {
        Path propsPath = projectDir.toPath().resolve("gradle.properties");
        Properties props = new Properties();

        if (Files.exists(propsPath)) {
            plan.analyzedFiles.add("gradle.properties");
            try (InputStream is = Files.newInputStream(propsPath)) {
                props.load(is);
            }
        }

        // Check parallel execution
        if (!props.containsKey("org.gradle.parallel") ||
            !"true".equals(props.getProperty("org.gradle.parallel"))) {
            plan.fixes.add(new Fix(
                "PERF-" + String.format("%03d", fixId++),
                "gradle.properties",
                0, 1,
                "Enable parallel execution",
                "PARALLEL",
                "HIGH",
                true,
                props.containsKey("org.gradle.parallel")
                    ? "org.gradle.parallel=" + props.getProperty("org.gradle.parallel")
                    : "# (missing)",
                "org.gradle.parallel=true",
                "Parallel execution builds independent subprojects simultaneously.",
                "10-40% faster multi-project builds"
            ));
        }

        // Check build cache
        if (!props.containsKey("org.gradle.caching") ||
            !"true".equals(props.getProperty("org.gradle.caching"))) {
            plan.fixes.add(new Fix(
                "PERF-" + String.format("%03d", fixId++),
                "gradle.properties",
                0, 1,
                "Enable build cache",
                "CACHING",
                "HIGH",
                true,
                props.containsKey("org.gradle.caching")
                    ? "org.gradle.caching=" + props.getProperty("org.gradle.caching")
                    : "# (missing)",
                "org.gradle.caching=true",
                "Build cache reuses outputs from previous builds.",
                "20-90% faster incremental builds"
            ));
        }

        // Check configuration cache
        if (!props.containsKey("org.gradle.configuration-cache") ||
            !"true".equals(props.getProperty("org.gradle.configuration-cache"))) {
            plan.fixes.add(new Fix(
                "PERF-" + String.format("%03d", fixId++),
                "gradle.properties",
                0, 1,
                "Enable configuration cache",
                "CONFIG_CACHE",
                "HIGH",
                false, // Not auto-fixable - run /fix-config-cache first to ensure compatibility
                props.containsKey("org.gradle.configuration-cache")
                    ? "org.gradle.configuration-cache=" + props.getProperty("org.gradle.configuration-cache")
                    : "# (missing)",
                "org.gradle.configuration-cache=true",
                "Configuration cache caches the configuration phase result. Run /fix-config-cache first to ensure code compatibility before enabling.",
                "30-70% faster configuration phase"
            ));
        }

        // Check file system watching
        if (!props.containsKey("org.gradle.vfs.watch") ||
            !"true".equals(props.getProperty("org.gradle.vfs.watch"))) {
            plan.fixes.add(new Fix(
                "PERF-" + String.format("%03d", fixId++),
                "gradle.properties",
                0, 1,
                "Enable file system watching",
                "VFS_WATCH",
                "MEDIUM",
                true,
                props.containsKey("org.gradle.vfs.watch")
                    ? "org.gradle.vfs.watch=" + props.getProperty("org.gradle.vfs.watch")
                    : "# (missing)",
                "org.gradle.vfs.watch=true",
                "File system watching improves incremental build performance.",
                "5-20% faster incremental builds"
            ));
        }

        // Check JVM args
        String jvmArgs = props.getProperty("org.gradle.jvmargs", "");
        fixId = analyzeJvmArgs(jvmArgs, plan.fixes, fixId);

        return fixId;
    }

    private static int analyzeJvmArgs(String jvmArgs, List<Fix> fixes, int fixId) {
        // Check heap size
        if (!jvmArgs.contains("-Xmx") || jvmArgs.matches(".*-Xmx[0-9]+[mM].*") &&
            !jvmArgs.matches(".*-Xmx[2-9][gG].*")) {
            String currentHeap = extractArg(jvmArgs, "-Xmx");
            if (currentHeap == null || parseMemory(currentHeap) < 2048) {
                fixes.add(new Fix(
                    "PERF-" + String.format("%03d", fixId++),
                    "gradle.properties",
                    0, 1,
                    "Increase Gradle daemon heap size",
                    "JVM_ARGS",
                    "MEDIUM",
                    true,
                    currentHeap != null ? "-Xmx" + currentHeap : "# (no heap specified)",
                    "-Xmx2g",
                    "Larger heap reduces GC pressure for complex builds.",
                    "5-15% faster for large projects"
                ));
            }
        }

        // Check for modern GC
        if (!jvmArgs.contains("UseG1GC") && !jvmArgs.contains("UseZGC") && !jvmArgs.contains("UseShenandoahGC")) {
            fixes.add(new Fix(
                "PERF-" + String.format("%03d", fixId++),
                "gradle.properties",
                0, 1,
                "Use modern garbage collector",
                "JVM_ARGS",
                "LOW",
                true,
                "# (default GC)",
                "-XX:+UseG1GC",
                "G1GC provides better performance for Gradle workloads.",
                "2-10% faster with reduced pause times"
            ));
        }

        // Check metaspace
        if (!jvmArgs.contains("MaxMetaspaceSize")) {
            fixes.add(new Fix(
                "PERF-" + String.format("%03d", fixId++),
                "gradle.properties",
                0, 1,
                "Set metaspace size limit",
                "JVM_ARGS",
                "LOW",
                true,
                "# (no metaspace limit)",
                "-XX:MaxMetaspaceSize=512m",
                "Explicit metaspace limit prevents memory bloat.",
                "Improved daemon stability"
            ));
        }

        return fixId;
    }

    private static int analyzeBuildScript(Path file, String relativePath, List<Fix> fixes,
            int fixId, boolean isKotlin) throws IOException {
        String content = Files.readString(file);
        String[] lines = content.split("\n", -1);

        // Check for tasks.all {} anti-pattern
        Pattern tasksAll = Pattern.compile("tasks\\.all\\s*\\{");
        Matcher matcher = tasksAll.matcher(content);
        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            fixes.add(new Fix(
                "PERF-" + String.format("%03d", fixId++),
                relativePath,
                lineNum, 1,
                "Replace tasks.all with tasks.configureEach",
                "TASK_AVOIDANCE",
                "MEDIUM",
                true,
                "tasks.all {",
                "tasks.configureEach {",
                "tasks.all {} eagerly configures all tasks. configureEach is lazy.",
                "Faster configuration phase"
            ));
        }

        // Check for tasks.withType without configureEach
        Pattern withTypeEager = Pattern.compile("tasks\\.withType\\s*[<(][^)>]+[>)]\\s*\\{");
        matcher = withTypeEager.matcher(content);
        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            String original = matcher.group(0);
            // Only flag if not already using configureEach pattern
            if (!content.substring(Math.max(0, matcher.start() - 20), matcher.start())
                    .contains("configureEach")) {
                String replacement = isKotlin
                    ? original.replace("withType", "withType").replace("{", ".configureEach {")
                    : original.replace("{", ".configureEach {");

                fixes.add(new Fix(
                    "PERF-" + String.format("%03d", fixId++),
                    relativePath,
                    lineNum, 1,
                    "Use configureEach with withType",
                    "TASK_AVOIDANCE",
                    "MEDIUM",
                    true,
                    original,
                    replacement,
                    "withType {} eagerly configures matching tasks. Add .configureEach for lazy configuration.",
                    "Faster configuration phase"
                ));
            }
        }

        // Check for allprojects/subprojects blocks
        Pattern allSubprojects = Pattern.compile("(allprojects|subprojects)\\s*\\{");
        matcher = allSubprojects.matcher(content);
        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            String blockType = matcher.group(1);
            fixes.add(new Fix(
                "PERF-" + String.format("%03d", fixId++),
                relativePath,
                lineNum, 1,
                "Consider convention plugins instead of " + blockType,
                "BUILD_STRUCTURE",
                "LOW",
                false,
                blockType + " {",
                "// Use convention plugins in buildSrc or included build",
                blockType + " blocks apply configuration eagerly. Convention plugins are more performant and maintainable.",
                "Better build organization, easier maintenance"
            ));
        }

        // Check for dependency resolution at configuration time
        Pattern configResolve = Pattern.compile("configurations\\.[a-zA-Z]+\\.resolve\\(\\)");
        matcher = configResolve.matcher(content);
        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            fixes.add(new Fix(
                "PERF-" + String.format("%03d", fixId++),
                relativePath,
                lineNum, 1,
                "Avoid resolving configurations at configuration time",
                "CONFIG_TIME_RESOLUTION",
                "HIGH",
                false,
                matcher.group(0),
                "// Move resolution to task execution time",
                "Resolving configurations during configuration slows every build invocation.",
                "Significantly faster configuration"
            ));
        }

        // Check for afterEvaluate
        Pattern afterEval = Pattern.compile("afterEvaluate\\s*\\{");
        matcher = afterEval.matcher(content);
        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            fixes.add(new Fix(
                "PERF-" + String.format("%03d", fixId++),
                relativePath,
                lineNum, 1,
                "Avoid afterEvaluate blocks",
                "LIFECYCLE",
                "MEDIUM",
                false,
                "afterEvaluate {",
                "// Use lazy configuration APIs instead",
                "afterEvaluate delays configuration and can cause ordering issues. Use lazy APIs.",
                "More predictable, faster configuration"
            ));
        }

        return fixId;
    }

    private static String extractArg(String jvmArgs, String prefix) {
        Pattern pattern = Pattern.compile(prefix + "([0-9]+[mMgG]?)");
        Matcher matcher = pattern.matcher(jvmArgs);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static long parseMemory(String mem) {
        if (mem == null) return 0;
        mem = mem.toLowerCase();
        long value = Long.parseLong(mem.replaceAll("[^0-9]", ""));
        if (mem.contains("g")) return value * 1024;
        return value;
    }

    private static int getLineNumber(String content, int position) {
        int line = 1;
        for (int i = 0; i < position && i < content.length(); i++) {
            if (content.charAt(i) == '\n') line++;
        }
        return line;
    }

    private static String estimateImprovement(List<Fix> fixes) {
        int highCount = (int) fixes.stream().filter(f -> "HIGH".equals(f.impact)).count();
        int mediumCount = (int) fixes.stream().filter(f -> "MEDIUM".equals(f.impact)).count();

        if (highCount >= 3) return "30-60% faster builds possible";
        if (highCount >= 1) return "15-40% faster builds possible";
        if (mediumCount >= 2) return "10-20% faster builds possible";
        return "Minor improvements possible";
    }

    private static List<String> generateBenchmarkCommands() {
        List<String> commands = new ArrayList<>();
        commands.add("# Benchmark commands (run before and after optimizations):");
        commands.add("");
        commands.add("# Clean build timing");
        commands.add("./gradlew clean && time ./gradlew build");
        commands.add("");
        commands.add("# Incremental build timing");
        commands.add("time ./gradlew build");
        commands.add("");
        commands.add("# Configuration time (with --dry-run)");
        commands.add("time ./gradlew help --dry-run");
        commands.add("");
        commands.add("# Build scan for detailed analysis");
        commands.add("./gradlew build --scan");
        return commands;
    }

    private static void printHumanReadable(OptimizationPlan plan) {
        System.out.println("=== Gradle Performance Optimization Plan ===\n");

        System.out.println("Project: " + plan.project);
        System.out.println("Files analyzed: " + plan.analyzedFiles.size());
        for (String f : plan.analyzedFiles) {
            System.out.println("  - " + f);
        }
        System.out.println();

        System.out.println("Summary:");
        System.out.println("  Total optimizations: " + plan.summary.total);
        System.out.println("  HIGH impact:         " + plan.summary.highImpact);
        System.out.println("  MEDIUM impact:       " + plan.summary.mediumImpact);
        System.out.println("  LOW impact:          " + plan.summary.lowImpact);
        System.out.println("  Auto-fixable:        " + plan.summary.autoFixable);
        System.out.println();
        System.out.println("  Estimated improvement: " + plan.summary.estimatedImprovement);
        System.out.println();

        if (plan.summary.byCategory != null && !plan.summary.byCategory.isEmpty()) {
            System.out.println("By Category:");
            for (Map.Entry<String, Long> entry : plan.summary.byCategory.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            }
            System.out.println();
        }

        if (!plan.fixes.isEmpty()) {
            System.out.println("Optimizations:\n");

            for (Fix fix : plan.fixes) {
                String icon = switch (fix.impact) {
                    case "HIGH" -> "●";
                    case "MEDIUM" -> "◐";
                    case "LOW" -> "○";
                    default -> "?";
                };
                String autoIcon = fix.autoFixable ? "✓" : "✗";

                System.out.println(fix.id + " [" + icon + " " + fix.impact + " impact] [" + autoIcon + " auto]");
                System.out.println("  File: " + fix.file + (fix.line > 0 ? ":" + fix.line : ""));
                System.out.println("  Issue: " + fix.issue);
                System.out.println("  Category: " + fix.category);
                System.out.println();
                System.out.println("  Current:");
                System.out.println("    " + fix.original);
                System.out.println();
                System.out.println("  Recommended:");
                for (String line : fix.replacement.split("\n")) {
                    System.out.println("    " + line);
                }
                System.out.println();
                System.out.println("  " + fix.explanation);
                System.out.println("  Expected: " + fix.expectedImpact);
                System.out.println();
                System.out.println("  " + "─".repeat(60));
                System.out.println();
            }
        } else {
            System.out.println("✓ No performance issues found - build looks well optimized!");
        }

        if (plan.benchmarkCommands != null && !plan.benchmarkCommands.isEmpty()) {
            System.out.println("\n═══════════════════════════════════════════════════════════════");
            System.out.println("BENCHMARK COMMANDS");
            System.out.println("═══════════════════════════════════════════════════════════════\n");
            for (String cmd : plan.benchmarkCommands) {
                System.out.println(cmd);
            }
        }
    }

    // ========== Data Classes ==========

    static class OptimizationPlan {
        String project;
        List<String> analyzedFiles;
        List<Fix> fixes;
        Summary summary;
        List<String> benchmarkCommands;
    }

    static class Fix {
        String id;
        String file;
        int line;
        int column;
        String issue;
        String category;
        String impact;
        boolean autoFixable;
        String original;
        String replacement;
        String explanation;
        String expectedImpact;

        Fix(String id, String file, int line, int column, String issue, String category,
                String impact, boolean autoFixable, String original, String replacement,
                String explanation, String expectedImpact) {
            this.id = id;
            this.file = file;
            this.line = line;
            this.column = column;
            this.issue = issue;
            this.category = category;
            this.impact = impact;
            this.autoFixable = autoFixable;
            this.original = original;
            this.replacement = replacement;
            this.explanation = explanation;
            this.expectedImpact = expectedImpact;
        }
    }

    static class Summary {
        int total;
        int highImpact;
        int mediumImpact;
        int lowImpact;
        int autoFixable;
        String estimatedImprovement;
        Map<String, Long> byCategory;
    }
}
