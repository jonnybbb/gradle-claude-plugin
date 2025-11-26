///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//REPOS mavencentral,gradle=https://repo.gradle.org/gradle/libs-releases
//DEPS org.gradle:gradle-tooling-api:9.2.1
//DEPS org.slf4j:slf4j-simple:2.0.9
//DEPS com.google.code.gson:gson:2.10.1

import org.gradle.tooling.*;
import org.gradle.tooling.model.*;
import org.gradle.tooling.model.build.*;
import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * Gradle Build Health Check
 * 
 * Quick health assessment providing an overall score and key metrics:
 * - Performance configuration
 * - Caching setup
 * - Build structure
 * - Task implementation quality
 * - Dependency management
 * 
 * Usage: build-health-check.java <project-dir> [--json] [--verbose]
 */
class build_health_check {
    
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static boolean jsonOutput = false;
    private static boolean verbose = false;
    
    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        
        if (args.length < 1) {
            System.err.println("Usage: build-health-check.java <project-dir> [--json] [--verbose]");
            System.err.println("  project-dir: Path to Gradle project root");
            System.err.println("  --json: Output results as JSON");
            System.err.println("  --verbose: Show detailed breakdown");
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
            HealthReport report = checkHealth(projectDir);
            
            if (jsonOutput) {
                System.out.println(gson.toJson(report));
            } else {
                printHumanReadable(report);
            }
            
        } catch (Exception e) {
            System.err.println("Error checking health: " + e.getMessage());
            if (!jsonOutput) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }
    
    private static HealthReport checkHealth(File projectDir) throws Exception {
        HealthReport report = new HealthReport();
        report.projectDir = projectDir.getAbsolutePath();
        report.timestamp = System.currentTimeMillis();
        report.categories = new ArrayList<>();
        
        // Get basic project info via Tooling API
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(projectDir)
                .connect()) {
            
            BuildEnvironment env = connection.getModel(BuildEnvironment.class);
            GradleProject project = connection.getModel(GradleProject.class);
            
            report.gradleVersion = env.getGradle().getGradleVersion();
            report.projectName = project.getName();
            report.subprojectCount = countSubprojects(project);
        }
        
        // Check each category
        report.categories.add(checkPerformanceConfig(projectDir));
        report.categories.add(checkCachingSetup(projectDir));
        report.categories.add(checkBuildStructure(projectDir));
        report.categories.add(checkTaskQuality(projectDir));
        report.categories.add(checkDependencyManagement(projectDir));
        
        // Calculate overall score
        int totalScore = 0;
        int totalWeight = 0;
        for (CategoryScore cat : report.categories) {
            totalScore += cat.score * cat.weight;
            totalWeight += cat.weight;
        }
        report.overallScore = totalWeight > 0 ? totalScore / totalWeight : 0;
        
        // Determine status
        if (report.overallScore >= 80) {
            report.status = "HEALTHY";
            report.statusEmoji = "✅";
        } else if (report.overallScore >= 50) {
            report.status = "NEEDS_ATTENTION";
            report.statusEmoji = "⚠️";
        } else {
            report.status = "CRITICAL";
            report.statusEmoji = "❌";
        }
        
        // Generate top recommendations
        report.topRecommendations = generateTopRecommendations(report);
        
