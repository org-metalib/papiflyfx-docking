# PapiflyFX Docking Agent Team

This repository is managed by a team of specialized AI agents. Each agent has a specific domain of expertise and follows the project's [SOLID principles](spec/.principals/SOLID/README.md) and [Coding Guidelines](README.md).

## Agent Roles & Responsibilities

### 1. Core Architect (@core-architect)
- **Primary Domain**: `papiflyfx-docking-api`, `papiflyfx-docking-docks`.
- **Responsibilities**:
  - Maintains the foundational docking API and its core implementation.
  - Ensures layout serialization, floating windows, ribbon shell behavior, and session persistence remain robust.
  - Guards the SOLID principles across the codebase.
  - Reviews and approves changes to shared contracts and SPIs.
- **Focus Area**: `org.metalib.papifly.fx.docks`, `org.metalib.papifly.fx.api`, `org.metalib.papifly.fx.api.ribbon`.

### 2. Feature Developer (@feature-dev)
- **Primary Domain**: Content modules (`code`, `tree`, `media`, `hugo`, `github`).
- **Responsibilities**:
  - Implements new dockable content types and maintains their functional state.
  - Ensures proper integration with `ContentFactory`, `ContentStateAdapter`, and feature-module `RibbonProvider` contributions where applicable.
  - Builds programmatic UI components and layout logic for feature docks.
  - Follows JavaFX best practices (no FXML).
- **Focus Area**: `papiflyfx-docking-code`, `papiflyfx-docking-tree`, `papiflyfx-docking-media`, `papiflyfx-docking-hugo`, `papiflyfx-docking-github`.

### 3. Build & Runtime Engineer (@ops-engineer)
- **Primary Domain**: `pom.xml`, `papiflyfx-docking-bom`, `papiflyfx-docking-archetype`, `papiflyfx-docking-settings-api`, `papiflyfx-docking-settings`, `papiflyfx-docking-samples`.
- **Responsibilities**:
  - Manages the multi-module Maven build and dependency versions.
  - Maintains the BOM, archetype templates, and release/publishing profiles.
  - Maintains the settings runtime, persistence, and secret-store backends.
  - Ensures all modules compile and tests pass in headless mode.
  - Updates demo samples to showcase new framework features.
- **Focus Area**: Root `pom.xml`, `papiflyfx-docking-bom`, `papiflyfx-docking-archetype`, `papiflyfx-docking-settings`, `papiflyfx-docking-samples`.

### 4. Auth & Security Specialist (@auth-specialist)
- **Primary Domain**: `papiflyfx-docking-login-idapi`, `papiflyfx-docking-login-session-api`, `papiflyfx-docking-login`.
- **Responsibilities**:
  - Manages identity provider (IDP) integrations and the authentication SPI.
  - Ensures secure session lifecycle and storage.
  - Implements the functional login UI and its integration with the framework.
  - Handles secret management and encrypted storage.
- **Focus Area**: `org.metalib.papifly.fx.login`.

### 5. UI/UX Designer (@ui-ux-designer)
- **Primary Domain**: `papiflyfx-docking-api` (Theme API and shared UI primitives), CSS in `code`, `tree`, `github`.
- **Responsibilities**:
  - Defines and leads the visual identity and user experience across all modules.
  - Sets standards for theme definitions, color palettes, and CSS styling.
  - Maintains and extends the shared `org.metalib.papifly.fx.ui` package (`UiMetrics`, `UiStyleSupport`, `UiCommonStyles`, `UiChip`, `UiChipToggle`, `UiChipLabel`, `UiStatusSlot`, `UiPillButton`) and `ui-common.css`.
  - Ensures all new UI chrome, overlays, and controls use `-pf-ui-*` CSS tokens from the shared token vocabulary.
  - Optimizes layout consistency, spacing, and ergonomic UI flows.
  - Reviews and audits UI implementations for accessibility and polish.
- **Focus Area**: CSS stylesheets, `Theme` implementations, `org.metalib.papifly.fx.ui` package in `papiflyfx-docking-api`, and UI component layout code.

### 6. QA & Test Engineer (@qa-engineer)
- **Primary Domain**: Test strategy, test infrastructure, coverage analysis, regression suites.
- **Responsibilities**:
  - Owns the testing approach across unit, integration, and UI levels for all modules.
  - Identifies coverage gaps, especially in session restore, floating windows, drag-and-drop, and theme switching.
  - Ensures bug fixes include regression tests and that test suites remain green and deterministic.
  - Maintains TestFX/Monocle configuration, headless profiles, and shared test utilities.
