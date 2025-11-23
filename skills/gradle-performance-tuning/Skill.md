---
name: gradle-performance-tuning
description: Optimizes Gradle build performance through caching, parallelization, and daemon tuning. Use when builds are slow or performance optimization is needed.
version: 1.0.0
---

# Gradle Performance Tuning

Optimize Gradle build performance for faster builds.

## When to Use

Invoke when users mention:
- Slow builds
- Performance optimization
- Build speed improvements
- Build cache issues
- Gradle daemon problems

## Quick Optimizations

### 1. Build Cache
```properties
org.gradle.caching=true
```
See `build-cache.md` for detailed cache configuration

### 2. Parallel Execution
```properties
org.gradle.parallel=true
org.gradle.workers.max=8
```
See `parallel-builds.md` for parallel execution details

### 3. Daemon Tuning
```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```
See `daemon-optimization.md` for heap sizing guidelines

### 4. Configuration Cache
```properties
org.gradle.configuration-cache=true
```
See `configuration-cache.md` for compatibility and troubleshooting

## Analysis Steps

1. Check current gradle.properties settings
2. Measure baseline build time with `--profile`
3. Identify bottlenecks
4. Apply optimizations incrementally
5. Measure improvements

See `performance-analysis.md` for detailed profiling guide

## Priority Recommendations

1. **HIGH**: Enable build cache (50%+ speedup)
2. **HIGH**: Optimize daemon heap for project size
3. **MEDIUM**: Enable parallel execution
4. **MEDIUM**: Configuration cache (Gradle 8+)
5. **LOW**: Tune worker threads

## Reference Files

- **Build cache**: `build-cache.md`
- **Configuration cache**: `configuration-cache.md`
- **Daemon optimization**: `daemon-optimization.md`
- **Parallel builds**: `parallel-builds.md`
- **Performance analysis**: `performance-analysis.md`
- **Common issues**: `performance-issues.md`
