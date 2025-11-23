---
name: gradle-troubleshooting
description: Diagnoses build failures, :check task errors, dependency issues, and provides automated fixes for common Gradle problems.
version: 1.0.0
---

# Gradle Troubleshooting

Diagnose and fix Gradle build failures.

## When to Use

Invoke when users report:
- Build failures
- :check task failures
- Compilation errors
- Test failures
- Dependency resolution errors

## Common Issues & Fixes

### 1. Dependency Resolution
```bash
# Diagnose
gradle dependencyInsight --dependency <lib>

# Fix
repositories {
    mavenCentral()
}
```

### 2. OutOfMemoryError
```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```

### 3. Compilation Errors
```kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
```

### 4. Configuration Cache Issues
```kotlin
// ✅ Good: Lazy task registration
tasks.register("myTask") { }
```

## Diagnostic Commands

```bash
# Stack trace
gradle build --stacktrace

# Detailed logging
gradle build --info

# Build scan
gradle build --scan
```

## Emergency Reset

```bash
gradle clean
rm -rf .gradle/ build/
gradle --stop
gradle build --rerun-tasks
```
