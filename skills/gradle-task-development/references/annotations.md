# Task Input/Output Annotations

## Input Annotations

| Annotation | Type | Caching |
|------------|------|---------|
| @Input | Simple value (String, Int, Boolean) | Content-based |
| @InputFile | RegularFileProperty | Content-based |
| @InputFiles | ConfigurableFileCollection | Content-based |
| @InputDirectory | DirectoryProperty | Content-based |
| @Classpath | Files (order matters) | Content-based |
| @CompileClasspath | Files (ABI only) | ABI-based |
| @Nested | Complex object with annotations | Recursive |
| @Internal | Not tracked | Ignored |
| @Optional | Nullable input | Can be null |

## Output Annotations

| Annotation | Type |
|------------|------|
| @OutputFile | RegularFileProperty |
| @OutputFiles | Map<String, File> |
| @OutputDirectory | DirectoryProperty |
| @OutputDirectories | Map<String, File> |
| @Destroys | Files destroyed by task |
| @LocalState | Local cache state |

## Path Sensitivity

```kotlin
@get:InputFiles
@get:PathSensitive(PathSensitivity.RELATIVE)  // Most common
abstract val sources: ConfigurableFileCollection
```

| Sensitivity | Behavior |
|-------------|----------|
| ABSOLUTE | Full path matters |
| RELATIVE | Relative path matters |
| NAME_ONLY | Only filename matters |
| NONE | Only content matters |

## Normalization

```kotlin
@get:InputFiles
@get:PathSensitive(PathSensitivity.RELATIVE)
@get:NormalizeLineEndings
abstract val sources: ConfigurableFileCollection
```

| Normalizer | Effect |
|------------|--------|
| @NormalizeLineEndings | Ignore CRLF vs LF |
| @IgnoreEmptyDirectories | Ignore empty dirs |
| @Classpath | Ignore non-ABI changes |

## Example: Fully Annotated Task

```kotlin
@CacheableTask
abstract class ProcessTask : DefaultTask() {
    @get:Input
    abstract val version: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val config: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Internal  // Not for caching
    abstract val tempDir: DirectoryProperty

    @TaskAction
    fun process() { /* ... */ }
}
```
