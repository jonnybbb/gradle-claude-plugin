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

/**
 * End-to-end test for flaky test detection using the /diagnose flaky-tests command.
 *
 * <p>This test verifies that the flaky-test-analyzer agent can detect flaky tests by running
 * the same tests multiple times and analyzing build scan data to identify tests that pass/fail
 * intermittently.
 *
 * <p>Uses Gradle TestKit to run actual Gradle builds against the flaky-tests fixture.
 *
 * <h2>Test Scenario:</h2>
 * <ol>
 *   <li>Run tests multiple times (some will pass, some will fail due to randomness)</li>
 *   <li>Wait for build scans to be indexed on Develocity server</li>
 *   <li>Run the flaky-test-analyzer agent (/diagnose flaky-tests)</li>
 *   <li>Verify it performs Pareto analysis, classifies root causes, and provides recommendations</li>
 * </ol>
 *
 * <h2>Prerequisites:</h2>
 * <ul>
 *   <li>DEVELOCITY_ACCESS_KEY environment variable (Develocity server access key)</li>
 *   <li>DEVELOCITY_SERVER environment variable (required)</li>
 * </ul>
 *
 * <h2>Running:</h2>
 * <pre>
 * ./gradlew develocityE2ETests --tests "*FlakyTestsE2ETest*"
 * </pre>
 */
