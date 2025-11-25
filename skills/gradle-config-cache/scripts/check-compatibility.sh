#!/usr/bin/env bash

# Configuration Cache Compatibility Checker
# This script helps analyze a Gradle build for Configuration Cache compatibility

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="${1:-.}"
TASK="${2:-help}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=====================================${NC}"
echo -e "${BLUE}Configuration Cache Compatibility Check${NC}"
echo -e "${BLUE}=====================================${NC}"
echo ""

# Change to build directory
cd "$BUILD_DIR"

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    echo -e "${RED}Error: gradlew not found in $BUILD_DIR${NC}"
    exit 1
fi

echo -e "${YELLOW}Step 1: Testing with help task (minimal configuration)${NC}"
./gradlew --configuration-cache help 2>&1 | tee /tmp/cc-help-output.txt

if grep -q "Configuration cache entry stored" /tmp/cc-help-output.txt; then
    echo -e "${GREEN}✓ Help task succeeded - basic compatibility OK${NC}"
else
    echo -e "${RED}✗ Help task failed - fundamental issues detected${NC}"
fi

echo ""
echo -e "${YELLOW}Step 2: Testing with target task (full build logic)${NC}"
./gradlew --configuration-cache "$TASK" 2>&1 | tee /tmp/cc-task-output.txt

if grep -q "Configuration cache entry stored" /tmp/cc-task-output.txt; then
    echo -e "${GREEN}✓ Task '$TASK' succeeded${NC}"
else
    echo -e "${RED}✗ Task '$TASK' failed${NC}"
fi

echo ""
echo -e "${YELLOW}Step 3: Testing cache reuse${NC}"
./gradlew --configuration-cache "$TASK" 2>&1 | tee /tmp/cc-reuse-output.txt

if grep -q "Reusing configuration cache" /tmp/cc-reuse-output.txt; then
    echo -e "${GREEN}✓ Configuration cache reused successfully${NC}"
elif grep -q "Configuration cache cannot be reused" /tmp/cc-reuse-output.txt; then
    echo -e "${YELLOW}⚠ Cache invalidated - check for configuration inputs${NC}"
    grep "cannot be reused" /tmp/cc-reuse-output.txt || true
else
    echo -e "${RED}✗ Cache reuse failed${NC}"
fi

echo ""
echo -e "${YELLOW}Step 4: Running with warning mode to discover all issues${NC}"
./gradlew --configuration-cache --configuration-cache-problems=warn "$TASK" 2>&1 | tee /tmp/cc-warn-output.txt

# Extract problem count
PROBLEM_COUNT=$(grep -o '[0-9]\+ problem' /tmp/cc-warn-output.txt | head -1 | grep -o '[0-9]\+' || echo "0")

if [ "$PROBLEM_COUNT" -eq 0 ]; then
    echo -e "${GREEN}✓ No configuration cache problems detected${NC}"
else
    echo -e "${YELLOW}⚠ Found $PROBLEM_COUNT configuration cache problem(s)${NC}"
    
    # Find and display HTML report location
    REPORT_PATH=$(grep -o 'file://[^ ]*configuration-cache-report.html' /tmp/cc-warn-output.txt | head -1 | sed 's/file:\/\///')
    
    if [ -n "$REPORT_PATH" ] && [ -f "$REPORT_PATH" ]; then
        echo -e "${BLUE}Configuration cache report available at:${NC}"
        echo -e "${BLUE}file://$REPORT_PATH${NC}"
    fi
fi

echo ""
echo -e "${BLUE}=====================================${NC}"
echo -e "${BLUE}Summary${NC}"
echo -e "${BLUE}=====================================${NC}"

# Generate summary
if [ "$PROBLEM_COUNT" -eq 0 ]; then
    if grep -q "Reusing configuration cache" /tmp/cc-reuse-output.txt; then
        echo -e "${GREEN}✓ Build is fully Configuration Cache compatible${NC}"
        echo -e "${GREEN}✓ Cache is being stored and reused correctly${NC}"
    else
        echo -e "${YELLOW}⚠ Build is compatible but cache may not be reusable${NC}"
        echo -e "${YELLOW}  Check for configuration inputs that change between runs${NC}"
    fi
else
    echo -e "${YELLOW}⚠ Build has $PROBLEM_COUNT configuration cache problem(s)${NC}"
    echo -e "${YELLOW}  Review the HTML report for details${NC}"
    echo ""
    echo -e "${BLUE}Common next steps:${NC}"
    echo "  1. Open the HTML report to see detailed problem descriptions"
    echo "  2. Review common-problems.md for solutions"
    echo "  3. Fix problems iteratively, starting with storing issues"
    echo "  4. Use api-replacements.md for Project API replacements"
fi

echo ""
echo -e "${BLUE}Configuration Cache Settings:${NC}"
if [ -f "gradle.properties" ]; then
    if grep -q "org.gradle.configuration-cache=true" gradle.properties; then
        echo -e "${GREEN}✓ Configuration cache enabled in gradle.properties${NC}"
    else
        echo -e "${YELLOW}⚠ Configuration cache not enabled by default${NC}"
        echo "  Add 'org.gradle.configuration-cache=true' to gradle.properties"
    fi
else
    echo -e "${YELLOW}⚠ No gradle.properties file found${NC}"
fi

echo ""
echo -e "${BLUE}For more information:${NC}"
echo "  • Gradle docs: https://docs.gradle.org/current/userguide/configuration_cache.html"
echo "  • View common-problems.md for detailed solutions"
echo "  • View api-replacements.md for Project API alternatives"
