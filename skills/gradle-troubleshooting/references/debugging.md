# Gradle Debugging Reference

## Diagnostic Commands

### Quick Diagnostics
```bash
# Show Gradle version and environment
./gradlew --version

# List all tasks (including hidden)
./gradlew tasks --all

# Show dependencies tree
./gradlew dependencies

# Show specific configuration dependencies
./gradlew dependencies --configuration runtimeClasspath

# Find dependency origin
./gradlew dependencyInsight --dependency guava
```

### Verbose Output
```bash
# Debug logging
./gradlew build --debug

# Info logging (less verbose than debug)
./gradlew build --info

# Stack trace on failure
./gradlew build --stacktrace

# Full stack trace
./gradlew build --full-stacktrace
```

### Build Scans
```bash
# Generate build scan
./gradlew build --scan

# Scan with agreement
./gradlew build --scan --no-scan-accept-tos-for-sharing
```

Build scans provide:
- Timeline of task execution
- Dependency resolution details
- Build cache performance
- Configuration time breakdown
- Test results
- Environment information

## Debugging Task Execution

### Task Inputs and Outputs
```bash
# Show task inputs/outputs (Gradle 8.6+)
./gradlew :myTask --show-inputs-outputs

# Show reason for task execution
./gradlew build --console=plain -Dorg.gradle.logging.level=info
```

### Task Order and Dependencies
```bash
# Dry run to see task execution order
./gradlew build --dry-run

# Visualize task graph
./gradlew build --task-graph  # Requires plugin
```

## Debugging Configuration Cache

```bash
# Run with configuration cache
./gradlew build --configuration-cache

# Report configuration cache problems
./gradlew build --configuration-cache-problems=warn

# Force configuration cache rebuild
./gradlew build --configuration-cache --no-configuration-cache-reuse
```

## Debugging Build Cache

```bash
# Show cache operations
./gradlew build --build-cache --info | grep -i cache

# Scan provides detailed cache info
./gradlew build --scan
```

## JVM Debugging

### Debugging Build Scripts
```bash
# Debug Gradle daemon
./gradlew build -Dorg.gradle.debug=true

# Debug specific task (via build.gradle.kts)
# tasks.withType<JavaCompile> {
#     options.isFork = true
#     options.forkOptions.jvmArgs = listOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005")
# }
```

### Memory Issues
```bash
# Monitor daemon memory
./gradlew --status

# Stop all daemons
./gradlew --stop

# Run without daemon (one-off debugging)
./gradlew build --no-daemon
```

## Common Debugging Patterns

### Dependency Conflicts
```kotlin
// In build.gradle.kts
configurations.all {
    resolutionStrategy {
        failOnVersionConflict()
    }
}
```

### Plugin Problems
```kotlin
// See applied plugins
afterEvaluate {
    println("Applied plugins:")
    plugins.forEach { println("  ${it.javaClass.name}") }
}
```

### Configuration Phase Issues
```kotlin
// Print task configuration order
gradle.taskGraph.whenReady {
    allTasks.forEach { println("Task: ${it.path}") }
}
```

### Execution Phase Issues
```kotlin
// Add debugging to any task
tasks.named("build") {
    doFirst { println("Starting build...") }
    doLast { println("Build finished.") }
}
```

## IDE Integration

### IntelliJ IDEA
1. Open Gradle tool window
2. Right-click task → Debug
3. Set breakpoints in build.gradle.kts

### VS Code
1. Install Gradle extension
2. Add launch configuration for Gradle
3. Attach debugger to daemon

## Environment Variables for Debugging

```bash
# Enable Gradle debug logging
export GRADLE_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"

# Show Gradle internal logging
export GRADLE_DEBUG=true

# Verbose dependency resolution
export GRADLE_RESOLUTION_RULES=verbose
```

## Troubleshooting Checklist

1. **Clear caches**: `./gradlew cleanBuildCache` or delete `.gradle/caches`
2. **Stop daemons**: `./gradlew --stop`
3. **Check wrapper**: Verify `gradle/wrapper/gradle-wrapper.properties`
4. **Check JAVA_HOME**: Ensure correct JDK version
5. **Update Gradle**: Try latest stable version
6. **Run with --info**: Get detailed execution info
7. **Generate scan**: `--scan` for comprehensive analysis
