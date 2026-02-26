# Implementation Plan: Inline Collapsible Node Info (Option B)

## Progress snapshot

- [x] Research completed in `spec/papiflyfx-docking-tree/review2-node-info/research.md`
- [x] Detailed implementation plan prepared in this document
- [ ] Phase 1 implemented: API + node-info state model
- [ ] Phase 2 implemented: mixed flattened rows (item + info rows)
- [ ] Phase 3 implemented: variable-height viewport metrics + hit-testing
- [ ] Phase 4 implemented: inline JavaFX node host virtualization
- [ ] Phase 5 implemented: input/edit/drag-drop integration
- [ ] Phase 6 implemented: state persistence + adapter version bump
- [ ] Phase 7 implemented: tests and regression validation

## Objective

Implement full inline collapsible node-info sections directly inside the tree viewport, where each expanded node can render rich JavaFX content (rich text/HTML host, table, card/form) below the node row while keeping virtualization, scrolling, selection, keyboard navigation, editing, drag/drop, search, and state persistence stable.

## Final behavior requirements

1. Any non-null node can expose optional info content via provider API.
2. Info content is shown inline as a collapsible row directly after the node's item row.
3. Content is rendered as JavaFX `Node` (not canvas text simulation) and supports:
   - rich text/HTML via provider-supplied node (e.g., `WebView` from consumer module),
   - table content (`TableView`),
   - card/form content (`VBox`, `GridPane`, controls).
4. Tree remains virtualized:
   - only visible inline info nodes are mounted in scene graph,
   - off-screen info nodes are unmounted or pooled.
5. Keyboard/mouse behavior remains predictable:
   - item navigation remains item-focused,
   - info rows are non-selectable tree rows,
   - dedicated toggle gesture exists (`Alt+Enter` + pointer toggle zone).
6. Save/restore includes expanded info state.

## Non-negotiable design constraints

- Breaking API/state compatibility is allowed where required by the inline architecture.
- Do not break fixed behaviors already covered by tests (search reveal, selection/focus flow, edit flow).
- No direct hard dependency in tree core on a specific rich-content control; use provider contract returning `Node`.
- Preserve rendering performance by keeping item row paint on canvas and hosting rich content in a separate overlay layer.

---

## Phase 1 — API and node-info state model

### Files to add

- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/api/TreeNodeInfoProvider.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/model/TreeNodeInfoMode.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/model/TreeNodeInfoModel.java`

### Files to update

- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/api/TreeView.java`

### Tasks

- [ ] Add provider API for per-item info content and preferred height.
- [ ] Add node-info expansion model with `SINGLE` and `MULTIPLE` modes.
- [ ] Wire model/provider into `TreeView`.
- [ ] Expose new public methods for toggling/collapsing info.

### Snippet: provider contract

```java
package org.metalib.papifly.fx.tree.api;

import javafx.scene.Node;

public interface TreeNodeInfoProvider<T> {
    Node createContent(TreeItem<T> item);

    default double preferredHeight(TreeItem<T> item, double availableWidth) {
        return 160.0;
    }

    default void disposeContent(TreeItem<T> item, Node content) {
        // no-op by default
    }
}
```

### Snippet: info expansion model

```java
package org.metalib.papifly.fx.tree.model;

import org.metalib.papifly.fx.tree.api.TreeItem;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TreeNodeInfoModel<T> {
    public interface Listener<T> {
        void onInfoExpandedChanged(TreeItem<T> item, boolean expanded);
    }

    private final Set<TreeItem<T>> expandedItems = new LinkedHashSet<>();
    private final CopyOnWriteArrayList<Listener<T>> listeners = new CopyOnWriteArrayList<>();
    private TreeNodeInfoMode mode = TreeNodeInfoMode.SINGLE;

    public boolean isExpanded(TreeItem<T> item) { return expandedItems.contains(item); }
    public Set<TreeItem<T>> getExpandedItems() { return Set.copyOf(expandedItems); }
    public TreeNodeInfoMode getMode() { return mode; }

    public void setMode(TreeNodeInfoMode mode) { /* collapse extras when SINGLE */ }
    public void toggle(TreeItem<T> item) { /* expand/collapse + notify */ }
    public void setExpanded(TreeItem<T> item, boolean expanded) { /* mode-aware */ }
    public void clear() { /* collapse all + notify */ }
}
```

