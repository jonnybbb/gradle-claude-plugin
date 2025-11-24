---
description: Comprehensive Gradle build health analysis that checks configuration, performance, dependencies, caching, and provides actionable recommendations for optimization.
---

# Gradle Doctor - Build Health Analysis

Perform a comprehensive analysis of your Gradle build configuration, identify issues, and provide actionable recommendations.

## What This Command Does

1. **Analyzes Build Configuration**: Reviews gradle.properties, build files, and settings
2. **Checks Performance**: Evaluates daemon configuration, caching, and parallelization
3. **Examines Dependencies**: Identifies conflicts, outdated versions, and security issues
4. **Reviews Task Configuration**: Validates task inputs/outputs and cacheability
5. **Tests Build Cache**: Verifies cache configuration and hit rates
6. **Generates Report**: Comprehensive health report with prioritized recommendations

## Usage

```
/doctor
```

Optionally specify scope:
```
/doctor --full          # Complete analysis (default)
/doctor --quick         # Fast check (configuration only)
/doctor --performance   # Focus on performance
/doctor --dependencies  # Focus on dependencies
/doctor --cache         # Focus on caching
```

## Health Check Categories

### 1. ✅ Build Configuration

**Checks:**
- gradle.properties exists and is properly configured
- Gradle wrapper version (should be 8.5+)
- Java version compatibility
- gradle-wrapper.properties integrity

**Example Output:**
```
=== Build Configuration ===

✅ Gradle Version: 8.5
✅ Java Version: 17 (compatible)
✅ gradle.properties found
⚠️  Gradle wrapper distribution: bin (consider all for offline builds)

Recommendations:
1. Update Gradle wrapper to 8.5 for latest features
2. Consider using 'all' distribution for IDE integration
```

### 2. 🚀 Performance Configuration

**Checks:**
- Daemon configuration and heap size
- Parallel build settings
- Build cache configuration
- Configuration cache status
- Worker threads configuration

**Example Output:**
```
=== Performance Configuration ===

✅ Gradle daemon enabled
✅ Parallel builds enabled
✅ Build cache enabled
⚠️  Configuration cache not enabled
❌ Daemon heap size: 2g (recommended: 4g for this project)

Performance Score: 7/10

Recommendations:
1. HIGH: Increase daemon heap size to 4g
   Add to gradle.properties: org.gradle.jvmargs=-Xmx4g
2. MEDIUM: Enable configuration cache for faster builds
   Add to gradle.properties: org.gradle.configuration-cache=true
3. LOW: Consider increasing worker threads to 8 (current: 4)
```

### 3. 📦 Dependency Health

**Checks:**
- Version conflicts
- Outdated dependencies
- Security vulnerabilities (CVEs)
- Transitive dependency depth
- Dependency locking status

**Example Output:**
```
=== Dependency Health ===

✅ No version conflicts detected
⚠️  3 dependencies have updates available
❌ 1 dependency has known security vulnerability

Outdated Dependencies:
- com.google.guava:guava 31.1-jre → 32.1.3-jre
- org.slf4j:slf4j-api 1.7.36 → 2.0.9
- junit:junit 4.13.2 → 5.10.1 (major upgrade)

Security Issues:
- CVE-2023-1234 affects org.example:vulnerable-lib:1.0.0
  Severity: HIGH
  Fixed in: 1.2.0
  Recommendation: Upgrade immediately

Recommendations:
1. CRITICAL: Upgrade vulnerable-lib to 1.2.0+
2. HIGH: Update guava and slf4j to latest versions
3. MEDIUM: Consider migrating from JUnit 4 to JUnit 5
4. LOW: Enable dependency locking for reproducible builds
```

### 4. 💾 Cache Configuration

**Checks:**
- Local build cache configuration
- Remote build cache setup
- Task cacheability
- Cache hit rate (if available)
- Cache directory size

**Example Output:**
```
=== Cache Configuration ===

✅ Local build cache enabled
❌ Remote build cache not configured
⚠️  3 custom tasks not cacheable

Local Cache:
- Location: ~/.gradle/caches/build-cache-1
- Size: 2.3 GB
- Entries: 1,245

Non-Cacheable Tasks:
1. :app:generateBuildInfo
   Missing: @CacheableTask annotation, proper input/output annotations
2. :lib:processResources
   Issue: Uses absolute paths in inputs
3. :app:customTask
   Issue: No input/output declarations

Recommendations:
1. HIGH: Make custom tasks cacheable (50% build time savings potential)
2. MEDIUM: Set up remote build cache for team collaboration
3. LOW: Clean old cache entries (remove entries older than 30 days)
```

