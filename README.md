# papiflyfx-docking

PapiflyFX Docking Framework. A multi-module Java/JavaFX project for IDE-style docking layouts, built around the `papiflyfx-docking-docks` module.

## Modules

- `papiflyfx-docking-docks`: Docking/layout UI components, drag-and-drop, floating windows, minimize/maximize, and JSON session persistence.

## Requirements

- Java 25
- Maven 3.12+
- JavaFX 23.0.1 (managed via Maven)

## Build and test

```bash
mvn clean package
mvn test
```

### Module build/test (docks)

```bash
mvn -pl papiflyfx-docking-docks -am clean package
mvn -pl papiflyfx-docking-docks -am test
```

### Run the JavaFX demo (docks)

```bash
mvn javafx:run -pl papiflyfx-docking-docks
```

## Documentation

- Module README: `papiflyfx-docking-docks/README.md`
- Specs and plans: `spec/papiflyfx-docking-docks/`
- Implementation plan: `spec/papiflyfx-docking-docks/IMPLEMENTATION_PLAN.md`

## References
- Architecture spec: `spec/papiflyfx-docks/README.md`
- Implementation plan: `spec/papiflyfx-docks/IMPLEMENTATION_PLAN.md`

## Similar Projects
- [Drombler FX The modular application framework for JavaFX.](https://www.drombler.org/drombler-fx/)
- [BentoFX: A docking system for JavaFX](https://github.com/Col-E/BentoFX)
- [DockFX: docking frameworks available in the JavaFX RIA platform](https://github.com/RobertBColton/DockFX)
- [FxDock: simple docking framework that works well on Mac, Windows, and Linux with multiple monitors](https://github.com/andy-goryachev/FxDock)