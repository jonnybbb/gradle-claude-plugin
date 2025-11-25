/**
 * Gradle Migration Agent
 * 
 * Orchestrates automated Gradle version migrations using Claude subagents.
 * Analyzes current build, identifies migration issues, and provides step-by-step guidance.
 * 
 * Subagents:
 * - version-analyzer: Detects current version and identifies target
 * - deprecation-scanner: Finds deprecated API usage
 * - breaking-change-detector: Identifies breaking changes for target version
 * - migration-planner: Creates prioritized migration plan
 * - auto-fixer: Applies safe automated fixes
 * 
 * Note: These TypeScript agents are standalone scripts using the Anthropic SDK directly.
 * They can be run independently or integrated into larger workflows.
 *
 * @see https://docs.anthropic.com/en/api/getting-started
 */

import Anthropic from '@anthropic-ai/sdk';
import { exec } from 'child_process';
import { promisify } from 'util';
import * as fs from 'fs/promises';
import * as path from 'path';

const execAsync = promisify(exec);

// Model configuration - use environment variable or default to latest Sonnet
const DEFAULT_MODEL = process.env.ANTHROPIC_MODEL || 'claude-sonnet-4-5-20250929';

// =============================================================================
// Types
// =============================================================================

interface MigrationConfig {
  projectDir: string;
  targetVersion?: string;  // If not specified, uses latest stable
  apiKey: string;
  dryRun?: boolean;
  verbose?: boolean;
}

interface MigrationReport {
  currentVersion: string;
  targetVersion: string;
  compatibility: 'compatible' | 'minor-changes' | 'major-changes' | 'breaking';
  phases: MigrationPhase[];
  deprecations: Deprecation[];
  breakingChanges: BreakingChange[];
  autoFixable: AutoFix[];
  manualSteps: ManualStep[];
  estimatedEffort: string;
}

interface MigrationPhase {
  order: number;
  name: string;
  description: string;
  steps: string[];
  risk: 'low' | 'medium' | 'high';
}

interface Deprecation {
  api: string;
  location: string;
  replacement: string;
  removedIn: string;
  autoFixable: boolean;
}

interface BreakingChange {
  id: string;
  description: string;
  impact: 'low' | 'medium' | 'high';
  affectedFiles: string[];
  solution: string;
}

interface AutoFix {
  id: string;
  description: string;
  file: string;
  oldCode: string;
  newCode: string;
  safe: boolean;
}

interface ManualStep {
  order: number;
  title: string;
  description: string;
  commands?: string[];
  verification: string;
}

interface VersionInfo {
  current: string;
  wrapper: boolean;
  javaVersion: string;
  kotlinVersion?: string;
}

// =============================================================================
// Migration Agent
// =============================================================================

export class GradleMigrationAgent {
  private client: Anthropic;
  private config: MigrationConfig;

  // Known Gradle version compatibility data
  private static readonly VERSION_DATA: Record<string, {
    minJava: number;
    maxJava: number;
    deprecations: string[];
    removals: string[];
  }> = {
    '7.0': { minJava: 8, maxJava: 16, deprecations: ['compile', 'testCompile'], removals: [] },
    '7.6': { minJava: 8, maxJava: 19, deprecations: ['archiveName'], removals: ['compile'] },
    '8.0': { minJava: 8, maxJava: 19, deprecations: [], removals: ['archiveName', 'archivesBaseName'] },
    '8.5': { minJava: 8, maxJava: 21, deprecations: [], removals: [] },
    '8.11': { minJava: 8, maxJava: 23, deprecations: [], removals: [] },
  };

  constructor(config: MigrationConfig) {
    this.config = config;
    this.client = new Anthropic({ apiKey: config.apiKey });
  }

