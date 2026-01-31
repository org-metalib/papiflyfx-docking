# Copilot instructions for this repository

Purpose
- Give Copilot short, practical context about this repo so completions are relevant and actionable.

Repository at-a-glance
- Multi-module Maven project (parent POM at repository root).
- Modules:
  - `papiflyfx-docks` — docking/layout UI components and tests
- Project docs and plans live under `spec/` (implementation plans, UX notes, reports).

Key configuration
- Java (Maven) source/target: 25 (see `<maven.compiler.source>` / `<maven.compiler.target>` in root `pom.xml`).
- JavaFX version managed: 23.0.1
- TA4J version managed: 0.21.0
- Uses Maven Wrapper: use `./mvnw` on macOS/Linux or `mvnw.cmd` on Windows.

Frequently used commands
- Build entire project: `mvn clean package`
- Run tests for entire project: `mvn test`
- Build or test a single module (example for docks):
  - Build module: `mvn -pl papiflyfx-docks -am clean package`
  - Test module: `mvn -pl papiflyfx-docks -am test`
- Run a specific test class with surefire/failsafe: `mvn -Dtest=FullyQualifiedTestName test`

Notes about Java / JavaFX runtime
- The project compiler properties set Java 25. Ensure your JDK matches or adjust `maven.compiler.source`/`target` in the POM.
- JavaFX is used; to run JavaFX apps locally in the IDE or from command line you may need to add JavaFX modules to the module path. IDEs like IntelliJ can configure this automatically when the correct JavaFX SDK is set.

Testing and CI guidance
- Unit & integration tests live under `src/test/java` in each module. Check `target/surefire-reports` for JUnit output after runs.
- Tests in this repo sometimes exercise JavaFX UI pieces; these may require a headless-friendly setup (or a CI runner with display). If UI tests fail on CI, try configuring a headless JFX environment or mock UI dependencies.

Useful search keywords for Copilot (help it find relevant code)
- `DockManager`, `DockTabGroup`, `DemoAppLayout`, `LayoutSerializer`, `LayoutPersistence`, `ta4j`, `javafx`, `org.metalib.papifly.fx.docks`, `org.metalib.papifly.fx.chart`
- File globs:
  - `**/papiflyfx-docks/**`
  - `spec/**` for design notes and implementation plans

Code style & PR tips
- Keep changes module-scoped when possible (edit only the relevant submodule).
- Add or update unit tests for behavioral changes. Small, focused tests are preferred.
- When introducing JavaFX UI code, include simple non-UI unit tests if possible to keep CI stable.

If something looks wrong
- If builds fail locally, run `mvn -X` for verbose Maven debug output.
- Confirm JDK version with `java -version` and `mvn -v` to show Maven & environment details.

Assumptions
- If your environment lacks JDK 25 with javafx, adapt the POM to a supported JDK or install a matching 
  JDK (SDKMAN, Homebrew, or vendor installers).

Where to look next
- Top-level `pom.xml` for managed versions and modules.
- `spec/` for design and implementation notes.
- Module `README.md` files for module-specific info (for example `papiflyfx-docking-docks/README.md`).

Quick checklist for common tasks (for Copilot to suggest actionable snippets)
- Build: run `mvn clean package`
- Run tests for a module: `mvn -pl <module> -am test`
- Search for docking code: look for `DockManager`, `DockTabGroup`, `Layout*` classes

End of instructions
