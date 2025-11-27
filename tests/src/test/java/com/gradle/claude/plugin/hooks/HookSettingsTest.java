package com.gradle.claude.plugin.hooks;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for hook settings functionality.
 * Validates that hooks respect the .claude/gradle-plugin.local.md settings file.
 */
@Tag("hooks")
@DisplayName("Hook Settings Tests")
class HookSettingsTest {

    private static final Path PLUGIN_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path HOOKS_DIR = PLUGIN_ROOT.resolve("hooks");
    private static final Path SETTINGS_LIB = HOOKS_DIR.resolve("lib/settings.sh");

    @TempDir
    Path tempDir;

    // =========================================================================
    // File Structure Tests
    // =========================================================================

    @Test
    @DisplayName("Settings library should exist")
    void settingsLibraryShouldExist() {
        assertThat(Files.exists(SETTINGS_LIB))
            .as("settings.sh library should exist at hooks/lib/settings.sh")
            .isTrue();
    }

    @Test
    @DisplayName("Settings template should exist")
    void settingsTemplateShouldExist() {
        Path template = HOOKS_DIR.resolve("settings.template.md");
        assertThat(Files.exists(template))
            .as("settings.template.md should exist")
            .isTrue();
    }

    @Test
    @DisplayName("Hook scripts should source settings library")
    void hookScriptsShouldSourceSettingsLibrary() throws IOException {
        String[] hookScripts = {"session-start.sh", "post-edit-build-file.sh"};

        for (String script : hookScripts) {
            Path scriptPath = HOOKS_DIR.resolve(script);
            String content = Files.readString(scriptPath);

            assertThat(content)
                .as("Hook script %s should source settings.sh", script)
                .contains("source")
                .contains("settings.sh");

            assertThat(content)
                .as("Hook script %s should check if hook is enabled", script)
                .contains("is_hook_enabled");
        }
    }

    // =========================================================================
    // Settings Library Function Tests
    // =========================================================================

    @Test
    @DisplayName("is_hook_enabled should return true when no settings file exists")
    void isHookEnabledShouldReturnTrueWithNoSettingsFile() throws Exception {
        // No settings file in tempDir
        int exitCode = runIsHookEnabled("sessionStart", tempDir);

        assertThat(exitCode)
            .as("Hook should be enabled (exit 0) when no settings file exists")
            .isZero();
    }

    @Test
    @DisplayName("is_hook_enabled should return true when hooks.enabled is true")
    void isHookEnabledShouldReturnTrueWhenGlobalEnabled() throws Exception {
        createSettingsFile(tempDir, """
            ---
            hooks:
              enabled: true
            ---
            """);

        int exitCode = runIsHookEnabled("sessionStart", tempDir);

        assertThat(exitCode)
            .as("Hook should be enabled when hooks.enabled is true")
            .isZero();
    }

    @Test
    @DisplayName("is_hook_enabled should return false when hooks.enabled is false")
    void isHookEnabledShouldReturnFalseWhenGlobalDisabled() throws Exception {
        createSettingsFile(tempDir, """
            ---
            hooks:
              enabled: false
            ---
            """);

        int exitCode = runIsHookEnabled("sessionStart", tempDir);

        assertThat(exitCode)
            .as("Hook should be disabled when hooks.enabled is false")
            .isNotZero();
    }

    @ParameterizedTest(name = "Hook ''{0}'' should be disabled when set to false")
    @ValueSource(strings = {"sessionStart", "postToolUse"})
    @DisplayName("Individual hooks can be disabled")
    void individualHooksCanBeDisabled(String hookName) throws Exception {
        createSettingsFile(tempDir, String.format("""
            ---
            hooks:
              enabled: true
              %s: false
            ---
            """, hookName));

        int exitCode = runIsHookEnabled(hookName, tempDir);

        assertThat(exitCode)
            .as("Hook %s should be disabled when set to false", hookName)
            .isNotZero();
    }

    @ParameterizedTest(name = "Hook ''{0}'' should be enabled when set to true")
    @ValueSource(strings = {"sessionStart", "postToolUse"})
    @DisplayName("Individual hooks can be explicitly enabled")
    void individualHooksCanBeEnabled(String hookName) throws Exception {
        createSettingsFile(tempDir, String.format("""
            ---
            hooks:
              enabled: true
              %s: true
            ---
            """, hookName));

        int exitCode = runIsHookEnabled(hookName, tempDir);

        assertThat(exitCode)
            .as("Hook %s should be enabled when set to true", hookName)
            .isZero();
    }

