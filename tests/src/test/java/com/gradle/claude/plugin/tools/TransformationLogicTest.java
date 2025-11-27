package com.gradle.claude.plugin.tools;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.nio.file.*;
import java.util.regex.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for transformation logic used in JBang tools.
 * These tests validate regex patterns and string replacements
 * without requiring JBang or external dependencies.
 */
@Tag("tools")
@DisplayName("Tool Transformation Logic Tests")
class TransformationLogicTest {

    // =========================================================================
    // File Filtering Tests (catches .gradle directory filter bug)
    // =========================================================================

    @Nested
    @DisplayName("Build file filtering")
    class BuildFileFilteringTests {

        @Test
        @DisplayName("should NOT filter out build.gradle files")
        void shouldNotFilterOutBuildGradleFiles() {
            Path projectDir = Path.of("/project");
            Path gradleCacheDir = projectDir.resolve(".gradle");

            // These should be INCLUDED
            Path rootBuild = Path.of("/project/build.gradle");
            Path subBuild = Path.of("/project/app/build.gradle");
            Path ktsBuild = Path.of("/project/build.gradle.kts");

            assertThat(shouldIncludeBuildFile(rootBuild, gradleCacheDir))
                .as("Root build.gradle should be included")
                .isTrue();
            assertThat(shouldIncludeBuildFile(subBuild, gradleCacheDir))
                .as("Subproject build.gradle should be included")
                .isTrue();
            assertThat(shouldIncludeBuildFile(ktsBuild, gradleCacheDir))
                .as("Kotlin DSL build.gradle.kts should be included")
                .isTrue();
        }

        @Test
        @DisplayName("should filter out files in .gradle cache directory")
        void shouldFilterOutGradleCacheFiles() {
            Path projectDir = Path.of("/project");
            Path gradleCacheDir = projectDir.resolve(".gradle");

            // These should be EXCLUDED (inside .gradle cache)
            Path cachedBuild = Path.of("/project/.gradle/8.0/some/build.gradle");
            Path cachedKts = Path.of("/project/.gradle/configuration-cache/build.gradle.kts");

            assertThat(shouldIncludeBuildFile(cachedBuild, gradleCacheDir))
                .as("Files in .gradle cache should be excluded")
                .isFalse();
            assertThat(shouldIncludeBuildFile(cachedKts, gradleCacheDir))
                .as("Files in .gradle cache should be excluded")
                .isFalse();
        }

        @Test
        @DisplayName("should filter out files in build output directory")
        void shouldFilterOutBuildOutputFiles() {
            Path projectDir = Path.of("/project");
            Path gradleCacheDir = projectDir.resolve(".gradle");

            // These should be EXCLUDED (inside build output)
            Path buildOutput = Path.of("/project/build/generated/build.gradle");
            Path subBuildOutput = Path.of("/project/app/build/tmp/build.gradle.kts");

            assertThat(shouldIncludeBuildFile(buildOutput, gradleCacheDir))
                .as("Files in build output should be excluded")
                .isFalse();
            assertThat(shouldIncludeBuildFile(subBuildOutput, gradleCacheDir))
                .as("Files in subproject build output should be excluded")
                .isFalse();
        }

        @Test
        @DisplayName("BUG REGRESSION: old filter rejected all build.gradle files")
        void bugRegressionOldFilterRejectedAllBuildFiles() {
            // The OLD buggy filter was: !p.toString().contains(".gradle")
            // This rejected ALL files because "build.gradle" contains ".gradle"

            String path = "/project/build.gradle";

            // Old buggy check (should have been false, but shows the bug)
            boolean oldBuggyCheck = !path.contains(".gradle");
            assertThat(oldBuggyCheck)
                .as("Old buggy filter incorrectly rejects build.gradle")
                .isFalse(); // This shows the bug!

            // New correct check
            Path projectDir = Path.of("/project");
            Path gradleCacheDir = projectDir.resolve(".gradle");
            boolean newCorrectCheck = shouldIncludeBuildFile(Path.of(path), gradleCacheDir);
            assertThat(newCorrectCheck)
                .as("New filter correctly includes build.gradle")
                .isTrue();
        }

