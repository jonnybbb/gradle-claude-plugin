/**
 * Gradle Migration Agent
 *
 * Guides Gradle version migrations (6/7 → 8/9) with automated compatibility checks
 * and upgrade path recommendations.
 */

import {
  query,
  tool,
  createSdkMcpServer,
  type CallToolResult,
} from '@anthropic-ai/claude-agent-sdk';
import { z } from 'zod';
import { execSync } from 'child_process';
import { existsSync, readFileSync, writeFileSync } from 'fs';
import { join } from 'path';

const checkCurrentVersionTool = tool(
  'check_gradle_version',
  'Checks current Gradle version and wrapper configuration',
  {
    projectDir: z.string().optional(),
  },
  async (args): Promise<CallToolResult> => {
    const cwd = args.projectDir || process.cwd();
    const wrapperProps = join(cwd, 'gradle/wrapper/gradle-wrapper.properties');

    if (!existsSync(wrapperProps)) {
      return {
        content: [{
          type: 'text',
          text: JSON.stringify({
            error: 'No Gradle wrapper found',
            recommendation: 'Initialize wrapper: gradle wrapper --gradle-version=8.5',
          }),
        }],
      };
    }

    const props = readFileSync(wrapperProps, 'utf-8');
    const versionMatch = props.match(/distributionUrl=.*gradle-([0-9.]+)/);
    const currentVersion = versionMatch ? versionMatch[1] : 'unknown';

    const majorVersion = parseInt(currentVersion.split('.')[0]);
    const migrationPath = {
      currentVersion,
      majorVersion,
      needsMigration: majorVersion < 8,
      recommendedTarget: '8.5',
      migrationSteps: [] as string[],
    };

    if (majorVersion < 6) {
      migrationPath.migrationSteps = [
        'Upgrade to Gradle 6.9 first',
        'Then upgrade to Gradle 7.6',
        'Finally upgrade to Gradle 8.5',
      ];
    } else if (majorVersion === 6) {
      migrationPath.migrationSteps = [
        'Upgrade to Gradle 7.6 first',
        'Fix deprecation warnings',
        'Then upgrade to Gradle 8.5',
      ];
    } else if (majorVersion === 7) {
      migrationPath.migrationSteps = [
        'Update to Gradle 7.6 (latest 7.x)',
        'Fix all deprecation warnings',
        'Upgrade to Gradle 8.5',
      ];
    } else {
      migrationPath.needsMigration = false;
      migrationPath.migrationSteps = ['Already on Gradle 8+'];
    }

    return {
      content: [{ type: 'text', text: JSON.stringify(migrationPath, null, 2) }],
    };
  }
);

