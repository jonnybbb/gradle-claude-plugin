# Incremental Task Development

**Source**: https://docs.gradle.org/current/userguide/custom_tasks.html#incremental_tasks  
**Gradle Version**: 7.0+, optimized for 8+

## Overview

Incremental tasks process only changed files rather than all inputs. This dramatically improves build performance for large file sets.

**Performance Impact**: 50-95% faster execution when few files change

## Basic Incremental Task

**Kotlin DSL:**
```kotlin
abstract class IncrementalReverseTask : DefaultTask() {
    @get:Incremental
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val inputFiles: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun execute(inputChanges: InputChanges) {
        if (!inputChanges.isIncremental) {
            // Full rebuild
            logger.lifecycle("Non-incremental build: processing all files")
            project.delete(outputDir)
            outputDir.asFile.get().mkdirs()
        }
        
        inputChanges.getFileChanges(inputFiles).forEach { change ->
            val targetFile = outputDir.file(change.file.name).get().asFile
            
            when (change.changeType) {
                ChangeType.ADDED, ChangeType.MODIFIED -> {
                    logger.lifecycle("${change.changeType}: ${change.file.name}")
                    processFile(change.file, targetFile)
                }
                ChangeType.REMOVED -> {
                    logger.lifecycle("REMOVED: ${change.file.name}")
                    targetFile.delete()
                }
            }
        }
    }
    
    private fun processFile(input: File, output: File) {
        output.writeText(input.readText().reversed())
    }
}
```

**Groovy DSL:**
```groovy
abstract class IncrementalReverseTask extends DefaultTask {
    @Incremental
    @InputFiles
    @PathSensitive(PathSensitivity.NAME_ONLY)
    abstract ConfigurableFileCollection getInputFiles()
    
    @OutputDirectory
    abstract DirectoryProperty getOutputDir()
    
    @TaskAction
    void execute(InputChanges inputChanges) {
        if (!inputChanges.incremental) {
            project.delete(outputDir)
            outputDir.asFile.get().mkdirs()
        }
        
        inputChanges.getFileChanges(inputFiles).each { change ->
            def targetFile = outputDir.file(change.file.name).get().asFile
            
            switch (change.changeType) {
                case ChangeType.ADDED:
                case ChangeType.MODIFIED:
                    processFile(change.file, targetFile)
                    break
                case ChangeType.REMOVED:
                    targetFile.delete()
                    break
            }
        }
    }
    
    private void processFile(File input, File output) {
        output.text = input.text.reverse()
    }
}
```

## Key Components

### 1. @Incremental Annotation

Marks inputs that support incremental processing:

```kotlin
@get:Incremental
@get:InputFiles
abstract val sources: ConfigurableFileCollection
```

### 2. InputChanges Parameter

Provides information about changed files:

```kotlin
@TaskAction
fun execute(inputChanges: InputChanges) {
    // Check if incremental
    if (inputChanges.isIncremental) {
        // Process only changes
    } else {
        // Full rebuild
    }
}
```

### 3. Change Types

```kotlin
when (change.changeType) {
    ChangeType.ADDED -> {
        // File was added
    }
    ChangeType.MODIFIED -> {
        // File was modified
    }
    ChangeType.REMOVED -> {
        // File was removed
    }
}
```

## When Incremental Fails

Gradle falls back to full rebuild when:

1. **No previous execution** - First run
2. **Output changes** - Output directory modified externally
3. **Task configuration changes** - Task inputs/outputs changed
4. **Forced rebuild** - `--rerun-tasks` flag used
5. **Cache miss** - Build cache enabled but no hit

**Always handle non-incremental case:**

```kotlin
@TaskAction
fun execute(inputChanges: InputChanges) {
    if (!inputChanges.isIncremental) {
        // Clean outputs
        project.delete(outputDir)
        outputDir.asFile.get().mkdirs()
        
        // Process all files
        inputFiles.forEach { file ->
            processFile(file)
        }
        return
    }
    
    // Handle incremental changes
    inputChanges.getFileChanges(inputFiles).forEach { change ->
        // Process change
    }
}
```

