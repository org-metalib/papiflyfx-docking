#set($h1 = '#')
#set($h2 = '##')
#set($h3 = '###')
${h1} CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

${h2} Agent Team

This repository uses a multi-agent model defined in [`AGENTS.md`](AGENTS.md). When working here, identify which agent role applies and operate within that role's domain.

${h2} Project Overview

${artifactId} is a JavaFX desktop application built on the PapiflyFX Docking framework.

- groupId: `${groupId}`
- version: `${version}`
- Java: `${javaVersion}`
- JavaFX: `${javafxVersion}`
- PapiflyFX: `${papiflyfxVersion}`
- Maven: `${mavenVersion}` via `./mvnw`
- Package prefix: `${package}`

${h2} Build Commands

```bash
${h1} Java setup (SDKMAN)
sdk use java 25.0.1.fx-zulu

${h1} Compile all modules
./mvnw compile

${h1} Full build
./mvnw clean package

${h1} Test all modules
./mvnw test

${h1} Headless test run (CI mode, default)
./mvnw -Dtestfx.headless=true test

${h1} Interactive UI test run
./mvnw -Dtestfx.headless=false test

${h1} Run the application
./mvnw -pl ${rootArtifactId}-app javafx:run

${h1} Focused module build/test
./mvnw -pl ${rootArtifactId}-app -am clean package
```

${h2} Module Structure

- Root `pom.xml` - aggregator with dependency/plugin management
- `${rootArtifactId}-app/` - main application module with JavaFX entry point

${h2} Dependency Management

PapiflyFX framework dependencies are managed via the `papiflyfx-docking-bom` BOM import. Add framework modules without version tags:

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-code</artifactId>
</dependency>
```

${h2} Working Conventions

- Use `./mvnw`, not bare `mvn`.
- Keep dependency versions centralized in the parent POM properties.
- UI is programmatic JavaFX (no FXML).
- Tests default to headless mode. Pass `-Dtestfx.headless=false` for interactive runs.

${h2} Testing Notes

- Test stack: JUnit Jupiter, TestFX, Monocle.
- Surefire disables the module path (`useModulePath=false`) and includes `--enable-native-access`, `--add-exports`, and `--add-opens` flags.
- The `headless-tests` profile activates on `-Dtestfx.headless=true`.