const scanDeprecatedAPIsTool = tool(
  'scan_deprecated_apis',
  'Scans build files for deprecated Gradle APIs',
  {
    projectDir: z.string().optional(),
  },
  async (args): Promise<CallToolResult> => {
    const cwd = args.projectDir || process.cwd();
    const deprecatedPatterns = [
      { pattern: /\.compile\s*\(/, api: 'compile configuration', replacement: 'implementation' },
      { pattern: /\.runtime\s*\(/, api: 'runtime configuration', replacement: 'runtimeOnly' },
      { pattern: /\.convention\./, api: 'Project.convention', replacement: 'extensions' },
      { pattern: /baseName\s*=/, api: 'Jar.baseName', replacement: 'archiveBaseName.set()' },
      { pattern: /destinationDir\s*=/, api: 'Task.destinationDir', replacement: 'destinationDirectory.set()' },
    ];

    const findings: any[] = [];
    const buildFiles = ['build.gradle', 'build.gradle.kts'];

    for (const file of buildFiles) {
      const filePath = join(cwd, file);
      if (existsSync(filePath)) {
        const content = readFileSync(filePath, 'utf-8');
        const lines = content.split('\n');

        deprecatedPatterns.forEach(({ pattern, api, replacement }) => {
          lines.forEach((line, index) => {
            if (pattern.test(line)) {
              findings.push({
                file,
                line: index + 1,
                api,
                replacement,
                code: line.trim(),
              });
            }
          });
        });
      }
    }

    return {
      content: [{
        type: 'text',
        text: JSON.stringify({
          totalFindings: findings.length,
          findings,
          recommendations: findings.length > 0
            ? ['Fix deprecated APIs before upgrading', 'Run with --warning-mode=all to see all warnings']
            : ['No deprecated APIs found in build files'],
        }, null, 2),
      }],
    };
  }
);

const updateGradleWrapperTool = tool(
  'update_gradle_wrapper',
  'Updates Gradle wrapper to specified version',
  {
    targetVersion: z.string().describe('Target Gradle version (e.g., 8.5)'),
    projectDir: z.string().optional(),
    distributionType: z.enum(['bin', 'all']).optional().describe('Distribution type (bin or all)'),
  },
  async (args): Promise<CallToolResult> => {
    try {
      const cwd = args.projectDir || process.cwd();
      const gradleCmd = existsSync(join(cwd, 'gradlew')) ? './gradlew' : 'gradle';
      const distType = args.distributionType || 'bin';

      const command = `${gradleCmd} wrapper --gradle-version=${args.targetVersion} --distribution-type=${distType}`;

      console.log(`Updating wrapper: ${command}`);

      const output = execSync(command, { cwd, encoding: 'utf-8' }).toString();

      return {
        content: [{
          type: 'text',
          text: JSON.stringify({
            success: true,
            version: args.targetVersion,
            output: output.trim(),
            nextSteps: [
              `Verify: ./gradlew --version`,
              `Run build: ./gradlew build --warning-mode=all`,
              `Fix any deprecation warnings`,
            ],
          }, null, 2),
        }],
      };
    } catch (error: any) {
      return {
        content: [{
          type: 'text',
          text: `Failed to update wrapper: ${error.message}`,
        }],
        isError: true,
      };
    }
  }
);

const checkPluginCompatibilityTool = tool(
  'check_plugin_compatibility',
  'Checks if applied plugins are compatible with target Gradle version',
  {
    targetVersion: z.string(),
    projectDir: z.string().optional(),
  },
  async (args): Promise<CallToolResult> => {
    const cwd = args.projectDir || process.cwd();
    const targetMajor = parseInt(args.targetVersion.split('.')[0]);

    // Known compatibility requirements for common plugins
    const pluginRequirements: Record<string, number> = {
      'com.android.application': 7, // AGP 7+ requires Gradle 7+
      'org.springframework.boot': targetMajor >= 8 ? 3 : 2, // Spring Boot 3 for Gradle 8+
      'org.jetbrains.kotlin.jvm': 1.7, // Kotlin 1.7+ for Gradle 8
    };

    const buildFiles = ['build.gradle', 'build.gradle.kts'];
    const findings: any[] = [];

    for (const file of buildFiles) {
      const filePath = join(cwd, file);
      if (existsSync(filePath)) {
        const content = readFileSync(filePath, 'utf-8');

        Object.entries(pluginRequirements).forEach(([plugin, minVersion]) => {
          if (content.includes(plugin)) {
            findings.push({
              plugin,
              requiredVersion: minVersion,
              recommendation: `Ensure ${plugin} is version ${minVersion}+ for Gradle ${args.targetVersion}`,
            });
          }
        });
      }
    }

    return {
      content: [{
        type: 'text',
        text: JSON.stringify({
          targetGradle: args.targetVersion,
          pluginsChecked: findings.length,
          findings,
          recommendations: [
            'Update plugins to versions compatible with target Gradle version',
            'Check plugin release notes for compatibility information',
            'Test build after upgrading plugins',
          ],
        }, null, 2),
      }],
    };
  }
);

export const mcpServer = createSdkMcpServer({
  name: 'gradle-migration-tools',
  version: '1.0.0',
  tools: [
    checkCurrentVersionTool,
    scanDeprecatedAPIsTool,
    updateGradleWrapperTool,
    checkPluginCompatibilityTool,
  ],
});

export async function runGradleMigrationAgent(
  projectDir: string,
  targetVersion?: string
): Promise<void> {
  const systemPrompt = `You are an expert Gradle migration specialist with deep knowledge of:
- Gradle version upgrade paths and breaking changes
- Deprecated API identification and replacement
- Plugin compatibility across Gradle versions
- Migration best practices and strategies

Your role is to:
1. Analyze current Gradle version and configuration
2. Identify deprecated APIs and incompatibilities
3. Provide step-by-step migration guidance
4. Check plugin compatibility
5. Automate wrapper updates when appropriate

Always:
- Recommend incremental upgrades (6→7→8, not 6→8)
- Fix deprecation warnings before major upgrades
- Provide specific code examples for replacements
- Test at each migration step
- Document breaking changes`;

  const prompt = targetVersion
    ? `Guide migration to Gradle ${targetVersion} for project in ${projectDir}`
    : `Analyze migration requirements for project in ${projectDir}`;

  try {
    const response = await query({
      prompt,
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
    console.error('Migration agent execution failed:', error);
    process.exit(1);
  }
}

export { checkCurrentVersionTool, scanDeprecatedAPIsTool, updateGradleWrapperTool, checkPluginCompatibilityTool };

if (require.main === module) {
  const projectDir = process.argv[2] || process.cwd();
  const targetVersion = process.argv[3];

  runGradleMigrationAgent(projectDir, targetVersion).catch((error) => {
    console.error('Fatal error:', error);
    process.exit(1);
  });
}
