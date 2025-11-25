///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//REPOS mavencentral,gradle=https://repo.gradle.org/gradle/libs-releases
//DEPS org.gradle:gradle-tooling-api:9.2.1
//DEPS org.slf4j:slf4j-simple:2.0.9
//DEPS com.google.code.gson:gson:2.10.1

import org.gradle.tooling.*;
import org.gradle.tooling.events.*;
import org.gradle.tooling.events.task.*;
import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * Gradle Performance Profiler using Tooling API
 * 
 * Profiles build execution and provides performance metrics:
 * - Total build time breakdown
 * - Configuration vs execution time
 * - Slowest tasks identification
 * - Task outcome analysis (executed, up-to-date, from-cache)
 * - Parallelization efficiency
 * 
 * Usage: performance-profiler.java <project-dir> [task] [--json]
 */
class performance_profiler {
    
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static boolean jsonOutput = false;
    
    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        
        if (args.length < 1) {
            System.err.println("Usage: performance-profiler.java <project-dir> [task] [--json]");
            System.err.println("  project-dir: Path to Gradle project root");
            System.err.println("  task: Task to profile (default: help)");
            System.err.println("  --json: Output results as JSON");
            System.exit(1);
        }
        
        File projectDir = new File(args[0]);
        String task = "help";
        
