# SOLID Refactoring Progress

## Status

In progress.

The first refactoring slice from `spec/.principals/SOLID/solid-plan.md` has been implemented and validated. This slice focused on the two highest-priority architectural problems:

1. `DockManager` carrying too many tree and session responsibilities
2. `AuthenticationCategory` depending on `DefaultAuthSessionBroker`

## Completed

### 1. Extracted dock tree responsibilities from `DockManager`

Added:

- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockTreeService.java`

What changed:

- Moved dock tree mutation and restore-hint placement logic out of `DockManager`
- Centralized:
  - element removal
  - leaf detaching
  - default insert behavior
  - restore-from-hint behavior
  - tree traversal for leaf collection

Impact:

- `DockManager` is now a thinner facade for tree operations
- tree mutation logic is isolated behind a focused collaborator

### 2. Extracted dock session responsibilities from `DockManager`

Added:

- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockSessionService.java`

What changed:

- Moved layout/session capture and restore orchestration out of `DockManager`
- Centralized:
  - layout capture
  - session capture
  - session restore
  - session JSON string/file persistence delegation
  - content-state refresh before capture
  - restore-hint DTO conversion

Impact:

- `DockManager` no longer owns serialization/session orchestration details directly
- session behavior can now evolve with less pressure on the public dock manager type

### 3. Reduced `DockManager` to delegation for extracted concerns

Updated:

- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java`

What changed:

- Added `DockTreeService` and `DockSessionService` collaborators
- Delegated:
  - `capture()`
  - `captureSession()`
  - `restoreSession(...)`
  - `saveSessionToString()`
  - `restoreSessionFromString(...)`
  - `saveSessionToFile(...)`
  - `loadSessionFromFile(...)`
  - tree mutation calls used by float/dock/minimize/maximize flows
- Kept the public API stable

Impact:

- Significant SRP improvement without breaking callers
- Lower future cost for extracting the remaining window lifecycle logic

### 4. Introduced a broker-admin capability to remove concrete login broker coupling

Added:

- `papiflyfx-docking-login/src/main/java/org/metalib/papifly/fx/login/api/AuthSessionAdmin.java`

Updated:

- `papiflyfx-docking-login/src/main/java/org/metalib/papifly/fx/login/core/DefaultAuthSessionBroker.java`

What changed:

- Added a focused administrative capability for session maintenance
- `DefaultAuthSessionBroker` now implements `AuthSessionAdmin`
- Marked `upsertSession(...)` and `removeSession(...)` as interface overrides

Impact:

- Replaced concrete-type dependency with a capability-based dependency
- Improved substitutability for `AuthSessionBroker` consumers

### 5. Refactored `AuthenticationCategory` away from concrete broker usage

Updated:

- `papiflyfx-docking-login/src/main/java/org/metalib/papifly/fx/login/settings/AuthenticationCategory.java`

What changed:

- Removed `DefaultAuthSessionBroker` dependency
- Added constructor-based broker injection through `Supplier<AuthSessionBroker>`
- Default constructor still integrates with `LoginRuntime` for current ServiceLoader wiring
- Session-draft and session-removal behavior now depend on `AuthSessionAdmin`
- If admin capability is unavailable, the UI returns an explicit warning instead of silently relying on a concrete type

Impact:

- `AuthenticationCategory` is less tightly coupled to the default runtime
- The settings UI can now be tested or reused with alternate broker implementations more safely

## Validation

Executed:

```bash
./mvnw -pl papiflyfx-docking-docks,papiflyfx-docking-login,papiflyfx-docking-settings -am test -Dtestfx.headless=true
```

Result:

- Build success
- Affected module tests passed

## Remaining Roadmap Items

### High priority

- Extract floating/minimize/maximize behavior into a dedicated dock window lifecycle service
- Remove the remaining runtime-service-locator coupling from `AuthenticationCategory`
- Add characterization tests that explicitly cover broker substitutability and category lifecycle assumptions

### Medium priority

- Replace `ProviderSettingsResolver` hard-coded provider branching with provider contributors
- Replace `SecretStoreFactory` hard-coded backend selection with a backend SPI
- Split `IdentityProvider` into capability-specific interfaces

### Longer-term

- Redesign the `SettingsCategory` lifecycle so `buildSettingsPane(...)` is not a hidden precondition for `apply(...)` and `reset(...)`
- Move provider settings UI composition to provider-contributed sections instead of one central authentication category
- Add architecture checks to prevent new concrete downcasts and central switch-based extension points

## Notes

- This change set intentionally preserved the existing public `DockManager` API to reduce compatibility risk.
- The window lifecycle logic inside `DockManager` is still large and remains the next extraction target.
