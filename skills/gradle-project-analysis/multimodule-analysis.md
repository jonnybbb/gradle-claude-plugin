# Multi-Module Project Analysis

## Detecting Multi-Module Projects

Check for `settings.gradle` or `settings.gradle.kts` file in project root.

### Flat Structure
```kotlin
// settings.gradle.kts
rootProject.name = "my-project"

include(":app")
include(":lib-core")
include(":lib-utils")
```

### Hierarchical Structure
```kotlin
// settings.gradle.kts
rootProject.name = "my-project"

include(":apps:web-app")
include(":apps:cli-app")
include(":libs:core")
include(":libs:utils")
```

### Composite Builds
```kotlin
// settings.gradle.kts
includeBuild("../platform")
includeBuild("../shared-libs")
```

## Module Relationship Mapping

### Inter-Module Dependencies
```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":lib-core"))
    api(project(":lib-api"))  // Exposed to consumers
}
```

### Dependency Graph Example
```
:app
  → :lib-core (implementation)
  → :lib-api (api)

:lib-core
  → :lib-utils (implementation)
  → :lib-api (api)
```

## Best Practices

1. Keep module dependencies acyclic (no circular dependencies)
2. Use `api` only for dependencies that are part of module's API
3. Prefer `implementation` to limit transitive dependencies
4. Group related modules in hierarchical structure
5. Use buildSrc/ for shared build logic
