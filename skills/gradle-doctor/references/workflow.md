# Doctor Analysis Workflow

Detailed workflow for comprehensive build health analysis.

## Phase 1: Data Collection

1. **Run gradle-analyzer.java**
   - Extract project metadata
   - Identify Gradle version
   - Count modules and tasks
   - Parse build configuration

2. **Run cache-validator.java**
   - Check cache configuration
   - Identify compatibility issues
   - Suggest fixes

3. **Collect Configuration**
   - gradle.properties settings
   - Wrapper version
   - JVM arguments

## Phase 2: Subagent Analysis

### Performance Subagent
- Analyze build times
- Profile task execution
- Check parallelization
- Review JVM settings

### Cache Subagent
- Validate cache setup
- Check compatibility
- Identify cache misses
- Review task cacheability

### Dependency Subagent
- Detect conflicts
- Check version alignment
- Review transitive dependencies
- Suggest constraints

### Structure Subagent
- Review project organization
- Check for convention plugins
- Analyze build file patterns
- Suggest improvements

## Phase 3: Synthesis

1. **Aggregate Findings**
   - Combine subagent results
   - Correlate issues
   - Identify root causes

2. **Prioritize Recommendations**
   - HIGH: Critical issues
   - MEDIUM: Optimization opportunities
   - LOW: Nice-to-have improvements

3. **Identify Quick Wins**
   - Low effort, high impact
   - Safe auto-fixes
   - Immediate value

4. **Generate Report**
   - Summary score
   - Detailed findings
   - Action items
   - Effort estimates

## Timing Expectations

| Scan Type | Duration | Coverage |
|-----------|----------|----------|
| --quick | 10-30s | Essential checks |
| default | 1-3min | Standard analysis |
| --deep | 5-15min | Full analysis |
