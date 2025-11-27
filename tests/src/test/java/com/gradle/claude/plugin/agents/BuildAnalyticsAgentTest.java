package com.gradle.claude.plugin.agents;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.regex.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for the build-analytics agent.
 * Validates structure, frontmatter, DRV MCP tool references, and content.
 */
@Tag("agents")
@DisplayName("Build Analytics Agent Tests")
class BuildAnalyticsAgentTest {

    private static final Path PLUGIN_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path AGENT_PATH = PLUGIN_ROOT.resolve("agents/build-analytics.md");

    private String content;
    private Map<String, Object> frontmatter;
    private String body;

    @BeforeEach
    void setUp() throws IOException {
        content = Files.readString(AGENT_PATH);
        String[] parts = content.split("---", 3);
        if (parts.length >= 3) {
            try {
                Yaml yaml = new Yaml();
                frontmatter = yaml.load(parts[1]);
            } catch (Exception e) {
                // If YAML parsing fails due to complex examples, manually extract key fields
                frontmatter = new java.util.HashMap<>();
                String fm = parts[1];

                // Extract name
                var nameMatcher = java.util.regex.Pattern.compile("^name:\\s*(.+)$", java.util.regex.Pattern.MULTILINE).matcher(fm);
                if (nameMatcher.find()) {
                    frontmatter.put("name", nameMatcher.group(1).trim());
                }

                // Extract description (multiline)
                int descStart = fm.indexOf("description:");
                int toolsStart = fm.indexOf("tools:");
                if (descStart >= 0 && toolsStart > descStart) {
                    frontmatter.put("description", fm.substring(descStart + 12, toolsStart).trim());
                }

                // Extract tools (supports MCP tool names with underscores)
                var toolsMatcher = java.util.regex.Pattern.compile("tools:\\s*\\n((?:\\s+-\\s+[\\w_]+\\n?)+)", java.util.regex.Pattern.MULTILINE).matcher(fm);
                if (toolsMatcher.find()) {
                    String toolsBlock = toolsMatcher.group(1);
                    java.util.List<String> tools = new java.util.ArrayList<>();
                    var toolMatcher = java.util.regex.Pattern.compile("-\\s+([\\w_]+)").matcher(toolsBlock);
                    while (toolMatcher.find()) {
                        tools.add(toolMatcher.group(1));
                    }
                    frontmatter.put("tools", tools);
                }

                // Extract model
                var modelMatcher = java.util.regex.Pattern.compile("^model:\\s*(\\w+)$", java.util.regex.Pattern.MULTILINE).matcher(fm);
                if (modelMatcher.find()) {
                    frontmatter.put("model", modelMatcher.group(1).trim());
                }
            }
            body = parts[2].trim();
        }
    }

    // =========================================================================
    // Structure Tests
    // =========================================================================

    @Test
    @DisplayName("Agent file should exist")
    void agentFileShouldExist() {
        assertThat(AGENT_PATH)
            .as("Agent file should exist")
            .exists()
            .isRegularFile();
    }

    @Test
    @DisplayName("Agent should have valid frontmatter")
    void agentShouldHaveValidFrontmatter() {
        assertThat(frontmatter)
            .as("Frontmatter should not be empty")
            .isNotEmpty();

        assertThat(frontmatter)
            .as("Should have name")
            .containsKey("name");

        assertThat(frontmatter)
            .as("Should have description")
            .containsKey("description");

        assertThat(frontmatter)
            .as("Should have tools")
            .containsKey("tools");
    }

    @Test
    @DisplayName("Agent name should be correct")
    void agentNameShouldBeCorrect() {
        String name = (String) frontmatter.get("name");
        assertThat(name).isEqualTo("gradle-build-analytics-agent");
    }

    // =========================================================================
    // Description Tests
    // =========================================================================

    @Test
    @DisplayName("Description should have examples")
    void descriptionShouldHaveExamples() {
        String description = (String) frontmatter.get("description");

        assertThat(description)
            .as("Should have example blocks")
            .contains("<example>")
            .contains("</example>");
    }

    @Test
    @DisplayName("Description should mention aggregate analytics")
    void descriptionShouldMentionAggregateAnalytics() {
        String description = (String) frontmatter.get("description");

        assertThat(description.toLowerCase())
            .as("Should mention aggregate or trend analysis")
            .containsAnyOf("aggregate", "trend", "across", "pattern");
    }

    // =========================================================================
    // Tools Tests
    // =========================================================================

    @Test
    @DisplayName("Agent should have DRV MCP tools")
    @SuppressWarnings("unchecked")
    void agentShouldHaveDrvMcpTools() {
        List<String> tools = (List<String>) frontmatter.get("tools");

        assertThat(tools)
            .as("Should have mcp__drv__execute_query")
            .contains("mcp__drv__execute_query");

        assertThat(tools)
            .as("Should have mcp__drv__describe_table")
            .contains("mcp__drv__describe_table");

        assertThat(tools)
            .as("Should have mcp__drv__list_tables")
            .contains("mcp__drv__list_tables");
    }

