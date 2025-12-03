package com.example;

public class ApiClient {
    private final String apiKey;

    public ApiClient() {
        this.apiKey = System.getenv("EXAMPLE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("EXAMPLE_API_KEY environment variable is required but not set."
            );
        }

        if (!apiKey.startsWith("prod_")) {
            throw new IllegalStateException(
                    "EXAMPLE_API_KEY must start with 'prod_' prefix. Got: " + maskKey(apiKey)
            );
        }
    }

    /**
     * Returns true if the client is properly configured.
     */
    public boolean isConfigured() {
        return apiKey != null && apiKey.startsWith("prod_");
    }

    /**
     * Returns the configured API key.
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Simulates making an API call.
     */
    public String callApi(String endpoint) {
        if (!isConfigured()) {
            throw new IllegalStateException("ApiClient is not properly configured");
        }
        // Simulate API response
        return "Response from " + endpoint + " (authenticated)";
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 4) {
            return "***";
        }
        return key.substring(0, 4) + "***";
    }
}
