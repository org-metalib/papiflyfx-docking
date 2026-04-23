# Ribbon 5 Review — Core Architect Perspective

**Priority:** P1 (High)  
**Lead Agent:** `@core-architect`  
**Required Reviewers:** `@spec-steward`, `@qa-engineer`  
**Workflow:** review-only; emit findings into the `Findings` section at the bottom of this file.

## Goal

Audit the ribbon API/SPI, runtime orchestration, and session format against the iteration 1 (`ribbon-1`) and iteration 2 (`ribbon-2`) invariants and the repository's SOLID principles. Report architectural risks and contract gaps that should shape the next iteration.

## Scope

### In scope

1. `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/` public contracts.
2. `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/` runtime.
3. `DockManager` ribbon context plumbing (`trackRibbonContext`, `syncRibbonContextFromTree`, `resolveActiveRibbonLeaf`, `buildRibbonContext`).
4. Session extension hook (`RibbonSessionCodec`, `RibbonSessionStateContributor`) as it interacts with `DockSessionService`/`DockSessionExtensionCodec`.

### Out of scope

1. Feature-module provider implementations (go in `review-feature-dev.md`).
2. Build, packaging, samples, dependency hygiene (go in `review-ops-engineer.md`).
3. Visual/theme concerns (go in `review-ui-ux-designer.md`).
4. Test harness changes (go in `review-qa-engineer.md`).

## Review Questions

### A. API/SPI shape (`papiflyfx-docking-api`)

1. Does `PapiflyCommand` honor Interface Segregation? It currently exposes id, label, tooltip, icons, enabled/selected state, and execution — confirm each field is needed by every provider and that no UI type leaks.
2. Are `BoolState`/`MutableBoolState` the right level of abstraction, or are they a minimal reinvention of `javafx.beans.property.BooleanProperty` that duplicates concerns (invalidation, binding semantics, thread affinity)?
3. Does `RibbonContext` expose a sound extension model via `attribute(...)` and `capability(...)`? Are attribute keys typed, collision-safe, and discoverable? Who owns the canonical `RibbonContextAttributes` vocabulary?
4. Are the spec records (`RibbonTabSpec`, `RibbonGroupSpec`, `RibbonButtonSpec`, `RibbonToggleSpec`, `RibbonSplitButtonSpec`, `RibbonMenuSpec`) actually value types (immutable, no behavior), or do they carry hidden mutable state through commands and state props?
5. Is `RibbonIconHandle` the right abstraction compared to the shared UI primitives in `org.metalib.papifly.fx.ui` (see `UiStyleSupport`, `UiCommonStyles`)? Is there duplication with `RibbonIconLoader`?
6. Does `RibbonProvider` document ordering, stability, and exception semantics for `ServiceLoader` discovery? What happens if two providers claim the same tab id?

### B. Canonical command identity and refresh lifecycle

1. `CommandRegistry` (196 LOC) promises single-instance command identity. Confirm:
   - That the registry is the only source of truth and is always consulted before a node is built (see `RibbonManager.refresh()` and `RibbonGroup.resolveControlNode(...)`).
   - That post-materialization pruning does not evict commands still referenced by the QAT.
   - That command reference equality is preserved across context changes that keep the command id stable.
2. `RibbonManager.refresh()` runs a two-phase pass (canonicalize → materialize). Verify that providers cannot defeat canonicalization by returning fresh `MutableBoolState` instances on every call; if they can, the UI may bind to stale state. `GitHubRibbonProvider` allocates state per call — confirm this is safe or codify the contract.
3. Is `refresh()` idempotent, re-entrant, and FX-thread-only? Are there documented preconditions?
4. What is the fallback behavior when a `RibbonProvider#getTabs(...)` throws? The research hypothesized logging only. Confirm that one failing provider cannot take down the ribbon for all others and that the failure surface is observable (telemetry or log).

### C. DockManager integration

