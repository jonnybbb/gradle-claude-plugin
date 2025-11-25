# Task Development Basics

**Source**: https://docs.gradle.org/current/userguide/custom_tasks.html  
**Gradle Version**: 7.0+, optimized for 8+

## Overview

Custom tasks extend Gradle's functionality by defining new build actions. Properly implemented tasks are cacheable, incremental, and composable.

## Task Types

### 1. Simple Tasks (Ad-hoc)

**Kotlin DSL:**
```kotlin
tasks.register("hello") {
    doLast {
        println("Hello World")
    }
}
```

**Groovy DSL:**
```groovy
tasks.register('hello') {
    doLast {
        println 'Hello World'
    }
}
```

**Use Case**: One-off tasks, quick automation

### 2. Enhanced Tasks (Typed Tasks)

**Kotlin DSL:**
```kotlin
abstract class GreetingTask : DefaultTask() {
    @get:Input
    abstract val greeting: Property<String>
    
    @TaskAction
    fun greet() {
        println(greeting.get())
    }
}

tasks.register<GreetingTask>("greet") {
    greeting.set("Hello from typed task")
}
```

**Groovy DSL:**
```groovy
abstract class GreetingTask extends DefaultTask {
    @Input
    abstract Property<String> getGreeting()
    
    @TaskAction
    void greet() {
        println greeting.get()
    }
}

tasks.register('greet', GreetingTask) {
    greeting = 'Hello from typed task'
}
```

**Use Case**: Reusable tasks with configuration, proper up-to-date checking

## Task Anatomy

### Essential Components

```kotlin
abstract class ProcessFilesTask : DefaultTask() {
    // 1. Inputs - What affects the task output
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection
    
    @get:Input
    abstract val processMode: Property<String>
    
    // 2. Outputs - What the task produces
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    // 3. Services (optional) - Gradle-provided services
    @get:Inject
    abstract val execOperations: ExecOperations
    
    // 4. Task Action - The work
    @TaskAction
    fun process() {
        val sources = sourceFiles.files
        val output = outputDir.asFile.get()
        val mode = processMode.get()
        
        // Process files
        sources.forEach { file ->
            processFile(file, output, mode)
        }
    }
    
    private fun processFile(input: File, outputDir: File, mode: String) {
        // Implementation
    }
}
```

## Input/Output Annotations

### Input Annotations

```kotlin
// Simple value input
@get:Input
abstract val stringValue: Property<String>

@get:Input
abstract val numberValue: Property<Int>

// File inputs
@get:InputFile
abstract val inputFile: RegularFileProperty

@get:InputFiles
abstract val inputFiles: ConfigurableFileCollection

@get:InputDirectory
abstract val inputDir: DirectoryProperty

// Classpath (special handling)
@get:Classpath
abstract val classpath: ConfigurableFileCollection

// For inputs that don't affect outputs
@get:Internal
abstract val debugMode: Property<Boolean>
```

### Path Sensitivity

Controls when file changes trigger re-execution:

```kotlin
// Absolute path matters
@get:InputFiles
@get:PathSensitive(PathSensitivity.ABSOLUTE)
abstract val absoluteFiles: ConfigurableFileCollection

// Relative path matters (most common)
@get:InputFiles
@get:PathSensitive(PathSensitivity.RELATIVE)
abstract val relativeFiles: ConfigurableFileCollection

// Only name matters
@get:InputFiles
@get:PathSensitive(PathSensitivity.NAME_ONLY)
abstract val nameOnlyFiles: ConfigurableFileCollection

// Content only (ignore paths)
@get:InputFiles
@get:PathSensitive(PathSensitivity.NONE)
abstract val contentFiles: ConfigurableFileCollection
```

### Output Annotations

```kotlin
// Single file output
@get:OutputFile
abstract val outputFile: RegularFileProperty

// Directory output (most common)
@get:OutputDirectory
abstract val outputDir: DirectoryProperty

// Multiple outputs
@get:OutputFiles
abstract val outputFiles: ConfigurableFileCollection
```

## Task Configuration

### Lazy Configuration

**Kotlin DSL:**
```kotlin
tasks.register<ProcessFilesTask>("processFiles") {
    sourceFiles.from(fileTree("src"))
    processMode.set("release")
    outputDir.set(layout.buildDirectory.dir("processed"))
}
```

