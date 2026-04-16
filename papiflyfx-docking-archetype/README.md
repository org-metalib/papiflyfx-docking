# papiflyfx-docking-archetype

Maven archetype that bootstraps a multi-module PapiflyFX docking application with CI/CD, headless testing, and a multi-agent spec-first documentation setup.

## Quick Start

### 1. Install the archetype locally

```bash
./mvnw -pl papiflyfx-docking-bom,papiflyfx-docking-archetype -am clean install
```

### 2. Generate a new project

```bash
mvn archetype:generate \
  -DarchetypeGroupId=org.metalib.papifly.docking \
  -DarchetypeArtifactId=papiflyfx-docking-archetype \
  -DarchetypeVersion=0.0.17-SNAPSHOT \
  -DgroupId=com.example \
  -DartifactId=my-app \
  -Dversion=0.1.0-SNAPSHOT \
  -Dpackage=com.example.myapp \
  -DinteractiveMode=false
```

### 3. Set up and run

```bash
cd my-app
mvn wrapper:wrapper -Dmaven=3.9.12
./mvnw clean package
./mvnw -pl my-app-app javafx:run
```

## Archetype Properties

| Property | Default | Description |
|----------|---------|-------------|
| `groupId` | *(required)* | Maven group ID for the generated project |
| `artifactId` | *(required)* | Maven artifact ID (also used as root directory name) |
| `version` | *(required)* | Initial project version |
| `package` | *(required)* | Java package for generated source files |
| `papiflyfxVersion` | `0.0.17-SNAPSHOT` | PapiflyFX framework version (BOM import version) |
| `javaVersion` | `25` | Java compiler release level |
| `javafxVersion` | `25.0.2` | JavaFX version for dependency management |
| `mavenVersion` | `3.9.12` | Maven version for the wrapper properties |

## Generated Project Structure

```
my-app/
├── pom.xml                          # root aggregator POM
├── .mvn/wrapper/
│   └── maven-wrapper.properties     # Maven 3.9.12
├── my-app-app/
│   ├── pom.xml                      # app module POM
│   └── src/
│       ├── main/java/com/example/myapp/
│       │   ├── App.java             # JavaFX Application with DockManager
│       │   └── AppLauncher.java     # main() trampoline
│       └── test/java/com/example/myapp/
│           └── AppTest.java         # TestFX smoke test
├── .github/
│   ├── workflows/ci.yml            # GitHub Actions CI
│   └── copilot-instructions.md     # Copilot context
├── AGENTS.md                        # agent team (app-dev, ops-engineer, spec-steward)
├── CLAUDE.md                        # Claude Code project instructions
├── README.md                        # setup, build, test, and run guide
└── spec/
    └── agents/
        └── README.md               # agent operating protocol
```

## What the Generated Project Includes

### Build setup
- Root aggregator POM with centralized `dependencyManagement` and `pluginManagement`
- PapiflyFX BOM import — add framework modules without version tags
- `os-maven-plugin` for automatic JavaFX platform classifier resolution
- `maven-enforcer-plugin` requiring Maven >= 3.9
- Platform profiles for macOS (x86/aarch64), Windows, and Linux (x86/aarch64)

### Application module
- `App.java` — minimal JavaFX `Application` with a `DockManager` and empty tab group
- `AppLauncher.java` — `main()` trampoline for classpath launches
- `AppTest.java` — TestFX smoke test verifying the application starts
- Surefire configured with `useModulePath=false`, `--enable-native-access`, `--add-exports`, `--add-opens`
- `headless-tests` profile activated by `-Dtestfx.headless=true`

### CI/CD
- `.github/workflows/ci.yml` — builds and tests on push/PR to `main` using Zulu JDK+FX

### Multi-agent documentation
- `AGENTS.md` — three-role agent team tailored to a new application
- `CLAUDE.md` — Claude Code instructions with build commands and project context
- `.github/copilot-instructions.md` — Copilot context
- `spec/agents/README.md` — agent operating model with intake/plan/implement/validate workflow

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 25 ([Zulu FX](https://www.azul.com/downloads/) recommended) |
| Maven | >= 3.9 |

```bash
sdk install java 25.0.1.fx-zulu
sdk use java 25.0.1.fx-zulu
```

## Adding Framework Modules

The generated project imports the PapiflyFX BOM. Add any framework module to `<artifactId>-app/pom.xml` without a version tag:

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-code</artifactId>
</dependency>
```

See [`papiflyfx-docking-bom/README.md`](../papiflyfx-docking-bom/README.md) for the full list of managed artifacts.