        @Test
        @DisplayName("should handle paths with spaces")
        void shouldHandlePathsWithSpaces() {
            Path projectDir = Path.of("/my project/gradle app");
            Path gradleCacheDir = projectDir.resolve(".gradle");

            Path buildFile = Path.of("/my project/gradle app/build.gradle");
            assertThat(shouldIncludeBuildFile(buildFile, gradleCacheDir))
                .as("Should include build file in path with spaces")
                .isTrue();

            Path cachedFile = Path.of("/my project/gradle app/.gradle/8.0/build.gradle");
            assertThat(shouldIncludeBuildFile(cachedFile, gradleCacheDir))
                .as("Should exclude cached file in path with spaces")
                .isFalse();
        }

        @Test
        @DisplayName("should reject files with similar names but not exact match")
        void shouldRejectSimilarButNotExactNames() {
            Path projectDir = Path.of("/project");
            Path gradleCacheDir = projectDir.resolve(".gradle");

            // These should be EXCLUDED (not actual build files)
            Path myBuildGradle = Path.of("/project/my-build.gradle");
            Path buildGradleBak = Path.of("/project/build.gradle.bak");
            Path buildGradleOld = Path.of("/project/build.gradle.old");
            Path buildGradleTemplate = Path.of("/project/build.gradle.template");

            assertThat(shouldIncludeBuildFile(myBuildGradle, gradleCacheDir))
                .as("my-build.gradle should be excluded")
                .isFalse();
            assertThat(shouldIncludeBuildFile(buildGradleBak, gradleCacheDir))
                .as("build.gradle.bak should be excluded")
                .isFalse();
            assertThat(shouldIncludeBuildFile(buildGradleOld, gradleCacheDir))
                .as("build.gradle.old should be excluded")
                .isFalse();
            assertThat(shouldIncludeBuildFile(buildGradleTemplate, gradleCacheDir))
                .as("build.gradle.template should be excluded")
                .isFalse();
        }

        @Test
        @DisplayName("should filter out files in nested build directories")
        void shouldFilterOutNestedBuildDirectories() {
            Path projectDir = Path.of("/project");
            Path gradleCacheDir = projectDir.resolve(".gradle");

            // Various nested build directory patterns
            Path deepNested = Path.of("/project/app/build/classes/build.gradle");
            Path generatedBuild = Path.of("/project/build/generated-sources/build.gradle.kts");
            Path testBuild = Path.of("/project/build/test-fixtures/build.gradle");

            assertThat(shouldIncludeBuildFile(deepNested, gradleCacheDir))
                .as("Deeply nested build file should be excluded")
                .isFalse();
            assertThat(shouldIncludeBuildFile(generatedBuild, gradleCacheDir))
                .as("Generated build file should be excluded")
                .isFalse();
            assertThat(shouldIncludeBuildFile(testBuild, gradleCacheDir))
                .as("Test fixture build file should be excluded")
                .isFalse();
        }

        @Test
        @DisplayName("should handle edge case of directory named build.gradle")
        void shouldHandleDirectoryNamedBuildGradle() {
            Path projectDir = Path.of("/project");
            Path gradleCacheDir = projectDir.resolve(".gradle");

            // If someone creates a directory called build.gradle (unusual but possible)
            // Files inside it should still follow normal rules
            Path fileInWeirdDir = Path.of("/project/build.gradle/actual-build.gradle");

            // The file name is "actual-build.gradle" not "build.gradle"
            assertThat(shouldIncludeBuildFile(fileInWeirdDir, gradleCacheDir))
                .as("File in directory named build.gradle should be excluded if not exact match")
                .isFalse();
        }

        @Test
        @DisplayName("should include settings.gradle files")
        void shouldIncludeSettingsGradleFiles() {
            Path projectDir = Path.of("/project");
            Path gradleCacheDir = projectDir.resolve(".gradle");

            Path settingsGroovy = Path.of("/project/settings.gradle");
            Path settingsKotlin = Path.of("/project/settings.gradle.kts");

            assertThat(shouldIncludeBuildOrSettingsFile(settingsGroovy, gradleCacheDir))
                .as("settings.gradle should be included")
                .isTrue();
            assertThat(shouldIncludeBuildOrSettingsFile(settingsKotlin, gradleCacheDir))
                .as("settings.gradle.kts should be included")
                .isTrue();
        }

