# Plan — GitHub And Hugo Ribbon Samples

**Priority:** P2 (Normal)  
**Planning Lead:** @spec-steward  
**Implementation Lead:** @ops-engineer  
**Required Reviewers:** @feature-dev, @ui-ux-designer, @qa-engineer, @spec-steward  
**Workflow:** plan -> implementation -> validation

## Goal

Add two first-class SamplesApp demos that showcase the feature-module ribbon providers independently:

1. `GitHub Ribbon`
2. `Hugo Ribbon`

Each demo should mount a `RibbonDockHost` over a representative `DockManager` layout, expose the correct typed ribbon action capability from active content, and make the provider-specific ribbon tabs visible without requiring a real GitHub checkout, network access, or a locally installed Hugo CLI.

## Current Baseline

The SamplesApp catalog already includes:

1. `Docks -> Ribbon Shell`, which uses `RibbonDockHost` and ServiceLoader-discovered ribbon providers.
2. `Hugo -> Hugo Preview`, which demonstrates Hugo preview content in a dock but does not mount the ribbon shell.
3. `GitHub -> GitHub Toolbar`, which demonstrates the standalone GitHub toolbar but does not mount the ribbon shell.

The existing ribbon providers are already available through ServiceLoader:

1. `GitHubRibbonProvider` contributes a `GitHub` tab and resolves `GitHubRibbonActions` from `RibbonContext#capability(...)`.
2. `HugoRibbonProvider` contributes a global `Hugo` tab and a contextual `Hugo Editor` tab. It resolves `HugoRibbonActions` from the ribbon context and uses content metadata/title heuristics to decide when `Hugo Editor` is visible.

`DockManager` builds ribbon context from the active `DockLeaf` and registers the active content node under its concrete class. A sample content node can therefore expose provider actions by implementing `GitHubRibbonActions` or `HugoRibbonActions` directly.

## Scope

### In Scope

1. Add a GitHub ribbon sample scene under `papiflyfx-docking-samples`.
2. Add a Hugo ribbon sample scene under `papiflyfx-docking-samples`.
3. Register both samples in `SampleCatalog` so they appear as SamplesApp navigation entries.
4. Use lightweight in-memory sample action implementations that are deterministic in headless tests.
5. Validate that the expected ribbon tabs and representative commands render in SamplesApp.
6. Add or extend sample tests so both demos load and expose their provider tabs.

### Out Of Scope

1. Changing ribbon provider public APIs or SPI contracts.
2. Replacing the existing `GitHub Toolbar` or `Hugo Preview` demos.
3. Executing real Git operations, real GitHub API calls, browser launches, or real Hugo CLI commands from the samples.
4. Redesigning ribbon visuals beyond any small sample-specific layout polish needed for a complete demo.
5. Persisting sample action logs across app restarts.

## Impacted Modules

| Module | Planned responsibility |
| --- | --- |
| `papiflyfx-docking-samples` | new sample scene classes, catalog entries, deterministic sample actions, smoke/integration tests |
| `papiflyfx-docking-github` | no implementation change expected; existing provider/action contract is consumed |
| `papiflyfx-docking-hugo` | no implementation change expected; existing provider/action contract is consumed |
| `papiflyfx-docking-docks` | no implementation change expected; existing `RibbonDockHost` and context propagation are consumed |
| `spec/**` | planning/progress/validation artifacts |

## Key Invariants

1. Samples must use existing extension points: `RibbonProvider`, `RibbonDockHost`, `RibbonContext#capability(...)`, and `DockManager` active-content context propagation.
2. Feature samples must not require external credentials, network calls, mutable local repositories, or local Hugo installation.
3. The existing `Ribbon Shell` sample should remain a generic multi-provider discovery demo.
4. The new demos should be explicit product examples that make the GitHub and Hugo ribbon capabilities obvious from the SamplesApp navigation tree.
5. Existing theme binding and disposal behavior must keep working when users switch samples.
6. The plan does not authorize public API changes. If implementation discovers an API gap, pause and update the spec before changing contracts.

## Proposed Implementation

### Phase 1 — Add GitHub Ribbon Sample

1. Create a new `SampleScene` in `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/github/`, tentatively `GitHubRibbonSample`.
2. Build a `DockManager` with at least one active leaf whose content node implements `GitHubRibbonActions`.
3. Use a small programmatic content pane that shows repository status, current branch, pending changes, and an action log.
4. Implement `GitHubRibbonActions` as deterministic sample behavior:
   - capability checks return `true` for demo-safe commands such as pull, push, fetch, new branch, pull request, issues, commit, stage, and discard
   - merge/rebase may remain disabled if the UI should communicate "coming soon"
   - command methods append visible log messages and update simple counters/state where useful
5. Mount the dock layout inside `RibbonDockHost`.
6. Prefer an explicit `RibbonManager` with the discovered provider set unless implementation needs to isolate the sample to only `GitHubRibbonProvider`. If the sample is isolated, document why in the class-level Javadoc or a test name.
7. Bind the sample `DockManager` theme to the SamplesApp theme property and set the owner stage.

### Phase 2 — Add Hugo Ribbon Sample

1. Create a new `SampleScene` in `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/hugo/`, tentatively `HugoRibbonSample`.
2. Build a `DockManager` with a Hugo preview leaf and a markdown editor-like leaf.
3. Ensure the active content exposes `HugoRibbonActions`, either by:
   - making the active content node implement `HugoRibbonActions`, or
   - wrapping display content in a small sample node that implements the action interface.
