# Claude Code - Gradle Expert Plugin

[![Version](https://img.shields.io/badge/version-1.1.0-blue.svg)](https://github.com/jonnybbb/claude-gradle-skills)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Claude Code](https://img.shields.io/badge/Claude%20Code-Plugin-purple.svg)](https://docs.claude.com/en/docs/claude-code/plugins)

This Claude Code plugin provides comprehensive Gradle expertise covering all aspects of Gradle build automation based on [official Gradle documentation](https://docs.gradle.org/current/userguide/userguide.html).

## 📦 Installation

### Option 1: Install from Marketplace (Recommended)

Add this marketplace to Claude Code:

```bash
/plugin marketplace add jonnybbb/claude-gradle-skills
```

Then install the plugin:

```bash
/plugin install gradle-expert
```

### Option 2: Install Directly from GitHub

```bash
/plugin install jonnybbb/claude-gradle-skills
```

### Option 3: Local Development

Clone the repository and install locally:

```bash
git clone https://github.com/jonnybbb/claude-gradle-skills.git
cd claude-gradle-skills
# In Claude Code, run:
/plugin install /path/to/claude-gradle-skills
```

## Overview

The Gradle Expert plugin provides Claude Code with comprehensive Gradle knowledge across multiple domains:

### 🎯 General Gradle Expertise
- Project initialization and structure
- Dependency management and resolution
- Task creation and configuration
- Plugin development and application
- Build script organization
- Multi-module project setup
- Source set configuration
- Publishing artifacts

### ⚡ Performance Optimization
- Configuration cache (50-80% faster builds)
- Build cache (30-70% faster clean builds)
- Parallel execution (20-50% faster multi-module builds)
- File system watching
- JVM memory tuning
- Kotlin incremental compilation
- KSP over KAPT optimization

### 🔧 Troubleshooting & Debugging
- Dependency conflict resolution
- Build failure diagnosis
- Task debugging
- Build scan analysis
- Performance profiling
- Cache issues
- Version conflicts

### 🏗️ Multi-Module Projects
- Project structure best practices
- Inter-module dependencies
- Composite builds
- Shared configuration
- Version catalog management

### 🧪 Testing Configuration
- JUnit 5 setup
- Test filtering and execution
- Integration test configuration
- Parallel test execution
- Test reporting

## Features

### 📁 Plugin Structure

This plugin includes multiple specialized skills:

```
.
├── .claude-plugin/
│   ├── plugin.json              # Plugin manifest
│   └── marketplace.json         # Marketplace configuration
├── skills/
│   ├── general/
│   │   └── SKILL.md            # General Gradle expertise
│   └── performance/
│       └── SKILL.md            # Performance optimization skill
├── templates/
│   └── gradle.properties.optimized  # Optimized gradle.properties template
├── QUICK_REFERENCE.md           # Quick reference guide
├── LICENSE                      # MIT License
└── README.md                    # This file
```

## Usage with Claude Code

### Quick Start Examples

#### 1. **General Gradle Help**
```
How do I create a multi-module Gradle project?
```

```
Help me set up JUnit 5 in my Gradle project
```

```
How do I manage dependencies for a multi-module project?
```

#### 2. **Performance Optimization**
```
Analyze my Gradle build configuration and suggest performance improvements
```

```
Apply recommended Gradle performance optimizations to my gradle.properties
```

```
Help me enable configuration cache in my project
```

#### 3. **Troubleshooting**
```
My Gradle build is failing with a dependency conflict. Help me resolve it.
```

```
Why is my Gradle build so slow?
```

```
I'm getting an OutOfMemoryError during builds. How do I fix it?
```

#### 4. **Project Setup**
```
Set up a new Spring Boot project with Gradle
```

```
Help me configure Kotlin DSL for my Gradle build
```

```
Create a Gradle plugin for my custom build logic
```

## Skills Included

### 1. General Gradle Skill

Provides expertise in:
- Project setup and initialization
- Dependency management (configurations, catalogs, constraints)
- Plugin development and application
- Task management and lifecycle
- Build script best practices
- Multi-module project structure
- Testing configuration
- Publishing and distribution
- IDE integration
- Gradle wrapper management

**Key Commands Covered:**
```bash
gradle init
./gradlew build
./gradlew dependencies
./gradlew tasks --all
./gradlew wrapper --gradle-version 8.5
```

### 2. Performance Optimization Skill

Specialized knowledge for optimizing build performance:
- Configuration cache setup and troubleshooting
- Build cache configuration (local and remote)
- Parallel execution tuning
- File system watching
- JVM memory configuration
- Kotlin-specific optimizations
- Dependency resolution optimization
- Build profiling and analysis

**Expected Performance Improvements:**
| Optimization | Improvement |
|--------------|-------------|
| Configuration Cache | 50-80% faster subsequent builds |
| Build Cache | 30-70% faster clean builds |
| Parallel Execution | 20-50% faster multi-module builds |
| Combined | 70-90% faster in optimal scenarios |

## Templates

### Optimized gradle.properties

A complete, production-ready `gradle.properties` template is available in `templates/gradle.properties.optimized`. It includes:

- All core performance optimizations
- Detailed comments explaining each setting
- Memory configurations for different project sizes
- Kotlin-specific optimizations
- Android-specific optimizations (when applicable)
- Remote build cache configuration examples

### Using the Template

1. Copy the template to your project root:
   ```bash
   cp templates/gradle.properties.optimized /path/to/your/project/gradle.properties
   ```

2. Adjust memory settings based on your project size:
   - Small projects: `-Xmx1g`
   - Medium projects: `-Xmx2g`
   - Large projects: `-Xmx4g`
   - Very large projects: `-Xmx6g`

3. Test the configuration:
   ```bash
   ./gradlew clean build --scan
   ```

## Common Use Cases

### Setting Up a New Project
```
Initialize a new Kotlin application with Gradle using best practices
```

Claude will help you:
- Run `gradle init` with appropriate parameters
- Set up build.gradle.kts with modern APIs
- Configure dependencies and repositories
- Add testing framework
- Set up performance optimizations

### Migrating to Kotlin DSL
```
Help me migrate my Groovy build.gradle to Kotlin build.gradle.kts
```

Claude will:
- Convert syntax from Groovy to Kotlin
- Use type-safe accessors
- Apply Kotlin DSL best practices
- Update string interpolation
- Fix common migration issues

### Optimizing Build Performance
```
My builds are taking 5 minutes. Optimize my Gradle configuration.
```

Claude will:
- Analyze current configuration
- Enable configuration cache
- Set up build cache
- Configure parallel execution
- Tune JVM memory
- Provide measurement strategy

### Resolving Dependency Conflicts
```
I have a version conflict between guava 30.0 and 31.0. How do I resolve it?
```

Claude will:
- Show dependency tree analysis
- Explain conflict resolution strategies
- Provide force/constraint solutions
- Suggest dependency management best practices

## Best Practices

### 1. Always Use the Gradle Wrapper
```bash
# Update wrapper to latest version
./gradlew wrapper --gradle-version 8.5
```

### 2. Prefer Modern APIs
```groovy
// Modern (recommended)
tasks.register('myTask') {
    // Lazy configuration
}

// Old (avoid)
task myTask {
    // Eager configuration
}
```

### 3. Use Dependency Catalogs
```toml
# gradle/libs.versions.toml
[versions]
junit = "5.10.0"

[libraries]
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
```

### 4. Enable Performance Features
```properties
# gradle.properties
org.gradle.configuration-cache=true
org.gradle.caching=true
org.gradle.parallel=true
```

### 5. Keep Gradle Updated
```bash
./gradlew wrapper --gradle-version latest
```

## Troubleshooting

### Out of Memory Errors

Increase heap size in gradle.properties:
```properties
org.gradle.jvmargs=-Xmx6g -XX:MaxMetaspaceSize=1536m
```

### Dependency Resolution Failures

```bash
# View dependency tree
./gradlew dependencies

# Refresh dependencies
./gradlew build --refresh-dependencies
```

### Slow Builds

```bash
# Generate build scan for analysis
./gradlew build --scan

# Profile the build
./gradlew build --profile
```

## Resources

- [Official Gradle Documentation](https://docs.gradle.org/current/userguide/userguide.html)
- [Gradle Performance Guide](https://docs.gradle.org/current/userguide/performance.html)
- [Gradle Best Practices](https://docs.gradle.org/current/userguide/best_practices_performance.html)
- [Gradle Plugin Portal](https://plugins.gradle.org/)
- [Build Scans](https://scans.gradle.com)
- [Gradle Releases](https://gradle.org/releases/)

## 🛠️ Plugin Development

### Testing Locally

1. Clone this repository
2. Make your changes to skills or templates
3. Install locally in Claude Code:
   ```bash
   /plugin install /path/to/claude-gradle-skills
   ```
4. Test the plugin functionality
5. Uninstall and reinstall after changes:
   ```bash
   /plugin uninstall gradle-expert
   /plugin install /path/to/claude-gradle-skills
   ```

### Plugin Structure

- **`skills/general/SKILL.md`**: General Gradle expertise
- **`skills/performance/SKILL.md`**: Performance optimization knowledge
- **`.claude-plugin/plugin.json`**: Plugin metadata and configuration
- **`.claude-plugin/marketplace.json`**: Marketplace listing configuration
- **`templates/`**: Reusable configuration templates

### Adding New Skills

1. Create a new directory under `skills/` (e.g., `skills/android/`)
2. Add `SKILL.md` with expertise in that domain
3. Update version in `.claude-plugin/plugin.json` and `.claude-plugin/marketplace.json`
4. Document the new skill in README.md
5. Test with real-world scenarios

## Contributing

Contributions are welcome! To improve this plugin:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Update skills in `skills/*/SKILL.md` with new best practices
4. Update `templates/` with new configurations
5. Update this README with usage examples
6. Test with real-world Gradle projects
7. Bump version numbers in plugin.json and marketplace.json
8. Submit a pull request

### Contribution Ideas

- Add Android-specific Gradle skill
- Add Spring Boot/Micronaut Gradle configurations
- Include more build script examples
- Add troubleshooting guides for specific scenarios
- Create additional templates for different project types
- Add slash commands for common Gradle operations
- Create integration test examples
- Add Docker/container build configurations

## 📄 License

This plugin is released under the [MIT License](LICENSE). See LICENSE file for details.

## 🙏 Acknowledgments

- Based on official [Gradle Documentation](https://docs.gradle.org/)
- Built for the [Claude Code](https://www.anthropic.com/news/claude-code-plugins) plugin ecosystem

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/jonnybbb/claude-gradle-skills/issues)
- **Discussions**: [GitHub Discussions](https://github.com/jonnybbb/claude-gradle-skills/discussions)
- **Documentation**: [Claude Code Docs](https://docs.claude.com/en/docs/claude-code)

## Version History

### v1.1.0 (2025-11-05)
- **BREAKING**: Renamed plugin from `gradle-performance` to `gradle-expert`
- Added comprehensive general Gradle skill covering all aspects of Gradle
- Restructured skills into modular format: `general/` and `performance/`
- Expanded scope beyond just performance to include:
  - Project setup and structure
  - Dependency management
  - Task configuration
  - Plugin development
  - Multi-module projects
  - Testing
  - Publishing
  - Troubleshooting
- Updated keywords and descriptions to reflect broader scope
- Maintained all existing performance optimization features

### v1.0.0 (2025-11-05)
- Initial release with comprehensive Gradle performance optimizations based on Gradle 8.x documentation
- Skills for configuration cache, build cache, parallel execution, file system watching
- JVM memory optimization guidance
- Kotlin-specific optimizations (incremental compilation, KSP)
- Optimized gradle.properties template
- Quick reference guide
- Full Claude Code plugin marketplace support

---

Made with ❤️ for the Gradle and Claude Code communities
