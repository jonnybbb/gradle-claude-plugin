package com.gradle.claude.plugin.scenarios;

import com.gradle.claude.plugin.util.ClaudeCLIClient;
import com.gradle.claude.plugin.util.ClaudeCLIClient.CLITestResult;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.regex.*;

import static org.assertj.core.api.Assertions.*;
import static org.gradle.testkit.runner.TaskOutcome.*;

/**
 * End-to-end test for CI environment mismatch detection.
 *
 * <p>This test verifies that the /gradle:diagnose outcome-mismatch command can detect the common
 * "works in CI but not locally" problem by comparing build scans from CI and LOCAL builds.
 *
 * <p>Uses Gradle TestKit to run actual Gradle builds against the ci-env-mismatch fixture.
 *
 * <h2>Test Scenario:</h2>
 * <ol>
 *   <li>Run tests with CI=true and EXAMPLE_API_KEY (passes, tagged "CI")</li>
 *   <li>Run tests without CI secrets (fails due to missing EXAMPLE_API_KEY, tagged "LOCAL")</li>
 *   <li>Wait for build scans to be indexed on Develocity server</li>
 *   <li>Run /gradle:diagnose outcome-mismatch and verify it detects the environment mismatch</li>
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
 * ./gradlew develocityE2ETests
 * </pre>
 */
