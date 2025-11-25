package com.gradle.claude.plugin.commands;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for the /migrate command structure and content.
 */
@Tag("skills")
@DisplayName("Migrate Command Tests")
class MigrateCommandTest {

    private static final Path PLUGIN_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path COMMAND_PATH = PLUGIN_ROOT.resolve("commands/migrate.md");
    private static String commandContent;
    private static Map<String, Object> frontmatter;
    private static String body;

    @BeforeAll
    static void setUp() throws IOException {
        assertThat(COMMAND_PATH)
            .as("migrate.md should exist")
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
    @DisplayName("Command should have correct argument hint")
    void commandShouldHaveCorrectArgumentHint() {
        String argumentHint = (String) frontmatter.get("argument-hint");

        assertThat(argumentHint)
            .as("Should include target-version argument")
            .contains("<target-version>");

        assertThat(argumentHint)
            .as("Should include --full flag")
            .contains("--full");

        assertThat(argumentHint)
            .as("Should include --dry-run flag")
            .contains("--dry-run");
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
            .as("Should allow essential tools")
            .contains("Read")
            .contains("Bash")
            .contains("Edit");
    }

    @Test
    @DisplayName("Command body should document arguments")
    void commandBodyShouldDocumentArguments() {
        assertThat(body)
            .as("Should document target-version")
            .containsIgnoringCase("target-version");

        assertThat(body)
            .as("Should document --full flag")
            .contains("--full");

        assertThat(body)
            .as("Should document --dry-run flag")
            .contains("--dry-run");

        assertThat(body)
            .as("Should document --no-verify flag")
            .contains("--no-verify");
    }

    @Test
    @DisplayName("Command body should document workflow steps")
    void commandBodyShouldDocumentWorkflowSteps() {
        assertThat(body)
            .as("Should document analyze step")
            .containsIgnoringCase("analyze");

        assertThat(body)
            .as("Should document plan step")
            .containsAnyOf("Plan", "plan", "Strategy");

        assertThat(body)
            .as("Should document execute step")
            .containsAnyOf("Execute", "execute", "Run");

        assertThat(body)
            .as("Should document verify step")
            .containsIgnoringCase("verify");
    }

    @Test
    @DisplayName("Command should reference openrewrite_runner tool")
    void commandShouldReferenceOpenRewriteRunner() {
        assertThat(body)
            .as("Should reference jbang")
            .contains("jbang");

        assertThat(body)
            .as("Should reference openrewrite_runner")
            .contains("openrewrite_runner");
    }

    @Test
    @DisplayName("Command should document strategy selection")
    void commandShouldDocumentStrategySelection() {
        assertThat(body)
            .as("Should document strategy selection based on project size")
            .containsAnyOf("Strategy", "strategy", "modules");
    }

    @Test
    @DisplayName("Command should document --full migration mode")
    void commandShouldDocumentFullMode() {
        assertThat(body)
            .as("Should document Kotlin DSL migration")
            .containsIgnoringCase("kotlin");

        assertThat(body)
            .as("Should document plugins block migration")
            .containsIgnoringCase("plugin");

        assertThat(body)
            .as("Should document version catalog migration")
            .containsIgnoringCase("version catalog");
    }

    @Test
    @DisplayName("Command should document error recovery")
    void commandShouldDocumentErrorRecovery() {
        assertThat(body)
            .as("Should document error recovery")
            .containsIgnoringCase("error");

        assertThat(body)
            .as("Should document rollback option")
            .containsIgnoringCase("rollback");
    }

    @Test
    @DisplayName("Command should show example usage")
    void commandShouldShowExampleUsage() {
        assertThat(body)
            .as("Should show basic usage example")
            .contains("/migrate");

        assertThat(body)
            .as("Should show version number in example")
            .containsPattern("/migrate\\s+[89]\\.0");
    }

    @Test
    @DisplayName("Command should document git checkpoint")
    void commandShouldDocumentGitCheckpoint() {
        assertThat(body)
            .as("Should mention git checkpoint/stash")
            .containsAnyOf("checkpoint", "stash", "git");
    }

    @Test
    @DisplayName("Command should document related commands")
    void commandShouldDocumentRelatedCommands() {
        assertThat(body)
            .as("Should reference /openrewrite command")
            .contains("/openrewrite");

        assertThat(body)
            .as("Should reference /doctor or /fix-config-cache")
            .containsAnyOf("/doctor", "/fix-config-cache");
    }
}
