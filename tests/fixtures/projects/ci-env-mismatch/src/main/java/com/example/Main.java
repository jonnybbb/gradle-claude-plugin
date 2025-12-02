package com.example;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello from ci-env-mismatch fixture!");
        String ci = System.getenv("CI");
        System.out.println("CI environment: " + (ci != null ? ci : "not set"));
    }
}
