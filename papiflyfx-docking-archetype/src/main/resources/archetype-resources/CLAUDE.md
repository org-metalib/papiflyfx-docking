# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Agent Team

This repository uses a multi-agent model defined in [`AGENTS.md`](AGENTS.md). When working here, identify which agent role applies and operate within that role's domain.

## Project Overview

${artifactId} is a JavaFX desktop application built on the PapiflyFX Docking framework.

- groupId: `${groupId}`
- version: `${version}`
- Java: `${javaVersion}`
- JavaFX: `${javafxVersion}`
- PapiflyFX: `${papiflyfxVersion}`
- Maven: `${mavenVersion}` via `./mvnw`
- Package prefix: `${package}`

## Build Commands

```bash
# Java setup (SDKMAN)
sdk use java 25.0.1.fx-zulu

# Compile all modules
./mvnw compile

# Full build
./mvnw clean package

# Test all modules
./mvnw test

# Headless test run (CI mode, default)
./mvnw -Dtestfx.headless=true test

# Interactive UI test run
./mvnw -Dtestfx.headless=false test

# Run the application
./mvnw -pl ${rootArtifactId}-app javafx:run

# Focused module build/test
./mvnw -pl ${rootArtifactId}-app -am clean package
```

## Module Structure

- Root `pom.xml` - aggregator with dependency/plugin management
- `${rootArtifactId}-app/` - main application module with JavaFX entry point

## Dependency Management

PapiflyFX framework dependencies are managed via the `papiflyfx-docking-bom` BOM import. Add framework modules without version tags:

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-code</artifactId>
</dependency>
```

## Working Conventions

- Use `./mvnw`, not bare `mvn`.
- Keep dependency versions centralized in the parent POM properties.
- UI is programmatic JavaFX (no FXML).
- Tests default to headless mode. Pass `-Dtestfx.headless=false` for interactive runs.

## Testing Notes

- Test stack: JUnit Jupiter, TestFX, Monocle.
- Surefire disables the module path (`useModulePath=false`) and includes `--enable-native-access`, `--add-exports`, and `--add-opens` flags.
- The `headless-tests` profile activates on `-Dtestfx.headless=true`.
