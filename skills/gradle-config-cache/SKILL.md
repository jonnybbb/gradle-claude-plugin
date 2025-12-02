---
name: gradle-config-cache
description: This skill should be used when the user asks to "enable configuration cache", "fix configuration cache errors", "make tasks configuration cache compatible", "fix Task.project at execution time", "speed up Gradle builds with caching", or mentions configuration cache problems, compatibility issues, cache invalidation, or needs to migrate custom tasks/plugins to support configuration cache.
---

# Gradle Configuration Cache

Enable, diagnose, and fix Gradle configuration cache issues to achieve 30-80% faster incremental builds.

## What is Configuration Cache?

Configuration cache stores the result of Gradle's configuration phase, allowing builds to skip it entirely when nothing affecting build configuration has changed.

**Key benefits:**
- Skip entire configuration phase on cache hits
- Enable task-level parallelism within projects
- Significantly faster incremental builds (30-80%)

## Quick Start

### Enable Configuration Cache

```properties
# gradle.properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn
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

Open the report:
```bash
# macOS
open build/reports/configuration-cache/*/configuration-cache-report.html

# Linux
xdg-open build/reports/configuration-cache/*/configuration-cache-report.html
```

## Most Common Problems

### Problem 1: Task.project Access at Execution Time

**Error:** "Invocation of 'Task.project' at execution time is unsupported"

**Cause:** Tasks access `project` during `doLast`/`doFirst` or `@TaskAction`

**Quick Fix:** Capture values during configuration phase

```kotlin
// ❌ Fails - project access at execution time
tasks.register("bad") {
    doLast {
        println(project.name)
    }
}

// ✅ Works - capture during configuration
tasks.register("good") {
    val projectName = project.name
    doLast {
        println(projectName)
    }
}
```

### Problem 2: System Properties & Environment Variables

**Error:** Accessing system properties/environment variables at execution time

**Quick Fix:** Use Gradle's Provider API

```kotlin
// ❌ Fails
tasks.register("bad") {
    doLast {
        val home = System.getProperty("user.home")
    }
}

// ✅ Works - use providers
tasks.register("good") {
    val userHome = providers.systemProperty("user.home")
    doLast {
        println("Home: ${userHome.get()}")
    }
}
```

For all problem types and solutions, see [references/common-problems.md](references/common-problems.md).

## Quick Reference Table

| Problem | Solution |
|---------|----------|
| Task.project access | Capture values during configuration |
| System.getProperty() | Use providers.systemProperty() |
| System.getenv() | Use providers.environmentVariable() |
| project.copy {} | Inject FileSystemOperations |
| project.exec {} | Inject ExecOperations |
| File reading | Declare as @InputFile or use providers.fileContents() |
| Build listeners | Use BuildService with lifecycle hooks |
| SourceSet/Configuration | Convert to FileCollection |

See [references/patterns.md](references/patterns.md) for fix patterns with code examples.

## Diagnosis Workflow

### Step 1: Test Basic Compatibility

```bash
./gradlew --configuration-cache help
```

Check for "Configuration cache entry stored" (success) or errors.

### Step 2: Find All Issues

```bash
./gradlew --configuration-cache-problems=warn build
```

Review the HTML report for all issues.

### Step 3: Fix Problems Iteratively

**Priority order:**
1. Problems when storing the configuration cache
2. Problems when loading the configuration cache
3. Configuration inputs causing unnecessary invalidation

### Step 4: Verify Cache Reuse

```bash
# First run - stores cache
./gradlew --configuration-cache build

# Second run - should reuse
./gradlew --configuration-cache build
# Should see: "Reusing configuration cache."
```

For detailed migration strategy, see [references/migration.md](references/migration.md).

## Configuration Options

```properties
# gradle.properties

# Enable configuration cache
org.gradle.configuration-cache=true

# Warning mode (for migration)
org.gradle.configuration-cache.problems=warn

# Limit problems before failure
org.gradle.configuration-cache.max-problems=10

# Read-only mode (for CI)
org.gradle.configuration-cache.read-only=true

# Parallel configuration caching (Incubating)
org.gradle.configuration-cache.parallel=true
```

## Marking Incompatible Tasks

If a task can't be fixed immediately:

```kotlin
tasks.register("legacyTask") {
    notCompatibleWithConfigurationCache("Uses reflection to discover tasks")
    doLast { /* task logic */ }
}
```

## Injectable Services

For configuration cache compatible tasks, inject services instead of using `project`:

| Service | Purpose |
|---------|---------|
| `FileSystemOperations` | copy, delete, sync |
| `ExecOperations` | exec, javaexec |
| `ArchiveOperations` | zipTree, tarTree |

Example:
```kotlin
abstract class MyTask : DefaultTask() {
    @get:Inject abstract val fs: FileSystemOperations

    @TaskAction
    fun execute() {
        fs.copy { from("src"); into("dest") }
    }
}
```

## Testing Compatibility

Test tasks with Gradle TestKit:

```kotlin
@Test
fun `task is configuration cache compatible`() {
    // First run - store cache
    runner().withArguments("--configuration-cache", "myTask").build()

    // Second run - reuse cache
    val result = runner().withArguments("--configuration-cache", "myTask").build()

    assertTrue(result.output.contains("Reusing configuration cache."))
}
```

## Using Build Scans (Develocity)

Build Scans provide additional debugging:

```bash
./gradlew build --configuration-cache --scan
```

Build Scans show:
- Configuration cache hit/miss status
- All problems with full stack traces
- Configuration inputs (what caused invalidation)

## Invalidating Cache Manually

```bash
rm -rf .gradle/configuration-cache
```

Gradle automatically cleans unused entries after 7 days.

## Related Skills & Resources

**Plugin skills:**
- `gradle-performance` - Overall build optimization
- `gradle-doctor` - Build health checks
- `gradle-troubleshooting` - Other build issues

**JBang tools:**
```bash
jbang ${CLAUDE_PLUGIN_ROOT}/tools/config-cache-fixer.java /path/to/project --fix
```

**Reference documentation:**
- [references/common-problems.md](references/common-problems.md) - All problems with detailed solutions
- [references/patterns.md](references/patterns.md) - Fix patterns with code examples
- [references/migration.md](references/migration.md) - Step-by-step migration guide
- [references/api-replacements.md](references/api-replacements.md) - API replacement reference
- [references/plugins.md](references/plugins.md) - Plugin compatibility list
- [references/debugging.md](references/debugging.md) - Advanced debugging techniques

**Official Gradle documentation:**
- [Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html)
- [Requirements](https://docs.gradle.org/current/userguide/configuration_cache_requirements.html)
- [Debugging](https://docs.gradle.org/current/userguide/configuration_cache_debugging.html)
