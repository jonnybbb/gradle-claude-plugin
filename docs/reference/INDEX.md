# Gradle Expert Documentation Reference Index

This index maps Gradle topics to focused reference documentation files. Skills should reference these files rather than embedding large documentation blocks.

## Performance & Optimization

- `performance-tuning.md` - Build performance optimization strategies
- `parallel-execution.md` - Parallel task execution configuration
- `daemon-optimization.md` - Gradle daemon tuning
- `jvm-args.md` - JVM arguments and memory configuration

## Caching

- `configuration-cache.md` - Configuration cache concepts and troubleshooting
- `build-cache.md` - Build cache setup and optimization
- `cache-compatibility.md` - Common cache compatibility issues and fixes
- `remote-cache-setup.md` - Remote cache infrastructure

## Task Development

- `task-basics.md` - Task implementation fundamentals
- `incremental-tasks.md` - Incremental task development
- `task-inputs-outputs.md` - Task input/output configuration
- `task-avoidance.md` - Task avoidance and lazy configuration

## Plugin Development

- `plugin-basics.md` - Plugin development fundamentals
- `plugin-testing.md` - Plugin testing strategies
- `plugin-publishing.md` - Plugin publishing and distribution
- `extension-objects.md` - Extension object design

## Build Structure

- `multi-project.md` - Multi-project build organization
- `composite-builds.md` - Composite build patterns
- `buildSrc.md` - buildSrc and convention plugins
- `settings-gradle.md` - Settings file configuration

## Dependencies

- `dependency-management.md` - Dependency management fundamentals
- `version-catalogs.md` - Version catalog usage
- `conflict-resolution.md` - Dependency conflict strategies
- `dependency-constraints.md` - Dependency constraints and platforms

## Troubleshooting

- `common-errors.md` - Common build errors and solutions
- `debugging-builds.md` - Build debugging techniques
- `scan-analysis.md` - Build scan interpretation
- `logging-configuration.md` - Logging configuration

## Migration

- `gradle-6-to-7.md` - Gradle 6 to 7 migration guide
- `gradle-7-to-8.md` - Gradle 7 to 8 migration guide
- `gradle-8-to-9.md` - Gradle 8 to 9 migration guide
- `breaking-changes.md` - Breaking changes by version
- `deprecated-features.md` - Deprecated feature replacements

## Tooling API

- `tooling-api-basics.md` - Gradle Tooling API fundamentals
- `model-building.md` - Model building and queries
- `progress-events.md` - Progress event handling
- `connection-management.md` - Connection lifecycle

## DSL Reference

- `kotlin-dsl-patterns.md` - Kotlin DSL idioms
- `groovy-dsl-patterns.md` - Groovy DSL idioms
- `dsl-migration.md` - Groovy to Kotlin DSL migration

## Usage Guidelines

**For Skills:**
- Reference specific files using relative paths: `../../docs/reference/topic.md`
- Keep skill files focused - offload detailed reference to these docs
- Update references when new patterns emerge

**For Documentation Maintenance:**
- Keep each file under 500 lines
- Focus on actionable information
- Include both Kotlin and Groovy examples
- Update from official Gradle docs quarterly
