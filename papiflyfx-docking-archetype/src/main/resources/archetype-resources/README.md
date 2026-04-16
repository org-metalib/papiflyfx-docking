#set($h1 = '#')
#set($h2 = '##')
#set($h3 = '###')
${h1} ${artifactId}

A JavaFX desktop application built on the [PapiflyFX Docking](https://github.com/org-metalib/papiflyfx-docking) framework.

${h2} Requirements

| Tool | Version |
|------|---------|
| Java | ${javaVersion} ([Zulu FX](https://www.azul.com/downloads/) recommended) |
| Maven | >= 3.9 (wrapper included) |
| JavaFX | ${javafxVersion} (managed via Maven) |

${h2} Environment Setup

```bash
${h1} Install SDKMAN (if not already installed)
curl -s "https://get.sdkman.io" | bash

${h1} Install and activate Java ${javaVersion} with JavaFX
sdk install java 25.0.1.fx-zulu
sdk use java 25.0.1.fx-zulu

${h1} Verify
java -version
```

${h2} Post-Generation Setup

After generating this project from the archetype, install the Maven wrapper:

```bash
mvn wrapper:wrapper -Dmaven=${mavenVersion}
```

${h2} Build & Run

```bash
${h1} Compile all modules
./mvnw compile

${h1} Full build (compile + test + package)
./mvnw clean package

${h1} Run the application
./mvnw -pl ${rootArtifactId}-app javafx:run
```

${h2} Tests

```bash
${h1} Run all tests (headless by default)
./mvnw test

${h1} Headless UI tests (CI / no display)
./mvnw -Dtestfx.headless=true test

${h1} Interactive UI tests (requires display)
./mvnw -Dtestfx.headless=false test
```

${h2} Project Structure

```
${rootArtifactId}/
├── pom.xml                         ${h1} root aggregator POM
├── .mvn/wrapper/                   ${h1} Maven wrapper
├── ${rootArtifactId}-app/          ${h1} main application module
│   ├── pom.xml
│   └── src/
│       ├── main/java/              ${h1} application source
│       └── test/java/              ${h1} tests
├── .github/
│   ├── workflows/ci.yml           ${h1} CI workflow
│   └── copilot-instructions.md    ${h1} Copilot context
├── AGENTS.md                       ${h1} agent team definition
├── CLAUDE.md                       ${h1} Claude Code instructions
├── README.md                       ${h1} this file
└── spec/                           ${h1} specs and planning docs
    └── agents/README.md
```

${h2} Adding PapiflyFX Modules

The PapiflyFX BOM is already imported. Add framework modules without version tags:

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-code</artifactId>
</dependency>
```

Available modules: `papiflyfx-docking-api`, `papiflyfx-docking-docks`, `papiflyfx-docking-code`, `papiflyfx-docking-tree`, `papiflyfx-docking-media`, `papiflyfx-docking-hugo`, `papiflyfx-docking-github`, `papiflyfx-docking-settings-api`, `papiflyfx-docking-settings`, `papiflyfx-docking-login-idapi`, `papiflyfx-docking-login-session-api`, `papiflyfx-docking-login`.
