package org.metalib.papifly.fx.code.render;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.util.Duration;
import org.metalib.papifly.fx.code.command.CaretRange;
import org.metalib.papifly.fx.code.command.MultiCaretModel;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.document.DocumentChangeEvent;
import org.metalib.papifly.fx.code.document.DocumentChangeListener;
import org.metalib.papifly.fx.code.lexer.TokenMap;
import org.metalib.papifly.fx.code.search.SearchMatch;
import org.metalib.papifly.fx.code.theme.CodeEditorTheme;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Canvas-based virtualized text renderer.
 * <p>
 * Draws only the visible lines of a {@link Document} on a {@link Canvas},
 * with caret and selection rendering. Listens for document changes and
 * redraws as needed.
 */
public class Viewport extends Region {

    private static final int PREFETCH_LINES = 2;
    private static final Duration DEFAULT_CARET_BLINK_DELAY = Duration.millis(500);
    private static final Duration DEFAULT_CARET_BLINK_PERIOD = Duration.millis(500);

    private final Canvas canvas;
    private final GlyphCache glyphCache;
    private final SelectionModel selectionModel;
    private final ChangeListener<Number> caretLineListener = (obs, oldValue, newValue) -> onCaretLineChanged(oldValue.intValue(), newValue.intValue());
    private final ChangeListener<Number> caretColumnListener = (obs, oldValue, newValue) -> onCaretColumnChanged();
    private final ChangeListener<Number> anchorLineListener = (obs, oldValue, newValue) -> onSelectionAnchorChanged();
    private final ChangeListener<Number> anchorColumnListener = (obs, oldValue, newValue) -> onSelectionAnchorChanged();
    private final PauseTransition caretBlinkDelay = new PauseTransition(DEFAULT_CARET_BLINK_DELAY);
    private final Timeline caretBlinkTimeline = new Timeline();
    private final ViewportInvalidationPlanner invalidationPlanner = new ViewportInvalidationPlanner();

    private CodeEditorTheme theme = CodeEditorTheme.dark();
    private Document document;
    private double scrollOffset;
    private boolean dirty = true;
    private boolean fullRedrawRequired = true;
    private final BitSet dirtyLines = new BitSet();
    private boolean disposed;
    private TokenMap tokenMap = TokenMap.empty();
    private List<SearchMatch> searchMatches = List.of();
    private Map<Integer, List<Integer>> searchMatchIndexesByLine = Map.of();
    private int currentSearchMatchIndex = -1;
    private MultiCaretModel multiCaretModel;

    private int firstVisibleLine;
    private int visibleLineCount;
    private int previousCaretLine = -1;
    private boolean previousSelectionActive;
    private int previousSelectionStartLine = -1;
    private int previousSelectionEndLine = -1;
    private final List<RenderLine> renderLines = new ArrayList<>();
    private final List<RenderPass> renderPasses = List.of(
        new BackgroundPass(),
        new SearchPass(),
        new SelectionPass(),
        new TextPass(),
        new CaretPass()
    );
    private boolean caretBlinkActive;
    private boolean caretVisible = true;

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

        configureCaretBlink(DEFAULT_CARET_BLINK_DELAY, DEFAULT_CARET_BLINK_PERIOD);
        caretBlinkDelay.setOnFinished(event -> {
            if (caretBlinkActive && !disposed) {
                caretBlinkTimeline.playFromStart();
            }
        });
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
        this.searchMatchIndexesByLine = indexMatchesByLine(this.searchMatches);
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
     * Enables/disables caret blinking and caret visibility.
     * <p>
     * Typically bound to editor focus state.
     */
    public void setCaretBlinkActive(boolean active) {
        if (caretBlinkActive == active) {
            return;
        }
        caretBlinkActive = active;
        if (active) {
            showCaretAndRestartBlink();
        } else {
            stopCaretBlink();
            if (caretVisible) {
                caretVisible = false;
                markCaretLinesDirty();
            }
        }
    }

    /**
     * Returns true when caret blink animation is active.
     */
    public boolean isCaretBlinkActive() {
        return caretBlinkActive;
    }

