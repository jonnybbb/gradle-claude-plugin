#!/bin/bash
# Shared settings functions for Gradle plugin hooks
#
# Usage in hook scripts:
#   source "${CLAUDE_PLUGIN_ROOT}/hooks/lib/settings.sh"
#   if ! is_hook_enabled "sessionStart"; then exit 0; fi

# Path to project settings file
get_settings_file() {
    local project_dir="${CLAUDE_PROJECT_DIR:-.}"
    echo "$project_dir/.claude/gradle-plugin.local.md"
}

# Extract YAML value from frontmatter
# Args: $1 = settings file content, $2 = key path (e.g., "hooks.enabled")
get_yaml_value() {
    local content="$1"
    local key="$2"

    # Extract frontmatter between --- markers
    local frontmatter
    frontmatter=$(echo "$content" | sed -n '/^---$/,/^---$/p' | sed '1d;$d')

    # Parse nested keys (supports hooks.enabled, hooks.sessionStart, etc.)
    local value=""
    case "$key" in
        hooks.enabled)
            value=$(echo "$frontmatter" | grep -A10 "^hooks:" | grep "enabled:" | head -1 | sed 's/.*enabled:\s*//' | tr -d ' ')
            ;;
        hooks.sessionStart)
            value=$(echo "$frontmatter" | grep -A10 "^hooks:" | grep "sessionStart:" | head -1 | sed 's/.*sessionStart:\s*//' | tr -d ' ')
            ;;
        hooks.postToolUse)
            value=$(echo "$frontmatter" | grep -A10 "^hooks:" | grep "postToolUse:" | head -1 | sed 's/.*postToolUse:\s*//' | tr -d ' ')
            ;;
    esac

    echo "$value"
}

# Check if a specific hook is enabled
# Args: $1 = hook name (sessionStart, postToolUse)
# Returns: 0 if enabled, 1 if disabled
is_hook_enabled() {
    local hook_name="$1"
    local settings_file
    settings_file=$(get_settings_file)

    # If no settings file exists, hooks are enabled by default
    if [[ ! -f "$settings_file" ]]; then
        return 0
    fi

    local content
    content=$(cat "$settings_file")

    # Check global hooks.enabled first (case-insensitive)
    local global_enabled
    global_enabled=$(get_yaml_value "$content" "hooks.enabled" | tr '[:upper:]' '[:lower:]')
    if [[ "$global_enabled" == "false" ]]; then
        return 1
    fi

    # Check specific hook setting (case-insensitive)
    local hook_enabled
    hook_enabled=$(get_yaml_value "$content" "hooks.$hook_name" | tr '[:upper:]' '[:lower:]')
    if [[ "$hook_enabled" == "false" ]]; then
        return 1
    fi

    return 0
}
