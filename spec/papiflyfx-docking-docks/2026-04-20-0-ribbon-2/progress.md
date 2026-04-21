# Progress — Docking Ribbon 2

**Status:** Phase 2 (runtime command architecture) implementation complete; sandbox build validation blocked, on-host validation pending
**Current Milestone:** Phase 2 — long-lived `CommandRegistry`, separated materialization/command-state lifecycle, ID-first QAT
**Priority:** P1 (High)
**Lead Agent:** @core-architect
**Required Reviewers:** @ui-ux-designer, @feature-dev, @qa-engineer, @ops-engineer, @spec-steward
**Compatibility Stance:** Compatibility is not a concern for implementation; deliberate breakage is allowed.

## Completion summary

- Research: 100%
- Planning: 100%
- Phase 1 — API contracts: 100% (code changes landed; on-host build validation still outstanding)
- Phase 2 — Runtime command architecture: 100% (code changes landed; on-host build validation outstanding)
- Phase 3 — Layout and rendering efficiency: 0%
- Phase 4 — Persistence extension generalization: 0%
- Phase 5 — Provider migration and test closure: 0%

## Accomplishments

- [2026-04-20] Created ribbon-2 research dossier with concept/design/implementation gap analysis.
- [2026-04-20] Created phased implementation plan with acceptance criteria and validation strategy.
- [2026-04-20] Established risks/mitigations and module ownership boundaries for implementation start.
- [2026-04-20] Recorded explicit non-compatibility policy for API/session/provider changes in ribbon-2 specs.
- [2026-04-20] Phase 1.1 — Introduced UI-neutral `BoolState` / `MutableBoolState` in `papiflyfx-docking-api`; refactored `PapiflyCommand` so `enabled`/`selected` are `BoolState` components (no JavaFX types in the public contract).
- [2026-04-20] Phase 1.1 — Added `JavaFxCommandBindings` adapter in `papiflyfx-docking-docks` to bridge `BoolState` to read-only `BooleanProperty` (for `disableProperty`) and to bidirectional `BooleanProperty` (for toggle `selectedProperty`) with FX-thread dispatch and a re-entrancy guard.
- [2026-04-20] Phase 1.2 — Added typed capability registry to `RibbonContext` (`Map<Class<?>, Object> capabilities`, `capability(Class)`, `withCapability(Class, T)`); wired `DockManager#buildRibbonContext` to publish the active content node under its concrete class.
- [2026-04-20] Phase 1.2 — Deprecated `RibbonContextAttributes.ACTIVE_CONTENT_NODE` for removal with migration note pointing providers to `RibbonContext.capability(Class)`. Transitional attribute still populated so existing providers compile.
- [2026-04-20] Phase 1.3 — Renamed `RibbonGroupSpec.reductionPriority` to `collapseOrder` with explicit Javadoc convention ("smaller values collapse earlier; recommended 10/20/30"). Updated runtime comparators (`RibbonManager`, `Ribbon.applyAdaptiveLayout`) and accumulator fields accordingly.
- [2026-04-20] Phase 1.4 — Documented intentional breakage: `papiflyfx-docking-api/…/package-info.java` call-out; Javadoc notes in `PapiflyCommand`, `RibbonContext`, and `RibbonContextAttributes`.
- [2026-04-20] Propagated compile fixes across touched consumers: `SampleRibbonProvider`, `GitHubRibbonProvider`, `HugoRibbonProvider` (switched from `SimpleBooleanProperty` to `MutableBoolState` and renamed reduction-priority constants to `COLLAPSE_LATE/MID/EARLY`). Updated provider tests to use `command.enabled()` instead of `command.enabledProperty()`.
- [2026-04-21] Phase 2.1 — Introduced `CommandRegistry` in `papiflyfx-docking-docks` as the long-lived owner of canonical `PapiflyCommand` identities. Supports `canonicalize`/`register` (first-wins semantics), `find`/`contains` lookup, `unregister`/`retain` pruning, and ordered `ids()` / `commands()` access. Not thread-safe by design — owned and mutated only by `RibbonManager` on the FX thread.
- [2026-04-21] Phase 2.2 — Refactored `RibbonManager#refresh()` to separate materialization from command-state ownership. The refresh cycle now (a) collects and merges provider tab contributions, (b) walks the merged tab/group/control tree and replaces each embedded `PapiflyCommand` with its canonical registry instance, (c) prunes the registry down to commands reachable through visible tabs plus QAT identifiers, and (d) publishes the canonicalized tabs and the derived QAT command view. Contextual tab recomputation no longer churns command instances, so UI bindings, shortcut wiring, and QAT selections remain stable across context changes.
- [2026-04-21] Phase 2.2 — `resolveCommandsById(List<String>)` now consults the registry directly instead of scanning the visible tab tree, which makes the lookup O(n) over the requested identifier list and tolerant of hidden contextual commands.
- [2026-04-21] Phase 2.3 — Converted the Quick Access Toolbar runtime state to ID-first: `RibbonManager#getQuickAccessCommandIds()` is the mutable primary `ObservableList<String>`, and `getQuickAccessCommands()` is a derived, unmodifiable `ObservableList<PapiflyCommand>` view resolved through the registry. Restore works when contextual tabs are hidden (ids persist but the command only renders when reachable); duplicates are preserved in the id list and deduplicated in the derived view; blank/unknown identifiers survive the id list but are skipped in the view.
- [2026-04-21] Phase 2.3 — Added `RibbonManager#addQuickAccessCommand(PapiflyCommand)` helper for host-owned QAT commands (application-level Save/Undo/Redo that are not contributed by any provider); the helper canonicalizes through the registry and appends the id if not already pinned.
- [2026-04-21] Phase 2.3 — Updated `Ribbon#captureSessionState()` to read QAT identifiers from `getQuickAccessCommandIds()` so hidden contextual commands survive round-trip; updated `Ribbon#restoreSessionState(RibbonSessionData)` to `setAll` onto the id list rather than the now-unmodifiable command view.
- [2026-04-21] Phase 2.4 — Added unit tests `CommandRegistryTest` (canonicalize first-wins, insertion order preservation, blank/unknown id lookup, `register` previous-value reporting, `unregister` + `retain` semantics).
- [2026-04-21] Phase 2.4 — Extended `RibbonManagerTest` with three Phase 2 invariants: canonicalization stability across refresh cycles (`assertSame` between registry lookup and the rendered button's command), pruning of commands no longer reachable (pinned contextual command survives a hiding context change then is evicted once unpinned and refresh re-runs), and the ID-first QAT derivation (duplicates preserved in id list, dedup'd + dropped-on-miss in derived view).
- [2026-04-21] Phase 2.4 — Added `RibbonCommandRegistryFxTest` (TestFX, headless) exercising command-identity stability across contextual refreshes, QAT capture/restore with hidden contextual commands (explicitly forcing registry eviction between capture and restore), and tolerance of completely unknown QAT identifiers. Test mounts bare `Ribbon` + `RibbonManager` (no `RibbonDockHost`) so the context property stays unbound and can be driven directly.
- [2026-04-21] Phase 2.4 — Updated `RibbonSessionPersistenceFxTest` (round-trip through `DockManager#saveSessionToString` / `restoreSessionFromString` behind the bound context property): round-trip assertion pinned the ids against the derived view; the "missing tab and command" case now asserts both that the id list retains the unresolved legacy id and that the derived command view only exposes the resolvable subset.
- [2026-04-21] Phase 2.5 — Updated `RibbonShellSample` to use `ribbonManager.addQuickAccessCommand(...)` in place of `getQuickAccessCommands().setAll(...)`, matching the new ID-first QAT contract. Confirmed via grep that no other consumer (`papiflyfx-docking-github`, `papiflyfx-docking-hugo`, `papiflyfx-docking-code`, `papiflyfx-docking-login`, `papiflyfx-docking-settings`, `papiflyfx-docking-tree`, `papiflyfx-docking-media`) mutates the QAT runtime API — they only implement `RibbonProvider`, which is unchanged.