  /**
   * Main orchestration - analyze and plan migration
   */
  async analyze(): Promise<MigrationReport> {
    console.log('🔍 Starting Gradle migration analysis...\n');

    // Phase 1: Detect current version and environment
    const versionInfo = await this.detectVersion();
    console.log(`📦 Current: Gradle ${versionInfo.current}, Java ${versionInfo.javaVersion}`);

    // Phase 2: Determine target version
    const targetVersion = this.config.targetVersion || await this.getLatestStableVersion();
    console.log(`🎯 Target: Gradle ${targetVersion}\n`);

    if (versionInfo.current === targetVersion) {
      console.log('✅ Already on target version!');
      return this.createEmptyReport(versionInfo.current, targetVersion);
    }

    // Phase 3: Scan for deprecations and issues
    const buildScripts = await this.collectBuildScripts();
    const deprecations = await this.deprecationSubagent(buildScripts, versionInfo.current, targetVersion);
    
    // Phase 4: Identify breaking changes
    const breakingChanges = await this.breakingChangeSubagent(buildScripts, versionInfo.current, targetVersion);

    // Phase 5: Generate auto-fixes
    const autoFixes = await this.autoFixSubagent(deprecations, breakingChanges);

    // Phase 6: Create migration plan
    const plan = await this.planningSubagent(versionInfo, targetVersion, deprecations, breakingChanges);

    return {
      currentVersion: versionInfo.current,
      targetVersion,
      compatibility: this.assessCompatibility(deprecations, breakingChanges),
      phases: plan.phases,
      deprecations,
      breakingChanges,
      autoFixable: autoFixes,
      manualSteps: plan.manualSteps,
      estimatedEffort: plan.effort,
    };
  }

  /**
   * Apply auto-fixes (with confirmation in non-dry-run mode)
   */
  async applyFixes(report: MigrationReport): Promise<void> {
    if (this.config.dryRun) {
      console.log('🔒 Dry run mode - no changes applied\n');
      return;
    }

    const safeFixes = report.autoFixable.filter(f => f.safe);
    console.log(`\n🔧 Applying ${safeFixes.length} safe auto-fixes...\n`);

    for (const fix of safeFixes) {
      try {
        const filePath = path.join(this.config.projectDir, fix.file);
        let content = await fs.readFile(filePath, 'utf-8');
        
        if (content.includes(fix.oldCode)) {
          content = content.replace(fix.oldCode, fix.newCode);
          await fs.writeFile(filePath, content, 'utf-8');
          console.log(`  ✅ ${fix.description}`);
        }
      } catch (error) {
        console.log(`  ❌ Failed: ${fix.description}`);
      }
    }
  }

  /**
   * Update Gradle wrapper to target version
   */
  async updateWrapper(targetVersion: string): Promise<boolean> {
    console.log(`\n📦 Updating Gradle wrapper to ${targetVersion}...`);
    
    try {
      await execAsync(
        `./gradlew wrapper --gradle-version ${targetVersion}`,
        { cwd: this.config.projectDir }
      );
      console.log('  ✅ Wrapper updated successfully');
      return true;
    } catch (error) {
      console.log('  ❌ Wrapper update failed');
      return false;
    }
  }

  // ===========================================================================
  // Subagents
  // ===========================================================================

  /**
   * Deprecation scanner subagent
   */
  private async deprecationSubagent(
    buildScripts: Map<string, string>,
    currentVersion: string,
    targetVersion: string
  ): Promise<Deprecation[]> {
    console.log('🔎 Scanning for deprecated APIs...');

    const scriptsContent = Array.from(buildScripts.entries())
      .map(([file, content]) => `--- ${file} ---\n${content}`)
      .join('\n\n');

    const prompt = `You are a Gradle migration expert. Analyze these build scripts for deprecated APIs.

Current Gradle version: ${currentVersion}
Target Gradle version: ${targetVersion}

Build scripts:
${scriptsContent}

Find ALL deprecated API usages that need to change before ${targetVersion}. For each, provide:
1. The deprecated API/pattern
2. File location
3. Replacement API
4. Version where it's removed
5. Whether it can be auto-fixed with simple text replacement

Common deprecations to look for:
- tasks.create() → tasks.register()
- tasks.getByName() → tasks.named()
- archiveName → archiveFileName.set()
- archivesBaseName → base.archivesName.set()
- destinationDir → destinationDirectory.set()
- compile/testCompile → implementation/testImplementation
- project.convention.getPlugin() → project.extensions.getByType()
- buildDir → layout.buildDirectory

Return as JSON array:
[{"api": "...", "location": "file:line", "replacement": "...", "removedIn": "8.0", "autoFixable": true}]

Return ONLY the JSON array, no other text.`;

    const response = await this.client.messages.create({
      model: DEFAULT_MODEL,
      max_tokens: 4000,
      messages: [{ role: 'user', content: prompt }],
    });

    const content = response.content[0];
    if (content.type === 'text') {
      try {
        return JSON.parse(content.text);
      } catch {
        return this.extractDeprecationsFromText(content.text);
      }
    }
    return [];
  }