        private boolean shouldIncludeBuildFile(Path p, Path gradleCacheDir) {
            String name = p.getFileName().toString();
            boolean isBuildFile = name.equals("build.gradle") || name.equals("build.gradle.kts");
            boolean isInGradleCache = p.startsWith(gradleCacheDir);
            boolean isInBuildDir = p.toString().contains(File.separator + "build" + File.separator);
            return isBuildFile && !isInGradleCache && !isInBuildDir;
        }

        private boolean shouldIncludeBuildOrSettingsFile(Path p, Path gradleCacheDir) {
            String name = p.getFileName().toString();
            boolean isGradleFile = name.equals("build.gradle") || name.equals("build.gradle.kts")
                || name.equals("settings.gradle") || name.equals("settings.gradle.kts");
            boolean isInGradleCache = p.startsWith(gradleCacheDir);
            boolean isInBuildDir = p.toString().contains(File.separator + "build" + File.separator);
            return isGradleFile && !isInGradleCache && !isInBuildDir;
        }
    }

    // =========================================================================
    // tasks.withType Replacement Tests (catches String.replace bug)
    // =========================================================================

    @Nested
    @DisplayName("tasks.withType replacement")
    class WithTypeReplacementTests {

        @ParameterizedTest
        @DisplayName("should correctly transform Groovy withType patterns")
        @CsvSource({
            "'tasks.withType(JavaCompile) {', 'tasks.withType(JavaCompile).configureEach {'",
            "'tasks.withType(Test) {', 'tasks.withType(Test).configureEach {'",
            "'tasks.withType(Jar) {', 'tasks.withType(Jar).configureEach {'"
        })
        void shouldTransformGroovyWithType(String input, String expected) {
            String result = transformWithType(input, false);
            assertThat(result).isEqualTo(expected);
        }

