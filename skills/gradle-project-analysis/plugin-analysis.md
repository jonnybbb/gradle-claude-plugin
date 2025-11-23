# Plugin Analysis

## Core Gradle Plugins

- `java` - Basic Java compilation
- `java-library` - Java library with API/implementation separation
- `application` - Java application with run task
- `groovy` - Groovy language support
- `scala` - Scala language support

## Popular Community Plugins

### Spring Boot
```kotlin
plugins {
    id("org.springframework.boot") version "3.1.5"
    id("io.spring.dependency-management") version "1.1.4"
}
```

### Kotlin
```kotlin
plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.spring") version "1.9.20"
}
```

### Android
```kotlin
plugins {
    id("com.android.application") version "8.1.0"
}
```

### Shadow (Fat JAR)
```kotlin
plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}
```

## Extracting Plugin Information

From build files, extract:
1. Plugin ID
2. Plugin version (if specified)
3. Whether it's a core or community plugin
4. Plugin configuration blocks

Example extraction from:
```kotlin
plugins {
    id("org.springframework.boot") version "3.1.5"
}
```

Results in:
- Plugin: org.springframework.boot
- Version: 3.1.5
- Type: Community
