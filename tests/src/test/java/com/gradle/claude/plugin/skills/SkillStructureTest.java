package com.gradle.claude.plugin.skills;

import com.gradle.claude.plugin.util.SkillLoader;
import com.gradle.claude.plugin.util.SkillLoader.Skill;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for skill file structure and best practices compliance.
 * These tests do NOT require an API key - they validate skill files directly.
 */
@Tag("skills")
@DisplayName("Skill Structure Tests")
class SkillStructureTest {

    private static final Path PLUGIN_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static SkillLoader skillLoader;
    private static List<Skill> allSkills;

    @BeforeAll
    static void setUp() throws IOException {
        skillLoader = new SkillLoader(PLUGIN_ROOT);
        allSkills = skillLoader.loadAllSkills();
    }

    @Test
    @DisplayName("Plugin should have exactly 14 skills")
    void shouldHaveExpectedSkillCount() {
        assertThat(allSkills).hasSize(14);
    }

    static Stream<Skill> allSkillsProvider() {
        return allSkills.stream();
    }

    @ParameterizedTest(name = "Skill ''{0}'' has valid name")
    @MethodSource("allSkillsProvider")
    @DisplayName("All skills should have valid kebab-case names")
    void skillShouldHaveValidName(Skill skill) {
        assertThat(skill.name())
            .as("Skill name should be kebab-case")
            .matches("^[a-z][a-z0-9-]*[a-z0-9]$");
    }

    @ParameterizedTest(name = "Skill ''{0}'' has third-person description")
    @MethodSource("allSkillsProvider")
    @DisplayName("All skills should use third-person description format")
    void skillShouldHaveThirdPersonDescription(Skill skill) {
        assertThat(skill.hasThirdPersonDescription())
            .as("Description should start with 'This skill should be used when'")
            .isTrue();
    }

    @ParameterizedTest(name = "Skill ''{0}'' has trigger phrases")
    @MethodSource("allSkillsProvider")
    @DisplayName("All skills should have specific trigger phrases in quotes")
    void skillShouldHaveTriggerPhrases(Skill skill) {
        List<String> triggers = skill.getTriggerPhrases();
        assertThat(triggers)
            .as("Skill should have at least 3 trigger phrases in quotes")
            .hasSizeGreaterThanOrEqualTo(3);
    }

    @ParameterizedTest(name = "Skill ''{0}'' has appropriate word count")
    @MethodSource("allSkillsProvider")
    @DisplayName("Skills should have lean body (under 3000 words)")
    void skillShouldHaveLeanBody(Skill skill) {
        int wordCount = skill.getWordCount();
        assertThat(wordCount)
            .as("Skill body should be lean (under 3000 words for progressive disclosure)")
            .isLessThan(3000);

        // Also check it's not too sparse
        assertThat(wordCount)
            .as("Skill body should have substantial content (at least 200 words)")
            .isGreaterThan(200);
    }

    @ParameterizedTest(name = "Skill ''{0}'' has no second-person pronouns")
    @MethodSource("allSkillsProvider")
    @DisplayName("Skills should use imperative form, not second-person")
    void skillShouldNotHaveSecondPerson(Skill skill) {
        String body = skill.body().toLowerCase();

        // Check for common second-person patterns
        assertThat(body)
            .as("Skill body should not use 'you' (second-person)")
            .doesNotContainPattern("\\byou\\b")
            .doesNotContainPattern("\\byour\\b")
            .doesNotContainPattern("\\byourself\\b");
    }

    @Test
    @DisplayName("Each skill should have a references/ directory with at least one file")
    void skillsShouldHaveReferences() throws IOException {
        for (Skill skill : allSkills) {
            var refs = skillLoader.loadReferences(skill.directoryName());
            assertThat(refs)
                .as("Skill '%s' should have reference files for progressive disclosure", skill.name())
                .isNotEmpty();
        }
    }

    @Test
    @DisplayName("Skill names should match directory names")
    void skillNamesShouldMatchDirectories() {
        for (Skill skill : allSkills) {
            assertThat(skill.name())
                .as("Skill name in frontmatter should match directory name")
                .isEqualTo(skill.directoryName());
        }
    }

    @Test
    @DisplayName("Expected skills should exist")
    void expectedSkillsShouldExist() {
        List<String> expectedSkills = List.of(
            "gradle-performance",
            "gradle-config-cache",
            "gradle-build-cache",
            "gradle-dependencies",
            "gradle-migration",
            "gradle-doctor",
            "gradle-troubleshooting",
            "gradle-structure",
            "gradle-task-development",
            "gradle-plugin-development",
            "gradle-openrewrite",
            "gradle-best-practices",
            "develocity",
            "develocity-analytics"
        );

        List<String> actualNames = allSkills.stream()
            .map(Skill::name)
            .toList();

        assertThat(actualNames)
            .as("All expected skills should be present")
            .containsAll(expectedSkills);
    }

    @Test
    @DisplayName("Skills should reference related files that exist")
    void skillReferencesShouldExist() throws IOException {
        for (Skill skill : allSkills) {
            String body = skill.body();

            // Extract references like [references/file.md](references/file.md)
            var matcher = java.util.regex.Pattern
                .compile("references/([\\w-]+\\.md)")
                .matcher(body);

            while (matcher.find()) {
                String refFile = matcher.group(1);
                var refs = skillLoader.loadReferences(skill.directoryName());
                assertThat(refs)
                    .as("Referenced file '%s' should exist for skill '%s'", refFile, skill.name())
                    .containsKey(refFile);
            }
        }
    }
}
