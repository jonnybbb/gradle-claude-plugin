# Build Gradle Project

Build and compile a Gradle project, including running tests and creating distribution packages.

## Task

You are tasked with building a Gradle project. Follow these steps:

1. **Check Project Status**: Verify the project has a `build.gradle` or `build.gradle.kts` file.

2. **Clean Build (Optional)**: If requested or if there are build issues, run a clean build:
   ```bash
   ./gradlew clean build
   ```

3. **Standard Build**: For a normal build, run:
   ```bash
   ./gradlew build
   ```

4. **Understand Build Tasks**: The `build` task typically includes:
   - `compileJava` / `compileKotlin` - Compile source code
   - `processResources` - Process resource files
   - `classes` - Assemble compiled classes
   - `test` - Run unit tests
   - `jar` / `assemble` - Create JAR files
   - `check` - Run all checks including tests

5. **Monitor Output**: Watch for:
   - Compilation errors
   - Test failures
   - Build success message with elapsed time
   - Location of built artifacts (usually in `build/libs/`)

6. **Handle Errors**: If build fails:
   - Read the error messages carefully
   - Check for missing dependencies
   - Verify Java/Kotlin version compatibility
   - Look for syntax errors in build scripts
   - Run with `--stacktrace` or `--debug` for more information

## Common Build Commands

```bash
# Standard build
./gradlew build

# Clean and build
./gradlew clean build

# Build without running tests
./gradlew build -x test

# Build with verbose output
./gradlew build --info

# Build with full debug information
./gradlew build --debug --stacktrace

# Continuous build (rebuilds on file changes)
./gradlew build --continuous
```

## Build Output Locations

- **JARs**: `build/libs/`
- **Compiled classes**: `build/classes/`
- **Test results**: `build/reports/tests/`
- **Test reports (HTML)**: `build/reports/tests/test/index.html`

## Performance Tips

- Use `--parallel` for multi-module projects
- Enable build cache with `--build-cache`
- Use Gradle daemon (enabled by default)
- Increase heap size if needed: `org.gradle.jvmargs=-Xmx2g` in `gradle.properties`

## Notes

- Always use the wrapper (`./gradlew`) instead of globally installed Gradle
- Build artifacts are placed in the `build/` directory by default
- The `build` task is cumulative - it depends on many other tasks
- Use `./gradlew tasks` to see all available build-related tasks