### Snippet: `TreeView` API surface additions

```java
private final TreeNodeInfoModel<T> nodeInfoModel = new TreeNodeInfoModel<>();
private final ObjectProperty<TreeNodeInfoProvider<T>> nodeInfoProvider =
    new SimpleObjectProperty<>(this, "nodeInfoProvider");

public TreeNodeInfoModel<T> getNodeInfoModel() { return nodeInfoModel; }
public TreeNodeInfoProvider<T> getNodeInfoProvider() { return nodeInfoProvider.get(); }
public void setNodeInfoProvider(TreeNodeInfoProvider<T> provider) { nodeInfoProvider.set(provider); }
public ObjectProperty<TreeNodeInfoProvider<T>> nodeInfoProviderProperty() { return nodeInfoProvider; }

public void toggleNodeInfo(TreeItem<T> item) { nodeInfoModel.toggle(item); }
public void collapseNodeInfo(TreeItem<T> item) { nodeInfoModel.setExpanded(item, false); }
public void collapseAllNodeInfo() { nodeInfoModel.clear(); }
public void setNodeInfoMode(TreeNodeInfoMode mode) { nodeInfoModel.setMode(mode); }
```

---

## Phase 2 — Mixed flattened rows (item + info rows)

### Files to update

- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/model/FlattenedRow.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/model/FlattenedTree.java`

### Tasks

- [ ] Replace current item-only flattened row representation with row kinds.
- [ ] Insert info rows directly after expanded item rows.
- [ ] Keep helper APIs for item-only navigation and selection logic.
- [ ] Maintain fast lookup maps for item row index and info row index.

### Snippet: row-kind aware flattened row

```java
public record FlattenedRow<T>(
    RowKind rowKind,
    TreeItem<T> item,       // for ITEM row: item itself; for INFO row: owner item
    int depth
) {
    public enum RowKind { ITEM, INFO }

    public boolean isItemRow() { return rowKind == RowKind.ITEM; }
    public boolean isInfoRow() { return rowKind == RowKind.INFO; }
}
```

### Snippet: flatten algorithm with inline info rows

```java
private void flatten(TreeItem<T> item, int depth) {
    int itemRowIndex = rows.size();
    rows.add(new FlattenedRow<>(FlattenedRow.RowKind.ITEM, item, depth));
    itemRowIndexByItem.put(item, itemRowIndex);

    if (nodeInfoProvider != null && nodeInfoModel.isExpanded(item)) {
        int infoRowIndex = rows.size();
        rows.add(new FlattenedRow<>(FlattenedRow.RowKind.INFO, item, depth));
        infoRowIndexByItem.put(item, infoRowIndex);
    }

    if (item.isLeaf() || !expansionModel.isExpanded(item)) {
        return;
    }
    for (TreeItem<T> child : item.getChildren()) {
        flatten(child, depth + 1);
    }
}
```

### Required `FlattenedTree` helper additions

- `boolean isItemRow(int rowIndex)`
- `boolean isInfoRow(int rowIndex)`
- `TreeItem<T> getOwnerItem(int rowIndex)`
- `int itemRowIndexOf(TreeItem<T> item)`
- `int infoRowIndexOf(TreeItem<T> item)`
- `List<TreeItem<T>> visibleItems()` must return only item rows

This keeps existing controller assumptions explicit and avoids accidental selection/navigation into info rows.

---

## Phase 3 — Variable-height viewport metrics, hit-testing, and row geometry

### Files to update

- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/render/TreeViewport.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/render/TreeRenderRow.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/render/TreeRenderContext.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/render/TreeBackgroundPass.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/render/TreeContentPass.java`

