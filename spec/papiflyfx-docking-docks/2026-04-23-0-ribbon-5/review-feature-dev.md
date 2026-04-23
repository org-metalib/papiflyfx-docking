# Ribbon 5 Review — Feature Developer Perspective

**Priority:** P1 (High)  
**Lead Agent:** `@feature-dev`  
**Required Reviewers:** `@core-architect`, `@spec-steward`  
**Workflow:** review-only; emit findings into the `Findings` section at the bottom of this file.

## Goal

Evaluate the ribbon SPI from the perspective of a feature-module author (`code`, `tree`, `media`, `hugo`, `github`, or a new content module). Surface friction in provider onboarding, capability resolution, contextual-tab heuristics, command lifecycle, and parity between the built-in providers.

## Scope

### In scope

1. `GitHubRibbonProvider` in `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ribbon/`.
2. `HugoRibbonProvider` in `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/ribbon/`.
3. `SampleRibbonProvider` in `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/ribbon/`.
4. `GitHubRibbonActions` and `HugoRibbonActions` capability interfaces (locate them, confirm their module of residence and visibility, cite line numbers).
5. The pattern by which a `DockLeaf` content node exposes typed actions to `RibbonContext#capability(...)`.

### Out of scope

1. Ribbon API redesign (belongs to `@core-architect`).
2. Global CSS/theme work (belongs to `@ui-ux-designer`).
3. Sample catalog wiring (belongs to `@ops-engineer` under `review-ops-engineer.md`).

## Review Questions

### A. Provider onboarding ergonomics

1. How many lines of boilerplate does a trivial feature provider cost today? Compare `SampleRibbonProvider` (~150 LOC) and `GitHubRibbonProvider` (~150 LOC). Identify repeated patterns that could move into an API-level helper (e.g., `RibbonProviders.tab(...)`, `RibbonProviders.group(...)`).
2. Is `ServiceLoader` discovery documented well enough for a downstream consumer (archetype-generated app) to add a provider without reading `papiflyfx-docking-docks` source? Check the archetype template under `papiflyfx-docking-archetype/`.
3. Are icon-loading conventions (`RibbonIconLoader`, `RibbonIconHandle`) consistent across modules, or does each module reinvent its own icon path convention?
4. Can a feature module introduce a provider without depending on `papiflyfx-docking-docks` (runtime) — only on `papiflyfx-docking-api`? Confirm each existing provider's dependency list.

### B. Capability resolution contract

1. Typed capability resolution from `RibbonContext#capability(Class<T>)` replaced raw-node routing in iteration 2. Validate that:
   - The active content node is registered under **every** relevant capability type (not just its concrete class).
   - A capability interface defined in a feature module can be resolved even if the ribbon provider lives in a different module.
2. What happens when the active leaf's content implements the action interface, but is inside a floating window? Confirm floating-window leaves still participate in capability lookup.
3. What happens when the active leaf has no content yet (factory pending), or the content is a placeholder (e.g., error view)? Confirm provider commands degrade gracefully (disabled, not crashing).

### C. Contextual tab heuristics

1. `HugoRibbonProvider` reportedly uses five heuristics for its contextual `Hugo Editor` tab (factory id, content type key, file extension, path pattern, type key content). Enumerate each heuristic, cite the exact lines, and answer:
   - Are heuristics ordered deterministically?
   - Can heuristics conflict across providers (two providers claiming the same leaf)?
   - Are they documented anywhere outside the source?
2. Should contextual-tab activation move from heuristic matching to explicit context attributes set by the content module (e.g., `RibbonContextAttributes.CONTENT_KIND`)? Propose a migration path.
3. How does contextual-tab activation interact with QAT restore? If a QAT command lived on a contextual tab, is it retained when the tab becomes invisible? Cross-check with `review-core-architect.md` question D.3.

### D. Command state lifecycle

