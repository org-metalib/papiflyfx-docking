# PapiflyFX Application Archetype - Progress

**Lead**: @ops-engineer | **Reviewer**: @spec-steward

## Status: Implementation Complete - Phase 3 Validation Pending

### Completed

- [x] **Research** — extracted all version baselines, build conventions, CI patterns, and agent documentation structure from the repository (see `research.md`)
- [x] **Plan** — detailed implementation plan covering BOM module, archetype module, generated project layout, CI/CD, multi-agent docs, environment setup, build/test/run commands, validation strategy, and risks (see `plan.md`)
- [x] **Phase 1** — `papiflyfx-docking-bom` module created with all 12 publishable framework artifacts in `dependencyManagement`
- [x] **Phase 2** — `papiflyfx-docking-archetype` module created with all templates:
  - Root POM template with BOM import, platform profiles, enforcer, headless defaults
  - `app` module POM with Surefire/TestFX/javafx-maven-plugin configuration
  - `App.java` with DockManager starter, `AppLauncher.java` trampoline, `AppTest.java` smoke test
  - `.github/workflows/ci.yml` matching the framework's CI pattern
  - `AGENTS.md`, `CLAUDE.md`, `.github/copilot-instructions.md`, `spec/agents/README.md`
  - `README.md` with environment setup, build, test, and run instructions
  - `.mvn/wrapper/maven-wrapper.properties`
- [x] **Build validation** — full reactor compile passes (16 modules, BUILD SUCCESS)

### Pending

- [ ] **Phase 3** — end-to-end archetype generation test (`mvn archetype:generate` + build + run)
- [ ] **Phase 4** — root README documentation updates
- [ ] **@spec-steward review** of implementation
- [ ] **@core-architect review** of BOM shape

### Key Decisions Made

1. **Module name `app`** instead of `main` — avoids Java keyword confusion, clearer semantics
2. **BOM as child module** of root POM — keeps version synchronized with release plugin
3. **Maven wrapper via post-generation command** — avoids shell script packaging issues in archetypes
4. **No release workflow in generated app** — most new apps don't publish to Maven Central immediately
5. **Starter agent team of 3 roles** — `@app-dev`, `@ops-engineer`, `@spec-steward` — appropriate for a new application
6. **Velocity `#set($dollar='$')` escaping** — used to separate archetype properties from generated Maven properties in POM templates

### Files Created

| File | Purpose |
|------|---------|
| `papiflyfx-docking-bom/pom.xml` | BOM module |
| `papiflyfx-docking-archetype/pom.xml` | Archetype module POM |
| `papiflyfx-docking-archetype/src/main/resources/META-INF/maven/archetype-metadata.xml` | Archetype descriptor |
| `papiflyfx-docking-archetype/src/main/resources/archetype-resources/pom.xml` | Generated root POM template |
| `papiflyfx-docking-archetype/src/main/resources/archetype-resources/app/pom.xml` | Generated app module POM template |
| `papiflyfx-docking-archetype/src/main/resources/archetype-resources/app/src/main/java/App.java` | Generated JavaFX Application |
| `papiflyfx-docking-archetype/src/main/resources/archetype-resources/app/src/main/java/AppLauncher.java` | Generated main() trampoline |
| `papiflyfx-docking-archetype/src/main/resources/archetype-resources/app/src/test/java/AppTest.java` | Generated smoke test |
| `papiflyfx-docking-archetype/src/main/resources/archetype-resources/.github/workflows/ci.yml` | Generated CI workflow |
| `papiflyfx-docking-archetype/src/main/resources/archetype-resources/.github/copilot-instructions.md` | Generated Copilot instructions |
| `papiflyfx-docking-archetype/src/main/resources/archetype-resources/AGENTS.md` | Generated agent team |
| `papiflyfx-docking-archetype/src/main/resources/archetype-resources/CLAUDE.md` | Generated Claude Code instructions |
| `papiflyfx-docking-archetype/src/main/resources/archetype-resources/README.md` | Generated README |
| `papiflyfx-docking-archetype/src/main/resources/archetype-resources/spec/agents/README.md` | Generated agent operating model |
| `papiflyfx-docking-archetype/src/main/resources/archetype-resources/.mvn/wrapper/maven-wrapper.properties` | Generated Maven wrapper properties |

### Files Modified

| File | Change |
|------|--------|
| `pom.xml` (root) | Added `papiflyfx-docking-bom` (first module) and `papiflyfx-docking-archetype` (last module) to `<modules>` |
