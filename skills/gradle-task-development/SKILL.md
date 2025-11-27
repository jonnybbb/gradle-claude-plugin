---
name: gradle-task-development
description: This skill should be used when the user asks to "create a custom task", "write a Gradle task", "fix task up-to-date check", "make task incremental", "use Worker API", "add task inputs/outputs", or mentions @TaskAction, DefaultTask, task annotations (@Input, @OutputFile), tasks.register(), or task caching.
---

# Gradle Task Development

## Overview

Custom tasks extend Gradle's capabilities with project-specific build logic. Properly designed tasks are incremental, cacheable, and configuration-cache compatible.

For incremental tasks, see [references/incremental.md](references/incremental.md).
For input/output annotations, see [references/annotations.md](references/annotations.md).

## Quick Start

### Ad-hoc Task

```kotlin
tasks.register("hello") {
    group = "custom"
    description = "Says hello"
    
    val outputFile = layout.buildDirectory.file("hello.txt")
    outputs.file(outputFile)
    
    doLast {
        outputFile.get().asFile.writeText("Hello World!")
    }
}
```

### Typed Task

```kotlin
abstract class GreetingTask : DefaultTask() {
    @get:Input
    abstract val message: Property<String>
    
    @get:OutputFile
    abstract val outputFile: RegularFileProperty
    
    @TaskAction
    fun greet() {
        outputFile.asFile.get().writeText(message.get())
    }
}

tasks.register<GreetingTask>("greet") {
    message.set("Hello!")
    outputFile.set(layout.buildDirectory.file("greeting.txt"))
}
```

## Making Tasks Cacheable

```kotlin
@CacheableTask
abstract class ProcessTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun process() { /* ... */ }
}
```

## Service Injection

```kotlin
abstract class MyTask : DefaultTask() {
    @get:Inject abstract val fs: FileSystemOperations
    @get:Inject abstract val exec: ExecOperations
    
    @TaskAction
    fun execute() {
        fs.copy { from("src"); into("dest") }
        exec.exec { commandLine("echo", "done") }
    }
}
```

## Quick Reference

| Annotation | Purpose |
|------------|---------|
| @Input | Simple value (String, Int, etc.) |
| @InputFile | Single input file |
| @InputFiles | Multiple input files |
| @InputDirectory | Input directory |
| @OutputFile | Single output file |
| @OutputDirectory | Output directory |
| @Classpath | Classpath input |
| @Internal | Not tracked for up-to-date |

## Best Practices

1. Always use `tasks.register()` (not `create()`)
2. Use Property API for all inputs
3. Declare all inputs/outputs
4. Add `@PathSensitive(RELATIVE)` for file inputs
5. Inject services instead of using project

## JBang Tools

```bash
# Analyze task implementations in a project
jbang ${CLAUDE_PLUGIN_ROOT}/tools/task-analyzer.java /path/to/project --json
```

## Related Files

- [references/incremental.md](references/incremental.md) - Incremental processing
- [references/annotations.md](references/annotations.md) - Input/output annotations
- [references/worker-api.md](references/worker-api.md) - Parallel processing
