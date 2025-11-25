---
name: gradle-best-practices
description: This skill should be used when the user asks about "Gradle best practices", "build script conventions", "Kotlin DSL vs Groovy", "plugin application patterns", "gradle.properties usage", "project naming conventions", "avoiding internal APIs", "Gradle upgrade strategies", or wants guidance on writing maintainable, idiomatic Gradle builds.
---

# Gradle Best Practices

## Overview

Apply official Gradle best practices for maintainable, performant, and upgrade-friendly builds. These recommendations help ensure your build scripts are idiomatic, portable, and ready for future Gradle versions.

For detailed examples, see [references/best-practices-details.md](references/best-practices-details.md).

## Quick Reference

| Practice | Recommendation |
|----------|----------------|
| Build script DSL | Prefer Kotlin DSL (`build.gradle.kts`) |
| Plugin application | Use `plugins {}` block, not `apply plugin` |
| Gradle version | Stay on latest minor version |
| Properties | Use `gradle.properties` in root project |
| Root project | Always name it in `settings.gradle.kts` |
| Internal APIs | Never use `*.internal.*` packages |
| Subproject properties | Don't use `gradle.properties` in subprojects |

## 1. Use Kotlin DSL

Prefer `build.gradle.kts` over `build.gradle` for new builds.

**Benefits:**
- Strict typing with better IDE support
- Improved readability
- Single-language stack for Kotlin projects

```kotlin
// settings.gradle.kts
rootProject.name = "my-project"

// build.gradle.kts
plugins {
    id("java")
    id("com.example.plugin") version "1.0.0"
}
```

## 2. Use Latest Minor Version

Stay on the latest minor version of your current major Gradle release.

```bash
# Update Gradle wrapper
./gradlew wrapper --gradle-version 8.14

# Check current version
./gradlew --version
```

**Strategy:**
- Try upgrading directly to latest minor
- If issues arise, upgrade one minor at a time
- Upgrade Gradle before plugins
- Consult changelogs when updating

## 3. Apply Plugins Using `plugins` Block

Always use the `plugins {}` block instead of `apply plugin`.

### Don't Do This

```kotlin
buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.example:plugin:1.0.0")
    }
}
apply(plugin = "java")
apply(plugin = "com.example.plugin")
```

### Do This Instead

```kotlin
plugins {
    id("java")
    id("com.example.plugin") version "1.0.0"
}
```

**Benefits:**
- More concise and less error-prone
- Better plugin class loading and reuse
- Idempotent and side-effect free
- Better tooling support

## 4. Do Not Use Internal APIs

Never use APIs from packages containing `internal` or types with `Internal`/`Impl` suffixes.

### Don't Do This

```kotlin
import org.gradle.api.internal.attributes.AttributeContainerInternal

// Casting to internal types - will break on upgrades
val badMap = (attributes as AttributeContainerInternal).asMap()
```

### Do This Instead

```kotlin
// Use public APIs only
val goodMap = attributes.keySet().associate {
    Attribute.of(it.name, it.type) to attributes.getAttribute(it)
}
```

**Why:**
- Internal APIs change without notice between releases
- Even minor releases may break internal API usage
- Submit feature requests if public API is missing

## 5. Set Build Flags in `gradle.properties`

Set Gradle properties in the root project's `gradle.properties` file, not on command line.

### Don't Do This

```bash
# Relying on command-line properties
./gradlew build -Dorg.gradle.parallel=true -Dorg.gradle.caching=true
```

### Do This Instead

```properties
# gradle.properties (in root project)
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.jvmargs=-Xmx4g
```

**Benefits:**
- Consistent across environments and developers
- Version controlled
- Not forgotten between builds

**Note:** Properties are not inherited in composite builds.

## 6. Name Your Root Project

Always set `rootProject.name` in `settings.gradle.kts`.

### Don't Do This

```kotlin
// settings.gradle.kts
// Left empty - project name derived from directory
```

### Do This Instead

```kotlin
// settings.gradle.kts
rootProject.name = "my-project"

include("app", "lib")
```

**Why:**
- Directory names may contain special characters
- Ensures consistent names in logs, reports, error messages
- Makes task paths reliable across environments

## 7. No `gradle.properties` in Subprojects

Don't place `gradle.properties` files in subproject directories.

### Don't Do This

```
my-project/
├── gradle.properties      # Root properties
├── app/
│   ├── gradle.properties  # Subproject properties - DON'T
│   └── build.gradle.kts
└── lib/
    ├── gradle.properties  # Subproject properties - DON'T
    └── build.gradle.kts
```

### Do This Instead

Keep all properties in the root `gradle.properties` or use extension properties in build scripts.

## Checklist

Use this checklist to verify best practices compliance:

- [ ] Using Kotlin DSL for build scripts
- [ ] On latest minor Gradle version
- [ ] All plugins applied via `plugins {}` block
- [ ] No internal API usage (`*.internal.*`)
- [ ] Build properties in root `gradle.properties`
- [ ] Root project named in `settings.gradle.kts`
- [ ] No `gradle.properties` in subprojects

## Related Resources

- [Gradle Best Practices Documentation](https://docs.gradle.org/current/userguide/best_practices_general.html)
- [Kotlin DSL Primer](https://docs.gradle.org/current/userguide/kotlin_dsl.html)
- [Using Plugins](https://docs.gradle.org/current/userguide/plugins.html)