    @Test
    @DisplayName("Global disable should override individual enable")
    void globalDisableShouldOverrideIndividualEnable() throws Exception {
        createSettingsFile(tempDir, """
            ---
            hooks:
              enabled: false
              sessionStart: true
              postToolUse: true
            ---
            """);

        int sessionStartExit = runIsHookEnabled("sessionStart", tempDir);
        int postToolUseExit = runIsHookEnabled("postToolUse", tempDir);

        assertThat(sessionStartExit)
            .as("sessionStart should be disabled when global is false")
            .isNotZero();
        assertThat(postToolUseExit)
            .as("postToolUse should be disabled when global is false")
            .isNotZero();
    }

    @Test
    @DisplayName("One hook disabled should not affect other hooks")
    void oneHookDisabledShouldNotAffectOthers() throws Exception {
        createSettingsFile(tempDir, """
            ---
            hooks:
              enabled: true
              sessionStart: false
              postToolUse: true
            ---
            """);

        int sessionStartExit = runIsHookEnabled("sessionStart", tempDir);
        int postToolUseExit = runIsHookEnabled("postToolUse", tempDir);

        assertThat(sessionStartExit)
            .as("sessionStart should be disabled")
            .isNotZero();
        assertThat(postToolUseExit)
            .as("postToolUse should remain enabled")
            .isZero();
    }

    // =========================================================================
    // Edge Case Tests
    // =========================================================================

    @Test
    @DisplayName("Should handle empty settings file gracefully")
    void shouldHandleEmptySettingsFile() throws Exception {
        createSettingsFile(tempDir, "");

        int exitCode = runIsHookEnabled("sessionStart", tempDir);

        assertThat(exitCode)
            .as("Hook should be enabled with empty settings file")
            .isZero();
    }

    @Test
    @DisplayName("Should handle settings file without hooks section")
    void shouldHandleSettingsFileWithoutHooksSection() throws Exception {
        createSettingsFile(tempDir, """
            ---
            someOtherSetting: value
            ---
            Some markdown content.
            """);

        int exitCode = runIsHookEnabled("sessionStart", tempDir);

        assertThat(exitCode)
            .as("Hook should be enabled when hooks section is missing")
            .isZero();
    }

    @Test
    @DisplayName("Should handle malformed YAML gracefully")
    void shouldHandleMalformedYamlGracefully() throws Exception {
        createSettingsFile(tempDir, """
            ---
            hooks:
            : broken yaml
              not valid
            ---
            """);

        // Should not crash, hooks should remain enabled by default
        int exitCode = runIsHookEnabled("sessionStart", tempDir);

        // When YAML is malformed, we default to enabled
        assertThat(exitCode)
            .as("Hook should be enabled (fail-open) with malformed YAML")
            .isZero();
    }

    @Test
    @DisplayName("Should handle settings file with only markdown content")
    void shouldHandleSettingsFileWithOnlyMarkdownContent() throws Exception {
        createSettingsFile(tempDir, """
            # Just some markdown

            No YAML frontmatter here.
            """);

        int exitCode = runIsHookEnabled("sessionStart", tempDir);

        assertThat(exitCode)
            .as("Hook should be enabled with no YAML frontmatter")
            .isZero();
    }

    @ParameterizedTest(name = "Value ''{0}'' for enabled should result in exit code {1}")
    @CsvSource({
        "true, 0",
        "false, 1",
        "TRUE, 0",
        "FALSE, 1",
        "yes, 0",
        "no, 0"  // 'no' is not 'false', so defaults to enabled
    })
    @DisplayName("Should handle various boolean representations")
    void shouldHandleVariousBooleanRepresentations(String value, int expectedExitCode) throws Exception {
        createSettingsFile(tempDir, String.format("""
            ---
            hooks:
              enabled: %s
            ---
            """, value));

        int exitCode = runIsHookEnabled("sessionStart", tempDir);

        assertThat(exitCode)
            .as("enabled: %s should result in exit code %d", value, expectedExitCode)
            .isEqualTo(expectedExitCode);
    }

    @Test
    @DisplayName("Should handle settings file with extra whitespace")
    void shouldHandleSettingsFileWithExtraWhitespace() throws Exception {
        createSettingsFile(tempDir, """
            ---
            hooks:
              enabled:   true
              sessionStart:  false
            ---
            """);

        int sessionStartExit = runIsHookEnabled("sessionStart", tempDir);

        assertThat(sessionStartExit)
            .as("Should parse correctly with extra whitespace")
            .isNotZero();
    }

    @Test
    @DisplayName("Should handle very long settings file")
    void shouldHandleVeryLongSettingsFile() throws Exception {
        StringBuilder longContent = new StringBuilder();
        longContent.append("---\nhooks:\n  enabled: true\n");
        // Add many hook settings
        for (int i = 0; i < 50; i++) {
            longContent.append(String.format("  customHook%d: true\n", i));
        }
        longContent.append("---\n");
        // Add lots of markdown content
        for (int i = 0; i < 100; i++) {
            longContent.append(String.format("## Section %d\nSome content here.\n\n", i));
        }

        createSettingsFile(tempDir, longContent.toString());

        int exitCode = runIsHookEnabled("sessionStart", tempDir);

        assertThat(exitCode)
            .as("Should handle long settings file")
            .isZero();
    }

