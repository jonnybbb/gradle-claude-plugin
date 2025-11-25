# Kotlin DSL Patterns

**Gradle Version**: 7.0+, optimized for 8+

## Overview

Common patterns and idioms for Gradle Kotlin DSL.

## Applying Plugins

```kotlin
plugins {
    java
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("com.example.plugin")
}
```

## Dependencies

```kotlin
dependencies {
    // Single line
    implementation("com.google.guava:guava:31.1-jre")
    
    // From version catalog
    implementation(libs.guava)
    
    // Project dependency
    implementation(project(":lib"))
    
    // Multiple
    implementation("group:artifact:version")
    testImplementation("junit:junit:4.13.2")
}
```

## Task Configuration

```kotlin
// Configure existing task
tasks.named<Test>("test") {
    useJUnitPlatform()
}

// Register new task
tasks.register<Copy>("copyFiles") {
    from("src")
    into("dest")
}

// With type inference
tasks.register("build") {
    dependsOn("compile", "test")
}
```

## Custom Properties

```kotlin
// Define
val myProperty by extra("value")

// Read
val value = extra["myProperty"] as String

// Or with delegate
val myProp: String by extra
```

## Source Sets

```kotlin
sourceSets {
    main {
        java.srcDir("src/main/java")
        resources.srcDir("src/main/resources")
    }
    test {
        java.srcDir("src/test/java")
    }
}
```

## Configurations

```kotlin
configurations {
    // Create new
    val integrationTest by creating {
        extendsFrom(testImplementation.get())
    }
    
    // Configure existing
    all {
        resolutionStrategy {
            force("com.google.guava:guava:31.1-jre")
        }
    }
}
```

## Conditional Logic

```kotlin
if (project.hasProperty("production")) {
    dependencies {
        runtimeOnly("com.example:prod-lib:1.0")
    }
}

when (project.name) {
    "app" -> {
        plugins.apply("application")
    }
    "lib" -> {
        plugins.apply("java-library")
    }
}
```

## Extension Functions

```kotlin
fun Project.customFunction() {
    println("Custom function for ${project.name}")
}

// Usage
customFunction()
```

## Provider API

```kotlin
val myProvider = providers.gradleProperty("my.property")
    .orElse("default")

tasks.register("showValue") {
    val value = myProvider
    doLast {
        println(value.get())
    }
}
```

## Type-Safe Accessors

```kotlin
// For plugins
plugins {
    `java-library`
    `maven-publish`
}

// For tasks
tasks.test {
    useJUnitPlatform()
}

// For extensions
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
```

## Multi-Line Strings

```kotlin
tasks.register("generate") {
    doLast {
        val code = """
            package com.example;
            
            public class Generated {
                // Generated code
            }
        """.trimIndent()
        
        file("Generated.java").writeText(code)
    }
}
```

## Quick Reference

```kotlin
plugins { java }

repositories { mavenCentral() }

dependencies {
    implementation("group:artifact:version")
    testImplementation(libs.junit)
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
```
