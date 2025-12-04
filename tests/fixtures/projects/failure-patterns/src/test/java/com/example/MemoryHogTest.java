package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that trigger resource exhaustion when FAILURE_TYPE=resource.
 */
class MemoryHogTest {

    private final MemoryHog memoryHog = new MemoryHog();

    @Test
    @DisplayName("Memory consumption test (FAILS with OOM when FAILURE_TYPE=resource)")
    @EnabledIfSystemProperty(named = "FAILURE_TYPE", matches = "resource")
    void testMemoryConsumption_shouldOOM() {
        // This test will cause OutOfMemoryError when run with -Xmx16m
        // The error will be captured in the build scan
        memoryHog.consumeMemory();
    }

    @Test
    @DisplayName("Normal memory test (passes)")
    void testNormalOperation() {
        // This test always passes - just verifies the class exists
        assertNotNull(memoryHog);
    }
}
