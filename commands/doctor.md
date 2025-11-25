---
description: Run comprehensive Gradle build health analysis
allowed-tools: Read, Glob, Grep, Bash, AskUserQuestion, mcp__develocity__getBuilds, mcp__develocity__getTestResults
---

# Gradle Doctor - Comprehensive Health Check

You are running the Gradle doctor command. Perform a comprehensive health check on the Gradle project in the current directory.

## Analysis Steps

### Step 1: Run Analysis Tools

Run the build health check tool:

```bash
jbang tools/build-health-check.java . --json
```

Additionally run these tools for deeper analysis:

```bash
# Configuration cache compatibility
jbang tools/cache-validator.java . --json

# Performance analysis
jbang tools/performance-fixer.java . --json

# Task analysis
jbang tools/task-analyzer.java . --json
```

### Step 1.5: Query Develocity (if available)

If the Develocity MCP tools are available (`mcp__develocity__getBuilds` and `mcp__develocity__getTestResults`), query for additional insights. Skip this step gracefully if Develocity is not configured.

#### Step 1.5.1: Gather Context

Before querying Develocity, you MUST establish the correct context. Develocity queries require proper filtering to return relevant results.

**Auto-detect what you can:**

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

**Ask user for clarification if needed:**

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

#### Step 1.5.2: Query Recent Builds

**Query recent builds** (last 7 days, up to 50 builds):
- Use `mcp__develocity__getBuilds` with:
  - `project`: the detected root project name (REQUIRED - ask if unknown)
  - `fromDate`: 7 days ago in ISO-8601 format
  - `maxBuilds`: 50
  - `additionalDataToInclude`: `["attributes", "build_performance", "caching", "failures"]`
  - `userTags`: (optional) filter by `["CI"]` or `["LOCAL"]` if user specified
  - `username`: (optional) if user asked for "my builds"

#### Step 1.5.3: Query for Flaky Tests

**Query for flaky tests** (last 7 days):
- Use `mcp__develocity__getTestResults` with:
  - `project`: the detected root project name (REQUIRED - ask if unknown)
  - `fromDate`: 7 days ago
  - `includeOutcomes`: `["flaky", "failed"]`
  - `userTags`: (optional) match the build query filters

**Important**: The Develocity MCP server URL and credentials are user/environment-specific. If the MCP tools fail or are not available, continue with local analysis only and note that Develocity insights are unavailable.

### Step 2: Analyze Results

Categorize findings into:

1. **Performance** - configuration time, task execution, parallelization
2. **Caching** - build cache and configuration cache setup
3. **Dependencies** - conflicts, version management, resolution
4. **Structure** - project organization, conventions
5. **Compatibility** - deprecated APIs, version-specific issues
6. **Build History** (Develocity) - recent build success rate, failure patterns
7. **Test Health** (Develocity) - flaky tests, test failure trends

### Step 3: Present Health Report

```
═══════════════════════════════════════════════════════════════
                    GRADLE BUILD HEALTH REPORT
═══════════════════════════════════════════════════════════════

Project: /path/to/project
Gradle Version: X.X

┌──────────────────┬────────┬──────────────────────────────────┐
│ Category         │ Status │ Summary                          │
├──────────────────┼────────┼──────────────────────────────────┤
│ Performance      │ ⚠      │ 3 optimizations available        │
│ Config Cache     │ ✗      │ 16 compatibility issues          │
│ Build Cache      │ ✓      │ Enabled and working              │
│ Dependencies     │ ✓      │ No conflicts detected            │
│ Structure        │ ⚠      │ Consider convention plugins      │
├──────────────────┼────────┼──────────────────────────────────┤
│ Build History    │ ⚠      │ 23% failure rate (7 days)        │
│ Test Health      │ ✗      │ 3 flaky tests detected           │
└──────────────────┴────────┴──────────────────────────────────┘

Overall Health: NEEDS ATTENTION

═══════════════════════════════════════════════════════════════
```

### Step 4: Detailed Findings

For each category with issues, provide details:

