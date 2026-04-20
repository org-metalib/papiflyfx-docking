# PapiflyFX Application Archetype - Research

**Lead**: @ops-engineer | **Reviewer**: @spec-steward | **Date**: 2026-04-14

## 1. Current Repository Baseline (Source of Truth)

All values extracted from the root `pom.xml` (version `0.0.19-SNAPSHOT`).

### Coordinates & Versions

| Property | Value                         |
|----------|-------------------------------|
| groupId | `org.metalib.papifly.docking` |
| artifactId | `papiflyfx-docking`           |
| version | `0.0.19-SNAPSHOT`             |
| Java (compiler release) | `25`                          |
| JavaFX | `25.0.2`                      |
| Maven (wrapper) | `3.9.12`                      |
| Maven wrapper version | `3.3.4`                       |
| Maven minimum required | `3.9`                         |
| JUnit Jupiter | `5.10.2`                      |
| TestFX | `4.0.18`                      |
| Monocle | `21.0.2`                      |
| Surefire | `3.2.5`                       |
| javafx-maven-plugin | `0.0.8`                       |
| os-maven-plugin | `1.7.1`                       |
| maven-enforcer-plugin | `3.6.2`                       |
| SDKMAN identifier | `25.0.1.fx-zulu`              |

### Build Conventions Already In Use

1. **Root-managed `dependencyManagement`** - all JavaFX, JUnit, TestFX, Monocle versions centralized in parent POM properties.
2. **Root-managed `pluginManagement`** - Surefire, javafx-maven-plugin, enforcer, release, source, javadoc, deploy, install, assembly, jar, gpg, scm, versions plugins all versioned centrally.
3. **`os-maven-plugin`** as a build extension for OS detection (`os.detected.classifier`).
4. **Platform profiles** for JavaFX classifier resolution: `mac`, `mac-aarch64`, `win`, `linux`, `linux-aarch64` - activated by `os.detected.classifier` property.
5. **`maven-enforcer-plugin`** enforcing Maven >= 3.9.
6. **Headless test defaults**: `testfx.headless=true`, `testfx.robot=glass`, `monocle.platform=Headless`, `prism.order=sw`, `prism.text=t2k`, `java.awt.headless=true` in root POM properties.
7. **`headless-tests` profile** in UI modules activated by `-Dtestfx.headless=true`, overriding `testfx.platform=Monocle`.
8. **Surefire `argLine`** in UI modules: `--enable-native-access`, `--add-exports`, `--add-opens` for JavaFX internals.
9. **`useModulePath=false`** in Surefire for UI modules.
10. **Maven wrapper** (`mvnw`/`mvnw.cmd`) with wrapper properties at `.mvn/wrapper/maven-wrapper.properties`.
11. **Release/publishing profiles**: `gpg-sign`, `maven-central-publishing` with `central-publishing-maven-plugin`.

### GitHub Actions Conventions

Two workflows exist:

| Workflow | File | Triggers | Purpose |
|----------|------|----------|---------|
| `Build Test` | `test.yaml` | push (non-main), PR (non-main), manual | `./mvnw package` headless |
| `Build` | `build.yaml` | push to main, manual | `release:prepare release:perform` to Maven Central |

Both use `actions/setup-java@v4` with `distribution: 'zulu'`, `java-version: '25.0.1'`, `java-package: 'jdk+fx'`, and `cache: 'maven'`.

### Multi-Agent Documentation

| File | Purpose |
|------|---------|
| `AGENTS.md` | Role definitions, routing rules, review gates |
| `CLAUDE.md` | Claude Code project instructions |
| `.github/copilot-instructions.md` | Copilot context |
| `spec/agents/README.md` | Shared operating protocol |
| `spec/agents/playbook.md` | Day-to-day workflow |
| `spec/agents/ops-engineer.md` | Per-role spec |
| `spec/agents/spec-steward.md` | Per-role spec |
| `spec/agents/*.md` | Other role specs |

### Module Dependency Pattern

Modules reference sibling artifacts via `${project.version}`. No BOM exists today - each consumer repeats version declarations manually. The `papiflyfx-docking-samples` module pulls 8 framework modules as direct dependencies.

## 2. BOM Analysis

### Problem

External applications that depend on PapiflyFX must repeat version numbers for every framework artifact. If an app uses `docks`, `code`, `tree`, and `settings`, that is 4+ version declarations that must stay in sync.

### Recommendation: Add `papiflyfx-docking-bom`

A BOM (Bill of Materials) module with `<packaging>pom</packaging>` that lists all publishable framework artifacts in `<dependencyManagement>`. Applications import it via `<scope>import</scope><type>pom</type>` and declare framework dependencies without version tags.

**Benefits**:
- Single version declaration for consumers
- Guarantees compatible artifact set
- Standard Maven convention (Spring Boot BOM, Jackson BOM, etc.)
- The archetype-generated app can reference one BOM version property instead of N

**Shape**:
```xml
<groupId>org.metalib.papifly.docking</groupId>
<artifactId>papiflyfx-docking-bom</artifactId>
<version>${project.version}</version>
<packaging>pom</packaging>
```

Lists all publishable artifacts (excludes `samples`):
- `papiflyfx-docking-api`
- `papiflyfx-docking-docks`
- `papiflyfx-docking-settings-api`
- `papiflyfx-docking-settings`
- `papiflyfx-docking-login-idapi`
- `papiflyfx-docking-login-session-api`
- `papiflyfx-docking-login`
- `papiflyfx-docking-code`
- `papiflyfx-docking-tree`
- `papiflyfx-docking-media`
- `papiflyfx-docking-hugo`
- `papiflyfx-docking-github`

**Note**: The BOM module should be listed first in the parent `<modules>` list so it is built before consumers. It should NOT inherit the parent POM's `<dependencyManagement>` for JavaFX etc. - it only exports framework artifact versions.

## 3. Maven Archetype Approach

Standard Maven archetype. Uses Velocity templates. Generated via `mvn archetype:generate`. Well-understood by the Maven ecosystem.

### Archetype Module Structure

```
papiflyfx-docking-archetype/
  pom.xml
  src/main/resources/
    META-INF/maven/archetype-metadata.xml
    archetype-resources/
      pom.xml                          # generated root POM
      .mvn/wrapper/maven-wrapper.properties
      mvnw
      mvnw.cmd
      app/
        pom.xml                        # generated app module POM
        src/main/java/.../App.java
        src/test/java/.../AppTest.java
      .github/workflows/ci.yml
      AGENTS.md
      CLAUDE.md
      .github/copilot-instructions.md
      spec/agents/README.md
      README.md
```

## 4. Generated Application Module Naming

The prompt suggests `main` as the application module name. However, `main` is a Java keyword and can cause confusion. **Recommendation**: Use `app` as the default module name. The archetype property `appModuleName` can override this. The startup command becomes:

```bash
./mvnw -pl app javafx:run
```