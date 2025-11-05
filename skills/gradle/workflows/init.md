# Initialize Gradle Project

Initialize a new Gradle project with the appropriate project type and configuration.

## Task

You are tasked with initializing a new Gradle project. Follow these steps:

1. **Determine Project Type**: Ask the user what type of project they want to create if not specified:
   - Java application
   - Java library
   - Kotlin application
   - Kotlin library
   - Groovy application
   - Groovy library
   - Scala application
   - Scala library
   - Basic (minimal Gradle project)

2. **Choose Build Script DSL**: Ask if they prefer Groovy DSL (`build.gradle`) or Kotlin DSL (`build.gradle.kts`) if not specified.

3. **Initialize Project**: Run the appropriate `gradle init` command:
   ```bash
   gradle init --type <project-type> --dsl <groovy|kotlin>
   ```

4. **Explain Structure**: After initialization, explain the created structure:
   - `build.gradle` or `build.gradle.kts` - Main build script
   - `settings.gradle` or `settings.gradle.kts` - Project settings
   - `gradlew` and `gradlew.bat` - Gradle wrapper scripts
   - `gradle/wrapper/` - Wrapper configuration and JAR
   - `src/` - Source code directories

5. **Verify Setup**: Run `./gradlew tasks` to verify the project is properly initialized.

## Common Project Types

- `java-application` - Standalone Java application with a main class
- `java-library` - Reusable Java library
- `kotlin-application` - Standalone Kotlin application
- `kotlin-library` - Reusable Kotlin library
- `groovy-application` - Standalone Groovy application
- `groovy-library` - Reusable Groovy library
- `scala-application` - Standalone Scala application
- `scala-library` - Reusable Scala library
- `basic` - Minimal Gradle project without specific language

## Example

```bash
# Initialize a Java application with Kotlin DSL
gradle init --type java-application --dsl kotlin

# Or using the wrapper
./gradlew init --type java-library --dsl groovy
```

## Notes

- If Gradle is not installed globally, you can download the wrapper first or use SDKMAN
- The init command is interactive by default if parameters are not specified
- You can add `--package <package-name>` to set the base package name
- Use `--project-name <name>` to set the project name