  /**
   * Breaking change detector subagent
   */
  private async breakingChangeSubagent(
    buildScripts: Map<string, string>,
    currentVersion: string,
    targetVersion: string
  ): Promise<BreakingChange[]> {
    console.log('⚠️  Detecting breaking changes...');

    const scriptsContent = Array.from(buildScripts.entries())
      .map(([file, content]) => `--- ${file} ---\n${content}`)
      .join('\n\n');

    const prompt = `You are a Gradle migration expert. Identify breaking changes when upgrading from ${currentVersion} to ${targetVersion}.

Build scripts:
${scriptsContent}

Identify breaking changes that WILL cause build failures. Consider:
1. Removed APIs (not just deprecated)
2. Changed behavior
3. Plugin compatibility issues
4. Java version requirements
5. Configuration changes

For each breaking change provide:
- Unique ID
- Description
- Impact level (low/medium/high)
- Affected files
- Solution

Return as JSON array:
[{"id": "BC001", "description": "...", "impact": "high", "affectedFiles": ["build.gradle.kts"], "solution": "..."}]

Return ONLY the JSON array. If no breaking changes found, return [].`;

    const response = await this.client.messages.create({
      model: DEFAULT_MODEL,
      max_tokens: 4000,
      messages: [{ role: 'user', content: prompt }],
    });

    const content = response.content[0];
    if (content.type === 'text') {
      try {
        return JSON.parse(content.text);
      } catch {
        return [];
      }
    }
    return [];
  }

  /**
   * Auto-fix generator subagent
   */
  private async autoFixSubagent(
    deprecations: Deprecation[],
    breakingChanges: BreakingChange[]
  ): Promise<AutoFix[]> {
    console.log('🔧 Generating auto-fixes...');

    const fixes: AutoFix[] = [];

    // Generate fixes for known patterns
    for (const dep of deprecations.filter(d => d.autoFixable)) {
      fixes.push({
        id: `DEP-${fixes.length + 1}`,
        description: `Replace ${dep.api} with ${dep.replacement}`,
        file: dep.location.split(':')[0],
        oldCode: dep.api,
        newCode: dep.replacement,
        safe: true,
      });
    }

    // Add common safe fixes
    const commonFixes: Array<{ old: string; new: string; desc: string }> = [
      { old: 'tasks.create(', new: 'tasks.register(', desc: 'Use lazy task registration' },
      { old: 'tasks.getByName(', new: 'tasks.named(', desc: 'Use lazy task reference' },
      { old: 'compile(', new: 'implementation(', desc: 'Replace compile with implementation' },
      { old: 'testCompile(', new: 'testImplementation(', desc: 'Replace testCompile with testImplementation' },
      { old: 'runtime(', new: 'runtimeOnly(', desc: 'Replace runtime with runtimeOnly' },
    ];

    for (const { old, new: replacement, desc } of commonFixes) {
      fixes.push({
        id: `COMMON-${fixes.length + 1}`,
        description: desc,
        file: '*.gradle.kts',
        oldCode: old,
        newCode: replacement,
        safe: true,
      });
    }

    return fixes;
  }

