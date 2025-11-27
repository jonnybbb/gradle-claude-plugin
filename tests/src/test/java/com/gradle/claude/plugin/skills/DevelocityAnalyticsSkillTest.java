package com.gradle.claude.plugin.skills;

import com.gradle.claude.plugin.util.SkillLoader;
import com.gradle.claude.plugin.util.SkillLoader.Skill;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.regex.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for the develocity-analytics skill.
 * Validates structure, content, SQL patterns, and MCP tool references.
 */
@Tag("skills")
@DisplayName("Develocity Analytics Skill Tests")
class DevelocityAnalyticsSkillTest {

    private static final Path PLUGIN_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path SKILL_DIR = PLUGIN_ROOT.resolve("skills/develocity-analytics");
    private static SkillLoader skillLoader;
    private static Skill skill;
    private static Map<String, String> references;

    @BeforeAll
    static void setUp() throws IOException {
        skillLoader = new SkillLoader(PLUGIN_ROOT);
        skill = skillLoader.loadSkill("develocity-analytics");
        references = skillLoader.loadReferences("develocity-analytics");
    }

    // =========================================================================
    // Structure Tests
    // =========================================================================

    @Test
    @DisplayName("Skill directory should exist with required files")
    void skillDirectoryShouldExistWithRequiredFiles() {
        assertThat(SKILL_DIR).exists().isDirectory();
        assertThat(SKILL_DIR.resolve("SKILL.md")).exists().isRegularFile();
        assertThat(SKILL_DIR.resolve("references")).exists().isDirectory();
    }

    @Test
    @DisplayName("Skill should have required reference files")
    void skillShouldHaveRequiredReferenceFiles() {
        assertThat(references)
            .as("Should have sql-patterns.md")
            .containsKey("sql-patterns.md");

        assertThat(references)
            .as("Should have dashboards.md")
            .containsKey("dashboards.md");
    }

    @Test
    @DisplayName("Skill name should match directory")
    void skillNameShouldMatchDirectory() {
        assertThat(skill.name()).isEqualTo("develocity-analytics");
        assertThat(skill.directoryName()).isEqualTo("develocity-analytics");
    }

    // =========================================================================
    // Description Tests
    // =========================================================================

    @Test
    @DisplayName("Skill should have comprehensive trigger phrases")
    void skillShouldHaveComprehensiveTriggerPhrases() {
        List<String> triggers = skill.getTriggerPhrases();

        assertThat(triggers)
            .as("Should have at least 5 trigger phrases")
            .hasSizeGreaterThanOrEqualTo(5);

        // Check for key trigger phrases
        String description = skill.description().toLowerCase();
        assertThat(description)
            .as("Should trigger on 'build trends'")
            .contains("build trends");

        assertThat(description)
            .as("Should trigger on 'cache hit rate'")
            .contains("cache hit rate");

        assertThat(description)
            .as("Should trigger on 'optimization opportunities'")
            .contains("optimization opportunities");

        assertThat(description)
            .as("Should trigger on 'aggregate'")
            .contains("aggregate");
    }

    @Test
    @DisplayName("Description should differentiate from individual build queries")
    void descriptionShouldDifferentiateFromIndividualBuildQueries() {
        String description = skill.description().toLowerCase();

        assertThat(description)
            .as("Should mention analyzing patterns across builds")
            .containsAnyOf("across builds", "aggregate", "trends");
    }

    // =========================================================================
    // Content Tests
    // =========================================================================

    @Test
    @DisplayName("Skill body should reference DRV MCP tools")
    void skillBodyShouldReferenceDrvMcpTools() {
        String body = skill.body();

        assertThat(body)
            .as("Should reference mcp__drv__execute_query")
            .contains("mcp__drv__execute_query");

        assertThat(body)
            .as("Should reference mcp__drv__list_tables")
            .contains("mcp__drv__list_tables");

        assertThat(body)
            .as("Should reference mcp__drv__describe_table")
            .contains("mcp__drv__describe_table");
    }

    @Test
    @DisplayName("Skill body should have 'When to Use' decision table")
    void skillBodyShouldHaveWhenToUseDecisionTable() {
        String body = skill.body();

        assertThat(body)
            .as("Should have when to use section")
            .containsIgnoringCase("when to use");

        assertThat(body)
            .as("Should differentiate DRV from individual build queries")
            .contains("mcp__develocity__");
    }

