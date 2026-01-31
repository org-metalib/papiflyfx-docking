# Plan: Add Save/Restore State API for DockManager Root

## Overview
Save and restore the entire `dockManager.getRoot()` layout to JSON files, enabling users to persist and reload their dock configuration. Leverage existing `capture()/restore()` methods and `LayoutSerializer` to serialize to JSON.

## Steps

### 1. Examine Existing Serialization Infrastructure
- Review `LayoutSerializer.java` to understand Map-based serialization flow
- Verify that `DockManager.capture()` traverses live objects and generates `LayoutNode` DTOs
- Confirm `DockManager.restore(LayoutNode layout)` can rebuild layouts from serialized data
- Check `LayoutNode`, `LeafData`, `SplitData`, `TabGroupData` classes for completeness

### 2. Create LayoutPersistence Utility Class
- Create new file: `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/serial/LayoutPersistence.java`
- Wrap Jackson ObjectMapper for JSON I/O around `LayoutSerializer`
- Implement methods:
  - `toJsonString(LayoutNode layout)` — serialize to JSON string
  - `fromJsonString(String json)` — deserialize from JSON string
  - `toJsonFile(LayoutNode layout, Path path)` — write to file
  - `fromJsonFile(Path path)` — read from file
- Handle JSON serialization/deserialization errors gracefully

### 3. Extend DockManager API
Add public convenience methods to `DockManager.java`:
- `String saveToString()` — captures current layout and returns JSON string
- `void restoreFromString(String json)` — parses JSON and restores layout
- `void saveToFile(Path path)` — captures layout and writes to JSON file
- `void loadFromFile(Path path)` — reads JSON file and restores layout

Optional fluent variants:
- `saveToFile(File file)` — overload for `java.io.File`
- `loadFromFile(File file)` — overload for `java.io.File`

### 4. Add Content Factory Restoration Support
- Create callback mechanism to allow custom `ContentFactory` implementation to supply content nodes during restore
- Add method to `DockManager`: `void setContentFactory(ContentFactory factory)`
- `ContentFactory` interface should have method: `Node createContent(String contentFactoryId, String leafId)`
- Update `LayoutFactory` to use `ContentFactory` during `LayoutNode` → `DockElement` conversion

### 5. Documentation Updates
- Update `README.md` with save/restore usage examples:
  - Basic save/load workflow
  - Using custom `ContentFactory` for content restoration
  - Error handling patterns
- Add inline JavaDoc to new public API methods

### 6. Integration Tests
Create `papiflyfx-docks/src/test/java/org/metalib/papifly/fx/docks/serial/LayoutPersistenceFxTest.java`:
- Test round-trip serialization: save → load → structure equality
- Test JSON string export/import
- Test file I/O (temp files)
- Test with complex layouts (splits, tab groups, etc.)
- Test error handling (invalid JSON, missing files, corrupted data)

Create `papiflyfx-docks/src/test/java/org/metalib/papifly/fx/docks/DockManagerPersistenceFxTest.java`:
- Test `DockManager.saveToString()` / `loadFromString()`
- Test `DockManager.saveToFile()` / `loadFromFile()`
- Test with `DemoApp` initial layout
- Verify layout properties preserved (divider positions, active tabs, etc.)

## Considerations

### JSON Library Choice
- **Recommendation**: Use Jackson (likely already available via transitive dependencies)
- Check `pom.xml` for existing Jackson dependency
- If not present, add: `com.fasterxml.jackson.core:jackson-databind`

### Error Handling Strategy
- Use unchecked exceptions (`RuntimeException` subclasses) for API fluency
- Specific exception types:
  - `LayoutSerializationException` — JSON serialization/deserialization errors
  - `LayoutFileIOException` — file not found, I/O errors
  - `InvalidLayoutException` — corrupted layout data

### Content Factory Restoration
- Two approaches:
  1. **Simple**: Store only leaf IDs and titles; require `ContentFactory` callback to provide content nodes
  2. **Advanced**: Store serialized content nodes (out of scope for v1)
- Recommend approach #1 for v1

### Backwards Compatibility
- Ensure existing `capture()` and `restore()` methods unchanged
- New persistence methods are additive, no breaking changes

### File Format Decisions
- **JSON pretty-print**: Enable for readability (small performance cost)
- **Version field**: Consider adding layout version to JSON for future migrations
- **File extension**: Recommend `.dock-layout.json` for clarity

## Success Criteria
- [x] Existing `capture()` / `restore()` tests still pass
- [x] New serialization tests validate round-trip consistency
- [x] File I/O tests verify save/load workflow
- [x] Example in README demonstrates save/restore usage
- [x] No breaking changes to existing DockManager API
- [x] Error handling covers common failure scenarios

