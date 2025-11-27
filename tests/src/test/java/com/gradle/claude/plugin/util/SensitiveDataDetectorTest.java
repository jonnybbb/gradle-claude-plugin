package com.gradle.claude.plugin.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for SensitiveDataDetector utility.
 */
@Tag("utils")
@DisplayName("SensitiveDataDetector Tests")
class SensitiveDataDetectorTest {

    // =========================================================================
    // Tests for detecting actual secrets
    // =========================================================================

    @Test
    @DisplayName("Should detect API key assignments")
    void shouldDetectApiKeyAssignments() {
        String content = "api_key = 'sk-1234567890abcdef1234567890abcdef'";

        assertThat(SensitiveDataDetector.isClean(content))
            .as("Should detect API key")
            .isFalse();
    }

    @Test
    @DisplayName("Should detect AWS credentials")
    void shouldDetectAwsCredentials() {
        String content = "AWS_SECRET_ACCESS_KEY=verysecretkey123";

        assertThat(SensitiveDataDetector.isClean(content))
            .as("Should detect AWS secret key")
            .isFalse();
    }

    @Test
    @DisplayName("Should detect AWS Access Key ID pattern")
    void shouldDetectAwsAccessKeyId() {
        String content = "aws_access_key_id = AKIAIOSFODNN7EXAMPLE";

        assertThat(SensitiveDataDetector.isClean(content))
            .as("Should detect AWS access key ID")
            .isFalse();
    }

    @Test
    @DisplayName("Should detect private key headers")
    void shouldDetectPrivateKeyHeaders() {
        String content = "-----BEGIN RSA PRIVATE KEY-----\nMIIE...";

        assertThat(SensitiveDataDetector.isClean(content))
            .as("Should detect private key header")
            .isFalse();
    }

    @Test
    @DisplayName("Should detect database connection strings with passwords")
    void shouldDetectDbConnectionStrings() {
        String content = "jdbc:postgresql://user:password123@localhost:5432/db";

        assertThat(SensitiveDataDetector.isClean(content))
            .as("Should detect DB connection string with password")
            .isFalse();
    }

    @Test
    @DisplayName("Should detect password assignments")
    void shouldDetectPasswordAssignments() {
        String content = "password = 'supersecretpassword123'";

        assertThat(SensitiveDataDetector.isClean(content))
            .as("Should detect password assignment")
            .isFalse();
    }

    @Test
    @DisplayName("Should detect GitHub tokens")
    void shouldDetectGitHubTokens() {
        String content = "token = ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

        assertThat(SensitiveDataDetector.isClean(content))
            .as("Should detect GitHub token")
            .isFalse();
    }

    // =========================================================================
    // Tests for allowlisting (avoiding false positives)
    // =========================================================================

    @Test
    @DisplayName("Should allow example/placeholder values")
    void shouldAllowExampleValues() {
        String content = "api_key = 'example-api-key-placeholder'";

        assertThat(SensitiveDataDetector.isClean(content))
            .as("Should allow example values")
            .isTrue();
    }

    @Test
    @DisplayName("Should allow test/mock values")
    void shouldAllowTestValues() {
        String content = "password = 'test-password-for-unit-tests'";

        assertThat(SensitiveDataDetector.isClean(content))
            .as("Should allow test values")
            .isTrue();
    }

    @Test
    @DisplayName("Should allow default placeholder values")
    void shouldAllowDefaultPlaceholders() {
        String content = "secret = 'default_secret'";

        assertThat(SensitiveDataDetector.isClean(content))
            .as("Should allow default placeholder")
            .isTrue();
    }

    @Test
    @DisplayName("Should allow Gradle property references")
    void shouldAllowGradlePropertyReferences() {
        String content = "password = ${DB_PASSWORD}";

        assertThat(SensitiveDataDetector.isClean(content))
            .as("Should allow property references")
            .isTrue();
    }

    @Test
    @DisplayName("Should allow environment variable references")
    void shouldAllowEnvVarReferences() {
        String content = "api_key = $API_KEY";

        assertThat(SensitiveDataDetector.isClean(content))
            .as("Should allow env var references")
            .isTrue();
    }

    // =========================================================================
    // Tests for clean content
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {
        "plugins { java }",
        "version = '1.0.0'",
        "group = 'com.example'",
        "dependencies { implementation 'org.example:lib:1.0' }",
        "tasks.register('build') { }",
        "// This is a comment about passwords",
        "val dbUrl = System.getProperty(\"db.url\")"
    })
    @DisplayName("Should not flag clean Gradle build files")
    void shouldNotFlagCleanContent(String content) {
        assertThat(SensitiveDataDetector.isClean(content))
            .as("Should not flag: %s", content)
            .isTrue();
    }

    // =========================================================================
    // Tests for detection details
    // =========================================================================

    @Test
    @DisplayName("Should return detected pattern descriptions")
    void shouldReturnPatternDescriptions() {
        String content = "api_key = 'sk-1234567890abcdef1234567890abcdef'";

        var detected = SensitiveDataDetector.detect(content);

        assertThat(detected)
            .isNotEmpty()
            .extracting(SensitiveDataDetector.SensitivePattern::description)
            .contains("API key assignment");
    }

    @Test
    @DisplayName("Should detect multiple patterns in same content")
    void shouldDetectMultiplePatterns() {
        String content = """
            api_key = 'sk-1234567890abcdef1234567890abcdef'
            -----BEGIN PRIVATE KEY-----
            MIIEvQIBADANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA
            """;

        var detected = SensitiveDataDetector.detect(content);

        assertThat(detected.size())
            .as("Should detect multiple patterns")
            .isGreaterThanOrEqualTo(2);
    }
}
