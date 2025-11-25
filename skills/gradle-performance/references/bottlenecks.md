# Common Bottlenecks & Solutions

Detailed solutions for performance issues.

## Long Configuration Phase

**Problem**: Configuration takes >20% of build time

**Detection**:
```bash
./gradlew help --profile
# Check "Configuration" time in report
```

### Lazy Task Registration

```kotlin
// ❌ Bad: Eager creation
tasks.create("myTask") {
    // Configures immediately
}

// ✅ Good: Lazy registration
tasks.register("myTask") {
    // Deferred until needed
}
```

### Avoid Configuration-Time Resolution

```kotlin
// ❌ Bad: Resolves during configuration
val classpath = configurations.compileClasspath.files

// ✅ Good: Lazy resolution
val classpath = configurations.compileClasspath.map { it.files }
```

### Use Provider API

```kotlin
// ❌ Bad: Eager evaluation
val version = project.property("version") as String

// ✅ Good: Lazy evaluation
val version = providers.gradleProperty("version")
```

## Slow Task Execution

**Detection**: Use `--profile` or `--scan`

### Proper Input/Output Declaration

```kotlin
abstract class ProcessTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun process() {
        // Gradle handles up-to-date checking
    }
}
```

### Incremental Tasks

```kotlin
abstract class IncrementalTask : DefaultTask() {
    @get:Incremental
    @get:InputFiles
    abstract val sources: ConfigurableFileCollection
    
    @TaskAction
    fun execute(inputChanges: InputChanges) {
        if (inputChanges.isIncremental) {
            // Process only changed files
            inputChanges.getFileChanges(sources).forEach { change ->
                when (change.changeType) {
                    ChangeType.ADDED, ChangeType.MODIFIED -> processFile(change.file)
                    ChangeType.REMOVED -> deleteOutput(change.file)
                }
            }
        } else {
            processAllFiles()
        }
    }
}
```

## Slow Dependency Resolution

**Problem**: Resolution takes >10s

### Use Version Catalogs

```toml
# gradle/libs.versions.toml
[versions]
kotlin = "1.9.21"

[libraries]
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
```

### Enable Dependency Locking

```kotlin
dependencyLocking {
    lockAllConfigurations()
}
```

```bash
./gradlew dependencies --write-locks
```

## Multi-Project Issues

**Problem**: Not parallelizing effectively

### Proper Project Dependencies

```kotlin
// ✅ Declare explicit dependencies
dependencies {
    implementation(project(":core"))
}
```

### Avoid Cross-Project Configuration

```kotlin
// ❌ Bad: Accessing other project
val coreVersion = project(":core").version

// ✅ Good: Use shared properties
val coreVersion = providers.gradleProperty("core.version")
```

## Worker API for Parallelization

```kotlin
abstract class ParallelTask : DefaultTask() {
    @get:Inject
    abstract val workerExecutor: WorkerExecutor
    
    @TaskAction
    fun execute() {
        val workQueue = workerExecutor.noIsolation()
        
        sources.forEach { file ->
            workQueue.submit(ProcessWorker::class.java) {
                inputFile.set(file)
            }
        }
    }
}
```
