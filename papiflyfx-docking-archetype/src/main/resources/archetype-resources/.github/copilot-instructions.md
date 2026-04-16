#set($h1 = '#')
#set($h2 = '##')
${h1} Copilot instructions for this repository

${h2} Repository at-a-glance
- Multi-module Maven project (parent POM at repository root).
- Modules: `${rootArtifactId}-app` (main application).
- Built on the PapiflyFX Docking framework.

${h2} Key configuration
- Java: ${javaVersion}
- JavaFX: ${javafxVersion} (managed via Maven)
- PapiflyFX: ${papiflyfxVersion} (imported via BOM)
- Uses Maven Wrapper: `./mvnw` on macOS/Linux, `mvnw.cmd` on Windows.

${h2} Frequently used commands
- Build entire project: `./mvnw clean package`
- Run tests: `./mvnw test`
- Run the application: `./mvnw -pl ${rootArtifactId}-app javafx:run`
- Headless tests (CI): `./mvnw -Dtestfx.headless=true test`

${h2} Code style
- Java ${javaVersion}, 4-space indentation, programmatic JavaFX (no FXML).
- Package prefix: `${package}`

${h2} Where to look next
- `CLAUDE.md` for full project instructions.
- `AGENTS.md` for agent roles and workflow.
- `spec/` for design and implementation notes.
