# Common Gradle Errors and Fixes

## Dependency Resolution Errors

### Error: Could not resolve all dependencies
```
FAILURE: Build failed with an exception.
* What went wrong:
Could not resolve all dependencies for configuration ':compileClasspath'.
> Could not find com.example:library:1.0.0.
```

**Root Causes:**
1. Missing repository configuration
2. Incorrect dependency coordinates
3. Private repository requires authentication
4. Network connectivity issues

**Diagnostic Commands:**
```bash
# See which repositories are configured
gradle dependencies --configuration compileClasspath

# Refresh dependencies
gradle build --refresh-dependencies

# Check dependency insight
gradle dependencyInsight --dependency library --configuration compileClasspath
```

**Fixes:**

**Fix 1: Add Missing Repository**
```kotlin
repositories {
    mavenCentral()
    google()  // For Android dependencies
    maven {
        url = uri("https://repo.example.com/maven")
    }
}
```

**Fix 2: Correct Dependency Coordinates**
```kotlin
dependencies {
    // ❌ Wrong
    implementation("com.example:library:1.0.0")

    // ✅ Correct (check Maven Central for actual coordinates)
    implementation("com.example:library-core:1.0.0")
}
```

**Fix 3: Add Authentication**
```kotlin
repositories {
    maven {
        url = uri("https://private.repo.com/maven")
        credentials {
            username = providers.gradleProperty("repoUser").orNull
            password = providers.gradleProperty("repoPassword").orNull
        }
    }
}
```

### Error: Dependency conflict
```
> Could not resolve all dependencies for configuration ':runtimeClasspath'.
> Conflict found for the following module:
    - com.google.guava:guava between versions 30.1-jre and 31.0-jre
```

**Diagnostic Commands:**
```bash
# See conflict details
gradle dependencyInsight --dependency guava --configuration runtimeClasspath

# Generate dependency tree
gradle dependencies --configuration runtimeClasspath
```

**Fixes:**

**Fix 1: Force Specific Version**
```kotlin
configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:31.0-jre")
    }
}
```

**Fix 2: Use Dependency Constraints (Preferred)**
```kotlin
dependencies {
    constraints {
        implementation("com.google.guava:guava:31.0-jre")
    }
}
```

**Fix 3: Exclude Transitive Dependency**
```kotlin
dependencies {
    implementation("com.example:library:1.0.0") {
        exclude(group = "com.google.guava", module = "guava")
    }
    implementation("com.google.guava:guava:31.0-jre")
}
```

## Memory Errors

### Error: OutOfMemoryError: Java heap space
```
> java.lang.OutOfMemoryError: Java heap space
```

**Root Causes:**
1. Insufficient heap size for build
2. Memory leak in build script or plugin
3. Too many parallel workers

**Diagnostic Commands:**
```bash
# Run with heap dump on OOM
gradle build -Dorg.gradle.jvmargs="-Xmx4g -XX:+HeapDumpOnOutOfMemoryError"

# Check current memory settings
gradle --version
```

**Fixes:**

**Fix 1: Increase Heap Size (gradle.properties)**
```properties
# Small projects (<10 modules)
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m

# Medium projects (10-30 modules)
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g

# Large projects (30+ modules)
org.gradle.jvmargs=-Xmx8g -XX:MaxMetaspaceSize=2g
```

**Fix 2: Reduce Parallel Workers**
```properties
# Reduce from default (number of CPU cores)
org.gradle.workers.max=4
```

**Fix 3: Enable Gradle Daemon with More Memory**
```properties
org.gradle.daemon=true
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC
```

### Error: OutOfMemoryError: Metaspace
```
> java.lang.OutOfMemoryError: Metaspace
```

**Root Cause:** Insufficient metaspace for class metadata

**Fix:**
```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```

## Compilation Errors

### Error: Incompatible Java version
```
> error: invalid source release: 17
```

**Root Causes:**
1. Java toolchain not configured
2. JAVA_HOME points to older JDK
3. sourceCompatibility doesn't match JDK

**Diagnostic Commands:**
```bash
# Check Java version
java -version
echo $JAVA_HOME

# Check Gradle's Java version
gradle --version
```

**Fixes:**

**Fix 1: Configure Java Toolchain (Recommended)**
```kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
```

**Fix 2: Set sourceCompatibility**
```kotlin
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
```

**Fix 3: Update JAVA_HOME**
```bash
# In ~/.bashrc or ~/.zshrc
export JAVA_HOME=/path/to/jdk-17
```

### Error: Cannot access class
```
> error: cannot access com.example.MyClass
  class file for com.example.MyClass not found
```

**Root Cause:** Missing compile dependency

**Fix:**
```kotlin
dependencies {
    // ❌ Wrong: implementation doesn't expose to consumers
    implementation("com.example:library:1.0.0")

    // ✅ Correct: api exposes to consumers
    api("com.example:library:1.0.0")
}
```

## Task Execution Errors

### Error: Task output caching disabled
```
> Task :compileJava NO-SOURCE
Caching disabled for task ':compileJava': Task has unmapped inputs
```

**Root Cause:** Task inputs not properly declared

**Fix (Custom Task):**
```kotlin
abstract class MyTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun execute() {
        // Task logic
    }
}
```

### Error: Circular dependency
```
> Circular dependency between the following tasks:
  :taskA
  \--- :taskB
       \--- :taskA
```

**Root Cause:** Tasks depend on each other

