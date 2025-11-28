#!/bin/bash
# =============================================================================
# Test Tools Against Fixtures
# =============================================================================
# Runs all JBang tools against test fixtures and validates outputs.
# Requires: jbang, jq
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR/../../.."
FIXTURES_DIR="$SCRIPT_DIR/../projects"
EXPECTED_DIR="$SCRIPT_DIR/../expected-outputs"
TOOLS_DIR="$ROOT_DIR/tools"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PASS=0
FAIL=0

# =============================================================================
# Helper Functions
# =============================================================================

log_pass() {
    echo -e "${GREEN}✓ PASS${NC}: $1"
    ((++PASS))
}

log_fail() {
    echo -e "${RED}✗ FAIL${NC}: $1"
    ((++FAIL))
}

log_test() {
    echo -e "${YELLOW}TEST${NC}: $1"
}

# Check prerequisites
check_prereqs() {
    if ! command -v jbang &> /dev/null; then
        echo "ERROR: jbang not found. Install from https://jbang.dev"
        exit 1
    fi
    
    if ! command -v jq &> /dev/null; then
        echo "ERROR: jq not found. Install with: brew install jq"
        exit 1
    fi
}

# =============================================================================
# Test: gradle-analyzer.java
# =============================================================================

test_gradle_analyzer() {
    local project=$1
    local expected_file="$EXPECTED_DIR/$project.json"
    
    log_test "gradle-analyzer.java on $project"
    
    local output=$(jbang "$TOOLS_DIR/gradle-analyzer.java" "$FIXTURES_DIR/$project" --json 2>/dev/null)
    
    # Validate JSON output
    if ! echo "$output" | jq . > /dev/null 2>&1; then
        log_fail "Invalid JSON output for $project"
        return
    fi
    
    # Check Gradle version detection
    local detected_version=$(echo "$output" | jq -r '.gradleVersion // empty')
    local expected_version=$(jq -r '.gradleVersion' "$expected_file")
    
    if [[ "$detected_version" == "$expected_version" ]]; then
        log_pass "Gradle version detection: $detected_version"
    else
        log_fail "Gradle version: expected $expected_version, got $detected_version"
    fi
    
    # Check wrapper detection
    local has_wrapper=$(echo "$output" | jq -r '.healthIndicators.hasWrapper // false')
    local expected_wrapper=$(jq -r '.hasWrapper' "$expected_file")
    
    if [[ "$has_wrapper" == "$expected_wrapper" ]]; then
        log_pass "Wrapper detection"
    else
        log_fail "Wrapper detection: expected $expected_wrapper, got $has_wrapper"
    fi
}

# =============================================================================
# Test: build-health-check.java
# =============================================================================

test_health_check() {
    local project=$1
    local expected_file="$EXPECTED_DIR/$project.json"
    
    log_test "build-health-check.java on $project"
    
    local output=$(jbang "$TOOLS_DIR/build-health-check.java" "$FIXTURES_DIR/$project" --json 2>/dev/null)
    
    # Validate JSON output
    if ! echo "$output" | jq . > /dev/null 2>&1; then
        log_fail "Invalid JSON output for $project"
        return
    fi
    
    # Check score within expected range
    local score=$(echo "$output" | jq -r '.overallScore // 0')
    local min_score=$(jq -r '.healthCheck.expectedScoreMin' "$expected_file")
    local max_score=$(jq -r '.healthCheck.expectedScoreMax' "$expected_file")
    
    if [[ $score -ge $min_score && $score -le $max_score ]]; then
        log_pass "Health score $score in range [$min_score, $max_score]"
    else
        log_fail "Health score $score not in range [$min_score, $max_score]"
    fi
    
    # Check status
    local status=$(echo "$output" | jq -r '.status // empty')
    local expected_status=$(jq -r '.healthCheck.expectedStatus' "$expected_file")
    
    if [[ "$status" == "$expected_status" ]]; then
        log_pass "Status: $status"
    else
        log_fail "Status: expected $expected_status, got $status"
    fi
}

# =============================================================================
# Test: task-analyzer.java
# =============================================================================

