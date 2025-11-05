# /gradle-build

Build the current Gradle project.

## Description

This command builds the current Gradle project, compiling source code, running tests, and creating build artifacts.

## Usage

```
/gradle-build [options]
```

## Options

- `clean` - Perform a clean build (removes previous build artifacts first)
- `skip-tests` - Build without running tests
- `info` - Show detailed build information
- `debug` - Show full debug output

## What This Command Does

1. Checks that the project has Gradle build files
2. Runs `./gradlew build` (or with specified options)
3. Monitors the build progress
4. Reports success or failure with detailed information
5. Shows location of build artifacts

## Examples

**Standard build:**
```
/gradle-build
```

**Clean build:**
```
/gradle-build clean
```

**Build without tests:**
```
/gradle-build skip-tests
```

**Build with detailed output:**
```
/gradle-build info
```

## Output

The command will show:
- Build progress and tasks executed
- Test results summary
- Build success/failure status
- Location of generated artifacts (JARs, WARs, etc.)
- Build duration

## Related

- See `gradle/workflows/build.md` for detailed build process
- Use `/gradle-test` to run tests separately
