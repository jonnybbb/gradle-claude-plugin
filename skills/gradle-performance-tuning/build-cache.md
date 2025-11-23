# Build Cache Configuration

## Local Build Cache

```kotlin
// settings.gradle.kts
buildCache {
    local {
        directory = file("${rootDir}/.gradle/build-cache")
        removeUnusedEntriesAfterDays = 30
        isEnabled = true
    }
}
```

## Remote Build Cache

```kotlin
buildCache {
    local {
        isEnabled = !System.getenv().containsKey("CI")
    }
    remote<HttpBuildCache> {
        url = uri("https://build-cache.example.com/cache/")
        isPush = System.getenv("CI")?.toBoolean() ?: false
        credentials {
            username = providers.environmentVariable("CACHE_USERNAME").orNull
            password = providers.environmentVariable("CACHE_PASSWORD").orNull
        }
    }
}
```

## Making Tasks Cacheable

```kotlin
@CacheableTask
abstract class MyTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
}
```

## Common Cache Miss Causes

1. **Absolute paths**: Use `PathSensitivity.RELATIVE`
2. **Missing annotations**: Annotate all inputs/outputs
3. **Timestamps**: Don't include current time in outputs
4. **Random values**: Ensure deterministic task logic