    @Test
    @DisplayName("Should handle settings file with unicode content")
    void shouldHandleSettingsFileWithUnicode() throws Exception {
        createSettingsFile(tempDir, """
            ---
            hooks:
              enabled: true
              sessionStart: true
            ---
            # 日本語のコメント
            This file contains unicode characters: 中文, русский, العربية
            """);

        int exitCode = runIsHookEnabled("sessionStart", tempDir);

        assertThat(exitCode)
            .as("Should handle unicode in settings file")
            .isZero();
    }

    @Test
    @DisplayName("Should handle settings file with CRLF line endings")
    void shouldHandleSettingsFileWithCrlfLineEndings() throws Exception {
        String content = "---\r\nhooks:\r\n  enabled: true\r\n---\r\n";
        createSettingsFile(tempDir, content);

        int exitCode = runIsHookEnabled("sessionStart", tempDir);

        assertThat(exitCode)
            .as("Should handle CRLF line endings")
            .isZero();
    }

    @Test
    @DisplayName("Should handle deeply nested hook configuration")
    void shouldHandleDeeplyNestedConfig() throws Exception {
        createSettingsFile(tempDir, """
            ---
            hooks:
              enabled: true
              nested:
                deeply:
                  value: true
              sessionStart: true
            ---
            """);

        int exitCode = runIsHookEnabled("sessionStart", tempDir);

        assertThat(exitCode)
            .as("Should handle deeply nested configuration")
            .isZero();
    }

    @Test
    @DisplayName("Should handle multiple frontmatter delimiters in content")
    void shouldHandleMultipleFrontmatterDelimitersInContent() throws Exception {
        createSettingsFile(tempDir, """
            ---
            hooks:
              enabled: true
            ---
            Some content.

            Here's a code block:
            ```yaml
            ---
            another: yaml
            ---
            ```

            More content.
            """);

        int exitCode = runIsHookEnabled("sessionStart", tempDir);

        assertThat(exitCode)
            .as("Should handle --- in markdown content correctly")
            .isZero();
    }

    @Test
    @DisplayName("Should handle settings file with standalone YAML comments")
    void shouldHandleYamlComments() throws Exception {
        // Note: Shell-based YAML parsing has limitations with inline comments
        // This test uses standalone comment lines which are properly handled
        createSettingsFile(tempDir, """
            ---
            # This is a comment describing hooks
            hooks:
              enabled: true
              # Session start is disabled below
              sessionStart: false
            ---
            """);

        int sessionStartExit = runIsHookEnabled("sessionStart", tempDir);

        assertThat(sessionStartExit)
            .as("Should respect setting with standalone comments")
            .isNotZero();
    }

    @Test
    @DisplayName("Should handle hook names with different casings")
    void shouldHandleHookNamesWithDifferentCasings() throws Exception {
        // Test camelCase hook name
        createSettingsFile(tempDir, """
            ---
            hooks:
              enabled: true
              sessionStart: false
            ---
            """);

        int exitCode = runIsHookEnabled("sessionStart", tempDir);
        assertThat(exitCode)
            .as("Should handle camelCase hook name")
            .isNotZero();
    }

    @Test
    @DisplayName("Should handle settings file with special characters in values")
    void shouldHandleSpecialCharactersInValues() throws Exception {
        createSettingsFile(tempDir, """
            ---
            hooks:
              enabled: true
              customSetting: "value with spaces"
              anotherSetting: 'quoted value'
            ---
            """);

        int exitCode = runIsHookEnabled("sessionStart", tempDir);

        assertThat(exitCode)
            .as("Should handle special characters in YAML values")
            .isZero();
    }

    // =========================================================================
    // Integration Tests - Full Hook Scripts
    // =========================================================================

    @Test
    @DisplayName("session-start.sh should exit early when disabled")
    void sessionStartShouldExitEarlyWhenDisabled() throws Exception {
        createSettingsFile(tempDir, """
            ---
            hooks:
              sessionStart: false
            ---
            """);

        // Create minimal Gradle project structure
        createGradleProject(tempDir);

        String output = runHookScript("session-start.sh", tempDir);

        // When disabled, script should exit without producing output
        assertThat(output.trim())
            .as("session-start.sh should produce no output when disabled")
            .isEmpty();
    }

