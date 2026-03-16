# Repository Guidelines

## Project Structure & Module Organization
- Root Maven aggregator: `pom.xml` defines the multi-module build.
- Modules:
  - `papiflyfx-docking-api/` — shared public API.
  - `papiflyfx-docking-docks/` — docking framework (`src/main/java/org/metalib/papifly/fx/docks`).
  - `papiflyfx-docking-code/`, `papiflyfx-docking-tree/`, `papiflyfx-docking-media/`, `papiflyfx-docking-hugo/`, `papiflyfx-docking-github/`, `papiflyfx-docking-samples/` — feature modules and runnable samples.
- Tests live under `src/test/java` per module (active suites include `papiflyfx-docking-docks`, `papiflyfx-docking-media`, and `papiflyfx-docking-github`).
- Architecture and design references are kept under `spec/` (e.g., `spec/papiflyfx-docking-docks/README.md`).

## Build, Test, and Development Commands
- Java setup (SDKMAN): `sdk use java 25.0.1.fx-zulu` before running Java/Maven commands.
- Compile all modules: `./mvnw compile`.
- Run demos:
  - Docks demo: `./mvnw javafx:run -pl papiflyfx-docking-docks`.
  - Samples demo: `./mvnw javafx:run -pl papiflyfx-docking-samples`.
- Run tests (all modules): `./mvnw test`.
- Run tests (docks only): `./mvnw test -pl papiflyfx-docking-docks`.
- Headless UI tests: `./mvnw -Dtestfx.headless=true test`.

## Coding Style & Naming Conventions
- Java 25, 4-space indentation, standard JavaFX idioms.
- Packages use `org.metalib.papifly.fx.*`; classes are `PascalCase`, methods/fields `camelCase`.
- Favor descriptive class suffixes seen in the codebase (`*Manager`, `*Controller`, `*Renderer`).
- Keep public APIs documented with Javadoc when introducing new entry points.

## Maven Module Guidelines
- Modules should be self-contained and have a single responsibility, unless they are tightly coupled.
- Modules should be independent of other modules, with clear boundaries and dependencies managed through Maven.
- All versions of a module dependencies should be manage in parent poms.
- All plugin versions should be manage in parent poms with pluginManagement section.
- All plugin dependencies should be managed in parent poms via properties.

## Testing Guidelines
- Frameworks: JUnit Jupiter + TestFX (UI tests).
- Naming: `*Test` and UI-focused `*FxTest` under module `src/test/java` directories.
- Prefer deterministic UI tests; use the headless flag for CI or non-GUI environments.

## Commit & Pull Request Guidelines
- Commit messages are short, lowercase, and descriptive (no strict conventional-commit format).
- PRs should include:
  - A concise summary and rationale.
  - Linked issues or spec docs when applicable (e.g., `spec/…`).
  - Screenshots or short clips for UI changes.
  - Notes on how to run or validate the change.
