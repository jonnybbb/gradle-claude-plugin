package com.example;

/**
 * Simple calculator class for demonstrating test failures.
 */
public class Calculator {

    public int add(int a, int b) {
        return a + b;
    }

    public int subtract(int a, int b) {
        return a - b;
    }

    public int multiply(int a, int b) {
        return a * b;
    }

    public int divide(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return a / b;
    }

    /**
     * Intentionally buggy method - returns wrong result.
     * Used for demonstrating test assertion failures.
     */
    public int buggySquare(int n) {
        // Bug: should be n * n, but returns n + n
        return n + n;
    }
}
