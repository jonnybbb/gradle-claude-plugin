# Configuration Cache Debugging Guide

Complete guide for diagnosing and debugging configuration cache problems.

## HTML Problem Report

Gradle generates a detailed HTML report when configuration cache problems occur.

### Report Location

The report is generated at:

```
build/reports/configuration-cache/<task-hash>/configuration-cache-report.html
```

The exact path is printed in the console output:

```
See the complete report at file:///path/to/project/build/reports/configuration-cache/abcd1234/configuration-cache-report.html
```

### Report Structure

The report contains several sections:

1. **Summary** - Total problem count, build configuration inputs
2. **Problems** - Issues grouped by message, with:
   - Problem description
   - Task or script causing the problem
   - Stack trace showing exact location
   - Link to documentation
3. **Configuration Inputs** - Files, system properties, and environment variables read during configuration
4. **Invalidation Causes** - What changed to cause cache invalidation

### Interpreting the Report

**Problem entry example:**

```
Task `:compileJava` of type `org.gradle.api.tasks.compile.JavaCompile`:
  - invocation of 'Task.project' at execution time is unsupported
    at MyBuildScript$_run_closure1.doCall(build.gradle.kts:27)
```

This tells you:
- **Task**: `:compileJava`
- **Issue**: Accessing `project` during execution
- **Location**: `build.gradle.kts` line 27

### Opening the Report Automatically

```bash
# macOS
open build/reports/configuration-cache/*/configuration-cache-report.html

# Linux
xdg-open build/reports/configuration-cache/*/configuration-cache-report.html

# Windows
start build\reports\configuration-cache\*\configuration-cache-report.html
```

## Build Scan Integration (Develocity)

Build Scans provide additional configuration cache debugging capabilities.

### Enabling Build Scans

```kotlin
// settings.gradle.kts
plugins {
    id("com.gradle.develocity") version "3.17.5"
}

develocity {
    buildScan {
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
        uploadInBackground.set(false)
    }
}
```

### Configuration Cache in Build Scans

Build Scans show:

1. **Configuration Cache Summary** (Performance tab)
   - Cache hit/miss status
   - Time saved by cache reuse
   - Cache entry size

2. **Configuration Cache Problems** (Build tab → Configuration Cache)
   - All problems with full stack traces
   - Problems grouped by type
   - Links to documentation

3. **Configuration Inputs** (Build tab → Configuration Cache)
   - Files read during configuration
   - System properties accessed
   - Environment variables accessed
   - Why cache was invalidated

### Accessing via Develocity MCP Server

If you have the Develocity MCP server configured, you can query build scan data:

```
# Get recent builds with configuration cache issues
mcp__develocity__getBuilds(
  query: "configurationCache:problems>0",
  maxBuilds: 10
)

# Get specific build's configuration cache details
mcp__develocity__getBuild(buildId: "abc123")
```

### Build Scan Comparison

Compare cache behavior between builds:
1. Open two build scans
2. Click "Compare builds"
3. Review "Configuration Cache" differences

## Debug Mode

Enable debug mode for detailed serialization information:

### Enable via Command Line

```bash
./gradlew build --configuration-cache -Dorg.gradle.configuration-cache.debug=true
```

### Enable via gradle.properties

```properties
org.gradle.configuration-cache.debug=true
```

### Debug Output

Debug mode provides:
- Detailed serialization traces
- Object graph information
- Exact serialization failure points
- Memory usage during serialization

Example debug output:

```
Configuration cache state could not be cached:
  Task `:app:processResources` of type `ProcessResources`
    - field `project` of type `DefaultProject` is not serializable
      Stack trace:
        at org.gradle.internal.cc.impl....
        at build_xyz.run(build.gradle.kts:42)
```

## Command Line Options Reference

| Option | Description |
|--------|-------------|
| `--configuration-cache` | Enable configuration cache |
| `--no-configuration-cache` | Disable configuration cache |
| `--configuration-cache-problems=warn` | Report problems but continue |
| `--configuration-cache-problems=fail` | Fail on first problem (default when enabled) |
| `-Dorg.gradle.configuration-cache.debug=true` | Enable debug mode |

### gradle.properties Options

