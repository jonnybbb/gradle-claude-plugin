# Build Cache Optimization

Improve cache hit rates and performance.

## Path Sensitivity

```kotlin
// ✅ Relative - works across machines
@get:PathSensitive(PathSensitivity.RELATIVE)
abstract val sources: ConfigurableFileCollection

// ❌ Absolute - cache miss on different machines
@get:PathSensitive(PathSensitivity.ABSOLUTE)
```

| Sensitivity | Use When |
|-------------|----------|
| RELATIVE | Default choice, paths relative to project |
| NAME_ONLY | Only filename matters |
| NONE | Content only, ignore all paths |
| ABSOLUTE | Rarely - when absolute path is significant |

## Normalize Line Endings

```kotlin
@get:InputFiles
@get:PathSensitive(PathSensitivity.RELATIVE)
@get:NormalizeLineEndings
abstract val sources: ConfigurableFileCollection
```

## Deterministic Outputs

```kotlin
// ❌ Non-deterministic - cache invalidation
outputFile.writeText("Built at: ${System.currentTimeMillis()}")

// ✅ Deterministic
outputFile.writeText("Version: ${version.get()}")
```

## Declare All Inputs

```kotlin
// ❌ Undeclared input causes cache misses
@TaskAction
fun execute() {
    val config = System.getenv("CONFIG")  // Not tracked!
}

// ✅ Declared input
@get:Input
abstract val config: Property<String>
```

## Pin Toolchains

```kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}
```

## Measuring Hit Rate

```bash
# Use build scan
./gradlew build --build-cache --scan
# Check Performance → Build Cache

# Manual check
./gradlew build --build-cache 2>&1 | grep -c "FROM-CACHE"
```

## Target Metrics

| Scenario | Expected Hit Rate |
|----------|-------------------|
| Same machine repeat | 90%+ |
| CI with remote cache | 60-80% |
| After dependency update | 40-60% |