    @Test
    @DisplayName("Agent should have basic tools")
    @SuppressWarnings("unchecked")
    void agentShouldHaveBasicTools() {
        List<String> tools = (List<String>) frontmatter.get("tools");

        assertThat(tools)
            .as("Should have Read tool")
            .contains("Read");

        assertThat(tools)
            .as("Should have Bash tool")
            .contains("Bash");
    }

    // =========================================================================
    // Content Tests
    // =========================================================================

    @Test
    @DisplayName("Agent body should document key tables")
    void agentBodyShouldDocumentKeyTables() {
        assertThat(body)
            .as("Should mention build_summary table")
            .contains("build_summary");

        assertThat(body)
            .as("Should mention unit_execution_summary table")
            .contains("unit_execution_summary");
    }

    @Test
    @DisplayName("Agent body should have SQL queries with partition filter")
    void agentBodyShouldHaveSqlQueriesWithPartitionFilter() {
        Pattern sqlBlockPattern = Pattern.compile("```sql\\s*\\n(.*?)```", Pattern.DOTALL);
        Matcher matcher = sqlBlockPattern.matcher(body);

        int sqlBlockCount = 0;
        while (matcher.find()) {
            sqlBlockCount++;
            String sqlBlock = matcher.group(1);

            assertThat(sqlBlock)
                .as("SQL query should filter by build_start_date partition")
                .contains("build_start_date");
        }

        assertThat(sqlBlockCount)
            .as("Agent should have at least one SQL query")
            .isGreaterThan(0);
    }

    @Test
    @DisplayName("Agent body should differentiate from individual build queries")
    void agentBodyShouldDifferentiateFromIndividualBuildQueries() {
        assertThat(body)
            .as("Should mention when to use this agent vs Develocity MCP")
            .containsAnyOf("individual build", "mcp__develocity__");
    }

    @Test
    @DisplayName("Agent body should have error handling section")
    void agentBodyShouldHaveErrorHandlingSection() {
        assertThat(body.toLowerCase())
            .as("Should document error handling")
            .containsAnyOf("error handling", "drv tools unavailable", "not configured");
    }

    @Test
    @DisplayName("Agent body should have output format section")
    void agentBodyShouldHaveOutputFormatSection() {
        assertThat(body)
            .as("Should have output format section")
            .containsIgnoringCase("output format");

        assertThat(body)
            .as("Should have report template")
            .containsAnyOf("ANALYTICS REPORT", "DASHBOARD", "KEY METRICS");
    }

    @Test
    @DisplayName("Agent body should reference related commands and skills")
    void agentBodyShouldReferenceRelatedCommandsAndSkills() {
        assertThat(body)
            .as("Should reference develocity-analytics skill")
            .contains("develocity-analytics");

        assertThat(body)
            .as("Should reference related commands")
            .containsAnyOf("/analyze-cache-trends", "/build-health-dashboard");
    }

    @Test
    @DisplayName("Agent body should have context gathering step")
    void agentBodyShouldHaveContextGatheringStep() {
        assertThat(body)
            .as("Should gather project context")
            .contains("settings.gradle");

        assertThat(body.toLowerCase())
            .as("Should mention time range")
            .containsAnyOf("time range", "days", "interval");
    }

    // =========================================================================
    // SQL Query Validation Tests
    // =========================================================================

    @Test
    @DisplayName("SQL queries should have valid structure")
    void sqlQueriesShouldHaveValidStructure() {
        Pattern sqlBlockPattern = Pattern.compile("```sql\\s*\\n(.*?)```", Pattern.DOTALL);
        Matcher matcher = sqlBlockPattern.matcher(body);

        int fullQueryCount = 0;
        while (matcher.find()) {
            String sqlBlock = matcher.group(1).trim().toUpperCase();

            // Skip partial SQL snippets (e.g., just WHERE clauses for examples)
            if (!sqlBlock.startsWith("SELECT")) {
                continue;
            }
            fullQueryCount++;

            assertThat(sqlBlock)
                .as("SQL should have FROM clause")
                .contains("FROM");

            assertThat(sqlBlock)
                .as("SQL should have WHERE clause")
                .contains("WHERE");

            assertThat(sqlBlock)
                .as("SQL should reference valid DRV tables")
                .containsAnyOf("BUILD_SUMMARY", "UNIT_EXECUTION_SUMMARY", "BUILD");
        }

        assertThat(fullQueryCount)
            .as("Agent should have at least one full SQL query")
            .isGreaterThan(0);
    }

    @Test
    @DisplayName("SQL queries should use correct column names")
    void sqlQueriesShouldUseCorrectColumnNames() {
        assertThat(body)
            .as("Should use projectname (not project_name)")
            .doesNotContain("project_name =")
            .contains("projectname");

        assertThat(body)
            .as("Should use build_start_date for partition")
            .contains("build_start_date");
    }
}
