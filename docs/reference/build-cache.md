# Build Cache Reference

**Source**: https://docs.gradle.org/current/userguide/build_cache.html  
**Gradle Version**: 4.0+, optimized for 8+

## Overview

The build cache stores task outputs and reuses them across builds, even on different machines. This dramatically accelerates clean builds and CI/CD pipelines.

**Performance Impact**: 50-90% faster clean builds with populated cache

## Quick Start

### Enable Local Cache

**gradle.properties:**
```properties
org.gradle.caching=true
```

**Command line:**
```bash
./gradlew build --build-cache
```

### First vs Second Build

```bash
# First build (populates cache)
./gradlew clean build --build-cache
# Time: 120s

# Second build (uses cache)
./gradlew clean build --build-cache
# Time: 15s (87% faster)
```

## How Build Cache Works

### Task Cacheability

```
Task Inputs → Hash → Cache Key
                      ↓
        ┌─────────────┴─────────────┐
        ↓                           ↓
    Cache Hit                   Cache Miss
        ↓                           ↓
  Load Outputs              Execute Task
        ↓                           ↓
  Skip Execution            Store Outputs
```

### Cache Key Calculation

Based on:
1. Task implementation class
2. Input properties (files, values)
3. Output property names
4. Task action code
5. Classpath (for Java tasks)

## Task Cacheability

### Make Task Cacheable

**Kotlin DSL:**
```kotlin
@CacheableTask
abstract class ProcessFilesTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFiles: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun process() {
        // Task implementation
    }
}
```

**Groovy DSL:**
```groovy
@CacheableTask
abstract class ProcessFilesTask extends DefaultTask {
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getInputFiles()
    
    @OutputDirectory
    abstract DirectoryProperty getOutputDir()
    
    @TaskAction
    void process() {
        // Task implementation
    }
}
```

### Built-in Cacheable Tasks

These tasks are cacheable by default (Gradle 8+):
- `JavaCompile`
- `KotlinCompile`
- `Test`
- `Jar`, `War`, `Zip`
- `JavaExec`
- Many more

### Check Task Cacheability

```bash
# Show cacheable tasks
./gradlew help --info | grep -i cacheable

# Run with build cache enabled
./gradlew build --build-cache --info | grep "FROM-CACHE"
```

## Local Build Cache

### Configuration

**gradle.properties:**
```properties
org.gradle.caching=true
```

**build.gradle.kts:**
```kotlin
buildCache {
    local {
        isEnabled = true
        directory = File(rootDir, "build-cache")
        removeUnusedEntriesAfterDays = 30
    }
}
```

### Cache Location

Default: `$GRADLE_USER_HOME/caches/build-cache-1` (usually `~/.gradle/caches/build-cache-1`)

Custom:
```kotlin
buildCache {
    local {
        directory = file("/tmp/custom-cache")
    }
}
```

### Cache Maintenance

```bash
# View cache size
du -sh ~/.gradle/caches/build-cache-1

# Clean cache
rm -rf ~/.gradle/caches/build-cache-1

# Gradle will recreate on next build
```

## Remote Build Cache

### HTTP Build Cache

**build.gradle.kts:**
```kotlin
buildCache {
    remote<HttpBuildCache> {
        url = uri("https://cache.example.com")
        isPush = true
        
        credentials {
            username = System.getenv("CACHE_USERNAME")
            password = System.getenv("CACHE_PASSWORD")
        }
    }
}
```

**Groovy DSL:**
```groovy
buildCache {
    remote(HttpBuildCache) {
        url = 'https://cache.example.com'
        push = true
        
        credentials {
            username = System.getenv('CACHE_USERNAME')
            password = System.getenv('CACHE_PASSWORD')
        }
    }
}
```

### Local + Remote

```kotlin
buildCache {
    local {
        isEnabled = true
    }
    
    remote<HttpBuildCache> {
        url = uri("https://cache.example.com")
        
        // Pull from remote, but only push from CI
        isPush = System.getenv("CI") != null
    }
}
```

### Custom Build Cache

Implement `BuildCache` interface:

```kotlin
interface CustomBuildCache : BuildCache {
    var endpoint: String
    var region: String
}

class CustomBuildCacheService : BuildCacheService {
    override fun load(key: BuildCacheKey, reader: BuildCacheEntryReader): Boolean {
        // Load from custom storage
    }
    
    override fun store(key: BuildCacheKey, writer: BuildCacheEntryWriter) {
        // Store to custom storage
    }
    
    override fun close() {
        // Cleanup
    }
}
```

## Cache Configuration Patterns

### CI/CD Optimization

