---
name: gradle-config-cache
description: This skill should be used when the user asks to "enable configuration cache", "fix configuration cache errors", "make tasks configuration cache compatible", "fix Task.project at execution time", "speed up Gradle builds with caching", or mentions configuration cache problems, compatibility issues, cache invalidation, or needs to migrate custom tasks/plugins to support configuration cache.
---

# Gradle Configuration Cache

Enable, diagnose, and fix Gradle configuration cache issues to achieve 30-80% faster incremental builds.

## What is Configuration Cache?

Configuration cache stores the result of Gradle's configuration phase, allowing builds to skip it entirely when nothing affecting build configuration has changed. This dramatically improves build performance.

**Key benefits:**
- Skip entire configuration phase on cache hits
- Enable task-level parallelism within projects
- Cache dependency resolution results
- Significantly faster incremental builds

## Quick Start

### Enable Configuration Cache

Add to `gradle.properties`:

```properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn
```

Or use command line:

```bash
./gradlew --configuration-cache build
```

### First Test

```bash
# First run - stores cache
./gradlew --configuration-cache build

# Second run - should reuse cache
./gradlew --configuration-cache build
# Look for: "Reusing configuration cache."
```

### View Problems Report

When issues are detected, Gradle generates an HTML report:

```
build/reports/configuration-cache/<hash>/configuration-cache-report.html
```

The exact path is shown in console output. The report shows:
- Problems grouped by message and task
- Exact file and line number with stack traces
- Build configuration inputs (files, system properties, env vars)
- Links to documentation

**Open the report:**
```bash
# macOS
open build/reports/configuration-cache/*/configuration-cache-report.html

# Linux
xdg-open build/reports/configuration-cache/*/configuration-cache-report.html
```

### Enable Debug Mode

For complex issues, enable debug mode for detailed serialization traces:

```bash
./gradlew build --configuration-cache -Dorg.gradle.configuration-cache.debug=true
```

### Use Build Scans (Develocity)

Build Scans provide additional debugging capabilities:

```bash
./gradlew build --configuration-cache --scan
```

Build Scans show:
- Configuration cache hit/miss status
- All problems with full stack traces
- Configuration inputs (what caused invalidation)
- Time saved by cache reuse

**If using Develocity MCP server**, query build scan data for configuration cache problems programmatically.

## Common Problems & Fixes

### Problem 1: Task.project Access at Execution Time

**Error:** "Invocation of 'Task.project' at execution time is unsupported"

**Cause:** Tasks access `project` during `doLast`/`doFirst` or `@TaskAction`

**Fix:** Capture values during configuration phase

<details>
<summary>Kotlin DSL Example</summary>

```kotlin
// ❌ Fails - project access at execution time
tasks.register("bad") {
    doLast {
        println(project.name)
        println(project.version)
    }
}

// ✅ Works - capture during configuration
tasks.register("good") {
    val projectName = project.name
    val projectVersion = project.version

    doLast {
        println(projectName)
        println(projectVersion)
    }
}
```
</details>

<details>
<summary>Groovy DSL Example</summary>

```groovy
// ❌ Fails
tasks.register('bad') {
    doLast {
        println project.name
        println project.version
    }
}

// ✅ Works
tasks.register('good') {
    def projectName = project.name
    def projectVersion = project.version

    doLast {
        println projectName
        println projectVersion
    }
}
```
</details>

### Problem 2: System Properties & Environment Variables

**Error:** Accessing system properties/environment variables at execution time

**Cause:** Direct calls to `System.getProperty()` or `System.getenv()`

**Fix:** Use Gradle's Provider API

<details>
<summary>Kotlin DSL Example</summary>

```kotlin
// ❌ Fails
tasks.register("bad") {
    doLast {
        val home = System.getProperty("user.home")
        val path = System.getenv("PATH")
    }
}

// ✅ Works - use providers
tasks.register("good") {
    val userHome = providers.systemProperty("user.home")
    val path = providers.environmentVariable("PATH")

    doLast {
        println("Home: ${userHome.get()}")
        println("Path: ${path.get()}")
    }
}
```
</details>

<details>
<summary>Groovy DSL Example</summary>

