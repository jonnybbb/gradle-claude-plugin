---
description: Analyze build cache performance trends over time using DRV aggregate data
allowed-tools: Read, Grep, Bash, AskUserQuestion, mcp__drv__execute_query, mcp__drv__describe_table
---

# Analyze Cache Trends

Analyze build cache performance trends using Develocity Reporting Kit (DRV) aggregate data.

## Step 1: Gather Context

Determine the project and time range:

```bash
# Get project name
grep -E "^rootProject.name" settings.gradle* 2>/dev/null | head -1
```

If project name cannot be detected, ask the user:
- "What is the project name in Develocity?"

Default time range: 30 days. Ask if user wants different.

## Step 2: Query Cache Trends

Execute the following SQL query via `mcp__drv__execute_query`:

```sql
SELECT
  build_start_date,
  environment,
  COUNT(*) as builds,
  ROUND(AVG(CASE WHEN local_build_cache_avoidance_millis + remote_build_cache_avoidance_millis > 0
      THEN 1.0 ELSE 0.0 END) * 100, 1) as cache_hit_rate_pct,
  ROUND(SUM(local_build_cache_avoidance_millis) / 60000.0, 1) as local_cache_savings_min,
  ROUND(SUM(remote_build_cache_avoidance_millis) / 60000.0, 1) as remote_cache_savings_min,
  SUM(CASE WHEN local_build_cache_disabled_due_to_error THEN 1 ELSE 0 END) as local_cache_errors,
  SUM(CASE WHEN remote_build_cache_disabled_due_to_error THEN 1 ELSE 0 END) as remote_cache_errors
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '30' DAY
  AND projectname = '{project}'
  AND build_cache_enabled = true
GROUP BY build_start_date, environment
ORDER BY build_start_date DESC
```

Replace `{project}` with the actual project name.

## Step 3: Query Non-Cacheable Tasks

Find the tasks consuming the most uncached time:

```sql
SELECT
  goal_or_task_path,
  goal_or_task_type,
  non_cacheability_category,
  COUNT(*) as executions,
  ROUND(AVG(goal_or_task_duration_millis) / 1000.0, 1) as avg_seconds,
  ROUND(SUM(goal_or_task_duration_millis) / 60000.0, 1) as total_minutes
FROM unit_execution_summary
WHERE build_start_date >= current_date - INTERVAL '7' DAY
  AND projectname = '{project}'
  AND avoidance_outcome = 'executed_not_cacheable'
GROUP BY goal_or_task_path, goal_or_task_type, non_cacheability_category
ORDER BY total_minutes DESC
LIMIT 15
```

## Step 4: Query Cache Configuration Issues

Check for builds with cache disabled or errors:

```sql
SELECT
  build_start_date,
  SUM(CASE WHEN NOT build_cache_enabled THEN 1 ELSE 0 END) as cache_disabled,
  SUM(CASE WHEN NOT local_build_cache_enabled THEN 1 ELSE 0 END) as local_disabled,
  SUM(CASE WHEN NOT remote_build_cache_enabled THEN 1 ELSE 0 END) as remote_disabled,
  SUM(CASE WHEN local_build_cache_disabled_due_to_error THEN 1 ELSE 0 END) as local_errors,
  SUM(CASE WHEN remote_build_cache_disabled_due_to_error THEN 1 ELSE 0 END) as remote_errors
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '7' DAY
  AND projectname = '{project}'
GROUP BY build_start_date
HAVING SUM(CASE WHEN NOT build_cache_enabled THEN 1 ELSE 0 END) > 0
   OR SUM(CASE WHEN local_build_cache_disabled_due_to_error THEN 1 ELSE 0 END) > 0
   OR SUM(CASE WHEN remote_build_cache_disabled_due_to_error THEN 1 ELSE 0 END) > 0
ORDER BY build_start_date DESC
```

## Step 5: Present Report

```
═══════════════════════════════════════════════════════════════
                BUILD CACHE TREND ANALYSIS
═══════════════════════════════════════════════════════════════

Project: {project}
Period: Last 30 days
Builds analyzed: {count}

┌─────────────────────────────────────────────────────────────┐
│ CACHE PERFORMANCE SUMMARY                                   │
├─────────────────────────────────────────────────────────────┤
│ Overall hit rate:     {rate}%                               │
│ Time saved (local):   {local_min} minutes                   │
│ Time saved (remote):  {remote_min} minutes                  │
│ Total time saved:     {total_hours} hours                   │
└─────────────────────────────────────────────────────────────┘

── Trend (Last 7 Days) ───────────────────────────────────────

Date        CI Builds   CI Hit%   Local Builds   Local Hit%
──────────────────────────────────────────────────────────────
2025-01-15     23        78%          12           65%
2025-01-14     21        75%          15           62%
...

── Top Non-Cacheable Tasks ───────────────────────────────────

These tasks consume the most time and are not cached:

1. :app:compileKotlin (overlapping_outputs)
   → 45.2 min total | 2.1 min avg | 21 executions

2. :lib:test (executed_cacheable but cache miss)
   → 32.1 min total | 1.5 min avg | 21 executions

── Cache Configuration Issues ────────────────────────────────

{issues_or_none}

═══════════════════════════════════════════════════════════════
                    RECOMMENDATIONS
═══════════════════════════════════════════════════════════════

Based on the analysis:

1. {recommendation_1}
2. {recommendation_2}

═══════════════════════════════════════════════════════════════
```

## Recommendation Logic

| Finding | Recommendation |
|---------|----------------|
| Hit rate < 50% | Check remote cache connectivity, verify cache keys |
| Cache disabled on some builds | Review CI configuration, check `org.gradle.caching` |
| Cache errors | Check cache server health, review error logs |
| High non-cacheable time | Consider making tasks cacheable or fixing `overlapping_outputs` |
| Good hit rate (>70%) | "Cache is performing well" |

## Error Handling

If DRV tools are unavailable:
```
DRV MCP server not configured.

To analyze cache trends, configure the DRV MCP server.
Alternatively, use mcp__develocity__getBuilds for individual build analysis.
```

## Related

- `/optimize-performance` - Apply performance optimizations
- `develocity-analytics` skill - More SQL query patterns
- `gradle-build-cache` skill - Cache configuration guidance
