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

### Command Line Debugging

```bash
# Check cache status - look for FROM-CACHE
./gradlew build --build-cache | grep "FROM-CACHE"

# Why task executed? Use --info for details
./gradlew build --build-cache --info | grep -E "(executed|not up-to-date|FROM-CACHE)"

# Full debug logging for cache operations
./gradlew build --build-cache --debug 2>&1 | grep -i cache
```

### Build Scans (Develocity)

Build Scans provide the best cache debugging experience:

```bash
./gradlew build --build-cache --scan
```

In the Build Scan:
1. Go to **Performance → Build Cache**
2. View cache hit rate and time savings
3. Click any task to see:
   - Complete cache key
   - All inputs with their hashes
   - Why cache was missed

**Compare builds** to diagnose cross-machine cache misses:
1. Generate scans on both machines
2. Click "Compare builds" in Build Scan
3. Navigate to problem task
4. See input differences highlighted

### Develocity MCP Server

If you have the Develocity MCP server configured, query cache data:
- `mcp__develocity__getBuilds` - Find builds with low cache hit rates
- `mcp__develocity__getBuild` - Get detailed task cache information

See [references/debugging.md](references/debugging.md) for comprehensive debugging guide.

## Quick Reference

| Problem | Solution |
|---------|----------|
| Task not cached | Add `@CacheableTask` annotation |
| Cache miss across machines | Use `@PathSensitive(RELATIVE)` |
| Non-deterministic output | Remove timestamps/random values |
| Undeclared input | Add `@Input` annotation |
| "Caching disabled" | Check for missing outputs |
| Line ending differences | Add `@NormalizeLineEndings` |
| Java version mismatch | Pin toolchain version |
| File encoding issues | Set `-Dfile.encoding=UTF-8` |
| Overlapping outputs | Use separate output directories |
| Classpath order changes | Use `@Classpath` or `@CompileClasspath` |
| Volatile inputs | Use providers for deferred evaluation |

## Version Notes

- **Gradle 4.x-6.x**: Basic cache
- **Gradle 7.x**: More tasks cacheable
- **Gradle 8.x**: Most tasks cacheable by default

## Related Files

- [references/debugging.md](references/debugging.md) - Debugging cache misses, Build Scan analysis
- [references/remote-setup.md](references/remote-setup.md) - Remote cache configuration
- [references/optimization.md](references/optimization.md) - Cache hit rate optimization
- [references/ci-patterns.md](references/ci-patterns.md) - CI/CD integration

## Official Documentation

- [Build Cache Reference](https://docs.gradle.org/current/userguide/build_cache.html)
- [Common Caching Problems](https://docs.gradle.org/current/userguide/common_caching_problems.html)
- [Build Cache Debugging](https://docs.gradle.org/current/userguide/build_cache_debugging.html)
- [Build Scan Cache Guide](https://docs.gradle.com/develocity/build-scans/#build_cache)