    @Test
    @DisplayName("Skill body should document key tables")
    void skillBodyShouldDocumentKeyTables() {
        String body = skill.body();

        assertThat(body)
            .as("Should mention build_summary table")
            .contains("build_summary");

        assertThat(body)
            .as("Should mention partition filter requirement")
            .contains("build_start_date");
    }

    @Test
    @DisplayName("Skill body should have quick start SQL queries")
    void skillBodyShouldHaveQuickStartSqlQueries() {
        String body = skill.body();

        assertThat(body)
            .as("Should have SQL code blocks")
            .contains("```sql");

        assertThat(body)
            .as("Should have SELECT statements")
            .containsIgnoringCase("SELECT");

        assertThat(body)
            .as("Should have FROM build_summary")
            .contains("FROM build_summary");
    }

    @Test
    @DisplayName("Skill body should reference the reference files")
    void skillBodyShouldReferenceTheReferenceFiles() {
        String body = skill.body();

        assertThat(body)
            .as("Should reference sql-patterns.md")
            .contains("sql-patterns.md");

        assertThat(body)
            .as("Should reference dashboards.md")
            .contains("dashboards.md");
    }

    @Test
    @DisplayName("Skill body should have error handling section")
    void skillBodyShouldHaveErrorHandlingSection() {
        String body = skill.body().toLowerCase();

        assertThat(body)
            .as("Should document error handling")
            .containsAnyOf("error handling", "if drv tools unavailable", "not configured");
    }

    // =========================================================================
    // SQL Patterns Reference Tests
    // =========================================================================

    @Test
    @DisplayName("SQL patterns reference should have table of contents")
    void sqlPatternsReferenceShouldHaveTableOfContents() {
        String sqlPatterns = references.get("sql-patterns.md");

        assertThat(sqlPatterns)
            .as("Should have table of contents")
            .containsIgnoringCase("table of contents");
    }

    @Test
    @DisplayName("SQL patterns should cover key analysis areas")
    void sqlPatternsShouldCoverKeyAnalysisAreas() {
        String sqlPatterns = references.get("sql-patterns.md");

        assertThat(sqlPatterns)
            .as("Should cover build cache analysis")
            .containsIgnoringCase("build cache");

        assertThat(sqlPatterns)
            .as("Should cover configuration cache")
            .containsIgnoringCase("configuration cache");

        assertThat(sqlPatterns)
            .as("Should cover build performance")
            .containsIgnoringCase("build performance");

        assertThat(sqlPatterns)
            .as("Should cover task analysis")
            .containsIgnoringCase("task");

        assertThat(sqlPatterns)
            .as("Should cover test performance")
            .containsIgnoringCase("test");

        assertThat(sqlPatterns)
            .as("Should cover failure analysis")
            .containsIgnoringCase("failure");
    }

    @Test
    @DisplayName("SQL patterns should all use partition filter")
    void sqlPatternsShouldAllUsePartitionFilter() {
        String sqlPatterns = references.get("sql-patterns.md");

        // Count SQL blocks
        Pattern sqlBlockPattern = Pattern.compile("```sql\\s*\\n(.*?)```", Pattern.DOTALL);
        Matcher matcher = sqlBlockPattern.matcher(sqlPatterns);

        int sqlBlockCount = 0;
        int partitionFilterCount = 0;

        while (matcher.find()) {
            sqlBlockCount++;
            String sqlBlock = matcher.group(1);
            if (sqlBlock.contains("build_start_date")) {
                partitionFilterCount++;
            }
        }

        assertThat(sqlBlockCount)
            .as("Should have multiple SQL blocks")
            .isGreaterThan(5);

        assertThat(partitionFilterCount)
            .as("All SQL queries should filter by build_start_date partition")
            .isEqualTo(sqlBlockCount);
    }

