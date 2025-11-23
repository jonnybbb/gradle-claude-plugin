---
name: gradle-project-analysis
description: Analyzes Gradle project structure, dependencies, build configuration, and module relationships. Claude uses this automatically when you ask about project structure, module dependencies, build file analysis, or gradle configuration review.
---

# Gradle Project Analysis Skill

This skill enables Claude to autonomously analyze your Gradle projects by inspecting build configurations, mapping dependencies, and understanding project topology.

## When Claude Uses This Skill

Claude will automatically invoke this skill when you:
- Ask to "analyze the project structure"
- Request a "dependency graph" or "module relationship diagram"
- Want to "understand the gradle configuration"
- Ask for "project topology" or "multi-module structure"
- Request "build file analysis" or "configuration review"
- Inquire about "what gradle plugins are used"
- Ask "how is this project organized"

## Capabilities

### 1. Project Structure Detection

**Single-Module Projects:**
- Build file analysis (build.gradle / build.gradle.kts)
- Plugin application patterns
- Dependency configurations
- Task definitions
- Custom configurations

**Multi-Module Projects:**
- Root project configuration (settings.gradle / settings.gradle.kts)
- Submodule discovery and hierarchy
- Module dependency relationships
- Shared configuration patterns
- buildSrc/ convention plugins
- Composite builds and included builds

### 2. Build File Analysis

**Groovy DSL (build.gradle):**
```groovy
plugins {
    id 'java-library'
    id 'maven-publish'
}

dependencies {
    api 'com.google.guava:guava:32.1.3-jre'
    implementation 'org.slf4j:slf4j-api:2.0.9'
}
```

**Kotlin DSL (build.gradle.kts):**
```kotlin
plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api("com.google.guava:guava:32.1.3-jre")
    implementation("org.slf4j:slf4j-api:2.0.9")
}
```

### 3. Configuration Analysis

**Detected Patterns:**
- Version catalogs (libs.versions.toml)
- Gradle properties (gradle.properties)
- Settings plugins
- Convention plugins (buildSrc/)
- Precompiled script plugins
- Plugin management configuration
- Repository declarations
- Dependency constraints
- Custom configurations
- Task configuration patterns

### 4. Dependency Mapping

**Analysis Scope:**
- Direct dependencies (api, implementation, compileOnly, runtimeOnly)
- Transitive dependencies
- Test dependencies (testImplementation, testRuntimeOnly)
- Dependency constraints
- Platform dependencies
- BOM imports
- Version catalog usage
- Inter-module dependencies in multi-module projects

### 5. Plugin Discovery

**Identified Plugins:**
- Core Gradle plugins (java, java-library, application, etc.)
- Third-party plugins (Spring Boot, Shadow, etc.)
- Custom plugins (buildSrc/ or published)
- Plugin versions and sources
- Plugin application order
- Plugin configurations

## Implementation Approach

### Step 1: Scan Project Files

```bash
# Discover project structure
find . -name "build.gradle" -o -name "build.gradle.kts" -o -name "settings.gradle" -o -name "settings.gradle.kts"

# Check for version catalog
ls -la gradle/libs.versions.toml 2>/dev/null

# Check for buildSrc
ls -la buildSrc/ 2>/dev/null
```

### Step 2: Parse Build Files

The skill reads and parses:
- `settings.gradle[.kts]` - Project structure and module inclusion
- `build.gradle[.kts]` - Build configuration for each module
- `gradle.properties` - Build properties and configuration
- `gradle/libs.versions.toml` - Version catalog (if present)
- `buildSrc/` - Convention plugins and build logic

### Step 3: Extract Configuration

**From build.gradle.kts:**
```kotlin
// Extracted information:
// - Applied plugins: java-library, maven-publish
// - Dependencies: guava (api), slf4j-api (implementation)
// - Repositories: mavenCentral()
// - Custom tasks: Any defined tasks
// - Java toolchain: languageVersion
```

### Step 4: Generate Analysis Report

**Output Format:**
```
=== Gradle Project Analysis ===

Project: example-project
Gradle Version: 8.5
Build Files: Kotlin DSL

--- Project Structure ---
Root: example-project
├── :app (application)
├── :lib-core (java-library)
└── :lib-utils (java-library)

--- Applied Plugins ---
Root:
  - org.jetbrains.kotlin.jvm (1.9.20)

:app:
  - application
  - org.springframework.boot (3.1.5)

:lib-core:
  - java-library

--- Dependencies ---
:app -> :lib-core (implementation)
:lib-core -> :lib-utils (api)

External Dependencies:
  - com.google.guava:guava:32.1.3-jre
  - org.springframework.boot:spring-boot-starter-web:3.1.5

--- Configuration Patterns ---
✓ Version catalog detected (gradle/libs.versions.toml)
✓ Convention plugins in buildSrc/
✓ Consistent Kotlin DSL usage
⚠ No dependency locking enabled

--- Recommendations ---
1. Consider enabling dependency locking for reproducible builds
2. Review transitive dependencies for version conflicts
3. Evaluate build cache and configuration cache enablement
```

