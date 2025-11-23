---
name: gradle-performance-tuning
description: Optimizes Gradle build performance through caching strategies, parallelization, incremental builds, and daemon tuning. Claude uses this when you ask to speed up builds, improve build times, optimize gradle performance, or fix slow builds.
---

# Gradle Performance Tuning Skill

This skill enables Claude to analyze and optimize Gradle build performance across multiple dimensions including build cache, configuration cache, daemon tuning, and parallel execution.

## When Claude Uses This Skill

Claude will automatically invoke this skill when you:
- Ask to "speed up my gradle builds"
- Say "my builds are slow" or "gradle is taking too long"
- Request "performance optimization" or "build performance tuning"
- Ask "how can I make gradle faster"
- Want to "optimize build times" or "improve compilation speed"
- Inquire about "build cache" or "configuration cache"
- Ask about "parallel builds" or "incremental compilation"

## Performance Optimization Areas

### 1. Build Cache Configuration

**Local Build Cache:**
```kotlin
// settings.gradle.kts
buildCache {
    local {
        directory = file("${rootDir}/.gradle/build-cache")
        removeUnusedEntriesAfterDays = 30
    }
}
```

**Remote Build Cache:**
```kotlin
// settings.gradle.kts
buildCache {
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

**Benefits:**
- Avoid redundant task executions
- Share build outputs across machines
- Faster CI/CD pipelines
- Reduced build times by 50-90% for cache hits

### 2. Configuration Cache

**Enable Configuration Cache:**
```kotlin
// gradle.properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn
```

**Benefits:**
- Skip configuration phase on subsequent builds
- Reduce build startup time by up to 80%
- Better performance for large multi-module projects
- Improved IDE sync times

**Common Issues and Fixes:**

**Issue 1: Task Configuration Avoidance**
```kotlin
// ❌ Bad: Configured eagerly
tasks.named("test").configure {
    useJUnitPlatform()
}

// ✅ Good: Lazy configuration
tasks.named<Test>("test") {
    useJUnitPlatform()
}
```

**Issue 2: Build Logic Usage**
```kotlin
// ❌ Bad: Runtime API usage
project.tasks.create("myTask")

// ✅ Good: Provider API
tasks.register("myTask") {
    // Configuration
}
```

### 3. Gradle Daemon Optimization

**Daemon Configuration:**
```properties
# gradle.properties
org.gradle.daemon=true
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError
org.gradle.daemon.idletimeout=3600000
org.gradle.parallel=true
org.gradle.caching=true
```

**Heap Size Guidelines:**
- Small projects (< 10 modules): 2GB (`-Xmx2g`)
- Medium projects (10-50 modules): 4GB (`-Xmx4g`)
- Large projects (50+ modules): 8GB+ (`-Xmx8g`)

**Daemon Health Check:**
```bash
# Check daemon status
gradle --status

# Stop daemons
gradle --stop

