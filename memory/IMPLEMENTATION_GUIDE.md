# Gradle Expert - Implementation Guide

This guide provides step-by-step instructions for completing the Gradle Expert plugin framework.

## Current Status

### ✅ Completed Components

1. **Framework Architecture** - Complete
   - Directory structure established
   - Plugin manifest (plugin.json)
   - Reference documentation index

2. **Documentation Layer** - Partial (3 of 25+ docs)
   - ✅ configuration-cache.md
   - ✅ performance-tuning.md
   - ✅ tooling-api-basics.md
   - ⏳ 22+ more reference docs needed

3. **Tool Layer** - Partial (2 of 5 tools)
   - ✅ gradle-analyzer.java (complete)
   - ✅ cache-validator.java (complete)
   - ⏳ performance-profiler.java
   - ⏳ task-analyzer.java
   - ⏳ build-health-check.java

4. **Agent Layer** - Partial (1 of 2 agents)
   - ✅ doctor-agent.ts (complete framework)
   - ⏳ migration-agent.ts

5. **Skills Layer** - Partial (2 of 10 skills)
   - ✅ gradle-doctor/SKILL.md
   - ✅ gradle-performance/SKILL.md
   - ⏳ gradle-config-cache/SKILL.md
   - ⏳ gradle-build-cache/SKILL.md
   - ⏳ gradle-task-development/SKILL.md
   - ⏳ gradle-plugin-development/SKILL.md
   - ⏳ gradle-dependencies/SKILL.md
   - ⏳ gradle-migration/SKILL.md
   - ⏳ gradle-troubleshooting/SKILL.md
   - ⏳ gradle-structure/SKILL.md

## Implementation Phases

### Phase 1: Complete Reference Documentation (Priority: HIGH)

Reference docs are the foundation - skills and tools depend on them.

#### 1.1 Performance & Optimization References

Create in `docs/reference/`:

- `parallel-execution.md`: Parallel build configuration
- `daemon-optimization.md`: Gradle daemon tuning
- `jvm-args.md`: JVM arguments and memory

**Template Pattern**:
```markdown
# [Topic] Reference

**Source**: [Gradle docs URL]
**Gradle Version**: X.x+

## Overview
[Brief description + performance impact]

## Key Concepts
[2-3 main concepts]

## Common Patterns
[Code examples in Kotlin + Groovy]

## Troubleshooting
[Common issues + solutions]

## Version-Specific Notes
[Differences by version]
```

#### 1.2 Caching References

Create in `docs/reference/`:

- `build-cache.md`: Build cache fundamentals
- `cache-compatibility.md`: Common compatibility issues
- `remote-cache-setup.md`: Remote cache infrastructure

#### 1.3 Task & Plugin Development References

Create in `docs/reference/`:

- `task-basics.md`: Task implementation fundamentals
- `incremental-tasks.md`: Incremental task patterns
- `task-inputs-outputs.md`: Input/output declaration
- `task-avoidance.md`: Task configuration avoidance
- `plugin-basics.md`: Plugin development fundamentals
- `plugin-testing.md`: Plugin testing strategies
- `extension-objects.md`: Extension object design

#### 1.4 Build Structure References

Create in `docs/reference/`:

- `multi-project.md`: Multi-project build patterns
- `composite-builds.md`: Composite build usage
- `buildSrc.md`: buildSrc and convention plugins
- `settings-gradle.md`: Settings file configuration

#### 1.5 Dependencies References

Create in `docs/reference/`:

- `dependency-management.md`: Dependency fundamentals
- `version-catalogs.md`: Version catalog patterns
- `conflict-resolution.md`: Dependency conflict strategies
- `dependency-constraints.md`: Constraints and platforms

#### 1.6 Troubleshooting References

Create in `docs/reference/`:

- `common-errors.md`: Common build errors + solutions
- `debugging-builds.md`: Build debugging techniques
- `scan-analysis.md`: Build scan interpretation
- `logging-configuration.md`: Logging setup

#### 1.7 Migration References

Create in `docs/reference/`:

- `gradle-6-to-7.md`: Migration guide with breaking changes
- `gradle-7-to-8.md`: Migration guide with breaking changes
- `gradle-8-to-9.md`: Migration guide (when Gradle 9 releases)
- `breaking-changes.md`: Breaking changes by version
- `deprecated-features.md`: Deprecated feature replacements

#### 1.8 DSL References

Create in `docs/reference/`:

- `kotlin-dsl-patterns.md`: Kotlin DSL idioms
- `groovy-dsl-patterns.md`: Groovy DSL idioms
- `dsl-migration.md`: Groovy to Kotlin DSL migration

### Phase 2: Complete JBang Tools (Priority: HIGH)

Tools provide programmatic analysis capabilities.

#### 2.1 performance-profiler.java

**Purpose**: Analyze build performance and identify bottlenecks

**Features**:
- Parse build scan data (if available)
- Parse --profile report
- Identify slowest tasks
- Measure configuration vs execution time ratio
- Detect parallel execution opportunities

**Template**: Use `gradle-analyzer.java` as base

**Output**: Performance metrics + bottleneck analysis

