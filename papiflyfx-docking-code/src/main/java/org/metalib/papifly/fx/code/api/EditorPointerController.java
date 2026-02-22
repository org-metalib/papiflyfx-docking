package org.metalib.papifly.fx.code.api;

import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import org.metalib.papifly.fx.code.command.CaretRange;
import org.metalib.papifly.fx.code.command.MultiCaretModel;
import org.metalib.papifly.fx.code.command.WordBoundary;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.render.SelectionModel;
import org.metalib.papifly.fx.code.render.Viewport;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;

/**
 * Handles mouse and wheel input orchestration for caret and selection behavior.
 */
final class EditorPointerController {

    private final BooleanSupplier disposedSupplier;
    private final Runnable clearPreferredVerticalColumn;
    private final Runnable requestFocusAction;
    private final Runnable resetCaretBlinkAction;
    private final Viewport viewport;
    private final Document document;
    private final SelectionModel selectionModel;
    private final MultiCaretModel multiCaretModel;
    private final Runnable markViewportDirty;
    private final DoubleConsumer setVerticalScrollOffset;
    private final double scrollLineFactor;

    private boolean boxSelectionActive;
    private int boxAnchorLine;
    private int boxAnchorCol;

    EditorPointerController(
        BooleanSupplier disposedSupplier,
        Runnable clearPreferredVerticalColumn,
        Runnable requestFocusAction,
        Runnable resetCaretBlinkAction,
        Viewport viewport,
        Document document,
        SelectionModel selectionModel,
        MultiCaretModel multiCaretModel,
        Runnable markViewportDirty,
        DoubleConsumer setVerticalScrollOffset,
        double scrollLineFactor
    ) {
        this.disposedSupplier = Objects.requireNonNull(disposedSupplier, "disposedSupplier");
        this.clearPreferredVerticalColumn = Objects.requireNonNull(clearPreferredVerticalColumn,
            "clearPreferredVerticalColumn");
        this.requestFocusAction = Objects.requireNonNull(requestFocusAction, "requestFocusAction");
        this.resetCaretBlinkAction = Objects.requireNonNull(resetCaretBlinkAction, "resetCaretBlinkAction");
        this.viewport = Objects.requireNonNull(viewport, "viewport");
        this.document = Objects.requireNonNull(document, "document");
        this.selectionModel = Objects.requireNonNull(selectionModel, "selectionModel");
        this.multiCaretModel = Objects.requireNonNull(multiCaretModel, "multiCaretModel");
        this.markViewportDirty = Objects.requireNonNull(markViewportDirty, "markViewportDirty");
        this.setVerticalScrollOffset = Objects.requireNonNull(setVerticalScrollOffset, "setVerticalScrollOffset");
        this.scrollLineFactor = scrollLineFactor;
    }

    void handleMousePressed(MouseEvent event) {
        if (disposedSupplier.getAsBoolean()) {
            return;
        }
        clearPreferredVerticalColumn.run();
        requestFocusAction.run();
        resetCaretBlinkAction.run();

        Point2D viewportPoint = toViewportLocalPoint(event);
        if (viewportPoint == null) {
            return;
        }

        int line = viewport.getLineAtY(viewportPoint.getY());
        if (line < 0) {
            return;
        }
        int col = clampColumn(line, viewport.getColumnAtX(viewportPoint.getX()));

        if (event.getClickCount() >= 3) {
            handleTripleClick(line);
            return;
        }
        if (event.getClickCount() == 2) {
            handleDoubleClick(line, col);
            return;
        }
        if (event.isAltDown() && !event.isShiftDown()) {
            handleAltClick(line, col);
            return;
        }
        if ((event.isShiftDown() && event.isAltDown()) || event.getButton() == MouseButton.MIDDLE) {
            startBoxSelection(line, col);
            return;
        }

        multiCaretModel.clearSecondaryCarets();
        if (event.isShiftDown()) {
            selectionModel.moveCaretWithSelection(line, col);
        } else {
            selectionModel.moveCaret(line, col);
        }
        markViewportDirty.run();
    }

