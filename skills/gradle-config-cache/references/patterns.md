# Configuration Cache Fix Patterns

Detailed patterns for fixing configuration cache compatibility issues.

## Task.project Access Patterns

### Accessing Project Properties

```kotlin
// ❌ Problem
tasks.register("bad") {
    doLast {
        println(project.name)
        println(project.version)
        println(project.buildDir)
    }
}

// ✅ Solution - capture during configuration
tasks.register("good") {
    val projectName = project.name
    val projectVersion = project.version
    val buildDir = project.layout.buildDirectory
    
    doLast {
        println(projectName)
        println(projectVersion)
        println(buildDir.get())
    }
}
```

### Accessing Project Methods

```kotlin
// ❌ Problem
tasks.register("bad") {
    doLast {
        project.file("output.txt").writeText("result")
        project.exec { commandLine("echo", "hello") }
    }
}

// ✅ Solution - use injected services
abstract class MyTask : DefaultTask() {
    @get:Inject abstract val fs: FileSystemOperations
    @get:Inject abstract val exec: ExecOperations
    
    @get:OutputFile
    abstract val outputFile: RegularFileProperty
    
    @TaskAction
    fun execute() {
        outputFile.asFile.get().writeText("result")
        exec.exec { commandLine("echo", "hello") }
    }
}
```

### File Operations

```kotlin
// ❌ Problem - project.copy in doLast
tasks.register("bad") {
    doLast {
        project.copy {
            from("src/resources")
            into("build/output")
        }
    }
}

// ✅ Solution - typed task with injection
abstract class CopyResourcesTask : DefaultTask() {
    @get:Inject
    abstract val fs: FileSystemOperations
    
    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty
    
    @get:OutputDirectory
    abstract val destDir: DirectoryProperty
    
    @TaskAction
    fun execute() {
        fs.copy {
            from(sourceDir)
            into(destDir)
        }
    }
}

tasks.register<CopyResourcesTask>("copyResources") {
    sourceDir.set(file("src/resources"))
    destDir.set(layout.buildDirectory.dir("output"))
}
```

## Provider API Patterns

### System Properties

```kotlin
// ❌ Problem
tasks.register("bad") {
    doLast {
        val home = System.getProperty("user.home")
        val javaVersion = System.getProperty("java.version")
    }
}

// ✅ Solution
tasks.register("good") {
    val userHome = providers.systemProperty("user.home")
    val javaVersion = providers.systemProperty("java.version")
    
    doLast {
        println("Home: ${userHome.get()}")
        println("Java: ${javaVersion.get()}")
    }
}
```

### Environment Variables

```kotlin
// ❌ Problem
tasks.register("bad") {
    doLast {
        val path = System.getenv("PATH")
        val ci = System.getenv("CI")
    }
}

// ✅ Solution
tasks.register("good") {
    val path = providers.environmentVariable("PATH")
    val ci = providers.environmentVariable("CI").orElse("false")
    
    doLast {
        println("Path: ${path.get()}")
        println("CI: ${ci.get()}")
    }
}
```

### Gradle Properties

```kotlin
// ❌ Problem
tasks.register("bad") {
    doLast {
        val version = project.property("myVersion") as String
    }
}

// ✅ Solution
tasks.register("good") {
    val version = providers.gradleProperty("myVersion")
    
    doLast {
        println("Version: ${version.get()}")
    }
}
```

## Build Service Patterns

### Shared State

```kotlin
// ❌ Problem - mutable shared state
val sharedData = mutableMapOf<String, String>()

tasks.register("task1") {
    doLast { sharedData["key"] = "value" }
}

// ✅ Solution - BuildService
abstract class SharedDataService : BuildService<BuildServiceParameters.None> {
    private val data = java.util.concurrent.ConcurrentHashMap<String, String>()
    
    fun put(key: String, value: String) { data[key] = value }
    fun get(key: String): String? = data[key]
}

val sharedService = gradle.sharedServices.registerIfAbsent(
    "sharedData", 
    SharedDataService::class
) {}

abstract class MyTask : DefaultTask() {
    @get:Internal
    abstract val dataService: Property<SharedDataService>
    
    @TaskAction
    fun execute() {
        dataService.get().put("key", "value")
    }
}
```

### Build Listeners

```kotlin
// ❌ Problem - listeners not serializable
gradle.taskGraph.whenReady {
    println("Task graph ready with ${allTasks.size} tasks")
}

// ✅ Solution - BuildService with BuildOperationListener
abstract class BuildLifecycleService : 
    BuildService<BuildServiceParameters.None>,
    org.gradle.internal.operations.BuildOperationListener {
    
    override fun started(descriptor: BuildOperationDescriptor, event: OperationStartEvent) {}
    override fun progress(identifier: OperationIdentifier, event: OperationProgressEvent) {}
    override fun finished(descriptor: BuildOperationDescriptor, event: OperationFinishEvent) {}
}
```

## Custom Task Patterns

### Typed Task Template

```kotlin
@CacheableTask
abstract class ConfigCacheCompatibleTask : DefaultTask() {
    // Injected services
    @get:Inject abstract val fs: FileSystemOperations
    @get:Inject abstract val exec: ExecOperations
    @get:Inject abstract val archive: ArchiveOperations
    
    // Inputs
    @get:Input
    abstract val config: Property<String>
    
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection
    
    // Outputs
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun execute() {
        // All project access happens via injected services
        // All values come from declared inputs
        fs.copy {
            from(sources)
            into(outputDir)
        }
    }
}
```

### Ad-hoc Task Template

```kotlin
tasks.register("configCacheSafe") {
    // Capture everything during configuration
    val projectName = project.name
    val buildDir = project.layout.buildDirectory
    val version = providers.gradleProperty("version").orElse("1.0")
    
    // Declare outputs for up-to-date checking
    outputs.file(buildDir.file("info.txt"))
    
    doLast {
        // Only use captured values
        val outputFile = buildDir.file("info.txt").get().asFile
        outputFile.writeText("Project: $projectName, Version: ${version.get()}")
    }
}
```

## File Reading Patterns

```kotlin
// ❌ Problem - reading file in doLast
tasks.register("bad") {
    doLast {
        val content = file("config.properties").readText()
    }
}

// ✅ Solution - declare as input
abstract class ReadConfigTask : DefaultTask() {
    @get:InputFile
    abstract val configFile: RegularFileProperty
    
    @get:OutputFile
    abstract val outputFile: RegularFileProperty
    
    @TaskAction
    fun execute() {
        val content = configFile.asFile.get().readText()
        outputFile.asFile.get().writeText("Processed: $content")
    }
}

tasks.register<ReadConfigTask>("readConfig") {
    configFile.set(file("config.properties"))
    outputFile.set(layout.buildDirectory.file("processed.txt"))
}
```

## Troubleshooting

### Problem Report Analysis

The HTML report at `build/reports/configuration-cache/` shows:
- Which task caused the problem
- Exact line number in build script
- Stack trace showing the call path
- Suggested fix

### Common Error Messages

| Error | Cause | Fix |
|-------|-------|-----|
| "invocation of 'Task.project'" | project access in doLast | Capture during configuration |
| "cannot serialize object" | Non-serializable field | Use Property API or @Internal |
| "accessing system property" | System.getProperty() | Use providers.systemProperty() |
| "build listener" | gradle.taskGraph.whenReady | Use BuildService |

### Debugging Tips

1. Enable verbose mode: `--info`
2. Check the HTML report
3. Look for stack trace pointing to exact line
4. Search for `project.` in doLast/doFirst blocks
