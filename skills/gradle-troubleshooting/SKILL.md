---
name: gradle-troubleshooting
description: Diagnoses and fixes Gradle build failures, :check task errors (checkstyle, lint, errorprone), dependency issues, and provides automated fixes for common problems. Claude uses this when builds fail, tests don't pass, or you encounter gradle errors.
---

# Gradle Troubleshooting Skill

This skill enables Claude to diagnose and fix Gradle build failures, detect `:check` task issues, and provide automated solutions for common problems.

## When Claude Uses This Skill

Claude will automatically invoke this skill when you:
- Report "build failed" or "gradle error"
- Encounter `:check` task failures (checkstyle, lint, errorprone)
- Ask about "why build is failing"
- Need to "debug gradle issue"
- Report "tests failing" or "compilation errors"

## Automated Troubleshooting

### :check Task Failures

**Detecting Check Failures:**
```bash
# Run check task
./gradlew check

# Common failures:
# - checkstyle violations
# - PMD violations
# - SpotBugs issues
# - Lint errors
# - ErrorProne warnings
```

**Auto-Fix: Checkstyle**
```kotlin
// Apply checkstyle fixes automatically
tasks.register("fixCheckstyle") {
    doLast {
        // Parse checkstyle report
        val report = file("build/reports/checkstyle/main.xml")
        // Apply automated fixes for common issues
    }
}
```

**Auto-Fix: Lint**
```bash
# Android lint auto-fix
./gradlew lintFix
```

### Common Build Failures

### 1. Dependency Resolution Failure

**Error:**
```
Could not resolve com.example:library:1.0.0.
```

**Diagnosis:**
```bash
# Check dependency insight
gradle dependencyInsight --dependency library

# Verify repository configuration
gradle dependencies --configuration compileClasspath
```

**Fixes:**
```kotlin
// Fix 1: Add missing repository
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

// Fix 2: Update version or use dynamic version
dependencies {
    implementation("com.example:library:+")  // Latest
}

// Fix 3: Exclude problematic transitive dependency
dependencies {
    implementation("com.example:parent:1.0") {
        exclude(group = "com.example", module = "problematic")
    }
}
```

### 2. OutOfMemoryError

**Error:**
```
OutOfMemoryError: Java heap space
```

**Diagnosis:**
```bash
# Check current heap size
gradle --status

# Review daemon logs
cat ~/.gradle/daemon/*/daemon-*.out.log | grep -i "memory"
```

**Fixes:**
```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError

# For specific tasks
org.gradle.jvmargs=-Xmx4g
org.gradle.daemon.jvmargs=-Xmx4g
```

**Immediate Fix:**
```bash
# Stop daemon and restart
gradle --stop
gradle build
```

### 3. Compilation Errors

**Error:**
```
Compilation failed; see the compiler error output for details.
```

**Diagnosis:**
```bash
# Verbose compilation
gradle compileJava --info

# Check Java version
gradle -version
java -version
```

**Fixes:**
```kotlin
// Fix 1: Update Java toolchain
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Fix 2: Configure compiler options
tasks.withType<JavaCompile> {
    options.release.set(17)
    options.encoding = "UTF-8"
}

// Fix 3: Add missing dependencies
dependencies {
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
}
```

### 4. Test Failures

**Error:**
```
Test failed with X failures
```

**Diagnosis:**
```bash
# Run tests with stack traces
gradle test --stacktrace

# Run specific test
gradle test --tests com.example.MyTest

# View test report
open build/reports/tests/test/index.html
```

**Fixes:**
```kotlin
// Fix 1: Configure test platform
tasks.test {
    useJUnitPlatform()
}

// Fix 2: Add missing test dependencies
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Fix 3: Increase test timeout
tasks.test {
    timeout.set(Duration.ofMinutes(10))
}

// Fix 4: Disable flaky tests temporarily
tasks.test {
    filter {
        excludeTestsMatching("*FlakyTest")
    }
}
```

### 5. Configuration Cache Issues

**Error:**
```
Configuration cache problems found
```

**Diagnosis:**
```bash
# Run with detailed reporting
gradle build --configuration-cache --configuration-cache-problems=warn

# View report
open build/reports/configuration-cache/*/configuration-cache-report.html
```

