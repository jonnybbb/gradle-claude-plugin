# Gradle Expert Framework - Testing Strategy

## Overview

This document outlines a complete testing strategy covering all framework components:
- JBang Tools (5 tools)
- Claude Skills (10 skills)
- Claude Agents (2 agents)
- Documentation (32 reference docs)
- Examples (14 files)

## Test Fixtures Required

### Gradle Project Fixtures

We need test projects representing common real-world scenarios:

```
tests/fixtures/
├── projects/
│   ├── simple-java/              # Single module Java project
│   ├── simple-kotlin/            # Single module Kotlin project
│   ├── multi-module/             # Multi-project build (5+ modules)
│   ├── android-app/              # Android application
│   ├── spring-boot/              # Spring Boot application
│   ├── gradle-plugin/            # Gradle plugin project
│   ├── legacy-groovy/            # Groovy DSL, old patterns
│   ├── config-cache-clean/       # 100% config cache compatible
│   ├── config-cache-broken/      # Multiple config cache issues
│   ├── mixed-dsl/                # Both Kotlin and Groovy DSL
│   └── version-matrix/           # For testing multiple Gradle versions
│       ├── gradle-7.6/
│       ├── gradle-8.0/
│       ├── gradle-8.5/
│       └── gradle-8.11/
├── expected-outputs/             # Expected tool outputs for comparison
│   ├── simple-java/
│   │   ├── health-check.json
│   │   ├── task-analysis.json
│   │   └── gradle-analysis.json
│   └── ...
└── scripts/
    ├── setup-fixtures.sh         # Create/reset test fixtures
    └── verify-fixtures.sh        # Ensure fixtures build correctly
```

### Project Specifications

#### 1. simple-java
```
Purpose: Baseline healthy project
Gradle: 8.11
Features:
  - Version catalog
  - Convention plugins (buildSrc)
  - All lazy task registration
  - Config cache compatible
  - Build cache enabled
Expected health score: 90+
```

#### 2. config-cache-broken
```
Purpose: Test issue detection
Gradle: 8.5
Intentional issues:
  - 3x project.copy in doLast
  - 2x project.exec in doLast
  - 4x System.getProperty()
  - 2x System.getenv()
  - 3x tasks.create() (eager)
  - 2x tasks.getByName() (eager)
Expected: 15+ issues detected
```

#### 3. legacy-groovy
```
Purpose: Migration testing
Gradle: 7.6
Features:
  - All Groovy DSL
  - compile/testCompile configurations
  - archiveName usage
  - No version catalog
  - No config cache
Expected: Migration plan with 10+ deprecations
```

---

## Phase 1: Tool Testing

### 1.1 Unit Tests for JBang Tools

Each tool needs a test harness. Since JBang scripts are Java, we can use JUnit:

```java
// test/tools/GradleAnalyzerTest.java
public class GradleAnalyzerTest {
    
    @Test
    void analyzesSimpleProject() {
        var result = runTool("gradle-analyzer.java", 
            "tests/fixtures/projects/simple-java", "--json");
        
        var json = parseJson(result.stdout);
        assertThat(json.get("gradleVersion")).isEqualTo("8.11");
        assertThat(json.get("hasWrapper")).isTrue();
        assertThat(json.get("buildFiles")).hasSize(2);
    }
    
    @Test
    void handlesNonGradleDirectory() {
        var result = runTool("gradle-analyzer.java", "/tmp/empty");
        assertThat(result.exitCode).isEqualTo(1);
        assertThat(result.stderr).contains("Not a Gradle project");
    }
}
```

### 1.2 Tool Test Matrix

| Tool | Test Cases | Priority |
|------|------------|----------|
| gradle-analyzer.java | 12 | HIGH |
| cache-validator.java | 10 | HIGH |
| build-health-check.java | 15 | HIGH |
| task-analyzer.java | 18 | HIGH |
| performance-profiler.java | 8 | MEDIUM |

### 1.3 gradle-analyzer.java Tests

