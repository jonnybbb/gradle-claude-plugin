package com.gradle.claude.plugin.scenarios;

import com.gradle.claude.plugin.util.ClaudeCLIClient;
import com.gradle.claude.plugin.util.ClaudeCLIClient.CLITestResult;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.*;

import static org.assertj.core.api.Assertions.*;
import static org.gradle.testkit.runner.TaskOutcome.*;

/**
 * End-to-end test for failure pattern detection using /diagnose failure-patterns command.
 *
 * <p>This test verifies that the failure-pattern-analyzer agent can detect and categorize
 * different types of build failures by analyzing build scan data in Develocity.
 *
 * <h2>Test Scenarios:</h2>
 * <ol>
 *   <li>Compilation failure - Missing semicolon, type errors</li>
 *   <li>Test failure - Assertion failures with stack traces</li>
 *   <li>Dependency resolution failure - Non-existent artifact</li>
 *   <li>Resource failure - OutOfMemoryError during tests</li>
 *   <li>Successful build - Baseline for comparison</li>
 *   <li>Run /diagnose failure-patterns to analyze and categorize all failures</li>
 * </ol>
 *
 * <h2>Prerequisites:</h2>
 * <ul>
 *   <li>DEVELOCITY_ACCESS_KEY environment variable</li>
 *   <li>DEVELOCITY_SERVER environment variable (required)</li>
 * </ul>
 *
 * <h2>Running:</h2>
 * <pre>
 * ./gradlew develocityE2ETests --tests "*FailurePatternsE2ETest*"
 * </pre>
 */
