# Multi-Project Build Patterns

Detailed patterns for organizing multi-project Gradle builds.

## Basic Structure

```
my-app/
├── settings.gradle.kts
├── build.gradle.kts          # Root build (optional)
├── gradle.properties
├── gradle/
│   └── libs.versions.toml    # Version catalog
├── app/
│   ├── build.gradle.kts
│   └── src/
├── core/
│   ├── build.gradle.kts
│   └── src/
└── shared/
    ├── build.gradle.kts
    └── src/
```

## Settings File

```kotlin
// settings.gradle.kts
rootProject.name = "my-app"

// Simple includes
include("app")
include("core")
include("shared")

// Nested projects
include("backend:api")
include("backend:service")
include("frontend:web")
include("frontend:mobile")

// Custom project directory
include("my-lib")
project(":my-lib").projectDir = file("libs/my-library")
```

## Root Build File

```kotlin
// build.gradle.kts (root)
plugins {
    // Apply but don't activate
    java apply false
    kotlin("jvm") version "1.9.21" apply false
}

// Shared configuration for ALL projects (including root)
allprojects {
    group = "com.example"
    version = "1.0.0"
    
    repositories {
        mavenCentral()
    }
}

// Configuration for subprojects only
subprojects {
    // Common plugins
    apply(plugin = "java-library")
    
    // Common dependencies
    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    }
    
    tasks.test {
        useJUnitPlatform()
    }
}
```

## Project Dependencies

```kotlin
// app/build.gradle.kts
plugins {
    application
}

dependencies {
    implementation(project(":core"))
    implementation(project(":shared"))
}

application {
    mainClass.set("com.example.Main")
}
```

## Typed Project Dependencies

```kotlin
// Better: type-safe project dependencies
dependencies {
    // Access other project's configurations
    implementation(project(":core"))
    
    // Test fixtures from another project
    testImplementation(testFixtures(project(":core")))
    
    // Specific configuration
    compileOnly(project(":api", "apiElements"))
}
```

## Avoiding Cross-Project Configuration

```kotlin
// ❌ Bad: Accessing other project's internals
val coreVersion = project(":core").version
val coreSource = project(":core").sourceSets.main

// ✅ Good: Use shared properties
val coreVersion = providers.gradleProperty("core.version")

// ✅ Good: Use project dependencies
dependencies {
    implementation(project(":core"))
}
```

## Sharing Configuration

### Option 1: Convention Plugins (Recommended)

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

### Option 2: Shared Properties

```properties
# gradle.properties
javaVersion=17
kotlinVersion=1.9.21
```

```kotlin
// Any build.gradle.kts
val javaVersion: String by project
```

### Option 3: Extra Properties

```kotlin
// Root build.gradle.kts
extra["springVersion"] = "6.1.0"

// Subproject build.gradle.kts
val springVersion: String by rootProject.extra
```

## Task Dependencies Across Projects

```kotlin
// app/build.gradle.kts
tasks.named("build") {
    // Depend on task in another project
    dependsOn(":core:build")
}

// Or using project dependency (preferred)
dependencies {
    implementation(project(":core"))
}
// Gradle automatically orders tasks correctly
```

## Parallel Execution

```properties
# gradle.properties
org.gradle.parallel=true
```

Ensure projects don't have hidden dependencies for safe parallelization.

## Project Isolation (Gradle 9+)

```properties
# Enable strict project isolation
org.gradle.unsafe.isolated-projects=true
```

Prevents cross-project configuration access.
