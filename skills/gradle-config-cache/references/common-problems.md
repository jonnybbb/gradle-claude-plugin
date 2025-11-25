# Common Configuration Cache Problems and Solutions

This reference provides detailed solutions for the most common Configuration Cache problems.

## Disallowed Types Referenced by Tasks

### Problem: Referencing Gradle Model Types

Tasks must not reference Gradle model types like `Project`, `SourceSet`, `Configuration`, etc.

#### Example: SourceSet Reference (WRONG)

**Kotlin DSL:**
```kotlin
abstract class SomeTask : DefaultTask() {
    @get:Input 
    lateinit var sourceSet: SourceSet // ❌ Not allowed
    
    @TaskAction
    fun action() {
        val classpathFiles = sourceSet.compileClasspath.files
    }
}
```

**Groovy DSL:**
```groovy
abstract class SomeTask extends DefaultTask {
    @Input 
    SourceSet sourceSet // ❌ Not allowed
    
    @TaskAction
    void action() {
        def classpathFiles = sourceSet.compileClasspath.files
    }
}
```

#### Solution: Use FileCollection

**Kotlin DSL:**
```kotlin
abstract class SomeTask : DefaultTask() {
    @get:InputFiles 
    @get:Classpath
    abstract val classpath: ConfigurableFileCollection // ✅ Correct
    
    @TaskAction
    fun action() {
        val classpathFiles = classpath.files
    }
}

// Configure the task
tasks.register<SomeTask>("someTask") {
    classpath.from(sourceSets.main.get().compileClasspath)
}
```

**Groovy DSL:**
```groovy
abstract class SomeTask extends DefaultTask {
    @InputFiles 
    @Classpath
    abstract ConfigurableFileCollection getClasspath() // ✅ Correct
    
    @TaskAction
    void action() {
        def classpathFiles = classpath.files
    }
}

// Configure the task
tasks.register('someTask', SomeTask) {
    classpath.from(sourceSets.main.compileClasspath)
}
```

### Problem: Referencing Configuration for Resolved Files

#### Example (WRONG)

**Kotlin DSL:**
```kotlin
abstract class MyTask : DefaultTask() {
    @get:Input
    lateinit var configuration: Configuration // ❌ Not allowed
}
```

**Groovy DSL:**
```groovy
abstract class MyTask extends DefaultTask {
    @Input
    Configuration configuration // ❌ Not allowed
}
```

#### Solution: Use FileCollection

**Kotlin DSL:**
```kotlin
abstract class MyTask : DefaultTask() {
    @get:InputFiles
    @get:Classpath
    abstract val classpath: ConfigurableFileCollection // ✅ Correct
}

tasks.register<MyTask>("myTask") {
    classpath.from(configurations.runtimeClasspath)
}
```

**Groovy DSL:**
```groovy
abstract class MyTask extends DefaultTask {
    @InputFiles
    @Classpath
    abstract ConfigurableFileCollection getClasspath() // ✅ Correct
}

tasks.register('myTask', MyTask) {
    classpath.from(configurations.runtimeClasspath)
}
```

## Using Project at Execution Time

### Problem: Task Actions Calling project.copy()

#### Example (WRONG)

**Kotlin DSL:**
```kotlin
abstract class SomeTask : DefaultTask() {
    @TaskAction
    fun action() {
        project.copy { // ❌ Not allowed at execution time
            from("source")
            into("destination")
        }
    }
}
```

**Groovy DSL:**
```groovy
abstract class SomeTask extends DefaultTask {
    @TaskAction
    void action() {
        project.copy { // ❌ Not allowed at execution time
            from 'source'
            into 'destination'
        }
    }
}
```

#### Solution: Use Injected FileSystemOperations

**Kotlin DSL:**
```kotlin
abstract class SomeTask : DefaultTask() {
    @get:Inject 
    abstract val fs: FileSystemOperations // ✅ Inject service
    
    @TaskAction
    fun action() {
        fs.copy {
            from("source")
            into("destination")
        }
    }
}
```

