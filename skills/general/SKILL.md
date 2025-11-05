# Gradle Expert Skill

You are an expert in Gradle build automation. When working with Gradle projects, provide comprehensive guidance on all aspects of Gradle based on official documentation and industry best practices.

## Core Competencies

### 1. Project Setup and Structure

#### Initializing New Projects
```bash
# Initialize new Gradle project
gradle init

# Initialize specific project type
gradle init --type java-application
gradle init --type java-library
gradle init --type kotlin-application
gradle init --type kotlin-library
```

#### Multi-Module Projects
**Best Practices:**
- Use `settings.gradle` to define module structure
- Keep modules focused and cohesive
- Define dependencies between modules properly
- Use composite builds for complex scenarios

**Example settings.gradle:**
```groovy
rootProject.name = 'my-project'

include 'core'
include 'api'
include 'web'
include 'mobile'

// Optional: organize modules in directories
project(':core').projectDir = file('modules/core')
```

#### Build Script Structure
**Recommended Organization:**
```groovy
// 1. Plugins
plugins {
    id 'java'
    id 'application'
}

// 2. Project metadata
group = 'com.example'
version = '1.0.0'

// 3. Repositories
repositories {
    mavenCentral()
}

// 4. Dependencies
dependencies {
    implementation 'com.google.guava:guava:31.1-jre'
    testImplementation 'junit:junit:4.13.2'
}

// 5. Task configuration
tasks.named('test') {
    useJUnitPlatform()
}

// 6. Custom tasks
tasks.register('customTask') {
    doLast {
        // Task action
    }
}
```

### 2. Dependency Management

#### Dependency Configurations
**Common Configurations:**
- `implementation`: Compile and runtime dependencies (not exposed to consumers)
- `api`: Compile and runtime dependencies (exposed to consumers) - requires `java-library` plugin
- `compileOnly`: Compile-time only dependencies
- `runtimeOnly`: Runtime-only dependencies
- `testImplementation`: Test compile and runtime
- `testCompileOnly`: Test compile-time only
- `testRuntimeOnly`: Test runtime-only

**Examples:**
```groovy
dependencies {
    // Production code
    implementation 'com.google.guava:guava:31.1-jre'
    api 'com.fasterxml.jackson.core:jackson-databind:2.14.0'
    compileOnly 'org.projectlombok:lombok:1.18.24'
    runtimeOnly 'com.h2database:h2:2.1.214'

    // Test code
    testImplementation 'org.junit.jupiter:junit-jupiter:5.9.0'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

#### Version Management
**Dependency Catalogs (Gradle 7.0+):**
```toml
# gradle/libs.versions.toml
[versions]
kotlin = "1.9.20"
junit = "5.10.0"

[libraries]
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
```

**Usage in build.gradle:**
```groovy
dependencies {
    implementation libs.kotlin.stdlib
    testImplementation libs.junit.jupiter
}
```

#### Dependency Constraints
```groovy
dependencies {
    constraints {
        implementation('org.apache.commons:commons-lang3:3.12.0')
    }
}
```

#### Resolving Conflicts
```groovy
configurations.all {
    resolutionStrategy {
        // Force specific version
        force 'com.google.guava:guava:31.1-jre'

        // Fail on version conflict
        failOnVersionConflict()

        // Prefer specific version
        preferProjectModules()
    }
}
```

### 3. Plugin Development

#### Applying Plugins
```groovy
// Core plugins (no version needed)
plugins {
    id 'java'
    id 'application'
}

// Community plugins (version required)
plugins {
    id 'com.github.johnrengelman.shadow' version '8.1.1'
}
```

#### Custom Plugin (buildSrc)
```kotlin
// buildSrc/src/main/kotlin/MyCustomPlugin.kt
import org.gradle.api.Plugin
import org.gradle.api.Project

class MyCustomPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("myTask") {
            doLast {
                println("Running custom task")
            }
        }
    }
}
```

**Usage:**
```groovy
// build.gradle
plugins {
    id 'my-custom-plugin'
}
```

### 4. Task Management

#### Creating Tasks
```groovy
// Simple task
tasks.register('hello') {
    doLast {
        println 'Hello, Gradle!'
    }
}

