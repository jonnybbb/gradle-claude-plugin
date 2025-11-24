---
name: gradle-troubleshooting
description: Diagnoses build failures, :check task errors, dependency issues, and provides automated fixes for common Gradle problems.
version: 1.0.0
---

# Gradle Troubleshooting

Diagnose and fix Gradle build failures with AI-powered analysis and automated fixes.

## When to Use

Invoke when users report:
- Build failures or errors
- :check task failures (checkstyle, lint, errorprone)
- Compilation errors
- Test failures or flaky tests
- Dependency resolution errors
- Performance degradation
- Configuration cache issues

## Quick Diagnostic Commands

```bash
# Generate build scan for detailed analysis
gradle build --scan

# Show stack trace
gradle build --stacktrace

# Detailed logging
gradle build --info

# Full debug output
gradle build --debug
```

## Common Quick Fixes

### OutOfMemoryError
```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```

### Java Version Mismatch
```kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
```

### Missing Repository
```kotlin
repositories {
    mavenCentral()
    google()
}
```

## Reference Files

### Comprehensive Error Solutions
See `common-errors.md` for detailed solutions to:
- Dependency resolution errors
- Memory errors (heap space, metaspace)
- Compilation errors
- Task execution errors
- Configuration cache errors
- Build cache errors
- Plugin errors
- Test execution errors
- Emergency reset procedures

### Advanced Diagnostics
See `diagnostic-techniques.md` for:
- Build scans analysis
- Profiling builds
- Debugging build scripts
- Network diagnostics
- Gradle daemon diagnostics
- Build environment diagnostics
- Root cause analysis workflows

### Gradle Doctor Integration
See `gradle-doctor-integration.md` for:
- Automated build health checks
- AI-driven troubleshooting based on gradle-doctor output
- Java configuration validation
- GC monitoring and optimization
- Repository performance analysis
- Build cache performance tracking
- Platform compatibility checks
- Integration patterns (passive analysis, active diagnostics, automated fixes)

## Troubleshooting Workflow

1. **Capture Error**: Get full stack trace and build output
2. **Quick Analysis**: Check for common patterns in error message
3. **Deep Diagnostics**: Generate build scan or use gradle-doctor
4. **Apply Fix**:
   - High confidence → Automatic fix
   - Medium confidence → Suggest fix for user approval
   - Low confidence → Provide multiple options with explanations
5. **Verify**: Test fix with `gradle build --dry-run` then full build
6. **Document**: Record issue and resolution for future reference

## Emergency Reset

```bash
gradle --stop
gradle clean
rm -rf .gradle/ build/
gradle build --rerun-tasks
```
