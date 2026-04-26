# PapiflyFX Docks Specs

This directory contains docking-framework design notes, implementation plans, and delivery records. Older dated directories are preserved for traceability; use the status links below before treating an older plan as current architecture.

## Current Ribbon Entry Points

- [Ribbon current status](ribbon-status.md) - canonical status for Ribbon 1 through Ribbon 6 candidates, current implementation baseline, authoritative session shape, and carry-forward roadmap.
- [Ribbon provider authoring](ribbon-provider-authoring.md) - provider ids, command ids, ServiceLoader registration, typed context metadata, explicit capability contribution, contextual tabs, QAT semantics, focused tests, and Maven selector flags.
- [Ribbon release and migration notes](ribbon-release-notes.md) - Ribbon 2 breaking-change context, current Ribbon 5 session contract, and Ribbon 6 design-only migration notes.

## Historical Docking Specs

The early docking documents in this directory describe the original docking layout concept and some constraints that have since changed. For example, the framework now uses CSS in shared UI/ribbon styling even though the first spec framed the project as "no CSS." Treat those documents as historical unless they are linked by a current status page or module README.

## Ribbon Iterations

| Iteration | Status | Notes |
| --- | --- | --- |
| [Ribbon 1](2026-04-19-0-ribbon/README.md) | Historical baseline | Introduced command/ribbon SPI, runtime shell, adaptive sizing, QAT, contextual tabs, and an early top-level `ribbon` session example that is now superseded. |
| [Ribbon 2](2026-04-20-0-ribbon-2/README.md) | Implemented breaking hardening | Established UI-neutral command/context contracts, command registry, id-first QAT, SVG icon support, and `extensions.ribbon` session persistence. |
| [Ribbon 3](2026-04-23-0-ribbon-3/README.md) | Implemented targeted fix | Closed SamplesApp ribbon label/caption clipping with focused geometry validation. |
| [Ribbon 4](2026-04-23-0-ribbon-4/README.md) | Implemented samples | Added GitHub and Hugo ribbon SamplesApp demos and headless coverage. |
| [Ribbon 5](2026-04-23-0-ribbon-5/README.md) | Closed consolidated follow-up | Stabilized runtime/accessibility/provider/test docs and closed the review loop. |
| [Ribbon 6](2026-04-23-0-ribbon-6/README.md) | Design-only | Records future breaking API/session candidates. No Ribbon 6 behavior is implemented by Ribbon 5. |

## Session Documentation Rule

Current ribbon session state is namespaced under `extensions.ribbon`, with:

- `extensions.ribbon.minimized`
- `extensions.ribbon.selectedTabId`
- `extensions.ribbon.quickAccessCommandIds`

Any historical document showing a top-level `ribbon` object describes the Ribbon 1 persistence shape and is not authoritative for current code.

## Historical Original Docks Concept

The following section is the original docks concept text preserved for traceability. It is historical and partially superseded by the current implementation and the ribbon status links above.

### Core Component Hierarchy

The framework will not use standard layout containers directly. Instead, it will use a wrapper pattern where every docking element is a class that manages its own internal JavaFX nodes.

Base interface: `DockElement`

- `getNode()`: returns the actual JavaFX `Region`.
- `getMetadata()`: returns a `DockData` object: ID, title, icon, and state.
- `serialize()`: returns a DTO for layout persistence.

Concrete structural nodes:

1. `DockLeaf`: terminal node containing user content wrapped in a dock pane.
2. `DockSplitGroup`: split container managing two `DockElement` instances and a divider ratio.
3. `DockTabGroup`: tab stack for `DockLeaf` instances.

### Pure Code Layout Engine

The original concept used recursive composition and a `LayoutFactory#build(LayoutNode root)` method to instantiate split and leaf nodes from the data model.

The original theming concept described a `Theme` record passed through constructors:

```java
public record Theme(
    Paint background,
    Paint accentColor,
    CornerRadii cornerRadius,
    Font headerFont
) {}
```

Current implementation details have evolved, including shared CSS token usage in UI/ribbon surfaces.

### Interaction And Drag-And-Drop

The original design framed `DockManager` as the central coordinator for scene/root state and drag-and-drop. Drag initiation starts from tab mouse interaction, an overlay layer renders drop hints, and hit testing maps pointer position to the hovered dock leaf.

### State Management And Serialization

The original persistence concept described capture as a traversal of live split and tab groups into JSON, with restore rebuilding the node tree from layout DTOs. Current session persistence also includes content-state adapters and namespaced extension payloads such as `extensions.ribbon`.

### Original Tradeoffs

| Challenge | Programmatic solution |
| --- | --- |
| Complexity | Fluent APIs for common leaf/content construction. |
| Theming | Theme properties and node style updates. |
| Memory | Explicit disposal for leaves and bound resources. |
| Deep nesting | Tree flattening for redundant split groups. |
