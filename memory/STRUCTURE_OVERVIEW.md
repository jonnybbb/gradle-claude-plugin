# Gradle Expert - Project Structure Overview

Quick reference for the complete framework architecture.

## Directory Tree

```
gradle-expert/
│
├── plugin.json                          # Plugin manifest - registry of all skills, tools, agents
├── README.md                            # Main documentation
├── IMPLEMENTATION_GUIDE.md              # Step-by-step completion guide
├── ARCHITECTURE.md                      # (To create) Detailed architecture docs
│
├── skills/                              # 10 specialized skills (2/10 complete)
│   │
│   ├── gradle-doctor/                   # ✅ COMPLETE
│   │   ├── SKILL.md                    # Orchestrator skill, runs comprehensive analysis
│   │   └── examples/
│   │
│   ├── gradle-performance/              # ✅ COMPLETE
│   │   ├── SKILL.md                    # Performance tuning and optimization
│   │   └── examples/
│   │
│   ├── gradle-config-cache/             # ⏳ TODO
│   │   └── SKILL.md                    # Configuration cache troubleshooting
│   │
│   ├── gradle-build-cache/              # ⏳ TODO
│   │   └── SKILL.md                    # Build cache optimization
│   │
│   ├── gradle-task-development/         # ⏳ TODO
│   │   ├── SKILL.md                    # Custom task creation (/createTask, /reviewTask)
│   │   └── templates/
│   │
│   ├── gradle-plugin-development/       # ⏳ TODO
│   │   ├── SKILL.md                    # Plugin scaffolding (/createPlugin)
│   │   └── templates/
│   │
│   ├── gradle-dependencies/             # ⏳ TODO
│   │   └── SKILL.md                    # Dependency conflict resolution
│   │
│   ├── gradle-migration/                # ⏳ TODO
│   │   └── SKILL.md                    # Version migration guides (6→7→8→9)
│   │
│   ├── gradle-troubleshooting/          # ⏳ TODO
│   │   └── SKILL.md                    # Build failure detection and fixes
│   │
│   └── gradle-structure/                # ⏳ TODO
│       └── SKILL.md                    # Build organization, multi-project setup
│
├── tools/                               # JBang-based Gradle Tooling API tools (2/5 complete)
│   │
│   ├── gradle-analyzer.java            # ✅ COMPLETE - Project metadata extraction
│   │                                   # Usage: jbang gradle-analyzer.java <project> [--json]
│   │                                   # Output: Project structure, tasks, health indicators
│   │
│   ├── cache-validator.java            # ✅ COMPLETE - Cache configuration validation
│   │                                   # Usage: jbang cache-validator.java <project> [--fix]
│   │                                   # Output: Cache validation report with auto-fixes
│   │
│   ├── performance-profiler.java       # ⏳ TODO - Build performance analysis
│   │                                   # Will analyze: build times, bottlenecks, task execution
│   │
│   ├── task-analyzer.java              # ⏳ TODO - Task implementation analysis
│   │                                   # Will analyze: cacheability, incremental support
│   │
│   └── build-health-check.java         # ⏳ TODO - Quick health check
│                                       # Will provide: health score, critical issues
│
├── agents/                              # TypeScript subagent orchestrators (1/2 complete)
│   ├── package.json                    # ✅ Node dependencies
│   ├── tsconfig.json                   # ✅ TypeScript configuration
│   │
│   ├── src/
│   │   ├── doctor-agent.ts             # ✅ COMPLETE - Health check orchestrator
│   │   │                               # Subagents: performance, cache, deps, structure
│   │   │                               # Usage: Run via /doctor command
│   │   │
│   │   └── migration-agent.ts          # ⏳ TODO - Migration orchestrator
│   │                                   # Subagents: version-analyzer, breaker-detector,
│   │                                   #           auto-fixer, validation
│   │
│   └── dist/                           # Compiled JavaScript output (npm run build)
│
├── docs/                                # Reference documentation (3/32+ files complete)
│   └── reference/
│       │
│       ├── INDEX.md                    # ✅ Documentation index and usage guide
│       │
│       ├── performance-tuning.md       # ✅ COMPLETE - Performance optimization strategies
│       ├── configuration-cache.md      # ✅ COMPLETE - Config cache patterns and fixes
│       ├── tooling-api-basics.md       # ✅ COMPLETE - Gradle Tooling API reference
│       │
│       ├── parallel-execution.md       # ⏳ TODO - Parallel build configuration
│       ├── daemon-optimization.md      # ⏳ TODO - Gradle daemon tuning
│       ├── jvm-args.md                 # ⏳ TODO - JVM arguments
│       │
│       ├── build-cache.md              # ⏳ TODO - Build cache fundamentals
│       ├── cache-compatibility.md      # ⏳ TODO - Cache compatibility issues
│       ├── remote-cache-setup.md       # ⏳ TODO - Remote cache infrastructure
│       │
│       ├── task-basics.md              # ⏳ TODO - Task implementation fundamentals
│       ├── incremental-tasks.md        # ⏳ TODO - Incremental task patterns
│       ├── task-inputs-outputs.md      # ⏳ TODO - Input/output declaration
│       ├── task-avoidance.md           # ⏳ TODO - Task avoidance patterns
│       │
│       ├── plugin-basics.md            # ⏳ TODO - Plugin development fundamentals
│       ├── plugin-testing.md           # ⏳ TODO - Plugin testing strategies
│       ├── extension-objects.md        # ⏳ TODO - Extension design patterns
│       │
│       ├── multi-project.md            # ⏳ TODO - Multi-project build organization
│       ├── composite-builds.md         # ⏳ TODO - Composite build patterns
│       ├── buildSrc.md                 # ⏳ TODO - buildSrc and conventions
│       ├── settings-gradle.md          # ⏳ TODO - Settings file configuration
│       │
│       ├── dependency-management.md    # ⏳ TODO - Dependency fundamentals
│       ├── version-catalogs.md         # ⏳ TODO - Version catalog patterns
│       ├── conflict-resolution.md      # ⏳ TODO - Conflict resolution strategies
│       ├── dependency-constraints.md   # ⏳ TODO - Constraints and platforms
│       │
│       ├── common-errors.md            # ⏳ TODO - Common errors and solutions
│       ├── debugging-builds.md         # ⏳ TODO - Build debugging techniques
│       ├── scan-analysis.md            # ⏳ TODO - Build scan interpretation
│       ├── logging-configuration.md    # ⏳ TODO - Logging setup
│       │
│       ├── gradle-6-to-7.md            # ⏳ TODO - Gradle 6→7 migration
│       ├── gradle-7-to-8.md            # ⏳ TODO - Gradle 7→8 migration
│       ├── gradle-8-to-9.md            # ⏳ TODO - Gradle 8→9 migration (when available)
│       ├── breaking-changes.md         # ⏳ TODO - Breaking changes by version
│       ├── deprecated-features.md      # ⏳ TODO - Deprecated feature replacements
│       │
│       ├── kotlin-dsl-patterns.md      # ⏳ TODO - Kotlin DSL idioms
│       ├── groovy-dsl-patterns.md      # ⏳ TODO - Groovy DSL idioms
│       └── dsl-migration.md            # ⏳ TODO - Groovy→Kotlin migration
│
├── templates/                           # ⏳ TODO - Code generation templates
│   ├── plugin-scaffolding/
│   │   ├── build.gradle.kts.template
│   │   ├── Plugin.kt.template
│   │   ├── Extension.kt.template
│   │   └── Task.kt.template
│   │
│   └── task-templates/
│       ├── BasicTask.kt.template
│       ├── IncrementalTask.kt.template
│       └── WorkerApiTask.kt.template
│
└── examples/                            # ⏳ TODO - Usage examples
    ├── performance-optimization/
    ├── cache-troubleshooting/
    └── migration-guides/
```

