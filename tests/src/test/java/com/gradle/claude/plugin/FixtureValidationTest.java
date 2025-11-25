package com.gradle.claude.plugin;

import com.gradle.claude.plugin.util.FixtureLoader;
import com.gradle.claude.plugin.util.FixtureLoader.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
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

        // Should use modern Gradle
        assertThat(fixture.gradleVersion())
            .as("simple-java should use Gradle 8.x")
            .startsWith("8.");
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
}