1. `GitHubRibbonProvider` allocates new `MutableBoolState` instances on each `getTabs(...)` call. Two risks:
   - If the UI binds to a stale instance, enablement updates are lost.
   - If providers are called on every `syncRibbonContextFromTree()`, allocations add pressure.
   
   Confirm the current behavior, cite the binding path in `RibbonControlFactory`, and decide whether the contract should force providers to cache state.
2. Is there a pattern for reactive enablement (e.g., repo became dirty, Hugo server started)? Today the only refresh path is a full `RibbonManager.refresh()`. Propose a finer-grained signal if needed.
3. How do providers signal "command is running" (busy state) vs "command is disabled"? Confirm the current `BoolState` pair (enabled + selected) is sufficient for typical feature flows.

### E. Parity across built-in providers

1. Walk each provider (`github`, `hugo`, `sample`) and record:
   - tabs contributed
   - groups per tab with collapse order
   - controls per group with small/medium/large modes
   - capability dependency (yes/no, type)
2. Flag any divergence that looks like drift rather than intentional:
   - inconsistent collapse-order constants
   - different naming conventions for command ids (`github.fetch` vs `hugo.preview` vs `sample.*`)
   - different tooltip styles

### F. New-content-module readiness

1. What would it cost to ship a ribbon contribution for `papiflyfx-docking-code`? Specifically:
   - Which code-editor commands belong on a ribbon?
   - What capability interface would `code` expose to `RibbonContext`?
   - What contextual-tab identifier/factory-id should the ribbon-2 heuristics honor?
2. Same question for `papiflyfx-docking-tree`.
3. Same question for `papiflyfx-docking-media`.
4. Identify the missing documentation (ideally in `papiflyfx-docking-docks/README.md` or a new `spec/.../ribbon-provider-authoring.md`) that would make these additions straightforward.

### G. Command id namespace hygiene

1. Is there a documented namespace convention (e.g., `<module>.<action>`)?
2. Are there collision risks if two modules register a command with the same id? `CommandRegistry` canonicalizes by id — confirm which wins and whether the loser is observable.
3. Should command ids become URI-like (`pf://github/fetch`) or stay flat strings? Record trade-offs.

## Review Procedure

1. Read each provider end-to-end.
2. Locate the capability action interfaces (`GitHubRibbonActions`, `HugoRibbonActions`) and note their module, package, visibility, and method list.
3. For each review question, record observations with file/line citations.
4. Draft a skeleton "new provider checklist" for feature modules that do not yet contribute a ribbon; do not commit it as a separate doc in this review, reference it in the finding instead.

## Deliverable

Populate the `Findings` section below using the common template:

```md
### F-<NN>: <short title>
**Severity:** P0|P1|P2|P3  
**Area:** <Provider boilerplate | Capability resolution | Contextual tabs | Command lifecycle | Parity | Readiness | Namespaces>  
**Evidence:** <file:line citations>  
**Risk:** <what a future feature module hits>  
**Suggested follow-up:** <lead role, rough cost S/M/L>
```

## Validation

No automated validation is required. For any finding about reactive enablement, cite the existing test (or test absence) in `papiflyfx-docking-github`, `papiflyfx-docking-hugo`, or `papiflyfx-docking-docks`.

## Findings

