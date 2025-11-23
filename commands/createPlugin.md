---
description: Generate scaffolding for a new Gradle plugin with proper structure, extension modeling, and task registration. Creates both convention plugins (buildSrc) and binary plugins with complete setup.
---

# Create Gradle Plugin

Generate a complete Gradle plugin with proper structure and best practices.

## What This Command Does

1. **Analyzes Project Structure**: Determines if you have buildSrc/ or need a binary plugin
2. **Generates Plugin Scaffolding**: Creates plugin class, extension, and tasks
3. **Sets Up Testing**: Includes TestKit functional tests
4. **Provides Both DSLs**: Generates examples in Kotlin and Groovy DSL
5. **Applies Best Practices**: Uses Provider API, lazy configuration, and proper annotations

## Usage

```
/createPlugin
```

You'll be prompted for:
- **Plugin type**: Convention plugin (buildSrc) or binary plugin
- **Plugin ID**: e.g., `java-conventions` or `com.example.my-plugin`
- **Plugin name**: Human-readable name
- **Extension name**: Configuration extension (optional)
- **Tasks to create**: List of task names (optional)

## Plugin Types

### Convention Plugin (buildSrc)

Best for:
- Sharing configuration across modules in same project
- Project-specific build conventions
- Quick iteration without publishing

Location: `buildSrc/src/main/kotlin/`

### Binary Plugin

Best for:
- Sharing across multiple projects
- Publishing to plugin portal
- Reusable components

Location: Separate plugin project

## Generated Structure

### Convention Plugin

```
buildSrc/
├── build.gradle.kts
└── src/main/kotlin/
    └── my-conventions.gradle.kts
```

### Binary Plugin

```
my-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/example/
│   │   │       ├── MyPlugin.kt
│   │   │       ├── MyPluginExtension.kt
│   │   │       └── tasks/
│   │   │           └── MyTask.kt
│   │   └── resources/
│   │       └── META-INF/gradle-plugins/
│   │           └── com.example.my-plugin.properties
│   └── test/
│       └── kotlin/
│           └── com/example/
│               └── MyPluginTest.kt
└── README.md
```

## Example Output

When you run `/createPlugin`:

**Prompt:**
```
What type of plugin? (convention/binary): convention
Plugin ID: java-library-conventions
Extension name (optional): javaLib
Tasks to create (comma-separated, optional): generateBuildInfo
```

**Generated buildSrc/src/main/kotlin/java-library-conventions.gradle.kts:**
```kotlin
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `java-library`
    `maven-publish`
}

// Extension for configuration
interface JavaLibConventionsExtension {
    val javaVersion: Property<Int>
    val enableJavadoc: Property<Boolean>
}

val javaLib = extensions.create<JavaLibConventionsExtension>("javaLib")
javaLib.javaVersion.convention(17)
javaLib.enableJavadoc.convention(true)

// Configure Java toolchain
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaLib.javaVersion.get()))
    }
    if (javaLib.enableJavadoc.get()) {
        withJavadocJar()
    }
    withSourcesJar()
}

// Configure dependencies
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Configure test task
tasks.test {
    useJUnitPlatform()
    testLogging {
        events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}

// Configure publishing
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

// Custom task: generateBuildInfo
tasks.register("generateBuildInfo") {
    val outputDir = layout.buildDirectory.dir("generated/build-info")

    outputs.dir(outputDir)

    doLast {
        val buildInfo = """
            |version=${project.version}
            |buildTime=${java.time.Instant.now()}
            |javaVersion=${javaLib.javaVersion.get()}
        """.trimMargin()

        outputDir.get().asFile.mkdirs()
        outputDir.get().file("build-info.properties").asFile.writeText(buildInfo)
    }
}
```

## What You Get

✅ **Fully Functional Plugin**: Ready to use immediately
✅ **Best Practices**: Provider API, lazy configuration, proper annotations
✅ **Testing Setup**: Functional tests with Gradle TestKit
✅ **Documentation**: README with usage examples
✅ **Both DSLs**: Examples in Kotlin and Groovy
✅ **Extension Modeling**: Type-safe configuration

## Next Steps

After generating the plugin:

1. **Review Generated Code**: Understand the plugin structure
2. **Customize Logic**: Add your specific build logic
3. **Test the Plugin**: Run functional tests
4. **Apply to Modules**: Use the plugin in your project

```kotlin
// In a module's build.gradle.kts
plugins {
    id("java-library-conventions")
}

javaLib {
    javaVersion.set(21)
    enableJavadoc.set(false)
}
```

5. **Iterate**: Refine based on project needs

## Advanced Options

- **Multi-language support**: Generate Groovy or Java implementations
- **Complex extensions**: Nested configuration objects
- **Task graph dependencies**: Proper task ordering
- **Variant-aware**: Android plugin compatibility

## Related

- `/createTask` - Create custom tasks
- `/doctor` - Analyze plugin health
- See `gradle-plugin-development` skill for detailed guidance