**Fixes:**
```kotlin
// Fix 1: Use lazy task registration
// ❌ Bad
tasks.create("myTask")

// ✅ Good
tasks.register("myTask")

// Fix 2: Use Provider API
// ❌ Bad
val version = project.version.toString()

// ✅ Good
val version = provider { project.version.toString() }

// Fix 3: Disable for incompatible plugins
gradle.properties:
org.gradle.configuration-cache=false
```

### 6. Task Execution Failure

**Error:**
```
Execution failed for task ':taskName'
```

**Diagnosis:**
```bash
# Run with stack trace
gradle taskName --stacktrace

# Run with full debugging
gradle taskName --debug > debug.log
```

**Fixes:**
```kotlin
// Fix 1: Add proper task inputs
tasks.register<MyTask>("myTask") {
    inputs.files(fileTree("src"))
    outputs.dir("build/output")
}

// Fix 2: Handle task dependencies
tasks.named("myTask") {
    dependsOn("otherTask")
}

// Fix 3: Configure task properly
tasks.withType<JavaCompile>().configureEach {
    options.isIncremental = true
}
```

## Active Fixes vs Passive Suggestions

### Active Fixes (Automated)

Claude will automatically apply fixes for:
- **Formatting issues** (checkstyle, spotless)
- **Missing dependencies** (clear error messages)
- **Simple configuration** (adding repositories)
- **Gradle wrapper updates**
- **Property file updates**

### Passive Suggestions (Manual)

Claude will provide guidance for:
- **Complex refactoring** (architecture changes)
- **Plugin migrations** (require testing)
- **API updates** (potential breaking changes)
- **Performance tuning** (needs profiling)
- **Custom task logic** (business logic)

## Diagnostic Commands

```bash
# Full dependency tree
gradle dependencies > deps.txt

# Build scan for detailed analysis
gradle build --scan

# Performance profile
gradle build --profile

# Daemon status
gradle --status

# Configuration cache report
gradle build --configuration-cache

# Dependency verification
gradle dependencies --write-verification-metadata sha256

# Clean and rebuild
gradle clean build --rerun-tasks
```

## Build Failure Patterns

### Pattern 1: "Task not found"

**Fix:**
```bash
# List available tasks
gradle tasks --all

# Verify task name spelling
gradle help --task taskName
```

### Pattern 2: "Plugin not found"

**Fix:**
```kotlin
// Add plugin repository
settings.gradle.kts:
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// Update plugin version
plugins {
    id("plugin.id") version "latest.version"
}
```

### Pattern 3: "Circular dependency"

**Fix:**
```kotlin
// Identify cycle
gradle dependencies --configuration compileClasspath

// Break cycle with api/implementation
dependencies {
    api(project(":module-a"))  // Exposed in API
    implementation(project(":module-b"))  // Internal
}
```

## Troubleshooting Checklist

- [ ] Check Gradle version compatibility
- [ ] Verify Java version matches requirements
- [ ] Review recent dependency changes
- [ ] Check plugin versions
- [ ] Clean build (`gradle clean`)
- [ ] Stop and restart daemon
- [ ] Run with `--stacktrace` and `--info`
- [ ] Check build scan for insights
- [ ] Review recent code changes
- [ ] Verify network connectivity (for dependencies)

## Emergency Commands

```bash
# Nuclear option: clean everything
gradle clean
rm -rf .gradle/
rm -rf build/
gradle --stop

# Rebuild from scratch
gradle clean build --rerun-tasks --no-build-cache

# Update dependencies
gradle build --refresh-dependencies

# Verbose debugging
gradle build --stacktrace --info --debug > debug.log 2>&1
```

## Best Practices

1. **Enable Stack Traces:** Always use `--stacktrace` for errors
2. **Use Build Scans:** Leverage gradle.com build scans
3. **Check Recent Changes:** Review git diff before troubleshooting
4. **Incremental Debugging:** Fix one issue at a time
5. **Document Solutions:** Keep notes on fixes for future reference
6. **Update Regularly:** Keep Gradle and plugins up-to-date
7. **Monitor Logs:** Review daemon and build logs regularly
