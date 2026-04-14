# papiflyfx-docking

A multi-module Java/JavaFX framework for IDE-style docking layouts — drag-and-drop panels, floating windows, minimize/maximize, tab groups, and JSON session persistence.

## Overview

PapiflyFX Docking provides composable UI building blocks for desktop applications that need flexible, resizable, dockable panel layouts (similar to IntelliJ IDEA, VS Code, or Eclipse). The project is organized as a Maven multi-module build targeting **Java 25** with **JavaFX 25.0.2**.

## Requirements

| Tool | Version |
|------|---------|
| Java | 25 ([Zulu FX](https://www.azul.com/downloads/) recommended) |
| Maven | ≥ 3.9 (wrapper included — `./mvnw`) |
| JavaFX | 25.0.2 (managed via Maven, auto-resolved per platform) |

### Java setup (SDKMAN)

```bash
sdk use java 25.0.1.fx-zulu
```

## Build & Run

```bash
# compile all modules
./mvnw compile

# full build (compile + test + package)
./mvnw clean package
```

### Run demos

```bash
# docks demo
./mvnw javafx:run -pl papiflyfx-docking-docks

# samples demo
./mvnw javafx:run -pl papiflyfx-docking-samples
```

## Tests

The project uses **JUnit Jupiter 5** and **TestFX** for UI tests.

```bash
# run all tests
./mvnw test

# run tests for a single module
./mvnw test -pl papiflyfx-docking-docks

# headless UI tests (CI / no display)
./mvnw -Dtestfx.headless=true test

# headless for a single module
./mvnw -Dtestfx.headless=true test -pl papiflyfx-docking-docks
```

## UI Standards

PapiflyFX Docking uses a single runtime styling source: [`Theme`](./papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/docking/api/Theme.java). Cross-module UI standards are projected from that API through shared helpers in `papiflyfx-docking-api` rather than through separate per-module theme systems.

Shared UI standards are implemented in:

- `org.metalib.papifly.fx.ui.UiMetrics` for the shared 4px spacing grid and standard heights/radii
- `org.metalib.papifly.fx.ui.UiStyleSupport` and `UiCommonStyles` for `Theme` -> CSS variable projection and stylesheet loading
- `org.metalib.papifly.fx.ui.ui-common.css` for shared popup, field, chip, pill, icon-button, and status-slot styling
- `org.metalib.papifly.fx.ui.UiPillButton`, `UiChip`, `UiChipToggle`, `UiChipLabel`, and `UiStatusSlot` for lightweight reusable JavaFX primitives

Current shared metrics:

- Spacing scale: `4 / 8 / 12 / 16 / 20 / 24`
- Radii: `4 / 8 / 12 / 999`
- Standard heights: compact controls `24`, regular controls `28`, toolbar minimum `44`

Shared CSS variables use the `-pf-ui-*` prefix. Examples include:

- Surface tokens such as `-pf-ui-surface-overlay` and `-pf-ui-surface-control`
- Text tokens such as `-pf-ui-text-primary` and `-pf-ui-text-muted`
- Border/status tokens such as `-pf-ui-border-focus`, `-pf-ui-accent`, `-pf-ui-success`, `-pf-ui-warning`, and `-pf-ui-danger`
- Metric tokens such as `-pf-ui-space-*`, `-pf-ui-radius-*`, and `-pf-ui-control-height-*`

Module guidance:

- Bind dock content to `ObjectProperty<Theme>` and keep that property as the only runtime theme source.
- Prefer shared `Ui*` controls and `ui-common.css` surface classes before introducing module-local styling.
- Keep canvas layout metrics aligned to `UiMetrics` or theme-derived dimensions instead of local ad hoc numbers.

The rollout plan and token reference live in [`spec/ui-standards/research.md`](./spec/ui-standards/research.md) and [`spec/ui-standards/plan.md`](./spec/ui-standards/plan.md).

## Environment Variables

<!-- TODO: document any required env vars if applicable -->

No special environment variables are required for building or running. SDKMAN is used to manage the Java version (see [Requirements](#requirements)).

## Project Structure

```
papiflyfx-docking/
├── pom.xml                        # root aggregator POM
├── mvnw / mvnw.cmd                # Maven wrapper
├── papiflyfx-docking-api/         # shared API & interfaces
├── papiflyfx-docking-settings-api/ # settings and secret-management SPI
├── papiflyfx-docking-docks/       # core docking framework (drag-drop, tabs, floating, persistence)
├── papiflyfx-docking-settings/    # settings runtime and persistence
├── papiflyfx-docking-login-idapi/ # login provider SPI
├── papiflyfx-docking-login-session-api/ # auth session SPI
├── papiflyfx-docking-login/       # login runtime and UI
├── papiflyfx-docking-code/        # code-editor docking panel
├── papiflyfx-docking-tree/        # tree-view docking panel
├── papiflyfx-docking-media/       # media (audio/video) viewer panels
├── papiflyfx-docking-hugo/        # Hugo preview panel
├── papiflyfx-docking-github/      # GitHub toolbar/content integration
├── papiflyfx-docking-samples/     # demo applications
└── spec/                          # architecture specs & design docs
    ├── papiflyfx-docking-api/
    ├── papiflyfx-docking-code/
    ├── papiflyfx-docking-docks/
    ├── papiflyfx-docking-github/
    ├── papiflyfx-docking-hugo/
    ├── papiflyfx-docking-media/
    ├── papiflyfx-docking-settings/
    ├── papiflyfx-docking-samples/
    └── papiflyfx-docking-tree/
```

### Module summary

| Module | Description |
|--------|-------------|
| `papiflyfx-docking-api` | Shared docking API plus `Theme`, shared UI metrics, CSS token helpers, and lightweight reusable UI primitives |
| `papiflyfx-docking-settings-api` | Settings and secret-management SPI |
| `papiflyfx-docking-docks` | Core docking/layout UI — drag-and-drop, floating windows, minimize/maximize, JSON session persistence |
| `papiflyfx-docking-settings` | Composable settings runtime, event-driven UI, tokenized styling, and secure secret storage |
| `papiflyfx-docking-login-idapi` | Identity-provider SPI and built-in provider contracts |
| `papiflyfx-docking-login-session-api` | Authentication session lifecycle and storage SPI |
| `papiflyfx-docking-login` | Login runtime and UI integration |
| `papiflyfx-docking-code` | Canvas-based code editor with search/go-to-line overlays and runtime theme binding |
| `papiflyfx-docking-tree` | Canvas-based virtualized tree with search, inline info, and runtime theme binding |
| `papiflyfx-docking-media` | Image, SVG, audio, video, and embedded viewers with theme-aware host controls |
| `papiflyfx-docking-hugo` | Hugo preview content with theme-aware host chrome and embedded `WebView` rendering |
| `papiflyfx-docking-github` | GitHub workflow toolbar using shared pills, chips, popup surfaces, and status slots |
| `papiflyfx-docking-samples` | Demo/sample applications showcasing the framework |

## Documentation

- Module specs and design docs: `spec/` directory
- UI standards research and rollout plan: `spec/ui-standards/research.md`, `spec/ui-standards/plan.md`
- Agent operating model, playbook, prompt pack, and cheat sheet: `spec/agents/README.md`, `spec/agents/playbook.md`, `spec/agents/prompts.md`, and `spec/agents/cheatsheet.md`

## Similar Projects

- [Drombler FX — modular application framework for JavaFX](https://www.drombler.org/drombler-fx/)
- [BentoFX — a docking system for JavaFX](https://github.com/Col-E/BentoFX)
- [DockFX — docking framework for JavaFX](https://github.com/RobertBColton/DockFX)
- [FxDock — simple docking framework with multi-monitor support](https://github.com/andy-goryachev/FxDock)

## License

[Apache License, Version 2.0](LICENSE)