**Groovy DSL:**
```groovy
abstract class SomeTask extends DefaultTask {
    @Inject 
    abstract FileSystemOperations getFs() // ✅ Inject service
    
    @TaskAction
    void action() {
        fs.copy {
            from 'source'
            into 'destination'
        }
    }
}
```

### Problem: Ad-hoc Task Using Project

#### Example (WRONG)

**Kotlin DSL:**
```kotlin
tasks.register("someTask") {
    doLast {
        project.copy { // ❌ Not allowed
            from("source")
            into("destination")
        }
    }
}
```

**Groovy DSL:**
```groovy
tasks.register('someTask') {
    doLast {
        project.copy { // ❌ Not allowed
            from 'source'
            into 'destination'
        }
    }
}
```

#### Solution: Use Injected Service via Interface

**Kotlin DSL:**
```kotlin
interface Injected {
    @get:Inject val fs: FileSystemOperations
}

tasks.register("someTask") {
    val injected = project.objects.newInstance<Injected>()
    doLast {
        injected.fs.copy { // ✅ Use injected service
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
}

tasks.register('someTask') {
    def injected = project.objects.newInstance(Injected)
    doLast {
        injected.fs.copy { // ✅ Use injected service
            from 'source'
            into 'destination'
        }
    }
}
```

### Problem: Using Task.getProject() in Task Action

Capture what you need from the project during configuration, not execution.

#### Example (WRONG)

**Kotlin DSL:**
```kotlin
abstract class MyTask : DefaultTask() {
    @TaskAction
    fun action() {
        val version = project.version.toString() // ❌ Not allowed
        println("Building version $version")
    }
}
```

**Groovy DSL:**
```groovy
abstract class MyTask extends DefaultTask {
    @TaskAction
    void action() {
        def version = project.version.toString() // ❌ Not allowed
        println("Building version $version")
    }
}
```

#### Solution: Declare as Task Property

**Kotlin DSL:**
```kotlin
abstract class MyTask : DefaultTask() {
    @get:Input
    abstract val projectVersion: Property<String>
    
    @TaskAction
    fun action() {
        val version = projectVersion.get() // ✅ Use task property
        println("Building version $version")
    }
}

tasks.register<MyTask>("myTask") {
    projectVersion.set(project.version.toString())
}
```

**Groovy DSL:**
```groovy
abstract class MyTask extends DefaultTask {
    @Input
    abstract Property<String> getProjectVersion()
    
    @TaskAction
    void action() {
        def version = projectVersion.get() // ✅ Use task property
        println("Building version $version")
    }
}

tasks.register('myTask', MyTask) {
    projectVersion.set(project.version.toString())
}
```

## Reading System Properties and Environment Variables

### Problem: Direct Access at Configuration Time

#### Example (WRONG)

**Kotlin DSL:**
```kotlin
tasks.register("someTask") {
    val destination = System.getProperty("someDestination") // ❌ Cache miss on change
    inputs.dir("source")
    outputs.dir(destination)
}
```

**Groovy DSL:**
```groovy
tasks.register('someTask') {
    def destination = System.getProperty('someDestination') // ❌ Cache miss on change
    inputs.dir('source')
    outputs.dir(destination)
}
```

#### Solution: Use Provider

**Kotlin DSL:**
```kotlin
abstract class SomeTask : DefaultTask() {
    @get:OutputDirectory
    abstract val destination: DirectoryProperty
}

tasks.register<SomeTask>("someTask") {
    inputs.dir("source")
    destination.set(layout.projectDirectory.dir(
        providers.systemProperty("someDestination") // ✅ Deferred reading
    ))
}
```

**Groovy DSL:**
```groovy
abstract class SomeTask extends DefaultTask {
    @OutputDirectory
    abstract DirectoryProperty getDestination()
}

tasks.register('someTask', SomeTask) {
    inputs.dir('source')
    destination.set(layout.projectDirectory.dir(
        providers.systemProperty('someDestination') // ✅ Deferred reading
    ))
}
```

### Problem: Filtering Environment Variables with Custom Predicate

#### Example (WRONG)

