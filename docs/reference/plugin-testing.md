# Plugin Testing

**Source**: https://docs.gradle.org/current/userguide/testing_gradle_plugins.html  
**Gradle Version**: 7.0+

## Overview

Testing strategies for Gradle plugins to ensure reliability.

## Test Types

### 1. Unit Tests

Test plugin logic in isolation.

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
        
        val task = project.tasks.findByName("myTask")
        assertNotNull(task)
    }
}
```

### 2. Functional Tests

Test plugin in real Gradle build.

```kotlin
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals

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
                message.set("Test message")
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
        assertTrue(result.output.contains("Test message"))
    }
}
```

## Test Setup

### Dependencies

```kotlin
// build.gradle.kts
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
```

### Plugin Classpath

```kotlin
// Ensure plugin is available in tests
tasks.named<PluginUnderTestMetadata>("pluginUnderTestMetadata") {
    pluginClasspath.from(configurations.compileClasspath)
}
```

## Testing Patterns

### Test Extension Configuration

```kotlin
@Test
fun `extension has correct defaults`() {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply("com.example.my-plugin")
    
    val extension = project.extensions
        .getByType(MyExtension::class.java)
    
    assertEquals("default", extension.message.get())
}
```

### Test Task Configuration

```kotlin
@Test
fun `task is configured correctly`() {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply("com.example.my-plugin")
    
    val task = project.tasks
        .named("myTask", MyTask::class.java)
        .get()
    
    assertNotNull(task.inputFiles)
    assertNotNull(task.outputDir)
}
```

### Test Multi-Project Setup

```kotlin
@Test
fun `applies to subprojects`() {
    val root = ProjectBuilder.builder().build()
    val sub = ProjectBuilder.builder()
        .withParent(root)
        .withName("sub")
        .build()
    
    root.plugins.apply("com.example.my-plugin")
    
    assertNotNull(sub.extensions.findByName("myPlugin"))
}
```

## Functional Test Patterns

### Test Build Success

```kotlin
@Test
fun `build succeeds`() {
    setupProject()
    
    val result = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withArguments("build")
        .withPluginClasspath()
        .build()
    
    assertEquals(TaskOutcome.SUCCESS, result.task(":build")?.outcome)
}
```

### Test Build Failure

```kotlin
@Test
fun `build fails with invalid config`() {
    setupInvalidProject()
    
    val result = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withArguments("build")
        .withPluginClasspath()
        .buildAndFail()
    
    assertTrue(result.output.contains("Invalid configuration"))
}
```

### Test Output Verification

```kotlin
@Test
fun `generates expected output`() {
    setupProject()
    
    val result = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withArguments("myTask")
        .withPluginClasspath()
        .build()
    
    val outputFile = File(testProjectDir, "build/output.txt")
    assertTrue(outputFile.exists())
    assertEquals("Expected content", outputFile.readText())
}
```

## Test Helpers

### Project Builder Helper

```kotlin
fun createProject(): Project {
    return ProjectBuilder.builder()
        .withProjectDir(testProjectDir)
        .build()
        .also {
            it.plugins.apply("com.example.my-plugin")
        }
}
```

### File Setup Helper

```kotlin
fun setupBuildFile(content: String) {
    File(testProjectDir, "build.gradle.kts").writeText(content)
}

fun setupSettingsFile(content: String) {
    File(testProjectDir, "settings.gradle.kts").writeText(content)
}
```

## Best Practices

1. **Test both unit and functional**
2. **Use @TempDir for isolation**
3. **Test with multiple Gradle versions**
4. **Test edge cases and failures**
5. **Mock external dependencies**

## Related Documentation

- [Plugin Basics](plugin-basics.md): Plugin development
- [Extension Objects](extension-objects.md): Extension design
- [Task Basics](task-basics.md): Task development

## Quick Reference

```kotlin
// Unit test
@Test
fun testPlugin() {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply("my-plugin")
    assertNotNull(project.tasks.findByName("myTask"))
}

// Functional test
@Test
fun testBuild(@TempDir dir: File) {
    File(dir, "build.gradle.kts").writeText("""
        plugins { id("my-plugin") }
    """)
    
    val result = GradleRunner.create()
        .withProjectDir(dir)
        .withArguments("myTask")
        .withPluginClasspath()
        .build()
    
    assertEquals(TaskOutcome.SUCCESS, result.task(":myTask")?.outcome)
}
```
