# Gradle Performance Optimization Skill

You are an expert in Gradle build optimization. When asked to analyze or improve Gradle build performance, follow these comprehensive guidelines based on official Gradle documentation.

## Core Performance Optimizations

### 1. Configuration Cache (Highest Impact)
**Purpose**: Caches the result of the configuration phase, allowing Gradle to skip it entirely when inputs haven't changed.

**Configuration**:
```properties
# In gradle.properties
org.gradle.configuration-cache=true
```

**Command Line**:
```bash
./gradlew --configuration-cache
```

**Benefits**:
- Dramatically reduces build times (up to 80% faster for subsequent builds)
- Skips entire configuration phase when nothing changed
- Most impactful optimization available

**Considerations**:
- May require build script changes to be compatible
- Check compatibility: `./gradlew help --configuration-cache`

### 2. Build Cache (Essential)
**Purpose**: Reuses task outputs from previous builds, even across different machines.

**Configuration**:
```properties
# In gradle.properties
org.gradle.caching=true
```

**Command Line**:
```bash
./gradlew --build-cache
```

**Benefits**:
- Avoids re-executing tasks with identical inputs
- Can reduce build times by 50% or more
- Works with remote cache servers for team-wide benefits

**Remote Cache Setup**:
```groovy
// In settings.gradle
buildCache {
    remote(HttpBuildCache) {
        url = 'https://your-cache-server.com/cache/'
        push = true
    }
}
```

### 3. Parallel Execution
**Purpose**: Executes tasks from different projects in parallel.

**Configuration**:
```properties
# In gradle.properties
org.gradle.parallel=true
```

**Benefits**:
- Utilizes multiple CPU cores
- Particularly effective for multi-project builds
- Can reduce build times by 30-50% for large projects

**Worker Count**:
```properties
# Control max parallel workers (default is number of CPU cores)
org.gradle.workers.max=4
```

### 4. File System Watching
**Purpose**: Gradle watches the file system to detect changes, improving incremental build performance.

**Configuration**:
```properties
# In gradle.properties
org.gradle.vfs.watch=true
```

**Benefits**:
- Faster incremental builds
- More accurate change detection
- Reduces file system scanning overhead

### 5. Gradle Daemon (Usually Default)
**Purpose**: Keeps Gradle running in the background to avoid startup overhead.

**Configuration**:
```properties
# In gradle.properties (enabled by default, but ensure it's not disabled)
org.gradle.daemon=true
```

**Benefits**:
- Eliminates JVM startup time
- Maintains warm JVM with optimized code
- Caches project structure and dependencies

## Memory Configuration

### JVM Memory Settings
**For Large Projects**:
```properties
# In gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
```

**For Medium Projects**:
```properties
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
```

**For Small Projects**:
```properties
org.gradle.jvmargs=-Xmx1g -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8
```

**Modern JVM Optimizations** (Java 17+):
```properties
org.gradle.jvmargs=-Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:ParallelGCThreads=4 -XX:ConcGCThreads=2 -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
```

## Best Practices

### 1. Keep Gradle Updated
- Each release includes performance improvements
- Upgrade regularly: `./gradlew wrapper --gradle-version 8.11`
- Check releases: https://gradle.org/releases/

### 2. Avoid Configuration Phase Work
**Bad**:
```groovy
def result = expensiveComputation() // Runs during configuration
tasks.register('myTask') {
    // ...
}
```

**Good**:
```groovy
tasks.register('myTask') {
    doLast {
        def result = expensiveComputation() // Runs only when task executes
    }
}
```

### 3. Use Task Configuration Avoidance
**Modern API** (lazy evaluation):
```groovy
tasks.register('myTask', MyTaskType) {
    // Configuration runs only when needed
}
```

**Old API** (eager evaluation):
```groovy
task myTask(type: MyTaskType) {
    // Configuration runs immediately
}
```

### 4. Modularize Your Build
- Split monolithic projects into modules
- Enables parallel execution and incremental builds
- Gradle rebuilds only changed modules

### 5. Optimize Dependencies
```properties
# In gradle.properties
# Use dependency verification
org.gradle.dependency.verification=lenient

# Disable unnecessary features
android.enableJetifier=false
android.useAndroidX=true
```

### 6. Use KSP Over KAPT (Kotlin Projects)
**KAPT** (slower):
```kotlin
plugins {
    kotlin("kapt")
}

dependencies {
    kapt("com.google.dagger:dagger-compiler:2.x")
}
```

