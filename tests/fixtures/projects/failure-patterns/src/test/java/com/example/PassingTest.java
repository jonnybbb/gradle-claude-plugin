package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that always pass - used as baseline for comparison.
 */
class PassingTest {

    @Test
    @DisplayName("Simple passing test 1")
    void testAlwaysPasses1() {
        assertTrue(true);
    }

    @Test
    @DisplayName("Simple passing test 2")
    void testAlwaysPasses2() {
        assertEquals(4, 2 + 2);
    }

    @Test
    @DisplayName("Simple passing test 3")
    void testAlwaysPasses3() {
        assertNotNull("not null");
    }
}