```
Test Cases:
├── Basic Analysis
│   ├── TC-GA-001: Analyze simple Java project
│   ├── TC-GA-002: Analyze multi-module project
│   ├── TC-GA-003: Analyze Android project
│   └── TC-GA-004: Analyze Kotlin DSL project
├── Edge Cases
│   ├── TC-GA-005: Missing gradle wrapper
│   ├── TC-GA-006: Empty project (no build files)
│   ├── TC-GA-007: Corrupted build files
│   └── TC-GA-008: Very large project (100+ modules)
├── Output Formats
│   ├── TC-GA-009: Human-readable output
│   ├── TC-GA-010: JSON output parsing
│   └── TC-GA-011: Output with --verbose
└── Version Compatibility
    ├── TC-GA-012: Gradle 7.6 project
    ├── TC-GA-013: Gradle 8.0 project
    └── TC-GA-014: Gradle 8.11 project
```

### 1.4 build-health-check.java Tests

```
Test Cases:
├── Scoring
│   ├── TC-BH-001: Perfect score project (90+)
│   ├── TC-BH-002: Needs attention project (50-80)
│   ├── TC-BH-003: Critical score project (<50)
│   └── TC-BH-004: Score calculation accuracy
├── Category Detection
│   ├── TC-BH-005: Performance settings detection
│   ├── TC-BH-006: Caching setup detection
│   ├── TC-BH-007: Build structure detection
│   ├── TC-BH-008: Task quality scoring
│   └── TC-BH-009: Dependency management scoring
├── Recommendations
│   ├── TC-BH-010: Prioritized recommendations
│   ├── TC-BH-011: Quick wins identification
│   └── TC-BH-012: Recommendation accuracy
└── Output
    ├── TC-BH-013: Human-readable format
    ├── TC-BH-014: JSON output
    └── TC-BH-015: Verbose mode details
```

### 1.5 task-analyzer.java Tests

```
Test Cases:
├── Pattern Detection
│   ├── TC-TA-001: Detect tasks.create() usage
│   ├── TC-TA-002: Detect tasks.getByName() usage
│   ├── TC-TA-003: Detect project.copy in doLast
│   ├── TC-TA-004: Detect project.exec in doLast
│   ├── TC-TA-005: Detect System.getProperty()
│   ├── TC-TA-006: Detect System.getenv()
│   ├── TC-TA-007: Count tasks.register() (positive)
│   └── TC-TA-008: Count tasks.named() (positive)
├── Scoring
│   ├── TC-TA-009: Lazy registration score calculation
│   ├── TC-TA-010: Overall score with critical issues
│   └── TC-TA-011: Overall score with warnings only
├── Auto-Fix
│   ├── TC-TA-012: --fix replaces create with register
│   ├── TC-TA-013: --fix replaces getByName with named
│   ├── TC-TA-014: --fix preserves complex patterns
│   └── TC-TA-015: --fix creates backup
├── File Handling
│   ├── TC-TA-016: Analyze .gradle.kts files
│   ├── TC-TA-017: Analyze .gradle files
│   ├── TC-TA-018: Analyze buildSrc files
│   └── TC-TA-019: Skip .gradle and build directories
```

### 1.6 Tool Test Execution