# View daemon logs
cat ~/.gradle/daemon/*/daemon-*.out.log
```

### 4. Parallel Execution

**Enable Parallel Builds:**
```properties
# gradle.properties
org.gradle.parallel=true
org.gradle.workers.max=8
```

**Project Structure for Parallelism:**
```kotlin
// settings.gradle.kts - Enable parallel execution
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

// Ensure modules can build independently
// Avoid cross-project task dependencies
```

**Best Practices:**
- Decouple modules to avoid sequential dependencies
- Use `api` vs `implementation` correctly
- Minimize cross-project task dependencies
- Configure worker threads based on CPU cores

### 5. Incremental Compilation

**Java Incremental Compilation:**
```kotlin
tasks.withType<JavaCompile>().configureEach {
    options.isIncremental = true
    options.isFork = true
    options.forkOptions.jvmArgs = listOf("-Xmx2g")
}
```

**Kotlin Incremental Compilation:**
```kotlin
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    incremental = true
    kotlinOptions {
        jvmTarget = "17"
    }
}
```

**Annotation Processing Optimization:**
```kotlin
kapt {
    useBuildCache = true
    arguments {
        arg("key", "value")
    }
}
```

### 6. Task Output Caching

**Making Tasks Cacheable:**
```kotlin
@CacheableTask
abstract class MyCustomTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun execute() {
        // Task implementation
    }
}
```

**Task Inputs and Outputs:**
```kotlin
tasks.register<MyTask>("processData") {
    inputs.files(fileTree("src/data"))
        .withPropertyName("dataFiles")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    outputs.dir(layout.buildDirectory.dir("processed-data"))
    outputs.cacheIf { true }
}
```

### 7. Dependency Resolution Optimization

**Dependency Locking:**
```kotlin
// Lock dependency versions for consistent builds
dependencyLocking {
    lockAllConfigurations()
}
```

**Repository Order:**
```kotlin
// Prioritize faster repositories
repositories {
    mavenLocal()  // Fastest: Local cache
    mavenCentral()
    google()
    gradlePluginPortal()
}
```

**Dependency Resolution Rules:**
```kotlin
configurations.all {
    resolutionStrategy {
        cacheDynamicVersionsFor(10, "minutes")
        cacheChangingModulesFor(4, "hours")
    }
}
```

### 8. Test Execution Optimization

**Test Selection:**
```bash
# Run only changed tests
gradle test --continuous

# Run tests in parallel
gradle test --parallel --max-workers=8
```

**Test Configuration:**
```kotlin
tasks.test {
    useJUnitPlatform()

    // Parallel test execution
    maxParallelForks = Runtime.getRuntime().availableProcessors() / 2

    // Test caching
    outputs.upToDateWhen { false }  // or proper caching logic

    // JVM args for tests
    jvmArgs("-Xmx1g")
}
```

## Performance Analysis

### Build Scan Integration

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
    }
}
```

### Profile Report

```bash
# Generate performance profile
gradle build --profile

# Output: build/reports/profile/profile-*.html
```

### Analyzing Build Performance

**Key Metrics:**
1. **Total Build Time**: Overall duration
2. **Configuration Time**: Settings evaluation + project configuration
3. **Task Execution Time**: Actual build work
4. **Cache Hit Rate**: Percentage of tasks loaded from cache
5. **Daemon Startup Time**: Daemon initialization overhead

**Target Metrics:**
- Configuration time: < 10% of total build time
- Cache hit rate: > 70% for typical development builds
- Parallel efficiency: Near-linear scaling with available cores

## Performance Tuning Workflow

### Step 1: Baseline Measurement

```bash
# Clean build without cache
gradle clean build --no-build-cache --profile

# Record total time and bottlenecks
```

### Step 2: Enable Build Cache

```bash
# Enable local build cache
echo "org.gradle.caching=true" >> gradle.properties

# Test with cache
gradle clean build --build-cache --profile
```

### Step 3: Enable Configuration Cache

```bash
# Enable configuration cache
echo "org.gradle.configuration-cache=true" >> gradle.properties

# Test configuration cache
gradle build --configuration-cache
```

### Step 4: Optimize Daemon

```bash
# Tune JVM settings
echo "org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g" >> gradle.properties

# Test daemon performance
gradle --stop && gradle build --profile
```

### Step 5: Enable Parallelization

```bash
# Enable parallel execution
echo "org.gradle.parallel=true" >> gradle.properties

# Test parallel builds
gradle build --parallel --profile
```

### Step 6: Verify Improvements

```bash
# Generate build scan for detailed analysis
gradle build --scan
```

## Performance Scripts

**scripts/analyze-performance.sh:**
```bash
#!/bin/bash
# Analyzes Gradle build performance

PROJECT_DIR="${1:-.}"
cd "$PROJECT_DIR" || exit 1

echo "=== Gradle Performance Analysis ==="
echo ""

# Check current configuration
echo "Current Configuration:"
echo "---"
grep -E "org.gradle.(daemon|parallel|caching|configuration-cache)" gradle.properties 2>/dev/null || echo "No performance settings configured"
echo ""

# Run clean build with profiling
echo "Running clean build with profiling..."
gradle clean build --profile --no-build-cache

# Extract performance metrics
PROFILE_FILE=$(find build/reports/profile -name "profile-*.html" -type f -printf '%T@ %p\n' | sort -n | tail -1 | cut -f2- -d" ")

if [[ -f "$PROFILE_FILE" ]]; then
    echo ""
    echo "Profile report: $PROFILE_FILE"
    echo "Open in browser to view detailed performance breakdown"
fi

# Test cache performance
echo ""
echo "Testing build cache performance..."
gradle clean
gradle build --build-cache

echo ""
echo "=== Recommendations ==="
[[ ! -f gradle.properties ]] && echo "⚠ Create gradle.properties with performance settings"
grep -q "org.gradle.caching=true" gradle.properties 2>/dev/null || echo "⚠ Enable build cache: org.gradle.caching=true"
grep -q "org.gradle.parallel=true" gradle.properties 2>/dev/null || echo "⚠ Enable parallel builds: org.gradle.parallel=true"
grep -q "org.gradle.configuration-cache=true" gradle.properties 2>/dev/null || echo "⚠ Consider configuration cache: org.gradle.configuration-cache=true"
```

**scripts/suggest-optimizations.sh:**
```bash
#!/bin/bash
# Suggests performance optimizations based on project analysis

PROJECT_DIR="${1:-.}"
cd "$PROJECT_DIR" || exit 1

echo "=== Performance Optimization Suggestions ==="
echo ""

# Check gradle.properties
if [[ ! -f gradle.properties ]]; then
    echo "❌ No gradle.properties found"
    echo "   Create gradle.properties with:"
    echo "   org.gradle.caching=true"
    echo "   org.gradle.parallel=true"
    echo "   org.gradle.jvmargs=-Xmx4g"
    echo ""
fi

# Check for buildSrc
if [[ -d buildSrc ]]; then
    echo "⚠ buildSrc detected - may slow configuration"
    echo "  Consider composite builds or published plugins"
    echo ""
fi

# Check module count
MODULE_COUNT=$(find . -name "build.gradle*" | wc -l)
echo "Modules: $MODULE_COUNT"
if [[ $MODULE_COUNT -gt 20 ]]; then
    echo "  ✓ Multi-module project - parallel builds recommended"
    echo "  ✓ Configuration cache will provide significant benefit"
else
    echo "  → Standard optimizations apply"
fi
echo ""

# Check for test tasks
if find . -name "build.gradle*" -exec grep -l "test {" {} \; | grep -q .; then
    echo "✓ Tests detected"
    echo "  Consider: maxParallelForks for test parallelization"
    echo ""
fi

echo "=== Recommended Actions ==="
echo "1. Enable build cache for task output reuse"
echo "2. Configure Gradle daemon with appropriate heap size"
echo "3. Enable parallel execution for multi-module builds"
echo "4. Consider configuration cache after resolving compatibility"
echo "5. Use build scans for detailed performance insights"
```

## Common Performance Issues

### Issue 1: Slow Configuration Phase

**Symptoms:**
- Configuration takes > 10% of build time
- IDE sync is slow

**Solutions:**
- Enable configuration cache
- Avoid eager task creation
- Use lazy task registration
- Minimize buildSrc usage

### Issue 2: Low Cache Hit Rate

**Symptoms:**
- Tasks re-execute unnecessarily
- Cache hit rate < 50%

**Solutions:**
- Ensure task inputs/outputs are properly declared
- Use `PathSensitivity.RELATIVE` for portable caches
- Avoid absolute paths in task inputs
- Check for unstable task inputs (timestamps, random values)

### Issue 3: Memory Issues

**Symptoms:**
- OutOfMemoryError during builds
- Daemon crashes frequently

**Solutions:**
- Increase daemon heap size (`-Xmx`)
- Increase metaspace (`-XX:MaxMetaspaceSize`)
- Enable heap dumps for analysis
- Check for memory leaks in custom tasks

### Issue 4: Slow Test Execution

**Symptoms:**
- Tests take majority of build time
- Sequential test execution

**Solutions:**
- Enable test parallelization (`maxParallelForks`)
- Use test filtering for local development
- Implement test caching properly
- Consider test distribution (Gradle Enterprise)

## Integration with Build Scans

```kotlin
// Enable build scans for detailed analysis
plugins {
    id("com.gradle.enterprise") version "3.15.1"
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"

        // Publish on every build
        publishAlways()

        // Tag builds
        tag("CI")
        tag("performance-test")

        // Add custom values
        value("Git Commit", providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText.get().trim())
    }
}
```

## Best Practices

1. **Measure First**: Always baseline performance before optimizations
2. **Incremental Changes**: Apply one optimization at a time
3. **Monitor Metrics**: Track configuration time, execution time, cache hit rate
4. **Use Build Scans**: Leverage Gradle build scans for detailed insights
5. **Profile Regularly**: Run `--profile` to identify bottlenecks
6. **Test Across Scenarios**: Clean build, incremental build, cache hit
7. **Document Settings**: Comment performance settings in gradle.properties

## Performance Tuning Checklist

- [ ] Enable local build cache
- [ ] Configure Gradle daemon heap size
- [ ] Enable parallel execution
- [ ] Test configuration cache compatibility
- [ ] Optimize task inputs/outputs for caching
- [ ] Configure incremental compilation
- [ ] Optimize test execution with parallelization
- [ ] Set up build scans for monitoring
- [ ] Review and optimize dependency resolution
- [ ] Profile builds regularly
- [ ] Monitor cache hit rates
- [ ] Document performance settings