    @Test
    @DisplayName("SQL patterns should have valid SQL syntax structure")
    void sqlPatternsShouldHaveValidSqlSyntaxStructure() {
        String sqlPatterns = references.get("sql-patterns.md");

        Pattern sqlBlockPattern = Pattern.compile("```sql\\s*\\n(.*?)```", Pattern.DOTALL);
        Matcher matcher = sqlBlockPattern.matcher(sqlPatterns);

        while (matcher.find()) {
            String sqlBlock = matcher.group(1).trim();

            // Basic SQL syntax checks
            assertThat(sqlBlock.toUpperCase())
                .as("SQL should start with SELECT")
                .startsWith("SELECT");

            assertThat(sqlBlock.toUpperCase())
                .as("SQL should have FROM clause")
                .contains("FROM");

            assertThat(sqlBlock.toUpperCase())
                .as("SQL should have WHERE clause")
                .contains("WHERE");
        }
    }

    @Test
    @DisplayName("SQL patterns should use correct column names")
    void sqlPatternsShouldUseCorrectColumnNames() {
        String sqlPatterns = references.get("sql-patterns.md");

        // These are known columns in build_summary
        assertThat(sqlPatterns)
            .as("Should use projectname column")
            .contains("projectname");

        assertThat(sqlPatterns)
            .as("Should use environment column")
            .contains("environment");

        assertThat(sqlPatterns)
            .as("Should use build_duration_millis or similar")
            .containsAnyOf("build_duration_millis", "build_duration");
    }

    // =========================================================================
    // Dashboards Reference Tests
    // =========================================================================

    @Test
    @DisplayName("Dashboards reference should have table of contents")
    void dashboardsReferenceShouldHaveTableOfContents() {
        String dashboards = references.get("dashboards.md");

        assertThat(dashboards)
            .as("Should have table of contents")
            .containsIgnoringCase("table of contents");
    }

    @Test
    @DisplayName("Dashboards reference should document key dashboard categories")
    void dashboardsReferenceShouldDocumentKeyDashboardCategories() {
        String dashboards = references.get("dashboards.md");

        assertThat(dashboards)
            .as("Should document build cache dashboards")
            .containsIgnoringCase("build cache");

        assertThat(dashboards)
            .as("Should document configuration cache dashboards")
            .containsIgnoringCase("configuration cache");

        assertThat(dashboards)
            .as("Should document test acceleration dashboards")
            .containsIgnoringCase("test acceleration");

        assertThat(dashboards)
            .as("Should document build settings dashboards")
            .containsIgnoringCase("build settings");
    }

    @Test
    @DisplayName("Dashboards reference should have dashboard IDs")
    void dashboardsReferenceShouldHaveDashboardIds() {
        String dashboards = references.get("dashboards.md");

        // Check for some known dashboard IDs
        assertThat(dashboards)
            .as("Should have gradle-build-cache-setting-gradle dashboard")
            .contains("gradle-build-cache-setting-gradle");

        assertThat(dashboards)
            .as("Should have build-failures dashboard")
            .contains("build-failures");

        assertThat(dashboards)
            .as("Should have realized-build-cache-savings dashboard")
            .contains("realized-build-cache-savings");
    }

    @Test
    @DisplayName("Dashboards reference should explain how to use fetch_query_by_panel")
    void dashboardsReferenceShouldExplainHowToUseFetchQueryByPanel() {
        String dashboards = references.get("dashboards.md");

        assertThat(dashboards)
            .as("Should reference fetch_query_by_panel")
            .contains("fetch_query_by_panel");
    }

    // =========================================================================
    // Word Count and Lean Content Tests
    // =========================================================================

    @Test
    @DisplayName("Skill body should be lean (under 1500 words)")
    void skillBodyShouldBeLean() {
        int wordCount = skill.getWordCount();

        assertThat(wordCount)
            .as("SKILL.md should be lean for progressive disclosure (under 1500 words)")
            .isLessThan(1500);

        assertThat(wordCount)
            .as("SKILL.md should have substantial content (at least 300 words)")
            .isGreaterThan(300);
    }

    @Test
    @DisplayName("Reference files should have substantial content")
    void referenceFilesShouldHaveSubstantialContent() {
        String sqlPatterns = references.get("sql-patterns.md");
        String dashboards = references.get("dashboards.md");

        int sqlWordCount = sqlPatterns.split("\\s+").length;
        int dashboardWordCount = dashboards.split("\\s+").length;

        assertThat(sqlWordCount)
            .as("SQL patterns should have substantial content")
            .isGreaterThan(500);

        assertThat(dashboardWordCount)
            .as("Dashboards reference should have substantial content")
            .isGreaterThan(200);
    }
}
