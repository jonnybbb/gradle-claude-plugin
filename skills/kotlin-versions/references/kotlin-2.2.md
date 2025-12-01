# Kotlin 2.2.x Reference

Major features: Context parameters, context-sensitive resolution, nested type aliases (all preview). Stable guard conditions, multi-dollar interpolation, Base64/HexFormat APIs.

## Table of Contents
- [Context Parameters (Preview)](#context-parameters-preview)
- [Context-Sensitive Resolution (Preview)](#context-sensitive-resolution-preview)
- [Nested Type Aliases (Beta)](#nested-type-aliases-beta)
- [Stable Guard Conditions](#stable-guard-conditions)
- [Stable Multi-Dollar String Interpolation](#stable-multi-dollar-string-interpolation)
- [Annotation @all Meta-Target (Preview)](#annotation-all-meta-target-preview)
- [Kotlin/JVM Updates](#kotlinjvm-updates)
- [Kotlin/Native Updates](#kotlinnative-updates)
- [Kotlin/Wasm Updates](#kotlinwasm-updates)
- [Standard Library Updates](#standard-library-updates)
- [Compose Compiler Updates](#compose-compiler-updates)
- [Gradle Updates](#gradle-updates)

---

## Context Parameters (Preview)

Context parameters replace context receivers with a cleaner design.

```kotlin
// Enable with: -Xcontext-parameters

// Declare context parameter
context(logger: Logger)
fun processData(data: String) {
    logger.log("Processing: $data")  // Access via name
    // Note: unlike context receivers, no implicit 'this' scope
}

// Use underscore to make available for resolution without naming
context(_: Logger)
fun logMessage(msg: String) {
    log(msg)  // Resolved from context
}

// Multiple context parameters
context(logger: Logger, config: AppConfig)
fun initializeApp() {
    logger.log("Starting with config: ${config.appName}")
}

// Usage
fun main() {
    val logger = ConsoleLogger()
    val config = AppConfig("MyApp")
    
    with(logger) {
        with(config) {
            initializeApp()
        }
    }
}

// Context parameters on properties
context(logger: Logger)
val debugEnabled: Boolean
    get() {
        logger.log("Checking debug status")
        return BuildConfig.DEBUG
    }
```

**Key Differences from Context Receivers:**
- Must use parameter name to access members (no implicit scope)
- Cleaner and more explicit semantics
- Better IDE support and error messages

**Enable in Gradle:**
```kotlin
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}
```

---

## Context-Sensitive Resolution (Preview)

Omit type names for enum entries and sealed class members when type is known.

```kotlin
// Enable with: -Xcontext-sensitive-resolution

enum class Color { RED, GREEN, BLUE }
sealed interface Result {
    data class Success(val data: String) : Result
    data object Error : Result
}

// Before: full qualification required
fun setColor(color: Color) { }
setColor(Color.RED)

// After: type can be inferred
fun setColor(color: Color) { }
setColor(.RED)  // Compiler infers Color.RED

// Works in various contexts
fun process(result: Result): String = when (result) {
    is .Success -> result.data  // Result.Success inferred
    .Error -> "error"           // Result.Error inferred
}

// Explicit return type
fun getDefaultColor(): Color = .BLUE

// Variable with declared type
val defaultColor: Color = .RED

// When subject type inference
fun describe(color: Color) = when (color) {
    .RED -> "warm"
    .GREEN -> "natural"
    .BLUE -> "cool"
}

// Type checks and casts
val result: Result = getResult()
if (result is .Success) {
    println(result.data)
}
```

**Enable in Gradle:**
```kotlin
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-sensitive-resolution")
    }
}
```

---

## Nested Type Aliases (Beta)

Define type aliases inside classes and objects.

```kotlin
// Enable with: -Xnested-type-aliases

class UserRepository {
    // Type alias nested inside class
    typealias Id = Long
    typealias Callback = (User) -> Unit
    
    fun findById(id: Id): User? = TODO()
    fun observe(callback: Callback) { }
}

// Usage
fun createUser(): UserRepository.Id = 123L

object Config {
    typealias Port = Int
    typealias Host = String
    
    data class ServerConfig(val host: Host, val port: Port)
}

val config = Config.ServerConfig("localhost", 8080)

// Constraints:
// - Cannot capture type parameters from outer class
class Container<T> {
    // typealias Element = T  // Error: can't reference T
    typealias Callback = () -> Unit  // OK: doesn't use T
}
```

**Enable in Gradle:**
```kotlin
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xnested-type-aliases")
    }
}
```

---

## Stable Guard Conditions

Guard conditions in `when` are now stable (promoted from preview in 2.1.0).

```kotlin
// No compiler flag needed in 2.2.0+

sealed interface Response {
    data class Success(val data: String) : Response
    data class Error(val code: Int, val message: String) : Response
}

fun handle(response: Response) = when (response) {
    is Response.Success if response.data.isEmpty() -> "Empty success"
    is Response.Success -> "Success: ${response.data}"
    is Response.Error if response.code == 404 -> "Not found"
    is Response.Error if response.code >= 500 -> "Server error: ${response.message}"
    is Response.Error -> "Error ${response.code}"
}

// Complex guards
fun categorize(value: Any?) = when (value) {
    null -> "null"
    is String if value.isBlank() -> "blank string"
    is String if value.length < 10 -> "short string"
    is String -> "long string"
    is List<*> if value.isEmpty() -> "empty list"
    is List<*> if value.size > 100 -> "large list"
    is List<*> -> "list of ${value.size} items"
    else -> "other: $value"
}
```

---

## Stable Multi-Dollar String Interpolation

Multi-dollar interpolation is now stable (promoted from preview in 2.1.0).

```kotlin
// No compiler flag needed in 2.2.0+

// JSON schema example
val name = "user"
val schema = $$"""
{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "https://example.com/$${name}.schema.json",
    "type": "object",
    "properties": {
        "name": { "type": "string" }
    }
}
""".trimIndent()

// Regular expressions
val regex = $$"""^\$\d+\.\d{2}$$"""  // Matches prices like $10.99

// Shell script templates
val script = $$"""
#!/bin/bash
VAR=$1
echo "Argument: $VAR"
echo "From Kotlin: $${kotlinVar}"
"""

// Multiple dollar levels
val template = $$$"""
    Single: $var (literal)
    Double: $$var (literal)  
    Triple: $$${expr} (interpolated)
"""
```

---

## Annotation @all Meta-Target (Preview)

Apply annotation to all applicable targets of a property.

```kotlin
// Enable with: -Xannotation-target-all

@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY_GETTER
)
annotation class Validated

// Before: had to annotate each target
data class User(
    @param:Validated
    @property:Validated
    @field:Validated
    @get:Validated
    val email: String
)

// After: @all applies to all applicable targets
data class User(
    @all:Validated val email: String
)

// With JVM records
@JvmRecord
data class User(
    @all:Email val email: String  // Also applies to RECORD_COMPONENT
)
```

**Enable in Gradle:**
```kotlin
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-target-all")
    }
}
```

---

## Kotlin/JVM Updates

### JVM Default Methods for Interface Functions

```kotlin
// Interface functions now compile to JVM default methods by default
interface Greeting {
    fun greet(): String = "Hello!"  // Generates JVM default method
}

// Control with -jvm-default option:
// - enable (default): generates defaults + compatibility bridges
// - no-compatibility: generates only defaults (smaller bytecode)
// - disable: old behavior (DefaultImpls class)

kotlin {
    compilerOptions {
        jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
    }
}
```

### @JvmExposeBoxed for Inline Value Classes

```kotlin
@JvmInline
value class UserId(val id: Long)

// Before: Java couldn't easily use value classes
// After: expose boxed version to Java
@JvmExposeBoxed
@JvmInline
value class UserId(val id: Long)

// Now Java can use:
// UserId userId = new UserId(123L);

// Can apply at function level too
@JvmInline
value class Email(val value: String) {
    @JvmExposeBoxed
    fun getDomain(): String = value.substringAfter("@")
}

// Module-wide: -Xjvm-expose-boxed compiler option
```

### Annotations in Kotlin Metadata

```kotlin
// Enable writing annotations to metadata
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xwrite-kmetadata-annotations")
    }
}

// Read annotations via kotlin-metadata-jvm library
@OptIn(ExperimentalAnnotationsInMetadata::class)
fun readAnnotations(klass: KmClass) {
    klass.annotations.forEach { annotation ->
        println(annotation)
    }
}
```

### Java 24 Bytecode Support

```kotlin
kotlin {
    jvmToolchain(24)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_24)
    }
}
```

---

## Kotlin/Native Updates

### LLVM Update to 19

Better performance, bug fixes, security updates.

### Per-Object Memory Allocation (Experimental)

```kotlin
// gradle.properties
kotlin.native.binary.pagedAllocator=false
```

### Latin-1 String Encoding Support (Experimental)

```kotlin
// gradle.properties  
kotlin.native.binary.useLatin1ForStringEncoding=true
```

Reduces memory for ASCII-only strings.

### Memory Tagging on Apple Platforms

GC memory now tagged (ID 246) for debugging with Xcode Instruments VM Tracker.

---

## Kotlin/Wasm Updates

### Separated Build Infrastructure

```kotlin
// wasmJs and js targets now have separate infrastructure
kotlin {
    wasmJs {
        browser()
        // Uses build/wasm directory
        // Separate NPM tasks: wasmJsNpmInstall, etc.
    }
    js {
        browser()
        // Uses build/js directory
    }
}
```

### Per-Project Binaryen Configuration

```kotlin
kotlin {
    wasmJs {
        browser()
        // Configure Binaryen per project
        applyBinaryen {
            binaryenArgs = listOf("-O3", "--enable-gc")
        }
    }
}
```

### Custom Formatters in Dev Builds

Enabled by default - shows Kotlin values properly in browser DevTools.

---

## Standard Library Updates

### Stable Base64 Encoding/Decoding

```kotlin
import kotlin.io.encoding.Base64

// Encoding schemes
val data = "Hello, World!".encodeToByteArray()

// Default (RFC 4648)
val base64 = Base64.Default.encode(data)
val decoded = Base64.Default.decode(base64)

// URL-safe
val urlSafe = Base64.UrlSafe.encode(data)

// MIME (line breaks every 76 chars)
val mime = Base64.Mime.encode(data)

// PEM (line breaks every 64 chars)
val pem = Base64.Pem.encode(data)

// JVM streams
inputStream.encodingWith(Base64.Default).use { encoded ->
    // read Base64 encoded data
}
```

### Stable HexFormat

```kotlin
import kotlin.text.HexFormat

// Format bytes
val bytes = byteArrayOf(0x48, 0x65, 0x6c, 0x6c, 0x6f)
println(bytes.toHexString())  // "48656c6c6f"

// Custom format
val format = HexFormat {
    upperCase = true
    bytes {
        byteSeparator = " "
        bytesPerLine = 16
    }
}
println(bytes.toHexString(format))  // "48 65 6C 6C 6F"

// Format numbers
val number = 255
println(number.toHexString())  // "ff"

val numFormat = HexFormat {
    number {
        prefix = "0x"
        removeLeadingZeros = true
    }
}
println(number.toHexString(numFormat))  // "0xff"

// Parse hex
val parsed = "48656c6c6f".hexToByteArray()
```

---

## Compose Compiler Updates

### PausableComposition Enabled by Default

Allows heavy compositions to be split across frames.

```kotlin
// To disable:
composeCompiler {
    featureFlags.add(ComposeFeatureFlag.PausableComposition.disabled())
}
```

### OptimizeNonSkippingGroups Enabled by Default

Improves runtime performance by removing unnecessary group calls.

```kotlin
// To disable:
composeCompiler {
    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups.disabled())
}
```

### Composable Function References Support

```kotlin
@Composable
fun Greeting(name: String) {
    Text("Hello, $name!")
}

@Composable
fun App() {
    // Function references now work
    val greeter: @Composable (String) -> Unit = ::Greeting
    greeter("World")
}
```

---

## Gradle Updates

### Unified Compiler Warning Management

```kotlin
// New -Xwarning-level option
kotlin {
    compilerOptions {
        // Raise specific warnings to errors
        freeCompilerArgs.add("-Xwarning-level=NOTHING_TO_INLINE:error")
        
        // Suppress specific warnings
        freeCompilerArgs.add("-Xwarning-level=DEPRECATION:disabled")
    }
}
```

### Binary Compatibility Validation (Experimental)

```kotlin
kotlin {
    binaryCompatibilityValidation {
        enabled = true
    }
}

// Run validation
./gradlew checkLegacyAbi

// Update reference dump
./gradlew updateLegacyAbi
```

### Build Tools API (Experimental)

```kotlin
// gradle.properties
kotlin.jvm.target.validation.mode=error

// Enable BTA for KGP
kotlin.bta.enabled=true
```

### Rich Console Output

Color and formatting in Gradle build output for Kotlin tasks.

---

## Migration Checklist

- [ ] Update Kotlin version to 2.2.x in build scripts
- [ ] Remove preview flags for guard conditions and multi-dollar interpolation
- [ ] Migrate from context receivers to context parameters if using them
- [ ] Try context-sensitive resolution for cleaner enum/sealed class usage
- [ ] Consider nested type aliases for better code organization
- [ ] Update Base64/HexFormat usage from Experimental to stable imports
- [ ] Review @all annotation target for property annotations
- [ ] Consider enabling -Xjvm-expose-boxed for value classes used from Java
- [ ] Update Compose compiler feature flags if using non-default settings
- [ ] Enable binary compatibility validation for library projects
- [ ] Test with new JVM default method generation behavior
