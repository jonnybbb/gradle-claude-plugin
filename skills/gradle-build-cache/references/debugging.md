# Build Cache Debugging Guide

Complete guide for diagnosing and fixing build cache issues.

## Diagnosing Cache Misses

### Check Task Outcome

```bash
# Run with verbose output
./gradlew build --build-cache --info 2>&1 | grep -E "(FROM-CACHE|executed|up-to-date)"

# Count cache hits vs executions
./gradlew build --build-cache 2>&1 | grep -c "FROM-CACHE"
```

Task outcomes:
- **FROM-CACHE**: Task output loaded from build cache
- **UP-TO-DATE**: Task skipped (inputs unchanged)
- **(executed)**: Task ran and potentially cached output

### Use Build Scans (Develocity)

Build Scans provide the best debugging experience for cache issues:

```bash
./gradlew build --build-cache --scan
```

In the Build Scan:
1. Go to **Performance → Build Cache**
2. See cache hit rate and savings
3. Click individual tasks to see:
   - Cache key hash
   - All inputs with their hashes
   - Why cache was missed

### Compare Build Scans

To diagnose why the same task has different cache keys:

1. Run build on machine A, get scan link
2. Run build on machine B, get scan link
3. In Build Scan, click **Compare builds**
4. Select the two scans
5. Navigate to the task in question
6. See input differences highlighted

### Build Scan via Develocity MCP Server

If you have the Develocity MCP server configured, query cache data:

```
# Find builds with low cache hit rates
mcp__develocity__getBuilds(
  query: "buildCacheHitRate<50",
  maxBuilds: 10
)

# Get task-level cache details
mcp__develocity__getBuild(buildId: "abc123")
```

## Common Caching Problems

### Problem 1: Missing Path Sensitivity

**Symptom:** Cache miss on different machines despite same source code

**Cause:** Absolute paths in task inputs

```kotlin
// ❌ Wrong - absolute paths cause cache misses
@get:InputFiles
abstract val sources: ConfigurableFileCollection

// ✅ Correct - relative paths
@get:InputFiles
@get:PathSensitive(PathSensitivity.RELATIVE)
abstract val sources: ConfigurableFileCollection
```

### Problem 2: Java Version Differences

**Symptom:** Cache misses between machines with different JDKs

**Cause:** Java toolchain not pinned

```kotlin
// ✅ Pin Java version for consistent caching
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}
```

### Problem 3: System File Encoding

**Symptom:** Cache misses on Windows vs Linux/macOS

**Cause:** Different default file encodings

```kotlin
// ✅ Explicitly set encoding
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}
```

Also in `gradle.properties`:
```properties
org.gradle.jvmargs=-Dfile.encoding=UTF-8
```

### Problem 4: Line Ending Differences

**Symptom:** Cache misses between Windows (CRLF) and Unix (LF)

**Solution:** Normalize line endings for inputs

```kotlin
@get:InputFiles
@get:PathSensitive(PathSensitivity.RELATIVE)
@get:NormalizeLineEndings
abstract val sources: ConfigurableFileCollection
```

Or configure Git:
```bash
git config --global core.autocrlf input
```

### Problem 5: Non-Deterministic Outputs

**Symptom:** Task outputs differ despite same inputs

**Common causes:**
- Timestamps in generated files
- Random values
- Unordered collections
- Build-time metadata

```kotlin
// ❌ Non-deterministic
manifest {
    attributes("Built-Time" to System.currentTimeMillis())
}

// ✅ Deterministic (or omit entirely)
manifest {
    attributes("Implementation-Version" to project.version)
}
```

For JAR/ZIP files:
```kotlin
tasks.withType<Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
```

### Problem 6: Volatile Inputs at Configuration Time

**Symptom:** Cache invalidation on every build

**Cause:** Reading volatile values during configuration

```kotlin
// ❌ Read at configuration time - cache miss every time
val gitSha = "git rev-parse HEAD".execute().text.trim()

// ✅ Use provider - deferred until execution
val gitSha = providers.exec {
    commandLine("git", "rev-parse", "HEAD")
}.standardOutput.asText.map { it.trim() }
```

### Problem 7: Overlapping Outputs

**Symptom:** Tasks interfere with each other's cache

**Cause:** Multiple tasks writing to same directory

