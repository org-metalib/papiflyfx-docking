package org.metalib.papifly.fx.code.api;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.metalib.papifly.fx.code.state.EditorStateData;

import java.util.List;

/**
 * Minimal code editor node scaffold.
 * <p>
 * This class currently exposes editable state properties and a placeholder UI.
 * Rendering/editor behavior is added in later phases.
 */
public class CodeEditor extends StackPane {

    private static final String DEFAULT_LANGUAGE = "plain-text";
    private static final String DEFAULT_TITLE = "Code Editor";

    private final StringProperty filePath = new SimpleStringProperty(this, "filePath", "");
    private final IntegerProperty cursorLine = new SimpleIntegerProperty(this, "cursorLine", 0);
    private final IntegerProperty cursorColumn = new SimpleIntegerProperty(this, "cursorColumn", 0);
    private final DoubleProperty verticalScrollOffset = new SimpleDoubleProperty(this, "verticalScrollOffset", 0.0);
    private final StringProperty languageId = new SimpleStringProperty(this, "languageId", DEFAULT_LANGUAGE);

    private List<Integer> foldedLines = List.of();
    private final Label placeholderLabel;

    /**
     * Creates an empty editor scaffold.
     */
    public CodeEditor() {
        placeholderLabel = new Label(DEFAULT_TITLE);
        setMinSize(0, 0);
        setPrefSize(640, 480);
        setAlignment(Pos.CENTER);
        getChildren().add(placeholderLabel);

        filePath.addListener((obs, oldValue, newValue) -> updatePlaceholderText());
    }

    /**
     * Captures current editor state into a serializable DTO.
     */
    public EditorStateData captureState() {
        return new EditorStateData(
            filePath.get(),
            cursorLine.get(),
            cursorColumn.get(),
            verticalScrollOffset.get(),
            languageId.get(),
            foldedLines
        );
    }

    /**
     * Applies state to the editor.
     */
    public void applyState(EditorStateData state) {
        if (state == null) {
            return;
        }
        setFilePath(state.filePath());
        setCursorLine(state.cursorLine());
        setCursorColumn(state.cursorColumn());
        setVerticalScrollOffset(state.verticalScrollOffset());
        setLanguageId(state.languageId());
        setFoldedLines(state.foldedLines());
    }

    /**
     * Gets the file path.
     */
    public String getFilePath() {
        return filePath.get();
    }

    /**
     * Sets the file path.
     */
    public void setFilePath(String filePath) {
        this.filePath.set(filePath == null ? "" : filePath);
    }

    /**
     * File path property.
     */
    public StringProperty filePathProperty() {
        return filePath;
    }

    /**
     * Gets the caret line.
     */
    public int getCursorLine() {
        return cursorLine.get();
    }

    /**
     * Sets the caret line.
     */
    public void setCursorLine(int cursorLine) {
        this.cursorLine.set(Math.max(0, cursorLine));
    }

    /**
     * Caret line property.
     */
    public IntegerProperty cursorLineProperty() {
        return cursorLine;
    }

    /**
     * Gets the caret column.
     */
    public int getCursorColumn() {
        return cursorColumn.get();
    }

    /**
     * Sets the caret column.
     */
    public void setCursorColumn(int cursorColumn) {
        this.cursorColumn.set(Math.max(0, cursorColumn));
    }

    /**
     * Caret column property.
     */
    public IntegerProperty cursorColumnProperty() {
        return cursorColumn;
    }

    /**
     * Gets vertical scroll offset.
     */
    public double getVerticalScrollOffset() {
        return verticalScrollOffset.get();
    }

    /**
     * Sets vertical scroll offset.
     */
    public void setVerticalScrollOffset(double verticalScrollOffset) {
        this.verticalScrollOffset.set(Math.max(0.0, verticalScrollOffset));
    }

    /**
     * Vertical scroll offset property.
     */
    public DoubleProperty verticalScrollOffsetProperty() {
        return verticalScrollOffset;
    }

    /**
     * Gets language identifier.
     */
    public String getLanguageId() {
        return languageId.get();
    }

    /**
     * Sets language identifier.
     */
    public void setLanguageId(String languageId) {
        this.languageId.set(languageId == null || languageId.isBlank() ? DEFAULT_LANGUAGE : languageId);
    }

    /**
     * Language identifier property.
     */
    public StringProperty languageIdProperty() {
        return languageId;
    }

    /**
     * Gets immutable folded lines snapshot.
     */
    public List<Integer> getFoldedLines() {
        return foldedLines;
    }

    /**
     * Sets folded lines snapshot.
     */
    public void setFoldedLines(List<Integer> foldedLines) {
        this.foldedLines = foldedLines == null ? List.of() : List.copyOf(foldedLines);
    }

    private void updatePlaceholderText() {
        String path = getFilePath();
        if (path == null || path.isBlank()) {
            placeholderLabel.setText(DEFAULT_TITLE);
            return;
        }
        placeholderLabel.setText(DEFAULT_TITLE + ": " + path);
    }
}
