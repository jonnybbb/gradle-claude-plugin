# CI Environment Mismatch Fixture

This fixture tests the `/gradle:doctor` command's ability to detect the common "works in CI but not locally" problem caused by missing environment variables.

## Scenarios

This fixture demonstrates two common CI/local environment mismatches:

### 1. Build Task Verification (verifyEnvironment)

The build contains a `verifyEnvironment` task that:
- **Passes** when `CI=true` environment variable is set
- **Fails** when `CI` is not set or is false

### 2. Unit Tests

Two test classes demonstrate different failure patterns:

#### ApiKeyTest - Local FAILS, CI PASSES
- Requires `EXAMPLE_API_KEY` environment variable
- Should be set in CI via secrets/environment configuration
- Fails locally because the variable is not set

#### ServiceConfigTest - Local PASSES, CI FAILS
- Loads a resource file `/serviceconfig.json`
- Passes locally where the file exists in `src/test/resources/`
- May fail in CI if resources are not properly packaged (simulates misconfiguration)

## Build Scan Configuration

- **Server**: `ge.gradle.org`
- **Tags**: `CI` (when CI=true) or `LOCAL` (when CI is not set)
- **Custom Value**: `CI_ENV` captures the CI environment variable value

## Usage

### Manual Testing

```bash
# Task verification - should FAIL (simulates local development)
./gradlew verifyEnvironment --scan

# Task verification - should PASS (simulates CI environment)
CI=true ./gradlew verifyEnvironment --scan

# Unit tests - ApiKeyTest fails locally, ServiceConfigTest passes
./gradlew test --scan

# Unit tests - all pass when environment variable is set
EXAMPLE_API_KEY=prod_test_key ./gradlew test --scan
```

### Automated Test (JUnit 5)

Run the end-to-end test:

```bash
# Add credentials to tests/local.env
echo "DEVELOCITY_ACCESS_KEY=your-ge-access-key" >> tests/local.env
echo "ANTHROPIC_API_KEY=your-anthropic-key" >> tests/local.env

# Run the test
cd tests
./gradlew develocityE2ETests
```

**Test Class**: `com.gradle.claude.plugin.scenarios.CiEnvMismatchE2ETest`

The test will:
1. Run a CI build with `CI=true` (passes, publishes build scan)
2. Run a LOCAL build without CI (fails, publishes build scan)
3. Wait for build scans to be indexed (polls with 30s initial delay, 10s intervals, 5min timeout)
4. Run `/gradle:doctor` agent via Claude API
5. Verify that doctor identifies the environment mismatch and suggests the fix

## Expected Doctor Output

The `/gradle:doctor` command should:
1. Detect that CI builds pass but LOCAL builds fail
2. Compare environment variables between builds
3. Identify that `CI_ENV` differs ("true" vs "not set")
4. Suggest the fix: `CI=true ./gradlew <task>`

## MCP Configuration

The `.claude/mcp.json` file configures a project-local Develocity MCP server for `ge.gradle.org`. The access key is passed via `DEVELOCITY_ACCESS_KEY` environment variable.

## Prerequisites

- `DEVELOCITY_ACCESS_KEY` environment variable (ge.gradle.org access key)
- `claude` CLI installed
- `jq` installed
