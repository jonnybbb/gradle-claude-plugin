# Changelog

All notable changes to the Gradle Plugin for Claude Code will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-11-05

### Added
- Initial release of Gradle plugin for Claude Code
- Gradle skill with 6 workflows:
  - Project initialization (init)
  - Building projects (build)
  - Running tests (test)
  - Managing dependencies (dependencies)
  - Working with tasks (tasks)
  - Managing Gradle wrapper (wrapper)
- Three slash commands:
  - `/gradle-init` - Initialize new Gradle projects
  - `/gradle-build` - Build projects with options
  - `/gradle-test` - Run tests with filtering
- Gradle Troubleshooter agent for debugging build issues
- Event hooks for:
  - Gradle project detection
  - Pre-commit build validation
  - Build file change notifications
- Comprehensive documentation for all features
- MIT License

### Features
- Support for Java, Kotlin, Groovy, Scala, and Android projects
- Interactive project initialization
- Dependency management and conflict resolution
- Task discovery and execution
- Gradle wrapper version management
- Build troubleshooting and debugging assistance
- Automatic project detection

## [Unreleased]

### Planned
- Android-specific workflows and commands
- Multi-module project support enhancements
- Gradle plugin development assistance
- Build cache optimization guidance
- Performance profiling integration
