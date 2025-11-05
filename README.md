# Claude Code - Gradle Performance Optimization Skills

This repository provides Claude Code with comprehensive knowledge and tools for optimizing Gradle build performance based on [official Gradle documentation](https://docs.gradle.org/current/userguide/performance.html).

## Overview

The Gradle Performance skill enables Claude Code to:
- Analyze existing Gradle build configurations
- Recommend performance optimizations
- Apply best practices from official Gradle documentation
- Generate optimized `gradle.properties` configurations
- Identify common performance bottlenecks

## Features

### 🚀 Core Optimizations Covered

1. **Configuration Cache** - Cache configuration phase results (50-80% faster builds)
2. **Build Cache** - Reuse task outputs from previous builds (30-70% faster)
3. **Parallel Execution** - Execute tasks in parallel across projects
4. **File System Watching** - Improve incremental build performance
5. **JVM Memory Tuning** - Optimize memory settings for your project size
6. **Kotlin Optimizations** - Incremental compilation and KSP over KAPT
7. **Dependency Management** - Optimize dependency resolution

### 📁 Repository Structure

```
.
├── .claude/
│   └── skills/
│       └── gradle-performance.md    # Main performance optimization skill
├── templates/
│   └── gradle.properties.optimized  # Optimized gradle.properties template
└── README.md                        # This file
```

## Usage with Claude Code

### Quick Start

1. **Ask Claude to analyze your build**:
   ```
   Analyze my Gradle build configuration and suggest performance improvements
   ```

2. **Apply optimizations**:
   ```
   Apply recommended Gradle performance optimizations to my gradle.properties
   ```

3. **Get specific guidance**:
   ```
   How can I enable configuration cache in my Gradle project?
   ```

### Common Use Cases

#### Analyze Existing Configuration
```
Review my gradle.properties and identify missing performance optimizations
```

#### Generate Optimized Configuration
```
Create an optimized gradle.properties for a large multi-module Android project
```

#### Troubleshoot Performance Issues
```
My Gradle builds are slow. Help me identify and fix performance bottlenecks
```

#### Migration Assistance
```
Help me migrate to configuration cache
```

## Performance Optimizations

### Key Settings

The skill provides guidance on all critical Gradle performance settings:

```properties
# Configuration Cache (Gradle 8.1+)
org.gradle.configuration-cache=true

# Build Cache
org.gradle.caching=true

# Parallel Execution
org.gradle.parallel=true

# File System Watching
org.gradle.vfs.watch=true

# Memory Configuration
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g

# Kotlin Optimizations
kotlin.incremental=true
kotlin.daemon.jvmargs=-Xmx2g
```

### Expected Performance Improvements

| Optimization | Expected Improvement |
|--------------|---------------------|
| Configuration Cache | 50-80% faster subsequent builds |
| Build Cache | 30-70% faster clean builds |
| Parallel Execution | 20-50% faster multi-module builds |
| Combined Optimizations | 70-90% faster in optimal scenarios |

## Templates

### Optimized gradle.properties

A complete, production-ready `gradle.properties` template is available in `templates/gradle.properties.optimized`. It includes:

- All core performance optimizations
- Detailed comments explaining each setting
- Memory configurations for different project sizes
- Kotlin-specific optimizations
- Android-specific optimizations (when applicable)
- Remote build cache configuration examples

### Using the Template

1. Copy the template to your project root:
   ```bash
   cp templates/gradle.properties.optimized /path/to/your/project/gradle.properties
   ```

2. Adjust memory settings based on your project size:
   - Small projects: `-Xmx1g`
   - Medium projects: `-Xmx2g`
   - Large projects: `-Xmx4g`
   - Very large projects: `-Xmx6g`

3. Test the configuration:
   ```bash
   ./gradlew clean build --scan
   ```

## Best Practices

### 1. Always Measure Performance

Before and after applying optimizations:
```bash
# Generate build scan
./gradlew build --scan

# Generate profile report
./gradlew build --profile
```

### 2. Verify Configuration Cache Compatibility

```bash
# Test configuration cache
./gradlew build --configuration-cache

# Check for problems
./gradlew build --configuration-cache --configuration-cache-problems=warn
```

### 3. Keep Gradle Updated

```bash
# Update to latest Gradle version
./gradlew wrapper --gradle-version 8.11
```

### 4. Use Modern Gradle APIs

- Prefer `tasks.register()` over `task` for lazy evaluation
- Avoid work in configuration phase
- Use task configuration avoidance

## Troubleshooting

### Out of Memory Errors

Increase heap size in gradle.properties:
```properties
org.gradle.jvmargs=-Xmx6g -XX:MaxMetaspaceSize=1536m
```

### Configuration Cache Issues

Check compatibility and warnings:
```bash
./gradlew build --configuration-cache --configuration-cache-problems=warn
```

Common fixes:
- Avoid `buildscript` block side effects
- Use lazy task configuration
- Don't use `Project` at execution time

### Slow Dependency Resolution

Add to build.gradle:
```groovy
configurations.all {
    resolutionStrategy {
        cacheDynamicVersionsFor 10, 'minutes'
        cacheChangingModulesFor 4, 'hours'
    }
}
```

## Advanced Topics

### Remote Build Cache

Configure in `settings.gradle`:
```groovy
buildCache {
    local {
        enabled = true
    }
    remote(HttpBuildCache) {
        url = 'https://your-cache-server.com/cache/'
        push = true
    }
}
```

### Kotlin KSP vs KAPT

Migrate from KAPT to KSP for better performance:

**Before (KAPT)**:
```kotlin
plugins {
    kotlin("kapt")
}
dependencies {
    kapt("com.google.dagger:dagger-compiler:2.x")
}
```

**After (KSP)**:
```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.9.x-1.0.x"
}
dependencies {
    ksp("com.google.dagger:dagger-compiler:2.x")
}
```

## Resources

- [Official Gradle Performance Guide](https://docs.gradle.org/current/userguide/performance.html)
- [Gradle Best Practices](https://docs.gradle.org/current/userguide/best_practices_performance.html)
- [Build Scans](https://scans.gradle.com)
- [Gradle Releases](https://gradle.org/releases/)

## Contributing

To improve this skill:

1. Update `.claude/skills/gradle-performance.md` with new best practices
2. Update `templates/gradle.properties.optimized` with new settings
3. Update this README with usage examples
4. Test with real-world Gradle projects

## License

This repository is provided as-is for use with Claude Code.

## Version History

- **v1.0** - Initial release with comprehensive Gradle performance optimizations based on Gradle 8.x documentation
