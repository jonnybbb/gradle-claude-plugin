# Contributing to Claude Code Plugin Marketplace

Thank you for your interest in contributing to the Claude Code Plugin Marketplace! This document provides guidelines for contributing plugins and improvements to the marketplace.

## Ways to Contribute

1. **Submit a New Plugin**: Share your Gradle skills with the community
2. **Improve Existing Plugins**: Enhance documentation or add new skills
3. **Fix Bugs**: Report or fix issues in plugins
4. **Improve Documentation**: Help make the marketplace more accessible
5. **Suggest Features**: Propose new capabilities for the marketplace

## Submitting a New Plugin

### Prerequisites

Before submitting a plugin:
- [ ] Read the [Plugin Development Guide](docs/plugin-development-guide.md)
- [ ] Ensure your plugin provides unique or improved functionality
- [ ] Test your plugin thoroughly
- [ ] Create comprehensive documentation

### Submission Process

1. **Fork the Repository**
   ```bash
   git clone https://github.com/jonnybbb/claude-gradle-skills.git
   cd claude-gradle-skills
   ```

2. **Create Your Plugin**
   ```bash
   mkdir -p plugins/your-plugin-name
   cd plugins/your-plugin-name
   ```

3. **Create Required Files**
   - `plugin.json` - Plugin manifest (required)
   - `README.md` - Plugin documentation (required)
   - `CHANGELOG.md` - Version history (recommended)

4. **Validate Your Plugin**
   - Ensure `plugin.json` conforms to `plugin-manifest-schema.json`
   - Verify all skills are properly documented
   - Test all example scenarios
   - Check for typos and formatting issues

5. **Update the Registry**
   
   Add your plugin to `plugins/registry.json`:
   ```json
   {
     "id": "your.plugin-id",
     "name": "Your Plugin Name",
     "version": "1.0.0",
     "description": "Brief description",
     "author": {
       "name": "Your Name"
     },
     "category": "build-tools",
     "repository": "./your-plugin-name"
   }
   ```

6. **Create a Pull Request**
   
   - Use a clear, descriptive title
   - Describe what your plugin does
   - List the skills it provides
   - Mention any dependencies or requirements
   - Include testing steps

### Pull Request Template

```markdown
## Plugin Submission: [Plugin Name]

### Description
[Brief description of what your plugin does]

### Skills Provided
- **Skill 1**: [Description]
- **Skill 2**: [Description]

### Category
[e.g., build-tools, testing, etc.]

### Dependencies
[List any plugin dependencies or "None"]

### Testing
[How you tested the plugin]

### Checklist
- [ ] Plugin manifest is valid
- [ ] README is complete
- [ ] All skills are documented with examples
- [ ] Registry.json is updated
- [ ] No conflicts with existing plugins
```

## Plugin Quality Guidelines

### Manifest Requirements
- ✅ Valid JSON format
- ✅ Follows schema specification
- ✅ Unique plugin ID (reverse domain notation)
- ✅ Semantic versioning
- ✅ Clear, concise description
- ✅ At least 3 keywords
- ✅ Valid license identifier

### Documentation Requirements
- ✅ README with overview and usage
- ✅ Each skill documented with examples
- ✅ Requirements clearly stated
- ✅ Installation/setup instructions
- ✅ No broken links

### Skill Requirements
- ✅ Clear, specific skill descriptions
- ✅ At least 3 usage examples per skill
- ✅ Realistic, practical examples
- ✅ No overlapping with existing skills (unless providing clear improvements)

## Updating Existing Plugins

To update an existing plugin:

1. **Minor Updates** (documentation, examples):
   - Fork and make changes
   - Submit PR with clear description

2. **Major Updates** (new skills, breaking changes):
   - Bump version appropriately
   - Update CHANGELOG.md
   - Update registry.json
   - Document breaking changes in PR

## Code of Conduct

### Our Standards

- **Be Respectful**: Treat everyone with respect and kindness
- **Be Constructive**: Provide helpful feedback and suggestions
- **Be Collaborative**: Work together to improve the marketplace
- **Be Patient**: Remember that everyone is learning

### Unacceptable Behavior

- Harassment or discrimination
- Trolling or insulting comments
- Spam or self-promotion without value
- Publishing others' private information

## Review Process

All submissions go through a review process:

1. **Automated Checks**: Schema validation, link checking
2. **Manual Review**: Quality and usefulness assessment
3. **Community Feedback**: Input from other contributors
4. **Approval**: Merge when all checks pass and reviews are positive

Typical review time: 3-7 days

## Getting Help

- **Questions**: Open a GitHub Discussion
- **Issues**: Report bugs via GitHub Issues
- **Chat**: Join our community Discord (if available)
- **Email**: Contact maintainers directly for private matters

## Plugin Naming Conventions

- Use lowercase with hyphens: `my-plugin-name`
- Be descriptive but concise
- Avoid trademarked names
- Use reverse domain notation for IDs: `com.example.plugin-name`

## License

By contributing, you agree that your contributions will be licensed under the same license as the project (MIT License).

## Recognition

Contributors will be:
- Listed in plugin manifests as authors
- Credited in CHANGELOG entries
- Featured in community highlights (with permission)

---

Thank you for contributing to the Claude Code Plugin Marketplace! Your plugins help developers be more productive with Gradle. 🙏
