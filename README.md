# Claude Gradle Plugin

A comprehensive Claude plugin that transforms the Gradle User Guide into intelligent tools, skills, and agents for developers and build engineers. Provides expert assistance for Gradle 8+ and 9+ with migration support from Gradle 6/7.

## 🚀 Features

### Intelligent Skills

Nine specialized skills that Claude invokes autonomously:

- **🔍 gradle-project-analysis** - Analyzes project structure, dependencies, and configuration
- **⚡ gradle-performance-tuning** - Optimizes build performance through caching and parallelization
- **📦 gradle-dependency-resolver** - Resolves dependency conflicts and manages versions
- **💾 gradle-cache-optimization** - Optimizes build cache and configuration cache
- **🛠️ gradle-task-development** - Creates custom tasks with proper caching support
- **🔌 gradle-plugin-development** - Develops convention and binary plugins
- **🔄 gradle-migration-assistant** - Guides version migrations (6/7 → 8/9)
- **🔧 gradle-troubleshooting** - Diagnoses and fixes build failures
- **🏗️ gradle-build-structuring** - Organizes multi-module projects

### Slash Commands

Four powerful commands for common tasks:

- **`/createPlugin`** - Generate plugin scaffolding with best practices
- **`/createTask`** - Create custom tasks with proper inputs/outputs
- **`/doctor`** - Comprehensive build health analysis
- **`/reviewTask`** - Analyze and improve existing tasks

### TypeScript Agents

Four specialized agents for complex workflows:

- **gradle-build-agent** - Executes and manages complex builds
- **gradle-doctor-agent** - Comprehensive health diagnostics
- **gradle-migration-agent** - Version migration guidance
- **gradle-dependency-agent** - Advanced dependency analysis

### JBang Tools

Gradle Tooling API integration for programmatic analysis:

- **analyze-build-health.jbang** - Build health scoring and analysis
- **dependency-graph.jbang** - Dependency graph generation and conflict detection

## 📋 Requirements

- **Claude Code**: Latest version
- **Gradle**: 8.0+ (primary support for 8.5+ and 9.0+)
- **Java**: 17+ (for JBang tools)
- **Node.js**: 18+ (for TypeScript agents)
- **JBang**: Latest (optional, for Tooling API features)

## 🔧 Installation

### From Source

```bash
# Clone the repository
git clone https://github.com/jonnybbb/claude-gradle-plugin.git
cd claude-gradle-plugin

# Install TypeScript dependencies
npm install

# Build TypeScript agents
npm run build

# Make JBang scripts executable (optional)
chmod +x scripts/jbang/*.jbang
```

### Via Claude Plugin Marketplace

*(Coming soon)*

```
# Install from marketplace
claude plugin install gradle-development-suite
```

## 🎯 Quick Start

### Using Skills

Skills are automatically invoked by Claude based on your requests:

```
User: "Analyze my project structure"
→ Invokes: gradle-project-analysis skill

User: "My builds are slow, help me optimize"
→ Invokes: gradle-performance-tuning skill

User: "I have a dependency conflict with guava"
→ Invokes: gradle-dependency-resolver skill
```

### Using Commands

Execute slash commands directly:

```bash
# Generate a new plugin
/createPlugin

# Create a custom task
/createTask

# Run build health check
/doctor

# Review an existing task
/reviewTask processResources
```

### Using Agents

Agents handle complex, multi-step workflows:

```typescript
// Directly in TypeScript
import { runGradleBuildAgent } from './agents/gradle-build-agent';

await runGradleBuildAgent(projectDir, ['clean', 'build']);
```

Or invoke via Claude:

```
User: "Run a comprehensive build analysis with detailed diagnostics"
→ Claude invokes gradle-doctor-agent
```

## 📚 Documentation

### Project Structure

