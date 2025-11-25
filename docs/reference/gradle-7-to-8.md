# Gradle 7 to 8 Migration Guide

**Source**: https://docs.gradle.org/8.0/userguide/upgrading_version_7.html  
**Target**: Gradle 8.x

## Overview

Gradle 8 brings performance improvements, better caching, and removes deprecated features from Gradle 7.

## Prerequisites

```bash
# Check current version
./gradlew --version

# Update wrapper
./gradlew wrapper --gradle-version 8.11
```

## Breaking Changes

### 1. Configuration Cache

**Now stable and recommended:**
```properties
# gradle.properties
org.gradle.configuration-cache=true
```

**Common compatibility issues:**
```kotlin
// ❌ Task.project at execution
doLast { println(project.name) }

// ✅ Capture during configuration
val projectName = project.name
doLast { println(projectName) }
```

### 2. Removed APIs

**AbstractArchiveTask:**
```kotlin
// ❌ Removed
archiveName = "custom.jar"

// ✅ Use
archiveFileName.set("custom.jar")
```

**BasePluginConvention:**
```kotlin
// ❌ Removed
archivesBaseName = "myapp"

// ✅ Use
base.archivesName.set("myapp")
```

### 3. Variant Selection

**Stricter variant selection:**
```kotlin
// May need explicit capabilities
configurations.all {
    resolutionStrategy {
        capabilitiesResolution {
            withCapability("com.example:capability") {
                selectHighestVersion()
            }
        }
    }
}
```

### 4. Property API

**No more implicit conversion:**
```kotlin
// ❌ Removed
val value: String = property("key")

// ✅ Use
val value: String = property("key").get() as String
// Or better
val value = providers.gradleProperty("key").get()
```

## Deprecation Warnings

Run with warnings to find issues:
```bash
./gradlew build --warning-mode=all
```

## Migration Steps

1. **Update wrapper:**
```bash
./gradlew wrapper --gradle-version 8.11
```

2. **Run build:**
```bash
./gradlew build --warning-mode=all
```

3. **Fix deprecations:**
- Replace removed APIs
- Update to Property API
- Fix configuration cache issues

4. **Enable configuration cache:**
```properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn
```

5. **Test thoroughly:**
```bash
./gradlew clean build --configuration-cache
```

## Common Migration Issues

### Issue: Build fails immediately

**Solution:** Run with `--stacktrace` to identify cause
```bash
./gradlew build --stacktrace
```

### Issue: Custom tasks break

**Solution:** Update to Property API
```kotlin
abstract class MyTask : DefaultTask() {
    @get:Input
    abstract val myProp: Property<String>
}
```

### Issue: Configuration cache problems

**Solution:** Fix compatibility
- Capture project properties
- Use Provider API
- Avoid runtime project access

## Performance Benefits

- **Configuration cache:** 30-80% faster builds
- **Better task avoidance:** Fewer task executions
- **Improved caching:** Better cache hit rates
- **Faster dependency resolution:** Optimized algorithms

## Testing Migration

```bash
# Test on feature branch
git checkout -b gradle-8-migration

# Update and test
./gradlew wrapper --gradle-version 8.11
./gradlew clean build --configuration-cache

# Compare performance
time ./gradlew clean build  # Gradle 7
# vs
time ./gradlew clean build  # Gradle 8
```

## Related Documentation

- [Configuration Cache](configuration-cache.md)
- [Breaking Changes](breaking-changes.md)
- [Gradle 6 to 7](gradle-6-to-7.md)
