package org.metalib.papifly.fx.code.render;

import javafx.beans.value.ChangeListener;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import org.metalib.papifly.fx.code.command.CaretRange;
import org.metalib.papifly.fx.code.command.MultiCaretModel;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.document.DocumentChangeListener;
import org.metalib.papifly.fx.code.lexer.Token;
import org.metalib.papifly.fx.code.lexer.TokenMap;
import org.metalib.papifly.fx.code.lexer.TokenType;
import org.metalib.papifly.fx.code.search.SearchMatch;
import org.metalib.papifly.fx.code.theme.CodeEditorTheme;

import org.metalib.papifly.fx.code.document.DocumentChangeEvent;

import java.util.ArrayList;
import java.util.BitSet;
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

    private final Canvas canvas;
    private final GlyphCache glyphCache;
    private final SelectionModel selectionModel;
    private final ChangeListener<Number> caretLineListener = (obs, oldValue, newValue) -> onCaretLineChanged(oldValue.intValue(), newValue.intValue());
    private final ChangeListener<Number> caretColumnListener = (obs, oldValue, newValue) -> onCaretColumnChanged();
    private final ChangeListener<Number> anchorLineListener = (obs, oldValue, newValue) -> onSelectionAnchorChanged();
    private final ChangeListener<Number> anchorColumnListener = (obs, oldValue, newValue) -> onSelectionAnchorChanged();

    private CodeEditorTheme theme = CodeEditorTheme.dark();
    private Document document;
    private double scrollOffset;
    private boolean dirty = true;
    private boolean fullRedrawRequired = true;
    private final BitSet dirtyLines = new BitSet();
    private boolean disposed;
    private TokenMap tokenMap = TokenMap.empty();
    private List<SearchMatch> searchMatches = List.of();
    private int currentSearchMatchIndex = -1;
    private MultiCaretModel multiCaretModel;

    private int firstVisibleLine;
    private int visibleLineCount;
    private int previousCaretLine = -1;
    private boolean previousSelectionActive;
    private int previousSelectionStartLine = -1;
    private int previousSelectionEndLine = -1;
    private final List<RenderLine> renderLines = new ArrayList<>();

    private final DocumentChangeListener changeListener = this::onDocumentChanged;

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
        selectionModel.anchorLineProperty().addListener(anchorLineListener);
        selectionModel.anchorColumnProperty().addListener(anchorColumnListener);
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
     * Sets the editor theme palette and triggers a redraw.
     */
    public void setTheme(CodeEditorTheme theme) {
        this.theme = theme == null ? CodeEditorTheme.dark() : theme;
        markDirty();
    }

    /**
     * Returns the current editor theme.
     */
    public CodeEditorTheme getTheme() {
        return theme;
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
     * Sets search match highlights to render.
     */
    public void setSearchMatches(List<SearchMatch> matches, int currentIndex) {
        this.searchMatches = matches == null ? List.of() : matches;
        this.currentSearchMatchIndex = currentIndex;
        markDirty();
    }

    /**
     * Returns the current search matches.
     */
    public List<SearchMatch> getSearchMatches() {
        return searchMatches;
    }

    /**
     * Sets the multi-caret model for rendering multiple carets and selections.
     */
    public void setMultiCaretModel(MultiCaretModel multiCaretModel) {
        this.multiCaretModel = multiCaretModel;
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
     * Marks the viewport as needing a full redraw and schedules one.
     */
    public void markDirty() {
        dirty = true;
        fullRedrawRequired = true;
        requestLayout();
    }

    /**
     * Marks specific document lines as dirty for incremental redraw.
     */
    public void markLinesDirty(int startLine, int endLine) {
        for (int i = startLine; i <= endLine; i++) {
            dirtyLines.set(i);
        }
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
        int line = (int) Math.floor((y + scrollOffset) / lineHeight);
        if (line < 0) {
            return -1;
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

    private void onDocumentChanged(DocumentChangeEvent event) {
        if (document == null) {
            markDirty();
            return;
        }
        int startLine = document.getLineForOffset(Math.min(event.offset(), document.length()));
        // For inserts/deletes that may affect all lines from startLine onward
        int endLine = document.getLineCount() - 1;
        markLinesDirty(startLine, endLine);
    }

    private void onCaretLineChanged(int oldLine, int newLine) {
        // Dirty both old and new caret lines for highlight update
        dirtyLines.set(oldLine);
        dirtyLines.set(newLine);
        markSelectionRangeDirty();
        dirty = true;
        requestLayout();
    }

    private void onCaretColumnChanged() {
        // Dirty the current caret line for caret position update
        int caretLine = selectionModel.getCaretLine();
        dirtyLines.set(caretLine);
        markSelectionRangeDirty();
        dirty = true;
        requestLayout();
    }

    private void onSelectionAnchorChanged() {
        markSelectionRangeDirty();
        dirty = true;
        requestLayout();
    }

    private void markSelectionRangeDirty() {
        if (multiCaretModel != null && multiCaretModel.hasMultipleCarets()) {
            fullRedrawRequired = true;
            return;
        }
        boolean hasSelection = selectionModel.hasSelection();
        if (!previousSelectionActive && !hasSelection) {
            return;
        }
        if (previousSelectionActive) {
            dirtyLineRange(previousSelectionStartLine, previousSelectionEndLine);
        }
        if (hasSelection) {
            int startLine = selectionModel.getSelectionStartLine();
            int endLine = selectionModel.getSelectionEndLine();
            dirtyLineRange(startLine, endLine);
            previousSelectionStartLine = startLine;
            previousSelectionEndLine = endLine;
        } else {
            previousSelectionStartLine = -1;
            previousSelectionEndLine = -1;
        }
        previousSelectionActive = hasSelection;
    }

    private void dirtyLineRange(int startLine, int endLine) {
        if (startLine < 0 || endLine < 0 || startLine > endLine) {
            return;
        }
        dirtyLines.set(startLine, endLine + 1);
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        if (w != canvas.getWidth() || h != canvas.getHeight()) {
            canvas.setWidth(w);
            canvas.setHeight(h);
            fullRedrawRequired = true;
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
        double charWidth = glyphCache.getCharWidth();
        List<CaretRange> activeCarets = collectActiveCarets();
        boolean hasMultiCarets = !activeCarets.isEmpty();

        int previousFirstVisible = firstVisibleLine;

        // Compute visible range
        computeVisibleRange(h, lineHeight);
        buildRenderLines();

        // Determine if we need full redraw or can do incremental
        boolean doFullRedraw = fullRedrawRequired || previousFirstVisible != firstVisibleLine;
        fullRedrawRequired = false;

        if (doFullRedraw) {
            // Full redraw path
            dirtyLines.clear();
            gc.setFill(theme.editorBackground());
            gc.fillRect(0, 0, w, h);

            drawCurrentLineHighlight(gc, w);
            drawSearchHighlights(gc, lineHeight, charWidth);
            drawSelection(gc, w, lineHeight, charWidth, activeCarets);
            drawText(gc, lineHeight, charWidth);
            drawCaret(gc, lineHeight, charWidth, activeCarets);
        } else {
            // Incremental redraw: only repaint dirty lines
            int caretLine = selectionModel.getCaretLine();
            // Always include caret line and previous caret line
            dirtyLines.set(caretLine);
            if (previousCaretLine >= 0) {
                dirtyLines.set(previousCaretLine);
            }

            for (RenderLine rl : renderLines) {
                if (dirtyLines.get(rl.lineIndex())) {
                    // Clear the line area
                    gc.setFill(theme.editorBackground());
                    gc.fillRect(0, rl.y(), w, lineHeight);

                    // Redraw current-line highlight
                    if (rl.lineIndex() == caretLine && !selectionModel.hasSelection()) {
                        gc.setFill(theme.currentLineColor());
                        gc.fillRect(0, rl.y(), w, lineHeight);
                    }

                    // Redraw search highlights on this line
                    drawSearchHighlightsForLine(gc, rl, lineHeight, charWidth);

                    // Redraw selection on this line
                    drawSelectionForLine(gc, rl, w, lineHeight, charWidth, activeCarets);

                    // Redraw text
                    gc.setFont(glyphCache.getFont());
                    drawTokenizedLine(gc, rl, charWidth, glyphCache.getBaselineOffset());

                    // Redraw caret(s)
                    if (hasMultiCarets) {
                        gc.setFill(theme.caretColor());
                        for (CaretRange cr : activeCarets) {
                            if (rl.lineIndex() == cr.caretLine()) {
                                gc.fillRect(cr.caretColumn() * charWidth, rl.y(), 2, lineHeight);
                            }
                        }
                    } else if (rl.lineIndex() == caretLine) {
                        double caretX = selectionModel.getCaretColumn() * charWidth;
                        gc.setFill(theme.caretColor());
                        gc.fillRect(caretX, rl.y(), 2, lineHeight);
                    }
                }
            }
            dirtyLines.clear();
        }
        previousCaretLine = selectionModel.getCaretLine();
    }

    private List<CaretRange> collectActiveCarets() {
        if (document == null || multiCaretModel == null || !multiCaretModel.hasMultipleCarets()) {
            return List.of();
        }
        return multiCaretModel.allCarets(document);
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
                gc.setFill(theme.currentLineColor());
                gc.fillRect(0, rl.y(), w, glyphCache.getLineHeight());
                break;
            }
        }
    }

    private void drawSelection(
        GraphicsContext gc,
        double w,
        double lineHeight,
        double charWidth,
        List<CaretRange> activeCarets
    ) {
        if (!activeCarets.isEmpty()) {
            gc.setFill(theme.selectionColor());
            for (CaretRange caret : activeCarets) {
                if (caret.hasSelection()) {
                    drawSelectionRange(gc, caret.getStartLine(), caret.getStartColumn(),
                        caret.getEndLine(), caret.getEndColumn(), w, lineHeight, charWidth);
                }
            }
            return;
        }
        if (!selectionModel.hasSelection()) {
            return;
        }
        gc.setFill(theme.selectionColor());
        drawSelectionRange(gc, selectionModel.getSelectionStartLine(), selectionModel.getSelectionStartColumn(),
            selectionModel.getSelectionEndLine(), selectionModel.getSelectionEndColumn(),
            w, lineHeight, charWidth);
    }

    private void drawSelectionRange(GraphicsContext gc, int startLine, int startCol,
                                     int endLine, int endCol,
                                     double w, double lineHeight, double charWidth) {
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

        // Text baseline offset derived from actual font metrics.
        double baseline = glyphCache.getBaselineOffset();

        for (RenderLine rl : renderLines) {
            drawTokenizedLine(gc, rl, charWidth, baseline);
        }
    }

    private void drawTokenizedLine(GraphicsContext gc, RenderLine renderLine, double charWidth, double baseline) {
        String text = renderLine.text();
        List<Token> tokens = renderLine.tokens();
        if (tokens.isEmpty()) {
            gc.setFill(theme.editorForeground());
            gc.fillText(text, 0, renderLine.y() + baseline);
            return;
        }

        int cursor = 0;
        int textLength = text.length();
        for (Token token : tokens) {
            int start = Math.max(0, Math.min(token.startColumn(), textLength));
            int end = Math.max(start, Math.min(token.endColumn(), textLength));
            if (start > cursor) {
                drawSegment(gc, text, cursor, start, renderLine.y(), charWidth, baseline, theme.editorForeground());
            }
            if (end > start) {
                drawSegment(gc, text, start, end, renderLine.y(), charWidth, baseline, tokenColor(token.type()));
            }
            cursor = Math.max(cursor, end);
        }
        if (cursor < textLength) {
            drawSegment(gc, text, cursor, textLength, renderLine.y(), charWidth, baseline, theme.editorForeground());
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
        Paint color
    ) {
        if (endColumn <= startColumn) {
            return;
        }
        gc.setFill(color);
        gc.fillText(text.substring(startColumn, endColumn), startColumn * charWidth, y + baseline);
    }

    private Paint tokenColor(TokenType tokenType) {
        if (tokenType == null) {
            return theme.editorForeground();
        }
        return switch (tokenType) {
            case KEYWORD -> theme.keywordColor();
            case STRING -> theme.stringColor();
            case COMMENT -> theme.commentColor();
            case NUMBER -> theme.numberColor();
            case BOOLEAN -> theme.booleanColor();
            case NULL_LITERAL -> theme.nullLiteralColor();
            case HEADLINE -> theme.headlineColor();
            case LIST_ITEM -> theme.listItemColor();
            case CODE_BLOCK -> theme.codeBlockColor();
            case TEXT -> theme.editorForeground();
            default -> theme.editorForeground();
        };
    }

    private void drawSearchHighlightsForLine(GraphicsContext gc, RenderLine rl, double lineHeight, double charWidth) {
        if (searchMatches.isEmpty()) {
            return;
        }
        for (int i = 0; i < searchMatches.size(); i++) {
            SearchMatch match = searchMatches.get(i);
            if (rl.lineIndex() == match.line()) {
                double x = match.startColumn() * charWidth;
                double w = (match.endColumn() - match.startColumn()) * charWidth;
                gc.setFill(i == currentSearchMatchIndex ? theme.searchCurrentColor() : theme.searchHighlightColor());
                gc.fillRect(x, rl.y(), w, lineHeight);
            }
        }
    }

    private void drawSelectionForLine(
        GraphicsContext gc,
        RenderLine rl,
        double w,
        double lineHeight,
        double charWidth,
        List<CaretRange> activeCarets
    ) {
        if (!activeCarets.isEmpty()) {
            gc.setFill(theme.selectionColor());
            for (CaretRange caret : activeCarets) {
                if (caret.hasSelection()) {
                    drawSelectionRangeForLine(gc, rl, caret.getStartLine(), caret.getStartColumn(),
                        caret.getEndLine(), caret.getEndColumn(), w, lineHeight, charWidth);
                }
            }
            return;
        }
        if (!selectionModel.hasSelection()) {
            return;
        }
        gc.setFill(theme.selectionColor());
        drawSelectionRangeForLine(gc, rl, selectionModel.getSelectionStartLine(), selectionModel.getSelectionStartColumn(),
            selectionModel.getSelectionEndLine(), selectionModel.getSelectionEndColumn(),
            w, lineHeight, charWidth);
    }

    private void drawSelectionRangeForLine(GraphicsContext gc, RenderLine rl,
                                            int startLine, int startCol, int endLine, int endCol,
                                            double w, double lineHeight, double charWidth) {
        int line = rl.lineIndex();
        if (line < startLine || line > endLine) {
            return;
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

    private void drawSearchHighlights(GraphicsContext gc, double lineHeight, double charWidth) {
        if (searchMatches.isEmpty()) {
            return;
        }
        for (int i = 0; i < searchMatches.size(); i++) {
            SearchMatch match = searchMatches.get(i);
            for (RenderLine rl : renderLines) {
                if (rl.lineIndex() == match.line()) {
                    double x = match.startColumn() * charWidth;
                    double w = (match.endColumn() - match.startColumn()) * charWidth;
                    gc.setFill(i == currentSearchMatchIndex ? theme.searchCurrentColor() : theme.searchHighlightColor());
                    gc.fillRect(x, rl.y(), w, lineHeight);
                    break;
                }
            }
        }
    }

    private void drawCaret(GraphicsContext gc, double lineHeight, double charWidth, List<CaretRange> activeCarets) {
        if (!activeCarets.isEmpty()) {
            gc.setFill(theme.caretColor());
            for (CaretRange caret : activeCarets) {
                for (RenderLine rl : renderLines) {
                    if (rl.lineIndex() == caret.caretLine()) {
                        double caretX = caret.caretColumn() * charWidth;
                        gc.fillRect(caretX, rl.y(), 2, lineHeight);
                    }
                }
            }
            return;
        }
        int caretLine = selectionModel.getCaretLine();
        for (RenderLine rl : renderLines) {
            if (rl.lineIndex() == caretLine) {
                double caretX = selectionModel.getCaretColumn() * charWidth;
                gc.setFill(theme.caretColor());
                gc.fillRect(caretX, rl.y(), 2, lineHeight);
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
        selectionModel.anchorLineProperty().removeListener(anchorLineListener);
        selectionModel.anchorColumnProperty().removeListener(anchorColumnListener);
        renderLines.clear();
        dirtyLines.clear();
        tokenMap = TokenMap.empty();
        searchMatches = List.of();
        currentSearchMatchIndex = -1;
    }
}
