# Plugin Publishing

## Plugin Portal Publishing

### Build Configuration

```kotlin
// build.gradle.kts
plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "com.example"
version = "1.0.0"

gradlePlugin {
    website.set("https://github.com/example/my-plugin")
    vcsUrl.set("https://github.com/example/my-plugin")

    plugins {
        create("myPlugin") {
            id = "com.example.my-plugin"
            displayName = "My Plugin"
            description = "Does useful things"
            tags.set(listOf("utility", "automation"))
            implementationClass = "com.example.MyPlugin"
        }
    }
}
```

### Publish

```bash
# Set credentials
export GRADLE_PUBLISH_KEY=your-key
export GRADLE_PUBLISH_SECRET=your-secret

# Publish
./gradlew publishPlugins
```

## Maven Repository Publishing

```kotlin
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("My Plugin")
                description.set("Useful Gradle plugin")
                url.set("https://github.com/example/my-plugin")
            }
        }
    }

    repositories {
        maven {
            url = uri("https://maven.example.com/releases")
            credentials {
                username = System.getenv("MAVEN_USER")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}
```

## Version Compatibility

```kotlin
gradlePlugin {
    plugins {
        create("myPlugin") {
            // Specify minimum Gradle version
        }
    }
}

// Test against multiple versions
tasks.test {
    systemProperty("gradle.version", gradle.gradleVersion)
}
```

## Signing

```kotlin
plugins {
    signing
}

signing {
    useGpgCmd()
    sign(publishing.publications["maven"])
}
```
