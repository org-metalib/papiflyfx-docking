# PapiflyFX Docking — Development Guidelines

## Build & Configuration

- **Java**: 25 (Zulu FX variant via SDKMAN). Activate before any Maven command:
  ```bash
  sdk use java 25.0.1.fx-zulu
  ```
- **Maven wrapper** (`./mvnw`) is included; requires Maven ≥ 3.9.
- **Compile all modules**:
  ```bash
  ./mvnw compile
  ```
- **Package all modules**:
  ```bash
  ./mvnw clean package
  ```
- **Single module** (with transitive deps):
  ```bash
  ./mvnw -pl papiflyfx-docking-docks -am clean package
  ```
- All dependency and plugin versions are managed in the root `pom.xml` via `<dependencyManagement>`, `<pluginManagement>`, and properties. Never declare versions in child POMs.

## Module Dependency Flow

```
api ← docks ← code (test-only) ← samples
         ↑
       media
       tree
```

Key modules: `papiflyfx-docking-api`, `papiflyfx-docking-docks`, `papiflyfx-docking-code`, `papiflyfx-docking-tree`, `papiflyfx-docking-media`, `papiflyfx-docking-samples`.

## Testing

### Frameworks & Versions
- JUnit Jupiter 5.10.2, TestFX 4.0.18, Monocle 21.0.2 (headless rendering).

### Running Tests
```bash
# All modules, headless (CI / no display)
./mvnw test -Dtestfx.headless=true

# Single module
./mvnw test -pl papiflyfx-docking-docks

# Single test class
./mvnw -Dtest=DockSessionSerializerTest test -pl papiflyfx-docking-docks

# Headless single module
./mvnw -Dtestfx.headless=true test -pl papiflyfx-docking-docks
```

### Test Naming Conventions
- `*Test` — pure unit tests (no JavaFX toolkit required, or only non-UI JavaFX types like `Orientation`).
- `*FxTest` — UI tests that start the JavaFX toolkit via TestFX; require either a display or headless mode (`-Dtestfx.headless=true`).

### Surefire Configuration Notes
- All module POMs set `useModulePath=false` in surefire config.
- JVM args include `--enable-native-access=javafx.graphics` and various `--add-exports`/`--add-opens` flags — these are already configured in each module's POM.
- Headless properties (`testfx.headless`, `testfx.robot`, `monocle.platform`, etc.) are defined in the docks POM properties and activated by passing `-Dtestfx.headless=true`.

### Writing a New Unit Test
Place test classes under `<module>/src/test/java` in the matching package. Example pure unit test (no JavaFX toolkit):

```java
package org.metalib.papifly.fx.docks.serial;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MyNewTest {
    @Test
    void exampleAssertion() {
        assertEquals(4, 2 + 2);
    }
}
```

Run with:
```bash
./mvnw -Dtest=MyNewTest test -pl papiflyfx-docking-docks
```

### Writing a New FX Test
FX tests extend `ApplicationTest` from TestFX:

```java
package org.metalib.papifly.fx.docks;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import static org.testfx.assertions.api.Assertions.assertThat;

class MyNewFxTest extends ApplicationTest {
    @Override
    public void start(Stage stage) {
        stage.setScene(new Scene(new Label("Hello"), 200, 100));
        stage.show();
    }

    @Test
    void labelIsVisible() {
        assertThat(lookup("Hello").queryLabeled()).isVisible();
    }
}
```

Run headless:
```bash
./mvnw -Dtestfx.headless=true -Dtest=MyNewFxTest test -pl papiflyfx-docking-docks
```

## Code Style

- **Java 25**, 4-space indentation, no tabs.
- Package prefix: `org.metalib.papifly.fx.*`
- Classes: `PascalCase`; methods/fields: `camelCase`.
- Descriptive class suffixes: `*Manager`, `*Controller`, `*Renderer`, `*Factory`, `*Adapter`.
- No FXML, no CSS — all UI is programmatic JavaFX.
- No external JSON library — serialization uses `java.util.Map`-based hand-rolled JSON (`DockSessionSerializer`).
- Document new public API entry points with Javadoc.

## Running Demo Applications

```bash
# Samples demo
./mvnw javafx:run -pl papiflyfx-docking-samples

# Docks standalone demo
./mvnw javafx:run -pl papiflyfx-docking-docks
```

## Architecture References

- Spec documents: `spec/` directory (per-module subdirectories).
- Module READMEs: `papiflyfx-docking-docks/README.md`, `papiflyfx-docking-code/README.md`.
- Agent guidelines: `AGENTS.md`, `CLAUDE.md`.