**KSP** (faster):
```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.9.x-1.0.x"
}

dependencies {
    ksp("com.google.dagger:dagger-compiler:2.x")
}
```

### 7. Incremental Compilation (Kotlin)
```properties
# In gradle.properties
kotlin.incremental=true
kotlin.incremental.js=true
```

## Performance Analysis

### 1. Build Scan
```bash
./gradlew build --scan
```
- Provides detailed performance breakdown
- Identifies slow tasks and configuration issues
- Free for individual developers
- View at: https://scans.gradle.com

### 2. Profile Report
```bash
./gradlew build --profile
```
- Generates HTML report in `build/reports/profile/`
- Shows task execution times
- Identifies configuration bottlenecks

### 3. Dry Run
```bash
./gradlew build --dry-run
```
- Shows task execution order without running tasks
- Useful for understanding task graph

### 4. Debug Configuration Cache
```bash
./gradlew build --configuration-cache --configuration-cache-problems=warn
```
- Shows configuration cache compatibility issues
- Helps migrate to configuration cache

## Complete Optimized gradle.properties Template

```properties
# Gradle Build Performance Optimization
# Based on https://docs.gradle.org/current/userguide/performance.html

# Enable Configuration Cache (Gradle 8.1+)
# Caches configuration phase results for dramatic speedup
org.gradle.configuration-cache=true

# Enable Build Cache
# Reuses task outputs from previous builds
org.gradle.caching=true

# Enable Parallel Execution
# Executes tasks from different projects in parallel
org.gradle.parallel=true

# Enable File System Watching
# Improves incremental build performance
org.gradle.vfs.watch=true

# Enable Gradle Daemon (usually default)
# Keeps Gradle running in background
org.gradle.daemon=true

# Configure JVM Memory
# Adjust based on project size
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8

# Configure Worker Processes
# Default is CPU cores, adjust if needed
# org.gradle.workers.max=8

# Kotlin Incremental Compilation
kotlin.incremental=true

# Kotlin Daemon Memory
kotlin.daemon.jvmargs=-Xmx2g

# Disable unnecessary features
# Uncomment if not using AndroidX
# android.enableJetifier=false
# android.useAndroidX=true

# Enable Gradle build cache HTTP client timeout (optional)
# org.gradle.cache.timeout=60000
```

## Action Steps for Analysis

When asked to analyze or improve Gradle build performance:

1. **Check for gradle.properties**: Look for existing gradle.properties in project root
2. **Analyze Current Configuration**: Review current settings
3. **Identify Missing Optimizations**: Compare with best practices above
4. **Check Build Scripts**: Look for configuration phase anti-patterns
5. **Review Dependencies**: Check for unnecessary dependencies or outdated versions
6. **Generate Recommendations**: Provide specific, actionable improvements
7. **Create/Update gradle.properties**: Apply recommended settings
8. **Suggest Testing**: Recommend running `./gradlew build --scan` to measure improvements

## Compatibility Notes

- **Configuration Cache**: Requires Gradle 8.1+, may need build script adjustments
- **File System Watching**: Works best on Gradle 7.0+
- **Build Cache**: Available in all modern Gradle versions
- **Parallel Execution**: Ensure projects don't have cross-project dependencies

## Common Issues and Solutions

### Out of Memory Errors
```properties
# Increase heap size
org.gradle.jvmargs=-Xmx6g -XX:MaxMetaspaceSize=1g
```

### Configuration Cache Failures
```bash
# Check problems
./gradlew build --configuration-cache --configuration-cache-problems=warn

# Common fixes:
# - Avoid `buildscript` block side effects
# - Use lazy task configuration
# - Avoid using Project at execution time
```

### Slow Dependency Resolution
```groovy
// In build.gradle
configurations.all {
    resolutionStrategy {
        cacheDynamicVersionsFor 10, 'minutes'
        cacheChangingModulesFor 4, 'hours'
    }
}
```

## Measurement and Validation

Always measure performance improvements:

```bash
# Before changes
./gradlew clean build --scan

# After changes
./gradlew clean build --scan

# Compare build scans
```

Expected improvements:
- Configuration Cache: 50-80% faster subsequent builds
- Build Cache: 30-70% faster clean builds
- Parallel Execution: 20-50% faster for multi-module projects
- Combined: 70-90% faster in optimal scenarios
