# Ribbon 5 Review — Build & Runtime Engineer Perspective

**Priority:** P2 (Normal)  
**Lead Agent:** `@ops-engineer`  
**Required Reviewers:** `@spec-steward`, `@qa-engineer`  
**Workflow:** review-only; emit findings into the `Findings` section at the bottom of this file.

## Goal

Assess the ribbon from a build, module-boundary, samples, and persistence-wiring perspective. The goal is to keep `./mvnw clean package` green in headless mode, keep the BOM publishable, keep samples representative, and keep settings-backed restore sound.

## Scope

### In scope

1. Root `pom.xml` and module POMs that depend on ribbon (`papiflyfx-docking-docks`, `papiflyfx-docking-github`, `papiflyfx-docking-hugo`, `papiflyfx-docking-samples`).
2. `papiflyfx-docking-bom` — confirm ribbon-related modules are included appropriately and that `papiflyfx-docking-samples` remains excluded.
3. `papiflyfx-docking-archetype` — confirm the archetype template exposes the ribbon SPI cleanly for generated apps and sets up `ServiceLoader` registration.
4. `papiflyfx-docking-samples` — ribbon-facing samples and the `SampleCatalog`.
5. `papiflyfx-docking-settings` and `papiflyfx-docking-settings-api` — any settings-backed ribbon preferences (QAT, minimized default, selected tab default).
6. Headless profile (`headless-tests`) behavior under `-Dtestfx.headless=true` for ribbon-heavy modules.

### Out of scope

1. Ribbon API/runtime internals (`@core-architect`).
2. CSS token work (`@ui-ux-designer`).
3. Test determinism in TestFX/Monocle (`@qa-engineer`).

## Review Questions

### A. Module boundaries and dependency direction

1. Confirm the declared dependency graph:
   - `papiflyfx-docking-docks` → `papiflyfx-docking-api` (ribbon SPI).
   - `papiflyfx-docking-github` → `papiflyfx-docking-api` only (no runtime dependency on docks)? Verify.
   - `papiflyfx-docking-hugo` → `papiflyfx-docking-api` only? Verify.
   - `papiflyfx-docking-samples` → all runtime modules as needed.
2. Does any feature-module provider accidentally import a `papiflyfx-docking-docks` ribbon class (e.g., `RibbonControlFactory`, `RibbonManager`)? That would break the ribbon-2 decoupling goal.
3. Is there a provided/runtime scope issue for `ServiceLoader` descriptors (`META-INF/services/org.metalib.papifly.fx.api.ribbon.RibbonProvider`)? Check every `src/main/resources/META-INF/services/` path.

### B. BOM and archetype surface

1. Does `papiflyfx-docking-bom/pom.xml` list the ribbon-relevant modules for downstream consumers? Note: samples must stay excluded (per `CLAUDE.md`).
2. Does the archetype under `papiflyfx-docking-archetype/` scaffold a `RibbonDockHost` and a sample provider for generated apps? If not, should it?
3. Archetype integration tests live under `papiflyfx-docking-archetype/src/test/resources/projects/`. Confirm any of them exercise ribbon wiring end-to-end.

### C. Sample coverage

1. Cross-check `SampleCatalog` entries for ribbon:
   - `Docks -> Ribbon Shell` (generic multi-provider demo).
   - `GitHub -> GitHub Toolbar` (existing, non-ribbon).
   - `Hugo -> Hugo Preview` (existing, non-ribbon).
   - `GitHub -> GitHub Ribbon` (planned in ribbon-4, not yet implemented).
   - `Hugo -> Hugo Ribbon` (planned in ribbon-4, not yet implemented).
2. Is the Ribbon Shell sample compatible with `SamplesRuntimeSupport.setSharedRuntime(...)` so settings/login restore does not race with ribbon restore?
3. Confirm that running `./mvnw javafx:run -pl papiflyfx-docking-samples` does not depend on network access to render the ribbon.
4. Is there a recommended default theme pairing for the Ribbon Shell demo (light/dark)? Coordinate with `review-ui-ux-designer.md` section A.

### D. Persistence wiring

