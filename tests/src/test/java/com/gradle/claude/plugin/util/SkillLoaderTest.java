package com.gradle.claude.plugin.util;

import com.gradle.claude.plugin.util.SkillLoader.Skill;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for SkillLoader utility class edge cases.
 * Validates parsing, error handling, and edge case behavior.
 */
@Tag("utils")
@DisplayName("SkillLoader Utility Tests")
class SkillLoaderTest {

    private static final Path PLUGIN_ROOT = Path.of("..").toAbsolutePath().normalize();

    @TempDir
    Path tempDir;

    // =========================================================================
    // Basic Loading Tests
    // =========================================================================

    @Test
    @DisplayName("Should load all skills from real plugin")
    void shouldLoadAllSkillsFromRealPlugin() throws IOException {
        SkillLoader loader = new SkillLoader(PLUGIN_ROOT);
        List<Skill> skills = loader.loadAllSkills();

        assertThat(skills)
            .as("Should load multiple skills")
            .isNotEmpty();
    }

    @Test
    @DisplayName("Should load a specific skill by name")
    void shouldLoadSpecificSkillByName() throws IOException {
        SkillLoader loader = new SkillLoader(PLUGIN_ROOT);
        Skill skill = loader.loadSkill("gradle-config-cache");

        assertThat(skill.name())
            .isEqualTo("gradle-config-cache");
        assertThat(skill.description())
            .isNotBlank();
        assertThat(skill.body())
            .isNotBlank();
    }

