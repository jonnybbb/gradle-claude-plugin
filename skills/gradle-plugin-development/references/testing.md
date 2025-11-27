# Plugin Testing

Unit and functional testing patterns for Gradle plugins.

## Unit Testing (ProjectBuilder)

```kotlin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import kotlin.test.*

class MyPluginTest {
    @Test
    fun `plugin applies successfully`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.example.my-plugin")
        
        assertNotNull(project.extensions.findByName("myPlugin"))
    }
    
    @Test
    fun `plugin registers task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.example.my-plugin")
        
        assertNotNull(project.tasks.findByName("myTask"))
    }
    
    @Test
    fun `extension has defaults`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.example.my-plugin")
        
        val extension = project.extensions.getByType(MyExtension::class.java)
        assertEquals("Default", extension.message.get())
    }
}
```

## Functional Testing (TestKit)

```kotlin
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.*

class MyPluginFunctionalTest {
    @TempDir
    lateinit var testProjectDir: File
    
    @Test
    fun `can run task`() {
        // Setup
        File(testProjectDir, "settings.gradle.kts").writeText("""
            rootProject.name = "test"
        """.trimIndent())
        
        File(testProjectDir, "build.gradle.kts").writeText("""
            plugins {
                id("com.example.my-plugin")
            }
            
            myPlugin {
                message.set("Test")
            }
        """.trimIndent())
        
        // Execute
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("myTask")
            .withPluginClasspath()
            .build()
        
        // Verify
        assertEquals(TaskOutcome.SUCCESS, result.task(":myTask")?.outcome)
    }
    
    @Test
    fun `handles failure`() {
        // Setup with invalid config...
        
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("myTask")
            .withPluginClasspath()
            .buildAndFail()
        
        assertTrue(result.output.contains("expected error"))
    }
}
```

## Test Dependencies

```kotlin
// build.gradle.kts
dependencies {
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
```

## Test Filtering

Run specific tests using `--tests`:

```bash
# Single test class
./gradlew test --tests MyPluginTest

# Single test method
./gradlew test --tests "MyPluginTest.plugin applies successfully"

# Wildcard patterns
./gradlew test --tests "*PluginTest"
./gradlew test --tests "*functional*"

# Package wildcard
./gradlew test --tests "com.example.*"

# Multiple patterns
./gradlew test --tests MyPluginTest --tests MyTaskTest

# Run functional tests only (by source set)
./gradlew functionalTest --tests "*"
```

### Configure in Build Script

```kotlin
tasks.test {
    useJUnitPlatform()

    // Include/exclude patterns
    filter {
        includeTestsMatching("*Test")
        excludeTestsMatching("*IntegrationTest")
    }

    // Or use system property
    if (project.hasProperty("testFilter")) {
        filter {
            includeTestsMatching(project.property("testFilter") as String)
        }
    }
}
```

### JUnit 5 Tags

```kotlin
// In test class
@Tag("fast")
class FastTest { ... }

@Tag("slow")
class SlowTest { ... }

// In build.gradle.kts
tasks.test {
    useJUnitPlatform {
        includeTags("fast")
        excludeTags("slow")
    }
}
```

### Run with Filtering

```bash
# By tag (requires build script config)
./gradlew test -PincludeTags=fast

# Continue on failure
./gradlew test --tests "*" --continue

# With verbose output
./gradlew test --tests MyPluginTest --info
```

## Testing Multiple Gradle Versions

```kotlin
@ParameterizedTest
@ValueSource(strings = ["7.6.4", "8.5", "8.11"])
fun `works with Gradle version`(version: String) {
    val result = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withGradleVersion(version)
        .withArguments("myTask")
        .withPluginClasspath()
        .build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":myTask")?.outcome)
}
```

## Best Practices

1. Test both unit and functional
2. Use @TempDir for test projects
3. Test multiple Gradle versions
4. Test configuration cache compatibility
5. Use `--tests` filtering for faster development cycles
6. Tag tests (fast/slow/integration) for CI optimization