- **Focus Area**: `src/test/` across all modules, Surefire configuration, headless profiles, benchmark tags.

### 7. Spec & Delivery Steward (@spec-steward)
- **Primary Domain**: `spec/`, root `README.md`, module-level documentation, roadmap and progress documents.
- **Responsibilities**:
  - Owns task intake for ambiguous or cross-cutting work and routes it to the correct specialist.
  - Maintains `research.md`, `plan.md`, `progress.md`, and `validation.md` style artifacts under `spec/`.
  - Keeps specs, roadmap, README files, and implementation status aligned with the codebase.
  - Verifies that cross-module changes include explicit scope, acceptance criteria, and validation notes.
- **Focus Area**: `spec/**`, repository-level docs, change plans, and delivery tracking.

---

## Agent Operating Model

- Use `spec/agents/README.md` as the shared operating protocol for task routing, handoffs, review gates, and the definition of done. Supplementary resources in the same directory: `spec/agents/playbook.md` (day-to-day workflow), `spec/agents/prompts.md` (ready-to-paste session starters), and `spec/agents/cheatsheet.md` (quick routing reference).
- When a task explicitly names an agent handle, also load the corresponding role brief in `spec/agents/<role>.md` before making ownership-sensitive changes.
- Assign exactly one lead agent per task. Supporting agents may advise or review, but ownership stays with the lead until handoff.
- Route work by primary domain first:
  - Docking core, layout model, serialization, shared API/SPIs: `@core-architect`
  - Content modules and dockable features: `@feature-dev`
  - Build, BOM/archetype, settings, samples, dependency management, release readiness: `@ops-engineer`
  - Authentication, sessions, IDPs, secret handling: `@auth-specialist`
  - Theme, CSS, interaction polish, accessibility, ergonomic review: `@ui-ux-designer`
  - Test strategy, test infrastructure, coverage gaps, regression suites: `@qa-engineer`
  - Specs, roadmap, planning, progress tracking, cross-cutting coordination: `@spec-steward`
- Cross-cutting work must declare a lead plus the required reviewers before implementation starts.
- Shared contract changes require review from the owning specialist before they are considered complete.
- No agent should update the same file concurrently with another agent without an explicit handoff note.

## Delivery Workflow

- Follow the repository's spec-first workflow for non-trivial changes:
  - Research: capture findings in the relevant `spec/.../research.md` when the area is unfamiliar, risky, or architectural.
  - Planning: document the intended approach in `plan.md`, including affected modules, invariants, and validation strategy.
  - Implementation: keep `progress.md` current as major phases are completed.
  - Validation: record automated checks, manual verification, and remaining risks in `validation.md` or the progress log.
- Any task that changes public APIs, session formats, security behavior, or user-facing flows should update the relevant spec and README documentation before closing.

## Review Gates

- Changes to `papiflyfx-docking-api` or `papiflyfx-docking-docks` require `@core-architect` review.
- Changes involving secrets, tokens, OAuth, session storage, or authentication flows require `@auth-specialist` review.
- Changes to build logic, dependency versions, publishing, BOM/archetype templates, settings persistence, or sample coverage require `@ops-engineer` review.
- Changes to theme definitions, CSS, visual interaction states, or layout ergonomics require `@ui-ux-designer` review.
- Changes to roadmap items, plans, progress notes, or repository-level docs require `@spec-steward` review plus the owning domain agent for technical accuracy.
- Changes to test infrastructure, headless profiles, Surefire configuration, or test utilities require `@qa-engineer` review.
- Bug fixes should include a regression test validated by `@qa-engineer`.
- New dockable content, restorable content flows, or feature-module ribbon contributions should be reviewed by `@feature-dev` and, when shared contracts change, `@core-architect`.

## Repository Guidelines

