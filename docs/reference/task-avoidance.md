# Task Configuration Avoidance

**Source**: https://docs.gradle.org/current/userguide/task_configuration_avoidance.html  
**Gradle Version**: 7.0+

## Overview

Task configuration avoidance delays task creation and configuration until needed, improving build performance.

## Problem: Eager Task Creation

```kotlin
// ❌ Bad: Creates and configures task immediately
tasks.create("myTask") {
    doLast {
        println("Task executed")
    }
}
```

**Impact:** Configuration runs even if task never executes.

## Solution: Lazy Task Registration

```kotlin
// ✅ Good: Creates task only when needed
tasks.register("myTask") {
    doLast {
        println("Task executed")
    }
}
```

**Benefit:** Configuration runs only when task executes.

## Task References

### Getting Tasks

```kotlin
// ❌ Realizes task
val myTask = tasks.named("myTask").get()

// ✅ Returns Provider
val myTask = tasks.named("myTask")
```

### Task Dependencies

```kotlin
// ❌ Realizes tasks
tasks.register("deploy") {
    dependsOn(tasks.named("build").get())
}

// ✅ Use task provider
tasks.register("deploy") {
    dependsOn(tasks.named("build"))
}

// ✅ Or task name
tasks.register("deploy") {
    dependsOn("build")
}
```

## Configuration Patterns

### Configuring Existing Tasks

```kotlin
// ❌ Realizes task
tasks.named("test").get().apply {
    useJUnitPlatform()
}

// ✅ Configure without realizing
tasks.named<Test>("test") {
    useJUnitPlatform()
}
```

### Conditional Configuration

```kotlin
// ❌ Realizes all tasks
if (project.hasProperty("release")) {
    tasks.named("build").get().dependsOn("sign")
}

// ✅ Configure lazily
tasks.named("build") {
    if (project.hasProperty("release")) {
        dependsOn("sign")
    }
}
```

## Task Collections

### Iterating Tasks

```kotlin
// ❌ Realizes all tasks
tasks.forEach { task ->
    println(task.name)
}

// ✅ Use matching
tasks.matching { it.name.startsWith("test") }.configureEach {
    // Configure matching tasks
}

// ✅ Use withType
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
```

### Accessing Task Properties

```kotlin
// ❌ Realizes task
val outputDir = tasks.named("compileJava").get().destinationDirectory

// ✅ Use map
val outputDir = tasks.named<JavaCompile>("compileJava")
    .flatMap { it.destinationDirectory }
```

## Provider API

### Property Access

```kotlin
abstract class MyTask : DefaultTask() {
    @get:Input
    abstract val inputValue: Property<String>
    
    @TaskAction
    fun execute() {
        // ❌ Bad: Eager evaluation
        // val value: String = inputValue.get()
        
        // ✅ Good: Lazy evaluation
        logger.lifecycle(inputValue.get())
    }
}

// Configure
tasks.register<MyTask>("myTask") {
    // ✅ Lazy wiring
    inputValue.set(providers.gradleProperty("my.property"))
}
```

### File Properties

```kotlin
abstract class ProcessTask : DefaultTask() {
    @get:InputFiles
    abstract val inputFiles: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun execute() {
        val inputs = inputFiles.files  // Evaluated when task runs
        val output = outputDir.asFile.get()
        // Process files
    }
}

// Configure
tasks.register<ProcessTask>("process") {
    inputFiles.from(fileTree("src"))
    outputDir.set(layout.buildDirectory.dir("output"))
}
```

## Custom Task Types

```kotlin
// ✅ Good: Use abstract properties
abstract class GoodTask : DefaultTask() {
    @get:Input
    abstract val message: Property<String>
    
    @TaskAction
    fun execute() {
        println(message.get())
    }
}

// ❌ Bad: Direct fields
abstract class BadTask : DefaultTask() {
    @Input
    var message: String = ""  // Eager, not lazy
    
    @TaskAction
    fun execute() {
        println(message)
    }
}
```

## Performance Impact

### Without Avoidance

```
Configuration: 15s (configure all tasks)
Execution: 45s
Total: 60s
```

### With Avoidance

```
Configuration: 2s (configure only needed tasks)
Execution: 45s
Total: 47s (22% faster)
```

## Migration Guide

### Step 1: Replace create with register

```kotlin
// Before
tasks.create("myTask") { }

// After
tasks.register("myTask") { }
```

### Step 2: Use named() instead of getByName()

```kotlin
// Before
val task = tasks.getByName("build")

// After
val task = tasks.named("build")
```

### Step 3: Use Property API

```kotlin
// Before
var value: String = ""

// After
abstract val value: Property<String>
```

### Step 4: Lazy Dependencies

```kotlin
// Before
dependsOn(tasks.getByName("build"))

// After
dependsOn(tasks.named("build"))
```

## Best Practices

1. **Always use register()** instead of create()
2. **Use named()** instead of getByName()
3. **Use Property API** for task properties
4. **Avoid get()** on task providers
5. **Use configureEach()** for collections

## Common Pitfalls

### Realizing Tasks Accidentally

```kotlin
// ❌ Realizes all tasks
tasks.all { task ->
    println(task.name)
}

// ✅ Don't realize
tasks.names.forEach { name ->
    println(name)
}
```

### Accessing Task in Configuration

```kotlin
// ❌ Realizes task
val buildTask = tasks.named("build").get()
buildTask.group = "custom"

// ✅ Configure without realizing
tasks.named("build") {
    group = "custom"
}
```

## Related Documentation

- [Task Basics](task-basics.md): Task fundamentals
- [Performance Tuning](performance-tuning.md): Optimization
- [Configuration Cache](configuration-cache.md): Cache optimization

## Quick Reference

```kotlin
// ✅ Do
tasks.register("task") { }
tasks.named("task") { }
tasks.named<Test>("test") { }
tasks.withType<Test>().configureEach { }
abstract val prop: Property<String>
dependsOn(tasks.named("build"))

// ❌ Don't
tasks.create("task") { }
tasks.getByName("task")
tasks.named("task").get()
tasks.all { }
var prop: String = ""
dependsOn(tasks.named("build").get())
```
