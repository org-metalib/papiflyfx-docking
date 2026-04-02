# papiflyfx-docking SOLID Audit

## Scope

This audit focuses on the framework seams and runtime hotspots that define the architectural shape of the project:

- `papiflyfx-docking-docks`
- `papiflyfx-docking-settings`
- `papiflyfx-docking-settings-api`
- `papiflyfx-docking-login`
- `papiflyfx-docking-login-idapi`

The findings below are based on direct inspection of the current codebase, not on generic SOLID advice.

## Executive Summary

The codebase already has good module boundaries at the Maven level, but several core runtime classes collapse too many responsibilities back into single classes. The most serious problem is `DockManager`, which has become the framework’s god object. The second pattern is “pluggable in theory, hard-coded in practice”: provider configuration, secret-store selection, and login settings still require edits to central classes instead of extension through dedicated SPIs.

The main architectural risks are:

1. Core orchestration classes are absorbing UI, persistence, state transitions, and infrastructure details.
2. Public abstractions exist, but some consumers still downcast to concrete implementations, which breaks substitutability.
3. Extension points are incomplete, so adding providers/backends forces modifications to framework code.

## 1. Single Responsibility Principle

### Assessment

Partially aligned at the module level, violated in several core classes.

### Violations

#### 1.1 `DockManager` is a god object

**Evidence**

- UI composition and drag wiring live in the constructor and setup methods: `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:98-145`
- Theme application also lives here: `.../DockManager.java:161-174`
- Session capture and restore logic lives here: `.../DockManager.java:568-823`
- Floating, minimize, restore, maximize, and internal tree surgery also live here: `.../DockManager.java:957-1464`

**Why this violates SRP**

`DockManager` currently acts as:

- root JavaFX container factory
- drag/drop coordinator
- theme applicator
- floating window orchestrator
- minimize/maximize state manager
- layout/session persistence coordinator
- low-level dock tree mutator

That is several reasons to change in one class.

**Impact**

- Maintainability: any change in floating windows, restore hints, serialization, or theme behavior touches the same 1,540-line class.
- Testability: isolated unit testing is difficult because layout mutation, UI nodes, and persistence logic are intertwined.
- Scalability: adding new states such as pinned tabs, workspace-level sessions, or detachable toolbars will make this class harder to evolve safely.

#### 1.2 `DefaultAuthSessionBroker` mixes workflow orchestration with infrastructure and persistence details

**Evidence**

- Session state and in-memory token bookkeeping: `papiflyfx-docking-login/src/main/java/org/metalib/papifly/fx/login/core/DefaultAuthSessionBroker.java:41-52`
- Auth flow orchestration: `.../DefaultAuthSessionBroker.java:94-198`
- Browser launch, callback server management, and OAuth state handling: `.../DefaultAuthSessionBroker.java:299-506`
- Token persistence and Google-specific validation/retry logic: `.../DefaultAuthSessionBroker.java:508-672`

**Why this violates SRP**

The class is simultaneously:

- auth application service
- OAuth callback coordinator
- browser integration adapter
- session repository
- refresh-token persistence adapter
- provider-specific policy engine

**Impact**

- Maintainability: provider-specific exceptions such as Google offline-access retry leak into a generic broker.
- Testability: end-to-end auth behavior is hard to exercise without indirect control over browser/callback/runtime details.
- Scalability: supporting additional auth flows or enterprise policies will keep inflating one class instead of composing reusable policies.

#### 1.3 `AuthenticationCategory` mixes settings UI, secrets management, and session administration

**Evidence**

- Provider configuration UI and control creation: `papiflyfx-docking-login/src/main/java/org/metalib/papifly/fx/login/settings/AuthenticationCategory.java:162-255`
- Settings persistence and secret writes: `.../AuthenticationCategory.java:258-305`
- Session administration actions and runtime manipulation: `.../AuthenticationCategory.java:313-423`

**Impact**

- Maintainability: adding a provider means editing UI layout, storage bindings, and runtime session behavior in one place.
- Testability: the class cannot be verified as a simple settings form because it mutates live runtime state.
- Scalability: this category will become unmanageable as the number of providers grows.

## 2. Open/Closed Principle

### Assessment

Violated in provider and backend extension seams.

### Violations

#### 2.1 `ProviderSettingsResolver.resolve(...)` is hard-coded by provider id

**Evidence**

- Provider switch: `papiflyfx-docking-login/src/main/java/org/metalib/papifly/fx/login/core/ProviderSettingsResolver.java:35-69`
- Provider-specific builders:
  - Google: `.../ProviderSettingsResolver.java:72-90`
  - GitHub: `.../ProviderSettingsResolver.java:92-119`
  - Generic OIDC: `.../ProviderSettingsResolver.java:121-146`

