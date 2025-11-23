---
description: Create custom Gradle tasks with proper inputs, outputs, caching support, and incremental build capabilities. Generates tasks in both Kotlin and Groovy DSL.
---

# Create Gradle Task

Generate a custom Gradle task with best practices for inputs, outputs, and caching.

## What This Command Does

1. **Generates Task Class**: Creates properly annotated task class
2. **Configures Inputs/Outputs**: Sets up for up-to-date checks and caching
3. **Implements Task Logic**: Scaffolds task action with your requirements
4. **Adds Registration**: Includes task registration in build file
5. **Provides Examples**: Shows usage in both Kotlin and Groovy DSL

## Usage

```
/createTask
```

You'll be prompted for:
- **Task name**: e.g., `generateReport` or `processData`
- **Task type**: Simple, File Processing, Incremental, or Custom
- **Input files/values**: What the task consumes
- **Output files/directories**: What the task produces
- **Cacheable**: Should the task use build cache?
- **DSL preference**: Kotlin or Groovy (or both)

## Task Types

### 1. Simple Task (No Inputs/Outputs)

```kotlin
tasks.register("hello") {
    doLast {
        println("Hello, Gradle!")
    }
}
```

### 2. File Processing Task

```kotlin
@CacheableTask
abstract class ProcessFilesTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun process() {
        val output = outputDir.get().asFile
        output.mkdirs()

        inputFiles.forEach { file ->
            // Process each file
            val processed = file.readText().uppercase()
            File(output, file.name).writeText(processed)
        }
    }
}
```

### 3. Incremental Task

```kotlin
@CacheableTask
abstract class IncrementalProcessTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun processIncrementally(inputChanges: InputChanges) {
        if (inputChanges.isIncremental) {
            // Process only changed files
            inputChanges.getFileChanges(inputFiles).forEach { change ->
                when (change.changeType) {
                    ChangeType.ADDED, ChangeType.MODIFIED -> processFile(change.file)
                    ChangeType.REMOVED -> deleteOutput(change.file)
                }
            }
        } else {
            // Full rebuild
            inputFiles.forEach { processFile(it) }
        }
    }
}
```

### 4. Custom Task with Configuration

```kotlin
@CacheableTask
abstract class CustomReportTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val dataFile: RegularFileProperty

    @get:Input
    abstract val reportFormat: Property<String>

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    init {
        reportFormat.convention("html")
    }

    @TaskAction
    fun generate() {
        val data = dataFile.get().asFile.readText()
        val output = reportFile.get().asFile

        when (reportFormat.get()) {
            "html" -> generateHtml(data, output)
            "json" -> generateJson(data, output)
            else -> throw IllegalArgumentException("Unknown format")
        }
    }
}
```

## Example Session

**Prompt:**
```
What type of task? (simple/file-processing/incremental/custom): file-processing
Task name: processMarkdown
Input files pattern: src/docs/**/*.md
Output directory: build/processed-docs
Should be cacheable? (yes/no): yes
DSL preference? (kotlin/groovy/both): kotlin
```

**Generated Task Class:**
```kotlin
// buildSrc/src/main/kotlin/ProcessMarkdownTask.kt
package com.example.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.tasks.*

@CacheableTask
abstract class ProcessMarkdownTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:SkipWhenEmpty
    abstract val inputFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val options: MapProperty<String, String>

    init {
        description = "Processes Markdown files"
        group = "documentation"
    }

    @TaskAction
    fun process() {
        val output = outputDir.get().asFile
        output.mkdirs()

        logger.lifecycle("Processing ${inputFiles.files.size} Markdown files...")

        inputFiles.forEach { file ->
            logger.info("Processing: ${file.name}")

            // TODO: Implement your processing logic
            val content = file.readText()
            val processedContent = processMarkdown(content)

            val outputFile = File(output, file.name.replace(".md", ".html"))
            outputFile.writeText(processedContent)
        }

        logger.lifecycle("Processed files written to: ${output.absolutePath}")
    }

    private fun processMarkdown(content: String): String {
        // TODO: Implement Markdown processing
        // Example: Convert Markdown to HTML
        return "<html><body>$content</body></html>"
    }
}
```

