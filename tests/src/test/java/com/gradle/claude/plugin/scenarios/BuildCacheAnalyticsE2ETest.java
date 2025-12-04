package com.gradle.claude.plugin.scenarios;

import com.gradle.claude.plugin.util.ClaudeCLIClient;
import com.gradle.claude.plugin.util.ClaudeCLIClient.CLITestResult;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.File;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.*;

import static org.assertj.core.api.Assertions.*;
import static org.gradle.testkit.runner.TaskOutcome.*;

/**
 * End-to-end test for build cache analytics.
 *
 * <p>This test verifies that build cache behavior can be analyzed using Develocity build scans.
 * It runs builds with different cache scenarios and validates that cache performance metrics
 * are correctly captured and can be analyzed.
 *
 * <h2>Test Scenarios:</h2>
 * <ol>
 *   <li>Fresh build - All cache misses (new cache directory)</li>
 *   <li>Warm build - All cache hits (same inputs, populated cache)</li>
 *   <li>Partial build - Some cache hits, some misses (changed inputs)</li>
 *   <li>Invalidated build - Cache invalidation due to changed sources</li>
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
 * ./gradlew develocityE2ETests --tests "*BuildCacheAnalyticsE2ETest*"
 * </pre>
 */
@Tag("e2e")
@Tag("develocity")
@DisplayName("Build Cache Analytics E2E Test")
@EnabledIfEnvironmentVariable(named = "DEVELOCITY_ACCESS_KEY", matches = ".+")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BuildCacheAnalyticsE2ETest {

    private static final Path PLUGIN_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path FIXTURE_PATH = PLUGIN_ROOT.resolve("tests/fixtures/projects/build-cache-analytics");
    private static final Path MCP_CONFIG_PATH = PLUGIN_ROOT.resolve("tests/src/test/resources/e2e-mcp-config.json");
    private static final String GRADLE_VERSION = "9.2.1";

    // Unique tag for this test run - used to filter builds in Develocity
    private static final String TEST_RUN_TAG = "e2e-cache-analytics-" + System.currentTimeMillis();

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

        // Clean up any existing cache directories
        cleanCacheDirectories();
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

        // Clean cache directories
        cleanCacheDirectories();

        System.out.println("\n=== Build Scan Summary ===");
        buildScans.forEach((scenario, id) ->
                System.out.println(scenario + ": " + develocityServer + "/s/" + id));
    }

    private static void cleanCacheDirectories() {
        // Clean up scenario-specific cache directories
        for (String scenario : Arrays.asList("fresh", "warm", "partial", "invalidate")) {
            Path cacheDir = FIXTURE_PATH.resolve("build-cache-" + scenario);
            if (Files.exists(cacheDir)) {
                try {
                    deleteDirectory(cacheDir.toFile());
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    private static void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    @Test
    @Order(1)
    @DisplayName("Step 1a: Fresh build (all cache misses)")
    void runFreshBuild() {
        System.out.println("\n=== Step 1a: Running fresh build (CACHE_SCENARIO=fresh) ===\n");

        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("CACHE_SCENARIO", "fresh");
        env.put("BUILD_NUMBER", "1");
        env.put("E2E_TEST_TAG", TEST_RUN_TAG);

        BuildResult result = GradleRunner.create()
                .withProjectDir(FIXTURE_PATH.toFile())
                .withGradleVersion(GRADLE_VERSION)
                .withArguments("fullBuild", "--scan", "--info", "--build-cache")
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
        buildScans.put("fresh", buildScanId);

        System.out.println("✅ Fresh build (cache misses): " + develocityServer + "/s/" + buildScanId);
    }

    @Test
    @Order(2)
    @DisplayName("Step 1b: Warm build (cache hits)")
    void runWarmBuild() {
        System.out.println("\n=== Step 1b: Running warm build (CACHE_SCENARIO=fresh, same cache) ===\n");

        // Run same scenario again to get cache hits
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("CACHE_SCENARIO", "fresh"); // Same cache directory
        env.put("BUILD_NUMBER", "2");
        env.put("E2E_TEST_TAG", TEST_RUN_TAG);

        // Clean build directory but keep cache
        try {
            GradleRunner.create()
                    .withProjectDir(FIXTURE_PATH.toFile())
                    .withGradleVersion(GRADLE_VERSION)
                    .withArguments("clean", "--quiet")
                    .withEnvironment(env)
                    .build();
        } catch (Exception e) {
            // Ignore
        }

        BuildResult result = GradleRunner.create()
                .withProjectDir(FIXTURE_PATH.toFile())
                .withGradleVersion(GRADLE_VERSION)
                .withArguments("fullBuild", "--scan", "--info", "--build-cache")
                .withEnvironment(env)
                .forwardOutput()
                .build();

        assertThat(result.task(":build"))
                .as("build task should exist")
                .isNotNull();

        assertThat(result.task(":build").getOutcome())
                .as("Build should succeed")
                .isEqualTo(SUCCESS);

        // Verify cache hits in output
        String output = result.getOutput();
        assertThat(output)
                .as("Should have FROM-CACHE outcomes")
                .contains("FROM-CACHE");

        String buildScanId = extractBuildScanId(output);
        assertThat(buildScanId).as("Build scan ID should be captured").isNotNull();
        buildScans.put("warm", buildScanId);

        System.out.println("✅ Warm build (cache hits): " + develocityServer + "/s/" + buildScanId);
    }

    @Test
    @Order(3)
    @DisplayName("Step 1c: Partial cache hit build (some inputs changed)")
    void runPartialBuild() {
        System.out.println("\n=== Step 1c: Running partial build (CACHE_SCENARIO=partial) ===\n");

        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("CACHE_SCENARIO", "partial");
        env.put("BUILD_NUMBER", "3");
        env.put("E2E_TEST_TAG", TEST_RUN_TAG);

        // Clean build directory
        try {
            GradleRunner.create()
                    .withProjectDir(FIXTURE_PATH.toFile())
                    .withGradleVersion(GRADLE_VERSION)
                    .withArguments("clean", "--quiet")
                    .withEnvironment(env)
                    .build();
        } catch (Exception e) {
            // Ignore
        }

        BuildResult result = GradleRunner.create()
                .withProjectDir(FIXTURE_PATH.toFile())
                .withGradleVersion(GRADLE_VERSION)
                .withArguments("fullBuild", "--scan", "--info", "--build-cache")
                .withEnvironment(env)
                .forwardOutput()
                .build();

        assertThat(result.task(":build"))
                .as("build task should exist")
                .isNotNull();

        String buildScanId = extractBuildScanId(result.getOutput());
        assertThat(buildScanId).as("Build scan ID should be captured").isNotNull();
        buildScans.put("partial", buildScanId);

        System.out.println("✅ Partial build: " + develocityServer + "/s/" + buildScanId);
    }

    @Test
    @Order(4)
    @DisplayName("Step 1d: Invalidated build (sources changed)")
    void runInvalidatedBuild() {
        System.out.println("\n=== Step 1d: Running invalidated build (CACHE_SCENARIO=invalidate) ===\n");

        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("CACHE_SCENARIO", "invalidate");
        env.put("BUILD_NUMBER", "4");
        env.put("E2E_TEST_TAG", TEST_RUN_TAG);

        // Clean build directory
        try {
            GradleRunner.create()
                    .withProjectDir(FIXTURE_PATH.toFile())
                    .withGradleVersion(GRADLE_VERSION)
                    .withArguments("clean", "--quiet")
                    .withEnvironment(env)
                    .build();
        } catch (Exception e) {
            // Ignore
        }

        BuildResult result = GradleRunner.create()
                .withProjectDir(FIXTURE_PATH.toFile())
                .withGradleVersion(GRADLE_VERSION)
                .withArguments("fullBuild", "--scan", "--info", "--build-cache")
                .withEnvironment(env)
                .forwardOutput()
                .build();

        assertThat(result.task(":build"))
                .as("build task should exist")
                .isNotNull();

        String buildScanId = extractBuildScanId(result.getOutput());
        assertThat(buildScanId).as("Build scan ID should be captured").isNotNull();
        buildScans.put("invalidate", buildScanId);

        System.out.println("✅ Invalidated build: " + develocityServer + "/s/" + buildScanId);
    }

    @Test
    @Order(5)
    @DisplayName("Step 2: Wait for build scans to be indexed")
    void waitForBuildScans() throws Exception {
        System.out.println("\n=== Step 2: Waiting for build scans to be indexed ===\n");

        assertThat(buildScans)
                .as("Should have build scan IDs from all cache scenarios")
                .containsKeys("fresh", "warm", "partial", "invalidate");

        System.out.println("Captured build scans:");
        buildScans.forEach((scenario, id) ->
                System.out.println("  " + scenario + ": " + develocityServer + "/s/" + id));

        Duration indexingDelay = Duration.ofSeconds(15);
        System.out.println("\nWaiting " + indexingDelay.toSeconds() + "s for Develocity indexing...");
        Thread.sleep(indexingDelay.toMillis());

        System.out.println("Build scans should now be indexed and queryable.");
    }

    @Test
    @Order(6)
    @DisplayName("Step 3: Analyze build cache performance across scenarios")
    void analyzeCachePerformance() throws Exception {
        System.out.println("\n=== Step 3: Analyzing build cache performance ===\n");

        // Minimal context - the agent should discover cache performance patterns on its own
        // Only provide the tag to filter builds from this specific test run
        String projectContext = String.format("""
                Project: build-cache-analytics

                Filter builds using tag: %s

                Multiple builds with different cache scenarios have been executed.
                Analyze build cache performance across these builds.
                """,
                TEST_RUN_TAG
        );

        // Run /gradle:diagnose cache-performance command using Claude CLI with plugin support
        String prompt = String.format("/gradle:diagnose cache-performance\n\n%s", projectContext);
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
            System.out.println("=== Build Cache Analysis Output ===\n");
            System.out.println(response);
            System.out.println("\n=== End Build Cache Analysis Output ===\n");

            // Verify the analysis discusses caching
            assertThat(response.toLowerCase())
                    .as("Analysis should mention cache hits")
                    .containsAnyOf("cache hit", "cached", "from-cache", "hit rate");

            assertThat(response.toLowerCase())
                    .as("Analysis should mention cache misses")
                    .containsAnyOf("cache miss", "not cached", "miss", "avoided");

            // Verify the analysis compares scenarios
            assertThat(response.toLowerCase())
                    .as("Analysis should compare warm vs fresh builds")
                    .containsAnyOf("warm", "fresh", "first build", "second build", "repeat");

            // Verify recommendations
            assertThat(response.toLowerCase())
                    .as("Analysis should provide recommendations")
                    .containsAnyOf("recommend", "improve", "optimize", "input", "output");

            System.out.println("\n✅ Build cache analysis completed successfully!");
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
