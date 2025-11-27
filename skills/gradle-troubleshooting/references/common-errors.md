# Common Gradle Errors

Quick reference for frequent errors and fixes.

## Dependency Errors

### Could Not Find

```
Could not find com.example:library:1.0.0
```

**Causes**:
- Repository not configured
- Artifact doesn't exist
- Network/proxy issues

**Fix**:
```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://custom.repo.com") }
}
```

### Version Conflict

```
Conflict: guava between 30.1-jre and 31.0-jre
```

**Fix**:
```kotlin
configurations.all {
    resolutionStrategy.force("com.google.guava:guava:31.1-jre")
}
```

### Duplicate Classes

```
Duplicate class found in modules A and B
```

**Fix**:
```kotlin
configurations.all {
    exclude(group = "org.duplicate", module = "module")
}
```

## Runtime Errors

### NoSuchMethodError

```
java.lang.NoSuchMethodError: com.example.Library.method()V
```

**Causes**:
- Compiled against different version than runtime
- Transitive dependency version mismatch
- Split packages across JARs

**Diagnosis**:
```bash
# Find which JARs contain the class
./gradlew dependencies --configuration runtimeClasspath | grep library

# Deep dive into specific dependency
./gradlew dependencyInsight --dependency library --configuration runtimeClasspath
```

**Fix**:
```kotlin
dependencies {
    // Force consistent version
    constraints {
        implementation("com.example:library:2.0.0")
    }
}
```

### ClassNotFoundException / NoClassDefFoundError

```
java.lang.ClassNotFoundException: com.example.MissingClass
java.lang.NoClassDefFoundError: com/example/MissingClass
```

**Causes**:
- Dependency not on runtime classpath
- Using `compileOnly` when `implementation` needed
- Transitive dependency excluded

**Diagnosis**:
```bash
# Check if class is in any dependency
./gradlew dependencies --configuration runtimeClasspath

# Search for the class in JARs
find ~/.gradle/caches -name "*.jar" -exec jar -tf {} \; 2>/dev/null | grep MissingClass
```

**Fix**:
```kotlin
dependencies {
    // Change from compileOnly to implementation
    implementation("com.example:library:1.0.0")

    // Or add missing transitive dependency explicitly
    runtimeOnly("com.example:missing-transitive:1.0.0")
}
```

### AbstractMethodError

```
java.lang.AbstractMethodError: Receiver class does not define or inherit implementation
```

**Causes**:
- Interface/abstract class version mismatch
- Library compiled against older API version

**Fix**:
```kotlin
dependencies {
    // Align versions across related libraries
    implementation(platform("com.example:bom:1.0.0"))
    implementation("com.example:library-core")
    implementation("com.example:library-impl")
}
```

### LinkageError: loader constraint violation

```
LinkageError: loader constraint violation: loader previously initiated loading for a different type
```

**Causes**:
- Same class loaded by different classloaders
- Split packages across modules

**Fix**:
```kotlin
configurations.all {
    // Exclude duplicate to ensure single source
    exclude(group = "javax.annotation", module = "javax.annotation-api")
}

dependencies {
    // Use Jakarta instead
    implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")
}
```

## Compilation Errors

### Cannot Find Symbol

```
error: cannot find symbol
```

**Causes**:
- Missing dependency
- Wrong source set
- Import missing

**Fix**: Check `dependencies` block

### Incompatible Java Version

```
error: invalid target release: 17
```

**Fix**:
```kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
```

## Task Errors

### Task Not Found

```
Task 'xyz' not found in root project
```

**Fix**: `./gradlew tasks --all` to list available tasks

### Circular Dependency

```
Circular dependency between tasks
```

**Fix**: Remove circular `dependsOn` relationships

## Memory Errors

### OutOfMemoryError: Java heap space

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4g
```

### OutOfMemoryError: Metaspace

```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```

## Configuration Cache

### Task.project at execution time

**Fix**: Capture values during configuration:
```kotlin
val name = project.name
doLast { println(name) }
```

See gradle-config-cache skill for more patterns.

## Daemon Errors

### Daemon Disappeared / Daemon Process Exited

```
Gradle Daemon disappeared unexpectedly (it may have been killed or may have crashed)
```

**Causes**:
- Out of memory in daemon
- Daemon killed by OS/CI
- Corrupted daemon state

**Fix**:
```bash
# Stop all daemons
./gradlew --stop

# Run without daemon (debugging)
./gradlew build --no-daemon

# Increase daemon memory
# gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```

### Could Not Create Daemon Process

```
Could not create the Java Virtual Machine
Error: Could not create daemon process
```

**Causes**:
- Insufficient memory for JVM
- Invalid JVM args in gradle.properties
- Java installation issues

**Fix**:
```bash
# Check JVM args
cat gradle.properties | grep jvmargs

# Reduce memory if system constrained
org.gradle.jvmargs=-Xmx2g

# Verify Java installation
java -version
```

### Daemon is Busy

```
Starting a Gradle Daemon, 1 busy Daemon could not be reused
```

**Causes**:
- Another build running
- Daemon incompatible (different JVM options)

**Fix**:
```bash
# Check daemon status
./gradlew --status

# Stop busy daemons
./gradlew --stop
```

## Network Errors

### Connection Timed Out

```
Connection timed out: repo.maven.apache.org
```

**Causes**:
- Network issues
- Firewall blocking
- Proxy misconfiguration

**Fix**:
```properties
# gradle.properties - configure proxy
systemProp.http.proxyHost=proxy.example.com
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=proxy.example.com
systemProp.https.proxyPort=8080
```

### SSL/TLS Certificate Errors

```
PKIX path building failed: unable to find valid certification path
```

**Causes**:
- Corporate proxy with SSL inspection
- Self-signed certificates
- Outdated Java truststore

**Fix**:
```bash
# Import certificate to Java truststore
keytool -import -alias mycert -file cert.pem -keystore $JAVA_HOME/lib/security/cacerts

# Or disable SSL verification (NOT recommended for production)
# gradle.properties
systemProp.javax.net.ssl.trustStore=/path/to/truststore.jks
```

### Repository Authentication Failed

```
401 Unauthorized: repository.example.com
```

**Fix**:
```kotlin
// build.gradle.kts
repositories {
    maven {
        url = uri("https://repository.example.com/maven")
        credentials {
            username = providers.gradleProperty("repoUser").orNull
            password = providers.gradleProperty("repoPassword").orNull
        }
    }
}
```

```properties
# ~/.gradle/gradle.properties (NOT in project)
repoUser=myuser
repoPassword=mytoken
```

## Gradle Wrapper Errors

### gradlew: Permission denied

```bash
chmod +x gradlew
```

### Invalid Gradle Distribution

```
Could not install Gradle distribution from 'https://services.gradle.org/distributions/gradle-X.Y-bin.zip'
```

**Causes**:
- Network issue downloading
- Corrupted distribution cache

**Fix**:
```bash
# Clear distribution cache
rm -rf ~/.gradle/wrapper/dists/gradle-X.Y*

# Re-run to redownload
./gradlew --version
```

### Wrapper JAR Mismatch

```
Wrapper properties file 'gradle/wrapper/gradle-wrapper.properties' was modified
```

**Fix**:
```bash
# Regenerate wrapper
./gradlew wrapper --gradle-version 9.2.1

# Verify SHA-256 (security)
./gradlew wrapper --gradle-version 9.2.1 --gradle-distribution-sha256-sum <sha256>
```
