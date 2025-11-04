# Gradle Wrapper Plugin

An example plugin demonstrating how to provide Gradle skills to Claude Code.

## Overview

This plugin enables Claude Code to work with Gradle projects, including initializing new projects, building, testing, and managing dependencies.

## Skills Provided

### gradle-init
Initialize a new Gradle project with your choice of project type and build script DSL.

**Usage Examples:**
- "Create a new Java application using Gradle"
- "Initialize a Kotlin library project with Gradle"
- "Set up a basic Java library with Gradle wrapper"

### gradle-build
Build, compile, test, and package your Gradle projects.

**Usage Examples:**
- "Build the project with Gradle"
- "Run all tests using Gradle"
- "Create a distribution package"

### gradle-dependencies
Manage and inspect project dependencies.

**Usage Examples:**
- "Show me the dependency tree"
- "Add JUnit 5 as a test dependency"
- "Update the Spring Boot version to 3.2.0"

### gradle-tasks
List and execute available Gradle tasks.

**Usage Examples:**
- "What Gradle tasks are available?"
- "Run the test task"
- "Execute clean and build tasks"

## Requirements

- Gradle 7.0 or higher
- Java 11 or higher (for most Gradle projects)

## Installation

This is an example plugin included in the marketplace by default. For production use, plugins would be installed through the Claude Code plugin system.

## Contributing

See the main repository's CONTRIBUTING.md for guidelines on enhancing this plugin or creating new ones.

## License

MIT License - See LICENSE file for details
