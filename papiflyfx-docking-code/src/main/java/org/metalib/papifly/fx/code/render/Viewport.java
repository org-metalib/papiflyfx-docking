package org.metalib.papifly.fx.code.render;

import javafx.beans.value.ChangeListener;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.document.DocumentChangeListener;
import org.metalib.papifly.fx.code.lexer.Token;
import org.metalib.papifly.fx.code.lexer.TokenMap;
import org.metalib.papifly.fx.code.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * Canvas-based virtualized text renderer.
 * <p>
 * Draws only the visible lines of a {@link Document} on a {@link Canvas},
 * with caret and selection rendering. Listens for document changes and
 * redraws as needed.
 */
public class Viewport extends Region {

    private static final int PREFETCH_LINES = 2;
    private static final Color BACKGROUND_COLOR = Color.web("#1e1e1e");
    private static final Color TEXT_COLOR = Color.web("#d4d4d4");
    private static final Color CURRENT_LINE_COLOR = Color.web("#2a2d2e");
    private static final Color SELECTION_COLOR = Color.web("#264f78");
    private static final Color CARET_COLOR = Color.web("#aeafad");
    private static final Color KEYWORD_COLOR = Color.web("#569cd6");
    private static final Color STRING_COLOR = Color.web("#ce9178");
    private static final Color COMMENT_COLOR = Color.web("#6a9955");
    private static final Color NUMBER_COLOR = Color.web("#b5cea8");
    private static final Color BOOLEAN_COLOR = Color.web("#4ec9b0");
    private static final Color NULL_COLOR = Color.web("#4ec9b0");
    private static final Color HEADLINE_COLOR = Color.web("#569cd6");
    private static final Color LIST_ITEM_COLOR = Color.web("#9cdcfe");
    private static final Color CODE_BLOCK_COLOR = Color.web("#d7ba7d");

    private final Canvas canvas;
    private final GlyphCache glyphCache;
    private final SelectionModel selectionModel;
    private final ChangeListener<Number> caretLineListener = (obs, oldValue, newValue) -> markDirty();
    private final ChangeListener<Number> caretColumnListener = (obs, oldValue, newValue) -> markDirty();

    private Document document;
    private double scrollOffset;
    private boolean dirty = true;
    private boolean disposed;
    private TokenMap tokenMap = TokenMap.empty();

    private int firstVisibleLine;
    private int visibleLineCount;
    private final List<RenderLine> renderLines = new ArrayList<>();

    private final DocumentChangeListener changeListener = event -> markDirty();

    /**
     * Creates a viewport with the given selection model.
     */
    public Viewport(SelectionModel selectionModel) {
        this.selectionModel = selectionModel;
        this.glyphCache = new GlyphCache();
        this.canvas = new Canvas();

        getChildren().add(canvas);

        // Redraw on caret move
        selectionModel.caretLineProperty().addListener(caretLineListener);
        selectionModel.caretColumnProperty().addListener(caretColumnListener);
    }

    /**
     * Sets the document to render.
     */
    public void setDocument(Document document) {
        if (this.document != null) {
            this.document.removeChangeListener(changeListener);
        }
        this.document = document;
        if (this.document != null) {
            this.document.addChangeListener(changeListener);
        }
        markDirty();
    }

    /**
     * Returns the current document.
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Returns the glyph cache for font metrics.
     */
    public GlyphCache getGlyphCache() {
        return glyphCache;
    }

    /**
     * Returns the selection model.
     */
    public SelectionModel getSelectionModel() {
        return selectionModel;
    }

    /**
     * Sets syntax token map used for per-line rendering.
     */
    public void setTokenMap(TokenMap tokenMap) {
        this.tokenMap = tokenMap == null ? TokenMap.empty() : tokenMap;
        markDirty();
    }

    /**
     * Returns current token map.
     */
    public TokenMap getTokenMap() {
        return tokenMap;
    }

    /**
     * Sets the font for rendering.
     */
    public void setFont(Font font) {
        glyphCache.setFont(font);
        markDirty();
    }

    /**
     * Sets the vertical scroll offset in pixels.
     */
    public void setScrollOffset(double offset) {
        double maxScroll = computeMaxScrollOffset();
        this.scrollOffset = Math.max(0, Math.min(offset, maxScroll));
        markDirty();
    }

