package com.example;

/**
 * Service that is designed for local development only.
 * This service explicitly fails in CI environments to prevent
 * accidental use of development-only features in production pipelines.
 *
 * Scenario: LOCAL passes, CI fails
 * - Local developers can use this service freely
 * - CI builds should not use this service (it's dev-only tooling)
 */
public class LocalDevService {
    private final boolean devMode;

    /**
     * Creates a LocalDevService.
     *
     * @throws IllegalStateException if running in a CI environment (CI=true)
     */
    public LocalDevService() {
        String ciEnv = System.getenv("CI");
        if ("true".equalsIgnoreCase(ciEnv)) {
            throw new IllegalStateException(
                    "LocalDevService cannot be used in CI environments. " +
                    "This service is intended for local development only. " +
                    "CI detected: CI=" + ciEnv
            );
        }
        this.devMode = true;
    }

    /**
     * Returns true if running in development mode.
     */
    public boolean isDevMode() {
        return devMode;
    }

    /**
     * Simulates a local development operation.
     */
    public String runLocalTask(String taskName) {
        if (!devMode) {
            throw new IllegalStateException("Not in development mode");
        }
        return "Local task '" + taskName + "' completed (dev mode)";
    }
}
