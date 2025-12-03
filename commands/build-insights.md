---
description: Get an overview of build health from Develocity (recent builds, failure rate, flaky tests, cache performance, trends)
allowed-tools: Read, Glob, Grep, Bash, AskUserQuestion, mcp__develocity__get_builds, mcp__develocity__get_build_by_id, mcp__develocity__get_build_failures, mcp__develocity__get_test_results, mcp__develocity__get_develocity_server_url, mcp__drv__execute_query, mcp__drv__list_tables
---

# Build Insights - Build Health Overview

You are running the build-insights command. Provide a high-level overview of build health by querying both Develocity (for recent builds) and DRV (for trends).

## Prerequisites

This command requires the Develocity MCP server to be configured. If tools are unavailable, inform the user:

```
Develocity MCP server not configured.

To use /build-insights, configure the Develocity MCP server in your Claude Code settings.
For local build analysis, use /doctor instead.
```

## Step 1: Gather Context

Before querying Develocity, establish the correct context to filter results appropriately.

### Auto-detect what you can:

1. **Project name** from `settings.gradle(.kts)`:
   ```bash
   grep -E "^rootProject.name" settings.gradle* 2>/dev/null | head -1
   ```

2. **Current git branch**:
   ```bash
   git branch --show-current 2>/dev/null
   ```

3. **Current username** (for "my builds" context):
   ```bash
   git config user.name 2>/dev/null
   ```

### Ask user for clarification if needed:

If any of the following are unclear or ambiguous, use `AskUserQuestion` to clarify BEFORE querying Develocity:

| Context | When to ask |
|---------|-------------|
| **Project** | If `rootProject.name` cannot be detected, or if analyzing a multi-repo setup |
| **Username** | If user asks about "my builds" but git username doesn't match Develocity username |
| **Branch** | If user wants branch-specific analysis (CI vs local, feature branch vs main) |
| **Time range** | If user wants something other than "last 7 days" default |
| **Build type** | If user wants to filter by CI vs LOCAL builds |

Example clarification questions:
- "What is your Develocity username? (Your git username is 'jdoe' - is that the same?)"
- "Should I analyze builds from all branches, or just the current branch ('feature/xyz')?"
- "Should I include only CI builds, only local builds, or both?"

## Step 2: Query Develocity

### 2.1: Get Develocity Server URL

First, call `mcp__develocity__get_develocity_server_url` to get the correct URL for Build Scan links.

### 2.2: Query Recent Builds

Query recent builds (last 7 days, up to 50 builds):

Use `mcp__develocity__get_builds` with:
- `project`: the detected root project name (REQUIRED - ask if unknown)
- `fromDate`: 7 days ago in ISO-8601 format
- `maxBuilds`: 50
- `additionalDataToInclude`: `["attributes", "build_performance", "caching"]`
- `userTags`: (optional) filter by `["CI"]` or `["LOCAL"]` if user specified
- `username`: (optional) if user asked for "my builds"

### 2.3: Query for Flaky/Failed Tests

Query for test health (last 7 days):

Use `mcp__develocity__get_test_results` with:
- `project`: the detected root project name (REQUIRED - ask if unknown)
- `fromDate`: 7 days ago
- `includeOutcomes`: `["flaky", "failed"]`
- `userTags`: (optional) match the build query filters

### 2.4: Quick Environment Check

Query a sample of CI and LOCAL builds to detect obvious mismatches:

**CI builds** (up to 5):
- Use `mcp__develocity__get_builds` with `userTags`: `["CI"]`, `maxBuilds`: 5

**LOCAL builds** (up to 5):
- Use `mcp__develocity__get_builds` with `userTags`: `["LOCAL"]`, `maxBuilds`: 5

Note any significant outcome differences (e.g., CI passing but LOCAL failing).

### 2.5: Query Trends from DRV (if available)

Query DRV for high-level trend indicators. If DRV tools are unavailable, skip this step gracefully.

**Cache hit rate trend** (this week vs last week):
```sql
SELECT
  CASE WHEN build_start_date >= current_date - INTERVAL '7' DAY THEN 'this_week' ELSE 'last_week' END as period,
  COUNT(*) as builds,
  ROUND(AVG(CASE WHEN local_build_cache_hit_count + remote_build_cache_hit_count > 0 THEN 1.0 ELSE 0.0 END) * 100, 1) as cache_hit_pct
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '14' DAY
  AND projectname = '{project}'
  AND build_cache_enabled = true
GROUP BY 1
```

**Failure rate trend** (this week vs last week):
```sql
SELECT
  CASE WHEN build_start_date >= current_date - INTERVAL '7' DAY THEN 'this_week' ELSE 'last_week' END as period,
  COUNT(*) as builds,
  ROUND(SUM(CASE WHEN has_failed THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 1) as failure_pct
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '14' DAY
  AND projectname = '{project}'
GROUP BY 1
```

**Build time trend** (this week vs last week):
```sql
SELECT
  CASE WHEN build_start_date >= current_date - INTERVAL '7' DAY THEN 'this_week' ELSE 'last_week' END as period,
  ROUND(AVG(build_duration_millis) / 1000 / 60, 1) as avg_build_minutes
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '14' DAY
  AND projectname = '{project}'
  AND has_failed = false
GROUP BY 1
```