## Component Status Summary

### ✅ Complete (Foundation) - 23% done

**Infrastructure**:
- plugin.json (main manifest)
- README.md (overview)
- IMPLEMENTATION_GUIDE.md (completion roadmap)
- Reference documentation index

**Skills** (2/10):
- gradle-doctor (orchestrator)
- gradle-performance (optimization)

**Tools** (2/5):
- gradle-analyzer.java (project analysis)
- cache-validator.java (cache validation)

**Agents** (1/2):
- doctor-agent.ts (health check orchestrator)

**Documentation** (3/32+):
- configuration-cache.md
- performance-tuning.md
- tooling-api-basics.md

### ⏳ Remaining Work - 77%

**Skills** (8 remaining):
- gradle-config-cache
- gradle-build-cache
- gradle-task-development
- gradle-plugin-development
- gradle-dependencies
- gradle-migration
- gradle-troubleshooting
- gradle-structure

**Tools** (3 remaining):
- performance-profiler.java
- task-analyzer.java
- build-health-check.java

**Agents** (1 remaining):
- migration-agent.ts

**Documentation** (29+ remaining):
- All task, plugin, dependency, migration, troubleshooting references
- DSL pattern guides
- Additional performance references

**Templates** (8+ files):
- Plugin scaffolding templates
- Task templates

