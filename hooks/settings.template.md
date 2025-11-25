# Gradle Plugin Settings

Configure the Gradle Claude Plugin behavior for this project.

Copy this file to `.claude/gradle-plugin.local.md` in your project root to customize settings.

---
hooks:
  # Enable/disable all hooks (default: true)
  enabled: true

  # SessionStart hook - runs when opening a Gradle project
  # Shows quick health check results and suggestions
  sessionStart: true

  # PostToolUse hook - runs after editing build files
  # Warns about configuration cache issues and deprecated patterns
  postToolUse: true
---

## Notes

- Settings are read from `.claude/gradle-plugin.local.md` in your project root
- Changes take effect immediately (no restart needed)
- Set `hooks.enabled: false` to disable all hooks at once
- Set individual hooks to `false` for granular control
