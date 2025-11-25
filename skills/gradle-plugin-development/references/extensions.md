# DSL Extension Design

## Basic Extension

```kotlin
// Extension class
abstract class MyExtension {
    abstract val message: Property<String>
    abstract val count: Property<Int>

    init {
        message.convention("Default message")
        count.convention(1)
    }
}

// In plugin
class MyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create<MyExtension>("myPlugin")

        project.tasks.register<MyTask>("myTask") {
            message.set(extension.message)
            count.set(extension.count)
        }
    }
}
```

## Usage in Build Script

```kotlin
myPlugin {
    message.set("Hello")
    count.set(5)
}
```

## Nested Extensions

```kotlin
abstract class MyExtension {
    abstract val server: ServerConfig
    abstract val client: ClientConfig
}

abstract class ServerConfig {
    abstract val port: Property<Int>
    abstract val host: Property<String>
}

// Usage
myPlugin {
    server {
        port.set(8080)
        host.set("localhost")
    }
}
```

## Named Domain Object Container

```kotlin
abstract class MyExtension(objects: ObjectFactory) {
    val environments: NamedDomainObjectContainer<Environment> =
        objects.domainObjectContainer(Environment::class)
}

abstract class Environment(val name: String) {
    abstract val url: Property<String>
}

// Usage
myPlugin {
    environments {
        create("dev") { url.set("http://dev.example.com") }
        create("prod") { url.set("https://example.com") }
    }
}
```

## Conventions (Defaults)

```kotlin
abstract class MyExtension(project: Project) {
    abstract val outputDir: DirectoryProperty

    init {
        outputDir.convention(project.layout.buildDirectory.dir("my-output"))
    }
}
```

## Best Practices

1. Use Property/Provider API for lazy evaluation
2. Set conventions for sensible defaults
3. Use abstract properties with ObjectFactory injection
4. Support configuration from gradle.properties