### Tasks

- [ ] Introduce row metrics arrays (`rowTop[]`, `rowHeight[]`, `contentHeight`).
- [ ] Replace all `index * rowHeight()` assumptions with metrics lookup.
- [ ] Use binary search for `y -> rowIndex` conversion.
- [ ] Build visible rows via offset window intersection.
- [ ] Extend `HitInfo` with row kind and info-toggle hit metadata.

### Snippet: row metrics core

```java
private double[] rowTops = new double[0];
private double[] rowHeights = new double[0];
private double fullContentHeight;

private void rebuildRowMetrics() {
    int count = rowCount();
    rowTops = new double[count];
    rowHeights = new double[count];
    double y = 0.0;
    for (int i = 0; i < count; i++) {
        FlattenedRow<T> row = flattenedTree.getRow(i);
        rowTops[i] = y;
        double height = row.isInfoRow()
            ? infoRowHeight(row.item(), effectiveTextWidth)
            : rowHeight();
        rowHeights[i] = Math.max(1.0, height);
        y += rowHeights[i];
    }
    fullContentHeight = y;
}
```

### Snippet: binary-search row lookup

```java
public int rowIndexAtY(double localY) {
    if (rowCount() == 0) return -1;
    double absoluteY = localY + scrollOffset;
    int lo = 0;
    int hi = rowCount() - 1;
    while (lo <= hi) {
        int mid = (lo + hi) >>> 1;
        double top = rowTops[mid];
        double bottom = top + rowHeights[mid];
        if (absoluteY < top) hi = mid - 1;
        else if (absoluteY >= bottom) lo = mid + 1;
        else return mid;
    }
    return clamp(lo, 0, rowCount() - 1);
}
```

### Snippet: richer hit info

```java
public record HitInfo<T>(
    TreeItem<T> item,
    int rowIndex,
    FlattenedRow.RowKind rowKind,
    int depth,
    double x,
    double y,
    double width,
    double height,
    boolean disclosureHit,
    boolean infoToggleHit
) {}
```

### Rendering behavior updates

- Item rows continue to be painted by canvas passes.
- Info rows get lightweight row background paint in `TreeBackgroundPass`.
- `TreeContentPass` skips icon/text/disclosure drawing on info rows.
- `TreeRenderRow` must carry `rowKind`.

---

## Phase 4 — Inline JavaFX node host virtualization

### Files to add

- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/render/TreeInlineInfoHost.java`

### Files to update

- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/api/TreeView.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/render/TreeViewport.java`

### Tasks

- [ ] Add dedicated overlay pane for inline info nodes.
- [ ] Mount nodes only for visible info rows.
- [ ] Keep provider-created nodes cached by owner item for reuse.
- [ ] Dispose cached nodes on provider change, root replacement, and `TreeView.dispose()`.

### Snippet: host class shape

```java
final class TreeInlineInfoHost<T> {
    private final Pane layer = new Pane();
    private final Map<TreeItem<T>, Node> cache = new IdentityHashMap<>();
    private final Set<TreeItem<T>> mountedItems = new HashSet<>();

    Pane layer() { return layer; }

    void sync(List<TreeRenderRow<T>> visibleRows, TreeNodeInfoProvider<T> provider, double width) {
        mountedItems.clear();
        for (TreeRenderRow<T> row : visibleRows) {
            if (!row.isInfoRow()) continue;
            TreeItem<T> item = row.item();
            Node content = cache.computeIfAbsent(item, provider::createContent);
            if (content == null) continue;
            if (content.getParent() != layer) layer.getChildren().add(content);
            content.resizeRelocate(0.0, row.y(), width, row.height());
            mountedItems.add(item);
        }
        layer.getChildren().removeIf(node -> !mountedItems.contains(ownerOf(node)));
    }

    void clear(TreeNodeInfoProvider<T> provider) { /* dispose all cached nodes */ }
}
```

