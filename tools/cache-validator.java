///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//REPOS mavencentral,gradle=https://repo.gradle.org/gradle/libs-releases
//DEPS org.gradle:gradle-tooling-api:9.2.1
//DEPS org.slf4j:slf4j-simple:2.0.9
//DEPS com.google.code.gson:gson:2.10.1

import org.gradle.tooling.*;
import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Gradle Cache Validator
 * 
 * Validates build cache and configuration cache setup and identifies common issues:
 * - Configuration cache compatibility problems
 * - Build cache configuration
 * - Task cacheability issues
 * - Common anti-patterns
 * 
 * Usage: cache-validator.java <project-dir> [--fix]
 */
class cache_validator {
    
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static boolean autoFix = false;
    
    // Common configuration cache problem patterns
    private static final Pattern PROJECT_AT_EXECUTION = 
        Pattern.compile("project\\.[a-zA-Z]+");
    private static final Pattern SYSTEM_PROPERTY_DIRECT = 
        Pattern.compile("System\\.getProperty\\(");
    
    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        
        if (args.length < 1) {
            System.err.println("Usage: cache-validator.java <project-dir> [--fix]");
            System.err.println("  project-dir: Path to Gradle project root");
            System.err.println("  --fix: Attempt to automatically fix issues");
            System.exit(1);
        }
        