```
── Performance Issues ──────────────────────────────────────────

1. Parallel execution disabled
   Impact: HIGH
   Fix: Add org.gradle.parallel=true to gradle.properties

2. Build cache disabled
   Impact: HIGH
   Fix: Add org.gradle.caching=true to gradle.properties

3. tasks.all {} usage detected
   Impact: MEDIUM
   Fix: Replace with tasks.configureEach {}

── Configuration Cache Issues ──────────────────────────────────

Found 16 compatibility issues:
  • 4 System.getProperty calls at configuration time
  • 3 eager task creation patterns
  • 5 $buildDir usages
  • 2 tasks.getByName calls
  • 2 project references in task actions

── Build History (Develocity) ─────────────────────────────────

Last 7 days: 47 builds analyzed
  • Success rate: 77% (36 passed, 11 failed)
  • Avg build time: 2m 34s
  • Cache hit rate: 68%

Recent failures (last 3):
  • 2025-01-15 14:32 - CompilationFailedException in :app:compileKotlin
  • 2025-01-15 10:15 - TestFailure in UserServiceTest.testLogin
  • 2025-01-14 16:45 - OOM during :app:test (heap exhausted)

Build Scan links:
  • https://develocity.example.com/s/abc123
  • https://develocity.example.com/s/def456

── Test Health (Develocity) ───────────────────────────────────

Flaky tests detected (3):
  1. UserServiceTest.testConcurrentLogin
     Flaky rate: 23% (7/30 runs failed)

  2. PaymentProcessorTest.testTimeout
     Flaky rate: 15% (3/20 runs failed)

  3. DatabaseConnectionTest.testPoolExhaustion
     Flaky rate: 10% (2/20 runs failed)

Top failing tests (last 7 days):
  1. IntegrationTest.testFullWorkflow - 8 failures
  2. ApiClientTest.testRetry - 5 failures

═══════════════════════════════════════════════════════════════
```

**Note**: Build History and Test Health sections only appear if Develocity is configured and accessible. If unavailable, display:
```
── Develocity Insights ────────────────────────────────────────
Develocity not configured. To enable build history and test
health insights, configure the Develocity MCP server.
```

### Step 5: Recommend Next Steps

IMPORTANT: Always end with specific command recommendations based on findings.

```
═══════════════════════════════════════════════════════════════
                    RECOMMENDED ACTIONS
═══════════════════════════════════════════════════════════════

Based on the analysis, run these commands in order:

1. /optimize-performance --dry-run
   → Review and apply performance settings (3 fixes)

2. /fix-config-cache --dry-run
   → Fix configuration cache compatibility (16 issues)

3. /upgrade 9.0 --dry-run
   → Upgrade from Gradle 7.6 to 9.0 (if applicable)

Run with --dry-run first to preview changes, then without to apply.

For manual issues that can't be auto-fixed, see:
  • gradle-config-cache skill for config cache patterns
  • gradle-performance skill for advanced tuning

═══════════════════════════════════════════════════════════════
```

## Recommendation Logic

Suggest commands based on what was found:

| Finding | Recommend |
|---------|-----------|
| Performance settings missing | `/optimize-performance` |
| Config cache issues found | `/fix-config-cache` |
| Gradle version < 8.0 | `/upgrade <latest>` |
| Deprecated APIs detected | `/upgrade` |
| High failure rate (>20%) | Investigate recent Build Scan failures |
| Flaky tests detected | Review flaky tests, consider `@RerunFailingTests` |
| Low cache hit rate (<50%) | Check cache configuration, run `/optimize-performance` |
| OOM failures | Increase heap in `gradle.properties` |
| All clean | "Build is healthy!" |

Only recommend commands that are relevant to the findings. Don't suggest `/upgrade` if already on latest version.

## Example Output

For a project with issues:

```
Recommended actions (in order):
  1. /optimize-performance --auto  (quick wins: parallel, caching)
  2. /fix-config-cache --dry-run   (16 issues to review)
```

For a project with Develocity insights showing issues:

```
Recommended actions (in order):
  1. /optimize-performance --auto  (quick wins: parallel, caching)
  2. /fix-config-cache --dry-run   (16 issues to review)

Develocity insights:
  3. Investigate flaky tests: UserServiceTest.testConcurrentLogin (23% flaky)
     → Consider adding retry logic or fixing race condition
  4. Review OOM failure from 2025-01-14:
     → Build Scan: https://develocity.example.com/s/abc123
     → Try increasing heap: org.gradle.jvmargs=-Xmx4g
```

For a healthy project:

```
✓ Build is healthy! No immediate actions needed.

Optional improvements:
  • Consider enabling configuration cache for faster builds
  • Run /optimize-performance --benchmark to measure current performance
```

For a project without Develocity:

```
✓ Local analysis complete. Build is healthy!

Note: Develocity MCP server not configured.
      Configure it to get build history and test health insights.
```

## Related

- `/fix-config-cache` - Fix configuration cache compatibility issues
- `/optimize-performance` - Optimize build performance settings
- `/upgrade` - Upgrade to newer Gradle version
- `gradle-doctor` skill - Detailed health check guidance