1. `trackRibbonContext(...)` wires listeners on `activeTabIndexProperty`. Confirm listeners are removed on detach and do not leak across floating-window lifecycle transitions.
2. `syncRibbonContextFromTree()` is called on every active-leaf change, mouse press, minimize/maximize, and float/dock. Is synchronous refresh acceptable, or should we debounce/coalesce on the FX pulse?
3. `resolveActiveRibbonLeaf(...)` — does it correctly handle floating windows, minimized leaves, and maximized leaves? Cross-check the semantics against `DockState` and the `FloatingWindowManager` contract.
4. Does `DockManager` register the active content node under its concrete class for capability lookup (per `ribbon-2` plan)? Confirm the capability type is the concrete class and that feature modules do not need to re-declare interfaces in a central registry.

### D. Session format and persistence

1. Persistence schema documented in `ribbon-1/README.md`:

    ```json
    {
      "type": "dockSession",
      "version": 2,
      "layout": { },
      "ribbon": {
        "minimized": true,
        "selectedTabId": "hugo-editor",
        "quickAccessCommandIds": ["github.fetch", "hugo.preview"]
      }
    }
    ```

    Verify `RibbonSessionCodec` still emits exactly this shape and that the serializer rejects or filters unknown values safely.
2. Confirm that `DockSessionStateContributor` is the only hook used by ribbon persistence, and that no ribbon state leaks into `LayoutNode`.
3. Ensure `RibbonManager.getQuickAccessCommandIds()` is treated as the source of truth per `CLAUDE.md`; confirm there is no reliance on the derived `getQuickAccessCommands()` for persistence.
4. Check restore tolerance for:
   - unknown/missing tab id,
   - unknown/missing QAT command ids,
   - future fields (forward compatibility),
   - legacy sessions without a `ribbon` block.

### E. SOLID audit

1. **Single Responsibility:** is `RibbonManager` (523 LOC) one responsibility or several (provider discovery, tab merging, canonicalization, QAT state, refresh scheduling)? Propose a split if responsibilities blur.
2. **Single Responsibility:** same question for `Ribbon` (438 LOC) — it renders the strip, holds adaptive-layout logic, and owns QAT composition.
3. **Open/Closed:** can a new control type be added without editing `RibbonControlFactory` (410 LOC)? Current design uses sealed spec records and a factory switch. Assess whether a visitor or strategy pattern is warranted.
4. **Liskov Substitution:** do all `RibbonProvider` implementations honor the same contract around tab visibility, command equality, and null handling?
5. **Interface Segregation:** does `RibbonContext` force consumers to depend on `attribute(...)` when they only need `capability(...)` (or vice versa)?
6. **Dependency Inversion:** does `papiflyfx-docking-docks` ribbon runtime depend on API abstractions only, or does `RibbonControlFactory` leak into anything feature modules can see?

### F. Extensibility surface for iteration 3+

1. What would a KeyTips implementation cost given the current shape? Does the API need to reserve a `commandKeyTip` slot now to avoid a later breaking change?
2. What would a gallery control cost? Is there a way to add it without widening `RibbonControlSpec`?
3. Can we support per-user ribbon customization without changing the session schema? If not, note the breaking-change boundary explicitly.
4. Is there a path for command i18n (localized labels/tooltips) without every provider implementing its own pipeline?

## Review Procedure

1. Open the API files listed above and take notes on each review question, citing file and line.
2. Diff the current implementation against the `ribbon-2` plan's invariants (see `spec/papiflyfx-docking-docks/2026-04-20-0-ribbon-2/plan.md` key invariants).
3. Confirm the current code honors the `CLAUDE.md` "Ribbon Integration Pattern" guidance (especially canonical command identity and QAT ID source of truth).
4. For each risk, record: severity (P0–P3), affected file/line, suggested owner, suggested remediation cost (S/M/L).

## Deliverable

Populate the `Findings` section below in the form:

```md
### F-<NN>: <short title>
**Severity:** P0|P1|P2|P3  
**Area:** <API | CommandRegistry | RibbonManager | DockManager integration | Session | SOLID | Extensibility>  
**Evidence:** <file:line citations or method references>  
**Risk:** <what could go wrong and when>  
**Suggested follow-up:** <lead role, rough cost S/M/L>
```

