# /gradle-test

Run tests in the current Gradle project.

## Description

This command runs tests for the current Gradle project and provides a summary of test results.

## Usage

```
/gradle-test [test-pattern]
```

## Parameters

- `test-pattern` (optional) - Pattern to match specific tests (e.g., `*IntegrationTest`, `MyTestClass`)

## What This Command Does

1. Verifies the project has test sources
2. Runs `./gradlew test` (or with test filters)
3. Monitors test execution
4. Shows test results summary
5. Opens or describes the HTML test report
6. Provides debugging help if tests fail

## Examples

**Run all tests:**
```
/gradle-test
```

**Run specific test class:**
```
/gradle-test MyServiceTest
```

**Run tests matching pattern:**
```
/gradle-test *IntegrationTest
```

## Output

The command will show:
- Number of tests run
- Number of tests passed/failed/skipped
- Failed test details with stack traces
- Location of HTML test report
- Suggestions for fixing failures

## Related

- See `gradle/workflows/test.md` for detailed testing process
- Use `/gradle-build` to run a full build including tests
