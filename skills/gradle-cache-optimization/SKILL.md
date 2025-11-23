---
name: gradle-cache-optimization
description: Optimizes Gradle build cache and configuration cache, diagnoses cache misses, ensures task cacheability, and configures remote build caches. Claude uses this when you ask about build cache issues, cache misses, task cacheability, or cache configuration.
---

# Gradle Cache Optimization Skill

This skill enables Claude to optimize both build cache and configuration cache, diagnose cache-related issues, and ensure maximum cache efficiency.

## When Claude Uses This Skill

Claude will automatically invoke this skill when you:
- Ask about "build cache" or "cache misses"
- Report "tasks not using cache" or "low cache hit rate"
- Want to "configure remote build cache"
- Ask about "configuration cache" issues
- Request "task cacheability" analysis
- Inquire about "cache optimization"

## Build Cache Optimization

### Enable Build Cache

**gradle.properties:**
```properties
org.gradle.caching=true
```

**settings.gradle.kts:**
```kotlin
buildCache {
    local {
        directory = file("${rootDir}/.gradle/build-cache")
        removeUnusedEntriesAfterDays = 30
        isEnabled = true
    }
}
```

### Remote Build Cache

```kotlin
// settings.gradle.kts
buildCache {
    local {
        isEnabled = !System.getenv().containsKey("CI")
    }
    remote<HttpBuildCache> {
        url = uri("https://build-cache.example.com/cache/")
        isPush = System.getenv("CI")?.toBoolean() ?: false
        credentials {
            username = providers.environmentVariable("CACHE_USERNAME").orNull
            password = providers.environmentVariable("CACHE_PASSWORD").orNull
        }
    }
}
```

### Making Tasks Cacheable

**Kotlin DSL:**
```kotlin
@CacheableTask
abstract class CustomProcessingTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFile: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val inputFiles: ConfigurableFileCollection

    @get:Input
    abstract val processingMode: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Internal
    val temporaryDir: File = temporaryDir

    @TaskAction
    fun process() {
        // Task implementation
    }
}
```

**Path Sensitivity Options:**
- `ABSOLUTE`: Full path must match (not portable)
- `RELATIVE`: Path relative to root (recommended)
- `NAME_ONLY`: Only file name matters
- `NONE`: Ignore path completely

### Diagnosing Cache Misses

**Enable Verbose Caching:**
```bash
gradle build --build-cache -Dorg.gradle.caching.debug=true
```

**Common Cache Miss Causes:**

1. **Absolute Paths in Inputs:**
```kotlin
// ❌ Bad: Absolute path breaks portability
val configFile = File("/home/user/config.properties")

// ✅ Good: Relative path
val configFile = layout.projectDirectory.file("config.properties")
```

2. **Timestamps in Outputs:**
```kotlin
// ❌ Bad: Current time breaks caching
val buildTime = System.currentTimeMillis()

// ✅ Good: Use deterministic values or exclude from inputs
@get:Internal
val buildTime: Long = System.currentTimeMillis()
```

3. **Non-Deterministic Task Logic:**
```kotlin
// ❌ Bad: Random values
val randomId = UUID.randomUUID()

// ✅ Good: Deterministic based on inputs
val deterministicId = inputFile.get().asFile.hashCode()
```

4. **Missing Input Annotations:**
```kotlin
// ❌ Bad: Unannotated property affects output
class MyTask : DefaultTask() {
    var configFile: File? = null  // Not tracked!
}

// ✅ Good: Properly annotated
abstract class MyTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val configFile: RegularFileProperty
}
```

### Task Output Caching Strategy

```kotlin
tasks.register<ProcessResources>("processResources") {
    from("src/main/resources")
    into(layout.buildDirectory.dir("resources"))

    // Always cache this task
    outputs.cacheIf { true }

    // Or conditional caching
    outputs.cacheIf("Slow processing task") {
        inputs.files.files.size > 100
    }
}
```

## Configuration Cache Optimization

### Enable Configuration Cache

```properties
# gradle.properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn
```

### Configuration Cache Compatibility

**Avoid Task Configuration Avoidance Issues:**

```kotlin
// ❌ Bad: Eager task configuration
tasks.getByName("test").configure {
    useJUnitPlatform()
}

// ✅ Good: Lazy task configuration
tasks.named<Test>("test") {
    useJUnitPlatform()
}
```

**Use Provider API:**

```kotlin
// ❌ Bad: Eager evaluation
val version = project.version.toString()

// ✅ Good: Lazy evaluation
val version: Provider<String> = provider { project.version.toString() }
```

