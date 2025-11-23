/**
 * Gradle Build Agent
 *
 * Executes and manages complex Gradle builds with automated error detection,
 * intelligent retry logic, and detailed reporting.
 */

import {
  query,
  tool,
  createSdkMcpServer,
  type CallToolResult,
  type SDKUserMessage,
} from '@anthropic-ai/claude-agent-sdk';
import { z } from 'zod';
import { execSync, spawn } from 'child_process';
import { existsSync, readFileSync, writeFileSync } from 'fs';
import { join } from 'path';

// ============================================================================
// Tools
// ============================================================================

/**
 * Execute a Gradle build with specified tasks and options
 */
const executeBuildTool = tool(
  'execute_gradle_build',
  'Executes Gradle build tasks with specified options and intelligent error handling',
  {
    tasks: z.array(z.string()).describe('Gradle tasks to execute (e.g., ["clean", "build"])'),
    options: z.array(z.string()).optional().describe('Gradle CLI options (e.g., ["--info", "--parallel"])'),
    projectDir: z.string().optional().describe('Project directory path (defaults to current)'),
    continueOnFailure: z.boolean().optional().describe('Continue even if build fails'),
    captureOutput: z.boolean().optional().describe('Capture and return full output'),
  },
  async (args): Promise<CallToolResult> => {
    try {
      const cwd = args.projectDir || process.cwd();
      const gradleCmd = existsSync(join(cwd, 'gradlew')) ? './gradlew' : 'gradle';

      const command = [
        gradleCmd,
        ...args.tasks,
        ...(args.options || []),
      ].join(' ');

      console.log(`Executing: ${command}`);
      console.log(`Directory: ${cwd}`);

      const result = execSync(command, {
        cwd,
        encoding: 'utf-8',
        maxBuffer: 10 * 1024 * 1024, // 10MB buffer
      }).toString();

      return {
        content: [{
          type: 'text' as const,
          text: args.captureOutput ? result : `Build completed successfully\n\nOutput preview:\n${result.slice(-1000)}`,
        }],
      };
    } catch (error: any) {
      const errorOutput = error.stdout?.toString() || error.message;
      if (args.continueOnFailure) {
        return {
          content: [{
            type: 'text' as const,
            text: `Build failed but continuing as requested:\n${errorOutput}`,
          }],
        };
      }
      return {
        content: [{
          type: 'text' as const,
          text: `Build failed:\n${errorOutput}`,
        }],
        isError: true,
      };
    }
  }
);

/**
 * Analyze build failure and suggest fixes
 */
const analyzeBuildFailureTool = tool(
  'analyze_build_failure',
  'Analyzes Gradle build failures and suggests specific fixes',
  {
    errorLog: z.string().describe('Build error log or output'),
    projectDir: z.string().optional().describe('Project directory'),
  },
  async (args): Promise<CallToolResult> => {
    const { errorLog } = args;
    const analysis = [];

    // Dependency resolution failures
    if (errorLog.includes('Could not resolve')) {
      analysis.push({
        type: 'DEPENDENCY_RESOLUTION',
        message: 'Dependency resolution failure detected',
        suggestions: [
          'Check repository configuration in build.gradle',
          'Verify dependency coordinates are correct',
          'Try running with --refresh-dependencies',
          'Check network connectivity to repositories',
        ],
      });
    }

    // Compilation errors
    if (errorLog.includes('Compilation failed') || errorLog.includes('error: ')) {
      analysis.push({
        type: 'COMPILATION_ERROR',
        message: 'Compilation error detected',
        suggestions: [
          'Review compiler error messages above',
          'Check Java/Kotlin version compatibility',
          'Verify all dependencies are on classpath',
          'Check for syntax errors in source files',
        ],
      });
    }

    // Out of memory errors
    if (errorLog.includes('OutOfMemoryError') || errorLog.includes('GC overhead limit')) {
      analysis.push({
        type: 'MEMORY_ERROR',
        message: 'Out of memory error detected',
        suggestions: [
          'Increase heap size in gradle.properties: org.gradle.jvmargs=-Xmx4g',
          'Stop daemon: gradle --stop',
          'Check for memory leaks in build logic',
          'Consider using --no-daemon for this build',
        ],
      });
    }

    // Configuration cache issues
    if (errorLog.includes('Configuration cache problems')) {
      analysis.push({
        type: 'CONFIGURATION_CACHE',
        message: 'Configuration cache compatibility issue',
        suggestions: [
          'Review configuration cache report in build/reports/configuration-cache/',
          'Disable temporarily with -Dorg.gradle.configuration-cache=false',
          'Update plugins to configuration-cache compatible versions',
          'Fix serialization issues in build logic',
        ],
      });
    }

    // Task execution failures
    const taskFailureMatch = errorLog.match(/Execution failed for task '([^']+)'/);
    if (taskFailureMatch) {
      analysis.push({
        type: 'TASK_EXECUTION',
        message: `Task execution failure: ${taskFailureMatch[1]}`,
        suggestions: [
          `Run with --stacktrace to see full error: gradle ${taskFailureMatch[1]} --stacktrace`,
          'Check task inputs and outputs are properly configured',
          'Review task action implementation',
          'Try running task in isolation to isolate issue',
        ],
      });
    }

    const report = {
      summary: `Found ${analysis.length} issue(s)`,
      issues: analysis,
      nextSteps: [
        'Address critical issues first (memory, dependencies)',
        'Run with --info or --debug for more details',
        'Check build scan if available for detailed insights',
      ],
    };

    return {
      content: [{
        type: 'text' as const,
        text: JSON.stringify(report, null, 2),
      }],
    };
  }
);

