# Gradle Performance Tuning - Reference Guide

## Build Cache Deep Dive

### Local Build Cache

**Configuration in settings.gradle.kts:**
```kotlin
buildCache {
    local {
        directory = file("${rootDir}/.gradle/build-cache")
        removeUnusedEntriesAfterDays = 30
        isEnabled = true
    }
}
```

**How Build Cache Works:**
1. Task declares inputs and outputs properly
2. Gradle calculates cache key from inputs
3. Before execution, checks cache for matching key
4. If found, copies outputs from cache (cache hit)
5. If not found, executes task and stores outputs (cache miss)

**Cache Key Composition:**
- Task implementation class path
- Task action implementations
- Names and values of task inputs
- Classpath of task actions
- Output property names

### Remote Build Cache

**HTTP Build Cache:**
```kotlin
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

**Cache Strategy:**
- **Local dev**: Read from remote, write to local
- **CI**: Read and write to remote
- **PR builds**: Read-only from remote

### Task Output Caching

**Making Tasks Cacheable:**
```kotlin
@CacheableTask
abstract class ProcessDataTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFile: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val configFiles: ConfigurableFileCollection

    @get:Input
    abstract val processingMode: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Internal  // Not part of cache key
    val temporaryWorkDir: File = temporaryDir

    @TaskAction
    fun process() {
        // Deterministic task logic
    }
}
```

**Path Sensitivity Levels:**
- `ABSOLUTE`: Full path must match (NOT portable - avoid)
- `RELATIVE`: Path relative to project root (RECOMMENDED)
- `NAME_ONLY`: Only filename matters
- `NONE`: Path ignored completely

### Cache Miss Analysis

**Common Causes:**

1. **Absolute Paths in Inputs**
```kotlin
// ❌ Bad - absolute path breaks cache portability
val config = File("/home/user/project/config.json")

// ✅ Good - relative path
val config = layout.projectDirectory.file("config.json")
```

2. **Unstable Inputs (Timestamps)**
```kotlin
// ❌ Bad - changes every time
@get:Input
val buildTime = System.currentTimeMillis()

// ✅ Good - mark as internal or remove
@get:Internal
val buildTime = System.currentTimeMillis()
```

3. **Missing Input Annotations**
```kotlin
// ❌ Bad - file not tracked
class MyTask : DefaultTask() {
    var configFile: File? = null
}

// ✅ Good - properly annotated
abstract class MyTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val configFile: RegularFileProperty
}
```

4. **Non-Deterministic Output**
```kotlin
// ❌ Bad - random or time-dependent output
output.writeText("Built at: ${System.currentTimeMillis()}")

// ✅ Good - deterministic output
output.writeText("Version: ${version.get()}")
```

## Configuration Cache Deep Dive

### What is Configuration Cache?

Configuration cache stores the result of the configuration phase, allowing Gradle to skip it on subsequent builds.

**Benefits:**
- Skip configuration phase (can be 80%+ of build time for large projects)
- Faster IDE sync
- Better build performance for large multi-module projects

### Enabling Configuration Cache

```properties
# gradle.properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn  # or 'fail'
```

### Configuration Cache Compatibility

**Problem 1: Task Configuration Avoidance**
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

**Problem 2: Build Listener Usage**
```kotlin
// ❌ Bad: Build listeners not compatible
gradle.buildFinished {
    println("Build finished")
}

// ✅ Good: Use build service
abstract class BuildFinishedService : BuildService<BuildServiceParameters.None>

val serviceProvider = gradle.sharedServices.registerIfAbsent("buildFinished", BuildFinishedService::class) {}
```

**Problem 3: Runtime Project API Usage**
```kotlin
// ❌ Bad: Accessing project at execution time
tasks.register("myTask") {
    doLast {
        project.tasks.forEach { println(it.name) }
    }
}

// ✅ Good: Capture at configuration time
tasks.register("myTask") {
    val taskNames = tasks.names
    doLast {
        taskNames.forEach { println(it) }
    }
}
```

**Problem 4: Provider API Usage**
```kotlin
// ❌ Bad: Eager evaluation
val version = project.version.toString()

