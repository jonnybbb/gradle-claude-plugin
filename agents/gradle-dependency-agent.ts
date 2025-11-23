/**
 * Gradle Dependency Agent
 *
 * Advanced dependency analysis and conflict resolution for Gradle projects.
 */

import {
  query,
  tool,
  createSdkMcpServer,
  type CallToolResult,
} from '@anthropic-ai/claude-agent-sdk';
import { z } from 'zod';
import { execSync } from 'child_process';
import { existsSync } from 'fs';
import { join } from 'path';

const getDependencyTreeTool = tool(
  'get_dependency_tree',
  'Gets dependency tree for specified configuration',
  {
    configuration: z.string().describe('Configuration name (e.g., compileClasspath, runtimeClasspath)'),
    projectDir: z.string().optional(),
  },
  async (args): Promise<CallToolResult> => {
    try {
      const cwd = args.projectDir || process.cwd();
      const gradleCmd = existsSync(join(cwd, 'gradlew')) ? './gradlew' : 'gradle';

      const output = execSync(
        `${gradleCmd} dependencies --configuration=${args.configuration}`,
        { cwd, encoding: 'utf-8', maxBuffer: 10 * 1024 * 1024 }
      ).toString();

      return {
        content: [{
          type: 'text',
          text: output,
        }],
      };
    } catch (error: any) {
      return {
        content: [{
          type: 'text',
          text: `Failed to get dependency tree: ${error.message}`,
        }],
        isError: true,
      };
    }
  }
);

const findDependencyConflictsTool = tool(
  'find_dependency_conflicts',
  'Identifies version conflicts in dependencies',
  {
    configuration: z.string().optional().describe('Configuration to check (defaults to compileClasspath)'),
    projectDir: z.string().optional(),
  },
  async (args): Promise<CallToolResult> => {
    try {
      const cwd = args.projectDir || process.cwd();
      const config = args.configuration || 'compileClasspath';
      const gradleCmd = existsSync(join(cwd, 'gradlew')) ? './gradlew' : 'gradle';

      const output = execSync(
        `${gradleCmd} dependencies --configuration=${config}`,
        { cwd, encoding: 'utf-8', maxBuffer: 10 * 1024 * 1024 }
      ).toString();

      // Find conflicts (marked with -> in dependency tree)
      const conflicts: any[] = [];
      const lines = output.split('\n');

      lines.forEach((line, index) => {
        if (line.includes('->')) {
          const match = line.match(/([^\s:]+:[^\s:]+):([^\s]+)\s+->\s+([^\s]+)/);
          if (match) {
            conflicts.push({
              dependency: match[1],
              requestedVersion: match[2],
              selectedVersion: match[3],
              line: index + 1,
            });
          }
        }
      });

      const analysis = {
        configuration: config,
        conflictCount: conflicts.length,
        conflicts,
        recommendations: [] as string[],
      };

      if (conflicts.length > 0) {
        analysis.recommendations = [
          `Run: gradle dependencyInsight --dependency <lib> --configuration ${config}`,
          'Consider using dependency constraints to align versions',
          'Review use of platforms/BOMs for version management',
          'Check if conflicts cause runtime issues',
        ];

        // Group by library
        const grouped = conflicts.reduce((acc, c) => {
          if (!acc[c.dependency]) acc[c.dependency] = [];
          acc[c.dependency].push(c);
          return acc;
        }, {} as Record<string, any[]>);

        analysis.recommendations.push(
          '',
          'Most conflicted libraries:',
          ...Object.entries(grouped)
            .sort((a, b) => b[1].length - a[1].length)
            .slice(0, 5)
            .map(([lib, conflicts]) => `  ${lib}: ${conflicts.length} conflicts`)
        );
      }

      return {
        content: [{
          type: 'text',
          text: JSON.stringify(analysis, null, 2),
        }],
      };
    } catch (error: any) {
      return {
        content: [{
          type: 'text',
          text: `Failed to find conflicts: ${error.message}`,
        }],
        isError: true,
      };
    }
  }
);

const getDependencyInsightTool = tool(
  'get_dependency_insight',
  'Gets detailed insight into why a dependency is included and its version selection',
  {
    dependency: z.string().describe('Dependency to investigate (e.g., guava, slf4j-api)'),
    configuration: z.string().optional(),
    projectDir: z.string().optional(),
  },
  async (args): Promise<CallToolResult> => {
    try {
      const cwd = args.projectDir || process.cwd();
      const config = args.configuration || 'compileClasspath';
      const gradleCmd = existsSync(join(cwd, 'gradlew')) ? './gradlew' : 'gradle';

      const output = execSync(
        `${gradleCmd} dependencyInsight --dependency ${args.dependency} --configuration ${config}`,
        { cwd, encoding: 'utf-8', maxBuffer: 10 * 1024 * 1024 }
      ).toString();

      return {
        content: [{
          type: 'text',
          text: output,
        }],
      };
    } catch (error: any) {
      return {
        content: [{
          type: 'text',
          text: `Failed to get dependency insight: ${error.message}`,
        }],
        isError: true,
      };
    }
  }
);

