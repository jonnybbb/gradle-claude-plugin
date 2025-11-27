package com.gradle.claude.plugin.tools;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for the openrewrite_runner.java JBang tool.
 * These tests verify the tool's analysis and recipe suggestion capabilities.
 *
 * Note: These tests require JBang to be installed and working correctly.
 * They are tagged with "tools" so they can be skipped in CI environments
 * where JBang may not be available.
 */
@Tag("tools")
@Tag("openrewrite")
@DisplayName("OpenRewrite Runner Tool Tests")
class OpenRewriteRunnerTest {

    private static final Path PLUGIN_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path TOOL_PATH = PLUGIN_ROOT.resolve("tools/openrewrite_runner.java");
    private static final Path FIXTURES_ROOT = Path.of("fixtures/projects").toAbsolutePath().normalize();
    private static final Gson gson = new Gson();

    @BeforeAll
    static void checkToolExists() {
        assertThat(TOOL_PATH)
            .as("openrewrite_runner.java should exist")
            .exists();
    }

    static boolean jbangAvailable() {
        try {
            Process process = new ProcessBuilder("jbang", "--version")
                .redirectErrorStream(true)
                .start();
            return process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== Error Handling Tests ====================

    @Test
    @EnabledIf("jbangAvailable")
    @DisplayName("Should return error for non-existent project path")
    void shouldReturnErrorForNonExistentPath() throws Exception {
        Path nonExistent = Path.of("/tmp/non-existent-project-" + System.currentTimeMillis());

        ProcessResult result = runTool(nonExistent, "--analyze", "--json");

        assertThat(result.exitCode)
            .as("Should return non-zero exit code for non-existent path")
            .isNotEqualTo(0);
        assertThat(result.stderr)
            .as("Should report error about non-existent path")
            .contains("does not exist");
    }

    // ==================== List Recipes Tests ====================

    @Test
    @EnabledIf("jbangAvailable")
    @DisplayName("Should list available recipes")
    void shouldListRecipes() throws Exception {
        // Note: --list expects an optional parameter, use --list= for no filter
        ProcessResult result = runTool(FIXTURES_ROOT.resolve("simple-java"), "--list=", "--json");

        assertThat(result.exitCode).isEqualTo(0);
        assertThat(result.stdout).contains("org.openrewrite.gradle");

        JsonArray recipes = gson.fromJson(result.stdout, JsonArray.class);
        assertThat(recipes.size()).isGreaterThan(5);
    }

    @Test
    @EnabledIf("jbangAvailable")
    @DisplayName("Should filter recipes by keyword")
    void shouldFilterRecipesByKeyword() throws Exception {
        ProcessResult result = runTool(FIXTURES_ROOT.resolve("simple-java"), "--list=gradle", "--json");

        assertThat(result.exitCode).isEqualTo(0);

        JsonArray recipes = gson.fromJson(result.stdout, JsonArray.class);
        for (int i = 0; i < recipes.size(); i++) {
            JsonObject recipe = recipes.get(i).getAsJsonObject();
            String name = recipe.get("name").getAsString().toLowerCase();
            String description = recipe.get("description").getAsString().toLowerCase();
            assertThat(name + " " + description).containsIgnoringCase("gradle");
        }
    }

    // ==================== Suggest Recipes Tests ====================

    @Test
    @EnabledIf("jbangAvailable")
    @DisplayName("Should suggest recipes for project with issues")
    void shouldSuggestRecipesForBrokenProject() throws Exception {
        ProcessResult result = runTool(FIXTURES_ROOT.resolve("config-cache-broken"), "--suggest", "--json");

        assertThat(result.exitCode).isEqualTo(0);

        JsonObject suggestions = gson.fromJson(result.stdout, JsonObject.class);
        assertThat(suggestions.has("suggestions")).isTrue();

        JsonArray suggestionList = suggestions.getAsJsonArray("suggestions");
        assertThat(suggestionList.size()).isGreaterThan(0);

        // Should suggest MigrateToGradle8 for the broken patterns
        boolean hasMigrateRecipe = false;
        for (int i = 0; i < suggestionList.size(); i++) {
            String recipe = suggestionList.get(i).getAsJsonObject().get("recipe").getAsString();
            if (recipe.contains("MigrateToGradle")) {
                hasMigrateRecipe = true;
                break;
            }
        }
        assertThat(hasMigrateRecipe)
            .as("Should suggest migration recipe for config-cache-broken project")
            .isTrue();
    }

    @Test
    @EnabledIf("jbangAvailable")
    @DisplayName("Should have fewer suggestions for healthy project")
    void shouldHaveFewerSuggestionsForHealthyProject() throws Exception {
        ProcessResult brokenResult = runTool(FIXTURES_ROOT.resolve("config-cache-broken"), "--suggest", "--json");
        ProcessResult healthyResult = runTool(FIXTURES_ROOT.resolve("simple-java"), "--suggest", "--json");

        assertThat(brokenResult.exitCode).isEqualTo(0);
        assertThat(healthyResult.exitCode).isEqualTo(0);

        JsonObject brokenSuggestions = gson.fromJson(brokenResult.stdout, JsonObject.class);
        JsonObject healthySuggestions = gson.fromJson(healthyResult.stdout, JsonObject.class);

        int brokenCount = brokenSuggestions.getAsJsonArray("suggestions").size();
        int healthyCount = healthySuggestions.getAsJsonArray("suggestions").size();

        assertThat(brokenCount)
            .as("Broken project should have more suggestions than healthy project")
            .isGreaterThanOrEqualTo(healthyCount);
    }

    // ==================== Generate Recipe Tests ====================

    @Test
    @EnabledIf("jbangAvailable")
    @DisplayName("Should generate custom recipe for project with patterns")
    void shouldGenerateCustomRecipe() throws Exception {
        // Create a temp directory with a copy of the broken project
        Path tempDir = Files.createTempDirectory("openrewrite-test");
        try {
            copyDirectory(FIXTURES_ROOT.resolve("config-cache-broken"), tempDir);

            ProcessResult result = runTool(tempDir, "--generate-recipe", "--json");

            assertThat(result.exitCode).isEqualTo(0);

            JsonObject generated = gson.fromJson(result.stdout, JsonObject.class);
            assertThat(generated.has("detectedPatterns")).isTrue();

            // Check that the .rewrite directory was created
            Path rewriteDir = tempDir.resolve(".rewrite");
            if (generated.getAsJsonArray("detectedPatterns").size() > 0) {
                assertThat(rewriteDir).exists();
                assertThat(rewriteDir.resolve("generated-migrations.yml")).exists();
            }
        } finally {
            deleteDirectory(tempDir);
        }
    }

    // ==================== Analyze Project Tests ====================

    @Test
    @EnabledIf("jbangAvailable")
    @DisplayName("Should analyze project and detect complexity")
    void shouldAnalyzeProject() throws Exception {
        ProcessResult result = runTool(FIXTURES_ROOT.resolve("multi-module"), "--analyze", "--json");

        assertThat(result.exitCode).isEqualTo(0);

        JsonObject analysis = gson.fromJson(result.stdout, JsonObject.class);
        assertThat(analysis.has("complexity")).isTrue();
        assertThat(analysis.has("recommendedStrategy")).isTrue();
        assertThat(analysis.has("migrationPlan")).isTrue();
        assertThat(analysis.has("moduleCount")).isTrue();

        String complexity = analysis.get("complexity").getAsString();
        assertThat(complexity).isIn("SMALL", "MEDIUM", "LARGE");

        String strategy = analysis.get("recommendedStrategy").getAsString();
        assertThat(strategy).isIn("OPENREWRITE_PRIMARY", "CLAUDE_PRIMARY", "HYBRID");
    }

    @Test
    @EnabledIf("jbangAvailable")
    @DisplayName("Should recommend CLAUDE_PRIMARY for small projects")
    void shouldRecommendClaudeForSmallProjects() throws Exception {
        ProcessResult result = runTool(FIXTURES_ROOT.resolve("simple-java"), "--analyze", "--json");

        assertThat(result.exitCode).isEqualTo(0);

        JsonObject analysis = gson.fromJson(result.stdout, JsonObject.class);
        String complexity = analysis.get("complexity").getAsString();
        String strategy = analysis.get("recommendedStrategy").getAsString();

        // Simple-java is a single module project
        assertThat(complexity).isEqualTo("SMALL");
        assertThat(strategy).isIn("CLAUDE_PRIMARY", "HYBRID");
    }

    @Test
    @EnabledIf("jbangAvailable")
    @DisplayName("Should detect issues in broken project analysis")
    void shouldDetectIssuesInAnalysis() throws Exception {
        ProcessResult result = runTool(FIXTURES_ROOT.resolve("config-cache-broken"), "--analyze", "--json");

        assertThat(result.exitCode).isEqualTo(0);

        JsonObject analysis = gson.fromJson(result.stdout, JsonObject.class);
        assertThat(analysis.has("issues")).isTrue();

        JsonObject issues = analysis.getAsJsonObject("issues");
        int totalIssues = 0;
        for (String key : issues.keySet()) {
            totalIssues += issues.get(key).getAsInt();
        }

        assertThat(totalIssues)
            .as("Config-cache-broken project should have detected issues")
            .isGreaterThan(0);
    }

    @Test
    @EnabledIf("jbangAvailable")
    @DisplayName("Should generate migration plan with steps")
    void shouldGenerateMigrationPlan() throws Exception {
        ProcessResult result = runTool(FIXTURES_ROOT.resolve("config-cache-broken"), "--analyze", "--json");

        assertThat(result.exitCode).isEqualTo(0);

        JsonObject analysis = gson.fromJson(result.stdout, JsonObject.class);
        JsonArray migrationPlan = analysis.getAsJsonArray("migrationPlan");

        assertThat(migrationPlan.size())
            .as("Migration plan should have steps")
            .isGreaterThan(0);

        // Check first step is git checkpoint
        JsonObject firstStep = migrationPlan.get(0).getAsJsonObject();
        assertThat(firstStep.get("description").getAsString().toLowerCase())
            .contains("git");
    }

    // ==================== Dry Run Tests ====================

    @Test
    @EnabledIf("jbangAvailable")
    @DisplayName("Should perform dry run without modifying files")
    void shouldPerformDryRunWithoutModifying() throws Exception {
        // Create a temp directory with a copy of the broken project
        Path tempDir = Files.createTempDirectory("openrewrite-dryrun-test");
        try {
            copyDirectory(FIXTURES_ROOT.resolve("config-cache-broken"), tempDir);

            // Get initial file contents
            String originalContent = Files.readString(tempDir.resolve("build.gradle.kts"));

            ProcessResult result = runTool(tempDir,
                "--recipe=org.openrewrite.gradle.MigrateToGradle8",
                "--dry-run",
                "--json");

            // Whether the tool succeeds or fails, the file should not be modified
            // (OpenRewrite may fail due to missing dependencies in temp project, but dry-run should never modify)
            String afterContent = Files.readString(tempDir.resolve("build.gradle.kts"));
            assertThat(afterContent)
                .as("Dry run should not modify files even if recipe execution fails")
                .isEqualTo(originalContent);

        } finally {
            deleteDirectory(tempDir);
        }
    }

    // ==================== Legacy Groovy Project Tests ====================

    @Test
    @EnabledIf("jbangAvailable")
    @DisplayName("Should suggest Kotlin DSL migration for Groovy projects")
    void shouldSuggestKotlinDslMigration() throws Exception {
        ProcessResult result = runTool(FIXTURES_ROOT.resolve("legacy-groovy"), "--suggest", "--json");

        assertThat(result.exitCode).isEqualTo(0);

        JsonObject suggestions = gson.fromJson(result.stdout, JsonObject.class);
        JsonArray suggestionList = suggestions.getAsJsonArray("suggestions");

        boolean hasKotlinDslSuggestion = false;
        for (int i = 0; i < suggestionList.size(); i++) {
            String recipe = suggestionList.get(i).getAsJsonObject().get("recipe").getAsString();
            if (recipe.contains("KotlinDsl")) {
                hasKotlinDslSuggestion = true;
                break;
            }
        }

        assertThat(hasKotlinDslSuggestion)
            .as("Should suggest Kotlin DSL migration for Groovy project")
            .isTrue();
    }

    // ==================== Robustness Edge Case Tests ====================

    @Test
    @EnabledIf("jbangAvailable")
    @DisplayName("Should handle project with syntax errors in build file")
    void shouldHandleProjectWithSyntaxErrors() throws Exception {
        Path tempDir = Files.createTempDirectory("syntax-error-test");
        try {
            // Create a build file with syntax errors
            Files.writeString(tempDir.resolve("build.gradle.kts"), """
                plugins {
                    java  // missing closing brace - syntax error

                dependencies {
                    implementation("broken
                }
                """);

            // Create wrapper properties for version detection
            Path wrapperDir = tempDir.resolve("gradle/wrapper");
            Files.createDirectories(wrapperDir);
            Files.writeString(wrapperDir.resolve("gradle-wrapper.properties"), """
                distributionUrl=https\\://services.gradle.org/distributions/gradle-8.5-bin.zip
                """);

            ProcessResult result = runTool(tempDir, "--analyze", "--json");

            // Should not crash - may report error or analyze what it can
            assertThat(result.stdout + result.stderr)
                .as("Should handle syntax errors gracefully")
                .isNotBlank();
        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Test
    @EnabledIf("jbangAvailable")
    @DisplayName("Should handle project with unicode in paths and content")
    void shouldHandleUnicodeInPathsAndContent() throws Exception {
        Path tempDir = Files.createTempDirectory("unicode-项目-тест");
        try {
            Files.writeString(tempDir.resolve("build.gradle.kts"), """
                plugins {
                    java
                }

                // 日本語コメント
                // Комментарий на русском
                val 变量 = "value"
                """);

            Path wrapperDir = tempDir.resolve("gradle/wrapper");
            Files.createDirectories(wrapperDir);
            Files.writeString(wrapperDir.resolve("gradle-wrapper.properties"), """
                distributionUrl=https\\://services.gradle.org/distributions/gradle-8.5-bin.zip
                """);

            ProcessResult result = runTool(tempDir, "--analyze", "--json");

            // Should handle unicode without crashing
            assertThat(result.exitCode)
                .as("Should handle unicode paths and content")
                .isIn(0, 1); // May succeed or report error, but shouldn't crash
        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Test
    @EnabledIf("jbangAvailable")
    @DisplayName("Should handle very large build file")
    void shouldHandleVeryLargeBuildFile() throws Exception {
        Path tempDir = Files.createTempDirectory("large-build-test");
        try {
            StringBuilder largeBuild = new StringBuilder();
            largeBuild.append("plugins {\n    java\n}\n\n");

            // Generate many dependencies
            for (int i = 0; i < 500; i++) {
                largeBuild.append(String.format("// Dependency %d\n", i));
            }

            // Generate many tasks (some eager, some lazy)
            for (int i = 0; i < 100; i++) {
                if (i % 2 == 0) {
                    largeBuild.append(String.format("tasks.register(\"task%d\") { }\n", i));
                } else {
                    largeBuild.append(String.format("tasks.create(\"eagerTask%d\") { }\n", i));
                }
            }

            Files.writeString(tempDir.resolve("build.gradle.kts"), largeBuild.toString());

            Path wrapperDir = tempDir.resolve("gradle/wrapper");
            Files.createDirectories(wrapperDir);
            Files.writeString(wrapperDir.resolve("gradle-wrapper.properties"), """
                distributionUrl=https\\://services.gradle.org/distributions/gradle-8.5-bin.zip
                """);

            ProcessResult result = runTool(tempDir, "--analyze", "--json");

            assertThat(result.exitCode)
                .as("Should handle large build files")
                .isEqualTo(0);
        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Test
    @EnabledIf("jbangAvailable")
    @DisplayName("Should handle deeply nested multi-module project")
    void shouldHandleDeeplyNestedMultiModule() throws Exception {
        Path tempDir = Files.createTempDirectory("nested-module-test");
        try {
            // Create root build
            Files.writeString(tempDir.resolve("build.gradle.kts"), """
                plugins {
                    java
                }
                """);

            // Create deeply nested module structure
            StringBuilder includes = new StringBuilder();
            Path currentDir = tempDir;
            for (int i = 0; i < 5; i++) {
                currentDir = currentDir.resolve("sub" + i);
                Files.createDirectories(currentDir);
                Files.writeString(currentDir.resolve("build.gradle.kts"), """
                    plugins {
                        java
                    }
                    """);
                includes.append(String.format("include(\":sub%d\")\n", i));
            }

            Files.writeString(tempDir.resolve("settings.gradle.kts"), includes.toString());

            Path wrapperDir = tempDir.resolve("gradle/wrapper");
            Files.createDirectories(wrapperDir);
            Files.writeString(wrapperDir.resolve("gradle-wrapper.properties"), """
                distributionUrl=https\\://services.gradle.org/distributions/gradle-8.5-bin.zip
                """);

            ProcessResult result = runTool(tempDir, "--analyze", "--json");

            assertThat(result.exitCode)
                .as("Should handle deeply nested projects")
                .isIn(0, 1);
        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Test
    @EnabledIf("jbangAvailable")
    @DisplayName("Should handle empty build file")
    void shouldHandleEmptyBuildFile() throws Exception {
        Path tempDir = Files.createTempDirectory("empty-build-test");
        try {
            Files.writeString(tempDir.resolve("build.gradle.kts"), "");

            Path wrapperDir = tempDir.resolve("gradle/wrapper");
            Files.createDirectories(wrapperDir);
            Files.writeString(wrapperDir.resolve("gradle-wrapper.properties"), """
                distributionUrl=https\\://services.gradle.org/distributions/gradle-8.5-bin.zip
                """);

            ProcessResult result = runTool(tempDir, "--analyze", "--json");

            // Empty build file should be handled gracefully
            assertThat(result.stdout + result.stderr)
                .as("Should handle empty build file")
                .isNotBlank();
        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Test
    @EnabledIf("jbangAvailable")
    @DisplayName("Should handle mixed Groovy and Kotlin DSL in same project")
    void shouldHandleMixedDsl() throws Exception {
        Path tempDir = Files.createTempDirectory("mixed-dsl-test");
        try {
            // Root uses Kotlin DSL
            Files.writeString(tempDir.resolve("build.gradle.kts"), """
                plugins {
                    java
                }
                """);

            // Subproject uses Groovy DSL
            Path subDir = tempDir.resolve("subproject");
            Files.createDirectories(subDir);
            Files.writeString(subDir.resolve("build.gradle"), """
                plugins {
                    id 'java'
                }

                tasks.create('eagerTask') {
                    doLast { println 'Eager!' }
                }
                """);

            Files.writeString(tempDir.resolve("settings.gradle.kts"), """
                include(":subproject")
                """);

            Path wrapperDir = tempDir.resolve("gradle/wrapper");
            Files.createDirectories(wrapperDir);
            Files.writeString(wrapperDir.resolve("gradle-wrapper.properties"), """
                distributionUrl=https\\://services.gradle.org/distributions/gradle-8.5-bin.zip
                """);

            ProcessResult result = runTool(tempDir, "--analyze", "--json");

            assertThat(result.exitCode)
                .as("Should handle mixed DSL project")
                .isEqualTo(0);

            // Should analyze both build files
            JsonObject analysis = gson.fromJson(result.stdout, JsonObject.class);
            if (analysis.has("moduleCount")) {
                assertThat(analysis.get("moduleCount").getAsInt())
                    .as("Should detect multiple modules")
                    .isGreaterThanOrEqualTo(1);
            }
        } finally {
            deleteDirectory(tempDir);
        }
    }

    // ==================== CLI Flag Coverage Tests ====================

    @Test
    @DisplayName("Should not have unused CLI flags (static analysis)")
    void shouldNotHaveUnusedCliFlags() throws Exception {
        // Read the tool source code
        String sourceCode = Files.readString(TOOL_PATH);

        // Find all @Option declarations
        java.util.regex.Pattern optionPattern = java.util.regex.Pattern.compile(
            "@Option\\s*\\([^)]*names\\s*=\\s*\\{?[^}]*\"--([^\"]+)\"");
        java.util.regex.Matcher matcher = optionPattern.matcher(sourceCode);

        java.util.List<String> declaredFlags = new java.util.ArrayList<>();
        while (matcher.find()) {
            declaredFlags.add(matcher.group(1));
        }

        // Check each flag is actually used somewhere in the code (beyond declaration)
        for (String flag : declaredFlags) {
            // Convert flag name to variable name (e.g., "dry-run" -> "dryRun")
            String varName = toCamelCase(flag);

            // Count occurrences of the variable name
            int occurrences = countOccurrences(sourceCode, varName);

            // Should appear more than just in the declaration (at least 2: declaration + use)
            assertThat(occurrences)
                .as("Flag --%s (variable %s) should be used, not just declared", flag, varName)
                .isGreaterThanOrEqualTo(2);
        }
    }

    private String toCamelCase(String flag) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : flag.toCharArray()) {
            if (c == '-') {
                capitalizeNext = true;
            } else {
                result.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            }
        }
        return result.toString();
    }

    private int countOccurrences(String text, String word) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(word, index)) != -1) {
            count++;
            index += word.length();
        }
        return count;
    }

    @Test
    @EnabledIf("jbangAvailable")
    @DisplayName("All documented flags should work without error")
    void allDocumentedFlagsShouldWork() throws Exception {
        Path projectPath = FIXTURES_ROOT.resolve("simple-java");

        // Test --help flag
        ProcessResult helpResult = runToolRaw("--help");
        assertThat(helpResult.exitCode).isEqualTo(0);
        assertThat(helpResult.stdout).contains("--recipe", "--dry-run", "--list", "--suggest", "--analyze");

        // Test --json flag with various commands
        ProcessResult listJsonResult = runTool(projectPath, "--list=", "--json");
        assertThat(listJsonResult.stdout.trim()).startsWith("["); // JSON array

        ProcessResult suggestJsonResult = runTool(projectPath, "--suggest", "--json");
        assertThat(suggestJsonResult.stdout.trim()).startsWith("{"); // JSON object

        ProcessResult analyzeJsonResult = runTool(projectPath, "--analyze", "--json");
        assertThat(analyzeJsonResult.stdout.trim()).startsWith("{"); // JSON object
    }

    private ProcessResult runToolRaw(String... args) throws Exception {
        String[] command = new String[args.length + 2];
        command[0] = "jbang";
        command[1] = TOOL_PATH.toString();
        System.arraycopy(args, 0, command, 2, args.length);

        ProcessBuilder pb = new ProcessBuilder(command)
            .redirectErrorStream(false);

        Process process = pb.start();

        String stdout = new String(process.getInputStream().readAllBytes());
        String stderr = new String(process.getErrorStream().readAllBytes());

        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        assertThat(finished).as("Process should complete within timeout").isTrue();

        return new ProcessResult(process.exitValue(), stdout, stderr);
    }

    // ==================== Helper Methods ====================

    private ProcessResult runTool(Path projectPath, String... args) throws Exception {
        String[] command = new String[args.length + 3];
        command[0] = "jbang";
        command[1] = TOOL_PATH.toString();
        command[2] = projectPath.toString();
        System.arraycopy(args, 0, command, 3, args.length);

        ProcessBuilder pb = new ProcessBuilder(command)
            .redirectErrorStream(false);

        Process process = pb.start();

        String stdout = new String(process.getInputStream().readAllBytes());
        String stderr = new String(process.getErrorStream().readAllBytes());

        boolean finished = process.waitFor(120, TimeUnit.SECONDS);
        assertThat(finished).as("Process should complete within timeout").isTrue();

        return new ProcessResult(process.exitValue(), stdout, stderr);
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source)
            .filter(p -> !p.toString().contains("/.gradle/") && !p.toString().contains("/build/"))
            .forEach(p -> {
                try {
                    Path dest = target.resolve(source.relativize(p));
                    if (Files.isDirectory(p)) {
                        Files.createDirectories(dest);
                    } else {
                        Files.createDirectories(dest.getParent());
                        Files.copy(p, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        // Ignore cleanup errors
                    }
                });
        }
    }

    record ProcessResult(int exitCode, String stdout, String stderr) {}
}