## Advanced Patterns

### Multiple Incremental Inputs

```kotlin
abstract class MultiInputTask : DefaultTask() {
    @get:Incremental
    @get:InputFiles
    abstract val sourceFiles: ConfigurableFileCollection
    
    @get:Incremental
    @get:InputFiles
    abstract val resourceFiles: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun execute(inputChanges: InputChanges) {
        // Process source changes
        inputChanges.getFileChanges(sourceFiles).forEach { change ->
            processSourceChange(change)
        }
        
        // Process resource changes
        inputChanges.getFileChanges(resourceFiles).forEach { change ->
            processResourceChange(change)
        }
    }
}
```

### Derived Outputs

When output files have different names than inputs:

```kotlin
abstract class TranspileTask : DefaultTask() {
    @get:Incremental
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun execute(inputChanges: InputChanges) {
        if (!inputChanges.isIncremental) {
            project.delete(outputDir)
        }
        
        inputChanges.getFileChanges(sourceFiles).forEach { change ->
            // Derive output name: .ts -> .js
            val outputFile = outputDir.file(
                change.normalizedPath.replace(".ts", ".js")
            ).get().asFile
            
            when (change.changeType) {
                ChangeType.ADDED, ChangeType.MODIFIED -> {
                    transpile(change.file, outputFile)
                }
                ChangeType.REMOVED -> {
                    outputFile.delete()
                }
            }
        }
    }
}
```

### Incremental with Dependencies

When output files depend on multiple inputs:

```kotlin
abstract class CompileTask : DefaultTask() {
    @get:Incremental
    @get:InputFiles
    abstract val sources: ConfigurableFileCollection
    
    @get:InputFiles  // Not incremental - all files needed for compilation
    @get:Classpath
    abstract val classpath: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun execute(inputChanges: InputChanges) {
        if (!inputChanges.isIncremental) {
            // Full recompilation
            compileAll()
            return
        }
        
        // Incremental compilation
        val changedFiles = mutableSetOf<File>()
        inputChanges.getFileChanges(sources).forEach { change ->
            when (change.changeType) {
                ChangeType.ADDED, ChangeType.MODIFIED -> {
                    changedFiles.add(change.file)
                }
                ChangeType.REMOVED -> {
                    deleteOutput(change.file)
                }
            }
        }
        
        // Compile changed files with dependencies
        compile(changedFiles, classpath.files)
    }
}
```

## Stale Outputs

Handle outputs that become stale when inputs are removed:

```kotlin
abstract class ProcessTask : DefaultTask() {
    @get:Incremental
    @get:InputFiles
    abstract val inputs: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun execute(inputChanges: InputChanges) {
        val processedFiles = mutableSetOf<String>()
        
        inputChanges.getFileChanges(inputs).forEach { change ->
            when (change.changeType) {
                ChangeType.ADDED, ChangeType.MODIFIED -> {
                    val outputFile = getOutputFile(change.file)
                    processFile(change.file, outputFile)
                    processedFiles.add(outputFile.name)
                }
                ChangeType.REMOVED -> {
                    getOutputFile(change.file).delete()
                }
            }
        }
        
        // Clean stale outputs (outputs without corresponding inputs)
        if (inputChanges.isIncremental) {
            outputDir.asFile.get().listFiles()?.forEach { file ->
                if (file.name !in processedFiles && !hasCorrespondingInput(file)) {
                    logger.lifecycle("Removing stale output: ${file.name}")
                    file.delete()
                }
            }
        }
    }
}
```

## Incremental with Classpath

Special handling for classpath inputs:

```kotlin
abstract class ClasspathTask : DefaultTask() {
    @get:Incremental
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection
    
    @get:Classpath  // Changes trigger full rebuild
    abstract val classpath: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun execute(inputChanges: InputChanges) {
        // Classpath changes force full rebuild
        // Only source changes can be incremental
        
        if (!inputChanges.isIncremental) {
            logger.lifecycle("Full rebuild (classpath or outputs changed)")
            processAll()
            return
        }
        
        logger.lifecycle("Incremental build")
        inputChanges.getFileChanges(sources).forEach { change ->
            processChange(change)
        }
    }
}
```