**Avoid Build Listener Usage:**

```kotlin
// ❌ Bad: Build listener incompatible with configuration cache
gradle.buildFinished {
    println("Build finished")
}

// ✅ Good: Use build service
abstract class BuildFinishedService : BuildService<BuildFinishedService.Params> {
    interface Params : BuildServiceParameters
}
```

### Detecting Configuration Cache Issues

```bash
# Run with detailed problem reporting
gradle build --configuration-cache --configuration-cache-problems=warn

# View configuration cache report
cat build/reports/configuration-cache/*/configuration-cache-report.html
```

### Common Configuration Cache Problems

**Problem 1: Runtime API Usage**
```kotlin
// ❌ Bad: project.tasks at execution time
tasks.register("myTask") {
    doLast {
        project.tasks.forEach { println(it.name) }
    }
}

// ✅ Good: Capture tasks at configuration time
tasks.register("myTask") {
    val taskNames = tasks.names
    doLast {
        taskNames.forEach { println(it) }
    }
}
```

**Problem 2: Serialization Issues**
```kotlin
// ❌ Bad: Non-serializable closure capturing
tasks.register("process") {
    val service = createService()  // Not serializable
    doLast {
        service.process()
    }
}

// ✅ Good: Use build service
abstract class MyBuildService : BuildService<MyBuildService.Params> {
    interface Params : BuildServiceParameters
}

val serviceProvider = gradle.sharedServices.registerIfAbsent("myService", MyBuildService::class) {}

tasks.register("process") {
    val service = serviceProvider
    doLast {
        service.get().process()
    }
}
```

## Cache Diagnostics

### scripts/cache-diagnostics.sh

```bash
#!/bin/bash
PROJECT_DIR="${1:-.}"
cd "$PROJECT_DIR" || exit 1

echo "=== Build Cache Diagnostics ==="
echo ""

# Check cache configuration
echo "Cache Configuration:"
grep -E "org.gradle.caching" gradle.properties 2>/dev/null || echo "Build cache not enabled in gradle.properties"
echo ""

# Check cache directory
CACHE_DIR="${HOME}/.gradle/caches/build-cache-1"
if [[ -d "$CACHE_DIR" ]]; then
    echo "Local Cache Directory: $CACHE_DIR"
    echo "Cache Size: $(du -sh "$CACHE_DIR" | cut -f1)"
    echo "Entry Count: $(find "$CACHE_DIR" -type f | wc -l)"
else
    echo "No local cache directory found"
fi
echo ""

# Run build with cache debugging
echo "Running build with cache debugging..."
gradle clean build --build-cache -Dorg.gradle.caching.debug=true 2>&1 | grep -E "(cache|FROM-CACHE)" || echo "No cache information"
```

## Cacheability Checklist

**For Each Custom Task:**
- [ ] Annotate with `@CacheableTask`
- [ ] All inputs properly annotated (`@InputFile`, `@InputFiles`, `@Input`)
- [ ] All outputs properly annotated (`@OutputFile`, `@OutputDirectory`)
- [ ] Use `@PathSensitive` with appropriate sensitivity
- [ ] No absolute paths in inputs
- [ ] No random/timestamp values affecting outputs
- [ ] Task logic is deterministic
- [ ] Internal values annotated with `@Internal`
- [ ] Test cacheability with `--build-cache` flag

**For Configuration Cache:**
- [ ] Use lazy task registration (`tasks.register`)
- [ ] Use Provider API for deferred evaluation
- [ ] Avoid task configuration at execution time
- [ ] No build listeners (use build services)
- [ ] Serializable task inputs
- [ ] No runtime project API usage
- [ ] Test with `--configuration-cache`

## Best Practices

1. **Use Relative Paths:** Always use `PathSensitivity.RELATIVE` for portable caches
2. **Annotate Everything:** Properly annotate all task inputs and outputs
3. **Test Cacheability:** Regularly test with `gradle clean build --build-cache`
4. **Monitor Cache Hit Rates:** Track cache effectiveness with build scans
5. **Remote Cache for Teams:** Set up remote cache for shared cache across team
6. **Incremental Adoption:** Enable configuration cache gradually, fixing issues
7. **Document Custom Tasks:** Clearly document caching behavior of custom tasks

## Integration with Other Skills

- **gradle-performance-tuning**: Cache optimization improves build performance
- **gradle-task-development**: Ensure new tasks are properly cacheable
- **gradle-troubleshooting**: Diagnose cache-related build issues