**Fix:** Remove circular dependency
```kotlin
// ❌ Wrong: Circular
tasks.named("taskA") {
    dependsOn("taskB")
}
tasks.named("taskB") {
    dependsOn("taskA")
}

// ✅ Correct: Linear dependency chain
tasks.named("taskB") {
    dependsOn("taskA")
}
tasks.named("taskC") {
    dependsOn("taskB")
}
```

## Configuration Cache Errors

### Error: Configuration cache problems
```
Configuration cache problems found:
- Task `:someTask` of type `SomeTask` field `project` cannot be serialized
```

**Root Causes:**
1. Task stores Project reference
2. Task configuration happens at execution time
3. Using deprecated APIs

**Fixes:**

**Fix 1: Use Providers Instead of Direct Values**
```kotlin
// ❌ Wrong: Eager evaluation
abstract class MyTask : DefaultTask() {
    var version: String = project.version.toString()  // Breaks configuration cache
}

// ✅ Correct: Lazy evaluation
abstract class MyTask : DefaultTask() {
    @get:Input
    abstract val version: Property<String>
}

tasks.register<MyTask>("myTask") {
    version.set(project.provider { project.version.toString() })
}
```

**Fix 2: Remove Project References**
```kotlin
// ❌ Wrong: Stores Project
abstract class MyTask : DefaultTask() {
    lateinit var project: Project  // Not serializable
}

// ✅ Correct: No Project reference needed
abstract class MyTask : DefaultTask() {
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty
}
```

**Fix 3: Configure Tasks at Configuration Time**
```kotlin
// ❌ Wrong: Task configuration at execution time
tasks.register("myTask") {
    doFirst {
        dependsOn("otherTask")  // Too late!
    }
}

// ✅ Correct: Task configuration at configuration time
tasks.register("myTask") {
    dependsOn("otherTask")
}
```

## Build Cache Errors

### Error: Build cache is disabled
```
Build cache is disabled by default for 'build' cache.
```

**Root Cause:** Build cache not enabled

**Fix (gradle.properties):**
```properties
org.gradle.caching=true
```

**Or in settings.gradle.kts:**
```kotlin
buildCache {
    local {
        isEnabled = true
    }
}
```

### Error: Cache key computation failed
```
> Execution failed for task ':compileJava'.
  Cannot compute cache key for task ':compileJava'
```

**Root Cause:** Non-serializable task inputs

**Fix:** Ensure all inputs are serializable
```kotlin
abstract class MyTask : DefaultTask() {
    // ❌ Wrong: Custom object not serializable
    @get:Input
    var config: MyConfig = MyConfig()

    // ✅ Correct: Use primitive types or implement Serializable
    @get:Input
    abstract val configValue: Property<String>
}
```

## Plugin Errors

### Error: Plugin not found
```
> Plugin [id: 'com.example.plugin', version: '1.0.0'] was not found
```

**Root Cause:** Plugin repository not configured

**Fix (settings.gradle.kts):**
```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://plugins.example.com/m2/")
        }
    }
}
```

### Error: Plugin version conflict
```
> Plugin 'com.example.plugin' version '1.0.0' was requested, but version '2.0.0' was already applied
```

**Root Cause:** Plugin applied multiple times with different versions

**Fix (settings.gradle.kts):**
```kotlin
pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.example.plugin") {
                useVersion("2.0.0")
            }
        }
    }
}
```

## Test Execution Errors

### Error: No tests found
```
> Task :test
No tests found for given includes
```

**Root Causes:**
1. Test framework not configured
2. Test files in wrong location
3. Test classes don't follow naming convention

**Fixes:**

**Fix 1: Configure JUnit Platform**
```kotlin
tasks.test {
    useJUnitPlatform()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}
```

**Fix 2: Check Test Location**
```
src/test/java/         ✅ Correct
src/tests/java/        ❌ Wrong
test/java/             ❌ Wrong
```

**Fix 3: Follow Naming Convention**
```kotlin
// ✅ Detected by default
class MyServiceTest { }
class TestMyService { }
class MyServiceTests { }

// ❌ Not detected by default (need configuration)
class MyServiceSpec { }
```

### Error: Test failed with exception
```
> Task :test FAILED
com.example.MyTest > testMethod FAILED
    java.lang.AssertionError: expected:<true> but was:<false>
```

**Diagnostic Commands:**
```bash
# Run specific test
gradle test --tests com.example.MyTest.testMethod

# Run with stack trace
gradle test --stacktrace

# Debug test
gradle test --debug-jvm
```

## Emergency Reset Procedures

### Complete Build Reset
```bash
# Stop daemon
gradle --stop

# Clean build outputs
gradle clean

# Delete Gradle cache (nuclear option)
rm -rf ~/.gradle/caches/
rm -rf .gradle/

# Delete build directories
find . -type d -name "build" -exec rm -rf {} +

# Rebuild from scratch
gradle build --rerun-tasks
```

### Configuration Cache Reset
```bash
# Delete configuration cache
rm -rf .gradle/configuration-cache/

# Rebuild with fresh configuration
gradle build --no-configuration-cache
gradle build  # Now with configuration cache
```

### Dependency Cache Reset
```bash
# Refresh all dependencies
gradle build --refresh-dependencies

# Delete dependency cache
rm -rf ~/.gradle/caches/modules-2/

# Force re-download
gradle build --refresh-dependencies
```