Prioritize findings that block iteration 3 ergonomics (customization UI, KeyTips, galleries, localization) or that put session compatibility at risk.

## Validation

No automated validation is required for this review. However, when a finding recommends a behavior change, cite the test that currently covers (or fails to cover) the behavior.

## Findings

<!-- Populate during review. Keep one entry per distinct finding. -->

### F-01: Providers defeat command canonicalization by re-emitting `MutableBoolState` per call
**Severity:** P1
**Area:** CommandRegistry
**Evidence:**
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ribbon/GitHubRibbonProvider.java:124-136` — `command(...)` builds `new MutableBoolState(enabled)` on every `getTabs(context)` invocation, with `enabled` computed from `canRun.test(actions)` for the current context.
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/CommandRegistry.java:71-79` — `canonicalize(...)` keeps the *first* instance for a given id and discards subsequent instances verbatim.
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonManager.java:436-440` — `canonicalizeAndTrack(...)` feeds everything through the registry, so the `enabled`/`selected` BoolState produced on the *second* provider call is silently dropped.

**Risk:** Once a command id is first canonicalized, providers can never push a new enabled/selected state by re-emitting a spec. The ribbon appears to be reactive but in practice only reflects the first context. Any provider that does not *mutate* the canonical `BoolState` (vs. allocating a new one) will ship a broken enablement surface as soon as the first GitHub/Hugo capability becomes available. The contract is documented at `CommandRegistry:30-33` ("mutate the enabled / selected fields on the existing command rather than emitting a new instance") but is not enforced and the shipped provider violates it.

**Suggested follow-up:** `@core-architect` + `@feature-dev`, cost **M**. Either (a) teach `CommandRegistry` to project the incoming command's boolean snapshots onto the canonical BoolState on every `canonicalize()` call, or (b) provide a small `ProviderCommands` helper (cached per id) that feature modules use instead of ad-hoc `new MutableBoolState(...)`. Either way, ship a contract test in `CommandRegistryTest` that fails when the second `canonicalize(...)` call would change the surfaced enabled value.

### F-02: `JavaFxCommandBindings.readOnly` / `bidirectional` leak listeners on canonical `BoolState`
**Severity:** P1
**Area:** CommandRegistry / RibbonManager (command lifecycle)
**Evidence:**
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/JavaFxCommandBindings.java:43-49, 59-87` — both factories register a `BoolState.Listener` and never expose a detach hook.
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonControlFactory.java:107-108, 147-148, 168-169, 228` — every control/menu-item creation calls these factories to bind `disableProperty()`/`selectedProperty()`.
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonGroup.java:201-217` — `configureLauncher(...)` rebinds `launcherButton.disableProperty()` from `refreshPresentationMetadata()` on *every* `updateSpec(newSpec)` call, even when the spec is structurally identical.
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/MutableBoolState.java:17-51` — the canonical state stores all listeners in a `CopyOnWriteArrayList` and only releases them via `removeListener`.

**Risk:** Because commands are deliberately long-lived (canonical identity is a ribbon-2 invariant), each orphaned listener keeps a `ReadOnlyBooleanWrapper`/`SimpleBooleanProperty` (and the button it was bound to) reachable for the full lifetime of the canonical BoolState. Under frequent context/refresh churn — particularly launcher rebinding on every `RibbonGroup.updateSpec` — this is an unbounded growth of listeners and retained scene nodes, and a silent performance cliff. Also defeats the "cache hit" intent of ribbon-2 telemetry because listeners keep piling up even when nodes are cached.

**Suggested follow-up:** `@core-architect`, cost **S**. Return a `Subscription`/`AutoCloseable` from `JavaFxCommandBindings.readOnly/bidirectional` and make call sites (RibbonGroup launcher, RibbonControlFactory) store the subscription on the owning node so it can be released when the node is evicted or rebound. Add a focused unit test that counts `MutableBoolState.listenersSize()` across N refresh cycles with stable commands and asserts it stays bounded.

### F-03: Session format documented in README does not match the persisted shape
**Severity:** P1
**Area:** Session
**Evidence:**
- `spec/papiflyfx-docking-docks/2026-04-19-0-ribbon/README.md` (snippet copied into the current review at `review-core-architect.md:58-71`) shows the ribbon payload at the *top level* under `"ribbon": {...}`.
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonSessionStateContributor.java:11` — declares `EXTENSION_NAMESPACE = "ribbon"`.
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonSessionCodec.java:16` — `EXTENSION_PATH = "extensions.ribbon"`.
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/serial/DockSessionSerializer.java:34, 111-113, 189-195` — extensions are serialized under the top-level `"extensions"` object.

