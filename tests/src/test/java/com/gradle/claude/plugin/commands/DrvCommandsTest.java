package com.gradle.claude.plugin.commands;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.regex.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for DRV-based commands.
 * Validates structure, frontmatter, SQL queries, and MCP tool references.
 */
@Tag("skills")
@DisplayName("DRV Commands Tests")
class DrvCommandsTest {

    private static final Path PLUGIN_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path COMMANDS_DIR = PLUGIN_ROOT.resolve("commands");

    // =========================================================================
    // Parameterized Tests for Both Commands
    // =========================================================================

    @ParameterizedTest(name = "Command ''{0}'' should exist")
    @ValueSource(strings = {"analyze-cache-trends.md", "build-health-dashboard.md"})
    @DisplayName("DRV commands should exist")
    void drvCommandsShouldExist(String commandFile) {
        Path commandPath = COMMANDS_DIR.resolve(commandFile);
        assertThat(commandPath)
            .as("Command file %s should exist", commandFile)
            .exists()
            .isRegularFile();
    }

    @ParameterizedTest(name = "Command ''{0}'' should have valid frontmatter")
    @ValueSource(strings = {"analyze-cache-trends.md", "build-health-dashboard.md"})
    @DisplayName("DRV commands should have valid frontmatter")
    void drvCommandsShouldHaveValidFrontmatter(String commandFile) throws IOException {
        Map<String, Object> frontmatter = parseFrontmatter(commandFile);

        assertThat(frontmatter)
            .as("Frontmatter should not be empty")
            .isNotEmpty();

        assertThat(frontmatter)
            .as("Should have description")
            .containsKey("description");

        assertThat(frontmatter)
            .as("Should have allowed-tools")
            .containsKey("allowed-tools");
    }

    @ParameterizedTest(name = "Command ''{0}'' should allow DRV MCP tools")
    @ValueSource(strings = {"analyze-cache-trends.md", "build-health-dashboard.md"})
    @DisplayName("DRV commands should allow DRV MCP tools")
    void drvCommandsShouldAllowDrvMcpTools(String commandFile) throws IOException {
        Map<String, Object> frontmatter = parseFrontmatter(commandFile);
        String allowedTools = frontmatter.get("allowed-tools").toString();

        assertThat(allowedTools)
            .as("Should allow mcp__drv__execute_query")
            .contains("mcp__drv__execute_query");

        assertThat(allowedTools)
            .as("Should allow mcp__drv__describe_table")
            .contains("mcp__drv__describe_table");
    }

    @ParameterizedTest(name = "Command ''{0}'' should have SQL queries with partition filter")
    @ValueSource(strings = {"analyze-cache-trends.md", "build-health-dashboard.md"})
    @DisplayName("DRV commands SQL queries should use partition filter")
    void drvCommandsSqlQueriesShouldUsePartitionFilter(String commandFile) throws IOException {
        String content = Files.readString(COMMANDS_DIR.resolve(commandFile));

        Pattern sqlBlockPattern = Pattern.compile("```sql\\s*\\n(.*?)```", Pattern.DOTALL);
        Matcher matcher = sqlBlockPattern.matcher(content);

        int sqlBlockCount = 0;
        while (matcher.find()) {
            sqlBlockCount++;
            String sqlBlock = matcher.group(1);

            assertThat(sqlBlock)
                .as("SQL query should filter by build_start_date partition")
                .contains("build_start_date");
        }

        assertThat(sqlBlockCount)
            .as("Command should have at least one SQL query")
            .isGreaterThan(0);
    }

    @ParameterizedTest(name = "Command ''{0}'' should have error handling section")
    @ValueSource(strings = {"analyze-cache-trends.md", "build-health-dashboard.md"})
    @DisplayName("DRV commands should have error handling")
    void drvCommandsShouldHaveErrorHandling(String commandFile) throws IOException {
        String content = Files.readString(COMMANDS_DIR.resolve(commandFile)).toLowerCase();

        assertThat(content)
            .as("Should document what happens when DRV is unavailable")
            .containsAnyOf("error handling", "drv tools unavailable", "not configured", "if drv");
    }

    @ParameterizedTest(name = "Command ''{0}'' should document context gathering")
    @ValueSource(strings = {"analyze-cache-trends.md", "build-health-dashboard.md"})
    @DisplayName("DRV commands should document context gathering")
    void drvCommandsShouldDocumentContextGathering(String commandFile) throws IOException {
        String content = Files.readString(COMMANDS_DIR.resolve(commandFile)).toLowerCase();

        assertThat(content)
            .as("Should gather project context")
            .containsAnyOf("project", "rootproject");

        assertThat(content)
            .as("Should mention settings.gradle for project detection")
            .contains("settings.gradle");
    }

    // =========================================================================
    // Analyze Cache Trends Command Specific Tests
    // =========================================================================

