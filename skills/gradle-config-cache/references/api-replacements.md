# Project API Replacements for Configuration Cache

When tasks execute, they must not use the `Project` object. This reference provides alternatives for common `Project` methods.

## Quick Reference Table

| Instead of | Use |
|------------|-----|
| `project.copy { ... }` | `@Inject FileSystemOperations` |
| `project.delete { ... }` | `@Inject FileSystemOperations` |
| `project.sync { ... }` | `@Inject FileSystemOperations` |
| `project.mkdir(path)` | `@Inject FileSystemOperations` |
| `project.file(path)` | Task input/output property or script variable with `layout.projectDirectory.file(path)` |
| `project.files(paths)` | Task input/output property or script variable with `project.files(paths)` |
| `project.fileTree(dir)` | Task input/output property or script variable with `project.fileTree(dir)` |
| `project.relativePath(file)` | Task input/output property or script variable with `layout.projectDirectory.asFile.toPath().relativize(file.toPath())` |
| `project.uri(path)` | Task input/output property or script variable with `layout.projectDirectory.file(path).asFile.toURI()` |
| `project.exec { ... }` | `@Inject ExecOperations` |
| `project.javaexec { ... }` | `@Inject ExecOperations` |
| `project.logger` | `@Inject LoggingManager` or `Task.logger` |
| `project.provider { ... }` | `@Inject ProviderFactory` |
| `project.providers` | `@Inject ProviderFactory` |
| `project.layout` | `@Inject ProjectLayout` |
| `project.objects` | `@Inject ObjectFactory` |
| `project.resources` | `@Inject TextResourceFactory` |
| `project.name` | Task input/output property or script variable with `project.name` |
| `project.version` | Task input/output property or script variable with `project.version` |
| `project.group` | Task input/output property or script variable with `project.group` |
| `project.path` | Task input/output property or script variable with `project.path` |
| `project.description` | Task input/output property or script variable with `project.description` |
| `project.buildDir` | Task input/output property or script variable with `layout.buildDirectory` |
| `project.projectDir` | Task input/output property or script variable with `layout.projectDirectory` |
| `project.rootDir` | Task input/output property or script variable with `layout.rootDirectory` |
| `project.rootProject` | Task input/output property or script variable to capture needed values |
| `project.parent` | Task input/output property or script variable to capture needed values |
| `project.findProperty(name)` | Task input/output property or script variable with `providers.gradleProperty(name)` |
| `project.hasProperty(name)` | Task input/output property or script variable with `providers.gradleProperty(name).isPresent` |
| `project.property(name)` | Task input/output property or script variable with `providers.gradleProperty(name)` |
| `project.configurations` | Task input/output property with resolved `FileCollection` |
| `project.dependencies` | Not supported; declare dependencies in build script |
| `project.repositories` | Not supported; declare repositories in build script |
| `project.extensions` | Not supported; access during configuration time |
| `project.convention` | Not supported; access during configuration time |
| `project.gradle` | Not supported; use injected services |
| Kotlin/Groovy/Java APIs | Standard language APIs are available |

## Detailed Examples

### FileSystemOperations Service

Replace `project.copy`, `project.delete`, `project.sync`, `project.mkdir`:

**Kotlin DSL:**
```kotlin
abstract class MyTask : DefaultTask() {
    @get:Inject
    abstract val fs: FileSystemOperations
    
    @TaskAction
    fun action() {
        // Copy files
        fs.copy {
            from("source")
            into("destination")
        }
        
        // Delete files
        fs.delete {
            delete("obsolete")
        }
        
        // Sync directories
        fs.sync {
            from("source")
            into("destination")
        }
    }
}
```

**Groovy DSL:**
```groovy
abstract class MyTask extends DefaultTask {
    @Inject
    abstract FileSystemOperations getFs()
    
    @TaskAction
    void action() {
        // Copy files
        fs.copy {
            from 'source'
            into 'destination'
        }
        
        // Delete files
        fs.delete {
            delete 'obsolete'
        }
        
        // Sync directories
        fs.sync {
            from 'source'
            into 'destination'
        }
    }
}
```

