package com.gradle.claude.plugin.util;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility for detecting potentially sensitive data in test fixtures.
 * Uses patterns based on common secret detection tools (gitleaks, detect-secrets).
 *
 * This is intentionally conservative to avoid false positives in test code.
 */
public final class SensitiveDataDetector {

    private SensitiveDataDetector() {}

    /**
     * Patterns that indicate potentially sensitive data.
     * Each pattern is paired with a description for reporting.
     */
    public record SensitivePattern(Pattern pattern, String description) {}

    /**
     * Common patterns for sensitive data detection.
     * Based on patterns from gitleaks and detect-secrets.
     */
    public static final List<SensitivePattern> PATTERNS = List.of(
        // API Keys - generic patterns
        new SensitivePattern(
            Pattern.compile("(?i)(api[_-]?key|apikey)\\s*[=:]\\s*['\"][a-zA-Z0-9_\\-]{20,}['\"]"),
            "API key assignment"
        ),

        // AWS patterns
        new SensitivePattern(
            Pattern.compile("(?i)aws[_-]?(secret[_-]?access[_-]?key|access[_-]?key[_-]?id)\\s*[=:]"),
            "AWS credential"
        ),
        new SensitivePattern(
            Pattern.compile("AKIA[0-9A-Z]{16}"),
            "AWS Access Key ID"
        ),

        // Private keys
        new SensitivePattern(
            Pattern.compile("-----BEGIN (RSA |EC |DSA |OPENSSH )?PRIVATE KEY-----"),
            "Private key header"
        ),

        // Database connection strings with passwords
        // Handles: jdbc:postgresql://user:pass@host, mongodb://user:pass@host, etc.
        new SensitivePattern(
            Pattern.compile("(?i)(jdbc:[a-z0-9]+://|mongodb(\\+srv)?://|mysql://|postgres://|postgresql://|redis://|mariadb://)[^:]+:[^@]+@"),
            "Database connection string with credentials"
        ),

        // Generic secret assignments (but not "secret" as a value)
        new SensitivePattern(
            Pattern.compile("(?i)(secret|password|passwd|pwd)\\s*[=:]\\s*['\"][^'\"]{8,}['\"]"),
            "Secret/password assignment"
        ),

        // Bearer tokens
        new SensitivePattern(
            Pattern.compile("(?i)bearer\\s+[a-zA-Z0-9_\\-\\.]{20,}"),
            "Bearer token"
        ),

        // GitHub tokens
        new SensitivePattern(
            Pattern.compile("gh[pousr]_[A-Za-z0-9_]{36,}"),
            "GitHub token"
        ),

        // Slack tokens
        new SensitivePattern(
            Pattern.compile("xox[baprs]-[0-9]{10,}-[0-9]{10,}-[a-zA-Z0-9]{24}"),
            "Slack token"
        ),

        // Generic high-entropy strings that look like secrets
        new SensitivePattern(
            Pattern.compile("(?i)(token|secret|key|password)\\s*[=:]\\s*['\"][a-zA-Z0-9+/=]{32,}['\"]"),
            "High-entropy secret value"
        )
    );

    /**
     * Allowlist patterns - matches that should be ignored.
     * These are common false positives in test/example code.
     */
    public static final List<Pattern> ALLOWLIST = List.of(
        // Example/placeholder values
        Pattern.compile("(?i)(example|placeholder|dummy|test|mock|fake|sample)"),
        // Default values commonly used in tests
        Pattern.compile("(?i)default[-_]?(key|password|secret|token)"),
        // Gradle property references
        Pattern.compile("\\$\\{[^}]+\\}"),
        // Environment variable references
        Pattern.compile("\\$[A-Z_]+"),
        // Comments about passwords (not actual passwords)
        // Use line start anchor to avoid matching // in URLs like jdbc:postgresql://
        Pattern.compile("(^|\\s)//.*password|^#.*password|/\\*.*password", Pattern.MULTILINE)
    );

    /**
     * Check if content contains potentially sensitive data.
     *
     * @param content The content to check
     * @return List of detected sensitive patterns (empty if none found)
     */
    public static List<SensitivePattern> detect(String content) {
        return PATTERNS.stream()
            .filter(sp -> {
                var matcher = sp.pattern().matcher(content);
                while (matcher.find()) {
                    String match = matcher.group();
                    // Check if the match is allowlisted
                    boolean isAllowlisted = ALLOWLIST.stream()
                        .anyMatch(allowPattern -> allowPattern.matcher(match).find());
                    if (!isAllowlisted) {
                        return true;
                    }
                }
                return false;
            })
            .toList();
    }

    /**
     * Check if content is clean (no sensitive data detected).
     */
    public static boolean isClean(String content) {
        return detect(content).isEmpty();
    }
}