    @Nested
    @DisplayName("Analyze Cache Trends Command")
    class AnalyzeCacheTrendsCommandTests {

        private static final String COMMAND_FILE = "analyze-cache-trends.md";
        private String content;
        private Map<String, Object> frontmatter;
        private String body;

        @BeforeEach
        void setUp() throws IOException {
            content = Files.readString(COMMANDS_DIR.resolve(COMMAND_FILE));
            String[] parts = content.split("---", 3);
            if (parts.length >= 3) {
                Yaml yaml = new Yaml();
                frontmatter = yaml.load(parts[1]);
                body = parts[2].trim();
            }
        }

        @Test
        @DisplayName("Description should mention cache trends")
        void descriptionShouldMentionCacheTrends() {
            String description = (String) frontmatter.get("description");

            assertThat(description.toLowerCase())
                .as("Description should mention cache")
                .contains("cache");

            assertThat(description.toLowerCase())
                .as("Description should mention trends")
                .containsAnyOf("trend", "over time", "performance");
        }

        @Test
        @DisplayName("Should query cache hit rate trend")
        void shouldQueryCacheHitRateTrend() {
            assertThat(body)
                .as("Should query cache avoidance metrics")
                .containsAnyOf("cache_avoidance", "hit_rate", "cache_hit");

            assertThat(body)
                .as("Should group by date for trends")
                .contains("GROUP BY")
                .contains("build_start_date");
        }

        @Test
        @DisplayName("Should query non-cacheable tasks")
        void shouldQueryNonCacheableTasks() {
            assertThat(body.toLowerCase())
                .as("Should analyze non-cacheable tasks")
                .containsAnyOf("non_cacheable", "not_cacheable", "executed_not_cacheable");

            assertThat(body)
                .as("Should use unit_execution_summary table")
                .contains("unit_execution_summary");
        }

        @Test
        @DisplayName("Should query cache configuration issues")
        void shouldQueryCacheConfigurationIssues() {
            assertThat(body)
                .as("Should check for cache disabled")
                .containsAnyOf("cache_enabled", "cache_disabled");

            assertThat(body)
                .as("Should check for cache errors")
                .containsAnyOf("disabled_due_to_error", "cache_error");
        }

        @Test
        @DisplayName("Should present formatted report")
        void shouldPresentFormattedReport() {
            assertThat(body)
                .as("Should have report header")
                .containsAnyOf("CACHE TREND", "BUILD CACHE");

            assertThat(body)
                .as("Should have recommendations section")
                .containsIgnoringCase("recommend");
        }

        @Test
        @DisplayName("Should reference related commands")
        void shouldReferenceRelatedCommands() {
            assertThat(body)
                .as("Should reference optimize-performance command")
                .contains("/optimize-performance");
        }
    }

    // =========================================================================
    // Build Health Dashboard Command Specific Tests
    // =========================================================================

    @Nested
    @DisplayName("Build Health Dashboard Command")
    class BuildHealthDashboardCommandTests {

        private static final String COMMAND_FILE = "build-health-dashboard.md";
        private String content;
        private Map<String, Object> frontmatter;
        private String body;

        @BeforeEach
        void setUp() throws IOException {
            content = Files.readString(COMMANDS_DIR.resolve(COMMAND_FILE));
            String[] parts = content.split("---", 3);
            if (parts.length >= 3) {
                Yaml yaml = new Yaml();
                frontmatter = yaml.load(parts[1]);
                body = parts[2].trim();
            }
        }

        @Test
        @DisplayName("Description should mention dashboard and health")
        void descriptionShouldMentionDashboardAndHealth() {
            String description = (String) frontmatter.get("description");

            assertThat(description.toLowerCase())
                .as("Description should mention dashboard or health")
                .containsAnyOf("dashboard", "health", "metrics");
        }

        @Test
        @DisplayName("Should query build overview metrics")
        void shouldQueryBuildOverviewMetrics() {
            assertThat(body)
                .as("Should query build counts")
                .contains("COUNT(*)");

            assertThat(body)
                .as("Should query failure rate")
                .containsAnyOf("has_failed", "failure_rate");

            assertThat(body)
                .as("Should query by environment")
                .contains("environment");
        }

        @Test
        @DisplayName("Should query cache metrics")
        void shouldQueryCacheMetrics() {
            assertThat(body)
                .as("Should query cache avoidance")
                .containsAnyOf("cache_avoidance", "cache_hit");

            assertThat(body)
                .as("Should query configuration cache")
                .containsAnyOf("config_cache", "configuration_cache");
        }

        @Test
        @DisplayName("Should query test metrics")
        void shouldQueryTestMetrics() {
            assertThat(body)
                .as("Should query test class count")
                .containsAnyOf("test_classes_count", "tests");

            assertThat(body)
                .as("Should query PTS status")
                .containsAnyOf("pts", "predictive_test_selection", "test_acceleration");
        }

