package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Calculator - includes intentional assertion failures when FAILURE_TYPE=test.
 */
class CalculatorTest {

    private final Calculator calculator = new Calculator();

    @Test
    @DisplayName("Addition works correctly")
    void testAdd() {
        assertEquals(5, calculator.add(2, 3));
    }

    @Test
    @DisplayName("Subtraction works correctly")
    void testSubtract() {
        assertEquals(2, calculator.subtract(5, 3));
    }

    @Test
    @DisplayName("Division by zero throws exception")
    void testDivideByZero() {
        assertThrows(ArithmeticException.class, () -> calculator.divide(10, 0));
    }

    @Test
    @DisplayName("Buggy square function (FAILS when FAILURE_TYPE=test)")
    @EnabledIfSystemProperty(named = "FAILURE_TYPE", matches = "test")
    void testBuggySquare_shouldFail() {
        // This test will FAIL because buggySquare returns n+n instead of n*n
        int result = calculator.buggySquare(5);
        assertEquals(25, result,
            "Expected 5^2 = 25 but got " + result + " (bug: returns n+n instead of n*n)");
    }

    @Test
    @DisplayName("Buggy square function - demonstrating the bug")
    @EnabledIfSystemProperty(named = "FAILURE_TYPE", matches = "test")
    void testBuggySquare_anotherFailure() {
        // Another failing test to demonstrate multiple test failures
        assertEquals(100, calculator.buggySquare(10),
            "Expected 10^2 = 100 but buggy implementation returns 20");
    }
}
