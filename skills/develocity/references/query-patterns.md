# Develocity Query Patterns

## Table of Contents
- [Build Analysis Patterns](#build-analysis-patterns)
- [Test Analysis Patterns](#test-analysis-patterns)
- [Performance Analysis Patterns](#performance-analysis-patterns)
- [Troubleshooting Patterns](#troubleshooting-patterns)
- [Advanced Filtering](#advanced-filtering)

## Build Analysis Patterns

### Build Success Rate Over Time
```
mcp__develocity__getBuilds:
  project: "my-project"
  fromDate: "2025-01-01"
  toDate: "2025-01-25"
  maxBuilds: 500
  additionalDataToInclude: ["attributes"]
```
Calculate: `succeeded / total * 100`

### Compare CI vs Local Builds
```
# CI builds
mcp__develocity__getBuilds:
  project: "my-project"
  userTags: ["CI"]
  fromDate: "2025-01-18"
  maxBuilds: 100
  additionalDataToInclude: ["build_performance", "caching"]

# Local builds
mcp__develocity__getBuilds:
  project: "my-project"
  userTags: ["LOCAL"]
  fromDate: "2025-01-18"
  maxBuilds: 100
  additionalDataToInclude: ["build_performance", "caching"]
```

### Builds by Specific User
```
mcp__develocity__getBuilds:
  project: "my-project"
  username: "john.doe"
  fromDate: "2025-01-18"
  maxBuilds: 50
```

### Builds on Specific Branch
```
mcp__develocity__getBuilds:
  project: "my-project"
  userTags: ["main"]  # Branch name as tag
  fromDate: "2025-01-18"
```

### Long-Running Builds
```
mcp__develocity__getBuilds:
  project: "my-project"
  buildDuration: ">10m"
  fromDate: "2025-01-18"
  additionalDataToInclude: ["build_performance"]
```

### Builds with Specific Tasks
```
mcp__develocity__getBuilds:
  project: "my-project"
  requestedTargets: ["build", "test"]
  fromDate: "2025-01-18"
```

## Test Analysis Patterns

### All Flaky Tests (Summary)
```
mcp__develocity__getTestResults:
  project: "my-project"
  fromDate: "2025-01-18"
  includeOutcomes: ["flaky"]
```

### Flaky Tests in Specific Package
```
mcp__develocity__getTestResults:
  project: "my-project"
  fromDate: "2025-01-18"
  includeOutcomes: ["flaky"]
  testContainer: "com.example.service.*"
```

### Failed Test Cases with Details
```
mcp__develocity__getTestResults:
  project: "my-project"
  fromDate: "2025-01-18"
  includeOutcomes: ["failed"]
  testContainer: "com.example.UserServiceTest"
  includeTestCases: true
  limit: 50
```

### Most Frequently Failing Tests
```
mcp__develocity__getTestResults:
  project: "my-project"
  fromDate: "2025-01-01"  # Longer time range
  includeOutcomes: ["failed"]
  limit: 100
```
Sort by failure count to find most problematic tests.

### Skipped Tests Analysis
```
mcp__develocity__getTestResults:
  project: "my-project"
  fromDate: "2025-01-18"
  includeOutcomes: ["skipped"]
```

## Performance Analysis Patterns

### Build Time Trends
```
mcp__develocity__getBuilds:
  project: "my-project"
  fromDate: "2025-01-01"
  maxBuilds: 200
  additionalDataToInclude: ["build_performance"]
```
Track avg build time over weeks.

### Cache Efficiency Analysis
```
mcp__develocity__getBuilds:
  project: "my-project"
  fromDate: "2025-01-18"
  maxBuilds: 100
  additionalDataToInclude: ["caching"]
```
Look for: cache hit rate, cache misses by task type.

### Configuration Time Analysis
```
mcp__develocity__getBuilds:
  project: "my-project"
  fromDate: "2025-01-18"
  maxBuilds: 50
  additionalDataToInclude: ["build_performance", "attributes"]
```
Check configuration vs execution time ratio.

### Slowest Tasks Identification
```
mcp__develocity__getBuilds:
  project: "my-project"
  fromDate: "2025-01-18"
  maxBuilds: 20
  additionalDataToInclude: ["build_performance", "caching"]
```
Caching data includes slowest 100 task executions per build.

## Troubleshooting Patterns

### Recent Failures with Stack Traces
```
mcp__develocity__getBuilds:
  project: "my-project"
  buildOutcome: "failed"
  fromDate: "2025-01-18"
  maxBuilds: 10
  additionalDataToInclude: ["failures"]
```

### OOM Failures
Query failures then filter results for `OutOfMemoryError` in stack traces.
```
mcp__develocity__getBuilds:
  project: "my-project"
  buildOutcome: "failed"
  fromDate: "2025-01-18"
  maxBuilds: 50
  additionalDataToInclude: ["failures", "attributes"]
```

### Compilation Failures
```
mcp__develocity__getBuilds:
  project: "my-project"
  buildOutcome: "failed"
  fromDate: "2025-01-18"
  additionalDataToInclude: ["failures"]
```
Filter for `CompilationFailedException` in results.

### Test Failures in CI
```
mcp__develocity__getBuilds:
  project: "my-project"
  buildOutcome: "failed"
  userTags: ["CI"]
  fromDate: "2025-01-18"
  additionalDataToInclude: ["failures", "test_performance"]
```

## Advanced Filtering

### Exclude Specific Users
```
mcp__develocity__getBuilds:
  project: "my-project"
  username: "not:bot-user"
  fromDate: "2025-01-18"
```

### Exclude CI Builds
```
mcp__develocity__getBuilds:
  project: "my-project"
  userTags: ["not:CI"]
  fromDate: "2025-01-18"
```

### Wildcard Project Matching
```
mcp__develocity__getBuilds:
  project: "my-*"  # All projects starting with "my-"
  fromDate: "2025-01-18"
  maxBuilds: 100
```

### Custom Key-Value Filtering
```
mcp__develocity__getBuilds:
  project: "my-project"
  values: ["environment=production", "team=platform"]
  fromDate: "2025-01-18"
```

### Combined Filters
```
mcp__develocity__getBuilds:
  project: "my-project"
  username: "john.doe"
  userTags: ["CI", "main"]
  buildOutcome: "failed"
  fromDate: "2025-01-18"
  maxBuilds: 20
  additionalDataToInclude: ["failures", "build_performance"]
```

## Data Interpretation Tips

### Success Rate Thresholds
- > 95%: Healthy
- 80-95%: Needs attention
- < 80%: Critical - investigate immediately

### Cache Hit Rate Thresholds
- > 70%: Good
- 50-70%: Room for improvement
- < 50%: Cache configuration issues likely

### Flaky Test Thresholds
- < 5% flaky rate: Acceptable
- 5-15% flaky rate: Should fix
- > 15% flaky rate: Critical - blocks reliable CI

### Build Time Variance
High variance in build times often indicates:
- Inconsistent cache hits
- Resource contention
- Flaky network dependencies