### Snippet: TreeView stacking order

```java
getChildren().addAll(
    viewport,
    inlineInfoHost.layer(),
    dragDropController.overlayCanvas(),
    editController.editorNode(),
    searchOverlay
);
```

### Notes

- `inlineInfoHost.layer()` must be clipped to viewport text area.
- Mouse transparency should be `false` because info controls are interactive.
- Overlay must be synchronized in layout pass after viewport computes visible rows.

---

## Phase 5 — Input, editing, and drag-drop integration

### Files to update

- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/controller/TreePointerController.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/controller/TreeInputController.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/controller/TreeEditController.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/controller/TreeDragDropController.java`

### Tasks

- [ ] Add pointer toggle zone for info collapse/expand.
- [ ] Add keyboard shortcut to toggle focused item info (`Alt+Enter`).
- [ ] Keep navigation/select/edit on item rows only.
- [ ] Ensure drag/drop targets resolve to nearest item row, never raw info row.

### Snippet: pointer integration

```java
if (hitInfo.infoToggleHit() && hitInfo.rowKind() == FlattenedRow.RowKind.ITEM) {
    nodeInfoModel.toggle(hitInfo.item());
    viewport.ensureItemVisible(hitInfo.item());
    viewport.markDirty();
    event.consume();
    return true;
}

if (hitInfo.rowKind() == FlattenedRow.RowKind.INFO) {
    selectionModel.selectOnly(hitInfo.item()); // owner item
    selectionModel.setFocusedItem(hitInfo.item());
    viewport.markDirty();
    event.consume();
    return true;
}
```

### Snippet: keyboard shortcut

```java
case ENTER -> {
    if (event.isAltDown()) {
        yield consume(event, toggleFocusedInfo());
    }
    yield consume(event, toggleFocusedExpansion());
}
```

### Snippet: edit guard

```java
public void startEdit(TreeViewport.HitInfo<T> hitInfo) {
    if (hitInfo == null || hitInfo.item() == null || hitInfo.rowKind() != FlattenedRow.RowKind.ITEM) {
        return;
    }
    // existing edit startup
}
```

### Drag/drop rule

`resolveDropHint(x, y)` must normalize any info-row hit to its owner item row bounds before computing BEFORE/INSIDE/AFTER regions.

---

## Phase 6 — State persistence and adapter versioning

### Files to update

- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/util/TreeViewStateData.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/util/TreeStateCodec.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/api/TreeView.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/api/TreeViewStateAdapter.java`

### Tasks

- [ ] Persist `expandedInfoPaths` next to existing path/state fields.
- [ ] Restore info expansion after root is available.
- [ ] Bump `TreeViewStateAdapter.VERSION` to `2`.
- [ ] Require `expandedInfoPaths` in v2 payloads (no backward decode requirement).

### Snippet: state data extension

```java
public record TreeViewStateData(
    List<List<Integer>> expandedPaths,
    List<List<Integer>> expandedInfoPaths,
    List<List<Integer>> selectedPaths,
    List<Integer> focusedPath,
    double scrollOffset,
    double horizontalScrollOffset
) {
    public static TreeViewStateData empty() {
        return new TreeViewStateData(List.of(), List.of(), List.of(), List.of(), 0.0, 0.0);
    }
}
```

### Snippet: codec v2

```java
map.put("expandedInfoPaths", encodePaths(safeData.expandedInfoPaths()));

Object rawExpandedInfoPaths = map.get("expandedInfoPaths");
if (!(rawExpandedInfoPaths instanceof List<?>)) {
    throw new IllegalArgumentException("Missing expandedInfoPaths");
}
List<List<Integer>> expandedInfoPaths = decodePaths(rawExpandedInfoPaths);
return new TreeViewStateData(
    expandedPaths,
    expandedInfoPaths,
    selectedPaths,
    focusedPath,
    scrollOffset,
    horizontalScrollOffset
);
```

