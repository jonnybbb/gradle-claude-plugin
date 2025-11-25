# Gradle Migration Checklist

## Pre-Migration Assessment

### Environment Check
- [ ] Document current Gradle version
- [ ] List all applied plugins and their versions
- [ ] Identify custom tasks and plugins
- [ ] Review gradle.properties settings
- [ ] Check CI/CD pipeline configuration
- [ ] Note any Gradle Enterprise/Develocity integration

### Backup
- [ ] Commit all current changes
- [ ] Create migration branch
- [ ] Document current build behavior (times, outputs)

## Gradle 7 to 8 Migration

### Wrapper Update
```bash
./gradlew wrapper --gradle-version 8.11
```

### Breaking Changes Checklist

#### Build Configuration
- [ ] Replace `compile` with `implementation`/`api`
- [ ] Replace `testCompile` with `testImplementation`
- [ ] Replace `runtime` with `runtimeOnly`
- [ ] Update `buildDir` to `layout.buildDirectory`

#### Task Changes
- [ ] Replace `doFirst`/`doLast` with `Task.doFirst`/`doLast`
- [ ] Replace `dependsOn` when used for file dependencies
- [ ] Migrate `tasks.create` to `tasks.register`
- [ ] Update deprecated task properties

#### Plugin Updates
- [ ] Update kotlin plugin to 1.9+
- [ ] Update spring-boot plugin to 3.x
- [ ] Update android gradle plugin to 8.x
- [ ] Check third-party plugins for compatibility

### Configuration Cache
- [ ] Test with `--configuration-cache`
- [ ] Fix Task.project access in doFirst/doLast
- [ ] Replace System.getProperty with providers
- [ ] Update custom tasks for CC compatibility

### Version Catalog
- [ ] Create `gradle/libs.versions.toml`
- [ ] Migrate dependency declarations
- [ ] Update plugin declarations to use aliases

## Gradle 6 to 7 Migration

### Build Configuration
- [ ] Remove `enableFeaturePreview("VERSION_CATALOGS")`
- [ ] Update to new publishing configuration
- [ ] Migrate from `compile` configurations

### Plugin Changes
- [ ] Update java-library usage
- [ ] Update kotlin plugin version
- [ ] Review custom plugin configurations

## Post-Migration Verification

### Functional Tests
- [ ] Clean build succeeds: `./gradlew clean build`
- [ ] Tests pass: `./gradlew test`
- [ ] Publishable artifacts correct: `./gradlew assemble`
- [ ] Build scan shows no issues: `./gradlew build --scan`

### Performance Check
- [ ] Compare build times before/after
- [ ] Check cache hit rates
- [ ] Verify daemon stability

### CI/CD Verification
- [ ] Pipeline builds succeed
- [ ] Cache integration working
- [ ] Deployment artifacts correct

## Rollback Plan

If migration fails:
```bash
# Revert wrapper
git checkout gradle/wrapper/gradle-wrapper.properties

# Or reset to pre-migration commit
git reset --hard pre-migration-commit
```

## Common Migration Issues

| Issue | Solution |
|-------|----------|
| Plugin not compatible | Check for updated version or alternative |
| Deprecated API warning | Follow deprecation message guidance |
| Task execution fails | Check task dependencies and inputs |
| Build cache misses | Verify input/output annotations |
| Configuration cache fail | See gradle-config-cache skill |

## Resources

- [Gradle Upgrade Guide](https://docs.gradle.org/current/userguide/upgrading_version_7.html)
- [Gradle Compatibility Matrix](https://docs.gradle.org/current/userguide/compatibility.html)
- [Migration Tool](https://gradle.org/releases/)
