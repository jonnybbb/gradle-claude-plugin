package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DataProcessor - demonstrates cacheable test execution.
 */
class DataProcessorTest {

    private final DataProcessor processor = new DataProcessor();

    @Test
    @DisplayName("Process returns deterministic output")
    void testProcessDeterministic() {
        String result1 = processor.process("test");
        String result2 = processor.process("test");
        assertEquals(result1, result2, "Same input should produce same output");
    }

    @Test
    @DisplayName("Process handles empty input")
    void testProcessEmpty() {
        assertEquals("empty", processor.process(""));
        assertEquals("empty", processor.process(null));
    }

    @Test
    @DisplayName("Transform uppercase works")
    void testTransformUppercase() {
        assertEquals("HELLO", processor.transform("hello", DataProcessor.TransformType.UPPERCASE));
    }

    @Test
    @DisplayName("Transform lowercase works")
    void testTransformLowercase() {
        assertEquals("hello", processor.transform("HELLO", DataProcessor.TransformType.LOWERCASE));
    }

    @Test
    @DisplayName("Transform reverse works")
    void testTransformReverse() {
        assertEquals("olleh", processor.transform("hello", DataProcessor.TransformType.REVERSE));
    }

    @Test
    @DisplayName("Transform hash is deterministic")
    void testTransformHash() {
        String hash1 = processor.transform("test", DataProcessor.TransformType.HASH);
        String hash2 = processor.transform("test", DataProcessor.TransformType.HASH);
        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length()); // SHA-256 produces 64 hex chars
    }
}
