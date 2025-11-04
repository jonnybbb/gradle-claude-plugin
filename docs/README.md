# Documentation

Welcome to the Claude Code Plugin Marketplace documentation!

## 📚 Documentation Overview

### For Everyone
- **[Main README](../README.md)** - Overview of the marketplace and project goals

### For Plugin Users
- **[Quick Start Guide](quick-start.md)** - Get started quickly with finding and using plugins
- **[Plugins Directory](../plugins/README.md)** - Browse available plugins

### For Plugin Developers
- **[Plugin Development Guide](plugin-development-guide.md)** - Complete guide to creating plugins
- **[Quick Start Guide](quick-start.md)** - Fast track to creating your first plugin
- **[Contributing Guidelines](../CONTRIBUTING.md)** - How to submit plugins and improvements

### Technical Reference
- **[Plugin Manifest Schema](../plugin-manifest-schema.json)** - JSON schema for plugin manifests
- **[Plugin Registry Format](../plugins/registry.json)** - Registry structure and format

## 🎯 Common Use Cases

### "I want to use plugins"
1. Start with the [Quick Start Guide](quick-start.md)
2. Browse the [Plugins Directory](../plugins/)
3. Read individual plugin README files

### "I want to create a plugin"
1. Read the [Plugin Development Guide](plugin-development-guide.md)
2. Study the [Example Plugin](../plugins/example-gradle-plugin/)
3. Follow the [Quick Start Guide](quick-start.md) for step-by-step instructions
4. Review [Contributing Guidelines](../CONTRIBUTING.md) before submitting

### "I want to contribute"
1. Check [Contributing Guidelines](../CONTRIBUTING.md)
2. Review the [Plugin Development Guide](plugin-development-guide.md)
3. Look at existing plugins for examples
4. Submit your Pull Request

## 📖 Key Concepts

### Plugin
A package that provides one or more skills for Claude Code to help with Gradle projects.

### Skill
A specific capability that a plugin provides (e.g., "initialize a Gradle project", "run tests").

### Manifest
The `plugin.json` file that describes a plugin, its skills, requirements, and metadata.

### Registry
The central index (`plugins/registry.json`) that lists all available plugins.

### Category
A classification for plugins (build-tools, testing, code-generation, etc.).

## 🔧 Tools and Utilities

### JSON Schema Validation
```bash
npm install -g ajv-cli
ajv validate -s plugin-manifest-schema.json -d plugins/my-plugin/plugin.json
```

### Link Checking
```bash
npm install -g markdown-link-check
markdown-link-check README.md
```

### Pretty Print Registry
```bash
jq . < plugins/registry.json
```

## 🌟 Examples

### Example Plugin
The [example-gradle-plugin](../plugins/example-gradle-plugin/) demonstrates best practices for:
- Plugin manifest structure
- Skill definitions with examples
- Documentation organization
- README format

### Example Manifest
See [plugin-manifest-schema.json](../plugin-manifest-schema.json) for the complete schema with all available fields.

## 🆘 Getting Help

- **Questions**: Open a GitHub Discussion
- **Bugs**: Report via GitHub Issues
- **Ideas**: Share in GitHub Discussions
- **Pull Requests**: Follow [Contributing Guidelines](../CONTRIBUTING.md)

## 📝 Documentation Standards

When contributing documentation:
- Use clear, concise language
- Provide practical examples
- Include code snippets where helpful
- Link to related documentation
- Keep formatting consistent

## 🔄 Keeping Documentation Updated

Documentation should be updated when:
- New plugins are added
- Schema changes occur
- Best practices evolve
- User feedback indicates confusion
- New features are added

## 📋 Document Checklist

For plugin documentation:
- [ ] Clear overview/description
- [ ] Requirements listed
- [ ] Installation/usage instructions
- [ ] Skill descriptions with examples
- [ ] Links work correctly
- [ ] Formatting is consistent
- [ ] License information included

---

Need something not covered here? [Open an issue](https://github.com/jonnybbb/claude-gradle-skills/issues) and let us know!
