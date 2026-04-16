# PapiflyFX Application Archetype - Progress

**Lead**: @ops-engineer | **Reviewer**: @spec-steward

## Status: Complete

### Completed

- [x] **Research** — extracted all version baselines, build conventions, CI patterns, and agent documentation structure from the repository (see `research.md`)
- [x] **Plan** — detailed implementation plan covering BOM module, archetype module, generated project layout, CI/CD, multi-agent docs, environment setup, build/test/run commands, validation strategy, and risks (see `plan.md`)
- [x] **Phase 1** — `papiflyfx-docking-bom` module created with all 12 publishable framework artifacts in `dependencyManagement`
- [x] **Phase 2** — `papiflyfx-docking-archetype` module created with all templates
- [x] **Build validation** — full reactor compile passes (16 modules, BUILD SUCCESS)
- [x] **Phase 3** — end-to-end archetype generation test
  - `./mvnw -pl papiflyfx-docking-bom,papiflyfx-docking-archetype -am clean install` — BUILD SUCCESS
  - `mvn archetype:generate` — project generated with correct structure
  - `./mvnw clean package` — generated project compiles and tests pass (1 test, 0 failures)
  - `./mvnw -Dtestfx.headless=true test` — headless tests pass
  - Generated structure matches plan: `app/` directory, correct package layout, all docs preserved
- [x] **Phase 4** — archetype integration test
  - Created `src/test/resources/projects/basic/` with `archetype.properties` and `goal.txt`
  - `./mvnw -pl papiflyfx-docking-archetype verify` — BUILD SUCCESS (generates, compiles, tests, and packages the IT project)
- [x] **Phase 5** — root README updated with BOM and archetype sections
- [x] **Phase 6** — progress tracking updated

### Bugs Fixed During Phase 3

1. **Root POM module path mismatch** (High) — template had `<module>${rootArtifactId}-app</module>`, changed to `<module>app</module>` to match the `dir="app"` in archetype-metadata.xml
2. **Velocity `##` eating Markdown headings** (Medium) — all filtered Markdown templates now use `#set($h1 = '#')` / `$h2` / `$h3` variables to escape heading markers
3. **Documentation referencing wrong module path** (Medium) — all doc templates and archetype README updated to use `app/` for directory references and `-pl app` for commands
4. **`getMainContainer()` API does not exist** — App.java template changed to `getRootPane()` which is the actual DockManager API
5. **`App.main(args)` does not exist** — AppLauncher.java template changed to `Application.launch(App.class, args)` matching the framework samples pattern
6. **LICENSE link removed** (Low) — generated README no longer links to a non-existent LICENSE file

### Key Decisions Made

1. **Module name `app`** instead of `main` — avoids Java keyword confusion, clearer semantics
2. **BOM as child module** of root POM — keeps version synchronized with release plugin
3. **Maven wrapper via post-generation command** — avoids shell script packaging issues in archetypes
4. **No release workflow in generated app** — most new apps don't publish to Maven Central immediately
5. **Starter agent team of 3 roles** — `@app-dev`, `@ops-engineer`, `@spec-steward` — appropriate for a new application
6. **Velocity `#set($h = '#')` escaping** — cleanest approach for Markdown headings in Velocity-filtered templates
7. **Directory `app/` vs artifactId `${rootArtifactId}-app`** — physical directory is `app/`, Maven coordinate uses `${rootArtifactId}-app` for uniqueness

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
| `papiflyfx-docking-archetype/src/test/resources/projects/basic/archetype.properties` | Archetype IT properties |
| `papiflyfx-docking-archetype/src/test/resources/projects/basic/goal.txt` | Archetype IT goal |

### Files Modified

| File | Change |
|------|--------|
| `pom.xml` (root) | Added `papiflyfx-docking-bom` (first module) and `papiflyfx-docking-archetype` (last module) to `<modules>` |
| `README.md` (root) | Added BOM usage, archetype quick-start, and module entries for BOM/archetype |
| `papiflyfx-docking-archetype/README.md` | Fixed directory references from `my-app-app` to `app` |
