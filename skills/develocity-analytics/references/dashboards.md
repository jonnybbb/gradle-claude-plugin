# DRV Dashboards Reference

Use `mcp__drv__fetch_query_by_panel` with dashboard ID to get pre-built queries.

## Table of Contents

- [Build Volume & Overview](#build-volume--overview)
- [Build Cache](#build-cache)
- [Configuration Cache](#configuration-cache)
- [Build Settings](#build-settings)
- [Test Acceleration](#test-acceleration)
- [Failures](#failures)
- [Dependencies](#dependencies)
- [Infrastructure](#infrastructure)

---

## Build Volume & Overview

| Dashboard ID | Description | Use When |
|--------------|-------------|----------|
| `global-volume` | All projects and builds overview | Starting analysis, understanding scale |
| `project-volume` | Single project build distribution | Deep-dive into specific project |
| `git-repositories` | Git repos for builds | Understanding repo coverage |

---

## Build Cache

| Dashboard ID | Description | Use When |
|--------------|-------------|----------|
| `gradle-build-cache-setting-gradle` | Builds with cache disabled | Finding cache adoption gaps |
| `build-cache-configuration` | Cache config on agents | Diagnosing cache setup |
| `build-cache-errors` | Cache errors disabling caching | Investigating cache failures |
| `realized-build-cache-savings` | Actual savings from caching | Measuring cache ROI |
| `potential-build-cache-savings` | Estimated savable time | Identifying optimization potential |

---

## Configuration Cache

| Dashboard ID | Description | Use When |
|--------------|-------------|----------|
| `gradle-configuration-cache-setting-gradl` | Builds with config cache disabled | Finding adoption gaps |
| `realized-configuration-cache-savings` | Config cache behavior overview | Measuring config cache ROI |

---

## Build Settings

### Gradle

| Dashboard ID | Description | Use When |
|--------------|-------------|----------|
| `gradle-00---settings-overview-gradle` | All Gradle settings impacting performance | Initial assessment |
| `gradle-daemon-setting-gradle` | Daemon disabled builds | Finding daemon issues |
| `gradle-parallel-setting-gradle` | Parallel execution disabled | Finding parallelization gaps |
| `gradle-parallel-setting-and-module-count` | Multi-module with parallel off | High-impact optimization targets |
| `gradle-worker-pool-size-and-available-co` | Under-utilized workers | Resource optimization |
| `gradle-cpu-usage` | CPU utilization during builds | Infrastructure sizing |
| `gradle-re-run-tasks-setting-gradle` | `--rerun-tasks` usage | Finding wasteful builds |
| `gradle-refresh-dependencies-setting-grad` | `--refresh-dependencies` usage | Finding slow dep resolution |
| `gradle-capture-file-fingerprints-setting` | File fingerprints disabled | PTS/cache investigation prep |
| `gradle-background-upload-setting-gradle` | Background upload config | Build Scan upload issues |
| `gradle-build-deprecations` | Deprecation warnings | Migration preparation |

### Maven

| Dashboard ID | Description | Use When |
|--------------|-------------|----------|
| `maven-00---settings-overview-maven` | All Maven settings impacting performance | Initial assessment |
| `maven-daemon-setting-maven` | Maven Daemon disabled | Finding daemon gaps |
| `maven-multi-threading-setting-maven` | Multi-threading disabled | Finding parallelization gaps |
| `maven-multi-threading-setting-and-module` | Multi-module with threading off | High-impact targets |
| `maven-thread-pool-size-and-available-cor` | Under-utilized threads | Resource optimization |
| `maven-cpu-usage` | CPU utilization | Infrastructure sizing |
| `maven-build-cache-setting-maven` | Develocity cache disabled | Cache adoption |
| `maven-re-run-goals-setting-maven` | Rerun goals usage | Finding wasteful builds |
| `maven-update-snapshots-setting-maven` | Update snapshots usage | Dep resolution issues |
| `maven-fail-never-setting-maven` | `--fail-never` usage | Hidden failures |
| `maven-resume-setting-maven` | Maven 4.x resume flag | Failure recovery |
| `maven-capture-file-fingerprints-setting-` | File fingerprints disabled | PTS prep |
| `maven-background-upload-setting-maven` | Background upload config | Build Scan issues |

---

## Test Acceleration

| Dashboard ID | Description | Use When |
|--------------|-------------|----------|
| `potential-predictive-test-selection-savi` | PTS savings potential | Evaluating PTS adoption |
| `realized-predictive-test-selection-savin` | Actual PTS savings | Measuring PTS ROI |
| `realized-test-distribution-savings` | Test Distribution savings | Measuring TD ROI |
| `realized-test-acceleration-savings` | Combined PTS+TD savings | Overall test acceleration ROI |

---

## Failures

| Dashboard ID | Description | Use When |
|--------------|-------------|----------|
| `build-failures` | All build failures overview | Failure investigation |

---

## Dependencies

| Dashboard ID | Description | Use When |
|--------------|-------------|----------|
| `dependencies` | Dependencies overview | Security/license audit |
| `jvm-dependencies` | JVM-specific dependencies | Java ecosystem analysis |
| `dependency-downloading` | Download metrics | Network performance |

---

## Infrastructure

| Dashboard ID | Description | Use When |
|--------------|-------------|----------|
| `ci-providers` | CI systems used | Understanding CI landscape |
| `build-tools` | Build tools and versions | Version adoption tracking |
| `jvms` | JVM versions and vendors | JVM standardization |
| `plugins` | Gradle plugins applied | Plugin audit |
| `maven-extensions` | Maven extensions | Extension audit |

---

## Using Dashboard Queries

```
# Get all dashboards
mcp__drv__fetch_query_by_panel(path="")

# Get panels in specific dashboard
mcp__drv__fetch_query_by_panel(path="gradle-build-cache-setting-gradle")

# Get all panels across all dashboards
mcp__drv__fetch_query_by_panel(path="all")
```
