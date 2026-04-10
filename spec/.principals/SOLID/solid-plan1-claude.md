# SOLID Principles Audit — papiflyfx-docking

**Date:** 2026-04-02
**Scope:** Full codebase audit across all modules
**Auditor:** Senior Software Architect / Clean Code Expert

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Single Responsibility Principle (SRP)](#1-single-responsibility-principle-srp)
3. [Open/Closed Principle (OCP)](#2-openclosed-principle-ocp)
4. [Liskov Substitution Principle (LSP)](#3-liskov-substitution-principle-lsp)
5. [Interface Segregation Principle (ISP)](#4-interface-segregation-principle-isp)
6. [Dependency Inversion Principle (DIP)](#5-dependency-inversion-principle-dip)
7. [Before vs. After — Critical Violation](#6-before-vs-after--critical-violation)
8. [Refactoring Roadmap](#7-refactoring-roadmap)

---

## Executive Summary

The papiflyfx-docking framework has a strong modular foundation — the API/SPI split, ServiceLoader usage, and content-factory pattern show thoughtful architecture. However, several key classes have accumulated too many responsibilities, and concrete coupling in the runtime layer undermines the abstractions established by the SPI modules.

| Principle | Severity | Violation Count | Key Hot Spots |
|-----------|----------|----------------|---------------|
| **SRP** | **High** | 10 | `DockManager`, `DefaultAuthSessionBroker`, `AuthenticationCategory` |
| **OCP** | **High** | 6 | `LayoutSerializer`, `DockTreeService`, `SecretStoreFactory`, `ViewerFactory` |
| **LSP** | Medium | 2 | `DockLeaf`/`DockElement` hierarchy, `DisposableContent` |
| **ISP** | Medium | 2 | `SettingsCategory`, `Theme` record |
| **DIP** | **High** | 4 | `DockManager`, `SettingsRuntime`, `LoginRuntime` |

---

## 1. Single Responsibility Principle (SRP)

> *A class should have only one reason to change.*

### 1.1 `DockManager` — 1073 lines, 80+ methods

**File:** `papiflyfx-docking-docks/.../docks/DockManager.java`

**Mixed responsibilities:**
- Layout management (`setRoot`, `capture`, `restore`)
- Floating window management (`floatLeaf`, `dockLeaf`)
- Minimize/maximize state (`minimizeLeaf`, `restoreLeaf`, `maximizeLeaf`)
- Drag & drop coordination (`setupDragHandlers`)
- Theme propagation (`applyTheme`, `themeProperty`)
- Session persistence (`saveSessionToFile`, `loadSessionFromFile`)
- Window management (`setOwnerStage`, `resolveOwnerStage`)
- Factory wiring (`setContentFactory`, `setContentStateRegistry`)

**Impact:** Any change to floating logic, theme handling, or persistence requires editing the same 1073-line class. Testing one concern means constructing the entire DockManager.

### 1.2 `DefaultAuthSessionBroker` — 743 lines

**File:** `papiflyfx-docking-login/.../login/core/DefaultAuthSessionBroker.java`

**Mixed responsibilities:**
- Session state machine (authState, activeSession, deviceCode)
- Browser launching and OAuth callback handling
- Token refresh logic
- Provider configuration resolution
- Session persistence to storage
- OAuth state validation
- Authorization-code and device-code flows

**Impact:** Token refresh changes ripple into session management. Browser launching quirks affect the entire broker.

### 1.3 `AuthenticationCategory` — 605 lines

**File:** `papiflyfx-docking-login/.../login/settings/AuthenticationCategory.java`

**Mixed responsibilities:**
- 30+ UI control declarations
- OAuth provider configuration forms
- Session state display
- Token revocation actions
- Session switching logic

**Impact:** Any UI tweak touches the same class as backend token operations. Untestable without the full settings UI stack.

### 1.4 Additional SRP Violations

| Class | Lines | File | Responsibilities |
|-------|-------|------|-----------------|
| `DockTabGroup` | 676 | `docks/core/DockTabGroup.java` | Tab bar UI + tab lifecycle + button factory + theme styling |
| `DockSplitGroup` | 345 | `docks/core/DockSplitGroup.java` | Divider drag handling + split layout + child disposal |
| `GenericOidcProvider` | 229 | `login-idapi/.../providers/GenericOidcProvider.java` | Auth requests + code exchange + user fetch + token refresh + JSON parsing |
| `HugoPreviewPane` | 133+ | `hugo/.../HugoPreviewPane.java` | Process lifecycle + WebView + navigation + file watching + theme |
| `SettingsPanel` | ~200 | `settings/.../ui/SettingsPanel.java` | Category discovery + search + persistence + theme + UI layout |
| `JsonSettingsStorage` | ~150 | `settings/.../persist/JsonSettingsStorage.java` | Type conversion + file I/O + scope management + migration |
| `LoginDockPane` | 127 | `login/.../ui/LoginDockPane.java` | View routing + view creation + device flow lifecycle + error handling |

---

## 2. Open/Closed Principle (OCP)

> *Open for extension, closed for modification.*

### 2.1 `DockTreeService` — Pervasive `instanceof` chains

**File:** `papiflyfx-docking-docks/.../docks/DockTreeService.java` (lines 24–189)

```java
// Lines 24, 62, 80, 86, 118, 150, 157, 171, 182
if (parent instanceof DockSplitGroup split) { ... }
if (parent instanceof DockTabGroup tabGroup) { ... }
```

Adding a new `DockElement` subtype (e.g., `DockAccordionGroup`) requires touching **every** `instanceof` branch.

### 2.2 `LayoutSerializer` — Switch on sealed types

**File:** `papiflyfx-docking-docks/.../serial/LayoutSerializer.java` (lines 59–63, 139–144)

```java
return switch (node) {
    case LeafData leaf -> serializeLeaf(leaf);
    case SplitData split -> serializeSplit(split);
    case TabGroupData tabGroup -> serializeTabGroup(tabGroup);
};
```

### 2.3 `SecretStoreFactory` — Hardcoded OS detection + `instanceof` mapping

**File:** `papiflyfx-docking-settings/.../secret/SecretStoreFactory.java` (lines 16–48)

```java
if (os.contains("mac"))  return new KeychainSecretStore(...);
if (os.contains("win"))  return new WinCredSecretStore(...);
if (os.contains("linux")) return new LibsecretSecretStore(...);
```

And the reverse mapping:

```java
if (secretStore instanceof KeychainSecretStore) return "macOS Keychain";
if (secretStore instanceof EncryptedFileSecretStore) return "Encrypted File";
// ... 3 more
```

### 2.4 Additional OCP Violations

| Class | File | Pattern |
|-------|------|---------|
| `LayoutFactory` | `docks/layout/LayoutFactory.java:95-99` | Switch on `LayoutNode` types |
| `HitTester` | `docks/drag/HitTester.java:68-112` | `instanceof` checks to determine hit behavior |
| `ViewerFactory` | `media/.../viewer/ViewerFactory.java:10-24` | Switch on `UrlKind` enum |
| `SettingControlFactory` | `settings/.../controls/SettingControlFactory.java:11-22` | Switch on `SettingType` to create controls |
| `ProviderSettingsResolver` | `login/.../core/ProviderSettingsResolver.java:35-70` | Switch on provider ID strings |

**Impact:** Every new node type, viewer kind, setting type, or secret store backend forces edits to the corresponding factory/service. This scales poorly and is error-prone.

---

## 3. Liskov Substitution Principle (LSP)

> *Subtypes must be substitutable for their base types.*

### 3.1 `DockLeaf` is not a `DockElement`

**File:** `papiflyfx-docking-docks/.../core/DockLeaf.java`

`DockLeaf` does not implement `DockElement`. Only `DockTabGroup` and `DockSplitGroup` do. This means code that accepts a `DockElement` cannot process a leaf directly — it must always be wrapped in a `DockTabGroup`. The asymmetry forces callers to special-case leaves vs. groups.

**Impact:** The layout tree has an implicit constraint that the framework doesn't express in the type system.

### 3.2 `DisposableContent` — Runtime capability discovery

**File:** `papiflyfx-docking-api/.../api/DisposableContent.java`

```java
// DockLeaf.java:196
if (node instanceof DisposableContent disposable) { disposable.dispose(); }
```

Content may or may not implement `DisposableContent`. Callers rely on runtime `instanceof` rather than a guaranteed contract. This is a minor LSP concern — the pattern is common in Java, but it means the `Node` base type does not guarantee disposal semantics.

---

## 4. Interface Segregation Principle (ISP)

> *Clients should not be forced to depend on methods they don't use.*

### 4.1 `SettingsCategory` — 8 unrelated methods

**File:** `papiflyfx-docking-settings-api/.../api/SettingsCategory.java` (lines 7–38)

```java
public interface SettingsCategory {
    String id();
    String displayName();
    Node icon();
    int order();
    List<SettingDefinition<?>> definitions();
    List<SettingsAction> actions();
    Node buildSettingsPane(SettingsContext context);
    void apply();
    void reset();
    boolean isDirty();
}
```

A category that only contributes metadata and definitions must still implement `buildSettingsPane`, `apply`, `reset`, and `isDirty`. This forces stub implementations and violates ISP.

**Suggested split:**
- `SettingsCategoryMetadata` — `id()`, `displayName()`, `icon()`, `order()`
- `SettingsCategoryDefinitions` — `definitions()`, `actions()`
- `SettingsCategoryUI` — `buildSettingsPane(context)`, `apply()`, `reset()`, `isDirty()`

### 4.2 `Theme` record — 21 properties

**File:** `papiflyfx-docking-api/.../api/Theme.java` (lines 35–57)

The `Theme` record bundles colors, fonts, dimensions, and paddings into a single 21-field type. A code editor that only needs `editorFont` and `editorBackground` is forced to depend on `tabHeight`, `minimizedBarColor`, and 18 other fields.

**Suggested split:**
- `ThemeColors` — background, foreground, accent, selection, border colors
- `ThemeFonts` — UI font, editor font, sizes
- `ThemeDimensions` — tab height, padding, divider width

---

## 5. Dependency Inversion Principle (DIP)

> *Depend on abstractions, not concretions.*

### 5.1 `DockManager` — Direct concrete instantiation

**File:** `papiflyfx-docking-docks/.../docks/DockManager.java` (lines 97–124)

```java
minimizedStore = new MinimizedStore();           // concrete
minimizedBar = new MinimizedBar(themeProperty);   // concrete
layoutFactory = new LayoutFactory(themeProperty);  // concrete
treeService = new DockTreeService(this);           // concrete
sessionService = new DockSessionService(this, treeService); // concrete
```

The highest-level orchestrator directly instantiates every collaborator. There are no interfaces for `MinimizedStore`, `LayoutFactory`, `DockTreeService`, or `DockSessionService`.

**Impact:** Impossible to substitute test doubles. Cannot swap layout factory without forking DockManager.

### 5.2 `SettingsRuntime` — Hardcoded storage and factory calls

**File:** `papiflyfx-docking-settings/.../runtime/SettingsRuntime.java` (lines 41–52)

```java
JsonSettingsStorage storage = new JsonSettingsStorage(...);
SecretStoreFactory.createDefault(applicationDir);
```

High-level runtime depends on concrete `JsonSettingsStorage` and static factory. Also maintains a global singleton via `AtomicReference<SettingsRuntime>`.

### 5.3 `LoginRuntime` — Concrete broker creation

**File:** `papiflyfx-docking-login/.../runtime/LoginRuntime.java` (lines 62–70)

```java
return new DefaultAuthSessionBroker(registry, provider.storage(), provider.secretStore());
```

### 5.4 `LayoutFactory` — Hardcoded placeholder

**File:** `papiflyfx-docking-docks/.../layout/LayoutFactory.java` (lines 137–140)

Creates `Label` nodes directly as missing-content placeholders instead of delegating to a configurable placeholder factory.

---

## 6. Before vs. After — Critical Violation

### `DockManager` SRP + DIP — The Most Impactful Violation

The `DockManager` class violates both SRP (8 responsibilities) and DIP (5 concrete instantiations). Decomposing it produces the largest improvement in testability, maintainability, and extensibility.

### BEFORE (current code, abridged)

```java
public class DockManager {

    // 8 concrete fields — all instantiated directly
    private final MinimizedBar minimizedBar;
    private final MinimizedStore minimizedStore;
    private final FloatingWindowManager floatingManager;
    private final LayoutFactory layoutFactory;
    private final DockTreeService treeService;
    private final DockSessionService sessionService;
    private final DragManager dragManager;
    private final OverlayCanvas overlayLayer;

    public DockManager(Theme theme) {
        this.themeProperty = new SimpleObjectProperty<>(theme);
        // ... 40 lines of wiring ...
        minimizedStore = new MinimizedStore();
        minimizedBar = new MinimizedBar(themeProperty);
        layoutFactory = new LayoutFactory(themeProperty);
        treeService = new DockTreeService(this);
        sessionService = new DockSessionService(this, treeService);
        dragManager = new DragManager(this::getRoot, overlayLayer,
            this::setRoot, themeProperty, this::createTabGroup);
        setupDragHandlers();
    }

    // --- Floating (90 lines) ---
    public void floatLeaf(DockLeaf leaf, DockTabGroup source, Rectangle2D bounds) { ... }
    public void dockLeaf(DockLeaf leaf, FloatingDockWindow window) { ... }

    // --- Minimize/Maximize (80 lines) ---
    public void minimizeLeaf(DockLeaf leaf, DockTabGroup source) { ... }
    public void restoreLeaf(DockLeaf leaf) { ... }
    public void maximizeLeaf(DockLeaf leaf, DockTabGroup source) { ... }
    public void restoreMaximized() { ... }

    // --- Session persistence (60 lines) ---
    public void saveSessionToFile(Path path) { ... }
    public void loadSessionFromFile(Path path) { ... }
    public DockSessionData captureSession() { ... }
    public void restoreSession(DockSessionData session) { ... }

    // --- Theme (30 lines) ---
    private void applyTheme(Theme theme) { ... }

    // --- Layout tree operations (100 lines) ---
    // ... 80+ total methods ...
}
```

### AFTER (refactored, abridged)

```java
// --- New: focused service interfaces ---

public interface DockFloatingService {
    void floatLeaf(DockLeaf leaf, DockTabGroup source, Rectangle2D bounds);
    void dockLeaf(DockLeaf leaf, FloatingDockWindow window);
    FloatingWindowManager floatingManager();
}

public interface DockMinMaxService {
    void minimizeLeaf(DockLeaf leaf, DockTabGroup source);
    void restoreLeaf(DockLeaf leaf);
    void maximizeLeaf(DockLeaf leaf, DockTabGroup source);
    void restoreMaximized();
}

public interface DockSessionService {
    DockSessionData captureSession();
    void restoreSession(DockSessionData session);
    void saveToFile(Path path);
    void loadFromFile(Path path);
}

public interface DockThemeService {
    ObjectProperty<Theme> themeProperty();
    void applyTheme(Theme theme);
}

// --- Refactored DockManager: orchestrator only ---

public class DockManager {

    private final BorderPane mainContainer;
    private final StackPane rootPane;
    private final ObjectProperty<DockElement> rootElement;

    // Depends on abstractions, not concretions
    private final DockFloatingService floatingService;
    private final DockMinMaxService minMaxService;
    private final DockSessionService sessionService;
    private final DockThemeService themeService;
    private final DockTreeService treeService;
    private final DragManager dragManager;

    // Constructor accepts abstractions — enables testing with mocks
    public DockManager(Theme theme, DockManagerServices services) {
        this.themeService = services.themeService();
        this.floatingService = services.floatingService();
        this.minMaxService = services.minMaxService();
        this.sessionService = services.sessionService();
        this.treeService = services.treeService();
        this.dragManager = services.dragManager();

        // Thin wiring — no business logic
        rootPane = new StackPane();
        mainContainer = new BorderPane(rootPane);
        mainContainer.setBottom(minMaxService.minimizedBar());
        themeService.applyTheme(theme);
    }

    // Static convenience factory preserves the easy one-liner API
    public static DockManager createDefault(Theme theme) {
        return new DockManager(theme, DockManagerServices.createDefault(theme));
    }

    // --- Public API delegates to focused services ---

    public void floatLeaf(DockLeaf leaf, DockTabGroup src, Rectangle2D b) {
        floatingService.floatLeaf(leaf, src, b);
    }

    public void minimizeLeaf(DockLeaf leaf, DockTabGroup src) {
        minMaxService.minimizeLeaf(leaf, src);
    }

    public DockSessionData captureSession() {
        return sessionService.captureSession();
    }

    // Root/layout methods stay here — that IS the manager's responsibility
    public DockElement getRoot() { return rootElement.get(); }
    public void setRoot(DockElement element) { ... }

    public Region getView() { return mainContainer; }
}
```

**Key improvements:**
1. **SRP:** Each service owns one concern. `DockManager` is reduced to ~150 lines of orchestration.
2. **DIP:** `DockManager` depends on interfaces, not concrete classes. Test doubles are trivial.
3. **OCP:** New docking behaviors (e.g., pinning, stacking) can be added as new services without editing `DockManager`.
4. **Backward compatibility:** `DockManager.createDefault(theme)` preserves the existing one-liner API.

---

## 7. Refactoring Roadmap

### Phase 1 — Extract DockManager Services (High Impact, Low Risk)

| Step | Action | Files Affected |
|------|--------|---------------|
| 1.1 | Extract `DockFloatingService` interface + `DefaultDockFloatingService` | `DockManager.java`, new files |
| 1.2 | Extract `DockMinMaxService` interface + `DefaultDockMinMaxService` | `DockManager.java`, new files |
| 1.3 | Extract `DockSessionService` interface (rename existing class to impl) | `DockManager.java`, `DockSessionService.java` |
| 1.4 | Extract `DockThemeService` interface + `DefaultDockThemeService` | `DockManager.java`, new files |
| 1.5 | Create `DockManagerServices` builder/record for DI wiring | New file |
| 1.6 | Add `DockManager.createDefault(Theme)` static factory | `DockManager.java` |
| 1.7 | Update tests to use service interfaces | Test files |

**Estimated risk:** Low — public API preserved via delegation.

### Phase 2 — Eliminate instanceof Chains (High Impact, Medium Risk)

| Step | Action | Files Affected |
|------|--------|---------------|
| 2.1 | Add `accept(DockElementVisitor)` to `DockElement` interface | `DockElement.java` |
| 2.2 | Implement visitor in `DockTabGroup`, `DockSplitGroup` | Core files |
| 2.3 | Refactor `DockTreeService` to use visitor pattern | `DockTreeService.java` |
| 2.4 | Refactor `HitTester` to use visitor pattern | `HitTester.java` |
| 2.5 | Refactor `LayoutSerializer` to use polymorphic `serialize()` on `LayoutNode` | `LayoutSerializer.java`, `LayoutNode` subtypes |
| 2.6 | Add `backendName()` method to `SecretStore` interface | `SecretStore.java`, all implementations |

### Phase 3 — Split Fat Interfaces (Medium Impact, Low Risk)

| Step | Action | Files Affected |
|------|--------|---------------|
| 3.1 | Extract `SettingsCategoryMetadata` from `SettingsCategory` | `SettingsCategory.java`, new interface |
| 3.2 | Extract `SettingsCategoryUI` from `SettingsCategory` | `SettingsCategory.java`, new interface |
| 3.3 | Provide default `SettingsCategory` that composes both | New abstract class |
| 3.4 | Extract `ThemeColors`, `ThemeFonts`, `ThemeDimensions` from `Theme` | `Theme.java`, new records |

### Phase 4 — Decompose Large Classes (High Impact, High Risk)

| Step | Action | Files Affected |
|------|--------|---------------|
| 4.1 | Extract `TokenManager` from `DefaultAuthSessionBroker` | `DefaultAuthSessionBroker.java`, new class |
| 4.2 | Extract `OAuthFlowExecutor` from `DefaultAuthSessionBroker` | `DefaultAuthSessionBroker.java`, new class |
| 4.3 | Extract `SessionPersistenceService` from `DefaultAuthSessionBroker` | `DefaultAuthSessionBroker.java`, new class |
| 4.4 | Split `AuthenticationCategory` into `AuthCategoryUI` + `AuthCategoryController` | `AuthenticationCategory.java`, new classes |
| 4.5 | Extract `TabBarRenderer` from `DockTabGroup` | `DockTabGroup.java`, new class |
| 4.6 | Extract `DividerHandler` from `DockSplitGroup` | `DockSplitGroup.java`, new class |

### Phase 5 — Invert Dependencies in Runtime Modules (Medium Impact, Medium Risk)

| Step | Action | Files Affected |
|------|--------|---------------|
| 5.1 | Extract `SettingsStorageFactory` interface; make `SettingsRuntime` accept it | `SettingsRuntime.java`, new interface |
| 5.2 | Remove global `DEFAULT_RUNTIME` singleton; use explicit DI | `SettingsRuntime.java`, callers |
| 5.3 | Extract `AuthSessionBrokerFactory` interface for `LoginRuntime` | `LoginRuntime.java`, new interface |
| 5.4 | Extract `PlaceholderFactory` interface for `LayoutFactory` | `LayoutFactory.java`, new interface |

### Priority Order

```
Phase 1 ──► Phase 2 ──► Phase 3
                           │
Phase 4 ─────────────────►─┘
                           │
Phase 5 ─────────────────►─┘
```

- **Phase 1** is the highest-leverage change: it unlocks unit testing of DockManager and sets the DI pattern for the rest of the codebase.
- **Phase 2** should follow immediately — the `instanceof` chains are the most common OCP violation and touch the most files.
- **Phases 3–5** can be interleaved based on which modules are under active development.

---

## Appendix: Violation Inventory

| # | Principle | Class | Module | Severity |
|---|-----------|-------|--------|----------|
| 1 | SRP | `DockManager` | docks | High |
| 2 | SRP | `DefaultAuthSessionBroker` | login | High |
| 3 | SRP | `AuthenticationCategory` | login | High |
| 4 | SRP | `DockTabGroup` | docks | Medium |
| 5 | SRP | `DockSplitGroup` | docks | Medium |
| 6 | SRP | `GenericOidcProvider` | login-idapi | Medium |
| 7 | SRP | `HugoPreviewPane` | hugo | Medium |
| 8 | SRP | `SettingsPanel` | settings | Medium |
| 9 | SRP | `JsonSettingsStorage` | settings | Medium |
| 10 | SRP | `LoginDockPane` | login | Low |
| 11 | OCP | `DockTreeService` | docks | High |
| 12 | OCP | `LayoutSerializer` | docks | Medium |
| 13 | OCP | `SecretStoreFactory` | settings | Medium |
| 14 | OCP | `ViewerFactory` | media | Medium |
| 15 | OCP | `SettingControlFactory` | settings | Medium |
| 16 | OCP | `ProviderSettingsResolver` | login | Medium |
| 17 | LSP | `DockLeaf`/`DockElement` | docks | Medium |
| 18 | LSP | `DisposableContent` | api | Low |
| 19 | ISP | `SettingsCategory` | settings-api | Medium |
| 20 | ISP | `Theme` | api | Low |
| 21 | DIP | `DockManager` constructor | docks | High |
| 22 | DIP | `SettingsRuntime` | settings | Medium |
| 23 | DIP | `LoginRuntime` | login | Medium |
| 24 | DIP | `LayoutFactory` placeholder | docks | Low |
