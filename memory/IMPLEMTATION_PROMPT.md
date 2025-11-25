You are an expert software architect and plugin developer specializing in Gradle build tooling and Claude plugin development. Your task is to design and implement a comprehensive Claude Plugin that transforms Gradle user documentation into a powerful suite of tools, agents, and skills for developers and build engineers.

## Context and Background

You are creating a Claude Plugin (NOT a Gradle plugin) that will be distributed through the Claude marketplace. The plugin should analyze the Gradle User Guide (https://docs.gradle.org/current/userguide/userguide.html) and create intelligent tools that help with day-to-day Gradle development tasks.

## Target Requirements

**Version Support:**
- Primary support: Gradle 8+ and Gradle 9+
- Migration skill must include Gradle 6 and 7 for upgrade paths to Gradle 8/9

**Core Skills to Extract from Documentation:**
- Performance tuning
- Build cache management
- Configuration cache optimization
- Task development (Groovy and Kotlin DSL)
- Plugin development
- Build structuring
- Dependency management
- Gradle version migration
- Complex workflow build agents
- Troubleshooting and diagnostics

**Technical Implementation:**
- Use TypeScript for agent development
- Create tools using JBang with Java 25
- Leverage Gradle Tooling API (https://docs.gradle.org/current/userguide/tooling_api.html) for programmatic Gradle task execution
- Implement project-specific analysis and recommendations
- Always provide examples in both Groovy and Kotlin DSL

## Specific Commands and Skills

**Core Commands:**
- `/createPlugin` - Generate new Gradle plugin scaffolding
- `/createTask` - Create custom task implementations
- `/doctor` - Comprehensive build health analysis
- `/reviewTask` - Analyze existing task implementations

**Domain-Specific Skills:**
- Configuration cache troubleshooting and optimization
- Build cache analysis and fixes
- Performance tuning recommendations
- Dependency conflict resolution
- Migration pathway guidance

**Automated Troubleshooting:**
- Detect `:check` task failures (checkstyle, lint, errorprone, build script errors)
- Apply active fixes for straightforward issues
- Provide passive suggestions for complex changes requiring manual intervention

## Output Requirements

1. **Plugin Structure:** Design the complete Claude plugin architecture using plugin development best practices

2. **Skill Definitions:** Create individual skills using the skill creator skill, ensuring each skill addresses specific Gradle domains

3. **Agent Implementation:** Develop TypeScript-based agents for complex workflows and multi-step processes using the claude agent sdk

4. **Tool Creation:** Implement JBang-based Java 25 tools for programmatic Gradle interactions

5. **Documentation Integration:** Extract and transform official Gradle documentation into actionable, contextual guidance

6. **Common Use Cases:** Include guided examples for:
    - Troubleshooting build cache errors
    - Ensuring task cacheability
    - Performance optimization workflows
    - Migration best practices

## Implementation Guidelines

- Analyze existing build files to provide project-specific recommendations
- Handle cross-cutting concerns (build cache affects performance tuning)
- Implement hooks for immediate failure detection and resolution
- Provide both automated fixes and educational guidance
- Maintain consistency across Groovy and Kotlin DSL examples
- Ensure enterprise-ready reliability and scalability

## Deliverables

Provide a complete implementation including:
1. Claude plugin manifest and configuration
2. All skill definitions with detailed implementations
3. TypeScript agent code for complex workflows
4. JBang tool scripts for Gradle integration
5. Documentation extraction and transformation logic
6. Example usage scenarios and troubleshooting guides
7. Integration patterns with the Gradle Tooling API

Ensure the plugin serves both novice and expert users without artificial complexity distinctions, focusing on practical, actionable assistance for real-world Gradle development challenges.


