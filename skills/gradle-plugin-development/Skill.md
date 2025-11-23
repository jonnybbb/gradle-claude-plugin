---
name: gradle-plugin-development
description: Develops custom Gradle plugins including convention plugins, precompiled scripts, and binary plugins with extension modeling.
version: 1.0.0
---

# Gradle Plugin Development

Develop custom Gradle plugins for shared build logic.

## When to Use

Invoke when users want to:
- Create gradle plugins
- Implement convention plugins
- Develop precompiled script plugins
- Add plugin extensions
- Organize build logic

## Convention Plugins (buildSrc)

### Directory Structure
```
buildSrc/
├── build.gradle.kts
└── src/main/kotlin/
    └── java-conventions.gradle.kts
```

### buildSrc/build.gradle.kts
```kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}
```

### Convention Plugin
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

### Usage
```kotlin
plugins {
    id("java-conventions")
}
```

## Binary Plugins

```kotlin
abstract class MyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create<MyExtension>("myPlugin")

        project.tasks.register<MyTask>("myTask") {
            config.set(extension.setting)
        }
    }
}
```

## Best Practices

1. Use convention plugins in buildSrc/ for shared config
2. Type-safe configuration with extensions
3. Lazy task registration
4. Test with Gradle TestKit
