# Parallel Execution

**Gradle Version**: 7.0+

## Overview

Parallel execution runs independent tasks concurrently, reducing build time.

## Enable Parallel Execution

**gradle.properties:**
```properties
org.gradle.parallel=true
org.gradle.workers.max=8
```

## How It Works

Gradle analyzes task dependencies and executes independent tasks in parallel:

```
compileJava (Project A) ║ compileJava (Project B)
        ↓               ║         ↓
   processResources     ║    processResources
        ↓               ║         ↓
      classes           ║       classes
        ↘               ↓         ↙
                      jar
```

## Configuration

### Worker Count

```properties
# Auto-detect (default)
org.gradle.workers.max=auto

# Specific count
org.gradle.workers.max=4

# Max available
org.gradle.workers.max=max
```

### Per-Project Parallelism

```kotlin
// build.gradle.kts
gradle.startParameter.maxWorkerCount = 4
```

## Worker API

For parallel work within tasks:

```kotlin
abstract class ParallelTask : DefaultTask() {
    @get:Inject
    abstract val workerExecutor: WorkerExecutor
    
    @TaskAction
    fun execute() {
        val workQueue = workerExecutor.noIsolation()
        
        files.forEach { file ->
            workQueue.submit(ProcessWorker::class.java) {
                inputFile.set(file)
            }
        }
    }
}

abstract class ProcessWorker : WorkAction<Parameters> {
    interface Parameters : WorkParameters {
        val inputFile: RegularFileProperty
    }
    
    override fun execute() {
        // Process file in parallel
    }
}
```

## Limitations

- Tasks with shared state may conflict
- I/O-bound tasks benefit less
- Small projects see minimal improvement

## Performance Impact

**Single-threaded:**
- Clean build: 120s

**Parallel (4 workers):**
- Clean build: 45s (62% faster)

## Best Practices

1. **Enable in gradle.properties**
2. **Avoid shared mutable state**
3. **Use Worker API for task parallelism**
4. **Monitor with --scan**
5. **Adjust worker count based on CPU**

## Quick Reference

```properties
# gradle.properties
org.gradle.parallel=true
org.gradle.workers.max=8
org.gradle.caching=true
```
