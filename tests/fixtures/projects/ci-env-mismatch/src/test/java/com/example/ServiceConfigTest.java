package com.example;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Scenario: Local PASSES, CI FAILS
 * This test depends on a resource file that exists locally but might not be
 * properly packaged or available in the CI environment.
 */
class ServiceConfigTest {
    @Test
    void testConfigLoading() {
        InputStream stream = this.getClass().getResourceAsStream("/serviceconfig.json");
        assertNotNull(stream, "Config file not found!");
    }
}