Calculate trend direction:
- **Improving**: This week better than last week (higher cache hit, lower failure rate, faster builds)
- **Stable**: Within 5% of last week
- **Declining**: This week worse than last week

## Step 3: Present Build Insights Report

```
═══════════════════════════════════════════════════════════════
                      BUILD INSIGHTS
═══════════════════════════════════════════════════════════════

Project: <project-name>
Period: Last 7 days
Builds analyzed: <count>

┌──────────────────────┬──────────────────────────────────────┐
│ Metric               │ Value                      │ Trend   │
├──────────────────────┼────────────────────────────┼─────────┤
│ Success Rate         │ 77% (36/47 builds)         │ ↑ +5%   │
│ Avg Build Time       │ 2m 34s                     │ ↓ -12%  │
│ Cache Hit Rate       │ 68%                        │ → stable│
│ Flaky Tests          │ 3 detected                 │         │
│ CI vs Local          │ ⚠ Mismatch detected        │         │
└──────────────────────┴────────────────────────────┴─────────┘

Trend: vs previous 7 days (↑ improving, ↓ worsening, → stable)

═══════════════════════════════════════════════════════════════
```

## Step 4: Detailed Sections

### Build History

```
── Recent Build Activity ──────────────────────────────────────

Last 7 days: 47 builds analyzed
  • Success rate: 77% (36 passed, 11 failed)
  • Avg build time: 2m 34s
  • Cache hit rate: 68%

Recent failures (last 3):
  • 2025-01-15 14:32 - CompilationFailedException in :app:compileKotlin
  • 2025-01-15 10:15 - TestFailure in UserServiceTest.testLogin
  • 2025-01-14 16:45 - OOM during :app:test (heap exhausted)

Build Scans:
  • https://develocity.example.com/s/abc123
  • https://develocity.example.com/s/def456
```

### Test Health Summary

```
── Test Health ────────────────────────────────────────────────

Flaky tests detected (3):
  1. UserServiceTest.testConcurrentLogin - 23% flaky
  2. PaymentProcessorTest.testTimeout - 15% flaky
  3. DatabaseConnectionTest.testPoolExhaustion - 10% flaky

Top failing tests (last 7 days):
  1. IntegrationTest.testFullWorkflow - 8 failures
  2. ApiClientTest.testRetry - 5 failures

For detailed flaky test analysis: /diagnose flaky-tests
```

### Environment Check

```
── CI vs Local Comparison ─────────────────────────────────────

CI builds:    5 analyzed, 5 passed (100%)
LOCAL builds: 5 analyzed, 2 passed (40%)

⚠ Potential environment mismatch detected

For detailed analysis: /diagnose outcome-mismatch
```

## Step 5: Recommendations

Based on the insights, provide actionable next steps:

```
═══════════════════════════════════════════════════════════════
                    RECOMMENDED ACTIONS
═══════════════════════════════════════════════════════════════

Based on the analysis:

1. Investigate flaky tests (3 detected)
   → Run: /diagnose flaky-tests

2. CI vs Local mismatch detected
   → Run: /diagnose outcome-mismatch

3. Recent OOM failure detected
   → Consider increasing heap: org.gradle.jvmargs=-Xmx4g

4. Cache hit rate below 70%
   → Run: /doctor to check local cache configuration

═══════════════════════════════════════════════════════════════
```

## Recommendation Logic

| Finding | Recommend |
|---------|-----------|
| Flaky tests > 0 | `/diagnose flaky-tests` |
| CI vs LOCAL mismatch | `/diagnose outcome-mismatch` |
| High failure rate (>20%) | `/diagnose failure-patterns` |
| Low cache hit rate (<50%) | `/doctor` to check cache config |
| OOM failures | Increase heap in `gradle.properties` |
| All healthy | "Builds are healthy!" |

## Example Output

For a project with issues:

```
BUILD INSIGHTS: project-name (Last 7 days)

Key Metrics:
  • Success Rate: 77% (36/47)      ↑ +5% vs last week
  • Cache Hit Rate: 68%            → stable
  • Build Time: 2m 34s             ↓ -12% (faster)
  • Flaky Tests: 3

Issues Found:
  ⚠ 3 flaky tests affecting reliability
  ⚠ CI vs Local outcome mismatch
  ⚠ 1 OOM failure

Next Steps:
  1. /diagnose flaky-tests
  2. /diagnose outcome-mismatch
```

For a healthy project:

```
BUILD INSIGHTS: project-name (Last 7 days)

Key Metrics:
  • Success Rate: 98% (49/50)      ↑ +3% vs last week
  • Cache Hit Rate: 85%            ↑ +7% vs last week
  • Build Time: 1m 45s             → stable
  • Flaky Tests: 0

✓ Builds are healthy! All metrics stable or improving.
```

Note: Trend data requires DRV. If unavailable, trends will be omitted but current metrics still shown.

## Related Commands

- `/doctor` - Local build script health check
- `/diagnose flaky-tests` - Deep-dive into flaky test analysis
- `/diagnose outcome-mismatch` - Investigate CI vs local differences
- `/diagnose failure-patterns` - Analyze recurring failure patterns