```bash
#!/bin/bash
# scripts/test-tools.sh

set -e

FIXTURES="tests/fixtures/projects"
TOOLS="tools"

echo "=== Tool Tests ==="

# Test each tool against fixtures
for project in simple-java multi-module config-cache-broken legacy-groovy; do
    echo "Testing against: $project"
    
    # gradle-analyzer
    jbang $TOOLS/gradle-analyzer.java $FIXTURES/$project --json > /tmp/ga-result.json
    diff /tmp/ga-result.json tests/fixtures/expected-outputs/$project/gradle-analysis.json
    
    # build-health-check
    jbang $TOOLS/build-health-check.java $FIXTURES/$project --json > /tmp/bh-result.json
    # Validate score within expected range
    score=$(jq '.overallScore' /tmp/bh-result.json)
    expected_min=$(jq '.expectedScoreMin' tests/fixtures/expected-outputs/$project/health-check.json)
    expected_max=$(jq '.expectedScoreMax' tests/fixtures/expected-outputs/$project/health-check.json)
    
    if [[ $score -lt $expected_min || $score -gt $expected_max ]]; then
        echo "FAIL: Score $score not in range [$expected_min, $expected_max]"
        exit 1
    fi
    
    # task-analyzer
    jbang $TOOLS/task-analyzer.java $FIXTURES/$project --json > /tmp/ta-result.json
    # Validate issue counts
    
    echo "✅ $project passed"
done

echo "=== All tool tests passed ==="
```

---

## Phase 2: Documentation Testing

### 2.1 Code Example Validation

All code examples in documentation must compile:

```kotlin
// test/docs/CodeExampleTest.kt
class CodeExampleTest {
    
    @ParameterizedTest
    @MethodSource("kotlinExamples")
    fun `kotlin examples compile`(example: CodeExample) {
        val result = compileKotlinScript(example.code)
        assertThat(result.success)
            .withFailMessage("${example.file}:${example.line} - ${result.error}")
            .isTrue()
    }
    
    @ParameterizedTest
    @MethodSource("groovyExamples")  
    fun `groovy examples compile`(example: CodeExample) {
        val result = compileGroovyScript(example.code)
        assertThat(result.success)
            .withFailMessage("${example.file}:${example.line} - ${result.error}")
            .isTrue()
    }
}
```

### 2.2 Documentation Test Matrix

| Category | Files | Tests |
|----------|-------|-------|
| Reference Docs | 32 | Code compilation, link validation |
| Skill SKILL.md | 10 | Format validation, reference links |
| Skill References | 18 | Code compilation |
| Examples | 14 | Full build execution |

### 2.3 Link Validation

```bash
#!/bin/bash
# scripts/validate-links.sh

# Find all markdown files
find docs skills examples -name "*.md" | while read file; do
    # Extract internal links
    grep -oE '\[.*\]\(([^)]+)\)' "$file" | while read link; do
        target=$(echo "$link" | sed 's/.*(\([^)]*\)).*/\1/')
        
        # Skip external links
        [[ $target == http* ]] && continue
        
        # Check file exists
        dir=$(dirname "$file")
        if [[ ! -f "$dir/$target" && ! -f "$target" ]]; then
            echo "BROKEN: $file -> $target"
        fi
    done
done
```

### 2.4 Example Project Testing

```bash
#!/bin/bash
# scripts/test-examples.sh

set -e

echo "=== Example Tests ==="

# Test multi-project example builds
cd examples/sample-projects/multi-project

# Should pass with config cache
./gradlew build --configuration-cache --dry-run

# Verify structure
[[ -f settings.gradle.kts ]] || exit 1
[[ -f build.gradle.kts ]] || exit 1
[[ -f gradle.properties ]] || exit 1
[[ -f buildSrc/build.gradle.kts ]] || exit 1

echo "✅ Example projects valid"
```

---

## Phase 3: Skill Testing

### 3.1 Skill Testing Approach

Skills are tested via Claude API calls with specific prompts:

```typescript
// test/skills/skill-test-harness.ts

interface SkillTestCase {
    name: string;
    triggerPrompt: string;
    expectedBehaviors: string[];
    forbiddenPatterns: string[];
}

async function testSkill(skill: string, testCase: SkillTestCase) {
    const response = await claude.messages.create({
        model: 'claude-sonnet-4-20250514',
        system: loadSkill(skill),
        messages: [{ role: 'user', content: testCase.triggerPrompt }]
    });
    
    const content = response.content[0].text;
    
    // Verify expected behaviors
    for (const behavior of testCase.expectedBehaviors) {
        expect(content).toContain(behavior);
    }
    
    // Verify no forbidden patterns
    for (const pattern of testCase.forbiddenPatterns) {
        expect(content).not.toContain(pattern);
    }
}
```