    /**
     * Returns the current vertical scroll offset.
     */
    public double getScrollOffset() {
        return scrollOffset;
    }

    /**
     * Returns the first visible line index.
     */
    public int getFirstVisibleLine() {
        return firstVisibleLine;
    }

    /**
     * Returns the count of visible lines.
     */
    public int getVisibleLineCount() {
        return visibleLineCount;
    }

    /**
     * Marks the viewport as needing a redraw and schedules one.
     */
    public void markDirty() {
        dirty = true;
        requestLayout();
    }

    /**
     * Returns true if the viewport needs redrawing.
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Ensures the caret line is visible by adjusting scroll offset.
     */
    public void ensureCaretVisible() {
        if (document == null) {
            return;
        }
        double lineHeight = glyphCache.getLineHeight();
        int caretLine = selectionModel.getCaretLine();
        double caretY = caretLine * lineHeight;
        double viewportHeight = getHeight();

        if (caretY < scrollOffset) {
            setScrollOffset(caretY);
        } else if (caretY + lineHeight > scrollOffset + viewportHeight) {
            setScrollOffset(caretY + lineHeight - viewportHeight);
        }
    }

    /**
     * Returns the line index at the given y pixel coordinate, or -1 if outside.
     */
    public int getLineAtY(double y) {
        if (document == null) {
            return -1;
        }
        double lineHeight = glyphCache.getLineHeight();
        int line = (int) ((y + scrollOffset) / lineHeight);
        if (line < 0) {
            return 0;
        }
        if (line >= document.getLineCount()) {
            return document.getLineCount() - 1;
        }
        return line;
    }

