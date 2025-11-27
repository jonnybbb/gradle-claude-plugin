---
name: develocity-analytics
description: This skill should be used when the user asks about "build trends", "cache hit rate over time", "slowest tasks", "optimization opportunities", "build health", "aggregate metrics", "across builds", "dashboard", "failure rate", "what projects have", "why are builds slow", "cache performance", "test acceleration potential", or "deprecations across projects". Use for analyzing patterns across many builds via Develocity Reporting Kit (DRV) SQL analytics rather than inspecting individual Build Scans.
---

# Develocity Analytics (DRV)

SQL-based aggregate analytics across builds via `mcp__drv__*` MCP tools.

## When to Use

| Question Type | Use Tool |
|--------------|----------|
| "Why did build X fail?" | `mcp__develocity__get_build_failures` |
| "What's our cache hit rate trend?" | `mcp__drv__execute_query` |
| "Show flaky tests" | `mcp__develocity__get_test_results` |
| "Which projects have cache disabled?" | `mcp__drv__execute_query` |
| "What optimization opportunities exist?" | `mcp__drv__execute_query` + dashboards |

## MCP Tools

| Tool | Purpose |
|------|---------|
| `mcp__drv__execute_query` | Execute SQL against DRV tables |
| `mcp__drv__list_tables` | List available tables |
| `mcp__drv__describe_table` | Get table schema |
| `mcp__drv__fetch_query_by_panel` | Get dashboard panel queries |

## Key Tables

| Table | Use For |
|-------|---------|
| `build_summary` | Fast queries, flattened build data |
| `build` | Full data with nested structures |
| `unit_execution_summary` | Task/goal execution details |

**Critical:** Always filter by `build_start_date` partition for performance:
```sql
WHERE build_start_date >= current_date - INTERVAL '7' DAY
```

## Quick Start Queries

### Build cache hit rate (last 7 days)
```sql
SELECT build_start_date, COUNT(*) as builds,
  ROUND(AVG(CASE WHEN local_build_cache_avoidance_millis + remote_build_cache_avoidance_millis > 0
      THEN 1.0 ELSE 0.0 END) * 100, 1) as hit_rate_pct
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '7' DAY
  AND projectname = '{project}'
  AND build_cache_enabled = true
GROUP BY build_start_date ORDER BY build_start_date DESC
```

### Failure rate by environment
```sql
SELECT environment, COUNT(*) as builds,
  ROUND(SUM(CASE WHEN has_failed THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 1) as failure_pct
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '7' DAY
  AND projectname = '{project}'
GROUP BY environment
```

### Projects with cache disabled
```sql
SELECT projectname, COUNT(*) as builds,
  SUM(CASE WHEN NOT build_cache_enabled THEN 1 ELSE 0 END) as cache_off
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '7' DAY AND build_tool = 'Gradle'
GROUP BY projectname HAVING SUM(CASE WHEN NOT build_cache_enabled THEN 1 ELSE 0 END) > 0
ORDER BY cache_off DESC
```

## References

- **SQL patterns**: See [references/sql-patterns.md](references/sql-patterns.md) for cache, config-cache, performance, task, test, failure, and deprecation queries
- **Dashboard IDs**: See [references/dashboards.md](references/dashboards.md) for pre-built dashboard queries via `fetch_query_by_panel`

## Presenting Results

Format aggregate data clearly:

```
Build Cache Performance (last 7 days)
=====================================
Project: my-app
Builds analyzed: 247

Cache hit rate: 73% (target: 80%)
Time saved: 42.3 hours
Trend: +5% vs previous week

Top optimization opportunities:
1. :app:compileKotlin - 23% of uncached time (not cacheable: overlapping_outputs)
2. :lib:test - 18% of uncached time (cacheable but missing)
```

## Context Gathering

Before querying, establish:
- **Project name**: `grep -E "^rootProject.name" settings.gradle* 2>/dev/null`
- **Time range**: Default to 7 days, ask if user wants different
- **Environment**: CI vs LOCAL (use `environment` column or `tags`)

## Error Handling

If DRV tools unavailable:
- Note that DRV is not configured
- Fall back to `mcp__develocity__*` tools for individual build queries
- Suggest configuring DRV MCP server for aggregate analytics
