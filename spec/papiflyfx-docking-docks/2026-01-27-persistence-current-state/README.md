# Persistence - Current State

## Update

- Layout-only persistence APIs were removed (save/restore layout-only methods and LayoutPersistence).
- Session persistence is now the only save/restore API and format.

## Scope and APIs

- Layout-only persistence: removed (previously `DockManager.saveToString`, `saveToFile`, `restoreFromString`,
  `loadFromFile` using `capture()` with `LayoutPersistence` + `LayoutSerializer`).
- Session persistence lives in `DockManager.saveSessionToString`, `saveSessionToFile`,
  `restoreSessionFromString`, `loadSessionFromFile`.
  - Uses `captureSession()` to serialize layout plus floating/minimized/maximized state.
  - Serializes via `DockSessionPersistence` + `DockSessionSerializer` (which delegates layout
    node serialization to `LayoutSerializer`).

## Findings (Behavior Differences)

- State captured:
  - Layout-only captures `rootElement` only (no floating/minimized/maximized).
  - Session captures layout + floating leaves (bounds + restore hints), minimized leaves (restore hints),
    and maximized leaf data.
- Restore semantics:
  - `restore()` only replaces `rootElement` and leaves floating/minimized state intact.
  - `restoreSession()` clears floating windows, restore hints, minimized store, and unmaximizes
    before applying the session layout.
- JSON schema:
  - Layout-only JSON is a raw `LayoutNode` map (`leaf`/`split`/`tabGroup`).
  - Session JSON wraps that layout under `layout` and adds `type: dockSession`, `version`, plus
    `floating`, `minimized`, `maximized` sections. The formats are not interchangeable.
- Empty-state behavior:
  - `saveToString()` returns an empty string when there is no root layout.
  - `saveSessionToString()` always emits JSON (because `captureSession()` never returns null),
    even when `layout` is null.
- File I/O behavior:
  - `DockSessionPersistence.toJsonFile()` creates parent directories.
  - `LayoutPersistence.toJsonFile()` does not.
- Maximized restoration gap:
  - `captureSession()` includes maximized data, but `restoreSession()` explicitly skips restoring it
    (known limitation in `DockManager`).
- Exception types differ (`LayoutFileIOException` vs `SessionFileIOException`), even though the
  operational semantics are similar.

## Can They Be Merged Without Losing Functionality?

Short answer: not as a direct replacement. The two APIs encode different scopes and restore
semantics. Replacing layout persistence with session persistence would lose:

- The ability to restore a layout without clearing existing floating/minimized state.
- The empty-string sentinel for "no layout".
- The simpler layout-only JSON schema (if anything consumes it).

A no-loss merge is still possible if the merged API preserves both scopes via options.

## Suggestions (No-Loss Merge Path)

- Introduce a single persistence API with an explicit scope:
  - Example: `save(PersistenceMode mode)` / `restore(String json, RestoreMode mode)`.
  - `PersistenceMode` could include `LAYOUT_ONLY` and `FULL_SESSION`.
  - `RestoreMode` could include `REPLACE_ALL` (clear floating/minimized/maximized) and
    `MERGE_LAYOUT_ONLY` (keep external state).
- Make session JSON the canonical format, and allow "layout-only" saves by emitting a session
  with only the `layout` field populated.
- Decide and document empty-layout semantics:
  - If a session is restored with `layout: null` under `REPLACE_ALL`, call `setRoot(null)` to
    match the layout-only "empty" outcome.
- Align file I/O behavior between persistence classes (either both create parent dirs or neither).
- If the API surface remains split, consider renaming to emphasize scope (`saveLayout` vs `saveSession`)
  to avoid conceptual ambiguity.
