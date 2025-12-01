package com.gradle.claude.plugin.commands;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for the /upgrade command structure and content.
 */
@Tag("skills")
@DisplayName("Upgrade Command Tests")
class UpgradeCommandTest {

    private static final Path PLUGIN_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path COMMAND_PATH = PLUGIN_ROOT.resolve("commands/upgrade.md");
    private static String commandContent;
    private static Map<String, Object> frontmatter;
    private static String body;

    @BeforeAll
    static void setUp() throws IOException {
        assertThat(COMMAND_PATH)
            .as("upgrade.md should exist")
            .exists();

        commandContent = Files.readString(COMMAND_PATH);

        // Parse frontmatter
        String[] parts = commandContent.split("---", 3);
        if (parts.length >= 3) {
            Yaml yaml = new Yaml();
            frontmatter = yaml.load(parts[1]);
            body = parts[2].trim();
        } else {
            frontmatter = Map.of();
            body = commandContent;
        }
    }

    // =========================================================================
    // Frontmatter Tests
    // =========================================================================

    @Test
    @DisplayName("Command should have valid frontmatter")
    void commandShouldHaveValidFrontmatter() {
        assertThat(frontmatter)
            .as("Frontmatter should not be empty")
            .isNotEmpty();

        assertThat(frontmatter)
            .as("Should have description")
            .containsKey("description");

        assertThat(frontmatter)
            .as("Should have argument-hint")
            .containsKey("argument-hint");
    }

    @Test
    @DisplayName("Command should have meaningful description")
    void commandShouldHaveMeaningfulDescription() {
        String description = (String) frontmatter.get("description");

        assertThat(description)
            .as("Description should mention upgrade")
            .containsIgnoringCase("upgrade");

        assertThat(description)
            .as("Description should mention Gradle")
            .containsIgnoringCase("gradle");
    }

    @Test
    @DisplayName("Command should have correct argument hint")
    void commandShouldHaveCorrectArgumentHint() {
        String argumentHint = (String) frontmatter.get("argument-hint");

        assertThat(argumentHint)
            .as("Should include target-version argument")
            .contains("<target-version>");

        assertThat(argumentHint)
            .as("Should include --auto flag")
            .contains("--auto");

        assertThat(argumentHint)
            .as("Should include --dry-run flag")
            .contains("--dry-run");

        assertThat(argumentHint)
            .as("Should include --engine flag")
            .contains("--engine");
    }

    @Test
    @DisplayName("Command should have allowed tools")
    void commandShouldHaveAllowedTools() {
        Object tools = frontmatter.get("allowed-tools");
        assertThat(tools)
            .as("Should have allowed-tools")
            .isNotNull();

        String toolsStr = tools.toString();
        assertThat(toolsStr)
            .as("Should allow Read tool")
            .contains("Read");

        assertThat(toolsStr)
            .as("Should allow Edit tool")
            .contains("Edit");

        assertThat(toolsStr)
            .as("Should allow Bash tool")
            .contains("Bash");

        assertThat(toolsStr)
            .as("Should allow Glob tool")
            .contains("Glob");
    }

    // =========================================================================
    // Argument Documentation Tests
    // =========================================================================

    @Test
    @DisplayName("Command body should document all arguments")
    void commandBodyShouldDocumentArguments() {
        assertThat(body)
            .as("Should document target-version")
            .containsIgnoringCase("target-version");

        assertThat(body)
            .as("Should document --auto flag")
            .contains("--auto");

        assertThat(body)
            .as("Should document --dry-run flag")
            .contains("--dry-run");
    }

    @Test
    @DisplayName("Command should document engine selection")
    void commandShouldDocumentEngineSelection() {
        assertThat(body)
            .as("Should document --engine=openrewrite option")
            .contains("--engine=openrewrite");

        assertThat(body)
            .as("Should document OpenRewrite mode")
            .containsIgnoringCase("openrewrite");
    }

    @Test
    @DisplayName("Command should document OpenRewrite workflow for large projects")
    void commandShouldDocumentOpenRewriteWorkflow() {
        assertThat(body)
            .as("Should document when to use OpenRewrite")
            .containsAnyOf("large project", "bulk transformation", "Recommended workflow");

        assertThat(body)
            .as("Should show OpenRewrite recipe usage")
            .contains("MigrateToGradle");

        assertThat(body)
            .as("Should document combined Claude and OpenRewrite workflow")
            .containsAnyOf("Claude", "edge case", "remaining issue");
    }

    // =========================================================================
    // Workflow Documentation Tests
    // =========================================================================

    @Test
    @DisplayName("Command body should document workflow steps")
    void commandBodyShouldDocumentWorkflowSteps() {
        assertThat(body)
            .as("Should document detect/analyze step")
            .containsAnyOf("Detect", "detect", "Analyze", "analyze");

        assertThat(body)
            .as("Should document migration summary step")
            .containsAnyOf("Summary", "summary", "Plan");

        assertThat(body)
            .as("Should document apply step")
            .containsAnyOf("Apply", "apply", "Fix");

        assertThat(body)
            .as("Should document verification step")
            .containsAnyOf("Verification", "verification", "Verify", "verify");
    }

