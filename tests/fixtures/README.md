# Test Fixtures

Test projects for validating the Gradle Expert Framework tools.

## Fixtures Overview

| Project | Gradle | DSL | Purpose | Expected Score |
|---------|--------|-----|---------|----------------|
| simple-java | 8.11 | Kotlin | Healthy baseline | 85-100 |
| config-cache-broken | 8.5 | Kotlin | Issue detection | 30-55 |
| legacy-groovy | 7.6 | Groovy | Migration testing | 25-45 |
| multi-module | 8.11 | Kotlin | Scale testing | 75-95 |
| spring-boot | 8.5 | Kotlin | Platform testing | 70-90 |
| ci-env-mismatch | 9.2.1 | Kotlin | CI vs LOCAL env detection | 70-90 |

## Fixture Details

### simple-java
**Purpose**: Healthy baseline - all best practices applied

Features:
- Gradle 8.11
- Version catalog
- All lazy task registration
- Configuration cache compatible
- All performance settings enabled
- Provider API usage

Expected: Health score 90+, zero issues

### config-cache-broken
**Purpose**: Test issue detection capabilities

Intentional issues:
- 3x `tasks.create()` (eager)
- 2x `tasks.getByName()` (eager)
- 3x `project.copy/exec/delete` in doLast
- 4x `System.getProperty()` / `System.getenv()`
- 3x `Task.project` access at execution time
- Direct `buildDir` access

Expected: 12-18 issues detected

### legacy-groovy
**Purpose**: Migration testing from 7.x to 8.x

Deprecated patterns:
- `archiveName` property
- `archivesBaseName` property
- `destinationDir` property
- `mainClassName` (old application property)
- Eager task creation with `task` keyword
- Old dependency configurations
- JUnit 4 instead of JUnit 5

Expected: 8-15 deprecations, 3-6 breaking changes for 8.x

### multi-module
**Purpose**: Multi-project build scale testing

Structure:
```
multi-module/
├── app/          (application)
├── core/         (library)
├── common/       (shared utilities)
└── api/          (interfaces)
```

Features:
- 4 subprojects with dependencies
- Version catalog
- Typesafe project accessors
- Root aggregation tasks

### spring-boot
**Purpose**: Platform/framework compatibility testing

Features:
- Spring Boot 3.2.0
- Spring dependency management
- Actuator endpoints
- Web starter

### ci-env-mismatch
**Purpose**: End-to-end testing of environment mismatch detection

This fixture tests the `/gradle:doctor` command's ability to detect the common "works in CI but not locally" problem.

Features:
- Develocity plugin (configurable via `DEVELOCITY_SERVER` env var, defaults to `ge.gradle.org`)
- `verifyEnvironment` task that requires `CI=true`
- Build scans tagged with `CI` or `LOCAL`
- Custom value capturing `CI_ENV` environment variable

Test scenario:
- `CI=true ./gradlew verifyEnvironment` → PASS (tag: CI)
- `./gradlew verifyEnvironment` → FAIL (tag: LOCAL)

**JUnit 5 Test**: `CiEnvMismatchE2ETest`
- Run with: `./gradlew develocityE2ETests`
- Requires `DEVELOCITY_ACCESS_KEY` and `ANTHROPIC_API_KEY` environment variables
- Creates real build scans on Develocity server (configurable via `DEVELOCITY_SERVER`)
- Verifies `/gradle:doctor` detects the environment mismatch

## Running Tests

All fixture tests are implemented as JUnit 5 tests.

### Prerequisites
```bash
# Install JBang (required for tool tests)
curl -Ls https://sh.jbang.dev | bash
```

### Run All Tests
```bash
cd tests
./gradlew test
```

### Run Specific Test Categories
```bash
# Fixture validation tests
./gradlew test --tests "*FixtureValidationTest"

# JBang tool output tests
./gradlew toolTests

# Develocity E2E tests (requires API keys)
./gradlew develocityE2ETests
```

### Manual Tool Testing
```bash
# Test individual tool (from plugin root)
jbang tools/build-health-check.java tests/fixtures/projects/simple-java

# With JSON output
jbang tools/task-analyzer.java tests/fixtures/projects/config-cache-broken --json
```

## Adding New Fixtures

1. Create project directory under `projects/`
2. Add required Gradle files:
   - `settings.gradle(.kts)`
   - `build.gradle(.kts)`
   - `gradle.properties`
   - `gradle/wrapper/gradle-wrapper.properties`
3. Update `FixtureValidationTest.java`:
   - Increment expected fixture count
   - Add to `expectedFixtures` list
   - Add to `fixturePurposes` map

## Test Matrix

| Tool | simple-java | config-cache-broken | legacy-groovy | multi-module | spring-boot | ci-env-mismatch |
|------|-------------|---------------------|---------------|--------------|-------------|-----------------|
| gradle-analyzer | yes | yes | yes | yes | yes | yes |
| build-health-check | yes | yes | yes | yes | yes | yes |
| task-analyzer | yes | yes | yes | yes | yes | yes |
| cache-validator | yes | yes | yes | yes | yes | yes |
| performance-profiler | limited | limited | limited | limited | limited | limited |

*limited = Requires actual Gradle execution

## End-to-End Tests

### CI Environment Mismatch Test

Tests the `/gradle:doctor` command's Develocity integration for detecting environment mismatches.

**JUnit 5 Test Class**: `com.gradle.claude.plugin.scenarios.CiEnvMismatchE2ETest`

```bash
# Add credentials to tests/local.env
echo "DEVELOCITY_ACCESS_KEY=your-ge-access-key" >> tests/local.env
echo "ANTHROPIC_API_KEY=your-anthropic-key" >> tests/local.env

# Run the E2E test
cd tests
./gradlew develocityE2ETests
```

**What it tests:**
1. Runs CI build with `CI=true` (passes, publishes build scan with "CI" tag)
2. Runs LOCAL build without CI (fails, publishes build scan with "LOCAL" tag)
3. Waits for build scans to be indexed on Develocity server (polls with timeout)
4. Runs `/gradle:doctor` agent and verifies it identifies the CI environment variable mismatch
5. Verifies doctor suggests the correct fix (`CI=true ./gradlew <task>`)

**Test Tags**: `@Tag("e2e")`, `@Tag("develocity")`