#### 2.2 task-analyzer.java

**Purpose**: Analyze task implementations for optimization

**Features**:
- Extract task source code
- Check input/output declarations
- Detect incremental task support
- Identify cacheability issues
- Suggest optimizations

**Key Checks**:
- Proper @InputFiles, @OutputDirectory annotations
- PathSensitivity configuration
- Incremental task implementation
- Cacheable annotation presence

#### 2.3 build-health-check.java

**Purpose**: Quick health check focusing on critical issues

**Features**:
- Validate gradle.properties
- Check wrapper version
- Verify essential optimizations
- Detect critical anti-patterns
- Generate health score (0-100)

**Health Score Factors**:
- Gradle version (current = 100, older = penalty)
- Performance settings (parallel, caching, etc.)
- Wrapper presence
- buildSrc usage
- Version catalog usage

### Phase 3: Complete Skills (Priority: MEDIUM)

Skills are the user-facing interface.

#### 3.1 gradle-config-cache/SKILL.md

**Focus**: Configuration cache migration and troubleshooting

**Key Sections**:
- When to Use
- Common Compatibility Issues
- Detection Patterns
- Auto-Fix Strategies
- Migration Steps
- Reference to configuration-cache.md

**Template**: Use `gradle-performance/SKILL.md` as base

#### 3.2 gradle-build-cache/SKILL.md

**Focus**: Build cache setup and optimization

**Key Sections**:
- Local vs Remote Cache
- Task Cacheability
- Cache Hit Rate Optimization
- Remote Cache Infrastructure
- Reference to build-cache.md

#### 3.3 gradle-task-development/SKILL.md

**Focus**: Custom task creation

**Commands**: `/createTask`, `/reviewTask`

**Key Sections**:
- Task Basics
- Incremental Tasks
- Input/Output Declaration
- Testing Tasks
- Code Generation Templates

#### 3.4 gradle-plugin-development/SKILL.md

**Focus**: Plugin scaffolding and development

**Commands**: `/createPlugin`

**Key Sections**:
- Plugin Structure
- Extension Objects
- Task Registration
- Testing Plugins
- Publishing
- Code Generation Templates

#### 3.5 gradle-dependencies/SKILL.md

**Focus**: Dependency management

**Key Sections**:
- Conflict Resolution
- Version Catalogs
- Dependency Locking
- Vulnerability Detection
- Optimization

#### 3.6 gradle-migration/SKILL.md

**Focus**: Version migration

**Key Sections**:
- Migration Paths (6→7, 7→8, 8→9)
- Breaking Change Detection
- Automated Fixes
- Manual Migration Steps
- Verification

#### 3.7 gradle-troubleshooting/SKILL.md

**Focus**: Build failure diagnosis

**Key Sections**:
- Common Errors
- Automated Detection
- Auto-Fix Capabilities
- Diagnostic Commands
- Build Scan Integration

#### 3.8 gradle-structure/SKILL.md

**Focus**: Build organization

**Key Sections**:
- Multi-Project Setup
- Composite Builds
- buildSrc Usage
- Convention Plugins
- Project Dependencies

### Phase 4: Complete Subagents (Priority: LOW)

Subagents orchestrate complex workflows.

#### 4.1 migration-agent.ts

**Purpose**: Orchestrate Gradle version migration

**Subagents**:
1. **version-analyzer**: Detect current/target versions
2. **breaking-change-detector**: Scan for breaking changes
3. **auto-fixer**: Apply automated fixes
4. **validation**: Run builds to verify migration

**Workflow**:
```
1. Analyze current version
2. Identify target version
3. List breaking changes
4. Apply auto-fixes (safe changes)
5. List manual changes required
6. Validate with test build
7. Generate migration report
```

**Template**: Use `doctor-agent.ts` as base

### Phase 5: Templates & Examples (Priority: LOW)

Code generation templates and usage examples.

#### 5.1 Plugin Scaffolding Templates

Create in `templates/plugin-scaffolding/`:

- `build.gradle.kts.template`: Plugin build file
- `Plugin.kt.template`: Main plugin class
- `Extension.kt.template`: Extension object
- `Task.kt.template`: Custom task
- `settings.gradle.kts.template`: Settings file

#### 5.2 Task Templates

Create in `templates/task-templates/`:

- `BasicTask.kt.template`: Simple task
- `IncrementalTask.kt.template`: Incremental task
- `WorkerApiTask.kt.template`: Parallel execution task

#### 5.3 Usage Examples

Create in `examples/`:

- `performance-optimization/`: Step-by-step optimization example
- `cache-troubleshooting/`: Cache issue resolution
- `migration-guides/`: Version migration examples
- `plugin-development/`: Complete plugin example

## Implementation Best Practices

### Reference Documentation

1. **Keep files focused**: <500 lines per file
2. **Include both DSLs**: Kotlin and Groovy examples
3. **Reference official docs**: Link to https://docs.gradle.org
4. **Version-specific notes**: Highlight differences by version
5. **Actionable content**: Focus on how-to, not theory

### JBang Tools