**Risk:** The documented schema advertises `{ "type": "dockSession", "version": 2, "ribbon": {...} }` but the codec actually writes `{ "type": "dockSession", "version": 2, "extensions": { "ribbon": {...} } }`. Anyone onboarding against the spec will build readers against the wrong shape; any external tooling or storage migration relying on the README is broken. This is a spec-vs-code drift, not a code bug, but it blocks iteration 3 ergonomics (customization export/import) until resolved.

**Suggested follow-up:** `@spec-steward`, cost **S**. Update ribbon-1/ribbon-2 session documentation to show the nested `extensions.ribbon.{...}` shape and cross-reference `DockSessionExtensionCodec`. Keep this finding tracked as a spec debt in the ribbon-5 consolidated follow-up, not a code change.

### F-04: No collision diagnostics when two providers claim the same tab id
**Severity:** P1
**Area:** API
**Evidence:**
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/RibbonProvider.java:16-44` — the SPI is silent on collisions.
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonManager.java:363-372, 466-494` — `mergeTabs(...)` and `TabAccumulator.merge(tab)` silently adopt the first tab's `label`/`order` and OR the `contextual` flag; contradictions are invisible.
- `spec/papiflyfx-docking-docks/2026-04-20-0-ribbon-2/plan.md:99` — acceptance criterion explicitly requires "Duplicate tab/group ID conflicts produce explicit diagnostics," but no such diagnostic exists.

**Risk:** Two providers can both ship a `"home"` tab with divergent labels/orders and the user sees only the first provider's presentation without any log or test failure. This is a latent fragility as the ribbon ecosystem grows, and a direct regression against ribbon-2's stated acceptance criteria.

**Suggested follow-up:** `@core-architect` + `@qa-engineer`, cost **S**. Emit a `WARNING` log (or structured telemetry event) when `TabAccumulator.merge(...)` receives a spec whose `label`/`order` differ from the accumulator's initial values. Document the first-wins policy in `RibbonTabSpec` javadoc and in `RibbonProvider`. Add a `RibbonManagerTest` that asserts the warning fires.