```groovy
// ❌ Fails
tasks.register('bad') {
    doLast {
        def home = System.getProperty('user.home')
        def path = System.getenv('PATH')
    }
}

// ✅ Works
tasks.register('good') {
    def userHome = providers.systemProperty('user.home')
    def path = providers.environmentVariable('PATH')

    doLast {
        println "Home: ${userHome.get()}"
        println "Path: ${path.get()}"
    }
}
```
</details>

### Problem 3: File Operations (copy, delete, exec)

**Error:** "Invocation of 'Task.project' method 'copy' at execution time"

**Cause:** Using `project.copy {}`, `project.delete {}`, or `project.exec {}` in task actions

**Fix:** Inject service interfaces

<details>
<summary>Kotlin DSL Example</summary>

```kotlin
// ❌ Fails - project.copy at execution time
tasks.register("bad") {
    doLast {
        project.copy {
            from("src")
            into("dest")
        }
    }
}

// ✅ Works - inject FileSystemOperations
abstract class CopyTask : DefaultTask() {
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

tasks.register<CopyTask>("copyFiles") {
    sourceDir.set(file("src"))
    destDir.set(layout.buildDirectory.dir("dest"))
}
```
</details>

**Injectable services:**
- `FileSystemOperations` - copy, delete, sync
- `ExecOperations` - exec, javaexec
- `ArchiveOperations` - zipTree, tarTree
- `LoggingManager` - logging configuration

### Problem 4: Disallowed Types (SourceSet, Configuration)

**Error:** "SourceSet cannot be serialized" or "Configuration cannot be serialized"

**Cause:** Task references types like `SourceSet`, `Configuration`, `SourceSetContainer`

**Fix:** Convert to `FileCollection` or capture specific values

<details>
<summary>Kotlin DSL Example</summary>

```kotlin
// ❌ Fails - SourceSet not serializable
abstract class BadTask : DefaultTask() {
    @get:Input
    var sourceSet: SourceSet? = null
}

// ✅ Works - use FileCollection
abstract class GoodTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection
}

// Configuration
plugins {
    java
}

tasks.register<GoodTask>("processSource") {
    sourceFiles.from(sourceSets["main"].java.srcDirs)
}
```
</details>

### Problem 5: Reading Files Without Declaration

**Error:** Undeclared file reading causes cache invalidation

**Cause:** Reading files with `File.readText()` without declaring as input

**Fix:** Declare files as task inputs or use `providers.fileContents()`

<details>
<summary>Kotlin DSL Example</summary>

```kotlin
// ❌ Problem - undeclared file read
tasks.register("bad") {
    doLast {
        val config = file("config.txt").readText()
    }
}

// ✅ Solution 1 - declare as input
abstract class ReadConfigTask : DefaultTask() {
    @get:InputFile
    abstract val configFile: RegularFileProperty

    @TaskAction
    fun execute() {
        val content = configFile.asFile.get().readText()
        println(content)
    }
}

tasks.register<ReadConfigTask>("readConfig") {
    configFile.set(file("config.txt"))
}

// ✅ Solution 2 - use provider
tasks.register("readConfig") {
    val config = providers.fileContents(
        layout.projectDirectory.file("config.txt")
    ).asText

    doLast {
        println(config.get())
    }
}
```
</details>

### Problem 6: Build Listeners

**Error:** Build listeners are not configuration cache compatible

**Cause:** Using `gradle.taskGraph.whenReady {}` or similar hooks

**Fix:** Use Build Services with appropriate lifecycle hooks

<details>
<summary>Kotlin DSL Example</summary>

```kotlin
// ❌ Fails - listener not serializable
gradle.taskGraph.whenReady {
    println("Task graph ready with ${allTasks.size} tasks")
}

// ✅ Works - use BuildService
abstract class BuildLifecycleService :
    BuildService<BuildServiceParameters.None>,
    BuildEventsListenerRegistry {

    fun onTaskGraphReady() {
        // Custom logic here
    }
}

val lifecycleService = gradle.sharedServices.registerIfAbsent(
    "buildLifecycle",
    BuildLifecycleService::class
) {}
```
</details>

## Configuration Cache Compatible Task Template

Use this template when creating new tasks:

<details>
<summary>Kotlin DSL</summary>

