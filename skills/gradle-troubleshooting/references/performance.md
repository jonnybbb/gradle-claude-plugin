# Performance Troubleshooting

## Quick Assessment

```bash
# Generate profile report
./gradlew build --profile

# Generate build scan (recommended)
./gradlew build --scan
```

## Common Performance Issues

### Slow Configuration Phase

**Symptoms**: Build takes long before any task runs

**Diagnosis**:
```bash
./gradlew help --scan
```
Check "Configuration" time in the build scan.

**Common Causes**:
- Plugin resolution at configuration time
- Network requests during configuration
- Expensive script logic
- Many subprojects without lazy configuration

**Fixes**:
```kotlin
// Avoid at configuration time
plugins {
    // Use version catalogs instead of string interpolation
    alias(libs.plugins.kotlin)
}

// Use lazy task registration
tasks.register("myTask") {
    // Configured only when task is needed
}
```

### Slow Dependency Resolution

**Symptoms**: Long "Resolving dependencies" phase

**Diagnosis**:
```bash
./gradlew dependencies --write-locks --scan
```

**Fixes**:
```kotlin
// Enable dependency locking
dependencyLocking {
    lockAllConfigurations()
}

// Use repository content filtering
repositories {
    mavenCentral {
        content {
            includeGroupByRegex("com\\.example.*")
        }
    }
}
```

### Cache Misses

**Symptoms**: Tasks re-run when they should be cached

**Diagnosis**:
```bash
./gradlew build --scan
# Check "Build cache" section
```

**Common Causes**:
- Non-reproducible inputs (timestamps, absolute paths)
- Missing @Input annotations
- @Internal used incorrectly

**Fixes**:
```kotlin
// Normalize inputs
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}

// Use PathSensitivity
@InputFiles
@PathSensitive(PathSensitivity.RELATIVE)
abstract val inputFiles: ConfigurableFileCollection
```

### Memory Issues

**Symptoms**: OutOfMemoryError, slow GC, swap usage

**Diagnosis**:
```bash
./gradlew build --info | grep -i "heap\|memory\|gc"
```

**Fixes**:
```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+UseParallelGC
```

### Daemon Issues

**Symptoms**: First build slow, daemon restarts frequently

**Diagnosis**:
```bash
./gradlew --status
```

**Fixes**:
```properties
# gradle.properties
org.gradle.daemon=true
org.gradle.daemon.idletimeout=10800000
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC
```

## Performance Optimization Checklist

| Issue | Quick Check | Solution |
|-------|-------------|----------|
| Slow config | `--scan` config time | Lazy APIs |
| No caching | `--scan` cache hits | Add @CacheableTask |
| Serial execution | `--scan` critical path | org.gradle.parallel=true |
| Low parallelism | CPU usage | org.gradle.workers.max |
| Memory pressure | Heap usage | Increase -Xmx |
| Daemon restarts | `--status` | Match JVM args |

## Parallel Execution

```properties
# gradle.properties
org.gradle.parallel=true
org.gradle.workers.max=8
```

Check for issues:
```bash
./gradlew build --scan
# Look for "Critical path" in timeline
```

## Build Cache Configuration

```properties
# gradle.properties
org.gradle.caching=true
```

Remote cache:
```kotlin
// settings.gradle.kts
buildCache {
    remote<HttpBuildCache> {
        url = uri("https://cache.example.com/")
        isPush = System.getenv("CI") != null
    }
}
```

## File System Watching

```properties
# gradle.properties
org.gradle.vfs.watch=true
```

This caches file system state between builds, reducing I/O.

## Configuration Cache

```properties
# gradle.properties
org.gradle.configuration-cache=true
```

Eliminates configuration phase on repeat builds.