## Intentional breakages (Ribbon 2)

### Phase 1
- `PapiflyCommand` component types changed: `BooleanProperty enabledProperty` / `BooleanProperty selectedProperty` → `BoolState enabled` / `BoolState selected`. The accessor methods are now `enabled()` / `selected()` (record accessors), not `enabledProperty()` / `selectedProperty()`.
- `RibbonContext` gained a 5th component (`Map<Class<?>, Object> capabilities`). Existing callers that construct the record directly now must pass the map (a 4-arg convenience constructor is provided to default it to empty).
- `RibbonContextAttributes.ACTIVE_CONTENT_NODE` is marked `@Deprecated(forRemoval = true)`. Providers must migrate to `RibbonContext.capability(SomeApi.class)`.
- `RibbonGroupSpec.reductionPriority` renamed to `collapseOrder`; accessor and constructor parameter name both changed.
- All JavaFX types removed from `papiflyfx-docking-api/.../ribbon/`. Downstream code that relied on JavaFX observables coming from the API must switch to `BoolState` listeners or use the runtime's `JavaFxCommandBindings` adapter (package-private in docks).

### Phase 2
- `RibbonManager#getQuickAccessCommands()` now returns an **unmodifiable** `ObservableList<PapiflyCommand>`. Code that previously called `.setAll(...)`, `.add(...)`, `.clear()`, etc. on the returned list will fail at runtime with `UnsupportedOperationException`.
- **Replacement API:** `RibbonManager#getQuickAccessCommandIds()` returns the mutable primary `ObservableList<String>`. Hosts manage QAT state by identifier; the command view is re-derived automatically.
- New helper `RibbonManager#addQuickAccessCommand(PapiflyCommand)` canonicalizes the supplied command in the registry and pins its id on the QAT. Use this for host-owned (non-provider) QAT entries.
- New public accessor `RibbonManager#getCommandRegistry()` exposes the long-lived `CommandRegistry`. Providers should not mutate the registry directly — all canonicalization happens inside `refresh()`.
- `RibbonSessionData#quickAccessCommandIds()` is now the authoritative serialized QAT payload; the persisted list may include identifiers that are not currently resolvable (hidden contextual or missing providers), and they will remain pinned after restore. Derived UI rendering only surfaces the resolvable subset.
- `Ribbon#captureSessionState()` now reads the id list, not the derived command view, so hidden contextual pins round-trip. Providers/tests that asserted the derived-view contents must switch to asserting the id list, then asserting the derived view for the reachable subset.
- `RibbonManager` imports no longer include the `collectCommandsById` / `collectControlCommands` helpers (removed). External callers that reached into these via reflection (none expected) will break.

