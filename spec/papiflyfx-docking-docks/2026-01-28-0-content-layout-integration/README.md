#
Content Layout Integration

## Suggestions

When I serialize the application layout I would like to include information about dockleaf content.

What would be your suggestions? The content could represent a different types:
- a form
- a chart
- media

Save your suggestions in the sections below.

What if I don't know the content attributes I would like to save with the layout information. What would be an integration technique to achieve this?

### Integration technique when attributes are unknown
- Introduce a small extension point (adapter) for content to supply its own state.
- Persist only a stable identity in the layout: `typeKey`, `contentId`, and `version`.
- If a provider exists for the `typeKey`, let it save and restore a free-form `state` map.
- Use a registry (or Java `ServiceLoader`) so new content types can plug in without changing the layout format.
- When a provider is missing, create a placeholder dockleaf and keep the `state` for round-trip safety.

## Progress

- Added `LeafContentData` to persist content identity/state alongside `contentFactoryId`.
- Added `ContentStateAdapter` + `ContentStateRegistry` to allow pluggable content save/restore.
- Extended layout serialization to include a `content` block for leaves.
- Wired capture/restore in `DockManager`, `DockLeaf`, and `LayoutFactory` (with placeholder content fallback).
- Added a session serializer round-trip test covering content state.
- Tests not run.
