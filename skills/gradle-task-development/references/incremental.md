# Incremental Task Processing

Process only changed files for faster builds.

## Basic Pattern

```kotlin
abstract class IncrementalTask : DefaultTask() {
    @get:Incremental
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun execute(inputChanges: InputChanges) {
        if (!inputChanges.isIncremental) {
            // Full rebuild
            outputDir.asFile.get().deleteRecursively()
            outputDir.asFile.get().mkdirs()
            processAll()
            return
        }
        
        // Incremental processing
        inputChanges.getFileChanges(sources).forEach { change ->
            when (change.changeType) {
                ChangeType.ADDED, ChangeType.MODIFIED -> {
                    processFile(change.file)
                }
                ChangeType.REMOVED -> {
                    deleteOutput(change.file)
                }
            }
        }
    }
}
```

## When Incremental Fails

Incremental execution is skipped when:
- First run (no previous state)
- Output directory cleaned
- Non-incremental input changed
- Task implementation changed

## Multiple Incremental Inputs

```kotlin
@get:Incremental
@get:InputFiles
abstract val sources: ConfigurableFileCollection

@get:Incremental  
@get:InputFiles
abstract val resources: ConfigurableFileCollection

@TaskAction
fun execute(inputChanges: InputChanges) {
    inputChanges.getFileChanges(sources).forEach { /* ... */ }
    inputChanges.getFileChanges(resources).forEach { /* ... */ }
}
```

## Output Mapping

```kotlin
private fun getOutputFile(inputFile: File): File {
    val relativePath = inputFile.relativeTo(sourceDir.asFile.get())
    return outputDir.file(relativePath.path + ".processed").get().asFile
}
```
