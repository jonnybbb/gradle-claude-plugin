# buildSrc Best Practices

## Overview

`buildSrc` is a special directory that Gradle compiles and adds to the build classpath before evaluating any build scripts. It provides a convenient location for convention plugins and shared build logic.

## Directory Structure

```
my-project/
├── buildSrc/
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       ├── java-conventions.gradle.kts      # Convention plugin
│       └── com/example/CustomTask.kt        # Custom task class
├── app/
│   └── build.gradle.kts
└── settings.gradle.kts
```

## buildSrc build.gradle.kts

```kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    // Add plugin dependencies for convention plugins
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    implementation("org.springframework.boot:spring-boot-gradle-plugin:3.2.0")
}
```

## Convention Plugin Example

```kotlin
// buildSrc/src/main/kotlin/java-conventions.gradle.kts
plugins {
    java
    `maven-publish`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

Apply in subprojects:
```kotlin
plugins {
    id("java-conventions")
}
```

## Best Practices

1. **Prefer convention plugins over script plugins** - Type-safe, testable, IDE support
2. **Keep buildSrc focused** - Only shared logic, not application code
3. **Use version catalogs** - Define versions in `gradle/libs.versions.toml`, not buildSrc
4. **Enable configuration cache** - Test compatibility early
5. **Consider included builds** - For larger codebases, use `includeBuild()` instead

## Migration from allprojects/subprojects

### Before (Legacy)
```kotlin
// root build.gradle.kts
subprojects {
    apply(plugin = "java")

    repositories {
        mavenCentral()
    }
}
```

### After (Convention Plugin)
```kotlin
// buildSrc/src/main/kotlin/java-base.gradle.kts
plugins {
    java
}

repositories {
    mavenCentral()
}
```

```kotlin
// subproject/build.gradle.kts
plugins {
    id("java-base")
}
```

## Testing Convention Plugins

```kotlin
// buildSrc/src/test/kotlin/ConventionPluginTest.kt
class ConventionPluginTest {
    @Test
    fun `java-conventions applies java plugin`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("java-conventions")

        assertThat(project.plugins.hasPlugin(JavaPlugin::class.java)).isTrue()
    }
}
```

## Common Pitfalls

| Issue | Solution |
|-------|----------|
| Slow builds | buildSrc changes trigger full rebuild - consider included builds |
| Circular dependencies | Extract shared code to separate module |
| Missing plugin dependency | Add to buildSrc dependencies block |
| Configuration cache issues | Avoid static state, use providers |
