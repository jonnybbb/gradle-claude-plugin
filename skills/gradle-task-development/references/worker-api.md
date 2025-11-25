# Worker API for Parallel Task Execution

## Overview

The Worker API allows splitting task work into parallel units, improving performance on multi-core machines.

## Basic Worker API

```kotlin
abstract class ProcessTask : DefaultTask() {
    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun process() {
        sources.files.forEach { file ->
            workerExecutor.noIsolation().submit(ProcessWorkAction::class) {
                inputFile.set(file)
                outputDir.set(this@ProcessTask.outputDir)
            }
        }
    }
}

abstract class ProcessWorkAction : WorkAction<ProcessWorkParameters> {
    override fun execute() {
        val input = parameters.inputFile.asFile.get()
        val output = parameters.outputDir.asFile.get()
        // Process file...
    }
}

interface ProcessWorkParameters : WorkParameters {
    val inputFile: RegularFileProperty
    val outputDir: DirectoryProperty
}
```

## Isolation Modes

| Mode | Use Case |
|------|----------|
| noIsolation() | Shared classpath, fastest |
| classLoaderIsolation() | Separate classloader |
| processIsolation() | Separate JVM process |

```kotlin
// Classloader isolation with custom classpath
workerExecutor.classLoaderIsolation {
    classpath.from(toolClasspath)
}.submit(WorkAction::class) { }

// Process isolation with JVM args
workerExecutor.processIsolation {
    forkOptions.maxHeapSize = "512m"
}.submit(WorkAction::class) { }
```

## Waiting for Workers

```kotlin
@TaskAction
fun process() {
    sources.files.forEach { file ->
        workerExecutor.noIsolation().submit(WorkAction::class) { }
    }
    // Explicit wait (usually automatic)
    workerExecutor.await()
}
```

## Best Practices

1. Use `noIsolation()` unless you need classpath separation
2. Keep work units balanced in size
3. Avoid shared mutable state
4. Use `processIsolation()` for memory-intensive work
