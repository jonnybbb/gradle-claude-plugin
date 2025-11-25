# buildSrc and Convention Plugins

**Gradle Version**: 7.0+

## Overview

buildSrc provides custom build logic and convention plugins for your project.

## Structure

```
project/
├── buildSrc/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── src/main/kotlin/
│       ├── java-conventions.gradle.kts
│       ├── kotlin-conventions.gradle.kts
│       └── CustomPlugin.kt
└── app/
    └── build.gradle.kts
```

## Creating Convention Plugin

**buildSrc/src/main/kotlin/java-conventions.gradle.kts:**
```kotlin
plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnitPlatform()
}
```

## Applying Convention

**app/build.gradle.kts:**
```kotlin
plugins {
    id("java-conventions")
}

dependencies {
    implementation("com.google.guava:guava:31.1-jre")
}
```

## Custom Plugin in buildSrc

**buildSrc/src/main/kotlin/CustomPlugin.kt:**
```kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project

class CustomPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("customTask") {
            doLast {
                println("Custom task executed")
            }
        }
    }
}
```

**Apply:**
```kotlin
plugins {
    id("CustomPlugin")  // No version needed
}
```

## buildSrc build.gradle.kts

```kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:8.1.0")
}
```

## Benefits

1. **Type-safe:** Compile-time checking
2. **IDE support:** Auto-completion
3. **Reusable:** Across subprojects
4. **Versioned:** With your project
5. **Fast:** No network/resolution

## vs Separate Plugin

| Feature | buildSrc | Separate Plugin |
|---------|----------|-----------------|
| Setup | Simple | Complex |
| Reuse | Single project | Multiple projects |
| Publishing | No | Yes |
| Versioning | Project version | Independent |
| Updates | Automatic | Manual |

## Quick Reference

```kotlin
// buildSrc/src/main/kotlin/my-conventions.gradle.kts
plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// app/build.gradle.kts
plugins {
    id("my-conventions")
}
```