**Kotlin DSL:**
```kotlin
val jdkLocations = System.getenv().filterKeys { 
    it.startsWith("JDK_") // ❌ All env vars become inputs
}
```

**Groovy DSL:**
```groovy
def jdkLocations = System.getenv().findAll { 
    key, _ -> key.startsWith("JDK_") // ❌ All env vars become inputs
}
```

#### Solution: Use Prefix-Based Provider

**Kotlin DSL:**
```kotlin
val jdkLocationsProvider = providers.environmentVariablesPrefixedBy("JDK_") // ✅ Correct
```

**Groovy DSL:**
```groovy
def jdkLocationsProvider = providers.environmentVariablesPrefixedBy("JDK_") // ✅ Correct
```

## Running External Processes at Configuration Time

### Problem: Using exec() Directly

#### Example (WRONG)

**Kotlin DSL:**
```kotlin
val gitVersion = "git --version".execute().text // ❌ Not tracked
```

**Groovy DSL:**
```groovy
def gitVersion = "git --version".execute().text // ❌ Not tracked
```

#### Solution: Use providers.exec()

**Kotlin DSL:**
```kotlin
val gitVersion = providers.exec {
    commandLine("git", "--version")
}.standardOutput.asText.get() // ✅ Properly tracked
```

**Groovy DSL:**
```groovy
def gitVersion = providers.exec {
    commandLine("git", "--version")
}.standardOutput.asText.get() // ✅ Properly tracked
```

#### Solution: Use Custom ValueSource for Complex Cases

**Kotlin DSL:**
```kotlin
abstract class GitVersionValueSource : ValueSource<String, ValueSourceParameters.None> {
    @get:Inject
    abstract val execOperations: ExecOperations
    
    override fun obtain(): String {
        val output = ByteArrayOutputStream()
        execOperations.exec {
            commandLine("git", "--version")
            standardOutput = output
        }
        return String(output.toByteArray(), Charset.defaultCharset())
    }
}

val gitVersionProvider = providers.of(GitVersionValueSource::class) {}
```

**Groovy DSL:**
```groovy
abstract class GitVersionValueSource implements ValueSource<String, ValueSourceParameters.None> {
    @Inject
    abstract ExecOperations getExecOperations()
    
    String obtain() {
        ByteArrayOutputStream output = new ByteArrayOutputStream()
        execOperations.exec {
            it.commandLine "git", "--version"
            it.standardOutput = output
        }
        return new String(output.toByteArray(), Charset.defaultCharset())
    }
}

def gitVersionProvider = providers.of(GitVersionValueSource.class) {}
```

## Undeclared File Reading

### Problem: Direct File Reading

#### Example (WRONG)

**Kotlin DSL:**
```kotlin
val config = file("some.conf").readText() // ❌ Not tracked
```

**Groovy DSL:**
```groovy
def config = file('some.conf').text // ❌ Not tracked
```

#### Solution: Use providers.fileContents()

**Kotlin DSL:**
```kotlin
val config = providers.fileContents(
    layout.projectDirectory.file("some.conf")
).asText // ✅ Properly tracked
```

**Groovy DSL:**
```groovy
def config = providers.fileContents(
    layout.projectDirectory.file('some.conf')
).asText // ✅ Properly tracked
```

## Accessing Top-Level Build Script Methods/Variables at Execution Time

### Problem: Groovy Top-Level Method

#### Example (WRONG)

**Groovy DSL:**
```groovy
def listFiles(File dir) {
    dir.listFiles({ file -> file.isFile() } as FileFilter).name.sort()
}

tasks.register('listFiles') {
    doLast {
        println listFiles(file('data')) // ❌ Method not found at execution
    }
}
```

#### Solution: Convert to Static Method in Class

**Groovy DSL:**
```groovy
class Files {
    static def listFiles(File dir) {
        dir.listFiles({ file -> file.isFile() } as FileFilter).name.sort()
    }
}

tasks.register('listFiles') {
    doLast {
        println Files.listFiles(file('data')) // ✅ Works correctly
    }
}
```

### Problem: Kotlin Top-Level Function

#### Example (WRONG)

