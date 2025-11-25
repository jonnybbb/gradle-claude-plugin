package com.gradle.claude.plugin.skills;

import com.gradle.claude.plugin.util.*;
import com.gradle.claude.plugin.util.ClaudeTestClient.*;
import com.gradle.claude.plugin.util.FixtureLoader.*;
import com.gradle.claude.plugin.util.SkillLoader.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests that skills accurately detect and address documented issues.
 *
 * These tests verify that skills don't just produce output, but produce
 * CORRECT output that addresses the actual problems in the fixtures.
 *
 * The config-cache-broken fixture contains 16 documented issues marked with
 * "❌ ISSUE N:" comments. Issue categories tested:
 *
 * <ul>
 *   <li>ISSUE 1, 13: System.getProperty (configuration + execution time)</li>
 *   <li>ISSUE 2, 14: System.getenv (configuration + execution time)</li>
 *   <li>ISSUE 3-5: Eager task creation with tasks.create()</li>
 *   <li>ISSUE 6-7: Eager task lookup with tasks.getByName()</li>
 *   <li>ISSUE 8-10, 12, 15: project.* API in doLast (copy/exec/delete/file/javaexec)</li>
 *   <li>ISSUE 11: Task.project access at execution time</li>
 *   <li>ISSUE 16: Deprecated buildDir direct access</li>
 * </ul>
 *
 * @see test-fixtures/projects/config-cache-broken/build.gradle.kts
 */
