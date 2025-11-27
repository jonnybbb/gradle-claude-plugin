# DRV SQL Query Patterns

Common SQL patterns for querying Develocity Reporting Kit data via `mcp__drv__execute_query`.

**Critical:** Always filter by `build_start_date` partition for performance.

## Table of Contents

- [Build Cache Analysis](#build-cache-analysis)
- [Configuration Cache](#configuration-cache)
- [Build Performance](#build-performance)
- [Task/Goal Analysis](#taskgoal-analysis)
- [Test Performance](#test-performance)
- [Failure Analysis](#failure-analysis)
- [Dependency Analysis](#dependency-analysis)
- [Deprecations](#deprecations)

---

## Build Cache Analysis

### Cache hit rate trend
```sql
SELECT
  build_start_date,
  COUNT(*) as builds,
  ROUND(AVG(CASE WHEN local_build_cache_avoidance_millis + remote_build_cache_avoidance_millis > 0
      THEN 1.0 ELSE 0.0 END) * 100, 1) as hit_rate_pct,
  ROUND(SUM(local_build_cache_avoidance_millis + remote_build_cache_avoidance_millis) / 60000.0, 1) as minutes_saved
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '30' DAY
  AND projectname = '{project}'
  AND build_cache_enabled = true
GROUP BY build_start_date
ORDER BY build_start_date DESC
```

### Cache configuration issues
```sql
SELECT
  projectname,
  COUNT(*) as builds,
  SUM(CASE WHEN NOT build_cache_enabled THEN 1 ELSE 0 END) as cache_disabled,
  SUM(CASE WHEN local_build_cache_disabled_due_to_error THEN 1 ELSE 0 END) as local_errors,
  SUM(CASE WHEN remote_build_cache_disabled_due_to_error THEN 1 ELSE 0 END) as remote_errors
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '7' DAY
  AND build_tool = 'Gradle'
GROUP BY projectname
HAVING SUM(CASE WHEN NOT build_cache_enabled THEN 1 ELSE 0 END) > 0
   OR SUM(CASE WHEN local_build_cache_disabled_due_to_error THEN 1 ELSE 0 END) > 0
ORDER BY builds DESC
```

### Non-cacheable tasks (high impact)
```sql
SELECT
  goal_or_task_path,
  goal_or_task_type,
  non_cacheability_category,
  COUNT(*) as executions,
  ROUND(SUM(goal_or_task_duration_millis) / 60000.0, 1) as total_minutes
FROM unit_execution_summary
WHERE build_start_date >= current_date - INTERVAL '7' DAY
  AND projectname = '{project}'
  AND avoidance_outcome = 'executed_not_cacheable'
GROUP BY goal_or_task_path, goal_or_task_type, non_cacheability_category
ORDER BY total_minutes DESC
LIMIT 20
```

---

## Configuration Cache

### Adoption trend
```sql
SELECT
  build_start_date,
  COUNT(*) as total_builds,
  SUM(CASE WHEN config_cache_enabled THEN 1 ELSE 0 END) as cc_enabled,
  SUM(CASE WHEN config_cache_used THEN 1 ELSE 0 END) as cc_hits,
  ROUND(SUM(CASE WHEN config_cache_used THEN 1 ELSE 0 END) * 100.0 /
        NULLIF(SUM(CASE WHEN config_cache_enabled THEN 1 ELSE 0 END), 0), 1) as hit_rate_pct
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '30' DAY
  AND build_tool = 'Gradle'
  AND projectname = '{project}'
GROUP BY build_start_date
ORDER BY build_start_date DESC
```

### Miss reasons
```sql
SELECT
  gradle_configuration_cache_outcome,
  FLATTEN(gradle_configuration_cache_miss_reasons) as miss_reason,
  COUNT(*) as occurrences
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '7' DAY
  AND config_cache_enabled = true
  AND projectname = '{project}'
  AND gradle_configuration_cache_miss_reasons IS NOT NULL
GROUP BY gradle_configuration_cache_outcome, miss_reason
ORDER BY occurrences DESC
```

---

## Build Performance

### Build duration trend
```sql
SELECT
  build_start_date,
  environment,
  COUNT(*) as builds,
  ROUND(AVG(build_duration_millis) / 1000.0, 1) as avg_seconds,
  ROUND(APPROX_PERCENTILE(build_duration_millis, 0.5) / 1000.0, 1) as p50_seconds,
  ROUND(APPROX_PERCENTILE(build_duration_millis, 0.95) / 1000.0, 1) as p95_seconds
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '30' DAY
  AND projectname = '{project}'
GROUP BY build_start_date, environment
ORDER BY build_start_date DESC, environment
```

### CPU utilization (underutilized builds)
```sql
SELECT
  projectname,
  COUNT(*) as builds,
  AVG(number_of_cores) as avg_cores,
  AVG(number_of_workers) as avg_workers,
  AVG(resource_usage_execution_all_processes_cpu_p75) as avg_cpu_p75,
  AVG(serialization_factor) as avg_parallelization
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '7' DAY
  AND build_tool = 'Gradle'
  AND resource_usage_execution_all_processes_cpu_p75 IS NOT NULL
  AND resource_usage_execution_all_processes_cpu_p75 < 50
GROUP BY projectname
HAVING COUNT(*) >= 10
ORDER BY builds DESC
```

### Parallelization opportunities
```sql
SELECT
  projectname,
  COUNT(*) as builds,
  AVG(serialization_factor) as avg_serialization,
  SUM(CASE WHEN NOT parallel_enabled THEN 1 ELSE 0 END) as parallel_disabled_count,
  AVG(module_count) as avg_modules
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '7' DAY
  AND build_tool = 'Gradle'
  AND module_count > 1
GROUP BY projectname
HAVING AVG(serialization_factor) < 1.5
ORDER BY builds DESC
```

---

## Task/Goal Analysis

### Slowest tasks (by total time)
```sql
SELECT
  goal_or_task_path,
  goal_or_task_type,
  COUNT(*) as executions,
  ROUND(AVG(goal_or_task_duration_millis) / 1000.0, 1) as avg_seconds,
  ROUND(SUM(goal_or_task_duration_millis) / 60000.0, 1) as total_minutes
FROM unit_execution_summary
WHERE build_start_date >= current_date - INTERVAL '7' DAY
  AND projectname = '{project}'
  AND avoidance_outcome LIKE 'executed%'
GROUP BY goal_or_task_path, goal_or_task_type
ORDER BY total_minutes DESC
LIMIT 20
```

### Task avoidance breakdown
```sql
SELECT
  avoidance_outcome,
  COUNT(*) as count,
  ROUND(SUM(goal_or_task_duration_millis) / 60000.0, 1) as total_minutes,
  ROUND(SUM(avoidance_savings_millis) / 60000.0, 1) as savings_minutes
FROM unit_execution_summary
WHERE build_start_date >= current_date - INTERVAL '7' DAY
  AND projectname = '{project}'
GROUP BY avoidance_outcome
ORDER BY count DESC
```

---

## Test Performance

### Test execution time trend
```sql
SELECT
  build_start_date,
  COUNT(*) as builds,
  SUM(tests_test_classes_count) as total_test_classes,
  ROUND(SUM(serial_test_work_units_execution_time) / 60000.0, 1) as total_test_minutes
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '30' DAY
  AND projectname = '{project}'
  AND tests_test_classes_count > 0
GROUP BY build_start_date
ORDER BY build_start_date DESC
```

### PTS savings potential
```sql
SELECT
  projectname,
  COUNT(*) as builds,
  test_acceleration_pts_usage_status,
  ROUND(SUM(test_acceleration_pts_simulation_standard_wall_clock_savings_potential) / 60000.0, 1) as potential_savings_min,
  ROUND(SUM(test_acceleration_pts_usage_wall_clock_savings_millis) / 60000.0, 1) as actual_savings_min
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '7' DAY
  AND tests_test_classes_count > 0
GROUP BY projectname, test_acceleration_pts_usage_status
ORDER BY potential_savings_min DESC NULLS LAST
```

---

## Failure Analysis

### Failure rate trend
```sql
SELECT
  build_start_date,
  environment,
  COUNT(*) as total_builds,
  SUM(CASE WHEN has_failed THEN 1 ELSE 0 END) as failed,
  ROUND(SUM(CASE WHEN has_failed THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 1) as failure_rate_pct,
  SUM(CASE WHEN has_non_verification_failure THEN 1 ELSE 0 END) as infra_failures
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '30' DAY
  AND projectname = '{project}'
GROUP BY build_start_date, environment
ORDER BY build_start_date DESC
```

### Non-verification failures (infrastructure issues)
```sql
SELECT
  projectname,
  environment,
  COUNT(*) as infra_failures
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '7' DAY
  AND has_non_verification_failure = true
GROUP BY projectname, environment
ORDER BY infra_failures DESC
```

---

## Dependency Analysis

### Dependency download impact
```sql
SELECT
  build_start_date,
  COUNT(*) as builds,
  SUM(file_download_count) as total_downloads,
  ROUND(SUM(file_download_size) / 1048576.0, 1) as total_mb,
  ROUND(SUM(wall_clock_network_request_time_millis) / 60000.0, 1) as network_minutes
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '7' DAY
  AND projectname = '{project}'
  AND file_download_count > 0
GROUP BY build_start_date
ORDER BY build_start_date DESC
```

---

## Deprecations

### Deprecation warnings by project
```sql
SELECT
  projectname,
  COUNT(*) as builds_with_deprecations,
  CARDINALITY(FLATTEN(ARRAY_AGG(TRANSFORM(gradle_deprecations, x -> x.summary)))) as unique_deprecations
FROM build_summary
WHERE build_start_date >= current_date - INTERVAL '7' DAY
  AND build_tool = 'Gradle'
  AND gradle_deprecations IS NOT NULL
  AND CARDINALITY(gradle_deprecations) > 0
GROUP BY projectname
ORDER BY unique_deprecations DESC
```

### Deprecation details (single project)
```sql
SELECT DISTINCT
  dep.summary,
  dep.removaldetails,
  dep.advice
FROM build_summary
CROSS JOIN UNNEST(gradle_deprecations) AS t(dep)
WHERE build_start_date >= current_date - INTERVAL '7' DAY
  AND projectname = '{project}'
  AND gradle_deprecations IS NOT NULL
LIMIT 50
```