        File projectDir = new File(args[0]);
        autoFix = args.length > 1 && "--fix".equals(args[1]);
        
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            System.err.println("Error: Project directory does not exist: " + projectDir);
            System.exit(1);
        }
        
        try {
            ValidationResult result = validateProject(projectDir);
            printReport(result);
            
            if (autoFix && !result.issues.isEmpty()) {
                System.out.println("\n=== Applying Fixes ===");
                applyFixes(projectDir, result);
            }
            
            System.exit(result.issues.isEmpty() ? 0 : 1);
            
        } catch (Exception e) {
            System.err.println("Error validating project: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static ValidationResult validateProject(File projectDir) throws IOException {
        ValidationResult result = new ValidationResult();
        
        // Check gradle.properties
        File gradleProps = new File(projectDir, "gradle.properties");
        if (gradleProps.exists()) {
            Properties props = new Properties();
            props.load(new FileInputStream(gradleProps));
            
            result.buildCacheEnabled = "true".equals(props.getProperty("org.gradle.caching"));
            result.configCacheEnabled = "true".equals(props.getProperty("org.gradle.configuration-cache"));
            result.parallelEnabled = "true".equals(props.getProperty("org.gradle.parallel"));
        } else {
            result.issues.add(new Issue(
                IssueSeverity.WARNING,
                "No gradle.properties file found",
                "Create gradle.properties with performance optimizations",
                "create_gradle_properties"
            ));
        }
        
        // Validate cache configuration
        if (!result.buildCacheEnabled) {
            result.issues.add(new Issue(
                IssueSeverity.WARNING,
                "Build cache not enabled",
                "Add 'org.gradle.caching=true' to gradle.properties",
                "enable_build_cache"
            ));
        }
        
        if (!result.configCacheEnabled) {
            result.issues.add(new Issue(
                IssueSeverity.INFO,
                "Configuration cache not enabled",
                "Add 'org.gradle.configuration-cache=true' to gradle.properties",
                "enable_config_cache"
            ));
        }
        
        // Scan build files for common issues
        scanBuildFiles(projectDir, result);
        
        // Check for build cache directory
        File buildCacheDir = new File(System.getProperty("user.home"), ".gradle/caches/build-cache-1");
        result.buildCacheExists = buildCacheDir.exists();
        
        return result;
    }
    
    private static void scanBuildFiles(File dir, ValidationResult result) throws IOException {
        File[] buildFiles = dir.listFiles((d, name) -> 
            name.equals("build.gradle") || name.equals("build.gradle.kts")
        );
        
        if (buildFiles != null) {
            for (File buildFile : buildFiles) {
                scanFile(buildFile, result);
            }
        }
        
        // Recursively scan subprojects
        File[] subdirs = dir.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File subdir : subdirs) {
                if (!subdir.getName().equals("build") && 
                    !subdir.getName().equals(".gradle") &&
                    !subdir.getName().startsWith(".")) {
                    scanBuildFiles(subdir, result);
                }
            }
        }
    }
    
    private static void scanFile(File file, ValidationResult result) throws IOException {
        String content = Files.readString(file.toPath());
        List<String> lines = Files.readAllLines(file.toPath());
        
        // Check for project access in doLast/doFirst
        Pattern doLastPattern = Pattern.compile("doLast\\s*\\{([^}]+)\\}", Pattern.DOTALL);
        Matcher doLastMatcher = doLastPattern.matcher(content);
        
        while (doLastMatcher.find()) {
            String taskBody = doLastMatcher.group(1);
            if (PROJECT_AT_EXECUTION.matcher(taskBody).find()) {
                result.issues.add(new Issue(
                    IssueSeverity.ERROR,
                    "Task.project access during execution in " + file.getName(),
                    "Capture project properties during configuration phase",
                    "fix_project_access"
                ));
            }
            
            if (SYSTEM_PROPERTY_DIRECT.matcher(taskBody).find()) {
                result.issues.add(new Issue(
                    IssueSeverity.ERROR,
                    "Direct System.getProperty() call in task execution in " + file.getName(),
                    "Use providers.systemProperty() instead",
                    "fix_system_property"
                ));
            }
        }
        
        // Check for eager task creation
        Pattern eagerTaskPattern = Pattern.compile("tasks\\.create\\(");
        if (eagerTaskPattern.matcher(content).find()) {
            result.issues.add(new Issue(
                IssueSeverity.WARNING,
                "Eager task creation found in " + file.getName(),
                "Use tasks.register() for lazy task creation",
                "fix_eager_tasks"
            ));
        }
        
        // Check for configuration-time resolution
        Pattern configResolutionPattern = Pattern.compile("configurations\\.[a-zA-Z]+\\.files");
        if (configResolutionPattern.matcher(content).find()) {
            result.issues.add(new Issue(
                IssueSeverity.ERROR,
                "Configuration resolved at configuration time in " + file.getName(),
                "Move resolution to task execution phase",
                "fix_config_resolution"
            ));
        }
        
        result.filesScanned++;
    }
    
    private static void applyFixes(File projectDir, ValidationResult result) {
        for (Issue issue : result.issues) {
            if (issue.fixId != null) {
                try {
                    switch (issue.fixId) {
                        case "create_gradle_properties":
                        case "enable_build_cache":
                        case "enable_config_cache":
                            fixGradleProperties(projectDir);
                            System.out.println("✓ Updated gradle.properties");
                            break;
                        default:
                            System.out.println("⚠ Manual fix required: " + issue.description);
                    }
                } catch (IOException e) {
                    System.err.println("✗ Failed to fix: " + issue.description);
                }
            }
        }
    }
    
    private static void fixGradleProperties(File projectDir) throws IOException {
        File propsFile = new File(projectDir, "gradle.properties");
        Properties props = new Properties();
        
        if (propsFile.exists()) {
            props.load(new FileInputStream(propsFile));
        }
        
        // Add performance optimizations if not present
        if (!props.containsKey("org.gradle.caching")) {
            props.setProperty("org.gradle.caching", "true");
        }
        if (!props.containsKey("org.gradle.configuration-cache")) {
            props.setProperty("org.gradle.configuration-cache", "true");
        }
        if (!props.containsKey("org.gradle.parallel")) {
            props.setProperty("org.gradle.parallel", "true");
        }
        if (!props.containsKey("org.gradle.daemon")) {
            props.setProperty("org.gradle.daemon", "true");
        }
        
        // Write back
        try (FileOutputStream out = new FileOutputStream(propsFile)) {
            props.store(out, "Gradle performance optimizations");
        }
    }
    
    private static void printReport(ValidationResult result) {
        System.out.println("=== Cache Validation Report ===\n");
        
        System.out.println("Configuration:");
        System.out.println("  Build Cache: " + (result.buildCacheEnabled ? "✓ Enabled" : "✗ Disabled"));
        System.out.println("  Configuration Cache: " + (result.configCacheEnabled ? "✓ Enabled" : "✗ Disabled"));
        System.out.println("  Parallel Execution: " + (result.parallelEnabled ? "✓ Enabled" : "✗ Disabled"));
        System.out.println("  Build Cache Dir: " + (result.buildCacheExists ? "✓ Exists" : "⚠ Not yet created"));
        System.out.println("\nFiles Scanned: " + result.filesScanned);
        System.out.println();
        
        if (result.issues.isEmpty()) {
            System.out.println("✓ No issues found!");
        } else {
            System.out.println("Issues Found: " + result.issues.size());
            System.out.println();
            
            // Group by severity
            Map<IssueSeverity, List<Issue>> bySeverity = new HashMap<>();
            for (Issue issue : result.issues) {
                bySeverity.computeIfAbsent(issue.severity, k -> new ArrayList<>()).add(issue);
            }
            
            for (IssueSeverity severity : IssueSeverity.values()) {
                List<Issue> issues = bySeverity.get(severity);
                if (issues != null && !issues.isEmpty()) {
                    System.out.println(severity + " (" + issues.size() + "):");
                    for (Issue issue : issues) {
                        System.out.println("  " + issue.description);
                        System.out.println("    → " + issue.recommendation);
                        System.out.println();
                    }
                }
            }
            
            if (autoFix) {
                long fixableCount = result.issues.stream()
                    .filter(i -> i.fixId != null)
                    .count();
                System.out.println("Fixable: " + fixableCount + " of " + result.issues.size());
            }
        }
    }
    
    enum IssueSeverity {
        ERROR, WARNING, INFO
    }
    
    static class Issue {
        IssueSeverity severity;
        String description;
        String recommendation;
        String fixId;
        
        Issue(IssueSeverity severity, String description, String recommendation, String fixId) {
            this.severity = severity;
            this.description = description;
            this.recommendation = recommendation;
            this.fixId = fixId;
        }
    }
    
    static class ValidationResult {
        boolean buildCacheEnabled;
        boolean configCacheEnabled;
        boolean parallelEnabled;
        boolean buildCacheExists;
        int filesScanned;
        List<Issue> issues = new ArrayList<>();
    }
}