### F-01: Provider authoring is still copy-heavy and under-documented for generated apps
**Severity:** P2  
**Area:** Provider boilerplate  
**Evidence:** `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ribbon/GitHubRibbonProvider.java:19`, `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ribbon/GitHubRibbonProvider.java:40`, `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ribbon/GitHubRibbonProvider.java:116`, `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProvider.java:25`, `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProvider.java:46`, `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProvider.java:300`, `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/ribbon/SampleRibbonProvider.java:20`, `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/ribbon/SampleRibbonProvider.java:28`, `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/ribbon/SampleRibbonProvider.java:135`, `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/RibbonProvider.java:5`, `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/package-info.java:45`, `papiflyfx-docking-archetype/src/main/resources/archetype-resources/README.md:85`, `papiflyfx-docking-archetype/src/main/resources/archetype-resources/__rootArtifactId__-app/pom.xml:15`, `papiflyfx-docking-github/pom.xml:15`, `papiflyfx-docking-github/pom.xml:47`, `papiflyfx-docking-hugo/pom.xml:15`, `papiflyfx-docking-hugo/pom.xml:25`, `papiflyfx-docking-samples/pom.xml:15`  
**Risk:** A new content module pays roughly the same authoring cost as the current trivial providers: `SampleRibbonProvider` is 151 LOC, `GitHubRibbonProvider` is 149 LOC, and `HugoRibbonProvider` grows to 355 LOC. The repeated work is visible in provider id/order, tab/group constructors, command prefixing, icon wrapping, enabled-state construction, and action dispatch lambdas. Mainline feature providers can depend only on `papiflyfx-docking-api` plus their feature dependencies (`github` and `hugo` keep `papiflyfx-docking-docks` test-scoped), but the archetype-generated app only documents adding module dependencies and has no ribbon provider class, `META-INF/services/org.metalib.papifly.fx.api.ribbon.RibbonProvider` template, icon guidance, namespace guidance, or authoring checklist.  
**Suggested follow-up:** `@core-architect` + `@spec-steward`, M. Add API-level builders/helpers such as `RibbonProviders.tab(...)`, `RibbonProviders.group(...)`, and `RibbonCommands.action(...)`, plus a provider-authoring section or generated sample. New provider checklist: choose `<module>.ribbon.provider` id and order; create a public action interface in the feature API package; make the active content root implement it or expose it explicitly; keep command ids stable under `<module>.ribbon.<action>`; use `RibbonIconHandle.of("octicon:<name>")` or documented resource paths; register the provider in `META-INF/services`; add a no-capability disabled-state test, a capability-resolution test, and a contextual-visibility test.

### F-02: Canonical command identity freezes provider-computed enabled and selected state
**Severity:** P1  
**Area:** Command lifecycle  
**Evidence:** `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ribbon/GitHubRibbonProvider.java:125`, `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ribbon/GitHubRibbonProvider.java:133`, `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProvider.java:309`, `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProvider.java:316`, `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProvider.java:332`, `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProvider.java:339`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonManager.java:325`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonManager.java:436`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/CommandRegistry.java:71`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonControlFactory.java:167`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/JavaFxCommandBindings.java:43`, `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/RibbonManagerTest.java:136`, `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/RibbonCommandRegistryFxTest.java:54`  
**Risk:** The current refresh path calls providers repeatedly, providers allocate new `MutableBoolState` instances from the current capability snapshot, and `CommandRegistry#canonicalize` returns the first command instance for a command id without copying state from later emissions. The JavaFX controls bind to the canonical command's original `BoolState`, so a command first materialized without actions can stay disabled after a valid capability appears, and Hugo's server toggle can keep an old selected state after the server starts or stops. Existing tests prove identity stability and QAT restoration, but I did not find a regression that proves enabled/selected values update across context refreshes for GitHub, Hugo, or the docks runtime.  
**Suggested follow-up:** `@core-architect` lead with `@feature-dev` review, M. Define whether providers must cache state per command id or whether the registry should merge state from newly emitted specs into the canonical command. Add focused tests where a provider first emits `enabled=false`, then a later context emits `enabled=true`, and the rendered control updates without replacing the command id.

