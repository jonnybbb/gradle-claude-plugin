# Claude Code Plugin Marketplace

Welcome to the Claude Code Plugin Marketplace for Gradle skills! This repository hosts a collection of plugins that extend Claude Code's capabilities to work with Gradle projects.

## 🎯 What is This?

The Claude Code Plugin Marketplace is a centralized repository where developers can discover, share, and contribute plugins that provide Gradle-related skills to Claude Code. Each plugin enhances Claude Code's ability to help you with specific Gradle tasks, from project initialization to build automation.

## 🚀 Getting Started

### For Plugin Users

Browse available plugins in the [`plugins/`](plugins/) directory or check the [Plugin Registry](plugins/registry.json) for a complete list.

**Featured Plugins:**
- **Gradle Wrapper Plugin** - Initialize and build Gradle projects

### For Plugin Developers

Want to create your own plugin? Check out our guides:
- 📖 [Plugin Development Guide](docs/plugin-development-guide.md) - Learn how to create plugins
- 🤝 [Contributing Guidelines](CONTRIBUTING.md) - How to submit your plugin
- 🔍 [Plugin Manifest Schema](plugin-manifest-schema.json) - Technical specification

## 📦 Plugin Structure

```
claude-gradle-skills/
├── plugins/                  # All plugins
│   ├── registry.json        # Plugin registry/index
│   └── example-gradle-plugin/
│       ├── plugin.json      # Plugin manifest
│       └── README.md        # Plugin documentation
├── docs/                    # Documentation
│   └── plugin-development-guide.md
├── plugin-manifest-schema.json  # Schema definition
└── CONTRIBUTING.md         # Contribution guidelines
```

## 🎨 Plugin Categories

Plugins are organized into categories:
- **build-tools** - Build system utilities and wrappers
- **testing** - Testing frameworks and tools
- **code-generation** - Code scaffolding and generation
- **dependency-management** - Dependency resolution and updates
- **documentation** - Documentation generation
- **deployment** - Deployment and release tools
- **code-quality** - Linting, formatting, and analysis

## 🤝 Contributing

We welcome contributions! Here's how you can help:

1. **Create a Plugin** - Share your Gradle expertise
2. **Improve Documentation** - Make guides clearer
3. **Report Issues** - Help us fix problems
4. **Suggest Features** - Propose new capabilities

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

## 📋 Plugin Requirements

All plugins must:
- ✅ Include a valid `plugin.json` manifest
- ✅ Provide comprehensive documentation
- ✅ Define clear, useful skills
- ✅ Include practical examples
- ✅ Follow semantic versioning

## 🔍 Example Plugin

Check out the [example-gradle-plugin](plugins/example-gradle-plugin/) to see a complete, well-structured plugin that demonstrates best practices.

## 📄 License

This project is licensed under the MIT License - see individual plugin directories for specific plugin licenses.

## 🙏 Acknowledgments

Thanks to all contributors who help make Claude Code more powerful for Gradle developers!

---

**Ready to get started?** Check out the [Plugin Development Guide](docs/plugin-development-guide.md) or browse existing plugins in the [`plugins/`](plugins/) directory!
