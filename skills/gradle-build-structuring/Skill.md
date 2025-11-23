---
name: gradle-build-structuring
description: Organizes multi-module Gradle projects, implements build conventions, and structures composite builds for optimal project layout.
version: 1.0.0
---

# Gradle Build Structuring

Organize Gradle projects effectively.

## When to Use

Invoke when users ask about:
- Project organization
- Multi-module setup
- Build structure
- Module organization
- Composite builds

## Project Types

### Single-Module
```
my-project/
├── src/
├── build.gradle.kts
└── settings.gradle.kts
```

### Multi-Module (Flat)
```
my-project/
├── app/
├── lib-core/
├── lib-utils/
└── settings.gradle.kts
```

### Multi-Module (Hierarchical)
```
my-project/
├── apps/
│   └── web-app/
├── libs/
│   └── core/
└── settings.gradle.kts
```

## settings.gradle.kts

```kotlin
rootProject.name = "my-project"

// Flat
include(":app")
include(":lib-core")

// Hierarchical
include(":apps:web-app")
include(":libs:core")
```

## Build Conventions

```kotlin
// buildSrc/src/main/kotlin/java-conventions.gradle.kts
plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
```

## Version Catalog

```toml
# gradle/libs.versions.toml
[versions]
junit = "5.10.1"

[libraries]
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
```

## Best Practices

1. Use buildSrc for conventions
2. Leverage version catalogs
3. API vs implementation correctly
4. Logical module grouping
5. Enable parallel builds