test_task_analyzer() {
    local project=$1
    local expected_file="$EXPECTED_DIR/$project.json"

    log_test "task-analyzer.java on $project"

    local output=$(jbang "$TOOLS_DIR/task-analyzer.java" "$FIXTURES_DIR/$project" --json 2>/dev/null)

    # Validate JSON output
    if ! echo "$output" | jq . > /dev/null 2>&1; then
        log_fail "Invalid JSON output for $project"
        return
    fi

    # CRITICAL: Check that build files were actually analyzed (catches .gradle filter bug)
    local total_files=$(echo "$output" | jq -r '.totalFiles // 0')
    if [[ "$total_files" -eq 0 ]]; then
        log_fail "No build files analyzed - likely a filter bug"
        return
    fi
    log_pass "Analyzed $total_files build file(s)"

    # Check eager creates count (correct JSON path: .eagerTaskCreations, not .patterns.eagerCreates)
    local eager_creates=$(echo "$output" | jq -r '.eagerTaskCreations // 0')
    local expected_eager=$(jq -r '.taskAnalysis.expectedEagerCreates // 0' "$expected_file")

    if [[ "$eager_creates" == "$expected_eager" ]]; then
        log_pass "Eager creates: $eager_creates"
    else
        log_fail "Eager creates: expected $expected_eager, got $eager_creates"
    fi

    # Check lazy registrations (tasks.register)
    local lazy_registrations=$(echo "$output" | jq -r '.lazyTaskRegistrations // 0')
    local expected_lazy=$(jq -r '.taskAnalysis.expectedLazyRegisters // 0' "$expected_file")

    if [[ "$lazy_registrations" -ge "$expected_lazy" ]]; then
        log_pass "Lazy registrations: $lazy_registrations (expected >= $expected_lazy)"
    else
        log_fail "Lazy registrations: expected >= $expected_lazy, got $lazy_registrations"
    fi

    # Check config cache issues (if applicable)
    local cc_issues=$(echo "$output" | jq -r '.configCacheIssues // 0')
    local expected_cc=$(jq -r '.taskAnalysis.expectedConfigCacheIssues // 0' "$expected_file")

    # Handle range expectations
    if [[ $(jq -r '.taskAnalysis.expectedConfigCacheIssues | type' "$expected_file") == "object" ]]; then
        local min=$(jq -r '.taskAnalysis.expectedConfigCacheIssues.min' "$expected_file")
        local max=$(jq -r '.taskAnalysis.expectedConfigCacheIssues.max' "$expected_file")
        if [[ $cc_issues -ge $min && $cc_issues -le $max ]]; then
            log_pass "Config cache issues $cc_issues in range [$min, $max]"
        else
            log_fail "Config cache issues $cc_issues not in range [$min, $max]"
        fi
    else
        if [[ "$cc_issues" == "$expected_cc" ]]; then
            log_pass "Config cache issues: $cc_issues"
        else
            log_fail "Config cache issues: expected $expected_cc, got $cc_issues"
        fi
    fi
}

# =============================================================================
# Test: cache-validator.java
# =============================================================================

test_cache_validator() {
    local project=$1
    
    log_test "cache-validator.java on $project"
    
    local output=$(jbang "$TOOLS_DIR/cache-validator.java" "$FIXTURES_DIR/$project" 2>/dev/null)
    
    # Basic output validation
    if [[ -n "$output" ]]; then
        log_pass "Cache validator produced output"
    else
        log_fail "Cache validator produced no output"
    fi
}

# =============================================================================
# Test: performance-profiler.java
# =============================================================================

test_performance_profiler() {
    local project=$1
    
    log_test "performance-profiler.java on $project (dry-run)"
    
    # Performance profiler needs actual build, so just test it runs
    local exit_code=0
    jbang "$TOOLS_DIR/performance-profiler.java" "$FIXTURES_DIR/$project" help 2>/dev/null || exit_code=$?
    
    if [[ $exit_code -eq 0 ]]; then
        log_pass "Performance profiler executable"
    else
        log_fail "Performance profiler failed to run"
    fi
}

# =============================================================================
# Main Test Execution
# =============================================================================

main() {
    echo "=============================================="
    echo "     Gradle Expert Framework - Tool Tests"
    echo "=============================================="
    echo ""
    
    check_prereqs
    
    # Test each fixture
    for project in simple-java config-cache-broken legacy-groovy multi-module spring-boot; do
        echo ""
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo " Testing: $project"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        
        test_gradle_analyzer "$project"
        test_health_check "$project"
        test_task_analyzer "$project"
        test_cache_validator "$project"
        test_performance_profiler "$project"
    done
    
    # Summary
    echo ""
    echo "=============================================="
    echo "                  SUMMARY"
    echo "=============================================="
    echo -e "  ${GREEN}Passed${NC}: $PASS"
    echo -e "  ${RED}Failed${NC}: $FAIL"
    echo ""
    
    if [[ $FAIL -gt 0 ]]; then
        echo -e "${RED}Some tests failed!${NC}"
        exit 1
    else
        echo -e "${GREEN}All tests passed!${NC}"
        exit 0
    fi
}

main "$@"
