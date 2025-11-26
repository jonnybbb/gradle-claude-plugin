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
 * Configuration Cache Fixer - Phase 1 of Active Automation
 *
 * Analyzes build files for configuration cache issues and generates
 * structured JSON with exact fixes, confidence levels, and auto-fixability.
 *
 * Usage: config-cache-fixer.java <project-dir> [--json] [--verbose]
 *
 * Output: Structured fix plan with:
 *   - Exact line/column locations
 *   - Original code and replacement
 *   - Confidence level (HIGH/MEDIUM/LOW)
 *   - Auto-fixable flag
 *   - Explanations for each fix
 */
class config_cache_fixer {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static boolean jsonOutput = false;
    private static boolean verbose = false;

    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");

        if (args.length < 1) {
            System.err.println("Usage: config-cache-fixer.java <project-dir> [--json] [--verbose]");
            System.err.println("  project-dir: Path to Gradle project root");
            System.err.println("  --json: Output structured JSON fix plan");
            System.err.println("  --verbose: Include additional context");
            System.exit(1);
        }

        File projectDir = new File(args[0]);

        for (int i = 1; i < args.length; i++) {
            if ("--json".equals(args[i])) jsonOutput = true;
            if ("--verbose".equals(args[i])) verbose = true;
        }

        if (!projectDir.exists() || !projectDir.isDirectory()) {
            System.err.println("Error: Project directory does not exist: " + projectDir);
            System.exit(1);
        }

