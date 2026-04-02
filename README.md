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

## Environment Variables

<!-- TODO: document any required env vars if applicable -->

No special environment variables are required for building or running. SDKMAN is used to manage the Java version (see [Requirements](#requirements)).

## Project Structure

```
papiflyfx-docking/
├── pom.xml                        # root aggregator POM
├── mvnw / mvnw.cmd                # Maven wrapper
├── papiflyfx-docking-api/         # shared API & interfaces
├── papiflyfx-docking-docks/       # core docking framework (drag-drop, tabs, floating, persistence)
├── papiflyfx-docking-code/        # code-editor docking panel
├── papiflyfx-docking-tree/        # tree-view docking panel
├── papiflyfx-docking-media/       # media (audio/video) viewer panels
├── papiflyfx-docking-samples/     # demo applications
└── spec/                          # architecture specs & design docs
    ├── papiflyfx-docking-api/
    ├── papiflyfx-docking-code/
    ├── papiflyfx-docking-docks/
    ├── papiflyfx-docking-media/
    ├── papiflyfx-docking-samples/
    └── papiflyfx-docking-tree/
```

### Module summary

| Module | Description |
|--------|-------------|
| `papiflyfx-docking-api` | Shared interfaces and data types used across modules |
| `papiflyfx-docking-docks` | Core docking/layout UI — drag-and-drop, floating windows, minimize/maximize, JSON session persistence |
| `papiflyfx-docking-code` | Code-editor panel integration <!-- TODO: confirm scope --> |
| `papiflyfx-docking-tree` | Tree-view panel component <!-- TODO: confirm scope --> |
| `papiflyfx-docking-media` | Audio and video viewer panels with transport controls |
| `papiflyfx-docking-samples` | Demo/sample applications showcasing the framework |

## Documentation

- Module specs and design docs: `spec/` directory
- Docks implementation plan: `spec/papiflyfx-docking-docks/IMPLEMENTATION_PLAN.md`

## Similar Projects

- [Drombler FX — modular application framework for JavaFX](https://www.drombler.org/drombler-fx/)
- [BentoFX — a docking system for JavaFX](https://github.com/Col-E/BentoFX)
- [DockFX — docking framework for JavaFX](https://github.com/RobertBColton/DockFX)
- [FxDock — simple docking framework with multi-monitor support](https://github.com/andy-goryachev/FxDock)

## License

[Apache License, Version 2.0](LICENSE)
