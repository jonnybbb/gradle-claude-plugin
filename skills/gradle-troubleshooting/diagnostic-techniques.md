# Advanced Diagnostic Techniques

## Build Scans

### What are Build Scans?
Build Scans are detailed records of build execution, providing insights into:
- Performance bottlenecks
- Task execution timeline
- Dependency resolution
- Test results
- Plugin applications
- Environment configuration

### Creating Build Scans

**Command:**
```bash
gradle build --scan
```

**Output:**
```
Publishing build scan...
https://gradle.com/s/abc123def456
```

### Analyzing Build Scans

**Performance Tab:**
- Configuration time
- Task execution time
- Dependency resolution time
- Parallel execution efficiency

**Timeline Tab:**
- Task execution order
- Parallel task execution visualization
- Task dependencies graph

**Dependencies Tab:**
- Resolved versions
- Version conflicts
- Dependency insights

**Infrastructure Tab:**
- Gradle version
- Java version
- OS and hardware
- Environment variables

### Using Build Scans for Troubleshooting

**Scenario 1: Slow Builds**
1. Generate build scan: `gradle build --scan`
2. Check Performance → Configuration time (should be <10% of total)
3. Check Performance → Task execution (identify slowest tasks)
4. Check Timeline → Look for serialized tasks that could be parallel

**Scenario 2: Dependency Issues**
1. Generate build scan: `gradle build --scan`
2. Check Dependencies → Conflicts
3. Check Dependencies → Resolved versions
4. Use dependency insight for specific libraries

**Scenario 3: Test Failures**
1. Generate build scan: `gradle test --scan`
2. Check Tests → Failed tests
3. Check Tests → Execution time
4. View full stack traces

## Profiling Builds

### Build Time Profiling

**Enable profiling:**
```bash
gradle build --profile
```

**Output location:**
```
build/reports/profile/profile-<timestamp>.html
```

**Report sections:**
1. **Configuration**: Time spent evaluating build scripts
2. **Dependency Resolution**: Time resolving dependencies
3. **Task Execution**: Time executing each task

**Analyzing profile reports:**
- Configuration >10% of total → Consider configuration cache
- Dependency resolution >5% → Enable caching, check for dynamic versions
- Few tasks in parallel → Review task dependencies, enable parallel execution

### Heap Dump Analysis

**Generate heap dump on OOM:**
```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/gradle-heap-dump.hprof
```

**Analyze heap dump:**
```bash
# Using jhat (built-in to JDK)
jhat /tmp/gradle-heap-dump.hprof
# Open http://localhost:7000 in browser

# Using Eclipse MAT (Memory Analyzer Tool)
# Download from https://eclipse.dev/mat/
# Open heap dump file in MAT
```

**Common memory issues:**
- Large collections (List, Map) retained in memory
- Gradle daemon not being reused
- Plugin memory leaks
- Build script holding references to large objects

### Thread Dump Analysis

**Generate thread dump:**
```bash
# Find Gradle daemon PID
jps | grep GradleDaemon

# Generate thread dump
jstack <PID> > gradle-thread-dump.txt
```

**Analyze thread dump:**
- Look for BLOCKED threads
- Check for deadlocks
- Identify long-running operations

## Debugging Build Scripts

### Enable Debug Logging

```bash
# Full debug output
gradle build --debug > build-debug.log 2>&1

# Info level (less verbose)
gradle build --info

# Specific logging
gradle build -Dorg.gradle.logging.level=debug
```

### Debugging Task Execution

**Add logging to custom tasks:**
```kotlin
abstract class MyTask : DefaultTask() {
    @TaskAction
    fun execute() {
        logger.lifecycle("Starting task execution")
        logger.info("Input file: ${inputFile.get()}")
        logger.debug("Processing logic...")

        try {
            // Task logic
        } catch (e: Exception) {
            logger.error("Task failed", e)
            throw e
        }

        logger.lifecycle("Task completed successfully")
    }
}
```

**Logging levels:**
- `ERROR`: Critical errors
- `QUIET`: Important messages (always shown)
- `LIFECYCLE`: Standard progress messages (default)
- `INFO`: Detailed information (shown with --info)
- `DEBUG`: Debug information (shown with --debug)