    @Test
    @DisplayName("Command should document wrapper update")
    void commandShouldDocumentWrapperUpdate() {
        assertThat(body)
            .as("Should document wrapper update")
            .containsIgnoringCase("wrapper");

        assertThat(body)
            .as("Should include wrapper update command")
            .contains("--gradle-version");
    }

    // =========================================================================
    // Migration Pattern Tests
    // =========================================================================

    @Test
    @DisplayName("Command should document Gradle 7 to 8 migration patterns")
    void commandShouldDocumentGradle7To8Patterns() {
        assertThat(body)
            .as("Should document archivesBaseName deprecation")
            .containsIgnoringCase("archivesbasename");

        assertThat(body)
            .as("Should document buildDir replacement")
            .containsAnyOf("$buildDir", "buildDir", "layout.buildDirectory");
    }

    @Test
    @DisplayName("Command should document Gradle 8 to 9 migration patterns")
    void commandShouldDocumentGradle8To9Patterns() {
        assertThat(body)
            .as("Should document sourceCompatibility migration")
            .containsIgnoringCase("sourcecompatibility");

        assertThat(body)
            .as("Should document convention deprecation")
            .containsIgnoringCase("convention");
    }

    @Test
    @DisplayName("Command should document task avoidance API")
    void commandShouldDocumentTaskAvoidanceApi() {
        assertThat(body)
            .as("Should document tasks.register")
            .contains("tasks.register");

        assertThat(body)
            .as("Should mention task avoidance or lazy registration")
            .containsAnyOf("task avoidance", "lazy", "register");
    }

    // =========================================================================
    // Tool Reference Tests
    // =========================================================================

    @Test
    @DisplayName("Command should reference migration-fixer tool")
    void commandShouldReferenceMigrationFixerTool() {
        assertThat(body)
            .as("Should reference jbang")
            .contains("jbang");

        assertThat(body)
            .as("Should reference migration-fixer tool")
            .contains("migration-fixer");
    }

    @Test
    @DisplayName("Command should use CLAUDE_PLUGIN_ROOT for tool paths")
    void commandShouldUseClaudePluginRoot() {
        assertThat(body)
            .as("Should use ${CLAUDE_PLUGIN_ROOT} for tool paths")
            .contains("${CLAUDE_PLUGIN_ROOT}");
    }

    // =========================================================================
    // Example Usage Tests
    // =========================================================================

    @Test
    @DisplayName("Command should show example usage")
    void commandShouldShowExampleUsage() {
        assertThat(body)
            .as("Should show /upgrade command in examples")
            .contains("/upgrade");

        assertThat(body)
            .as("Should show version number in example")
            .containsPattern("/upgrade\\s+\\d+\\.\\d+");
    }

    @Test
    @DisplayName("Command should show dry-run example")
    void commandShouldShowDryRunExample() {
        assertThat(body)
            .as("Should show --dry-run example")
            .containsPattern("/upgrade.*--dry-run");
    }

    @Test
    @DisplayName("Command should show auto mode example")
    void commandShouldShowAutoModeExample() {
        assertThat(body)
            .as("Should show --auto example")
            .containsPattern("/upgrade.*--auto");
    }

    // =========================================================================
    // Related Commands Tests
    // =========================================================================

    @Test
    @DisplayName("Command should document related commands")
    void commandShouldDocumentRelatedCommands() {
        assertThat(body)
            .as("Should reference /fix-config-cache command")
            .contains("/fix-config-cache");

        assertThat(body)
            .as("Should reference /doctor command")
            .contains("/doctor");
    }

    @Test
    @DisplayName("Command should reference optimize-performance")
    void commandShouldReferenceOptimizePerformance() {
        assertThat(body)
            .as("Should reference optimize-performance command")
            .contains("/optimize-performance");
    }

    // =========================================================================
    // Output Format Tests
    // =========================================================================

    @Test
    @DisplayName("Command should document migration summary format")
    void commandShouldDocumentMigrationSummaryFormat() {
        assertThat(body)
            .as("Should show migration plan format")
            .containsIgnoringCase("migration");

        assertThat(body)
            .as("Should show current/target version")
            .containsAnyOf("Current Version", "Target Version", "current", "target");
    }

    @Test
    @DisplayName("Command should document confidence levels")
    void commandShouldDocumentConfidenceLevels() {
        assertThat(body)
            .as("Should document HIGH confidence fixes")
            .containsIgnoringCase("high");

        assertThat(body)
            .as("Should document MEDIUM confidence fixes")
            .containsIgnoringCase("medium");
    }

    @Test
    @DisplayName("Command should document final summary output")
    void commandShouldDocumentFinalSummary() {
        assertThat(body)
            .as("Should show completion message")
            .containsAnyOf("COMPLETE", "complete", "Success", "success");

        assertThat(body)
            .as("Should document next steps")
            .containsIgnoringCase("next steps");
    }
}
