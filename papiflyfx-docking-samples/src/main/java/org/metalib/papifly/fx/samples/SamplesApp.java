package org.metalib.papifly.fx.samples;

import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.samples.catalog.SampleCatalog;

import java.util.ArrayList;
import java.util.List;

/**
 * Main application shell for the PapiflyFX Docking Samples.
 *
 * <p>Layout: top bar (title + theme toggle) | left ListView (catalog) | center content area.</p>
 */
public class SamplesApp extends Application {

    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>(Theme.dark());
    private Stage primaryStage;
    private final StackPane contentArea = new StackPane();

    /**
     * Creates the samples application.
     */
    public SamplesApp() {
        // Default constructor.
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        HBox topBar = buildTopBar();
        ListView<Object> sampleList = buildSampleList();
        buildContentArea();

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setLeft(sampleList);
        root.setCenter(contentArea);

        stage.setTitle("PapiflyFX Docking Samples");
        stage.setScene(new Scene(root, 1200, 800));
        stage.show();
    }

    private HBox buildTopBar() {
        Label titleLabel = new Label("PapiflyFX Docking Samples");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        ToggleButton themeToggle = new ToggleButton("Light Mode");
        themeToggle.setStyle("-fx-background-color: #0e639c; -fx-text-fill: white;");
        themeToggle.setSelected(false);
        themeToggle.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            themeProperty.set(isSelected ? Theme.light() : Theme.dark());
            themeToggle.setText(isSelected ? "Dark Mode" : "Light Mode");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(8, titleLabel, spacer, themeToggle);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(8, 12, 8, 12));
        topBar.setStyle("-fx-background-color: #3c3c3c;");
        return topBar;
    }

    private ListView<Object> buildSampleList() {
        List<Object> items = new ArrayList<>();
        String currentCategory = null;
        for (SampleScene sample : SampleCatalog.all()) {
            if (!sample.category().equals(currentCategory)) {
                currentCategory = sample.category();
                items.add(currentCategory);
            }
            items.add(sample);
        }

        ListView<Object> listView = new ListView<>();
        listView.getItems().setAll(items);
        listView.setPrefWidth(190);
        listView.setMinWidth(140);
        listView.setStyle("-fx-background-color: #252526;");

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    setDisable(false);
                } else if (item instanceof String) {
                    setText((String) item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #888888; -fx-background-color: #1e1e1e; -fx-padding: 6 8 6 8;");
                    setDisable(true);
                } else if (item instanceof SampleScene sample) {
                    setText("  " + sample.title());
                    setStyle("-fx-text-fill: #cccccc; -fx-background-color: #252526;");
                    setDisable(false);
                }
            }
        });

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal instanceof SampleScene sample) {
                Node content = sample.build(primaryStage, themeProperty);
                contentArea.getChildren().setAll(content);
            }
        });

        return listView;
    }

    private void buildContentArea() {
        Label placeholder = new Label("Select a sample from the list");
        placeholder.setStyle("-fx-text-fill: #888888; -fx-font-size: 14px;");
        contentArea.getChildren().add(placeholder);
        contentArea.setStyle("-fx-background-color: #1e1e1e;");
    }

    /**
     * Launches the samples application.
     *
     * @param args launcher arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