## Validation status

### Phase 1
- Code-level review: complete.
- Build validation: **not yet executed on host**. Sandbox cannot run Java 25 + Maven Central (no network access to `repo.maven.apache.org`, only Java 11 available). Commands queued for on-host run:
  - `./mvnw -pl papiflyfx-docking-api,papiflyfx-docking-docks -am compile`
  - `./mvnw -pl papiflyfx-docking-api,papiflyfx-docking-docks -am -Dtestfx.headless=true test`
  - `./mvnw -pl papiflyfx-docking-github,papiflyfx-docking-hugo -am compile`

### Phase 2
- Code-level review: complete; all touched modules compile-consistent via static inspection (imports resolved, types match, record component names match).
- Build validation: **blocked in sandbox** (same Java 25 / Maven Central restrictions as Phase 1). Commands queued for on-host run:
  - `./mvnw -pl papiflyfx-docking-api,papiflyfx-docking-docks -am compile`
  - `./mvnw -pl papiflyfx-docking-api,papiflyfx-docking-docks -am -Dtestfx.headless=true test`
  - `./mvnw -pl papiflyfx-docking-samples -am -Dtestfx.headless=true compile`
  - After green: `./mvnw -pl papiflyfx-docking-github,papiflyfx-docking-hugo -am compile` (sanity — neither module touches the QAT runtime API, but ensures the `RibbonProvider` contract is unchanged).
- Test surface added in Phase 2:
  - `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/CommandRegistryTest.java` (7 unit tests)
  - `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/RibbonCommandRegistryFxTest.java` (3 TestFX cases, headless)
  - `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/RibbonManagerTest.java` (+3 cases: canonicalization stability, pruning, ID-first QAT derivation)
  - `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/RibbonSessionPersistenceFxTest.java` (existing cases reworked for id-list semantics; new assertion that unresolvable pins survive on the id list)