    @Test
    @DisplayName("session-start.sh should run normally when enabled")
    void sessionStartShouldRunNormallyWhenEnabled() throws Exception {
        createSettingsFile(tempDir, """
            ---
            hooks:
              sessionStart: true
            ---
            """);

        // Create minimal Gradle project structure
        createGradleProject(tempDir);

        String output = runHookScript("session-start.sh", tempDir);

        // When enabled in a Gradle project, should produce JSON output
        assertThat(output)
            .as("session-start.sh should produce JSON output when enabled")
            .contains("continue")
            .contains("true");
    }

    @Test
    @DisplayName("post-edit-build-file.sh should exit early when disabled")
    void postEditBuildFileShouldExitEarlyWhenDisabled() throws Exception {
        createSettingsFile(tempDir, """
            ---
            hooks:
              postToolUse: false
            ---
            """);

        String output = runPostEditHook("build.gradle", tempDir);

        // When disabled, should return silent success JSON
        assertThat(output)
            .as("post-edit-build-file.sh should return suppressOutput:true when disabled")
            .contains("suppressOutput")
            .contains("true");
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Creates the .claude/gradle-plugin.local.md settings file.
     */
    private void createSettingsFile(Path projectDir, String content) throws IOException {
        Path claudeDir = projectDir.resolve(".claude");
        Files.createDirectories(claudeDir);
        Files.writeString(claudeDir.resolve("gradle-plugin.local.md"), content);
    }

    /**
     * Creates a minimal Gradle project structure for hook testing.
     */
    private void createGradleProject(Path projectDir) throws IOException {
        // Create build.gradle
        Files.writeString(projectDir.resolve("build.gradle"), """
            plugins {
                id 'java'
            }
            """);

        // Create gradle.properties
        Files.writeString(projectDir.resolve("gradle.properties"), """
            org.gradle.parallel=true
            org.gradle.caching=true
            """);

        // Create wrapper properties directory
        Path wrapperDir = projectDir.resolve("gradle/wrapper");
        Files.createDirectories(wrapperDir);
        Files.writeString(wrapperDir.resolve("gradle-wrapper.properties"), """
            distributionUrl=https\\://services.gradle.org/distributions/gradle-8.5-bin.zip
            """);
    }

    /**
     * Runs the is_hook_enabled function from settings.sh and returns the exit code.
     */
    private int runIsHookEnabled(String hookName, Path projectDir) throws Exception {
        // Create a test script that sources settings.sh and calls is_hook_enabled
        String testScript = String.format("""
            #!/bin/bash
            export CLAUDE_PROJECT_DIR="%s"
            source "%s"
            if is_hook_enabled "%s"; then
                exit 0
            else
                exit 1
            fi
            """, projectDir.toAbsolutePath(), SETTINGS_LIB.toAbsolutePath(), hookName);

        return runBashScript(testScript, projectDir);
    }

    /**
     * Runs a hook script and returns its output.
     */
    private String runHookScript(String scriptName, Path projectDir) throws Exception {
        Path scriptPath = HOOKS_DIR.resolve(scriptName);

        ProcessBuilder pb = new ProcessBuilder("bash", scriptPath.toString());
        pb.environment().put("CLAUDE_PROJECT_DIR", projectDir.toAbsolutePath().toString());
        pb.environment().put("CLAUDE_PLUGIN_ROOT", PLUGIN_ROOT.toAbsolutePath().toString());
        pb.directory(projectDir.toFile());
        pb.redirectErrorStream(true);

        return executeAndCapture(pb);
    }

    /**
     * Runs the post-edit-build-file.sh hook with simulated input.
     */
    private String runPostEditHook(String filePath, Path projectDir) throws Exception {
        Path scriptPath = HOOKS_DIR.resolve("post-edit-build-file.sh");

        // Simulate hook input JSON
        String hookInput = String.format("""
            {"tool_input": {"file_path": "%s/%s"}}
            """, projectDir.toAbsolutePath(), filePath);

        ProcessBuilder pb = new ProcessBuilder("bash", scriptPath.toString());
        pb.environment().put("CLAUDE_PROJECT_DIR", projectDir.toAbsolutePath().toString());
        pb.environment().put("CLAUDE_PLUGIN_ROOT", PLUGIN_ROOT.toAbsolutePath().toString());
        pb.directory(projectDir.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Write input to stdin
        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write(hookInput.getBytes());
            stdin.flush();
        }

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        process.waitFor(10, TimeUnit.SECONDS);
        return output.toString();
    }

    /**
     * Runs a bash script and returns the exit code.
     */
    private int runBashScript(String script, Path workDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", script);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Consume output to prevent blocking
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) {
                // Discard output
            }
        }

        boolean completed = process.waitFor(10, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("Script execution timed out");
        }

        return process.exitValue();
    }

    /**
     * Executes a process and captures its output.
     */
    private String executeAndCapture(ProcessBuilder pb) throws Exception {
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean completed = process.waitFor(10, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("Process execution timed out");
        }

        return output.toString();
    }
}