## Performance Optimization

### Batching Changes

Process multiple changes together:

```kotlin
@TaskAction
fun execute(inputChanges: InputChanges) {
    val changes = inputChanges.getFileChanges(sources).toList()
    
    // Batch by type
    val added = changes.filter { it.changeType == ChangeType.ADDED }
    val modified = changes.filter { it.changeType == ChangeType.MODIFIED }
    val removed = changes.filter { it.changeType == ChangeType.REMOVED }
    
    // Process in batches
    processAdded(added.map { it.file })
    processModified(modified.map { it.file })
    processRemoved(removed.map { it.file })
}
```

### Parallel Processing

Use Worker API for parallel incremental processing:

```kotlin
abstract class ParallelIncrementalTask : DefaultTask() {
    @get:Incremental
    @get:InputFiles
    abstract val sources: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @get:Inject
    abstract val workerExecutor: WorkerExecutor
    
    @TaskAction
    fun execute(inputChanges: InputChanges) {
        val workQueue = workerExecutor.noIsolation()
        
        inputChanges.getFileChanges(sources).forEach { change ->
            when (change.changeType) {
                ChangeType.ADDED, ChangeType.MODIFIED -> {
                    workQueue.submit(ProcessWorker::class.java) {
                        inputFile.set(change.file)
                        outputDir.set(this@ParallelIncrementalTask.outputDir)
                    }
                }
                ChangeType.REMOVED -> {
                    getOutputFile(change.file).delete()
                }
            }
        }
    }
}
```

## Testing Incremental Tasks

### Test Full Build

```kotlin
@Test
fun testFullBuild() {
    val result = GradleRunner.create()
        .withProjectDir(projectDir)
        .withArguments("processFiles")
        .build()
    
    assertEquals(TaskOutcome.SUCCESS, result.task(":processFiles")?.outcome)
    // Verify all outputs created
}
```

### Test Incremental Build

```kotlin
@Test
fun testIncrementalBuild() {
    // First build
    GradleRunner.create()
        .withProjectDir(projectDir)
        .withArguments("processFiles")
        .build()
    
    // Modify one file
    File(projectDir, "src/file1.txt").appendText("\nmodified")
    
    // Second build (incremental)
    val result = GradleRunner.create()
        .withProjectDir(projectDir)
        .withArguments("processFiles", "--info")
        .build()
    
    // Verify incremental execution
    assertTrue(result.output.contains("Incremental build"))
}
```

### Test File Removal

```kotlin
@Test
fun testFileRemoval() {
    // First build
    GradleRunner.create()
        .withProjectDir(projectDir)
        .withArguments("processFiles")
        .build()
    
    val outputFile = File(projectDir, "build/output/file1.txt")
    assertTrue(outputFile.exists())
    
    // Remove input
    File(projectDir, "src/file1.txt").delete()
    
    // Second build
    GradleRunner.create()
        .withProjectDir(projectDir)
        .withArguments("processFiles")
        .build()
    
    // Verify output removed
    assertFalse(outputFile.exists())
}
```

## Common Patterns

### Source → Binary Compilation

```kotlin
abstract class CompileTask : DefaultTask() {
    @get:Incremental
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val classesDir: DirectoryProperty
    
    @TaskAction
    fun execute(inputChanges: InputChanges) {
        if (!inputChanges.isIncremental) {
            compileAll()
            return
        }
        
        inputChanges.getFileChanges(sourceFiles).forEach { change ->
            val sourcePath = change.normalizedPath
            val classPath = sourcePath.replace(".java", ".class")
            val classFile = classesDir.file(classPath).get().asFile
            
            when (change.changeType) {
                ChangeType.ADDED, ChangeType.MODIFIED -> {
                    compile(change.file, classFile)
                }
                ChangeType.REMOVED -> {
                    classFile.delete()
                }
            }
        }
    }
}
```