### ExecOperations Service

Replace `project.exec` and `project.javaexec`:

**Kotlin DSL:**
```kotlin
abstract class MyTask : DefaultTask() {
    @get:Inject
    abstract val exec: ExecOperations
    
    @TaskAction
    fun action() {
        // Execute command
        exec.exec {
            commandLine("git", "status")
        }
        
        // Execute Java
        exec.javaexec {
            mainClass.set("com.example.Main")
            classpath(configurations.runtimeClasspath)
        }
    }
}
```

**Groovy DSL:**
```groovy
abstract class MyTask extends DefaultTask {
    @Inject
    abstract ExecOperations getExec()
    
    @TaskAction
    void action() {
        // Execute command
        exec.exec {
            commandLine 'git', 'status'
        }
        
        // Execute Java
        exec.javaexec {
            mainClass = 'com.example.Main'
            classpath configurations.runtimeClasspath
        }
    }
}
```

### ProjectLayout Service

Replace `project.buildDir`, `project.projectDir`, `project.file()`:

**Kotlin DSL:**
```kotlin
abstract class MyTask : DefaultTask() {
    @get:Inject
    abstract val layout: ProjectLayout
    
    @get:InputFile
    abstract val inputFile: RegularFileProperty
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun action() {
        val projectDir = layout.projectDirectory.asFile
        val buildDir = layout.buildDirectory.asFile.get()
        
        // Access files
        val file = inputFile.asFile.get()
        val output = outputDir.asFile.get()
    }
}

tasks.register<MyTask>("myTask") {
    inputFile.set(layout.projectDirectory.file("input.txt"))
    outputDir.set(layout.buildDirectory.dir("output"))
}
```

**Groovy DSL:**
```groovy
abstract class MyTask extends DefaultTask {
    @Inject
    abstract ProjectLayout getLayout()
    
    @InputFile
    abstract RegularFileProperty getInputFile()
    
    @OutputDirectory
    abstract DirectoryProperty getOutputDir()
    
    @TaskAction
    void action() {
        def projectDir = layout.projectDirectory.asFile
        def buildDir = layout.buildDirectory.asFile.get()
        
        // Access files
        def file = inputFile.asFile.get()
        def output = outputDir.asFile.get()
    }
}

tasks.register('myTask', MyTask) {
    inputFile.set(layout.projectDirectory.file('input.txt'))
    outputDir.set(layout.buildDirectory.dir('output'))
}
```

### ProviderFactory Service

Replace `project.provider`, `project.providers`:

**Kotlin DSL:**
```kotlin
abstract class MyTask : DefaultTask() {
    @get:Inject
    abstract val providers: ProviderFactory
    
    @get:Input
    abstract val systemProp: Property<String>
    
    @get:Input
    abstract val envVar: Property<String>
    
    @TaskAction
    fun action() {
        println("System property: ${systemProp.get()}")
        println("Environment variable: ${envVar.get()}")
    }
}

tasks.register<MyTask>("myTask") {
    systemProp.set(providers.systemProperty("user.home"))
    envVar.set(providers.environmentVariable("PATH"))
}
```

**Groovy DSL:**
```groovy
abstract class MyTask extends DefaultTask {
    @Inject
    abstract ProviderFactory getProviders()
    
    @Input
    abstract Property<String> getSystemProp()
    
    @Input
    abstract Property<String> getEnvVar()
    
    @TaskAction
    void action() {
        println("System property: ${systemProp.get()}")
        println("Environment variable: ${envVar.get()}")
    }
}

tasks.register('myTask', MyTask) {
    systemProp.set(providers.systemProperty('user.home'))
    envVar.set(providers.environmentVariable('PATH'))
}
```

### ObjectFactory Service

Replace `project.objects`:

**Kotlin DSL:**
```kotlin
abstract class MyTask : DefaultTask() {
    @get:Inject
    abstract val objects: ObjectFactory
    
    @TaskAction
    fun action() {
        // Create properties
        val prop = objects.property(String::class.java)
        prop.set("value")
        
        // Create file collections
        val files = objects.fileCollection()
        files.from("file1", "file2")
        
        // Create named domain objects
        val container = objects.domainObjectContainer(MyType::class.java)
    }
}
```