```kotlin
// ❌ Overlapping outputs
tasks.register("generateA") {
    outputs.dir("build/generated")
}
tasks.register("generateB") {
    outputs.dir("build/generated")  // Same directory!
}

// ✅ Separate outputs
tasks.register("generateA") {
    outputs.dir("build/generated/a")
}
tasks.register("generateB") {
    outputs.dir("build/generated/b")
}
```

### Problem 8: Missing Classpath Normalization

**Symptom:** Compile tasks miss cache when JAR order changes

**Solution:** Use proper classpath annotations

```kotlin
// For compile classpaths (only ABI matters)
@get:CompileClasspath
abstract val compileClasspath: ConfigurableFileCollection

// For runtime classpaths (full content matters)
@get:Classpath
abstract val runtimeClasspath: ConfigurableFileCollection
```

### Problem 9: Undeclared Inputs

**Symptom:** Task produces correct output but cache is not reused

**Cause:** Input affects output but is not declared

```kotlin
// ❌ Undeclared input
@TaskAction
fun execute() {
    val config = System.getenv("CONFIG")  // Not tracked!
    // ...
}

// ✅ Declared input
@get:Input
@get:Optional
abstract val config: Property<String>
```

### Problem 10: Order-Sensitive Inputs

**Symptom:** Cache misses when file order changes

**Cause:** Input order affects task behavior

```kotlin
// If order matters, sort inputs
@get:InputFiles
@get:PathSensitive(PathSensitivity.RELATIVE)
val sortedSources: FileCollection
    get() = sources.asFileTree.sorted()
```

## Command Line Options

| Option | Description |
|--------|-------------|
| `--build-cache` | Enable build cache |
| `--no-build-cache` | Disable build cache |
| `--info` | Show cache hit/miss info |
| `--debug` | Detailed cache logging |
| `--scan` | Generate Build Scan with cache details |
| `--rerun-tasks` | Force re-execution (ignore cache) |
| `--offline` | Use local cache only |

## Debugging Workflow

### Step 1: Identify Problem Task

```bash
./gradlew build --build-cache --info 2>&1 | grep "executed"
```

### Step 2: Check Why Task Executed

```bash
./gradlew :module:taskName --build-cache --info
```

Look for messages like:
- "Task has no outputs declared"
- "Input property 'x' has changed"
- "Output property 'y' file has changed"

### Step 3: Compare Cache Keys

Generate Build Scans on both machines:
```bash
# Machine A
./gradlew build --build-cache --scan

# Machine B
./gradlew build --build-cache --scan
```

Compare scans to see input differences.

### Step 4: Verify Fix

```bash
# Clean and rebuild twice
./gradlew clean
./gradlew build --build-cache
./gradlew clean
./gradlew build --build-cache  # Should see FROM-CACHE
```

## Build Cache Inspection

### Local Cache Location

```
~/.gradle/caches/build-cache-1/
```

### Clear Local Cache

```bash
rm -rf ~/.gradle/caches/build-cache-1/
```

### Cache Statistics

Use Build Scans for statistics:
- Hit rate percentage
- Bytes saved
- Time saved
- Cache entry sizes

## Configuration Options

```properties
# gradle.properties

# Enable caching
org.gradle.caching=true

# Debug cache operations
org.gradle.caching.debug=true
```

```kotlin
// settings.gradle.kts

buildCache {
    local {
        isEnabled = true
        directory = file("${rootDir}/.gradle/build-cache")
        removeUnusedEntriesAfterDays = 7
    }
}
```

## Testing Cache Behavior

### Verify Task is Cacheable

```kotlin
@Test
fun `task is cacheable`() {
    // First build
    val result1 = runner()
        .withArguments("--build-cache", "myTask")
        .build()

    // Clean outputs
    runner().withArguments("clean").build()

    // Second build should use cache
    val result2 = runner()
        .withArguments("--build-cache", "myTask")
        .build()

    assertTrue(result2.output.contains("FROM-CACHE"))
}
```

### Verify Cache Key Stability

Run same task twice and compare:
```bash
./gradlew myTask --build-cache --info | grep "Build cache key"
```

The cache key should be identical for identical inputs.

## Related

- [Build Cache Reference](https://docs.gradle.org/current/userguide/build_cache.html)
- [Common Caching Problems](https://docs.gradle.org/current/userguide/common_caching_problems.html)
- [Build Cache Debugging](https://docs.gradle.org/current/userguide/build_cache_debugging.html)
- [Build Scan Cache Guide](https://docs.gradle.com/develocity/build-scans/#build_cache)
