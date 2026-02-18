# API Review 0 (Codex) - `papiflyfx-docking-api`

Date: 2026-02-18  
Scope: Validate `spec/papiflyfx-docking-api/README.md` proposal against current code in `papiflyfx-docking-docks` and `papiflyfx-docking-code`.

## Verdict

The proposal is directionally correct and solves the current compile-time coupling from `code` to `docks`, but it is not implementation-complete yet. Two high-impact migration constraints must be handled explicitly:

1. package strategy (avoid split packages),
2. test-scope dependency strategy for `papiflyfx-docking-code`.

## What Is Confirmed

1. Coupling is currently unidirectional (`code` -> `docks`).
- `papiflyfx-docking-code/pom.xml:27`
- `papiflyfx-docking-code/pom.xml:30`

2. Main-source coupling points in `code` are exactly the five API candidates listed in the proposal.
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorFactory.java:4`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorStateAdapter.java:6`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorStateAdapter.java:7`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:35`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:36`

3. The selected types are actively used by docking runtime/public API and are valid extraction candidates.
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:22`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:24`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:30`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:40`

## Findings

### 1. HIGH - "Keep package names" causes split-package risk for `layout` and `layout.data`

If only these classes move to `api` while keeping current packages, both artifacts will contain:
- `org.metalib.papifly.fx.docks.layout` (already also contains `LayoutFactory`, `ContentStateRegistry`),
- `org.metalib.papifly.fx.docks.layout.data` (already also contains `LayoutNode`, `LeafData`, etc.).

Evidence:
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/LayoutFactory.java:1`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/ContentStateRegistry.java:1`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/data/LeafData.java:1`

Impact:
- split packages are fragile and JPMS-incompatible.

Recommendation:
- Prefer new API package namespace (for example `org.metalib.papifly.fx.docking.api.*`) for extracted classes.

### 2. HIGH - "code no longer depends on docks" is not true for current tests

Main code can depend only on `api`, but current `code` tests directly import docking runtime classes.

Evidence:
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorDockingIntegrationTest.java:11`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorDockingIntegrationTest.java:14`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorIntegrationTest.java:14`

Impact:
- removing compile-scope `docks` dependency without adding test-scope replacement breaks test compilation.

Recommendation:
- Keep `docks` as `test` scope in `papiflyfx-docking-code`, or move docking integration tests to `papiflyfx-docking-docks`.

### 3. MEDIUM - ServiceLoader descriptor migration is missing from plan

`CodeEditorStateAdapter` is currently published via service descriptor keyed by current interface FQCN.

Evidence:
- `papiflyfx-docking-code/src/main/resources/META-INF/services/org.metalib.papifly.fx.docks.layout.ContentStateAdapter:1`

Impact:
- if package names change, adapter auto-discovery silently fails unless the descriptor filename is updated.

Recommendation:
- Add explicit migration task for `META-INF/services` descriptor path update.

### 4. MEDIUM - `DisposableContent` carries a dock-core Javadoc link

`DisposableContent` Javadoc references `DockLeaf` directly.

Evidence:
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/DisposableContent.java:5`

Impact:
- moving this interface into `api` without doc cleanup leaves undesirable back-reference to `docks` internals.

Recommendation:
- remove/replace the `DockLeaf` Javadoc link with neutral wording.

### 5. MEDIUM - Moving full `Theme` to API freezes a broad surface

`Theme` currently includes many docking-internal style knobs; `code` uses only `background` and `accentColor`.

Evidence:
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/theme/Theme.java:13`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeMapper.java:28`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeMapper.java:31`

Impact:
- API stability burden increases for fields that `code` does not need.

Recommendation:
- Either accept this larger stable contract, or introduce a narrower API-level theme view for content components.

### 6. LOW - Aggregator module ordering must be updated explicitly

Root currently has only `docks` and `code` modules.

Evidence:
- `pom.xml:13`

Recommendation:
- add `papiflyfx-docking-api` to root modules and place it before `docks` and `code`.

## Minimal Adjusted Plan

1. Decide package strategy first (recommended: dedicated `...docking.api...` namespace).
2. Add `papiflyfx-docking-api` module to root `pom.xml` and wire dependencies (`docks` -> `api`, `code` -> `api`).
3. Move/extract the five shared types and update imports.
4. Update ServiceLoader descriptor path if interface package changes.
5. Keep/relocate `code` integration tests so test compilation still has docking runtime types.
6. Run:
- `mvn -pl papiflyfx-docking-api,papiflyfx-docking-docks,papiflyfx-docking-code -am compile`
- `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true test`
- `mvn -pl papiflyfx-docking-docks -am -Dtestfx.headless=true test`
