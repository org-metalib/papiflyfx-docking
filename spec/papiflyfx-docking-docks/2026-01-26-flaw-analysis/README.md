# PapiflyFX Docks Flaw Analysis (2026-01-26)

## Scope
- Module: papiflyfx-docks
- Review: docking tree (core), drag/drop, floating/minimize/maximize, layout serialization, theme wiring, layout persistence

## Findings
- High: Split collapse reparenting can keep a child attached to the old split while inserting it into the parent, which risks `IllegalArgumentException` (node already has a parent) and leaves split/theme listeners undisposed during close/minimize or drag cleanup. Evidence: `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:335` `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:1071` `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/drag/DragManager.java:335`.
- High: Session restore builds floating/minimized leaves without wiring close handlers, so restored tabs cannot be closed and their lifecycle callbacks never fire. Evidence: `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:576` `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:304`.
- Medium: Maximized state is captured in session data but never restored, so session persistence silently drops maximized layout state. Evidence: `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:555` `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:645`.
- Medium: Restoring a session with floating leaves throws if `setOwnerStage` was not called, which aborts restores mid-flight. Evidence: `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:596` `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:713`.
- Low: Drag self-drop guard is stubbed, so the "drop on self" check never triggers and invalid self-drops may execute. Evidence: `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/drag/DragContext.java:90` `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/drag/DragManager.java:153`.
- Low: Drag overlay colors are hardcoded and ignore theme drop hint settings, causing theming inconsistencies. Evidence: `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/render/OverlayCanvas.java:17` `papiflyfx-docks/src/main/java/org/metalib/papifly/fx/docks/theme/Theme.java:13`.

## Actions
- Add a safe "collapse split" helper that detaches remaining children before reparenting and disposes removed splits/tab groups; use it in `DockManager` and `DragManager` cleanup, plus add regression coverage for nested split close/minimize/drag.
- Wire `setupLeafCloseHandler` (and any other required handlers) for leaves created during `restoreSession`, including floating/minimized restores, so post-restore leaf actions function.
- Decide on maximized persistence behavior: implement restore (find leaf by ID and call `maximizeLeaf` after layout restoration) or stop capturing maximized state to avoid false expectations.
- Make session restore resilient when owner stage is unset by deferring floating restoration or docking those leaves, and document the required call order.
- Bind overlay hint colors to theme properties (or push `Theme` into `OverlayCanvas`) so drag hints update with theme changes.
- Implement `isDropOnSelf` or remove its usage and codify the intended self-drop behavior with drag tests.
- Document test flags for TestFX runs, including `-Dtestfx.headless=true` to enable Monocle and related properties.