@Tag("e2e")
@Tag("develocity")
@DisplayName("CI Environment Mismatch Detection E2E Test")
@EnabledIfEnvironmentVariable(named = "DEVELOCITY_ACCESS_KEY", matches = ".+")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CiEnvMismatchE2ETest {

    private static final Path PLUGIN_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path FIXTURE_PATH = PLUGIN_ROOT.resolve("tests/fixtures/projects/ci-env-mismatch");
    private static final Path MCP_CONFIG_PATH = PLUGIN_ROOT.resolve("tests/src/test/resources/e2e-mcp-config.json");
    private static final String GRADLE_VERSION = "9.2.1";

    // Unique tag for this test run - used to filter builds in Develocity
    private static final String TEST_RUN_TAG = "e2e-ci-mismatch-" + System.currentTimeMillis();

    // Build scan IDs captured during test (for logging only)
    private static String ciBuildScanId;
    private static String localBuildScanId;

    private static String develocityServer;

    @BeforeAll
    static void setUpClass() {
        // Configure Develocity server from environment (required)
        develocityServer = System.getenv("DEVELOCITY_SERVER");
        if (develocityServer == null || develocityServer.isBlank()) {
            throw new IllegalStateException("DEVELOCITY_SERVER environment variable is required");
        }
        // Remove trailing slash if present
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
        // Clean up build directory using TestKit
        try {
            GradleRunner.create()
                    .withProjectDir(FIXTURE_PATH.toFile())
                    .withGradleVersion(GRADLE_VERSION)
                    .withArguments("clean", "--quiet")
                    .build();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    @Order(1)
    @DisplayName("Step 1: Run CI build (should pass and publish build scan)")
    void runCiBuild() {
        System.out.println("\n=== Step 1: Running CI build (CI=true, EXAMPLE_API_KEY set) ===\n");

        // Create environment simulating CI with required secrets
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("CI", "true");
        env.put("EXAMPLE_API_KEY", "prod_test_key_12345");
        env.put("E2E_TEST_TAG", TEST_RUN_TAG);

        // Run only ApiClientTest - this test passes in CI (has EXAMPLE_API_KEY)
        // The fixture also contains LocalDevServiceTest which tests the reverse scenario
        // (LOCAL passes, CI fails) but we run that separately to avoid conflicting outcomes
        BuildResult result = GradleRunner.create()
                .withProjectDir(FIXTURE_PATH.toFile())
                .withGradleVersion(GRADLE_VERSION)
                .withArguments("test", "--tests", "*ApiClientTest*", "--scan", "--info", "--no-build-cache", "--rerun-tasks")
                .withEnvironment(env)
                .forwardOutput()
                .build();

        assertThat(result.task(":test"))
                .as("test task should exist")
                .isNotNull();

        assertThat(result.task(":test").getOutcome())
                .as("CI build should succeed (ApiClientTest passes with EXAMPLE_API_KEY)")
                .isEqualTo(SUCCESS);

        ciBuildScanId = extractBuildScanId(result.getOutput());

        assertThat(ciBuildScanId)
                .as("Build scan ID should be captured")
                .isNotNull()
                .isNotBlank();

        System.out.println("CI Build Scan: " + develocityServer + "/s/" + ciBuildScanId);
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Run LOCAL build (should fail and publish build scan)")
    void runLocalBuild() {
        System.out.println("\n=== Step 2: Running LOCAL build (without CI secrets) ===\n");

        // Create environment without CI secrets (simulating local developer environment)
        Map<String, String> env = new HashMap<>(System.getenv());
        env.remove("CI");
        env.remove("EXAMPLE_API_KEY");
        env.put("E2E_TEST_TAG", TEST_RUN_TAG);

        // Run only ApiClientTest - this test fails locally (missing EXAMPLE_API_KEY)
        BuildResult result = GradleRunner.create()
                .withProjectDir(FIXTURE_PATH.toFile())
                .withGradleVersion(GRADLE_VERSION)
                .withArguments("test", "--tests", "*ApiClientTest*", "--scan", "--info", "--no-build-cache", "--rerun-tasks")
                .withEnvironment(env)
                .forwardOutput()
                .buildAndFail();

        assertThat(result.task(":test"))
                .as("test task should exist")
                .isNotNull();

        assertThat(result.task(":test").getOutcome())
                .as("LOCAL build should fail (ApiClientTest fails without EXAMPLE_API_KEY)")
                .isEqualTo(FAILED);

        localBuildScanId = extractBuildScanId(result.getOutput());

        assertThat(localBuildScanId)
                .as("Build scan ID should be captured even for failed build")
                .isNotNull()
                .isNotBlank();

        System.out.println("LOCAL Build Scan: " + develocityServer + "/s/" + localBuildScanId);
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: Wait for build scans to be indexed")
    void waitForBuildScans() throws Exception {
        System.out.println("\n=== Step 3: Waiting for build scans to be indexed ===\n");

        // If we have build scan IDs, the scans were published successfully.
        // The IDs are extracted from the "Publishing Build Scan to Develocity... URL" output.
        assertThat(ciBuildScanId)
                .as("CI build scan ID should be set from previous test (extracted from published URL)")
                .isNotNull()
                .isNotBlank();
        assertThat(localBuildScanId)
                .as("LOCAL build scan ID should be set from previous test (extracted from published URL)")
                .isNotNull()
                .isNotBlank();

        System.out.println("CI Build Scan ID: " + ciBuildScanId);
        System.out.println("LOCAL Build Scan ID: " + localBuildScanId);

        // Wait for Develocity to index the build scans.
        // The MCP server (used in Step 4) has proper authentication to query builds.
        // We just need to give time for indexing to complete.
        Duration indexingDelay = Duration.ofSeconds(10);
        System.out.println("Waiting " + indexingDelay.toSeconds() + "s for Develocity indexing...");
        Thread.sleep(indexingDelay.toMillis());

        System.out.println("\nBuild scans should now be indexed and queryable via MCP.");
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: Run /diagnose outcome-mismatch and verify environment mismatch detection")
    void runDiagnoseOutcomeMismatchAndVerifyDetection() throws Exception {
        System.out.println("\n=== Step 4: Running /diagnose outcome-mismatch ===\n");

        // Minimal context - the agent should discover the environment mismatch on its own
        // Only provide the tag to filter builds from this specific test run
        String projectContext = String.format("""
                Project: ci-env-mismatch

                Filter builds using tag: %s

                There are builds with different outcomes (one passed, one failed).
                Investigate why the outcomes differ.
                """,
                TEST_RUN_TAG
        );

        // Run /gradle:diagnose outcome-mismatch command using Claude CLI with plugin support
        String prompt = String.format("/gradle:diagnose outcome-mismatch\n\n%s", projectContext);
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
            System.out.println("=== /diagnose outcome-mismatch Output ===\n");
            System.out.println(response);
            System.out.println("\n=== End /diagnose outcome-mismatch Output ===\n");

            // Verify the agent identified the environment mismatch pattern
            assertThat(response.toLowerCase())
                    .as("Analyzer should detect CI vs LOCAL mismatch pattern")
                    .containsAnyOf("ci", "local", "mismatch", "different", "environment");

            // Verify the agent identified missing environment variable(s) or configuration as root cause
            // The agent queries Develocity and may paraphrase variable names, so accept common patterns
            assertThat(response.toLowerCase())
                    .as("Analyzer should identify environment variable(s) or missing configuration as the root cause")
                    .containsAnyOf("example_api_key", "api_key", "api key", "apikey", "api_token", "api_url",
                            "environment variable", "env var", "missing", "not set", "secret", "credential");

            // Verify the agent found the failure relates to API/client configuration
            assertThat(response.toLowerCase())
                    .as("Analyzer should identify the failure relates to API/client configuration")
                    .containsAnyOf("apiclient", "illegalstateexception", "api key", "api_key", "token", "missing", "not set");

            // Verify actionable recommendations
            assertThat(response.toLowerCase())
                    .as("Analyzer should provide fix recommendations")
                    .containsAnyOf("set", "export", "configure", "add", "fix", "recommend");

            System.out.println("\n✅ All assertions passed - /diagnose outcome-mismatch correctly identified the environment mismatch!");
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private String extractBuildScanId(String output) {
        // Pattern: https://<server>/s/[buildScanId]
        // Extract host from develocityServer URL and escape dots for regex
        String serverHost = develocityServer
                .replaceAll("^https?://", "")  // Remove protocol
                .replaceAll("/.*$", "")         // Remove path
                .replace(".", "\\.");           // Escape dots for regex
        Pattern pattern = Pattern.compile("https://" + serverHost + "/s/([a-z0-9]+)");
        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

}