**Groovy DSL:**
```groovy
tasks.register('processFiles', ProcessFilesTask) {
    sourceFiles.from fileTree('src')
    processMode = 'release'
    outputDir = layout.buildDirectory.dir('processed')
}
```

### Task Dependencies

```kotlin
tasks.register("deploy") {
    dependsOn("build", "test")
    
    doLast {
        // Deploy logic
    }
}

// Or with task references
val build by tasks.getting
val deploy by tasks.registering {
    dependsOn(build)
}
```

### Task Ordering

```kotlin
// Must run after (if both execute)
tasks.named("test") {
    mustRunAfter("compileJava")
}

// Should run after (preferred ordering)
tasks.named("integrationTest") {
    shouldRunAfter("test")
}

// Finalized by (always runs after)
tasks.named("build") {
    finalizedBy("generateReport")
}
```

## Best Practices

### 1. Use Abstract Properties

```kotlin
// ✅ Good: Abstract property
abstract class GoodTask : DefaultTask() {
    @get:Input
    abstract val value: Property<String>
}

// ❌ Bad: Direct field
abstract class BadTask : DefaultTask() {
    @Input
    var value: String = ""  // Not lazy, not cacheable
}
```

### 2. Use Provider API

```kotlin
// ✅ Good: Lazy evaluation
tasks.register<ProcessTask>("process") {
    val projectVersion = providers.gradleProperty("version")
    version.set(projectVersion)
}

// ❌ Bad: Eager evaluation
tasks.register<ProcessTask>("process") {
    version.set(project.property("version") as String)  // Evaluated at configuration
}
```

### 3. Declare All Inputs/Outputs

```kotlin
// ✅ Good: All inputs/outputs declared
abstract class GoodTask : DefaultTask() {
    @get:InputFiles
    abstract val sources: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val output: DirectoryProperty
    
    @TaskAction
    fun execute() {
        // Task is up-to-date and cacheable
    }
}

// ❌ Bad: Undeclared inputs/outputs
abstract class BadTask : DefaultTask() {
    @TaskAction
    fun execute() {
        val files = File("src").listFiles()  // Undeclared input
        File("build/output").mkdirs()        // Undeclared output
    }
}
```

### 4. Use Gradle Services

```kotlin
abstract class ServiceTask : DefaultTask() {
    @get:Inject
    abstract val fs: FileSystemOperations
    
    @get:Inject
    abstract val exec: ExecOperations
    
    @TaskAction
    fun execute() {
        // Copy files
        fs.copy {
            from("src")
            into("dest")
        }
        
        // Execute command
        exec.exec {
            commandLine("./script.sh")
        }
    }
}
```

## Common Patterns

### File Processing

```kotlin
abstract class ProcessFilesTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun process() {
        val output = outputDir.asFile.get()
        output.mkdirs()
        
        sources.forEach { file ->
            val outFile = File(output, file.name)
            // Process file -> outFile
        }
    }
}
```

### Command Execution

```kotlin
abstract class ExecTask : DefaultTask() {
    @get:Input
    abstract val command: ListProperty<String>
    
    @get:OutputFile
    abstract val outputFile: RegularFileProperty
    
    @get:Inject
    abstract val exec: ExecOperations
    
    @TaskAction
    fun execute() {
        val output = ByteArrayOutputStream()
        
        exec.exec {
            commandLine(command.get())
            standardOutput = output
        }
        
        outputFile.asFile.get().writeBytes(output.toByteArray())
    }
}
```

### Code Generation

```kotlin
abstract class GenerateTask : DefaultTask() {
    @get:Input
    abstract val packageName: Property<String>
    
    @get:Input
    abstract val className: Property<String>
    
    @get:OutputFile
    abstract val outputFile: RegularFileProperty
    
    @TaskAction
    fun generate() {
        val pkg = packageName.get()
        val cls = className.get()
        val file = outputFile.asFile.get()
        
        file.writeText("""
            package $pkg;
            
            public class $cls {
                // Generated code
            }
        """.trimIndent())
    }
}
```

## Task Lifecycle

