#!/bin/bash
# PostToolUse hook for build file validation
# Phase 6 of Active Automation
#
# This hook runs after Edit/Write operations on Gradle build files.
# It performs quick validation and warns about introduced issues.
#
# To disable this hook, create .claude/gradle-plugin.local.md with:
#   hooks:
#     postToolUse: false

set -euo pipefail

# Source settings library
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/settings.sh"

# Check if this hook is enabled
if ! is_hook_enabled "postToolUse"; then
    echo '{"continue": true, "suppressOutput": true}'
    exit 0
fi

# Read hook input from stdin
input=$(cat)

# Extract file path from tool input
file_path=$(echo "$input" | jq -r '.tool_input.file_path // .tool_input.path // ""')

# Check if this is a Gradle build file
is_build_file() {
    local path="$1"
    [[ "$path" == *"build.gradle"* ]] || \
    [[ "$path" == *"build.gradle.kts"* ]] || \
    [[ "$path" == *"settings.gradle"* ]] || \
    [[ "$path" == *"settings.gradle.kts"* ]] || \
    [[ "$path" == *"gradle.properties"* ]] || \
    [[ "$path" == *"libs.versions.toml"* ]]
}

# Quick validation of build file content
validate_build_file() {
    local path="$1"
    local warnings=()

    # Skip if file doesn't exist
    [[ -f "$path" ]] || return 0

    local content
    content=$(cat "$path")

    # Check for configuration cache issues (only for Gradle script files, not .gradle/ cache directory)
    local filename
    filename=$(basename "$path")
    if [[ "$filename" == *.gradle ]] || [[ "$filename" == *.gradle.kts ]]; then
        # Note: Use POSIX [[:space:]] and [[:alnum:]] instead of \s and \w for portability
        # BSD/macOS grep -E doesn't support \s and \w (those are PCRE features)

        # System.getProperty at configuration time
        if echo "$content" | grep -qE "System\.(getProperty|getenv)[[:space:]]*\("; then
            warnings+=("System.getProperty/getenv at configuration time (config cache incompatible)")
        fi

        # Eager task creation
        if echo "$content" | grep -qE "tasks\.create[[:space:]]*\(|task[[:space:]]+[[:alnum:]_]+[[:space:]]*\(|task[[:space:]]+[[:alnum:]_]+[[:space:]]*\{"; then
            warnings+=("eager task creation (use tasks.register instead)")
        fi

        # Direct buildDir usage
        if echo "$content" | grep -qE '\$buildDir|project\.buildDir'; then
            warnings+=("\$buildDir usage (use layout.buildDirectory instead)")
        fi

        # tasks.all anti-pattern
        if echo "$content" | grep -qE "tasks\.all[[:space:]]*\{"; then
            warnings+=("tasks.all {} (use tasks.configureEach {} for lazy configuration)")
        fi
    fi

    # Check gradle.properties
    if [[ "$path" == *"gradle.properties"* ]]; then
        if ! echo "$content" | grep -q "org.gradle.parallel"; then
            warnings+=("consider adding org.gradle.parallel=true")
        fi
    fi

    echo "${warnings[@]:-}"
}

# Main execution
main() {
    # Skip if not a build file
    if [[ -z "$file_path" ]] || ! is_build_file "$file_path"; then
        # Silent exit for non-build files
        echo '{"continue": true, "suppressOutput": true}'
        exit 0
    fi

    # Run validation
    local warnings
    warnings=$(validate_build_file "$file_path")

    # Build response
    if [[ -n "$warnings" ]]; then
        local message="Build file modified. Potential issues detected: $warnings. Consider running /fix-config-cache to address configuration cache compatibility."
        cat <<EOF
{
  "continue": true,
  "suppressOutput": false,
  "systemMessage": "$message"
}
EOF
    else
        # Silent success for clean edits
        echo '{"continue": true, "suppressOutput": true}'
    fi
}

main "$@"
