# Task Inputs and Outputs

**Gradle Version**: 7.0+

## Overview

Proper input/output declaration enables up-to-date checking, caching, and incremental builds.

## Input Types

### @Input
Simple values (strings, numbers, booleans):
```kotlin
@get:Input
abstract val version: Property<String>

@get:Input  
abstract val debug: Property<Boolean>
```

### @InputFile
Single file input:
```kotlin
@get:InputFile
@get:PathSensitive(PathSensitivity.RELATIVE)
abstract val configFile: RegularFileProperty
```

### @InputFiles
Multiple file inputs:
```kotlin
@get:InputFiles
@get:PathSensitive(PathSensitivity.RELATIVE)
abstract val sourceFiles: ConfigurableFileCollection
```

### @InputDirectory
Directory input:
```kotlin
@get:InputDirectory
@get:PathSensitive(PathSensitivity.RELATIVE)
abstract val sourceDir: DirectoryProperty
```

### @Classpath
Classpath input (special handling):
```kotlin
@get:Classpath
abstract val classpath: ConfigurableFileCollection
```

## Output Types

### @OutputFile
Single file output:
```kotlin
@get:OutputFile
abstract val outputFile: RegularFileProperty
```

### @OutputFiles
Multiple file outputs:
```kotlin
@get:OutputFiles
abstract val outputFiles: ConfigurableFileCollection
```

### @OutputDirectory
Directory output:
```kotlin
@get:OutputDirectory
abstract val outputDir: DirectoryProperty
```

## Path Sensitivity

Controls when file changes trigger re-execution:

```kotlin
// ABSOLUTE: Full path matters
@get:PathSensitive(PathSensitivity.ABSOLUTE)

// RELATIVE: Relative path matters (most common)
@get:PathSensitive(PathSensitivity.RELATIVE)

// NAME_ONLY: Only filename matters
@get:PathSensitive(PathSensitivity.NAME_ONLY)

// NONE: Only content matters, ignore path
@get:PathSensitive(PathSensitivity.NONE)
```

## Complete Example

```kotlin
abstract class ProcessTask : DefaultTask() {
    // Simple input
    @get:Input
    abstract val version: Property<String>
    
    // File inputs
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection
    
    // Classpath
    @get:Classpath
    abstract val classpath: ConfigurableFileCollection
    
    // Outputs
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun execute() {
        val ver = version.get()
        val srcs = sources.files
        val cp = classpath.files
        val output = outputDir.asFile.get()
        
        // Process files
    }
}
```

## Best Practices

1. **Declare all inputs/outputs**
2. **Use appropriate path sensitivity**
3. **Avoid @Internal for actual inputs**
4. **Use Property API for values**
5. **Normalize line endings if needed**

## Quick Reference

```kotlin
// Inputs
@get:Input                         // Values
@get:InputFile                     // Single file
@get:InputFiles                    // Multiple files
@get:InputDirectory                // Directory
@get:Classpath                     // Classpath

// Outputs
@get:OutputFile                    // Single file
@get:OutputFiles                   // Multiple files
@get:OutputDirectory               // Directory

// Modifiers
@get:PathSensitive(RELATIVE)       // Path handling
@get:NormalizeLineEndings          // Line ending normalization
@get:Optional                      // Optional input/output
```