```
Configuration Phase:
  1. Task registered (lazy)
  2. Task configured (when needed)
  3. Dependencies resolved
  4. Task graph built

Execution Phase:
  1. Check if up-to-date (inputs unchanged)
  2. Check cache (if enabled)
  3. Execute @TaskAction (if needed)
  4. Store outputs
  5. Store in cache (if enabled)
```

## Debugging Tasks

### Enable Logging

```kotlin
abstract class DebugTask : DefaultTask() {
    @TaskAction
    fun execute() {
        logger.lifecycle("Lifecycle message")
        logger.quiet("Important message")
        logger.info("Info message")
        logger.debug("Debug message")
    }
}
```

### Run with Options

```bash
# Show task inputs/outputs
./gradlew myTask --info

# Show why task executed
./gradlew myTask --info | grep "up-to-date"

# Rerun regardless of up-to-date
./gradlew myTask --rerun-tasks

# Show task execution time
./gradlew myTask --profile
```

## Task Testing

### Unit Testing Tasks

```kotlin
// Test setup
val project = ProjectBuilder.builder().build()

@Test
fun testTaskExecution() {
    val task = project.tasks.register("test", MyTask::class.java) {
        inputFile.set(File("input.txt"))
        outputFile.set(File("output.txt"))
    }.get()
    
    task.execute()
    
    assertTrue(task.outputFile.asFile.get().exists())
}
```

### Functional Testing

```kotlin
@Test
fun testTaskInRealProject() {
    val projectDir = File("test-project")
    val result = GradleRunner.create()
        .withProjectDir(projectDir)
        .withArguments("myTask")
        .build()
    
    assertEquals(TaskOutcome.SUCCESS, result.task(":myTask")?.outcome)
}
```

## Common Issues

### Issue: Task Always Runs

**Cause**: Missing or incorrect input/output declarations

**Solution**: Declare all inputs and outputs properly

```kotlin
// Add missing declarations
@get:InputFiles
abstract val missingInput: ConfigurableFileCollection

@get:OutputDirectory
abstract val missingOutput: DirectoryProperty
```

### Issue: Task Not Cacheable

**Cause**: Non-serializable inputs, missing annotations

**Solution**: Use primitive types, Property API, proper annotations

```kotlin
// ✅ Cacheable
@get:Input
abstract val value: Property<String>

// ❌ Not cacheable
@Input
var value: CustomObject = CustomObject()
```

### Issue: Configuration Cache Incompatible

**Cause**: Task.project access during execution, non-serializable state

**Solution**: Capture values during configuration

```kotlin
// ✅ Good: Capture during configuration
tasks.register("task") {
    val projectName = project.name  // Captured
    doLast {
        println(projectName)  // Use captured value
    }
}

// ❌ Bad: Access during execution
tasks.register("task") {
    doLast {
        println(project.name)  // Direct access fails
    }
}
```

## Version-Specific Notes

### Gradle 7.x
- Property API stable
- Configuration cache experimental
- Abstract properties recommended

### Gradle 8.x
- Configuration cache stable
- Enhanced up-to-date checking
- Better task avoidance

### Gradle 9.x (upcoming)
- Configuration cache default
- Improved caching algorithms
- Enhanced incremental support

## Related Documentation

- [Incremental Tasks](incremental-tasks.md): Handling file changes incrementally
- [Task Inputs/Outputs](task-inputs-outputs.md): Detailed annotation reference
- [Task Avoidance](task-avoidance.md): Lazy configuration patterns
- [Configuration Cache](configuration-cache.md): Cache compatibility
- [Plugin Basics](plugin-basics.md): Packaging tasks in plugins

## Quick Reference

```kotlin
// Complete task example
abstract class CompleteTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection
    
    @get:Input
    abstract val mode: Property<String>
    
    @get:OutputDirectory
    abstract val output: DirectoryProperty
    
    @get:Inject
    abstract val fs: FileSystemOperations
    
    @TaskAction
    fun execute() {
        logger.lifecycle("Processing in ${mode.get()} mode")
        fs.copy {
            from(sources)
            into(output)
        }
    }
}

// Registration
tasks.register<CompleteTask>("complete") {
    sources.from(fileTree("src"))
    mode.set("release")
    output.set(layout.buildDirectory.dir("output"))
}
```