        try {
            FixPlan plan = analyzeProject(projectDir);

            if (jsonOutput) {
                System.out.println(gson.toJson(plan));
            } else {
                printHumanReadable(plan);
            }

        } catch (Exception e) {
            System.err.println("Error analyzing project: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    private static FixPlan analyzeProject(File projectDir) throws IOException {
        FixPlan plan = new FixPlan();
        plan.project = projectDir.getAbsolutePath();
        plan.analyzedFiles = new ArrayList<>();
        plan.fixes = new ArrayList<>();

        // Find all build files
        List<Path> buildFiles = Files.walk(projectDir.toPath())
            .filter(p -> {
                String name = p.getFileName().toString();
                String pathStr = p.toString();
                boolean isBuildFile = name.equals("build.gradle") || name.equals("build.gradle.kts");
                boolean isInGradleDir = pathStr.contains("/.gradle/") || pathStr.contains(File.separator + ".gradle" + File.separator);
                boolean isInBuildDir = pathStr.contains("/build/") || pathStr.contains(File.separator + "build" + File.separator);
                return isBuildFile && !isInGradleDir && !isInBuildDir;
            })
            .collect(Collectors.toList());

        int fixId = 1;
        for (Path buildFile : buildFiles) {
            String relativePath = projectDir.toPath().relativize(buildFile).toString();
            plan.analyzedFiles.add(relativePath);
            fixId = analyzeFile(buildFile, relativePath, plan.fixes, fixId);
        }

        // Also check buildSrc
        Path buildSrcDir = projectDir.toPath().resolve("buildSrc/src/main");
        if (Files.exists(buildSrcDir)) {
            List<Path> buildSrcFiles = Files.walk(buildSrcDir)
                .filter(p -> {
                    String name = p.toString();
                    return name.endsWith(".kt") || name.endsWith(".java") ||
                           name.endsWith(".gradle.kts");
                })
                .collect(Collectors.toList());

            for (Path srcFile : buildSrcFiles) {
                String relativePath = projectDir.toPath().relativize(srcFile).toString();
                plan.analyzedFiles.add(relativePath);
                fixId = analyzeFile(srcFile, relativePath, plan.fixes, fixId);
            }
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

        // Group by category
        plan.summary.byCategory = plan.fixes.stream()
            .collect(Collectors.groupingBy(f -> f.category, Collectors.counting()));

        return plan;
    }

    private static int analyzeFile(Path file, String relativePath, List<Fix> fixes, int fixId)
            throws IOException {
        String content = Files.readString(file);
        String[] lines = content.split("\n", -1);
        boolean isKotlin = file.toString().endsWith(".kts") || file.toString().endsWith(".kt");

        // Pattern 1: System.getProperty at configuration time (outside doLast/doFirst)
        fixId = findSystemGetProperty(content, lines, relativePath, fixes, fixId, isKotlin);

        // Pattern 2: System.getenv at configuration time
        fixId = findSystemGetenv(content, lines, relativePath, fixes, fixId, isKotlin);

        // Pattern 3: tasks.create (eager task creation)
        fixId = findEagerTaskCreate(content, lines, relativePath, fixes, fixId, isKotlin);

        // Pattern 4: tasks.getByName (eager task access)
        fixId = findEagerGetByName(content, lines, relativePath, fixes, fixId, isKotlin);

        // Pattern 5: $buildDir usage (deprecated)
        fixId = findBuildDirUsage(content, lines, relativePath, fixes, fixId, isKotlin);

        // Pattern 6: project.copy/exec/delete in doLast (needs service injection)
        fixId = findProjectServiceInDoLast(content, lines, relativePath, fixes, fixId, isKotlin);

        // Pattern 7: project access in doLast/doFirst
        fixId = findProjectAccessInDoLast(content, lines, relativePath, fixes, fixId, isKotlin);

        return fixId;
    }

    // ========== Pattern Detection Methods ==========

    private static int findSystemGetProperty(String content, String[] lines, String file,
            List<Fix> fixes, int fixId, boolean isKotlin) {
        // Match System.getProperty("key") or System.getProperty("key", "default")
        Pattern pattern = Pattern.compile(
            "System\\.getProperty\\s*\\(\\s*\"([^\"]+)\"(?:\\s*,\\s*\"([^\"]*)\")?\\s*\\)");

        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            String lineContent = lines[lineNum - 1];

            // Skip if inside doLast/doFirst (handled separately)
            if (isInExecutionBlock(content, matcher.start())) {
                continue;
            }

            String key = matcher.group(1);
            String defaultVal = matcher.group(2);
            String original = matcher.group(0);

            String replacement;
            if (defaultVal != null) {
                replacement = isKotlin
                    ? "providers.systemProperty(\"" + key + "\").orElse(\"" + defaultVal + "\")"
                    : "getProviders().systemProperty(\"" + key + "\").orElse(\"" + defaultVal + "\")";
            } else {
                replacement = isKotlin
                    ? "providers.systemProperty(\"" + key + "\").orNull"
                    : "getProviders().systemProperty(\"" + key + "\").getOrNull()";
            }

            fixes.add(new Fix(
                "FIX-" + String.format("%03d", fixId++),
                file,
                lineNum,
                findColumn(lineContent, original),
                "System.getProperty at configuration time",
                "PROVIDER_API",
                "HIGH",
                true,
                original,
                replacement,
                "Configuration cache cannot serialize System.getProperty. Use Provider API to defer evaluation."
            ));
        }
        return fixId;
    }

    private static int findSystemGetenv(String content, String[] lines, String file,
            List<Fix> fixes, int fixId, boolean isKotlin) {
        Pattern pattern = Pattern.compile("System\\.getenv\\s*\\(\\s*\"([^\"]+)\"\\s*\\)");

        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            String lineContent = lines[lineNum - 1];

            if (isInExecutionBlock(content, matcher.start())) {
                continue;
            }

            String key = matcher.group(1);
            String original = matcher.group(0);

            String replacement = isKotlin
                ? "providers.environmentVariable(\"" + key + "\").orNull"
                : "getProviders().environmentVariable(\"" + key + "\").getOrNull()";

            fixes.add(new Fix(
                "FIX-" + String.format("%03d", fixId++),
                file,
                lineNum,
                findColumn(lineContent, original),
                "System.getenv at configuration time",
                "PROVIDER_API",
                "HIGH",
                true,
                original,
                replacement,
                "Configuration cache cannot serialize System.getenv. Use Provider API to defer evaluation."
            ));
        }
        return fixId;
    }

