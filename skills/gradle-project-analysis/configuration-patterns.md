# Configuration Patterns

## Version Catalogs (libs.versions.toml)

```toml
[versions]
kotlin = "1.9.20"
spring-boot = "3.1.5"

[libraries]
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
spring-boot-starter = { module = "org.springframework.boot:spring-boot-starter-web", version.ref = "spring-boot" }

[bundles]
spring = ["spring-boot-starter", "spring-boot-actuator"]

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
```

Usage:
```kotlin
dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.bundles.spring)
}
```

## Convention Plugins (buildSrc/)

```
buildSrc/
├── build.gradle.kts
└── src/main/kotlin/
    └── java-conventions.gradle.kts
```

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

## Gradle Properties (gradle.properties)

```properties
# Performance
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.jvmargs=-Xmx4g

# Project info
group=com.example
version=1.0.0
```

## Wrapper Configuration (gradle/wrapper/gradle-wrapper.properties)

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
```

Key: Extract Gradle version from distributionUrl
