package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiClientTest {

    @Test
    void testApiClientInitialization() {
        ApiClient client = new ApiClient();
        assertTrue(client.isConfigured(), "ApiClient should be configured");
    }

    @Test
    void testApiClientCanMakeCall() {
        ApiClient client = new ApiClient();
        String response = client.callApi("/users");
        assertNotNull(response, "API response should not be null");
        assertTrue(response.contains("authenticated"), "Response should indicate authentication");
    }
}