```kotlin
@CacheableTask
abstract class MyTask : DefaultTask() {
    // Inject services (no project access needed)
    @get:Inject abstract val fs: FileSystemOperations
    @get:Inject abstract val exec: ExecOperations

    // Inputs (use Property API)
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
        // All project access via injected services
        // All values from declared inputs
        fs.copy {
            from(sources)
            into(outputDir)
        }
    }
}
```
</details>

<details>
<summary>Groovy DSL</summary>

```groovy
@CacheableTask
abstract class MyTask extends DefaultTask {
    @Inject abstract FileSystemOperations getFs()
    @Inject abstract ExecOperations getExec()

    @Input
    abstract Property<String> getConfig()

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getSources()

    @OutputDirectory
    abstract DirectoryProperty getOutputDir()

    @TaskAction
    void execute() {
        fs.copy {
            from sources
            into outputDir
        }
    }
}
```
</details>

## Diagnosis Workflow

### Step 1: Test Basic Compatibility

Start with simple task to test minimal configuration:

```bash
./gradlew --configuration-cache help
```

Check for "Configuration cache entry stored" (success) or errors.

### Step 2: Test Target Tasks

```bash
./gradlew --configuration-cache build
```

Or use dry-run to find configuration-time problems:

```bash
./gradlew --configuration-cache --dry-run build
```

### Step 3: Use Warning Mode to Find All Issues

Don't stop at first failure - discover everything:

```bash
./gradlew --configuration-cache-problems=warn build
```

Review the HTML report (path shown in console) for all issues.

### Step 4: Fix Problems Iteratively

**Fix priority:**
1. Problems when storing the configuration cache
2. Problems when loading the configuration cache
3. Configuration inputs causing unnecessary invalidation

### Step 5: Mark Incompatible Tasks (Temporarily)

If a task can't be fixed immediately:

```kotlin
tasks.register("legacyTask") {
    notCompatibleWithConfigurationCache("Uses reflection to discover tasks")
    doLast { /* task logic */ }
}
```

### Step 6: Verify Cache Reuse

```bash
# First run - stores cache
./gradlew --configuration-cache build

# Second run - should reuse
./gradlew --configuration-cache build
# Should see: "Reusing configuration cache."
```

## Optimization: Reducing Cache Invalidation

### Use Providers for Dynamic Values

Connect providers directly instead of reading at configuration time:

```kotlin
abstract class MyTask : DefaultTask() {
    @get:Input
    abstract val systemProp: Property<String>
}

tasks.register<MyTask>("myTask") {
    // ✅ Provider connection - read at execution
    systemProp.set(providers.systemProperty("myProp"))

    // ❌ Direct read - becomes configuration input
    // systemProp.set(System.getProperty("myProp"))
}
```

### Minimize Configuration-Time Logic

Move logic from configuration to execution when possible:

```kotlin
// ❌ Configuration time - expensive, may not be needed
val allFiles = fileTree("src").files.size
tasks.register("countFiles") {
    println("Total files: $allFiles")
}

// ✅ Execution time - only when task runs
tasks.register("countFiles") {
    doLast {
        val allFiles = fileTree("src").files.size
        println("Total files: $allFiles")
    }
}
```

### Use Prefix-Based Environment Variable Access

```kotlin
// ✅ Only variables with prefix become inputs
val jdkVars = providers.environmentVariablesPrefixedBy("JDK_")

// ❌ ALL environment variables become inputs
val jdkVars = System.getenv().filterKeys { it.startsWith("JDK_") }
```

## Testing Configuration Cache Compatibility

Test tasks with Gradle TestKit:

```kotlin
@Test
fun `task is configuration cache compatible`() {
    buildFile.writeText("""
        plugins {
            id("my-plugin")
        }
    """)

    // First run - store cache
    runner()
        .withArguments("--configuration-cache", "myTask")
        .build()

    // Second run - reuse cache
    val result = runner()
        .withArguments("--configuration-cache", "myTask")
        .build()

    assertTrue(result.output.contains("Reusing configuration cache."))
}
```

## Migration Strategy

### Phase 1: Assessment (1-4 hours)

1. Enable with warning mode
2. Run full build
3. Review HTML report
4. Count and categorize problems

