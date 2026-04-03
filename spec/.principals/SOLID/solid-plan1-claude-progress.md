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

## Validation

Executed:

```bash
./mvnw -pl papiflyfx-docking-docks -am test -Dtestfx.headless=true
./mvnw -pl papiflyfx-docking-docks,papiflyfx-docking-settings,papiflyfx-docking-settings-api -am test -Dtestfx.headless=true
```

Result:

- Build success
- `papiflyfx-docking-docks` test suite passed
- 54 tests passed
- Follow-up validation across `papiflyfx-docking-docks`, `papiflyfx-docking-settings`, and `papiflyfx-docking-settings-api` passed
- 62 tests passed in the combined validation run

## Remaining Roadmap Items

### Still open from `solid-plan1-claude.md`

- Phase 3:
  - split fat interfaces such as `SettingsCategory`
  - decompose the `Theme` record into smaller concerns
- Phase 4:
  - extract collaborators from `DefaultAuthSessionBroker`
  - split `AuthenticationCategory`
  - decompose `DockTabGroup` and `DockSplitGroup` internally
- Phase 5:
  - invert runtime dependencies in `SettingsRuntime`
  - invert runtime dependencies in `LoginRuntime`
  - extract placeholder construction from `LayoutFactory`

### Notes

- This refactor intentionally preserved the existing `new DockManager()` and `new DockManager(Theme)` call sites used throughout the codebase.
- `LayoutFactory` and `DockTreeService` are still concrete collaborators owned by `DockManager`; the main Phase 1 lifecycle concerns are extracted, while broader inversion work remains for later phases.
- The docks-side visitor slice is complete enough to remove the targeted `DockElement`/`LayoutNode` branch hotspots, but broader non-docks SOLID work remains across the login and settings modules.
