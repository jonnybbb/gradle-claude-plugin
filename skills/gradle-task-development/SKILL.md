---
name: gradle-task-development
description: Creates and develops custom Gradle tasks in Groovy and Kotlin DSL with proper inputs, outputs, caching, and incremental build support. Claude uses this when you ask to create custom tasks, implement task actions, or configure task dependencies.
---

# Gradle Task Development Skill

This skill enables Claude to create custom Gradle tasks with proper configuration, caching support, and incremental build capabilities in both Groovy and Kotlin DSL.

## When Claude Uses This Skill

Claude will automatically invoke this skill when you:
- Ask to "create a custom task"
- Want to "implement a gradle task"
- Need "task input/output configuration"
- Request "incremental task" development
- Ask about "task dependencies" or "task ordering"
- Inquire about "cacheable tasks"

## Task Development Patterns

### Simple Ad-Hoc Task

**Kotlin DSL:**
```kotlin
tasks.register("hello") {
    doLast {
        println("Hello, Gradle!")
    }
}
```

**Groovy DSL:**
```groovy
tasks.register('hello') {
    doLast {
        println 'Hello, Gradle!'
    }
}
```

### Enhanced Task with Configuration

**Kotlin DSL:**
```kotlin
abstract class GreetingTask : DefaultTask() {
    @get:Input
    abstract val greeting: Property<String>

    @get:Input
    @get:Optional
    abstract val recipient: Property<String>

    init {
        greeting.convention("Hello")
        recipient.convention("World")
    }

    @TaskAction
    fun greet() {
        println("${greeting.get()}, ${recipient.get()}!")
    }
}

tasks.register<GreetingTask>("greet") {
    greeting.set("Hi")
    recipient.set("Gradle")
}
```

**Groovy DSL:**
```groovy
abstract class GreetingTask extends DefaultTask {
    @Input
    abstract Property<String> getGreeting()

    @Input
    @Optional
    abstract Property<String> getRecipient()

    GreetingTask() {
        greeting.convention("Hello")
        recipient.convention("World")
    }

    @TaskAction
    void greet() {
        println("${greeting.get()}, ${recipient.get()}!")
    }
}

tasks.register('greet', GreetingTask) {
    greeting = 'Hi'
    recipient = 'Gradle'
}
```

### File Processing Task

**Kotlin DSL:**
```kotlin
@CacheableTask
abstract class ProcessFilesTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFile: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val options: MapProperty<String, String>

    @TaskAction
    fun processFiles() {
        val input = inputFile.get().asFile
        val output = outputDir.get().asFile

        output.mkdirs()

        // Process files
        sourceFiles.forEach { file ->
            val processedContent = file.readText().uppercase()
            val outFile = File(output, file.name)
            outFile.writeText(processedContent)
        }

        logger.lifecycle("Processed ${sourceFiles.files.size} files")
    }
}

tasks.register<ProcessFilesTask>("processFiles") {
    inputFile.set(layout.projectDirectory.file("config.txt"))
    sourceFiles.from(fileTree("src/main/resources"))
    outputDir.set(layout.buildDirectory.dir("processed"))
    options.put("encoding", "UTF-8")
}
```

### Incremental Task

**Kotlin DSL:**
```kotlin
@CacheableTask
abstract class IncrementalProcessTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    abstract val inputFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun processIncrementally(inputChanges: InputChanges) {
        val outputDirectory = outputDir.get().asFile

        if (!inputChanges.isIncremental) {
            // Full rebuild
            project.delete(outputDirectory)
            outputDirectory.mkdirs()
            inputFiles.forEach { processFile(it, outputDirectory) }
        } else {
            // Incremental processing
            inputChanges.getFileChanges(inputFiles).forEach { change ->
                val targetFile = File(outputDirectory, change.file.name)

                when (change.changeType) {
                    ChangeType.ADDED, ChangeType.MODIFIED -> {
                        processFile(change.file, outputDirectory)
                    }
                    ChangeType.REMOVED -> {
                        targetFile.delete()
                    }
                }
            }
        }
    }

    private fun processFile(file: File, outputDir: File) {
        val content = file.readText().uppercase()
        File(outputDir, "${file.nameWithoutExtension}.processed").writeText(content)
    }
}
```

### Task with Dependencies

**Kotlin DSL:**
```kotlin
tasks.register<JavaCompile>("compileCustom") {
    source = fileTree("src/custom/java")
    classpath = configurations.getByName("compileClasspath")
    destinationDirectory.set(layout.buildDirectory.dir("classes/custom"))
}

tasks.register("buildCustom") {
    dependsOn("compileCustom")
    doLast {
        println("Custom build complete")
    }
}

// Task ordering without hard dependency
tasks.named("build") {
    shouldRunAfter("compileCustom")
}

// Finalize with task
tasks.named("test") {
    finalizedBy("generateTestReport")
}
```

## Task Input/Output Annotations