## Open risks

- `JavaFxCommandBindings` bridges `BoolState` to `BooleanProperty` via an internal `ReadOnlyBooleanWrapper`. The wrapper relies on `Platform.runLater` for off-FX-thread updates; if a provider mutates `BoolState` very frequently, this can create runLater churn. Mitigation: batched updates can be layered on once the command registry owns state transitions more broadly (Phase 3 candidate).
- `ACTIVE_CONTENT_NODE` still populated alongside the typed capability map for one transitional phase. If providers ignore the deprecation we carry the attribute longer than planned. Mitigation: Phase 5 migration gate, remove attribute then.
- Session schema unchanged in Phase 1; Phase 2 keeps the same `RibbonSessionData#quickAccessCommandIds` shape (list of ids) so persisted payloads still load. Phase 4 will introduce schema changes by design.
- Adaptive layout refactor deferred to Phase 3; semantic rename of `collapseOrder` should not alter observable layout behavior, but visual diffs should be spot-checked once tests run.
- **Phase 2 coupling to refresh cycles:** `commandRegistry.retain(reachable)` is invoked at the end of every `refresh()`. A pinned-but-unreachable command is only evicted when refresh re-runs *after* the QAT id was cleared. The new `commandRegistry_prunesCommandsNoLongerReachable` test pins this behavior; any future change that short-circuits refresh on identical tab specs would regress the eviction semantics.
- **Record reconstruction on canonicalize:** `canonicalizeTabs` rebuilds `RibbonTabSpec`, `RibbonGroupSpec`, and each `RibbonControlSpec` subtype on every refresh because records are immutable and commands must be replaced with canonical instances. If providers grow additional components (e.g., telemetry hooks, labels, adorners), every accessor must be threaded through the reconstruction paths in `RibbonManager#canonicalizeTabs` / `canonicalizeControl`. Mitigation: keep the reconstruction paths adjacent to spec evolution; consider a `withCommand(...)` builder on control specs in a follow-up.
- **Pre-existing `TabAccumulator` / `GroupAccumulator` double-merge:** both accumulators call `merge(initial)` from their constructors *and* the outer `computeIfAbsent(...).merge(contribution)` then merges the same contribution again. The current tests don't expose the duplication because they only assert group *ids*. This is independent of Phase 2 but is now adjacent to the canonicalization path — flagged as a follow-up for Phase 3 layout work.
- **Registry is not thread-safe.** All mutation happens through `RibbonManager` on the FX thread. If a future provider tries to trigger `refresh()` off-thread (e.g., via a background watcher) it must marshal onto the FX thread first. Document this invariant in Phase 5 provider-author guidance.
- **Sandbox validation blocker** persists (Java 25 not available, Maven Central not reachable). Phase 2 changes must be validated on the user's machine before handoff is considered closed.

## Next tasks

