#!/bin/bash
# =============================================================================
# Setup Test Fixtures
# =============================================================================
# Prepares test fixtures for running tool tests.
# Run from the gradle-expert root directory.
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FIXTURES_DIR="$SCRIPT_DIR/../projects"

echo "=== Setting Up Test Fixtures ==="
echo ""

# Verify fixtures exist
for project in simple-java config-cache-broken legacy-groovy multi-module spring-boot; do
    if [[ ! -d "$FIXTURES_DIR/$project" ]]; then
        echo "ERROR: Missing fixture: $project"
        exit 1
    fi
    echo "✓ Found: $project"
done

echo ""
echo "=== Verifying Fixture Structure ==="

# Check each fixture has required files
check_fixture() {
    local project=$1
    local required_files=("${@:2}")
    
    echo "Checking $project..."
    for file in "${required_files[@]}"; do
        if [[ ! -f "$FIXTURES_DIR/$project/$file" ]]; then
            echo "  ✗ Missing: $file"
            return 1
        fi
        echo "  ✓ $file"
    done
}

check_fixture "simple-java" \
    "settings.gradle.kts" \
    "build.gradle.kts" \
    "gradle.properties" \
    "gradle/libs.versions.toml" \
    "gradle/wrapper/gradle-wrapper.properties"

check_fixture "config-cache-broken" \
    "settings.gradle.kts" \
    "build.gradle.kts" \
    "gradle.properties" \
    "gradle/wrapper/gradle-wrapper.properties"

check_fixture "legacy-groovy" \
    "settings.gradle" \
    "build.gradle" \
    "gradle.properties" \
    "gradle/wrapper/gradle-wrapper.properties"

check_fixture "multi-module" \
    "settings.gradle.kts" \
    "build.gradle.kts" \
    "gradle.properties" \
    "gradle/libs.versions.toml" \
    "app/build.gradle.kts" \
    "core/build.gradle.kts"

check_fixture "spring-boot" \
    "settings.gradle.kts" \
    "build.gradle.kts" \
    "gradle.properties"

echo ""
echo "=== All Fixtures Ready ==="
echo ""
echo "Available fixtures:"
echo "  - simple-java        (Healthy baseline, Gradle 8.11)"
echo "  - config-cache-broken (Config cache issues, Gradle 8.5)"
echo "  - legacy-groovy      (Migration testing, Gradle 7.6)"
echo "  - multi-module       (Multi-project, Gradle 8.11)"
echo "  - spring-boot        (Spring Boot app, Gradle 8.5)"
echo ""
echo "Run tests with: ./scripts/test-tools.sh"
