package com.example;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that consumes memory for demonstrating OOM failures.
 */
public class MemoryHog {

    /**
     * Allocates large amounts of memory to trigger OutOfMemoryError.
     * Use with limited heap size (e.g., -Xmx16m) to cause OOM.
     */
    public void consumeMemory() {
        List<byte[]> memoryBlocks = new ArrayList<>();
        while (true) {
            // Allocate 1MB blocks until OOM
            memoryBlocks.add(new byte[1024 * 1024]);
        }
    }

    /**
     * Creates deeply nested objects to exhaust stack.
     */
    public long recursiveCount(long n) {
        if (n <= 0) return 0;
        return 1 + recursiveCount(n - 1);
    }
}