```
claude-gradle-plugin/
├── .claude-plugin/
│   └── plugin.json                    # Plugin manifest
├── commands/                           # Slash commands
│   ├── createPlugin.md
│   ├── createTask.md
│   ├── doctor.md
│   └── reviewTask.md
├── skills/                             # Claude-invoked skills
│   ├── gradle-project-analysis/
│   ├── gradle-performance-tuning/
│   ├── gradle-dependency-resolver/
│   ├── gradle-cache-optimization/
│   ├── gradle-task-development/
│   ├── gradle-plugin-development/
│   ├── gradle-migration-assistant/
│   ├── gradle-troubleshooting/
│   └── gradle-build-structuring/
├── agents/                             # TypeScript agents
│   ├── gradle-build-agent.ts
│   ├── gradle-doctor-agent.ts
│   ├── gradle-migration-agent.ts
│   └── gradle-dependency-agent.ts
├── hooks/                              # Event handlers
│   └── hooks.json
├── scripts/
│   └── jbang/                         # Gradle Tooling API tools
│       ├── analyze-build-health.jbang
│       └── dependency-graph.jbang
├── CLAUDE.md                          # Codebase context
├── package.json                       # Node dependencies
├── tsconfig.json                      # TypeScript config
└── README.md                          # This file
```

### Skill Examples

#### Project Analysis

```
User: "What's the structure of this project?"

Claude: I'll analyze your Gradle project structure.

=== Project Structure ===
Root: my-awesome-app
├── :app (application)
├── :lib-core (java-library)
└── :lib-utils (java-library)

--- Applied Plugins ---
:app: application, org.springframework.boot:3.1.5
:lib-core: java-library

--- Dependencies ---
:app → :lib-core (implementation)
:lib-core → :lib-utils (api)

--- Configuration ---
✓ Version catalog: gradle/libs.versions.toml
✓ Convention plugins: buildSrc/
⚠ No dependency locking enabled

--- Recommendations ---
1. Enable dependency locking for reproducible builds
2. Consider build cache and configuration cache
```

#### Performance Tuning

```
User: "Speed up my Gradle builds"

Claude: I'll analyze your build performance and suggest optimizations.

=== Performance Analysis ===

Current Configuration:
- Build cache: ❌ Not enabled
- Parallel builds: ❌ Not enabled
- Configuration cache: ❌ Not enabled
- Daemon heap: 2g (should be 4g)

Recommendations (High Impact First):

1. Enable Build Cache (50% faster builds)
   Add to gradle.properties:
   org.gradle.caching=true

2. Enable Parallel Execution (30% faster)
   Add to gradle.properties:
   org.gradle.parallel=true

3. Increase Daemon Heap (20% faster)
   Update gradle.properties:
   org.gradle.jvmargs=-Xmx4g

Potential Time Savings: 60-80% on typical builds
```

#### Dependency Conflict Resolution

```
User: "I have a version conflict with guava"

Claude: I'll analyze the conflict and provide resolution strategies.

=== Dependency Conflict Analysis ===

Conflict Detected:
  com.google.guava:guava
  Requested: 31.1-jre (by :app)
  Selected: 32.1.3-jre (by :lib-core)

Resolution Strategies:

Option 1: Force Specific Version (Recommended)
```kotlin
dependencies {
    constraints {
        implementation("com.google.guava:guava:32.1.3-jre") {
            because("Resolve version conflict, 31.x has CVE-2023-xxx")
        }
    }
}
```

Option 2: Use Platform/BOM
```kotlin
dependencies {
    implementation(platform("com.google.guava:guava-bom:32.1.3-jre"))
}
```

Recommendation: Use Option 1 (constraints) for better documentation
```

### Command Examples

#### /createPlugin

```
/createPlugin

> What type of plugin? convention
> Plugin ID: java-library-conventions
> Extension name: javaLib
> Tasks to create: generateBuildInfo

✅ Generated: buildSrc/src/main/kotlin/java-library-conventions.gradle.kts
✅ Includes: Extension, tasks, best practices
✅ Ready to use: apply plugin: "java-library-conventions"
```

