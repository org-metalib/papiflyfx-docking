# Progress — JSON / YAML / Markdown Module Split

Last updated: 2026-05-09
Lead: `@spec-steward`
Implementation owner: `@feature-dev`
Linked plan: [`plan.md`](plan.md)
Linked design: [`design.md`](design.md)
Linked prompt: [`prompt.md`](prompt.md)

## Overall Status

| Phase | Name                                       | Status        |
| ----- | ------------------------------------------ | ------------- |
| 0     | Pre-flight inventory                       | not-started   |
| 1     | Scaffold `papiflyfx-docking-code-json`     | not-started   |
| 2     | Scaffold `papiflyfx-docking-code-yaml`     | not-started   |
| 3     | Scaffold `papiflyfx-docking-code-markdown` | not-started   |
| 4     | Wire samples and BOM                       | not-started   |
| 5     | Documentation sweep                        | not-started   |
| 6     | Validation and acceptance gate             | not-started   |

Status legend: `not-started`, `in-progress`, `blocked`, `completed`.

## Phase 0 — Pre-flight Inventory

Status: `not-started`

- [ ] 0.1 Walk `papiflyfx-docking-code/src/main/java` and confirm the
      file inventory in `design.md §4.4`.
- [ ] 0.2 Repository-wide grep for `JsonLexer`, `YamlLexer`,
      `MarkdownLexer`, and matching fold providers; record consumers
      that are not the moving tests or `BuiltInLanguageSupportProvider`.
- [ ] 0.3 Confirm `LanguageSupportRegistry.bootstrap` still loads
      `ServiceLoader<LanguageSupportProvider>` by default.
- [ ] 0.4 Confirm Surefire `argLine` template choice for the new modules.
- [ ] 0.5 Snapshot test counts for each affected test class.

Notes:

- _none yet_

## Phase 1 — `papiflyfx-docking-code-json`

Status: `not-started`

### 1.1 Module skeleton

- [ ] 1.1.1 Create directory `papiflyfx-docking-code-json/`.
- [ ] 1.1.2 Add the directory tree from `design.md §4`.
- [ ] 1.1.3 Add module `pom.xml`.
- [ ] 1.1.4 Add module entry to root `pom.xml`.
- [ ] 1.1.5 Verify empty-module build.

### 1.2 Move sources

- [ ] 1.2.1 Move `JsonLexer.java`.
- [ ] 1.2.2 Update `package` declaration.
- [ ] 1.2.3 Move `JsonFoldProvider.java`.
- [ ] 1.2.4 Add `package-info.java`.
- [ ] 1.2.5 Add `JsonLanguageSupportProvider.java`.
- [ ] 1.2.6 Add `META-INF/services/...LanguageSupportProvider` descriptor.
- [ ] 1.2.7 Remove `json` entry from `BuiltInLanguageSupportProvider`.
- [ ] 1.2.8 Verify core module compile.

### 1.3 Move tests

- [ ] 1.3.1 Move `JsonLexerTest` and `JsonFoldProviderTest`.
- [ ] 1.3.2 Update package declarations and imports.
- [ ] 1.3.3 Add `JsonLanguageSupportProviderTest` and discovery smoke
      test.
- [ ] 1.3.4 Run module test suite.

### 1.4 Module README and validation

- [ ] 1.4.1 Author `papiflyfx-docking-code-json/README.md`.
- [ ] 1.4.2 Run samples headless test suite.
- [ ] 1.4.3 Update progress and record deviations.

Notes:

- _none yet_

## Phase 2 — `papiflyfx-docking-code-yaml`

Status: `not-started`

- [ ] 2.1 Module skeleton (mirrors steps 1.1.1–1.1.5).
- [ ] 2.2 Move `YamlLexer` and `YamlFoldProvider`.
- [ ] 2.3 Add `YamlLanguageSupportProvider`.
- [ ] 2.4 Add `META-INF/services/...LanguageSupportProvider` descriptor.
- [ ] 2.5 Remove `yaml` entry from `BuiltInLanguageSupportProvider`.
- [ ] 2.6 Move `YamlLexerTest` and `YamlFoldProviderTest`; add provider
      and discovery tests.
- [ ] 2.7 Author module `README.md`.
- [ ] 2.8 Build and run targeted Maven verification.

Notes:

- _none yet_

## Phase 3 — `papiflyfx-docking-code-markdown`

Status: `not-started`

- [ ] 3.1 Module skeleton (mirrors steps 1.1.1–1.1.5).
- [ ] 3.2 Move `MarkdownLexer` and `MarkdownFoldProvider`.
- [ ] 3.3 Add `MarkdownLanguageSupportProvider`.
- [ ] 3.4 Add `META-INF/services/...LanguageSupportProvider` descriptor.
- [ ] 3.5 Remove `markdown` entry from `BuiltInLanguageSupportProvider`.
- [ ] 3.6 Move `MarkdownLexerTest` and `MarkdownFoldProviderTest`; add
      provider and discovery tests.
- [ ] 3.7 Author module `README.md`.
- [ ] 3.8 Build and run targeted Maven verification.

Notes:

- _none yet_

## Phase 4 — Samples and BOM Wiring

Status: `not-started`

- [ ] 4.1 Add new module dependencies in `papiflyfx-docking-samples/pom.xml`.
- [ ] 4.2 Run `papiflyfx-docking-samples` headless tests.
- [ ] 4.3 Audit `papiflyfx-docking-bom/pom.xml`.
- [ ] 4.4 Audit `papiflyfx-docking-archetype`.
- [ ] 4.5 Add full-classpath discovery test in `papiflyfx-docking-samples`.

