# Version Catalogs

Centralize dependency versions in a single file.

## Setup

Create `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "1.9.21"
junit = "5.10.0"
guava = "31.1-jre"
spring = "6.1.0"

[libraries]
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
guava = { module = "com.google.guava:guava", version.ref = "guava" }
spring-core = { module = "org.springframework:spring-core", version.ref = "spring" }

[bundles]
testing = ["junit-jupiter"]
spring = ["spring-core"]

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
```

## Usage

```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.guava)
    implementation(libs.bundles.spring)
    testImplementation(libs.bundles.testing)
}
```

## Bundles

Group related dependencies:

```toml
[bundles]
spring = ["spring-core", "spring-context", "spring-beans"]
testing = ["junit-jupiter", "mockito", "assertj"]
```

## Rich Versions

```toml
[versions]
guava = { strictly = "[28.0, 32.0[", prefer = "31.1-jre" }
```

## Benefits

- Single source of truth
- Type-safe accessors (IDE support)
- Shareable across projects
- Easy version updates
