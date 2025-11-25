# Extension Objects

**Source**: https://docs.gradle.org/current/userguide/custom_plugins.html  
**Gradle Version**: 7.0+

## Overview

Extensions provide configuration DSL for plugins.

## Basic Extension

```kotlin
abstract class MyExtension {
    abstract val message: Property<String>
    abstract val enabled: Property<Boolean>
    
    init {
        message.convention("default")
        enabled.convention(true)
    }
}

// Plugin
class MyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions
            .create("myPlugin", MyExtension::class.java)
    }
}

// Usage
myPlugin {
    message.set("custom")
    enabled.set(false)
}
```

## Nested Extensions

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

## Collections

```kotlin
abstract class MyExtension(objects: ObjectFactory) {
    val items: NamedDomainObjectContainer<Item> =
        objects.domainObjectContainer(Item::class.java)
}

abstract class Item(val name: String) {
    abstract val value: Property<String>
}

// Usage
myPlugin {
    items {
        create("first") {
            value.set("1")
        }
        create("second") {
            value.set("2")
        }
    }
}
```

## Action Configuration

```kotlin
abstract class MyExtension {
    fun configure(action: Action<Config>) {
        // Apply configuration
    }
}

// Usage
myPlugin {
    configure {
        setting = "value"
    }
}
```

## Best Practices

1. **Use Property API**
2. **Set conventions (defaults)**
3. **Validate configuration**
4. **Document properties**

## Related Documentation

- [Plugin Basics](plugin-basics.md): Plugin development
- [Plugin Testing](plugin-testing.md): Testing extensions

## Quick Reference

```kotlin
abstract class MyExtension {
    abstract val prop: Property<String>
    
    init {
        prop.convention("default")
    }
}

// Create
val ext = project.extensions.create("myExt", MyExtension::class.java)

// Use
myExt {
    prop.set("value")
}
```
