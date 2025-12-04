---
name: kotlin-versions
description: This skill should be used when the user asks to "upgrade Kotlin version", "migrate Kotlin 1.9 to 2.0", "migrate Kotlin 2.0 to 2.1", "use K2 compiler", "use context parameters", "use guard conditions", "use data objects", "configure Kotlin Multiplatform", or mentions Kotlin/Wasm, Kotlin/Native, Compose compiler plugin, smart cast improvements, multi-dollar interpolation, or enum entries.
---

# Kotlin Versions Skill

Reference for Kotlin language features, version upgrades, and modern idiom adoption.

## Quick Reference

| Version | Key Features |
|---------|--------------|
| 1.9.x   | K2 compiler Beta (JVM), data objects, `..< ` operator, `entries` for enums, Kotlin/Wasm Alpha, secondary constructors in inline classes |
| 2.0.0   | K2 compiler Stable, smart cast improvements, new Compose compiler Gradle plugin, invokedynamic lambdas, Kotlin/Wasm improvements |
| 2.0.20  | UUIDs in stdlib, data class copy visibility, HexFormat, strong skipping mode in Compose |
| 2.1.0   | Guard conditions (preview), non-local break/continue (preview), multi-dollar interpolation (preview), Swift export (basic), Kotlin Multiplatform DSL improvements |
| 2.1.20  | Common atomic types, improved UUID support, Instant/Clock time tracking, kapt K2 default, Lombok @SuperBuilder support |
| 2.2.0   | Context parameters (preview), context-sensitive resolution (preview), nested type aliases, stable Base64/HexFormat, @all annotation target, JVM default methods for interfaces |

## Version-Specific References

Select the appropriate reference based on the starting version:

- **Kotlin 1.9.x Features**: See [references/kotlin-1.9.md](references/kotlin-1.9.md) - K2 Beta, data objects, time API, ..<operator
- **Kotlin 2.0.x Features**: See [references/kotlin-2.0.md](references/kotlin-2.0.md) - K2 Stable, smart casts, Compose compiler
- **Kotlin 2.1.x Features**: See [references/kotlin-2.1.md](references/kotlin-2.1.md) - Guard conditions, multi-dollar strings, Swift export
- **Kotlin 2.2.x Features**: See [references/kotlin-2.2.md](references/kotlin-2.2.md) - Context parameters, nested type aliases

## Build Tool Configuration

### Gradle (Kotlin DSL)

```kotlin
plugins {
    kotlin("jvm") version "2.2.0" // or multiplatform, android, etc.
}

kotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
        // For preview features:
        // freeCompilerArgs.add("-Xcontext-parameters")
    }
}
```

### Kotlin Multiplatform

```kotlin
plugins {
    kotlin("multiplatform") version "2.2.0"
}

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()
    // Source sets created automatically from default hierarchy template

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
```

### Compose Multiplatform

```kotlin
plugins {
    kotlin("multiplatform") version "2.2.0"
    kotlin("plugin.compose") version "2.2.0"
    id("org.jetbrains.compose") version "1.7.0"
}
```

## Common Migration Patterns

### Data Class Copy Visibility (2.0.20+)

```kotlin
// Before: copy() was always public even with private constructor
data class User private constructor(val name: String)
val user = User.create("test")
val copy = user.copy(name = "other") // Was possible!

// After (2.0.20+): copy() matches constructor visibility
@ConsistentCopyVisibility
data class User private constructor(val name: String) {
    companion object {
        fun create(name: String) = User(name)
    }
}
// user.copy() now causes compile error
```

### Data Objects (1.9.0+)

```kotlin
// Before
sealed interface Status {
    data class Success(val data: String) : Status
    object Loading : Status // toString() = "Loading"
}

// After (1.9.0+)
sealed interface Status {
    data class Success(val data: String) : Status
    data object Loading : Status // toString() = "Loading", proper equals/hashCode
}
```

### Guard Conditions in When (2.1.0+ preview, 2.2.0 stable)

```kotlin
// Before
fun process(value: Any) = when (value) {
    is String -> if (value.isEmpty()) "empty" else "string: $value"
    is Int -> if (value < 0) "negative" else "positive"
    else -> "unknown"
}

// After (2.2.0+)
fun process(value: Any) = when (value) {
    is String if value.isEmpty() -> "empty"
    is String -> "string: $value"
    is Int if value < 0 -> "negative"
    is Int -> "positive"
    else -> "unknown"
}
```

### Multi-Dollar String Interpolation (2.1.0+ preview, 2.2.0 stable)

```kotlin
// Before: escaping $ was cumbersome
val json = """
    {
        "${"$"}schema": "https://json-schema.org/draft/2020-12/schema",
        "name": "${name}"
    }
""".trimIndent()

// After (2.2.0+)
val json = $$"""
    {
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "name": $${name}
    }
""".trimIndent()
```

### Context Parameters (2.2.0+ preview)

```kotlin
// Before: manual parameter passing
class Logger { fun log(msg: String) = println(msg) }

fun processWithLogger(logger: Logger, data: String) {
    logger.log("Processing: $data")
}

// After (2.2.0+ preview) - enable with -Xcontext-parameters
context(logger: Logger)
fun process(data: String) {
    logger.log("Processing: $data")
}

fun main() {
    val logger = Logger()
    with(logger) {
        process("test") // logger available implicitly
    }
}
```

### Open-Ended Ranges (1.9.0+)

```kotlin
// Before
for (i in 0 until list.size) { }

// After (1.9.0+)
for (i in 0 ..< list.size) { }
```

### Enum Entries (1.9.0+)

```kotlin
enum class Color { RED, GREEN, BLUE }

// Before
val colors = Color.values() // Creates new array each call

// After (1.9.0+)
val colors = Color.entries // Returns immutable list, more efficient
```

### UUID Support (2.0.20+)

```kotlin
import kotlin.uuid.Uuid

// Generate random UUID
val id = Uuid.random()

// Parse UUID
val parsed = Uuid.parse("550e8400-e29b-41d4-a716-446655440000")

// Convert to/from Java UUID (JVM only)
val javaUuid = id.toJavaUuid()
val kotlinUuid = javaUuid.toKotlinUuid()
```

### Time API (1.9.0+)

```kotlin
import kotlin.time.*

// Measure execution time
val duration = measureTime {
    performOperation()
}

// Mark and measure
val mark = TimeSource.Monotonic.markNow()
performOperation()
val elapsed = mark.elapsedNow()

// Duration arithmetic
val timeout = 5.seconds
val remaining = timeout - elapsed
```

## Preview Feature Flags

Enable preview features in Gradle:

```kotlin
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-parameters",           // Context parameters (2.2.0)
            "-Xcontext-sensitive-resolution", // Context-sensitive resolution (2.2.0)
            "-Xnested-type-aliases",          // Nested type aliases (2.2.0)
            "-Xannotation-target-all"         // @all annotation target (2.2.0)
        )
    }
}
```

## K2 Compiler

The K2 compiler became stable in Kotlin 2.0.0. Key benefits:
- Up to 2x faster compilation
- Unified architecture across all platforms
- Better smart cast analysis
- Foundation for future language features

K2 is enabled by default starting from Kotlin 2.0.0. No configuration needed.