### 3.2 Skill Test Cases

#### gradle-config-cache

```yaml
test_cases:
  - name: "Trigger on config cache error"
    prompt: "I'm getting this error: 'Invocation of Task.project at execution time is unsupported'"
    expected:
      - "providers.systemProperty" 
      - "layout.buildDirectory"
      - "inject"
    forbidden:
      - "project.buildDir"
      - "System.getProperty"
      
  - name: "Trigger on enable request"
    prompt: "How do I enable configuration cache in Gradle 8?"
    expected:
      - "org.gradle.configuration-cache=true"
      - "gradle.properties"
      
  - name: "Fix project.copy issue"
    prompt: "My build fails with config cache because I use project.copy in doLast"
    expected:
      - "FileSystemOperations"
      - "@Inject"
      - "abstract"
```

#### gradle-performance

```yaml
test_cases:
  - name: "Slow build complaint"
    prompt: "My Gradle build takes 5 minutes, how can I speed it up?"
    expected:
      - "org.gradle.parallel=true"
      - "org.gradle.caching=true"
      - "configuration-cache"
    
  - name: "JVM tuning request"
    prompt: "What JVM args should I use for Gradle?"
    expected:
      - "-Xmx"
      - "org.gradle.jvmargs"
      - "HeapDumpOnOutOfMemoryError"
```

### 3.3 Skill Test Matrix

| Skill | Test Cases | Priority |
|-------|------------|----------|
| gradle-config-cache | 8 | HIGH |
| gradle-build-cache | 6 | HIGH |
| gradle-task-development | 7 | MEDIUM |
| gradle-plugin-development | 5 | MEDIUM |
| gradle-dependencies | 6 | MEDIUM |
| gradle-structure | 4 | LOW |
| gradle-migration | 6 | HIGH |
| gradle-troubleshooting | 8 | HIGH |
| gradle-doctor | 5 | MEDIUM |
| gradle-performance | 7 | HIGH |

---

## Phase 4: Agent Testing

### 4.1 Agent Test Approach

Agents require integration tests with mocked external dependencies:

```typescript
// test/agents/doctor-agent.test.ts

describe('GradleDoctorAgent', () => {
    let agent: GradleDoctorAgent;
    let mockAnthropic: MockAnthropic;
    
    beforeEach(() => {
        mockAnthropic = new MockAnthropic();
        agent = new GradleDoctorAgent({
            projectDir: 'tests/fixtures/projects/simple-java',
            jbangToolsDir: 'tools',
            apiKey: 'test-key'
        });
    });
    
    it('runs all subagents', async () => {
        const report = await agent.diagnose();
        
        expect(report.sections.performance).toBeDefined();
        expect(report.sections.caching).toBeDefined();
        expect(report.sections.dependencies).toBeDefined();
        expect(report.sections.structure).toBeDefined();
    });
    
    it('synthesizes recommendations', async () => {
        const report = await agent.diagnose();
        
        expect(report.recommendations.length).toBeGreaterThan(0);
        expect(report.recommendations[0]).toHaveProperty('priority');
        expect(report.recommendations[0]).toHaveProperty('title');
    });
    
    it('calculates overall health correctly', async () => {
        const report = await agent.diagnose();
        
        expect(['healthy', 'needs-attention', 'critical'])
            .toContain(report.overall);
    });
});
```

### 4.2 Migration Agent Tests

