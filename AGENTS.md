# PapiflyFX Docking Agent Team

This repository is managed by a team of specialized AI agents. Each agent has a specific domain of expertise and follows the project's [SOLID principles](spec/.principals/SOLID/README.md) and [Coding Guidelines](README.md).

## Agent Roles & Responsibilities

### 1. Core Architect (@core-architect)
- **Primary Domain**: `papiflyfx-docking-api`, `papiflyfx-docking-docks`.
- **Responsibilities**:
  - Maintains the foundational docking API and its core implementation.
  - Ensures layout serialization, floating windows, and session persistence remain robust.
  - Guards the SOLID principles across the codebase.
  - Reviews and approves changes to shared contracts and SPIs.
- **Focus Area**: `org.metalib.papifly.fx.docks`, `org.metalib.papifly.fx.api`.

### 2. Feature Developer (@feature-dev)
- **Primary Domain**: Content modules (`code`, `tree`, `media`, `hugo`, `github`).
- **Responsibilities**:
  - Implements new dockable content types and maintains their functional state.
  - Ensures proper integration with `ContentFactory` and `ContentStateAdapter`.
  - Builds programmatic UI components and layout logic for feature docks.
  - Follows JavaFX best practices (no FXML).
- **Focus Area**: `papiflyfx-docking-code`, `papiflyfx-docking-tree`, `papiflyfx-docking-media`, `papiflyfx-docking-hugo`, `papiflyfx-docking-github`.

### 3. Build & Runtime Engineer (@ops-engineer)
- **Primary Domain**: `pom.xml`, `papiflyfx-docking-settings`, `papiflyfx-docking-samples`.
- **Responsibilities**:
  - Manages the multi-module Maven build and dependency versions.
  - Maintains the settings runtime, persistence, and secret-store backends.
  - Ensures all modules compile and tests pass in headless mode.
  - Updates demo samples to showcase new framework features.
- **Focus Area**: Root `pom.xml`, `papiflyfx-docking-settings`, `papiflyfx-docking-samples`.

### 4. Auth & Security Specialist (@auth-specialist)
- **Primary Domain**: `papiflyfx-docking-login-idapi`, `papiflyfx-docking-login-session-api`, `papiflyfx-docking-login`.
- **Responsibilities**:
  - Manages identity provider (IDP) integrations and the authentication SPI.
  - Ensures secure session lifecycle and storage.
  - Implements the functional login UI and its integration with the framework.
  - Handles secret management and encrypted storage.
- **Focus Area**: `org.metalib.papifly.fx.login`.

### 5. UI/UX Designer (@ui-ux-designer)
- **Primary Domain**: `papiflyfx-docking-api` (Theme API), CSS in `code`, `tree`, `github`.
- **Responsibilities**:
  - Defines and leads the visual identity and user experience across all modules.
  - Sets standards for theme definitions, color palettes, and CSS styling.
  - Optimizes layout consistency, spacing, and ergonomic UI flows.
  - Reviews and audits UI implementations for accessibility and polish.
- **Focus Area**: CSS stylesheets, `Theme` implementations, and UI component layout code.

---

## Repository Guidelines

## Project Structure & Module Organization
- Root Maven aggregator: `pom.xml` defines the multi-module build and shared dependency/plugin management.
- Shared contract modules:
  - `papiflyfx-docking-api/` - public docking/content/theme API.
  - `papiflyfx-docking-settings-api/` - public settings and secret-management SPI.
  - `papiflyfx-docking-login-idapi/` - identity-provider SPI and built-in providers.
  - `papiflyfx-docking-login-session-api/` - auth session lifecycle and storage SPI.
- Core/runtime modules:
  - `papiflyfx-docking-docks/` - docking framework, drag/drop, floating windows, minimize/maximize, session persistence.
  - `papiflyfx-docking-settings/` - settings runtime, panel UI, JSON persistence, secret-store backends.
  - `papiflyfx-docking-login/` - authentication runtime, UI, and docking integration.
- Content modules:
  - `papiflyfx-docking-code/` - canvas-based code editor.
  - `papiflyfx-docking-tree/` - canvas-based virtualized tree.
  - `papiflyfx-docking-media/` - media/image/video viewer.
  - `papiflyfx-docking-hugo/` - Hugo preview content.
  - `papiflyfx-docking-github/` - GitHub toolbar integration.
- Runnable demos live in `papiflyfx-docking-samples/`.
- Tests live under `src/test/java` per module. Active suites currently exist in `code`, `docks`, `github`, `hugo`, `login-idapi`, `login`, `media`, `samples`, `settings`, and `tree`.
- Architecture and design references live under `spec/` plus module-level `README.md` files.

