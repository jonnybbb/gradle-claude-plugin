# Plugin Development Guide

Welcome to the Claude Code Plugin Marketplace! This guide will help you create and publish plugins that extend Claude Code's capabilities with Gradle skills.

## What is a Plugin?

A Claude Code plugin is a package that provides one or more "skills" - specific capabilities that Claude Code can use to help developers work with Gradle projects. Plugins are defined by a manifest file and can include documentation, examples, and skill definitions.

## Plugin Structure

Each plugin should follow this directory structure:

```
my-plugin/
├── plugin.json          # Plugin manifest (required)
├── README.md           # Plugin documentation (required)
├── CHANGELOG.md        # Version history (recommended)
├── skills/             # Skill definitions (optional)
│   ├── skill1.md
│   └── skill2.md
└── examples/           # Usage examples (optional)
    ├── example1.md
    └── example2.md
```

## Creating a Plugin Manifest

The `plugin.json` file is the heart of your plugin. It must conform to the `plugin-manifest-schema.json` schema.

### Required Fields

```json
{
  "id": "com.example.my-plugin",
  "name": "My Awesome Plugin",
  "version": "1.0.0",
  "description": "A brief description of what this plugin does",
  "author": {
    "name": "Your Name"
  }
}
```

### Recommended Fields

```json
{
  "repository": "https://github.com/username/my-plugin",
  "homepage": "https://myplugin.dev",
  "keywords": ["gradle", "build", "automation"],
  "license": "MIT",
  "category": "build-tools"
}
```

### Plugin Categories

Choose the category that best fits your plugin:
- **build-tools**: Build system utilities and wrappers
- **testing**: Testing frameworks and tools
- **code-generation**: Code scaffolding and generation
- **dependency-management**: Dependency resolution and updates
- **documentation**: Documentation generation and management
- **deployment**: Deployment and release tools
- **code-quality**: Linting, formatting, and analysis
- **other**: Anything else

## Defining Skills

Skills are the core capabilities your plugin provides. Each skill should be well-defined with:

```json
{
  "skills": [
    {
      "name": "my-skill",
      "description": "Clear description of what this skill does",
      "examples": [
        "Example usage scenario 1",
        "Example usage scenario 2",
        "Example usage scenario 3"
      ]
    }
  ]
}
```

### Skill Best Practices

1. **Be Specific**: Clearly define what the skill does
2. **Provide Examples**: Include 3-5 real-world usage examples
3. **Keep It Focused**: Each skill should do one thing well
4. **Document Limitations**: Note any version requirements or constraints

## Gradle Version Compatibility

Specify Gradle version requirements in your manifest:

```json
{
  "gradle": {
    "minVersion": "7.0",
    "maxVersion": "8.5"
  }
}
```

- `minVersion`: Required if your plugin needs specific Gradle features
- `maxVersion`: Use only if you know of compatibility issues with newer versions

## Plugin Dependencies

If your plugin requires other plugins, declare them:

```json
{
  "dependencies": [
    "com.example.base-plugin",
    "com.example.another-plugin"
  ]
}
```

## Versioning

Follow [Semantic Versioning](https://semver.org/) (SemVer):
- **MAJOR**: Incompatible API changes
- **MINOR**: New functionality in a backwards-compatible manner
- **PATCH**: Backwards-compatible bug fixes

Example: `1.2.3` or `2.0.0-beta.1`

## Testing Your Plugin

Before submitting your plugin:

1. **Validate the manifest**: Ensure it conforms to `plugin-manifest-schema.json`
2. **Test all skills**: Verify each skill works as documented
3. **Check examples**: Make sure all examples in the manifest are accurate
4. **Review documentation**: Ensure README is clear and complete

## Publishing Your Plugin

To publish your plugin to the marketplace:

1. Fork this repository
2. Add your plugin to the `plugins/` directory
3. Update `plugins/registry.json` to include your plugin
4. Submit a Pull Request with:
   - Your plugin directory
   - Updated registry.json
   - Clear description of what your plugin does

See `CONTRIBUTING.md` for detailed submission guidelines.

## Example Plugin

Check out `plugins/example-gradle-plugin/` for a complete example of a well-structured plugin.

## Support and Community

- **Issues**: Report bugs or request features via GitHub Issues
- **Discussions**: Join the community in GitHub Discussions
- **Contributing**: See CONTRIBUTING.md for how to help improve the marketplace

## Resources

- [Plugin Manifest Schema](../plugin-manifest-schema.json)
- [Example Plugin](../plugins/example-gradle-plugin/)
- [Registry Format](../plugins/registry.json)
- [Gradle Documentation](https://docs.gradle.org/)

---

Happy plugin development! 🚀