### Debugging Dependency Resolution

**Show dependency tree:**
```bash
# All configurations
gradle dependencies

# Specific configuration
gradle dependencies --configuration compileClasspath

# Focus on specific dependency
gradle dependencyInsight --dependency guava --configuration compileClasspath
```

**Understanding output:**
```
+--- com.google.guava:guava:30.1-jre -> 31.0-jre (*)
     \--- com.example:library:1.0.0
```
- `->` indicates version conflict (30.1-jre was requested, 31.0-jre was selected)
- `(*)` indicates subtree already shown elsewhere

**Force dependency resolution output:**
```kotlin
configurations.all {
    resolutionStrategy {
        // Log all dependency substitutions
        eachDependency {
            logger.lifecycle("Resolving ${requested.group}:${requested.name}:${requested.version}")
        }
    }
}
```

### Debugging Configuration Cache

**Diagnose configuration cache issues:**
```bash
# Generate report
gradle build --configuration-cache

# See detailed problems
gradle build --configuration-cache --stacktrace
```

**Common issues:**
```
Configuration cache entry discarded:
- field `project` of task `:myTask` cannot be serialized
```

**Fix with diagnostic output:**
```kotlin
tasks.register("myTask") {
    doFirst {
        println("Project: ${project.name}")  // ❌ Breaks configuration cache
    }
}

// Fix:
tasks.register("myTask") {
    val projectName = project.name  // Capture at configuration time
    doFirst {
        println("Project: $projectName")  // ✅ Configuration cache compatible
    }
}
```

## Network Diagnostics

### Repository Connection Issues

**Test repository connectivity:**
```bash
# Check DNS resolution
nslookup repo.maven.apache.org

# Test HTTP connection
curl -I https://repo.maven.apache.org/maven2/

# Measure download speed
curl -o /dev/null -w "Time: %{time_total}s\nSpeed: %{speed_download} bytes/sec\n" \
  https://repo.maven.apache.org/maven2/com/google/guava/guava/31.0-jre/guava-31.0-jre.jar
```

**Enable network debugging:**
```bash
gradle build --debug 2>&1 | grep -i "http\|download\|repository"
```

**Common network issues:**
1. **Slow repository connections**
   - Use repository mirrors
   - Configure repository order (fastest first)
   - Enable offline mode for local development

2. **Proxy configuration**
```properties
# gradle.properties
systemProp.http.proxyHost=proxy.example.com
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=proxy.example.com
systemProp.https.proxyPort=8080
systemProp.http.nonProxyHosts=localhost|127.0.0.1
```

3. **SSL/TLS issues**
```bash
# Disable SSL verification (not recommended for production)
gradle build -Djavax.net.ssl.trustStore=/path/to/truststore.jks \
  -Djavax.net.ssl.trustStorePassword=changeit
```

### Firewall and Security

**Corporate firewall issues:**
- Whitelist Gradle repository URLs
- Configure proxy settings
- Use internal Artifactory/Nexus mirror

**Certificate issues:**
```bash
# Import certificate to Java truststore
keytool -import -alias gradle-repo -file repo-cert.pem \
  -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit
```

## Gradle Daemon Diagnostics

### Daemon Status

```bash
# List running daemons
gradle --status

# Output example:
   PID STATUS   INFO
 12345 IDLE     7.6
 12346 BUSY     8.5
```

### Daemon Issues

**Issue 1: Multiple daemon versions**
```bash
# Stop all daemons
gradle --stop

# Rebuild with single version
gradle build
```

**Issue 2: Daemon memory issues**
```bash
# Check daemon memory usage
jps -lv | grep GradleDaemon

# Adjust daemon memory
# gradle.properties
org.gradle.jvmargs=-Xmx4g
```

**Issue 3: Daemon not reused**
```bash
# Check daemon compatibility
gradle --status --info

# Ensure consistent JVM args across projects
# Use gradle-wrapper.properties to lock Gradle version
```

### Daemon Logs

**Location:**
```
~/.gradle/daemon/<gradle-version>/daemon-<pid>.out.log
```

