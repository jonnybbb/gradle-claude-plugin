# Plugin Development Basics

**Source**: https://docs.gradle.org/current/userguide/custom_plugins.html  
**Gradle Version**: 7.0+, optimized for 8+

## Overview

Gradle plugins extend the build system with reusable functionality. Plugins can apply conventions, register tasks, create extensions, and integrate with the Gradle lifecycle.

## Plugin Types

### 1. Script Plugins

Simple plugins defined in `.gradle` or `.gradle.kts` files.

**example-plugin.gradle.kts:**
```kotlin
tasks.register("hello") {
    doLast {
        println("Hello from script plugin")
    }
}
```

**Apply:**
```kotlin
apply(from = "example-plugin.gradle.kts")
```

**Use Case**: Simple, project-specific conventions

### 2. Binary Plugins

Plugins packaged as JARs, published to repositories.

**Build file:**
```kotlin
plugins {
    id("com.example.my-plugin") version "1.0.0"
}
```

**Use Case**: Reusable plugins across projects

### 3. PreCompiled Script Plugins

Kotlin/Groovy scripts in `buildSrc` or convention plugins.

**buildSrc/src/main/kotlin/my-conventions.gradle.kts:**
```kotlin
plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
```

**Apply:**
```kotlin
plugins {
    id("my-conventions")
}
```

**Use Case**: Project-wide conventions

## Creating a Binary Plugin

### Plugin Class

**Kotlin:**
```kotlin
package com.example

import org.gradle.api.Plugin
import org.gradle.api.Project

class MyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Create extension
        val extension = project.extensions.create("myPlugin", MyExtension::class.java)
        
        // Register tasks
        project.tasks.register("myTask", MyTask::class.java) {
            message.set(extension.message)
        }
        
        // Apply other plugins
        project.plugins.apply("java-library")
        
        // Configure project
        project.afterEvaluate {
            configureProject(project, extension)
        }
    }
    
    private fun configureProject(project: Project, extension: MyExtension) {
        // Configuration after build file evaluation
    }
}
```

**Groovy:**
```groovy
package com.example

import org.gradle.api.Plugin
import org.gradle.api.Project

class MyPlugin implements Plugin<Project> {
    void apply(Project project) {
        def extension = project.extensions.create('myPlugin', MyExtension)
        
        project.tasks.register('myTask', MyTask) {
            message = extension.message
        }
        
        project.plugins.apply('java-library')
        
        project.afterEvaluate {
            configureProject(project, extension)
        }
    }
    
    private void configureProject(Project project, MyExtension extension) {
        // Configuration
    }
}
```

### Extension Class

**Kotlin:**
```kotlin
package com.example

import org.gradle.api.provider.Property

abstract class MyExtension {
    abstract val message: Property<String>
    
    abstract val enabled: Property<Boolean>
    
    init {
        // Set defaults
        message.convention("Default message")
        enabled.convention(true)
    }
}
```

**Groovy:**
```groovy
package com.example

import org.gradle.api.provider.Property

abstract class MyExtension {
    abstract Property<String> getMessage()
    abstract Property<Boolean> getEnabled()
}
```

### Task Class

```kotlin
package com.example

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

abstract class MyTask : DefaultTask() {
    @get:Input
    abstract val message: Property<String>
    
    @get:OutputFile
    abstract val outputFile: RegularFileProperty
    
    init {
        group = "my-plugin"
        description = "Executes my task"
        outputFile.convention(
            project.layout.buildDirectory.file("my-task-output.txt")
        )
    }
    
    @TaskAction
    fun execute() {
        logger.lifecycle("Message: ${message.get()}")
        outputFile.asFile.get().writeText(message.get())
    }
}
```

### Plugin Descriptor

**src/main/resources/META-INF/gradle-plugins/com.example.my-plugin.properties:**
```properties
implementation-class=com.example.MyPlugin
```

Filename: `<plugin-id>.properties`

## Plugin Build File

**build.gradle.kts:**
```kotlin
plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

group = "com.example"
version = "1.0.0"

gradlePlugin {
    plugins {
        create("myPlugin") {
            id = "com.example.my-plugin"
            implementationClass = "com.example.MyPlugin"
            displayName = "My Plugin"
            description = "A plugin that does something useful"
        }
    }
}

dependencies {
    implementation(gradleApi())
}
```