## Project Structure & Module Organization
- Root Maven aggregator: `pom.xml` defines the multi-module build and shared dependency/plugin management.
- Shared contract modules:
  - `papiflyfx-docking-api/` - public docking/content/theme API, ribbon contribution SPI (`org.metalib.papifly.fx.api.ribbon`), and shared UI primitives (`org.metalib.papifly.fx.ui`: `UiMetrics`, `UiStyleSupport`, `UiCommonStyles`, `UiChip`, `UiChipToggle`, `UiChipLabel`, `UiStatusSlot`, `UiPillButton`, `ui-common.css`, `-pf-ui-*` CSS tokens).
  - `papiflyfx-docking-settings-api/` - public settings and secret-management SPI.
  - `papiflyfx-docking-login-idapi/` - identity-provider SPI and built-in providers.
  - `papiflyfx-docking-login-session-api/` - auth session lifecycle and storage SPI.
- Core/runtime modules:
  - `papiflyfx-docking-docks/` - docking framework, drag/drop, floating windows, minimize/maximize, session persistence, and ribbon shell hosting (`Ribbon`, `RibbonManager`, `RibbonDockHost`).
  - `papiflyfx-docking-settings/` - settings runtime, panel UI, JSON persistence, secret-store backends.
  - `papiflyfx-docking-login/` - authentication runtime, UI, and docking integration.
- Content modules:
  - `papiflyfx-docking-code/` - canvas-based code editor.
  - `papiflyfx-docking-tree/` - canvas-based virtualized tree.
  - `papiflyfx-docking-media/` - media/image/video viewer.
  - `papiflyfx-docking-hugo/` - Hugo preview content.
  - `papiflyfx-docking-github/` - GitHub toolbar integration.
- Runnable demos live in `papiflyfx-docking-samples/`.
- Distribution/bootstrap modules:
  - `papiflyfx-docking-bom/` - BOM for external consumers; keeps version alignment outside the reactor and intentionally excludes `papiflyfx-docking-samples`.
  - `papiflyfx-docking-archetype/` - Maven archetype that generates a starter multi-module app with wrapper setup, CI, agent docs, and headless TestFX scaffolding.
- Tests live under `src/test/java` per module, with archetype integration coverage under `papiflyfx-docking-archetype/src/test/resources/projects/`. Active suites currently exist in `archetype`, `code`, `docks`, `github`, `hugo`, `login-idapi`, `login`, `media`, `samples`, `settings`, and `tree`.
- Architecture and design references live under `spec/` plus module-level `README.md` files. Notable sub-directories: `spec/papiflyfx-docking-code-lang-plugin/` (completed language-plugin SPI), `spec/papiflyfx-docking-langchain/` (LangChain4j integration research), `spec/ui-standards/` (UI standardization rollout).
- Agent coordination references live under `spec/agents/`.

## Build, Test, and Development Commands
- Java setup (SDKMAN): `sdk use java 25.0.1.fx-zulu`
- Compile all modules: `./mvnw compile`
- Full build: `./mvnw clean package`
- Run all tests: `./mvnw test`
- Explicit headless test run: `./mvnw -Dtestfx.headless=true test`
- Focused module build/test: `./mvnw -pl papiflyfx-docking-docks -am test`
- Single test class: `./mvnw -pl papiflyfx-docking-samples -am -Dtest=SamplesSmokeTest test`
- Install the BOM and archetype locally for generator work: `./mvnw -pl papiflyfx-docking-bom,papiflyfx-docking-archetype -am clean install`
- Code editor benchmarks only: `./mvnw -pl papiflyfx-docking-code -am -Dtestfx.headless=true -Dgroups=benchmark -Dsurefire.excludedGroups= test`
- Run demos:
  - Docks demo: `./mvnw javafx:run -pl papiflyfx-docking-docks`
  - Samples app: `./mvnw javafx:run -pl papiflyfx-docking-samples`
- The root POM defaults `testfx.headless=true`; pass `-Dtestfx.headless=false` for interactive UI runs.
- JavaFX classifier selection is handled by OS-specific Maven profiles in the root POM; use `./mvnw`, not bare `mvn`.
- Release/publishing profiles are opt-in in the root POM: `-Dgpg-sign.profile=yes` enables source/javadoc/signing attachments and `-Dmaven-central-publishing.profile=yes` enables Sonatype Central publishing.

