package com.gradle.claude.plugin.skills;

import com.gradle.claude.plugin.util.*;
import com.gradle.claude.plugin.util.ClaudeTestClient.*;
import com.gradle.claude.plugin.util.FixtureLoader.*;
import com.gradle.claude.plugin.util.SkillLoader.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the gradle-config-cache skill.
 * These tests use the Anthropic API to verify the skill produces valuable output.
 *
 * Tests are only run if ANTHROPIC_API_KEY is set.
 */
@Tag("ai")
@Tag("skills")
@DisplayName("Configuration Cache Skill Integration Tests")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class ConfigCacheSkillIntegrationTest {

    private static final Path PLUGIN_ROOT = Path.of("..").toAbsolutePath().normalize();

    private ClaudeTestClient claude;
    private SkillLoader skillLoader;
    private FixtureLoader fixtureLoader;
    private Skill configCacheSkill;
    private Fixture brokenFixture;

    @BeforeEach
    void setUp() throws IOException {
        claude = new ClaudeTestClient();
        skillLoader = new SkillLoader(PLUGIN_ROOT);
        fixtureLoader = new FixtureLoader(PLUGIN_ROOT);

        configCacheSkill = skillLoader.loadSkill("gradle-config-cache");
        brokenFixture = fixtureLoader.loadFixture("config-cache-broken");
    }

    @AfterEach
    void tearDown() {
        if (claude != null) {
            claude.close();
        }
    }

    @Test
    @DisplayName("Skill should identify System.getProperty issues")
    void shouldIdentifySystemGetPropertyIssues() {
        // The config-cache-broken fixture has System.getProperty at configuration time
        SkillTestResult result = claude.testSkill(
            configCacheSkill,
            brokenFixture.buildFileContent(),
            "What configuration cache issues does this build file have related to system properties?"
        );

        assertThat(result.containsAnyOf("System.getProperty", "system property", "providers.systemProperty"))
            .as("Response should mention System.getProperty issues")
            .isTrue();

        assertThat(result.containsAnyOf("providers", "Provider API", "configuration time"))
            .as("Response should suggest using Provider API")
            .isTrue();
    }

    @Test
    @DisplayName("Skill should identify eager task creation")
    void shouldIdentifyEagerTaskCreation() {
        SkillTestResult result = claude.testSkill(
            configCacheSkill,
            brokenFixture.buildFileContent(),
            "Are there any task creation issues in this build that would affect configuration cache?"
        );

        assertThat(result.containsAnyOf("tasks.create", "eager", "tasks.register"))
            .as("Response should identify tasks.create as problematic")
            .isTrue();

        assertThat(result.containsAnyOf("register", "lazy"))
            .as("Response should recommend lazy task registration")
            .isTrue();
    }

    @Test
    @DisplayName("Skill should identify project access at execution time")
    void shouldIdentifyProjectAccessAtExecutionTime() {
        SkillTestResult result = claude.testSkill(
            configCacheSkill,
            brokenFixture.buildFileContent(),
            "What problems does this build have with project access during task execution?"
        );

        assertThat(result.containsAnyOf("project.copy", "project.exec", "project.delete", "Task.project"))
            .as("Response should identify project access patterns")
            .isTrue();

        assertThat(result.containsAnyOf("FileSystemOperations", "ExecOperations", "inject", "@Inject"))
            .as("Response should suggest service injection")
            .isTrue();
    }

    @Test
    @DisplayName("Skill should provide actionable fixes with code examples")
    void shouldProvideActionableFixes() {
        SkillTestResult result = claude.testSkill(
            configCacheSkill,
            brokenFixture.buildFileContent(),
            "How do I fix the configuration cache issues in this build? Show me code examples."
        );

        assertThat(result.hasCodeExamples())
            .as("Response should include code examples")
            .isTrue();

        assertThat(result.response())
            .as("Response should show before/after patterns")
            .containsAnyOf("✅", "// ✅", "Works", "Fixed", "Replacement");
    }

    @Test
    @DisplayName("Skill should identify multiple issue categories")
    void shouldIdentifyMultipleIssueCategories() {
        // The fixture has 16+ issues across multiple categories
        SkillTestResult result = claude.testSkill(
            configCacheSkill,
            brokenFixture.buildFileContent(),
            "Analyze all configuration cache compatibility issues in this build file."
        );

        // Check that multiple categories are covered
        int categoriesFound = 0;
        if (result.containsAnyOf("System.getProperty", "System.getenv", "system property")) categoriesFound++;
        if (result.containsAnyOf("tasks.create", "getByName", "eager")) categoriesFound++;
        if (result.containsAnyOf("project.copy", "project.exec", "project.delete")) categoriesFound++;
        if (result.containsAnyOf("buildDir", "layout.buildDirectory")) categoriesFound++;

        assertThat(categoriesFound)
            .as("Response should cover at least 3 different issue categories")
            .isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Skill should work with healthy fixture (no false positives)")
    void shouldNotReportFalsePositivesForHealthyProject() throws IOException {
        Fixture healthyFixture = fixtureLoader.loadFixture("simple-java");

        SkillTestResult result = claude.testSkill(
            configCacheSkill,
            healthyFixture.buildFileContent(),
            "Does this build file have any configuration cache issues?"
        );

        // Should recognize the healthy patterns
        assertThat(result.containsAnyOf(
            "no issues", "looks good", "compatible", "follows best practices",
            "properly configured", "no problems", "well-structured"
        ))
            .as("Response should recognize healthy project has no major issues")
            .isTrue();
    }

    @Test
    @DisplayName("Skill should recommend configuration cache enablement steps")
    void shouldRecommendEnablementSteps() throws IOException {
        Fixture healthyFixture = fixtureLoader.loadFixture("simple-java");

        SkillTestResult result = claude.testSkill(
            configCacheSkill,
            healthyFixture.buildFileContent(),
            "How do I enable configuration cache for this project?"
        );

        assertThat(result.containsAllOf("gradle.properties", "configuration-cache"))
            .as("Response should mention gradle.properties setting")
            .isTrue();

        assertThat(result.containsAnyOf("org.gradle.configuration-cache=true", "--configuration-cache"))
            .as("Response should show exact property or flag")
            .isTrue();
    }
}
