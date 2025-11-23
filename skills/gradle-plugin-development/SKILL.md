---
name: gradle-plugin-development
description: Develops custom Gradle plugins including convention plugins, precompiled script plugins, and binary plugins with extension modeling. Claude uses this when you ask to create plugins, implement conventions, or organize build logic.
---

# Gradle Plugin Development Skill

This skill enables Claude to create and develop custom Gradle plugins for shared build logic, conventions, and reusable configurations.

## When Claude Uses This Skill

Claude will automatically invoke this skill when you:
- Ask to "create a gradle plugin"
- Want to "implement a convention plugin"
- Need "precompiled script plugin"
- Request "shared build logic"
- Ask about "plugin extensions" or "plugin configuration"
- Inquire about "buildSrc organization"

## Plugin Types

### 1. Precompiled Script Plugin (buildSrc)

**Directory Structure:**
```
buildSrc/
├── build.gradle.kts
└── src/main/kotlin/
    ├── java-library-conventions.gradle.kts
    ├── java-application-conventions.gradle.kts
    └── kotlin-library-conventions.gradle.kts
```

**buildSrc/build.gradle.kts:**
```kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.20")
}
```

**java-library-conventions.gradle.kts:**
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
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
```

**Usage in build.gradle.kts:**
```kotlin
plugins {
    id("java-library-conventions")
}
```

### 2. Binary Plugin

**Plugin Class:**
```kotlin
package com.example.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

abstract class MyCustomPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Create extension
        val extension = project.extensions.create<MyPluginExtension>("myPlugin")

        // Set defaults
        extension.enabled.convention(true)
        extension.version.convention("1.0.0")

        // Register tasks
        project.tasks.register<MyCustomTask>("myCustomTask") {
            enabled.set(extension.enabled)
            version.set(extension.version)
        }

        // Apply other plugins
        project.plugins.apply("java-library")

        // Configure dependencies
        project.dependencies.add("implementation", "com.example:my-lib:${extension.version.get()}")
    }
}

interface MyPluginExtension {
    val enabled: Property<Boolean>
    val version: Property<String>
}
```

**Task Class:**
```kotlin
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CacheableTask
abstract class MyCustomTask : DefaultTask() {
    @get:Input
    abstract val enabled: Property<Boolean>

    @get:Input
    abstract val version: Property<String>

    @TaskAction
    fun execute() {
        if (enabled.get()) {
            logger.lifecycle("Running with version: ${version.get()}")
        }
    }
}
```

**Plugin Descriptor:**
```
src/main/resources/META-INF/gradle-plugins/com.example.my-plugin.properties
```
```properties
implementation-class=com.example.gradle.MyCustomPlugin
```

**build.gradle.kts (plugin project):**
```kotlin
plugins {
    `java-gradle-plugin`
    `maven-publish`
    kotlin("jvm") version "1.9.20"
}

group = "com.example"
version = "1.0.0"

gradlePlugin {
    plugins {
        create("myCustomPlugin") {
            id = "com.example.my-plugin"
            implementationClass = "com.example.gradle.MyCustomPlugin"
        }
    }
}

dependencies {
    implementation(gradleApi())
}
```

### 3. Settings Plugin

**Plugin Class:**
```kotlin
package com.example.gradle

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

abstract class MySettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        // Configure dependency resolution management
        settings.dependencyResolutionManagement {
            repositories {
                mavenCentral()
                google()
            }

            versionCatalogs {
                create("libs") {
                    from(files("gradle/libs.versions.toml"))
                }
            }
        }

        // Include projects dynamically
        settings.rootProject.name = "my-project"

        // Auto-detect subprojects
        val rootDir = settings.rootDir
        rootDir.listFiles()?.filter { it.isDirectory && File(it, "build.gradle.kts").exists() }
            ?.forEach { dir ->
                settings.include(":${dir.name}")
            }
    }
}
```

## Plugin Development Patterns

### Extension with Nested Configuration

**Kotlin DSL:**
```kotlin
abstract class MyPluginExtension(project: Project) {
    val objects = project.objects

    @get:Input
    abstract val version: Property<String>

    @get:Nested
    abstract val database: DatabaseConfig

    @get:Nested
    abstract val server: ServerConfig

    init {
        version.convention("1.0.0")
    }
}