1. Ribbon state lives under `ribbon` in the session payload (per `ribbon-1/README.md`). Confirm:
   - `RibbonSessionStateContributor` is registered exactly once per `DockManager` instance.
   - Duplicate registration is rejected or deduped (no silent double-capture).
2. QAT command ids are the source of truth. Confirm settings-backed defaults (if any) do not collide with persisted session values.
3. If a session is restored before `ContentStateRegistry` is set (developer error), does the ribbon fail closed without corrupting the session file? Cross-check `DockSessionService` wiring.

### E. Headless profile and JavaFX flags

1. Confirm each ribbon-touching module (`docks`, `github`, `hugo`, `samples`) ships `--enable-native-access`, `--add-exports`, `--add-opens` Surefire flags as needed. Look for ad-hoc additions that should move to the parent POM.
2. Does `-Dtestfx.headless=true` work reliably for all ribbon integration tests on Linux, macOS, and Windows CI? Any platform-specific skips?
3. Are Monocle font metrics stable enough that ribbon layout tests do not produce false positives on differing DPI? Coordinate with `review-qa-engineer.md` section B.

### F. Release readiness

1. If ribbon public APIs shipped in `0.0.15-SNAPSHOT`, confirm the `papiflyfx-docking-api` Javadoc is exported by the release plugin when `-Dgpg-sign.profile=yes` is active.
2. Is there a deprecation path for anything removed between ribbon-1 and ribbon-2? The ribbon-2 plan explicitly accepted breaking changes; confirm the CHANGELOG or release notes record that.
3. Is `papiflyfx-docking-docks` version aligned with the BOM and archetype? A mismatch would block release.

### G. Observability and diagnostics

1. `RibbonLayoutTelemetry` exists. Is it wired into anything operators can turn on (system property, JUL logger, settings flag)? Operators debugging a sluggish ribbon should have a knob.
2. Are logging categories per-provider or per-component, and are they documented?
3. Can a downstream app disable ribbon entirely at runtime without removing the dependency (e.g., for a minimal embedded use case)?

## Review Procedure

1. Inspect the POM tree with `./mvnw -pl papiflyfx-docking-docks,papiflyfx-docking-github,papiflyfx-docking-hugo,papiflyfx-docking-samples,papiflyfx-docking-bom -am help:effective-pom` (only if helpful; do not commit output).
2. Read the `META-INF/services` descriptors under every module that claims a provider.
3. Confirm sample catalog entries by reading `SampleCatalog` in `papiflyfx-docking-samples`.
4. For each review question, record observations with file/line citations and flag anything that would block a clean release or a clean archetype scaffold.

## Deliverable

Populate the `Findings` section below using the common template:

```md
### F-<NN>: <short title>
**Severity:** P0|P1|P2|P3  
**Area:** <Module graph | BOM/archetype | Samples | Persistence wiring | Headless profile | Release | Observability>  
**Evidence:** <file:line citations or mvn output excerpts>  
**Risk:** <what a release or downstream consumer hits>  
**Suggested follow-up:** <lead role, rough cost S/M/L>
```

## Validation

Optional dry-runs (only if helpful for confidence; they are not required for a review):

1. `./mvnw -pl papiflyfx-docking-samples -am -Dtest=SamplesSmokeTest -Dtestfx.headless=true test`
2. `./mvnw -pl papiflyfx-docking-bom,papiflyfx-docking-archetype -am -DskipTests clean install`

Do not commit output into this file; summarize the effect in the finding instead.

## Findings

_Not yet started._

## Handoff Snapshot

Lead Agent: `@ops-engineer`  
Task Scope: build/runtime review of the ribbon feature — module boundaries, BOM, archetype, samples, persistence wiring, headless profile, release  
Impacted Modules: `spec/**` only  
Files Changed: this file (on completion)  
Key Invariants:

- no production code changes
- no POM edits
- samples must remain excluded from the BOM
- findings must cite files and, where relevant, `mvn` behavior

Validation Performed: source inspection; optional dry-runs as recorded in findings  
Open Risks / Follow-ups: recorded as numbered findings  
Required Reviewer: `@spec-steward`, `@qa-engineer`
