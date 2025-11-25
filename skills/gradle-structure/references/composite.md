# Composite Builds

Combine multiple independent builds for development.

## Use Cases

- Develop library and consumer together
- Work across multiple repositories
- Test plugin changes in consuming project
- Share build logic across projects

## Basic Setup

### Structure

```
workspace/
├── my-app/                 # Consumer
│   ├── settings.gradle.kts
│   └── build.gradle.kts
├── my-library/             # Included build
│   ├── settings.gradle.kts
│   └── build.gradle.kts
└── gradle-plugins/         # Included build
    ├── settings.gradle.kts
    └── build.gradle.kts
```

### Include Builds

```kotlin
// my-app/settings.gradle.kts
rootProject.name = "my-app"

// Include sibling projects
includeBuild("../my-library")
includeBuild("../gradle-plugins")
```

## Dependency Substitution

When you include a build, Gradle automatically substitutes dependencies:

```kotlin
// my-app/build.gradle.kts
dependencies {
    // This uses the included build, not Maven
    implementation("com.example:my-library:1.0")
}
```

The substitution happens when:
- Group and name match
- Included build produces matching artifact

### Explicit Substitution

```kotlin
// settings.gradle.kts
includeBuild("../my-library") {
    dependencySubstitution {
        substitute(module("com.example:my-library"))
            .using(project(":"))
    }
}
```

## Plugin Development

### Include Plugin Build

```kotlin
// settings.gradle.kts
pluginManagement {
    includeBuild("../my-gradle-plugin")
}

// Now you can use the plugin
plugins {
    id("com.example.my-plugin")
}
```

### Test Plugin Changes

```bash
# In plugin project
# Make changes...

# In consumer project
./gradlew build
# Uses local plugin changes immediately!
```

## Multi-Repository Setup

```kotlin
// settings.gradle.kts
rootProject.name = "main-app"

// Core libraries
includeBuild("../core-lib") {
    name = "core"  // Rename to avoid conflicts
}

// Shared utilities
includeBuild("../shared-utils")

// Plugin project
pluginManagement {
    includeBuild("../build-plugins")
}
```

## Build Coordination

### Running Tasks Across Builds

```bash
# Run task in included build
./gradlew :my-library:build

# Run all builds
./gradlew build
```

### Task Dependencies

```kotlin
tasks.named("build") {
    dependsOn(gradle.includedBuild("my-library").task(":build"))
}
```

## Composite vs Multi-Project

| Aspect | Composite | Multi-Project |
|--------|-----------|---------------|
| Repositories | Multiple | Single |
| Independence | High | Coupled |
| CI/CD | Separate | Together |
| IDE | Multiple windows | Single window |
| Version control | Separate repos | Single repo |

## Best Practices

1. **Use for development only** - CI should build from published artifacts
2. **Keep builds independent** - Each should build standalone
3. **Match artifact coordinates** - For automatic substitution
4. **Use pluginManagement** - For included plugin builds
5. **Name included builds** - Avoid conflicts

## Troubleshooting

### Substitution Not Working

Check that group:name matches exactly:

```kotlin
// Library publishes as:
group = "com.example"
// with artifact "my-library"

// Consumer must use exact coordinates:
implementation("com.example:my-library:1.0")
```

### Circular Dependencies

Composite builds cannot have circular includes. Structure as DAG.

### Version Conflicts

Included build version always wins over transitive dependencies.