**Groovy:**
```groovy
plugins {
    id 'java-gradle-plugin'
    id 'groovy'
}

group = 'com.example'
version = '1.0.0'

gradlePlugin {
    plugins {
        myPlugin {
            id = 'com.example.my-plugin'
            implementationClass = 'com.example.MyPlugin'
            displayName = 'My Plugin'
            description = 'A plugin that does something useful'
        }
    }
}

dependencies {
    implementation gradleApi()
}
```

## Project Structure

```
my-plugin/
├── build.gradle.kts
├── settings.gradle.kts
└── src/
    ├── main/
    │   ├── kotlin/  (or java/groovy)
    │   │   └── com/example/
    │   │       ├── MyPlugin.kt
    │   │       ├── MyExtension.kt
    │   │       └── MyTask.kt
    │   └── resources/
    │       └── META-INF/gradle-plugins/
    │           └── com.example.my-plugin.properties
    └── test/
        └── kotlin/
            └── com/example/
                └── MyPluginTest.kt
```

## Extension Patterns

### Simple Configuration

```kotlin
abstract class SimpleExtension {
    abstract val value: Property<String>
    abstract val enabled: Property<Boolean>
}

// Usage
myPlugin {
    value.set("custom")
    enabled.set(false)
}
```

### Nested Configuration

```kotlin
abstract class OuterExtension {
    abstract val value: Property<String>
    
    @get:Nested
    abstract val inner: InnerExtension
}

abstract class InnerExtension {
    abstract val setting: Property<String>
}

// Usage
myPlugin {
    value.set("outer")
    inner {
        setting.set("inner")
    }
}
```

### Collection Configuration

```kotlin
abstract class MyExtension {
    abstract val items: ListProperty<String>
    
    fun item(value: String) {
        items.add(value)
    }
}

// Usage
myPlugin {
    item("first")
    item("second")
}
```

### Named Domain Objects

```kotlin
class MyExtension(objects: ObjectFactory) {
    val servers: NamedDomainObjectContainer<ServerConfig> =
        objects.domainObjectContainer(ServerConfig::class.java)
}

abstract class ServerConfig(val name: String) {
    abstract val url: Property<String>
    abstract val port: Property<Int>
}

// Usage
myPlugin {
    servers {
        create("prod") {
            url.set("https://prod.example.com")
            port.set(443)
        }
        create("dev") {
            url.set("https://dev.example.com")
            port.set(8080)
        }
    }
}
```

## Plugin Dependencies

### Apply Other Plugins

```kotlin
class MyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Apply base plugin
        project.plugins.apply("java-library")
        
        // Or check and apply
        project.plugins.withType<JavaPlugin> {
            // Configure when Java plugin applied
        }
    }
}
```

### Conditional Application

```kotlin
class MyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.withId("kotlin") {
            // Configure only if Kotlin plugin present
            configureKotlin(project)
        }
    }
}
```

## Lifecycle Hooks

### After Evaluate

```kotlin
class MyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.afterEvaluate {
            // All build files evaluated
            // Safe to read extension values
            val extension = project.extensions.getByType<MyExtension>()
            configureTasks(project, extension)
        }
    }
}
```

### All Projects

```kotlin
class MyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.subprojects {
            // Apply to all subprojects
            plugins.apply("java-library")
        }
    }
}
```

## Multi-Project Plugins

### Root Plugin

```kotlin
class RootPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        require(project == project.rootProject) {
            "This plugin must be applied to root project only"
        }
        
        // Configure all projects
        project.allprojects {
            repositories {
                mavenCentral()
            }
        }
    }
}
```

### Subproject Plugin

```kotlin
class SubprojectPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        require(project != project.rootProject) {
            "This plugin must be applied to subprojects only"
        }
        
        // Subproject-specific configuration
    }
}
```

## Convention Plugins

**buildSrc/src/main/kotlin/java-conventions.gradle.kts:**
```kotlin
plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnitPlatform()
}
```

**Apply in projects:**
```kotlin
plugins {
    id("java-conventions")
}
```

## Publishing Plugins

### Local Maven

**build.gradle.kts:**
```kotlin
plugins {
    `java-gradle-plugin`
    `maven-publish`
}

publishing {
    repositories {
        mavenLocal()
    }
}
```