## Build, Test, and Development Commands
- Java setup (SDKMAN): `sdk use java 25.0.1.fx-zulu`
- Compile all modules: `./mvnw compile`
- Full build: `./mvnw clean package`
- Run all tests: `./mvnw test`
- Explicit headless test run: `./mvnw -Dtestfx.headless=true test`
- Focused module build/test: `./mvnw -pl papiflyfx-docking-docks -am test`
- Single test class: `./mvnw -pl papiflyfx-docking-samples -am -Dtest=SamplesSmokeTest test`
- Run demos:
  - Docks demo: `./mvnw javafx:run -pl papiflyfx-docking-docks`
  - Samples app: `./mvnw javafx:run -pl papiflyfx-docking-samples`
- The root POM defaults `testfx.headless=true`; pass `-Dtestfx.headless=false` for interactive UI runs.
- JavaFX classifier selection is handled by OS-specific Maven profiles in the root POM; use `./mvnw`, not bare `mvn`.

## Coding Style & Naming Conventions
- Java 25, 4-space indentation, standard JavaFX idioms.
- Packages use `org.metalib.papifly.fx.*`; classes are `PascalCase`, methods/fields `camelCase`.
- Favor descriptive suffixes already used in the codebase such as `*Manager`, `*Controller`, `*Factory`, `*StateAdapter`, and `*Renderer`.
- Keep new public APIs and SPIs documented with Javadoc.
- UI is programmatic JavaFX. No FXML is used. Most modules avoid CSS entirely, but `code`, `tree`, and `github` ship scoped stylesheets for overlays/theming, so follow the local module pattern instead of introducing global styling.

## SOLID Principles
- Single Responsibility: keep each module, class, and UI component focused on one concern. Follow the existing split between docking core, settings/runtime services, login/auth flows, and feature content modules.
- Open/Closed: prefer extending the system through `ContentFactory`, `ContentStateAdapter`, `SettingsCategory`, `SettingsContributor`, and `ServiceLoader` registration instead of editing central framework code for every new feature.
- Liskov Substitution: implementations of shared interfaces and SPIs must remain drop-in replacements. New factories, adapters, or stores should honor existing contracts without surprising side effects or narrower preconditions.
- Interface Segregation: keep public APIs and SPIs small and purpose-built. Add new focused interfaces when needed instead of growing broad "do everything" contracts.
- Dependency Inversion: depend on API/SPI modules (`papiflyfx-docking-api`, `papiflyfx-docking-settings-api`, `papiflyfx-docking-login-idapi`, `papiflyfx-docking-login-session-api`) rather than concrete runtime modules whenever a stable abstraction already exists.

## Docking Integration Patterns
- `DockManager#createTabGroup()` already wires drag and button handlers; do not call `setupTabGroupDragHandlers(...)` manually on groups created through that factory.
- `DockManager` overloads `setRoot(DockElement)` and `setRoot(LayoutNode)`; cast to `DockElement` if Java type inference becomes ambiguous.
- For session restore, create a `ContentStateRegistry`, register adapters manually or use `ContentStateRegistry.fromServiceLoader()`, then call `dockManager.setContentStateRegistry(...)` before restoring layouts/sessions.
- Restorable content should provide both a `ContentFactory` and a `ContentStateAdapter`, and the created `DockLeaf` must carry the matching `contentFactoryId`.
- Theme-aware content generally exposes `bindThemeProperty(...)`; wire it from the owning `DockManager` theme property.

## Maven Module Guidelines
- Keep modules self-contained with explicit boundaries and dependency flow managed through Maven.
- Manage dependency versions in parent POM properties and plugin versions in parent `pluginManagement`.
- Manage plugin dependencies centrally in parent POMs.
- Prefer `-pl <module> -am` for focused work so upstream dependencies build consistently.

## Testing Guidelines
- Frameworks: JUnit Jupiter + TestFX + Monocle.
- Naming: `*Test` for unit/integration tests and `*FxTest` for UI-heavy tests.
- `papiflyfx-docking-code` uses the `benchmark` JUnit tag for benchmarks; those are excluded by default.
- Keep UI tests deterministic and prefer headless runs in CI/sandbox environments.
- Surefire configuration, native-access flags, and JavaFX export/open arguments are already defined in module POMs; avoid reintroducing them ad hoc unless a non-Maven run requires it.

## Commit & Pull Request Guidelines
- Commit messages are short, lowercase, and descriptive.
- PRs should include:
  - a concise summary and rationale
  - linked issues or spec docs when applicable (for example `spec/...`)
  - screenshots or short clips for UI changes
  - notes describing how the change was run or validated
