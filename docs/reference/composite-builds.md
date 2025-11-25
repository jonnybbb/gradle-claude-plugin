# Composite Builds

**Source**: https://docs.gradle.org/current/userguide/composite_builds.html  
**Gradle Version**: 7.0+

## Overview

Composite builds allow multiple Gradle builds to work together, replacing binary dependencies with project dependencies.

## Basic Setup

**settings.gradle.kts:**
```kotlin
includeBuild("../library")
includeBuild("../common")
```

**Directory structure:**
```
workspace/
├── my-app/
│   ├── settings.gradle.kts  (includes libraries)
│   └── build.gradle.kts
├── library/
│   ├── settings.gradle.kts
│   └── build.gradle.kts
└── common/
    ├── settings.gradle.kts
    └── build.gradle.kts
```

## Dependency Substitution

**Automatic:**
```kotlin
// my-app/build.gradle.kts
dependencies {
    implementation("com.example:library:1.0")  // Replaced with project
}
```

**Explicit:**
```kotlin
// settings.gradle.kts
includeBuild("../library") {
    dependencySubstitution {
        substitute(module("com.example:library"))
            .using(project(":"))
    }
}
```

## Use Cases

### 1. Library Development

Develop library and consumer together:
```kotlin
// app/settings.gradle.kts
includeBuild("../my-library")
```

### 2. Multi-Repository Monorepo

Multiple repos, single build:
```kotlin
includeBuild("../repo1")
includeBuild("../repo2")
includeBuild("../repo3")
```

### 3. Plugin Development

Test plugin in consumer:
```kotlin
// app/settings.gradle.kts
includeBuild("../my-plugin")
```

## Task Execution

```bash
# Run in main build
./gradlew build

# Run in included build
./gradlew :library:build

# Run task across all builds
./gradlew assemble
```

## Benefits

1. **Fast feedback:** Changes immediately available
2. **No publishing:** Skip publish/consume cycle
3. **IDE integration:** Works in IntelliJ/Eclipse
4. **Parallel builds:** Can build independently

## vs Multi-Project Build

| Feature | Composite | Multi-Project |
|---------|-----------|---------------|
| Separate repos | ✅ Yes | ❌ No |
| Independent builds | ✅ Yes | ❌ No |
| Separate versioning | ✅ Yes | ❌ No |
| Build isolation | ✅ Yes | ❌ Shared |

## Quick Reference

```kotlin
// settings.gradle.kts
includeBuild("../library")

// build.gradle.kts
dependencies {
    implementation("com.example:library:1.0")
    // Automatically uses project instead
}
```