**Why this violates OCP**

Adding a new provider currently requires modifying the resolver itself instead of contributing a new extension object. The framework is not closed for modification at this seam.

**Impact**

- Maintainability: every new provider change risks regressions in existing providers.
- Scalability: the resolver becomes a growing conditional hub for provider behavior.

#### 2.2 `SecretStoreFactory` is a central `if`/`instanceof` registry

**Evidence**

- OS-specific selection: `papiflyfx-docking-settings/src/main/java/org/metalib/papifly/fx/settings/secret/SecretStoreFactory.java:16-29`
- Backend naming via `instanceof`: `.../SecretStoreFactory.java:31-47`

**Why this violates OCP**

Supporting a new secret backend or new platform requires editing the factory and its display-name logic.

**Impact**

- Maintainability: backend registration is centralized and brittle.
- Scalability: alternative secure stores for containers, CI, or custom enterprise environments are not first-class extensions.

#### 2.3 `AuthenticationCategory` is not open for new providers

**Evidence**

- Provider constants are hard-coded: `papiflyfx-docking-login/src/main/java/org/metalib/papifly/fx/login/settings/AuthenticationCategory.java:47-87`
- The form layout is hard-coded for Generic OIDC, Google, and GitHub only: `.../AuthenticationCategory.java:209-247`

**Impact**

- Maintainability: UI and settings definitions must be edited for every provider addition.
- Scalability: provider onboarding does not scale beyond a small fixed list.

## 3. Liskov Substitution Principle

### Assessment

Violated where abstractions are declared but concrete behavior is still required.

### Violations

#### 3.1 `AuthenticationCategory` breaks substitutability of `AuthSessionBroker`

**Evidence**

- The category asks for the abstraction `AuthSessionBroker`, then downcasts to `DefaultAuthSessionBroker` in `switchAccount(...)`: `papiflyfx-docking-login/src/main/java/org/metalib/papifly/fx/login/settings/AuthenticationCategory.java:374-378`
- The same downcast is repeated in `revokeSelectedToken(...)`: `.../AuthenticationCategory.java:390-394`

**Why this violates LSP**

Any valid `AuthSessionBroker` implementation should be substitutable. Here, substituting another broker silently removes required behavior because `upsertSession(...)` and `removeSession(...)` exist only on the concrete class.

**Impact**

- Maintainability: the UI is coupled to one broker implementation despite the presence of an interface.
- Testability: fake brokers cannot fully support the settings category unless they mimic the concrete class.
- Scalability: introducing a remote broker, persistent broker, or test broker will produce inconsistent behavior.

#### 3.2 `SettingsCategory` implementations impose an undocumented lifecycle precondition

**Evidence**

- `NetworkCategory.buildSettingsPane(...)` initializes controls: `papiflyfx-docking-settings/src/main/java/org/metalib/papifly/fx/settings/categories/NetworkCategory.java:68-96`
- `NetworkCategory.reset(...)` dereferences those controls directly: `.../NetworkCategory.java:112-119`
- The same pattern exists in `AuthenticationCategory`: build in `.../AuthenticationCategory.java:162-255`, then direct control usage in `.../AuthenticationCategory.java:279-305`

**Why this violates LSP**

The `SettingsCategory` interface does not declare that `buildSettingsPane(...)` must be called before `apply(...)` or `reset(...)`. Concrete implementations strengthen the preconditions of the abstraction.

**Impact**

- Maintainability: every consumer of the SPI must know an implicit call order.
- Testability: headless or non-UI tests cannot safely exercise categories through the interface alone.

## 4. Interface Segregation Principle

### Assessment

Violated in the login provider contracts.

### Violations

#### 4.1 `IdentityProvider` is a wide interface with optional behavior hidden behind failing defaults

**Evidence**

- Contract surface: `papiflyfx-docking-login-idapi/src/main/java/org/metalib/papifly/fx/login/idapi/IdentityProvider.java:7-31`
- Unsupported behavior is expressed as default runtime failure for:
  - `refreshToken(...)`
  - `requestDeviceCode(...)`
  - `pollDeviceToken(...)`
- `GenericOidcProvider` implements the interface but does not participate in device flow, while `GitHubProvider` does: `papiflyfx-docking-login-idapi/src/main/java/org/metalib/papifly/fx/login/idapi/providers/GenericOidcProvider.java:24-56`, `papiflyfx-docking-login-idapi/src/main/java/org/metalib/papifly/fx/login/idapi/providers/GitHubProvider.java:31-46`

