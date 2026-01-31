# papiflyfx-docks

This specification outlines a **Pure Programmatic UI** architecture for a JavaFX docking framework. By eschewing 
FXML and CSS, we gain total control over the scene graph, reduce layout overhead, and ensure strict type safety.

---

## 1. Core Component Hierarchy

The framework will not use standard layout containers directly. Instead, it will use a **Wrapper Pattern** where every docking element is a class that manages its own internal JavaFX nodes.

### Base Interface: `DockElement`

Every entity in the docking system must implement this.

* **`getNode()`**: Returns the actual JavaFX `Region`.
* **`getMetadata()`**: Returns a `DockData` object (ID, Title, Icon, State).
* **`serialize()`**: Returns a DTO for layout persistence.

### Concrete Structural Nodes

1. **`DockLeaf`**: The terminal node. It contains the user’s content (a `Node`) and is wrapped in a `DockPane` (the visual container with a title bar).
2. **`DockSplitGroup`**: Replaces the standard `SplitPane`. It manages two `DockElements` and a divider ratio.
3. **`DockTabGroup`**: Manages a stack of `DockLeaf` objects, providing a custom tab header.

---

## 2. The "Pure Code" Layout Engine

To avoid CSS and FXML, we use a **Recursive Composition** strategy.

### The `LayoutFactory`

Since we aren't using FXML loaders, we use a static factory to assemble the UI.

* **Method:** `build(LayoutNode root)`
* **Logic:** This method recursively traverses the data model. If it finds a `SplitData` object, it instantiates a `DockSplitGroup`; if it finds `LeafData`, it instantiates a `DockLeaf`.

### Manual Styling (The `Theme` POJO)

Without CSS, styling is handled by a `Theme` record passed through the constructor chain (Dependency Injection).

```java
public record Theme(
    Paint background,
    Paint accentColor,
    CornerRadii cornerRadius,
    Font headerFont
) {}

```

Each component listens to a `ThemeProperty` in the `DockManager` and updates its `Background` and `Border` objects programmatically when the theme changes.

---

## 3. Interaction & Drag-and-Drop Specification

This is the most complex logic in a programmatic implementation.

### The `DockManager` (The Brain)

The `DockManager` maintains a reference to the `Scene` and the `RootNode`. It acts as the global event bus.

### Drag-and-Drop Protocol

1. **Initiation:** A mouse-press on a `DockTab` triggers the `DragManager`.
2. **The Transparent Overlay:** A `StackPane` is used as the root of the `Scene`. The `DockingLayer` is the bottom child; the `OverlayLayer` (a transparent `Canvas`) is the top child.
3. **Hit-Testing:** As the mouse moves, the `DragManager` calculates the coordinates relative to the `DockingLayer` and determines which `DockLeaf` is being hovered over.
4. **The Drop Hint:**
  * The `Canvas` draws a semi-transparent rectangle over the target area.
  * **Logic:** Divide the target leaf into a 3x3 grid. The "North, South, East, West" sectors trigger a split; the "Center" sector triggers a tab-add.

---

## 4. State Management & Serialization

Because the UI is built programmatically, saving the state is a direct mapping of the object tree.

* **Snapshots:** The `DockManager` can trigger a `capture()` call that traverses the live `DockSplitGroup` and `DockTabGroup` objects.
* **Output:** A nested JSON structure representing the hierarchy, divider positions ( to ), and active tab indices.
* **Restoration:** On startup, the JSON is read into a `LayoutDefinition` POJO, which the `LayoutFactory` uses to reconstruct the entire `Node` tree.

---

## 5. Tradeoff Implementation Matrix

| Challenge | Programmatic Solution |
| --- | --- |
| **Complexity** | Use a **Fluent API** (e.g., `DockLeaf.withTitle("Editor").content(node)`) to reduce boilerplate. |
| **Theming** | Use JavaFX `ObjectProperty<Theme>` and bind node styles to it. |
| **Memory** | Manually call `dispose()` on `DockLeaf` when closed to unbind listeners and free the content node. |
| **Deep Nesting** | Implement a "Tree Flattener" that removes redundant `DockSplitGroups` (e.g., a splitter containing only one child). |

---

## 6. Next Steps

To begin the implementation, we should focus on the **Recursive Split Logic**.

**Would you like me to provide a Java implementation for the `DockSplitGroup` that handles the dynamic replacement of a Leaf with a SplitPane when a drop occurs?**