        for (int i = 1; i < args.length; i++) {
            if ("--json".equals(args[i])) {
                jsonOutput = true;
            } else {
                task = args[i];
            }
        }
        
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            System.err.println("Error: Project directory does not exist: " + projectDir);
            System.exit(1);
        }
        
        try {
            ProfileResult result = profileBuild(projectDir, task);
            
            if (jsonOutput) {
                System.out.println(gson.toJson(result));
            } else {
                printHumanReadable(result);
            }
            
        } catch (Exception e) {
            System.err.println("Error profiling build: " + e.getMessage());
            if (!jsonOutput) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }
    
    private static ProfileResult profileBuild(File projectDir, String task) {
        ProfileResult result = new ProfileResult();
        result.task = task;
        result.projectDir = projectDir.getAbsolutePath();
        
        List<TaskMetrics> taskMetrics = new CopyOnWriteArrayList<>();
        long[] configEndTime = new long[1];
        
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(projectDir)
                .connect()) {
            
            long startTime = System.currentTimeMillis();
            
            // Build with progress listener
            connection.newBuild()
                .forTasks(task)
                .addProgressListener(event -> {
                    if (event instanceof TaskFinishEvent) {
                        TaskFinishEvent taskEvent = (TaskFinishEvent) event;
                        TaskOperationDescriptor descriptor = taskEvent.getDescriptor();
                        TaskOperationResult opResult = taskEvent.getResult();
                        
                        TaskMetrics metrics = new TaskMetrics();
                        metrics.path = descriptor.getTaskPath();
                        metrics.startTime = opResult.getStartTime();
                        metrics.endTime = opResult.getEndTime();
                        metrics.duration = opResult.getEndTime() - opResult.getStartTime();
                        
                        if (opResult instanceof TaskSuccessResult) {
                            TaskSuccessResult success = (TaskSuccessResult) opResult;
                            metrics.outcome = success.isUpToDate() ? "UP-TO-DATE" :
                                            success.isFromCache() ? "FROM-CACHE" : "EXECUTED";
                            metrics.upToDate = success.isUpToDate();
                            metrics.fromCache = success.isFromCache();
                        } else if (opResult instanceof TaskSkippedResult) {
                            metrics.outcome = "SKIPPED";
                            metrics.skipped = true;
                        } else if (opResult instanceof TaskFailureResult) {
                            metrics.outcome = "FAILED";
                            metrics.failed = true;
                        }
                        
                        taskMetrics.add(metrics);
                    }
                }, OperationType.TASK)
                .addProgressListener(event -> {
                    // Capture when configuration ends (first task starts)
                    if (event instanceof TaskStartEvent && configEndTime[0] == 0) {
                        configEndTime[0] = System.currentTimeMillis();
                    }
                }, OperationType.TASK)
                .setStandardOutput(new ByteArrayOutputStream()) // Suppress output
                .setStandardError(new ByteArrayOutputStream())
                .run();
            
            long endTime = System.currentTimeMillis();
            
            // Calculate metrics
            result.totalTimeMs = endTime - startTime;
            result.configurationTimeMs = configEndTime[0] > 0 ? configEndTime[0] - startTime : 0;
            result.executionTimeMs = result.totalTimeMs - result.configurationTimeMs;
            result.configurationPercent = result.totalTimeMs > 0 
                ? (result.configurationTimeMs * 100.0 / result.totalTimeMs) : 0;
            
            result.taskMetrics = taskMetrics;
            result.totalTasks = taskMetrics.size();
            
            // Calculate task outcomes
            result.executedTasks = (int) taskMetrics.stream().filter(t -> "EXECUTED".equals(t.outcome)).count();
            result.upToDateTasks = (int) taskMetrics.stream().filter(t -> t.upToDate).count();
            result.fromCacheTasks = (int) taskMetrics.stream().filter(t -> t.fromCache).count();
            result.skippedTasks = (int) taskMetrics.stream().filter(t -> t.skipped).count();
            
            // Find slowest tasks
            result.slowestTasks = taskMetrics.stream()
                .filter(t -> !t.skipped && !t.upToDate && !t.fromCache)
                .sorted((a, b) -> Long.compare(b.duration, a.duration))
                .limit(10)
                .collect(Collectors.toList());
            
            // Calculate efficiency metrics
            long totalTaskTime = taskMetrics.stream().mapToLong(t -> t.duration).sum();
            result.parallelEfficiency = result.executionTimeMs > 0 
                ? (totalTaskTime * 100.0 / result.executionTimeMs) : 0;
            
            // Cache efficiency
            int cacheableCount = result.executedTasks + result.fromCacheTasks;
            result.cacheHitRate = cacheableCount > 0 
                ? (result.fromCacheTasks * 100.0 / cacheableCount) : 0;
            
            // Avoidance rate (up-to-date + from-cache)
            int avoidableTasks = result.executedTasks + result.upToDateTasks + result.fromCacheTasks;
            result.taskAvoidanceRate = avoidableTasks > 0 
                ? ((result.upToDateTasks + result.fromCacheTasks) * 100.0 / avoidableTasks) : 0;
            
            // Recommendations
            result.recommendations = generateRecommendations(result);
            
        }
        
        return result;
    }
    
    private static List<String> generateRecommendations(ProfileResult result) {
        List<String> recommendations = new ArrayList<>();
        
        // Configuration time check
        if (result.configurationPercent > 20) {
            recommendations.add("HIGH: Configuration takes " + String.format("%.1f%%", result.configurationPercent) + 
                " of build time. Consider enabling configuration cache and using lazy task registration.");
        }
        
        // Cache hit rate
        if (result.cacheHitRate < 50 && result.fromCacheTasks + result.executedTasks > 5) {
            recommendations.add("MEDIUM: Cache hit rate is " + String.format("%.1f%%", result.cacheHitRate) + 
                ". Review task inputs/outputs and path sensitivity.");
        }
        
        // Task avoidance
        if (result.taskAvoidanceRate < 30 && result.totalTasks > 10) {
            recommendations.add("MEDIUM: Task avoidance rate is " + String.format("%.1f%%", result.taskAvoidanceRate) + 
                ". Ensure tasks have proper inputs/outputs declared.");
        }
        
        // Parallelization
        if (result.parallelEfficiency > 150 && result.executionTimeMs > 5000) {
            recommendations.add("INFO: Good parallelization detected (" + String.format("%.0f%%", result.parallelEfficiency) + 
                " efficiency). Ensure org.gradle.parallel=true is set.");
        }
        
        // Slow tasks
        if (!result.slowestTasks.isEmpty() && result.slowestTasks.get(0).duration > 10000) {
            recommendations.add("MEDIUM: Slowest task '" + result.slowestTasks.get(0).path + 
                "' took " + formatDuration(result.slowestTasks.get(0).duration) + ". Consider optimization.");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("OK: Build performance looks healthy!");
        }
        
        return recommendations;
    }
    
    private static String formatDuration(long ms) {
        if (ms < 1000) return ms + "ms";
        if (ms < 60000) return String.format("%.1fs", ms / 1000.0);
        return String.format("%.1fm", ms / 60000.0);
    }
    
    private static void printHumanReadable(ProfileResult result) {
        System.out.println("=== Gradle Performance Profile ===\n");
        
        System.out.println("Build: " + result.task);
        System.out.println("Project: " + result.projectDir);
        System.out.println();
        
        System.out.println("Timing:");
        System.out.println("  Total Time:         " + formatDuration(result.totalTimeMs));
        System.out.println("  Configuration:      " + formatDuration(result.configurationTimeMs) + 
            " (" + String.format("%.1f%%", result.configurationPercent) + ")");
        System.out.println("  Execution:          " + formatDuration(result.executionTimeMs));
        System.out.println();
        
        System.out.println("Tasks (" + result.totalTasks + " total):");
        System.out.println("  Executed:           " + result.executedTasks);
        System.out.println("  Up-to-date:         " + result.upToDateTasks);
        System.out.println("  From cache:         " + result.fromCacheTasks);
        System.out.println("  Skipped:            " + result.skippedTasks);
        System.out.println();
        
        System.out.println("Efficiency:");
        System.out.println("  Cache Hit Rate:     " + String.format("%.1f%%", result.cacheHitRate));
        System.out.println("  Task Avoidance:     " + String.format("%.1f%%", result.taskAvoidanceRate));
        System.out.println("  Parallel Efficiency:" + String.format("%.0f%%", result.parallelEfficiency));
        System.out.println();
        
        if (!result.slowestTasks.isEmpty()) {
            System.out.println("Slowest Tasks:");
            for (TaskMetrics task : result.slowestTasks) {
                System.out.println("  " + task.path + ": " + formatDuration(task.duration));
            }
            System.out.println();
        }
        
        System.out.println("Recommendations:");
        for (String rec : result.recommendations) {
            System.out.println("  • " + rec);
        }
    }
    
    // Data classes
    static class ProfileResult {
        String task;
        String projectDir;
        long totalTimeMs;
        long configurationTimeMs;
        long executionTimeMs;
        double configurationPercent;
        int totalTasks;
        int executedTasks;
        int upToDateTasks;
        int fromCacheTasks;
        int skippedTasks;
        double cacheHitRate;
        double taskAvoidanceRate;
        double parallelEfficiency;
        List<TaskMetrics> taskMetrics;
        List<TaskMetrics> slowestTasks;
        List<String> recommendations;
    }
    
    static class TaskMetrics {
        String path;
        String outcome;
        long startTime;
        long endTime;
        long duration;
        boolean upToDate;
        boolean fromCache;
        boolean skipped;
        boolean failed;
    }
}
