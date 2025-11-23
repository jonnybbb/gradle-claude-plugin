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

## Optimization Areas

### 1. Build Cache
```properties
org.gradle.caching=true
```

### 2. Parallel Execution
```properties
org.gradle.parallel=true
org.gradle.workers.max=8
```

### 3. Daemon Tuning
```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```

### 4. Configuration Cache
```properties
org.gradle.configuration-cache=true
```

## Analysis Steps

1. Check current gradle.properties settings
2. Measure baseline build time
3. Identify bottlenecks (use --profile)
4. Apply optimizations incrementally
5. Measure improvements

## Recommendations Priority

1. HIGH: Enable build cache (50%+ speedup)
2. HIGH: Optimize daemon heap for project size
3. MEDIUM: Enable parallel execution
4. MEDIUM: Configuration cache (Gradle 8+)
5. LOW: Tune worker threads