    /**
     * Returns the column index at the given x pixel coordinate for a line.
     */
    public int getColumnAtX(double x) {
        double charWidth = glyphCache.getCharWidth();
        int col = (int) Math.round(x / charWidth);
        return Math.max(0, col);
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

    private void redrawIfDirty() {
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
        double charWidth = glyphCache.getCharWidth();

        // Compute visible range
        computeVisibleRange(h, lineHeight);
        buildRenderLines();

        // Clear background
        gc.setFill(BACKGROUND_COLOR);
        gc.fillRect(0, 0, w, h);

        // Draw layers
        drawCurrentLineHighlight(gc, w);
        drawSelection(gc, w, lineHeight, charWidth);
        drawText(gc, lineHeight, charWidth);
        drawCaret(gc, lineHeight, charWidth);
    }

    private void computeVisibleRange(double viewportHeight, double lineHeight) {
        int totalLines = document.getLineCount();
        firstVisibleLine = Math.max(0, (int) (scrollOffset / lineHeight) - PREFETCH_LINES);
        int lastVisible = Math.min(totalLines - 1,
            (int) ((scrollOffset + viewportHeight) / lineHeight) + PREFETCH_LINES);
        visibleLineCount = lastVisible - firstVisibleLine + 1;
    }

    private void buildRenderLines() {
        renderLines.clear();
        double lineHeight = glyphCache.getLineHeight();
        for (int i = 0; i < visibleLineCount; i++) {
            int lineIdx = firstVisibleLine + i;
            if (lineIdx >= document.getLineCount()) {
                break;
            }
            double y = lineIdx * lineHeight - scrollOffset;
            renderLines.add(new RenderLine(
                lineIdx,
                document.getLineText(lineIdx),
                y,
                tokenMap.tokensForLine(lineIdx)
            ));
        }
    }

    private void drawCurrentLineHighlight(GraphicsContext gc, double w) {
        int caretLine = selectionModel.getCaretLine();
        for (RenderLine rl : renderLines) {
            if (rl.lineIndex() == caretLine && !selectionModel.hasSelection()) {
                gc.setFill(CURRENT_LINE_COLOR);
                gc.fillRect(0, rl.y(), w, glyphCache.getLineHeight());
                break;
            }
        }
    }

    private void drawSelection(GraphicsContext gc, double w, double lineHeight, double charWidth) {
        if (!selectionModel.hasSelection()) {
            return;
        }
        int startLine = selectionModel.getSelectionStartLine();
        int startCol = selectionModel.getSelectionStartColumn();
        int endLine = selectionModel.getSelectionEndLine();
        int endCol = selectionModel.getSelectionEndColumn();

        gc.setFill(SELECTION_COLOR);
        for (RenderLine rl : renderLines) {
            int line = rl.lineIndex();
            if (line < startLine || line > endLine) {
                continue;
            }
            double selX;
            double selW;
            if (line == startLine && line == endLine) {
                selX = startCol * charWidth;
                selW = (endCol - startCol) * charWidth;
            } else if (line == startLine) {
                selX = startCol * charWidth;
                selW = w - selX;
            } else if (line == endLine) {
                selX = 0;
                selW = endCol * charWidth;
            } else {
                selX = 0;
                selW = w;
            }
            gc.fillRect(selX, rl.y(), selW, lineHeight);
        }
    }

    private void drawText(GraphicsContext gc, double lineHeight, double charWidth) {
        gc.setFont(glyphCache.getFont());

        // Text baseline offset (ascent from top)
        double baseline = lineHeight * 0.8;

        for (RenderLine rl : renderLines) {
            drawTokenizedLine(gc, rl, charWidth, baseline);
        }
    }

    private void drawTokenizedLine(GraphicsContext gc, RenderLine renderLine, double charWidth, double baseline) {
        String text = renderLine.text();
        List<Token> tokens = renderLine.tokens();
        if (tokens.isEmpty()) {
            gc.setFill(TEXT_COLOR);
            gc.fillText(text, 0, renderLine.y() + baseline);
            return;
        }

        int cursor = 0;
        int textLength = text.length();
        for (Token token : tokens) {
            int start = Math.max(0, Math.min(token.startColumn(), textLength));
            int end = Math.max(start, Math.min(token.endColumn(), textLength));
            if (start > cursor) {
                drawSegment(gc, text, cursor, start, renderLine.y(), charWidth, baseline, TEXT_COLOR);
            }
            if (end > start) {
                drawSegment(gc, text, start, end, renderLine.y(), charWidth, baseline, tokenColor(token.type()));
            }
            cursor = Math.max(cursor, end);
        }
        if (cursor < textLength) {
            drawSegment(gc, text, cursor, textLength, renderLine.y(), charWidth, baseline, TEXT_COLOR);
        }
    }

    private void drawSegment(
        GraphicsContext gc,
        String text,
        int startColumn,
        int endColumn,
        double y,
        double charWidth,
        double baseline,
        Color color
    ) {
        if (endColumn <= startColumn) {
            return;
        }
        gc.setFill(color);
        gc.fillText(text.substring(startColumn, endColumn), startColumn * charWidth, y + baseline);
    }

    private Color tokenColor(TokenType tokenType) {
        if (tokenType == null) {
            return TEXT_COLOR;
        }
        return switch (tokenType) {
            case KEYWORD -> KEYWORD_COLOR;
            case STRING -> STRING_COLOR;
            case COMMENT -> COMMENT_COLOR;
            case NUMBER -> NUMBER_COLOR;
            case BOOLEAN -> BOOLEAN_COLOR;
            case NULL_LITERAL -> NULL_COLOR;
            case HEADLINE -> HEADLINE_COLOR;
            case LIST_ITEM -> LIST_ITEM_COLOR;
            case CODE_BLOCK -> CODE_BLOCK_COLOR;
            case TEXT -> TEXT_COLOR;
            default -> TEXT_COLOR;
        };
    }

    private void drawCaret(GraphicsContext gc, double lineHeight, double charWidth) {
        int caretLine = selectionModel.getCaretLine();
        for (RenderLine rl : renderLines) {
            if (rl.lineIndex() == caretLine) {
                double caretX = selectionModel.getCaretColumn() * charWidth;
                gc.setStroke(CARET_COLOR);
                gc.setLineWidth(2);
                gc.strokeLine(caretX, rl.y(), caretX, rl.y() + lineHeight);
                break;
            }
        }
    }

    private double computeMaxScrollOffset() {
        if (document == null) {
            return 0;
        }
        double totalHeight = document.getLineCount() * glyphCache.getLineHeight();
        return Math.max(0, totalHeight - getHeight());
    }

    /**
     * Releases listeners and cached render data for this viewport.
     */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        setDocument(null);
        selectionModel.caretLineProperty().removeListener(caretLineListener);
        selectionModel.caretColumnProperty().removeListener(caretColumnListener);
        renderLines.clear();
        tokenMap = TokenMap.empty();
    }
}
