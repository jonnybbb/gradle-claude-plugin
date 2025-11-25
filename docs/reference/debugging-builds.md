# Build Debugging Techniques

**Gradle Version**: 7.0+

## Overview

Effective debugging strategies for Gradle build issues.

## Logging Levels

```bash
# Lifecycle (default)
./gradlew build

# Info (verbose)
./gradlew build --info

# Debug (very verbose)
./gradlew build --debug

# Quiet (errors only)
./gradlew build --quiet
```

## Stack Traces

```bash
# Show stack trace
./gradlew build --stacktrace

# Full stack trace
./gradlew build --full-stacktrace
```

## Dry Run

```bash
# Show tasks without executing
./gradlew build --dry-run
```

## Task Analysis

### Why Task Executed

```bash
# Show execution reason
./gradlew build --info | grep "Skipping\|Executing"

# Why not up-to-date?
./gradlew clean build --info | grep -A 5 "not up-to-date"
```

### Task Inputs/Outputs

```bash
# Show task details
./gradlew help --task build

# Input/output debugging
./gradlew build --info | grep -E "(Input|Output)"
```

## Dependency Debugging

```bash
# Show all dependencies
./gradlew dependencies

# Show specific configuration
./gradlew dependencies --configuration compileClasspath

# Why this version?
./gradlew dependencyInsight --dependency guava
```

## Build Scans

Most powerful debugging tool:

```bash
# Generate build scan
./gradlew build --scan

# Get detailed timeline, dependencies, failures
```

## Configuration Problems

### Configuration Time

```bash
# Profile configuration
./gradlew help --profile

# Check report: build/reports/profile/
```

### Configuration Cache Issues

```bash
# Run with configuration cache
./gradlew build --configuration-cache

# Show problems
./gradlew build --configuration-cache --configuration-cache-problems=warn
```

## Task Debugging

### Custom Logging

```kotlin
abstract class DebugTask : DefaultTask() {
    @TaskAction
    fun execute() {
        logger.lifecycle("Lifecycle message")
        logger.quiet("Important message")
        logger.info("Info message")
        logger.debug("Debug message")
        
        println("Direct output")
    }
}
```

### Print Inputs/Outputs

```kotlin
@TaskAction
fun execute() {
    logger.lifecycle("Inputs:")
    inputFiles.forEach {
        logger.lifecycle("  ${it.absolutePath}")
    }
    
    logger.lifecycle("Output: ${outputFile.get().asFile}")
}
```

## Debugging Techniques

### Breakpoints (IDE)

1. Set breakpoint in build file
2. Run Gradle with debug mode
3. Attach IDE debugger

```bash
# IntelliJ: Debug Gradle task
# or
./gradlew build --debug-jvm
# Then attach debugger to port 5005
```

### Gradlew Script Debugging

```bash
# Show Gradle execution
./gradlew build --debug | grep -i "gradle"
```

### Environment Inspection

```kotlin
tasks.register("showEnv") {
    doLast {
        println("Java: ${System.getProperty("java.version")}")
        println("Gradle: ${gradle.gradleVersion}")
        println("Project: ${project.name}")
        println("BuildDir: ${layout.buildDirectory.get()}")
    }
}
```

## Common Issues

### Build Fails Randomly

```bash
# Clean and rebuild
./gradlew clean build --rerun-tasks

# Check for parallel issues
./gradlew build --no-parallel

# Check daemon
./gradlew --stop
./gradlew build
```

### Task Not Found

```bash
# List all tasks
./gradlew tasks --all

# Show task details
./gradlew help --task taskName
```

### Slow Build

```bash
# Profile
./gradlew build --profile --scan

# Check configuration time
./gradlew help --profile
```

## Build Script Debugging

### Script Errors

```kotlin
// Print during configuration
println("Configuring ${project.name}")

// Print during execution
tasks.register("debug") {
    doLast {
        println("Executing debug")
    }
}
```

### Property Debugging

```kotlin
tasks.register("showProps") {
    doLast {
        project.properties.forEach { (key, value) ->
            println("$key = $value")
        }
    }
}
```

## Network Issues

```bash
# Show network activity
./gradlew build --info | grep -i "download\|http"

# Refresh dependencies
./gradlew build --refresh-dependencies
```

## Related Documentation

- [Common Errors](common-errors.md): Error solutions
- [Scan Analysis](scan-analysis.md): Build scan usage
- [Performance Tuning](performance-tuning.md): Optimization

## Quick Reference

```bash
# Debug commands
./gradlew build --info           # Verbose output
./gradlew build --debug          # Very verbose
./gradlew build --stacktrace     # Show stack traces
./gradlew build --scan           # Generate build scan
./gradlew build --profile        # Performance profile
./gradlew build --dry-run        # Show tasks
./gradlew dependencyInsight --dependency artifact

# Investigation
./gradlew dependencies           # All dependencies
./gradlew tasks --all           # All tasks
./gradlew help --task build     # Task details
./gradlew --status              # Daemon status
./gradlew --stop                # Stop daemons
```
