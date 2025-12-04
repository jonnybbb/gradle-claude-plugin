package com.gradle.claude.plugin.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test client that invokes the Claude CLI with MCP server support.
 * This enables real MCP tool execution (Develocity queries, DRV analytics, etc.)
 * for end-to-end testing of agents and skills.
 *
 * <p>Unlike ClaudeTestClient which calls the Anthropic API directly (and can't execute tools),
 * this client uses the Claude CLI which has full agent loop support with MCP tools.
 *
 * <h2>Required Environment Variables</h2>
 * <p>These must be set by the Gradle build (loaded from local.env or CI environment):
 * <ul>
 *   <li>DEVELOCITY_SERVER - Develocity server URL (e.g., https://develocity.grdev.net)</li>
 *   <li>DEVELOCITY_ACCESS_KEY - Access key for Develocity</li>
 *   <li>DRV_ACCESS_KEY - Access key for DRV</li>
 * </ul>
 * <p>Note: Claude CLI uses the user's configured API key (e.g., via AWS Bedrock).
 */
public class ClaudeCLIClient implements AutoCloseable {

    private final Path mcpConfigPath;
    private final Path pluginRoot;
    private final Path projectDir;
    private final String model;
    private final long timeoutMinutes;
    private final Gson gson = new Gson();

    private static final String DEFAULT_MODEL = null;
    private static final long DEFAULT_TIMEOUT_MINUTES = 5;

    private static final List<String> REQUIRED_ENV_VARS = List.of(
        "DEVELOCITY_SERVER",
        "DEVELOCITY_ACCESS_KEY",
        "DRV_ACCESS_KEY"
    );

    private static final List<String> CLAUDE_ENV_VARS = List.of(
        "CLAUDE_CODE_USE_BEDROCK",
        "AWS_BEARER_TOKEN_BEDROCK",
        "ANTHROPIC_SMALL_FAST_MODEL",
        "ANTHROPIC_MODEL",
        "AWS_REGION"
    );

    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{([A-Z_][A-Z0-9_]*)(?::-([^}]*))?\\}");

    /**
     * Create a client with the default MCP config for E2E tests.
     *
     * @param mcpConfigPath Path to MCP config JSON file
     * @param pluginRoot Path to the plugin root directory (for --plugin-dir)
     * @param projectDir Path to the project/fixture directory (working directory)
     */
    public ClaudeCLIClient(Path mcpConfigPath, Path pluginRoot, Path projectDir) {
        this(mcpConfigPath, pluginRoot, projectDir, DEFAULT_MODEL, DEFAULT_TIMEOUT_MINUTES);
    }

    /**
     * Create a client with custom settings.
     *
     * @param mcpConfigPath Path to MCP config JSON file
     * @param pluginRoot Path to the plugin root directory (for --plugin-dir)
     * @param projectDir Path to the project/fixture directory (working directory)
     * @param model Model to use (e.g., "haiku", "sonnet", "opus"), or null for default
     * @param timeoutMinutes Maximum time to wait for response
     */
    public ClaudeCLIClient(Path mcpConfigPath, Path pluginRoot, Path projectDir, String model, long timeoutMinutes) {
        if (!Files.exists(mcpConfigPath)) {
            throw new IllegalArgumentException("MCP config not found: " + mcpConfigPath);
        }
        if (!Files.exists(pluginRoot)) {
            throw new IllegalArgumentException("Plugin root not found: " + pluginRoot);
        }
        if (!Files.exists(projectDir)) {
            throw new IllegalArgumentException("Project dir not found: " + projectDir);
        }
        this.mcpConfigPath = mcpConfigPath;
        this.pluginRoot = pluginRoot;
        this.projectDir = projectDir;
        this.model = model;
        this.timeoutMinutes = timeoutMinutes;

        validateRequiredEnvVars();
    }

    private void validateRequiredEnvVars() {
        List<String> missing = new ArrayList<>();
        for (String var : REQUIRED_ENV_VARS) {
            String value = System.getenv(var);
            if (value == null || value.isEmpty()) {
                missing.add(var);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                "Missing required environment variables: " + missing +
                "\nFor local testing, create tests/local.env with the required variables." +
                "\nFor CI, set the environment variables in your CI configuration."
            );
        }
    }

    /**
     * Run a prompt through Claude CLI with MCP tools and plugin support.
     *
     * @param prompt The user prompt to send
     * @return The CLI result including response and tool executions
     */
    public CLITestResult run(String prompt) throws IOException, InterruptedException {
        return invokeCLI(null, prompt);
    }

    /**
     * Run a prompt with a custom system prompt.
     *
     * @param systemPrompt Optional system prompt (null to use default)
     * @param userPrompt The user prompt to send
     * @return The CLI result including response and tool executions
     */
    public CLITestResult run(String systemPrompt, String userPrompt) throws IOException, InterruptedException {
        return invokeCLI(systemPrompt, userPrompt);
    }

    private CLITestResult invokeCLI(String systemPrompt, String userMessage) throws IOException, InterruptedException {
        Path expandedMcpConfig = Files.createTempFile("claude-mcp-config-", ".json");

        try {
            // Expand environment variables in MCP config
            String mcpConfigContent = Files.readString(mcpConfigPath);
            String expandedConfig = expandEnvironmentVariables(mcpConfigContent);
            Files.writeString(expandedMcpConfig, expandedConfig);

            // Create settings.local.json with Bedrock config
            Path projectClaudeDir = projectDir.resolve(".claude");
            Files.createDirectories(projectClaudeDir);
            Path settingsFile = projectClaudeDir.resolve("settings.local.json");
            String settingsJson = buildSettingsJson();
            if (settingsJson != null) {
                Files.writeString(settingsFile, settingsJson);
            }

            // Build the claude CLI command
            List<String> command = new ArrayList<>();
            command.add("claude");
            command.add("--output-format");
            command.add("json");
            if (model != null && !model.isBlank()) {
                command.add("--model");
                command.add(model);
            }
            command.add("--mcp-config");
            command.add(expandedMcpConfig.toAbsolutePath().toString());
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                command.add("--system-prompt");
                command.add(systemPrompt);
            }
            command.add("--dangerously-skip-permissions");
            command.add("--plugin-dir");
            command.add(pluginRoot.toAbsolutePath().toString());
            command.add("-p");
            command.add(userMessage);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            pb.redirectInput(ProcessBuilder.Redirect.from(new java.io.File("/dev/null")));
            pb.directory(projectDir.toFile());

            // Pass Claude/Bedrock environment variables to the process
            Map<String, String> env = pb.environment();
            for (String varName : CLAUDE_ENV_VARS) {
                String value = System.getenv(varName);
                if (value != null && !value.isEmpty()) {
                    env.put(varName, value);
                }
            }

            long startTime = System.currentTimeMillis();
            Process process = pb.start();

            // Read stdout and stderr concurrently
            StringBuilder stdoutBuilder = new StringBuilder();
            StringBuilder stderrBuilder = new StringBuilder();

            Thread stdoutThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdoutBuilder.append(line).append("\n");
                    }
                } catch (IOException e) {
                    // Ignore
                }
            }, "claude-stdout-reader");

            Thread stderrThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderrBuilder.append(line).append("\n");
                    }
                } catch (IOException e) {
                    // Ignore
                }
            }, "claude-stderr-reader");

            stdoutThread.start();
            stderrThread.start();

            boolean completed = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);

            if (!completed) {
                process.destroyForcibly();
                stdoutThread.interrupt();
                stderrThread.interrupt();
                throw new RuntimeException("Claude CLI timed out after " + timeoutMinutes + " minutes");
            }

            stdoutThread.join(5000);
            stderrThread.join(5000);

            int exitCode = process.exitValue();
            String stdout = stdoutBuilder.toString();
            String stderr = stderrBuilder.toString();

            return parseResponse(stdout, stderr, exitCode);

        } finally {
            Files.deleteIfExists(expandedMcpConfig);
        }
    }

    private String buildSettingsJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"env\": {\n");

        List<String> entries = new ArrayList<>();
        for (String varName : CLAUDE_ENV_VARS) {
            String value = System.getenv(varName);
            if (value != null && !value.isEmpty()) {
                String escapedValue = value.replace("\\", "\\\\").replace("\"", "\\\"");
                entries.add("    \"" + varName + "\": \"" + escapedValue + "\"");
            }
        }

        if (entries.isEmpty()) {
            return null;
        }

        json.append(String.join(",\n", entries));
        json.append("\n  }\n");
        json.append("}\n");

        return json.toString();
    }

    private String expandEnvironmentVariables(String content) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = ENV_VAR_PATTERN.matcher(content);

        while (matcher.find()) {
            String varName = matcher.group(1);
            String defaultValue = matcher.group(2);

            String value = System.getenv(varName);
            if (value == null || value.isEmpty()) {
                value = defaultValue != null ? defaultValue : "";
            }

            // Strip hostname= prefix from access keys if present
            if ((varName.endsWith("_ACCESS_KEY") || varName.endsWith("_TOKEN")) && value.contains("=")) {
                value = value.substring(value.indexOf('=') + 1);
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private CLITestResult parseResponse(String stdout, String stderr, int exitCode) {
        List<ToolExecution> toolExecutions = new ArrayList<>();
        String finalResponse = "";

        if (exitCode != 0) {
            return new CLITestResult(
                false,
                "CLI exited with code " + exitCode + ": " + stderr,
                toolExecutions,
                0, 0
            );
        }

        try {
            JsonElement parsed = gson.fromJson(stdout, JsonElement.class);
            JsonObject root;

            if (parsed.isJsonArray()) {
                JsonArray events = parsed.getAsJsonArray();
                JsonObject resultEvent = null;

                for (JsonElement event : events) {
                    if (event.isJsonObject()) {
                        JsonObject obj = event.getAsJsonObject();
                        String type = obj.has("type") ? obj.get("type").getAsString() : "";

                        // Collect tool executions from assistant messages
                        if ("assistant".equals(type) && obj.has("message")) {
                            JsonObject message = obj.getAsJsonObject("message");
                            if (message.has("content")) {
                                JsonArray content = message.getAsJsonArray("content");
                                for (JsonElement contentElem : content) {
                                    JsonObject contentBlock = contentElem.getAsJsonObject();
                                    String contentType = contentBlock.has("type") ? contentBlock.get("type").getAsString() : "";
                                    if ("tool_use".equals(contentType)) {
                                        String toolName = contentBlock.has("name") ? contentBlock.get("name").getAsString() : "unknown";
                                        String toolInput = contentBlock.has("input") ? contentBlock.get("input").toString() : "{}";
                                        String toolId = contentBlock.has("id") ? contentBlock.get("id").getAsString() : "";
                                        toolExecutions.add(new ToolExecution(toolName, toolInput, toolId, null));
                                    }
                                }
                            }
                        }

                        // Collect tool results from user messages
                        if ("user".equals(type)) {
                            JsonArray content = null;
                            if (obj.has("message")) {
                                JsonObject message = obj.getAsJsonObject("message");
                                if (message.has("content")) {
                                    content = message.getAsJsonArray("content");
                                }
                            }
                            if (content != null) {
                                for (JsonElement contentElem : content) {
                                    JsonObject contentBlock = contentElem.getAsJsonObject();
                                    String contentType = contentBlock.has("type") ? contentBlock.get("type").getAsString() : "";
                                    if ("tool_result".equals(contentType)) {
                                        String toolUseId = contentBlock.has("tool_use_id") ? contentBlock.get("tool_use_id").getAsString() : "";
                                        String resultContent = "";
                                        if (contentBlock.has("content")) {
                                            JsonElement resultElem = contentBlock.get("content");
                                            if (resultElem.isJsonPrimitive()) {
                                                resultContent = resultElem.getAsString();
                                            } else if (resultElem.isJsonArray()) {
                                                StringBuilder sb = new StringBuilder();
                                                for (JsonElement item : resultElem.getAsJsonArray()) {
                                                    if (item.isJsonObject()) {
                                                        JsonObject itemObj = item.getAsJsonObject();
                                                        if (itemObj.has("text")) {
                                                            sb.append(itemObj.get("text").getAsString());
                                                        }
                                                    }
                                                }
                                                resultContent = sb.toString();
                                            }
                                        }
                                        for (int i = 0; i < toolExecutions.size(); i++) {
                                            ToolExecution exec = toolExecutions.get(i);
                                            if (exec.toolId().equals(toolUseId) && exec.result() == null) {
                                                toolExecutions.set(i, new ToolExecution(exec.toolName(), exec.input(), exec.toolId(), resultContent));
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if ("result".equals(type)) {
                            resultEvent = obj;
                        }
                    }
                }

                if (resultEvent != null) {
                    root = resultEvent;
                } else {
                    return new CLITestResult(true, stdout, toolExecutions, 0, 0);
                }
            } else if (parsed.isJsonObject()) {
                root = parsed.getAsJsonObject();
            } else {
                return new CLITestResult(true, stdout, toolExecutions, 0, 0);
            }

            if (root.has("result")) {
                finalResponse = root.get("result").getAsString();
            }

            // Extract tool executions from messages if available
            if (root.has("messages")) {
                JsonArray messages = root.getAsJsonArray("messages");
                for (JsonElement msgElem : messages) {
                    JsonObject msg = msgElem.getAsJsonObject();
                    String role = msg.has("role") ? msg.get("role").getAsString() : "";

                    if ("assistant".equals(role) && msg.has("content")) {
                        JsonArray content = msg.getAsJsonArray("content");
                        for (JsonElement contentElem : content) {
                            JsonObject contentBlock = contentElem.getAsJsonObject();
                            String type = contentBlock.has("type") ? contentBlock.get("type").getAsString() : "";

                            if ("tool_use".equals(type)) {
                                String toolName = contentBlock.has("name") ? contentBlock.get("name").getAsString() : "unknown";
                                String toolInput = contentBlock.has("input") ? contentBlock.get("input").toString() : "{}";
                                String toolId = contentBlock.has("id") ? contentBlock.get("id").getAsString() : "";
                                toolExecutions.add(new ToolExecution(toolName, toolInput, toolId, null));
                            }
                        }
                    } else if ("user".equals(role) && msg.has("content")) {
                        JsonArray content = msg.getAsJsonArray("content");
                        for (JsonElement contentElem : content) {
                            JsonObject contentBlock = contentElem.getAsJsonObject();
                            String type = contentBlock.has("type") ? contentBlock.get("type").getAsString() : "";

                            if ("tool_result".equals(type)) {
                                String toolUseId = contentBlock.has("tool_use_id") ? contentBlock.get("tool_use_id").getAsString() : "";
                                String resultContent = contentBlock.has("content") ? contentBlock.get("content").getAsString() : "";

                                for (int i = 0; i < toolExecutions.size(); i++) {
                                    ToolExecution exec = toolExecutions.get(i);
                                    if (exec.toolId().equals(toolUseId) && exec.result() == null) {
                                        toolExecutions.set(i, new ToolExecution(exec.toolName(), exec.input(), exec.toolId(), resultContent));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Check num_turns to infer tool usage if messages aren't available
            if (toolExecutions.isEmpty() && root.has("num_turns")) {
                int numTurns = root.get("num_turns").getAsInt();
                if (numTurns > 1) {
                    toolExecutions.add(new ToolExecution("unknown", "{}", "", "Tool execution inferred from num_turns"));
                }
            }

            long inputTokens = 0;
            long outputTokens = 0;
            if (root.has("usage")) {
                JsonObject usage = root.getAsJsonObject("usage");
                inputTokens = usage.has("input_tokens") ? usage.get("input_tokens").getAsLong() : 0;
                outputTokens = usage.has("output_tokens") ? usage.get("output_tokens").getAsLong() : 0;
            }

            return new CLITestResult(true, finalResponse, toolExecutions, inputTokens, outputTokens);

        } catch (Exception e) {
            return new CLITestResult(
                exitCode == 0,
                stdout.isBlank() ? stderr : stdout,
                toolExecutions,
                0, 0
            );
        }
    }

    @Override
    public void close() {
        // Nothing to clean up
    }

    /**
     * Result from a CLI invocation, including tool execution details.
     */
    public record CLITestResult(
        boolean success,
        String response,
        List<ToolExecution> toolExecutions,
        long inputTokens,
        long outputTokens
    ) {
        public boolean hasToolExecutions() {
            return !toolExecutions.isEmpty();
        }

        public boolean usedTool(String toolNamePattern) {
            return toolExecutions.stream()
                .anyMatch(t -> t.toolName().contains(toolNamePattern));
        }

        public List<ToolExecution> getToolExecutions(String toolNamePattern) {
            return toolExecutions.stream()
                .filter(t -> t.toolName().contains(toolNamePattern))
                .toList();
        }

        public boolean containsAllOf(String... expectedTerms) {
            String lowerResponse = response.toLowerCase();
            return Arrays.stream(expectedTerms)
                .allMatch(term -> lowerResponse.contains(term.toLowerCase()));
        }

        public boolean containsAnyOf(String... expectedTerms) {
            String lowerResponse = response.toLowerCase();
            return Arrays.stream(expectedTerms)
                .anyMatch(term -> lowerResponse.contains(term.toLowerCase()));
        }
    }

    /**
     * Details of a single tool execution.
     */
    public record ToolExecution(
        String toolName,
        String input,
        String toolId,
        String result
    ) {
        public boolean succeeded() {
            return result != null && !result.isBlank();
        }
    }
}