## Coding Style & Naming Conventions
- Java 25, 4-space indentation, standard JavaFX idioms.
- Packages use `org.metalib.papifly.fx.*`; classes are `PascalCase`, methods/fields `camelCase`.
- Favor descriptive suffixes already used in the codebase such as `*Manager`, `*Controller`, `*Factory`, `*StateAdapter`, and `*Renderer`.
- Keep new public APIs and SPIs documented with Javadoc.
- UI is programmatic JavaFX. No FXML is used. `papiflyfx-docking-api` ships shared `ui-common.css` with the `-pf-ui-*` CSS token vocabulary; `code`, `tree`, and `github` additionally ship scoped module-local stylesheets for overlays/theming. Follow the local module pattern instead of introducing new global styling.

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
- Ribbon-enabled hosts typically compose `RibbonDockHost` + `RibbonManager`; ribbon tabs are discovered through `ServiceLoader` `RibbonProvider`s such as `GitHubRibbonProvider`, `HugoRibbonProvider`, and `SampleRibbonProvider`.
- Persist ribbon Quick Access Toolbar state through `RibbonManager#getQuickAccessCommandIds()`; `getQuickAccessCommands()` is a derived view and should not be treated as the source of truth.
- For settings-backed restore and ServiceLoader flows, register the shared runtime once via `SettingsStateAdapter.setSharedRuntime(runtime)` and `DefaultSettingsServicesProvider.setSharedRuntime(runtime)` before session restore or settings discovery; `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/SamplesRuntimeSupport.java` is the reference pattern.
- Theme-aware content generally exposes `bindThemeProperty(...)`; wire it from the owning `DockManager` theme property.
- To add a new language to the code editor, implement `LanguageSupportProvider` (in `org.metalib.papifly.fx.code.language`) and register it via `ServiceLoader` in `META-INF/services`. `LexerRegistry` and `FoldProviderRegistry` no longer exist — use `LanguageSupportRegistry.bootstrap()` or `refreshServiceProviders()` for runtime hot-load. Built-in languages (`java`, `json`, `javascript`, `markdown`, `plain-text`) are shipped by `BuiltInLanguageSupportProvider`.

## Maven Module Guidelines
- Keep modules self-contained with explicit boundaries and dependency flow managed through Maven.
- Manage dependency versions in parent POM properties and plugin versions in parent `pluginManagement`.
- Manage plugin dependencies centrally in parent POMs.
- Keep `papiflyfx-docking-samples` out of the BOM and other published-consumer dependency surfaces; it is demo-only.
- Prefer `-pl <module> -am` for focused work so upstream dependencies build consistently.

## Testing Guidelines
- Frameworks: JUnit Jupiter + TestFX + Monocle.
- Naming: `*Test` for unit/integration tests and `*FxTest` for UI-heavy tests.
- `papiflyfx-docking-code` uses the `benchmark` JUnit tag for benchmarks; those are excluded by default.
- Keep UI tests deterministic and prefer headless runs in CI/sandbox environments.
- UI-heavy modules such as `docks`, `tree`, `media`, `hugo`, `github`, `login`, and `samples` ship a `headless-tests` Maven profile activated by `-Dtestfx.headless=true`; the archetype template generates the same profile for new apps.
- Use existing reference suites when touching shared flows: `RibbonShellSampleIntegrationFxTest` for ribbon-shell persistence/integration and `SettingsPanelFxTest` for settings UI/runtime regressions.
- Surefire configuration, native-access flags, and JavaFX export/open arguments are already defined in module POMs; avoid reintroducing them ad hoc unless a non-Maven run requires it.

## Commit & Pull Request Guidelines
- Commit messages are short, lowercase, and descriptive.
- PRs should include:
  - a concise summary and rationale
  - linked issues or spec docs when applicable (for example `spec/...`)
  - screenshots or short clips for UI changes
  - notes describing how the change was run or validated

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **papiflyfx-docking** (19947 symbols, 50286 relationships, 300 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> If any GitNexus tool warns the index is stale, run `npx gitnexus analyze` in terminal first.

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `gitnexus_impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `gitnexus_detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `gitnexus_query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `gitnexus_context({name: "symbolName"})`.

## Never Do

- NEVER edit a function, class, or method without first running `gitnexus_impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `gitnexus_rename` which understands the call graph.
- NEVER commit changes without running `gitnexus_detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/papiflyfx-docking/context` | Codebase overview, check index freshness |
| `gitnexus://repo/papiflyfx-docking/clusters` | All functional areas |
| `gitnexus://repo/papiflyfx-docking/processes` | All execution flows |
| `gitnexus://repo/papiflyfx-docking/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
