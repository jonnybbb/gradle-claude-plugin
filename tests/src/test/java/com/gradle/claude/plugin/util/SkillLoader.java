package com.gradle.claude.plugin.util;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * Loads and parses skill files from the plugin's skills/ directory.
 */
public class SkillLoader {

    private final Path pluginRoot;
    private final Yaml yaml = new Yaml();

    public SkillLoader(Path pluginRoot) {
        this.pluginRoot = pluginRoot;
    }

    /**
     * Load a skill by name (directory name under skills/).
     */
    public Skill loadSkill(String skillName) throws IOException {
        Path skillPath = pluginRoot.resolve("skills").resolve(skillName).resolve("SKILL.md");
        if (!Files.exists(skillPath)) {
            throw new IllegalArgumentException("Skill not found: " + skillName);
        }
        return parseSkillFile(skillPath);
    }

    /**
     * Load all skills from the plugin.
     */
    public List<Skill> loadAllSkills() throws IOException {
        Path skillsDir = pluginRoot.resolve("skills");
        try (Stream<Path> dirs = Files.list(skillsDir)) {
            return dirs
                .filter(Files::isDirectory)
                .filter(dir -> Files.exists(dir.resolve("SKILL.md")))
                .map(dir -> {
                    try {
                        return parseSkillFile(dir.resolve("SKILL.md"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        }
    }

    /**
     * Load all reference files for a skill.
     */
    public Map<String, String> loadReferences(String skillName) throws IOException {
        Path refsDir = pluginRoot.resolve("skills").resolve(skillName).resolve("references");
        Map<String, String> refs = new HashMap<>();

        if (Files.exists(refsDir)) {
            try (Stream<Path> files = Files.list(refsDir)) {
                files
                    .filter(f -> f.toString().endsWith(".md"))
                    .forEach(f -> {
                        try {
                            refs.put(f.getFileName().toString(), Files.readString(f));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            }
        }
        return refs;
    }

    private Skill parseSkillFile(Path skillPath) throws IOException {
        String content = Files.readString(skillPath);

        // Extract YAML frontmatter
        Pattern frontmatterPattern = Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n", Pattern.DOTALL);
        Matcher matcher = frontmatterPattern.matcher(content);

        if (!matcher.find()) {
            throw new IllegalArgumentException("No frontmatter found in: " + skillPath);
        }

        String frontmatterYaml = matcher.group(1);
        String body = content.substring(matcher.end());

        @SuppressWarnings("unchecked")
        Map<String, Object> frontmatter = yaml.load(frontmatterYaml);

        return new Skill(
            (String) frontmatter.get("name"),
            (String) frontmatter.get("description"),
            body,
            skillPath.getParent().getFileName().toString(),
            skillPath
        );
    }

    /**
     * Represents a parsed skill.
     */
    public record Skill(
        String name,
        String description,
        String body,
        String directoryName,
        Path path
    ) {
        /**
         * Get the full content as it would be presented to Claude.
         */
        public String getFullContent() {
            return body;
        }

        /**
         * Extract trigger phrases from description.
         */
        public List<String> getTriggerPhrases() {
            List<String> phrases = new ArrayList<>();
            Pattern quotePattern = Pattern.compile("\"([^\"]+)\"");
            Matcher matcher = quotePattern.matcher(description);
            while (matcher.find()) {
                phrases.add(matcher.group(1));
            }
            return phrases;
        }

        /**
         * Check if description uses third-person format.
         */
        public boolean hasThirdPersonDescription() {
            return description != null &&
                   description.startsWith("This skill should be used when");
        }

        /**
         * Count words in the body.
         */
        public int getWordCount() {
            return body.split("\\s+").length;
        }
    }
}