```properties
# Enable configuration cache
org.gradle.configuration-cache=true

# Problem handling: warn, fail (default)
org.gradle.configuration-cache.problems=warn

# Max problems before failing (default: 512)
org.gradle.configuration-cache.max-problems=10

# Read-only mode (don't store new entries)
org.gradle.configuration-cache.read-only=true

# Enable debug mode
org.gradle.configuration-cache.debug=true

# Integrity check (validates serialization)
org.gradle.configuration-cache.integrity-check=true

# Parallel configuration caching (incubating)
org.gradle.configuration-cache.parallel=true
```

## Integrity Check Mode

For subtle serialization issues, enable integrity checking:

```properties
org.gradle.configuration-cache.integrity-check=true
```

This validates that:
- Serialized state can be deserialized correctly
- No data corruption during round-trip
- All references are properly resolved

**When to use:**
- Intermittent cache failures
- Corrupted state suspicion
- After major refactoring

## Common Error Messages

### "Invocation of 'Task.project' at execution time is unsupported"

**Cause:** Task action (doLast/doFirst/@TaskAction) accesses `project`

**Solution:** Capture values during configuration or inject services

```kotlin
// Before
tasks.register("example") {
    doLast {
        println(project.name) // Fails
    }
}

// After
tasks.register("example") {
    val name = project.name // Capture at configuration time
    doLast {
        println(name) // Works
    }
}
```

### "cannot be serialized"

**Cause:** Task holds reference to non-serializable type

**Solution:** Convert to serializable equivalent

| Non-serializable | Serializable Alternative |
|------------------|--------------------------|
| `SourceSet` | `ConfigurableFileCollection` |
| `Configuration` | `ConfigurableFileCollection` |
| `Project` | Capture specific values |
| `Task` | `TaskProvider` |

### "Configuration cache entry reused" followed by failure

**Cause:** Execution-time code assumes configuration ran

**Solution:** Ensure all execution-time dependencies are declared as inputs

### "Configuration cache state could not be cached"

**Cause:** Serialization failed during cache store

**Steps:**
1. Enable debug mode to get stack trace
2. Find the exact field/value that failed
3. Convert to serializable type or capture during configuration

## Debugging Workflow

### Step 1: Enable Warning Mode

```bash
./gradlew build --configuration-cache-problems=warn
```

### Step 2: Review HTML Report

Open the generated report and categorize problems:
- Quick fixes (property captures, provider API)
- Complex fixes (service injection, task redesign)
- Third-party plugin issues

### Step 3: Enable Debug Mode for Complex Issues

```bash
./gradlew build --configuration-cache -Dorg.gradle.configuration-cache.debug=true 2>&1 | tee cc-debug.log
```

### Step 4: Check Build Scan for Full Context

```bash
./gradlew build --configuration-cache --scan
```

Review the build scan for:
- Full problem list with stack traces
- Configuration inputs
- Timing information

### Step 5: Test Incrementally

After each fix:

```bash
# Clear cache
rm -rf .gradle/configuration-cache

# Test with verbose output
./gradlew build --configuration-cache --info
```

### Step 6: Verify Cache Reuse

```bash
# Run twice
./gradlew build --configuration-cache
./gradlew build --configuration-cache

# Look for: "Reusing configuration cache."
```

## Invalidating Cache Manually

When debugging, you may need to clear the cache:

```bash
# Delete configuration cache entries
rm -rf .gradle/configuration-cache

# Or use Gradle
./gradlew --stop  # Stop daemon first
rm -rf .gradle/configuration-cache
```

Gradle automatically cleans entries older than 7 days.

## Getting Help

1. **HTML Report** - First stop for problem identification
2. **Build Scan** - Full context and comparison capabilities
3. **Debug Mode** - Detailed serialization traces
4. **Gradle Forums** - Community support
5. **Gradle Issue Tracker** - Bug reports

## Related

- [Configuration Cache Reference](https://docs.gradle.org/current/userguide/configuration_cache.html)
- [Configuration Cache Debugging](https://docs.gradle.org/current/userguide/configuration_cache_debugging.html)
- [Build Scan Configuration Cache](https://docs.gradle.com/enterprise/build-scans/#configuration_cache)
