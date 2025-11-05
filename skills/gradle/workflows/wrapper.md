# Gradle Wrapper Management

Manage and update the Gradle wrapper for consistent builds across environments.

## Task

You are tasked with managing the Gradle wrapper. Follow these steps:

1. **Check Current Wrapper Version**: View the current Gradle wrapper version:
   ```bash
   ./gradlew --version
   ```

2. **Update Wrapper**: Update to a specific Gradle version:
   ```bash
   ./gradlew wrapper --gradle-version 8.5
   ```

3. **Update to Latest**: Update to the latest Gradle version:
   ```bash
   ./gradlew wrapper --gradle-version latest
   ```

4. **Verify Update**: After updating, check the version again:
   ```bash
   ./gradlew --version
   ```

5. **Commit Wrapper**: Always commit wrapper files to version control:
   - `gradlew` (Unix script)
   - `gradlew.bat` (Windows script)
   - `gradle/wrapper/gradle-wrapper.jar`
   - `gradle/wrapper/gradle-wrapper.properties`

## Why Use Gradle Wrapper

The Gradle wrapper ensures:
- **Consistency**: Everyone uses the same Gradle version
- **No installation**: Team members don't need to install Gradle
- **Reproducibility**: Builds work the same way across machines
- **Version control**: Gradle version is tracked in your repository

## Wrapper Configuration

The wrapper is configured in `gradle/wrapper/gradle-wrapper.properties`:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

## Common Wrapper Commands

```bash
# Check current version
./gradlew --version

# Update to specific version
./gradlew wrapper --gradle-version 8.5

# Update to latest stable version
./gradlew wrapper --gradle-version latest

# Use full distribution (includes sources and docs)
./gradlew wrapper --gradle-version 8.5 --distribution-type all

# Generate wrapper if missing
gradle wrapper
```

## Distribution Types

- `bin` - Binary distribution (default, smaller download)
- `all` - Complete distribution with sources and documentation (larger, better for IDE)

Update distribution type:
```bash
./gradlew wrapper --gradle-version 8.5 --distribution-type all
```

## Troubleshooting

**Wrapper not found:**
- If `gradlew` doesn't exist, run `gradle wrapper` with a globally installed Gradle
- Or download wrapper files from another project

**Permission issues (Unix/Mac):**
```bash
chmod +x gradlew
```

**Checksum validation failure:**
- Check internet connection
- Verify the distribution URL in `gradle-wrapper.properties`
- Try re-generating the wrapper

**Old Gradle version:**
- Update with `./gradlew wrapper --gradle-version <version>`
- Commit the updated wrapper files

## Best Practices

1. **Always use wrapper**: Use `./gradlew` instead of `gradle`
2. **Keep wrapper updated**: Update regularly for bug fixes and features
3. **Commit wrapper files**: Include all wrapper files in version control
4. **Document version**: Note Gradle version in README
5. **Test after updates**: Run full build after updating wrapper

## Version Recommendations

- **Latest stable**: For new projects, use the latest stable version
- **LTS versions**: Consider Long-Term Support versions for production
- **Team consensus**: Ensure team agrees on version updates
- **Plugin compatibility**: Verify plugins work with target version

## Gradle Version History

Popular recent versions:
- Gradle 8.5 (2023) - Latest stable with performance improvements
- Gradle 8.0 (2023) - Major version with new features
- Gradle 7.6 (2023) - LTS version
- Gradle 7.0 (2021) - Java 16 support

Check [Gradle Releases](https://gradle.org/releases/) for latest versions.

## Notes

- The wrapper downloads and caches Gradle distributions in `~/.gradle/wrapper/dists/`
- First run after update will download the new Gradle version
- Wrapper works offline after initial download
- You can manually specify a custom distribution URL in wrapper properties
