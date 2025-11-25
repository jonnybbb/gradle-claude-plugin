package com.gradle.claude.plugin.util;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * Loads test fixture projects and extracts known issues.
 */
public class FixtureLoader {

    private final Path fixturesRoot;

    public FixtureLoader(Path pluginRoot) {
        this.fixturesRoot = pluginRoot.resolve("test-fixtures").resolve("projects");
    }

    /**
     * Load a fixture by name.
     */
    public Fixture loadFixture(String fixtureName) throws IOException {
        Path fixturePath = fixturesRoot.resolve(fixtureName);
        if (!Files.exists(fixturePath)) {
            throw new IllegalArgumentException("Fixture not found: " + fixtureName);
        }
        return parseFixture(fixturePath);
    }

    /**
     * Load all available fixtures.
     */
    public List<Fixture> loadAllFixtures() throws IOException {
        try (Stream<Path> dirs = Files.list(fixturesRoot)) {
            return dirs
                .filter(Files::isDirectory)
                .map(dir -> {
                    try {
                        return parseFixture(dir);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        }
    }

    private Fixture parseFixture(Path fixturePath) throws IOException {
        String name = fixturePath.getFileName().toString();

        // Find build file
        Path buildFile = Files.exists(fixturePath.resolve("build.gradle.kts"))
            ? fixturePath.resolve("build.gradle.kts")
            : fixturePath.resolve("build.gradle");

        String buildContent = Files.exists(buildFile) ? Files.readString(buildFile) : "";

        // Extract issues marked with ❌ ISSUE comments
        List<KnownIssue> issues = extractKnownIssues(buildContent);

        // Detect Gradle version from wrapper
        String gradleVersion = detectGradleVersion(fixturePath);

        // Detect project type
        ProjectType projectType = detectProjectType(fixturePath);

        return new Fixture(name, fixturePath, buildContent, issues, gradleVersion, projectType);
    }

    private List<KnownIssue> extractKnownIssues(String buildContent) {
        List<KnownIssue> issues = new ArrayList<>();

        // Pattern: // ❌ ISSUE N: description
        Pattern issuePattern = Pattern.compile("// ❌ ISSUE (\\d+): (.+)");
        Matcher matcher = issuePattern.matcher(buildContent);

        while (matcher.find()) {
            int issueNumber = Integer.parseInt(matcher.group(1));
            String description = matcher.group(2);
            IssueCategory category = categorizeIssue(description);
            issues.add(new KnownIssue(issueNumber, description, category));
        }

        return issues;
    }

    private IssueCategory categorizeIssue(String description) {
        String lower = description.toLowerCase();
        if (lower.contains("system.getproperty") || lower.contains("system.getenv")) {
            return IssueCategory.SYSTEM_ACCESS;
        } else if (lower.contains("eager") || lower.contains("tasks.create") || lower.contains("getbyname")) {
            return IssueCategory.EAGER_TASK;
        } else if (lower.contains("project.copy") || lower.contains("project.exec") ||
                   lower.contains("project.delete") || lower.contains("project.file") ||
                   lower.contains("project.javaexec")) {
            return IssueCategory.PROJECT_ACCESS;
        } else if (lower.contains("task.project") || lower.contains("builddir")) {
            return IssueCategory.CONFIG_CACHE;
        } else if (lower.contains("deprecated") || lower.contains("compile") || lower.contains("runtime")) {
            return IssueCategory.DEPRECATED_API;
        }
        return IssueCategory.OTHER;
    }

    private String detectGradleVersion(Path fixturePath) throws IOException {
        Path wrapperProps = fixturePath.resolve("gradle/wrapper/gradle-wrapper.properties");
        if (Files.exists(wrapperProps)) {
            String content = Files.readString(wrapperProps);
            Pattern versionPattern = Pattern.compile("gradle-(\\d+\\.\\d+(?:\\.\\d+)?)-");
            Matcher matcher = versionPattern.matcher(content);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "unknown";
    }

    private ProjectType detectProjectType(Path fixturePath) throws IOException {
        Path settingsFile = Files.exists(fixturePath.resolve("settings.gradle.kts"))
            ? fixturePath.resolve("settings.gradle.kts")
            : fixturePath.resolve("settings.gradle");

        if (Files.exists(settingsFile)) {
            String content = Files.readString(settingsFile);
            if (content.contains("include(") || content.contains("include ")) {
                return ProjectType.MULTI_PROJECT;
            }
            if (content.contains("includeBuild(") || content.contains("includeBuild ")) {
                return ProjectType.COMPOSITE;
            }
        }
        return ProjectType.SINGLE_PROJECT;
    }

    /**
     * Represents a test fixture project.
     */
    public record Fixture(
        String name,
        Path path,
        String buildFileContent,
        List<KnownIssue> knownIssues,
        String gradleVersion,
        ProjectType projectType
    ) {
        /**
         * Get all files in the fixture as a map of path -> content.
         */
        public Map<String, String> getAllFiles() throws IOException {
            Map<String, String> files = new HashMap<>();
            try (Stream<Path> walk = Files.walk(path)) {
                walk.filter(Files::isRegularFile)
                    .filter(f -> !f.toString().contains(".gradle/"))
                    .forEach(f -> {
                        try {
                            String relativePath = path.relativize(f).toString();
                            files.put(relativePath, Files.readString(f));
                        } catch (IOException e) {
                            // Skip binary files
                        }
                    });
            }
            return files;
        }

        /**
         * Check if fixture has issues of a specific category.
         */
        public boolean hasIssuesOfCategory(IssueCategory category) {
            return knownIssues.stream().anyMatch(i -> i.category() == category);
        }

        /**
         * Get count of issues by category.
         */
        public int countIssuesByCategory(IssueCategory category) {
            return (int) knownIssues.stream().filter(i -> i.category() == category).count();
        }
    }

    public record KnownIssue(int number, String description, IssueCategory category) {}

    public enum IssueCategory {
        EAGER_TASK,
        PROJECT_ACCESS,
        SYSTEM_ACCESS,
        CONFIG_CACHE,
        DEPRECATED_API,
        DEPENDENCY_ISSUE,
        OTHER
    }

    public enum ProjectType {
        SINGLE_PROJECT,
        MULTI_PROJECT,
        COMPOSITE
    }
}
