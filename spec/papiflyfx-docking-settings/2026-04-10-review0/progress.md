# Progress: PapiflyFX Settings Refactor (review0)

Current Milestone: **Phase 1: Runtime & Scope Safety**

- **Projected End Date:** 2026-04-17
- **Current Velocity:** Phase 1 completed in single session
- **Status:** [IN_PROGRESS — Phase 1 complete, awaiting review gates]

## Completion Summary
- **Overall Completion:** ~20%
- **Phase 1 (Runtime & Scope Safety):** 100%
- **Phase 2 (Composable UI Refactor):** 0%
- **Phase 3 (Security & Storage Hardening):** 0%
- **Phase 4 (State & Styling Cleanup):** 0%
- **Phase 5 (Documentation Sync):** 0%

## Recent Accomplishments
- [2026-04-10] Initialized `plan.md` and `progress.md` based on the unified total review plan.
- [2026-04-10] Status of the refactor plan updated to `accepted`.
- [2026-04-10] **B.1:** Refactored `SettingsStateAdapter` — replaced hidden default runtime with static `RuntimeHolder` pattern. No-arg ctor now fails if host has not called `setSharedRuntime()`.
- [2026-04-10] **B.2:** Refactored `DefaultSettingsServicesProvider` — same holder pattern. ServiceLoader consumers now receive the host-injected runtime.
- [2026-04-10] **B.3:** Updated `SamplesRuntimeSupport.initialize()` to call `setSharedRuntime()` on both holders before login runtime or ServiceLoader discovery. Login's `DefaultAuthSessionBrokerFactory` now receives the host runtime via the holder.
- [2026-04-10] **C.1:** Classified all 9 built-in categories by scope policy (Appearance=APP, Workspace=WS, Security=APP-fixed, Profiles=APP+WS, Network=APP, Shortcuts=APP, AI=APP, MCP=WS, Auth=APP).
- [2026-04-10] **C.2:** Added `supportedScopes()` default method to `SettingsCategoryDefinitions` API, deriving scopes from `definitions()`. Overridden in `SecurityCategory` and `ProfilesCategory` for custom scope behavior.
- [2026-04-10] **C.3:** Added `setSupportedScopes(Set<SettingScope>)` to `SettingsToolbar` with re-entrancy guard. `SettingsPanel.showCategory()` now dynamically updates toolbar scope options on category change. Scope selector is disabled when only one scope is available.
- [2026-04-10] All 8 settings module tests and all samples module tests pass (headless).

## Upcoming Tasks
- Phase 1 review gates: @core-architect, @auth-specialist, @ui-ux-designer, @qa-engineer, @spec-steward
- Phase 2 workstream A: Composable UI & Shared Validation

## Blockers & Risks
- **Risk:** Runtime bootstrap changes might break login or sample integration if not carefully implemented.
- **Mitigation:** ✅ Verified — `SamplesRuntimeSupport` registers the shared runtime before any consumer, and all integration tests pass.
- **Residual risk:** Third-party modules that discover `SettingsServicesProvider` or `SettingsStateAdapter` via ServiceLoader without calling `setSharedRuntime()` first will get a clear `IllegalStateException` (fail-closed by design).
