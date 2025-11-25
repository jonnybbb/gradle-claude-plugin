---
name: gradle-performance
description: This skill should be used when the user asks to "speed up Gradle builds", "optimize build performance", "reduce build times", "fix slow Gradle", "improve CI/CD performance", or mentions slow builds, long configuration time, build performance regression, parallelization, daemon issues, or JVM memory tuning.
---

# Gradle Performance

## Overview

Optimize Gradle builds through parallelization, caching, JVM tuning, and task optimization. Target: 30-80% faster builds.

For bottleneck solutions, see [references/bottlenecks.md](references/bottlenecks.md).
For JVM tuning, see [references/jvm-tuning.md](references/jvm-tuning.md).

## Quick Wins

### Enable Core Optimizations

```properties
# gradle.properties
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.vfs.watch=true
org.gradle.workers.max=8
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+UseParallelGC
```

**Impact**: 30-80% faster builds

### Profile the Build

```bash
# Visual timeline
./gradlew build --scan

# HTML report
./gradlew build --profile
```

## Performance Targets

| Metric | Target |
|--------|--------|
| Configuration time | <5% of total |
| Clean build | <2min typical |
| Incremental build | <10s |
| No-op build | <1s with config cache |

## Common Bottlenecks

| Problem | Detection | Solution |
|---------|-----------|----------|
| Long configuration | --profile | Lazy task registration |
| No parallelization | Check gradle.properties | Enable parallel=true |
| Cache misses | --scan | Fix @PathSensitive |
| Slow tasks | --profile | Check inputs/outputs |
| Memory pressure | GC logs | Increase -Xmx |

## Diagnosis Commands

```bash
# Configuration time
./gradlew help --profile

# Task execution
./gradlew build --scan

# Why task ran
./gradlew build --info | grep "up-to-date"

# Memory usage
./gradlew build -Dorg.gradle.jvmargs="-Xmx4g -XX:+PrintGCDetails"
```

## Quick Reference

| Optimization | Property |
|--------------|----------|
| Parallel execution | org.gradle.parallel=true |
| Build cache | org.gradle.caching=true |
| Config cache | org.gradle.configuration-cache=true |
| File watching | org.gradle.vfs.watch=true |
| Workers | org.gradle.workers.max=N |

## Related Files

- [references/bottlenecks.md](references/bottlenecks.md) - Common bottlenecks & fixes
- [references/jvm-tuning.md](references/jvm-tuning.md) - JVM optimization
- [references/measurement.md](references/measurement.md) - Before/after measurement