```typescript
// test/agents/migration-agent.test.ts

describe('GradleMigrationAgent', () => {
    
    it('detects current Gradle version', async () => {
        const agent = new GradleMigrationAgent({
            projectDir: 'tests/fixtures/projects/legacy-groovy',
            apiKey: 'test-key'
        });
        
        const report = await agent.analyze();
        expect(report.currentVersion).toBe('7.6');
    });
    
    it('finds deprecations for 7.6 -> 8.11', async () => {
        const agent = new GradleMigrationAgent({
            projectDir: 'tests/fixtures/projects/legacy-groovy',
            targetVersion: '8.11',
            apiKey: 'test-key'
        });
        
        const report = await agent.analyze();
        
        expect(report.deprecations.length).toBeGreaterThan(5);
        expect(report.deprecations.some(d => d.api.includes('archiveName'))).toBe(true);
    });
    
    it('generates safe auto-fixes', async () => {
        const report = await agent.analyze();
        
        const safeFixes = report.autoFixable.filter(f => f.safe);
        expect(safeFixes.length).toBeGreaterThan(0);
        
        // Verify fixes don't include dangerous patterns
        for (const fix of safeFixes) {
            expect(fix.newCode).not.toContain('rm -rf');
            expect(fix.newCode).not.toContain('DELETE');
        }
    });
    
    it('applies fixes in dry-run mode without changes', async () => {
        const agent = new GradleMigrationAgent({
            projectDir: 'tests/fixtures/projects/legacy-groovy',
            dryRun: true,
            apiKey: 'test-key'
        });
        
        const before = readFile('tests/fixtures/projects/legacy-groovy/build.gradle');
        await agent.applyFixes(await agent.analyze());
        const after = readFile('tests/fixtures/projects/legacy-groovy/build.gradle');
        
        expect(before).toBe(after);
    });
});
```

### 4.3 Agent Test Matrix

| Agent | Test Cases | Focus Areas |
|-------|------------|-------------|
| doctor-agent | 12 | Subagent orchestration, synthesis, recommendations |
| migration-agent | 15 | Version detection, deprecation scanning, safe fixes |

---

## Phase 5: Integration Testing

### 5.1 End-to-End Workflows

Test complete user workflows:

```bash
#!/bin/bash
# test/e2e/full-workflow.sh

PROJECT="tests/fixtures/projects/legacy-groovy"

echo "=== E2E: Full Migration Workflow ==="

# Step 1: Initial health check
echo "Step 1: Health check"
jbang tools/build-health-check.java $PROJECT --json > /tmp/before.json
before_score=$(jq '.overallScore' /tmp/before.json)
echo "Initial score: $before_score"

# Step 2: Task analysis
echo "Step 2: Task analysis"
jbang tools/task-analyzer.java $PROJECT --json > /tmp/tasks.json
issues=$(jq '.issues | length' /tmp/tasks.json)
echo "Issues found: $issues"

# Step 3: Apply safe fixes
echo "Step 3: Apply fixes"
jbang tools/task-analyzer.java $PROJECT --fix

# Step 4: Verify build still works
echo "Step 4: Verify build"
cd $PROJECT && ./gradlew build --dry-run && cd -

# Step 5: Re-check health
echo "Step 5: Re-check health"
jbang tools/build-health-check.java $PROJECT --json > /tmp/after.json
after_score=$(jq '.overallScore' /tmp/after.json)
echo "Final score: $after_score"

# Verify improvement
if [[ $after_score -le $before_score ]]; then
    echo "FAIL: Score did not improve"
    exit 1
fi

echo "✅ E2E workflow passed: $before_score -> $after_score"
```

### 5.2 Integration Test Scenarios

| Scenario | Description | Components |
|----------|-------------|------------|
| New Project Setup | User sets up optimized new project | Skills, Examples |
| Legacy Migration | Migrate Gradle 7.x to 8.x | Migration Agent, Tools |
| Performance Tuning | Optimize slow build | Doctor Agent, Performance Skill |
| Config Cache Adoption | Enable configuration cache | Tools, Config Cache Skill |
| CI/CD Setup | Configure for CI environment | Skills, Documentation |

---

## Phase 6: Regression Testing

### 6.1 Snapshot Testing

Save expected outputs and compare on each run:

```bash
# Generate snapshots
jbang tools/build-health-check.java tests/fixtures/projects/simple-java --json \
    > tests/fixtures/snapshots/simple-java-health.json

# Compare on test run
jbang tools/build-health-check.java tests/fixtures/projects/simple-java --json \
    | diff - tests/fixtures/snapshots/simple-java-health.json
```