4. Set leaf metadata so `HugoRibbonProvider` can show both expected tabs:
   - global `Hugo` tab should always appear
   - contextual `Hugo Editor` tab should appear when the active leaf is the Hugo preview factory id or a markdown content path/title that matches the provider heuristics
5. Implement actions as deterministic UI state changes:
   - `toggleServer()` flips a running flag and updates a status label
   - `newContent(...)`, `build()`, `mod(...)`, `env()`, `frontMatterTemplate()`, and `insertShortcode(...)` append visible log messages
6. Avoid starting `hugo server`, creating persistent content, opening external links, or depending on WebView network state.
7. Mount the dock layout inside `RibbonDockHost` and bind theme/owner stage consistently with the GitHub sample.

### Phase 3 — Catalog And Navigation

1. Register `GitHubRibbonSample` near `GitHubToolbarSample` in `SampleCatalog`.
2. Register `HugoRibbonSample` near `HugoPreviewSample` in `SampleCatalog`.
3. Use titles exactly as requested:
   - `GitHub Ribbon`
   - `Hugo Ribbon`
4. Keep existing sample titles unchanged so current smoke tests and navigation shortcuts remain valid.

### Phase 4 — Regression Coverage

1. Extend `SamplesSmokeTest` coverage implicitly through catalog registration and add focused assertions for the new titles.
2. Add a focused TestFX integration test for the new samples, or extend the existing ribbon sample integration test if that keeps the fixture compact.
3. Verify the GitHub sample:
   - `GitHub` tab is visible
   - representative commands such as `Pull`, `Push`, `New Branch`, and `Pull Request` are visible or discoverable in the ribbon
   - firing at least one command updates the sample log without throwing
4. Verify the Hugo sample:
   - `Hugo` tab is visible
   - `Hugo Editor` tab is visible when the active Hugo/markdown leaf is selected
   - firing `Server` or another deterministic command updates the sample status/log without invoking external processes
5. Keep tests geometry-tolerant. Assert provider tabs, command labels, and sample state changes rather than pixel-perfect layout.

## Acceptance Criteria

- [ ] SamplesApp navigation includes `GitHub -> GitHub Ribbon`.
- [ ] SamplesApp navigation includes `Hugo -> Hugo Ribbon`.
- [ ] Opening `GitHub Ribbon` displays a ribbon host with the `GitHub` provider tab and sample-safe action handling.
- [ ] Opening `Hugo Ribbon` displays a ribbon host with the `Hugo` provider tab and a contextual `Hugo Editor` tab for the active Hugo/markdown content.
- [ ] Both demos work without credentials, network access, real repository mutation, or Hugo CLI installation.
- [ ] Both demos bind to the global SamplesApp light/dark theme property.
- [ ] Sample disposal remains compatible with `SamplesApp#disposeContentArea()`.
- [ ] Headless tests cover catalog registration and representative provider-tab visibility.

## Validation Strategy

1. Compile the affected sample surface:
   - `./mvnw -pl papiflyfx-docking-samples -am compile`
2. Run focused sample tests:
   - `./mvnw -pl papiflyfx-docking-samples -am -Dtest=SamplesSmokeTest -Dtestfx.headless=true test`
   - `./mvnw -pl papiflyfx-docking-samples -am -Dtest=*Ribbon*FxTest -Dtestfx.headless=true test`
3. If new tests are not named by the wildcard above, run the exact new test class with `-Dtest=<ClassName>`.
4. Manually verify in SamplesApp:
   - launch `./mvnw javafx:run -pl papiflyfx-docking-samples`
   - open `GitHub -> GitHub Ribbon`
   - open `Hugo -> Hugo Ribbon`
   - toggle light/dark mode
   - click one representative ribbon command in each demo and confirm visible feedback

## Risks And Mitigations

| Risk | Mitigation |
| --- | --- |
| ServiceLoader discovery brings all providers into the dedicated demos, making the samples less focused | Prefer active-content context and tab selection to demonstrate the target provider; use an explicit provider list only if the UI becomes confusing |
| Provider commands are disabled because active content does not expose typed actions | Make the sample content node implement `GitHubRibbonActions` or `HugoRibbonActions` directly and validate through a focused test |
| Hugo contextual tab does not appear for the intended leaf | Set `contentFactoryId`, `LeafContentData`, title, and content id to match `HugoRibbonProvider` visibility heuristics |
| Tests become brittle around adaptive ribbon layout | Assert visible tabs/commands and action effects, not exact coordinates or sizes |
| Sample action methods accidentally call real integrations | Keep sample action classes private to `papiflyfx-docking-samples` and do not reuse production toolbar or process managers for command execution |

## Handoff Notes

Lead Agent: `@ops-engineer`  
Task Scope: add SamplesApp demos for GitHub and Hugo ribbon providers using deterministic sample content and action capabilities  
Impacted Modules: `papiflyfx-docking-samples`, `spec/**`  
Files Changed: `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-4/plan.md`  
Key Invariants: no public API changes, no external credentials/network/Hugo CLI dependency, preserve existing sample entries, use typed ribbon capabilities  
Validation Performed: source inspection and plan creation only  
Open Risks / Follow-ups: implementation should confirm whether explicit provider isolation is needed for focused demos  
Required Reviewer: `@spec-steward`