```bash
./gradlew publishToMavenLocal
```

### Gradle Plugin Portal

**build.gradle.kts:**
```kotlin
plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.2.1"
}

gradlePlugin {
    website.set("https://github.com/user/plugin")
    vcsUrl.set("https://github.com/user/plugin")
    
    plugins {
        create("myPlugin") {
            id = "com.example.my-plugin"
            implementationClass = "com.example.MyPlugin"
            displayName = "My Plugin"
            description = "Description"
            tags.set(listOf("build", "automation"))
        }
    }
}
```

```bash
./gradlew publishPlugins
```

## Testing Plugins

### Unit Testing

```kotlin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class MyPluginTest {
    @Test
    fun `plugin applies successfully`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.example.my-plugin")
        
        assertNotNull(project.extensions.findByName("myPlugin"))
    }
    
    @Test
    fun `plugin registers task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.example.my-plugin")
        
        assertNotNull(project.tasks.findByName("myTask"))
    }
}
```

### Functional Testing

```kotlin
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals

class MyPluginFunctionalTest {
    @TempDir
    lateinit var testProjectDir: File
    
    @Test
    fun `can run task`() {
        val buildFile = File(testProjectDir, "build.gradle.kts")
        buildFile.writeText("""
            plugins {
                id("com.example.my-plugin")
            }
            
            myPlugin {
                message.set("Test message")
            }
        """.trimIndent())
        
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("myTask")
            .withPluginClasspath()
            .build()
        
        assertEquals(TaskOutcome.SUCCESS, result.task(":myTask")?.outcome)
    }
}
```

## Best Practices

### 1. Use Provider API

```kotlin
// ✅ Good
abstract val value: Property<String>

// ❌ Bad
var value: String = ""
```

### 2. Lazy Configuration

```kotlin
// ✅ Good
project.tasks.register("myTask") {
    // Configured lazily
}

// ❌ Bad
project.tasks.create("myTask") {
    // Configured eagerly
}
```

### 3. Declare Extension Defaults

```kotlin
abstract class MyExtension {
    abstract val value: Property<String>
    
    init {
        value.convention("default")
    }
}
```

### 4. Use Project Isolation

```kotlin
// ✅ Good: Use services
@get:Inject
abstract val objects: ObjectFactory

// ❌ Bad: Direct project access in tasks
```

### 5. Version Compatibility

```kotlin
// Specify compatible Gradle version
tasks.withType<Wrapper> {
    gradleVersion = "8.5"
}
```

## Common Patterns

### Conditional Task Registration

```kotlin
class MyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("myPlugin", MyExtension::class.java)
        
        project.afterEvaluate {
            if (extension.enabled.get()) {
                project.tasks.register("myTask", MyTask::class.java)
            }
        }
    }
}
```

### Source Set Configuration

```kotlin
class MyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.withType<JavaPlugin> {
            val sourceSets = project.extensions.getByType<SourceSetContainer>()
            sourceSets.named("main") {
                java.srcDir("src/generated/java")
            }
        }
    }
}
```

### Dependency Management

```kotlin
class MyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.withType<JavaPlugin> {
            project.dependencies.add("implementation", "com.example:library:1.0")
        }
    }
}
```

## Related Documentation

- [Plugin Testing](plugin-testing.md): Testing strategies
- [Extension Objects](extension-objects.md): Extension design patterns
- [Task Basics](task-basics.md): Task development
- [Multi-Project](multi-project.md): Multi-project plugins

## Quick Reference

```kotlin
// Complete plugin example
package com.example

import org.gradle.api.*
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

class MyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("myPlugin", MyExtension::class.java)
        
        project.tasks.register("myTask", MyTask::class.java) {
            message.set(extension.message)
            outputFile.set(project.layout.buildDirectory.file("output.txt"))
        }
    }
}

abstract class MyExtension {
    abstract val message: Property<String>
    
    init {
        message.convention("Hello")
    }
}

abstract class MyTask : DefaultTask() {
    @get:Input
    abstract val message: Property<String>
    
    @get:OutputFile
    abstract val outputFile: RegularFileProperty
    
    @TaskAction
    fun execute() {
        outputFile.asFile.get().writeText(message.get())
    }
}
```
