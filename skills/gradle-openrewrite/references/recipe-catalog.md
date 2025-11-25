# OpenRewrite Recipe Catalog for Gradle

## Migration Recipes

### Gradle Version Migrations

| Recipe | Description | Source → Target |
|--------|-------------|-----------------|
| `org.openrewrite.gradle.MigrateToGradle8` | Migrate deprecated APIs | Gradle 7.x → 8.x |
| `org.openrewrite.gradle.MigrateToGradle7` | Migrate deprecated APIs | Gradle 6.x → 7.x |
| `org.openrewrite.gradle.UpdateGradleWrapper` | Update wrapper version | Any → Specified |

### DSL Migrations

| Recipe | Description |
|--------|-------------|
| `org.openrewrite.kotlin.gradle.MigrateToKotlinDsl` | Groovy → Kotlin DSL |
| `org.openrewrite.gradle.plugins.MigrateToPluginsBlock` | `apply plugin` → `plugins {}` |

### Dependency Management

| Recipe | Description |
|--------|-------------|
| `org.openrewrite.gradle.MigrateToVersionCatalog` | Inline deps → Version catalog |
| `org.openrewrite.gradle.ChangeDependencyGroupId` | Change dependency group |
| `org.openrewrite.gradle.ChangeDependencyArtifactId` | Change dependency artifact |
| `org.openrewrite.gradle.ChangeDependencyVersion` | Change dependency version |
| `org.openrewrite.gradle.UpgradeDependencyVersion` | Upgrade to newer version |
| `org.openrewrite.gradle.RemoveDependency` | Remove a dependency |

## Best Practice Recipes

### Build Script Improvements

| Recipe | Description |
|--------|-------------|
| `org.openrewrite.gradle.RemoveRedundantDependencyVersions` | Remove versions managed by platforms |
| `org.openrewrite.gradle.DependencyUseStringNotation` | Map notation → String notation |
| `org.openrewrite.gradle.DependencyUseMapNotation` | String notation → Map notation |

### Plugin Management

| Recipe | Description |
|--------|-------------|
| `org.openrewrite.gradle.plugins.AddPluginsBuildScriptBlock` | Add plugins block |
| `org.openrewrite.gradle.plugins.ChangePlugin` | Change plugin ID |
| `org.openrewrite.gradle.plugins.RemovePlugin` | Remove a plugin |
| `org.openrewrite.gradle.plugins.UpgradePluginVersion` | Upgrade plugin version |

## Search Recipes (Analysis Only)

These recipes find patterns without modifying code:

| Recipe | Description |
|--------|-------------|
| `org.openrewrite.gradle.search.FindGradleProject` | Find Gradle projects |
| `org.openrewrite.gradle.search.FindPlugins` | Find plugin applications |
| `org.openrewrite.gradle.search.FindDependency` | Find dependency declarations |
| `org.openrewrite.gradle.search.FindRepository` | Find repository declarations |

## Recipe Options

Many recipes accept configuration options:

### UpdateGradleWrapper

```yaml
recipe: org.openrewrite.gradle.UpdateGradleWrapper
options:
  version: "8.14"
  distribution: "bin"  # or "all"
```

### ChangeDependencyVersion

```yaml
recipe: org.openrewrite.gradle.ChangeDependencyVersion
options:
  groupId: "org.junit.jupiter"
  artifactId: "junit-jupiter"
  newVersion: "5.10.0"
```

### MigrateToVersionCatalog

```yaml
recipe: org.openrewrite.gradle.MigrateToVersionCatalog
options:
  catalogName: "libs"  # defaults to "libs"
```

## Composite Recipes

These recipes combine multiple transformations:

### MigrateToGradle8

Includes:
- Update deprecated task APIs
- Update deprecated property APIs
- Update deprecated dependency configurations
- Update build directory references

### MigrateToKotlinDsl

Includes:
- Convert build.gradle → build.gradle.kts
- Convert settings.gradle → settings.gradle.kts
- Update syntax (quotes, parentheses, etc.)
- Convert plugin application syntax

## Recipe Dependencies

Some recipes require additional dependencies:

```yaml
# .claude/gradle-plugin.local.md
---
openrewrite:
  additionalRecipes:
    # For Kotlin DSL migration
    - "org.openrewrite.recipe:rewrite-kotlin:LATEST"

    # For Spring-related recipes
    - "org.openrewrite.recipe:rewrite-spring:LATEST"

    # For testing framework migrations
    - "org.openrewrite.recipe:rewrite-testing-frameworks:LATEST"
---
```

## Running Recipes

### Single Recipe

```bash
/openrewrite run org.openrewrite.gradle.UpdateGradleWrapper
```

### With Options

```bash
/openrewrite run org.openrewrite.gradle.UpdateGradleWrapper --version=8.14
```

### Multiple Recipes

```bash
/openrewrite run org.openrewrite.gradle.MigrateToGradle8,org.openrewrite.gradle.UpdateGradleWrapper
```

### Dry Run

```bash
/openrewrite dry-run org.openrewrite.gradle.MigrateToGradle8
```

## Recipe Discovery

Find recipes matching your needs:

```bash
# List all Gradle recipes
/openrewrite list gradle

# Search for specific functionality
/openrewrite list dependency

# Get suggestions based on project analysis
/openrewrite suggest
```

## External Resources

- [Full Recipe Catalog](https://docs.openrewrite.org/recipes)
- [Gradle Recipes](https://docs.openrewrite.org/recipes/gradle)
- [Writing Custom Recipes](https://docs.openrewrite.org/authoring-recipes)
