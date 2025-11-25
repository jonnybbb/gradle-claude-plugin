# Platforms and BOMs

Align dependency versions across projects.

## What is a Platform?

A platform is a set of dependency constraints without actual code. It ensures consistent versions across modules.

## Using Platforms

### Import Platform (BOM)

```kotlin
dependencies {
    // Import platform - versions are recommendations
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.0"))
    
    // Use dependencies without versions
    implementation("org.springframework:spring-core")
    implementation("org.springframework:spring-context")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
```

### Enforce Platform

```kotlin
dependencies {
    // Enforce platform - overrides transitive versions
    implementation(enforcedPlatform("org.springframework.boot:spring-boot-dependencies:3.2.0"))
}
```

## Common Platforms

### Spring Boot

```kotlin
implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.0"))
```

### Jackson

```kotlin
implementation(platform("com.fasterxml.jackson:jackson-bom:2.16.0"))
```

### JUnit 5

```kotlin
testImplementation(platform("org.junit:junit-bom:5.10.0"))
testImplementation("org.junit.jupiter:junit-jupiter")
```

### AWS SDK

```kotlin
implementation(platform("software.amazon.awssdk:bom:2.21.0"))
implementation("software.amazon.awssdk:s3")
implementation("software.amazon.awssdk:dynamodb")
```

## Creating Your Own Platform

### Platform Project

```kotlin
// platform/build.gradle.kts
plugins {
    `java-platform`
}

dependencies {
    constraints {
        api("com.google.guava:guava:31.1-jre")
        api("org.apache.commons:commons-lang3:3.14.0")
        api("org.slf4j:slf4j-api:2.0.9")
        
        // Group related libraries
        api("com.fasterxml.jackson.core:jackson-core:2.16.0")
        api("com.fasterxml.jackson.core:jackson-databind:2.16.0")
        api("com.fasterxml.jackson.core:jackson-annotations:2.16.0")
    }
}
```

### Use in Projects

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(platform(project(":platform")))
    
    // No versions needed
    implementation("com.google.guava:guava")
    implementation("org.slf4j:slf4j-api")
}
```

## Platform with Transitives

```kotlin
// platform/build.gradle.kts
plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()  // Allow importing other platforms
}

dependencies {
    // Import other platforms
    api(platform("org.springframework.boot:spring-boot-dependencies:3.2.0"))
    
    // Add your constraints
    constraints {
        api("com.example:internal-lib:1.0.0")
    }
}
```

## Publishing Platforms

```kotlin
// platform/build.gradle.kts
plugins {
    `java-platform`
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("platform") {
            from(components["javaPlatform"])
        }
    }
}
```

## Platform vs Version Catalog

| Aspect | Platform | Version Catalog |
|--------|----------|-----------------|
| Scope | Runtime resolution | Build-time reference |
| Transitives | Affects transitives | No effect |
| Type | Dependency | Build configuration |
| Publishing | As Maven BOM | Not publishable |
| Use case | Version alignment | Version centralization |

### Use Both Together

```toml
# gradle/libs.versions.toml
[libraries]
spring-bom = "org.springframework.boot:spring-boot-dependencies:3.2.0"
spring-core = { module = "org.springframework:spring-core" }
```

```kotlin
dependencies {
    implementation(platform(libs.spring.bom))
    implementation(libs.spring.core)  // No version, from platform
}
```

## Best Practices

1. **Use platforms for alignment** across related libraries
2. **Import BOMs** from frameworks you use
3. **Create internal platform** for company-wide standards
4. **Use enforcedPlatform** when you need strict versions
5. **Combine with version catalog** for best of both