    private static int findEagerTaskCreate(String content, String[] lines, String file,
            List<Fix> fixes, int fixId, boolean isKotlin) {
        // Match tasks.create("name") or tasks.create<Type>("name")
        Pattern pattern = Pattern.compile(
            "tasks\\.create\\s*(<[^>]+>)?\\s*\\(\\s*\"([^\"]+)\"");

        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            String lineContent = lines[lineNum - 1];

            String type = matcher.group(1);
            String taskName = matcher.group(2);
            String original = matcher.group(0);

            String replacement;
            if (type != null) {
                replacement = "tasks.register" + type + "(\"" + taskName + "\"";
            } else {
                replacement = "tasks.register(\"" + taskName + "\"";
            }

            fixes.add(new Fix(
                "FIX-" + String.format("%03d", fixId++),
                file,
                lineNum,
                findColumn(lineContent, "tasks.create"),
                "Eager task creation with tasks.create()",
                "TASK_AVOIDANCE",
                "HIGH",
                true,
                original,
                replacement,
                "tasks.create() eagerly creates and configures tasks. Use tasks.register() for lazy registration."
            ));
        }
        return fixId;
    }

    private static int findEagerGetByName(String content, String[] lines, String file,
            List<Fix> fixes, int fixId, boolean isKotlin) {
        Pattern pattern = Pattern.compile(
            "tasks\\.getByName\\s*(<[^>]+>)?\\s*\\(\\s*\"([^\"]+)\"");

        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            String lineContent = lines[lineNum - 1];

            String type = matcher.group(1);
            String taskName = matcher.group(2);
            String original = matcher.group(0);

            String replacement;
            if (type != null) {
                replacement = "tasks.named" + type + "(\"" + taskName + "\"";
            } else {
                replacement = "tasks.named(\"" + taskName + "\"";
            }

            fixes.add(new Fix(
                "FIX-" + String.format("%03d", fixId++),
                file,
                lineNum,
                findColumn(lineContent, "tasks.getByName"),
                "Eager task access with tasks.getByName()",
                "TASK_AVOIDANCE",
                "HIGH",
                true,
                original,
                replacement,
                "tasks.getByName() eagerly resolves the task. Use tasks.named() for lazy access."
            ));
        }
        return fixId;
    }

    private static int findBuildDirUsage(String content, String[] lines, String file,
            List<Fix> fixes, int fixId, boolean isKotlin) {
        // Match $buildDir or "${buildDir}" in Kotlin/Groovy
        Pattern dollarPattern = Pattern.compile("\\$\\{?buildDir\\}?");

        Matcher matcher = dollarPattern.matcher(content);
        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            String lineContent = lines[lineNum - 1];
            String original = matcher.group(0);

            // Need context-aware replacement
            String replacement = "${layout.buildDirectory.get().asFile}";

            fixes.add(new Fix(
                "FIX-" + String.format("%03d", fixId++),
                file,
                lineNum,
                findColumn(lineContent, original),
                "Deprecated $buildDir usage",
                "DEPRECATED_API",
                "HIGH",
                true,
                original,
                replacement,
                "project.buildDir is deprecated. Use layout.buildDirectory for configuration cache compatibility."
            ));
        }

        // Also match project.buildDir
        Pattern projectBuildDir = Pattern.compile("project\\.buildDir");
        matcher = projectBuildDir.matcher(content);
        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            String lineContent = lines[lineNum - 1];

            fixes.add(new Fix(
                "FIX-" + String.format("%03d", fixId++),
                file,
                lineNum,
                findColumn(lineContent, "project.buildDir"),
                "Deprecated project.buildDir usage",
                "DEPRECATED_API",
                "HIGH",
                true,
                "project.buildDir",
                "layout.buildDirectory.get().asFile",
                "project.buildDir is deprecated. Use layout.buildDirectory for configuration cache compatibility."
            ));
        }
        return fixId;
    }

    private static int findProjectServiceInDoLast(String content, String[] lines, String file,
            List<Fix> fixes, int fixId, boolean isKotlin) {
        // Find doLast/doFirst blocks with proper brace balancing
        List<BlockMatch> blocks = findDoBlocks(content);

        for (BlockMatch block : blocks) {
            String blockContent = block.content();
            int blockStart = block.startPosition();

            // Check for project.copy
            if (blockContent.contains("project.copy")) {
                int lineNum = getLineNumber(content, blockStart);
                fixes.add(new Fix(
                    "FIX-" + String.format("%03d", fixId++),
                    file,
                    lineNum,
                    1,
                    "project.copy in " + block.blockType() + " block",
                    "SERVICE_INJECTION",
                    "MEDIUM",
                    false,
                    "project.copy { ... }",
                    "// Inject FileSystemOperations:\n" +
                    "// @Inject abstract val fs: FileSystemOperations\n" +
                    "// Then use: fs.copy { ... }",
                    "project.copy cannot be serialized. Inject FileSystemOperations service instead."
                ));
            }

            // Check for project.exec
            if (blockContent.contains("project.exec")) {
                int lineNum = getLineNumber(content, blockStart);
                fixes.add(new Fix(
                    "FIX-" + String.format("%03d", fixId++),
                    file,
                    lineNum,
                    1,
                    "project.exec in " + block.blockType() + " block",
                    "SERVICE_INJECTION",
                    "MEDIUM",
                    false,
                    "project.exec { ... }",
                    "// Inject ExecOperations:\n" +
                    "// @Inject abstract val execOps: ExecOperations\n" +
                    "// Then use: execOps.exec { ... }",
                    "project.exec cannot be serialized. Inject ExecOperations service instead."
                ));
            }

            // Check for project.javaexec
            if (blockContent.contains("project.javaexec")) {
                int lineNum = getLineNumber(content, blockStart);
                fixes.add(new Fix(
                    "FIX-" + String.format("%03d", fixId++),
                    file,
                    lineNum,
                    1,
                    "project.javaexec in " + block.blockType() + " block",
                    "SERVICE_INJECTION",
                    "MEDIUM",
                    false,
                    "project.javaexec { ... }",
                    "// Inject ExecOperations:\n" +
                    "// @Inject abstract val execOps: ExecOperations\n" +
                    "// Then use: execOps.javaexec { ... }",
                    "project.javaexec cannot be serialized. Inject ExecOperations service instead."
                ));
            }

            // Check for project.delete
            if (blockContent.contains("project.delete")) {
                int lineNum = getLineNumber(content, blockStart);
                fixes.add(new Fix(
                    "FIX-" + String.format("%03d", fixId++),
                    file,
                    lineNum,
                    1,
                    "project.delete in " + block.blockType() + " block",
                    "SERVICE_INJECTION",
                    "MEDIUM",
                    false,
                    "project.delete(...)",
                    "// Inject FileSystemOperations:\n" +
                    "// @Inject abstract val fs: FileSystemOperations\n" +
                    "// Then use: fs.delete { delete(...) }",
                    "project.delete cannot be serialized. Inject FileSystemOperations service instead."
                ));
            }

            // Check for project.file
            if (blockContent.contains("project.file")) {
                int lineNum = getLineNumber(content, blockStart);
                fixes.add(new Fix(
                    "FIX-" + String.format("%03d", fixId++),
                    file,
                    lineNum,
                    1,
                    "project.file in " + block.blockType() + " block",
                    "PROJECT_ACCESS",
                    "MEDIUM",
                    false,
                    "project.file(...)",
                    "// Capture at configuration time:\n" +
                    "// val configFile = layout.projectDirectory.file(\"config.properties\")\n" +
                    "// Then use configFile.asFile in doLast",
                    "project.file cannot be called at execution time. Capture the file reference during configuration."
                ));
            }
        }

        return fixId;
    }

    private static int findProjectAccessInDoLast(String content, String[] lines, String file,
            List<Fix> fixes, int fixId, boolean isKotlin) {
        // Find doLast/doFirst blocks with proper brace balancing
        List<BlockMatch> blocks = findDoBlocks(content);

        for (BlockMatch block : blocks) {
            String blockContent = block.content();
            int blockStart = block.startPosition();

            // Check for general project access (not already caught)
            // Look for "project." followed by property access
            Pattern projectAccess = Pattern.compile("project\\.(\\w+)");
            Matcher accessMatcher = projectAccess.matcher(blockContent);

            Set<String> alreadyHandled = Set.of("copy", "exec", "javaexec", "delete", "file");

            while (accessMatcher.find()) {
                String property = accessMatcher.group(1);
                if (!alreadyHandled.contains(property)) {
                    int lineNum = getLineNumber(content, blockStart);
                    fixes.add(new Fix(
                        "FIX-" + String.format("%03d", fixId++),
                        file,
                        lineNum,
                        1,
                        "project." + property + " access in " + blockMatcher.group(1) + " block",
                        "PROJECT_ACCESS",
                        "MEDIUM",
                        false,
                        "project." + property,
                        "// Capture at configuration time:\n" +
                        "// val " + property + "Value = project." + property + "\n" +
                        "// Then use " + property + "Value in doLast",
                        "Task.project cannot be accessed at execution time. Capture values during configuration."
                    ));
                    break; // Only report once per block
                }
            }

            // Check for System.getProperty in execution block
            if (blockContent.contains("System.getProperty")) {
                int lineNum = getLineNumber(content, blockStart);
                fixes.add(new Fix(
                    "FIX-" + String.format("%03d", fixId++),
                    file,
                    lineNum,
                    1,
                    "System.getProperty in " + blockMatcher.group(1) + " block",
                    "EXECUTION_TIME_INPUT",
                    "LOW",
                    false,
                    "System.getProperty(...)",
                    "// For execution-time system properties, use:\n" +
                    "// @Input val prop = providers.systemProperty(\"key\")\n" +
                    "// Then use prop.get() in doLast",
                    "While allowed in doLast, using Provider API enables better caching and tracking."
                ));
            }

            // Check for System.getenv in execution block
            if (blockContent.contains("System.getenv")) {
                int lineNum = getLineNumber(content, blockStart);
                fixes.add(new Fix(
                    "FIX-" + String.format("%03d", fixId++),
                    file,
                    lineNum,
                    1,
                    "System.getenv in " + blockMatcher.group(1) + " block",
                    "EXECUTION_TIME_INPUT",
                    "LOW",
                    false,
                    "System.getenv(...)",
                    "// For execution-time environment variables, use:\n" +
                    "// @Input val env = providers.environmentVariable(\"KEY\")\n" +
                    "// Then use env.get() in doLast",
                    "While allowed in doLast, using Provider API enables better caching and tracking."
                ));
            }
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

    /**
     * Finds all doLast/doFirst blocks with proper brace balancing.
     * Returns a list of matches with group(1)=blockType, group(2)=content, and start position.
     */
    private static List<BlockMatch> findDoBlocks(String content) {
        List<BlockMatch> results = new ArrayList<>();
        Pattern startPattern = Pattern.compile("(doLast|doFirst)\\s*\\{");
        Matcher startMatcher = startPattern.matcher(content);

        while (startMatcher.find()) {
            String blockType = startMatcher.group(1);
            int braceStart = startMatcher.end() - 1; // Position of opening brace
            int depth = 1;
            int i = braceStart + 1;

            while (i < content.length() && depth > 0) {
                char c = content.charAt(i);
                if (c == '{') depth++;
                else if (c == '}') depth--;
                i++;
            }

            if (depth == 0) {
                String blockContent = content.substring(braceStart + 1, i - 1);
                results.add(new BlockMatch(blockType, blockContent, startMatcher.start()));
            }
        }
        return results;
    }

    private record BlockMatch(String blockType, String content, int startPosition) {}

    private static int findColumn(String line, String match) {
        int idx = line.indexOf(match);
        return idx >= 0 ? idx + 1 : 1;
    }

    private static boolean isInExecutionBlock(String content, int position) {
        // Look backwards for doLast/doFirst
        String before = content.substring(0, position);
        int lastDoLast = Math.max(before.lastIndexOf("doLast"), before.lastIndexOf("doFirst"));
        if (lastDoLast < 0) return false;

        // Count braces to see if we're inside
        String between = content.substring(lastDoLast, position);
        int openBraces = 0;
        for (char c : between.toCharArray()) {
            if (c == '{') openBraces++;
            if (c == '}') openBraces--;
        }
        return openBraces > 0;
    }

    // ========== Output Methods ==========

    private static void printHumanReadable(FixPlan plan) {
        System.out.println("=== Configuration Cache Fix Plan ===\n");

        System.out.println("Project: " + plan.project);
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

            for (Fix fix : plan.fixes) {
                String icon = switch (fix.confidence) {
                    case "HIGH" -> "●";
                    case "MEDIUM" -> "◐";
                    case "LOW" -> "○";
                    default -> "?";
                };
                String autoIcon = fix.autoFixable ? "✓" : "✗";

                System.out.println(fix.id + " [" + icon + " " + fix.confidence + "] [" + autoIcon + " auto]");
                System.out.println("  File: " + fix.file + ":" + fix.line);
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
                System.out.println("  Explanation: " + fix.explanation);
                System.out.println();
                System.out.println("  " + "─".repeat(60));
                System.out.println();
            }
        } else {
            System.out.println("✓ No configuration cache issues found!");
        }
    }

    // ========== Data Classes ==========

    static class FixPlan {
        String project;
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
        String confidence;
        boolean autoFixable;
        String original;
        String replacement;
        String explanation;

        Fix(String id, String file, int line, int column, String issue, String category,
                String confidence, boolean autoFixable, String original, String replacement,
                String explanation) {
            this.id = id;
            this.file = file;
            this.line = line;
            this.column = column;
            this.issue = issue;
            this.category = category;
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
