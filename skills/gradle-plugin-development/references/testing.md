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

## Best Practices

1. Test both unit and functional
2. Use @TempDir for test projects
3. Test multiple Gradle versions
4. Test configuration cache compatibility