// ✅ Good: Lazy evaluation with Provider API
val version: Provider<String> = provider { project.version.toString() }
```

### Configuration Cache Reports

```bash
# Generate configuration cache report
gradle build --configuration-cache

# Report location
build/reports/configuration-cache/<build-id>/configuration-cache-report.html
```

## Gradle Daemon Optimization

### Daemon Heap Size

**Recommendations by Project Size:**
- Small (<10 modules): 2GB (`-Xmx2g`)
- Medium (10-30 modules): 4GB (`-Xmx4g`)
- Large (30-100 modules): 8GB (`-Xmx8g`)
- Very Large (100+ modules): 12GB+ (`-Xmx12g`)

**Configuration:**
```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${user.home}/gradle-heap-dump.hprof
```

### Daemon Lifecycle

```bash
# Check daemon status
gradle --status

# Stop all daemons
gradle --stop

# View daemon logs
cat ~/.gradle/daemon/*/daemon-*.out.log
```

### Daemon Health Issues

**Symptoms:**
- Slow builds
- OutOfMemoryError
- Daemon crashes
- High memory usage

**Solutions:**
1. Increase heap size
2. Stop and restart daemon
3. Check for memory leaks in build logic
4. Enable heap dumps for analysis

## Parallel Execution

### Enabling Parallel Builds

```properties
# gradle.properties
org.gradle.parallel=true
org.gradle.workers.max=8  # Typically number of CPU cores
```

### Parallel Execution Requirements

**Module Independence:**
- Modules must be truly independent
- No cross-project task dependencies
- Proper output/input declarations

**Best Practices:**
```kotlin
// ✅ Good: Use outputs as inputs
tasks.register<ProcessTask>("process") {
    inputFile.set(tasks.named<GenerateTask>("generate").flatMap { it.outputFile })
}

// ❌ Bad: Direct task dependency across projects
tasks.register("myTask") {
    dependsOn(project(":other").tasks.named("build"))
}
```

### Worker API for Task Parallelization

```kotlin
@CacheableTask
abstract class ParallelProcessTask : DefaultTask() {
    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun process() {
        val workQueue = workerExecutor.noIsolation()

        inputFiles.forEach { file ->
            workQueue.submit(ProcessWorkAction::class) {
                inputFile.set(file)
                outputFile.set(outputDir.file(file.name))
            }
        }
    }
}

abstract class ProcessWorkAction : WorkAction<ProcessWorkAction.Parameters> {
    interface Parameters : WorkParameters {
        val inputFile: RegularFileProperty
        val outputFile: RegularFileProperty
    }

    override fun execute() {
        val input = parameters.inputFile.get().asFile
        val output = parameters.outputFile.get().asFile
        // Process file in parallel
    }
}
```

## Incremental Compilation

### Java Incremental Compilation

```kotlin
tasks.withType<JavaCompile>().configureEach {
    options.isIncremental = true
    options.isFork = true
    options.forkOptions.jvmArgs = listOf("-Xmx2g")
}
```

### Kotlin Incremental Compilation

```kotlin
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    incremental = true
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}
```

### Annotation Processing Optimization

```kotlin
kapt {
    useBuildCache = true
    correctErrorTypes = true
    javacOptions {
        option("-Xmaxerrs", 500)
    }
}
```

## Test Execution Optimization

### Test Selection

```bash
# Run specific test class
gradle test --tests com.example.MyTest

# Run tests matching pattern
gradle test --tests "*IntegrationTest"

# Run single test method
gradle test --tests com.example.MyTest.testMethod
```

### Test Parallelization

```kotlin
tasks.test {
    useJUnitPlatform()

    // Parallel test execution
    maxParallelForks = Runtime.getRuntime().availableProcessors() / 2

    // Test caching
    outputs.upToDateWhen { false }  // Force re-run, or proper caching logic

    // JVM args for tests
    jvmArgs("-Xmx1g", "-XX:MaxMetaspaceSize=256m")

    // Test logging
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
}
```

### Continuous Testing

```bash
# Run tests continuously on file changes
gradle test --continuous
```

## Build Scan Integration

### Enable Build Scans

```kotlin
// settings.gradle.kts
plugins {
    id("com.gradle.enterprise") version "3.15.1"
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishAlways()

        tag("CI")
        tag("performance-test")

        value("Git Commit", providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText.get().trim())
    }
}
```

### Analyzing Build Scans

Build scans provide:
- Task execution timeline
- Configuration time breakdown
- Dependency resolution details
- Cache performance metrics
- Plugin application order
- Test execution details

## Performance Profiling

### Profile Report

```bash
gradle build --profile
```

Output: `build/reports/profile/profile-<timestamp>.html`

**Metrics Provided:**
- Total build time
- Configuration time
- Task execution time
- Dependency resolution time
- Per-task breakdown

### Build Scan for Deep Analysis

```bash
gradle build --scan
```

Provides URL to detailed analysis including:
- Performance insights
- Cache effectiveness
- Task input comparisons
- Incremental build analysis

## Performance Tuning Checklist

### Essential (High Impact)

- [ ] Enable build cache: `org.gradle.caching=true`
- [ ] Configure daemon heap: `org.gradle.jvmargs=-Xmx4g`
- [ ] Enable parallel builds: `org.gradle.parallel=true`
- [ ] Annotate task inputs/outputs correctly
- [ ] Use `@CacheableTask` on custom tasks
- [ ] Use `PathSensitivity.RELATIVE` for file inputs

### Recommended (Medium Impact)

- [ ] Enable configuration cache: `org.gradle.configuration-cache=true`
- [ ] Set up remote build cache for teams
- [ ] Optimize test execution (parallel forks)
- [ ] Use incremental compilation
- [ ] Implement Worker API for parallel task processing
- [ ] Configure dependency resolution caching

### Advanced (Lower Impact, Context-Dependent)

- [ ] Use composite builds for large projects
- [ ] Implement build services for expensive operations
- [ ] Optimize annotation processing
- [ ] Use test selection strategies
- [ ] Configure file watching for continuous builds
- [ ] Set up build scan publishing

## Performance Metrics

### Target Metrics

**Configuration Time:**
- Should be < 10% of total build time
- If higher, enable configuration cache

**Cache Hit Rate:**
- Target: > 70% for development builds
- Target: > 90% for CI builds with warm cache

**Parallel Efficiency:**
- Monitor with build scan
- Should see near-linear scaling up to core count

**Daemon Health:**
- Should start quickly (< 5 seconds)
- No memory errors
- No crashes

### Measuring Performance

```bash
# Baseline measurement
gradle clean build --no-build-cache --profile

# With cache
gradle clean build --build-cache --profile

# With configuration cache
gradle clean build --configuration-cache --profile

# Full optimization
gradle clean build --build-cache --configuration-cache --parallel --profile
```

## Common Performance Issues

### Slow Configuration Phase

**Symptoms:** Configuration takes > 30% of build time

**Causes:**
- Eager task creation
- Expensive configuration logic
- Many plugins applied
- buildSrc recompilation

**Solutions:**
- Enable configuration cache
- Use lazy task registration
- Minimize buildSrc changes
- Avoid expensive logic in configuration

### Low Cache Hit Rate

**Symptoms:** < 50% cache hits on development builds

**Causes:**
- Absolute paths in inputs
- Missing path sensitivity annotations
- Unstable task inputs (timestamps, random values)
- Incorrect input/output declarations

**Solutions:**
- Use relative paths
- Add `@PathSensitive(PathSensitivity.RELATIVE)`
- Remove/mark `@Internal` unstable inputs
- Verify all inputs/outputs annotated

### OutOfMemoryError

**Symptoms:** Daemon crashes with OOM

**Causes:**
- Insufficient heap size
- Memory leak in build logic
- Too many parallel workers

**Solutions:**
- Increase heap: `-Xmx8g`
- Reduce parallel workers
- Enable heap dumps and analyze
- Check for memory leaks in custom tasks

### Slow Test Execution

**Symptoms:** Tests take majority of build time

**Causes:**
- Sequential test execution
- No test caching
- Expensive test setup
- All tests run every time

**Solutions:**
- Enable parallel test execution
- Implement test caching properly
- Use test selection
- Optimize test fixtures
