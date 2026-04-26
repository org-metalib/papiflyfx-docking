# Prompt — Implement GitHub And Hugo Ribbon Samples

Use this prompt to start the implementation session for `spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-4`.

```text
As @ops-engineer, lead this PapiflyFX Docking SamplesApp task.

Read these first:
- AGENTS.md
- spec/agents/README.md
- spec/agents/playbook.md
- spec/agents/ops-engineer.md
- spec/agents/feature-dev.md
- spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-4/README.md
- spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-4/plan.md
- spec/papiflyfx-docking-docks/2026-04-23-0-ribbon-4/progress.md

Task:
Add two first-class SamplesApp demos:
- GitHub Ribbon
- Hugo Ribbon

Priority:
P2 (Normal)

Implementation lead:
@ops-engineer

Required reviewers:
@feature-dev, @ui-ux-designer, @qa-engineer, @spec-steward

Impacted modules:
- papiflyfx-docking-samples
- spec/**

Expected implementation:
1. Add `GitHubRibbonSample` under `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/github/`.
2. Add `HugoRibbonSample` under `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/hugo/`.
3. Register both samples in `SampleCatalog` with exact titles:
   - `GitHub Ribbon`
   - `Hugo Ribbon`
4. Build each demo with `DockManager` + `RibbonDockHost`.
5. Expose provider action capabilities through active sample content:
   - `GitHubRibbonActions` for the GitHub demo
   - `HugoRibbonActions` for the Hugo demo
6. Keep all actions deterministic and local. Do not require credentials, network, real Git repository mutation, browser launches, or Hugo CLI installation.
7. Add focused headless test coverage for catalog registration, provider tab visibility, and one representative safe command action per demo.
8. Update `progress.md` as phases complete and record validation results.

Key invariants:
- Do not change public ribbon API/SPI contracts unless implementation proves a real gap and the spec is updated first.
- Preserve existing `Ribbon Shell`, `GitHub Toolbar`, and `Hugo Preview` samples.
- Use existing `RibbonProvider`, `RibbonDockHost`, `RibbonContext#capability(...)`, and `DockManager` active-content context propagation.
- Bind sample `DockManager` themes to the SamplesApp theme property.
- Ensure sample disposal remains compatible with `SamplesApp#disposeContentArea()`.

Validation expectations:
- `./mvnw -pl papiflyfx-docking-samples -am compile`
- `./mvnw -pl papiflyfx-docking-samples -am -Dtest=SamplesSmokeTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`
- `./mvnw -pl papiflyfx-docking-samples -am -Dtest=*Ribbon*FxTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`

Acceptance criteria:
- SamplesApp navigation includes `GitHub -> GitHub Ribbon`.
- SamplesApp navigation includes `Hugo -> Hugo Ribbon`.
- Opening `GitHub Ribbon` displays a ribbon host with the `GitHub` tab and sample-safe action handling.
- Opening `Hugo Ribbon` displays a ribbon host with the `Hugo` tab and contextual `Hugo Editor` tab for active Hugo/markdown content.
- Both demos work without external credentials, network access, repository mutation, or Hugo CLI installation.
- Headless tests cover registration and representative provider-tab visibility.

Close with:
1. Files changed
2. Validation commands and results
3. Any remaining risks
4. Handoff block using the repository handoff contract
```
