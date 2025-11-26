package com.gradle.claude.plugin.tools;

import com.google.gson.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for JBang tool outputs.
 * These tests run the actual JBang tools against test fixtures
 * and verify they produce expected output.
 *
 * Requires: JBang installed and accessible, valid JAVA_HOME
 */
@Tag("tools")
@DisplayName("JBang Tool Output Tests")
class ToolOutputTest {

    private static final Path PLUGIN_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path TOOLS_DIR = PLUGIN_ROOT.resolve("tools");
    private static final Path FIXTURES_DIR = PLUGIN_ROOT.resolve("tests/fixtures/projects");
    private static final Gson gson = new Gson();

    private static boolean jbangAvailable;

    @BeforeAll
    static void checkJbangAvailability() {
        try {
            ProcessBuilder pb = new ProcessBuilder("jbang", "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);
            jbangAvailable = completed && process.exitValue() == 0;
        } catch (Exception e) {
            jbangAvailable = false;
        }
    }

    boolean isJbangAvailable() {
        return jbangAvailable;
    }

    @Test
    @EnabledIf("isJbangAvailable")
    @DisplayName("gradle-analyzer should analyze simple-java fixture")
    void gradleAnalyzerShouldAnalyzeSimpleJava() throws Exception {
        String output = runTool("gradle-analyzer.java", "simple-java");

        // Should identify project properties
        assertThat(output)
            .as("Should analyze project successfully")
            .containsAnyOf("java", "application", "project", "group", "version");
    }

    @Test
    @EnabledIf("isJbangAvailable")
    @DisplayName("gradle-analyzer should support JSON output")
    void gradleAnalyzerShouldSupportJsonOutput() throws Exception {
        String output = runTool("gradle-analyzer.java", "simple-java", "--json");

        // Filter out JBang's status messages
        String jsonOutput = filterJbangOutput(output);

        // Should be valid JSON (starts with { or [)
        String trimmed = jsonOutput.trim();
        assertThat(trimmed)
            .as("Output should be JSON format")
            .matches("(?s)^[{\\[].*[}\\]]$");
    }

    /**
     * Filter out JBang status messages from output.
     */
    private String filterJbangOutput(String output) {
        return java.util.Arrays.stream(output.split("\n"))
            .filter(line -> !line.startsWith("[jbang]"))
            .collect(java.util.stream.Collectors.joining("\n"));
    }

    @Test
    @EnabledIf("isJbangAvailable")
    @DisplayName("cache-validator should detect config-cache issues")
    void cacheValidatorShouldDetectIssues() throws Exception {
        String output = runTool("cache-validator.java", "config-cache-broken");

        // Should detect at least some issues
        assertThat(output.toLowerCase())
            .as("Should detect configuration cache issues")
            .containsAnyOf("issue", "problem", "warning", "error", "incompatible");
    }

    @Test
    @EnabledIf("isJbangAvailable")
    @DisplayName("cache-validator should pass healthy fixture")
    void cacheValidatorShouldPassHealthyFixture() throws Exception {
        String output = runTool("cache-validator.java", "simple-java");

        // Should not report critical issues for healthy project
        assertThat(output.toLowerCase())
            .as("Should not report critical issues for healthy project")
            .doesNotContain("critical", "fatal");
    }

    @Test
    @EnabledIf("isJbangAvailable")
    @DisplayName("build-health-check should produce health report")
    void buildHealthCheckShouldProduceReport() throws Exception {
        String output = runTool("build-health-check.java", "simple-java");

        // Should produce some kind of health assessment
        assertThat(output.toLowerCase())
            .as("Should produce health assessment")
            .containsAnyOf("health", "status", "check", "analysis", "report");
    }

    @Test
    @EnabledIf("isJbangAvailable")
    @DisplayName("task-analyzer should analyze tasks")
    void taskAnalyzerShouldAnalyzeTasks() throws Exception {
        String output = runTool("task-analyzer.java", "simple-java");

        // Should find standard Java tasks
        assertThat(output.toLowerCase())
            .as("Should identify Java tasks")
            .containsAnyOf("task", "compile", "test", "build", "jar");
    }

    @Test
    @EnabledIf("isJbangAvailable")
    @DisplayName("task-analyzer should find build files (not filter them out)")
    void taskAnalyzerShouldFindBuildFiles() throws Exception {
        String output = runTool("task-analyzer.java", "simple-java", "--json");
        String jsonOutput = filterJbangOutput(output);
        JsonObject json = gson.fromJson(jsonOutput, JsonObject.class);

        int totalFiles = json.get("totalFiles").getAsInt();
        assertThat(totalFiles)
            .as("Should analyze at least one build file (was filtering out build.gradle due to .gradle check)")
            .isGreaterThan(0);
    }

    @Test
    @EnabledIf("isJbangAvailable")
    @DisplayName("task-analyzer should detect lazy patterns in simple-java")
    void taskAnalyzerShouldDetectLazyPatterns() throws Exception {
        String output = runTool("task-analyzer.java", "simple-java", "--json");
        String jsonOutput = filterJbangOutput(output);
        JsonObject json = gson.fromJson(jsonOutput, JsonObject.class);

        int lazyRegistrations = json.get("lazyTaskRegistrations").getAsInt();
        int lazyAccess = json.get("lazyTaskAccess").getAsInt();

        // simple-java has: 1x tasks.register, 3x tasks.named
        assertThat(lazyRegistrations)
            .as("Should detect tasks.register calls")
            .isGreaterThanOrEqualTo(1);
        assertThat(lazyAccess)
            .as("Should detect tasks.named calls")
            .isGreaterThanOrEqualTo(3);
    }

    @Test
    @EnabledIf("isJbangAvailable")
    @DisplayName("task-analyzer should detect issues in config-cache-broken")
    void taskAnalyzerShouldDetectIssuesInBrokenFixture() throws Exception {
        String output = runTool("task-analyzer.java", "config-cache-broken", "--json");
        String jsonOutput = filterJbangOutput(output);
        JsonObject json = gson.fromJson(jsonOutput, JsonObject.class);

        int totalFiles = json.get("totalFiles").getAsInt();
        int eagerCreates = json.get("eagerTaskCreations").getAsInt();
        int configCacheIssues = json.get("configCacheIssues").getAsInt();

        assertThat(totalFiles)
            .as("Should analyze build files")
            .isGreaterThan(0);
        assertThat(eagerCreates)
            .as("Should detect eager task creations (tasks.create)")
            .isGreaterThanOrEqualTo(3);
        assertThat(configCacheIssues)
            .as("Should detect config cache issues")
            .isGreaterThanOrEqualTo(5);
    }

    @Test
    @EnabledIf("isJbangAvailable")
    @DisplayName("build-health-check should find build files")
    void buildHealthCheckShouldFindBuildFiles() throws Exception {
        String output = runTool("build-health-check.java", "simple-java", "--json");
        String jsonOutput = filterJbangOutput(output);
        JsonObject json = gson.fromJson(jsonOutput, JsonObject.class);

        // Verify the tool actually analyzed something
        assertThat(json.has("overallScore")).isTrue();
        int score = json.get("overallScore").getAsInt();
        assertThat(score)
            .as("Healthy project should have good score")
            .isGreaterThanOrEqualTo(70);
    }

    @Test
    @EnabledIf("isJbangAvailable")
    @DisplayName("build-health-check should detect issues in broken fixture")
    void buildHealthCheckShouldDetectIssuesInBrokenFixture() throws Exception {
        String output = runTool("build-health-check.java", "config-cache-broken", "--json");
        String jsonOutput = filterJbangOutput(output);
        JsonObject json = gson.fromJson(jsonOutput, JsonObject.class);

        int score = json.get("overallScore").getAsInt();
        String status = json.get("status").getAsString();

        assertThat(score)
            .as("Broken project should have lower score")
            .isLessThan(70);
        assertThat(status)
            .as("Should not be marked as HEALTHY")
            .isNotEqualTo("HEALTHY");
    }

    @Test
    @EnabledIf("isJbangAvailable")
    @DisplayName("performance-profiler should analyze build performance")
    void performanceProfilerShouldAnalyzeBuild() throws Exception {
        String output = runTool("performance-profiler.java", "simple-java");

        // Should provide performance-related output
        assertThat(output.toLowerCase())
            .as("Should analyze performance")
            .containsAnyOf("performance", "time", "configuration", "execution", "parallel");
    }

    @Test
    @EnabledIf("isJbangAvailable")
    @DisplayName("Tools should handle multi-module projects")
    void toolsShouldHandleMultiModuleProjects() throws Exception {
        String output = runTool("gradle-analyzer.java", "multi-module");

        // Should recognize multi-module structure
        assertThat(output.toLowerCase())
            .as("Should recognize multi-module structure")
            .containsAnyOf("module", "subproject", "app", "core", "common", "api");
    }

    @Test
    @DisplayName("All tool files should exist")
    void allToolFilesShouldExist() {
        String[] expectedTools = {
            "gradle-analyzer.java",
            "cache-validator.java",
            "build-health-check.java",
            "task-analyzer.java",
            "performance-profiler.java"
        };

        for (String tool : expectedTools) {
            Path toolPath = TOOLS_DIR.resolve(tool);
            assertThat(Files.exists(toolPath))
                .as("Tool file should exist: %s", tool)
                .isTrue();
        }
    }

    @Test
    @DisplayName("All tools should have JBang DEPS header")
    void allToolsShouldHaveJbangDeps() throws IOException {
        try (var files = Files.list(TOOLS_DIR)) {
            files
                .filter(f -> f.toString().endsWith(".java"))
                .forEach(toolPath -> {
                    try {
                        String content = Files.readString(toolPath);
                        assertThat(content)
                            .as("Tool %s should have //DEPS declaration", toolPath.getFileName())
                            .contains("//DEPS");

                        assertThat(content)
                            .as("Tool %s should depend on gradle-tooling-api", toolPath.getFileName())
                            .contains("gradle-tooling-api");
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        }
    }

    /**
     * Run a JBang tool against a fixture.
     */
    private String runTool(String toolName, String fixtureName, String... extraArgs) throws Exception {
        Path toolPath = TOOLS_DIR.resolve(toolName);
        Path fixturePath = FIXTURES_DIR.resolve(fixtureName);

        var command = new java.util.ArrayList<String>();
        command.add("jbang");
        command.add(toolPath.toString());
        command.add(fixturePath.toString());
        command.addAll(java.util.Arrays.asList(extraArgs));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(PLUGIN_ROOT.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean completed = process.waitFor(60, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("Tool execution timed out: " + toolName);
        }

        // Return output even if exit code is non-zero (for error analysis)
        return output.toString();
    }
}
