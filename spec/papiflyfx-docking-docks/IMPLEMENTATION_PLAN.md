# papiflyfx-docks Implementation Plan

This document outlines the step-by-step implementation plan for the `papiflyfx-docks` module,
a pure programmatic JavaFX docking framework.

## Overview

The implementation is structured into 7 phases, progressing from the core component hierarchy to advanced
interaction and serialization features.

---

## Phase 1: Core Hierarchy & Base Components
**Goal:** Establish the fundamental object model and visual wrappers.

| Component | Responsibility | Key Methods/Properties |
|-----------|----------------|------------------------|
| `DockElement` (Interface) | Base contract for all dockable entities. | `getNode()`, `getMetadata()`, `serialize()` |
| `DockData` (Record) | Metadata for docking elements. | `id`, `title`, `icon`, `state` |
| `DockLeaf` | Terminal node containing user content. | `content(Node)`, `withTitle(String)` |
| `DockPane` | Visual container for `DockLeaf` with title bar. | `titleBar`, `closeButton`, `contentArea` |
| `DockSplitGroup` | Manages two `DockElement`s with a divider. | `orientation`, `dividerPosition`, `replaceChild()` |
| `DockTabGroup` | Manages a stack of `DockLeaf` objects. | `tabs`, `activeTab`, `addLeaf()`, `removeLeaf()` |

---

## Phase 2: Theming & Manual Styling
**Goal:** Implement programmatic styling without CSS.

1. **Define `Theme` Record:**
   - Colors: `background`, `accentColor`, `headerBackground`, `textColor`.
   - Fonts: `headerFont`, `contentFont`.
   - Shapes: `cornerRadius`, `borderWidth`.

2. **Theming Infrastructure:**
   - Implement `ThemeProperty` in `DockManager`.
   - Create a base class or utility for components to bind their `Background` and `Border` to the theme.
   - Implement automatic updates when the theme changes.

---

## Phase 3: Layout Engine (Recursive Composition)
**Goal:** Build the UI tree from a data model.

1. **Data Model (DTOs):**
   - `LayoutNode` (Base DTO).
   - `SplitData` (DTO for `DockSplitGroup`).
   - `LeafData` (DTO for `DockLeaf`).
   - `TabData` (DTO for `DockTabGroup`).

2. **`LayoutFactory`:**
   - Static `build(LayoutNode root)` method.
   - Recursive traversal to instantiate concrete structural nodes.
   - Dependency injection of the `Theme`.

---

## Phase 4: Drag-and-Drop Infrastructure
**Goal:** Handle mouse interactions and spatial tracking.

1. **Layered Root:**
   - `DockManager` root using `StackPane`.
   - `DockingLayer`: Bottom child, contains the docking hierarchy.
   - `OverlayLayer`: Top child, a transparent `Canvas` for drop hints.

2. **`DragManager`:**
   - Global event filters for mouse press, drag, and release.
   - Capture initiation from `DockTab` or `DockPane` title bar.
   - Real-time coordinate translation relative to `DockingLayer`.

---

## Phase 5: Visual Feedback & Drop Logic
**Goal:** Provide drop hints and update the layout dynamically.

1. **Hit-Testing Engine:**
   - Recursive search to find the `DockLeaf` under the mouse.
   - 3x3 grid logic: Determine sector (North, South, East, West, Center).

2. **Drop Hint Rendering:**
   - `Canvas` drawing of semi-transparent blue rectangles.
   - Animation for smooth hint transitions.

3. **Dynamic Layout Update:**
   - Logic to split a `DockLeaf` into a `DockSplitGroup`.
   - Logic to merge a `DockLeaf` into a `DockTabGroup`.
   - "Tree Flattener" to maintain a clean hierarchy after moves.

---

## Phase 6: State Management & Serialization
**Goal:** Save and restore layout configurations.

1. **Snapshot System:**
   - `DockManager.capture()`: Traverses live objects and generates DTOs.
   - Capture divider positions (0.0 to 1.0).
   - Capture active tab indices.

2. **JSON Integration:**
   - Map DTOs to JSON using Jackson or similar.
   - File I/O for saving/loading `layout.json`.

3. **Restoration Flow:**
   - Load JSON -> `LayoutDefinition` -> `LayoutFactory.build()` -> `DockManager.setRoot()`.

---

## Phase 7: Refinement & Fluent API
**Goal:** Developer experience and optimization.

1. **Fluent API:**
   - DSL for manual layout assembly:
     ```java
     DockManager.create()
       .withTheme(Themes.DARK)
       .setRoot(
         Split.horizontal(
           Leaf.withTitle("Files").content(fileTree),
           TabGroup.of(
             Leaf.withTitle("Editor 1").content(editor1),
             Leaf.withTitle("Editor 2").content(editor2)
           )
         )
       );
     ```

2. **Memory Management:**
   - Implement `dispose()` on all components.
   - Explicitly unbind listeners and clear references to `Node` content.

3. **Edge Case Handling:**
   - Minimal size constraints for splitters.
   - Tab overflow handling (scrolling or menu).
   - Floating windows (optional/extended goal).

---

## Architecture Decisions

- **Composition over Inheritance:** Components wrap JavaFX nodes rather than extending them to hide internal Scene Graph complexity.
- **Pure Code:** No `.fxml` or `.css` files allowed in the module.
- **Reactive Theming:** Use `ObjectProperty<Theme>` for real-time visual updates.
- **Canvas Overlay:** Using a single Canvas for all DnD feedback reduces Node overhead during drags.
