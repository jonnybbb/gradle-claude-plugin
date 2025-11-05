# Run Gradle Tests

Run tests for a Gradle project and generate test reports.

## Task

You are tasked with running tests in a Gradle project. Follow these steps:

1. **Run All Tests**: Execute all tests in the project:
   ```bash
   ./gradlew test
   ```

2. **Run Specific Test Class**: To run a specific test class:
   ```bash
   ./gradlew test --tests com.example.MyTestClass
   ```

3. **Run Specific Test Method**: To run a specific test method:
   ```bash
   ./gradlew test --tests com.example.MyTestClass.testMethod
   ```

4. **Run Tests with Pattern**: To run tests matching a pattern:
   ```bash
   ./gradlew test --tests *IntegrationTest
   ```

5. **View Test Results**: After tests complete:
   - Check console output for pass/fail summary
   - Open HTML report at `build/reports/tests/test/index.html`
   - Review XML results at `build/test-results/test/`

6. **Handle Test Failures**: If tests fail:
   - Review the stack traces in console output
   - Check the HTML report for detailed failure information
   - Run individual failing tests for faster debugging
   - Use `--info` or `--debug` for more verbose output

## Common Test Commands

```bash
# Run all tests
./gradlew test

# Run tests and continue even if some fail
./gradlew test --continue

# Run specific test class
./gradlew test --tests MyTestClass

# Run tests with pattern matching
./gradlew test --tests *ServiceTest

# Run tests with verbose output
./gradlew test --info

# Clean and run tests
./gradlew clean test

# Run only failed tests from previous run
./gradlew test --rerun-tasks
```

## Test Types

Different test tasks for different test types:
- `test` - Unit tests
- `integrationTest` - Integration tests (if configured)
- `functionalTest` - Functional tests (if configured)

## Test Report Locations

- **HTML Report**: `build/reports/tests/test/index.html`
- **XML Results**: `build/test-results/test/`
- **Binary Results**: `build/test-results/test/binary/`

## Debugging Tests

```bash
# Run tests with full stack traces
./gradlew test --stacktrace

# Run tests with debug logging
./gradlew test --debug

# Run tests without using cached results
./gradlew cleanTest test

# Run tests and show standard output
./gradlew test --info
```

## Test Configuration

Common test configurations in `build.gradle`:

```groovy
test {
    useJUnitPlatform() // For JUnit 5
    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
    }
    maxParallelForks = Runtime.runtime.availableProcessors()
}
```

## Notes

- Gradle caches test results - use `--rerun-tasks` to force re-execution
- Test reports are overwritten on each run
- Use `check` task to run tests and all other verification tasks
- For multi-module projects, use `./gradlew test` to run all module tests