1. **Use Tooling API**: All tools should use Gradle Tooling API
2. **Error handling**: Handle connection failures gracefully
3. **Output formats**: Support both human-readable and JSON
4. **Resource cleanup**: Always close ProjectConnection
5. **Logging**: Use SLF4J with configurable levels

### Skills

1. **Clear trigger phrases**: When to use this skill
2. **Example conversations**: Show realistic usage
3. **Reference links**: Link to relevant docs
4. **Tool integration**: Show how to use JBang tools
5. **Version compatibility**: Note version-specific features

### Subagents

1. **Clear subagent boundaries**: Each subagent has one responsibility
2. **Structured output**: Use types/interfaces for results
3. **Error handling**: Handle Claude API failures
4. **Progress reporting**: Show progress for long operations
5. **Cost awareness**: Minimize API calls

## Testing Strategy

### Unit Tests

- JBang tools: Test against sample Gradle projects
- Subagents: Mock Claude API calls
- Skills: Verify reference links resolve

### Integration Tests

- Full workflow: Run /doctor on real projects
- Tool chain: Verify tool → agent → skill integration
- DSL coverage: Test both Kotlin and Groovy DSL

### Performance Tests

- Tool execution time: <30s for medium projects
- Agent orchestration: <2min for full /doctor run
- Context window usage: Monitor reference file sizes

## Deployment

### Package Structure

```
gradle-claude-plugin-v1.0.0/
├── plugin.json
├── README.md
├── skills/
├── tools/
├── agents/dist/
├── docs/
├── templates/
└── examples/
```

### Distribution

1. Build agents: `cd agents && npm run build`
2. Test all tools: `./test-tools.sh`
3. Verify skills: Check all reference links
4. Package: Create distributable archive
5. Publish: Upload to Claude plugin marketplace

## Development Workflow

### Daily Development

```bash
# 1. Pick next task from this guide
# 2. Create/edit files
# 3. Test changes
jbang tools/gradle-analyzer.java test-project

# 4. Verify references
./verify-references.sh

# 5. Commit
git commit -m "feat: add performance-profiler tool"
```

### Adding a New Skill

```bash
# 1. Create skill directory
mkdir -p skills/my-skill

# 2. Create SKILL.md from template
# 3. Add reference docs if needed
# 4. Register in plugin.json
# 5. Test with real questions
# 6. Document in README.md
```

## Completion Checklist

### Documentation (25 files)
- [ ] parallel-execution.md
- [ ] daemon-optimization.md
- [ ] jvm-args.md
- [ ] build-cache.md
- [ ] cache-compatibility.md
- [ ] remote-cache-setup.md
- [ ] task-basics.md
- [ ] incremental-tasks.md
- [ ] task-inputs-outputs.md
- [ ] task-avoidance.md
- [ ] plugin-basics.md
- [ ] plugin-testing.md
- [ ] extension-objects.md
- [ ] multi-project.md
- [ ] composite-builds.md
- [ ] buildSrc.md
- [ ] settings-gradle.md
- [ ] dependency-management.md
- [ ] version-catalogs.md
- [ ] conflict-resolution.md
- [ ] dependency-constraints.md
- [ ] common-errors.md
- [ ] debugging-builds.md
- [ ] scan-analysis.md
- [ ] logging-configuration.md
- [ ] gradle-6-to-7.md
- [ ] gradle-7-to-8.md
- [ ] gradle-8-to-9.md
- [ ] breaking-changes.md
- [ ] deprecated-features.md
- [ ] kotlin-dsl-patterns.md
- [ ] groovy-dsl-patterns.md
- [ ] dsl-migration.md

### Tools (3 tools)
- [ ] performance-profiler.java
- [ ] task-analyzer.java
- [ ] build-health-check.java

### Agents (1 agent)
- [ ] migration-agent.ts

### Skills (8 skills)
- [ ] gradle-config-cache/SKILL.md
- [ ] gradle-build-cache/SKILL.md
- [ ] gradle-task-development/SKILL.md
- [ ] gradle-plugin-development/SKILL.md
- [ ] gradle-dependencies/SKILL.md
- [ ] gradle-migration/SKILL.md
- [ ] gradle-troubleshooting/SKILL.md
- [ ] gradle-structure/SKILL.md

### Templates (8 templates)
- [ ] plugin-scaffolding templates
- [ ] task templates

### Examples (3 example projects)
- [ ] performance-optimization
- [ ] cache-troubleshooting
- [ ] migration-guides

## Estimated Timeline

- **Phase 1 (Documentation)**: 3-4 days (32 files)
- **Phase 2 (Tools)**: 1-2 days (3 tools)
- **Phase 3 (Skills)**: 2-3 days (8 skills)
- **Phase 4 (Subagents)**: 1 day (1 agent)
- **Phase 5 (Templates/Examples)**: 1-2 days

**Total**: 8-12 days for complete implementation

## Next Steps

1. **Immediate**: Complete Phase 1 (reference documentation)
2. **Short-term**: Complete Phase 2 (remaining tools)
3. **Mid-term**: Complete Phase 3 (remaining skills)
4. **Long-term**: Phases 4-5 (agents, templates, examples)

## Support

Questions or need clarification? Create an issue or discussion in the repository.