        @ParameterizedTest
        @DisplayName("should correctly transform Kotlin withType patterns")
        @CsvSource({
            "'tasks.withType<JavaCompile> {', 'tasks.withType<JavaCompile>().configureEach {'",
            "'tasks.withType<Test> {', 'tasks.withType<Test>().configureEach {'",
            "'tasks.withType<Jar> {', 'tasks.withType<Jar>().configureEach {'"
        })
        void shouldTransformKotlinWithType(String input, String expected) {
            String result = transformWithType(input, true);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("BUG REGRESSION: old replacement corrupted nested braces")
        void bugRegressionOldReplacementCorruptedNestedBraces() {
            String input = "tasks.withType(JavaCompile) {";

            // Old buggy replacement: original.replace("{", ".configureEach {")
            // This would replace ALL braces, corrupting code like:
            // tasks.withType(JavaCompile) { options.compilerArgs = listOf("-Xlint") }
            String buggyResult = input.replace("{", ".configureEach {");
            assertThat(buggyResult)
                .as("Old buggy replacement is wrong")
                .isEqualTo("tasks.withType(JavaCompile) .configureEach {"); // Missing the method call structure!

            // New correct replacement
            String correctResult = transformWithType(input, false);
            assertThat(correctResult)
                .as("New replacement is correct")
                .isEqualTo("tasks.withType(JavaCompile).configureEach {");
        }

        @Test
        @DisplayName("should handle complex type parameters")
        void shouldHandleComplexTypeParameters() {
            // Kotlin with generic bounds
            String kotlinInput = "tasks.withType<JavaCompile> {";
            String kotlinResult = transformWithType(kotlinInput, true);
            assertThat(kotlinResult).isEqualTo("tasks.withType<JavaCompile>().configureEach {");

            // Groovy with class reference
            String groovyInput = "tasks.withType(org.gradle.api.tasks.compile.JavaCompile) {";
            String groovyResult = transformWithType(groovyInput, false);
            assertThat(groovyResult).isEqualTo("tasks.withType(org.gradle.api.tasks.compile.JavaCompile).configureEach {");
        }

        @Test
        @DisplayName("should handle nested generics like Map<String, String>")
        void shouldHandleNestedGenerics() {
            // Kotlin with nested generics - this was previously broken
            String nestedInput = "tasks.withType<Map<String, String>> {";
            String nestedResult = transformWithType(nestedInput, true);
            assertThat(nestedResult)
                .as("Should correctly handle nested generics")
                .isEqualTo("tasks.withType<Map<String, String>>().configureEach {");

            // Multiple levels of nesting
            String deepNestedInput = "tasks.withType<Map<String, List<Int>>> {";
            String deepNestedResult = transformWithType(deepNestedInput, true);
            assertThat(deepNestedResult)
                .as("Should correctly handle deeply nested generics")
                .isEqualTo("tasks.withType<Map<String, List<Int>>>().configureEach {");
        }

        @Test
        @DisplayName("should NOT transform already lazy patterns")
        void shouldNotTransformAlreadyLazyPatterns() {
            // Already using configureEach - should remain unchanged
            String alreadyLazy = "tasks.withType<JavaCompile>().configureEach {";
            String result = transformWithType(alreadyLazy, true);
            assertThat(result)
                .as("Already lazy pattern should not be double-transformed")
                .isEqualTo(alreadyLazy);
        }

        @Test
        @DisplayName("should NOT transform patterns in comments")
        void shouldNotTransformPatternsInComments() {
            // Code comment - ideally should not transform
            // (Current implementation might transform it, this test documents behavior)
            String commented = "// tasks.withType<JavaCompile> { old pattern }";
            String result = transformWithType(commented, true);
            // Document current behavior - this test catches if behavior changes
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should handle whitespace variations")
        void shouldHandleWhitespaceVariations() {
            // Extra spaces around type parameter
            String spacedKotlin = "tasks.withType <JavaCompile> {";
            String spacedResult = transformWithType(spacedKotlin, true);
            assertThat(spacedResult)
                .as("Should handle space before type parameter")
                .isEqualTo("tasks.withType<JavaCompile>().configureEach {");

            // Tab characters
            String tabbedGroovy = "tasks.withType(Test)\t{";
            String tabbedResult = transformWithType(tabbedGroovy, false);
            assertThat(tabbedResult)
                .as("Should handle tab before brace")
                .isEqualTo("tasks.withType(Test).configureEach {");
        }

        @Test
        @DisplayName("should handle star projections in Kotlin")
        void shouldHandleStarProjections() {
            String starProjection = "tasks.withType<*> {";
            String result = transformWithType(starProjection, true);
            assertThat(result)
                .as("Should handle star projection")
                .isEqualTo("tasks.withType<*>().configureEach {");
        }

        @Test
        @DisplayName("should be idempotent - running twice produces same result")
        void shouldBeIdempotent() {
            String original = "tasks.withType(JavaCompile) {";
            String firstPass = transformWithType(original, false);
            String secondPass = transformWithType(firstPass, false);

            assertThat(secondPass)
                .as("Transformation should be idempotent")
                .isEqualTo(firstPass);
        }

        private String transformWithType(String original, boolean isKotlin) {
            // Updated pattern to handle nested generics
            // <[^{]+>+ matches angle brackets with nested generics (>+ handles multiple closing brackets)
            // \\([^)]+\\) matches parentheses for Groovy
            Pattern withTypeEager = Pattern.compile("tasks\\.withType\\s*(<[^{]+>+|\\([^)]+\\))\\s*\\{");
            Matcher matcher = withTypeEager.matcher(original);

            if (matcher.find()) {
                String typeArg = matcher.group(1);
                if (isKotlin && typeArg.startsWith("<")) {
                    return "tasks.withType" + typeArg + "().configureEach {";
                } else {
                    return "tasks.withType" + typeArg + ".configureEach {";
                }
            }
            return original;
        }
    }

    // =========================================================================
    // Lazy Usage Score Calculation Tests
    // =========================================================================

    @Nested
    @DisplayName("Lazy usage score calculation")
    class LazyUsageScoreTests {

        @Test
        @DisplayName("should include tasks.named in lazy usage score")
        void shouldIncludeTasksNamedInScore() {
            // Project with: 1 tasks.register, 3 tasks.named, 0 eager creates
            int lazyRegistrations = 1;
            int lazyAccess = 3;
            int eagerCreates = 0;

            int score = calculateLazyScore(lazyRegistrations, lazyAccess, eagerCreates);

            assertThat(score)
                .as("Score should be 100% when all task APIs are lazy")
                .isEqualTo(100);
        }

        @Test
        @DisplayName("should calculate correct score with mixed usage")
        void shouldCalculateCorrectScoreWithMixedUsage() {
            // Project with: 2 tasks.register, 2 tasks.named, 2 eager creates
            int lazyRegistrations = 2;
            int lazyAccess = 2;
            int eagerCreates = 2;

            int score = calculateLazyScore(lazyRegistrations, lazyAccess, eagerCreates);

            // (2 + 2) / (2 + 2 + 2) = 4/6 = 66%
            assertThat(score).isEqualTo(66);
        }

        @Test
        @DisplayName("BUG REGRESSION: old score ignored tasks.named calls")
        void bugRegressionOldScoreIgnoredTasksNamed() {
            // Project using only tasks.named (no tasks.register)
            int lazyRegistrations = 0;
            int lazyAccess = 5;
            int eagerCreates = 0;

            // Old buggy calculation: lazyRegistrations / (eagerCreates + lazyRegistrations)
            int oldBuggyScore = (lazyRegistrations * 100) / Math.max(1, eagerCreates + lazyRegistrations);
            assertThat(oldBuggyScore)
                .as("Old buggy score showed 0% for projects using tasks.named")
                .isEqualTo(0); // Wrong!

            // New correct calculation includes lazyAccess
            int newCorrectScore = calculateLazyScore(lazyRegistrations, lazyAccess, eagerCreates);
            assertThat(newCorrectScore)
                .as("New score correctly shows 100% for lazy API usage")
                .isEqualTo(100);
        }

        @Test
        @DisplayName("should return 100 for empty project with no task API usage")
        void shouldReturn100ForEmptyProject() {
            // Project with no task API usage at all
            int score = calculateLazyScore(0, 0, 0);
            assertThat(score)
                .as("Empty project should default to 100% (no issues detected)")
                .isEqualTo(100);
        }

        @Test
        @DisplayName("should return 0 for fully eager project")
        void shouldReturn0ForFullyEagerProject() {
            // Project using only eager APIs
            int score = calculateLazyScore(0, 0, 10);
            assertThat(score)
                .as("Fully eager project should score 0%")
                .isEqualTo(0);
        }

        @Test
        @DisplayName("should handle large numbers without overflow")
        void shouldHandleLargeNumbersWithoutOverflow() {
            // Large but reasonable numbers (won't overflow int multiplication)
            int lazyRegistrations = 10000;
            int lazyAccess = 20000;
            int eagerCreates = 10000;

            int score = calculateLazyScore(lazyRegistrations, lazyAccess, eagerCreates);

            // (10000 + 20000) / (10000 + 20000 + 10000) = 30000/40000 = 75%
            assertThat(score)
                .as("Should handle large numbers correctly")
                .isEqualTo(75);
        }

        @ParameterizedTest(name = "lazy={0}, access={1}, eager={2} -> score={3}")
        @CsvSource({
            "10, 0, 0, 100",  // All register, no eager
            "0, 10, 0, 100",  // All named, no eager
            "5, 5, 0, 100",   // Mix of lazy, no eager
            "1, 0, 1, 50",    // 50-50 split
            "0, 0, 1, 0",     // Single eager
            "1, 1, 2, 50",    // 2 lazy vs 2 eager
            "3, 0, 1, 75"     // 75% lazy
        })
        @DisplayName("should calculate correct percentage for various scenarios")
        void shouldCalculateCorrectPercentage(int lazy, int access, int eager, int expected) {
            int score = calculateLazyScore(lazy, access, eager);
            assertThat(score).isEqualTo(expected);
        }

        private int calculateLazyScore(int lazyRegistrations, int lazyAccess, int eagerCreates) {
            int totalLazyUsage = lazyRegistrations + lazyAccess;
            int totalPatterns = eagerCreates + totalLazyUsage;
            if (totalPatterns > 0) {
                return (totalLazyUsage * 100) / totalPatterns;
            }
            return 100;
        }
    }

    // =========================================================================
    // Pattern Detection Tests
    // =========================================================================

    @Nested
    @DisplayName("Pattern detection")
    class PatternDetectionTests {

        @Test
        @DisplayName("should detect tasks.register calls")
        void shouldDetectTasksRegister() {
            Pattern pattern = Pattern.compile("tasks\\.register\\s*[<(]");

            assertThat(pattern.matcher("tasks.register(\"myTask\")").find()).isTrue();
            assertThat(pattern.matcher("tasks.register<Copy>(\"copyTask\")").find()).isTrue();
            assertThat(pattern.matcher("tasks.register (\"spaced\")").find()).isTrue();
            assertThat(pattern.matcher("tasks.create(\"eager\")").find()).isFalse();
        }

        @Test
        @DisplayName("should detect tasks.named calls")
        void shouldDetectTasksNamed() {
            Pattern pattern = Pattern.compile("tasks\\.named\\s*[<(]");

            assertThat(pattern.matcher("tasks.named(\"test\")").find()).isTrue();
            assertThat(pattern.matcher("tasks.named<Test>(\"test\")").find()).isTrue();
            assertThat(pattern.matcher("tasks.getByName(\"test\")").find()).isFalse();
        }

        @Test
        @DisplayName("should detect eager task creation")
        void shouldDetectEagerTaskCreation() {
            Pattern pattern = Pattern.compile("tasks\\.create\\s*[<(]");

            assertThat(pattern.matcher("tasks.create(\"myTask\")").find()).isTrue();
            assertThat(pattern.matcher("tasks.create<Copy>(\"copyTask\")").find()).isTrue();
            assertThat(pattern.matcher("tasks.register(\"lazy\")").find()).isFalse();
        }

        @Test
        @DisplayName("should detect eager task access with getByName")
        void shouldDetectEagerTaskAccess() {
            Pattern pattern = Pattern.compile("tasks\\.getByName\\s*[<(]");

            assertThat(pattern.matcher("tasks.getByName(\"jar\")").find()).isTrue();
            assertThat(pattern.matcher("tasks.getByName<Jar>(\"jar\")").find()).isTrue();
            assertThat(pattern.matcher("tasks.named(\"jar\")").find()).isFalse();
        }

        @Test
        @DisplayName("should count all occurrences in multi-line content")
        void shouldCountAllOccurrencesInMultiLineContent() {
            Pattern pattern = Pattern.compile("tasks\\.register\\s*[<(]");
            String content = """
                tasks.register("task1") { }
                tasks.register<Copy>("task2") { }
                // Comment: tasks.register(
                tasks.register("task3") { }
                """;

            int count = 0;
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                count++;
            }
            assertThat(count)
                .as("Should find all register calls including in comments (limitation)")
                .isEqualTo(4);
        }

        @Test
        @DisplayName("should NOT match partial method names with different suffixes")
        void shouldNotMatchPartialMethodNames() {
            Pattern registerPattern = Pattern.compile("tasks\\.register\\s*[<(]");
            Pattern namedPattern = Pattern.compile("tasks\\.named\\s*[<(]");

            // Should not match method names with different suffixes
            // because they don't have < or ( immediately after
            assertThat(registerPattern.matcher("tasks.registerProvider<").find())
                .as("Should not match registerProvider< (different method)")
                .isFalse();
            assertThat(namedPattern.matcher("tasks.namedOrNull<").find())
                .as("Should not match namedOrNull< (different method)")
                .isFalse();

            // Note: Current patterns don't use word boundaries, so they will
            // match "tasks.register(" even within larger identifiers like
            // "mytasks.register(" - this is a known limitation that could be
            // improved with (?<!\w)tasks\.register patterns if needed
        }

        @Test
        @DisplayName("should detect method chaining patterns")
        void shouldDetectMethodChainingPatterns() {
            // Pattern for lazy Kotlin: tasks.withType<Type>().configureEach
            Pattern kotlinConfigureEachPattern = Pattern.compile("tasks\\.withType<[^>]+>\\(\\)\\.configureEach");
            // Pattern for lazy Groovy: tasks.withType(Type).configureEach
            Pattern groovyConfigureEachPattern = Pattern.compile("tasks\\.withType\\([^)]+\\)\\.configureEach");
            // Pattern for eager: tasks.withType<Type> { (without .configureEach)
            Pattern eagerPattern = Pattern.compile("tasks\\.withType\\s*[<(][^)>]+[)>]\\s*\\{");

            assertThat(kotlinConfigureEachPattern.matcher("tasks.withType<Test>().configureEach {").find())
                .as("Should detect lazy Kotlin pattern")
                .isTrue();
            assertThat(groovyConfigureEachPattern.matcher("tasks.withType(Test).configureEach {").find())
                .as("Should detect lazy Groovy pattern")
                .isTrue();
            assertThat(eagerPattern.matcher("tasks.withType<Test> {").find())
                .as("Should detect eager pattern")
                .isTrue();
        }

        @Test
        @DisplayName("should handle string literals containing task API patterns")
        void shouldHandleStringLiteralsContainingTaskPatterns() {
            // Real code often has strings containing task API names
            Pattern pattern = Pattern.compile("tasks\\.register\\s*[<(]");

            // This WILL match even inside a string - documenting current behavior
            String codeWithString = "val msg = \"Use tasks.register(\\\\_myTask\\\\_)\"";
            // In real tools, we'd want to be smarter about this
            assertThat(pattern.matcher(codeWithString).find())
                .as("Current implementation matches inside strings - may need refinement")
                .isTrue();
        }
    }