        return report;
    }
    
    private static int countSubprojects(GradleProject project) {
        int count = 0;
        for (GradleProject child : project.getChildren()) {
            count++;
            count += countSubprojects(child);
        }
        return count;
    }
    
    private static CategoryScore checkPerformanceConfig(File projectDir) throws IOException {
        CategoryScore score = new CategoryScore();
        score.name = "Performance Configuration";
        score.weight = 25;
        score.checks = new ArrayList<>();
        
        int points = 0;
        int maxPoints = 0;
        
        // Check gradle.properties
        File gradleProps = new File(projectDir, "gradle.properties");
        Properties props = new Properties();
        if (gradleProps.exists()) {
            props.load(new FileInputStream(gradleProps));
        }
        
        // Parallel execution
        maxPoints += 20;
        boolean parallel = "true".equals(props.getProperty("org.gradle.parallel"));
        if (parallel) {
            points += 20;
            score.checks.add(new Check("Parallel execution", true, null));
        } else {
            score.checks.add(new Check("Parallel execution", false, 
                "Add org.gradle.parallel=true to gradle.properties"));
        }
        
        // Daemon
        maxPoints += 10;
        boolean daemon = !"false".equals(props.getProperty("org.gradle.daemon"));
        if (daemon) {
            points += 10;
            score.checks.add(new Check("Daemon enabled", true, null));
        } else {
            score.checks.add(new Check("Daemon enabled", false,
                "Remove org.gradle.daemon=false"));
        }
        
        // File system watching
        maxPoints += 15;
        boolean vfsWatch = "true".equals(props.getProperty("org.gradle.vfs.watch"));
        if (vfsWatch) {
            points += 15;
            score.checks.add(new Check("File system watching", true, null));
        } else {
            score.checks.add(new Check("File system watching", false,
                "Add org.gradle.vfs.watch=true"));
        }
        
        // JVM args configured
        maxPoints += 15;
        String jvmArgs = props.getProperty("org.gradle.jvmargs");
        if (jvmArgs != null && jvmArgs.contains("-Xmx")) {
            points += 15;
            score.checks.add(new Check("JVM memory configured", true, null));
        } else {
            score.checks.add(new Check("JVM memory configured", false,
                "Add org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g"));
        }
        
        // Workers configured
        maxPoints += 10;
        String workers = props.getProperty("org.gradle.workers.max");
        if (workers != null) {
            points += 10;
            score.checks.add(new Check("Workers configured", true, null));
        } else {
            score.checks.add(new Check("Workers configured", false,
                "Add org.gradle.workers.max=<cpu_count>"));
        }
        
        score.score = maxPoints > 0 ? (points * 100) / maxPoints : 0;
        return score;
    }
    
    private static CategoryScore checkCachingSetup(File projectDir) throws IOException {
        CategoryScore score = new CategoryScore();
        score.name = "Caching Setup";
        score.weight = 25;
        score.checks = new ArrayList<>();
        
        int points = 0;
        int maxPoints = 0;
        
        File gradleProps = new File(projectDir, "gradle.properties");
        Properties props = new Properties();
        if (gradleProps.exists()) {
            props.load(new FileInputStream(gradleProps));
        }
        
        // Build cache
        maxPoints += 30;
        boolean buildCache = "true".equals(props.getProperty("org.gradle.caching"));
        if (buildCache) {
            points += 30;
            score.checks.add(new Check("Build cache enabled", true, null));
        } else {
            score.checks.add(new Check("Build cache enabled", false,
                "Add org.gradle.caching=true"));
        }
        
        // Configuration cache
        maxPoints += 40;
        boolean configCache = "true".equals(props.getProperty("org.gradle.configuration-cache"));
        if (configCache) {
            points += 40;
            score.checks.add(new Check("Configuration cache enabled", true, null));
        } else {
            score.checks.add(new Check("Configuration cache enabled", false,
                "Add org.gradle.configuration-cache=true"));
        }
        
        // Check for remote cache in settings
        maxPoints += 30;
        File settingsKts = new File(projectDir, "settings.gradle.kts");
        File settingsGroovy = new File(projectDir, "settings.gradle");
        boolean hasRemoteCache = false;
        
        if (settingsKts.exists()) {
            String content = Files.readString(settingsKts.toPath());
            hasRemoteCache = content.contains("remote<HttpBuildCache>") || 
                            content.contains("buildCache");
        } else if (settingsGroovy.exists()) {
            String content = Files.readString(settingsGroovy.toPath());
            hasRemoteCache = content.contains("HttpBuildCache") || 
                            content.contains("buildCache");
        }
        
        if (hasRemoteCache) {
            points += 30;
            score.checks.add(new Check("Remote cache configured", true, null));
        } else {
            score.checks.add(new Check("Remote cache configured", false,
                "Consider setting up remote build cache for team sharing"));
        }
        
        score.score = maxPoints > 0 ? (points * 100) / maxPoints : 0;
        return score;
    }
    
    private static CategoryScore checkBuildStructure(File projectDir) throws IOException {
        CategoryScore score = new CategoryScore();
        score.name = "Build Structure";
        score.weight = 20;
        score.checks = new ArrayList<>();
        
        int points = 0;
        int maxPoints = 0;
        
        // Gradle wrapper
        maxPoints += 25;
        boolean hasWrapper = new File(projectDir, "gradlew").exists();
        if (hasWrapper) {
            points += 25;
            score.checks.add(new Check("Gradle wrapper present", true, null));
        } else {
            score.checks.add(new Check("Gradle wrapper present", false,
                "Run: gradle wrapper"));
        }
        
        // Version catalog
        maxPoints += 25;
        boolean hasVersionCatalog = new File(projectDir, "gradle/libs.versions.toml").exists();
        if (hasVersionCatalog) {
            points += 25;
            score.checks.add(new Check("Version catalog", true, null));
        } else {
            score.checks.add(new Check("Version catalog", false,
                "Create gradle/libs.versions.toml for centralized versions"));
        }
        
        // buildSrc or convention plugins
        maxPoints += 25;
        boolean hasBuildSrc = new File(projectDir, "buildSrc").exists();
        boolean hasBuildLogic = new File(projectDir, "build-logic").exists();
        if (hasBuildSrc || hasBuildLogic) {
            points += 25;
            score.checks.add(new Check("Convention plugins (buildSrc)", true, null));
        } else {
            score.checks.add(new Check("Convention plugins (buildSrc)", false,
                "Create buildSrc for shared build logic"));
        }
        
        // Kotlin DSL
        maxPoints += 15;
        boolean useKotlinDsl = new File(projectDir, "build.gradle.kts").exists();
        if (useKotlinDsl) {
            points += 15;
            score.checks.add(new Check("Kotlin DSL", true, null));
        } else {
            score.checks.add(new Check("Kotlin DSL", false,
                "Consider migrating to Kotlin DSL for type safety"));
        }
        
        // gradle.properties exists
        maxPoints += 10;
        boolean hasGradleProps = new File(projectDir, "gradle.properties").exists();
        if (hasGradleProps) {
            points += 10;
            score.checks.add(new Check("gradle.properties exists", true, null));
        } else {
            score.checks.add(new Check("gradle.properties exists", false,
                "Create gradle.properties for build configuration"));
        }
        
        score.score = maxPoints > 0 ? (points * 100) / maxPoints : 0;
        return score;
    }
    
    private static CategoryScore checkTaskQuality(File projectDir) throws IOException {
        CategoryScore score = new CategoryScore();
        score.name = "Task Quality";
        score.weight = 15;
        score.checks = new ArrayList<>();
        
        int eagerCreates = 0;
        int lazyRegisters = 0;
        int projectInDoLast = 0;
        
        // Analyze build files
        List<Path> buildFiles = Files.walk(projectDir.toPath())
            .filter(p -> {
                String name = p.getFileName().toString();
                return (name.equals("build.gradle") || name.equals("build.gradle.kts")) &&
                       !p.toString().contains(".gradle") &&
                       !p.toString().contains("build/");
            })
            .collect(Collectors.toList());
        
        Pattern eagerPattern = Pattern.compile("tasks\\.create\\s*[<(]");
        Pattern lazyPattern = Pattern.compile("tasks\\.register\\s*[<(]");
        Pattern projectDoLast = Pattern.compile("(doLast|doFirst)\\s*\\{[^}]*\\bproject\\.");
        
        for (Path file : buildFiles) {
            String content = Files.readString(file);
            
            Matcher eager = eagerPattern.matcher(content);
            while (eager.find()) eagerCreates++;
            
            Matcher lazy = lazyPattern.matcher(content);
            while (lazy.find()) lazyRegisters++;
            
            Matcher projDl = projectDoLast.matcher(content);
            while (projDl.find()) projectInDoLast++;
        }
        
        int points = 0;
        int maxPoints = 0;
        
        // Lazy registration ratio
        maxPoints += 50;
        int totalTasks = eagerCreates + lazyRegisters;
        if (totalTasks == 0) {
            points += 50;
            score.checks.add(new Check("Lazy task registration", true, null));
        } else {
            int lazyPercent = (lazyRegisters * 100) / totalTasks;
            if (lazyPercent >= 80) {
                points += 50;
                score.checks.add(new Check("Lazy task registration (" + lazyPercent + "%)", true, null));
            } else if (lazyPercent >= 50) {
                points += 25;
                score.checks.add(new Check("Lazy task registration (" + lazyPercent + "%)", false,
                    "Convert " + eagerCreates + " tasks.create() to tasks.register()"));
            } else {
                score.checks.add(new Check("Lazy task registration (" + lazyPercent + "%)", false,
                    "Convert " + eagerCreates + " tasks.create() to tasks.register()"));
            }
        }
        
        // Config cache compatibility
        maxPoints += 50;
        if (projectInDoLast == 0) {
            points += 50;
            score.checks.add(new Check("Config cache compatible", true, null));
        } else {
            score.checks.add(new Check("Config cache compatible", false,
                "Fix " + projectInDoLast + " project access in doLast/doFirst"));
        }
        
        score.score = maxPoints > 0 ? (points * 100) / maxPoints : 0;
        return score;
    }
    
    private static CategoryScore checkDependencyManagement(File projectDir) throws IOException {
        CategoryScore score = new CategoryScore();
        score.name = "Dependency Management";
        score.weight = 15;
        score.checks = new ArrayList<>();
        
        int points = 0;
        int maxPoints = 0;
        
        // Version catalog check (shared with structure but weighted differently here)
        maxPoints += 40;
        boolean hasVersionCatalog = new File(projectDir, "gradle/libs.versions.toml").exists();
        if (hasVersionCatalog) {
            points += 40;
            score.checks.add(new Check("Centralized versions", true, null));
        } else {
            score.checks.add(new Check("Centralized versions", false,
                "Use version catalog or platform for version management"));
        }
        
        // Check for dependency locking
        maxPoints += 30;
        boolean hasLockFiles = Files.exists(projectDir.toPath().resolve("gradle.lockfile")) ||
                              Files.exists(projectDir.toPath().resolve("gradle/dependency-locks"));
        if (hasLockFiles) {
            points += 30;
            score.checks.add(new Check("Dependency locking", true, null));
        } else {
            score.checks.add(new Check("Dependency locking", false,
                "Consider enabling dependency locking for reproducibility"));
        }
        
        // Check for platform/BOM usage in build files
        maxPoints += 30;
        boolean usesPlatform = false;
        List<Path> buildFiles = Files.walk(projectDir.toPath())
            .filter(p -> p.getFileName().toString().matches("build\\.gradle(\\.kts)?"))
            .limit(5)
            .collect(Collectors.toList());
        
        for (Path file : buildFiles) {
            String content = Files.readString(file);
            if (content.contains("platform(") || content.contains("enforcedPlatform(")) {
                usesPlatform = true;
                break;
            }
        }
        
        if (usesPlatform) {
            points += 30;
            score.checks.add(new Check("Platform/BOM usage", true, null));
        } else {
            score.checks.add(new Check("Platform/BOM usage", false,
                "Consider using platforms for version alignment"));
        }
        
        score.score = maxPoints > 0 ? (points * 100) / maxPoints : 0;
        return score;
    }
    
    private static List<String> generateTopRecommendations(HealthReport report) {
        List<String> recommendations = new ArrayList<>();
        
        // Collect all failed checks with recommendations
        List<Check> failedChecks = report.categories.stream()
            .flatMap(cat -> cat.checks.stream())
            .filter(c -> !c.passed && c.recommendation != null)
            .collect(Collectors.toList());
        
        // Prioritize by category weight
        Map<String, Integer> categoryWeights = report.categories.stream()
            .collect(Collectors.toMap(c -> c.name, c -> c.weight));
        
        // Sort and take top 5
        failedChecks.stream()
            .limit(5)
            .forEach(check -> recommendations.add(check.recommendation));
        
        if (recommendations.isEmpty()) {
            recommendations.add("✓ Build health looks good! No critical issues found.");
        }
        
        return recommendations;
    }
    
    private static void printHumanReadable(HealthReport report) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║           GRADLE BUILD HEALTH CHECK                      ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
        
        System.out.println("Project: " + report.projectName);
        System.out.println("Gradle:  " + report.gradleVersion);
        System.out.println("Modules: " + (report.subprojectCount + 1));
        System.out.println();
        
        // Overall score with ASCII art
        System.out.println("┌────────────────────────────────────────┐");
        System.out.printf("│  OVERALL SCORE: %s %3d/100              │%n", 
            report.statusEmoji, report.overallScore);
        System.out.println("│  " + getScoreBar(report.overallScore) + "  │");
        System.out.println("│  Status: " + String.format("%-29s", report.status) + "│");
        System.out.println("└────────────────────────────────────────┘");
        System.out.println();
        
        // Category scores
        System.out.println("Category Breakdown:");
        for (CategoryScore cat : report.categories) {
            String emoji = cat.score >= 80 ? "✅" : cat.score >= 50 ? "⚠️" : "❌";
            System.out.printf("  %s %-28s %3d/100%n", emoji, cat.name, cat.score);
            
            if (verbose) {
                for (Check check : cat.checks) {
                    String checkEmoji = check.passed ? "  ✓" : "  ✗";
                    System.out.println("      " + checkEmoji + " " + check.name);
                }
            }
        }
        System.out.println();
        
        // Top recommendations
        System.out.println("Top Recommendations:");
        for (int i = 0; i < report.topRecommendations.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + report.topRecommendations.get(i));
        }
        System.out.println();
        
        // Quick command
        System.out.println("Run with --verbose for detailed breakdown");
        System.out.println("Run with --json for machine-readable output");
    }
    
    private static String getScoreBar(int score) {
        int filled = score / 5;
        int empty = 20 - filled;
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < filled; i++) bar.append("█");
        for (int i = 0; i < empty; i++) bar.append("░");
        bar.append("]");
        return bar.toString();
    }
    
    // Data classes
    static class HealthReport {
        String projectDir;
        String projectName;
        String gradleVersion;
        int subprojectCount;
        long timestamp;
        int overallScore;
        String status;
        String statusEmoji;
        List<CategoryScore> categories;
        List<String> topRecommendations;
    }
    
    static class CategoryScore {
        String name;
        int score;
        int weight;
        List<Check> checks;
    }
    
    static class Check {
        String name;
        boolean passed;
        String recommendation;
        
        Check(String name, boolean passed, String recommendation) {
            this.name = name;
            this.passed = passed;
            this.recommendation = recommendation;
        }
    }
}