**Groovy DSL:**
```groovy
abstract class MyTask extends DefaultTask {
    @Inject
    abstract ObjectFactory getObjects()
    
    @TaskAction
    void action() {
        // Create properties
        def prop = objects.property(String)
        prop.set('value')
        
        // Create file collections
        def files = objects.fileCollection()
        files.from('file1', 'file2')
        
        // Create named domain objects
        def container = objects.domainObjectContainer(MyType)
    }
}
```

### LoggingManager Service

Replace `project.logger`:

**Kotlin DSL:**
```kotlin
abstract class MyTask : DefaultTask() {
    // Option 1: Use Task.logger (no injection needed)
    @TaskAction
    fun action() {
        logger.info("Using task logger")
    }
}

// Option 2: Inject LoggingManager
abstract class MyTask2 : DefaultTask() {
    @get:Inject
    abstract val logging: LoggingManager
    
    @TaskAction
    fun action() {
        logging.captureStandardOutput(LogLevel.INFO)
    }
}
```

**Groovy DSL:**
```groovy
abstract class MyTask extends DefaultTask {
    // Option 1: Use Task.logger (no injection needed)
    @TaskAction
    void action() {
        logger.info('Using task logger')
    }
}

// Option 2: Inject LoggingManager
abstract class MyTask2 extends DefaultTask {
    @Inject
    abstract LoggingManager getLogging()
    
    @TaskAction
    void action() {
        logging.captureStandardOutput(LogLevel.INFO)
    }
}
```

## Capturing Project Information

For project metadata (name, version, etc.), capture it during configuration:

**Kotlin DSL:**
```kotlin
abstract class MyTask : DefaultTask() {
    @get:Input
    abstract val projectName: Property<String>
    
    @get:Input
    abstract val projectVersion: Property<String>
    
    @TaskAction
    fun action() {
        println("Building ${projectName.get()} version ${projectVersion.get()}")
    }
}

tasks.register<MyTask>("myTask") {
    projectName.set(project.name)
    projectVersion.set(project.version.toString())
}
```

**Groovy DSL:**
```groovy
abstract class MyTask extends DefaultTask {
    @Input
    abstract Property<String> getProjectName()
    
    @Input
    abstract Property<String> getProjectVersion()
    
    @TaskAction
    void action() {
        println("Building ${projectName.get()} version ${projectVersion.get()}")
    }
}

tasks.register('myTask', MyTask) {
    projectName.set(project.name)
    projectVersion.set(project.version.toString())
}
```

## Available Injected Services

All these services can be injected using `@Inject` (Groovy) or `@get:Inject` (Kotlin):

- `FileSystemOperations` - File operations
- `ExecOperations` - Process execution
- `ProjectLayout` - Project directory structure
- `ProviderFactory` - Creating providers
- `ObjectFactory` - Creating Gradle objects
- `LoggingManager` - Advanced logging
- `TextResourceFactory` - Text resource creation
- `ArchiveOperations` - Archive operations (zip, tar)
- `WorkerExecutor` - Parallel task execution
- `ToolchainSpec` - Java toolchain access
- `IsolationStrategy` - Classloader/process isolation

## Service Injection in Ad-hoc Tasks

**Kotlin DSL:**
```kotlin
interface Injected {
    @get:Inject val fs: FileSystemOperations
    @get:Inject val exec: ExecOperations
    @get:Inject val providers: ProviderFactory
}

tasks.register("adHocTask") {
    val injected = project.objects.newInstance<Injected>()
    
    doLast {
        injected.fs.copy {
            from("source")
            into("destination")
        }
    }
}
```

**Groovy DSL:**
```groovy
interface Injected {
    @Inject FileSystemOperations getFs()
    @Inject ExecOperations getExec()
    @Inject ProviderFactory getProviders()
}

tasks.register('adHocTask') {
    def injected = project.objects.newInstance(Injected)
    
    doLast {
        injected.fs.copy {
            from 'source'
            into 'destination'
        }
    }
}
```
