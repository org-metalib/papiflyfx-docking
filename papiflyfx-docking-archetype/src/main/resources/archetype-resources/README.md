# ${artifactId}

A JavaFX desktop application built on the [PapiflyFX Docking](https://github.com/org-metalib/papiflyfx-docking) framework.

## Requirements

| Tool | Version |
|------|---------|
| Java | ${javaVersion} ([Zulu FX](https://www.azul.com/downloads/) recommended) |
| Maven | >= 3.9 (wrapper included) |
| JavaFX | ${javafxVersion} (managed via Maven) |

## Environment Setup

```bash
# Install SDKMAN (if not already installed)
curl -s "https://get.sdkman.io" | bash

# Install and activate Java ${javaVersion} with JavaFX
sdk install java 25.0.1.fx-zulu
sdk use java 25.0.1.fx-zulu

# Verify
java -version
```

## Post-Generation Setup

After generating this project from the archetype, install the Maven wrapper:

```bash
mvn wrapper:wrapper -Dmaven=${mavenVersion}
```

## Build & Run

```bash
# Compile all modules
./mvnw compile

# Full build (compile + test + package)
./mvnw clean package

# Run the application
./mvnw -pl ${rootArtifactId}-app javafx:run
```

## Tests

```bash
# Run all tests (headless by default)
./mvnw test

# Headless UI tests (CI / no display)
./mvnw -Dtestfx.headless=true test

# Interactive UI tests (requires display)
./mvnw -Dtestfx.headless=false test
```

## Project Structure

```
${rootArtifactId}/
├── pom.xml                         # root aggregator POM
├── .mvn/wrapper/                   # Maven wrapper
├── ${rootArtifactId}-app/          # main application module
│   ├── pom.xml
│   └── src/
│       ├── main/java/              # application source
│       └── test/java/              # tests
├── .github/
│   ├── workflows/ci.yml           # CI workflow
│   └── copilot-instructions.md    # Copilot context
├── AGENTS.md                       # agent team definition
├── CLAUDE.md                       # Claude Code instructions
├── README.md                       # this file
└── spec/                           # specs and planning docs
    └── agents/README.md
```

## Adding PapiflyFX Modules

The PapiflyFX BOM is already imported. Add framework modules without version tags:

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-code</artifactId>
</dependency>
```

Available modules: `papiflyfx-docking-api`, `papiflyfx-docking-docks`, `papiflyfx-docking-code`, `papiflyfx-docking-tree`, `papiflyfx-docking-media`, `papiflyfx-docking-hugo`, `papiflyfx-docking-github`, `papiflyfx-docking-settings-api`, `papiflyfx-docking-settings`, `papiflyfx-docking-login-idapi`, `papiflyfx-docking-login-session-api`, `papiflyfx-docking-login`.

## License

[Apache License, Version 2.0](LICENSE)
