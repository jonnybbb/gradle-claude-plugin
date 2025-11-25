# Performance Tuning Reference

**Source:** https://docs.gradle.org/current/userguide/performance.html  
**Target:** Gradle 7+ (optimized for 8+)

## Quick Wins

### 1. Enable Daemon (Default in Gradle 3+)
```properties
org.gradle.daemon=true
```
**Impact:** 10-30% faster builds

### 2. Parallel Execution
```properties
org.gradle.parallel=true
org.gradle.workers.max=4
```
**Impact:** 20-50% faster for multi-project builds

### 3. Configuration Cache
```properties
org.gradle.configuration-cache=true
```
**Impact:** 30-80% faster incremental builds

### 4. Build Cache
```properties
org.gradle.caching=true
```
**Impact:** 50-90% faster clean builds (with populated cache)

### 5. JVM Arguments
```properties
org.gradle.jvmargs=-Xmx4g -XX:+HeapDumpOnOutOfMemoryError -XX:+UseParallelGC
```
**Impact:** Prevents OOM, improves GC performance

## gradle.properties Optimization Template

```properties
# Gradle 8.x Optimized Configuration

# Daemon
org.gradle.daemon=true
org.gradle.daemon.idletimeout=3600000

# Performance
org.gradle.parallel=true
org.gradle.workers.max=8
org.gradle.caching=true
org.gradle.configuration-cache=true

# JVM
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError -XX:+UseParallelGC

# File watching (Gradle 7+)
org.gradle.vfs.watch=true

# Configuration on demand (use with caution)
# org.gradle.configureondemand=true
```

## Performance Profiling

### Build Scan
```bash
./gradlew build --scan
```
**Analysis:**
- Timeline view for task execution
- Configuration time breakdown
- Dependency resolution time
- Plugin overhead

### Profile Report
```bash
./gradlew build --profile
```
**Location:** `build/reports/profile/profile-<timestamp>.html`

### Custom Profiling
```kotlin
// build.gradle.kts
gradle.taskGraph.whenReady {
    val start = System.currentTimeMillis()
    gradle.taskGraph.afterTask { task, state ->
        val duration = System.currentTimeMillis() - start
        if (duration > 1000) {
            println("${task.path} took ${duration}ms")
        }
    }
}
```

## Optimization Strategies

### 1. Reduce Configuration Time

#### Avoid Eager Task Creation
```kotlin
// ❌ Bad: Creates all tasks during configuration
tasks.create("build1") { }
tasks.create("build2") { }

// ✅ Good: Lazy task registration
tasks.register("build1") { }
tasks.register("build2") { }
```

#### Lazy Property Access
```kotlin
// ❌ Bad: Evaluates immediately
val version = project.property("version") as String

// ✅ Good: Lazy evaluation
val version = providers.gradleProperty("version")
```

### 2. Optimize Task Execution

#### Declare Inputs/Outputs
```kotlin
abstract class CustomTask : DefaultTask() {
    @get:InputFiles
    abstract val sources: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun execute() {
        // Task avoidance and caching work automatically
    }
}
```

#### Incremental Tasks
```kotlin
abstract class IncrementalTask : DefaultTask() {
    @get:Incremental
    @get:InputFiles
    abstract val sources: ConfigurableFileCollection
    
    @TaskAction
    fun execute(inputChanges: InputChanges) {
        inputChanges.getFileChanges(sources).forEach { change ->
            when (change.changeType) {
                ChangeType.ADDED, ChangeType.MODIFIED -> process(change.file)
                ChangeType.REMOVED -> delete(change.file)
            }
        }
    }
}
```

### 3. Dependency Resolution

#### Use Version Catalogs
```toml
# gradle/libs.versions.toml
[versions]
kotlin = "1.9.21"

[libraries]
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
```

#### Dependency Constraints
```kotlin
dependencies {
    constraints {
        implementation("com.google.guava:guava") {
            version {
                strictly("[28.0-jre, 30.0-jre[")
                prefer("29.0-jre")
            }
        }
    }
}
```

### 4. Multi-Project Optimization

#### Parallel Project Execution
```kotlin
// settings.gradle.kts
rootProject.name = "my-app"

include(":app")
include(":lib1")
include(":lib2")

// Enable parallel execution for independent projects
gradle.startParameter.isParallelProjectExecutionEnabled = true
```

#### Composite Builds
```kotlin
// settings.gradle.kts
includeBuild("../shared-library")
```

### 5. Plugin Optimization

#### Use precompiled script plugins
```kotlin
// buildSrc/src/main/kotlin/my-conventions.gradle.kts
plugins {
    id("java-library")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
```

## Measuring Impact

### Baseline
```bash
./gradlew clean build --no-daemon --no-build-cache --no-configuration-cache
```

### With Optimizations
```bash
./gradlew clean build
```

### Metrics to Track
- **Configuration time:** Should be < 5% of total build time
- **Task execution time:** Should show improvement with caching
- **Clean build time:** Should improve significantly with build cache
- **Incremental build time:** Should be sub-second for small changes

## Common Bottlenecks

1. **Long configuration phase** → Use lazy task registration, configuration cache
2. **Slow dependency resolution** → Use version catalogs, dependency locking
3. **Non-cacheable tasks** → Declare inputs/outputs properly
4. **Large monorepo** → Consider composite builds, parallel execution
5. **Heavy plugins** → Profile and consider alternatives

## Anti-Patterns

- Configuration-time resolution: `configurations.compileClasspath.files`
- Eager task creation: `tasks.create()` instead of `tasks.register()`
- Direct project references in multi-project: Use capabilities/dependencies
- Mutable shared state between projects
- Heavy computation during configuration phase

## Version-Specific Features

- **Gradle 7.0+:** File system watching, configuration cache stable
- **Gradle 8.0+:** Configuration cache recommended by default
- **Gradle 8.1+:** Improved parallel execution
- **Gradle 8.5+:** Better incremental compilation
