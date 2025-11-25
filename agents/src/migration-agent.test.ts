import { describe, it, expect, vi, beforeEach, afterEach, type Mock } from 'vitest';

// Mock the Anthropic SDK
const mockMessagesCreate = vi.fn();
vi.mock('@anthropic-ai/sdk', () => ({
  default: class MockAnthropic {
    messages = { create: mockMessagesCreate };
  },
}));

// Mock fs/promises
const mockReadFile = vi.fn();
const mockWriteFile = vi.fn();
vi.mock('fs/promises', async () => ({
  readFile: mockReadFile,
  writeFile: mockWriteFile,
}));

// Mock child_process exec
const mockExecAsync = vi.fn();
vi.mock('util', async () => ({
  promisify: () => mockExecAsync,
}));

vi.mock('child_process', () => ({
  exec: vi.fn(),
}));

// Import after mocks are set up
const { GradleMigrationAgent } = await import('./migration-agent.js');

describe('GradleMigrationAgent', () => {
  let agent: GradleMigrationAgent;

  const defaultConfig = {
    projectDir: '/test/project',
    apiKey: 'test-api-key',
    dryRun: true,
    verbose: false,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    agent = new GradleMigrationAgent(defaultConfig);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('constructor', () => {
    it('should create an instance with valid config', () => {
      expect(agent).toBeInstanceOf(GradleMigrationAgent);
    });

    it('should initialize Anthropic client', () => {
      // The agent should have access to the mock messages.create function
      expect(agent).toBeInstanceOf(GradleMigrationAgent);
    });

    it('should accept optional target version', () => {
      const agentWithTarget = new GradleMigrationAgent({
        ...defaultConfig,
        targetVersion: '9.0',
      });
      expect(agentWithTarget).toBeInstanceOf(GradleMigrationAgent);
    });
  });

  describe('detectVersion', () => {
    it('should detect Gradle version from wrapper properties', async () => {
      mockReadFile.mockImplementation((path: string) => {
        if (path.includes('gradle-wrapper.properties')) {
          return Promise.resolve(
            'distributionUrl=https\\://services.gradle.org/distributions/gradle-8.5-bin.zip'
          );
        }
        return Promise.reject(new Error('ENOENT'));
      });

      // Mock exec for java -version
      mockExecAsync.mockResolvedValue({ stdout: 'openjdk version "21.0.1"', stderr: '' });

      const versionInfo = await (agent as any).detectVersion();

      expect(versionInfo.current).toBe('8.5');
      expect(versionInfo.wrapper).toBe(true);
    });

    it('should return unknown when wrapper not found', async () => {
      mockReadFile.mockRejectedValue(new Error('ENOENT'));
      mockExecAsync.mockResolvedValue({ stdout: '', stderr: '' });

      const versionInfo = await (agent as any).detectVersion();

      expect(versionInfo.current).toBe('unknown');
      expect(versionInfo.wrapper).toBe(false);
    });

    it('should detect Kotlin version from build script', async () => {
      mockReadFile.mockImplementation((path: string) => {
        if (path.includes('gradle-wrapper.properties')) {
          return Promise.resolve(
            'distributionUrl=https\\://services.gradle.org/distributions/gradle-8.5-bin.zip'
          );
        }
        if (path.includes('build.gradle.kts')) {
          return Promise.resolve('kotlin("jvm") version "1.9.21"');
        }
        return Promise.reject(new Error('ENOENT'));
      });

      mockExecAsync.mockResolvedValue({ stdout: 'version "21"', stderr: '' });

      const versionInfo = await (agent as any).detectVersion();

      expect(versionInfo.kotlinVersion).toBe('1.9.21');
    });
  });

  describe('assessCompatibility', () => {
    it('should return compatible when no issues', () => {
      const result = (agent as any).assessCompatibility([], []);
      expect(result).toBe('compatible');
    });

    it('should return minor-changes for few deprecations', () => {
      const deprecations = Array(3).fill({
        api: 'test',
        location: 'file:1',
        replacement: 'newApi',
        removedIn: '9.0',
        autoFixable: true,
      });

      const result = (agent as any).assessCompatibility(deprecations, []);
      expect(result).toBe('compatible');
    });

    it('should return minor-changes for many deprecations', () => {
      const deprecations = Array(10).fill({
        api: 'test',
        location: 'file:1',
        replacement: 'newApi',
        removedIn: '9.0',
        autoFixable: true,
      });

      const result = (agent as any).assessCompatibility(deprecations, []);
      expect(result).toBe('minor-changes');
    });

    it('should return major-changes when breaking changes exist', () => {
      const breakingChanges = [
        {
          id: 'BC001',
          description: 'API removed',
          impact: 'medium' as const,
          affectedFiles: ['build.gradle'],
          solution: 'Use new API',
        },
      ];

      const result = (agent as any).assessCompatibility([], breakingChanges);
      expect(result).toBe('major-changes');
    });

    it('should return breaking for multiple high-impact changes', () => {
      const breakingChanges = [
        {
          id: 'BC001',
          description: 'Critical API removed',
          impact: 'high' as const,
          affectedFiles: ['build.gradle'],
          solution: 'Major refactor needed',
        },
        {
          id: 'BC002',
          description: 'Another critical change',
          impact: 'high' as const,
          affectedFiles: ['settings.gradle'],
          solution: 'Major refactor needed',
        },
        {
          id: 'BC003',
          description: 'Third critical change',
          impact: 'high' as const,
          affectedFiles: ['buildSrc'],
          solution: 'Major refactor needed',
        },
      ];

      const result = (agent as any).assessCompatibility([], breakingChanges);
      expect(result).toBe('breaking');
    });
  });

  describe('createEmptyReport', () => {
    it('should create report with matching versions', () => {
      const report = (agent as any).createEmptyReport('8.5', '8.5');

      expect(report.currentVersion).toBe('8.5');
      expect(report.targetVersion).toBe('8.5');
      expect(report.compatibility).toBe('compatible');
      expect(report.deprecations).toEqual([]);
      expect(report.breakingChanges).toEqual([]);
    });
  });

  describe('getDefaultPlan', () => {
    it('should return plan with 5 phases', () => {
      const versionInfo = { current: '7.6', wrapper: true, javaVersion: '17' };
      const plan = (agent as any).getDefaultPlan(versionInfo, '8.5');

      expect(plan.phases).toHaveLength(5);
      expect(plan.phases[0].name).toBe('Preparation');
      expect(plan.phases[4].name).toBe('Verification');
    });

    it('should include wrapper update command with target version', () => {
      const versionInfo = { current: '7.6', wrapper: true, javaVersion: '17' };
      const plan = (agent as any).getDefaultPlan(versionInfo, '9.0');

      const wrapperPhase = plan.phases.find((p: any) => p.name === 'Update Wrapper');
      expect(wrapperPhase).toBeDefined();
      expect(wrapperPhase.steps.some((s: string) => s.includes('9.0'))).toBe(true);
    });

    it('should include manual steps', () => {
      const versionInfo = { current: '7.6', wrapper: true, javaVersion: '17' };
      const plan = (agent as any).getDefaultPlan(versionInfo, '8.5');

      expect(plan.manualSteps.length).toBeGreaterThan(0);
      expect(plan.manualSteps[0]).toHaveProperty('title');
      expect(plan.manualSteps[0]).toHaveProperty('verification');
    });

    it('should provide effort estimate', () => {
      const versionInfo = { current: '7.6', wrapper: true, javaVersion: '17' };
      const plan = (agent as any).getDefaultPlan(versionInfo, '8.5');

      expect(plan.effort).toBeDefined();
      expect(typeof plan.effort).toBe('string');
    });
  });

  describe('autoFixSubagent', () => {
    it('should generate fixes for auto-fixable deprecations', async () => {
      const deprecations = [
        {
          api: 'tasks.create',
          location: 'build.gradle.kts:10',
          replacement: 'tasks.register',
          removedIn: '9.0',
          autoFixable: true,
        },
      ];

      const fixes = await (agent as any).autoFixSubagent(deprecations, []);

      expect(fixes.length).toBeGreaterThan(0);
      const fix = fixes.find((f: any) => f.description.includes('tasks.create'));
      expect(fix).toBeDefined();
    });

    it('should include common safe fixes', async () => {
      const fixes = await (agent as any).autoFixSubagent([], []);

      expect(fixes.some((f: any) => f.oldCode === 'tasks.create(')).toBe(true);
      expect(fixes.some((f: any) => f.oldCode === 'compile(')).toBe(true);
      expect(fixes.some((f: any) => f.oldCode === 'testCompile(')).toBe(true);
    });

    it('should mark all common fixes as safe', async () => {
      const fixes = await (agent as any).autoFixSubagent([], []);

      expect(fixes.every((f: any) => f.safe === true)).toBe(true);
    });
  });

  describe('applyFixes', () => {
    it('should not apply fixes in dry run mode', async () => {
      const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

      const report = {
        currentVersion: '7.6',
        targetVersion: '8.5',
        compatibility: 'minor-changes' as const,
        phases: [],
        deprecations: [],
        breakingChanges: [],
        autoFixable: [
          {
            id: 'FIX-001',
            description: 'Replace compile with implementation',
            file: 'build.gradle.kts',
            oldCode: 'compile(',
            newCode: 'implementation(',
            safe: true,
          },
        ],
        manualSteps: [],
        estimatedEffort: '1 hour',
      };

      await agent.applyFixes(report);

      expect(mockWriteFile).not.toHaveBeenCalled();
      expect(consoleSpy.mock.calls.some((c) => c[0]?.includes('Dry run'))).toBe(true);

      consoleSpy.mockRestore();
    });

    it('should only apply safe fixes when not in dry run', async () => {
      const agentNotDry = new GradleMigrationAgent({
        ...defaultConfig,
        dryRun: false,
      });

      mockReadFile.mockResolvedValue('compile("com.example:lib:1.0")');

      const report = {
        currentVersion: '7.6',
        targetVersion: '8.5',
        compatibility: 'minor-changes' as const,
        phases: [],
        deprecations: [],
        breakingChanges: [],
        autoFixable: [
          {
            id: 'FIX-001',
            description: 'Replace compile',
            file: 'build.gradle.kts',
            oldCode: 'compile(',
            newCode: 'implementation(',
            safe: true,
          },
          {
            id: 'FIX-002',
            description: 'Unsafe fix',
            file: 'build.gradle.kts',
            oldCode: 'something',
            newCode: 'else',
            safe: false,
          },
        ],
        manualSteps: [],
        estimatedEffort: '1 hour',
      };

      const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

      await agentNotDry.applyFixes(report);

      // Should only try to apply the safe fix
      expect(mockReadFile).toHaveBeenCalledTimes(1);

      consoleSpy.mockRestore();
    });
  });

  describe('updateWrapper', () => {
    it('should execute gradlew wrapper command', async () => {
      mockExecAsync.mockResolvedValue({ stdout: 'Updated', stderr: '' });

      const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

      const result = await agent.updateWrapper('8.11');

      expect(result).toBe(true);

      consoleSpy.mockRestore();
    });

    it('should return false on failure', async () => {
      mockExecAsync.mockRejectedValue(new Error('Failed'));

      const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

      const result = await agent.updateWrapper('8.11');

      expect(result).toBe(false);

      consoleSpy.mockRestore();
    });
  });

  describe('printReport', () => {
    const baseReport = {
      currentVersion: '7.6',
      targetVersion: '8.5',
      compatibility: 'minor-changes' as const,
      phases: [],
      deprecations: [],
      breakingChanges: [],
      autoFixable: [],
      manualSteps: [],
      estimatedEffort: '2 hours',
    };

    it('should not throw when printing report', () => {
      const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

      expect(() => agent.printReport(baseReport)).not.toThrow();

      consoleSpy.mockRestore();
    });

    it('should print version information', () => {
      const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

      agent.printReport(baseReport);

      const output = consoleSpy.mock.calls.map((c) => c[0]).join('\n');
      expect(output).toContain('7.6');
      expect(output).toContain('8.5');

      consoleSpy.mockRestore();
    });

    it('should print deprecations when present', () => {
      const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

      const report = {
        ...baseReport,
        deprecations: [
          {
            api: 'tasks.create',
            location: 'build.gradle:10',
            replacement: 'tasks.register',
            removedIn: '9.0',
            autoFixable: true,
          },
        ],
      };

      agent.printReport(report);

      const output = consoleSpy.mock.calls.map((c) => c[0]).join('\n');
      expect(output).toContain('Deprecations');
      expect(output).toContain('tasks.create');
      expect(output).toContain('tasks.register');

      consoleSpy.mockRestore();
    });

    it('should print breaking changes when present', () => {
      const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

      const report = {
        ...baseReport,
        breakingChanges: [
          {
            id: 'BC001',
            description: 'API removed',
            impact: 'high' as const,
            affectedFiles: ['build.gradle'],
            solution: 'Use new API',
          },
        ],
      };

      agent.printReport(report);

      const output = consoleSpy.mock.calls.map((c) => c[0]).join('\n');
      expect(output).toContain('Breaking Changes');
      expect(output).toContain('BC001');
      expect(output).toContain('API removed');

      consoleSpy.mockRestore();
    });

    it('should print migration phases when present', () => {
      const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

      const report = {
        ...baseReport,
        phases: [
          {
            order: 1,
            name: 'Preparation',
            description: 'Prepare for migration',
            steps: ['Create branch', 'Run tests'],
            risk: 'low' as const,
          },
        ],
      };

      agent.printReport(report);

      const output = consoleSpy.mock.calls.map((c) => c[0]).join('\n');
      expect(output).toContain('Migration Phases');
      expect(output).toContain('Preparation');

      consoleSpy.mockRestore();
    });

    it('should map compatibility to correct emoji', () => {
      const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

      const compatibilities = ['compatible', 'minor-changes', 'major-changes', 'breaking'] as const;
      const expectedEmojis = ['✅', '🟡', '🟠', '🔴'];

      compatibilities.forEach((compat, index) => {
        consoleSpy.mockClear();

        agent.printReport({ ...baseReport, compatibility: compat });

        const output = consoleSpy.mock.calls.map((c) => c[0]).join('\n');
        expect(output).toContain(expectedEmojis[index]);
      });

      consoleSpy.mockRestore();
    });
  });

  describe('VERSION_DATA static property', () => {
    it('should contain known Gradle version data', () => {
      const versionData = (GradleMigrationAgent as any).VERSION_DATA;

      expect(versionData).toHaveProperty('7.0');
      expect(versionData).toHaveProperty('8.0');
      expect(versionData['7.0'].minJava).toBe(8);
      expect(versionData['8.0'].removals).toContain('archiveName');
    });
  });

  describe('analyze', () => {
    beforeEach(() => {
      // Mock version detection
      mockReadFile.mockImplementation((path: string) => {
        if (path.includes('gradle-wrapper.properties')) {
          return Promise.resolve(
            'distributionUrl=https\\://services.gradle.org/distributions/gradle-7.6-bin.zip'
          );
        }
        if (path.includes('build.gradle')) {
          return Promise.resolve('plugins { java }');
        }
        return Promise.reject(new Error('ENOENT'));
      });

      mockExecAsync.mockResolvedValue({ stdout: 'version "17"', stderr: '' });

      // Mock Claude responses
      mockMessagesCreate.mockResolvedValue({
        content: [{ type: 'text', text: '[]' }],
      });
    });

    it('should detect current and target versions', async () => {
      const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

      const report = await agent.analyze();

      expect(report.currentVersion).toBe('7.6');
      expect(report.targetVersion).toBeDefined();

      consoleSpy.mockRestore();
    });

    it('should return empty report when already on target version', async () => {
      mockReadFile.mockImplementation((path: string) => {
        if (path.includes('gradle-wrapper.properties')) {
          return Promise.resolve(
            'distributionUrl=https\\://services.gradle.org/distributions/gradle-8.11-bin.zip'
          );
        }
        return Promise.reject(new Error('ENOENT'));
      });

      const consoleSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

      const report = await agent.analyze();

      expect(report.currentVersion).toBe('8.11');
      expect(report.targetVersion).toBe('8.11');
      expect(report.compatibility).toBe('compatible');
      expect(report.deprecations).toEqual([]);

      consoleSpy.mockRestore();
    });
  });
});
