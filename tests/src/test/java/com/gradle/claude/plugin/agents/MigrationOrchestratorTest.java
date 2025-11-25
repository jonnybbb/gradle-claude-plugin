package com.gradle.claude.plugin.agents;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for the migration-orchestrator agent structure and content.
 */
@Tag("skills")
@DisplayName("Migration Orchestrator Agent Tests")
class MigrationOrchestratorTest {

    private static final Path PLUGIN_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path AGENT_PATH = PLUGIN_ROOT.resolve("agents/migration-orchestrator.md");
    private static String agentContent;
    private static Map<String, Object> frontmatter;
    private static String body;

    @BeforeAll
    static void setUp() throws IOException {
        assertThat(AGENT_PATH)
            .as("migration-orchestrator.md should exist")
            .exists();

        agentContent = Files.readString(AGENT_PATH);

        // Parse frontmatter - handle complex YAML with examples
        String[] parts = agentContent.split("---", 3);
        if (parts.length >= 3) {
            try {
                Yaml yaml = new Yaml();
                frontmatter = yaml.load(parts[1]);
            } catch (Exception e) {
                // If YAML parsing fails, manually extract key fields
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

                // Extract tools
                var toolsMatcher = java.util.regex.Pattern.compile("tools:\\s*\\n((?:\\s+-\\s+\\w+\\n?)+)", java.util.regex.Pattern.MULTILINE).matcher(fm);
                if (toolsMatcher.find()) {
                    String toolsBlock = toolsMatcher.group(1);
                    java.util.List<String> tools = new java.util.ArrayList<>();
                    var toolMatcher = java.util.regex.Pattern.compile("-\\s+(\\w+)").matcher(toolsBlock);
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
        } else {
            frontmatter = Map.of();
            body = agentContent;
        }
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
    @DisplayName("Agent should have correct name")
    void agentShouldHaveCorrectName() {
        assertThat(frontmatter.get("name"))
            .isEqualTo("migration-orchestrator-agent");
    }

    @Test
    @DisplayName("Agent should have required tools")
    void agentShouldHaveRequiredTools() {
        Object tools = frontmatter.get("tools");
        assertThat(tools).isInstanceOf(java.util.List.class);

        @SuppressWarnings("unchecked")
        java.util.List<String> toolList = (java.util.List<String>) tools;

        assertThat(toolList)
            .as("Agent should have essential tools")
            .contains("Read", "Glob", "Grep", "Bash", "Edit");
    }

    @Test
    @DisplayName("Agent description should contain examples")
    void agentDescriptionShouldContainExamples() {
        String description = (String) frontmatter.get("description");

        assertThat(description)
            .as("Description should contain <example> blocks")
            .contains("<example>");

        // Count example blocks
        int exampleCount = description.split("<example>").length - 1;
        assertThat(exampleCount)
            .as("Should have multiple examples")
            .isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Agent body should document phases")
    void agentBodyShouldDocumentPhases() {
        assertThat(body)
            .as("Should document Phase 1: Project Analysis")
            .containsIgnoringCase("Phase 1");

        assertThat(body)
            .as("Should document Phase 2: Issue Detection")
            .containsIgnoringCase("Phase 2");

        assertThat(body)
            .as("Should document execution phase")
            .containsIgnoringCase("Execution");

        assertThat(body)
            .as("Should document verification phase")
            .containsIgnoringCase("Verification");
    }

    @Test
    @DisplayName("Agent should document complexity assessment")
    void agentShouldDocumentComplexityAssessment() {
        assertThat(body)
            .as("Should document complexity levels")
            .containsIgnoringCase("complexity")
            .containsAnyOf("LARGE", "modules", "build files", "Small", "Medium", "Large");
    }

    @Test
    @DisplayName("Agent should document engine selection logic")
    void agentShouldDocumentEngineSelectionLogic() {
        assertThat(body)
            .as("Should mention OpenRewrite engine")
            .containsIgnoringCase("OpenRewrite");

        assertThat(body)
            .as("Should mention Claude engine")
            .containsIgnoringCase("Claude");

        assertThat(body)
            .as("Should document engine selection criteria")
            .containsAnyOf("modules", "build_files", "build files");
    }

    @Test
    @DisplayName("Agent should document rollback strategy")
    void agentShouldDocumentRollbackStrategy() {
        assertThat(body)
            .as("Should document rollback capability")
            .containsIgnoringCase("rollback");

        assertThat(body)
            .as("Should document checkpoint/git stash")
            .containsAnyOf("checkpoint", "stash", "git");
    }

    @Test
    @DisplayName("Agent should document error handling")
    void agentShouldDocumentErrorHandling() {
        assertThat(body)
            .as("Should document error handling")
            .containsIgnoringCase("error");

        assertThat(body)
            .as("Should document failure scenarios")
            .containsAnyOf("failure", "failed", "Failure", "Failed");
    }

    @Test
    @DisplayName("Agent should reference openrewrite_runner tool")
    void agentShouldReferenceOpenRewriteRunner() {
        assertThat(body)
            .as("Should reference jbang openrewrite_runner")
            .contains("jbang")
            .contains("openrewrite_runner");
    }

    @Test
    @DisplayName("Agent should document migration plan output format")
    void agentShouldDocumentMigrationPlanOutput() {
        assertThat(body)
            .as("Should show migration plan table format")
            .contains("Step")
            .contains("Engine")
            .containsAnyOf("Est. Time", "Estimated", "Time");
    }

    @Test
    @DisplayName("Agent should document user interaction")
    void agentShouldDocumentUserInteraction() {
        assertThat(body)
            .as("Should mention user confirmation")
            .containsAnyOf("confirm", "Proceed", "Y/n", "[Y/n]");

        assertThat(body)
            .as("Should mention AskUserQuestion tool")
            .contains("AskUserQuestion");
    }

    @Test
    @DisplayName("Agent should have model specification")
    void agentShouldHaveModelSpec() {
        assertThat(frontmatter)
            .as("Should specify model")
            .containsKey("model");

        String model = (String) frontmatter.get("model");
        assertThat(model)
            .as("Model should be valid")
            .isIn("sonnet", "opus", "haiku");
    }

    @Test
    @DisplayName("Agent should document integration with other commands")
    void agentShouldDocumentIntegration() {
        assertThat(body)
            .as("Should reference /openrewrite command")
            .contains("/openrewrite");

        assertThat(body)
            .as("Should reference /fix-config-cache command")
            .containsAnyOf("/fix-config-cache", "fix-config-cache");
    }
}
