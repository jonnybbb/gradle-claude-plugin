package com.gradle.claude.plugin;

import com.gradle.claude.plugin.util.FixtureLoader;
import com.gradle.claude.plugin.util.FixtureLoader.*;
import com.gradle.claude.plugin.util.SensitiveDataDetector;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests to validate test fixtures are properly set up and documented.
 */
@Tag("fixtures")
@DisplayName("Test Fixture Validation")
class FixtureValidationTest {

    private static final Path PLUGIN_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static FixtureLoader fixtureLoader;
    private static List<Fixture> allFixtures;

    @BeforeAll
    static void setUp() throws IOException {
        fixtureLoader = new FixtureLoader(PLUGIN_ROOT);
        allFixtures = fixtureLoader.loadAllFixtures();
    }

    @Test
    @DisplayName("Should have expected fixture count")
    void shouldHaveExpectedFixtureCount() {
        assertThat(allFixtures).hasSize(5);
    }

    static Stream<Fixture> allFixturesProvider() {
        return allFixtures.stream();
    }

    @Test
    @DisplayName("Expected fixtures should exist")
    void expectedFixturesShouldExist() {
        List<String> expectedFixtures = List.of(
            "simple-java",
            "config-cache-broken",
            "legacy-groovy",
            "multi-module",
            "spring-boot"
        );

        List<String> actualNames = allFixtures.stream()
            .map(Fixture::name)
            .toList();

        assertThat(actualNames)
            .as("All expected fixtures should be present")
            .containsAll(expectedFixtures);
    }

    @ParameterizedTest(name = "Fixture ''{0}'' has build file")
    @MethodSource("allFixturesProvider")
    @DisplayName("All fixtures should have a build file")
    void fixtureShouldHaveBuildFile(Fixture fixture) {
        assertThat(fixture.buildFileContent())
            .as("Fixture '%s' should have non-empty build file", fixture.name())
            .isNotBlank();
    }

    @ParameterizedTest(name = "Fixture ''{0}'' has Gradle wrapper")
    @MethodSource("allFixturesProvider")
    @DisplayName("All fixtures should have Gradle wrapper version")
    void fixtureShouldHaveGradleWrapper(Fixture fixture) {
        assertThat(fixture.gradleVersion())
            .as("Fixture '%s' should have detectable Gradle version", fixture.name())
            .isNotEqualTo("unknown");
    }

    @Test
    @DisplayName("config-cache-broken should have documented issues")
    void configCacheBrokenShouldHaveDocumentedIssues() throws IOException {
        Fixture fixture = fixtureLoader.loadFixture("config-cache-broken");

        assertThat(fixture.knownIssues())
            .as("config-cache-broken should have at least 10 documented issues")
            .hasSizeGreaterThanOrEqualTo(10);

        // Should have issues across multiple categories
        assertThat(fixture.hasIssuesOfCategory(IssueCategory.EAGER_TASK))
            .as("Should have eager task issues")
            .isTrue();

        assertThat(fixture.hasIssuesOfCategory(IssueCategory.PROJECT_ACCESS))
            .as("Should have project access issues")
            .isTrue();

        assertThat(fixture.hasIssuesOfCategory(IssueCategory.SYSTEM_ACCESS))
            .as("Should have system access issues")
            .isTrue();
    }

    @Test
    @DisplayName("simple-java should be a healthy baseline")
    void simpleJavaShouldBeHealthyBaseline() throws IOException {
        Fixture fixture = fixtureLoader.loadFixture("simple-java");

        assertThat(fixture.knownIssues())
            .as("simple-java should have no documented issues (healthy baseline)")
            .isEmpty();

        assertThat(fixture.projectType())
            .as("simple-java should be a single project")
            .isEqualTo(ProjectType.SINGLE_PROJECT);

        // Should use modern Gradle (8.x or 9.x)
        assertThat(fixture.gradleVersion())
            .as("simple-java should use modern Gradle 8.x or 9.x")
            .matches("(8|9)\\.\\d+(\\.\\d+)?");
    }

    @Test
    @DisplayName("multi-module should be a multi-project build")
    void multiModuleShouldBeMultiProject() throws IOException {
        Fixture fixture = fixtureLoader.loadFixture("multi-module");

        assertThat(fixture.projectType())
            .as("multi-module should be detected as multi-project")
            .isEqualTo(ProjectType.MULTI_PROJECT);
    }

