# Gradle Patterns Cheat Sheet

Quick reference for common Gradle patterns and their modern replacements.

## Task APIs

| ❌ Avoid (Eager) | ✅ Use (Lazy) |
|------------------|---------------|
| `tasks.create("x")` | `tasks.register("x")` |
| `tasks.getByName("x")` | `tasks.named("x")` |
| `tasks.all { }` | `tasks.configureEach { }` |
| `tasks.withType<T>().all { }` | `tasks.withType<T>().configureEach { }` |
| `task("x") { }` | `tasks.register("x") { }` |

## Property Access

| ❌ Avoid | ✅ Use |
|----------|--------|
| `System.getProperty("x")` | `providers.systemProperty("x")` |
| `System.getenv("X")` | `providers.environmentVariable("X")` |
| `project.property("x")` | `providers.gradleProperty("x")` |
| `project.buildDir` | `layout.buildDirectory` |
| `project.projectDir` | `layout.projectDirectory` |
| `file("path")` | `layout.projectDirectory.file("path")` |

## Archive Properties (Gradle 8+)

| ❌ Removed | ✅ Replacement |
|------------|----------------|
| `archiveName = "x.jar"` | `archiveFileName.set("x.jar")` |
| `archiveBaseName = "x"` | `archiveBaseName.set("x")` |
| `archivesBaseName = "x"` | `base.archivesName.set("x")` |
| `destinationDir = file("x")` | `destinationDirectory.set(file("x"))` |
| `archivePath` | `archiveFile.get().asFile` |

## Configuration Names

| ❌ Removed | ✅ Replacement |
|------------|----------------|
| `compile` | `implementation` |
| `testCompile` | `testImplementation` |
| `runtime` | `runtimeOnly` |
| `testRuntime` | `testRuntimeOnly` |

## In doLast/doFirst Blocks

| ❌ Avoid | ✅ Use |
|----------|--------|
| `project.copy { }` | Inject `FileSystemOperations` |
| `project.exec { }` | Inject `ExecOperations` |
| `project.delete { }` | Inject `FileSystemOperations` |
| `project.javaexec { }` | Inject `ExecOperations` |
| `project.buildDir` | Capture `layout.buildDirectory` during config |
| `project.file()` | Capture during configuration phase |

## Service Injection Pattern

```kotlin
abstract class MyTask : DefaultTask() {
    @get:Inject abstract val fs: FileSystemOperations
    @get:Inject abstract val exec: ExecOperations
    @get:Inject abstract val archive: ArchiveOperations
    @get:Inject abstract val objects: ObjectFactory
    @get:Inject abstract val providers: ProviderFactory
    @get:Inject abstract val layout: ProjectLayout
    
    @TaskAction
    fun run() {
        fs.copy { from("src"); into("dest") }
        exec.exec { commandLine("echo", "hi") }
    }
}
```

## Plugin/Extension Access

| ❌ Avoid | ✅ Use |
|----------|--------|
| `project.convention.getPlugin(JavaPluginConvention::class)` | `project.extensions.getByType<JavaPluginExtension>()` |
| `project.convention.plugins["java"]` | `project.extensions.getByType<JavaPluginExtension>()` |

## Configuration Cache Checklist

✅ **DO:**
- Use `providers.*` for system/env properties
- Use `layout.*` for file/directory references
- Use lazy task registration (`register`, `named`)
- Inject services for file operations
- Declare all inputs/outputs on tasks

❌ **DON'T:**
- Access `project` in `doLast`/`doFirst`
- Use `System.getProperty()` or `System.getenv()`
- Use `project.buildDir` directly
- Call `project.copy {}` or `project.exec {}`
- Store non-serializable objects

## Quick Performance Wins

```properties
# gradle.properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.vfs.watch=true
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC
```

## Version Catalog Usage

```kotlin
// Access in build.gradle.kts
dependencies {
    implementation(libs.guava)
    implementation(platform(libs.spring.bom))
    testImplementation(libs.bundles.testing)
}

plugins {
    alias(libs.plugins.kotlin.jvm)
}
```

## Lazy Wiring Pattern

```kotlin
val generateTask = tasks.register<GenerateTask>("generate") {
    outputDir.set(layout.buildDirectory.dir("generated"))
}

tasks.named<JavaCompile>("compileJava") {
    source(generateTask.flatMap { it.outputDir })
}
```
