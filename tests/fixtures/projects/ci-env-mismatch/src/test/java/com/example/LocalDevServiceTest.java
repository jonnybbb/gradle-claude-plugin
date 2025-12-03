package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LocalDevService.
 *
 * This test passes LOCALLY (where CI is not set)
 * but fails in CI (where CI=true is set).
 *
 * This is the opposite scenario from ApiClientTest:
 * - ApiClientTest: CI passes, LOCAL fails (missing API key)
 * - LocalDevServiceTest: LOCAL passes, CI fails (dev-only service)
 */
class LocalDevServiceTest {

    @Test
    void testLocalDevServiceInitialization() {
        // This will throw IllegalStateException in CI (where CI=true)
        LocalDevService service = new LocalDevService();
        assertTrue(service.isDevMode(), "Service should be in dev mode");
    }

    @Test
    void testLocalDevServiceCanRunTasks() {
        LocalDevService service = new LocalDevService();
        String result = service.runLocalTask("compile-proto");
        assertNotNull(result, "Task result should not be null");
        assertTrue(result.contains("dev mode"), "Result should indicate dev mode");
    }
}
