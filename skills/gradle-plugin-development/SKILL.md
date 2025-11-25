---
name: gradle-plugin-development
description: This skill should be used when the user asks to "create a Gradle plugin", "write a custom plugin", "design plugin extension", "test Gradle plugin", "publish to Plugin Portal", or mentions Plugin<Project>, gradlePlugin block, DSL extensions, convention plugins, or buildSrc plugins.
---

# Gradle Plugin Development

## Overview

Gradle plugins package reusable build logic into shareable units. They can define tasks, extensions, and conventions.

For testing patterns, see [references/testing.md](references/testing.md).
For extension design, see [references/extensions.md](references/extensions.md).

## Quick Start

### Plugin Class

```kotlin
class MyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Create extension
        val extension = project.extensions.create("myPlugin", MyExtension::class.java)
        
        // Register task
        project.tasks.register("myTask", MyTask::class.java) {
            message.set(extension.message)
        }
    }
}

abstract class MyExtension {
    abstract val message: Property<String>
    init { message.convention("Default") }
}
```

### Build Configuration

```kotlin
// build.gradle.kts
plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        create("myPlugin") {
            id = "com.example.my-plugin"
            implementationClass = "com.example.MyPlugin"
        }
    }
}
```

## Convention Plugins (buildSrc)

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

Apply: `plugins { id("java-conventions") }`

## Quick Reference

| Pattern | Use Case |
|---------|----------|
| Binary plugin | Reusable across projects |
| Convention plugin | Shared within project |
| Settings plugin | Multi-project configuration |

## Best Practices

1. Use Property API for configuration
2. Set conventions (defaults) in plugin
3. Use `tasks.register()` for lazy registration
4. Test with both unit and functional tests
5. Support configuration cache

## Related Files

- [references/testing.md](references/testing.md) - Unit and functional testing
- [references/extensions.md](references/extensions.md) - DSL extension patterns
- [references/publishing.md](references/publishing.md) - Publishing to Plugin Portal
