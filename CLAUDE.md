# Claude Gradle Plugin - Development Context

## Project Overview

This is a comprehensive Claude plugin for Gradle development that transforms the official Gradle User Guide into actionable tools, skills, and agents. The plugin serves developers and build engineers working with Gradle 8+ and 9+, providing intelligent assistance for:

- Project analysis and structure understanding
- Build optimization and performance tuning
- Dependency management and conflict resolution
- Configuration cache and build cache troubleshooting
- Task and plugin development (Groovy and Kotlin DSL)
- Version migration guidance (Gradle 6/7 → 8/9)
- Automated troubleshooting and error detection

## Architecture

### Plugin Components

```
claude-gradle-plugin/
├── .claude-plugin/
│   └── plugin.json              # Plugin manifest and configuration
├── commands/                     # Slash commands for user-initiated actions
│   ├── createPlugin.md          # /createPlugin - Generate plugin scaffolding
│   ├── createTask.md            # /createTask - Create custom tasks
│   ├── doctor.md                # /doctor - Comprehensive build health check
│   └── reviewTask.md            # /reviewTask - Analyze task implementations
├── skills/                       # Model-invoked capabilities
│   ├── gradle-project-analysis/
│   ├── gradle-performance-tuning/
│   ├── gradle-dependency-resolver/
│   ├── gradle-cache-optimization/
│   ├── gradle-task-development/
│   ├── gradle-plugin-development/
│   ├── gradle-migration-assistant/
│   ├── gradle-troubleshooting/
│   └── gradle-build-structuring/
├── agents/                       # TypeScript agents for complex workflows
│   ├── gradle-build-agent.ts    # Build execution and management
│   ├── gradle-doctor-agent.ts   # Health analysis and diagnostics
│   ├── gradle-migration-agent.ts # Version migration guidance
│   └── gradle-dependency-agent.ts # Dependency analysis
├── hooks/                        # Event handlers for automation
│   └── hooks.json               # Build validation and error detection
├── scripts/                      # Utility scripts
│   ├── jbang/                   # JBang scripts using Gradle Tooling API
│   │   ├── analyze-build-health.jbang
│   │   ├── dependency-graph.jbang
│   │   ├── cache-diagnostics.jbang
│   │   └── migration-checker.jbang
│   └── helpers/                 # Shell helper scripts
└── CLAUDE.md                    # This file
```

## Core Expertise Domains

### 1. Project Analysis

**Capabilities:**
- Multi-module project structure detection
- Build file analysis (Groovy and Kotlin DSL)
- Plugin application pattern recognition
- Configuration hierarchy understanding
- Project topology visualization

**Key Patterns:**
- buildSrc/ conventions
- Composite builds
- Version catalogs (libs.versions.toml)
- Settings plugins
- Convention plugins

### 2. Performance Tuning

**Optimization Areas:**
- Build Cache (local and remote)
- Configuration Cache
- Gradle Daemon tuning
- Parallel execution
- Incremental compilation
- Task output caching
- Test selection strategies

**Metrics Analyzed:**
- Build duration
- Configuration time
- Task execution time
- Cache hit rates
- Daemon health

### 3. Dependency Management

**Capabilities:**
- Transitive dependency analysis
- Version conflict detection and resolution
- BOM (Bill of Materials) integration
- Dependency constraints management
- Version catalog creation
- Platform dependencies
- Dependency locking

**Common Issues:**
- Diamond dependency problems
- Version alignment
- API vs implementation configurations
- Forced versions

### 4. Build Cache Management

**Features:**
- Cache configuration validation
- Cache key analysis
- Remote cache setup
- Build scan integration
- Cache miss diagnostics
- Task cachability verification

**Troubleshooting:**
- Cache miss root causes
- Non-cacheable task identification
- Input/output tracking issues
- Path sensitivity problems

### 5. Configuration Cache

**Expertise:**
- Configuration cache adoption
- Incompatibility detection
- Serialization issue resolution
- Plugin compatibility checks
- Build logic refactoring

**Common Pitfalls:**
- Runtime API usage
- Task configuration avoidance
- Provider API adoption
- Lazy configuration

### 6. Task Development

**Languages Supported:**
- Kotlin DSL (build.gradle.kts)
- Groovy DSL (build.gradle)

**Development Areas:**
- Custom task implementation
- Incremental tasks
- Worker API usage
- Task input/output declarations
- Task dependencies and ordering
- Task caching optimization

**Best Practices:**
- Proper input annotation
- Output declarations
- Provider API usage
- Lazy configuration
- Type-safe accessors

### 7. Plugin Development

**Capabilities:**
- Plugin scaffolding generation
- Convention plugin patterns
- Precompiled script plugins
- Extension modeling
- Multi-project plugin organization

**Development Patterns:**
- buildSrc/ plugins
- Included builds
- Composite builds
- Published plugins

### 8. Migration Guidance

**Migration Paths:**
- Gradle 6.x → 8.x
- Gradle 7.x → 8.x
- Gradle 8.x → 9.x

**Migration Tasks:**
- Compatibility checking
- Deprecated API identification
- Wrapper updates
- Plugin compatibility verification
- Build script modernization
- Java version requirements

### 9. Troubleshooting

