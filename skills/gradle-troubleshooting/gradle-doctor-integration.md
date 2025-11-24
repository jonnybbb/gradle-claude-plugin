# Gradle Doctor Integration

## Overview

[Gradle Doctor](https://runningcode.github.io/gradle-doctor/) is a diagnostic plugin that provides automated build health checks and performance insights. Integration with this plugin enables AI-driven troubleshooting based on concrete diagnostic data.

## Diagnostic Capabilities

### 1. Java Configuration Validation
**What it checks:**
- JAVA_HOME correctness
- Java version compatibility
- Toolchain configuration

**Example output:**
```
⚠️  JAVA_HOME is set to /invalid/path
   Recommendation: Set JAVA_HOME to a valid JDK installation
```

**AI Integration:**
- Parse JAVA_HOME warnings
- Suggest valid JDK paths based on system detection
- Recommend gradle.properties toolchain configuration

### 2. Garbage Collection Monitoring
**What it checks:**
- GC overhead during build
- Inefficient GC configuration
- Parallel GC usage (deprecated)

**Example output:**
```
⚠️  Build spent 15% of time in GC
   Recommendation: Increase heap size in gradle.properties
```

**AI Integration:**
- Analyze GC overhead percentage
- Calculate optimal heap size based on project size
- Suggest org.gradle.jvmargs tuning

### 3. Repository Connection Speed
**What it checks:**
- Slow Maven/Gradle repository connections
- Network latency issues
- Missing repository mirrors

**Example output:**
```
⚠️  Maven Central connection took 5000ms
   Recommendation: Consider using a repository mirror
```

**AI Integration:**
- Identify slow repositories
- Suggest repository mirrors (Maven Central → Google Mirror, etc.)
- Recommend local repository cache configuration

### 4. Build Cache Performance
**What it checks:**
- Cache hit rates
- Cache configuration issues
- Non-cacheable tasks

**Example output:**
```
ℹ️  Build cache hit rate: 45%
   Recommendation: Enable remote build cache for better performance
```

**AI Integration:**
- Analyze cache hit rate trends
- Identify tasks that should be cacheable but aren't
- Suggest cacheIf configuration for custom tasks

### 5. Platform Compatibility
**What it checks:**
- Apple Silicon (M1/M2/M3) compatibility
- Rosetta 2 emulation detection
- Architecture mismatches

**Example output:**
```
⚠️  Running under Rosetta 2 emulation
   Recommendation: Use ARM-native Java distribution
```

**AI Integration:**
- Detect platform-specific issues
- Recommend native Java distributions (Azul Zulu ARM, etc.)
- Suggest architecture-specific dependency variants

### 6. Build Hygiene
**What it checks:**
- Empty source directories
- Unused dependencies
- Dagger annotation processor timing

**Example output:**
```
⚠️  Found 3 empty directories in src/
   Recommendation: Remove empty directories to improve scan time
```

**AI Integration:**
- Automated cleanup suggestions
- Dependency pruning recommendations
- Annotation processor optimization

### 7. Test Caching Issues
**What it checks:**
- Test tasks that should be cached but aren't
- Test configuration problems
- Flaky test detection

**AI Integration:**
- Identify test caching opportunities
- Suggest test configuration improvements
- Analyze test execution patterns

## Integration Architecture

### Level 1: Passive Analysis
**Use gradle-doctor output as diagnostic input:**

1. User runs `gradle build` with gradle-doctor applied
2. gradle-doctor prints diagnostics to console
3. AI parses console output to extract warnings
4. AI provides targeted recommendations based on warnings

**Implementation:**
- JBang script to execute build and capture gradle-doctor output
- Parser for gradle-doctor warning format
- Mapping of warnings → AI recommendation templates

### Level 2: Active Diagnostics
**Programmatically invoke gradle-doctor checks:**

1. AI detects build issues
2. AI invokes gradle-doctor via Gradle Tooling API
3. AI receives structured diagnostic data
4. AI generates fixes based on diagnostic results

**Implementation:**
- Gradle Tooling API integration
- Custom listener for gradle-doctor events
- Structured data extraction

### Level 3: Automated Fixes
**AI applies fixes automatically:**

1. gradle-doctor identifies issue
2. AI determines fix confidence (high/medium/low)
3. High confidence → automatic fix
4. Medium/low confidence → suggest fix for user approval

**Fix Examples:**
- **High confidence**: Increase heap size, fix JAVA_HOME
- **Medium confidence**: Add repository mirrors, enable remote cache
- **Low confidence**: Complex GC tuning, architecture migrations

## Configuration

### Applying gradle-doctor to Projects

**Kotlin DSL (settings.gradle.kts):**
```kotlin
plugins {
    id("com.osacky.doctor") version "0.10.0"
}

doctor {
    // Fail build on critical issues
    failOnEmptyDirectories.set(true)

    // Warn on suboptimal configuration
    warnWhenNotUsingParallelGC.set(true)

    // Java home validation
    javaHome {
        ensureJavaHomeMatches.set(true)
        ensureJavaHomeIsSet.set(true)
    }

    // GC overhead threshold
    GCWarningThreshold.set(0.10) // 10%

    // Repository connection timeout
    downloadSpeedWarningThreshold.set(0.5f) // 0.5 MB/s
}
```

**Groovy DSL (settings.gradle):**
```groovy
plugins {
    id 'com.osacky.doctor' version '0.10.0'
}

doctor {
    failOnEmptyDirectories = true
    warnWhenNotUsingParallelGC = true

    javaHome {
        ensureJavaHomeMatches = true
        ensureJavaHomeIsSet = true
    }

    GCWarningThreshold = 0.10
    downloadSpeedWarningThreshold = 0.5f
}
```

### Adjusting Thresholds

**Conservative (fewer warnings):**
```kotlin
doctor {
    GCWarningThreshold.set(0.20) // Warn only if >20% GC time
    downloadSpeedWarningThreshold.set(0.1f) // Only very slow connections
}
```

**Aggressive (more warnings):**
```kotlin
doctor {
    GCWarningThreshold.set(0.05) // Warn at 5% GC time
    downloadSpeedWarningThreshold.set(1.0f) // Warn on connections <1 MB/s
}
```

## AI Recommendation Patterns

### Pattern 1: GC Overhead → Heap Size
```
gradle-doctor: ⚠️ Build spent 18% of time in GC

AI Analysis:
- Current heap: Unknown (default ~512MB)
- Project size: 45 modules, 125k LOC
- Recommended heap: 4GB

AI Action:
Edit gradle.properties:
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError
```

### Pattern 2: Slow Repository → Mirror
```
gradle-doctor: ⚠️ Maven Central connection: 5200ms

AI Analysis:
- Repository: Maven Central
- Location: US West Coast
- Recommended mirror: Google Maven Mirror

AI Action:
Edit settings.gradle.kts:
dependencyResolutionManagement {
    repositories {
        google()  // Fast mirror for Maven Central artifacts
        mavenCentral()
    }
}
```

### Pattern 3: Cache Hit Rate → Remote Cache
```
gradle-doctor: ℹ️ Build cache hit rate: 42%

AI Analysis:
- Current: Local cache only
- Team size: 8 developers
- CI/CD: GitHub Actions
- Recommended: Remote cache (Gradle Enterprise or custom)

AI Action:
Suggest remote cache setup with configuration examples
```

### Pattern 4: Empty Directories → Cleanup
```
gradle-doctor: ⚠️ Found 5 empty directories

AI Analysis:
- Empty dirs: src/test/resources, src/main/assets, etc.
- Impact: Slower directory scanning

AI Action:
High confidence automatic fix:
rm -rf src/test/resources src/main/assets ...
```

## Build Scan Integration

gradle-doctor integrates with Gradle Build Scans by adding custom tags:

```
Build Scan tags added by gradle-doctor:
- doctor-gc-warning (if GC overhead high)
- doctor-java-home-issue (if JAVA_HOME invalid)
- doctor-slow-repo (if repository connections slow)
```

**AI Integration:**
- Parse Build Scan for gradle-doctor tags
- Correlate tags with build performance data
- Generate comprehensive reports linking issues

## Minimum Requirements

- **Gradle Version**: 6.1.1+ (recommended: 8.0+)
- **Java Version**: 11+ (for gradle-doctor plugin itself)
- **Compatibility**: Works with Configuration Cache and Build Cache

## Error Handling

### gradle-doctor Not Applied
```
AI Detection: No gradle-doctor diagnostics in output

AI Action:
1. Suggest applying gradle-doctor plugin
2. Provide settings.gradle[.kts] snippet
3. Explain benefits of automated diagnostics
```

### gradle-doctor Version Compatibility
```
AI Detection: gradle-doctor version incompatible with Gradle 9.0

AI Action:
1. Check gradle-doctor changelog
2. Recommend compatible version
3. Suggest workarounds if no compatible version exists
```

## Reference Links

- **Documentation**: https://runningcode.github.io/gradle-doctor/
- **GitHub**: https://github.com/runningcode/gradle-doctor
- **Changelog**: https://github.com/runningcode/gradle-doctor/releases
- **Plugin Portal**: https://plugins.gradle.org/plugin/com.osacky.doctor