**Examples** (3+ projects):
- Complete working examples

## Key Design Principles

### 1. Reference-Based Documentation
Skills reference small, focused docs rather than embedding large blocks. Keeps context window manageable.

**Pattern**:
```markdown
**Reference**: [../../docs/reference/topic.md](../../docs/reference/topic.md)
```

### 2. Tool-First Analysis
JBang tools provide programmatic analysis using Gradle Tooling API. Skills orchestrate tools.

**Pattern**:
```bash
jbang tools/gradle-analyzer.java . --json | process-in-skill
```

### 3. Subagent Orchestration
Complex workflows use subagents for specialized analysis, coordinated by main agents.

**Pattern**:
```typescript
performance-subagent → findings
cache-subagent → findings
dependency-subagent → findings
  ↓
doctor-agent synthesizes → recommendations
```

### 4. Layered Architecture
```
Skills (presentation) 
  → Agents (orchestration)
    → Tools (analysis) 
      → Gradle Tooling API
        → Reference Docs
```

## File Naming Conventions

- **Skills**: `skills/<skill-name>/SKILL.md` (uppercase SKILL.md)
- **Tools**: `tools/<tool-name>.java` (kebab-case, .java extension)
- **Agents**: `agents/src/<agent-name>.ts` (kebab-case, .ts extension)
- **Docs**: `docs/reference/<topic-name>.md` (kebab-case, .md extension)
- **Templates**: `templates/<type>/<Name>.template` (PascalCase with .template)

## Integration Points

### Skills ↔ Tools
Skills use JBang tools for analysis:
```
Skill mentions tool → User or Claude runs tool → Results inform skill
```

### Skills ↔ Agents
Skills trigger agents for complex workflows:
```
/doctor command → doctor-agent.ts orchestrates → Subagents analyze → Report
```

### Skills ↔ Docs
Skills reference docs for detailed guidance:
```
Skill provides overview → Links to reference doc → User gets details
```

### Tools ↔ Agents
Agents call tools programmatically:
```typescript
const analysis = await runTool('gradle-analyzer.java', [projectDir, '--json']);
```

## Testing Entry Points

### Test Skills
```bash
# Via Claude conversation
User: /doctor
User: My builds are slow, help optimize
```

### Test Tools
```bash
# Direct execution
jbang tools/gradle-analyzer.java test-project --json
jbang tools/cache-validator.java test-project --fix
```

### Test Agents
```bash
# Direct execution
cd agents
npm run build
ANTHROPIC_API_KEY=sk-... node dist/doctor-agent.js /path/to/project
```

### Test Documentation
```bash
# Verify all reference links resolve
./verify-references.sh
```

## Development Workflow

1. **Pick task** from IMPLEMENTATION_GUIDE.md
2. **Create files** following naming conventions
3. **Test locally** with sample projects
4. **Update this file** with completion status
5. **Commit** with descriptive message

## Quick Commands

```bash
# List all TODO items
grep -r "⏳ TODO" STRUCTURE_OVERVIEW.md

# Count complete vs remaining
grep "✅ COMPLETE" STRUCTURE_OVERVIEW.md | wc -l
grep "⏳ TODO" STRUCTURE_OVERVIEW.md | wc -l

# Test a tool
jbang tools/gradle-analyzer.java test-project

# Build agents
cd agents && npm run build

# Run agent
cd agents && ANTHROPIC_API_KEY=sk-... node dist/doctor-agent.js /path/to/project
```

## Next Steps

See `IMPLEMENTATION_GUIDE.md` for detailed implementation instructions.

**Priority Order**:
1. Complete reference documentation (foundation for everything else)
2. Complete remaining JBang tools (enable analysis capabilities)
3. Complete remaining skills (user-facing interface)
4. Complete subagents (complex workflows)
5. Add templates and examples (polish and usability)