**Kotlin DSL:**
```kotlin
fun listFiles(dir: File): List<String> =
    dir.listFiles { file: File -> file.isFile }.map { it.name }.sorted()

tasks.register("listFiles") {
    doLast {
        println(listFiles(file("data"))) // ❌ Cannot serialize
    }
}
```

#### Solution: Use Object and Narrow Scope

**Kotlin DSL:**
```kotlin
object Files {
    fun listFiles(dir: File): List<String> =
        dir.listFiles { file: File -> file.isFile }.map { it.name }.sorted()
}

tasks.register("listFiles") {
    val dir = file("data") // Capture in narrower scope
    doLast {
        println(Files.listFiles(dir)) // ✅ Works correctly
    }
}
```

## Build Listeners

### Problem: Registering Build Listeners

Build listeners registered at configuration time and triggered at execution time are not supported.

#### Solution: Use Build Services

**Kotlin DSL:**
```kotlin
abstract class MyBuildService : BuildService<BuildServiceParameters.None>,
    OperationCompletionListener {
    
    override fun onFinish(event: FinishEvent) {
        // Handle event
    }
}

val myService = gradle.sharedServices.registerIfAbsent("myService", MyBuildService::class) {
    // Configure service
}

// Register for task completion events
gradle.serviceRegistry.get(BuildEventsListenerRegistry::class.java)
    .onTaskCompletion(myService)
```

**Groovy DSL:**
```groovy
abstract class MyBuildService implements BuildService<BuildServiceParameters.None>,
    OperationCompletionListener {
    
    void onFinish(FinishEvent event) {
        // Handle event
    }
}

def myService = gradle.sharedServices.registerIfAbsent('myService', MyBuildService) {
    // Configure service
}

// Register for task completion events
gradle.serviceRegistry.get(BuildEventsListenerRegistry)
    .onTaskCompletion(myService)
```

## Sharing Mutable Objects Between Tasks

### Problem: Reference Equality Not Preserved

For performance, certain classes (String, File, Collection implementations) don't preserve reference equality after deserialization.

#### Best Practices

**Kotlin DSL:**
```kotlin
// ❌ Don't rely on reference equality for standard types
abstract class StatefulTask : DefaultTask() {
    @get:Internal
    var strings: List<String>? = null
}

// ✅ Wrap in user-defined class if needed
class StateHolder(val strings: List<String>)

abstract class StatefulTask : DefaultTask() {
    @get:Internal
    var holder: StateHolder? = null
}
```

**Groovy DSL:**
```groovy
// ❌ Don't rely on reference equality for standard types
abstract class StatefulTask extends DefaultTask {
    @Internal
    List<String> strings
}

// ✅ Wrap in user-defined class if needed
class StateHolder {
    final List<String> strings
    StateHolder(List<String> strings) { this.strings = strings }
}

abstract class StatefulTask extends DefaultTask {
    @Internal
    StateHolder holder
}
```

### Solution: Use Build Services for Cross-Task State

**Kotlin DSL:**
```kotlin
abstract class SharedStateService : BuildService<BuildServiceParameters.None> {
    val sharedData = mutableListOf<String>()
}

abstract class Task1 : DefaultTask() {
    @get:Internal
    abstract val stateService: Property<SharedStateService>
    
    @TaskAction
    fun action() {
        stateService.get().sharedData.add("data")
    }
}

val sharedService = gradle.sharedServices.registerIfAbsent(
    "sharedState", 
    SharedStateService::class
) {}

tasks.register<Task1>("task1") {
    stateService.set(sharedService)
}
```

**Groovy DSL:**
```groovy
abstract class SharedStateService implements BuildService<BuildServiceParameters.None> {
    final List<String> sharedData = []
}

abstract class Task1 extends DefaultTask {
    @Internal
    abstract Property<SharedStateService> getStateService()
    
    @TaskAction
    void action() {
        stateService.get().sharedData.add('data')
    }
}

def sharedService = gradle.sharedServices.registerIfAbsent(
    'sharedState', 
    SharedStateService
) {}

tasks.register('task1', Task1) {
    stateService.set(sharedService)
}
```
