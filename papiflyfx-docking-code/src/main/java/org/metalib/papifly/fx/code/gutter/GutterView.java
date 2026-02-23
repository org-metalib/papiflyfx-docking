package org.metalib.papifly.fx.code.gutter;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.render.GlyphCache;
import org.metalib.papifly.fx.code.render.WrapMap;
import org.metalib.papifly.fx.code.theme.CodeEditorTheme;

/**
 * Canvas-based gutter rendering line numbers and a marker lane.
 * <p>
 * Draws alongside the {@link org.metalib.papifly.fx.code.render.Viewport}
 * and synchronizes scroll position with it.
 */
public class GutterView extends Region {

    private static final double MARKER_LANE_WIDTH = 12;
    private static final double LINE_NUMBER_RIGHT_PADDING = 8;

    private final Canvas canvas;
    private final GlyphCache glyphCache;

    private CodeEditorTheme theme = CodeEditorTheme.dark();
    private Document document;
    private MarkerModel markerModel;
    private double scrollOffset;
    private int activeLineIndex = -1;
    private boolean dirty = true;
    private double computedWidth;
    private boolean wordWrap;
    private WrapMap wrapMap;

    /**
     * Creates a gutter view backed by the provided glyph cache.
     *
     * @param glyphCache glyph metrics cache shared with viewport rendering
     */
    public GutterView(GlyphCache glyphCache) {
        this.glyphCache = glyphCache;
        this.canvas = new Canvas();
        getChildren().add(canvas);
    }

    /**
     * Sets the document to display line numbers for.
     *
     * @param document document model to render in the gutter
     */
    public void setDocument(Document document) {
        this.document = document;
        recomputeWidth();
        markDirty();
    }

    /**
     * Sets the editor theme palette and triggers a redraw.
     *
     * @param theme theme palette to apply
     */
    public void setTheme(CodeEditorTheme theme) {
        this.theme = theme == null ? CodeEditorTheme.dark() : theme;
        markDirty();
    }

    /**
     * Returns the current editor theme.
     *
     * @return active gutter theme
     */
    public CodeEditorTheme getTheme() {
        return theme;
    }

    /**
     * Sets the marker model for the marker lane.
     *
     * @param markerModel marker model used to render line markers
     */
    public void setMarkerModel(MarkerModel markerModel) {
        this.markerModel = markerModel;
        markDirty();
    }

    /**
     * Returns the current marker model.
     *
     * @return marker model used by this gutter, may be {@code null}
     */
    public MarkerModel getMarkerModel() {
        return markerModel;
    }

    /**
     * Synchronizes scroll offset with the viewport.
     *
     * @param offset vertical scroll offset in pixels
     */
    public void setScrollOffset(double offset) {
        this.scrollOffset = offset;
        markDirty();
    }

    /**
     * Enables/disables wrap-aware gutter layout.
     *
     * @param wordWrap {@code true} to render wrap-aware line numbers
     */
    public void setWordWrap(boolean wordWrap) {
        if (this.wordWrap == wordWrap) {
            return;
        }
        this.wordWrap = wordWrap;
        markDirty();
    }

    /**
     * Sets wrap metadata shared by the viewport in wrap mode.
     *
     * @param wrapMap wrap metadata for visual row mapping
     */
    public void setWrapMap(WrapMap wrapMap) {
        this.wrapMap = wrapMap;
        markDirty();
    }

    /**
     * Sets the active (caret) line index for highlighting.
     *
     * @param lineIndex zero-based active line index
     */
    public void setActiveLineIndex(int lineIndex) {
        if (this.activeLineIndex != lineIndex) {
            this.activeLineIndex = lineIndex;
            markDirty();
        }
    }

    /**
     * Returns the computed preferred width of the gutter.
     *
     * @return computed gutter width in pixels
     */
    public double getComputedWidth() {
        return computedWidth;
    }

    /**
     * Recomputes the gutter width based on total line count.
     */
    public void recomputeWidth() {
        if (document == null) {
            computedWidth = 0;
            return;
        }
        int lineCount = document.getLineCount();
        int digits = Math.max(2, String.valueOf(lineCount).length());
        double charWidth = glyphCache.getCharWidth();
        computedWidth = MARKER_LANE_WIDTH + (digits * charWidth) + LINE_NUMBER_RIGHT_PADDING;
        setPrefWidth(computedWidth);
        setMinWidth(computedWidth);
        setMaxWidth(computedWidth);
    }

    /**
     * Marks gutter render cache dirty and requests layout.
     */
    public void markDirty() {
        dirty = true;
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        if (w != canvas.getWidth() || h != canvas.getHeight()) {
            canvas.setWidth(w);
            canvas.setHeight(h);
            dirty = true;
        }
        if (dirty) {
            dirty = false;
            redraw();
        }
    }

    private void redraw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0 || document == null) {
            return;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double lineHeight = glyphCache.getLineHeight();
        double baseline = glyphCache.getBaselineOffset();
        // Clear
        gc.setFill(theme.gutterBackground());
        gc.fillRect(0, 0, w, h);

        gc.setFont(glyphCache.getFont());

        if (wordWrap && wrapMap != null && wrapMap.hasData() && wrapMap.totalVisualRows() > 0) {
            int totalRows = wrapMap.totalVisualRows();
            int firstRow = Math.max(0, (int) (scrollOffset / lineHeight));
            int lastRow = Math.min(totalRows - 1, (int) ((scrollOffset + h) / lineHeight) + 1);
            for (int row = firstRow; row <= lastRow; row++) {
                WrapMap.VisualRow visualRow = wrapMap.visualRow(row);
                if (visualRow.startColumn() != 0) {
                    continue;
                }
                int line = visualRow.lineIndex();
                double y = row * lineHeight - scrollOffset;
                paintGutterLine(gc, w, baseline, lineHeight, line, y);
            }
            return;
        }

        int totalLines = document.getLineCount();
        int firstLine = Math.max(0, (int) (scrollOffset / lineHeight));
        int lastLine = Math.min(totalLines - 1, (int) ((scrollOffset + h) / lineHeight) + 1);
        for (int line = firstLine; line <= lastLine; line++) {
            double y = line * lineHeight - scrollOffset;
            paintGutterLine(gc, w, baseline, lineHeight, line, y);
        }
    }

    private void paintGutterLine(GraphicsContext gc, double width, double baseline, double lineHeight, int line, double y) {
        if (markerModel != null && markerModel.hasMarkers(line)) {
            MarkerType type = markerModel.getHighestPriorityType(line);
            if (type != null) {
                gc.setFill(markerColor(type));
                double markerSize = Math.min(lineHeight - 4, MARKER_LANE_WIDTH - 4);
                double markerX = (MARKER_LANE_WIDTH - markerSize) / 2;
                double markerY = y + (lineHeight - markerSize) / 2;
                gc.fillOval(markerX, markerY, markerSize, markerSize);
            }
        }

        String lineNum = String.valueOf(line + 1);
        double textX = width - LINE_NUMBER_RIGHT_PADDING - (lineNum.length() * glyphCache.getCharWidth());
        gc.setFill(line == activeLineIndex ? theme.lineNumberActiveColor() : theme.lineNumberColor());
        gc.fillText(lineNum, textX, y + baseline);
    }

    private Paint markerColor(MarkerType type) {
        return switch (type) {
            case ERROR -> theme.markerErrorColor();
            case WARNING -> theme.markerWarningColor();
            case INFO -> theme.markerInfoColor();
            case BREAKPOINT -> theme.markerBreakpointColor();
            case BOOKMARK -> theme.markerBookmarkColor();
        };
    }
}
