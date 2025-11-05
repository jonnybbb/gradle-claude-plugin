# Gradle Performance Optimization - Quick Reference

## Essential gradle.properties Settings

```properties
# Top 5 Performance Settings (add to gradle.properties in project root)
org.gradle.configuration-cache=true  # 50-80% faster subsequent builds
org.gradle.caching=true              # 30-70% faster clean builds
org.gradle.parallel=true             # 20-50% faster multi-module builds
org.gradle.vfs.watch=true            # Better incremental builds
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g  # Adequate memory
```

## Quick Command Reference

```bash
# Measure current performance
./gradlew build --scan

# Test with configuration cache
./gradlew build --configuration-cache

# Check configuration cache problems
./gradlew build --configuration-cache --configuration-cache-problems=warn

# Generate profile report
./gradlew build --profile

# Update Gradle version
./gradlew wrapper --gradle-version 8.11

# Clean build with cache
./gradlew clean build --build-cache
```

## Performance Checklist

- [ ] Enable configuration cache
- [ ] Enable build cache
- [ ] Enable parallel execution
- [ ] Enable file system watching
- [ ] Configure JVM memory appropriately
- [ ] Use Gradle 8.1 or later
- [ ] Enable Kotlin incremental compilation (if using Kotlin)
- [ ] Use KSP instead of KAPT (if using annotation processing)
- [ ] Avoid work in configuration phase
- [ ] Use lazy task configuration (`tasks.register()`)
- [ ] Modularize large projects
- [ ] Keep Gradle updated

## Common Issues and Quick Fixes

### Out of Memory
```properties
# Increase in gradle.properties
org.gradle.jvmargs=-Xmx6g -XX:MaxMetaspaceSize=1536m
```

### Configuration Cache Incompatible
```bash
# Check what needs fixing
./gradlew build --configuration-cache --configuration-cache-problems=warn

# Common fixes:
# - Use lazy configuration
# - Avoid Project at execution time
# - Remove buildscript side effects
```

### Slow Dependency Resolution
```groovy
// Add to build.gradle
configurations.all {
    resolutionStrategy {
        cacheDynamicVersionsFor 10, 'minutes'
        cacheChangingModulesFor 4, 'hours'
    }
}
```

## Memory Recommendations by Project Size

| Project Size | Heap Memory | Metaspace | Total RAM Needed |
|--------------|-------------|-----------|------------------|
| Small        | -Xmx1g      | -XX:MaxMetaspaceSize=384m | 2-3 GB |
| Medium       | -Xmx2g      | -XX:MaxMetaspaceSize=512m | 4-6 GB |
| Large        | -Xmx4g      | -XX:MaxMetaspaceSize=1g | 8-12 GB |
| Very Large   | -Xmx6g      | -XX:MaxMetaspaceSize=1536m | 16+ GB |

## Kotlin-Specific Optimizations

```properties
# In gradle.properties
kotlin.incremental=true
kotlin.daemon.jvmargs=-Xmx2g
kotlin.compiler.execution.strategy=daemon
```

```kotlin
// In build.gradle.kts - Use KSP instead of KAPT
plugins {
    id("com.google.devtools.ksp") version "1.9.x-1.0.x"
}

dependencies {
    ksp("your.annotation.processor")  // Not kapt()
}
```

## Expected Performance Gains

| Optimization | First Build | Subsequent Builds | Clean Builds |
|--------------|-------------|-------------------|--------------|
| Configuration Cache | No change | ⚡ 50-80% faster | No change |
| Build Cache | No change | ⚡ 30-50% faster | ⚡ 30-70% faster |
| Parallel Execution | ⚡ 20-50% faster | ⚡ 20-50% faster | ⚡ 20-50% faster |
| File System Watching | No change | ⚡ 10-20% faster | No change |
| **Combined** | ⚡ 20-50% faster | ⚡⚡ 70-90% faster | ⚡ 40-70% faster |

## Verification Steps

1. **Before optimization**: Run `./gradlew clean build --scan` and note the time
2. **Apply settings**: Add optimizations to `gradle.properties`
3. **First test**: Run `./gradlew clean build --scan` (may be slightly slower due to cache population)
4. **Second test**: Run `./gradlew clean build --scan` (should be much faster)
5. **Incremental test**: Make a small change, run `./gradlew build` (should be very fast)

## Build Scan Analysis

When you run `./gradlew build --scan`, look for:

- **Configuration time**: Should be minimal with configuration cache
- **Task execution time**: Should show parallel execution
- **Cache hits**: Should increase over time with build cache
- **Avoided tasks**: Shows work saved by up-to-date checks

## One-Liner Setup

Copy this to your project root and adjust as needed:

```bash
# Create optimized gradle.properties
cat > gradle.properties << 'EOF'
org.gradle.configuration-cache=true
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.vfs.watch=true
org.gradle.daemon=true
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
kotlin.incremental=true
kotlin.daemon.jvmargs=-Xmx2g
EOF

# Test it
./gradlew clean build --scan
```

## Resources

- Full documentation: See `README.md`
- Detailed skill: See `.claude/skills/gradle-performance.md`
- Template: See `templates/gradle.properties.optimized`
- Official docs: https://docs.gradle.org/current/userguide/performance.html