```kotlin
// settings.gradle.kts
buildCache {
    local {
        // Disable local cache on CI (no persistence between builds)
        isEnabled = !System.getenv("CI").toBoolean()
    }
    
    remote<HttpBuildCache> {
        url = uri("https://cache.example.com")
        
        // Always pull, only push from main branch
        isPush = System.getenv("CI_BRANCH") == "main"
        
        credentials {
            username = System.getenv("CACHE_USERNAME")
            password = System.getenv("CACHE_PASSWORD")
        }
    }
}
```

### Developer Workstation

```kotlin
buildCache {
    local {
        isEnabled = true
        directory = file("${System.getProperty("user.home")}/.gradle/build-cache")
        removeUnusedEntriesAfterDays = 7  // Clean frequently
    }
    
    remote<HttpBuildCache> {
        url = uri("https://cache.example.com")
        isPush = false  // Only pull, don't push from local
    }
}
```

### Multi-Project Build

```kotlin
// settings.gradle.kts
buildCache {
    local {
        isEnabled = true
        // Shared cache for all projects
        directory = file("${rootDir}/.gradle/build-cache")
    }
}
```

## Improving Cache Hit Rate

### 1. Use Proper Path Sensitivity

```kotlin
@get:InputFiles
@get:PathSensitive(PathSensitivity.RELATIVE)  // ✅ Good: Ignores absolute paths
abstract val sources: ConfigurableFileCollection

@get:InputFiles
@get:PathSensitive(PathSensitivity.ABSOLUTE)  // ❌ Bad: Cache miss on different paths
abstract val sources: ConfigurableFileCollection
```

### 2. Normalize File Line Endings

```kotlin
@get:InputFiles
@get:PathSensitive(PathSensitivity.RELATIVE)
@get:NormalizeLineEndings  // Ignore Windows vs Unix line endings
abstract val sources: ConfigurableFileCollection
```

### 3. Ignore Non-Deterministic Outputs

```kotlin
abstract class BuildTask : DefaultTask() {
    @get:OutputFile
    abstract val outputFile: RegularFileProperty
    
    @TaskAction
    fun build() {
        outputFile.asFile.get().writeText(
            "Built at: ${System.currentTimeMillis()}"  // ❌ Timestamp breaks caching
        )
    }
}

// ✅ Better: Exclude timestamps or make them deterministic
@TaskAction
fun build() {
    outputFile.asFile.get().writeText(
        "Built at: ${deterministic timestamp}"
    )
}
```

### 4. Use Consistent Tooling

```kotlin
// Specify exact tool versions
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
```

### 5. Declare All Inputs

```kotlin
// ❌ Bad: Hidden input (environment variable)
@TaskAction
fun execute() {
    val value = System.getenv("MY_VAR")  // Not declared
}

// ✅ Good: Declared input
@get:Input
abstract val myVar: Property<String>

// In build file
myTask {
    myVar.set(providers.environmentVariable("MY_VAR"))
}
```

## Cache Analysis

### View Cache Statistics

```bash
# Run with --scan
./gradlew build --build-cache --scan

# Open build scan URL and check:
# - Performance → Build cache
# - Cache hit rate
# - Task execution timeline
```

### Identify Non-Cacheable Tasks

```bash
./gradlew build --build-cache --info 2>&1 | grep "Caching disabled"

# Common reasons:
# - No @CacheableTask annotation
# - Missing input/output declarations
# - Non-deterministic outputs
```

### Cache Hit vs Miss

```bash
# Look for cache hits
./gradlew build --build-cache | grep "FROM-CACHE"

# Example output:
# :compileJava FROM-CACHE
# :processResources FROM-CACHE
# :classes UP-TO-DATE
```

## Common Issues

### Issue: Low Cache Hit Rate

**Symptoms**: Tasks execute even with populated cache

**Causes**:
1. Absolute paths in inputs
2. Non-deterministic outputs (timestamps, UUIDs)
3. Environment-specific inputs
4. Tool version differences

**Solutions**:

```kotlin
// 1. Use relative paths
@get:PathSensitive(PathSensitivity.RELATIVE)

// 2. Avoid timestamps in outputs
// Use git commit hash instead

// 3. Declare environment inputs
@get:Input
abstract val buildEnv: Property<String>

// 4. Pin tool versions
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
```

### Issue: Cache Growing Too Large

**Solution**: Configure cleanup

```kotlin
buildCache {
    local {
        removeUnusedEntriesAfterDays = 7  // More aggressive cleanup
    }
}
```

Or manual cleanup:
```bash
# Remove cache older than 7 days
find ~/.gradle/caches/build-cache-1 -type f -mtime +7 -delete
```

### Issue: Remote Cache Connection Failures

**Solution**: Add retry logic and fallback

