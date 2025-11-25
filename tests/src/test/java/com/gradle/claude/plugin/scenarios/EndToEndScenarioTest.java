package com.gradle.claude.plugin.scenarios;

import com.gradle.claude.plugin.util.*;
import com.gradle.claude.plugin.util.ClaudeTestClient.*;
import com.gradle.claude.plugin.util.FixtureLoader.*;
import com.gradle.claude.plugin.util.SkillLoader.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * End-to-end scenario tests that verify complete workflows.
 *
 * These tests simulate realistic user interactions and verify
 * that the plugin provides genuinely useful guidance.
 */
@Tag("ai")
@Tag("e2e")
@DisplayName("End-to-End Scenario Tests")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class EndToEndScenarioTest {

    private static final Path PLUGIN_ROOT = Path.of("..").toAbsolutePath().normalize();

    private ClaudeTestClient claude;
    private SkillLoader skillLoader;
    private FixtureLoader fixtureLoader;

    @BeforeEach
    void setUp() throws IOException {
        claude = new ClaudeTestClient();
        skillLoader = new SkillLoader(PLUGIN_ROOT);
        fixtureLoader = new FixtureLoader(PLUGIN_ROOT);
    }

    @AfterEach
    void tearDown() {
        if (claude != null) {
            claude.close();
        }
    }

    @Nested
    @DisplayName("Scenario: Developer Enabling Configuration Cache")
    class ConfigCacheEnablementScenario {

        @Test
        @DisplayName("Step 1: Initial assessment provides actionable findings")
        void initialAssessmentProvidesActionableFindings() throws IOException {
            Skill skill = skillLoader.loadSkill("gradle-config-cache");
            Fixture fixture = fixtureLoader.loadFixture("config-cache-broken");

            SkillTestResult result = claude.testSkill(
                skill,
                fixture.buildFileContent(),
                "I want to enable configuration cache for this project. What needs to be fixed first?"
            );

            // Should prioritize findings
            assertThat(result.response())
                .as("Should provide prioritized list of issues")
                .containsAnyOf("1.", "First", "Priority", "Start with", "Most critical");

            // Should be actionable
            assertThat(result.hasCodeExamples())
                .as("Should include code examples for fixes")
                .isTrue();

            // Should explain WHY each issue matters
            assertThat(result.response())
                .as("Should explain impact of issues")
                .containsAnyOf("configuration cache", "reuse", "cache invalidation", "serializ");
        }

        @Test
        @DisplayName("Step 2: Fix guidance is complete and copyable")
        void fixGuidanceIsCompleteAndCopyable() throws IOException {
            Skill skill = skillLoader.loadSkill("gradle-config-cache");
            Fixture fixture = fixtureLoader.loadFixture("config-cache-broken");

            SkillTestResult result = claude.testSkill(
                skill,
                fixture.buildFileContent(),
                """
                Fix the System.getProperty issue at line 27. Show me:
                1. The exact code to remove
                2. The exact replacement code
                Make it copy-paste ready.
                """
            );

            // Should have clear before/after
            assertThat(result.hasCodeExamples())
                .as("Should have code blocks")
                .isTrue();

            // Should reference the actual line/code
            assertThat(result.response())
                .as("Should reference the specific code")
                .containsAnyOf("dbUrl", "db.url", "System.getProperty");
        }

        @Test
        @DisplayName("Step 3: Verification instructions are provided")
        void verificationInstructionsAreProvided() throws IOException {
            Skill skill = skillLoader.loadSkill("gradle-config-cache");
            Fixture fixture = fixtureLoader.loadFixture("config-cache-broken");

            SkillTestResult result = claude.testSkill(
                skill,
                fixture.buildFileContent(),
                "After making the fixes, how do I verify configuration cache is working?"
            );

            // Should mention verification commands
            assertThat(result.response())
                .as("Should provide verification command")
                .containsAnyOf("--configuration-cache", "gradle.properties",
                               "org.gradle.configuration-cache");

            // Should explain what success looks like
            assertThat(result.response())
                .as("Should explain expected outcome")
                .containsAnyOf("reuse", "stored", "load", "Configuration cache entry");
        }
    }

    @Nested
    @DisplayName("Scenario: Developer Debugging Slow Build")
    class SlowBuildDebuggingScenario {

        @Test
        @DisplayName("Performance skill provides systematic diagnosis")
        void performanceSkillProvidesSystematicDiagnosis() throws IOException {
            Skill skill = skillLoader.loadSkill("gradle-performance");
            Fixture fixture = fixtureLoader.loadFixture("multi-module");

            SkillTestResult result = claude.testSkill(
                skill,
                fixture.buildFileContent(),
                "My builds are taking 5 minutes. How do I diagnose and fix performance issues?"
            );

            // Should provide diagnostic steps
            assertThat(result.response())
                .as("Should mention build scan")
                .containsAnyOf("--scan", "build scan", "--profile");

            // Should mention key performance settings
            assertThat(result.response())
                .as("Should mention parallelization")
                .containsAnyOf("parallel", "org.gradle.parallel");

            // Should mention caching
            assertThat(result.response())
                .as("Should mention caching")
                .containsAnyOf("cache", "caching", "org.gradle.caching");
        }

        @Test
        @DisplayName("Performance recommendations are specific to project type")
        void performanceRecommendationsAreProjectSpecific() throws IOException {
            Skill skill = skillLoader.loadSkill("gradle-performance");
            Fixture fixture = fixtureLoader.loadFixture("multi-module");

            SkillTestResult result = claude.testSkill(
                skill,
                fixture.buildFileContent(),
                "This is a multi-module Java project. What performance optimizations apply?"
            );

            // Should recognize multi-module
            assertThat(result.response())
                .as("Should address multi-module context")
                .containsAnyOf("multi-module", "multi-project", "subproject", "parallel");

            // Should provide relevant recommendations
            assertThat(result.response())
                .as("Should recommend parallel execution")
                .containsAnyOf("org.gradle.parallel", "parallel=true");
        }
    }

    @Nested
    @DisplayName("Scenario: Developer Creating Custom Task")
    class CustomTaskScenario {

        @Test
        @DisplayName("Task skill provides complete implementation template")
        void taskSkillProvidesCompleteTemplate() throws IOException {
            Skill skill = skillLoader.loadSkill("gradle-task-development");
            Fixture fixture = fixtureLoader.loadFixture("simple-java");

            SkillTestResult result = claude.testSkill(
                skill,
                fixture.buildFileContent(),
                "Create a custom task that generates a build info file with version and timestamp."
            );

            // Should provide complete task code
            assertThat(result.hasCodeExamples())
                .as("Should have code example")
                .isTrue();

            // Should include proper annotations
            assertThat(result.response())
                .as("Should show proper task structure")
                .containsAnyOf("@TaskAction", "abstract class", "DefaultTask");

            // Should include inputs/outputs
            assertThat(result.response())
                .as("Should define inputs/outputs")
                .containsAnyOf("@Input", "@Output", "inputs", "outputs");
        }

        @Test
        @DisplayName("Task recommendations include cache compatibility")
        void taskRecommendationsIncludeCacheCompatibility() throws IOException {
            Skill skill = skillLoader.loadSkill("gradle-task-development");
            Fixture fixture = fixtureLoader.loadFixture("simple-java");

            SkillTestResult result = claude.testSkill(
                skill,
                fixture.buildFileContent(),
                "How do I make my custom task work with build cache and configuration cache?"
            );

            // Should mention @CacheableTask
            assertThat(result.response())
                .as("Should mention cacheability")
                .containsAnyOf("@CacheableTask", "Cacheable", "cacheable");

            // Should mention input/output declarations (path sensitivity only applies to file inputs)
            assertThat(result.response())
                .as("Should mention proper input/output handling")
                .containsAnyOf("@Input", "@Output", "Property<", "inputs", "outputs");
        }
    }

    @Nested
    @DisplayName("Scenario: Doctor Workflow")
    class DoctorWorkflowScenario {

        @Test
        @DisplayName("Doctor provides comprehensive health report")
        void doctorProvidesComprehensiveHealthReport() throws IOException {
            Fixture fixture = fixtureLoader.loadFixture("config-cache-broken");
            Path agentPath = PLUGIN_ROOT.resolve("agents/doctor.md");
            String agentContent = Files.readString(agentPath);
            int endOfFrontmatter = agentContent.indexOf("---", 3) + 3;
            String doctorPrompt = agentContent.substring(endOfFrontmatter).trim();

            AgentTestResult result = claude.testAgent(
                doctorPrompt,
                fixture.buildFileContent(),
                "Run a full health check on this project."
            );

            // Should have structured output
            assertThat(result.hasStructuredOutput())
                .as("Should produce structured report")
                .isTrue();

            // Should cover multiple categories
            assertThat(result.response())
                .as("Should assess multiple health areas")
                .containsAnyOf("Configuration Cache", "Performance", "Dependencies", "Structure");

            // Should prioritize findings (case-insensitive check)
            assertThat(result.response().toLowerCase())
                .as("Should prioritize findings")
                .containsAnyOf("critical", "high", "priority", "warning", "important", "severe");
        }

        @Test
        @DisplayName("Doctor identifies the most impactful issues")
        void doctorIdentifiesMostImpactfulIssues() throws IOException {
            Fixture fixture = fixtureLoader.loadFixture("config-cache-broken");
            Path agentPath = PLUGIN_ROOT.resolve("agents/doctor.md");
            String agentContent = Files.readString(agentPath);
            int endOfFrontmatter = agentContent.indexOf("---", 3) + 3;
            String doctorPrompt = agentContent.substring(endOfFrontmatter).trim();

            AgentTestResult result = claude.testAgent(
                doctorPrompt,
                fixture.buildFileContent(),
                "What are the top 3 issues I should fix first in this project?"
            );

            // Should identify config cache issues
            assertThat(result.response())
                .as("Should identify configuration cache issues")
                .containsAnyOf("configuration cache", "config cache", "System.getProperty",
                               "eager", "tasks.create");

            // Should provide reasoning
            assertThat(result.response())
                .as("Should explain why these are priorities")
                .containsAnyOf("because", "impact", "performance", "modern", "best practice");
        }
    }

    @Nested
    @DisplayName("Scenario: Healthy Project Assessment")
    class HealthyProjectScenario {

        @Test
        @DisplayName("Skills correctly assess healthy projects")
        void skillsCorrectlyAssessHealthyProjects() throws IOException {
            Skill skill = skillLoader.loadSkill("gradle-doctor");
            Fixture fixture = fixtureLoader.loadFixture("simple-java");

            SkillTestResult result = claude.testSkill(
                skill,
                fixture.buildFileContent(),
                "Is this project following Gradle best practices? What's good and what could be improved?"
            );

            // Should recognize positive patterns
            assertThat(result.response())
                .as("Should recognize healthy patterns")
                .containsAnyOf("good", "well", "follows", "properly", "correct", "best practice");

            // Should still provide improvement suggestions (not critical issues)
            assertThat(result.response().toLowerCase())
                .as("Should offer minor suggestions")
                .containsAnyOf("could", "consider", "might", "optional", "further",
                               "recommend", "suggest", "enhance", "improve", "addition");
        }
    }
}
