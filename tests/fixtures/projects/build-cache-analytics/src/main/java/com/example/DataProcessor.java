package com.example;

import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;

/**
 * Data processor for demonstrating cacheable operations.
 */
public class DataProcessor {

    /**
     * Process input data - deterministic for caching.
     */
    public String process(String input) {
        if (Strings.isNullOrEmpty(input)) {
            return "empty";
        }

        // Compute hash of input for deterministic output
        String hash = Hashing.sha256()
            .hashString(input, StandardCharsets.UTF_8)
            .toString()
            .substring(0, 8);

        return String.format("processed_%s_%s", input.length(), hash);
    }

    /**
     * Transform data with configurable operation.
     */
    public String transform(String input, TransformType type) {
        return switch (type) {
            case UPPERCASE -> input.toUpperCase();
            case LOWERCASE -> input.toLowerCase();
            case REVERSE -> new StringBuilder(input).reverse().toString();
            case HASH -> Hashing.sha256()
                .hashString(input, StandardCharsets.UTF_8)
                .toString();
        };
    }

    public enum TransformType {
        UPPERCASE, LOWERCASE, REVERSE, HASH
    }
}