### F-05: `PapiflyCommand` violates Interface Segregation for push-button use cases
**Severity:** P2
**Area:** API / SOLID
**Evidence:**
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/PapiflyCommand.java:32-62` — the record always carries `selected` (defaulted to `MutableBoolState(false)` when null) even for commands that are conceptually pure actions and can never be toggled.

**Risk:** Every push-button command ships a live `MutableBoolState` that will collect listeners (see F-02) from `RibbonToggleSpec` wiring or future toggle-like UIs. The API surface suggests every command can be selected, which leaks a toggle concern into the base action contract. As iteration 3+ adds new control types (gallery/keytip), each will either ignore `selected` or reinvent state, deepening this ISP violation.

**Suggested follow-up:** `@core-architect`, cost **M**. Split `PapiflyCommand` into a minimal `Command` (id/label/tooltip/icons/enabled/action) plus a `ToggleCommand` subtype that adds `selected`. Keep `PapiflyCommand.of(id,label,action)` as a factory that returns the non-toggle variant. `RibbonToggleSpec` then takes the richer type. This is a breaking change; pair it with the ribbon-6 plan.

### F-06: `RibbonContextAttributes` is a bag of untyped string keys with no namespacing
**Severity:** P2
**Area:** API
**Evidence:**
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/RibbonContextAttributes.java:21-40` — flat `public static final String` keys with no ownership or typing.
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/RibbonContext.java:129-154` — attribute lookup is `Optional<Object>` with a runtime `Class<T>` narrowing overload.

**Risk:** Extension modules can silently collide (e.g., two modules using the key `"contentState"`). There is no typed attribute-key artifact, so IDE support is poor and refactor safety is low. `RibbonContextAttributes.ACTIVE_CONTENT_NODE` is marked for removal but still populated at `DockManager.java:940-942`, which tempts providers to keep casting instead of migrating to `capability(Class)`. This is also the natural home for KeyTips, localization, and customization metadata (iteration 3+) so the lack of a namespacing convention will bite us immediately.

**Suggested follow-up:** `@core-architect`, cost **M**. Introduce a typed `RibbonAttributeKey<T>` value type (id + declared `Class<T>`) and deprecate the raw-string constants in favor of generated keys. Document a namespacing rule (`<provider>.<attribute>`) in `RibbonContext` javadoc. Remove `ACTIVE_CONTENT_NODE` population from `DockManager.buildRibbonContext` in ribbon-6 now that `capability(Class)` is available.

### F-07: `RibbonManager` has too many responsibilities
**Severity:** P2
**Area:** SOLID / RibbonManager
**Evidence:**
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonManager.java` (523 LOC) owns, in one class: provider registration and ServiceLoader discovery (`discoverProviders`, L454-459), provider ordering (`PROVIDER_COMPARATOR`, L56-58), tab merging with `TabAccumulator`/`GroupAccumulator` (L466-523), command canonicalization and pruning (`canonicalizeTabs`/`canonicalizeControl`/`canonicalizeAndTrack`, L374-440), Quick Access Toolbar state (`quickAccessCommandIds`, `addQuickAccessCommand`, `refreshQuickAccessCommandView`, L72-228, L442-452), and refresh scheduling (`refresh()`, L325-345).

**Risk:** Future changes (e.g., debouncing refreshes per F-11, adding per-user customization per F-09, changing QAT semantics) will repeatedly touch this file and risk regressing unrelated concerns. The interaction of pruning + QAT + canonicalization is already subtle enough that documentation in `CommandRegistry.java:42-48` exists solely to warn callers. A split would also make targeted unit testing easier.

**Suggested follow-up:** `@core-architect`, cost **L**. Extract (1) `TabMerger` that owns `TabAccumulator`/`GroupAccumulator` + `mergeTabs`, (2) `RibbonCommandCanonicalizer` that owns `canonicalizeTabs`/`canonicalizeControl` and knows the `CommandRegistry`, (3) `QuickAccessState` that owns the id-list/view/pinning logic. Leave `RibbonManager` as the orchestrator that wires providers → merger → canonicalizer → tabs + QAT view. Schedule for ribbon-6.

### F-08: `RibbonControlSpec` sealed hierarchy forces central edits for new control types
**Severity:** P2
**Area:** SOLID (Open/Closed) / Extensibility
**Evidence:**
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/RibbonControlSpec.java:9-18` — sealed interface with an explicit `permits` list.
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonManager.java:408-434` — exhaustive `switch` on control subtypes.
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonControlFactory.java:46-53` — another exhaustive `switch`.
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonGroup.java:468-504` — a third `switch`, inside `ControlSignature.from(...)`.

