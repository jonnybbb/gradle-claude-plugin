///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//DEPS com.google.code.gson:gson:2.10.1

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Quick Gradle Build File Validator - Phase 6 of Active Automation
 *
 * Fast validation of Gradle build files for common issues.
 * Designed for use in hooks where speed is critical.
 *
 * Usage: quick-validate.java <file-path> [--json]
 *
 * Exit codes:
 *   0 - No issues found
 *   1 - Issues found (warnings in output)
 *   2 - Error (invalid input)
 */
class quick_validate {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static boolean jsonOutput = false;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: quick-validate.java <file-path> [--json]");
            System.exit(2);
        }

        Path filePath = Paths.get(args[0]);
        for (int i = 1; i < args.length; i++) {
            if ("--json".equals(args[i])) jsonOutput = true;
        }

        if (!Files.exists(filePath)) {
            System.err.println("Error: File not found: " + filePath);
            System.exit(2);
        }

        try {
            List<Issue> issues = validateFile(filePath);

            if (jsonOutput) {
                System.out.println(gson.toJson(Map.of(
                    "file", filePath.toString(),
                    "issues", issues,
                    "count", issues.size()
                )));
            } else {
                if (issues.isEmpty()) {
                    System.out.println("No issues found in " + filePath.getFileName());
                } else {
                    System.out.println("Issues found in " + filePath.getFileName() + ":");
                    for (Issue issue : issues) {
                        System.out.println("  [" + issue.severity + "] Line " + issue.line + ": " + issue.message);
                    }
                }
            }

            System.exit(issues.isEmpty() ? 0 : 1);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(2);
        }
    }

    private static List<Issue> validateFile(Path file) throws IOException {
        String content = Files.readString(file);
        String[] lines = content.split("\n", -1);
        String fileName = file.getFileName().toString();
        List<Issue> issues = new ArrayList<>();

        if (fileName.endsWith(".gradle") || fileName.endsWith(".gradle.kts")) {
            // Configuration cache issues
            checkPattern(content, lines, issues,
                Pattern.compile("System\\.getProperty\\s*\\("),
                "WARN", "System.getProperty at configuration time (config cache incompatible)");

            checkPattern(content, lines, issues,
                Pattern.compile("System\\.getenv\\s*\\("),
                "WARN", "System.getenv at configuration time (config cache incompatible)");

            checkPattern(content, lines, issues,
                Pattern.compile("tasks\\.create\\s*\\("),
                "WARN", "Eager task creation with tasks.create (use tasks.register)");

            checkPattern(content, lines, issues,
                Pattern.compile("\\$buildDir|project\\.buildDir"),
                "WARN", "Direct buildDir usage (use layout.buildDirectory)");

            checkPattern(content, lines, issues,
                Pattern.compile("tasks\\.getByName\\s*\\("),
                "INFO", "Eager task lookup with getByName (prefer tasks.named)");

            checkPattern(content, lines, issues,
                Pattern.compile("tasks\\.all\\s*\\{"),
                "WARN", "tasks.all {} eagerly configures all tasks (use configureEach)");

            // Groovy-specific eager task syntax
            if (fileName.endsWith(".gradle")) {
                checkPattern(content, lines, issues,
                    Pattern.compile("^\\s*task\\s+\\w+\\s*[({]", Pattern.MULTILINE),
                    "WARN", "Eager task definition (use tasks.register)");
            }

            // Performance issues
            checkPattern(content, lines, issues,
                Pattern.compile("afterEvaluate\\s*\\{"),
                "INFO", "afterEvaluate can cause ordering issues (prefer lazy APIs)");

            checkPattern(content, lines, issues,
                Pattern.compile("configurations\\.[a-zA-Z]+\\.resolve\\(\\)"),
                "WARN", "Configuration resolution at configuration time");

        } else if (fileName.equals("gradle.properties")) {
            // Check for missing performance settings
            if (!content.contains("org.gradle.parallel")) {
                issues.add(new Issue(1, "INFO", "Consider adding org.gradle.parallel=true"));
            }
            if (!content.contains("org.gradle.caching")) {
                issues.add(new Issue(1, "INFO", "Consider adding org.gradle.caching=true"));
            }
        }

        return issues;
    }

    private static void checkPattern(String content, String[] lines, List<Issue> issues,
            Pattern pattern, String severity, String message) {
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            int line = getLineNumber(content, matcher.start());
            issues.add(new Issue(line, severity, message));
        }
    }

    private static int getLineNumber(String content, int position) {
        int line = 1;
        for (int i = 0; i < position && i < content.length(); i++) {
            if (content.charAt(i) == '\n') line++;
        }
        return line;
    }

    record Issue(int line, String severity, String message) {}
}
