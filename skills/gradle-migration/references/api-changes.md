# API Migration Reference

Quick reference for deprecated/removed APIs and their replacements.

## Task APIs

| Old API | New API | Version |
|---------|---------|---------|
| `tasks.create()` | `tasks.register()` | 4.9+ |
| `tasks.getByName()` | `tasks.named()` | 4.9+ |
| `tasks.all {}` | `tasks.configureEach {}` | 4.9+ |
| `tasks.withType().all {}` | `tasks.withType().configureEach {}` | 4.9+ |

## Archive APIs

| Old API | New API | Removed |
|---------|---------|---------|
| `archiveName` | `archiveFileName.set()` | 8.0 |
| `archiveBaseName` | `archiveBaseName.set()` | 8.0 |
| `archivesBaseName` | `base.archivesName.set()` | 8.0 |
| `destinationDir` | `destinationDirectory.set()` | 8.0 |
| `archivePath` | `archiveFile.get().asFile` | 8.0 |

## Configuration APIs

| Old API | New API | Removed |
|---------|---------|---------|
| `compile` | `implementation` | 7.0 |
| `testCompile` | `testImplementation` | 7.0 |
| `runtime` | `runtimeOnly` | 7.0 |
| `configurations.compile` | `configurations.implementation` | 7.0 |

## Project APIs

| Old API | New API | Version |
|---------|---------|---------|
| `project.buildDir` | `project.layout.buildDirectory` | 7.1+ |
| `project.file()` in doLast | Capture during config | 6.6+ |
| `project.copy {}` in doLast | Inject `FileSystemOperations` | 6.6+ |
| `project.exec {}` in doLast | Inject `ExecOperations` | 6.6+ |

## Property APIs

| Old API | New API | Version |
|---------|---------|---------|
| `project.property("x")` | `providers.gradleProperty("x")` | 6.2+ |
| `System.getProperty("x")` | `providers.systemProperty("x")` | 6.1+ |
| `System.getenv("x")` | `providers.environmentVariable("x")` | 6.1+ |

## Plugin Convention APIs

| Old API | New API | Removed |
|---------|---------|---------|
| `JavaPluginConvention` | `JavaPluginExtension` | 8.0 |
| `ApplicationPluginConvention` | `JavaApplication` extension | 8.0 |
| `project.convention.getPlugin()` | `project.extensions.getByType()` | 8.0 |

## File Collection APIs

| Old API | New API | Version |
|---------|---------|---------|
| `fileCollection.files` | `fileCollection.files` (lazy: `.map {}`) | - |
| `sourceSets.main.output.classesDir` | `sourceSets.main.output.classesDirs` | 4.0 |

## Dependency APIs

| Old API | New API | Version |
|---------|---------|---------|
| `compile project(':lib')` | `implementation(project(":lib"))` | 7.0 |
| `force = true` | `version { strictly() }` | 6.0+ |

## Examples

### Task Registration

```kotlin
// ❌ Old
tasks.create("myTask", MyTask::class.java) {
    input.set("value")
}

// ✅ New
tasks.register<MyTask>("myTask") {
    input.set("value")
}
```

### Archive Configuration

```kotlin
// ❌ Old
tasks.jar {
    archiveName = "app-${version}.jar"
    destinationDir = file("$buildDir/dist")
}

// ✅ New
tasks.jar {
    archiveFileName.set("app-${version}.jar")
    destinationDirectory.set(layout.buildDirectory.dir("dist"))
}
```

### Provider API

```kotlin
// ❌ Old
val dbUrl = System.getProperty("db.url")
val env = System.getenv("ENV")

// ✅ New
val dbUrl = providers.systemProperty("db.url")
val env = providers.environmentVariable("ENV")
```