/**
 * Get build environment information
 */
const getBuildEnvironmentTool = tool(
  'get_build_environment',
  'Retrieves Gradle build environment information',
  {
    projectDir: z.string().optional().describe('Project directory'),
  },
  async (args): Promise<CallToolResult> => {
    try {
      const cwd = args.projectDir || process.cwd();
      const gradleCmd = existsSync(join(cwd, 'gradlew')) ? './gradlew' : 'gradle';

      // Get Gradle version
      const versionOutput = execSync(`${gradleCmd} --version`, {
        cwd,
        encoding: 'utf-8',
      }).toString();

      // Get build environment
      const envOutput = execSync(`${gradleCmd} buildEnvironment --console=plain`, {
        cwd,
        encoding: 'utf-8',
      }).toString();

      const environment = {
        gradleVersion: versionOutput,
        buildEnvironment: envOutput,
        projectDir: cwd,
        hasWrapper: existsSync(join(cwd, 'gradlew')),
      };

      return {
        content: [{
          type: 'text' as const,
          text: JSON.stringify(environment, null, 2),
        }],
      };
    } catch (error: any) {
      return {
        content: [{
          type: 'text' as const,
          text: `Failed to get build environment: ${error.message}`,
        }],
        isError: true,
      };
    }
  }
);

/**
 * Run build with profiling
 */
const profileBuildTool = tool(
  'profile_build',
  'Runs Gradle build with profiling to identify performance bottlenecks',
  {
    tasks: z.array(z.string()).describe('Tasks to profile'),
    projectDir: z.string().optional().describe('Project directory'),
  },
  async (args): Promise<CallToolResult> => {
    try {
      const cwd = args.projectDir || process.cwd();
      const gradleCmd = existsSync(join(cwd, 'gradlew')) ? './gradlew' : 'gradle';

      const command = `${gradleCmd} ${args.tasks.join(' ')} --profile`;

      console.log(`Profiling build: ${command}`);

      const output = execSync(command, {
        cwd,
        encoding: 'utf-8',
      }).toString();

      // Find profile report
      const profileReportPath = join(cwd, 'build/reports/profile');
      const profileFiles = existsSync(profileReportPath)
        ? require('fs').readdirSync(profileReportPath).filter((f: string) => f.endsWith('.html'))
        : [];

      const result = {
        success: true,
        output: output.slice(-500),
        profileReport: profileFiles.length > 0
          ? `Profile report: ${join(profileReportPath, profileFiles[0])}`
          : 'No profile report found',
      };

      return {
        content: [{
          type: 'text' as const,
          text: JSON.stringify(result, null, 2),
        }],
      };
    } catch (error: any) {
      return {
        content: [{
          type: 'text' as const,
          text: `Profiling failed: ${error.message}`,
        }],
        isError: true,
      };
    }
  }
);

// ============================================================================
// MCP Server
// ============================================================================

const mcpServer = createSdkMcpServer({
  name: 'gradle-build-tools',
  version: '1.0.0',
  tools: [
    executeBuildTool,
    analyzeBuildFailureTool,
    getBuildEnvironmentTool,
    profileBuildTool,
  ],
});

// ============================================================================
// Agent Main Function
// ============================================================================

export async function runGradleBuildAgent(
  projectDir: string,
  tasks: string[],
  userPrompt?: string
): Promise<void> {
  const systemPrompt = `You are an expert Gradle build agent with deep knowledge of:
- Gradle build system architecture and task execution
- Build failure diagnosis and resolution
- Performance optimization and profiling
- Build configuration and best practices

Your role is to:
1. Execute Gradle builds reliably with proper error handling
2. Analyze build failures and provide specific, actionable solutions
3. Optimize build performance through profiling and tuning
4. Guide users through complex build scenarios
5. Automate common build tasks and workflows

Always:
- Provide clear explanations of what you're doing
- Suggest specific commands and configurations
- Prioritize build stability and performance
- Use build cache and parallel execution when appropriate
- Capture and analyze build output for insights`;

  const defaultPrompt = userPrompt || `Execute Gradle build for tasks: ${tasks.join(', ')}`;

  try {
    const response = await query({
      prompt: defaultPrompt,
      options: {
        model: 'claude-sonnet-4-5-20250929',
        systemPrompt,
        mcpServers: [mcpServer],
      },
    });

    for await (const chunk of response) {
      if (chunk.type === 'text') {
        console.log(chunk.text);
      }
    }
  } catch (error) {
    console.error('Build agent execution failed:', error);
    process.exit(1);
  }
}

// Export for use as module
export { mcpServer, executeBuildTool, analyzeBuildFailureTool, getBuildEnvironmentTool, profileBuildTool };

// CLI entry point
if (require.main === module) {
  const args = process.argv.slice(2);
  const projectDir = process.cwd();
  const tasks = args.length > 0 ? args : ['build'];

  runGradleBuildAgent(projectDir, tasks).catch((error) => {
    console.error('Fatal error:', error);
    process.exit(1);
  });
}