### Snippet: TreeView capture/apply

```java
List<List<Integer>> expandedInfoPaths = nodeInfoModel.getExpandedItems().stream()
    .map(this::pathOf)
    .filter(path -> !path.isEmpty() || getRoot() != null)
    .toList();

for (List<Integer> path : safeState.expandedInfoPaths()) {
    TreeItem<T> item = resolvePath(path);
    if (item != null) {
        nodeInfoModel.setExpanded(item, true);
    }
}
```

---

## Phase 7 — Search behavior and policy

### Files to update

- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/api/TreeView.java`

### Tasks

- [ ] Keep default search behavior: search reveals/selects/focuses matching item and expands parent tree nodes.
- [ ] Do not auto-expand node info by default during search reveal.
- [ ] Ensure searching a node with already-expanded info keeps inline info stable.

### Snippet: explicit reveal policy

```java
private void revealMatch(TreeItem<T> match) {
    if (match == null) return;
    TreeItem<T> parent = match.getParent();
    while (parent != null) {
        expansionModel.setExpanded(parent, true);
        parent = parent.getParent();
    }
    selectionModel.selectOnly(match);
    selectionModel.setFocusedItem(match);
    viewport.ensureItemVisible(match);
    // No implicit nodeInfoModel.setExpanded(match, true)
    viewport.markDirty();
}
```

---

## Phase 8 — Test plan and validation gates

### Unit tests to add/update

- `model/TreeNodeInfoModelTest` (new):
  - single mode collapses previous item,
  - multiple mode keeps multiple expanded,
  - clear/toggle semantics.
- `model/FlattenedTreeTest` (update):
  - info row insertion order (`ITEM` then `INFO`),
  - `visibleItems()` excludes info rows,
  - row index lookups for item/info rows.
- `render/TreeViewportTest` (new):
  - binary search row hit with variable heights,
  - `ensureItemVisible` with mixed heights,
  - visible window row slicing with info rows.
- `util/TreeStateCodecTest` (new):
  - round-trip with `expandedInfoPaths`,
  - rejects payloads that omit `expandedInfoPaths`.

### FX tests to add/update

- `api/TreeViewFxTest` (update):
  - pointer toggles info row open/close,
  - `Alt+Enter` toggles focused item info,
  - item selection remains stable while interacting with info content,
  - drag/drop ignores raw info row as structural target,
  - state save/restore keeps expanded info rows.

### Validation commands (run repeatedly during implementation)

```bash
./mvnw -pl papiflyfx-docking-tree -am test -Dtestfx.headless=true
./mvnw -pl papiflyfx-docking-tree -am -DskipTests compile
```

### Exit criteria

- All new and existing tree-module tests pass.
- Inline info rows work with rich JavaFX content and remain virtualized.
- No regressions in selection, navigation, editing, drag/drop, search, and state restore.

---

## Work breakdown checklist (execution order)

1. [ ] Introduce node-info API/model in `TreeView` and supporting classes.
2. [ ] Refactor flattened row model to row kinds and integrate info rows.
3. [ ] Refactor viewport geometry to variable-height row metrics.
4. [ ] Add `TreeInlineInfoHost` and connect host lifecycle to `TreeView`.
5. [ ] Integrate pointer/keyboard/edit/drag-drop with row-kind-aware hit info.
6. [ ] Extend persistence (`expandedInfoPaths`) and bump adapter version.
7. [ ] Add/update unit + FX tests; fix regressions.
8. [ ] Final pass: run module compile + tests and verify behavior manually in samples.

## Immediate next steps

1. Start Phase 1 by adding `TreeNodeInfoProvider`, `TreeNodeInfoMode`, and `TreeNodeInfoModel`.
2. Wire `TreeView` to hold node-info model/provider and trigger viewport invalidation on info expansion changes.
3. Proceed to Phase 2 row model refactor before touching viewport math.
