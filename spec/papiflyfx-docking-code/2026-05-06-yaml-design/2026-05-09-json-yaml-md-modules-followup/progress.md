# Progress - Language Pack Follow-ups

Last updated: 2026-05-09
Lead: `@spec-steward`
Implementation owner: `@feature-dev`
Linked prompt: [`prompt.md`](prompt.md)
Linked design: [`design.md`](design.md)
Linked plan: [`plan.md`](plan.md)

## Overall Status

| Phase | Name | Status |
| ----- | ---- | ------ |
| 0 | Pre-flight inventory | not-started |
| 1 | Syntax style SPI in core | not-started |
| 2 | Migrate JSON / YAML / Markdown style scopes | not-started |
| 3 | Split Java language pack | not-started |
| 4 | Split JavaScript language pack | not-started |
| 5 | Per-language settings | not-started |
| 6 | Documentation and build topology sweep | not-started |
| 7 | Validation and acceptance gate | not-started |

Status legend: `not-started`, `in-progress`, `blocked`, `completed`.

## Phase 0 - Pre-flight Inventory

Status: `not-started`

- [ ] 0.1 Inspect current token/theme/render APIs.
- [ ] 0.2 Inspect `LanguageSupport.customTokenScopes()` and registry
      behavior.
- [ ] 0.3 Inventory Java and JavaScript source/test files and consumers.
- [ ] 0.4 Inventory settings code and settings UI support.
- [ ] 0.5 Snapshot affected test counts.

## Phase 1 - Syntax Style SPI in Core

Status: `not-started`

- [ ] 1.1 Add `SyntaxStyleScope`, `SyntaxStyleProvider`, and
      `SyntaxStyleRegistry`.
- [ ] 1.2 Add ServiceLoader discovery tests.
- [ ] 1.3 Extend `Token` with optional `styleScope`.
- [ ] 1.4 Add dynamic syntax-scope colors to `CodeEditorTheme`.
- [ ] 1.5 Merge style defaults in `CodeEditorThemeMapper`.
- [ ] 1.6 Resolve style-scope colors first in `TextPass`.
- [ ] 1.7 Add core regression tests.
- [ ] 1.8 Run focused core tests.

## Phase 2 - JSON / YAML / Markdown Style Scope Migration

Status: `not-started`

- [ ] 2.1 Add syntax style providers to JSON, YAML, and Markdown modules.
- [ ] 2.2 List owned scopes in each `LanguageSupport`.
- [ ] 2.3 Emit `json.key` from JSON lexer output.
- [ ] 2.4 Emit `yaml.*` scopes from YAML lexer output.
- [ ] 2.5 Emit `markdown.*` scopes from Markdown lexer output.
- [ ] 2.6 Decide deprecation timing for old language-specific `TokenType`
      constants.
- [ ] 2.7 Update module tests.
- [ ] 2.8 Run targeted language module tests.

## Phase 3 - Java Language Pack

Status: `not-started`

- [ ] 3.1 Create `papiflyfx-docking-code-java`.
- [ ] 3.2 Move Java lexer, fold provider, and tests.
- [ ] 3.3 Add `JavaLanguageSupportProvider`.
- [ ] 3.4 Remove Java from `BuiltInLanguageSupportProvider`.
- [ ] 3.5 Add provider and discovery tests.
- [ ] 3.6 Update reactor/BOM as needed.
- [ ] 3.7 Run targeted Java module tests.

## Phase 4 - JavaScript Language Pack

Status: `not-started`

- [ ] 4.1 Create `papiflyfx-docking-code-javascript`.
- [ ] 4.2 Move JavaScript lexer, fold provider, and tests.
- [ ] 4.3 Add `JavaScriptLanguageSupportProvider`.
- [ ] 4.4 Remove JavaScript from `BuiltInLanguageSupportProvider`.
- [ ] 4.5 Add provider and discovery tests.
- [ ] 4.6 Update reactor/BOM as needed.
- [ ] 4.7 Run targeted JavaScript module tests.

## Phase 5 - Per-language Settings

Status: `not-started`

- [ ] 5.1 Add `LanguageEditorSettings` and resolver.
- [ ] 5.2 Define documented settings keys.
- [ ] 5.3 Add editor properties for language settings.
- [ ] 5.4 Apply settings defaults through `EditorSettingsSupport`.
- [ ] 5.5 Update `EditorCategory` UI.
- [ ] 5.6 Wire behavior where supported or document property-only scope.
- [ ] 5.7 Add resolver/editor/settings UI tests.
- [ ] 5.8 Run settings and samples tests.