const suggestResolutionStrategyTool = tool(
  'suggest_resolution_strategy',
  'Suggests resolution strategies for dependency conflicts',
  {
    conflict: z.object({
      dependency: z.string(),
      requestedVersion: z.string(),
      selectedVersion: z.string(),
    }),
  },
  async (args): Promise<CallToolResult> => {
    const { dependency, requestedVersion, selectedVersion } = args.conflict;

    const suggestions = {
      conflict: args.conflict,
      strategies: [
        {
          approach: 'Force specific version',
          kotlinDSL: `
configurations.all {
    resolutionStrategy.force("${dependency}:${selectedVersion}")
}`,
          groovyDSL: `
configurations.all {
    resolutionStrategy {
        force '${dependency}:${selectedVersion}'
    }
}`,
          pros: ['Simple and direct', 'Immediate resolution'],
          cons: ['Affects all configurations', 'May hide underlying issues'],
        },
        {
          approach: 'Use dependency constraints',
          kotlinDSL: `
dependencies {
    constraints {
        implementation("${dependency}:${selectedVersion}") {
            because("Resolve version conflict")
        }
    }
}`,
          groovyDSL: `
dependencies {
    constraints {
        implementation('${dependency}:${selectedVersion}') {
            because 'Resolve version conflict'
        }
    }
}`,
          pros: ['More declarative', 'Better documentation with because()'],
          cons: ['Requires Gradle 4.6+'],
        },
        {
          approach: 'Use platform/BOM',
          kotlinDSL: `
dependencies {
    implementation(platform("com.example:bom:1.0.0"))
    implementation("${dependency}") // Version from BOM
}`,
          groovyDSL: `
dependencies {
    implementation platform('com.example:bom:1.0.0')
    implementation '${dependency}' // Version from BOM
}`,
          pros: ['Centralized version management', 'Industry best practice'],
          cons: ['Requires suitable BOM available'],
        },
        {
          approach: 'Exclude and add explicit dependency',
          kotlinDSL: `
dependencies {
    implementation("parent:library:1.0") {
        exclude(group = "${dependency.split(':')[0]}", module = "${dependency.split(':')[1]}")
    }
    implementation("${dependency}:${selectedVersion}")
}`,
          groovyDSL: `
dependencies {
    implementation('parent:library:1.0') {
        exclude group: '${dependency.split(':')[0]}', module: '${dependency.split(':')[1]}'
    }
    implementation '${dependency}:${selectedVersion}'
}`,
          pros: ['Precise control', 'Surgical fix'],
          cons: ['More verbose', 'Needs maintenance'],
        },
      ],
      recommendations: [
        'Investigate why different versions are requested',
        'Check if newer version is compatible with all consumers',
        'Consider updating requesting dependencies to align versions',
        'Test thoroughly after applying resolution strategy',
      ],
    };

    return {
      content: [{
        type: 'text',
        text: JSON.stringify(suggestions, null, 2),
      }],
    };
  }
);

export const mcpServer = createSdkMcpServer({
  name: 'gradle-dependency-tools',
  version: '1.0.0',
  tools: [
    getDependencyTreeTool,
    findDependencyConflictsTool,
    getDependencyInsightTool,
    suggestResolutionStrategyTool,
  ],
});

export async function runGradleDependencyAgent(
  projectDir: string,
  configuration?: string
): Promise<void> {
  const systemPrompt = `You are an expert Gradle dependency management specialist with deep knowledge of:
- Dependency resolution mechanisms and conflict resolution
- Transitive dependency analysis
- Version constraints and platform dependencies
- BOM (Bill of Materials) usage patterns
- Dependency verification and security

Your role is to:
1. Analyze dependency trees and identify conflicts
2. Provide specific resolution strategies
3. Explain version selection rationale
4. Recommend best practices for dependency management
5. Help migrate to modern dependency management patterns

Always:
- Provide code examples in both Kotlin and Groovy DSL
- Explain trade-offs between different approaches
- Prioritize maintainability and clarity
- Consider security implications
- Recommend platforms/BOMs when appropriate`;

  const prompt = configuration
    ? `Analyze dependencies for configuration ${configuration} in ${projectDir}`
    : `Perform comprehensive dependency analysis for project in ${projectDir}`;

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
    console.error('Dependency agent execution failed:', error);
    process.exit(1);
  }
}

export {
  getDependencyTreeTool,
  findDependencyConflictsTool,
  getDependencyInsightTool,
  suggestResolutionStrategyTool,
};

if (require.main === module) {
  const projectDir = process.argv[2] || process.cwd();
  const configuration = process.argv[3];

  runGradleDependencyAgent(projectDir, configuration).catch((error) => {
    console.error('Fatal error:', error);
    process.exit(1);
  });
}
