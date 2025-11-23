---
name: gradle-cache-optimization
description: Optimizes build and configuration cache, diagnoses cache misses, ensures task cacheability. Use for cache issues or task cacheability questions.
version: 1.0.0
---

# Gradle Cache Optimization

Optimize build cache and configuration cache for maximum efficiency.

## When to Use

Invoke for:
- Cache miss issues
- Tasks not using cache
- Cache configuration questions
- Task cacheability problems
- Configuration cache errors

## Build Cache Setup

```properties
# gradle.properties
org.gradle.caching=true
```

```kotlin
// settings.gradle.kts
buildCache {
    local {
        directory = file("${rootDir}/.gradle/build-cache")
        removeUnusedEntriesAfterDays = 30
    }
}
```

## Making Tasks Cacheable

```kotlin
@CacheableTask
abstract class MyTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
}
```

## Common Cache Miss Causes

1. **Absolute paths**: Use `PathSensitivity.RELATIVE`
2. **Missing annotations**: Annotate all inputs/outputs
3. **Timestamps**: Don't include current time in outputs
4. **Random values**: Ensure deterministic task logic

## Configuration Cache

```properties
org.gradle.configuration-cache=true
```

### Compatibility Fixes

```kotlin
// ❌ Bad
tasks.getByName("test")

// ✅ Good
tasks.named<Test>("test")
```
