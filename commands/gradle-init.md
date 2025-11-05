# /gradle-init

Initialize a new Gradle project with interactive configuration.

## Description

This command initializes a new Gradle project in the current directory. It will guide you through selecting the project type, build script language, and other configuration options.

## Usage

```
/gradle-init
```

## What This Command Does

1. Checks if the current directory is suitable for a new Gradle project
2. Asks you to specify:
   - Project type (Java application, Kotlin library, etc.)
   - Build script DSL (Groovy or Kotlin)
   - Project name and package structure
3. Runs `gradle init` with the appropriate parameters
4. Explains the generated project structure
5. Verifies the setup by running a test command

## Requirements

- Empty or nearly empty directory (or confirmation to proceed)
- Gradle installed globally, or this command will help set up the wrapper

## Examples

**Initialize a Java application:**
```
User: /gradle-init
Assistant: I'll help you initialize a new Gradle project. What type of project would you like to create?
```

**Quick initialization:**
```
User: /gradle-init - I want a Kotlin library with Kotlin DSL
Assistant: I'll initialize a Kotlin library project with Kotlin DSL...
```

## Related

- Use the `gradle` skill for more detailed workflow control
- See `gradle/workflows/init.md` for detailed initialization process
