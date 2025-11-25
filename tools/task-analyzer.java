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
 * Gradle Task Analyzer
 * 
 * Analyzes build scripts for task-related issues:
 * - Eager vs lazy task registration
 * - Configuration cache compatibility
 * - Input/output declarations
 * - Task avoidance patterns
 * - Common anti-patterns
 * 
 * Usage: task-analyzer.java <project-dir> [--json] [--fix]
 */
public class task_analyzer {
    
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static boolean jsonOutput = false;
    private static boolean autoFix = false;
    
    // Pattern definitions
    private static final Pattern EAGER_CREATE = Pattern.compile(
        "tasks\\.create\\s*[<(]", Pattern.MULTILINE);
    private static final Pattern EAGER_GET_BY_NAME = Pattern.compile(
        "tasks\\.getByName\\s*[<(]", Pattern.MULTILINE);
    private static final Pattern PROJECT_IN_DOLAST = Pattern.compile(
        "(doLast|doFirst)\\s*\\{[^}]*\\bproject\\.", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern SYSTEM_GETPROPERTY = Pattern.compile(
        "System\\.getProperty\\s*\\(", Pattern.MULTILINE);
    private static final Pattern SYSTEM_GETENV = Pattern.compile(
        "System\\.getenv\\s*\\(", Pattern.MULTILINE);
    private static final Pattern PROJECT_COPY = Pattern.compile(
        "(doLast|doFirst)\\s*\\{[^}]*project\\.copy\\s*\\{", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern PROJECT_EXEC = Pattern.compile(
        "(doLast|doFirst)\\s*\\{[^}]*project\\.exec\\s*\\{", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern CONFIG_RESOLUTION = Pattern.compile(
        "configurations\\.[a-zA-Z]+\\.files(?!\\s*\\{)", Pattern.MULTILINE);
    private static final Pattern TASK_REGISTER = Pattern.compile(
        "tasks\\.register\\s*[<(]", Pattern.MULTILINE);
    private static final Pattern TASK_NAMED = Pattern.compile(
        "tasks\\.named\\s*[<(]", Pattern.MULTILINE);
    
    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        
        if (args.length < 1) {
            System.err.println("Usage: task-analyzer.java <project-dir> [--json] [--fix]");
            System.err.println("  project-dir: Path to Gradle project root");
            System.err.println("  --json: Output results as JSON");
            System.err.println("  --fix: Suggest fixes inline");
            System.exit(1);
        }
        
        File projectDir = new File(args[0]);
        
        for (int i = 1; i < args.length; i++) {
            if ("--json".equals(args[i])) jsonOutput = true;
            if ("--fix".equals(args[i])) autoFix = true;
        }
        
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            System.err.println("Error: Project directory does not exist: " + projectDir);
            System.exit(1);
        }
        
        try {
            AnalysisResult result = analyzeProject(projectDir);
            
            if (jsonOutput) {
                System.out.println(gson.toJson(result));
            } else {
                printHumanReadable(result);
            }
            
        } catch (Exception e) {
            System.err.println("Error analyzing project: " + e.getMessage());
            if (!jsonOutput) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }
    
    private static AnalysisResult analyzeProject(File projectDir) throws IOException {
        AnalysisResult result = new AnalysisResult();
        result.projectDir = projectDir.getAbsolutePath();
        result.issues = new ArrayList<>();
        result.fileResults = new ArrayList<>();
        
        // Find all build files
        List<Path> buildFiles = Files.walk(projectDir.toPath())
            .filter(p -> {
                String name = p.getFileName().toString();
                return (name.equals("build.gradle") || name.equals("build.gradle.kts")) &&
                       !p.toString().contains(".gradle") &&
                       !p.toString().contains("build/");
            })
            .collect(Collectors.toList());
        
        for (Path buildFile : buildFiles) {
            FileResult fileResult = analyzeFile(buildFile);
            result.fileResults.add(fileResult);
            result.issues.addAll(fileResult.issues);
        }
        
        // Also check buildSrc
        Path buildSrcDir = projectDir.toPath().resolve("buildSrc/src/main");
        if (Files.exists(buildSrcDir)) {
            List<Path> buildSrcFiles = Files.walk(buildSrcDir)
                .filter(p -> p.toString().endsWith(".kt") || p.toString().endsWith(".java") || 
                            p.toString().endsWith(".gradle.kts"))
                .collect(Collectors.toList());
            
            for (Path srcFile : buildSrcFiles) {
                FileResult fileResult = analyzeFile(srcFile);
                result.fileResults.add(fileResult);
                result.issues.addAll(fileResult.issues);
            }
        }
        
        // Summary
        result.totalFiles = result.fileResults.size();
        result.totalIssues = result.issues.size();
        result.criticalIssues = (int) result.issues.stream()
            .filter(i -> "CRITICAL".equals(i.severity)).count();
        result.warningIssues = (int) result.issues.stream()
            .filter(i -> "WARNING".equals(i.severity)).count();
        result.infoIssues = (int) result.issues.stream()
            .filter(i -> "INFO".equals(i.severity)).count();
        
        // Count patterns
        result.eagerTaskCreations = (int) result.issues.stream()
            .filter(i -> i.type.contains("EAGER")).count();
        result.configCacheIssues = (int) result.issues.stream()
            .filter(i -> i.type.contains("CONFIG_CACHE")).count();
        result.lazyTaskRegistrations = result.fileResults.stream()
            .mapToInt(f -> f.lazyRegistrations).sum();
        
        // Health score (0-100)
        int totalPatterns = result.eagerTaskCreations + result.lazyTaskRegistrations;
        if (totalPatterns > 0) {
            result.lazyRegistrationScore = (result.lazyTaskRegistrations * 100) / totalPatterns;
        } else {
            result.lazyRegistrationScore = 100;
        }
        
        result.overallScore = calculateOverallScore(result);
        result.recommendations = generateRecommendations(result);
        
        return result;
    }
    
    private static FileResult analyzeFile(Path file) throws IOException {
        FileResult result = new FileResult();
        result.path = file.toString();
        result.issues = new ArrayList<>();
        
        String content = Files.readString(file);
        String[] lines = content.split("\n");
        
        // Check for eager task creation
        findPatternIssues(result, content, lines, EAGER_CREATE, 
            "EAGER_CREATE", "CRITICAL",
            "Eager task creation with tasks.create()",
            "Replace with tasks.register() for lazy configuration");
        
        findPatternIssues(result, content, lines, EAGER_GET_BY_NAME,
            "EAGER_GET_BY_NAME", "WARNING",
            "Eager task access with tasks.getByName()",
            "Replace with tasks.named() for lazy access");
        
        // Check for configuration cache issues
        findPatternIssues(result, content, lines, PROJECT_IN_DOLAST,
            "CONFIG_CACHE_PROJECT", "CRITICAL",
            "Project access in doLast/doFirst block",
            "Capture project values during configuration phase");
        
        findPatternIssues(result, content, lines, SYSTEM_GETPROPERTY,
            "CONFIG_CACHE_SYSPROP", "WARNING",
            "System.getProperty() may cause config cache issues",
            "Use providers.systemProperty() instead");
        
        findPatternIssues(result, content, lines, SYSTEM_GETENV,
            "CONFIG_CACHE_ENV", "WARNING",
            "System.getenv() may cause config cache issues",
            "Use providers.environmentVariable() instead");
        
        findPatternIssues(result, content, lines, PROJECT_COPY,
            "CONFIG_CACHE_COPY", "CRITICAL",
            "project.copy in doLast/doFirst block",
            "Inject FileSystemOperations service instead");
        
        findPatternIssues(result, content, lines, PROJECT_EXEC,
            "CONFIG_CACHE_EXEC", "CRITICAL",
            "project.exec in doLast/doFirst block",
            "Inject ExecOperations service instead");
        
        findPatternIssues(result, content, lines, CONFIG_RESOLUTION,
            "CONFIG_TIME_RESOLUTION", "WARNING",
            "Configuration resolved during configuration phase",
            "Use configurations.*.map { } for lazy resolution");
        
        // Count good patterns
        Matcher lazyMatcher = TASK_REGISTER.matcher(content);
        while (lazyMatcher.find()) {
            result.lazyRegistrations++;
        }
        
        Matcher namedMatcher = TASK_NAMED.matcher(content);
        while (namedMatcher.find()) {
            result.lazyAccess++;
        }
        
        return result;
    }
    
    private static void findPatternIssues(FileResult result, String content, String[] lines,
            Pattern pattern, String type, String severity, String message, String fix) {
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            Issue issue = new Issue();
            issue.type = type;
            issue.severity = severity;
            issue.message = message;
            issue.fix = fix;
            issue.file = result.path;
            
            // Find line number
            int pos = matcher.start();
            int lineNum = 1;
            for (int i = 0; i < pos && i < content.length(); i++) {
                if (content.charAt(i) == '\n') lineNum++;
            }
            issue.line = lineNum;
            
            // Get context
            if (lineNum > 0 && lineNum <= lines.length) {
                issue.context = lines[lineNum - 1].trim();
            }
            
            result.issues.add(issue);
        }
    }
    
    private static int calculateOverallScore(AnalysisResult result) {
        int score = 100;
        
        // Deduct for critical issues
        score -= result.criticalIssues * 15;
        
        // Deduct for warnings
        score -= result.warningIssues * 5;
        
        // Deduct for info
        score -= result.infoIssues * 1;
        
        // Bonus for lazy patterns
        if (result.lazyRegistrationScore > 80) {
            score += 5;
        }
        
        return Math.max(0, Math.min(100, score));
    }
    
    private static List<String> generateRecommendations(AnalysisResult result) {
        List<String> recommendations = new ArrayList<>();
        
        if (result.eagerTaskCreations > 0) {
            recommendations.add("Convert " + result.eagerTaskCreations + 
                " eager task creation(s) to lazy registration with tasks.register()");
        }
        
        if (result.configCacheIssues > 0) {
            recommendations.add("Fix " + result.configCacheIssues + 
                " configuration cache compatibility issue(s) to enable faster builds");
        }
        
        long sysPropertyIssues = result.issues.stream()
            .filter(i -> "CONFIG_CACHE_SYSPROP".equals(i.type)).count();
        if (sysPropertyIssues > 0) {
            recommendations.add("Replace " + sysPropertyIssues + 
                " System.getProperty() call(s) with providers.systemProperty()");
        }
        
        if (result.overallScore >= 80) {
            recommendations.add("✓ Task implementation quality is good!");
        } else if (result.overallScore >= 50) {
            recommendations.add("Task implementation needs improvement for optimal performance");
        } else {
            recommendations.add("Significant task implementation issues found - prioritize fixes");
        }
        
        return recommendations;
    }
    
    private static void printHumanReadable(AnalysisResult result) {
        System.out.println("=== Gradle Task Analysis ===\n");
        
        System.out.println("Project: " + result.projectDir);
        System.out.println("Files analyzed: " + result.totalFiles);
        System.out.println();
        
        System.out.println("Score: " + result.overallScore + "/100");
        System.out.println("  Lazy registration: " + result.lazyRegistrationScore + "%");
        System.out.println();
        
        System.out.println("Issues Found: " + result.totalIssues);
        System.out.println("  Critical: " + result.criticalIssues);
        System.out.println("  Warning:  " + result.warningIssues);
        System.out.println("  Info:     " + result.infoIssues);
        System.out.println();
        
        System.out.println("Patterns:");
        System.out.println("  Eager task creations:  " + result.eagerTaskCreations);
        System.out.println("  Lazy registrations:    " + result.lazyTaskRegistrations);
        System.out.println("  Config cache issues:   " + result.configCacheIssues);
        System.out.println();
        
        if (!result.issues.isEmpty()) {
            System.out.println("Issues by File:");
            
            Map<String, List<Issue>> byFile = result.issues.stream()
                .collect(Collectors.groupingBy(i -> i.file));
            
            for (Map.Entry<String, List<Issue>> entry : byFile.entrySet()) {
                System.out.println("\n  " + entry.getKey() + ":");
                for (Issue issue : entry.getValue()) {
                    String icon = "CRITICAL".equals(issue.severity) ? "✗" : 
                                 "WARNING".equals(issue.severity) ? "!" : "i";
                    System.out.println("    [" + icon + "] Line " + issue.line + ": " + issue.message);
                    if (autoFix) {
                        System.out.println("        Fix: " + issue.fix);
                    }
                    if (issue.context != null && !issue.context.isEmpty()) {
                        String ctx = issue.context.length() > 60 
                            ? issue.context.substring(0, 60) + "..." 
                            : issue.context;
                        System.out.println("        > " + ctx);
                    }
                }
            }
            System.out.println();
        }
        
        System.out.println("Recommendations:");
        for (String rec : result.recommendations) {
            System.out.println("  • " + rec);
        }
    }
    
    // Data classes
    static class AnalysisResult {
        String projectDir;
        int totalFiles;
        int totalIssues;
        int criticalIssues;
        int warningIssues;
        int infoIssues;
        int eagerTaskCreations;
        int lazyTaskRegistrations;
        int configCacheIssues;
        int lazyRegistrationScore;
        int overallScore;
        List<Issue> issues;
        List<FileResult> fileResults;
        List<String> recommendations;
    }
    
    static class FileResult {
        String path;
        List<Issue> issues;
        int lazyRegistrations;
        int lazyAccess;
    }
    
    static class Issue {
        String type;
        String severity;
        String message;
        String fix;
        String file;
        int line;
        String context;
    }
}
