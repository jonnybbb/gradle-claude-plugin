# Configuration Cache Compatibility

**Source**: https://docs.gradle.org/current/userguide/configuration_cache.html  
**Gradle Version**: 7.0+, stable in 8+

## Overview

Common configuration cache compatibility issues and solutions.

## Detection

```bash
# Run with configuration cache
./gradlew build --configuration-cache

# Show problems report
./gradlew build --configuration-cache-problems=warn
```

## Common Issues

### 1. Task.project Access During Execution

**Problem:**
```kotlin
tasks.register("bad") {
    doLast {
        println(project.name)  // ❌ Fails
    }
}
```

**Solution:**
```kotlin
tasks.register("good") {
    val projectName = project.name  // ✅ Capture during configuration
    doLast {
        println(projectName)
    }
}
```

### 2. System Properties

**Problem:**
```kotlin
tasks.register("bad") {
    doLast {
        System.getProperty("user.home")  // ❌ Fails
    }
}
```

**Solution:**
```kotlin
tasks.register("good") {
    val userHome = providers.systemProperty("user.home")
    doLast {
        println(userHome.get())  // ✅ Use Provider
    }
}
```

### 3. File Resolution

**Problem:**
```kotlin
tasks.register("bad") {
    doLast {
        file("src/main/java").listFiles()  // ❌ Fails
    }
}
```

**Solution:**
```kotlin
abstract class GoodTask : DefaultTask() {
    @get:InputFiles
    abstract val sources: ConfigurableFileCollection
    
    @TaskAction
    fun execute() {
        sources.files.forEach { file ->
            // ✅ Use declared inputs
        }
    }
}
```

### 4. Mutable Shared State

**Problem:**
```kotlin
val sharedList = mutableListOf<String>()  // ❌ Not serializable

tasks.register("bad") {
    doLast {
        sharedList.add("item")
    }
}
```

**Solution:**
```kotlin
// ✅ Use build service for shared state
abstract class CacheService : BuildService<BuildServiceParameters.None> {
    private val items = ConcurrentHashMap<String, String>()
    
    fun add(item: String) {
        items[item] = item
    }
}

abstract class GoodTask : DefaultTask() {
    @get:Internal
    abstract val cache: Property<CacheService>
    
    @TaskAction
    fun execute() {
        cache.get().add("item")
    }
}
```

### 5. Build Listeners

**Problem:**
```kotlin
gradle.taskGraph.whenReady {  // ❌ Not serializable
    println("Graph ready")
}
```

**Solution:**
```kotlin
// ✅ Use build service with lifecycle
abstract class LifecycleService : BuildService<BuildServiceParameters.None>,
    BuildServiceLifecycleListener {
    
    override fun projectsLoaded() {
        println("Projects loaded")
    }
}
```

## Diagnostic Tools

### Problem Report

```bash
./gradlew build --configuration-cache --configuration-cache-problems=warn

# View report:
# build/reports/configuration-cache/<task>/configuration-cache-report.html
```

### Enable Strict Mode

```properties
# gradle.properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=fail
```

## Migration Strategy

1. **Enable with warnings:**
```properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn
```

2. **Run build:**
```bash
./gradlew build
```

3. **Fix issues from report**

4. **Enable strict mode:**
```properties
org.gradle.configuration-cache.problems=fail
```

## Task Compatibility Checklist

- [ ] No `project` access in actions
- [ ] Use Provider API for properties
- [ ] Declare all inputs/outputs
- [ ] No mutable shared state
- [ ] No build listeners in tasks
- [ ] Serializable task configuration

## Related Documentation

- [Configuration Cache](configuration-cache.md): Full reference
- [Task Basics](task-basics.md): Task development
- [Build Cache](build-cache.md): Build cache (different)

## Quick Reference

```kotlin
// ❌ Don't
doLast { println(project.name) }
doLast { System.getProperty("x") }
val shared = mutableListOf<String>()

// ✅ Do
val name = project.name
doLast { println(name) }

val prop = providers.systemProperty("x")
doLast { println(prop.get()) }

// Use build service for shared state
```
