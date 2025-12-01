#!/bin/bash
# SessionStart hook for Gradle project detection
# Phase 6 of Active Automation
#
# This hook runs when Claude Code session starts in a Gradle project.
# It performs a quick health check and suggests relevant commands.
#
# To disable this hook, create .claude/gradle-plugin.local.md with:
#   hooks:
#     sessionStart: false

set -euo pipefail

# Source settings library
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/settings.sh"

# Check if this hook is enabled
if ! is_hook_enabled "sessionStart"; then
    exit 0
fi

# Check if this is a Gradle project
is_gradle_project() {
    local dir="${CLAUDE_PROJECT_DIR:-.}"
    [[ -f "$dir/build.gradle" ]] || \
    [[ -f "$dir/build.gradle.kts" ]] || \
    [[ -f "$dir/settings.gradle" ]] || \
    [[ -f "$dir/settings.gradle.kts" ]]
}

# Quick check for common issues (fast, no Gradle invocation)
quick_health_check() {
    local dir="${CLAUDE_PROJECT_DIR:-.}"
    local issues=()

    # Check gradle.properties for performance settings
    local props_file="$dir/gradle.properties"
    if [[ -f "$props_file" ]]; then
        if ! grep -q "org.gradle.parallel=true" "$props_file" 2>/dev/null; then
            issues+=("parallel execution disabled")
        fi
        if ! grep -q "org.gradle.caching=true" "$props_file" 2>/dev/null; then
            issues+=("build cache disabled")
        fi
    else
        issues+=("no gradle.properties (missing performance settings)")
    fi

    # Check for deprecated patterns in build files
    # Note: Use -E for extended regex (ERE) to support \s and \w, or use POSIX classes
    for build_file in "$dir/build.gradle" "$dir/build.gradle.kts"; do
        if [[ -f "$build_file" ]]; then
            # Match: task taskName( or task taskName {  (Groovy eager task syntax)
            if grep -Eq "task[[:space:]]+[[:alnum:]_]+[[:space:]]*\(" "$build_file" 2>/dev/null || \
               grep -Eq "task[[:space:]]+[[:alnum:]_]+[[:space:]]*\{" "$build_file" 2>/dev/null; then
                issues+=("eager task creation detected")
                break
            fi
        fi
    done

    # Check Gradle version
    local wrapper_props="$dir/gradle/wrapper/gradle-wrapper.properties"
    if [[ -f "$wrapper_props" ]]; then
        local version
        version=$(grep "distributionUrl" "$wrapper_props" | grep -oE "[0-9]+\.[0-9]+(\.[0-9]+)?" | head -1)
        if [[ -n "$version" ]]; then
            local major="${version%%.*}"
            if [[ "$major" -lt 8 ]]; then
                issues+=("Gradle $version (consider upgrading to 8.x+)")
            fi
        fi
    fi

    echo "${issues[@]:-}"
}

# Main execution
main() {
    # Only run for Gradle projects
    if ! is_gradle_project; then
        exit 0
    fi

    # Run quick health check
    local issues
    issues=$(quick_health_check)

    # Build system message
    local message=""

    if [[ -n "$issues" ]]; then
        message="Gradle project detected. Quick scan found potential improvements: $issues. Run /doctor for full analysis or /optimize-performance to auto-fix performance settings."
    else
        message="Gradle project detected. Build configuration looks good. Run /gradle:doctor for comprehensive health check."
    fi

    # Output JSON response
    cat <<EOF
{
  "continue": true,
  "suppressOutput": false,
  "systemMessage": "$message"
}
EOF
}

main "$@"
