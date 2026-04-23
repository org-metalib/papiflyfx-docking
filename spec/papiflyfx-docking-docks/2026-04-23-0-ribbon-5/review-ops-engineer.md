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

No P0/P1 release blockers found in the reviewed build/runtime surface.

Source-inspection notes:

- Module boundaries are mostly clean: `papiflyfx-docking-docks` depends on `papiflyfx-docking-api`; `papiflyfx-docking-github` and `papiflyfx-docking-hugo` depend on `papiflyfx-docking-api` plus `papiflyfx-docking-settings-api`, with `papiflyfx-docking-docks` only in test scope (`papiflyfx-docking-github/pom.xml:17`, `papiflyfx-docking-github/pom.xml:22`, `papiflyfx-docking-github/pom.xml:49`; `papiflyfx-docking-hugo/pom.xml:17`, `papiflyfx-docking-hugo/pom.xml:22`, `papiflyfx-docking-hugo/pom.xml:27`).
- Feature ribbon providers import the API ribbon package, not docks runtime classes (`papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ribbon/GitHubRibbonProvider.java:3`; `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProvider.java:3`).
- `ServiceLoader` descriptors exist for GitHub, Hugo, and sample ribbon providers (`papiflyfx-docking-github/src/main/resources/META-INF/services/org.metalib.papifly.fx.api.ribbon.RibbonProvider:1`; `papiflyfx-docking-hugo/src/main/resources/META-INF/services/org.metalib.papifly.fx.api.ribbon.RibbonProvider:1`; `papiflyfx-docking-samples/src/main/resources/META-INF/services/org.metalib.papifly.fx.api.ribbon.RibbonProvider:1`).
- The BOM includes ribbon-relevant published modules and continues to exclude samples (`papiflyfx-docking-bom/pom.xml:18`, `papiflyfx-docking-bom/pom.xml:23`, `papiflyfx-docking-bom/pom.xml:68`, `papiflyfx-docking-bom/pom.xml:73`).
- Samples now include `Ribbon Shell`, `Hugo Ribbon`, and `GitHub Ribbon` (`papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/catalog/SampleCatalog.java:56`, `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/catalog/SampleCatalog.java:70`, `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/catalog/SampleCatalog.java:72`), and `SamplesRuntimeSupport.initialize(...)` wires the shared settings runtime before the samples app builds content (`papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/SamplesApp.java:73`, `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/SamplesRuntimeSupport.java:26`).
- Ribbon session state is registered by `RibbonDockHost` under the `ribbon` namespace and duplicate namespaces fail fast (`papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonDockHost.java:40`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonDockHost.java:52`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonSessionStateContributor.java:11`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:598`).
- Validation: `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk use java 25.0.1.fx-zulu >/dev/null && ./mvnw -pl papiflyfx-docking-samples -am -Dtest=SamplesSmokeTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test` passed on 2026-04-23 with 13 tests, 0 failures, 0 errors, 0 skips.

### F-01: Archetype does not expose ribbon SPI wiring
**Severity:** P2
**Area:** BOM/archetype
**Evidence:** The generated app template still creates a bare `DockManager` scene (`papiflyfx-docking-archetype/src/main/resources/archetype-resources/__rootArtifactId__-app/src/main/java/App.java:16`, `papiflyfx-docking-archetype/src/main/resources/archetype-resources/__rootArtifactId__-app/src/main/java/App.java:20`). The generated app POM only pulls `papiflyfx-docking-docks` as the framework dependency (`papiflyfx-docking-archetype/src/main/resources/archetype-resources/__rootArtifactId__-app/pom.xml:15`, `papiflyfx-docking-archetype/src/main/resources/archetype-resources/__rootArtifactId__-app/pom.xml:19`), and the generated test only asserts startup (`papiflyfx-docking-archetype/src/main/resources/archetype-resources/__rootArtifactId__-app/src/test/java/AppTest.java:19`). `rg` found no `Ribbon`, `RibbonDockHost`, `RibbonProvider`, or `META-INF/services/org.metalib.papifly.fx.api.ribbon.RibbonProvider` path in `papiflyfx-docking-archetype/src/main/resources/archetype-resources`.
**Risk:** New downstream apps generated from the archetype do not demonstrate the newly published ribbon contribution path, so consumers can miss the required `ServiceLoader` descriptor and host wiring even though the API is in the BOM.
**Suggested follow-up:** `@ops-engineer` lead with `@core-architect` review, rough cost M. Add an optional/sample `RibbonDockHost` scaffold, a minimal generated `RibbonProvider`, descriptor registration, and an archetype integration assertion that the provider tab is visible.

### F-02: Surefire JavaFX/TestFX flags are duplicated across ribbon-touching modules
**Severity:** P3
**Area:** Headless profile
**Evidence:** Parent plugin management pins the Surefire version but does not centralize the shared JavaFX/TestFX `argLine` or system properties (`pom.xml:167`). Each ribbon-touching module repeats the same `--enable-native-access`, `--add-exports`, `--add-opens`, and TestFX system-property block with small local variations (`papiflyfx-docking-docks/pom.xml:55`, `papiflyfx-docking-github/pom.xml:87`, `papiflyfx-docking-hugo/pom.xml:76`, `papiflyfx-docking-samples/pom.xml:90`).
**Risk:** Headless behavior can drift by module as more ribbon/UI tests are added. A future JavaFX flag or Monocle property fix may land in one module but not the others, creating platform-specific CI failures.
**Suggested follow-up:** `@ops-engineer` lead with `@qa-engineer` review, rough cost S. Move common Surefire/TestFX configuration into parent plugin management or a shared Maven property, leaving only module-specific native-access additions such as `javafx.web` and `javafx.media` locally overridden.

### F-03: Ribbon layout telemetry is test-only and has no operator-facing switch
**Severity:** P3
**Area:** Observability
**Evidence:** `RibbonLayoutTelemetry` is package-private and documented as an internal test sink (`papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonLayoutTelemetry.java:3`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonLayoutTelemetry.java:7`). The ribbon defaults telemetry to noop (`papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/Ribbon.java:68`) and the setter is package-private (`papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/Ribbon.java:342`). The only direct caller found is the adaptive-layout FX test (`papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/RibbonAdaptiveLayoutFxTest.java:74`).
**Risk:** Operators and downstream app owners cannot enable layout/cache diagnostics when a ribbon becomes sluggish or collapses unexpectedly in production. Debugging requires a custom build or debugger instead of a documented runtime knob.
**Suggested follow-up:** `@core-architect` lead with `@ops-engineer` review, rough cost S. Add a JUL-backed or system-property-enabled telemetry adapter and document the logger/category or property in the docks README.

### F-04: Ribbon 2 breaking changes are documented in Javadocs but not in release notes
**Severity:** P3
**Area:** Release
**Evidence:** API Javadocs record the Ribbon 2 contract breaks (`papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/package-info.java:31`, `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/PapiflyCommand.java:14`), but `rg --files | rg 'CHANGELOG|RELEASE|release-notes|changes'` returned no repository changelog or release-notes file. The root README documentation section links the ribbon implementation dossier, not migration/release notes (`README.md:202`, `README.md:205`).
**Risk:** Consumers upgrading through published artifacts may not see breaking API notes unless they inspect Javadocs or internal spec progress. That weakens release readiness for the accepted ribbon-1 to ribbon-2 break.
**Suggested follow-up:** `@spec-steward` lead with `@ops-engineer` and `@core-architect` review, rough cost S. Add a concise release/migration note that lists the Ribbon 2 contract breaks and links the API Javadocs.

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
Open Risks / Follow-ups: F-01 archetype ribbon scaffold gap; F-02 duplicated Surefire/TestFX flags; F-03 telemetry not operator-facing; F-04 release notes gap
Required Reviewer: `@spec-steward`, `@qa-engineer`