    @Test
    @DisplayName("legacy-groovy should use Groovy DSL")
    void legacyGroovyShouldUseGroovyDsl() throws IOException {
        Fixture fixture = fixtureLoader.loadFixture("legacy-groovy");

        // Should use Groovy DSL (build.gradle not build.gradle.kts)
        assertThat(fixture.buildFileContent())
            .as("legacy-groovy should use Groovy DSL syntax")
            .doesNotContain("fun ")  // No Kotlin functions
            .doesNotContain("val ")  // No Kotlin val declarations
            .doesNotContain(": Property<"); // No Kotlin type declarations
    }

    @Test
    @DisplayName("Fixtures should have valid directory structure")
    void fixturesShouldHaveValidStructure() throws IOException {
        for (Fixture fixture : allFixtures) {
            var files = fixture.getAllFiles();

            // Should have at least a build file and wrapper
            assertThat(files)
                .as("Fixture '%s' should have multiple files", fixture.name())
                .hasSizeGreaterThanOrEqualTo(2);

            // Should have wrapper properties
            assertThat(files.keySet().stream()
                .anyMatch(f -> f.contains("gradle-wrapper.properties")))
                .as("Fixture '%s' should have gradle-wrapper.properties", fixture.name())
                .isTrue();
        }
    }

    @Test
    @DisplayName("Issue documentation should be consistent")
    void issueDocumentationShouldBeConsistent() throws IOException {
        Fixture fixture = fixtureLoader.loadFixture("config-cache-broken");

        // Issues should be numbered sequentially
        List<Integer> issueNumbers = fixture.knownIssues().stream()
            .map(KnownIssue::number)
            .sorted()
            .toList();

        for (int i = 0; i < issueNumbers.size(); i++) {
            assertThat(issueNumbers.get(i))
                .as("Issue numbers should be sequential starting from 1")
                .isEqualTo(i + 1);
        }
    }

    // =========================================================================
    // Issue Category Coverage Tests
    // =========================================================================

