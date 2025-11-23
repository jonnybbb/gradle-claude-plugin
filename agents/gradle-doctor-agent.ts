/**
 * Gradle Doctor Agent
 *
 * Comprehensive build health analysis agent that diagnoses configuration issues,
 * performance bottlenecks, and provides actionable recommendations.
 */

import {
  query,
  tool,
  createSdkMcpServer,
  type CallToolResult,
} from '@anthropic-ai/claude-agent-sdk';
import { z } from 'zod';
import { execSync } from 'child_process';
import { existsSync, readFileSync, statSync } from 'fs';
import { join } from 'path';

// ============================================================================
// Tools
// ============================================================================

const analyzeGradlePropertiesTool = tool(
  'analyze_gradle_properties',
  'Analyzes gradle.properties for performance and configuration issues',
  {
    projectDir: z.string().optional(),
  },
  async (args): Promise<CallToolResult> => {
    const cwd = args.projectDir || process.cwd();
    const propsPath = join(cwd, 'gradle.properties');

    if (!existsSync(propsPath)) {
      return {
        content: [{
          type: 'text',
          text: JSON.stringify({
            exists: false,
            issues: ['gradle.properties not found'],
            recommendations: [
              'Create gradle.properties with performance settings',
              'Add: org.gradle.caching=true',
              'Add: org.gradle.parallel=true',
              'Add: org.gradle.jvmargs=-Xmx4g',
            ],
          }),
        }],
      };
    }

    const content = readFileSync(propsPath, 'utf-8');
    const analysis = {
      exists: true,
      settings: {} as Record<string, string>,
      issues: [] as string[],
      warnings: [] as string[],
      recommendations: [] as string[],
    };

    // Parse properties
    content.split('\n').forEach(line => {
      const match = line.match(/^\s*([^#=]+)=(.*)$/);
      if (match) {
        analysis.settings[match[1].trim()] = match[2].trim();
      }
    });

    // Check for critical settings
    if (!analysis.settings['org.gradle.caching']) {
      analysis.issues.push('Build cache not enabled');
      analysis.recommendations.push('Add: org.gradle.caching=true');
    }

    if (!analysis.settings['org.gradle.parallel']) {
      analysis.warnings.push('Parallel builds not enabled');
      analysis.recommendations.push('Add: org.gradle.parallel=true');
    }

    if (!analysis.settings['org.gradle.jvmargs']) {
      analysis.issues.push('Daemon heap size not configured');
      analysis.recommendations.push('Add: org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g');
    }

    if (!analysis.settings['org.gradle.configuration-cache']) {
      analysis.warnings.push('Configuration cache not enabled');
      analysis.recommendations.push('Consider: org.gradle.configuration-cache=true');
    }

    return {
      content: [{ type: 'text', text: JSON.stringify(analysis, null, 2) }],
    };
  }
);

const analyzeDependenciesTool = tool(
  'analyze_project_dependencies',
  'Analyzes project dependencies for conflicts and security issues',
  {
    projectDir: z.string().optional(),
    configuration: z.string().optional().describe('Configuration to analyze (e.g., compileClasspath)'),
  },
  async (args): Promise<CallToolResult> => {
    try {
      const cwd = args.projectDir || process.cwd();
      const gradleCmd = existsSync(join(cwd, 'gradlew')) ? './gradlew' : 'gradle';
      const config = args.configuration || 'compileClasspath';

      const output = execSync(
        `${gradleCmd} dependencies --configuration=${config}`,
        { cwd, encoding: 'utf-8', maxBuffer: 10 * 1024 * 1024 }
      ).toString();

      // Parse conflicts (marked with (*) in dependency tree)
      const conflicts = output.match(/.*\(\*\).*/g) || [];

      const analysis = {
        configuration: config,
        totalDependencies: (output.match(/\+---/g) || []).length,
        conflicts: conflicts.map(c => c.trim()),
        hasConflicts: conflicts.length > 0,
        recommendations: [] as string[],
      };

      if (analysis.hasConflicts) {
        analysis.recommendations.push(
          'Run: gradle dependencyInsight --dependency <lib> to investigate',
          'Consider using dependency constraints or platforms',
          'Review transitive dependencies for version alignment'
        );
      }

      return {
        content: [{ type: 'text', text: JSON.stringify(analysis, null, 2) }],
      };
    } catch (error: any) {
      return {
        content: [{ type: 'text', text: `Dependency analysis failed: ${error.message}` }],
        isError: true,
      };
    }
  }
);

const analyzeBuildCacheTool = tool(
  'analyze_build_cache',
  'Analyzes build cache configuration and usage',
  {
    projectDir: z.string().optional(),
  },
  async (args): Promise<CallToolResult> => {
    const cwd = args.projectDir || process.cwd();
    const cacheDir = join(require('os').homedir(), '.gradle/caches/build-cache-1');

    const analysis = {
      localCacheExists: existsSync(cacheDir),
      cacheSize: 0,
      entryCount: 0,
      recommendations: [] as string[],
    };

    if (analysis.localCacheExists) {
      try {
        const stats = statSync(cacheDir);
        // Simplified - would need recursive calculation for accurate size
        analysis.cacheSize = stats.size;

        // Count entries
        const entries = require('fs').readdirSync(cacheDir);
        analysis.entryCount = entries.length;
      } catch (e) {
        // Ignore stat errors
      }
    }

    // Check settings.gradle for cache configuration
    const settingsFiles = ['settings.gradle.kts', 'settings.gradle'];
    let hasCacheConfig = false;

    for (const file of settingsFiles) {
      const settingsPath = join(cwd, file);
      if (existsSync(settingsPath)) {
        const content = readFileSync(settingsPath, 'utf-8');
        if (content.includes('buildCache')) {
          hasCacheConfig = true;
          break;
        }
      }
    }

    if (!hasCacheConfig) {
      analysis.recommendations.push(
        'Configure build cache in settings.gradle.kts',
        'Consider remote build cache for team collaboration'
      );
    }

    return {
      content: [{ type: 'text', text: JSON.stringify(analysis, null, 2) }],
    };
  }
);

const checkBuildPerformanceTool = tool(
  'check_build_performance',
  'Checks build performance configuration and settings',
  {
    projectDir: z.string().optional(),
  },
  async (args): Promise<CallToolResult> => {
    const cwd = args.projectDir || process.cwd();

    // Count modules
    const settingsFiles = ['settings.gradle.kts', 'settings.gradle'];
    let moduleCount = 1; // At least root project

    for (const file of settingsFiles) {
      const settingsPath = join(cwd, file);
      if (existsSync(settingsPath)) {
        const content = readFileSync(settingsPath, 'utf-8');
        const includes = content.match(/include\s*\(['"]:([^'"]+)['"]\)/g) || [];
        moduleCount += includes.length;
      }
    }

    const performance = {
      moduleCount,
      projectType: moduleCount > 1 ? 'multi-module' : 'single-module',
      recommendations: [] as string[],
    };

    if (moduleCount > 5) {
      performance.recommendations.push(
        'Consider enabling parallel builds: org.gradle.parallel=true',
        'Configuration cache will provide significant benefits',
        'Review module dependencies for optimization opportunities'
      );
    }

    if (moduleCount > 20) {
      performance.recommendations.push(
        'Large project detected - increase daemon heap to 8g',
        'Enable configuration cache for faster configuration',
        'Consider composite builds for better modularity'
      );
    }

    return {
      content: [{ type: 'text', text: JSON.stringify(performance, null, 2) }],
    };
  }
);

// ============================================================================
// MCP Server
// ============================================================================

export const mcpServer = createSdkMcpServer({
  name: 'gradle-doctor-tools',
  version: '1.0.0',
  tools: [
    analyzeGradlePropertiesTool,
    analyzeDependenciesTool,
    analyzeBuildCacheTool,
    checkBuildPerformanceTool,
  ],
});

// ============================================================================
// Agent Main Function
// ============================================================================

export async function runGradleDoctorAgent(
  projectDir: string,
  scope?: string
): Promise<void> {
  const systemPrompt = `You are an expert Gradle build doctor with expertise in:
- Build configuration analysis and optimization
- Performance tuning and bottleneck identification
- Dependency management and conflict resolution
- Build cache optimization
- Multi-module project organization

Your role is to:
1. Perform comprehensive build health analysis
2. Identify configuration issues and anti-patterns
3. Provide specific, actionable recommendations
4. Prioritize fixes by impact and effort
5. Generate detailed health reports

Always:
- Focus on high-impact optimizations first
- Provide concrete examples and fixes
- Explain the "why" behind recommendations
- Consider project size and complexity
- Balance performance vs maintainability`;

  const prompt = scope
    ? `Perform ${scope} analysis of Gradle project in ${projectDir}`
    : `Perform comprehensive build health analysis of Gradle project in ${projectDir}`;

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
    console.error('Doctor agent execution failed:', error);
    process.exit(1);
  }
}

export { analyzeGradlePropertiesTool, analyzeDependenciesTool, analyzeBuildCacheTool, checkBuildPerformanceTool };

if (require.main === module) {
  const projectDir = process.argv[2] || process.cwd();
  const scope = process.argv[3];

  runGradleDoctorAgent(projectDir, scope).catch((error) => {
    console.error('Fatal error:', error);
    process.exit(1);
  });
}
