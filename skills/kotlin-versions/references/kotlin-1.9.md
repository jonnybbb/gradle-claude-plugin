# Kotlin 1.9.x Reference

Major features: K2 compiler Beta, data objects, stable time API, open-ended ranges, Kotlin Multiplatform stabilization.

## Table of Contents
- [K2 Compiler (Beta)](#k2-compiler-beta)
- [Data Objects](#data-objects)
- [Open-Ended Ranges (..<)](#open-ended-ranges-)
- [Enum Entries Property](#enum-entries-property)
- [Stable Time API](#stable-time-api)
- [Inline Value Class Improvements](#inline-value-class-improvements)
- [Kotlin Multiplatform Stable](#kotlin-multiplatform-stable)
- [Kotlin/Native Updates](#kotlinnative-updates)
- [Kotlin/Wasm Alpha](#kotlinwasm-alpha)

---

## K2 Compiler (Beta)

The K2 compiler reached Beta status in 1.9.0, bringing significant performance improvements.

```kotlin
// Enable K2 in gradle.properties (1.9.x)
kotlin.experimental.tryK2=true

// Or set language version to 2.0 in build.gradle.kts
kotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}
```

**K2 Benefits:**
- Faster compilation (up to 2x improvement)
- Unified frontend architecture
- Better IDE support foundation
- Platform unification (JVM, Native, JS, Wasm)

---

## Data Objects

Stable in 1.9.0. `data object` provides proper `toString()`, `equals()`, and `hashCode()` for singleton objects.

```kotlin
// Before: plain object had generic toString()
sealed interface Response {
    data class Success(val data: String) : Response
    object Loading : Response  // toString() = "com.example.Response$Loading@1234"
}

// After: data object has meaningful toString()
sealed interface Response {
    data class Success(val data: String) : Response
    data object Loading : Response  // toString() = "Loading"
}

// Usage in sealed hierarchies
fun handle(response: Response) = when (response) {
    is Response.Success -> println(response.data)
    Response.Loading -> println("Loading...")
}
```

**Key characteristics:**
- `toString()` returns the object name
- `equals()` uses referential equality (singleton)
- `hashCode()` is consistent
- No `copy()` or `componentN()` functions (meaningless for singletons)

---

## Open-Ended Ranges (..<)

Stable in 1.9.0. The `..<` operator creates ranges excluding the upper bound.

```kotlin
// Before: using until function
for (i in 0 until 10) { }
val range = 0 until list.size

// After: using ..< operator
for (i in 0 ..< 10) { }
val range = 0 ..< list.size

// More readable in various contexts
when (value) {
    in 0 ..< 10 -> "single digit"
    in 10 ..< 100 -> "double digit"
    else -> "large"
}

// Works with any Comparable
val charRange = 'a' ..< 'z'  // 'a' to 'y'
```

---

## Enum Entries Property

Stable in 1.9.0. `entries` replaces `values()` for better performance.

```kotlin
enum class Direction { NORTH, SOUTH, EAST, WEST }

// Before: values() creates a new array every time
val directions = Direction.values()  // Array<Direction>

// After: entries returns an immutable list (cached)
val directions = Direction.entries  // List<Direction>

// Generic function replacement
// Before
inline fun <reified T : Enum<T>> printAll() {
    enumValues<T>().forEach { println(it) }
}

// After (Experimental in 1.9.0, Stable in 1.9.20)
inline fun <reified T : Enum<T>> printAll() {
    enumEntries<T>().forEach { println(it) }
}
```

**Benefits:**
- No array allocation on each call
- Returns immutable `List<E>` instead of `Array<E>`
- Better for iteration and functional operations

---

## Stable Time API

The `kotlin.time` API became stable in 1.9.0.

```kotlin
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.milliseconds

// Duration creation
val timeout = 5.seconds
val delay = 100.milliseconds
val combined = 1.seconds + 500.milliseconds

// Measuring time
val duration = measureTime {
    performOperation()
}
println("Took: $duration")

// Measuring with result
val (result, duration) = measureTimedValue {
    computeValue()
}

// Time marks for elapsed time
val mark = TimeSource.Monotonic.markNow()
performOperation()
val elapsed = mark.elapsedNow()

// Check if time has passed
val deadline = mark + 5.seconds
if (deadline.hasPassedNow()) {
    println("Timeout!")
}

// Duration arithmetic
val remaining = timeout - elapsed
val doubled = timeout * 2
val halved = timeout / 2
```

---

## Inline Value Class Improvements

Secondary constructors with bodies became stable in 1.9.0.

```kotlin
@JvmInline
value class Email(val value: String) {
    init {
        require(value.contains("@")) { "Invalid email" }
    }
    
    // Secondary constructor with body (new in 1.9.0)
    constructor(user: String, domain: String) : this("$user@$domain") {
        require(user.isNotBlank()) { "User cannot be blank" }
        require(domain.isNotBlank()) { "Domain cannot be blank" }
    }
}

@JvmInline
value class PositiveInt private constructor(val value: Int) {
    companion object {
        fun of(value: Int): PositiveInt {
            require(value > 0) { "Value must be positive" }
            return PositiveInt(value)
        }
    }
}
```

---

## Kotlin Multiplatform Stable

Kotlin Multiplatform reached Stable status in 1.9.20.

```kotlin
// Default hierarchy template (1.9.20+)
// Source sets created automatically based on targets
kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()
    // iosMain source set created automatically
}

// Renamed android block to androidTarget (1.9.20+)
kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
}

// Full Gradle configuration cache support (1.9.20)
// Enable in gradle.properties:
org.gradle.configuration-cache=true
```

---

## Kotlin/Native Updates

### Custom Memory Allocator (1.9.0+)

```kotlin
// Enable custom allocator in gradle.properties
kotlin.native.binary.memoryModel=experimental
```

### Improved GC Performance (1.9.20)
- Full parallel mark phase
- Better memory tracking
- Reduced GC pause times

### Incremental Compilation for klib (1.9.20)

```properties
# Enable in gradle.properties
kotlin.incremental.native=true
```

---

## Kotlin/Wasm Alpha

Kotlin/Wasm reached Alpha in 1.9.20.

```kotlin
// build.gradle.kts
kotlin {
    wasmJs {
        browser()
        binaries.executable()
    }
    // Or for WASI
    wasmWasi {
        nodejs()
        binaries.executable()
    }
}

// JavaScript interop
external fun alert(message: String)

@JsExport
fun greet(name: String): String = "Hello, $name!"

// WASI example
@WasmImport("wasi_snapshot_preview1", "fd_write")
external fun fdWrite(fd: Int, iovs: Int, iovsLen: Int, nwritten: Int): Int
```

---

## Standard Library Updates

### HexFormat (Experimental in 1.9.0)

```kotlin
@OptIn(ExperimentalStdlibApi::class)
fun main() {
    val bytes = byteArrayOf(0x48, 0x65, 0x6c, 0x6c, 0x6f)
    
    // Default formatting
    println(bytes.toHexString())  // "48656c6c6f"
    
    // Custom format
    val format = HexFormat {
        upperCase = true
        bytes.byteSeparator = " "
    }
    println(bytes.toHexString(format))  // "48 65 6C 6C 6F"
    
    // Parse hex string
    val parsed = "48656c6c6f".hexToByteArray()
}
```

### Regex Named Groups

```kotlin
val regex = """(?<year>\d{4})-(?<month>\d{2})-(?<day>\d{2})""".toRegex()
val match = regex.find("2024-01-15")

// Access by name (common function in 1.9.0)
val year = match?.groups?.get("year")?.value
val month = match?.groups?.get("month")?.value
```

---

## Migration Checklist

- [ ] Update Kotlin version to 1.9.x in build scripts
- [ ] Replace `values()` with `entries` for enum classes
- [ ] Replace `until` with `..<` for open-ended ranges
- [ ] Use `data object` for singleton sealed class members
- [ ] Migrate from experimental time API imports to stable ones
- [ ] Rename `android` block to `androidTarget` in multiplatform projects
- [ ] Consider enabling K2 compiler for testing (`kotlin.experimental.tryK2=true`)
- [ ] Enable Gradle configuration cache if applicable
