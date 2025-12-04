package com.example;

import java.util.Random;

/**
 * Service that uses random behavior - demonstrates flaky test scenarios.
 */
public class RandomService {

    private final Random random;

    public RandomService() {
        // Use system time for randomness - causes flakiness
        this.random = new Random();
    }

    public RandomService(long seed) {
        // Seeded random for deterministic tests
        this.random = new Random(seed);
    }

    /**
     * Returns a value between 0 and 100.
     * Due to randomness, tests asserting specific ranges may fail intermittently.
     */
    public int getRandomPercentage() {
        return random.nextInt(101);
    }

    /**
     * Simulates an operation that occasionally fails.
     * Roughly 30% failure rate.
     */
    public boolean performUnreliableOperation() {
        return random.nextDouble() > 0.3;
    }

    /**
     * Returns a value that is "usually" above threshold but not always.
     * Good target for flaky assertions.
     */
    public int getValueNearThreshold(int threshold) {
        // Returns value in range [threshold-10, threshold+10]
        return threshold - 10 + random.nextInt(21);
    }

    /**
     * Simulates timing-sensitive operation with random delay.
     */
    public long performTimedOperation() throws InterruptedException {
        long delay = 50 + random.nextInt(150); // 50-200ms
        Thread.sleep(delay);
        return delay;
    }
}