    @Test
    @DisplayName("Should throw exception for non-existent skill")
    void shouldThrowExceptionForNonExistentSkill() {
        SkillLoader loader = new SkillLoader(PLUGIN_ROOT);

        assertThatThrownBy(() -> loader.loadSkill("non-existent-skill"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    // =========================================================================
    // Frontmatter Parsing Tests
    // =========================================================================

    @Test
    @DisplayName("Should parse valid YAML frontmatter")
    void shouldParseValidYamlFrontmatter() throws IOException {
        createSkillDirectory("test-skill", """
            ---
            name: test-skill
            description: This skill should be used when testing parsing.
            ---
            # Test Skill

            This is the body content.
            """);

        SkillLoader loader = new SkillLoader(tempDir);
        Skill skill = loader.loadSkill("test-skill");

        assertThat(skill.name()).isEqualTo("test-skill");
        assertThat(skill.description()).isEqualTo("This skill should be used when testing parsing.");
        assertThat(skill.body()).contains("# Test Skill");
    }

    @Test
    @DisplayName("Should handle multiline descriptions")
    void shouldHandleMultilineDescriptions() throws IOException {
        createSkillDirectory("multi-desc", """
            ---
            name: multi-desc
            description: >
              This skill should be used when you have a very long description
              that spans multiple lines using YAML folded style.
            ---
            Content here.
            """);

        SkillLoader loader = new SkillLoader(tempDir);
        Skill skill = loader.loadSkill("multi-desc");

        assertThat(skill.description())
            .as("Should parse multiline description")
            .contains("very long description");
    }

    @Test
    @DisplayName("Should throw for missing frontmatter")
    void shouldThrowForMissingFrontmatter() throws IOException {
        createSkillDirectory("no-frontmatter", """
            # This skill has no frontmatter

            Just markdown content.
            """);

        SkillLoader loader = new SkillLoader(tempDir);

        assertThatThrownBy(() -> loader.loadSkill("no-frontmatter"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("frontmatter");
    }

    // =========================================================================
    // Reference Loading Tests
    // =========================================================================

    @Test
    @DisplayName("Should load references from skill directory")
    void shouldLoadReferencesFromSkillDirectory() throws IOException {
        SkillLoader loader = new SkillLoader(PLUGIN_ROOT);
        Map<String, String> refs = loader.loadReferences("gradle-config-cache");

        assertThat(refs)
            .as("Should load reference files")
            .isNotEmpty();
    }

    @Test
    @DisplayName("Should return empty map for skill without references")
    void shouldReturnEmptyMapForSkillWithoutReferences() throws IOException {
        createSkillDirectory("no-refs", """
            ---
            name: no-refs
            description: This skill should be used when testing.
            ---
            No references.
            """);

        SkillLoader loader = new SkillLoader(tempDir);
        Map<String, String> refs = loader.loadReferences("no-refs");

        assertThat(refs).isEmpty();
    }

    @Test
    @DisplayName("Should load multiple reference files")
    void shouldLoadMultipleReferenceFiles() throws IOException {
        Path skillDir = createSkillDirectory("multi-refs", """
            ---
            name: multi-refs
            description: This skill should be used when testing.
            ---
            Has multiple references.
            """);

        // Create references directory with multiple files
        Path refsDir = skillDir.resolve("references");
        Files.createDirectories(refsDir);
        Files.writeString(refsDir.resolve("ref1.md"), "# Reference 1");
        Files.writeString(refsDir.resolve("ref2.md"), "# Reference 2");
        Files.writeString(refsDir.resolve("not-markdown.txt"), "Should be ignored");

        SkillLoader loader = new SkillLoader(tempDir);
        Map<String, String> refs = loader.loadReferences("multi-refs");

        assertThat(refs)
            .as("Should load only .md files")
            .hasSize(2)
            .containsKeys("ref1.md", "ref2.md")
            .doesNotContainKey("not-markdown.txt");
    }

    // =========================================================================
    // Skill Record Method Tests
    // =========================================================================

    @Test
    @DisplayName("Should extract trigger phrases from description")
    void shouldExtractTriggerPhrasesFromDescription() throws IOException {
        createSkillDirectory("triggers", """
            ---
            name: triggers
            description: This skill should be used when user asks to "run tests", "execute build", or mentions "gradle daemon".
            ---
            Body content.
            """);

        SkillLoader loader = new SkillLoader(tempDir);
        Skill skill = loader.loadSkill("triggers");

        List<String> triggers = skill.getTriggerPhrases();
        assertThat(triggers)
            .as("Should extract quoted phrases")
            .containsExactlyInAnyOrder("run tests", "execute build", "gradle daemon");
    }

    @Test
    @DisplayName("Should return empty list when no trigger phrases")
    void shouldReturnEmptyListWhenNoTriggerPhrases() throws IOException {
        createSkillDirectory("no-triggers", """
            ---
            name: no-triggers
            description: This skill should be used when testing with no quoted phrases.
            ---
            Body.
            """);

        SkillLoader loader = new SkillLoader(tempDir);
        Skill skill = loader.loadSkill("no-triggers");

        List<String> triggers = skill.getTriggerPhrases();
        assertThat(triggers).isEmpty();
    }

    @Test
    @DisplayName("Should detect third-person description format")
    void shouldDetectThirdPersonDescriptionFormat() throws IOException {
        createSkillDirectory("third-person", """
            ---
            name: third-person
            description: This skill should be used when the user asks about testing.
            ---
            Body.
            """);

        createSkillDirectory("not-third-person", """
            ---
            name: not-third-person
            description: Use this for testing purposes.
            ---
            Body.
            """);

        SkillLoader loader = new SkillLoader(tempDir);

        Skill thirdPerson = loader.loadSkill("third-person");
        Skill notThirdPerson = loader.loadSkill("not-third-person");

        assertThat(thirdPerson.hasThirdPersonDescription()).isTrue();
        assertThat(notThirdPerson.hasThirdPersonDescription()).isFalse();
    }

    @Test
    @DisplayName("Should count words in body correctly")
    void shouldCountWordsInBodyCorrectly() throws IOException {
        createSkillDirectory("word-count", """
            ---
            name: word-count
            description: This skill should be used when testing word count.
            ---
            One two three four five.
            Six seven eight nine ten.
            """);

        SkillLoader loader = new SkillLoader(tempDir);
        Skill skill = loader.loadSkill("word-count");

        assertThat(skill.getWordCount())
            .as("Should count words (including newlines splits)")
            .isGreaterThanOrEqualTo(10);
    }

    // =========================================================================
    // Edge Case Tests
    // =========================================================================

    @Test
    @DisplayName("Should handle skill with unicode content")
    void shouldHandleSkillWithUnicodeContent() throws IOException {
        createSkillDirectory("unicode-skill", """
            ---
            name: unicode-skill
            description: This skill should be used when handling "日本語" or "中文" content.
            ---
            # Unicode Support

            - 日本語 (Japanese)
            - 中文 (Chinese)
            - русский (Russian)
            - العربية (Arabic)
            """);

        SkillLoader loader = new SkillLoader(tempDir);
        Skill skill = loader.loadSkill("unicode-skill");

        assertThat(skill.body()).contains("日本語", "中文", "русский");
        assertThat(skill.getTriggerPhrases()).contains("日本語", "中文");
    }

    @Test
    @DisplayName("Should handle skill with very long body")
    void shouldHandleSkillWithVeryLongBody() throws IOException {
        StringBuilder longBody = new StringBuilder();
        longBody.append("---\nname: long-body\ndescription: This skill should be used when testing.\n---\n");
        for (int i = 0; i < 500; i++) {
            longBody.append(String.format("## Section %d\n\nSome content for section %d.\n\n", i, i));
        }

        createSkillDirectoryRaw("long-body", longBody.toString());

        SkillLoader loader = new SkillLoader(tempDir);
        Skill skill = loader.loadSkill("long-body");

        assertThat(skill.getWordCount())
            .as("Should handle large content")
            .isGreaterThan(1000);
    }

    @Test
    @DisplayName("Should handle empty body after frontmatter")
    void shouldHandleEmptyBodyAfterFrontmatter() throws IOException {
        createSkillDirectory("empty-body", """
            ---
            name: empty-body
            description: This skill should be used when testing empty bodies.
            ---
            """);

        SkillLoader loader = new SkillLoader(tempDir);
        Skill skill = loader.loadSkill("empty-body");

        assertThat(skill.body().trim()).isEmpty();
        assertThat(skill.getWordCount()).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should handle special YAML characters in description")
    void shouldHandleSpecialYamlCharactersInDescription() throws IOException {
        createSkillDirectory("special-yaml", """
            ---
            name: special-yaml
            description: "This skill should be used when dealing with: colons, and special chars like & or *"
            ---
            Body content.
            """);

        SkillLoader loader = new SkillLoader(tempDir);
        Skill skill = loader.loadSkill("special-yaml");

        assertThat(skill.description())
            .contains("colons")
            .contains("&")
            .contains("*");
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private Path createSkillDirectory(String skillName, String content) throws IOException {
        Path skillDir = tempDir.resolve("skills").resolve(skillName);
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), content);
        return skillDir;
    }

    private void createSkillDirectoryRaw(String skillName, String content) throws IOException {
        Path skillDir = tempDir.resolve("skills").resolve(skillName);
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), content);
    }
}
