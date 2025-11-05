# Manage Gradle Dependencies

Manage, inspect, and troubleshoot project dependencies in Gradle projects.

## Task

You are tasked with managing dependencies in a Gradle project. Follow these steps:

1. **View Dependency Tree**: To see all dependencies and their transitive dependencies:
   ```bash
   ./gradlew dependencies
   ```

2. **View Specific Configuration**: To see dependencies for a specific configuration (e.g., compileClasspath):
   ```bash
   ./gradlew dependencies --configuration compileClasspath
   ```

3. **Add Dependencies**: To add a new dependency, edit `build.gradle` or `build.gradle.kts`:
   
   **Groovy DSL:**
   ```groovy
   dependencies {
       implementation 'org.springframework.boot:spring-boot-starter-web:3.2.0'
       testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
   }
   ```
   
   **Kotlin DSL:**
   ```kotlin
   dependencies {
       implementation("org.springframework.boot:spring-boot-starter-web:3.2.0")
       testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
   }
   ```

4. **Check for Updates**: View outdated dependencies:
   ```bash
   ./gradlew dependencyUpdates
   ```
   (Requires the `com.github.ben-manes.versions` plugin)

5. **Resolve Conflicts**: View dependency insight for conflict resolution:
   ```bash
   ./gradlew dependencyInsight --dependency <dependency-name>
   ```

## Common Dependency Scopes

- **implementation** - Compile and runtime dependency (not exposed to consumers)
- **api** - Compile and runtime dependency (exposed to consumers) - requires java-library plugin
- **compileOnly** - Compile-time only (not included in runtime)
- **runtimeOnly** - Runtime only (not needed for compilation)
- **testImplementation** - Test compile and runtime
- **testCompileOnly** - Test compile-time only
- **testRuntimeOnly** - Test runtime only

## Common Commands

```bash
# View all dependencies
./gradlew dependencies

# View runtime dependencies only
./gradlew dependencies --configuration runtimeClasspath

# View test dependencies only
./gradlew dependencies --configuration testRuntimeClasspath

# Find where a dependency comes from
./gradlew dependencyInsight --dependency slf4j-api

# Build with refreshed dependencies (re-download)
./gradlew build --refresh-dependencies

# View build script dependencies
./gradlew buildEnvironment
```

## Dependency Resolution Strategies

Force a specific version:
```groovy
configurations.all {
    resolutionStrategy {
        force 'org.slf4j:slf4j-api:2.0.9'
    }
}
```

Exclude transitive dependencies:
```groovy
dependencies {
    implementation('org.springframework.boot:spring-boot-starter-web') {
        exclude group: 'org.springframework.boot', module: 'spring-boot-starter-tomcat'
    }
}
```

## Troubleshooting Dependencies

Common issues:
- **Version conflicts**: Use `dependencyInsight` to find conflicts
- **Missing dependencies**: Check repository configuration in `build.gradle`
- **Transitive issues**: Use `exclude` to remove problematic transitive dependencies
- **Download failures**: Use `--refresh-dependencies` to re-download

## Popular Dependencies

Common libraries and their coordinates:
- JUnit 5: `org.junit.jupiter:junit-jupiter:5.10.0`
- Mockito: `org.mockito:mockito-core:5.7.0`
- AssertJ: `org.assertj:assertj-core:3.24.2`
- SLF4J: `org.slf4j:slf4j-api:2.0.9`
- Logback: `ch.qos.logback:logback-classic:1.4.14`
- Jackson: `com.fasterxml.jackson.core:jackson-databind:2.16.0`
- Gson: `com.google.code.gson:gson:2.10.1`

## Notes

- Gradle caches dependencies in `~/.gradle/caches/`
- Use Maven Central, JCenter (deprecated), or custom repositories
- Always specify explicit versions to avoid surprises
- Use dependency management plugins like Spring Dependency Management for BOM support