**Analyze logs:**
```bash
# View latest daemon log
ls -lt ~/.gradle/daemon/*/daemon-*.out.log | head -1 | xargs cat

# Search for errors
grep -i error ~/.gradle/daemon/*/daemon-*.out.log
```

## Build Environment Diagnostics

### Check Environment

```bash
# Show all environment info
gradle -version

# Show build environment
gradle buildEnvironment

# Show project properties
gradle properties
```

### Common Environment Issues

**Issue 1: JAVA_HOME mismatch**
```bash
# Check JAVA_HOME
echo $JAVA_HOME

# Check Gradle's Java
gradle -version | grep JVM

# Fix: Set JAVA_HOME consistently
export JAVA_HOME=/path/to/jdk-17
```

**Issue 2: Incompatible Gradle version**
```bash
# Check current version
gradle -version

# Update wrapper
gradle wrapper --gradle-version 8.5

# Use wrapper
./gradlew build
```

**Issue 3: Missing environment variables**
```bash
# Check required variables
printenv | grep -E "JAVA_HOME|GRADLE_HOME|PATH"

# Set in gradle.properties or ~/.gradle/gradle.properties
```

## Advanced Troubleshooting Tools

### Gradle Profiler

**Installation:**
```bash
brew install gradle-profiler  # macOS
# Or download from https://github.com/gradle/gradle-profiler
```

**Profile builds:**
```bash
# Benchmark build performance
gradle-profiler --benchmark --project-dir . assemble

# Compare scenarios
gradle-profiler --benchmark --scenario-file scenarios.conf
```

**scenarios.conf example:**
```
baseline {
    tasks = ["assemble"]
}

with-cache {
    tasks = ["assemble"]
    gradle-args = ["--build-cache"]
}

with-configuration-cache {
    tasks = ["assemble"]
    gradle-args = ["--configuration-cache"]
}
```

### Build Health Monitoring

**Track metrics over time:**
```bash
# Create baseline
gradle build --scan > build-001.log

# Compare subsequent builds
gradle build --scan > build-002.log

# Analyze trends
# - Build duration
# - Task execution time
# - Cache hit rates
# - Test execution time
```

**Automated health checks:**
- CI/CD integration (fail build if too slow)
- Build scan API for automated analysis
- Custom metrics collection

### Root Cause Analysis

**Systematic approach:**

1. **Reproduce the issue**
   - Isolate minimal reproduction case
   - Document exact steps
   - Note environment details

2. **Gather diagnostic data**
   - Build scan
   - Profile report
   - Debug logs
   - Heap dump (if memory issue)

3. **Analyze data**
   - Identify anomalies
   - Compare with baseline
   - Check for patterns

4. **Formulate hypothesis**
   - What changed?
   - Why did it break?
   - What fixes could work?

5. **Test fix**
   - Apply fix in isolation
   - Verify resolution
   - Check for side effects

6. **Document resolution**
   - Record root cause
   - Document fix
   - Update build health checks

## Useful Diagnostic Commands Summary

```bash
# Build diagnostics
gradle build --scan                    # Detailed build scan
gradle build --profile                 # Profile report
gradle build --debug                   # Full debug output
gradle build --info                    # Detailed logging
gradle build --dry-run                 # Preview execution plan

# Dependency diagnostics
gradle dependencies                    # Full dependency tree
gradle dependencyInsight --dependency <lib>  # Specific dependency
gradle buildEnvironment                # Plugin dependencies

# Task diagnostics
gradle tasks --all                     # All tasks
gradle help --task <task>              # Task details
gradle <task> --dry-run               # Preview task execution

# Cache diagnostics
gradle build --no-build-cache         # Disable build cache
gradle build --refresh-dependencies   # Force dependency refresh
gradle cleanBuildCache                # Clear build cache

# Configuration diagnostics
gradle build --no-configuration-cache  # Disable configuration cache
gradle properties                      # Show all properties
gradle buildEnvironment                # Show build env

# Daemon diagnostics
gradle --status                        # Daemon status
gradle --stop                          # Stop all daemons

# Environment diagnostics
gradle -version                        # Gradle and Java version
gradle buildEnvironment                # Build dependencies
```
