# Develocity MCP Server Setup

This guide explains how to configure the Develocity MCP server to enable Build Scan queries in Claude Code.

## Prerequisites

- Access to a Develocity instance (e.g., `https://ge.example.com`)
- A Develocity access key with read permissions
- Claude Code with MCP support

## Step 1: Get Develocity Access Key

1. Log into your Develocity instance
2. Go to **My Settings** → **Access Keys**
3. Create a new access key with appropriate permissions
4. Copy the key (you won't see it again)

## Step 2: Configure MCP Server

Add the Develocity MCP server to your Claude Code settings.

### Option A: Project-level Configuration

Create or edit `.mcp.json` in your project root:

```json
{
  "mcpServers": {
    "develocity": {
      "type": "sse",
      "url": "https://ge.example.com/mcp/sse",
      "headers": {
        "Authorization": "Bearer YOUR_ACCESS_KEY"
      }
    }
  }
}
```

### Option B: User-level Configuration

Add to `~/.claude/settings.json`:

```json
{
  "mcpServers": {
    "develocity": {
      "type": "sse",
      "url": "https://ge.example.com/mcp/sse",
      "headers": {
        "Authorization": "Bearer YOUR_ACCESS_KEY"
      }
    }
  }
}
```

### Option C: Environment Variable for Key

For better security, use an environment variable:

```json
{
  "mcpServers": {
    "develocity": {
      "type": "sse",
      "url": "https://ge.example.com/mcp/sse",
      "headers": {
        "Authorization": "Bearer ${DEVELOCITY_ACCESS_KEY}"
      }
    }
  }
}
```

Then set the environment variable:
```bash
export DEVELOCITY_ACCESS_KEY="your-key-here"
```

## Step 3: Verify Configuration

After restarting Claude Code, test the connection:

```
Ask Claude: "Can you check my recent builds?"
```

If configured correctly, Claude will use `mcp__develocity__getBuilds` to query your build history.

## Troubleshooting

### "MCP tools not available"

The Develocity MCP server is not configured or not connected:
1. Check your `.mcp.json` or settings for typos
2. Verify the Develocity URL is correct
3. Ensure your access key is valid
4. Restart Claude Code after configuration changes

### "403 Forbidden"

Your access key lacks permissions:
1. Check the key has read access to builds
2. Verify the project name matches what you have access to

### "Connection refused"

Network issues:
1. Check if you can access the Develocity URL in a browser
2. Ensure you're on the correct network (VPN if required)
3. Check firewall settings

### "No builds found"

Query parameters may be too restrictive:
1. Verify the project name matches `rootProject.name` in settings.gradle
2. Expand the date range
3. Remove filters to test basic connectivity

## Security Notes

- **Never commit access keys** to version control
- Use environment variables for keys in shared configurations
- Access keys can be revoked in Develocity if compromised
- Consider using project-level `.mcp.json` (gitignored) for team projects

## Available MCP Tools

Once configured, these tools become available:

| Tool | Description |
|------|-------------|
| `mcp__develocity__getBuilds` | Query build history with filters |
| `mcp__develocity__getTestResults` | Query test results and flaky tests |

See the [develocity skill](../SKILL.md) for query examples.

## Gradle Enterprise vs Develocity

Develocity is the new name for Gradle Enterprise. The MCP server works with both:
- Gradle Enterprise 2023.x and earlier
- Develocity 2024.x and later

The URL path `/mcp/sse` is the same for both.
