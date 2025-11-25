# Gradle Tooling API Reference

**Source:** https://docs.gradle.org/current/userguide/tooling_api.html  
**Target:** Gradle 7.0+ (recommended 8.0+)

## Overview

The Gradle Tooling API allows programmatic interaction with Gradle builds from external tools. Used for IDE integration, build analysis, and automation.

## Maven Coordinates

```xml
<dependency>
    <groupId>org.gradle</groupId>
    <artifactId>gradle-tooling-api</artifactId>
    <version>8.11</version>
</dependency>
```

**JBang annotation:**
```java
//DEPS org.gradle:gradle-tooling-api:8.11
```

## Basic Connection

```java
import org.gradle.tooling.*;

ProjectConnection connection = GradleConnector.newConnector()
    .forProjectDirectory(new File("/path/to/project"))
    .connect();

try {
    // Use connection
} finally {
    connection.close();
}
```

## Common Operations

### 1. Build Execution

```java
connection.newBuild()
    .forTasks("clean", "build")
    .setStandardOutput(System.out)
    .setStandardError(System.err)
    .run();
```

### 2. Model Retrieval

```java
// Get GradleProject model
GradleProject project = connection.getModel(GradleProject.class);

System.out.println("Project: " + project.getName());
for (GradleTask task : project.getTasks()) {
    System.out.println("  Task: " + task.getName());
}
```

### 3. Build Action

```java
BuildActionExecuter<String> executer = connection.action(new BuildAction<String>() {
    @Override
    public String execute(BuildController controller) {
        GradleProject project = controller.getModel(GradleProject.class);
        return project.getName();
    }
});

String result = executer.run();
```

## Advanced Patterns

### Progress Monitoring

```java
connection.newBuild()
    .forTasks("build")
    .addProgressListener(new ProgressListener() {
        @Override
        public void statusChanged(ProgressEvent event) {
            System.out.println("Progress: " + event.getDescription());
        }
    })
    .run();
```

### Custom Build Arguments

```java
connection.newBuild()
    .forTasks("build")
    .withArguments("--parallel", "--build-cache")
    .setJvmArguments("-Xmx2g", "-XX:+UseParallelGC")
    .run();
```

### Test Execution

```java
TestLauncher testLauncher = connection.newTestLauncher()
    .withJvmTestClasses("com.example.MyTest")
    .withTaskAndTestClasses("test", Arrays.asList("com.example.AnotherTest"))
    .addProgressListener(event -> {
        if (event instanceof TestProgressEvent) {
            TestProgressEvent testEvent = (TestProgressEvent) event;
            System.out.println("Test: " + testEvent.getDescriptor().getName());
        }
    });

testLauncher.run();
```

## Model Types

### GradleProject
```java
GradleProject project = connection.getModel(GradleProject.class);
- project.getName()
- project.getPath()
- project.getTasks()
- project.getChildren() // sub-projects
```

### GradleBuild
```java
GradleBuild build = connection.getModel(GradleBuild.class);
- build.getRootProject()
- build.getProjects()
```

### BuildEnvironment
```java
BuildEnvironment env = connection.getModel(BuildEnvironment.class);
- env.getGradle().getGradleVersion()
- env.getJava().getJavaHome()
```

### ProjectPublications (for dependency analysis)
```java
ProjectPublications pubs = connection.getModel(ProjectPublications.class);
```

## JBang Integration Pattern

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21+
//DEPS org.gradle:gradle-tooling-api:8.11
//DEPS org.slf4j:slf4j-simple:2.0.9

import org.gradle.tooling.*;
import java.io.File;

public class GradleAnalyzer {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: GradleAnalyzer <project-dir>");
            System.exit(1);
        }
        
        File projectDir = new File(args[0]);
        
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(projectDir)
                .connect()) {
            
            GradleProject project = connection.getModel(GradleProject.class);
            System.out.println("Analyzing: " + project.getName());
            
            // Perform analysis
            analyzeProject(project);
        }
    }
    
    private static void analyzeProject(GradleProject project) {
        // Implementation
    }
}
```

## Error Handling

```java
try {
    connection.newBuild()
        .forTasks("build")
        .run();
} catch (BuildException e) {
    // Build failed
    System.err.println("Build failed: " + e.getMessage());
} catch (BuildCancelledException e) {
    // Build was cancelled
    System.err.println("Build cancelled");
} catch (GradleConnectionException e) {
    // Connection problem
    System.err.println("Connection error: " + e.getMessage());
}
```

## Performance Considerations

### Connection Reuse
```java
// ✅ Good: Reuse connection
try (ProjectConnection connection = connect()) {
    connection.newBuild().forTasks("task1").run();
    connection.newBuild().forTasks("task2").run();
}

// ❌ Bad: Create connection per operation
connection1.newBuild().forTasks("task1").run();
connection1.close();
connection2.newBuild().forTasks("task2").run();
connection2.close();
```

### Parallel Operations
```java
// Multiple models can be fetched in one request
BuildActionExecuter<Map<String, Object>> executer = 
    connection.action(controller -> {
        Map<String, Object> models = new HashMap<>();
        models.put("project", controller.getModel(GradleProject.class));
        models.put("build", controller.getModel(GradleBuild.class));
        models.put("env", controller.getModel(BuildEnvironment.class));
        return models;
    });

Map<String, Object> allModels = executer.run();
```

## Version Compatibility

- **Tooling API 7.x:** Compatible with Gradle 7.0+
- **Tooling API 8.x:** Compatible with Gradle 7.0+ (recommended for 8.x targets)
- Always use Tooling API version >= target Gradle version

## Common Use Cases

### 1. Build Health Check
```java
// Check if build is healthy
GradleBuild build = connection.getModel(GradleBuild.class);
BuildEnvironment env = connection.getModel(BuildEnvironment.class);

System.out.println("Gradle: " + env.getGradle().getGradleVersion());
System.out.println("Java: " + env.getJava().getJavaHome());
```

### 2. Task Analysis
```java
GradleProject project = connection.getModel(GradleProject.class);
for (GradleTask task : project.getTasks()) {
    System.out.println(task.getName() + " - " + task.getDescription());
}
```

### 3. Dependency Analysis
```java
connection.action(controller -> {
    GradleProject project = controller.getModel(GradleProject.class);
    // Analyze dependencies
    return analyzeDependencies(project);
}).run();
```

## Testing Patterns

```java
@Test
void testBuildExecution() {
    File projectDir = new File("test-project");
    
    try (ProjectConnection connection = GradleConnector.newConnector()
            .forProjectDirectory(projectDir)
            .connect()) {
        
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        
        connection.newBuild()
            .forTasks("build")
            .setStandardOutput(output)
            .run();
        
        String buildOutput = output.toString();
        assertTrue(buildOutput.contains("BUILD SUCCESSFUL"));
    }
}
```

## Logging Configuration

```java
// SLF4J simple logger configuration
System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
System.setProperty("org.slf4j.simpleLogger.log.org.gradle", "info");
```

## Best Practices

1. **Always close connections** - Use try-with-resources
2. **Reuse connections** - Don't create new connection per operation
3. **Handle exceptions** - Build failures, cancellations, connection issues
4. **Use progress listeners** - For long-running operations
5. **Batch model requests** - Fetch multiple models in one action
6. **Match Tooling API version** - Use version >= target Gradle version
7. **Configure logging** - Control output verbosity
