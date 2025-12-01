# Kotlin 2.0.x Reference

Major milestone: K2 compiler becomes Stable. Also includes smart cast improvements, new Compose compiler plugin, UUID support.

## Table of Contents
- [K2 Compiler Stable](#k2-compiler-stable)
- [Smart Cast Improvements](#smart-cast-improvements)
- [Compose Compiler Plugin](#compose-compiler-plugin)
- [Lambda Generation with invokedynamic](#lambda-generation-with-invokedynamic)
- [UUID Support (2.0.20)](#uuid-support-2020)
- [Data Class Copy Visibility (2.0.20)](#data-class-copy-visibility-2020)
- [Kotlin Multiplatform Updates](#kotlin-multiplatform-updates)
- [Kotlin/Native Improvements](#kotlinnative-improvements)
- [Kotlin/Wasm Updates](#kotlinwasm-updates)
- [Standard Library Updates](#standard-library-updates)

---

## K2 Compiler Stable

The K2 compiler is now stable and enabled by default in Kotlin 2.0.0.

```kotlin
// K2 is now the default compiler - no configuration needed

// If you need to specify language version explicitly:
kotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}
```

**K2 Compiler Benefits:**
- Up to 94% faster compilation on large projects
- Unified frontend for all targets (JVM, Native, JS, Wasm)
- Better smart cast analysis
- Foundation for new language features
- Improved IDE performance (with K2 mode in IntelliJ)

**Enable K2 Mode in IntelliJ IDEA:**
Settings → Languages & Frameworks → Kotlin → Enable K2 mode

---

## Smart Cast Improvements

K2 brings significant improvements to smart casting.

### Local Variables and Further Scopes

```kotlin
// Before K2: smart cast didn't work for variables declared outside if
fun process(value: Any) {
    val isString = value is String
    if (isString) {
        // K1: value is still Any, K2: value is smart cast to String
        println(value.length)
    }
}

// K2: works correctly with boolean conditions
val condition = value != null && value.isNotEmpty()
if (condition) {
    println(value.length)  // Smart cast works
}
```

### Type Checks with Logical OR

```kotlin
interface A { val a: String }
interface B { val b: String }
class C : A, B { override val a = "a"; override val b = "b" }

fun process(obj: Any) {
    if (obj is A || obj is B) {
        // K2: smart cast to common supertype
        println(obj)  // Now works correctly
    }
}
```

### Inline Functions

```kotlin
fun process(input: String?) {
    // K2 treats inline functions as having implicit callsInPlace contract
    input?.let {
        // it is smart cast to String
        println(it.length)
    }
    
    // Variables captured in lambdas passed to inline functions
    // can be smart cast after the call
    if (input != null) {
        run { }
        println(input.length)  // Works in K2
    }
}
```

### Properties with Function Types

```kotlin
class Handler {
    val onClick: (() -> Unit)? = null
}

fun process(handler: Handler) {
    if (handler.onClick != null) {
        handler.onClick()  // K2: smart cast works
    }
}
```

### Exception Handling

```kotlin
fun process(value: String?) {
    try {
        if (value == null) throw IllegalArgumentException()
        // value is smart cast to String
    } catch (e: Exception) {
        // K2: compiler tracks nullability through catch blocks
    }
}
```

---

## Compose Compiler Plugin

The Compose compiler moved to the Kotlin repository in 2.0.0.

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.compose") version "2.0.0"
}

// For multiplatform projects
plugins {
    kotlin("multiplatform") version "2.0.0"
    kotlin("plugin.compose") version "2.0.0"
    id("org.jetbrains.compose") version "1.6.0"
}
```

**Compose Compiler 2.0.0 Improvements:**
- Strong skipping mode (experimental)
- Better performance for non-skippable groups
- Improved source information for debugging

---

## Lambda Generation with invokedynamic

Kotlin 2.0.0 uses `invokedynamic` for lambda generation by default on JVM.

```kotlin
// Before: lambdas compiled as anonymous classes
val action = { println("Hello") }  // Created LambdaImpl$1 class

// After (2.0.0): uses invokedynamic (smaller bytecode)
// No separate class file generated

// To restore old behavior:
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xlambdas=class")
    }
}
```

**Benefits:**
- Smaller binary size
- Faster cold start times
- Better alignment with JVM optimizations

**Limitations:**
- Lambda compiled to invokedynamic is not serializable
- `reflect()` doesn't work on these lambdas

---

## UUID Support (2.0.20)

Common UUID support added to the standard library.

```kotlin
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun main() {
    // Generate random UUID
    val id = Uuid.random()
    
    // Parse from string
    val parsed = Uuid.parse("550e8400-e29b-41d4-a716-446655440000")
    
    // Create from components
    val custom = Uuid.fromLongs(
        mostSignificantBits = 0x550e8400e29b41d4L,
        leastSignificantBits = 0xa716446655440000L
    )
    
    // Convert to string
    val str = id.toString()  // "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
    
    // JVM interop
    val javaUuid = id.toJavaUuid()
    val kotlinUuid = javaUuid.toKotlinUuid()
}
```

---

## Data Class Copy Visibility (2.0.20)

The `copy()` function visibility now matches the constructor visibility.

```kotlin
// Issue: copy() was always public, even with private constructor
data class User private constructor(
    val id: Int,
    val name: String
) {
    companion object {
        fun create(name: String) = User(nextId(), name)
    }
}

// Before 2.0.20: this worked (unintended)
val copy = user.copy(name = "Other")

// After 2.0.20: opt-in to new behavior
@ConsistentCopyVisibility
data class User private constructor(
    val id: Int,
    val name: String
)
// Now copy() is private, user.copy() causes compile error

// Enable module-wide in gradle.properties:
// kotlin.jvm.target.validation.mode=warning
// Or compiler option: -Xconsistent-data-class-copy-visibility
```

---

## Kotlin Multiplatform Updates

### New Gradle DSL for Compiler Options

```kotlin
// Project-level compiler options
kotlin {
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    
    // Target-level options
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
```

### Expected and Actual Declarations Improvements

```kotlin
// Different visibility levels now allowed
// Common code
expect internal class PlatformLogger

// Platform code (can be more permissive)
actual public class PlatformLogger
```

### Separated Common and Platform Sources

K2 enforces strict separation between common and platform source sets.

```kotlin
// Common code can no longer accidentally access platform code
// Function resolution is now consistent across platforms
```

---

## Kotlin/Native Improvements

### GC Performance Monitoring with Signposts

```kotlin
// On Apple platforms, GC reports pauses with signposts
// Visible in Xcode Instruments for debugging

// Enable in gradle.properties:
kotlin.native.binary.gc=cms
```

### Objective-C Conflict Resolution

```kotlin
// @ObjCSignatureOverride for handling conflicting overloads
class MyDelegate : CLLocationManagerDelegate {
    @ObjCSignatureOverride
    override fun locationManager(
        manager: CLLocationManager,
        didEnterRegion: CLRegion
    ) { }
    
    @ObjCSignatureOverride
    override fun locationManager(
        manager: CLLocationManager,
        didExitRegion: CLRegion
    ) { }
}
```

---

## Kotlin/Wasm Updates

### Named Exports

```kotlin
// Kotlin/Wasm now uses named exports instead of default exports
@JsExport
fun greet(name: String): String = "Hello, $name!"

// JavaScript usage:
// import { greet } from './module.mjs';
// greet("World");
```

### Binaryen Optimization

Binaryen optimizer is now enabled by default for production builds.

```kotlin
// build.gradle.kts
kotlin {
    wasmJs {
        browser()
        binaries.executable()
        // Binaryen runs automatically for production
    }
}
```

### TypeScript Definition Generation

```kotlin
// build.gradle.kts
kotlin {
    wasmJs {
        generateTypeScriptDefinitions()  // Generates .d.ts files
    }
}
```

---

## Standard Library Updates

### Stable AutoCloseable Interface

```kotlin
// AutoCloseable now available in common code
class MyResource : AutoCloseable {
    override fun close() {
        println("Closing resource")
    }
}

// Use with use() extension
MyResource().use { resource ->
    // work with resource
}  // automatically closed
```

### Common String.toCharArray(destination)

```kotlin
val str = "Hello"
val buffer = CharArray(10)
str.toCharArray(buffer)  // Common function now
```

### Stable enumValues/enumEntries

```kotlin
// enumEntries<T>() is now stable replacement for enumValues<T>()
inline fun <reified T : Enum<T>> printAllEntries() {
    enumEntries<T>().forEach { println(it) }
}
```

---

## Gradle Updates

### New Gradle DSL for Compiler Options

```kotlin
// Replaces kotlinOptions (deprecated)
tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

// kotlin.compilerOptions for whole project
kotlin {
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}
```

### JVM and Android Published Library Attribute

```kotlin
// org.gradle.jvm.environment attribute published by default
// Helps distinguish JVM and Android variants

// To disable:
// gradle.properties
kotlin.publishJvmEnvironmentAttribute=false
```

---

## Migration Checklist

- [ ] Update Kotlin version to 2.0.x in build scripts
- [ ] Update Compose compiler plugin to 2.0.x
- [ ] Replace `kotlinOptions {}` with `compilerOptions {}`
- [ ] Test smart cast behavior changes in K2
- [ ] Review data class copy() usage with private constructors
- [ ] Enable K2 mode in IntelliJ IDEA for better IDE experience
- [ ] Update Kotlin/Wasm imports to use named exports
- [ ] Consider enabling Gradle configuration cache
- [ ] Review lambda serialization if using Java serialization
