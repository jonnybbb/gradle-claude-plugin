# Plugins Directory

This directory contains all plugins available in the Claude Code Plugin Marketplace for Gradle skills.

## Plugin Registry

The [`registry.json`](registry.json) file serves as the central index of all available plugins. It provides a quick overview of each plugin including:
- Plugin ID and name
- Version
- Description
- Author
- Category
- Skills provided
- Repository location

## Available Plugins

### Example Gradle Plugin
**Category**: build-tools  
**Description**: Example plugin demonstrating Gradle wrapper skills

A comprehensive example showing how to create a well-structured plugin with multiple skills for Gradle project management.

[View Plugin Details →](example-gradle-plugin/)

## Plugin Categories

Plugins are organized into the following categories:

- **build-tools**: Build system utilities and wrappers
- **testing**: Testing frameworks and tools  
- **code-generation**: Code scaffolding and generation
- **dependency-management**: Dependency resolution and updates
- **documentation**: Documentation generation and management
- **deployment**: Deployment and release tools
- **code-quality**: Linting, formatting, and analysis
- **other**: Miscellaneous utilities

## Adding Your Plugin

Want to add your plugin to this marketplace?

1. Create your plugin directory: `plugins/your-plugin-name/`
2. Include required files:
   - `plugin.json` (manifest)
   - `README.md` (documentation)
3. Update `registry.json` with your plugin info
4. Submit a Pull Request

See [Contributing Guidelines](../CONTRIBUTING.md) for detailed instructions.

## Plugin Structure

Each plugin directory should follow this structure:

```
plugin-name/
├── plugin.json          # Plugin manifest (required)
├── README.md           # Plugin documentation (required)
├── CHANGELOG.md        # Version history (recommended)
├── LICENSE             # License file (recommended)
├── skills/             # Detailed skill documentation (optional)
│   ├── skill1.md
│   └── skill2.md
└── examples/           # Usage examples (optional)
    ├── example1.md
    └── example2.md
```

## Resources

- [Plugin Development Guide](../docs/plugin-development-guide.md)
- [Quick Start Guide](../docs/quick-start.md)
- [Plugin Manifest Schema](../plugin-manifest-schema.json)
- [Contributing Guidelines](../CONTRIBUTING.md)

---

Browse plugins, learn from examples, and contribute your own! 🚀