## Analysis Scripts

The skill includes helper scripts in `scripts/`:

**analyze-structure.sh:**
```bash
#!/bin/bash
# Analyzes Gradle project structure

PROJECT_DIR="${1:-.}"
cd "$PROJECT_DIR" || exit 1

echo "=== Project Structure Analysis ==="
echo ""

# Check for multi-module setup
if [[ -f "settings.gradle" || -f "settings.gradle.kts" ]]; then
    echo "Multi-module project detected"
    gradle -q projects
else
    echo "Single-module project"
fi

# List build files
echo ""
echo "Build files:"
find . -name "build.gradle" -o -name "build.gradle.kts" | sort

# Check for version catalog
echo ""
if [[ -f "gradle/libs.versions.toml" ]]; then
    echo "✓ Version catalog present"
else
    echo "⚠ No version catalog"
fi

# Check for buildSrc
if [[ -d "buildSrc" ]]; then
    echo "✓ buildSrc/ detected (convention plugins)"
fi
```

**generate-topology.sh:**
```bash
#!/bin/bash
# Generates project topology diagram

PROJECT_DIR="${1:-.}"
cd "$PROJECT_DIR" || exit 1

echo "=== Project Topology ==="
gradle -q projects --console=plain | grep -v "^Root project" | sed 's/^/  /'
```

## Use Cases

### Use Case 1: Understanding a New Project

**User:** "I just cloned this repository. Can you help me understand how it's structured?"

**Skill Action:**
1. Scan for settings.gradle[.kts] to identify multi-module structure
2. List all modules and their types (library, application, etc.)
3. Identify key plugins and frameworks (Spring Boot, Android, etc.)
4. Map inter-module dependencies
5. Highlight configuration patterns (version catalogs, buildSrc, etc.)

### Use Case 2: Dependency Audit

**User:** "What external dependencies does this project use?"

**Skill Action:**
1. Parse all build files to extract dependency declarations
2. Categorize by scope (api, implementation, test, etc.)
3. Identify version management approach (catalogs, BOM, hardcoded)
4. Check for transitive dependencies
5. Highlight potential issues (version conflicts, deprecated libraries)

### Use Case 3: Build Configuration Review

**User:** "Is our Gradle configuration following best practices?"

**Skill Action:**
1. Analyze build file organization
2. Check for version catalog usage
3. Verify plugin application patterns
4. Review repository declarations
5. Evaluate configuration cache compatibility
6. Suggest improvements based on Gradle 8+ best practices

## Integration with Other Skills

This skill provides foundational analysis that other skills build upon:

- **gradle-performance-tuning**: Uses project structure to recommend optimizations
- **gradle-dependency-resolver**: Leverages dependency mapping to resolve conflicts
- **gradle-cache-optimization**: Analyzes task configuration for cache optimization
- **gradle-migration-assistant**: Examines current configuration for migration planning

## Output Formats

### Text Report (Default)
Human-readable analysis with hierarchical structure and recommendations.

### JSON Export
```json
{
  "project": {
    "name": "example-project",
    "gradleVersion": "8.5",
    "dslType": "kotlin"
  },
  "modules": [
    {
      "name": ":app",
      "type": "application",
      "plugins": ["application", "org.springframework.boot"],
      "dependencies": {
        "implementation": [":lib-core"],
        "external": ["org.springframework.boot:spring-boot-starter-web:3.1.5"]
      }
    }
  ],
  "recommendations": [
    "Enable dependency locking",
    "Consider build cache"
  ]
}
```

### Markdown Report
Formatted markdown suitable for documentation or README files.

## Limitations

- Cannot analyze encrypted or obfuscated build files
- Requires read access to build files
- May not detect dynamic plugin application
- Script-based plugin logic may require manual review

## Best Practices

1. Run analysis from project root directory
2. Ensure gradle wrapper is present for accurate version detection
3. Review analysis output for multi-module projects carefully
4. Cross-reference with actual `gradle tasks` output
5. Use in combination with `/doctor` command for comprehensive review