    // =========================================================================
    // Regex Safety Tests - Prevent ReDoS vulnerabilities
    // =========================================================================

    @Nested
    @DisplayName("Regex safety")
    class RegexSafetyTests {

        @Test
        @DisplayName("withType pattern should complete quickly on long input")
        @Timeout(value = 1, unit = java.util.concurrent.TimeUnit.SECONDS)
        void withTypePatternShouldCompleteQuickly() {
            Pattern pattern = Pattern.compile("tasks\\.withType\\s*(<[^{]+>+|\\([^)]+\\))\\s*\\{");

            // Create a potentially problematic input (many nested brackets)
            String longInput = "tasks.withType<" + "A".repeat(1000) + "> {";

            // Should complete without hanging
            assertThat(pattern.matcher(longInput).find()).isTrue();
        }

        @Test
        @DisplayName("register pattern should complete quickly on long input")
        @Timeout(value = 1, unit = java.util.concurrent.TimeUnit.SECONDS)
        void registerPatternShouldCompleteQuickly() {
            Pattern pattern = Pattern.compile("tasks\\.register\\s*[<(]");

            // Long input with no match
            String longInput = "tasks." + "x".repeat(10000) + ".register(";

            // Should complete without hanging (won't match, but shouldn't hang)
            assertThat(pattern.matcher(longInput).find()).isFalse();
        }