## Phase 6 - Documentation and Build Topology Sweep

Status: `not-started`

- [ ] 6.1 Update samples dependencies and discovery tests.
- [ ] 6.2 Audit root POM, BOM, and archetype docs.
- [ ] 6.3 Update `papiflyfx-docking-code/README.md`.
- [ ] 6.4 Update language-pack READMEs.
- [ ] 6.5 Update root `README.md`, `CLAUDE.md`, and code spec index.
- [ ] 6.6 Cross-link from code language plugin spec.
- [ ] 6.7 Record final decisions here.

## Phase 7 - Validation and Acceptance Gate

Status: `not-started`

- [ ] 7.1 `./mvnw clean package`
- [ ] 7.2 `./mvnw -Dtestfx.headless=true test`
- [ ] 7.3 Targeted language-pack tests.
- [ ] 7.4 Samples headless tests.
- [ ] 7.5 Grep audit for moved Java / JavaScript classes and core built-ins.
- [ ] 7.6 Visual smoke in dark and light themes.
- [ ] 7.7 Reviewer sign-offs:
      - [ ] `@core-architect`
      - [ ] `@ops-engineer`
      - [ ] `@ui-ux-designer`
      - [ ] `@qa-engineer`

## Decision Log

| Date | Decision | Rationale | Source |
| ---- | -------- | --------- | ------ |
| 2026-05-09 | Treat this as one follow-up initiative with `@spec-steward` lead. | The work crosses public editor API, modules, theme behavior, settings, docs, and tests. | `prompt.md` |
| 2026-05-09 | Preserve public token and theme compatibility in the first implementation. | Removing enum constants or record fields would be a breaking API change and is not required to let modules own new style scopes. | `design.md` section 5 |
| 2026-05-09 | Add semantic style scopes before moving Java / JavaScript. | New and moved language modules should target the same long-term style API. | `plan.md` |
| 2026-05-09 | Keep dynamic fold kinds out of this scope. | The source follow-up calls for theme contribution, not fold model extensibility. | `design.md` section 3 |

Append new decisions in chronological order. Keep entries terse and link to
the relevant design or plan section.

## Validation Log

Record dated entries when running validation. Include command, outcome, and
agent. Example:

```text
2026-05-09 - @feature-dev
$ ./mvnw -pl papiflyfx-docking-code -am test
Result: BUILD SUCCESS
Notes: core token/theme changes pass focused tests.
```

No validation has been run for this follow-up yet. This task created planning
artifacts only.

## Risk Watch

| Risk | Status | Owner | Notes |
| ---- | ------ | ----- | ----- |
| `CodeEditorTheme` record compatibility break | open | `@core-architect` | Requires compatibility constructors/factories or a staged migration. |
| Style provider descriptor missing | open | `@qa-engineer` | Guard with per-module and samples discovery tests. |
| Palette drift after dynamic scope migration | open | `@ui-ux-designer` | Assert existing dark/light defaults and run visual smoke. |
| Java / JavaScript module classpath drift | open | `@ops-engineer` | Preserve packages, update samples and BOM explicitly. |
| Settings preferences resolve but do not affect editor behavior | open | `@feature-dev` | Document property-only scope if no save/indent hook exists yet. |

Update `Status` to `mitigated`, `accepted`, or `closed` once handled.

## Open Questions

- [ ] Should `SyntaxStyleProvider` live under `...code.theme` or a new
      `...code.style` package? (`@core-architect`)
- [ ] Should old language-specific `TokenType` constants be deprecated
      immediately after migrated lexers stop emitting them? (`@core-architect`)
- [ ] Should default style colors exactly preserve current JSON/YAML/Markdown
      palettes or receive a new visual review pass? (`@ui-ux-designer`)
- [ ] Should Java and JavaScript language packs remain independent only, or
      should a future aggregate artifact be considered? (`@ops-engineer`)
- [ ] Should trailing-newline policy be property-only until a save pipeline
      exists? (`@qa-engineer`)

## Handoff Notes

Use this section for ownership transitions per `spec/agents/README.md`.

```text
Lead Agent:
Task Scope:
Impacted Modules:
Files Changed:
Key Invariants:
Validation Performed:
Open Risks / Follow-ups:
Required Reviewer:
```

- 2026-05-09 - `@spec-steward` -> `@feature-dev`
  Context: follow-up prompt, design, and plan are created for the source
  module split's out-of-scope roadmap items.
  Open work: implement Phases 0-7.
  Blockers: reviewer decisions in `Open Questions` should be resolved before
  public API edits.
  References: `prompt.md`, `design.md`, `plan.md`.