// Typed task
tasks.register('copy', Copy) {
    from 'src'
    into 'build/output'
}

// Task with configuration
tasks.register('processFiles') {
    group = 'custom'
    description = 'Processes files'

    doFirst {
        println 'Starting...'
    }

    doLast {
        println 'Done!'
    }
}
```

#### Task Dependencies
```groovy
tasks.register('taskB') {
    dependsOn 'taskA'

    doLast {
        println 'Task B'
    }
}

// Must run after (but not a hard dependency)
tasks.named('test') {
    mustRunAfter 'build'
}

// Should run after (soft ordering)
tasks.named('deploy') {
    shouldRunAfter 'test'
}
```

#### Task Configuration Avoidance
**Modern API (Recommended):**
```groovy
tasks.register('myTask') {
    // Configured only when needed
}

tasks.named('test') {
    // Configured only when needed
}
```

**Old API (Avoid):**
```groovy
task myTask {
    // Configured immediately
}
```

### 5. Build Lifecycle

#### Gradle Build Phases
1. **Initialization**: Determines which projects participate in build
2. **Configuration**: Executes build scripts, creates task graph
3. **Execution**: Executes selected tasks

#### Lifecycle Hooks
```groovy
gradle.projectsEvaluated {
    println 'All projects evaluated'
}

gradle.taskGraph.whenReady {
    println 'Task graph ready'
}

gradle.buildFinished {
    println 'Build finished'
}
```

### 6. Testing

#### JUnit 5
```groovy
plugins {
    id 'java'
}

dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()

    testLogging {
        events "passed", "skipped", "failed"
    }

    // Configure test execution
    maxParallelForks = Runtime.runtime.availableProcessors().intdiv(2) ?: 1
}
```

#### Test Filtering
```bash
# Run specific test class
./gradlew test --tests com.example.MyTest

# Run tests matching pattern
./gradlew test --tests '*Integration*'

# Run specific test method
./gradlew test --tests com.example.MyTest.testMethod
```

### 7. Java/Kotlin Configuration

#### Java Toolchain
```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
```

#### Source Sets
```groovy
sourceSets {
    integrationTest {
        java {
            srcDir 'src/integration-test/java'
        }
        resources {
            srcDir 'src/integration-test/resources'
        }
    }
}
```

#### Kotlin DSL Configuration
```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "1.9.20"
}

dependencies {
    implementation(kotlin("stdlib"))
}

tasks.test {
    useJUnitPlatform()
}
```

### 8. Build Variants and Flavors (Android)

```groovy
android {
    buildTypes {
        debug {
            applicationIdSuffix ".debug"
            versionNameSuffix "-DEBUG"
        }
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt')
        }
    }

    flavorDimensions "version"
    productFlavors {
        free {
            dimension "version"
            applicationIdSuffix ".free"
        }
        paid {
            dimension "version"
            applicationIdSuffix ".paid"
        }
    }
}
```

### 9. Publishing

#### Maven Publish
```groovy
plugins {
    id 'maven-publish'
}

publishing {
    publications {
        maven(MavenPublication) {
            from components.java

            groupId = 'com.example'
            artifactId = 'my-library'
            version = '1.0.0'

            pom {
                name = 'My Library'
                description = 'A library for doing things'
                url = 'https://github.com/example/my-library'

                licenses {
                    license {
                        name = 'The Apache License, Version 2.0'
                        url = 'http://www.apache.org/licenses/LICENSE-2.0.txt'
                    }
                }
            }
        }
    }

    repositories {
        maven {
            url = uri("${buildDir}/repo")
        }
    }
}
```

### 10. Troubleshooting

#### Common Issues and Solutions

**Problem: OutOfMemoryError**
```properties
# In gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```

**Problem: Dependency Resolution Failures**
```bash
# View dependency tree
./gradlew dependencies

# View specific configuration
./gradlew dependencies --configuration implementation

# Refresh dependencies
./gradlew build --refresh-dependencies
```

**Problem: Task Not Up-to-Date**
```bash
# Explain why task ran
./gradlew build --info

# See task inputs/outputs
./gradlew help --task build
```

**Problem: Build Cache Issues**
```bash
# Clear build cache
rm -rf ~/.gradle/caches

# Disable cache for debugging
./gradlew build --no-build-cache
```

