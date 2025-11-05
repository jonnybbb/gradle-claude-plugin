# Gradle Tasks

List, discover, and execute Gradle tasks in a project.

## Task

You are tasked with working with Gradle tasks. Follow these steps:

1. **List All Tasks**: View all available tasks:
   ```bash
   ./gradlew tasks
   ```

2. **List All Tasks (Including Hidden)**: View all tasks including those not in main groups:
   ```bash
   ./gradlew tasks --all
   ```

3. **Execute a Task**: Run a specific task:
   ```bash
   ./gradlew <taskName>
   ```

4. **Execute Multiple Tasks**: Run multiple tasks in sequence:
   ```bash
   ./gradlew clean build test
   ```

5. **Get Task Help**: Get detailed information about a specific task:
   ```bash
   ./gradlew help --task <taskName>
   ```

6. **View Task Dependencies**: See what other tasks a task depends on:
   ```bash
   ./gradlew <taskName> --dry-run
   ```

## Common Task Groups

**Build tasks:**
- `assemble` - Assembles the outputs of this project
- `build` - Assembles and tests this project
- `clean` - Deletes the build directory
- `jar` - Assembles a JAR archive

**Verification tasks:**
- `check` - Runs all checks (including tests)
- `test` - Runs the unit tests

**Documentation tasks:**
- `javadoc` - Generates Javadoc API documentation
- `javadocJar` - Assembles a JAR containing Javadoc

**Help tasks:**
- `help` - Displays help information
- `tasks` - Displays the tasks runnable from root project
- `projects` - Displays the sub-projects
- `properties` - Displays the properties of root project
- `dependencies` - Displays all dependencies

## Executing Tasks

```bash
# Run a single task
./gradlew build

# Run multiple tasks
./gradlew clean build test

# Run task from specific project (multi-module)
./gradlew :subproject:build

# Run task with options
./gradlew test --tests MyTest

# Skip a task
./gradlew build -x test

# Run task with continuous mode
./gradlew test --continuous

# Run with performance profile
./gradlew build --profile
```

## Task Options

Common command-line options:
- `--info` - More detailed logging
- `--debug` - Full debug logging
- `--stacktrace` - Show stack traces
- `--scan` - Create build scan
- `--parallel` - Execute tasks in parallel
- `--offline` - Execute build without network access
- `--refresh-dependencies` - Force refresh of dependencies
- `--rerun-tasks` - Force tasks to run even if up-to-date
- `--continuous` - Continuous build mode
- `--dry-run` - Show what would be executed without running

## Custom Tasks

View custom tasks defined in your project:
```bash
./gradlew tasks --all | grep "other"
```

Example custom task in `build.gradle`:
```groovy
task hello {
    doLast {
        println 'Hello, Gradle!'
    }
}
```

Run it with:
```bash
./gradlew hello
```

## Task Graphs

View task execution graph:
```bash
./gradlew build --dry-run
```

This shows the order tasks will execute without actually running them.

## Debugging Tasks

```bash
# See why a task is executed or skipped
./gradlew build --info

# See full task execution details
./gradlew build --debug

# See what files changed to trigger re-execution
./gradlew build --info | grep "file changed"

# Profile task execution times
./gradlew build --profile
```

The profile report is saved to `build/reports/profile/`.

## Notes

- Tasks can depend on other tasks
- Gradle intelligently skips up-to-date tasks (incremental builds)
- Use task abbreviation: `./gradlew cB` for `clean build`
- Multi-module projects have tasks scoped by project
- Custom tasks can be added via plugins or directly in build scripts
