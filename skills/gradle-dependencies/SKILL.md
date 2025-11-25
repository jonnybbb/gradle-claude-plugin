---
name: gradle-dependencies
description: This skill should be used when the user asks to "add a dependency", "resolve version conflicts", "set up version catalog", "use a BOM", "fix dependency issues", "check dependencies", or mentions libs.versions.toml, dependency constraints, transitive dependencies, or "could not find" dependency errors.
---

# Gradle Dependencies

## Overview

Gradle's dependency management resolves, downloads, and manages library versions. Proper configuration prevents conflicts and ensures reproducible builds.

For version catalogs, see [references/catalogs.md](references/catalogs.md).
For conflict resolution, see [references/conflicts.md](references/conflicts.md).

## Quick Start

### Add Dependencies

```kotlin
dependencies {
    implementation("com.google.guava:guava:31.1-jre")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}
```

### Configuration Types

| Configuration | Visible to Consumers | Runtime |
|---------------|---------------------|---------|
| implementation | No | Yes |
| api | Yes | Yes |
| compileOnly | No | No |
| runtimeOnly | No | Yes |
| testImplementation | No | Test only |

## Version Catalogs

```toml
# gradle/libs.versions.toml
[versions]
guava = "31.1-jre"

[libraries]
guava = { module = "com.google.guava:guava", version.ref = "guava" }
```

```kotlin
dependencies {
    implementation(libs.guava)
}
```

## Resolve Conflicts

```kotlin
// Force version
configurations.all {
    resolutionStrategy.force("com.google.guava:guava:31.1-jre")
}

// Or use constraint (preferred)
dependencies {
    constraints {
        implementation("com.google.guava:guava:31.1-jre")
    }
}
```

## Diagnosis

```bash
# View all dependencies
./gradlew dependencies

# Why this version?
./gradlew dependencyInsight --dependency guava
```

## Quick Reference

| Problem | Solution |
|---------|----------|
| "Could not find" | Check repositories |
| Version conflict | Use constraint or force |
| Duplicate classes | Exclude transitive |
| NoSuchMethodError | Check runtime classpath |

## Related Files

- [references/catalogs.md](references/catalogs.md) - Version catalog setup
- [references/conflicts.md](references/conflicts.md) - Conflict resolution patterns
- [references/platforms.md](references/platforms.md) - Platform/BOM usage
