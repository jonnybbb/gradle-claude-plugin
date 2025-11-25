///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//REPOS mavencentral,gradle=https://repo.gradle.org/gradle/libs-releases
//DEPS org.gradle:gradle-tooling-api:9.2.1
//DEPS org.slf4j:slf4j-simple:2.0.9
//DEPS com.google.code.gson:gson:2.10.1

import org.gradle.tooling.*;
import org.gradle.tooling.model.*;
import org.gradle.tooling.model.build.*;
import org.gradle.tooling.events.*;
import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Gradle Project Analyzer using Tooling API
 * 
 * Analyzes a Gradle project and outputs structured information about:
 * - Build environment (Gradle version, Java version)
 * - Project structure (root, subprojects)
 * - Tasks (names, descriptions, groups)
 * - Basic build health indicators
 * 
 * Usage: gradle-analyzer.java <project-dir> [--json]
 */
class gradle_analyzer {
    
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static boolean jsonOutput = false;
    
    public static void main(String[] args) {
        // Configure SLF4J to reduce noise
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        
        if (args.length < 1) {
            System.err.println("Usage: gradle-analyzer.java <project-dir> [--json]");
            System.err.println("  project-dir: Path to Gradle project root");
            System.err.println("  --json: Output results as JSON");
            System.exit(1);
        }
        
        File projectDir = new File(args[0]);
        jsonOutput = args.length > 1 && "--json".equals(args[1]);
        
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            System.err.println("Error: Project directory does not exist: " + projectDir);
            System.exit(1);
        }
        
        // Check for Gradle wrapper or build file
        if (!hasGradleBuild(projectDir)) {
            System.err.println("Error: Not a Gradle project (no build.gradle[.kts] or settings.gradle[.kts] found)");
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
    
    private static boolean hasGradleBuild(File dir) {
        return new File(dir, "build.gradle").exists() ||
               new File(dir, "build.gradle.kts").exists() ||
               new File(dir, "settings.gradle").exists() ||
               new File(dir, "settings.gradle.kts").exists();
    }
    
    private static AnalysisResult analyzeProject(File projectDir) {
        AnalysisResult result = new AnalysisResult();
        
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(projectDir)
                .connect()) {
            
            // Fetch models
            GradleProject project = connection.getModel(GradleProject.class);
            BuildEnvironment env = connection.getModel(BuildEnvironment.class);
            
            // Extract build environment
            result.gradleVersion = env.getGradle().getGradleVersion();
            result.javaHome = env.getJava().getJavaHome().getAbsolutePath();
            
            // Extract project structure
            result.projectName = project.getName();
            result.projectPath = project.getPath();
            result.buildDirectory = project.getBuildDirectory().getAbsolutePath();
            
            // Count subprojects
            result.subprojects = countSubprojects(project);
            
            // Extract tasks
            result.tasks = new ArrayList<>();
            for (GradleTask task : project.getTasks()) {
                TaskInfo taskInfo = new TaskInfo();
                taskInfo.name = task.getName();
                taskInfo.path = task.getPath();
                taskInfo.description = task.getDescription();
                taskInfo.group = task.getGroup() != null ? task.getGroup() : "other";
                result.tasks.add(taskInfo);
            }
            
            // Health indicators
            result.healthIndicators = analyzeHealth(projectDir, project);
            
        }
        
        return result;
    }
    
    private static int countSubprojects(GradleProject project) {
        int count = 0;
        for (GradleProject child : project.getChildren()) {
            count++;
            count += countSubprojects(child);
        }
        return count;
    }
    
    private static HealthIndicators analyzeHealth(File projectDir, GradleProject project) {
        HealthIndicators health = new HealthIndicators();
        
        // Check for gradle.properties
        File gradleProperties = new File(projectDir, "gradle.properties");
        health.hasGradleProperties = gradleProperties.exists();
        
        if (health.hasGradleProperties) {
            try {
                Properties props = new Properties();
                props.load(new FileInputStream(gradleProperties));
                
                health.parallelEnabled = "true".equals(props.getProperty("org.gradle.parallel"));
                health.cachingEnabled = "true".equals(props.getProperty("org.gradle.caching"));
                health.configCacheEnabled = "true".equals(props.getProperty("org.gradle.configuration-cache"));
                health.daemonEnabled = !"false".equals(props.getProperty("org.gradle.daemon")); // default is true
                
            } catch (IOException e) {
                // Ignore
            }
        }
        
        // Check for wrapper
        health.hasWrapper = new File(projectDir, "gradlew").exists() || 
                           new File(projectDir, "gradlew.bat").exists();
        
        // Check for buildSrc
        health.hasBuildSrc = new File(projectDir, "buildSrc").exists();
        
        // Detect DSL
        health.useKotlinDsl = new File(projectDir, "build.gradle.kts").exists();
        
        return health;
    }
    
    private static void printHumanReadable(AnalysisResult result) {
        System.out.println("=== Gradle Project Analysis ===\n");
        
        System.out.println("Environment:");
        System.out.println("  Gradle Version: " + result.gradleVersion);
        System.out.println("  Java Home: " + result.javaHome);
        System.out.println();
        
        System.out.println("Project:");
        System.out.println("  Name: " + result.projectName);
        System.out.println("  Path: " + result.projectPath);
        System.out.println("  Build Dir: " + result.buildDirectory);
        System.out.println("  Subprojects: " + result.subprojects);
        System.out.println("  DSL: " + (result.healthIndicators.useKotlinDsl ? "Kotlin" : "Groovy"));
        System.out.println();
        
        System.out.println("Health Indicators:");
        System.out.println("  Gradle Wrapper: " + (result.healthIndicators.hasWrapper ? "✓" : "✗"));
        System.out.println("  gradle.properties: " + (result.healthIndicators.hasGradleProperties ? "✓" : "✗"));
        System.out.println("  Parallel Execution: " + (result.healthIndicators.parallelEnabled ? "✓" : "✗"));
        System.out.println("  Build Cache: " + (result.healthIndicators.cachingEnabled ? "✓" : "✗"));
        System.out.println("  Configuration Cache: " + (result.healthIndicators.configCacheEnabled ? "✓" : "✗"));
        System.out.println("  Daemon: " + (result.healthIndicators.daemonEnabled ? "✓" : "✗"));
        System.out.println("  buildSrc: " + (result.healthIndicators.hasBuildSrc ? "✓" : "✗"));
        System.out.println();
        
        System.out.println("Tasks (" + result.tasks.size() + " total):");
        Map<String, List<TaskInfo>> tasksByGroup = result.tasks.stream()
            .collect(Collectors.groupingBy(t -> t.group));
        
        tasksByGroup.keySet().stream().sorted().forEach(group -> {
            System.out.println("  " + group + ":");
            tasksByGroup.get(group).forEach(task -> {
                String desc = task.description != null && !task.description.isEmpty() 
                    ? " - " + task.description 
                    : "";
                System.out.println("    " + task.name + desc);
            });
        });
    }
    
    // Data classes for structured output
    static class AnalysisResult {
        String gradleVersion;
        String javaHome;
        String projectName;
        String projectPath;
        String buildDirectory;
        int subprojects;
        List<TaskInfo> tasks;
        HealthIndicators healthIndicators;
    }
    
    static class TaskInfo {
        String name;
        String path;
        String description;
        String group;
    }
    
    static class HealthIndicators {
        boolean hasWrapper;
        boolean hasGradleProperties;
        boolean parallelEnabled;
        boolean cachingEnabled;
        boolean configCacheEnabled;
        boolean daemonEnabled;
        boolean hasBuildSrc;
        boolean useKotlinDsl;
    }
}
