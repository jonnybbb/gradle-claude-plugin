# Kotlin 2.1.x Reference

Major features: Guard conditions, non-local break/continue, multi-dollar interpolation (all preview), basic Swift export, stable Multiplatform DSL.

## Table of Contents
- [Guard Conditions in When (Preview)](#guard-conditions-in-when-preview)
- [Non-local Break and Continue (Preview)](#non-local-break-and-continue-preview)
- [Multi-Dollar String Interpolation (Preview)](#multi-dollar-string-interpolation-preview)
- [SubclassOptInRequired Annotation](#subclassoptinrequired-annotation)
- [Basic Swift Export](#basic-swift-export)
- [Common Atomic Types (2.1.20)](#common-atomic-types-2120)
- [Time API: Instant and Clock (2.1.20)](#time-api-instant-and-clock-2120)
- [UUID Improvements (2.1.20)](#uuid-improvements-2120)
- [Kotlin Multiplatform Updates](#kotlin-multiplatform-updates)
- [Kotlin/Native Updates](#kotlinnative-updates)
- [Kotlin/Wasm Updates](#kotlinwasm-updates)
- [Compose Compiler Updates](#compose-compiler-updates)

---

## Guard Conditions in When (Preview)

Guard conditions allow additional conditions in `when` branches using `if`.

```kotlin
// Enable with compiler option:
// -Xwhen-guards

// Basic guard condition
fun describe(value: Any) = when (value) {
    is String if value.isEmpty() -> "empty string"
    is String if value.length > 100 -> "long string"
    is String -> "string: $value"
    is Int if value < 0 -> "negative number"
    is Int if value == 0 -> "zero"
    is Int -> "positive number"
    else -> "unknown"
}

// Multiple conditions
fun categorize(obj: Any) = when (obj) {
    is List<*> if obj.isEmpty() -> "empty list"
    is List<*> if obj.all { it is String } -> "string list"
    is List<*> -> "mixed list"
    else -> "not a list"
}

// Guard with else if
fun process(value: Int?) = when {
    value == null -> "null"
    value < 0 -> "negative"
    else if value == 0 -> "zero"  // else if supported
    else -> "positive"
}
```

**Enable in Gradle:**
```kotlin
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xwhen-guards")
    }
}
```

---

## Non-local Break and Continue (Preview)

Use `break` and `continue` in lambdas passed to inline functions.

```kotlin
// Enable with compiler option:
// -Xnon-local-break-continue

// Non-local break
fun findFirst(items: List<List<String>>) {
    for (list in items) {
        list.forEach { item ->
            if (item == "target") {
                break  // Breaks outer for loop
            }
            println(item)
        }
    }
}

// Non-local continue
fun processAll(items: List<List<String>>) {
    for (list in items) {
        list.forEach { item ->
            if (item.isEmpty()) {
                continue  // Continues to next iteration of outer loop
            }
            println(item)
        }
    }
}

// Works with labeled loops
outer@ for (i in 1..10) {
    (1..10).forEach { j ->
        if (i * j > 50) break@outer
        println("$i * $j = ${i * j}")
    }
}
```

**Enable in Gradle:**
```kotlin
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xnon-local-break-continue")
    }
}
```

---

## Multi-Dollar String Interpolation (Preview)

Control string interpolation with multiple dollar signs.

```kotlin
// Enable with compiler option:
// -Xmulti-dollar-interpolation

// Two dollar signs: $$ triggers interpolation
val name = "World"
val json = $$"""
{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "https://example.com/schemas/user",
    "name": "$${name}"
}
""".trimIndent()
// $schema and $id are literal, $${name} is interpolated

// More dollars = higher threshold
val template = $$$"""
    Price: $100 (literal)
    Discount: $$50 (literal)
    Total: $$${calculateTotal()} (interpolated)
""".trimIndent()
```

**Use cases:**
- JSON schemas with `$schema`, `$ref` fields
- Template engines using `$` for placeholders
- Regular expressions with `$` anchors
- Shell scripts with `$` variables

---

## SubclassOptInRequired Annotation

Require opt-in to extend experimental APIs.

```kotlin
@RequiresOptIn
annotation class ExperimentalApi

@SubclassOptInRequired(ExperimentalApi::class)
interface SharedFlow<T> : Flow<T> {
    val replayCache: List<T>
}

// Users must opt in to implement
@OptIn(ExperimentalApi::class)
class MySharedFlow<T> : SharedFlow<T> {
    override val replayCache: List<T> = emptyList()
    // ...
}
```

---

## Basic Swift Export

Export Kotlin code directly to Swift without Objective-C headers.

```kotlin
// build.gradle.kts
kotlin {
    iosArm64 {
        binaries.framework {
            baseName = "SharedModule"
        }
    }
    
    // Enable Swift export
    @OptIn(ExperimentalSwiftExportDsl::class)
    swiftExport {
        moduleName = "SharedKotlin"
        flattenPackage = "com.example.shared"
    }
}

// Enable in gradle.properties
kotlin.experimental.swift-export.enabled=true
```

**Swift Export Features:**
- Export multiple Gradle modules to Swift
- Custom Swift module names
- Package flattening rules
- Direct integration without Objective-C layer

---

## Common Atomic Types (2.1.20)

Atomic types available in common code for thread-safe operations.

```kotlin
import kotlin.concurrent.atomics.*

@OptIn(ExperimentalAtomicApi::class)
fun main() {
    // Atomic integers
    val counter = AtomicInt(0)
    counter.incrementAndGet()
    counter.addAndGet(5)
    val current = counter.load()
    counter.store(10)
    
    // Compare and swap
    val success = counter.compareAndSet(expect = 10, update = 20)
    
    // Atomic longs
    val longCounter = AtomicLong(0L)
    longCounter.addAndGet(100L)
    
    // Atomic references
    val atomicRef = AtomicReference<String?>(null)
    atomicRef.store("Hello")
    val value = atomicRef.load()
    
    // Atomic boolean
    val flag = AtomicBoolean(false)
    flag.store(true)
    
    // JVM interop
    val javaAtomic = counter.asJavaAtomic()
    val kotlinAtomic = javaAtomic.asKotlinAtomic()
}
```

---

## Time API: Instant and Clock (2.1.20)

Common `Instant` and `Clock` types added to stdlib.

```kotlin
import kotlin.time.*

@OptIn(ExperimentalTime::class)
fun main() {
    // Get current instant
    val now = Clock.System.now()
    
    // Create instant from epoch
    val epoch = Instant.fromEpochMilliseconds(0)
    val specific = Instant.fromEpochSeconds(1704067200, 0) // Jan 1, 2024
    
    // Duration arithmetic
    val later = now + 1.hours
    val earlier = now - 30.minutes
    val diff = later - earlier
    
    // Comparisons
    if (now > epoch) {
        println("After epoch")
    }
    
    // Convert to/from epoch
    val epochMs = now.toEpochMilliseconds()
    val epochSec = now.epochSeconds
    
    // JVM interop
    val javaInstant = now.toJavaInstant()
    val kotlinInstant = javaInstant.toKotlinInstant()
    
    // Custom clock for testing
    val fixedClock = object : Clock {
        override fun now(): Instant = specific
    }
}
```

---

## UUID Improvements (2.1.20)

Enhanced UUID API with better parsing and formatting.

```kotlin
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun main() {
    // Parse both formats
    val uuid1 = Uuid.parse("550e8400-e29b-41d4-a716-446655440000")  // hex-dash
    val uuid2 = Uuid.parse("550e8400e29b41d4a716446655440000")     // plain hex
    
    // Explicit format parsing
    val fromHexDash = Uuid.parseHexDash("550e8400-e29b-41d4-a716-446655440000")
    val fromHex = Uuid.parseHex("550e8400e29b41d4a716446655440000")
    
    // Explicit format output
    val hexDashStr = uuid1.toHexDashString()  // "550e8400-e29b-41d4-a716-446655440000"
    val hexStr = uuid1.toHexString()          // "550e8400e29b41d4a716446655440000"
    
    // UUIDs are now Comparable
    val uuids = listOf(uuid1, uuid2, Uuid.random())
    val sorted = uuids.sorted()
    
    // Comparison operators
    if (uuid1 < uuid2) {
        println("uuid1 comes before uuid2")
    }
}
```

---

## Kotlin Multiplatform Updates

### Stable Compiler Options DSL

```kotlin
// Extension level (all targets)
kotlin {
    compilerOptions {
        allWarningsAsErrors.set(true)
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

// Target level
kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

// Compilation level
tasks.named<KotlinCompile>("compileKotlinJvm") {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}
```

### Preview: Gradle Isolated Projects

```kotlin
// gradle.properties
org.gradle.configuration-cache.parallel=true
org.gradle.unsafe.isolated-projects=true

// Or just Kotlin plugin compatibility:
kotlin.kmp.isolated-projects.support=enable
```

### Publish Kotlin Libraries from Any Host

```kotlin
// gradle.properties
kotlin.native.enableKlibsCrossCompilation=true

// Can now build .klib artifacts for all targets from any host
// (Still need Mac for final iOS binaries)
```

---

## Kotlin/Native Updates

### LLVM Update (1.9.20 → 2.1.0: 11.1.0 → 16.0.0)

Better performance, bug fixes, security updates.

### iosArm64 Promoted to Tier 1

Full CI testing and source/binary compatibility guarantees.

### Deprecation of mimalloc Allocator

```kotlin
// Remove from gradle.properties:
// kotlin.native.binary.allocator=mimalloc

// New custom allocator is default
```

---

## Kotlin/Wasm Updates

### Incremental Compilation Support

```kotlin
// gradle.properties
kotlin.incremental.wasm=true
```

### kotlinx-browser Library

```kotlin
// Add dependency for browser APIs
implementation("org.jetbrains.kotlinx:kotlinx-browser:0.1")
```

### Custom Debugger Formatters

Enabled by default in development builds. Shows Kotlin values properly in browser DevTools.

---

## Compose Compiler Updates

### Support for Default Parameters in Open Functions

```kotlin
@Composable
open fun Greeting(
    name: String = "World",  // Now supported in open functions
    modifier: Modifier = Modifier
) {
    Text("Hello, $name!", modifier = modifier)
}
```

### Virtual Functions Can Be Restartable

```kotlin
// Functions that are final can now be restarted/skipped as usual
final class MyComponent {
    @Composable
    override fun Content() {  // Can be restartable now
        // ...
    }
}
```

### Source Information Enabled by Default

Composition trace markers included for all platforms.

---

## Gradle Updates

### Kotlin Gradle Plugin Compatible with Isolated Projects

```kotlin
// No longer need kotlin.kmp.isolated-projects.support=enable
// Compatibility built into KGP 2.1.20
```

### Custom Publication Variants

```kotlin
@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    jvm {
        // Create custom publication variant
        val customVariant = adhocSoftwareComponent()
        customVariant.addVariantsFromConfiguration(
            configurations.getByName("customConfiguration")
        ) { }
    }
}
```

---

## Migration Checklist

- [ ] Update Kotlin version to 2.1.x in build scripts
- [ ] Try preview features (guard conditions, multi-dollar interpolation)
- [ ] Update compiler options to use new DSL
- [ ] Consider using common atomic types instead of platform-specific
- [ ] Migrate to kotlin.time.Instant/Clock if using kotlinx-datetime
- [ ] Update UUID usage to leverage new parsing/formatting functions
- [ ] Enable incremental compilation for Kotlin/Wasm if using it
- [ ] Review Compose functions with default parameters in open classes
- [ ] Test with Gradle Isolated Projects if applicable
