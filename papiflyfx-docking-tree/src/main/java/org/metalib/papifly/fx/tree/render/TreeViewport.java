package org.metalib.papifly.fx.tree.render;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import org.metalib.papifly.fx.tree.api.TreeCellRenderer;
import org.metalib.papifly.fx.tree.api.TreeItem;
import org.metalib.papifly.fx.tree.model.FlattenedRow;
import org.metalib.papifly.fx.tree.model.FlattenedTree;
import org.metalib.papifly.fx.tree.model.TreeSelectionModel;
import org.metalib.papifly.fx.tree.theme.TreeViewTheme;
import org.metalib.papifly.fx.tree.util.GlyphCache;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class TreeViewport<T> extends Region {

    public static final double SCROLLBAR_WIDTH = 12.0;
    public static final double SCROLLBAR_THUMB_PAD = 2.0;
    public static final double MIN_THUMB_SIZE = 24.0;
    public static final double SCROLLBAR_RADIUS = 8.0;

    private final Canvas canvas = new Canvas();
    private final GlyphCache glyphCache = new GlyphCache();
    private final List<TreeRenderPass<T>> renderPasses = List.of(
        new TreeBackgroundPass<>(),
        new TreeConnectingLinesPass<>(),
        new TreeContentPass<>(),
        new TreeScrollbarPass<>()
    );
    private final FlattenedTree.FlattenedTreeListener flattenedTreeListener = this::markDirty;

    private FlattenedTree<T> flattenedTree;
    private TreeSelectionModel<T> selectionModel;
    private TreeViewTheme theme = TreeViewTheme.dark();
    private Function<T, Image> iconResolver = value -> null;
    private TreeCellRenderer<T> cellRenderer;

    private boolean dirty = true;
    private boolean fullRedrawRequired = true;
    private double scrollOffset;
    private double horizontalScrollOffset;
    private double effectiveTextWidth;
    private double effectiveTextHeight;
    private double contentWidth;
    private TreeItem<T> hoveredItem;
    private List<TreeRenderRow<T>> visibleRows = List.of();

    private boolean verticalScrollbarVisible;
    private boolean horizontalScrollbarVisible;
    private ScrollbarGeometry verticalScrollbarGeometry;
    private ScrollbarGeometry horizontalScrollbarGeometry;
    private ScrollbarPart scrollbarHoverPart = ScrollbarPart.NONE;
    private ScrollbarPart scrollbarActivePart = ScrollbarPart.NONE;
    private Runnable scrollbarVisibilityListener;

    public TreeViewport(FlattenedTree<T> flattenedTree, TreeSelectionModel<T> selectionModel) {
        this.flattenedTree = flattenedTree == null ? new FlattenedTree<>() : flattenedTree;
        this.flattenedTree.addListener(flattenedTreeListener);
        this.selectionModel = selectionModel == null ? new TreeSelectionModel<>() : selectionModel;
        glyphCache.setFont(theme.font());
        getChildren().add(canvas);
    }

    public FlattenedTree<T> getFlattenedTree() {
        return flattenedTree;
    }

    public void setFlattenedTree(FlattenedTree<T> flattenedTree) {
        FlattenedTree<T> resolved = flattenedTree == null ? new FlattenedTree<>() : flattenedTree;
        if (this.flattenedTree == resolved) {
            return;
        }
        if (this.flattenedTree != null) {
            this.flattenedTree.removeListener(flattenedTreeListener);
        }
        this.flattenedTree = resolved;
        this.flattenedTree.addListener(flattenedTreeListener);
        markDirty();
    }

    public TreeSelectionModel<T> getSelectionModel() {
        return selectionModel;
    }

    public void setSelectionModel(TreeSelectionModel<T> selectionModel) {
        this.selectionModel = selectionModel == null ? new TreeSelectionModel<>() : selectionModel;
        markDirty();
    }

    public TreeViewTheme getTheme() {
        return theme;
    }

    public void setTheme(TreeViewTheme theme) {
        this.theme = theme == null ? TreeViewTheme.dark() : theme;
        glyphCache.setFont(this.theme.font());
        markDirty();
    }

    public GlyphCache getGlyphCache() {
        return glyphCache;
    }

    public Function<T, Image> getIconResolver() {
        return iconResolver;
    }

    public void setIconResolver(Function<T, Image> iconResolver) {
        this.iconResolver = iconResolver == null ? value -> null : iconResolver;
        markDirty();
    }

    public TreeCellRenderer<T> getCellRenderer() {
        return cellRenderer;
    }

    public void setCellRenderer(TreeCellRenderer<T> cellRenderer) {
        this.cellRenderer = cellRenderer;
        markDirty();
    }

    public TreeItem<T> getHoveredItem() {
        return hoveredItem;
    }

    public void setHoveredItem(TreeItem<T> hoveredItem) {
        if (this.hoveredItem == hoveredItem) {
            return;
        }
        this.hoveredItem = hoveredItem;
        markDirty();
    }

    public void markDirty() {
        dirty = true;
        fullRedrawRequired = true;
        requestLayout();
    }

    public boolean isDirty() {
        return dirty;
    }

    public double getScrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(double offset) {
        double max = computeMaxScrollOffset(effectiveTextHeight, rowCount() * rowHeight());
        double clamped = clamp(offset, 0.0, max);
        if (Double.compare(scrollOffset, clamped) == 0) {
            return;
        }
        scrollOffset = clamped;
        markDirty();
    }

    public double getHorizontalScrollOffset() {
        return horizontalScrollOffset;
    }

    public void setHorizontalScrollOffset(double offset) {
        double max = computeMaxHorizontalScrollOffset(effectiveTextWidth, contentWidth);
        double clamped = clamp(offset, 0.0, max);
        if (Double.compare(horizontalScrollOffset, clamped) == 0) {
            return;
        }
        horizontalScrollOffset = clamped;
        markDirty();
    }

    public boolean isVerticalScrollbarVisible() {
        return verticalScrollbarVisible;
    }

    public boolean isHorizontalScrollbarVisible() {
        return horizontalScrollbarVisible;
    }

    public ScrollbarGeometry getVerticalScrollbarGeometry() {
        return verticalScrollbarGeometry;
    }

    public ScrollbarGeometry getHorizontalScrollbarGeometry() {
        return horizontalScrollbarGeometry;
    }

    public ScrollbarPart getScrollbarHoverPart() {
        return scrollbarHoverPart;
    }

    public void setScrollbarHoverPart(ScrollbarPart scrollbarHoverPart) {
        ScrollbarPart next = scrollbarHoverPart == null ? ScrollbarPart.NONE : scrollbarHoverPart;
        if (this.scrollbarHoverPart == next) {
            return;
        }
        this.scrollbarHoverPart = next;
        markDirty();
    }

    public ScrollbarPart getScrollbarActivePart() {
        return scrollbarActivePart;
    }

    public void setScrollbarActivePart(ScrollbarPart scrollbarActivePart) {
        ScrollbarPart next = scrollbarActivePart == null ? ScrollbarPart.NONE : scrollbarActivePart;
        if (this.scrollbarActivePart == next) {
            return;
        }
        this.scrollbarActivePart = next;
        markDirty();
    }

    public void setOnScrollbarVisibilityChanged(Runnable listener) {
        this.scrollbarVisibilityListener = listener;
    }

    public int rowCount() {
        return flattenedTree == null ? 0 : flattenedTree.size();
    }

    public double getEffectiveTextWidth() {
        return effectiveTextWidth;
    }

    public double getEffectiveTextHeight() {
        return effectiveTextHeight;
    }

    public double getContentWidth() {
        return contentWidth;
    }

    public double rowHeight() {
        return theme.rowHeight();
    }

    public int rowIndexAtY(double localY) {
        if (rowCount() <= 0) {
            return -1;
        }
        int row = (int) Math.floor((localY + scrollOffset) / rowHeight());
        return clamp(row, 0, rowCount() - 1);
    }

    public HitInfo<T> hitTest(double localX, double localY) {
        int rowIndex = rowIndexAtY(localY);
        if (rowIndex < 0) {
            return null;
        }
        FlattenedRow<T> flattenedRow = flattenedTree.getRow(rowIndex);
        TreeItem<T> item = flattenedRow.item();
        int depth = flattenedRow.depth();
        double y = rowIndex * rowHeight() - scrollOffset;
        double disclosureStart = depth * theme.indentWidth() - horizontalScrollOffset;
        double disclosureEnd = disclosureStart + theme.indentWidth();
        boolean disclosureHit = !item.isLeaf() && localX >= disclosureStart && localX <= disclosureEnd;
        return new HitInfo<>(
            item,
            rowIndex,
            depth,
            0.0,
            y,
            effectiveTextWidth,
            rowHeight(),
            disclosureHit
        );
    }

    public Bounds rowBounds(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rowCount()) {
            return null;
        }
        double y = rowIndex * rowHeight() - scrollOffset;
        return new BoundingBox(0.0, y, effectiveTextWidth, rowHeight());
    }

    public void ensureItemVisible(TreeItem<T> item) {
        int index = flattenedTree.indexOf(item);
        if (index < 0) {
            return;
        }
        double rowTop = index * rowHeight();
        double rowBottom = rowTop + rowHeight();
        if (rowTop < scrollOffset) {
            setScrollOffset(rowTop);
        } else if (rowBottom > scrollOffset + effectiveTextHeight) {
            setScrollOffset(rowBottom - effectiveTextHeight);
        }
    }

    public double verticalOffsetForThumbTop(double thumbTop) {
        if (!verticalScrollbarVisible || verticalScrollbarGeometry == null) {
            return scrollOffset;
        }
        double maxScroll = computeMaxScrollOffset(effectiveTextHeight, rowCount() * rowHeight());
        double trackStart = verticalScrollbarGeometry.trackY() + SCROLLBAR_THUMB_PAD;
        double travel = Math.max(0.0,
            verticalScrollbarGeometry.trackHeight() - (2 * SCROLLBAR_THUMB_PAD) - verticalScrollbarGeometry.thumbHeight());
        if (maxScroll <= 0 || travel <= 0) {
            return 0.0;
        }
        double clampedTop = clamp(thumbTop, trackStart, trackStart + travel);
        double ratio = (clampedTop - trackStart) / travel;
        return ratio * maxScroll;
    }

    public double horizontalOffsetForThumbLeft(double thumbLeft) {
        if (!horizontalScrollbarVisible || horizontalScrollbarGeometry == null) {
            return horizontalScrollOffset;
        }
        double maxScroll = computeMaxHorizontalScrollOffset(effectiveTextWidth, contentWidth);
        double trackStart = horizontalScrollbarGeometry.trackX() + SCROLLBAR_THUMB_PAD;
        double travel = Math.max(0.0,
            horizontalScrollbarGeometry.trackWidth() - (2 * SCROLLBAR_THUMB_PAD) - horizontalScrollbarGeometry.thumbWidth());
        if (maxScroll <= 0 || travel <= 0) {
            return 0.0;
        }
        double clampedLeft = clamp(thumbLeft, trackStart, trackStart + travel);
        double ratio = (clampedLeft - trackStart) / travel;
        return ratio * maxScroll;
    }

    public double verticalOffsetForTrackClick(double y) {
        if (!verticalScrollbarVisible || verticalScrollbarGeometry == null) {
            return scrollOffset;
        }
        return verticalOffsetForThumbTop(y - (verticalScrollbarGeometry.thumbHeight() * 0.5));
    }

    public double horizontalOffsetForTrackClick(double x) {
        if (!horizontalScrollbarVisible || horizontalScrollbarGeometry == null) {
            return horizontalScrollOffset;
        }
        return horizontalOffsetForThumbLeft(x - (horizontalScrollbarGeometry.thumbWidth() * 0.5));
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        if (Double.compare(width, canvas.getWidth()) != 0 || Double.compare(height, canvas.getHeight()) != 0) {
            canvas.setWidth(width);
            canvas.setHeight(height);
            dirty = true;
            fullRedrawRequired = true;
        }
        if (!dirty) {
            return;
        }
        dirty = false;
        redraw();
    }

    private void redraw() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        resolveViewportMetrics(width, height);
        buildVisibleRows();
        TreeRenderContext<T> context = new TreeRenderContext<>(
            graphics,
            theme,
            glyphCache,
            visibleRows,
            selectionModel,
            hoveredItem,
            width,
            height,
            effectiveTextWidth,
            effectiveTextHeight,
            rowHeight(),
            theme.indentWidth(),
            theme.iconSize(),
            glyphCache.getBaselineOffset(),
            scrollOffset,
            horizontalScrollOffset,
            verticalScrollbarVisible,
            horizontalScrollbarVisible,
            verticalScrollbarGeometry,
            horizontalScrollbarGeometry,
            scrollbarHoverPart,
            scrollbarActivePart,
            iconResolver,
            cellRenderer
        );
        if (fullRedrawRequired) {
            for (TreeRenderPass<T> pass : renderPasses) {
                pass.renderFull(context);
            }
        } else {
            for (TreeRenderRow<T> row : visibleRows) {
                for (TreeRenderPass<T> pass : renderPasses) {
                    pass.renderRow(context, row);
                }
            }
        }
        fullRedrawRequired = false;
    }

    private void resolveViewportMetrics(double viewportWidth, double viewportHeight) {
        boolean previousVerticalVisible = verticalScrollbarVisible;
        boolean previousHorizontalVisible = horizontalScrollbarVisible;
        double rowHeight = rowHeight();
        double fullContentHeight = rowCount() * rowHeight;
        contentWidth = computeContentWidth();

        boolean nextVerticalVisible = previousVerticalVisible;
        boolean nextHorizontalVisible = previousHorizontalVisible;

        for (int i = 0; i < 4; i++) {
            double candidateWidth = Math.max(0.0, viewportWidth - (nextVerticalVisible ? SCROLLBAR_WIDTH : 0.0));
            double candidateHeight = Math.max(0.0, viewportHeight - (nextHorizontalVisible ? SCROLLBAR_WIDTH : 0.0));
            boolean computedVerticalVisible = fullContentHeight > candidateHeight;
            boolean computedHorizontalVisible = contentWidth > candidateWidth;
            if (computedVerticalVisible == nextVerticalVisible && computedHorizontalVisible == nextHorizontalVisible) {
                break;
            }
            nextVerticalVisible = computedVerticalVisible;
            nextHorizontalVisible = computedHorizontalVisible;
        }

        verticalScrollbarVisible = nextVerticalVisible;
        horizontalScrollbarVisible = nextHorizontalVisible;
        effectiveTextWidth = Math.max(0.0, viewportWidth - (verticalScrollbarVisible ? SCROLLBAR_WIDTH : 0.0));
        effectiveTextHeight = Math.max(0.0, viewportHeight - (horizontalScrollbarVisible ? SCROLLBAR_WIDTH : 0.0));

        double maxVertical = computeMaxScrollOffset(effectiveTextHeight, fullContentHeight);
        double maxHorizontal = computeMaxHorizontalScrollOffset(effectiveTextWidth, contentWidth);
        scrollOffset = clamp(scrollOffset, 0.0, maxVertical);
        horizontalScrollOffset = clamp(horizontalScrollOffset, 0.0, maxHorizontal);

        verticalScrollbarGeometry = verticalScrollbarVisible
            ? buildVerticalScrollbarGeometry(fullContentHeight, maxVertical)
            : null;
        horizontalScrollbarGeometry = horizontalScrollbarVisible
            ? buildHorizontalScrollbarGeometry(contentWidth, maxHorizontal)
            : null;

        normalizeScrollbarInteractionState();
        if ((previousVerticalVisible != verticalScrollbarVisible || previousHorizontalVisible != horizontalScrollbarVisible)
            && scrollbarVisibilityListener != null) {
            scrollbarVisibilityListener.run();
        }
    }

    private void buildVisibleRows() {
        if (rowCount() <= 0) {
            visibleRows = List.of();
            return;
        }
        int firstVisible = clamp((int) Math.floor(scrollOffset / rowHeight()), 0, rowCount() - 1);
        int lastVisible = clamp((int) Math.ceil((scrollOffset + effectiveTextHeight) / rowHeight()), firstVisible, rowCount() - 1);
        List<TreeRenderRow<T>> rows = new ArrayList<>(Math.max(1, lastVisible - firstVisible + 1));
        for (int index = firstVisible; index <= lastVisible; index++) {
            FlattenedRow<T> flattenedRow = flattenedTree.getRow(index);
            TreeItem<T> item = flattenedRow.item();
            rows.add(new TreeRenderRow<>(
                item,
                index,
                flattenedRow.depth(),
                index * rowHeight() - scrollOffset,
                rowHeight(),
                item.isLeaf(),
                flattenedTree.getExpansionModel().isExpanded(item)
            ));
        }
        visibleRows = rows;
    }

    private double computeContentWidth() {
        if (rowCount() <= 0) {
            return 0.0;
        }
        double max = 0.0;
        for (FlattenedRow<T> row : flattenedTree.rows()) {
            TreeItem<T> item = row.item();
            double textWidth = glyphCache.measureTextWidth(String.valueOf(item.getValue()));
            double width = (row.depth() * theme.indentWidth())
                + theme.indentWidth()
                + theme.iconSize()
                + 10.0
                + textWidth;
            max = Math.max(max, width);
        }
        return max;
    }

    private ScrollbarGeometry buildVerticalScrollbarGeometry(double fullContentHeight, double maxOffset) {
        double trackX = effectiveTextWidth;
        double trackY = 0.0;
        double trackWidth = SCROLLBAR_WIDTH;
        double trackHeight = effectiveTextHeight;
        double thumbWidth = Math.max(1.0, trackWidth - (SCROLLBAR_THUMB_PAD * 2));
        double usableTrack = Math.max(0.0, trackHeight - (SCROLLBAR_THUMB_PAD * 2));
        if (usableTrack <= 0.0) {
            return new ScrollbarGeometry(trackX, trackY, trackWidth, trackHeight, trackX, trackY, thumbWidth, 0.0);
        }
        double ratio = effectiveTextHeight / Math.max(fullContentHeight, effectiveTextHeight);
        double thumbHeight = Math.max(MIN_THUMB_SIZE, usableTrack * ratio);
        thumbHeight = Math.min(usableTrack, thumbHeight);
        double thumbTravel = Math.max(0.0, usableTrack - thumbHeight);
        double thumbY = trackY + SCROLLBAR_THUMB_PAD;
        if (maxOffset > 0.0 && thumbTravel > 0.0) {
            thumbY += (scrollOffset / maxOffset) * thumbTravel;
        }
        double thumbX = trackX + SCROLLBAR_THUMB_PAD;
        return new ScrollbarGeometry(trackX, trackY, trackWidth, trackHeight, thumbX, thumbY, thumbWidth, thumbHeight);
    }

    private ScrollbarGeometry buildHorizontalScrollbarGeometry(double fullContentWidth, double maxOffset) {
        double trackX = 0.0;
        double trackY = effectiveTextHeight;
        double trackWidth = effectiveTextWidth;
        double trackHeight = SCROLLBAR_WIDTH;
        double thumbHeight = Math.max(1.0, trackHeight - (SCROLLBAR_THUMB_PAD * 2));
        double usableTrack = Math.max(0.0, trackWidth - (SCROLLBAR_THUMB_PAD * 2));
        if (usableTrack <= 0.0) {
            return new ScrollbarGeometry(trackX, trackY, trackWidth, trackHeight, trackX, trackY, 0.0, thumbHeight);
        }
        double ratio = effectiveTextWidth / Math.max(fullContentWidth, effectiveTextWidth);
        double thumbWidth = Math.max(MIN_THUMB_SIZE, usableTrack * ratio);
        thumbWidth = Math.min(usableTrack, thumbWidth);
        double thumbTravel = Math.max(0.0, usableTrack - thumbWidth);
        double thumbX = trackX + SCROLLBAR_THUMB_PAD;
        if (maxOffset > 0.0 && thumbTravel > 0.0) {
            thumbX += (horizontalScrollOffset / maxOffset) * thumbTravel;
        }
        double thumbY = trackY + SCROLLBAR_THUMB_PAD;
        return new ScrollbarGeometry(trackX, trackY, trackWidth, trackHeight, thumbX, thumbY, thumbWidth, thumbHeight);
    }

    private void normalizeScrollbarInteractionState() {
        if (!verticalScrollbarVisible) {
            if (scrollbarHoverPart == ScrollbarPart.VERTICAL_THUMB) {
                scrollbarHoverPart = ScrollbarPart.NONE;
            }
            if (scrollbarActivePart == ScrollbarPart.VERTICAL_THUMB) {
                scrollbarActivePart = ScrollbarPart.NONE;
            }
        }
        if (!horizontalScrollbarVisible) {
            if (scrollbarHoverPart == ScrollbarPart.HORIZONTAL_THUMB) {
                scrollbarHoverPart = ScrollbarPart.NONE;
            }
            if (scrollbarActivePart == ScrollbarPart.HORIZONTAL_THUMB) {
                scrollbarActivePart = ScrollbarPart.NONE;
            }
        }
    }

    private static double computeMaxScrollOffset(double viewportHeight, double contentHeight) {
        return Math.max(0.0, contentHeight - viewportHeight);
    }

    private static double computeMaxHorizontalScrollOffset(double viewportWidth, double contentWidth) {
        return Math.max(0.0, contentWidth - viewportWidth);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    public record HitInfo<T>(
        TreeItem<T> item,
        int rowIndex,
        int depth,
        double x,
        double y,
        double width,
        double height,
        boolean disclosureHit
    ) {
    }

    public record ScrollbarGeometry(
        double trackX,
        double trackY,
        double trackWidth,
        double trackHeight,
        double thumbX,
        double thumbY,
        double thumbWidth,
        double thumbHeight
    ) {
        public boolean containsTrack(double x, double y) {
            return x >= trackX && x <= trackX + trackWidth && y >= trackY && y <= trackY + trackHeight;
        }

        public boolean containsThumb(double x, double y) {
            return x >= thumbX && x <= thumbX + thumbWidth && y >= thumbY && y <= thumbY + thumbHeight;
        }
    }

    public enum ScrollbarPart {
        NONE,
        VERTICAL_THUMB,
        HORIZONTAL_THUMB
    }
}
