# Quick Start Guide

Get started with the Claude Code Plugin Marketplace in minutes!

## For Plugin Users

### Discovering Plugins

1. **Browse the Registry**
   
   Check [`plugins/registry.json`](../plugins/registry.json) for all available plugins:
   ```bash
   cat plugins/registry.json | jq '.plugins[] | {name, description, category}'
   ```

2. **Explore Plugin Details**
   
   Visit each plugin's directory in `plugins/` to read documentation and see examples.

3. **Filter by Category**
   
   Plugins are categorized: `build-tools`, `testing`, `code-generation`, etc.

### Using Plugin Skills

Once a plugin is installed in Claude Code, you can use its skills naturally:

**Example**: Using the Gradle Wrapper Plugin
```
"Create a new Java application with Gradle"
"Build my Gradle project"
"Show me the dependency tree"
```

## For Plugin Developers

### Creating Your First Plugin

**Step 1: Set Up Structure**
```bash
mkdir -p plugins/my-plugin
cd plugins/my-plugin
```

**Step 2: Create Plugin Manifest**

Create `plugin.json`:
```json
{
  "$schema": "../../plugin-manifest-schema.json",
  "id": "com.mycompany.my-plugin",
  "name": "My Plugin",
  "version": "1.0.0",
  "description": "What my plugin does",
  "author": {
    "name": "Your Name",
    "email": "your.email@example.com"
  },
  "category": "build-tools",
  "keywords": ["gradle", "build"],
  "license": "MIT",
  "skills": [
    {
      "name": "my-skill",
      "description": "What this skill does",
      "examples": [
        "Example usage 1",
        "Example usage 2",
        "Example usage 3"
      ]
    }
  ]
}
```

**Step 3: Create Documentation**

Create `README.md`:
```markdown
# My Plugin

Brief overview of what your plugin does.

## Skills

### my-skill
Description of the skill and how to use it.

## Requirements
- Gradle 7.0+

## License
MIT
```

**Step 4: Validate**
```bash
# Validate JSON format
cat plugin.json | jq .

# Check against schema (if you have a JSON schema validator)
```

**Step 5: Update Registry**

Add your plugin to `plugins/registry.json`.

**Step 6: Submit**

Create a Pull Request with your plugin!

## Common Tasks

### Validating a Plugin Manifest

```bash
# Check JSON syntax
jq . < plugins/my-plugin/plugin.json

# Validate against schema (requires ajv-cli)
npm install -g ajv-cli
ajv validate -s plugin-manifest-schema.json -d plugins/my-plugin/plugin.json
```

### Testing Plugin Documentation

```bash
# Check for broken links
npm install -g markdown-link-check
markdown-link-check plugins/my-plugin/README.md
```

### Viewing the Plugin Registry

```bash
# Pretty print the registry
jq . < plugins/registry.json

# List all plugins
jq '.plugins[].name' < plugins/registry.json

# Filter by category
jq '.plugins[] | select(.category=="build-tools")' < plugins/registry.json
```

## Best Practices

### DO ✅
- Start with a clear problem your plugin solves
- Provide 3-5 practical examples per skill
- Keep skill descriptions concise but complete
- Test your plugin with real Gradle projects
- Follow semantic versioning
- Update documentation when adding features

### DON'T ❌
- Create duplicate functionality without improvement
- Use vague or generic skill descriptions
- Skip examples or documentation
- Include untested code or examples
- Use non-standard directory structures
- Forget to update the registry

## Getting Help

- 📖 Read the full [Plugin Development Guide](plugin-development-guide.md)
- 🤝 Check [CONTRIBUTING.md](../CONTRIBUTING.md) for submission process
- 💬 Open a GitHub Discussion for questions
- 🐛 Report issues via GitHub Issues

## Next Steps

1. **Study the Example**: Review [`example-gradle-plugin`](../plugins/example-gradle-plugin/)
2. **Plan Your Plugin**: Define the skills you want to provide
3. **Build and Test**: Create your plugin and test thoroughly
4. **Submit**: Follow the contribution guidelines to submit

---

Happy plugin development! 🎉