#### /doctor

```
/doctor

=== Gradle Build Health Report ===

Overall Score: 78/100

CRITICAL ISSUES:
  1. ❌ Security vulnerability in org.example:lib:1.0
  2. ❌ Daemon heap too small

WARNINGS:
  1. ⚠️  Configuration cache not enabled
  2. ⚠️  3 tasks not cacheable

RECOMMENDATIONS:
  HIGH: Fix security vulnerability, increase heap
  MEDIUM: Enable configuration cache
  LOW: Update dependencies

Estimated improvement: 35-50% faster builds
```

### Migration Guide

Upgrading from Gradle 6 or 7 to Gradle 8:

```
User: "Help me migrate from Gradle 7 to Gradle 8"

Claude: I'll guide you through the migration process.

=== Migration Analysis ===

Current Version: 7.6
Target Version: 8.5
Migration Complexity: MEDIUM

Step 1: Update Wrapper
  ./gradlew wrapper --gradle-version=8.5

Step 2: Fix Deprecated APIs
  Found 3 deprecated usages:

  1. compile → implementation
     Line 23: compile 'com.google.guava:guava:32.1.3-jre'
     Fix: implementation 'com.google.guava:guava:32.1.3-jre'

  2. Project.convention → extensions
     Line 45: project.convention.getPlugin(JavaPluginConvention.class)
     Fix: project.extensions.getByType(JavaPluginExtension.class)

Step 3: Update Plugins
  - Spring Boot: 2.7.x → 3.1.5
  - Kotlin: 1.7.x → 1.9.20

Step 4: Test Build
  ./gradlew build --warning-mode=all

Would you like me to apply these fixes automatically?
```

## 🔧 Configuration

### gradle.properties (Recommended)

```properties
# Enable performance features
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.configuration-cache=true

# Optimize daemon
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError
org.gradle.daemon=true

# Worker threads (adjust based on CPU cores)
org.gradle.workers.max=8
```

### Version Catalog

Create `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "1.9.20"
spring-boot = "3.1.5"

[libraries]
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
spring-boot-starter = { module = "org.springframework.boot:spring-boot-starter-web", version.ref = "spring-boot" }

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
```

## 🎓 Best Practices

### Task Development

Always use:
- Abstract properties with Provider API
- Proper input/output annotations
- `@CacheableTask` for cacheable tasks
- `PathSensitivity.RELATIVE` for portability
- Lazy task registration with `tasks.register()`

### Plugin Development

Prefer:
- Convention plugins in `buildSrc/` for project-specific logic
- Version catalogs for dependency management
- Type-safe configuration with extensions
- Functional tests with Gradle TestKit

### Performance Optimization

Enable:
- Build cache (local and remote)
- Parallel execution
- Configuration cache (Gradle 8+)
- Incremental compilation
- Proper daemon heap size

## 🤝 Contributing

Contributions are welcome! Please see:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📝 License

MIT License - see [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Built on top of the [Claude Agent SDK](https://github.com/anthropics/claude-agent-sdk-typescript)
- Powered by [Gradle Tooling API](https://docs.gradle.org/current/userguide/tooling_api.html)
- Inspired by the [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html)
- Uses [JBang](https://www.jbang.dev/) for Java scripting

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/jonnybbb/claude-gradle-plugin/issues)
- **Documentation**: [CLAUDE.md](CLAUDE.md)
- **Gradle Docs**: [https://docs.gradle.org](https://docs.gradle.org)

## 🗺️ Roadmap

- [ ] Gradle Enterprise build scan integration
- [ ] Advanced cache diagnostics
- [ ] Automated performance regression detection
- [ ] CI/CD integration templates
- [ ] Docker and containerization support
- [ ] Custom report generation
- [ ] Interactive migration wizard
- [ ] Plugin marketplace publication

## ⭐ Star History

If you find this plugin useful, please consider starring the repository!

---

**Made with ❤️ for the Gradle community**
