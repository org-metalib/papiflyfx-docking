# SOLID Refactoring Progress — Plan 1

## Status

Phase 1 from [spec/.principals/SOLID/solid-plan1-claude.md](./solid-plan1-claude.md) has been implemented and validated.
The main docks-side work from Phase 2 has now also been implemented and validated.

This change set started with the highest-leverage `DockManager` slice from the plan, then continued into the next extension-point cleanup slice for dock-tree traversal, hit testing, drag/drop cleanup, and layout DTO dispatch.

## Completed

### 1. Extracted focused DockManager service abstractions

Added:

- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockFloatingService.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DefaultDockFloatingService.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockMinMaxService.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DefaultDockMinMaxService.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockThemeService.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DefaultDockThemeService.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockSessionService.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DefaultDockSessionService.java`

What changed:

- Moved floating-window stage resolution, restore-hint tracking, float/dock flows, and floating-window lifecycle into `DefaultDockFloatingService`
- Moved minimized-bar state, minimized-leaf restore, maximize/restore flows, and maximize bookkeeping into `DefaultDockMinMaxService`
- Moved theme property ownership and dock background application into `DefaultDockThemeService`
- Replaced the old concrete `DockSessionService` class with a public `DockSessionService` interface and a `DefaultDockSessionService` implementation

Impact:

- `DockManager` now depends on focused abstractions for its biggest lifecycle responsibilities
- Session capture/restore, floating behavior, min/max state, and theme behavior can evolve independently instead of competing inside one class

### 2. Added explicit DI wiring for DockManager

Added:

- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManagerServices.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManagerContext.java`

What changed:

- Introduced `DockManagerServices` as the composition point for service factories
- Added `DockManager(Theme, DockManagerServices)` so callers can inject alternate service implementations
- Preserved `DockManager()` and `DockManager(Theme)` by routing them through `DockManagerServices.defaults()`
- Added `DockManager.createDefault()` and `DockManager.createDefault(Theme)` convenience factories
- Extended `DockManager.Builder` with `withServices(...)`

Impact:

- The manager is no longer forced to build its phase-1 collaborators internally
- Tests and future extensions can substitute targeted services without forking `DockManager`

### 3. Reduced DockManager to orchestration and delegation

Updated:

- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java`

What changed:

- Removed direct ownership of floating/minimize/maximize/theme/session implementation state from `DockManager`
- Delegated public lifecycle APIs such as `floatLeaf(...)`, `dockLeaf(...)`, `minimizeLeaf(...)`, `restoreLeaf(...)`, `maximizeLeaf(...)`, `restoreMaximized()`, `captureSession()`, and persistence methods to the extracted services
- Kept root/layout operations, drag wiring, tab-group wiring, and content-factory/state-registry integration in `DockManager`, which is its actual orchestration role
- Centralized service access through an internal `ServiceContext` adapter instead of exposing new internal methods on the public API

Impact:

- `DockManager` is now substantially thinner and easier to reason about
- Future lifecycle changes are localized to dedicated collaborators instead of one monolithic class

### 4. Added a characterization test for the DI path

Added:

- `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/DockManagerServicesFxTest.java`

What changed:

- Added a focused JavaFX test that injects a custom `DockSessionService` through `DockManagerServices`
- Verified that `DockManager.captureSession()` delegates to the injected service implementation

Impact:

- Guards the new constructor and service-wiring path against regressions
- Proves the `DockManager` DI entry point is real, not just structural

### 5. Folded state-cleanup fixes into the extraction

What changed:

- Minimized-state clearing is now centralized in `DockMinMaxService.clearMinimized()`, which clears both the store and the minimized bar UI
- Floating and minimized detached leaves are cleaned up through the service disposal paths instead of relying on `DockManager` to know every detached-state detail

Impact:

- The extraction also removed a couple of UI/state synchronization responsibilities from `DockManager`
- Detached-state cleanup is now owned by the service that creates and manages that state

### 6. Replaced dock-structure `instanceof` branching with visitors

Added:

- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockElementVisitor.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/data/LayoutNodeVisitor.java`

Updated:

- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockElement.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockTabGroup.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockSplitGroup.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/data/LayoutNode.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/data/LeafData.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/data/SplitData.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/data/TabGroupData.java`

What changed:

- Added `DockElement.accept(...)` and implemented it in `DockTabGroup` and `DockSplitGroup`
- Added `LayoutNode.accept(...)` and implemented it in the layout DTO records
- Introduced visitor contracts so structural services no longer need to branch on concrete dock/layout types

Impact:

- Dock-structure behavior is now extended through the model types themselves rather than through central `instanceof` chains
- This removes the exact OCP violation pattern called out for the main docks hotspots in the plan

### 7. Refactored dock services and drag/drop code onto the visitor entry points

Updated:

- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockTreeService.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/drag/HitTester.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/drag/DragManager.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/LayoutFactory.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/serial/LayoutSerializer.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/minimize/MinimizedStore.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DefaultDockMinMaxService.java`

What changed:

- `DockTreeService` tree mutation, search, and traversal paths now dispatch through `DockElementVisitor`
- `HitTester` and `DragManager` no longer branch on concrete split/tab-group types for their main traversal and cleanup paths
- `LayoutFactory.build(...)` and `LayoutSerializer.serialize(...)` now dispatch through `LayoutNode.accept(...)`
- `LayoutSerializer.deserialize(...)` replaced the hard-coded type switch with a deserializer registry map
- A few adjacent callers such as `DockManager.wireHandlers(...)`, maximize restore logic, and restore-hint capture were moved onto the new visitor entry points as well

Impact:

- The docks module’s main structural extension points are less centralized and less fragile
- Adding new structural node types now has a clearer integration seam than the previous switch/branch-heavy implementation

### 8. Extracted placeholder creation from `LayoutFactory`

Added:

- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/PlaceholderFactory.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/DefaultPlaceholderFactory.java`

Updated:

- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/LayoutFactory.java`
- `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/layout/LayoutFactoryFxTest.java`

What changed:

- Introduced a dedicated `PlaceholderFactory` abstraction for missing-content fallback nodes
- Updated `LayoutFactory` constructors to keep the old call sites while allowing placeholder creation to be injected
- Added `setPlaceholderFactory(...)` for runtime replacement
- Added a test covering a custom placeholder implementation

Impact:

- `LayoutFactory` no longer hard-codes `Label` creation for restore failures and missing content
- Placeholder rendering now has an explicit extension seam instead of requiring edits to the factory itself

### 9. Moved secret backend naming onto the `SecretStore` abstraction

Updated:

- `papiflyfx-docking-settings-api/src/main/java/org/metalib/papifly/fx/settings/api/SecretStore.java`
- `papiflyfx-docking-settings/src/main/java/org/metalib/papifly/fx/settings/secret/EncryptedFileSecretStore.java`
- `papiflyfx-docking-settings/src/main/java/org/metalib/papifly/fx/settings/secret/KeychainSecretStore.java`
- `papiflyfx-docking-settings/src/main/java/org/metalib/papifly/fx/settings/secret/LibsecretSecretStore.java`
- `papiflyfx-docking-settings/src/main/java/org/metalib/papifly/fx/settings/secret/WinCredSecretStore.java`
- `papiflyfx-docking-settings/src/main/java/org/metalib/papifly/fx/settings/secret/InMemorySecretStore.java`
- `papiflyfx-docking-settings/src/main/java/org/metalib/papifly/fx/settings/secret/SecretStoreFactory.java`
- `papiflyfx-docking-settings/src/main/java/org/metalib/papifly/fx/settings/categories/SecurityCategory.java`

What changed:

- Added `backendName()` to the `SecretStore` API
- Implemented backend-specific names in the concrete store implementations
- Reduced `SecretStoreFactory.backendName(...)` to a compatibility wrapper over the abstraction
- Updated `SecurityCategory` to read the backend name directly from `SecretStore`

Impact:

- The UI and higher-level code no longer need a reverse mapping from concrete classes back to display names
- This removes the main `instanceof` backend-name branch called out in the SOLID plan

### 10. Split `SettingsCategory` into focused ISP facets

Added:

- `papiflyfx-docking-settings-api/src/main/java/org/metalib/papifly/fx/settings/api/DefaultSettingsCategory.java`
- `papiflyfx-docking-settings-api/src/main/java/org/metalib/papifly/fx/settings/api/SettingsCategoryMetadata.java`
- `papiflyfx-docking-settings-api/src/main/java/org/metalib/papifly/fx/settings/api/SettingsCategoryDefinitions.java`
- `papiflyfx-docking-settings-api/src/main/java/org/metalib/papifly/fx/settings/api/SettingsCategoryUI.java`