    void handleMouseDragged(MouseEvent event) {
        if (disposedSupplier.getAsBoolean()) {
            return;
        }
        clearPreferredVerticalColumn.run();
        resetCaretBlinkAction.run();

        Point2D viewportPoint = toViewportLocalPoint(event);
        if (viewportPoint == null) {
            return;
        }

        int line = viewport.getLineAtY(viewportPoint.getY());
        if (line < 0) {
            return;
        }
        int col = clampColumn(line, viewport.getColumnAtX(viewportPoint.getX()));
        if (boxSelectionActive) {
            updateBoxSelection(line, col);
            return;
        }
        selectionModel.moveCaretWithSelection(line, col);
        markViewportDirty.run();
    }

    void handleMouseReleased() {
        if (disposedSupplier.getAsBoolean()) {
            return;
        }
        boxSelectionActive = false;
    }

    void handleScroll(ScrollEvent event) {
        if (disposedSupplier.getAsBoolean()) {
            return;
        }
        double delta = -event.getDeltaY() * scrollLineFactor;
        double newOffset = viewport.getScrollOffset() + delta;
        setVerticalScrollOffset.accept(newOffset);
        event.consume();
    }

    void dispose() {
        boxSelectionActive = false;
    }

    private Point2D toViewportLocalPoint(MouseEvent event) {
        Point2D viewportPoint = viewport.sceneToLocal(event.getSceneX(), event.getSceneY());
        if (viewportPoint == null) {
            return null;
        }
        if (!isFinite(viewportPoint.getX()) || !isFinite(viewportPoint.getY())) {
            return null;
        }
        return viewportPoint;
    }

    private static boolean isFinite(double value) {
        return Double.isFinite(value);
    }

    private int clampColumn(int line, int column) {
        return Math.min(column, document.getLineText(line).length());
    }

    private void handleDoubleClick(int line, int col) {
        multiCaretModel.clearSecondaryCarets();
        String lineText = document.getLineText(line);
        if (lineText.isEmpty()) {
            selectionModel.moveCaret(line, 0);
            markViewportDirty.run();
            return;
        }
        int clampedCol = Math.min(col, lineText.length() - 1);
        int wordStart = clampedCol;
        int wordEnd = clampedCol;
        if (WordBoundary.isWordChar(lineText.charAt(clampedCol))) {
            while (wordStart > 0 && WordBoundary.isWordChar(lineText.charAt(wordStart - 1))) {
                wordStart--;
            }
            while (wordEnd < lineText.length() - 1 && WordBoundary.isWordChar(lineText.charAt(wordEnd + 1))) {
                wordEnd++;
            }
            wordEnd++;
        } else {
            wordEnd = clampedCol + 1;
            wordStart = clampedCol;
        }
        selectionModel.moveCaret(line, wordStart);
        selectionModel.moveCaretWithSelection(line, wordEnd);
        markViewportDirty.run();
    }

    private void handleTripleClick(int line) {
        multiCaretModel.clearSecondaryCarets();
        int lineLength = document.getLineText(line).length();
        selectionModel.moveCaret(line, 0);
        selectionModel.moveCaretWithSelection(line, lineLength);
        markViewportDirty.run();
    }

    private void handleAltClick(int line, int col) {
        multiCaretModel.addCaretNoStack(new CaretRange(line, col, line, col));
        markViewportDirty.run();
    }

    private void startBoxSelection(int line, int col) {
        boxSelectionActive = true;
        boxAnchorLine = line;
        boxAnchorCol = col;
        multiCaretModel.clearSecondaryCarets();
        selectionModel.moveCaret(line, col);
        markViewportDirty.run();
    }

    private void updateBoxSelection(int line, int col) {
        int minLine = Math.min(boxAnchorLine, line);
        int maxLine = Math.max(boxAnchorLine, line);
        int minCol = Math.min(boxAnchorCol, col);
        int maxCol = Math.max(boxAnchorCol, col);

        int firstLineLen = document.getLineText(minLine).length();
        int firstStart = Math.min(minCol, firstLineLen);
        int firstEnd = Math.min(maxCol, firstLineLen);
        selectionModel.moveCaret(minLine, firstStart);
        if (firstStart != firstEnd) {
            selectionModel.moveCaretWithSelection(minLine, firstEnd);
        }

        List<CaretRange> secondaries = new ArrayList<>();
        for (int i = minLine + 1; i <= maxLine; i++) {
            int lineLen = document.getLineText(i).length();
            int start = Math.min(minCol, lineLen);
            int end = Math.min(maxCol, lineLen);
            secondaries.add(new CaretRange(i, start, i, end));
        }
        multiCaretModel.setSecondaryCarets(secondaries);
        markViewportDirty.run();
    }
}