### F-03: Contextual tab activation relies on Hugo-specific heuristics instead of explicit content metadata
**Severity:** P2  
**Area:** Contextual tabs  
**Evidence:** `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProvider.java:63`, `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProvider.java:77`, `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProvider.java:81`, `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProvider.java:85`, `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProvider.java:89`, `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProvider.java:90`, `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProvider.java:91`, `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProvider.java:98`, `papiflyfx-docking-hugo/src/test/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProviderTest.java:69`, `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/RibbonCommandRegistryFxTest.java:72`  
**Risk:** The Hugo Editor tab is deterministic, but its claim rules are only source-level: active type key equals Hugo preview factory id, `CONTENT_FACTORY_ID` equals that id, markdown extension in content id or dock title, `/content/` in content id or dock title, and type key containing `markdown` or `hugo`. A code editor or tree module showing `content/post.md` could unintentionally activate the Hugo contextual tab, and a future provider could claim the same leaf with a different set of filename heuristics. QAT behavior itself is covered: contextual command ids survive while hidden and reappear when visible. The remaining risk is that the tab becomes visible for the wrong content because visibility is inferred rather than declared.  
**Suggested follow-up:** `@core-architect` lead with `@feature-dev` and `@spec-steward` review, M. Add explicit context attributes such as `RibbonContextAttributes.CONTENT_KIND` and `CONTENT_DOMAIN`, then migrate providers in two phases: emit attributes from content factories/state adapters or a content-context contributor hook, keep the current heuristics as fallback for one release, then narrow Hugo's fallback to Hugo-owned factory ids.

### F-04: Capability exposure is implicit and tied to the content root node
**Severity:** P2  
**Area:** Capability resolution  
**Evidence:** `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/api/GitHubRibbonActions.java:7`, `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/api/GitHubToolbar.java:66`, `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/api/HugoRibbonActions.java:6`, `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/api/HugoPreviewPane.java:42`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:937`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:938`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:945`, `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/RibbonContext.java:172`, `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/RibbonContext.java:178`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:971`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:980`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:1026`  
**Risk:** `GitHubRibbonActions` and `HugoRibbonActions` are public feature-module API interfaces, and the current lookup works when the active content root implements the interface because `RibbonContext#capability` scans values with `type.isInstance(...)`. However, `DockManager` only registers the content node under its concrete class, not under every exported action interface, and there is no explicit content-side hook to publish multiple capabilities or route actions from a nested controller. Floating windows are considered in focus resolution and validity checks, so floating leaves can participate once their floating tab group is tracked; the weak spot is authoring clarity and explicitness, not the current GitHub/Hugo happy path. Placeholder or factory-pending leaves degrade to missing capabilities, but the command-state freeze in F-02 can turn that graceful initial disabled state into a stale state.  
**Suggested follow-up:** `@core-architect` lead with `@feature-dev` review, M. Add a small explicit capability contribution contract, for example `RibbonCapabilityProvider#ribbonCapabilities()` on content roots or a `DockLeaf#setRibbonCapabilities(...)` API, and update docs to say whether root-node `implements ActionInterface` is sufficient or merely a fallback.

### F-05: Built-in providers do not expose a busy/running command state
**Severity:** P2  
**Area:** Command lifecycle  
**Evidence:** `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/PapiflyCommand.java:27`, `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/PapiflyCommand.java:38`, `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/PapiflyCommand.java:39`, `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/api/GitHubToolbar.java:81`, `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/api/GitHubToolbar.java:154`, `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/api/GitHubToolbar.java:186`, `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/api/GitHubToolbar.java:253`, `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/api/HugoPreviewPane.java:165`, `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/api/HugoPreviewPane.java:185`, `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProvider.java:104`  
**Risk:** `PapiflyCommand` only carries `enabled` and `selected`. GitHub has an internal busy indicator and disables some toolbar actions while busy, while Hugo has explicit server starting/running states, but the ribbon provider can only express disabled or selected. Feature modules that run long operations cannot show "running" separately from "not available", and toggles such as Hugo Server cannot distinguish "starting" from "running" in ribbon chrome.  
**Suggested follow-up:** `@core-architect` lead with `@ui-ux-designer` and `@feature-dev` review, S/M. Either document that busy belongs in feature-local content status only, or add a UI-neutral `CommandActivityState` / `busy` `BoolState` with JavaFX rendering support and tests.