Notes:

- _none yet_

## Phase 5 — Documentation Sweep

Status: `not-started`

- [ ] 5.1 Update `papiflyfx-docking-code/README.md` with language pack
      section.
- [ ] 5.2 Tighten each new module's `README.md` after Phases 1–3.
- [ ] 5.3 Update root `README.md` module list.
- [ ] 5.4 Update `CLAUDE.md` content modules and conventions.
- [ ] 5.5 Cross-link from `spec/papiflyfx-docking-code-lang-plugin/`.
- [ ] 5.6 Update `spec/papiflyfx-docking-code/README.md` if needed.
- [ ] 5.7 Optional roadmap updates.

Notes:

- _none yet_

## Phase 6 — Validation and Acceptance Gate

Status: `not-started`

- [ ] 6.1 `./mvnw clean package` from a fresh tree.
- [ ] 6.2 `./mvnw -Dtestfx.headless=true test` from a fresh tree.
- [ ] 6.3 Visual smoke check via `SamplesApp` for JSON, YAML, Markdown
      in dark and light themes.
- [ ] 6.4 Repo grep for old FQNs returns zero results.
- [ ] 6.5 `BuiltInLanguageSupportProvider` only registers `plain-text`,
      `java`, `javascript`.
- [ ] 6.6 Migrated test counts match Phase 0 snapshot.
- [ ] 6.7 Reviewer sign-offs:
      - [ ] `@core-architect`
      - [ ] `@ops-engineer`
      - [ ] `@ui-ux-designer`
      - [ ] `@qa-engineer`

Notes:

- _none yet_

## Decision Log

| Date       | Decision                                                                    | Rationale                                                                                                | Source             |
| ---------- | --------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- | ------------------ |
| 2026-05-09 | Keep `TokenType` in `papiflyfx-docking-code`.                               | Cross-cutting render contract; moving forces an indirection without benefit at this scope.                | `design.md §5 T1`  |
| 2026-05-09 | Keep `FoldKind` in `papiflyfx-docking-code`.                                | Same reasoning as T1.                                                                                     | `design.md §5 T2`  |
| 2026-05-09 | Keep `CodeEditorTheme` as a record in core; defer theme contribution SPI.   | Larger refactor with separate review path. Tracked as roadmap follow-up.                                  | `design.md §5 T3`  |
| 2026-05-09 | Keep `BuiltInLanguageSupportProvider` registering plain-text, java, JS.     | Ensures core editor stays usable with no language-pack modules on the classpath.                          | `design.md §5 T4`  |
| 2026-05-09 | Use existing `ServiceLoader<LanguageSupportProvider>` discovery, no SPI change. | The SPI was designed for this; new modules ship descriptors, no registry change required.            | `design.md §5 T5`  |

Append new decisions in chronological order. Keep entries terse; link to
the design or plan sections that motivate them.

## Validation Log

Record dated entries when running validation steps; include the command,
relevant output snippets, and the agent who ran it. Example:

```
2026-05-09 — @feature-dev
$ ./mvnw -pl papiflyfx-docking-code-json -am clean package
[INFO] BUILD SUCCESS
[INFO] Total time: 14.218 s
Notes: empty-module skeleton compiles; ServiceLoader descriptor pending Phase 1.2.
```

- _none yet_

## Risk Watch

| Risk                                                                                       | Status   | Last review | Owner            | Notes                                                              |
| ------------------------------------------------------------------------------------------ | -------- | ----------- | ---------------- | ------------------------------------------------------------------ |
| `META-INF/services` descriptor missing or misnamed                                         | open     | 2026-05-09  | `@feature-dev`   | Mitigation: discovery smoke test per `design.md §9.2`.             |
| Direct lexer imports break in unexpected places                                            | open     | 2026-05-09  | `@feature-dev`   | Mitigation: Phase 0 grep, migration note in module READMEs.        |
| Surefire / TestFX argLine regression on a new module                                       | open     | 2026-05-09  | `@qa-engineer`   | Mitigation: copy the core argLine when UI tests are added.         |
| Duplicate language registration if a stale `BuiltInLanguageSupportProvider` ships          | open     | 2026-05-09  | `@core-architect`| Mitigation: `ConflictPolicy.REJECT_ON_CONFLICT` will surface it.   |
| BOM / archetype drift                                                                      | open     | 2026-05-09  | `@ops-engineer`  | Phase 4 owns the audit.                                            |
| Spec docs drift (yaml-design, lang-plugin) reference old code paths                        | open     | 2026-05-09  | `@spec-steward`  | Phase 5 sweep updates references.                                  |

Update `Status` to `mitigated`, `accepted`, or `closed` once handled.

## Open Questions

Track open questions from `design.md §12` here as they are answered:

- [ ] Should `BuiltInLanguageSupportProvider` move from
      `.../folding/` to `.../language/`? (`@core-architect`)
- [ ] Should the BOM aggregate the new modules into a virtual
      `papiflyfx-docking-code-all` artifact? (`@ops-engineer`)
- [ ] Defer theme contribution SPI to a later iteration?
      (`@ui-ux-designer`)
- [ ] Place the discovery smoke test only in
      `papiflyfx-docking-samples`, or per-module? (`@qa-engineer`)

## Handoff Notes

Use this section to capture handoff context whenever ownership of a
phase moves between agents (per `spec/agents/README.md` handoff
contract). Format:

```
2026-05-09 — @spec-steward → @feature-dev
Context: design and plan approved.
Open work: Phase 0 inventory.
Blockers: none.
References: prompt.md, design.md, plan.md.
```

- _none yet_