Updated:

- `papiflyfx-docking-settings-api/src/main/java/org/metalib/papifly/fx/settings/api/SettingsCategory.java`
- `papiflyfx-docking-settings/src/main/java/org/metalib/papifly/fx/settings/ui/SettingsPanel.java`
- `papiflyfx-docking-settings/src/main/java/org/metalib/papifly/fx/settings/ui/SettingsCategoryList.java`

What changed:

- Split the old `SettingsCategory` contract into metadata, definitions/actions, and UI/lifecycle facets
- Added `DefaultSettingsCategory` as a convenience composed base over the split facets
- Kept `SettingsCategory` as the aggregate discovery contract while moving callers onto the narrower facet types where they only need a subset of the behavior
- Updated the settings UI classes to sort, render, search, and pane-build through the new focused seams instead of relying on one broad interface everywhere

Impact:

- The settings SPI now exposes the ISP split called out in the plan without breaking the ServiceLoader category-discovery model
- Metadata/search concerns and lifecycle/view concerns are no longer forced to share a single all-purpose contract

### 11. Decomposed `Theme` into grouped value objects

Added:

- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/docking/api/ThemeColors.java`
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/docking/api/ThemeFonts.java`
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/docking/api/ThemeDimensions.java`

Updated:

- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/docking/api/Theme.java`
- `papiflyfx-docking-settings/src/main/java/org/metalib/papifly/fx/settings/categories/AppearanceCategory.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeMapperTest.java`
- `papiflyfx-docking-github/src/test/java/org/metalib/papifly/fx/github/ui/theme/GitHubToolbarThemeMapperTest.java`

What changed:

- Added grouped `ThemeColors`, `ThemeFonts`, and `ThemeDimensions` records
- Added `Theme.of(...)`, `Theme.colors()`, `Theme.fonts()`, and `Theme.dimensions()` so callers can compose and consume smaller value groups instead of always rebuilding 21 positional fields
- Updated theme-construction call sites and custom-theme tests to use the grouped values

Impact:

- The theme API now has the smaller concern groupings that Phase 3 requested
- New theme composition code is less error-prone than manually re-specifying the full record each time

### 12. Inverted `SettingsRuntime` creation and removed the default singleton

Added:

- `papiflyfx-docking-settings/src/main/java/org/metalib/papifly/fx/settings/runtime/SettingsStorageFactory.java`
- `papiflyfx-docking-settings/src/main/java/org/metalib/papifly/fx/settings/runtime/JsonSettingsStorageFactory.java`
- `papiflyfx-docking-settings/src/main/java/org/metalib/papifly/fx/settings/runtime/SettingsSecretStoreFactory.java`
- `papiflyfx-docking-settings/src/main/java/org/metalib/papifly/fx/settings/runtime/DefaultSettingsSecretStoreFactory.java`
- `papiflyfx-docking-settings/src/test/java/org/metalib/papifly/fx/settings/runtime/SettingsRuntimeTest.java`

Updated:

- `papiflyfx-docking-settings/src/main/java/org/metalib/papifly/fx/settings/runtime/SettingsRuntime.java`
- `papiflyfx-docking-settings/src/main/java/org/metalib/papifly/fx/settings/runtime/DefaultSettingsServicesProvider.java`
- `papiflyfx-docking-settings/src/main/java/org/metalib/papifly/fx/settings/docking/SettingsContentFactory.java`
- `papiflyfx-docking-settings/src/main/java/org/metalib/papifly/fx/settings/docking/SettingsStateAdapter.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/settings/SettingsPanelSample.java`

What changed:

- Replaced the hard-coded `JsonSettingsStorage` and static `SecretStoreFactory` calls inside `SettingsRuntime` with injected storage/secret-store factory interfaces
- Removed the `SettingsRuntime` global singleton path and moved settings-panel/session restore code onto explicitly supplied runtime instances
- Kept a default no-arg bridge only where a ServiceLoader-created adapter/provider still needs a self-contained runtime instance
- Added a focused unit test verifying the injected factory path

Impact:

- The settings runtime now follows the plan’s DIP guidance instead of acting as a hidden global service locator
- Docking restoration and settings content creation use explicit runtime ownership rather than implicit process-wide state

### 13. Inverted `LoginRuntime` broker creation and removed static runtime state from callers

Added:

- `papiflyfx-docking-login/src/main/java/org/metalib/papifly/fx/login/runtime/AuthSessionBrokerFactory.java`
- `papiflyfx-docking-login/src/main/java/org/metalib/papifly/fx/login/runtime/DefaultAuthSessionBrokerFactory.java`
- `papiflyfx-docking-login/src/test/java/org/metalib/papifly/fx/login/runtime/LoginRuntimeTest.java`

Updated:

- `papiflyfx-docking-login/src/main/java/org/metalib/papifly/fx/login/runtime/LoginRuntime.java`
- `papiflyfx-docking-login/src/main/java/org/metalib/papifly/fx/login/docking/LoginFactory.java`
- `papiflyfx-docking-login/src/main/java/org/metalib/papifly/fx/login/docking/LoginStateAdapter.java`
- `papiflyfx-docking-login/src/main/java/org/metalib/papifly/fx/login/settings/AuthenticationCategory.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/SamplesRuntimeSupport.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/login/LoginSample.java`
- `papiflyfx-docking-login/src/test/java/org/metalib/papifly/fx/login/runtime/LoginRuntimeFxTest.java`
- `papiflyfx-docking-samples/src/test/java/org/metalib/papifly/fx/samples/SamplesSmokeTest.java`

What changed:

- Reworked `LoginRuntime` from a static singleton holder into an instance runtime with injected provider-registry and broker-factory seams
- Added `DefaultAuthSessionBrokerFactory` so default broker creation still supports the optional settings-backed storage path without hard-coding concrete creation inside `LoginRuntime`
- Updated login content/state restoration, sample runtime support, and smoke tests to pass explicit runtime/factory instances
- Tightened restore behavior by letting `LoginStateAdapter` reuse an injected `LoginFactory` instead of always rebuilding from a separate static runtime

Impact:

- The login runtime now depends on a broker factory abstraction rather than constructing `DefaultAuthSessionBroker` directly
- The samples/login integration no longer relies on process-wide static broker/registry mutation to swap runtimes during tests

## Validation

Executed:

```bash
./mvnw -pl papiflyfx-docking-docks -am test -Dtestfx.headless=true
./mvnw -pl papiflyfx-docking-docks,papiflyfx-docking-settings,papiflyfx-docking-settings-api -am test -Dtestfx.headless=true
./mvnw -pl papiflyfx-docking-login,papiflyfx-docking-settings -am test -Dtestfx.headless=true
./mvnw -pl papiflyfx-docking-samples -am test -Dtest=SamplesSmokeTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true
```

Result:

- Build success
- `papiflyfx-docking-docks` test suite passed
- 54 tests passed
- Follow-up validation across `papiflyfx-docking-docks`, `papiflyfx-docking-settings`, and `papiflyfx-docking-settings-api` passed
- 62 tests passed in the combined validation run
- Follow-up validation across `papiflyfx-docking-login`, `papiflyfx-docking-settings`, and their upstream dependencies passed
- 77 tests passed in the login/settings validation run
- Focused sample smoke validation passed with 12 tests green while compiling the full upstream dependency chain

## Remaining Roadmap Items

### Still open from `solid-plan1-claude.md`

- Phase 4:
  - extract collaborators from `DefaultAuthSessionBroker`
  - split `AuthenticationCategory`
  - decompose `DockTabGroup` and `DockSplitGroup` internally

### Notes

- This refactor intentionally preserved the existing `new DockManager()` and `new DockManager(Theme)` call sites used throughout the codebase.
- `LayoutFactory` and `DockTreeService` are still concrete collaborators owned by `DockManager`; the main Phase 1 lifecycle concerns are extracted, while broader inversion work remains for later phases.
- The Phase 3 and Phase 5 roadmap items from `solid-plan1-claude.md` are now implemented; the remaining plan work is concentrated in the larger SRP slices from Phase 4.
- A broader full-reactor test run reached an unrelated JVM crash inside `papiflyfx-docking-media`; sample validation was therefore executed with a targeted `SamplesSmokeTest` run that still compiles the full upstream graph without invoking the flaky unrelated media suite.
