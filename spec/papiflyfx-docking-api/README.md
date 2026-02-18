# Modularity and Decoupling Analysis

## Current State

The project consists of two main modules:
1. `papiflyfx-docking-docks`: Core docking framework and layout management.
2. `papiflyfx-docking-code`: A canvas-based code editor component that can be hosted within the docking framework.

### Coupling Analysis

- **Maven Dependency:** `papiflyfx-docking-code` depends directly on `papiflyfx-docking-docks`.
- **Interface Implementation:** `code` module implements several interfaces defined in `docks`:
    - `org.metalib.papifly.fx.docks.layout.ContentFactory`
    - `org.metalib.papifly.fx.docks.layout.ContentStateAdapter`
    - `org.metalib.papifly.fx.docks.layout.DisposableContent`
- **Data Model Coupling:** `code` module uses DTOs/records defined in `docks`:
    - `org.metalib.papifly.fx.docks.layout.data.LeafContentData`
    - `org.metalib.papifly.fx.docks.theme.Theme`
- **Directionality:** The coupling is strictly unidirectional (`code` -> `docks`). The `docks` module has no knowledge of the `code` module.

### Identified Issues

1. **Circular dependency risk:** While not currently circular, adding any feature to `docks` that might need to know about "code" would create a cycle.
2. **Heavyweight dependency:** To use the `CodeEditor` in a different context (without the full docking framework), a user would still have to pull in all of `papiflyfx-docking-docks`.
3. **API Stability:** Internal changes to `docks` (e.g., refactoring layout logic) might break `code` if they affect the shared interfaces.

## Proposed Solution: `papiflyfx-docking-api`

Introduce a lightweight API module that contains only the interfaces and DTOs required for content integration and styling.

## Validated Extraction Scope

The shared types identified for extraction are correct:
- `ContentFactory`
- `ContentStateAdapter`
- `DisposableContent`
- `LeafContentData`
- `Theme`

These are the only `docks` types referenced by `papiflyfx-docking-code` main sources.

## Required Design Decisions (Embedded Recommendations)

1. **Use a dedicated API package namespace.**
    - Recommended: `org.metalib.papifly.fx.docking.api.*`.
    - Do **not** keep `org.metalib.papifly.fx.docks.layout*` package names for moved classes; that would create split packages across `api` and `docks`.

2. **Separate main-scope and test-scope dependency goals.**
    - Main sources in `papiflyfx-docking-code` should depend only on `papiflyfx-docking-api`.
    - Tests in `papiflyfx-docking-code` currently include docking integration tests; keep `papiflyfx-docking-docks` as a `test`-scope dependency (or relocate those tests to `docks`).

3. **Migrate ServiceLoader contract explicitly.**
    - Update `META-INF/services/<ContentStateAdapter FQCN>` file name when `ContentStateAdapter` package changes.
    - Keep provider entry (`CodeEditorStateAdapter`) in sync with new interface package.

4. **Clean Javadoc/API boundary references during move.**
    - `DisposableContent` Javadoc should not link to `DockLeaf` from `docks`; use neutral API wording.

5. **Decide `Theme` API surface intentionally.**
    - Option A (simple): move full `Theme` to `api` and treat it as stable public API.
    - Option B (lean): define a smaller API-level theme contract for content components and map from docking internals.
    - Recommendation for v1 migration speed: Option A, with explicit compatibility commitment.

### Abstraction Layers (Target)

1. **`papiflyfx-docking-api`**:
    - `ContentFactory`
    - `ContentStateAdapter`
    - `DisposableContent`
    - `LeafContentData`
    - `Theme`

2. **`papiflyfx-docking-docks`**:
    - Depends on `api`.
    - Implements core docking logic using `api` interfaces.
    - Provides `DockManager`, `TabGroup`, `Split`, `Leaf`.

3. **`papiflyfx-docking-code`**:
    - Main sources depend on `api`.
    - May still depend on `docks` in test scope for docking integration tests.
    - Implements `api` interfaces to provide code editor content.

## Implementation Plan (Adjusted)

1. **Create `papiflyfx-docking-api` module**:
    - Create directory and `pom.xml`.
    - Add to root `pom.xml` module list **before** `docks` and `code`.
2. **Move Shared Files**:
    - Move identified interfaces/records to `api`.
    - Use `org.metalib.papifly.fx.docking.api.*` packages (avoid split packages).
    - Update Javadocs for boundary-neutral language.
3. **Update `docks`**:
    - Add dependency on `api`.
    - Fix imports.
    - Remove/rehome moved classes from `docks`.
4. **Update `code`**:
    - Replace main dependency on `docks` with dependency on `api`.
    - Keep `docks` as `test` scope if docking integration tests remain in `code`.
    - Fix imports.
5. **Update ServiceLoader descriptor**:
    - Rename/update `META-INF/services/...ContentStateAdapter` descriptor to match new interface FQCN.
6. **Validation**:
    - `mvn -pl papiflyfx-docking-api,papiflyfx-docking-docks,papiflyfx-docking-code -am compile`
    - `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true test`
    - `mvn -pl papiflyfx-docking-docks -am -Dtestfx.headless=true test`

## Acceptance Criteria

1. `papiflyfx-docking-code` main sources compile against `api` only.
2. `papiflyfx-docking-docks` compiles and runs with `api` imports.
3. ServiceLoader discovery of `CodeEditorStateAdapter` still works.
4. No split packages remain between `api` and `docks`.
5. Existing restore/theme integration tests pass without behavior regression.
