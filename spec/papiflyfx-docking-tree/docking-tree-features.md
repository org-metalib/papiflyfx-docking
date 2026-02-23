# Docking Tree

## Feature List

Building a `TreeView` from scratch in JavaFX requires a clear separation between the **Data Model** (the hierarchy), the **Virtualization Logic** (to handle large datasets), and the **Visual Representation** (the cells).

Below is a comprehensive specification for a custom `TreeView` component, categorized into foundational and professional-grade features.

---

### 1. Basic Feature List (The MVP)

These features are essential for a functional, user-friendly tree.

#### Core Architecture

* **Hierarchical Data Model:** A recursive `TreeItem<T>` structure that holds value, parent reference, and a list of children.
* **Virtualization:** Do not render nodes that aren't visible. The component should manage a "flattened" list of currently visible items (expanded parents + their children).
* **Expand/Collapse:** Basic toggle functionality for branch nodes via a "disclosure node" (usually an arrow/chevron).
* **Cell Factory:** Support for custom `TreeCell` implementations to allow developers to define how each node looks (e.g., adding icons or checkboxes).

#### Selection & Focus

* **Single Selection:** Highlight a single row and track the selected item.
* **Keyboard Navigation:** * `Up/Down`: Move focus/selection between visible rows.
* `Left/Right`: Collapse/Expand the focused node.


* **Focus Tracking:** Separate "focused" state from "selected" state.

#### Layout

* **Indentation:** Configurable padding based on the depth/level of the node.
* **Automatic Scrollbars:** A `ScrollPane` wrapper or internal scroll logic that updates based on the total height of expanded items.

---

### 2. Advanced Feature List (Enterprise Grade)

To compete with professional libraries, your component should handle complex UX scenarios.

#### Performance & Scaling

* **Lazy Loading (On-Demand):** Ability to load child nodes only when the parent is expanded (crucial for file systems or large databases).
* **Fixed Cell Size:** Performance optimization for massive trees where every row has a predictable height, allowing  scroll calculations.

#### Enhanced Interaction

* **Multi-Selection:** Support for `Shift+Click` (range) and `Ctrl+Click` (individual) selections.
* **Drag and Drop (DnD):** * Internal reordering (moving nodes within the tree).
* External DnD (dragging files into the tree or nodes out to other components).
* **In-place Editing:** Double-click or press `F2` to rename a node via an integrated `TextField`.
* **Context Menus:** Support for right-click actions specific to the node type (e.g., "Delete," "New Folder").

#### Visual & UX Polish

* **Multi-Column Support:** Transforming the `TreeView` into a `TreeTableView` where the tree hierarchy is just the first column.
* **Search & Filtering:** Real-time filtering that hides nodes not matching a query while maintaining the visibility of their parent chain.
* **Checkboxes with Tri-state Logic:** If a parent is checked, all children are checked. If some children are checked, the parent shows an "indeterminate" state.
* **Connecting Lines:** Optional CSS-styled lines that visually connect parent nodes to their children (common in IDEs).

---

### 3. Technical Implementation Specification

#### The "Flattening" Formula

To manage virtualization, you must convert the tree into a flat list. The number of visible rows  can be calculated as:



*Note: This is recursive. A child is only counted if all its ancestors are expanded.*

#### Recommended State Management

| Property | Type | Description |
| --- | --- | --- |
| `root` | `ObjectProperty<TreeItem<T>>` | The top-level node. |
| `selectionModel` | `SelectionModel<TreeItem<T>>` | Handles single/multiple selection logic. |
| `expandedItemCount` | `ReadOnlyIntegerProperty` | Total number of visible rows (for scrollbar scaling). |
| `cellFactory` | `Callback<TreeView<T>, TreeCell<T>>` | Generates the UI for each row. |

#### Component Lifecycle

1. **Model Change:** A `TreeItem` is expanded.
2. **Flattening:** The `TreeView` recalculates the visible list.
3. **Virtualization:** The `Skin` class determines which indices are within the current viewport.
4. **Cell Update:** `updateItem(T item, boolean empty)` is called on the visible `TreeCell` instances to recycle them.