1. Run Phase 1 + Phase 2 validation on host: `./mvnw -pl papiflyfx-docking-api,papiflyfx-docking-docks -am compile` and `-am -Dtestfx.headless=true test`. If green, `./mvnw -pl papiflyfx-docking-samples,papiflyfx-docking-github,papiflyfx-docking-hugo -am compile`.
2. Phase 3 bootstrap — rendering efficiency: tab skeleton reuse (don't rebuild visible tab nodes when only contextual visibility changes), group-node pooling, and adaptive layout refactor using the renamed `collapseOrder`.
3. Phase 4 bootstrap — generalize `RibbonSessionStateContributor` payload into a typed extension map; design migration path for existing session payloads.
4. Begin provider migration off `ACTIVE_CONTENT_NODE` to typed capability access (Phase 5 seed; do not start until Phase 3 is underway).
5. Prepare regression test list for collision diagnostics, capability-based context, and new extension payload schema.

## Handoff snapshot — Phase 2

Lead Agent: `@core-architect`
Task Scope: Ribbon 2 Phase 2 — long-lived `CommandRegistry`, `RibbonManager` refresh split between materialization and command state, ID-first QAT with hidden-contextual-tab robustness, Phase 2 test surface.
Impacted Modules: `papiflyfx-docking-docks` (primary), `papiflyfx-docking-samples` (compile fix for new QAT API), `spec/papiflyfx-docking-docks/2026-04-20-0-ribbon-2/**`.
Unaffected (confirmed by grep): `papiflyfx-docking-api` (no shape changes in Phase 2), `papiflyfx-docking-github`, `papiflyfx-docking-hugo`, `papiflyfx-docking-code`, `papiflyfx-docking-login`, `papiflyfx-docking-settings`, `papiflyfx-docking-tree`, `papiflyfx-docking-media` — none mutate the QAT runtime API; all participate only through `RibbonProvider`.

Files Changed — Phase 2 runtime:
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/CommandRegistry.java` (new)
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/RibbonManager.java` (rewritten — command registry, canonicalizing refresh cycle, ID-first QAT plumbing)
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/ribbon/Ribbon.java` (session capture reads id list; restore writes id list)

Files Changed — Phase 2 tests:
- `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/CommandRegistryTest.java` (new)
- `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/RibbonCommandRegistryFxTest.java` (new)
- `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/RibbonManagerTest.java` (+3 invariants)
- `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/ribbon/RibbonSessionPersistenceFxTest.java` (id-list semantics + unresolvable-pin survival)

Files Changed — Phase 2 sample/consumer adaptations:
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/RibbonShellSample.java` (QAT setup via `addQuickAccessCommand`)

Files Changed — Spec:
- `spec/papiflyfx-docking-docks/2026-04-20-0-ribbon-2/progress.md` (this file)

New/changed invariants (Phase 2):
- Canonicalization: `RibbonManager.getCommandRegistry().find(id)` and every `PapiflyCommand` reachable through `RibbonManager.getTabs()` refer to the **same instance** for a given id across `refresh()` cycles until the command becomes unreachable (not pinned, not in any visible tab) — at which point the next refresh evicts it.
- Retention: a QAT-pinned id is always retained across refresh cycles even if no visible tab currently contributes the command; a provider that later re-contributes the command will re-canonicalize to that retained instance.
- QAT id-list semantics: `getQuickAccessCommandIds()` preserves insertion order and duplicates; the derived `getQuickAccessCommands()` view deduplicates by id and drops unresolvable ids. Both lists are `ObservableList`s — downstream bindings must choose whichever semantics they need.
- Session round-trip: serialized QAT payload is the id list, not the command list. Restore pins all ids even if the providers in the restore-time runtime don't currently contribute them.
- FX-thread invariant: `CommandRegistry` mutation happens only inside `RibbonManager.refresh()` (on the FX thread) and the `addQuickAccessCommand(...)` / `getQuickAccessCommandIds()` pathways. Providers must not reach into the registry directly.

Reviewer focus:
- `@ui-ux-designer` — confirm that QAT rendering under hidden contextual tabs matches design intent (hidden pins silently disappear from the visible toolbar and reappear when the context returns). Confirm toggle `selectedProperty` stability is preserved now that the underlying command instance survives across refreshes.
- `@feature-dev` — confirm `addQuickAccessCommand(PapiflyCommand)` is the right host-owned QAT API (vs. e.g., a mandatory "host QAT provider"). Confirm the canonicalization contract is clear enough that future providers don't expect per-refresh identity changes.
- `@qa-engineer` — re-run the new Phase 2 test surface headlessly; confirm `RibbonCommandRegistryFxTest#qatRestore_preservesHiddenContextualCommandIds` passes reliably (the registry eviction timing is the subtle piece here — the test forces a `refresh()` between clear and restore to make eviction observable). Add regression cases if context-change storms expose pruning pathologies.
- `@ops-engineer` — confirm no serialization-on-disk change (the id list is already what we wrote in Phase 1). No bundling or launch-script impact expected.
- `@spec-steward` — confirm the Phase 2 breakage list is complete and aligned with the non-compatibility stance. Flag if any public accessor change needs louder call-out in the package-info.

Required Reviewer: `@spec-steward`