@Tag("e2e")
@Tag("develocity")
@DisplayName("Failure Patterns Detection E2E Test")
@EnabledIfEnvironmentVariable(named = "DEVELOCITY_ACCESS_KEY", matches = ".+")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FailurePatternsE2ETest {

    private static final Path PLUGIN_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path FIXTURE_PATH = PLUGIN_ROOT.resolve("tests/fixtures/projects/failure-patterns");
    private static final Path MCP_CONFIG_PATH = PLUGIN_ROOT.resolve("tests/src/test/resources/e2e-mcp-config.json");
    private static final String GRADLE_VERSION = "9.2.1";

    // Unique tag for this test run - used to filter builds in Develocity
    private static final String TEST_RUN_TAG = "e2e-failure-patterns-" + System.currentTimeMillis();

    // Build scan IDs captured during tests (for logging only)
    private static final Map<String, String> buildScans = new LinkedHashMap<>();

    private static String develocityServer;

    @BeforeAll
    static void setUpClass() {
        develocityServer = System.getenv("DEVELOCITY_SERVER");
        if (develocityServer == null || develocityServer.isBlank()) {
            throw new IllegalStateException("DEVELOCITY_SERVER environment variable is required");
        }
        develocityServer = develocityServer.replaceAll("/$", "");
        System.out.println("Using Develocity server: " + develocityServer);
        System.out.println("Test run tag: " + TEST_RUN_TAG);

        assertThat(FIXTURE_PATH)
                .as("Fixture directory should exist")
                .exists()
                .isDirectory();
    }

    @AfterAll
    static void tearDownClass() {
        try {
            GradleRunner.create()
                    .withProjectDir(FIXTURE_PATH.toFile())
                    .withGradleVersion(GRADLE_VERSION)
                    .withArguments("clean", "--quiet")
                    .build();
        } catch (Exception e) {
            // Ignore cleanup errors
        }

        System.out.println("\n=== Build Scan Summary ===");
        buildScans.forEach((type, id) ->
                System.out.println(type + ": " + develocityServer + "/s/" + id));
    }

    @Test
    @Order(1)
    @DisplayName("Step 1a: Run successful build (baseline)")
    void runSuccessfulBuild() {
        System.out.println("\n=== Step 1a: Running successful build (FAILURE_TYPE=none) ===\n");

        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("FAILURE_TYPE", "none");
        env.put("E2E_TEST_TAG", TEST_RUN_TAG);

        BuildResult result = GradleRunner.create()
                .withProjectDir(FIXTURE_PATH.toFile())
                .withGradleVersion(GRADLE_VERSION)
                .withArguments("build", "--scan", "--info", "--no-build-cache", "--rerun-tasks")
                .withEnvironment(env)
                .forwardOutput()
                .build();

        assertThat(result.task(":build"))
                .as("build task should exist")
                .isNotNull();

        assertThat(result.task(":build").getOutcome())
                .as("Build should succeed")
                .isEqualTo(SUCCESS);

        String buildScanId = extractBuildScanId(result.getOutput());
        assertThat(buildScanId).as("Build scan ID should be captured").isNotNull();
        buildScans.put("success", buildScanId);

        System.out.println("✅ Successful build: " + develocityServer + "/s/" + buildScanId);
    }

    @Test
    @Order(2)
    @DisplayName("Step 1b: Run compilation failure")
    void runCompilationFailure() {
        System.out.println("\n=== Step 1b: Running compilation failure (FAILURE_TYPE=compilation) ===\n");

        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("FAILURE_TYPE", "compilation");
        env.put("E2E_TEST_TAG", TEST_RUN_TAG);

        BuildResult result = GradleRunner.create()
                .withProjectDir(FIXTURE_PATH.toFile())
                .withGradleVersion(GRADLE_VERSION)
                .withArguments("build", "--scan", "--info", "--no-build-cache", "--rerun-tasks")
                .withEnvironment(env)
                .forwardOutput()
                .buildAndFail();

        assertThat(result.task(":compileJava"))
                .as("compileJava task should exist")
                .isNotNull();

        assertThat(result.task(":compileJava").getOutcome())
                .as("Compilation should fail")
                .isEqualTo(FAILED);

        String buildScanId = extractBuildScanId(result.getOutput());
        assertThat(buildScanId).as("Build scan ID should be captured").isNotNull();
        buildScans.put("compilation", buildScanId);

        System.out.println("❌ Compilation failure: " + develocityServer + "/s/" + buildScanId);
    }

    @Test
    @Order(3)
    @DisplayName("Step 1c: Run test failure")
    void runTestFailure() {
        System.out.println("\n=== Step 1c: Running test failure (FAILURE_TYPE=test) ===\n");

        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("FAILURE_TYPE", "test");
        env.put("E2E_TEST_TAG", TEST_RUN_TAG);

        BuildResult result = GradleRunner.create()
                .withProjectDir(FIXTURE_PATH.toFile())
                .withGradleVersion(GRADLE_VERSION)
                .withArguments("build", "--scan", "--info", "--no-build-cache", "--rerun-tasks")
                .withEnvironment(env)
                .forwardOutput()
                .buildAndFail();

        assertThat(result.task(":test"))
                .as("test task should exist")
                .isNotNull();

        assertThat(result.task(":test").getOutcome())
                .as("Tests should fail")
                .isEqualTo(FAILED);

        String buildScanId = extractBuildScanId(result.getOutput());
        assertThat(buildScanId).as("Build scan ID should be captured").isNotNull();
        buildScans.put("test", buildScanId);

        System.out.println("❌ Test failure: " + develocityServer + "/s/" + buildScanId);
    }

    @Test
    @Order(4)
    @DisplayName("Step 1d: Run dependency resolution failure")
    void runDependencyFailure() {
        System.out.println("\n=== Step 1d: Running dependency resolution failure (FAILURE_TYPE=dependency) ===\n");

        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("FAILURE_TYPE", "dependency");
        env.put("E2E_TEST_TAG", TEST_RUN_TAG);

        BuildResult result = GradleRunner.create()
                .withProjectDir(FIXTURE_PATH.toFile())
                .withGradleVersion(GRADLE_VERSION)
                .withArguments("build", "--scan", "--info", "--no-build-cache", "--rerun-tasks")
                .withEnvironment(env)
                .forwardOutput()
                .buildAndFail();

        // Dependency resolution happens before compilation
        String output = result.getOutput();
        assertThat(output.toLowerCase())
                .as("Output should indicate dependency resolution failure")
                .containsAnyOf("could not resolve", "could not find", "fake-library");

        String buildScanId = extractBuildScanId(output);
        assertThat(buildScanId).as("Build scan ID should be captured").isNotNull();
        buildScans.put("dependency", buildScanId);

        System.out.println("❌ Dependency failure: " + develocityServer + "/s/" + buildScanId);
    }

    @Test
    @Order(5)
    @DisplayName("Step 1e: Run resource exhaustion failure (OOM)")
    void runResourceFailure() {
        System.out.println("\n=== Step 1e: Running resource exhaustion failure (FAILURE_TYPE=resource) ===\n");

        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("FAILURE_TYPE", "resource");
        env.put("E2E_TEST_TAG", TEST_RUN_TAG);

        BuildResult result = GradleRunner.create()
                .withProjectDir(FIXTURE_PATH.toFile())
                .withGradleVersion(GRADLE_VERSION)
                .withArguments("build", "--scan", "--info", "--no-build-cache", "--rerun-tasks")
                .withEnvironment(env)
                .forwardOutput()
                .buildAndFail();

        String output = result.getOutput();
        // OOM can manifest in different ways: explicit error message OR test executor crash
        assertThat(output.toLowerCase())
                .as("Output should indicate resource exhaustion or test executor crash")
                .containsAnyOf("outofmemoryerror", "out of memory", "heap space", "java heap",
                        "could not complete execution", "test process encountered an unexpected problem");

        String buildScanId = extractBuildScanId(output);
        assertThat(buildScanId).as("Build scan ID should be captured").isNotNull();
        buildScans.put("resource", buildScanId);

        System.out.println("❌ Resource failure: " + develocityServer + "/s/" + buildScanId);
    }

    @Test
    @Order(6)
    @DisplayName("Step 2: Wait for build scans to be indexed")
    void waitForBuildScans() throws Exception {
        System.out.println("\n=== Step 2: Waiting for build scans to be indexed ===\n");

        assertThat(buildScans)
                .as("Should have build scan IDs from all failure types")
                .containsKeys("success", "compilation", "test", "dependency", "resource");

        System.out.println("Captured build scans:");
        buildScans.forEach((type, id) ->
                System.out.println("  " + type + ": " + develocityServer + "/s/" + id));

        Duration indexingDelay = Duration.ofSeconds(15);
        System.out.println("\nWaiting " + indexingDelay.toSeconds() + "s for Develocity indexing...");
        Thread.sleep(indexingDelay.toMillis());

        System.out.println("Build scans should now be indexed and queryable.");
    }

    @Test
    @Order(7)
    @DisplayName("Step 3: Run /diagnose failure-patterns and analyze failures")
    void analyzeFailurePatterns() throws Exception {
        System.out.println("\n=== Step 3: Running /diagnose failure-patterns ===\n");

        // Minimal context - the agent should discover failure patterns on its own
        // Only provide the tag to filter builds from this specific test run
        String projectContext = String.format("""
                Project: failure-patterns

                Filter builds using tag: %s

                Multiple builds with different failure types have been executed.
                Analyze and categorize the failure patterns.
                """,
                TEST_RUN_TAG
        );

        // Run /gradle:diagnose failure-patterns command using Claude CLI with plugin support
        String prompt = String.format("/gradle:diagnose failure-patterns\n\n%s", projectContext);
        try (ClaudeCLIClient claude = new ClaudeCLIClient(MCP_CONFIG_PATH, PLUGIN_ROOT, FIXTURE_PATH)) {
            CLITestResult result = claude.run(prompt);

            // Verify MCP tools were actually used
            assertThat(result.hasToolExecutions())
                    .as("Should execute real MCP tools (Develocity/DRV)")
                    .isTrue();

            System.out.println("=== MCP Tools Executed ===");
            result.toolExecutions().forEach(exec ->
                    System.out.println("  - " + exec.toolName() + ": " + (exec.succeeded() ? "✅" : "❌")));

            String response = result.response();
            System.out.println("=== /diagnose failure-patterns Output ===\n");
            System.out.println(response);
            System.out.println("\n=== End /diagnose failure-patterns Output ===\n");

            // Verify the analysis identifies different failure categories
            assertThat(response.toLowerCase())
                    .as("Analysis should mention compilation failures")
                    .containsAnyOf("compilation", "compile", "syntax");

            assertThat(response.toLowerCase())
                    .as("Analysis should mention test failures")
                    .containsAnyOf("test", "assertion", "calculator");

            assertThat(response.toLowerCase())
                    .as("Analysis should mention dependency failures")
                    .containsAnyOf("dependency", "resolution", "artifact", "could not find");

            assertThat(response.toLowerCase())
                    .as("Analysis should mention resource failures")
                    .containsAnyOf("memory", "oom", "heap", "resource");

            System.out.println("\n✅ /diagnose failure-patterns completed successfully!");
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private String extractBuildScanId(String output) {
        if (output == null) return null;

        String serverHost = develocityServer
                .replaceAll("^https?://", "")
                .replaceAll("/.*$", "")
                .replace(".", "\\.");
        Pattern pattern = Pattern.compile("https://" + serverHost + "/s/([a-z0-9]+)");
        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