**Automated Detection:**
- :check task failures (checkstyle, lint, errorprone)
- Build script syntax errors
- Plugin application issues
- Dependency resolution failures
- Task execution errors

**Resolution Strategies:**
- Active fixes (automated corrections)
- Passive suggestions (manual guidance)
- Root cause analysis
- Build scan interpretation

## Technical Implementation

### Gradle Tooling API Integration

All JBang scripts leverage the Gradle Tooling API for programmatic interaction:

```java
//DEPS org.gradle:gradle-tooling-api:8.5
//JAVA 17+

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

// Connect to project
GradleConnector connector = GradleConnector.newConnector()
    .forProjectDirectory(new File(projectDir));

try (ProjectConnection connection = connector.connect()) {
    // Execute tasks, query models, etc.
}
```

### TypeScript Agent SDK

Agents use the Claude Agent SDK for complex workflows:

```typescript
import { query, tool, createSdkMcpServer } from '@anthropic-ai/claude-agent-sdk';
import { z } from 'zod';

const myTool = tool(
  'tool_name',
  'Tool description',
  { param: z.string() },
  async (args) => {
    // Tool implementation
    return { content: [{ type: 'text', text: result }] };
  }
);
```

### Skill Organization

Each skill is self-contained with:
- SKILL.md (definition and triggers)
- scripts/ (helper scripts for complex operations)
- Clear trigger phrases in descriptions

## Code Conventions

### Build Files

**Kotlin DSL (Preferred):**
```kotlin
plugins {
    id("java-library")
    id("maven-publish")
}

dependencies {
    api(libs.guava)
    implementation(libs.slf4j.api)
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
```

**Groovy DSL (Legacy Support):**
```groovy
plugins {
    id 'java-library'
    id 'maven-publish'
}

dependencies {
    api libs.guava
    implementation libs.slf4j.api
    testImplementation libs.junit.jupiter
}

test {
    useJUnitPlatform()
}
```

### Version Catalogs

**libs.versions.toml:**
```toml
[versions]
guava = "32.1.3-jre"
junit = "5.10.1"

[libraries]
guava = { module = "com.google.guava:guava", version.ref = "guava" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }

[plugins]
shadow = { id = "com.github.johnrengelman.shadow", version = "8.1.1" }
```

### Convention Plugins

**buildSrc/src/main/kotlin/java-library-conventions.gradle.kts:**
```kotlin
plugins {
    `java-library`
    `maven-publish`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

tasks.test {
    useJUnitPlatform()
}
```

## Key Constraints

### Safety Rules

1. **Never modify gradle-wrapper.jar** without explicit verification
2. **Always backup** settings.gradle before structural changes
3. **Validate builds** with `gradle tasks` before suggesting optimizations
4. **Respect Java version** constraints in toolchain configuration
5. **Test incrementally** - don't apply all optimizations at once
6. **Preserve user preferences** in gradle.properties

### Best Practices

1. **Prefer Kotlin DSL** for new code, maintain consistency in existing codebases
2. **Use version catalogs** for dependency management
3. **Implement convention plugins** for shared configuration
4. **Enable build cache and configuration cache** when appropriate
5. **Use Provider API** for lazy configuration
6. **Annotate task inputs/outputs** properly for caching
7. **Follow Gradle best practices** from official documentation

## Documentation Sources

### Primary References

- [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html)
- [Gradle DSL Reference](https://docs.gradle.org/current/dsl/)
- [Gradle Tooling API](https://docs.gradle.org/current/userguide/tooling_api.html)
- [Gradle Plugin Development](https://docs.gradle.org/current/userguide/custom_plugins.html)
- [Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html)
- [Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)

### Documentation Extraction

The plugin extracts actionable guidance from official documentation through:

1. **Semantic Analysis:** Parse documentation structure to identify concepts
2. **Pattern Recognition:** Extract common patterns and anti-patterns
3. **Example Extraction:** Collect code examples for both Groovy and Kotlin DSL
4. **Best Practice Synthesis:** Combine recommendations into actionable advice
5. **Version-Specific Guidance:** Track changes across Gradle versions

## Testing Strategy

### Validation Approach

1. **Build File Validation:** Use `gradle validatePlugins`
2. **Dry Run Testing:** Test with `--dry-run` flag
3. **Incremental Changes:** Apply one optimization at a time
4. **Rollback Capability:** Maintain git history for easy reversion
5. **Multi-Version Testing:** Test across Gradle 8.x and 9.x

### Test Projects

Maintain test fixtures for:
- Single-module Java projects
- Multi-module Kotlin projects
- Android projects
- Spring Boot applications
- Mixed Groovy/Kotlin DSL projects

## User Interaction Patterns

### Novice Users

- Provide detailed explanations
- Include step-by-step guidance
- Explain "why" behind recommendations
- Show examples in both DSLs

### Expert Users

- Concise recommendations
- Focus on edge cases
- Provide advanced optimization techniques
- Reference specific Gradle APIs

**Note:** No artificial complexity distinctions - tailor responses based on context and user questions, not assumptions.

## Future Enhancements

- Build scan integration for performance analysis
- CI/CD integration patterns (GitHub Actions, GitLab CI, Jenkins)
- Docker and containerization support
- Gradle Enterprise integration
- Custom build cache backend configuration
- Advanced profiling and flame graph analysis