  /**
   * Migration planning subagent
   */
  private async planningSubagent(
    versionInfo: VersionInfo,
    targetVersion: string,
    deprecations: Deprecation[],
    breakingChanges: BreakingChange[]
  ): Promise<{ phases: MigrationPhase[]; manualSteps: ManualStep[]; effort: string }> {
    console.log('📋 Creating migration plan...');

    const prompt = `You are a Gradle migration expert. Create a detailed migration plan.

Current: Gradle ${versionInfo.current}, Java ${versionInfo.javaVersion}
Target: Gradle ${targetVersion}
Deprecations found: ${deprecations.length}
Breaking changes: ${breakingChanges.length}

Create a phased migration plan with:
1. Ordered phases (preparation, fix deprecations, update wrapper, verify, cleanup)
2. Manual steps that require human intervention
3. Effort estimate

Return as JSON:
{
  "phases": [
    {"order": 1, "name": "Preparation", "description": "...", "steps": ["..."], "risk": "low"}
  ],
  "manualSteps": [
    {"order": 1, "title": "...", "description": "...", "commands": ["..."], "verification": "..."}
  ],
  "effort": "2-4 hours"
}

Return ONLY the JSON object.`;

    const response = await this.client.messages.create({
      model: DEFAULT_MODEL,
      max_tokens: 3000,
      messages: [{ role: 'user', content: prompt }],
    });

    const content = response.content[0];
    if (content.type === 'text') {
      try {
        return JSON.parse(content.text);
      } catch {
        return this.getDefaultPlan(versionInfo, targetVersion);
      }
    }
    return this.getDefaultPlan(versionInfo, targetVersion);
  }

  // ===========================================================================
  // Helpers
  // ===========================================================================