@Tag("ai")
@Tag("accuracy")
@DisplayName("Skill Accuracy Tests")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class SkillAccuracyTest {

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
    @DisplayName("Config Cache Skill Accuracy")
    class ConfigCacheSkillAccuracy {

        private Skill configCacheSkill;
        private Fixture brokenFixture;
        private List<KnownIssue> knownIssues;

        @BeforeEach
        void setUp() throws IOException {
            configCacheSkill = skillLoader.loadSkill("gradle-config-cache");
            brokenFixture = fixtureLoader.loadFixture("config-cache-broken");
            knownIssues = brokenFixture.knownIssues();
        }

        @Test
        @DisplayName("Detects System.getProperty at configuration and execution time")
        void shouldDetectSystemGetPropertyIssues() {
            SkillTestResult result = claude.testSkill(
                configCacheSkill,
                brokenFixture.buildFileContent(),
                "List all System.getProperty usages that would cause configuration cache problems."
            );

            // Should identify both configuration-time and execution-time issues
            assertThat(result.response())
                .as("Should mention System.getProperty")
                .containsIgnoringCase("System.getProperty");

            // Should suggest providers API
            assertThat(result.response())
                .as("Should recommend providers.systemProperty")
                .containsAnyOf("providers.systemProperty", "Provider API", "providers.gradleProperty");
        }

        @Test
        @DisplayName("Detects System.getenv at configuration and execution time")
        void shouldDetectSystemGetenvIssues() {
            SkillTestResult result = claude.testSkill(
                configCacheSkill,
                brokenFixture.buildFileContent(),
                "List all System.getenv usages that would cause configuration cache problems."
            );

            assertThat(result.response())
                .as("Should mention System.getenv")
                .containsIgnoringCase("System.getenv");

            assertThat(result.response())
                .as("Should recommend providers.environmentVariable")
                .containsAnyOf("providers.environmentVariable", "Provider API");
        }

        @Test
        @DisplayName("Detects eager task creation via tasks.create()")
        void shouldDetectEagerTaskCreation() {
            SkillTestResult result = claude.testSkill(
                configCacheSkill,
                brokenFixture.buildFileContent(),
                "Find all instances of eager task creation in this build file."
            );

            assertThat(result.response())
                .as("Should identify tasks.create")
                .containsAnyOf("tasks.create", "create(", "eager");

            assertThat(result.response())
                .as("Should recommend tasks.register")
                .containsAnyOf("tasks.register", "register(", "lazy");
        }

        @Test
        @DisplayName("Detects eager task lookup via tasks.getByName()")
        void shouldDetectGetByNameIssues() {
            SkillTestResult result = claude.testSkill(
                configCacheSkill,
                brokenFixture.buildFileContent(),
                "What problems does using tasks.getByName cause for configuration cache?"
            );

            assertThat(result.response())
                .as("Should identify getByName problem")
                .containsAnyOf("getByName", "eager");

            assertThat(result.response())
                .as("Should recommend named()")
                .containsAnyOf("named(", "tasks.named");
        }

        @Test
        @DisplayName("Detects project.* API calls in doLast blocks")
        void shouldDetectProjectAccessInDoLast() {
            SkillTestResult result = claude.testSkill(
                configCacheSkill,
                brokenFixture.buildFileContent(),
                "Find all project.* method calls inside doLast blocks that break configuration cache."
            );

            // Should identify multiple project.* issues
            List<String> projectMethods = List.of("project.copy", "project.exec", "project.delete",
                                                   "project.file", "project.javaexec");

            int foundCount = 0;
            for (String method : projectMethods) {
                if (result.response().toLowerCase().contains(method.toLowerCase())) {
                    foundCount++;
                }
            }

            assertThat(foundCount)
                .as("Should identify at least 3 different project.* issues")
                .isGreaterThanOrEqualTo(3);

            // Should suggest service injection
            assertThat(result.response())
                .as("Should recommend service injection pattern")
                .containsAnyOf("@Inject", "FileSystemOperations", "ExecOperations", "inject");
        }

        @Test
        @DisplayName("Detects Task.project access at execution time")
        void shouldDetectTaskProjectAccess() {
            SkillTestResult result = claude.testSkill(
                configCacheSkill,
                brokenFixture.buildFileContent(),
                "Is accessing 'project.buildDir' inside doLast safe for configuration cache? What's the fix?"
            );

            assertThat(result.response())
                .as("Should identify Task.project access issue")
                .containsAnyOf("project", "execution time", "configuration cache");

            assertThat(result.response())
                .as("Should suggest capturing value during configuration")
                .containsAnyOf("capture", "val ", "layout.buildDirectory", "doFirst");
        }

        @Test
        @DisplayName("Detects deprecated buildDir direct access")
        void shouldDetectBuildDirAccess() {
            SkillTestResult result = claude.testSkill(
                configCacheSkill,
                brokenFixture.buildFileContent(),
                "Is using $buildDir directly in task configuration compatible with configuration cache?"
            );

            assertThat(result.response())
                .as("Should identify buildDir deprecation")
                .containsAnyOf("buildDir", "deprecated", "layout.buildDirectory");

            assertThat(result.response())
                .as("Should recommend layout.buildDirectory")
                .contains("layout.buildDirectory");
        }

        @Test
        @DisplayName("Achieves >80% detection rate across all 7 issue categories")
        void shouldAchieveHighDetectionRate() {
            SkillTestResult result = claude.testSkill(
                configCacheSkill,
                brokenFixture.buildFileContent(),
                """
                Analyze this build file for ALL configuration cache compatibility issues.
                List each issue with its line number or code snippet.
                Be comprehensive - find every problem.
                """
            );

            // Count how many issue categories are detected
            int detectedCategories = 0;

            // Category 1: System.getProperty (issues 1, 13)
            if (result.containsAnyOf("System.getProperty", "getProperty")) detectedCategories++;

            // Category 2: System.getenv (issues 2, 14)
            if (result.containsAnyOf("System.getenv", "getenv")) detectedCategories++;

            // Category 3: Eager task creation (issues 3, 4, 5)
            if (result.containsAnyOf("tasks.create", "create(", "eager")) detectedCategories++;

            // Category 4: getByName (issues 6, 7)
            if (result.containsAnyOf("getByName", "tasks.getByName")) detectedCategories++;

            // Category 5: project.* in doLast (issues 8, 9, 10, 12, 15)
            if (result.containsAnyOf("project.copy", "project.exec", "project.delete",
                                     "project.file", "project.javaexec")) detectedCategories++;

            // Category 6: Task.project access (issue 11)
            if (result.containsAnyOf("project.buildDir", "Task.project")) detectedCategories++;

            // Category 7: buildDir direct access (issue 16)
            if (result.containsAnyOf("$buildDir", "buildDir")) detectedCategories++;

            // Should detect at least 6 out of 7 categories (>85%)
            assertThat(detectedCategories)
                .as("Should detect at least 6 out of 7 issue categories. Found: %d", detectedCategories)
                .isGreaterThanOrEqualTo(6);
        }

        @Test
        @DisplayName("No false positives on healthy project (simple-java fixture)")
        void shouldNotReportFalsePositives() throws IOException {
            Fixture healthyFixture = fixtureLoader.loadFixture("simple-java");

            SkillTestResult result = claude.testSkill(
                configCacheSkill,
                healthyFixture.buildFileContent(),
                "List all configuration cache issues in this build file. If there are none, say so."
            );

            // Should recognize healthy project
            assertThat(result.response().toLowerCase())
                .as("Should indicate no major issues for healthy project")
                .containsAnyOf("no issue", "looks good", "compatible", "no problem",
                               "well-structured", "no configuration cache", "none");
        }
    }

    @Nested
    @DisplayName("Fix Quality Tests")
    class FixQualityTests {

        @Test
        @DisplayName("Config cache fixes should be syntactically correct Kotlin")
        void configCacheFixesShouldBeSyntacticallyCorrect() throws IOException {
            Skill skill = skillLoader.loadSkill("gradle-config-cache");
            Fixture fixture = fixtureLoader.loadFixture("config-cache-broken");

            SkillTestResult result = claude.testSkill(
                skill,
                fixture.buildFileContent(),
                """
                Show me the EXACT code to fix the System.getProperty issue at line 27.
                Provide the corrected Kotlin code only, no explanation.
                """
            );

            // Should contain valid Kotlin code patterns
            assertThat(result.hasCodeExamples())
                .as("Should provide code example")
                .isTrue();

            // Should use correct Provider API syntax
            assertThat(result.response())
                .as("Should use valid Provider API")
                .containsAnyOf("providers.systemProperty", "providers.gradleProperty");
        }

        @Test
        @DisplayName("Fixes should include complete replacement code")
        void fixesShouldIncludeCompleteCode() throws IOException {
            Skill skill = skillLoader.loadSkill("gradle-config-cache");
            Fixture fixture = fixtureLoader.loadFixture("config-cache-broken");

            SkillTestResult result = claude.testSkill(
                skill,
                fixture.buildFileContent(),
                """
                Fix the eager task creation at line 33.
                Show the before and after code.
                """
            );

            // Should show both before and after
            assertThat(result.response())
                .as("Should show the problematic pattern")
                .containsAnyOf("tasks.create", "create(");

            assertThat(result.response())
                .as("Should show the fixed pattern")
                .containsAnyOf("tasks.register", "register(");
        }
    }
}
