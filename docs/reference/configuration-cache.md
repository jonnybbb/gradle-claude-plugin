# Configuration Cache Reference

**Source:** https://docs.gradle.org/current/userguide/configuration_cache.html  
**Gradle Version:** 6.6+ (stable in 8+)

## Overview

The configuration cache improves build performance by caching the result of the configuration phase and reusing it for subsequent builds. This avoids re-executing build scripts when only task inputs have changed.

**Performance Impact:** 30-80% faster builds for incremental changes

## Compatibility Requirements

### Task Restrictions

Tasks must be serializable and avoid these patterns:

#### 1. Task.project Access During Execution

**Problem:**
```kotlin
tasks.register("bad") {
    doLast {
        println(project.name) // ❌ project access during execution
    }
}
```

**Solution:**
```kotlin
tasks.register("good") {
    val projectName = project.name // ✅ capture during configuration
    doLast {
        println(projectName)
    }
}
```

#### 2. Non-Serializable Captures

**Problem:**
```groovy
def config = new CustomObject() // ❌ non-serializable
tasks.register("bad") {
    doLast {
        println(config.value)
    }
}
```

**Solution:**
```groovy
String value = config.value // ✅ extract serializable value
tasks.register("good") {
    doLast {
        println(value)
    }
}
```

#### 3. System Property Access

**Problem:**
```kotlin
tasks.register("bad") {
    doLast {
        System.getProperty("user.home") // ❌ runtime system property
    }
}
```

**Solution:**
```kotlin
tasks.register("good") {
    val userHome = providers.systemProperty("user.home")
    doLast {
        println(userHome.get()) // ✅ use Provider API
    }
}
```

## Common Patterns

### Input/Output Declaration

```kotlin
abstract class ProcessTask : DefaultTask() {
    @get:InputFile
    abstract val inputFile: RegularFileProperty
    
    @get:OutputFile
    abstract val outputFile: RegularFileProperty
    
    @TaskAction
    fun process() {
        val input = inputFile.asFile.get()
        val output = outputFile.asFile.get()
        // process files
    }
}
```

### Using Provider API

```kotlin
// ✅ Good: Lazy evaluation
val version = providers.gradleProperty("app.version")
    .orElse("1.0.0")

tasks.register("printVersion") {
    val v = version // capture provider
    doLast {
        println("Version: ${v.get()}")
    }
}
```

### Service Injection

```kotlin
abstract class BuildTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations
    
    @get:Inject
    abstract val fileSystem: FileSystemOperations
    
    @TaskAction
    fun build() {
        execOperations.exec {
            commandLine("./gradlew", "build")
        }
    }
}
```

## Diagnostics

### Enable Configuration Cache

**gradle.properties:**
```properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn
```

**Command line:**
```bash
./gradlew build --configuration-cache
```

### Identify Problems

```bash
# Generate detailed report
./gradlew build --configuration-cache --configuration-cache-problems=warn

# Check report location
build/reports/configuration-cache/<task>/configuration-cache-report.html
```

### Problem Categories

1. **Task.project during execution** - Serialize values during configuration
2. **Build listener registration** - Use build service instead
3. **Mutable shared state** - Use `@Internal` or isolate state
4. **File collection evaluation** - Use `ConfigurableFileCollection`

## Migration Strategy

### Phase 1: Enable in Warn Mode
```properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn
```

### Phase 2: Fix High-Impact Issues
Focus on frequently executed tasks first

### Phase 3: Enable Strict Mode
```properties
org.gradle.configuration-cache.problems=fail
```

## Performance Verification

```bash
# Baseline without cache
./gradlew clean build --no-configuration-cache

# First run with cache (populates cache)
./gradlew clean build --configuration-cache

# Second run (uses cache)
./gradlew clean build --configuration-cache
```

**Expected:** 50-80% reduction in configuration time

## Version-Specific Notes

- **Gradle 6.6-7.x:** Experimental, requires opt-in
- **Gradle 8.0+:** Stable, recommended for all projects
- **Gradle 8.1+:** Improved diagnostics and error messages
- **Gradle 9.0+:** Expected to be default

## Tool Integration

JBang tool: `cache-validator.java` can automatically detect and suggest fixes for common configuration cache issues.
