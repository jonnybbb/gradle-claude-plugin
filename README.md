# Gradle Plugin for Claude Code

A comprehensive Claude Code plugin that provides Gradle build automation skills, commands, and agents to help you work effectively with Gradle projects.

## Features

### 🎯 Skills
- **Gradle Skill** - Complete Gradle project management with workflows for:
  - Project initialization
  - Building and compilation
  - Running tests
  - Managing dependencies
  - Executing tasks
  - Managing Gradle wrapper

### ⚡ Commands
- `/gradle-init` - Initialize a new Gradle project interactively
- `/gradle-build` - Build the current project with options
- `/gradle-test` - Run tests with pattern matching support

### 🤖 Agents
- **Gradle Troubleshooter** - Specialized agent for diagnosing and fixing Gradle build issues

### 🪝 Hooks
- Project detection - Automatically offers help when opening Gradle projects
- Build validation - Suggests validation before commits
- Configuration monitoring - Notifies when build files change

## Installation

1. Clone this repository or download the plugin
2. Install in Claude Code following the plugin installation instructions
3. The plugin will automatically detect Gradle projects

## Usage

### Using Skills
Simply ask Claude about Gradle tasks:
- "Initialize a new Java application with Gradle"
- "Build the project"
- "Run all tests"
- "Show me the dependency tree"

### Using Commands
Use slash commands for quick actions:
```
/gradle-init
/gradle-build
/gradle-test MyTestClass
```

### Using Agents
The Gradle Troubleshooter agent automatically assists with build issues, or you can invoke it explicitly when debugging problems.

## Requirements

- Gradle 7.0 or higher (or the plugin will help you set up the wrapper)
- Java 11 or higher for most Gradle projects

## Project Structure

```
.claude-plugin/        # Plugin manifest
├── plugin.json        # Plugin configuration

skills/                # Gradle skills
└── gradle/
    ├── SKILL.md       # Main skill definition
    └── workflows/     # Individual workflows
        ├── init.md
        ├── build.md
        ├── test.md
        ├── dependencies.md
        ├── tasks.md
        └── wrapper.md

commands/              # Slash commands
├── gradle-init.md
├── gradle-build.md
└── gradle-test.md

agents/                # Specialized agents
└── gradle-troubleshooter.md

hooks/                 # Event hooks
└── hooks.json
```

## Contributing

Contributions are welcome! Please feel free to submit issues or pull requests to improve the plugin.

## License

MIT License - See LICENSE file for details
