# Persistence Current State - Progress

## Summary
- Removed layout-only persistence API from DockManager and deleted LayoutPersistence.
- Refactored persistence tests and DemoApp to use session save/restore.
- Added DockSession serializer/persistence unit coverage and updated docs to session terminology.

## Notes
- Layout serialization stays as an internal helper for session JSON.
- DemoApp now saves sessions under `target/sessions`.
