---
name: develocity
description: This skill should be used when the user asks about "my builds", "build failures", "flaky tests", "cache hit rate", "build performance", "Build Scan", "Develocity", "build history", "CI builds", "test results", or wants to analyze patterns across builds using MCP tools.
---

# Develocity

Query Gradle Build Scan data via `mcp__develocity__getBuilds` and `mcp__develocity__getTestResults` MCP tools.

## Context Gathering (Required First Step)

Before ANY Develocity query, establish context. The MCP server URL/credentials are environment-specific.

**Auto-detect:**
```bash
# Project name
grep -E "^rootProject.name" settings.gradle* 2>/dev/null | head -1

# Current branch
git branch --show-current 2>/dev/null

# Git username
git config user.name 2>/dev/null
```

**Ask user when unclear:**

| Context | When to ask |
|---------|-------------|
| Project | Cannot detect `rootProject.name`, or multi-repo setup |
| Username | User asks "my builds" but git username may differ from Develocity username |
| Branch | User wants branch-specific analysis |
| Time range | User wants something other than default (7 days) |
| Build type | User wants CI-only or LOCAL-only builds |

Example: "What is the Develocity username? (Git shows 'jdoe' - is that correct?)"

## MCP Tools

### mcp__develocity__getBuilds

Query build history with filtering and optional detailed data.

**Key parameters:**
- `project` - Root project name (REQUIRED for meaningful results)
- `fromDate` / `toDate` - ISO-8601 format (e.g., `2025-01-15` or `2025-01-15T10:00:00Z`)
- `maxBuilds` - Limit results (default 100, max 1000)
- `buildOutcome` - `succeeded` or `failed`
- `username` - Filter by user who ran the build
- `userTags` - Filter by tags like `["CI"]`, `["LOCAL"]`, or branch names
- `additionalDataToInclude` - Array of: `attributes`, `build_performance`, `caching`, `failures`, `test_performance`

### mcp__develocity__getTestResults

Query test results with outcome filtering.

**Key parameters:**
- `project` - Root project name (REQUIRED)
- `fromDate` / `toDate` - ISO-8601 format
- `includeOutcomes` - REQUIRED array: `passed`, `failed`, `flaky`, `skipped`
- `testContainer` - Filter by test class (supports wildcards when not including test cases)
- `includeTestCases` - `true` for individual tests, `false` for container summaries
- `limit` - Max test cases (1-1000, default 100)

## Common Queries

**Recent build failures:**
```
mcp__develocity__getBuilds:
  project: "my-project"
  fromDate: "2025-01-18"  # 7 days ago
  buildOutcome: "failed"
  maxBuilds: 20
  additionalDataToInclude: ["failures"]
```

**My builds (user-specific):**
```
mcp__develocity__getBuilds:
  project: "my-project"
  username: "jdoe"
  fromDate: "2025-01-18"
  maxBuilds: 50
```

**CI builds only:**
```
mcp__develocity__getBuilds:
  project: "my-project"
  userTags: ["CI"]
  fromDate: "2025-01-18"
```

**Cache performance analysis:**
```
mcp__develocity__getBuilds:
  project: "my-project"
  fromDate: "2025-01-18"
  maxBuilds: 50
  additionalDataToInclude: ["caching", "build_performance"]
```

**Flaky tests:**
```
mcp__develocity__getTestResults:
  project: "my-project"
  fromDate: "2025-01-18"
  includeOutcomes: ["flaky"]
```

**Failed tests in specific class:**
```
mcp__develocity__getTestResults:
  project: "my-project"
  fromDate: "2025-01-18"
  includeOutcomes: ["failed"]
  testContainer: "com.example.UserServiceTest"
  includeTestCases: true
```

See [references/query-patterns.md](references/query-patterns.md) for more patterns.

## Presenting Results

**Build summary format:**
```
Last 7 days: 47 builds analyzed
  Success rate: 77% (36 passed, 11 failed)
  Avg build time: 2m 34s
  Cache hit rate: 68%

Recent failures:
  - 2025-01-15 14:32 - CompilationFailedException in :app:compileKotlin
    Build Scan: https://develocity.example.com/s/abc123
```

**Test health format:**
```
Flaky tests (3):
  1. UserServiceTest.testConcurrentLogin - 23% flaky (7/30 runs)
  2. PaymentProcessorTest.testTimeout - 15% flaky (3/20 runs)
```

## Failure Classification

Develocity classifies build failures into two types:

### Verification Failures (Expected)
Problems with **developer inputs** - expected during normal development.

| Type | Message Prefixes |
|------|------------------|
| Compilation | "Compilation failed" |
| Test | "Test failed", "Tests failed" |
| Lint/Analysis | "Analysis failed", "Lint failed", "Check failed" |
| Code generation | "Code generation failed", "Generation failed" |

**Key insight:** These failures mean the build is working correctly - catching real code problems.

### Non-Verification Failures (Unexpected)
Problems with **build infrastructure** - NOT expected during normal development.

| Type | Message Prefixes |
|------|------------------|
| Dependency | "Could not resolve", "Could not find" |
| Infrastructure | "Unexpected error", "Unexpected failure" |
| Configuration | "Could not create task" |

**Key insight:** These failures block ALL developers and should be prioritized.

### Custom Exception Classification

To ensure proper classification for custom exceptions:
```java
// Extend VerificationException for verification failures
public class MyCheckException extends org.gradle.api.tasks.VerificationException {
    public MyCheckException(String message) {
        super("Check failed: " + message);  // Use recognized prefix
    }
}
```

See [Failure Classification Guide](https://docs.gradle.com/develocity/current/guides/failure-classification-guide/) for details.

## Error Handling

If MCP tools fail or are unavailable:
- Note that Develocity is not configured
- Continue with local analysis if applicable
- Suggest user configure Develocity MCP server for build insights
- Direct them to [references/setup.md](references/setup.md) for configuration instructions

## Setup

See [references/setup.md](references/setup.md) for:
- How to get a Develocity access key
- MCP server configuration (project or user level)
- Troubleshooting connection issues