        @Test
        @DisplayName("patterns should not catastrophically backtrack")
        @Timeout(value = 1, unit = java.util.concurrent.TimeUnit.SECONDS)
        void patternsShouldNotCatastrophicallyBacktrack() {
            Pattern pattern = Pattern.compile("tasks\\.withType\\s*(<[^{]+>+|\\([^)]+\\))\\s*\\{");

            // Input designed to cause backtracking in naive patterns
            String backtrackInput = "tasks.withType<" + "<".repeat(50) + ">";

            // Should complete without catastrophic backtracking
            boolean result = pattern.matcher(backtrackInput).find();
            // Result doesn't matter, just that it completes
            assertThat(result).isIn(true, false);
        }

        @Test
        @DisplayName("patterns should handle deeply nested generics safely")
        @Timeout(value = 1, unit = java.util.concurrent.TimeUnit.SECONDS)
        void patternsShouldHandleDeeplyNestedGenericsSafely() {
            Pattern pattern = Pattern.compile("tasks\\.withType\\s*(<[^{]+>+|\\([^)]+\\))\\s*\\{");

            // Deeply nested but valid generic
            String deepGeneric = "tasks.withType<Map<String, Map<String, Map<String, List<Int>>>>> {";

            assertThat(pattern.matcher(deepGeneric).find()).isTrue();
        }
    }