@Tag("e2e")
@Tag("develocity")
@DisplayName("Flaky Tests Detection E2E Test")
@EnabledIfEnvironmentVariable(named = "DEVELOCITY_ACCESS_KEY", matches = ".+")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlakyTestsE2ETest {

    private static final Path PLUGIN_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path FIXTURE_PATH = PLUGIN_ROOT.resolve("tests/fixtures/projects/flaky-tests");
    private static final Path MCP_CONFIG_PATH = PLUGIN_ROOT.resolve("tests/src/test/resources/e2e-mcp-config.json");
    private static final String GRADLE_VERSION = "9.2.1";
    private static final int NUM_RUNS = 5; // Run tests multiple times to demonstrate flakiness

    // Unique tag for this test run - used to filter builds in Develocity
    private static final String TEST_RUN_TAG = "e2e-flaky-" + System.currentTimeMillis();

    // Build scan IDs captured during tests (for logging only)
    private static final List<String> buildScanIds = new ArrayList<>();
    private static int passCount = 0;
    private static int failCount = 0;

    private static String develocityServer;

    @BeforeAll
    static void setUpClass() {
        // Configure Develocity server from environment (required)
        develocityServer = System.getenv("DEVELOCITY_SERVER");
        if (develocityServer == null || develocityServer.isBlank()) {
            throw new IllegalStateException("DEVELOCITY_SERVER environment variable is required");
        }
        develocityServer = develocityServer.replaceAll("/$", "");
        System.out.println("Using Develocity server: " + develocityServer);
        System.out.println("Test run tag: " + TEST_RUN_TAG);

        // Verify fixture exists
        assertThat(FIXTURE_PATH)
                .as("Fixture directory should exist")
                .exists()
                .isDirectory();
    }

    @AfterAll
    static void tearDownClass() {
        // Clean up build directory
        try {
            GradleRunner.create()
                    .withProjectDir(FIXTURE_PATH.toFile())
                    .withGradleVersion(GRADLE_VERSION)
                    .withArguments("clean", "--quiet")
                    .build();
        } catch (Exception e) {
            // Ignore cleanup errors
        }

        System.out.println("\n=== Test Run Summary ===");
        System.out.println("Total runs: " + NUM_RUNS);
        System.out.println("Passed: " + passCount);
        System.out.println("Failed: " + failCount);
        System.out.println("Build Scans: " + buildScanIds);
    }

    @Test
    @Order(1)
    @DisplayName("Step 1: Run tests multiple times to generate flaky test data")
    void runTestsMultipleTimes() {
        System.out.println("\n=== Step 1: Running tests " + NUM_RUNS + " times ===\n");

        for (int i = 1; i <= NUM_RUNS; i++) {
            System.out.println("\n--- Run " + i + " of " + NUM_RUNS + " ---");

            Map<String, String> env = new HashMap<>(System.getenv());
            env.put("RUN_ID", String.valueOf(i));
            env.put("E2E_TEST_TAG", TEST_RUN_TAG);

            try {
                BuildResult result = GradleRunner.create()
                        .withProjectDir(FIXTURE_PATH.toFile())
                        .withGradleVersion(GRADLE_VERSION)
                        .withArguments("test", "--scan", "--info", "--no-build-cache", "--rerun-tasks")
                        .withEnvironment(env)
                        .forwardOutput()
                        .build();

                passCount++;
                String buildScanId = extractBuildScanId(result.getOutput());
                if (buildScanId != null) {
                    buildScanIds.add(buildScanId);
                    System.out.println("✅ Run " + i + " PASSED - Build Scan: " + develocityServer + "/s/" + buildScanId);
                }
            } catch (Exception e) {
                failCount++;
                // Extract build scan ID from failure output if available
                String output = e.getMessage() != null ? e.getMessage() : "";
                String buildScanId = extractBuildScanId(output);
                if (buildScanId != null) {
                    buildScanIds.add(buildScanId);
                }
                System.out.println("❌ Run " + i + " FAILED (expected for flaky tests)");
            }
        }

        // We expect a mix of passes and failures due to flaky tests
        System.out.println("\nTest execution complete. Passed: " + passCount + ", Failed: " + failCount);

        // We should have at least some build scan IDs
        assertThat(buildScanIds)
                .as("Should have captured at least one build scan ID")
                .isNotEmpty();
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Wait for build scans to be indexed")
    void waitForBuildScans() throws Exception {
        System.out.println("\n=== Step 2: Waiting for build scans to be indexed ===\n");

        assertThat(buildScanIds)
                .as("Should have build scan IDs from previous test runs")
                .isNotEmpty();

        System.out.println("Captured " + buildScanIds.size() + " build scan IDs:");
        buildScanIds.forEach(id -> System.out.println("  - " + develocityServer + "/s/" + id));

        // Wait for Develocity to index the build scans
        Duration indexingDelay = Duration.ofSeconds(15);
        System.out.println("\nWaiting " + indexingDelay.toSeconds() + "s for Develocity indexing...");
        Thread.sleep(indexingDelay.toMillis());

        System.out.println("Build scans should now be indexed and queryable.");
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: Run /diagnose flaky-tests and verify flaky test detection")
    void analyzeForFlakyTests() throws Exception {
        System.out.println("\n=== Step 3: Running /diagnose flaky-tests ===\n");

        // Minimal context - the agent should discover flaky tests on its own
        // Only provide the tag to filter builds from this specific test run
        String projectContext = String.format("""
                Project: flaky-tests

                Filter builds using tag: %s

                Analyze flaky tests from recent builds tagged with the above tag.
                """,
                TEST_RUN_TAG
        );

        // Run /gradle:diagnose flaky-tests command using Claude CLI with plugin support
        String prompt = String.format("/gradle:diagnose flaky-tests\n\n%s", projectContext);
        try (ClaudeCLIClient claude = new ClaudeCLIClient(MCP_CONFIG_PATH, PLUGIN_ROOT, FIXTURE_PATH)) {
            CLITestResult result = claude.run(prompt);

            // Verify MCP tools were actually used (not just generating fake function calls)
            assertThat(result.hasToolExecutions())
                    .as("Should execute real MCP tools (Develocity/DRV)")
                    .isTrue();

            System.out.println("=== MCP Tools Executed ===");
            result.toolExecutions().forEach(exec ->
                    System.out.println("  - " + exec.toolName() + ": " + (exec.succeeded() ? "✅" : "❌")));

            String response = result.response();
            System.out.println("=== /diagnose flaky-tests Output ===\n");
            System.out.println(response);
            System.out.println("\n=== End /diagnose flaky-tests Output ===\n");

            // Verify the analysis identifies flaky tests
            assertThat(response.toLowerCase())
                    .as("Analysis should mention flaky tests")
                    .containsAnyOf("flaky", "intermittent", "inconsistent", "sometimes");

            // Verify the analysis mentions test identifiers or containers
            assertThat(response.toLowerCase())
                    .as("Analysis should identify the flaky test class or container")
                    .containsAnyOf("random", "randomservice", "randomservicetest",
                            "test", "container", "class", "flaky-tests-fixture");

            // Verify impact assessment
            assertThat(response.toLowerCase())
                    .as("Analysis should include impact analysis")
                    .containsAnyOf("pareto", "impact", "vital", "failure", "rate", "%");

            // Verify root cause classification
            assertThat(response.toLowerCase())
                    .as("Analysis should classify root cause")
                    .containsAnyOf("root cause", "random", "timing", "seed", "deterministic");

            // Verify actionable recommendations
            assertThat(response.toLowerCase())
                    .as("Analysis should provide actionable recommendations")
                    .containsAnyOf("recommend", "fix", "action", "seed", "mock", "retry");

            System.out.println("\n✅ /diagnose flaky-tests completed successfully!");
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private String extractBuildScanId(String output) {
        if (output == null) return null;

        // Pattern: https://<server>/s/[buildScanId]
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
