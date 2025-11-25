package com.gradle.claude.plugin.agents;

import com.gradle.claude.plugin.util.*;
import com.gradle.claude.plugin.util.ClaudeTestClient.*;
import com.gradle.claude.plugin.util.FixtureLoader.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.nio.file.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the Gradle Doctor agent.
 * Verifies the agent produces structured, actionable health reports.
 */
@Tag("ai")
@Tag("agents")
@DisplayName("Doctor Agent Integration Tests")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class DoctorAgentIntegrationTest {

    private static final Path PLUGIN_ROOT = Path.of("..").toAbsolutePath().normalize();

    private ClaudeTestClient claude;
    private FixtureLoader fixtureLoader;
    private String doctorAgentPrompt;

    @BeforeEach
    void setUp() throws IOException {
        claude = new ClaudeTestClient();
        fixtureLoader = new FixtureLoader(PLUGIN_ROOT);

        // Load the doctor agent system prompt
        Path agentPath = PLUGIN_ROOT.resolve("agents/doctor.md");
        String agentContent = Files.readString(agentPath);

        // Extract system prompt (after frontmatter)
        int endOfFrontmatter = agentContent.indexOf("---", 3) + 3;
        doctorAgentPrompt = agentContent.substring(endOfFrontmatter).trim();
    }

    @AfterEach
    void tearDown() {
        if (claude != null) {
            claude.close();
        }
    }

    @Test
    @DisplayName("Agent should produce structured health report")
    void shouldProduceStructuredReport() throws IOException {
        Fixture fixture = fixtureLoader.loadFixture("config-cache-broken");

        AgentTestResult result = claude.testAgent(
            doctorAgentPrompt,
            fixture.buildFileContent(),
            "Perform a comprehensive health check on this Gradle project."
        );

        assertThat(result.hasStructuredOutput())
            .as("Response should have structured sections")
            .isTrue();

        // Check for expected report sections
        assertThat(result.response())
            .as("Response should have health categories")
            .containsAnyOf("Performance", "Caching", "Dependencies", "Structure");
    }

    @Test
    @DisplayName("Agent should identify issues with severity levels")
    void shouldIdentifyIssuesWithSeverity() throws IOException {
        Fixture fixture = fixtureLoader.loadFixture("config-cache-broken");

        AgentTestResult result = claude.testAgent(
            doctorAgentPrompt,
            fixture.buildFileContent(),
            "What issues does this project have? Rate their severity."
        );

        assertThat(result.hasSeverityIndicators())
            .as("Response should include severity indicators")
            .isTrue();
    }

    @Test
    @DisplayName("Agent should provide prioritized recommendations")
    void shouldProvidePrioritizedRecommendations() throws IOException {
        Fixture fixture = fixtureLoader.loadFixture("config-cache-broken");

        AgentTestResult result = claude.testAgent(
            doctorAgentPrompt,
            fixture.buildFileContent(),
            "What should I fix first in this project to improve build health?"
        );

        // Should have some prioritization indicators
        assertThat(result.response())
            .as("Response should prioritize issues")
            .containsAnyOf(
                "High Priority", "Priority", "First", "Most Important",
                "Critical", "1.", "Quick Win", "Start with"
            );
    }

    @Test
    @DisplayName("Agent should analyze multi-module project correctly")
    void shouldAnalyzeMultiModuleProject() throws IOException {
        Fixture fixture = fixtureLoader.loadFixture("multi-module");

        AgentTestResult result = claude.testAgent(
            doctorAgentPrompt,
            fixture.buildFileContent(),
            "Analyze this multi-module build for health issues."
        );

        // Should recognize it's a multi-module project
        assertThat(result.response())
            .as("Response should recognize multi-module structure")
            .containsAnyOf("multi-module", "multi-project", "subprojects", "modules");
    }

    @Test
    @DisplayName("Agent should give positive feedback for healthy project")
    void shouldGivePositiveFeedbackForHealthyProject() throws IOException {
        Fixture fixture = fixtureLoader.loadFixture("simple-java");

        AgentTestResult result = claude.testAgent(
            doctorAgentPrompt,
            fixture.buildFileContent(),
            "Rate the health of this Gradle project."
        );

        // Should recognize healthy patterns
        assertThat(result.response())
            .as("Response should recognize healthy patterns")
            .containsAnyOf(
                "healthy", "good", "well-structured", "best practices",
                "✅", "Healthy", "follows conventions"
            );
    }

    @Test
    @DisplayName("Agent should reference specialized skills for deep dives")
    void shouldReferenceSpecializedSkills() throws IOException {
        Fixture fixture = fixtureLoader.loadFixture("config-cache-broken");

        AgentTestResult result = claude.testAgent(
            doctorAgentPrompt,
            fixture.buildFileContent(),
            "Give me a full health analysis with recommendations for next steps."
        );

        // Should mention specialized skills or areas for further exploration
        assertThat(result.response())
            .as("Response should reference areas for deeper analysis")
            .containsAnyOf(
                "configuration cache", "build cache", "performance",
                "dependencies", "further", "detailed", "specific"
            );
    }
}