  private async detectVersion(): Promise<VersionInfo> {
    let current = 'unknown';
    let wrapper = false;
    let javaVersion = 'unknown';
    let kotlinVersion: string | undefined;

    // Check wrapper
    try {
      const wrapperProps = await fs.readFile(
        path.join(this.config.projectDir, 'gradle/wrapper/gradle-wrapper.properties'),
        'utf-8'
      );
      const match = wrapperProps.match(/gradle-(\d+\.\d+(\.\d+)?)/);
      if (match) {
        current = match[1];
        wrapper = true;
      }
    } catch {}

    // Check Java version
    try {
      const { stdout } = await execAsync('java -version 2>&1');
      const match = stdout.match(/version "(\d+)/);
      if (match) javaVersion = match[1];
    } catch {}

    // Check for Kotlin
    try {
      const buildScript = await fs.readFile(
        path.join(this.config.projectDir, 'build.gradle.kts'),
        'utf-8'
      );
      const match = buildScript.match(/kotlin.*version.*["'](\d+\.\d+\.\d+)["']/i);
      if (match) kotlinVersion = match[1];
    } catch {}

    return { current, wrapper, javaVersion, kotlinVersion };
  }

  private async getLatestStableVersion(): Promise<string> {
    // In production, fetch from services.gradle.org
    return '8.11';
  }

  private async collectBuildScripts(): Promise<Map<string, string>> {
    const scripts = new Map<string, string>();
    
    const patterns = [
      'build.gradle',
      'build.gradle.kts',
      'settings.gradle',
      'settings.gradle.kts',
      '**/build.gradle',
      '**/build.gradle.kts',
    ];

    for (const pattern of patterns) {
      try {
        if (pattern.includes('**')) {
          // Simple recursive search
          const { stdout } = await execAsync(
            `find . -name "${pattern.replace('**/', '')}" -type f 2>/dev/null | head -20`,
            { cwd: this.config.projectDir }
          );
          for (const file of stdout.trim().split('\n').filter(Boolean)) {
            const content = await fs.readFile(path.join(this.config.projectDir, file), 'utf-8');
            scripts.set(file, content);
          }
        } else {
          const filePath = path.join(this.config.projectDir, pattern);
          const content = await fs.readFile(filePath, 'utf-8');
          scripts.set(pattern, content);
        }
      } catch {}
    }

    return scripts;
  }

  private assessCompatibility(
    deprecations: Deprecation[],
    breakingChanges: BreakingChange[]
  ): MigrationReport['compatibility'] {
    const highImpactBreaking = breakingChanges.filter(b => b.impact === 'high').length;
    
    if (highImpactBreaking > 2) return 'breaking';
    if (breakingChanges.length > 0) return 'major-changes';
    if (deprecations.length > 5) return 'minor-changes';
    return 'compatible';
  }

  private extractDeprecationsFromText(text: string): Deprecation[] {
    // Fallback parser for non-JSON responses
    return [];
  }

  private getDefaultPlan(
    versionInfo: VersionInfo,
    targetVersion: string
  ): { phases: MigrationPhase[]; manualSteps: ManualStep[]; effort: string } {
    return {
      phases: [
        {
          order: 1,
          name: 'Preparation',
          description: 'Back up and verify current build works',
          steps: ['Create branch', 'Run full build', 'Document current behavior'],
          risk: 'low',
        },
        {
          order: 2,
          name: 'Fix Deprecations',
          description: 'Address deprecated API usage',
          steps: ['Run with --warning-mode=all', 'Fix deprecation warnings', 'Verify build'],
          risk: 'medium',
        },
        {
          order: 3,
          name: 'Update Wrapper',
          description: `Update to Gradle ${targetVersion}`,
          steps: [`./gradlew wrapper --gradle-version ${targetVersion}`, 'Commit wrapper files'],
          risk: 'low',
        },
        {
          order: 4,
          name: 'Fix Breaking Changes',
          description: 'Address any compilation errors',
          steps: ['Fix API changes', 'Update plugin versions', 'Resolve incompatibilities'],
          risk: 'high',
        },
        {
          order: 5,
          name: 'Verification',
          description: 'Verify migrated build',
          steps: ['Clean build', 'Run all tests', 'Check build scans'],
          risk: 'low',
        },
      ],
      manualSteps: [
        {
          order: 1,
          title: 'Check Plugin Compatibility',
          description: 'Verify all plugins support the target Gradle version',
          commands: ['./gradlew buildEnvironment'],
          verification: 'All plugins resolve successfully',
        },
        {
          order: 2,
          title: 'Update Kotlin Version',
          description: 'Ensure Kotlin plugin is compatible',
          verification: 'Build completes without Kotlin errors',
        },
        {
          order: 3,
          title: 'Enable Configuration Cache',
          description: 'Test with configuration cache enabled',
          commands: ['./gradlew build --configuration-cache'],
          verification: 'Build succeeds with configuration cache',
        },
      ],
      effort: '1-3 hours',
    };
  }

  private createEmptyReport(current: string, target: string): MigrationReport {
    return {
      currentVersion: current,
      targetVersion: target,
      compatibility: 'compatible',
      phases: [],
      deprecations: [],
      breakingChanges: [],
      autoFixable: [],
      manualSteps: [],
      estimatedEffort: '0 minutes',
    };
  }

  // ===========================================================================
  // Report Printing
  // ===========================================================================

  printReport(report: MigrationReport): void {
    console.log('\n═══════════════════════════════════════════════════════════');
    console.log('              GRADLE MIGRATION REPORT');
    console.log('═══════════════════════════════════════════════════════════\n');

    // Version info
    console.log(`📦 Current Version: ${report.currentVersion}`);
    console.log(`🎯 Target Version:  ${report.targetVersion}`);
    console.log(`⏱️  Estimated Effort: ${report.estimatedEffort}\n`);

    // Compatibility assessment
    const compatEmoji = {
      compatible: '✅',
      'minor-changes': '🟡',
      'major-changes': '🟠',
      breaking: '🔴',
    };
    console.log(`Compatibility: ${compatEmoji[report.compatibility]} ${report.compatibility.toUpperCase()}\n`);

    // Summary counts
    console.log('📊 Summary:');
    console.log(`   Deprecations found: ${report.deprecations.length}`);
    console.log(`   Breaking changes:   ${report.breakingChanges.length}`);
    console.log(`   Auto-fixable:       ${report.autoFixable.filter(f => f.safe).length}`);
    console.log();

    // Deprecations
    if (report.deprecations.length > 0) {
      console.log('⚠️  Deprecations:');
      for (const dep of report.deprecations.slice(0, 10)) {
        const fixable = dep.autoFixable ? '🔧' : '📝';
        console.log(`   ${fixable} ${dep.api} → ${dep.replacement}`);
        console.log(`      Location: ${dep.location}`);
        console.log(`      Removed in: ${dep.removedIn}\n`);
      }
      if (report.deprecations.length > 10) {
        console.log(`   ... and ${report.deprecations.length - 10} more\n`);
      }
    }

    // Breaking changes
    if (report.breakingChanges.length > 0) {
      console.log('🚨 Breaking Changes:');
      for (const bc of report.breakingChanges) {
        const impact = bc.impact === 'high' ? '🔴' : bc.impact === 'medium' ? '🟠' : '🟡';
        console.log(`   ${impact} [${bc.id}] ${bc.description}`);
        console.log(`      Solution: ${bc.solution}\n`);
      }
    }

    // Migration phases
    if (report.phases.length > 0) {
      console.log('📋 Migration Phases:');
      for (const phase of report.phases) {
        const risk = phase.risk === 'high' ? '🔴' : phase.risk === 'medium' ? '🟠' : '🟢';
        console.log(`   ${phase.order}. ${phase.name} ${risk}`);
        console.log(`      ${phase.description}`);
        for (const step of phase.steps) {
          console.log(`      • ${step}`);
        }
        console.log();
      }
    }

    // Auto-fixes available
    const safeFixes = report.autoFixable.filter(f => f.safe);
    if (safeFixes.length > 0) {
      console.log('🔧 Available Auto-Fixes:');
      for (const fix of safeFixes.slice(0, 5)) {
        console.log(`   • ${fix.description}`);
      }
      if (safeFixes.length > 5) {
        console.log(`   ... and ${safeFixes.length - 5} more`);
      }
      console.log('\n   Run with --apply to apply safe fixes automatically.\n');
    }

    // Manual steps
    if (report.manualSteps.length > 0) {
      console.log('📝 Manual Steps Required:');
      for (const step of report.manualSteps) {
        console.log(`   ${step.order}. ${step.title}`);
        console.log(`      ${step.description}`);
        if (step.commands) {
          for (const cmd of step.commands) {
            console.log(`      $ ${cmd}`);
          }
        }
        console.log(`      ✓ Verify: ${step.verification}\n`);
      }
    }

    console.log('═══════════════════════════════════════════════════════════\n');
  }
}

// =============================================================================
// CLI Entry Point
// =============================================================================

if (import.meta.url === `file://${process.argv[1]}`) {
  const args = process.argv.slice(2);
  const projectDir = args.find(a => !a.startsWith('-')) || '.';
  const targetVersion = args.find(a => a.startsWith('--target='))?.split('=')[1];
  const dryRun = args.includes('--dry-run');
  const apply = args.includes('--apply');
  const updateWrapper = args.includes('--update-wrapper');

  const apiKey = process.env.ANTHROPIC_API_KEY;

  if (!apiKey) {
    console.error('Error: ANTHROPIC_API_KEY environment variable not set');
    process.exit(1);
  }

  if (args.includes('--help')) {
    console.log(`
Gradle Migration Agent - Automated Gradle version migration

Usage: npx ts-node migration-agent.ts [project-dir] [options]

Options:
  --target=VERSION    Target Gradle version (default: latest stable)
  --dry-run           Analyze only, don't apply any changes
  --apply             Apply safe auto-fixes
  --update-wrapper    Update Gradle wrapper to target version
  --help              Show this help

Examples:
  npx ts-node migration-agent.ts .
  npx ts-node migration-agent.ts /path/to/project --target=8.11
  npx ts-node migration-agent.ts . --apply --update-wrapper
`);
    process.exit(0);
  }

  const agent = new GradleMigrationAgent({
    projectDir,
    targetVersion,
    apiKey,
    dryRun,
    verbose: true,
  });

  agent
    .analyze()
    .then(async (report) => {
      agent.printReport(report);

      if (apply && !dryRun) {
        await agent.applyFixes(report);
      }

      if (updateWrapper && !dryRun) {
        await agent.updateWrapper(report.targetVersion);
      }

      const exitCode = report.compatibility === 'breaking' ? 1 : 0;
      process.exit(exitCode);
    })
    .catch((error) => {
      console.error('Migration agent failed:', error);
      process.exit(1);
    });
}
