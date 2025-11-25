package com.gradle.claude.plugin.skills;

import com.gradle.claude.plugin.util.SkillLoader;
import com.gradle.claude.plugin.util.SkillLoader.Skill;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for the gradle-openrewrite skill structure and content.
 */
@Tag("skills")
@DisplayName("OpenRewrite Skill Tests")
class OpenRewriteSkillTest {

    private static final Path PLUGIN_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path SKILL_PATH = PLUGIN_ROOT.resolve("skills/gradle-openrewrite");
    private static SkillLoader skillLoader;
    private static Skill openRewriteSkill;

    @BeforeAll
    static void setUp() throws IOException {
        skillLoader = new SkillLoader(PLUGIN_ROOT);

        // Load the skill if it exists
        if (Files.exists(SKILL_PATH.resolve("SKILL.md"))) {
            var allSkills = skillLoader.loadAllSkills();
            openRewriteSkill = allSkills.stream()
                .filter(s -> s.name().equals("gradle-openrewrite"))
                .findFirst()
                .orElse(null);
        }
    }

    @Test
    @DisplayName("OpenRewrite skill should exist")
    void skillShouldExist() {
        assertThat(SKILL_PATH)
            .as("gradle-openrewrite skill directory should exist")
            .exists();

        assertThat(SKILL_PATH.resolve("SKILL.md"))
            .as("SKILL.md should exist")
            .exists();
    }

    @Test
    @DisplayName("Skill should have valid frontmatter")
    void skillShouldHaveValidFrontmatter() {
        assertThat(openRewriteSkill)
            .as("OpenRewrite skill should be loaded")
            .isNotNull();

        assertThat(openRewriteSkill.name())
            .isEqualTo("gradle-openrewrite");

        assertThat(openRewriteSkill.description())
            .as("Skill should have description")
            .isNotBlank();
    }

    @Test
    @DisplayName("Skill description should contain OpenRewrite trigger phrases")
    void skillShouldHaveTriggerPhrases() {
        assertThat(openRewriteSkill).isNotNull();

        String description = openRewriteSkill.description().toLowerCase();

        assertThat(description)
            .as("Description should mention OpenRewrite-related triggers")
            .containsAnyOf(
                "openrewrite",
                "automated refactoring",
                "large-scale migrations",
                "recipe",
                "bulk"
            );
    }

    @Test
    @DisplayName("Skill should have references directory")
    void skillShouldHaveReferences() throws IOException {
        Path referencesDir = SKILL_PATH.resolve("references");

        assertThat(referencesDir)
            .as("references/ directory should exist")
            .exists();

        Map<String, String> refs = skillLoader.loadReferences("gradle-openrewrite");

        assertThat(refs)
            .as("Should have reference files")
            .isNotEmpty();
    }

    @Test
    @DisplayName("Skill should have recipe-catalog.md reference")
    void skillShouldHaveRecipeCatalog() throws IOException {
        Map<String, String> refs = skillLoader.loadReferences("gradle-openrewrite");

        assertThat(refs)
            .as("Should have recipe-catalog.md")
            .containsKey("recipe-catalog.md");

        String catalog = refs.get("recipe-catalog.md");
        assertThat(catalog)
            .as("Recipe catalog should list Gradle recipes")
            .contains("org.openrewrite.gradle");
    }

    @Test
    @DisplayName("Skill should have custom-recipes.md reference")
    void skillShouldHaveCustomRecipes() throws IOException {
        Map<String, String> refs = skillLoader.loadReferences("gradle-openrewrite");

        assertThat(refs)
            .as("Should have custom-recipes.md")
            .containsKey("custom-recipes.md");
    }

    @Test
    @DisplayName("Skill should have recipe-mappings.yml reference")
    void skillShouldHaveRecipeMappings() throws IOException {
        Path mappingsFile = SKILL_PATH.resolve("references/recipe-mappings.yml");

        assertThat(mappingsFile)
            .as("recipe-mappings.yml should exist")
            .exists();

        String content = Files.readString(mappingsFile);
        assertThat(content)
            .as("Mappings should contain recipe patterns")
            .contains("patterns:")
            .contains("recipes:");
    }

    @Test
    @DisplayName("Skill body should document key commands")
    void skillBodyShouldDocumentCommands() {
        assertThat(openRewriteSkill).isNotNull();

        String body = openRewriteSkill.body();

        assertThat(body)
            .as("Should document /openrewrite command")
            .contains("/openrewrite");

        assertThat(body)
            .as("Should document dry-run option")
            .containsIgnoringCase("dry-run");

        assertThat(body)
            .as("Should document recipe execution")
            .contains("run");
    }

    @Test
    @DisplayName("Skill should document engine selection")
    void skillShouldDocumentEngineSelection() {
        assertThat(openRewriteSkill).isNotNull();

        String body = openRewriteSkill.body();

        assertThat(body)
            .as("Should mention when to use OpenRewrite vs Claude")
            .containsIgnoringCase("when to use");
    }

    @Test
    @DisplayName("Skill should document Phase 4 migration orchestrator")
    void skillShouldDocumentMigrationOrchestrator() {
        assertThat(openRewriteSkill).isNotNull();

        String body = openRewriteSkill.body();

        assertThat(body)
            .as("Should document /migrate command")
            .contains("/migrate");

        assertThat(body)
            .as("Should document complexity analysis")
            .containsAnyOf("SMALL", "MEDIUM", "LARGE", "complexity");
    }

    @Test
    @DisplayName("Skill should be lean (under word limit)")
    void skillShouldBeLean() {
        assertThat(openRewriteSkill).isNotNull();

        int wordCount = openRewriteSkill.getWordCount();

        assertThat(wordCount)
            .as("Skill body should be lean (under 3000 words)")
            .isLessThan(3000);

        assertThat(wordCount)
            .as("Skill body should have substantial content")
            .isGreaterThan(200);
    }

    @Test
    @DisplayName("Skill should reference hybrid approach")
    void skillShouldReferenceHybridApproach() {
        assertThat(openRewriteSkill).isNotNull();

        String body = openRewriteSkill.body();

        assertThat(body)
            .as("Should explain hybrid approach (Tooling API + Init Script)")
            .containsIgnoringCase("hybrid")
            .containsAnyOf("init script", "Init Script", "Tooling API");
    }
}