**Why this violates ISP**

Providers are forced to depend on a contract that bundles:

- auth-code PKCE
- token exchange
- refresh token flow
- device flow
- token revocation
- user profile lookup

The design avoids compile errors by pushing unsupported features into runtime failures, which is the opposite of interface segregation.

**Impact**

- Maintainability: capabilities are split between interface defaults and external metadata.
- Testability: test doubles must implement irrelevant methods or inherit failure behavior.
- Scalability: new auth flows will make the interface even wider unless the design is split now.

#### 4.2 `AuthSessionBroker` combines command, query, and UI-observable concerns

**Evidence**

- The interface mixes login commands, session queries, and JavaFX state properties: `papiflyfx-docking-login/src/main/java/org/metalib/papifly/fx/login/api/AuthSessionBroker.java:11-29`

**Impact**

- Maintainability: different clients depend on more broker surface than they need.
- Testability: simple read-only or write-only collaborators require a larger fake than necessary.

## 5. Dependency Inversion Principle

### Assessment

Violated in core runtime orchestration and settings UI integration.

### Violations

#### 5.1 `DefaultAuthSessionBroker` depends on concrete infrastructure instead of ports

**Evidence**

- Direct construction of `OAuthStateStore`: `papiflyfx-docking-login/src/main/java/org/metalib/papifly/fx/login/core/DefaultAuthSessionBroker.java:46`
- Direct construction of `ProviderSettingsResolver`: `.../DefaultAuthSessionBroker.java:82-86`
- Desktop/browser binding: `.../DefaultAuthSessionBroker.java:680-690`
- Loopback callback server binding: `.../DefaultAuthSessionBroker.java:699-719`

**Why this violates DIP**

The high-level authentication workflow depends directly on low-level details:

- JVM desktop API
- local loopback server
- in-memory OAuth state store
- concrete settings resolver

These should be injected as ports/adapters.

**Impact**

- Maintainability: replacing browser/callback mechanics affects the broker directly.
- Testability: infrastructure substitution requires package-private constructors and special hooks instead of stable abstractions.
- Scalability: headless, embedded, browserless, or remote auth variants are harder to support.

#### 5.2 `AuthenticationCategory` depends on runtime service locators and a concrete broker implementation

**Evidence**

- Static service locator usage: `papiflyfx-docking-login/src/main/java/org/metalib/papifly/fx/login/settings/AuthenticationCategory.java:202-204`, `313-342`, `374-378`, `408-422`
- Concrete downcast to `DefaultAuthSessionBroker`: `.../AuthenticationCategory.java:374-378`, `390-394`

**Impact**

- Maintainability: the settings UI cannot be composed with alternative runtime assemblies.
- Testability: UI tests must bootstrap global runtime state instead of injecting ports.
- Scalability: module reuse outside the default desktop runtime is unnecessarily constrained.

## Most Critical Violation

`DockManager` is the highest-risk violation because it sits at the center of the framework and currently controls presentation, state transitions, and persistence. Any new docking feature will continue to increase coupling unless this class is split first.

## Refactoring Roadmap

### Phase 1: Freeze behavior with characterization tests

1. Add characterization tests around `DockManager` for:
   - floating/docking round-trips
   - minimize/restore
   - maximize/restore
   - session capture/restore with floating and minimized leaves
2. Add characterization tests around `DefaultAuthSessionBroker` for:
   - auth-code flow lifecycle
   - device flow lifecycle
   - refresh-token reuse
   - logout with and without revocation
3. Add tests that expose current substitution issues:
   - `AuthenticationCategory` with a non-`DefaultAuthSessionBroker`
   - `SettingsCategory` `reset(...)` before `buildSettingsPane(...)`

### Phase 2: Split the `DockManager` responsibilities

4. Extract a `DockTreeService` responsible only for tree mutation:
   - `removeLeafFromDock(...)`
   - `insertLeafIntoDock(...)`
   - `tryRestoreWithHint(...)`
   - `findElementById(...)`
5. Extract a `DockSessionService` responsible for:
   - `captureSession()`
   - `restoreSession(...)`
   - content-state refresh before serialization
6. Extract a `DockWindowLifecycleService` responsible for:
   - floating
   - minimize/restore
   - maximize/restore
7. Reduce `DockManager` to a facade that delegates to those services and owns only the public API surface.

### Phase 3: Replace hard-coded provider/back-end logic with extension points

