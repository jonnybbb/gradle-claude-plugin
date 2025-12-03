package com.gradle.claude.plugin.scenarios;

import com.gradle.claude.plugin.util.ClaudeTestClient;
import com.gradle.claude.plugin.util.ClaudeTestClient.AgentTestResult;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.*;

import static org.assertj.core.api.Assertions.*;
import static org.gradle.testkit.runner.TaskOutcome.*;

/**
 * End-to-end test for CI environment mismatch detection.
 *
 * <p>This test verifies that the /gradle:doctor command can detect the common
 * "works in CI but not locally" problem by comparing build scans from CI and LOCAL builds.
 *
 * <p>Uses Gradle TestKit to run actual Gradle builds against the ci-env-mismatch fixture.
 *
 * <h2>Test Scenario:</h2>
 * <ol>
 *   <li>Run tests with CI=true and EXAMPLE_API_KEY (passes, tagged "CI")</li>
 *   <li>Run tests without CI secrets (fails due to missing EXAMPLE_API_KEY, tagged "LOCAL")</li>
 *   <li>Wait for build scans to be indexed on Develocity server</li>
 *   <li>Run /gradle:doctor and verify it detects the environment mismatch</li>
 * </ol>
 *
 * <h2>Prerequisites:</h2>
 * <ul>
 *   <li>DEVELOCITY_ACCESS_KEY environment variable (Develocity server access key)</li>
 *   <li>DEVELOCITY_SERVER environment variable (optional, defaults to https://ge.gradle.org)</li>
 *   <li>ANTHROPIC_API_KEY environment variable (for Claude API)</li>
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
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CiEnvMismatchE2ETest {

    private static final Path PLUGIN_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path FIXTURE_PATH = PLUGIN_ROOT.resolve("tests/fixtures/projects/ci-env-mismatch");
    private static final String GRADLE_VERSION = "9.2.1";

    // Configuration
    private static final Duration INITIAL_DELAY = Duration.ofSeconds(2);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(5);
    private static final Duration POLL_TIMEOUT = Duration.ofMinutes(1);

    // Build scan IDs captured during test
    private static String ciBuildScanId;
    private static String localBuildScanId;

    private static final String DEFAULT_DEVELOCITY_SERVER = "https://ge.gradle.org";

    private static HttpClient httpClient;
    private static String develocityServer;

    @BeforeAll
    static void setUpClass() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        // Configure Develocity server from environment (with default fallback)
        develocityServer = System.getenv("DEVELOCITY_SERVER");
        if (develocityServer == null || develocityServer.isBlank()) {
            develocityServer = DEFAULT_DEVELOCITY_SERVER;
        }
        // Remove trailing slash if present
        develocityServer = develocityServer.replaceAll("/$", "");
        System.out.println("Using Develocity server: " + develocityServer);

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

        BuildResult result = GradleRunner.create()
                .withProjectDir(FIXTURE_PATH.toFile())
                .withGradleVersion(GRADLE_VERSION)
                .withArguments("test", "--scan", "--info")
                .withEnvironment(env)
                .forwardOutput()
                .build();

        assertThat(result.task(":test"))
                .as("test task should exist")
                .isNotNull();

        assertThat(result.task(":test").getOutcome())
                .as("CI build should succeed")
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

        BuildResult result = GradleRunner.create()
                .withProjectDir(FIXTURE_PATH.toFile())
                .withGradleVersion(GRADLE_VERSION)
                .withArguments("test", "--scan", "--info")
                .withEnvironment(env)
                .forwardOutput()
                .buildAndFail();

        assertThat(result.task(":test"))
                .as("test task should exist")
                .isNotNull();

        assertThat(result.task(":test").getOutcome())
                .as("LOCAL build should fail")
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

        assertThat(ciBuildScanId)
                .as("CI build scan ID should be set from previous test")
                .isNotNull();
        assertThat(localBuildScanId)
                .as("LOCAL build scan ID should be set from previous test")
                .isNotNull();

        // Initial delay
        System.out.println("Waiting " + INITIAL_DELAY.toSeconds() + "s for initial indexing...");
        Thread.sleep(INITIAL_DELAY.toMillis());

        // Poll for both build scans
        long startTime = System.currentTimeMillis();
        long timeoutMillis = POLL_TIMEOUT.toMillis();
        boolean ciAvailable = false;
        boolean localAvailable = false;

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            if (!ciAvailable) {
                ciAvailable = isBuildScanAvailable(ciBuildScanId);
                System.out.println("CI build scan (" + ciBuildScanId + "): " + (ciAvailable ? "AVAILABLE" : "waiting..."));
            }

            if (!localAvailable) {
                localAvailable = isBuildScanAvailable(localBuildScanId);
                System.out.println("LOCAL build scan (" + localBuildScanId + "): " + (localAvailable ? "AVAILABLE" : "waiting..."));
            }

            if (ciAvailable && localAvailable) {
                break;
            }

            Thread.sleep(POLL_INTERVAL.toMillis());
        }

        assertThat(ciAvailable)
                .as("CI build scan should be available within timeout")
                .isTrue();
        assertThat(localAvailable)
                .as("LOCAL build scan should be available within timeout")
                .isTrue();

        System.out.println("\nBoth build scans are now available!");
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: Run /gradle:doctor and verify environment mismatch detection")
    void runDoctorAndVerifyDetection() throws Exception {
        System.out.println("\n=== Step 4: Running /gradle:doctor ===\n");

        // Load the doctor command prompt
        Path doctorCommandPath = PLUGIN_ROOT.resolve("commands/doctor.md");
        String doctorContent = Files.readString(doctorCommandPath);

        // Extract the prompt content (after frontmatter)
        int endOfFrontmatter = doctorContent.indexOf("---", 3) + 3;
        String doctorPrompt = doctorContent.substring(endOfFrontmatter).trim();

        // Create context about the project and builds
        String projectContext = String.format("""
                ## Project Information

                Project: ci-env-mismatch
                Gradle Version: %s

                ## Recent Build Scans

                This project has recent build scans on %s:

                1. CI Build (PASSED):
                   - Build Scan ID: %s
                   - URL: %s/s/%s

                2. LOCAL Build (FAILED):
                   - Build Scan ID: %s
                   - URL: %s/s/%s
                """,
                GRADLE_VERSION,
                develocityServer,
                ciBuildScanId, develocityServer, ciBuildScanId,
                localBuildScanId, develocityServer, localBuildScanId
        );

        // Run the doctor agent
        try (ClaudeTestClient claude = new ClaudeTestClient()) {
            AgentTestResult result = claude.testAgent(
                    doctorPrompt,
                    projectContext,
                    """
                    Analyze this project and its recent builds.
                    Focus on detecting why CI builds pass but LOCAL builds fail.
                    Compare the environment between the CI and LOCAL builds and identify the root cause.
                    Provide a specific fix recommendation.
                    """
            );

            String response = result.response();
            System.out.println("=== Doctor Output ===\n");
            System.out.println(response);
            System.out.println("\n=== End Doctor Output ===\n");

            // Verify environment mismatch detection
            assertThat(response.toLowerCase())
                    .as("Doctor should detect environment-related issue")
                    .containsAnyOf("environment", "api", "key", "secret");

            assertThat(response.toLowerCase())
                    .as("Doctor should identify the pattern of CI passing and LOCAL failing")
                    .containsAnyOf("ci", "local", "pass", "fail", "missing");

            // Verify suggestion about the missing environment variable
            assertThat(response.toUpperCase())
                    .as("Doctor should identify EXAMPLE_API_KEY as the issue")
                    .containsAnyOf("EXAMPLE_API_KEY", "API_KEY", "APIKEY");

            // Verify build scan references (check for server host or build scan IDs)
            String serverHost = develocityServer.replaceAll("^https?://", "").toLowerCase();
            assertThat(response.toLowerCase())
                    .as("Doctor should reference build scans")
                    .containsAnyOf("build scan", serverHost, ciBuildScanId.toLowerCase(), localBuildScanId.toLowerCase());

            System.out.println("\n✅ All assertions passed - Doctor successfully detected the environment mismatch!");
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

    private boolean isBuildScanAvailable(String buildScanId) {
        try {
            // Check the public build scan page - simpler and works without API auth
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(develocityServer + "/s/" + buildScanId))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            // Use discarding body handler - we only care about the status code
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

}