    @Test
    @DisplayName("config-cache-broken should cover all major issue categories")
    void configCacheBrokenShouldCoverAllMajorCategories() throws IOException {
        Fixture fixture = fixtureLoader.loadFixture("config-cache-broken");

        // Should have a good spread of issue types for comprehensive testing
        int eagerTaskCount = fixture.countIssuesByCategory(IssueCategory.EAGER_TASK);
        int projectAccessCount = fixture.countIssuesByCategory(IssueCategory.PROJECT_ACCESS);
        int systemAccessCount = fixture.countIssuesByCategory(IssueCategory.SYSTEM_ACCESS);

        assertThat(eagerTaskCount)
            .as("Should have multiple eager task issues for testing detection")
            .isGreaterThanOrEqualTo(3);

        assertThat(projectAccessCount)
            .as("Should have multiple project access issues")
            .isGreaterThanOrEqualTo(3);

        assertThat(systemAccessCount)
            .as("Should have multiple system access issues")
            .isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Issue descriptions should be meaningful")
    void issueDescriptionsShouldBeMeaningful() throws IOException {
        Fixture fixture = fixtureLoader.loadFixture("config-cache-broken");

        for (KnownIssue issue : fixture.knownIssues()) {
            assertThat(issue.description())
                .as("Issue %d description should be non-empty", issue.number())
                .isNotBlank();

            assertThat(issue.description().length())
                .as("Issue %d description should be descriptive (>10 chars)", issue.number())
                .isGreaterThan(10);
        }
    }

    // =========================================================================
    // Fixture Gradle Version Tests
    // =========================================================================

    @Test
    @DisplayName("Fixtures should use appropriate Gradle versions for their purpose")
    void fixturesShouldUseAppropriateGradleVersions() throws IOException {
        // simple-java should use latest for best practices baseline
        Fixture simpleJava = fixtureLoader.loadFixture("simple-java");
        assertThat(simpleJava.gradleVersion())
            .as("simple-java should use modern Gradle 8.x or 9.x")
            .matches("(8|9)\\.\\d+(\\.\\d+)?");

        // legacy-groovy should use older version for migration testing
        Fixture legacyGroovy = fixtureLoader.loadFixture("legacy-groovy");
        assertThat(legacyGroovy.gradleVersion())
            .as("legacy-groovy should use Gradle 7.x for migration testing")
            .startsWith("7.");
    }

    @Test
    @DisplayName("Fixtures should not use EOL Gradle versions")
    void fixturesShouldNotUseEolGradleVersions() throws IOException {
        for (Fixture fixture : allFixtures) {
            String version = fixture.gradleVersion();
            if (!"unknown".equals(version)) {
                int majorVersion = Integer.parseInt(version.split("\\.")[0]);
                assertThat(majorVersion)
                    .as("Fixture %s should use Gradle 7+ (Gradle 6.x is EOL)", fixture.name())
                    .isGreaterThanOrEqualTo(7);
            }
        }
    }

    // =========================================================================
    // Fixture Content Validation Tests
    // =========================================================================

    @Test
    @DisplayName("No fixture should contain sensitive data patterns")
    void noFixtureShouldContainSensitiveData() throws IOException {
        for (Fixture fixture : allFixtures) {
            Map<String, String> files = fixture.getAllFiles();

            for (var entry : files.entrySet()) {
                String content = entry.getValue();

                // Use SensitiveDataDetector for comprehensive detection with allowlisting
                var detectedPatterns = SensitiveDataDetector.detect(content);

                assertThat(detectedPatterns)
                    .as("File %s in fixture %s should not contain sensitive data. Found: %s",
                        entry.getKey(), fixture.name(),
                        detectedPatterns.stream()
                            .map(SensitiveDataDetector.SensitivePattern::description)
                            .toList())
                    .isEmpty();
            }
        }
    }

    @Test
    @DisplayName("Multi-module fixture should have meaningful subprojects")
    void multiModuleShouldHaveMeaningfulSubprojects() throws IOException {
        Fixture fixture = fixtureLoader.loadFixture("multi-module");
        Map<String, String> files = fixture.getAllFiles();

        // Should have multiple build files (root + subprojects)
        long buildFileCount = files.keySet().stream()
            .filter(f -> f.endsWith("build.gradle") || f.endsWith("build.gradle.kts"))
            .count();

        assertThat(buildFileCount)
            .as("Multi-module fixture should have at least 3 build files")
            .isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("spring-boot fixture should have Spring Boot specific configuration")
    void springBootFixtureShouldHaveSpringConfig() throws IOException {
        Fixture fixture = fixtureLoader.loadFixture("spring-boot");
        String buildContent = fixture.buildFileContent();

        assertThat(buildContent)
            .as("Spring Boot fixture should reference Spring Boot plugin")
            .containsIgnoringCase("spring");
    }

    // =========================================================================
    // Cross-Fixture Consistency Tests
    // =========================================================================

    @Test
    @DisplayName("All fixtures should have consistent wrapper distribution type")
    void allFixturesShouldHaveConsistentWrapperDistribution() throws IOException {
        for (Fixture fixture : allFixtures) {
            Map<String, String> files = fixture.getAllFiles();

            String wrapperProps = files.get("gradle/wrapper/gradle-wrapper.properties");
            if (wrapperProps != null) {
                // Should use -bin or -all distribution
                assertThat(wrapperProps)
                    .as("Fixture %s wrapper should use bin or all distribution", fixture.name())
                    .containsPattern("gradle-\\d+\\.\\d+(\\.\\d+)?-(bin|all)\\.zip");
            }
        }
    }

    @Test
    @DisplayName("Fixtures should have unique purposes")
    void fixturesShouldHaveUniquePurposes() {
        // Each fixture should serve a distinct testing purpose
        Map<String, String> fixturePurposes = Map.of(
            "simple-java", "healthy baseline",
            "config-cache-broken", "issue detection",
            "legacy-groovy", "migration testing",
            "multi-module", "scale testing",
            "spring-boot", "framework compatibility"
        );

        for (Fixture fixture : allFixtures) {
            assertThat(fixturePurposes)
                .as("Fixture %s should have a documented purpose", fixture.name())
                .containsKey(fixture.name());
        }
    }

    @Test
    @DisplayName("All fixtures should have gradle.properties")
    void allFixturesShouldHaveGradleProperties() throws IOException {
        for (Fixture fixture : allFixtures) {
            Map<String, String> files = fixture.getAllFiles();

            assertThat(files.keySet().stream().anyMatch(f -> f.equals("gradle.properties")))
                .as("Fixture %s should have gradle.properties", fixture.name())
                .isTrue();
        }
    }
}