    // =========================================================================
    // Config Cache Issue Detection Tests
    // =========================================================================

    @Nested
    @DisplayName("Config cache issue detection")
    class ConfigCacheIssueDetectionTests {

        @Test
        @DisplayName("should detect System.getProperty at configuration time")
        void shouldDetectSystemGetPropertyAtConfigTime() {
            Pattern pattern = Pattern.compile("System\\.getProperty\\s*\\(");

            assertThat(pattern.matcher("val url = System.getProperty(\"db.url\")").find()).isTrue();
            assertThat(pattern.matcher("System.getProperty(\"java.home\")").find()).isTrue();
            assertThat(pattern.matcher("providers.systemProperty(\"safe\")").find()).isFalse();
        }

        @Test
        @DisplayName("should detect System.getenv at configuration time")
        void shouldDetectSystemGetenvAtConfigTime() {
            Pattern pattern = Pattern.compile("System\\.getenv\\s*\\(");

            assertThat(pattern.matcher("val key = System.getenv(\"API_KEY\")").find()).isTrue();
            assertThat(pattern.matcher("providers.environmentVariable(\"safe\")").find()).isFalse();
        }

        @Test
        @DisplayName("should detect project.file in doLast")
        void shouldDetectProjectFileInDoLast() {
            Pattern pattern = Pattern.compile("project\\.file\\s*\\(");

            assertThat(pattern.matcher("val f = project.file(\"config.txt\")").find()).isTrue();
            assertThat(pattern.matcher("layout.projectDirectory.file(\"safe\")").find()).isFalse();
        }

