---
description: Run comprehensive Gradle build health analysis
allowed-tools: Read, Glob, Grep, Bash, AskUserQuestion
---

# Gradle Doctor - Comprehensive Health Check

You are running the Gradle doctor command. Perform a comprehensive health check on the Gradle project in the current directory.

## Analysis Steps

### Step 1: Run Analysis Tools

Run the build health check tool:

```bash
jbang ${CLAUDE_PLUGIN_ROOT}/tools/build-health-check.java . --json
```

Additionally run these tools for deeper analysis:

```bash
# Configuration cache compatibility
jbang ${CLAUDE_PLUGIN_ROOT}/tools/cache-validator.java . --json

# Performance analysis
jbang ${CLAUDE_PLUGIN_ROOT}/tools/performance-fixer.java . --json

# Task analysis
jbang ${CLAUDE_PLUGIN_ROOT}/tools/task-analyzer.java . --json
```

### Step 2: Analyze Results

Categorize findings into:

1. **Performance** - configuration time, task execution, parallelization
2. **Caching** - build cache and configuration cache setup
3. **Dependencies** - conflicts, version management, resolution
4. **Structure** - project organization, conventions
5. **Compatibility** - deprecated APIs, version-specific issues

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
│ Compatibility    │ ✓      │ No deprecated APIs detected      │
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

═══════════════════════════════════════════════════════════════
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
| All clean | "Build is healthy!" |

Only recommend commands that are relevant to the findings. Don't suggest `/upgrade` if already on latest version.

## Example Output

For a project with issues:

```
Recommended actions (in order):
  1. /optimize-performance --auto  (quick wins: parallel, caching)
  2. /fix-config-cache --dry-run   (16 issues to review)
```

For a healthy project:

```
✓ Build is healthy! No immediate actions needed.

Optional improvements:
  • Consider enabling configuration cache for faster builds
  • Run /optimize-performance --benchmark to measure current performance
```

## Related

- `/fix-config-cache` - Fix configuration cache compatibility issues
- `/optimize-performance` - Optimize build performance settings
- `/upgrade` - Upgrade to newer Gradle version
- `gradle-doctor` skill - Detailed health check guidance