**Risk:** Adding a gallery or keytip control (called out as iteration-3 goals in this review's §F2) is not just a new spec record — it requires changes in at least three switch sites and the `permits` clause in the API module. That is a coordinated breaking change across API + docks every time a new control type lands. Feature modules cannot contribute new control types without patching `papiflyfx-docking-docks`.

**Suggested follow-up:** `@core-architect`, cost **M**. Move toward a strategy/visitor pattern: `RibbonControlSpec` becomes a plain (non-sealed) interface with a `toRenderPlan(RibbonRenderContext)` or an `accept(RibbonControlVisitor)` method, and docks module exposes a visitor impl. Canonicalization and signature comparison each become visitors. Target for ribbon-6 so it can bake before adding galleries/keytips.

### F-09: Provider failures are logged but not observable to tests/telemetry
**Severity:** P2
**Area:** RibbonManager
**Evidence:**
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonManager.java:347-361` — `collectTabs(...)` catches `RuntimeException`, logs `Level.WARNING`, and swallows it.
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonLayoutTelemetry.java` (via repository file list) — telemetry API exists but is not hooked into provider failures.

**Risk:** One provider that throws under a specific context silently disappears from the ribbon. Integration tests cannot assert on that failure mode without parsing JUL output, and customers cannot detect a broken provider without inspecting logs. The failure surface is not part of the contract.

**Suggested follow-up:** `@core-architect` + `@qa-engineer`, cost **S**. Add `RibbonLayoutTelemetry#providerFailure(providerId, throwable)`, emit it from `collectTabs(...)`, and assert it in a new `RibbonManagerTest` that registers a deliberately-throwing provider. Update `RibbonProvider` javadoc to document the isolation + telemetry surface.

### F-10: `RibbonContext.capability(Class)` linear scan blurs host registration rules
**Severity:** P3
**Area:** API / DockManager integration
**Evidence:**
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/RibbonContext.java:172-184` — after a direct `capabilities.get(type)` miss, falls back to iterating every capability value and returning the first `type.isInstance(value)` match.
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:937-946` — the host registers the content node under its concrete class (`capabilities.put(contentNode.getClass(), contentNode)`) and also populates the deprecated `ACTIVE_CONTENT_NODE` attribute.

**Risk:** The capability lookup succeeds via the linear-scan fallback, which hides the fact that the host is registering by concrete class rather than by exported capability interface. A future host that registers two content nodes implementing `GitHubRibbonActions` (rare but legal if a provider nests content) gets non-deterministic first-match behavior. Providers also cannot easily tell whether a capability is "first-class" or "happened to fit" from the scan.

**Suggested follow-up:** `@core-architect`, cost **S**. Document the direct-lookup vs. scan fallback contract in the `capability(Class)` javadoc, or — better — change `DockManager.buildRibbonContext` to register capabilities under every interface the content node declares (walking the interface hierarchy once), so `get(type)` is always the hit path. Deprecate the linear-scan fallback in a later iteration.

### F-11: `syncRibbonContextFromTree` is invoked synchronously and unboundedly per FX pulse
**Severity:** P3
**Area:** DockManager integration
**Evidence:**
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:178, 228, 418, 691, 703, 713, 723, 733, 743, 754, 762` — `syncRibbonContextFromTree()` is called from every state transition (mouse press, minimize/maximize, float/dock, active-tab change, layout restore).
- `syncRibbonContextFromTree()` at `DockManager.java:858-861` calls `cleanupStaleRibbonContextListeners()` + `buildRibbonContext(...)` + `ribbonContext.set(...)`.
- That setter fires the `RibbonManager.contextProperty` listener at `RibbonManager.java:115`, which immediately calls `refresh()`.

**Risk:** Drag/drop and float/dock transitions can fire multiple of these events inside a single FX pulse, producing redundant `refresh()` cycles. Each cycle re-canonicalizes every command and re-materializes every tab spec (though node cache absorbs most of the cost). This compounds with F-02 (listener accumulation) and makes steady-state performance harder to reason about as more providers ship. There is no debouncing on the FX pulse.

**Suggested follow-up:** `@core-architect`, cost **M**. Coalesce `syncRibbonContextFromTree()` with a "request" pattern like `Ribbon.requestAdaptiveLayout()` (see `Ribbon.java:348-357`): set a flag, `Platform.runLater(...)` to drain it once per pulse. Cover with a new Fx test that drives the `DockManager` through a sequence of transitions in one pulse and asserts `refresh()` ran once (via telemetry).

### F-12: `RibbonTabSpec.visibleWhen` is dropped during canonicalization
**Severity:** P3
**Area:** RibbonManager
**Evidence:**
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonManager.java:396-403` — canonical tab is built with `ribbonContext -> true` as the predicate.
- Same in `TabAccumulator.toSpec()` at `RibbonManager.java:487-493`.

**Risk:** Anything that reads `manager.getTabs()` later (e.g., a future per-tab visibility rebind, or a sample app that wants to peek) sees a predicate that always returns true. This is fine today because `refresh()` already filtered, but it hides provider intent and makes it subtly unsafe to re-evaluate visibility without a full refresh. Consumers who expect the predicate to still match the provider contract will silently be wrong.

**Suggested follow-up:** `@core-architect`, cost **S**. Preserve the original `visibleWhen` on the canonical spec (or drop the predicate entirely from the spec that the manager publishes, since it serves no purpose post-materialization — make the value type explicit about that). Document either decision in `RibbonTabSpec`. Target for ribbon-6.

### F-13: `BoolState` duplicates `javafx.beans.property.BooleanProperty` with weaker guarantees
**Severity:** P3
**Area:** API
**Evidence:**
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/BoolState.java:1-93` — interface + `Listener` re-invents change notification.
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/api/ribbon/MutableBoolState.java:1-66` — a minimal implementation with a `CopyOnWriteArrayList`.
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/JavaFxCommandBindings.java:1-96` — a full bidirectional adapter layer just to project this back into JavaFX.

**Risk:** The abstraction cost is real (custom listener dispatch, no invalidation listeners, no binding composition) and the win is thin: the only non-JavaFX consumer in the current tree is the API module itself. Meanwhile the abstraction creates correctness pitfalls — see F-02 (listeners never removed), the silent `CopyOnWriteArrayList.addIfAbsent` dedup, and the fact that notifications are documented as "expected" on the UI thread but not enforced. Every new UI toolkit adapter will have to re-solve these questions.

**Suggested follow-up:** `@core-architect`, cost **M**. Either (a) explicitly justify the abstraction in the SPI javadoc with a concrete non-JavaFX use case, or (b) collapse it into a minimal `Observable<Boolean>` that is clearly ours (with `subscribe` returning an `AutoCloseable`, which also fixes F-02). Defer until a second UI toolkit is actually on the roadmap; note as a spec debt otherwise.

### F-14: Per-user customization path is not designed for; will require a schema bump
**Severity:** P2
**Area:** Extensibility / Session
**Evidence:**
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/data/RibbonSessionData.java` (referenced by `RibbonSessionCodec`) carries only `{ minimized, selectedTabId, quickAccessCommandIds }`.
- `RibbonSessionCodec.encode/decode` at `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonSessionCodec.java:19-40` hard-codes exactly those three fields.
- `RibbonManager` has no API for overriding provider-contributed order, visibility, or group composition.

**Risk:** Any iteration-3 "customize ribbon" feature (hide tab, reorder groups, pin/unpin commands beyond QAT) will require new fields under `extensions.ribbon.*`. The current codec rejects unknown fields in `optionalString/optionalStringList` (throws `IllegalArgumentException`), which is strict validation — so adding fields is *not* forward-compatible for clients that read with the current code. This is a breaking-change boundary today.

**Suggested follow-up:** `@core-architect` + `@spec-steward`, cost **M**. Before any customization work lands, (1) document the forward-compatibility policy (ignore-unknown vs. fail), and (2) relax the codec to log + skip unknown keys instead of throwing. The current "reject unknown value type" behavior is correct for malformed *known* fields but should not apply to unknown keys. Worth locking down in ribbon-6 design before customization work begins.

### Prioritization summary

- **Block iteration 3:** F-01 (provider state), F-02 (listener leak), F-03 (docs drift), F-04 (collision diagnostics), F-14 (customization path).
- **Put session compatibility at risk:** F-03, F-14.
- **SOLID debt that will slow feature work:** F-05, F-06, F-07, F-08.
- **Observability / lifecycle hygiene:** F-09, F-10, F-11, F-12, F-13.

## Handoff Snapshot

Lead Agent: `@core-architect`  
Task Scope: architectural review of the ribbon API, runtime, DockManager integration, and session format  
Impacted Modules: `spec/**` only  
Files Changed: this file (on completion)  
Key Invariants:

- no production code changes
- no API or session format changes
- findings must cite files and lines

Validation Performed: source inspection only  
Open Risks / Follow-ups: will be captured as numbered findings  
Required Reviewer: `@spec-steward`