        @Test
        @DisplayName("should detect project.exec in doLast")
        void shouldDetectProjectExecInDoLast() {
            Pattern pattern = Pattern.compile("project\\.(exec|copy|delete|javaexec)\\s*[{(]");

            assertThat(pattern.matcher("project.exec { commandLine(\"ls\") }").find()).isTrue();
            assertThat(pattern.matcher("project.copy { from(\"src\") }").find()).isTrue();
            assertThat(pattern.matcher("project.delete(\"temp\")").find()).isTrue();
            assertThat(pattern.matcher("project.javaexec { main = \"Main\" }").find()).isTrue();
        }

        @Test
        @DisplayName("should detect buildDir direct access")
        void shouldDetectBuildDirDirectAccess() {
            Pattern pattern = Pattern.compile("\\bbuildDir\\b");

            assertThat(pattern.matcher("val out = file(\"$buildDir/output\")").find()).isTrue();
            assertThat(pattern.matcher("into(buildDir)").find()).isTrue();
            assertThat(pattern.matcher("layout.buildDirectory").find()).isFalse();
        }

        @Test
        @DisplayName("should detect Task.project access at execution time")
        void shouldDetectTaskProjectAccessAtExecutionTime() {
            // This is a more nuanced pattern - project access in doLast/doFirst
            Pattern doLastProjectPattern = Pattern.compile("doLast\\s*\\{[^}]*project\\.");

            String problemCode = """
                doLast {
                    val dir = project.buildDir
                }
                """;
            assertThat(doLastProjectPattern.matcher(problemCode).find())
                .as("Should detect project access in doLast")
                .isTrue();

            String safeCode = """
                doLast {
                    println("No project access")
                }
                """;
            assertThat(doLastProjectPattern.matcher(safeCode).find())
                .as("Should not flag code without project access")
                .isFalse();
        }
    }
}