interface DatabaseConfig {
    val url: Property<String>
    val driver: Property<String>
}

interface ServerConfig {
    val port: Property<Int>
    val host: Property<String>
}
```

**Usage:**
```kotlin
myPlugin {
    version.set("2.0.0")
    database {
        url.set("jdbc:postgresql://localhost/mydb")
        driver.set("org.postgresql.Driver")
    }
    server {
        port.set(8080)
        host.set("localhost")
    }
}
```

### Cross-Project Configuration

**Convention Plugin:**
```kotlin
// buildSrc/src/main/kotlin/shared-conventions.gradle.kts

subprojects {
    // Apply common plugins
    apply(plugin = "java-library")
    apply(plugin = "jacoco")

    // Configure repositories
    repositories {
        mavenCentral()
    }

    // Configure dependencies
    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.10.1")
    }

    // Configure tasks
    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<JavaCompile> {
        options.release.set(17)
    }
}
```

### Plugin with Custom Task

**Complete Example:**
```kotlin
// Plugin
package com.example.gradle.codegen

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

class CodeGenPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create<CodeGenExtension>("codeGen")

        extension.outputDir.convention(project.layout.buildDirectory.dir("generated-src"))
        extension.templateDir.convention(project.layout.projectDirectory.dir("templates"))

        project.tasks.register<GenerateCodeTask>("generateCode") {
            templateDir.set(extension.templateDir)
            outputDir.set(extension.outputDir)
            packageName.set(extension.packageName)
        }

        // Add generated sources to source sets
        project.plugins.withId("java") {
            project.the<JavaPluginExtension>().sourceSets.getByName("main") {
                java.srcDir(extension.outputDir)
            }
        }

        // Make compileJava depend on code generation
        project.tasks.named("compileJava") {
            dependsOn("generateCode")
        }
    }
}

// Extension
abstract class CodeGenExtension {
    abstract val outputDir: DirectoryProperty
    abstract val templateDir: DirectoryProperty
    abstract val packageName: Property<String>
}

// Task
@CacheableTask
abstract class GenerateCodeTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val templateDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val packageName: Property<String>

    @TaskAction
    fun generate() {
        val templates = templateDir.get().asFile
        val output = outputDir.get().asFile

        output.mkdirs()

        templates.listFiles()?.forEach { template ->
            val content = template.readText()
                .replace("{{package}}", packageName.get())

            val outFile = File(output, template.name.replace(".template", ".java"))
            outFile.writeText(content)
        }
    }
}
```

## Plugin Testing

**Functional Test:**
```kotlin
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

class MyPluginTest {
    @TempDir
    lateinit var testProjectDir: File

    @Test
    fun `plugin applies successfully`() {
        val buildFile = File(testProjectDir, "build.gradle.kts")
        buildFile.writeText("""
            plugins {
                id("com.example.my-plugin")
            }

            myPlugin {
                version.set("1.0.0")
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("myCustomTask")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("Running with version: 1.0.0"))
    }
}
```

**build.gradle.kts (plugin project):**
```kotlin
plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "1.9.20"
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation(gradleTestKit())
}

tasks.test {
    useJUnitPlatform()
}
```

## Best Practices

1. **Use Convention Plugins for Shared Logic**: Prefer buildSrc over copy-paste
2. **Type-Safe Configuration**: Use abstract properties with Provider API
3. **Lazy Task Registration**: Always use `tasks.register()`
4. **Proper Task Inputs/Outputs**: Enable caching and up-to-date checks
5. **Test Your Plugins**: Use TestKit for functional tests
6. **Version Catalog Integration**: Support version catalogs in plugins
7. **Documentation**: Provide clear usage examples
8. **Publish to Plugin Portal**: Share reusable plugins

## Plugin Development Checklist

- [ ] Use `Plugin<Project>` or `Plugin<Settings>` interface
- [ ] Create extension with `extensions.create()`
- [ ] Use abstract properties with Provider API
- [ ] Register tasks with `tasks.register()`
- [ ] Configure defaults with `.convention()`
- [ ] Add proper task inputs/outputs
- [ ] Implement functional tests with TestKit
- [ ] Document plugin usage and configuration
- [ ] Publish to local Maven for testing
- [ ] Consider Gradle plugin portal publication
