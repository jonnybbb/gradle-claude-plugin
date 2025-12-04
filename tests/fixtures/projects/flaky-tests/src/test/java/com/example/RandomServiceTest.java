package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that demonstrate flaky behavior due to random values.
 * These tests will sometimes pass and sometimes fail.
 */
class RandomServiceTest {

    private final RandomService service = new RandomService();

    @Test
    @DisplayName("Random percentage should be above 20 (flaky - ~20% failure rate)")
    void testRandomPercentageAboveThreshold() {
        // This test is flaky because it expects random value > 20
        // but 20% of the time the value will be <= 20
        int value = service.getRandomPercentage();
        assertTrue(value > 20,
            "Expected value > 20 but got " + value);
    }

    @Test
    @DisplayName("Unreliable operation should succeed (flaky - ~30% failure rate)")
    void testUnreliableOperationSucceeds() {
        // This test is flaky because operation fails ~30% of time
        boolean result = service.performUnreliableOperation();
        assertTrue(result,
            "Unreliable operation failed unexpectedly");
    }

    @Test
    @DisplayName("Value near threshold should be above threshold (flaky - ~50% failure rate)")
    void testValueAboveThreshold() {
        // This test is flaky because value is equally likely to be
        // above or below the threshold
        int threshold = 50;
        int value = service.getValueNearThreshold(threshold);
        assertTrue(value >= threshold,
            "Expected value >= " + threshold + " but got " + value);
    }

    @Test
    @DisplayName("Timed operation should complete under 100ms (flaky - timing dependent)")
    void testTimedOperationFast() throws InterruptedException {
        // This test is flaky because operation takes 50-200ms randomly
        // so it fails when random delay > 100ms (~50% of time)
        long duration = service.performTimedOperation();
        assertTrue(duration < 100,
            "Expected operation to complete under 100ms but took " + duration + "ms");
    }

    @Test
    @DisplayName("Stable test - seeded random for comparison")
    void testWithSeededRandom() {
        // This test is stable because it uses seeded random
        RandomService seededService = new RandomService(42L);
        int value = seededService.getRandomPercentage();
        // With seed 42, first value is deterministic
        assertNotNull(value);
        assertTrue(value >= 0 && value <= 100);
    }
}