### 6.2 Version Compatibility Matrix

Test across Gradle versions:

| Gradle Version | Tool Compatibility | Skill Accuracy |
|----------------|-------------------|----------------|
| 7.6 | ✅ | ✅ |
| 8.0 | ✅ | ✅ |
| 8.5 | ✅ | ✅ |
| 8.11 | ✅ | ✅ |

---

## Test Execution Plan

### CI/CD Pipeline

```yaml
# .github/workflows/test.yml
name: Test Framework

on: [push, pull_request]

jobs:
  tool-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: jbangdev/setup-jbang@v3
      - name: Setup fixtures
        run: ./scripts/setup-fixtures.sh
      - name: Run tool tests
        run: ./scripts/test-tools.sh
        
  doc-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Validate code examples
        run: ./scripts/validate-code-examples.sh
      - name: Validate links
        run: ./scripts/validate-links.sh
        
  example-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: gradle/actions/setup-gradle@v3
      - name: Test examples build
        run: ./scripts/test-examples.sh
        
  agent-tests:
    runs-on: ubuntu-latest
    env:
      ANTHROPIC_API_KEY: ${{ secrets.ANTHROPIC_API_KEY }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
      - name: Install dependencies
        run: cd agents && npm install
      - name: Run agent tests
        run: cd agents && npm test
        
  e2e-tests:
    needs: [tool-tests, doc-tests]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run E2E tests
        run: ./test/e2e/full-workflow.sh
```

### Local Test Commands

```bash
# Run all tests
./scripts/test-all.sh

# Run specific test suites
./scripts/test-tools.sh
./scripts/test-docs.sh
./scripts/test-examples.sh
./scripts/test-agents.sh

# Run single tool test
jbang test/tools/GradleAnalyzerTest.java

# Update snapshots
./scripts/update-snapshots.sh
```

---

## Test Metrics & Coverage

### Target Metrics

| Metric | Target |
|--------|--------|
| Tool test coverage | 90%+ |
| Documentation code validation | 100% |
| Skill trigger accuracy | 95%+ |
| Agent success rate | 90%+ |
| E2E pass rate | 100% |

### Coverage Reports

```
test-reports/
├── tools/
│   ├── coverage.html
│   └── junit.xml
├── docs/
│   ├── code-validation.html
│   └── link-validation.html
├── skills/
│   └── accuracy-report.html
├── agents/
│   └── integration-report.html
└── e2e/
    └── workflow-report.html
```

---

## Implementation Priority

### Phase 1 (Week 1): Foundation
1. Create test fixtures (5 projects)
2. Tool unit test harness
3. Basic tool tests (critical paths)

### Phase 2 (Week 2): Documentation
4. Code example validation
5. Link validation
6. Example project tests

### Phase 3 (Week 3): Skills & Agents
7. Skill test harness
8. High-priority skill tests
9. Agent integration tests

### Phase 4 (Week 4): E2E & Polish
10. E2E workflow tests
11. CI/CD pipeline
12. Coverage reporting

---

## Estimated Effort

| Phase | Effort | Output |
|-------|--------|--------|
| Test Fixtures | 4 hours | 5 projects, scripts |
| Tool Tests | 8 hours | 50+ test cases |
| Doc Tests | 4 hours | Validation scripts |
| Skill Tests | 6 hours | 60+ test cases |
| Agent Tests | 6 hours | 27 test cases |
| E2E Tests | 4 hours | 5 workflows |
| CI/CD Setup | 3 hours | GitHub Actions |
| **Total** | **35 hours** | Complete test suite |

---

## Next Steps

1. **Create test fixture projects** - Start with simple-java and config-cache-broken
2. **Build tool test harness** - JUnit-based testing for JBang scripts
3. **Automate in CI** - GitHub Actions workflow

Ready to proceed with implementation?