        @Test
        @DisplayName("Should query dependency download metrics")
        void shouldQueryDependencyDownloadMetrics() {
            assertThat(body)
                .as("Should query file download count")
                .contains("file_download");

            assertThat(body)
                .as("Should query network time")
                .containsAnyOf("network", "download");
        }

        @Test
        @DisplayName("Should query settings issues")
        void shouldQuerySettingsIssues() {
            assertThat(body)
                .as("Should check parallel setting")
                .contains("parallel");

            assertThat(body)
                .as("Should check daemon setting")
                .contains("daemon");
        }

        @Test
        @DisplayName("Should have health score calculation")
        void shouldHaveHealthScoreCalculation() {
            assertThat(body.toLowerCase())
                .as("Should calculate health score")
                .containsAnyOf("health score", "score");

            assertThat(body)
                .as("Should document scoring criteria")
                .containsAnyOf("Weight", "weight", "Scoring", "scoring");
        }

        @Test
        @DisplayName("Should present formatted dashboard")
        void shouldPresentFormattedDashboard() {
            assertThat(body)
                .as("Should have dashboard header")
                .containsAnyOf("DASHBOARD", "Dashboard");

            assertThat(body)
                .as("Should have sections for different metrics")
                .contains("BUILD SUCCESS")
                .contains("BUILD PERFORMANCE");
        }

        @Test
        @DisplayName("Should reference related commands")
        void shouldReferenceRelatedCommands() {
            assertThat(body)
                .as("Should reference /doctor command")
                .contains("/doctor");
        }
    }

    // =========================================================================
    // SQL Query Validation Tests
    // =========================================================================

    @Nested
    @DisplayName("SQL Query Validation")
    class SqlQueryValidationTests {

        @ParameterizedTest(name = "SQL in ''{0}'' should have valid SELECT structure")
        @ValueSource(strings = {"analyze-cache-trends.md", "build-health-dashboard.md"})
        @DisplayName("SQL queries should have valid structure")
        void sqlQueriesShouldHaveValidStructure(String commandFile) throws IOException {
            String content = Files.readString(COMMANDS_DIR.resolve(commandFile));

            Pattern sqlBlockPattern = Pattern.compile("```sql\\s*\\n(.*?)```", Pattern.DOTALL);
            Matcher matcher = sqlBlockPattern.matcher(content);

            while (matcher.find()) {
                String sqlBlock = matcher.group(1).trim().toUpperCase();

                assertThat(sqlBlock)
                    .as("SQL should start with SELECT")
                    .startsWith("SELECT");

                assertThat(sqlBlock)
                    .as("SQL should have FROM clause")
                    .contains("FROM");

                assertThat(sqlBlock)
                    .as("SQL should have WHERE clause")
                    .contains("WHERE");

                // Validate table references
                assertThat(sqlBlock)
                    .as("SQL should reference valid DRV tables")
                    .containsAnyOf("BUILD_SUMMARY", "UNIT_EXECUTION_SUMMARY", "BUILD");
            }
        }

        @ParameterizedTest(name = "SQL in ''{0}'' should use INTERVAL for date filtering")
        @ValueSource(strings = {"analyze-cache-trends.md", "build-health-dashboard.md"})
        @DisplayName("SQL queries should use INTERVAL for date filtering")
        void sqlQueriesShouldUseIntervalForDateFiltering(String commandFile) throws IOException {
            String content = Files.readString(COMMANDS_DIR.resolve(commandFile));

            Pattern sqlBlockPattern = Pattern.compile("```sql\\s*\\n(.*?)```", Pattern.DOTALL);
            Matcher matcher = sqlBlockPattern.matcher(content);

            while (matcher.find()) {
                String sqlBlock = matcher.group(1);

                assertThat(sqlBlock.toUpperCase())
                    .as("SQL should use INTERVAL for relative date filtering")
                    .containsAnyOf("INTERVAL", "CURRENT_DATE");
            }
        }

        @ParameterizedTest(name = "SQL in ''{0}'' should have proper column references")
        @ValueSource(strings = {"analyze-cache-trends.md", "build-health-dashboard.md"})
        @DisplayName("SQL queries should use proper DRV column names")
        void sqlQueriesShouldUseProperColumnNames(String commandFile) throws IOException {
            String content = Files.readString(COMMANDS_DIR.resolve(commandFile));

            // Check for common correct column names
            assertThat(content)
                .as("Should use projectname (not project_name)")
                .doesNotContain("project_name =")
                .contains("projectname");

            assertThat(content)
                .as("Should use build_start_date for partition")
                .contains("build_start_date");
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private Map<String, Object> parseFrontmatter(String commandFile) throws IOException {
        String content = Files.readString(COMMANDS_DIR.resolve(commandFile));
        String[] parts = content.split("---", 3);
        if (parts.length >= 3) {
            Yaml yaml = new Yaml();
            return yaml.load(parts[1]);
        }
        return Map.of();
    }
}