### Asset Processing

```kotlin
abstract class OptimizeImagesTask : DefaultTask() {
    @get:Incremental
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val images: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val optimizedDir: DirectoryProperty
    
    @TaskAction
    fun execute(inputChanges: InputChanges) {
        if (!inputChanges.isIncremental) {
            project.delete(optimizedDir)
        }
        
        inputChanges.getFileChanges(images).forEach { change ->
            val outputFile = optimizedDir.file(change.normalizedPath).get().asFile
            
            when (change.changeType) {
                ChangeType.ADDED, ChangeType.MODIFIED -> {
                    outputFile.parentFile.mkdirs()
                    optimizeImage(change.file, outputFile)
                }
                ChangeType.REMOVED -> {
                    outputFile.delete()
                }
            }
        }
    }
}
```

## Best Practices

### 1. Always Handle Non-Incremental

```kotlin
// ✅ Good
if (!inputChanges.isIncremental) {
    // Full rebuild logic
    return
}
// Incremental logic

// ❌ Bad
// No non-incremental handling - fails on first run
```

### 2. Clean Outputs on Full Rebuild

```kotlin
// ✅ Good
if (!inputChanges.isIncremental) {
    project.delete(outputDir)
    outputDir.asFile.get().mkdirs()
}

// ❌ Bad
// Old outputs remain, causing stale file issues
```

### 3. Use Appropriate Path Sensitivity

```kotlin
// For relative imports
@get:PathSensitive(PathSensitivity.RELATIVE)

// For absolute paths
@get:PathSensitive(PathSensitivity.ABSOLUTE)

// For filename-only
@get:PathSensitive(PathSensitivity.NAME_ONLY)
```

### 4. Handle Derived Outputs

```kotlin
// Map input changes to correct output files
val outputFile = outputDir.file(
    change.normalizedPath.replace(sourceExt, targetExt)
).get().asFile
```

## Debugging

### Log Changes

```kotlin
inputChanges.getFileChanges(sources).forEach { change ->
    logger.lifecycle("${change.changeType}: ${change.file.name}")
}
```

### Verify Incrementality

```bash
# Run twice, check second run
./gradlew task --info | grep -i incremental

# Should see: "Incremental change detection enabled"
```

### Check Why Non-Incremental

```bash
./gradlew task --info

# Look for:
# - "Task has not declared any outputs"
# - "Output property ... has been removed"
# - "Task output caching is disabled"
```

## Limitations

1. **No cross-file dependencies**: If files depend on each other, changes may require full rebuild
2. **Complex transformations**: Some transformations can't be done incrementally
3. **Stateful processing**: Processing that requires global state may need full rebuild

## Version-Specific Notes

### Gradle 7.x
- Incremental tasks stable
- InputChanges API mature
- Good performance

### Gradle 8.x
- Improved change detection
- Better stale output handling
- Enhanced logging

### Gradle 9.x (upcoming)
- Further optimizations
- Reduced overhead
- Better diagnostics

## Related Documentation

- [Task Basics](task-basics.md): Task fundamentals
- [Task Inputs/Outputs](task-inputs-outputs.md): Input/output annotations
- [Configuration Cache](configuration-cache.md): Cache compatibility
- [Performance Tuning](performance-tuning.md): Build optimization

## Quick Reference

```kotlin
// Complete incremental task template
abstract class IncrementalTask : DefaultTask() {
    @get:Incremental
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputs: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val outputs: DirectoryProperty
    
    @TaskAction
    fun execute(inputChanges: InputChanges) {
        // Handle full rebuild
        if (!inputChanges.isIncremental) {
            project.delete(outputs)
            outputs.asFile.get().mkdirs()
            processAll()
            return
        }
        
        // Handle incremental changes
        inputChanges.getFileChanges(inputs).forEach { change ->
            val output = getOutputFile(change)
            when (change.changeType) {
                ChangeType.ADDED, ChangeType.MODIFIED -> process(change.file, output)
                ChangeType.REMOVED -> output.delete()
            }
        }
    }
}
```