### Phase 2: Quick Wins (2-8 hours)

Fix easy issues first (60-80% of problems):
- Property captures in doLast blocks
- System.getProperty → providers.systemProperty
- System.getenv → providers.environmentVariable

### Phase 3: Complex Fixes (2-16+ hours)

- Convert ad-hoc tasks to typed tasks
- Add service injection
- Replace build listeners with BuildService
- Update or replace incompatible plugins

### Phase 4: Validation

```bash
# Enable strict mode
./gradlew build --configuration-cache

# Verify cache reuse
./gradlew build  # Should see "Reusing configuration cache"
```

## Configuration Options

### Enable Persistently

```properties
# gradle.properties
org.gradle.configuration-cache=true
```

### Warning Mode (for migration)

```properties
org.gradle.configuration-cache.problems=warn
```

### Limit Problems Before Failure

```properties
org.gradle.configuration-cache.max-problems=10
```

### Read-Only Mode (for CI)

```properties
org.gradle.configuration-cache.read-only=true
```

### Parallel Configuration Caching (Incubating)

```properties
org.gradle.configuration-cache.parallel=true
```

### Strict Validation (debugging)

```properties
org.gradle.configuration-cache.integrity-check=true
```

## Quick Reference Table

| Problem | Solution |
|---------|----------|
| Task.project access | Capture values during configuration |
| System.getProperty() | Use providers.systemProperty() |
| System.getenv() | Use providers.environmentVariable() |
| project.copy {} | Inject FileSystemOperations |
| project.exec {} | Inject ExecOperations |
| project.delete {} | Inject FileSystemOperations |
| File reading | Declare as @InputFile or use providers.fileContents() |
| Build listeners | Use BuildService with lifecycle hooks |
| SourceSet/Configuration types | Convert to FileCollection |
| Gradle properties | Use providers.gradleProperty() |

## IDE Configuration

### IntelliJ IDEA / Android Studio

1. Run → Edit Configurations...
2. Select Templates → Gradle
3. Add VM options:
```
-Dorg.gradle.configuration-cache=true
-Dorg.gradle.configuration-cache.problems=warn
```

### Eclipse with Buildship

1. Preferences → Gradle
2. Add JVM arguments:
```
-Dorg.gradle.configuration-cache=true
-Dorg.gradle.configuration-cache.problems=warn
```

## Invalidating Cache Manually

If needed:

```bash
rm -rf .gradle/configuration-cache
```

Gradle automatically cleans unused entries after 7 days.

## Related Skills & Resources

**Plugin skills:**
- Use `/gradle-performance` for overall build optimization
- Use `/gradle-doctor` for build health checks
- Use `/gradle-troubleshooting` for other build issues

**JBang tools:**
```bash
# Detect and fix configuration cache issues
jbang ${CLAUDE_PLUGIN_ROOT}/tools/config-cache-fixer.java /path/to/project --fix
```

**Reference documentation:**
- [references/debugging.md](references/debugging.md) - HTML report, Build Scans, debug mode
- [references/patterns.md](references/patterns.md) - All fix patterns with examples
- [references/migration.md](references/migration.md) - Step-by-step migration guide
- [references/common-problems.md](references/common-problems.md) - Detailed problem solutions
- [references/plugins.md](references/plugins.md) - Plugin compatibility list

**Official Gradle documentation:**
- Configuration Cache: https://docs.gradle.org/current/userguide/configuration_cache.html
- Requirements: https://docs.gradle.org/current/userguide/configuration_cache_requirements.html
- Debugging: https://docs.gradle.org/current/userguide/configuration_cache_debugging.html

**Develocity/Build Scans:**
- Build Scan Configuration Cache: https://docs.gradle.com/develocity/build-scans/#configuration_cache

## When to Use This Skill

Invoke this skill when:
- Enabling configuration cache for the first time
- Seeing configuration cache errors or warnings
- Encountering "Task.project at execution time" errors
- Wanting to speed up Gradle builds (30-80% faster)
- Migrating custom tasks/plugins to be compatible
- Debugging configuration cache problems
- Optimizing cache hit rates
- Reviewing HTML reports and fixing specific issues
- Writing tests for configuration cache compatibility
- Users ask about Gradle build performance optimization