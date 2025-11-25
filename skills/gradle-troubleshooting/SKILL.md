---
name: gradle-troubleshooting
description: This skill should be used when the user asks "why doesn't my Gradle build work", "fix Gradle error", "debug build failure", "Gradle build fails", "task not found", "dependency not found", or reports build failures, error messages, unexpected build behavior, or needs help with stack traces, build scans, or debugging commands.
---

# Gradle Troubleshooting

## Overview

Systematic debugging quickly identifies root causes. Start with quick diagnostics, then dive deeper as needed.

For common errors, see [references/common-errors.md](references/common-errors.md).
For debugging techniques, see [references/debugging.md](references/debugging.md).

## Quick Diagnosis

```bash
# Show stack trace
./gradlew build --stacktrace

# Verbose output
./gradlew build --info

# Generate build scan
./gradlew build --scan

# Clean rebuild
./gradlew clean build
```

## Common Errors

### Could Not Find Dependency

```
Could not find com.example:library:1.0.0
```

**Fix**: Check repositories configuration:
```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://repo.example.com") }
}
```

### Task Not Found

```
Task 'myTask' not found
```

**Fix**: List tasks: `./gradlew tasks --all`

### OutOfMemoryError

```kotlin
// gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```

### Configuration Cache Error

See gradle-config-cache skill for fixes.

## Debugging Commands

```bash
# Why task ran?
./gradlew myTask --info | grep "up-to-date"

# Dependency analysis
./gradlew dependencies
./gradlew dependencyInsight --dependency guava

# Check daemon
./gradlew --status
./gradlew --stop
```

## Quick Reference

| Symptom | Check |
|---------|-------|
| Build fails | --stacktrace |
| Task always runs | --info, check inputs/outputs |
| Slow build | --profile or --scan |
| OOM | Increase -Xmx |
| Cache miss | Check @PathSensitive |

## Cache Cleanup

```bash
# Stop daemons
./gradlew --stop

# Clear caches
rm -rf ~/.gradle/caches/build-cache-1
rm -rf .gradle/configuration-cache

# Refresh dependencies
./gradlew build --refresh-dependencies
```

## Related Files

- [references/common-errors.md](references/common-errors.md) - Error catalog
- [references/debugging.md](references/debugging.md) - Debug techniques
- [references/performance.md](references/performance.md) - Slow build diagnosis
