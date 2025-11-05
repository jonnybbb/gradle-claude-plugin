# Claude Code - Gradle Performance Optimization Plugin

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/jonnybbb/claude-gradle-skills)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Claude Code](https://img.shields.io/badge/Claude%20Code-Plugin-purple.svg)](https://docs.claude.com/en/docs/claude-code/plugins)

This Claude Code plugin provides comprehensive knowledge and tools for optimizing Gradle build performance based on [official Gradle documentation](https://docs.gradle.org/current/userguide/performance.html).

## 📦 Installation

### Option 1: Install from Marketplace (Recommended)

Add this marketplace to Claude Code:

```bash
/plugin marketplace add jonnybbb/claude-gradle-skills
```

Then install the plugin:

```bash
/plugin install gradle-performance
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

The Gradle Performance plugin enables Claude Code to:
- Analyze existing Gradle build configurations
- Recommend performance optimizations
- Apply best practices from official Gradle documentation
- Generate optimized `gradle.properties` configurations
- Identify common performance bottlenecks
- Provide expert guidance on all Gradle performance features

## Features

### 🚀 Core Optimizations Covered

1. **Configuration Cache** - Cache configuration phase results (50-80% faster builds)
2. **Build Cache** - Reuse task outputs from previous builds (30-70% faster)
3. **Parallel Execution** - Execute tasks in parallel across projects
4. **File System Watching** - Improve incremental build performance
5. **JVM Memory Tuning** - Optimize memory settings for your project size
6. **Kotlin Optimizations** - Incremental compilation and KSP over KAPT
7. **Dependency Management** - Optimize dependency resolution

### 📁 Plugin Structure

This plugin follows the [Claude Code plugin format](https://docs.claude.com/en/docs/claude-code/plugins-reference):

```
.
├── .claude-plugin/
│   ├── plugin.json              # Plugin manifest
│   └── marketplace.json         # Marketplace configuration
├── skills/
│   └── gradle-performance/
│       └── SKILL.md             # Main performance optimization skill
├── templates/
│   └── gradle.properties.optimized  # Optimized gradle.properties template
├── QUICK_REFERENCE.md           # Quick reference guide
├── LICENSE                      # MIT License
└── README.md                    # This file
```

## Usage with Claude Code

### Quick Start

1. **Ask Claude to analyze your build**:
   ```
   Analyze my Gradle build configuration and suggest performance improvements
   ```

2. **Apply optimizations**:
   ```
   Apply recommended Gradle performance optimizations to my gradle.properties
   ```

3. **Get specific guidance**:
   ```
   How can I enable configuration cache in my Gradle project?
   ```

### Common Use Cases

#### Analyze Existing Configuration
```
Review my gradle.properties and identify missing performance optimizations
```

#### Generate Optimized Configuration
```
Create an optimized gradle.properties for a large multi-module Android project
```

#### Troubleshoot Performance Issues
```
My Gradle builds are slow. Help me identify and fix performance bottlenecks
```

#### Migration Assistance
```
Help me migrate to configuration cache
```

## Performance Optimizations

### Key Settings

The skill provides guidance on all critical Gradle performance settings:

```properties
# Configuration Cache (Gradle 8.1+)
org.gradle.configuration-cache=true

# Build Cache
org.gradle.caching=true

# Parallel Execution
org.gradle.parallel=true

# File System Watching
org.gradle.vfs.watch=true

# Memory Configuration
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g

# Kotlin Optimizations
kotlin.incremental=true
kotlin.daemon.jvmargs=-Xmx2g
```

### Expected Performance Improvements

| Optimization | Expected Improvement |
|--------------|---------------------|
| Configuration Cache | 50-80% faster subsequent builds |
| Build Cache | 30-70% faster clean builds |
| Parallel Execution | 20-50% faster multi-module builds |
| Combined Optimizations | 70-90% faster in optimal scenarios |

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

## Best Practices

### 1. Always Measure Performance

Before and after applying optimizations:
```bash
# Generate build scan
./gradlew build --scan

# Generate profile report
./gradlew build --profile
```

### 2. Verify Configuration Cache Compatibility

```bash
# Test configuration cache
./gradlew build --configuration-cache

# Check for problems
./gradlew build --configuration-cache --configuration-cache-problems=warn
```

### 3. Keep Gradle Updated

```bash
# Update to latest Gradle version
./gradlew wrapper --gradle-version 8.11
```

### 4. Use Modern Gradle APIs

- Prefer `tasks.register()` over `task` for lazy evaluation
- Avoid work in configuration phase
- Use task configuration avoidance

## Troubleshooting

### Out of Memory Errors

Increase heap size in gradle.properties:
```properties
org.gradle.jvmargs=-Xmx6g -XX:MaxMetaspaceSize=1536m
```

### Configuration Cache Issues

Check compatibility and warnings:
```bash
./gradlew build --configuration-cache --configuration-cache-problems=warn
```

Common fixes:
- Avoid `buildscript` block side effects
- Use lazy task configuration
- Don't use `Project` at execution time

### Slow Dependency Resolution

Add to build.gradle:
```groovy
configurations.all {
    resolutionStrategy {
        cacheDynamicVersionsFor 10, 'minutes'
        cacheChangingModulesFor 4, 'hours'
    }
}
```

## Advanced Topics

### Remote Build Cache

Configure in `settings.gradle`:
```groovy
buildCache {
    local {
        enabled = true
    }
    remote(HttpBuildCache) {
        url = 'https://your-cache-server.com/cache/'
        push = true
    }
}
```

### Kotlin KSP vs KAPT

Migrate from KAPT to KSP for better performance:

**Before (KAPT)**:
```kotlin
plugins {
    kotlin("kapt")
}
dependencies {
    kapt("com.google.dagger:dagger-compiler:2.x")
}
```

**After (KSP)**:
```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.9.x-1.0.x"
}
dependencies {
    ksp("com.google.dagger:dagger-compiler:2.x")
}
```

## Resources

- [Official Gradle Performance Guide](https://docs.gradle.org/current/userguide/performance.html)
- [Gradle Best Practices](https://docs.gradle.org/current/userguide/best_practices_performance.html)
- [Build Scans](https://scans.gradle.com)
- [Gradle Releases](https://gradle.org/releases/)

## 🛠️ Plugin Development

### Testing Locally

1. Clone this repository
2. Make your changes to the skill or templates
3. Install locally in Claude Code:
   ```bash
   /plugin install /path/to/claude-gradle-skills
   ```
4. Test the plugin functionality
5. Uninstall and reinstall after changes:
   ```bash
   /plugin uninstall gradle-performance
   /plugin install /path/to/claude-gradle-skills
   ```

### Plugin Structure

- **`skills/gradle-performance/SKILL.md`**: The main skill file containing all optimization knowledge
- **`.claude-plugin/plugin.json`**: Plugin metadata and configuration
- **`.claude-plugin/marketplace.json`**: Marketplace listing configuration
- **`templates/`**: Reusable configuration templates

### Adding Features

1. Edit `skills/gradle-performance/SKILL.md` to add new optimization techniques
2. Update `templates/gradle.properties.optimized` with new settings
3. Update version in `.claude-plugin/plugin.json` and `.claude-plugin/marketplace.json`
4. Update QUICK_REFERENCE.md with quick tips
5. Document changes in README.md

## Contributing

Contributions are welcome! To improve this plugin:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Update the skill in `skills/gradle-performance/SKILL.md` with new best practices
4. Update `templates/gradle.properties.optimized` with new settings
5. Update this README with usage examples
6. Test with real-world Gradle projects
7. Bump version numbers in plugin.json and marketplace.json
8. Submit a pull request

### Contribution Ideas

- Add more Gradle optimization techniques as they're released
- Include more build script examples
- Add troubleshooting guides for specific scenarios
- Create additional templates for different project types
- Add slash commands for common operations

## 📄 License

This plugin is released under the [MIT License](LICENSE). See LICENSE file for details.

## 🙏 Acknowledgments

- Based on official [Gradle Performance Documentation](https://docs.gradle.org/current/userguide/performance.html)
- Built for the [Claude Code](https://www.anthropic.com/news/claude-code-plugins) plugin ecosystem

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/jonnybbb/claude-gradle-skills/issues)
- **Discussions**: [GitHub Discussions](https://github.com/jonnybbb/claude-gradle-skills/discussions)
- **Documentation**: [Claude Code Docs](https://docs.claude.com/en/docs/claude-code)

## Version History

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