### F-06: Code, tree, and media are ready for ribbon contributions but lack action contracts and contextual ids
**Severity:** P2  
**Area:** Readiness  
**Evidence:** `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorFactory.java:15`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:470`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:481`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:492`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:960`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:964`, `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/api/TreeViewFactory.java:8`, `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/api/TreeView.java:537`, `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/api/TreeView.java:548`, `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/api/TreeView.java:572`, `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/api/TreeView.java:579`, `papiflyfx-docking-media/src/main/java/org/metalib/papifly/fx/media/api/MediaViewerFactory.java:8`, `papiflyfx-docking-media/src/main/java/org/metalib/papifly/fx/media/api/MediaViewer.java:31`, `papiflyfx-docking-media/src/main/java/org/metalib/papifly/fx/media/api/MediaViewer.java:64`, `papiflyfx-docking-media/src/main/java/org/metalib/papifly/fx/media/api/MediaViewer.java:68`  
**Risk:** The likely first ribbon contributions are straightforward, but each module would currently invent its own action interface, provider shape, contextual-tab id, and tests. Suggested first pass: code exposes `CodeRibbonActions` for Find, Replace, Go To Line, Fold All, Unfold All, and maybe language/wrap toggles under contextual tab `code-editor`; tree exposes `TreeRibbonActions` for Find, next/previous result, close search, expand/collapse focused/all, and reveal selection under `tree-view`; media exposes `MediaRibbonActions` for Open/Load, zoom reset/in/out for images, play/pause/seek for audio/video, and copy/open source URL under `media-viewer`. Without a provider-authoring doc, these modules are likely to drift in ids, tests, and capability naming.  
**Suggested follow-up:** `@feature-dev` lead with `@core-architect` review, M. Create `spec/papiflyfx-docking-docks/ribbon-provider-authoring.md` or extend `papiflyfx-docking-docks/README.md` with a checklist, then implement one module first as the reference provider before cloning the pattern.

### F-07: Command id collision handling is first-wins and silent
**Severity:** P2  
**Area:** Namespaces  
**Evidence:** `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/RibbonProvider.java:12`, `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/package-info.java:52`, `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ribbon/GitHubRibbonProvider.java:128`, `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/ribbon/HugoRibbonProvider.java:311`, `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/ribbon/SampleRibbonProvider.java:136`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/CommandRegistry.java:71`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/CommandRegistry.java:73`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/CommandRegistry.java:151`, `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/CommandRegistryTest.java:18`  
**Risk:** Existing providers use flat, dotted ids (`github.ribbon.*`, `hugo.ribbon.*`, `samples.ribbon.*`), which is readable and adequate if documented as `<module>.ribbon.<action>`. The runtime canonicalizes by id and the first command wins; later collisions are not surfaced through `RibbonManager` because it uses `canonicalize`, not `register`, so a downstream module can silently lose a command if it reuses another module's id. URI-like ids (`pf://github/fetch`) would clarify ownership but are noisier in session JSON and would not solve collision observability by themselves.  
**Suggested follow-up:** `@core-architect` + `@spec-steward`, S. Keep dotted strings, document the namespace convention, and add warning/diagnostic support when `canonicalize` sees a different command object with an existing id from a different provider refresh path.

## Handoff Snapshot

Lead Agent: `@feature-dev`  
Task Scope: feature-module review of the ribbon SPI, capability resolution, contextual tab heuristics, and provider parity  
Impacted Modules: `spec/**` only  
Files Changed: this file (on completion)  
Key Invariants:

- no production code changes
- no API or session format changes
- findings must cite file/line in provider sources

Validation Performed: source inspection only  
Open Risks / Follow-ups: recorded as numbered findings  
Required Reviewer: `@core-architect`, `@spec-steward`
