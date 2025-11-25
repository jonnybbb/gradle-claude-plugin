/**
 * Gradle Doctor Agent
 * 
 * Orchestrates comprehensive build health analysis using Claude subagents.
 * Delegates specific analysis tasks to specialized subagents and synthesizes results.
 * 
 * Subagents:
 * - performance-check: Analyzes build performance and identifies bottlenecks
 * - cache-validation: Validates build and configuration cache setup
 * - dependency-audit: Checks for dependency conflicts and vulnerabilities
 * - build-structure-review: Reviews project structure and organization
 * 
 * Reference: https://platform.claude.com/docs/en/agent-sdk/subagents
 */

import Anthropic from '@anthropic-ai/sdk';
import { exec } from 'child_process';
import { promisify } from 'util';
import * as fs from 'fs/promises';
import * as path from 'path';

const execAsync = promisify(exec);

interface DoctorConfig {
  projectDir: string;
  jbangToolsDir: string;
  apiKey: string;
  verbose?: boolean;
}

interface HealthReport {
  overall: 'healthy' | 'needs-attention' | 'critical';
  sections: {
    performance: SubagentResult;
    caching: SubagentResult;
    dependencies: SubagentResult;
    structure: SubagentResult;
  };
  recommendations: Recommendation[];
  quickFixes: QuickFix[];
}

interface SubagentResult {
  status: 'ok' | 'warning' | 'error';
  summary: string;
  details: string[];
  metrics?: Record<string, any>;
}

interface Recommendation {
  priority: 'high' | 'medium' | 'low';
  category: string;
  title: string;
  description: string;
  effort: 'quick' | 'moderate' | 'significant';
}

interface QuickFix {
  id: string;
  description: string;
  command: string;
  safe: boolean;
}

export class GradleDoctorAgent {
  private client: Anthropic;
  private config: DoctorConfig;

  constructor(config: DoctorConfig) {
    this.config = config;
    this.client = new Anthropic({ apiKey: config.apiKey });
  }

  /**
   * Main orchestration method - runs all subagents and synthesizes results
   */
  async diagnose(): Promise<HealthReport> {
    console.log('🔍 Starting Gradle health analysis...\n');

    // Collect baseline data using JBang tools
    const projectAnalysis = await this.runTool('gradle-analyzer.java', ['--json']);
    const cacheValidation = await this.runTool('cache-validator.java', []);

    // Initialize report
    const report: HealthReport = {
      overall: 'healthy',
      sections: {
        performance: await this.performanceSubagent(projectAnalysis),
        caching: await this.cacheSubagent(cacheValidation),
        dependencies: await this.dependencySubagent(projectAnalysis),
        structure: await this.structureSubagent(projectAnalysis),
      },
      recommendations: [],
      quickFixes: [],
    };

    // Synthesize results using main agent
    await this.synthesizeResults(report);

    return report;
  }

  /**
   * Performance analysis subagent
   */
  private async performanceSubagent(projectData: string): Promise<SubagentResult> {
    console.log('⚡ Analyzing performance...');

    const prompt = `You are a Gradle performance expert. Analyze this project data and identify performance issues:

${projectData}

Focus on:
1. Configuration time vs execution time ratio
2. Parallel execution opportunities
3. Build cache effectiveness
4. Task avoidance patterns
5. JVM arguments optimization

Provide a concise summary and specific actionable recommendations.`;

    const response = await this.client.messages.create({
      model: 'claude-sonnet-4-20250514',
      max_tokens: 2000,
      messages: [{ role: 'user', content: prompt }],
    });

    const content = response.content[0];
    const analysis = content.type === 'text' ? content.text : '';

    return this.parseSubagentResponse(analysis, 'performance');
  }

  /**
   * Cache validation subagent
   */
  private async cacheSubagent(cacheReport: string): Promise<SubagentResult> {
    console.log('🗄️  Validating caching...');

    const prompt = `You are a Gradle caching expert. Analyze this cache validation report:

${cacheReport}

Focus on:
1. Build cache configuration and effectiveness
2. Configuration cache compatibility
3. Task cacheability issues
4. Remote cache setup opportunities
5. Cache hit rate optimization

Provide a concise summary and specific recommendations.`;

    const response = await this.client.messages.create({
      model: 'claude-sonnet-4-20250514',
      max_tokens: 2000,
      messages: [{ role: 'user', content: prompt }],
    });

    const content = response.content[0];
    const analysis = content.type === 'text' ? content.text : '';

    return this.parseSubagentResponse(analysis, 'caching');
  }

  /**
   * Dependency audit subagent
   */
  private async dependencySubagent(projectData: string): Promise<SubagentResult> {
    console.log('📦 Auditing dependencies...');

    // Run dependency report
    try {
      await execAsync('./gradlew dependencies', { 
        cwd: this.config.projectDir,
        maxBuffer: 10 * 1024 * 1024 
      });
    } catch (error) {
      // Dependency resolution might fail, that's ok
    }

    const prompt = `You are a Gradle dependency expert. Analyze this project for dependency issues:

${projectData}

Focus on:
1. Potential dependency conflicts
2. Version alignment opportunities
3. Version catalog usage
4. Dependency resolution strategy
5. Unused dependency detection

Provide a concise summary and recommendations.`;

    const response = await this.client.messages.create({
      model: 'claude-sonnet-4-20250514',
      max_tokens: 2000,
      messages: [{ role: 'user', content: prompt }],
    });

    const content = response.content[0];
    const analysis = content.type === 'text' ? content.text : '';

    return this.parseSubagentResponse(analysis, 'dependencies');
  }