### Input Annotations

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Input` | Simple value input | Version number, boolean flag |
| `@InputFile` | Single file input | Configuration file |
| `@InputFiles` | Multiple files | Source files |
| `@InputDirectory` | Directory input | Resource directory |
| `@Nested` | Complex object input | Custom configuration object |
| `@Classpath` | Classpath input | Compile classpath |
| `@CompileClasspath` | Compile classpath (ignores timestamps) | Java/Kotlin compile classpath |
| `@Optional` | Optional input | May be null/absent |

### Output Annotations

| Annotation | Description | Example |
|------------|-------------|---------|
| `@OutputFile` | Single file output | Generated JAR |
| `@OutputFiles` | Multiple files | Generated sources |
| `@OutputDirectory` | Directory output | Compiled classes |
| `@OutputDirectories` | Multiple directories | Multiple output locations |
| `@LocalState` | Local state (not cached) | Temporary files |
| `@Destroys` | Destroys files | Clean task |

### Other Annotations

| Annotation | Description |
|------------|-------------|
| `@Internal` | Not part of up-to-date check |
| `@Console` | Console interaction (disables caching) |
| `@CacheableTask` | Mark task as cacheable |
| `@UntrackedTask` | Task doesn't participate in up-to-date checks |
| `@TaskAction` | Marks the action method |

## Advanced Task Features

### Using Worker API

```kotlin
@CacheableTask
abstract class ParallelProcessTask : DefaultTask() {

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun process() {
        val workQueue = workerExecutor.noIsolation()

        inputFiles.forEach { file ->
            workQueue.submit(ProcessWorkAction::class) {
                inputFile.set(file)
                outputFile.set(outputDir.file(file.name))
            }
        }
    }
}

abstract class ProcessWorkAction : WorkAction<ProcessWorkAction.Parameters> {
    interface Parameters : WorkParameters {
        val inputFile: RegularFileProperty
        val outputFile: RegularFileProperty
    }

    override fun execute() {
        val input = parameters.inputFile.get().asFile
        val output = parameters.outputFile.get().asFile
        // Process file
        output.writeText(input.readText().uppercase())
    }
}
```

### Using Build Services

```kotlin
abstract class DatabaseService : BuildService<DatabaseService.Params> {
    interface Params : BuildServiceParameters {
        val connectionString: Property<String>
    }

    fun query(sql: String): List<String> {
        // Database operations
        return listOf("result1", "result2")
    }
}

val dbServiceProvider = gradle.sharedServices.registerIfAbsent("database", DatabaseService::class) {
    parameters.connectionString.set("jdbc:...")
}

tasks.register<DatabaseQueryTask>("queryDatabase") {
    databaseService.set(dbServiceProvider)
}

abstract class DatabaseQueryTask : DefaultTask() {
    @get:Internal
    abstract val databaseService: Property<DatabaseService>

    @TaskAction
    fun query() {
        val results = databaseService.get().query("SELECT * FROM table")
        println(results)
    }
}
```

## Task Configuration Best Practices

**Lazy Configuration:**
```kotlin
// ✅ Good: Lazy task registration
tasks.register<MyTask>("myTask") {
    // Configuration
}

// ❌ Bad: Eager task creation
tasks.create("myTask", MyTask::class) {
    // Configuration
}
```

**Provider API:**
```kotlin
// ✅ Good: Using providers
val versionProvider: Provider<String> = provider { project.version.toString() }

tasks.register("printVersion") {
    val version = versionProvider
    doLast {
        println(version.get())
    }
}

// ❌ Bad: Eager evaluation
tasks.register("printVersion") {
    val version = project.version.toString()  // Evaluated at configuration time
    doLast {
        println(version)
    }
}
```

## Task Development Checklist

- [ ] Use abstract task class extending `DefaultTask`
- [ ] Annotate all inputs with appropriate annotations
- [ ] Annotate all outputs with appropriate annotations
- [ ] Use `@PathSensitive(PathSensitivity.RELATIVE)` for portability
- [ ] Add `@CacheableTask` if task should be cached
- [ ] Use Provider API for lazy evaluation
- [ ] Implement incremental processing if applicable
- [ ] Add logging with `logger.lifecycle()` or `logger.info()`
- [ ] Document task purpose and usage
- [ ] Test task with `--build-cache` and `--rerun-tasks`

## Examples for Both DSLs

### Custom Copy Task

**Kotlin DSL:**
```kotlin
@CacheableTask
abstract class CustomCopyTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val source: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val destination: DirectoryProperty

    @TaskAction
    fun copy() {
        project.copy {
            from(source)
            into(destination)
        }
    }
}

tasks.register<CustomCopyTask>("customCopy") {
    source.from("src/resources")
    destination.set(layout.buildDirectory.dir("custom-resources"))
}
```

**Groovy DSL:**
```groovy
@CacheableTask
abstract class CustomCopyTask extends DefaultTask {
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getSource()

    @OutputDirectory
    abstract DirectoryProperty getDestination()

    @TaskAction
    void copy() {
        project.copy {
            from(source)
            into(destination)
        }
    }
}

tasks.register('customCopy', CustomCopyTask) {
    source.from('src/resources')
    destination.set(layout.buildDirectory.dir('custom-resources'))
}
```

## Best Practices Summary

1. **Always use abstract properties** for task inputs/outputs
2. **Annotate properly** for up-to-date checks and caching
3. **Use lazy registration** with `tasks.register()`
4. **Leverage Provider API** for deferred evaluation
5. **Implement incremental processing** for file tasks
6. **Add appropriate logging** for visibility
7. **Document task behavior** in code comments
8. **Test cacheability** with build cache enabled
9. **Provide examples** in both Kotlin and Groovy DSL
10. **Follow Gradle best practices** from official documentation
