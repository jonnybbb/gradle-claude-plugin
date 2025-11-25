---
description: Optimize Gradle build performance with automatic fixes
argument-hint: "[--apply] [--benchmark] [--dry-run]"
allowed-tools: Read, Write, Edit, Glob, Grep, Bash, AskUserQuestion
---

# Gradle Performance Optimization Workflow

You are running the performance optimization workflow. This command analyzes the project for performance issues and helps apply optimizations.

## Arguments

Parse the command arguments:
- `--apply`: Automatically apply all auto-fixable optimizations
- `--benchmark`: Include before/after benchmark commands
- `--dry-run`: Show what would be optimized without making changes
- (default): Interactive mode - review each optimization

## Workflow Steps

### Step 1: Analyze Project

Run the performance-fixer tool:

```bash
jbang tools/performance-fixer.java . --json
```

Optionally run performance-profiler for actual metrics:

```bash
jbang tools/performance-profiler.java . build --json
```

### Step 2: Display Optimization Summary

Present findings:

```
═══════════════════════════════════════════════════════════════
           PERFORMANCE OPTIMIZATION PLAN
═══════════════════════════════════════════════════════════════

Project: /path/to/project
Estimated improvement: 30-60% faster builds possible

┌─────────────────────┬───────┬───────────────┐
│ Impact Level        │ Count │ Auto-fixable  │
├─────────────────────┼───────┼───────────────┤
│ HIGH                │     3 │ ✓ 2           │
│ MEDIUM              │     2 │ ✓ 2           │
│ LOW                 │     2 │ ✓ 2           │
├─────────────────────┼───────┼───────────────┤
│ Total               │     7 │ 6             │
└─────────────────────┴───────┴───────────────┘

Categories:
  • PARALLEL: Enable parallel execution
  • CACHING: Build cache settings
  • CONFIG_CACHE: Configuration cache
  • JVM_ARGS: JVM tuning
  • TASK_AVOIDANCE: Lazy task configuration
  • BUILD_STRUCTURE: Convention plugins

═══════════════════════════════════════════════════════════════
```

### Step 3: Apply Optimizations

Group optimizations by file and apply:

**gradle.properties optimizations:**
```
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.vfs.watch=true
org.gradle.jvmargs=-Xmx2g -XX:+UseG1GC -XX:MaxMetaspaceSize=512m
```

**Build script optimizations:**
- Replace `tasks.all {}` with `tasks.configureEach {}`
- Add `.configureEach` to `withType` calls
- Flag `afterEvaluate` and `allprojects/subprojects` for review

### Step 4: Run Benchmark (if requested)

If `--benchmark` flag:

```bash
# Before optimization
./gradlew clean
time ./gradlew build

# After optimization
./gradlew clean
time ./gradlew build
```

Report comparison:
```
═══════════════════════════════════════════════════════════════
               BENCHMARK RESULTS
═══════════════════════════════════════════════════════════════

Before: 45.2s
After:  28.1s
Improvement: 37.8% faster

═══════════════════════════════════════════════════════════════
```

### Step 5: Verification

Run a build to verify optimizations work:

```bash
./gradlew build 2>&1
```

### Step 6: Final Summary

```
═══════════════════════════════════════════════════════════════
               OPTIMIZATION COMPLETE
═══════════════════════════════════════════════════════════════

Applied optimizations:
  ✓ Enabled parallel execution
  ✓ Enabled build cache
  ✓ Enabled file system watching
  ✓ Increased heap to 2GB
  ✓ Enabled G1 garbage collector
  ✓ Set metaspace limit

Skipped (require manual review):
  ⚠ Configuration cache (may require code changes)
  ⚠ Convention plugins (structural change)

Next steps:
  1. Run full test suite: ./gradlew check
  2. Enable configuration cache with /fix-config-cache
  3. Consider Gradle Enterprise for build insights
  4. See gradle-performance skill for advanced tuning

═══════════════════════════════════════════════════════════════
```

## Optimization Reference

### gradle.properties Settings

| Setting | Impact | Description |
|---------|--------|-------------|
| `org.gradle.parallel=true` | HIGH | Build subprojects in parallel |
| `org.gradle.caching=true` | HIGH | Reuse task outputs from cache |
| `org.gradle.configuration-cache=true` | HIGH | Cache configuration phase |
| `org.gradle.vfs.watch=true` | MEDIUM | Watch filesystem for changes |
| `-Xmx2g` | MEDIUM | Larger heap for complex builds |
| `-XX:+UseG1GC` | LOW | Modern garbage collector |

### Build Script Patterns

| Anti-pattern | Fix | Impact |
|--------------|-----|--------|
| `tasks.all {}` | `tasks.configureEach {}` | Faster config |
| `withType() {}` | `withType().configureEach {}` | Faster config |
| `afterEvaluate {}` | Lazy APIs | Predictable config |
| `allprojects/subprojects` | Convention plugins | Better structure |

## Example Usage

```bash
# Analyze and suggest (interactive)
/optimize

# Auto-apply safe optimizations
/optimize --apply

# Preview without changes
/optimize --dry-run

# Include benchmarks
/optimize --apply --benchmark
```

## Tool Requirements

- JBang installed (for performance-fixer.java)
- Gradle wrapper present
- Write access to gradle.properties and build files

## Related

- `/fix-config-cache` - Fix configuration cache issues
- `/migrate-gradle` - Migrate to newer Gradle version
- `/doctor` - Full build health check
- `gradle-performance` skill - Detailed performance guidance