8. Introduce a provider config SPI, for example `ProviderConfigurationContributor`, that produces `ProviderRuntimeConfig` from `ProviderDescriptor` plus settings/secret access.
9. Move Google/GitHub/Generic OIDC configuration logic out of `ProviderSettingsResolver` into provider-specific contributors.
10. Introduce a `SecretStoreBackendProvider` SPI and replace `SecretStoreFactory` `if`/`instanceof` chains with discovery/registration.
11. Introduce a `LoginSettingsSectionContributor` SPI so provider settings panes are contributed by providers instead of hard-coded in `AuthenticationCategory`.

### Phase 4: Restore substitutability and segregate interfaces

12. Split `IdentityProvider` into smaller capability interfaces, for example:
   - `AuthorizationCodeProvider`
   - `RefreshTokenProvider`
   - `DeviceFlowProvider`
   - `TokenRevocationProvider`
   - `UserInfoProvider`
13. Update `ProviderDescriptor.capabilities()` to become an optimization or UI hint, not the source of truth for safe method dispatch.
14. Introduce an `AuthSessionAdmin` port for `upsertSession(...)` and `removeSession(...)`, or move those operations into the broker contract if they are part of supported behavior.
15. Make `AuthenticationCategory` depend only on injected interfaces, not `LoginRuntime` or `DefaultAuthSessionBroker`.

### Phase 5: Fix `SettingsCategory` lifecycle design

16. Separate category state from category view:
   - `SettingsCategoryModel` for read/apply/reset
   - `SettingsCategoryViewFactory` for JavaFX node creation
17. Alternatively, keep one type but make `apply(...)` and `reset(...)` operate on a dedicated view-model object instead of directly on cached controls.
18. Remove hidden assumptions that `buildSettingsPane(...)` must run before all other methods.

### Phase 6: Institutionalize the architecture

19. Add architecture tests or review rules that flag:
   - service-locator use in UI categories
   - downcasts from public interfaces to concrete runtime classes
   - new provider additions that modify central switch statements
20. Mark old monolithic entry points as deprecated only after adapters preserve compatibility for downstream modules.

## Before vs After

### Before

Current `DockManager.floatLeaf(...)` mixes state transition, restore-hint capture, tree mutation, and UI window creation in one method:

```java
public void floatLeaf(DockLeaf leaf) {
    if (!ensureFloatingWindowManager("float leaf")) {
        return;
    }

    if (maximizedLeaf == leaf) {
        restoreMaximized();
    }

    String leafId = leaf.getMetadata().id();
    RestoreHint hint = MinimizedStore.captureRestoreHint(leaf);
    floatingRestoreHints.put(leafId, hint);

    removeLeafFromDock(leaf);
    updateLeafState(leaf, DockState.FLOATING);

    FloatingDockWindow window = floatingWindowManager.floatLeaf(leaf);
    window.show();
}
```

### After

Refactor `DockManager` into a thin facade and move the behavior into focused collaborators:

```java
public final class DockManager {

    private final DockWindowLifecycleService windowLifecycleService;

    public void floatLeaf(DockLeaf leaf) {
        windowLifecycleService.floatLeaf(leaf);
    }
}

public final class DockWindowLifecycleService {

    private final DockTreeService dockTreeService;
    private final RestoreHintService restoreHintService;
    private final DockLeafStateService leafStateService;
    private final FloatingWindowPort floatingWindows;
    private final MaximizeStatePort maximizeState;

    public void floatLeaf(DockLeaf leaf) {
        maximizeState.restoreIfNeeded(leaf);

        RestoreHint hint = restoreHintService.captureForFloating(leaf);
        dockTreeService.detach(leaf);
        leafStateService.markFloating(leaf, hint);
        floatingWindows.show(leaf);
    }
}
```

### Why the “after” design is better

- `DockManager` keeps the public API but loses the implementation burden.
- Tree mutation becomes testable without JavaFX window behavior.
- Floating-window behavior can be replaced or mocked through `FloatingWindowPort`.
- Restore-hint logic stops leaking into unrelated concerns such as maximize/minimize handling.

## Priority Order

If this refactoring is staged over multiple iterations, the best order is:

1. Split `DockManager`
2. Remove `AuthenticationCategory` downcasts and service-locator coupling
3. Replace `ProviderSettingsResolver` hard-coded provider logic with provider contributors
4. Split `IdentityProvider`
5. Replace `SecretStoreFactory` branching with a backend SPI
6. Fix the `SettingsCategory` lifecycle contract

This order gives the highest reduction in architectural risk for the lowest compatibility cost.