#### Debugging Builds
```bash
# Run with debug logging
./gradlew build --debug

# Run with info logging
./gradlew build --info

# Run with stacktrace
./gradlew build --stacktrace

# Run with full stacktrace
./gradlew build --full-stacktrace

# Scan build performance
./gradlew build --scan

# Profile build
./gradlew build --profile
```

### 11. Gradle Wrapper

#### Updating Wrapper
```bash
# Update to specific version
./gradlew wrapper --gradle-version 8.5

# Update to latest release
./gradlew wrapper --gradle-version latest

# Use specific distribution type
./gradlew wrapper --gradle-version 8.5 --distribution-type all
```

#### Wrapper Configuration
```properties
# gradle/wrapper/gradle-wrapper.properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

### 12. Best Practices

#### General Guidelines
1. **Use the Gradle Wrapper** - Always commit wrapper to version control
2. **Use plugins DSL** - Prefer `plugins {}` block over legacy `apply plugin`
3. **Lazy configuration** - Use `tasks.register()` instead of `task`
4. **Type-safe accessors** - Use Kotlin DSL for better IDE support
5. **Dependency management** - Use version catalogs for multi-module projects
6. **Build cache** - Enable for faster builds
7. **Incremental builds** - Properly declare task inputs and outputs
8. **Multi-module structure** - Keep modules cohesive and loosely coupled

#### Performance Best Practices
See the `performance` skill for comprehensive performance optimization guidance.

#### Security Best Practices
```groovy
// Don't hardcode credentials
repositories {
    maven {
        url 'https://private-repo.example.com'
        credentials {
            username = System.getenv("REPO_USERNAME")
            password = System.getenv("REPO_PASSWORD")
        }
    }
}

// Use dependency verification
// Run: ./gradlew --write-verification-metadata sha256
```

## Action Steps for Gradle Tasks

When asked to work with Gradle:

1. **Identify project type**: Check for `settings.gradle`, `build.gradle`, or `.gradle.kts` files
2. **Understand structure**: Examine multi-module setup if applicable
3. **Check Gradle version**: Look at `gradle/wrapper/gradle-wrapper.properties`
4. **Analyze dependencies**: Review dependency configurations
5. **Examine tasks**: Run `./gradlew tasks` to see available tasks
6. **Provide solutions**: Offer specific, actionable recommendations
7. **Test changes**: Suggest running `./gradlew build` to verify

## Common Commands Reference

```bash
# Build project
./gradlew build

# Clean build
./gradlew clean build

# Run tests
./gradlew test

# Run specific task
./gradlew taskName

# List all tasks
./gradlew tasks --all

# View dependencies
./gradlew dependencies

# Refresh dependencies
./gradlew build --refresh-dependencies

# Generate build scan
./gradlew build --scan

# Update wrapper
./gradlew wrapper --gradle-version 8.5

# Debug build
./gradlew build --debug --stacktrace
```

## Integration with IDEs

### IntelliJ IDEA
- Automatically detects Gradle projects
- Delegates build to Gradle by default
- Refresh Gradle project: View → Tool Windows → Gradle → Reload

### VS Code
- Use "Gradle for Java" extension
- Gradle tasks appear in sidebar
- Configure Java toolchain in settings

### Eclipse
- Use Buildship Gradle Integration plugin
- Import as Gradle project
- Refresh: Right-click project → Gradle → Refresh Gradle Project

## Resources and Documentation

- **Official Gradle Docs**: https://docs.gradle.org/current/userguide/userguide.html
- **Gradle DSL Reference**: https://docs.gradle.org/current/dsl/
- **Gradle Plugin Portal**: https://plugins.gradle.org/
- **Gradle Build Scans**: https://scans.gradle.com
- **Gradle Releases**: https://gradle.org/releases/
- **Gradle Forums**: https://discuss.gradle.org/

## Working with This Skill

When providing Gradle assistance:
- Always use official Gradle best practices
- Prefer modern APIs over deprecated ones
- Explain the "why" behind recommendations
- Provide complete, runnable examples
- Reference official documentation when applicable
- Consider project context (Java, Kotlin, Android, etc.)
- Suggest performance optimizations when relevant (see performance skill)
