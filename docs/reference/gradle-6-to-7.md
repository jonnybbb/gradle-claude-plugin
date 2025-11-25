# Gradle 6 to 7 Migration Guide

**Source**: https://docs.gradle.org/7.0/userguide/upgrading_version_6.html  
**Target**: Gradle 7.x

## Overview

Gradle 7 adds build cache improvements, better performance, and removes deprecated features.

## Prerequisites

```bash
# Check current version
./gradlew --version

# Update wrapper
./gradlew wrapper --gradle-version 7.6.4
```

## Major Changes

### 1. Java 8+ Required

Gradle 7 requires Java 8 minimum, Java 11 recommended.

```bash
# Check Java version
java --version
```

### 2. Kotlin DSL Default

Kotlin DSL is now production-ready:

```kotlin
// build.gradle.kts (recommended)
plugins {
    java
}
```

### 3. Configuration Cache

Experimental configuration cache:

```properties
# gradle.properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn
```

## Breaking Changes

### Property Changes

```kotlin
// ❌ Removed
archiveName = "app.jar"

// ✅ Use
archiveFileName.set("app.jar")
```

### Task Dependencies

```kotlin
// ❌ Removed
dependsOn(project(":lib").tasks["build"])

// ✅ Use
dependsOn(":lib:build")
```

### File Collections

```kotlin
// ❌ Removed
val files = fileCollection.files

// ✅ Use
val files = fileCollection.elements.get()
```

## Migration Steps

1. **Update wrapper:**
```bash
./gradlew wrapper --gradle-version 7.6.4
```

2. **Run with warnings:**
```bash
./gradlew build --warning-mode=all
```

3. **Fix deprecations:**
- Update removed APIs
- Use Property API

4. **Test build:**
```bash
./gradlew clean build
```

## Performance Benefits

- **Build cache**: Stable, faster
- **File watching**: Better incremental builds
- **Dependency resolution**: Faster resolution

## Related Documentation

- [Gradle 7 to 8](gradle-7-to-8.md): Next migration
- [Configuration Cache](configuration-cache.md): Cache details

## Quick Reference

```bash
# Update wrapper
./gradlew wrapper --gradle-version 7.6.4

# Run with warnings
./gradlew build --warning-mode=all

# Test configuration cache
./gradlew build --configuration-cache
```
