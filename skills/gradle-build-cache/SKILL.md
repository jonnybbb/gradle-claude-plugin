---
name: gradle-build-cache
description: This skill should be used when the user asks to "enable build cache", "set up remote cache", "improve cache hit rate", "fix cache misses", "configure CI build caching", "make task cacheable", or mentions build cache configuration, @CacheableTask, cache optimization, or remote cache servers.
---

# Gradle Build Cache

## Overview

The build cache stores task outputs and reuses them across builds, even on different machines. This provides 50-90% faster clean builds with a populated cache.

For remote cache setup, see [references/remote-setup.md](references/remote-setup.md).
For cache optimization, see [references/optimization.md](references/optimization.md).

## Quick Start

### Enable Build Cache

```properties
# gradle.properties
org.gradle.caching=true
```

### Verify It Works

```bash
# First build (populates cache)
./gradlew clean build

# Second build (uses cache)
./gradlew clean build
# Look for "FROM-CACHE" in output
```

## Making Tasks Cacheable

```kotlin
@CacheableTask
abstract class MyTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)  // Required!
    abstract val sources: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun process() { /* ... */ }
}
```

### Requirements

- `@CacheableTask` annotation
- All inputs declared with annotations
- All outputs declared
- `@PathSensitive` on file inputs
- Deterministic outputs (no timestamps)

## Diagnosing Cache Issues

```bash
# Check cache status
./gradlew build --build-cache | grep "FROM-CACHE"

# Why task executed?
./gradlew build --build-cache --info | grep "not up-to-date"

# Use build scan
./gradlew build --build-cache --scan
```

## Quick Reference

| Problem | Solution |
|---------|----------|
| Task not cached | Add @CacheableTask |
| Cache miss across machines | Use @PathSensitive(RELATIVE) |
| Non-deterministic output | Remove timestamps/random values |
| Undeclared input | Add @Input annotation |
| "Caching disabled" | Check for missing outputs |

## Version Notes

- **Gradle 4.x-6.x**: Basic cache
- **Gradle 7.x**: More tasks cacheable
- **Gradle 8.x**: Most tasks cacheable by default

## Related Files

- [references/remote-setup.md](references/remote-setup.md) - Remote cache configuration
- [references/optimization.md](references/optimization.md) - Cache hit rate optimization
- [references/ci-patterns.md](references/ci-patterns.md) - CI/CD integration
