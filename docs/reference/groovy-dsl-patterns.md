# Groovy DSL Patterns

**Gradle Version**: 7.0+

## Overview

Common patterns for Gradle Groovy DSL.

## Applying Plugins

```groovy
plugins {
    id 'java'
    id 'org.jetbrains.kotlin.jvm' version '1.9.21'
}
```

## Dependencies

```groovy
dependencies {
    implementation 'com.google.guava:guava:31.1-jre'
    implementation libs.guava
    implementation project(':lib')
    testImplementation 'junit:junit:4.13.2'
}
```

## Task Configuration

```groovy
// Configure existing
test {
    useJUnitPlatform()
}

// Register new
tasks.register('copyFiles', Copy) {
    from 'src'
    into 'dest'
}
```

## Custom Properties

```groovy
ext {
    myProperty = 'value'
    anotherProperty = 'another'
}

// Read
def value = project.ext.myProperty
```

## Source Sets

```groovy
sourceSets {
    main {
        java.srcDir 'src/main/java'
        resources.srcDir 'src/main/resources'
    }
}
```

## Conditional Logic

```groovy
if (project.hasProperty('production')) {
    dependencies {
        runtimeOnly 'com.example:prod-lib:1.0'
    }
}
```

## Quick Reference

```groovy
plugins { id 'java' }
repositories { mavenCentral() }

dependencies {
    implementation 'group:artifact:version'
}

test {
    useJUnitPlatform()
}
```