    /**
     * Resets blink cycle and makes caret immediately visible.
     */
    public void resetCaretBlink() {
        if (disposed || !caretBlinkActive) {
            return;
        }
        if (!caretVisible) {
            caretVisible = true;
            markCaretLinesDirty();
        }
        restartCaretBlink();
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

    void setCaretBlinkTimings(Duration delay, Duration period) {
        Duration safeDelay = sanitizeDuration(delay, DEFAULT_CARET_BLINK_DELAY);
        Duration safePeriod = sanitizeDuration(period, DEFAULT_CARET_BLINK_PERIOD);
        configureCaretBlink(safeDelay, safePeriod);
        if (caretBlinkActive) {
            restartCaretBlink();
        }
    }

    boolean isCaretVisible() {
        return caretVisible;
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
        resetCaretBlink();
        ViewportInvalidationPlanner.InvalidationPlan plan = invalidationPlanner.plan(
            document,
            event,
            firstVisibleLine,
            visibleLineCount,
            PREFETCH_LINES
        );
        if (plan.fullRedraw()) {
            markDirty();
            return;
        }
        if (plan.hasLineRange()) {
            markLinesDirty(plan.startLine(), plan.endLine());
        }
    }

    private void onCaretLineChanged(int oldLine, int newLine) {
        // Dirty both old and new caret lines for highlight update
        dirtyLines.set(oldLine);
        dirtyLines.set(newLine);
        markSelectionRangeDirty();
        resetCaretBlink();
        dirty = true;
        requestLayout();
    }

    private void onCaretColumnChanged() {
        // Dirty the current caret line for caret position update
        int caretLine = selectionModel.getCaretLine();
        dirtyLines.set(caretLine);
        markSelectionRangeDirty();
        resetCaretBlink();
        dirty = true;
        requestLayout();
    }

    private void onSelectionAnchorChanged() {
        markSelectionRangeDirty();
        resetCaretBlink();
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
        boolean paintCaret = shouldPaintCaret();

        int previousFirstVisible = firstVisibleLine;

        // Compute visible range
        computeVisibleRange(h, lineHeight);
        buildRenderLines();

        // Determine if we need full redraw or can do incremental
        boolean doFullRedraw = fullRedrawRequired || previousFirstVisible != firstVisibleLine;
        fullRedrawRequired = false;
        RenderContext renderContext = new RenderContext(
            gc,
            theme,
            glyphCache,
            selectionModel,
            renderLines,
            activeCarets,
            hasMultiCarets,
            paintCaret,
            searchMatches,
            searchMatchIndexesByLine,
            currentSearchMatchIndex,
            firstVisibleLine,
            visibleLineCount,
            w,
            h,
            lineHeight,
            charWidth,
            glyphCache.getBaselineOffset(),
            scrollOffset
        );

        if (doFullRedraw) {
            dirtyLines.clear();
            for (RenderPass renderPass : renderPasses) {
                renderPass.renderFull(renderContext);
            }
        } else {
            int caretLine = selectionModel.getCaretLine();
            dirtyLines.set(caretLine);
            if (previousCaretLine >= 0) {
                dirtyLines.set(previousCaretLine);
            }

            for (RenderLine renderLine : renderLines) {
                if (dirtyLines.get(renderLine.lineIndex())) {
                    for (RenderPass renderPass : renderPasses) {
                        renderPass.renderLine(renderContext, renderLine);
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

    private Map<Integer, List<Integer>> indexMatchesByLine(List<SearchMatch> matches) {
        if (matches.isEmpty()) {
            return Map.of();
        }
        Map<Integer, List<Integer>> byLine = new HashMap<>();
        for (int i = 0; i < matches.size(); i++) {
            byLine.computeIfAbsent(matches.get(i).line(), key -> new ArrayList<>()).add(i);
        }
        return byLine;
    }

    private boolean shouldPaintCaret() {
        return caretBlinkActive && caretVisible;
    }

    private void showCaretAndRestartBlink() {
        if (!caretVisible) {
            caretVisible = true;
        }
        markCaretLinesDirty();
        restartCaretBlink();
    }

    private void restartCaretBlink() {
        if (disposed || !caretBlinkActive) {
            return;
        }
        stopCaretBlink();
        caretBlinkDelay.playFromStart();
    }

    private void stopCaretBlink() {
        caretBlinkDelay.stop();
        caretBlinkTimeline.stop();
    }

    private void toggleCaretVisibility() {
        if (disposed || !caretBlinkActive) {
            return;
        }
        caretVisible = !caretVisible;
        markCaretLinesDirty();
    }

    private void markCaretLinesDirty() {
        if (document == null) {
            return;
        }
        List<CaretRange> activeCarets = collectActiveCarets();
        if (!activeCarets.isEmpty()) {
            for (CaretRange caret : activeCarets) {
                dirtyLines.set(caret.caretLine());
            }
        } else {
            dirtyLines.set(selectionModel.getCaretLine());
            if (previousCaretLine >= 0) {
                dirtyLines.set(previousCaretLine);
            }
        }
        dirty = true;
        requestLayout();
    }

    private void configureCaretBlink(Duration delay, Duration period) {
        caretBlinkDelay.setDuration(delay);
        caretBlinkTimeline.getKeyFrames().setAll(new KeyFrame(period, event -> toggleCaretVisibility()));
        caretBlinkTimeline.setCycleCount(Animation.INDEFINITE);
    }

    private Duration sanitizeDuration(Duration value, Duration fallback) {
        if (value == null || value.isUnknown() || value.lessThanOrEqualTo(Duration.ZERO)) {
            return fallback;
        }
        return value;
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
        stopCaretBlink();
        caretBlinkActive = false;
        caretVisible = false;
        setDocument(null);
        selectionModel.caretLineProperty().removeListener(caretLineListener);
        selectionModel.caretColumnProperty().removeListener(caretColumnListener);
        selectionModel.anchorLineProperty().removeListener(anchorLineListener);
        selectionModel.anchorColumnProperty().removeListener(anchorColumnListener);
        renderLines.clear();
        dirtyLines.clear();
        tokenMap = TokenMap.empty();
        searchMatches = List.of();
        searchMatchIndexesByLine = Map.of();
        currentSearchMatchIndex = -1;
    }
}
