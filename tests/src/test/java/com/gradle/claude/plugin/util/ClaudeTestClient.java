package com.gradle.claude.plugin.util;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.*;

import java.util.*;

/**
 * Test client for interacting with Claude via the Anthropic SDK.
 * Used to verify that skills and agents produce valuable outputs.
 */
public class ClaudeTestClient implements AutoCloseable {

    private final AnthropicClient client;
    private final Model model;

    private static final Model DEFAULT_MODEL = Model.CLAUDE_HAIKU_4_5;

    public ClaudeTestClient() {
        this(System.getenv("ANTHROPIC_API_KEY"), DEFAULT_MODEL);
    }

    public ClaudeTestClient(String apiKey, Model model) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY environment variable not set");
        }
        this.client = AnthropicOkHttpClient.builder()
            .apiKey(apiKey)
            .build();
        this.model = model;
    }

    /**
     * Test a skill by providing its content and asking a question.
     * Returns Claude's response for validation.
     */
    public SkillTestResult testSkill(SkillLoader.Skill skill, String projectContext, String userQuestion) {
        String systemPrompt = buildSkillSystemPrompt(skill);

        String userMessage = String.format("""
            ## Project Context

            %s

            ## Question

            %s
            """, projectContext, userQuestion);

        Message response = client.messages().create(MessageCreateParams.builder()
            .model(model)
            .maxTokens(4096L)
            .system(systemPrompt)
            .addUserMessage(userMessage)
            .build());

        String responseText = extractTextContent(response);

        return new SkillTestResult(
            skill.name(),
            userQuestion,
            responseText,
            response.usage().inputTokens(),
            response.usage().outputTokens()
        );
    }

    /**
     * Test an agent workflow by simulating its behavior.
     */
    public AgentTestResult testAgent(String agentSystemPrompt, String projectContext, String task) {
        String userMessage = String.format("""
            ## Project to Analyze

            %s

            ## Task

            %s
            """, projectContext, task);

        Message response = client.messages().create(MessageCreateParams.builder()
            .model(model)
            .maxTokens(8192L)
            .system(agentSystemPrompt)
            .addUserMessage(userMessage)
            .build());

        String responseText = extractTextContent(response);

        return new AgentTestResult(
            task,
            responseText,
            response.usage().inputTokens(),
            response.usage().outputTokens()
        );
    }

    /**
     * Ask Claude to analyze a Gradle project for specific issues.
     */
    public AnalysisResult analyzeProject(String skillContent, String buildFileContent, IssueType issueType) {
        String systemPrompt = String.format("""
            You are a Gradle expert assistant. Use the following skill knowledge to analyze projects:

            %s

            Respond with a JSON array of issues found. Each issue should have:
            - "type": the issue category
            - "description": what the issue is
            - "line": approximate line number if known
            - "fix": how to fix it

            Only output the JSON array, no other text.
            """, skillContent);

        String userMessage = String.format("""
            Analyze this Gradle build file for %s issues:

            ```kotlin
            %s
            ```
            """, issueType.name().toLowerCase().replace("_", " "), buildFileContent);

        Message response = client.messages().create(MessageCreateParams.builder()
            .model(model)
            .maxTokens(4096L)
            .system(systemPrompt)
            .addUserMessage(userMessage)
            .build());

        String responseText = extractTextContent(response);

        return new AnalysisResult(issueType, responseText);
    }

    private String buildSkillSystemPrompt(SkillLoader.Skill skill) {
        return String.format("""
            You are a Gradle expert assistant with the following specialized knowledge:

            # %s

            %s

            Use this knowledge to help users with their Gradle questions.
            Provide specific, actionable guidance with code examples where appropriate.
            """, skill.name(), skill.getFullContent());
    }

    private String extractTextContent(Message response) {
        return response.content().stream()
            .flatMap(contentBlock -> contentBlock.text().stream())
            .map(TextBlock::text)
            .reduce("", (a, b) -> a + b);
    }

    @Override
    public void close() {
        // Client cleanup if needed
    }

    // Result types

    public record SkillTestResult(
        String skillName,
        String question,
        String response,
        long inputTokens,
        long outputTokens
    ) {
        /**
         * Check if response contains expected content.
         */
        public boolean containsAllOf(String... expectedTerms) {
            String lowerResponse = response.toLowerCase();
            return Arrays.stream(expectedTerms)
                .allMatch(term -> lowerResponse.contains(term.toLowerCase()));
        }

        /**
         * Check if response contains at least one of the expected terms.
         */
        public boolean containsAnyOf(String... expectedTerms) {
            String lowerResponse = response.toLowerCase();
            return Arrays.stream(expectedTerms)
                .anyMatch(term -> lowerResponse.contains(term.toLowerCase()));
        }

        /**
         * Check if response contains code examples.
         */
        public boolean hasCodeExamples() {
            return response.contains("```");
        }
    }

    public record AgentTestResult(
        String task,
        String response,
        long inputTokens,
        long outputTokens
    ) {
        /**
         * Check if response follows expected structure (has sections).
         */
        public boolean hasStructuredOutput() {
            return response.contains("##") || response.contains("===") ||
                   response.contains("1.") || response.contains("Phase");
        }

        /**
         * Check if response contains severity indicators.
         */
        public boolean hasSeverityIndicators() {
            return response.contains("✅") || response.contains("⚠️") ||
                   response.contains("❌") || response.contains("🔴") ||
                   response.contains("Critical") || response.contains("Warning");
        }
    }

    public record AnalysisResult(
        IssueType issueType,
        String rawResponse
    ) {
        /**
         * Count issues found in the response.
         */
        public int countIssuesFound() {
            // Simple heuristic: count array elements
            return (int) rawResponse.chars().filter(c -> c == '{').count();
        }
    }

    public enum IssueType {
        CONFIG_CACHE,
        EAGER_TASK,
        DEPRECATED_API,
        DEPENDENCY_CONFLICT,
        PERFORMANCE
    }
}