**Generated Registration:**
```kotlin
// build.gradle.kts
import com.example.tasks.ProcessMarkdownTask

tasks.register<ProcessMarkdownTask>("processMarkdown") {
    inputFiles.from(fileTree("src/docs") {
        include("**/*.md")
    })
    outputDir.set(layout.buildDirectory.dir("processed-docs"))

    // Optional configuration
    options.put("encoding", "UTF-8")
    options.put("theme", "default")
}

// Make build depend on this task (optional)
tasks.named("build") {
    dependsOn("processMarkdown")
}
```

## Generated Code Includes

✅ **Proper Annotations**: `@Input`, `@InputFiles`, `@OutputDirectory`, etc.
✅ **Path Sensitivity**: `@PathSensitive(PathSensitivity.RELATIVE)` for portable caches
✅ **Cacheability**: `@CacheableTask` annotation when requested
✅ **Logging**: Appropriate logging statements
✅ **Documentation**: Description and group assignment
✅ **Error Handling**: Basic validation and error messages
✅ **Provider API**: Uses abstract properties for lazy evaluation

## Input/Output Annotations Reference

### Inputs
- `@Input` - Simple values (String, Int, Boolean)
- `@InputFile` - Single file
- `@InputFiles` - Multiple files
- `@InputDirectory` - Directory
- `@Nested` - Complex object
- `@Optional` - Optional input

### Outputs
- `@OutputFile` - Single output file
- `@OutputFiles` - Multiple output files
- `@OutputDirectory` - Output directory
- `@LocalState` - Local state (not cached)

### Modifiers
- `@PathSensitive` - How paths affect caching
- `@SkipWhenEmpty` - Skip if inputs are empty
- `@Incremental` - Support incremental processing

## Task Registration Patterns

### Basic Registration
```kotlin
tasks.register<MyTask>("myTask") {
    inputFile.set(file("input.txt"))
    outputFile.set(layout.buildDirectory.file("output.txt"))
}
```

### With Dependencies
```kotlin
tasks.register<MyTask>("myTask") {
    inputFile.set(file("input.txt"))
    outputFile.set(layout.buildDirectory.file("output.txt"))

    dependsOn("generateInput")
    mustRunAfter("validate")
}
```

### With Configuration
```kotlin
tasks.register<MyTask>("myTask") {
    inputFile.set(file("input.txt"))
    outputFile.set(layout.buildDirectory.file("output.txt"))

    // Task-specific configuration
    options.put("verbose", "true")

    // Only if necessary
    onlyIf { file("input.txt").exists() }
}
```

## Best Practices Applied

1. **Abstract Properties**: Use `abstract val` for all inputs/outputs
2. **Lazy Evaluation**: Leverage Provider API
3. **Path Sensitivity**: Use `RELATIVE` for portable caches
4. **Proper Annotations**: Enable up-to-date checks and caching
5. **Logging**: Inform users of task progress
6. **Documentation**: Description and group for discoverability
7. **Validation**: Check inputs before processing
8. **Error Messages**: Clear, actionable error messages

## Testing Your Task

```kotlin
// Test the task
./gradlew processMarkdown

// Test with cache
./gradlew clean processMarkdown --build-cache
./gradlew clean processMarkdown --build-cache  // Should load from cache

// Test incrementally
./gradlew processMarkdown  // UP-TO-DATE if no changes

// Debug task inputs
./gradlew processMarkdown --info
```

## Related

- `/createPlugin` - Create plugins containing tasks
- `/reviewTask` - Review existing task implementation
- See `gradle-task-development` skill for detailed guidance
