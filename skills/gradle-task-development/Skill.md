---
name: gradle-task-development
description: Creates custom Gradle tasks with proper inputs, outputs, caching, and incremental build support in Kotlin and Groovy DSL.
version: 1.0.0
---

# Gradle Task Development

Create custom Gradle tasks following best practices.

## When to Use

Invoke when users want to:
- Create custom tasks
- Implement task actions
- Configure task inputs/outputs
- Add caching support
- Implement incremental tasks

## Basic Task Structure

```kotlin
@CacheableTask
abstract class MyTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun execute() {
        // Task implementation
    }
}
```

## Task Registration

```kotlin
tasks.register<MyTask>("myTask") {
    inputFile.set(file("input.txt"))
    outputFile.set(layout.buildDirectory.file("output.txt"))
}
```

## Annotations Reference

**Inputs:**
- `@Input` - Simple values
- `@InputFile` - Single file
- `@InputFiles` - Multiple files
- `@InputDirectory` - Directory

**Outputs:**
- `@OutputFile` - Single file
- `@OutputDirectory` - Directory
- `@Internal` - Not tracked

## Best Practices

1. Use abstract properties with Provider API
2. Annotate all inputs and outputs
3. Use `@PathSensitive(PathSensitivity.RELATIVE)`
4. Add `@CacheableTask` for cacheable tasks
5. Use lazy registration with `tasks.register()`