### 5. 🏗️ Project Structure

**Checks:**
- Multi-module organization
- buildSrc/ usage
- Version catalog adoption
- Convention plugins
- Module dependency graph

**Example Output:**
```
=== Project Structure ===

Project Type: Multi-module (5 modules)
Modules: :app, :lib-core, :lib-utils, :lib-api, :lib-data

✅ buildSrc/ with convention plugins detected
✅ Version catalog in use (gradle/libs.versions.toml)
⚠️  Module dependency depth: 3 levels (consider flattening)

Module Dependencies:
:app
  └── :lib-core
      ├── :lib-api
      └── :lib-utils
          └── :lib-data

Recommendations:
1. MEDIUM: Consider flattening dependency hierarchy
2. LOW: Review api vs implementation usage in :lib-core
```

### 6. 🎯 Task Configuration

**Checks:**
- Task inputs/outputs properly declared
- Incremental task support
- Task dependencies and ordering
- Unused tasks
- Task execution time

**Example Output:**
```
=== Task Configuration ===

Total Tasks: 127
Custom Tasks: 8
Cacheable Tasks: 5/8 (62%)

Slow Tasks (> 10s):
1. :app:test - 45s
2. :lib-core:compileJava - 15s
3. :app:processResources - 12s

Issues Found:
- :app:generateDocs: Missing @InputFiles annotation
- :lib:customProcess: No @CacheableTask annotation
- :app:assembleReport: Depends on task from different project

Recommendations:
1. HIGH: Add proper input/output annotations to custom tasks
2. MEDIUM: Make slow tasks cacheable
3. LOW: Review cross-project task dependencies
```

### 7. 🔧 Plugin Health

**Checks:**
- Plugin versions
- Plugin compatibility with Gradle version
- Deprecated plugin usage
- Plugin configuration

**Example Output:**
```
=== Plugin Health ===

Applied Plugins: 12
Core Plugins: 6
Community Plugins: 6

✅ All plugins compatible with Gradle 8.5
⚠️  2 plugins have newer versions available
❌ 1 deprecated plugin detected

Plugin Updates Available:
- com.github.johnrengelman.shadow: 8.1.0 → 8.1.1
- org.springframework.boot: 3.1.4 → 3.1.5

Deprecated Plugins:
- maven (replaced by maven-publish)
  Location: build.gradle.kts:3
  Migration: Apply 'maven-publish' instead

Recommendations:
1. HIGH: Replace maven plugin with maven-publish
2. MEDIUM: Update plugins to latest versions
```

## Complete Health Report

```
===============================================
    GRADLE BUILD HEALTH REPORT
===============================================

Project: my-awesome-project
Gradle Version: 8.5
Analysis Date: 2024-01-15 14:30:00

Overall Health Score: 78/100

SUMMARY:
  ✅ 15 checks passed
  ⚠️  8 warnings
  ❌ 3 critical issues

CRITICAL ISSUES (Fix Immediately):
  1. Security vulnerability in org.example:vulnerable-lib
     → Upgrade to version 1.2.0+

  2. Daemon heap size too small for project size
     → Increase to 4g in gradle.properties

  3. Deprecated maven plugin in use
     → Migrate to maven-publish plugin

WARNINGS (Address Soon):
  1. Configuration cache not enabled (-20% potential speedup)
  2. 3 custom tasks not cacheable (-30% potential speedup)
  3. 3 dependencies have updates available
  4. Module dependency hierarchy could be flattened
  5. No remote build cache configured
  6. Some plugins have updates available
  7. Gradle wrapper could be updated
  8. No dependency locking enabled

RECOMMENDATIONS BY PRIORITY:

HIGH PRIORITY:
  □ Fix security vulnerability in vulnerable-lib
  □ Increase daemon heap size to 4g
  □ Replace maven plugin with maven-publish
  □ Make custom tasks cacheable
  □ Update critical dependencies

MEDIUM PRIORITY:
  □ Enable configuration cache
  □ Update plugins to latest versions
  □ Set up remote build cache
  □ Review module dependency structure
  □ Add dependency locking

LOW PRIORITY:
  □ Update Gradle wrapper to latest
  □ Clean old build cache entries
  □ Increase worker threads
  □ Update non-critical dependencies
  □ Consider JUnit 4 → 5 migration

PERFORMANCE POTENTIAL:
  Estimated build time improvement: 35-50%
  - Configuration cache: ~20%
  - Task caching: ~30%
  - Daemon tuning: ~5%

NEXT STEPS:
  1. Run: gradle build --scan
     (Get detailed performance insights)

  2. Apply high-priority fixes first

  3. Re-run /doctor after changes to verify improvements

  4. Gradually address medium and low priority items

===============================================
Report saved to: build/reports/gradle-doctor/health-report.txt
HTML report: build/reports/gradle-doctor/health-report.html
===============================================
```

