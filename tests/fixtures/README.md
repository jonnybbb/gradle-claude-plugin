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

## Fixture Details

### simple-java
**Purpose**: Healthy baseline - all best practices applied

Features:
- ✅ Gradle 8.11
- ✅ Version catalog
- ✅ All lazy task registration
- ✅ Configuration cache compatible
- ✅ All performance settings enabled
- ✅ Provider API usage

Expected: Health score 90+, zero issues

### config-cache-broken  
**Purpose**: Test issue detection capabilities

Intentional issues:
- ❌ 3x `tasks.create()` (eager)
- ❌ 2x `tasks.getByName()` (eager)
- ❌ 3x `project.copy/exec/delete` in doLast
- ❌ 4x `System.getProperty()` / `System.getenv()`
- ❌ 3x `Task.project` access at execution time
- ❌ Direct `buildDir` access

Expected: 12-18 issues detected

### legacy-groovy
**Purpose**: Migration testing from 7.x to 8.x

Deprecated patterns:
- ❌ `archiveName` property
- ❌ `archivesBaseName` property
- ❌ `destinationDir` property
- ❌ `mainClassName` (old application property)
- ❌ Eager task creation with `task` keyword
- ❌ Old dependency configurations
- ❌ JUnit 4 instead of JUnit 5

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

## Running Tests

### Prerequisites
```bash
# Install JBang
curl -Ls https://sh.jbang.dev | bash

# Install jq (for JSON parsing)
brew install jq  # macOS
apt install jq   # Linux
```

### Setup
```bash
cd tests/fixtures
./scripts/setup-fixtures.sh
```

### Run Tests
```bash
./scripts/test-tools.sh
```

### Manual Testing
```bash
# Test individual tool (from plugin root)
jbang tools/build-health-check.java tests/fixtures/projects/simple-java

# With JSON output
jbang tools/task-analyzer.java tests/fixtures/projects/config-cache-broken --json
```

## Expected Outputs

The `expected-outputs/` directory contains JSON files with expected results:

```json
{
  "healthCheck": {
    "expectedScoreMin": 85,
    "expectedScoreMax": 100,
    "expectedStatus": "HEALTHY"
  },
  "taskAnalysis": {
    "expectedEagerCreates": 0,
    "expectedConfigCacheIssues": 0
  }
}
```

## Adding New Fixtures

1. Create project directory under `projects/`
2. Add required Gradle files:
   - `settings.gradle(.kts)`
   - `build.gradle(.kts)`
   - `gradle.properties`
   - `gradle/wrapper/gradle-wrapper.properties`
3. Add expected output in `expected-outputs/`
4. Update `scripts/setup-fixtures.sh`

## Test Matrix

| Tool | simple-java | config-cache-broken | legacy-groovy | multi-module | spring-boot |
|------|-------------|---------------------|---------------|--------------|-------------|
| gradle-analyzer | ✅ | ✅ | ✅ | ✅ | ✅ |
| build-health-check | ✅ | ✅ | ✅ | ✅ | ✅ |
| task-analyzer | ✅ | ✅ | ✅ | ✅ | ✅ |
| cache-validator | ✅ | ✅ | ✅ | ✅ | ✅ |
| performance-profiler | ⚠️ | ⚠️ | ⚠️ | ⚠️ | ⚠️ |

⚠️ = Requires actual Gradle execution (limited in CI)
