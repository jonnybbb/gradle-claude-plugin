# Build File Patterns

## Kotlin DSL (build.gradle.kts)

```kotlin
plugins {
    `java-library`
    `maven-publish`
}

group = "com.example"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    api("com.google.guava:guava:32.1.3-jre")
    implementation("org.slf4j:slf4j-api:2.0.9")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}
```

## Groovy DSL (build.gradle)

```groovy
plugins {
    id 'java-library'
    id 'maven-publish'
}

group = 'com.example'
version = '1.0.0'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    api 'com.google.guava:guava:32.1.3-jre'
    implementation 'org.slf4j:slf4j-api:2.0.9'
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.1'
}

repositories {
    mavenCentral()
}

test {
    useJUnitPlatform()
}
```

## Common Patterns to Identify

1. **Plugin Application**: `plugins { }` block
2. **Dependencies**: `dependencies { }` block
3. **Repositories**: `repositories { }` block
4. **Task Configuration**: `tasks.named()` or `tasks.register()`
5. **Custom Properties**: `ext { }` block (Groovy) or `val/var` (Kotlin)
6. **Configurations**: `configurations { }` block