## Quick Fixes

The `/doctor` command can automatically apply simple fixes:

```
Would you like to apply these quick fixes? (y/n)

QUICK FIXES AVAILABLE:
  1. Update gradle.properties with performance settings
  2. Add version catalog template
  3. Create .gitignore entries for build directories
  4. Add dependency locking configuration

Apply all? (y/n): y

✅ Applied 4 quick fixes
⚠️  Manual fixes required for 3 issues (see report)
```

## Gradle Doctor Plugin Integration

The `/doctor` command integrates with the [gradle-doctor plugin](https://github.com/runningcode/gradle-doctor) to provide enhanced automated diagnostics:

### Applying gradle-doctor

**Add to settings.gradle.kts:**
```kotlin
plugins {
    id("com.osacky.doctor") version "0.10.0"
}

doctor {
    javaHome {
        ensureJavaHomeMatches.set(true)
    }
    GCWarningThreshold.set(0.10)
}
```

### Enhanced Diagnostics with gradle-doctor

When gradle-doctor is applied, `/doctor` provides:

1. **Java Configuration Validation**
   - JAVA_HOME correctness
   - Toolchain compatibility
   - AI recommendations for JDK setup

2. **Garbage Collection Analysis**
   - GC overhead monitoring
   - Automatic heap size recommendations
   - GC algorithm optimization

3. **Repository Performance**
   - Connection speed analysis
   - Mirror suggestions for slow repositories
   - Network troubleshooting

4. **Build Cache Metrics**
   - Cache hit rate tracking
   - Non-cacheable task identification
   - Remote cache recommendations

5. **Platform Compatibility**
   - Apple Silicon (ARM) detection
   - Rosetta 2 warnings
   - Native JDK recommendations

### Example with gradle-doctor Integration

```
=== Gradle Doctor Analysis ===

⚠️  JAVA_HOME issue detected
    Current: /usr/lib/jvm/java-11-openjdk
    Expected: /usr/lib/jvm/java-17-openjdk
    → AI Fix: Updating gradle.properties with correct toolchain

⚠️  GC overhead: 15% of build time
    Current heap: 2g
    Recommended: 4g for project size
    → AI Fix: Increasing heap size in gradle.properties

✅ Repository connections: All under 1000ms
✅ Build cache hit rate: 78% (good)
⚠️  Running under Rosetta 2 emulation
    → Recommendation: Install ARM-native Java distribution
      Download: https://www.azul.com/downloads/?os=macos&architecture=arm-64-bit&package=jdk

Applied Fixes:
  ✅ Updated org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
  ✅ Added java.toolchain.languageVersion=17

Manual Action Required:
  □ Install ARM-native JDK for optimal performance
```

## Integration with Build Scans

```
=== Build Scan Integration ===

Run the following to generate a detailed build scan:

  ./gradlew build --scan

The build scan will provide:
  - Task execution timeline
  - Dependency resolution details
  - Performance bottlenecks
  - Cache effectiveness metrics
  - Configuration time breakdown

After generating scan, run:
  /doctor --analyze-scan <scan-url>

Note: gradle-doctor automatically adds diagnostic tags to Build Scans
```

## Continuous Health Monitoring

Add to CI/CD pipeline:

```yaml
# .github/workflows/gradle-health.yml
name: Gradle Health Check

on: [push, pull_request]

jobs:
  health:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Gradle Doctor
        run: ./gradlew doctor --fail-on-critical
```

## Related

- `/createPlugin` - Create plugins following best practices
- `/createTask` - Create properly configured tasks
- `/reviewTask` - Review specific task implementations
- See `gradle-troubleshooting` skill for detailed debugging
