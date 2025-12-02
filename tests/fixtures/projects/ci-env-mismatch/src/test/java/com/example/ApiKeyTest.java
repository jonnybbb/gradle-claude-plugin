package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Scenario: Local FAILS, CI PASSES
 * This test requires an environment variable that is set in CI but not locally.
 */
class ApiKeyTest {
    @Test
    void testPremiumFeatureIntegration() {
        // This variable should be set by the CI runner via secrets
        String apiKey = System.getenv("EXAMPLE_API_KEY");

        // The test fails locally because apiKey is null
        assertNotNull(apiKey, "Environment variable 'EXAMPLE_API_KEY' is not set!");

        // Example test logic
        assertTrue(apiKey.startsWith("prod_"), "API key format is incorrect.");
    }
}
