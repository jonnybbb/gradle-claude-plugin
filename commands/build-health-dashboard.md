---
description: Show key build health metrics dashboard from DRV aggregate data
allowed-tools: Read, Grep, Bash, AskUserQuestion, mcp__drv__execute_query, mcp__drv__describe_table
---

# Build Health Dashboard

Display a comprehensive build health dashboard using Develocity Reporting Kit (DRV) aggregate data.

## Step 1: Gather Context

Determine the project:

```bash
grep -E "^rootProject.name" settings.gradle* 2>/dev/null | head -1
```

If project cannot be detected, ask user for the Develocity project name.

## Step 2: Query Build Overview

Execute via `mcp__drv__execute_query`:

```sql
SELECT
  environment,
  COUNT(*) as builds,
  SUM(CASE WHEN has_failed THEN 1 ELSE 0 END) as failed,
  ROUND(SUM(CASE WHEN has_failed THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 1) as failure_rate_pct,
  SUM(CASE WHEN has_non_verification_failure THEN 1 ELSE 0 END) as infra_failures,
  ROUND(AVG(build_duration_millis) / 1000.0, 1) as avg_build_seconds,
  ROUND(APPROX_PERCENTILE(build_duration_millis, 0.95) / 1000.0, 1) as p95_build_seconds
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '7' DAY
  AND projectname = '{project}'
GROUP BY environment
```

## Step 3: Query Cache Metrics

```sql
SELECT
  ROUND(AVG(CASE WHEN local_build_cache_avoidance_millis + remote_build_cache_avoidance_millis > 0
      THEN 1.0 ELSE 0.0 END) * 100, 1) as cache_hit_rate_pct,
  ROUND(SUM(local_build_cache_avoidance_millis + remote_build_cache_avoidance_millis) / 3600000.0, 1) as hours_saved,
  SUM(CASE WHEN config_cache_enabled THEN 1 ELSE 0 END) as cc_enabled_builds,
  SUM(CASE WHEN config_cache_used THEN 1 ELSE 0 END) as cc_hits
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '7' DAY
  AND projectname = '{project}'
  AND build_tool = 'Gradle'
```

## Step 4: Query Test Metrics

```sql
SELECT
  SUM(tests_test_classes_count) as total_test_classes,
  ROUND(SUM(serial_test_work_units_execution_time) / 3600000.0, 1) as test_hours,
  test_acceleration_pts_usage_status,
  ROUND(SUM(test_acceleration_pts_usage_wall_clock_savings_millis) / 60000.0, 1) as pts_savings_min
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '7' DAY
  AND projectname = '{project}'
  AND tests_test_classes_count > 0
GROUP BY test_acceleration_pts_usage_status
```

## Step 5: Query Dependency Download Impact

```sql
SELECT
  COUNT(*) as builds_with_downloads,
  SUM(file_download_count) as total_files,
  ROUND(SUM(file_download_size) / 1073741824.0, 2) as total_gb,
  ROUND(SUM(wall_clock_network_request_time_millis) / 60000.0, 1) as network_minutes
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '7' DAY
  AND projectname = '{project}'
  AND file_download_count > 0
```

## Step 6: Query Settings Issues

```sql
SELECT
  SUM(CASE WHEN NOT parallel_enabled THEN 1 ELSE 0 END) as parallel_disabled,
  SUM(CASE WHEN NOT daemon_enabled THEN 1 ELSE 0 END) as daemon_disabled,
  SUM(CASE WHEN rerun_tasks_enabled THEN 1 ELSE 0 END) as rerun_tasks,
  SUM(CASE WHEN refresh_dependencies_enabled THEN 1 ELSE 0 END) as refresh_deps
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '7' DAY
  AND projectname = '{project}'
  AND build_tool = 'Gradle'
```

## Step 7: Present Dashboard

```
═══════════════════════════════════════════════════════════════
                  BUILD HEALTH DASHBOARD
═══════════════════════════════════════════════════════════════

Project: {project}
Period: Last 7 days
Total builds: {count}

┌─────────────────────────────────────────────────────────────┐
│                    BUILD SUCCESS                            │
├──────────────┬──────────┬─────────┬─────────────────────────┤
│ Environment  │ Builds   │ Failed  │ Success Rate            │
├──────────────┼──────────┼─────────┼─────────────────────────┤
│ CI           │   {ci}   │   {cf}  │ {cr}% ████████░░  │
│ Local        │   {lo}   │   {lf}  │ {lr}% ██████████  │
└──────────────┴──────────┴─────────┴─────────────────────────┘

Infrastructure failures: {infra} (non-verification failures)

┌─────────────────────────────────────────────────────────────┐
│                    BUILD PERFORMANCE                        │
├─────────────────────────────────────────────────────────────┤
│ Average build time:    {avg} seconds                        │
│ P95 build time:        {p95} seconds                        │
│ Cache hit rate:        {cache}%                             │
│ Time saved by cache:   {saved} hours                        │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    CONFIGURATION CACHE                      │
├─────────────────────────────────────────────────────────────┤
│ Enabled builds:        {cc_enabled}                         │
│ Cache hits:            {cc_hits}                            │
│ Hit rate:              {cc_rate}%                           │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    TEST EXECUTION                           │
├─────────────────────────────────────────────────────────────┤
│ Test classes run:      {tests}                              │
│ Total test time:       {test_hours} hours                   │
│ PTS status:            {pts_status}                         │
│ PTS savings:           {pts_min} minutes                    │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    DEPENDENCY DOWNLOADS                     │
├─────────────────────────────────────────────────────────────┤
│ Files downloaded:      {files}                              │
│ Data transferred:      {gb} GB                              │
│ Network time:          {net_min} minutes                    │
└─────────────────────────────────────────────────────────────┘

── SETTINGS ISSUES ───────────────────────────────────────────

{settings_issues_or_checkmark}

══════════════════════════════════════════════════════════════
                    HEALTH SCORE: {score}/100
══════════════════════════════════════════════════════════════

{recommendations}
```

## Health Score Calculation

| Metric | Weight | Scoring |
|--------|--------|---------|
| Success rate | 30 | 100% = 30pts, <80% = 0pts |
| Cache hit rate | 25 | >70% = 25pts, <50% = 0pts |
| Config cache adoption | 15 | >50% = 15pts, 0% = 0pts |
| No settings issues | 15 | All good = 15pts, issues = -5 each |
| No infra failures | 15 | 0 = 15pts, >5% = 0pts |

## Error Handling

If DRV tools are unavailable:
```
DRV MCP server not configured.

To view the build health dashboard, configure the DRV MCP server.
For individual build analysis, use /doctor command instead.
```

## Related

- `/doctor` - Local project health analysis
- `/analyze-cache-trends` - Detailed cache trend analysis
- `develocity-analytics` skill - Custom SQL queries