  /**
   * Build structure review subagent
   */
  private async structureSubagent(projectData: string): Promise<SubagentResult> {
    console.log('🏗️  Reviewing structure...');

    const prompt = `You are a Gradle build structure expert. Analyze this project structure:

${projectData}

Focus on:
1. Multi-project build organization
2. buildSrc or convention plugin usage
3. Settings file configuration
4. Project dependency graph
5. Build script organization

Provide a concise summary and recommendations.`;

    const response = await this.client.messages.create({
      model: 'claude-sonnet-4-20250514',
      max_tokens: 2000,
      messages: [{ role: 'user', content: prompt }],
    });

    const content = response.content[0];
    const analysis = content.type === 'text' ? content.text : '';

    return this.parseSubagentResponse(analysis, 'structure');
  }

  /**
   * Synthesize all subagent results into final recommendations
   */
  private async synthesizeResults(report: HealthReport): Promise<void> {
    console.log('🧠 Synthesizing results...\n');

    const synthesisPrompt = `You are the Gradle Doctor orchestrator. Review these analysis results and provide:
1. Overall health assessment (healthy/needs-attention/critical)
2. Top 5 prioritized recommendations
3. Quick wins (safe, high-impact fixes)

Performance: ${JSON.stringify(report.sections.performance)}
Caching: ${JSON.stringify(report.sections.caching)}
Dependencies: ${JSON.stringify(report.sections.dependencies)}
Structure: ${JSON.stringify(report.sections.structure)}

Format as JSON with: overall, recommendations[], quickFixes[]`;

    const response = await this.client.messages.create({
      model: 'claude-sonnet-4-20250514',
      max_tokens: 3000,
      messages: [{ role: 'user', content: synthesisPrompt }],
    });

    const content = response.content[0];
    if (content.type === 'text') {
      try {
        const synthesis = JSON.parse(content.text);
        report.overall = synthesis.overall;
        report.recommendations = synthesis.recommendations || [];
        report.quickFixes = synthesis.quickFixes || [];
      } catch (error) {
        console.error('Failed to parse synthesis:', error);
      }
    }
  }

  /**
   * Execute JBang tool
   */
  private async runTool(toolName: string, args: string[]): Promise<string> {
    const toolPath = path.join(this.config.jbangToolsDir, toolName);
    const projectArg = this.config.projectDir;
    
    try {
      const { stdout } = await execAsync(
        `jbang ${toolPath} ${projectArg} ${args.join(' ')}`,
        { maxBuffer: 10 * 1024 * 1024 }
      );
      return stdout;
    } catch (error: any) {
      console.error(`Tool ${toolName} failed:`, error.message);
      return '';
    }
  }

  /**
   * Parse subagent response into structured result
   */
  private parseSubagentResponse(analysis: string, category: string): SubagentResult {
    // Simple parsing - in production, use structured output
    const hasError = /error|critical|fail/i.test(analysis);
    const hasWarning = /warning|caution|consider/i.test(analysis);

    return {
      status: hasError ? 'error' : hasWarning ? 'warning' : 'ok',
      summary: analysis.split('\n')[0] || 'Analysis complete',
      details: analysis.split('\n').slice(1).filter(line => line.trim()),
    };
  }

  /**
   * Format and print health report
   */
  printReport(report: HealthReport): void {
    console.log('═══════════════════════════════════════');
    console.log('        GRADLE HEALTH REPORT');
    console.log('═══════════════════════════════════════\n');

    const statusEmoji = {
      healthy: '✅',
      'needs-attention': '⚠️',
      critical: '🚨',
    };

    console.log(`Overall Health: ${statusEmoji[report.overall]} ${report.overall.toUpperCase()}\n`);

    // Print section summaries
    console.log('Analysis Results:');
    for (const [section, result] of Object.entries(report.sections)) {
      const emoji = result.status === 'ok' ? '✅' : result.status === 'warning' ? '⚠️' : '❌';
      console.log(`  ${emoji} ${section}: ${result.summary}`);
    }

    // Print recommendations
    if (report.recommendations.length > 0) {
      console.log('\nTop Recommendations:');
      report.recommendations.forEach((rec, i) => {
        const priority = rec.priority === 'high' ? '🔴' : rec.priority === 'medium' ? '🟡' : '🟢';
        console.log(`  ${i + 1}. ${priority} ${rec.title}`);
        console.log(`     ${rec.description}`);
        console.log(`     Effort: ${rec.effort}\n`);
      });
    }

    // Print quick fixes
    if (report.quickFixes.length > 0) {
      console.log('Quick Fixes:');
      report.quickFixes.forEach((fix, i) => {
        console.log(`  ${i + 1}. ${fix.description}`);
        console.log(`     Command: ${fix.command}`);
        console.log(`     Safe: ${fix.safe ? '✅' : '⚠️'}\n`);
      });
    }

    console.log('═══════════════════════════════════════\n');
  }
}

// CLI entry point
if (import.meta.url === `file://${process.argv[1]}`) {
  const projectDir = process.argv[2] || '.';
  const apiKey = process.env.ANTHROPIC_API_KEY;

  if (!apiKey) {
    console.error('Error: ANTHROPIC_API_KEY environment variable not set');
    process.exit(1);
  }

  const agent = new GradleDoctorAgent({
    projectDir,
    jbangToolsDir: path.join(__dirname, '../../tools'),
    apiKey,
    verbose: true,
  });

  agent
    .diagnose()
    .then((report) => {
      agent.printReport(report);
      process.exit(report.overall === 'critical' ? 1 : 0);
    })
    .catch((error) => {
      console.error('Doctor agent failed:', error);
      process.exit(1);
    });
}