```kotlin
buildCache {
    remote<HttpBuildCache> {
        url = uri("https://cache.example.com")
        
        // Fail gracefully
        isPush = false  // Continue build if push fails
    }
}
```

### Issue: Credential Management

**Solution**: Use environment variables or credential helpers

```kotlin
buildCache {
    remote<HttpBuildCache> {
        url = uri("https://cache.example.com")
        
        credentials {
            username = providers.environmentVariable("CACHE_USER").getOrElse("")
            password = providers.environmentVariable("CACHE_TOKEN").getOrElse("")
        }
    }
}
```

## Build Cache Backends

### Gradle Enterprise

```kotlin
// Requires Gradle Enterprise plugin
plugins {
    id("com.gradle.enterprise") version "3.15"
}

gradleEnterprise {
    buildCache {
        remote(gradleEnterprise.buildCache) {
            isEnabled = true
            isPush = true
        }
    }
}
```

### HTTP Cache Server

Open-source options:
- Gradle Build Cache Node
- nginx with WebDAV
- S3-compatible storage

### Docker Registry

Use Docker registry as cache backend:
```kotlin
// Custom implementation required
```

## Cache Performance Metrics

### Measure Impact

```bash
# Baseline (no cache)
time ./gradlew clean build --no-build-cache

# With cache (first run - populate)
time ./gradlew clean build --build-cache

# With cache (second run - hit)
time ./gradlew clean build --build-cache
```

### Expected Results

| Scenario | Time | Cache Benefit |
|----------|------|---------------|
| Baseline | 120s | - |
| First run (populate) | 125s | -4% (overhead) |
| Second run (hit) | 15s | 87% faster |

### Track Over Time

```bash
# In CI, track cache metrics
./gradlew build --build-cache --scan

# Extract from build scan:
# - Cache hit rate
# - Time saved
# - Remote cache latency
```

## Best Practices

### 1. Enable Everywhere

```properties
# gradle.properties (check into VCS)
org.gradle.caching=true
```

### 2. Use Remote Cache for Teams

Share cache across team members and CI/CD.

### 3. Push from CI Only

```kotlin
isPush = System.getenv("CI") != null
```

### 4. Monitor Cache Hit Rate

Target: >80% cache hit rate for stable builds

### 5. Optimize Cacheable Tasks

Make custom tasks cacheable:
```kotlin
@CacheableTask
@get:InputFiles
@get:PathSensitive(PathSensitivity.RELATIVE)
```

### 6. Use Build Scans

Analyze cache effectiveness with build scans.

## Security Considerations

### 1. Sensitive Data in Cache

Avoid caching tasks with secrets:
```kotlin
tasks.named("processSecrets") {
    outputs.cacheIf { false }  // Never cache
}
```

### 2. Remote Cache Authentication

Always use HTTPS and authentication:
```kotlin
remote<HttpBuildCache> {
    url = uri("https://cache.example.com")  // HTTPS only
    credentials { ... }
}
```

### 3. Cache Poisoning

Validate cache entries:
- Use trusted remote cache only
- Monitor cache modifications
- Implement access controls

## Version-Specific Notes

### Gradle 4.x-6.x
- Basic build cache support
- Limited task cacheability
- Manual opt-in required

### Gradle 7.x
- More tasks cacheable by default
- Improved cache key generation
- Better error handling

### Gradle 8.x
- Most built-in tasks cacheable
- Enhanced remote cache support
- Better cache analytics
- Configuration cache + build cache work together

### Gradle 9.x (upcoming)
- Universal task caching
- Improved cache sharing
- Better diagnostics

## Related Documentation

- [Configuration Cache](configuration-cache.md): Configuration caching (different from build cache)
- [Performance Tuning](performance-tuning.md): Overall build optimization
- [Task Basics](task-basics.md): Making tasks cacheable
- [Remote Cache Setup](remote-cache-setup.md): Detailed remote cache configuration

## Quick Reference

```kotlin
// Complete build cache setup
// settings.gradle.kts
buildCache {
    local {
        isEnabled = true
        directory = file("${rootDir}/.gradle/build-cache")
        removeUnusedEntriesAfterDays = 30
    }
    
    remote<HttpBuildCache> {
        url = uri("https://cache.example.com")
        isPush = System.getenv("CI") != null
        
        credentials {
            username = System.getenv("CACHE_USER")
            password = System.getenv("CACHE_PASSWORD")
        }
    }
}

// Make custom task cacheable
@CacheableTask
abstract class MyTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputs: ConfigurableFileCollection
    
    @get:OutputDirectory
    abstract val outputs: DirectoryProperty
    
    @TaskAction
    fun execute() {
        // Deterministic processing
    }
}
```
