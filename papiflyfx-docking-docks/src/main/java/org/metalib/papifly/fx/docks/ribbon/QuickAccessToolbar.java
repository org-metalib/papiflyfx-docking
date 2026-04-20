package org.metalib.papifly.fx.docks.ribbon;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import org.metalib.papifly.fx.api.ribbon.PapiflyCommand;

import java.util.Objects;

/**
 * Compact toolbar that keeps host-selected commands visible independently of
 * the active ribbon tab.
 */
public class QuickAccessToolbar extends HBox {

    private final ObservableList<PapiflyCommand> commands = FXCollections.observableArrayList();
    private ClassLoader classLoader;

    /**
     * Creates an empty Quick Access Toolbar.
     */
    public QuickAccessToolbar() {
        this.classLoader = resolveClassLoader();
        getStyleClass().add("pf-ribbon-qat");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6.0);
        commands.addListener((ListChangeListener<PapiflyCommand>) change -> rebuild());
    }

    /**
     * Returns the mutable command list rendered by the toolbar.
     *
     * @return mutable command list
     */
    public ObservableList<PapiflyCommand> getCommands() {
        return commands;
    }

    /**
     * Sets the class loader used for command icon resolution.
     *
     * @param classLoader icon resolution class loader
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader == null ? resolveClassLoader() : classLoader;
        rebuild();
    }

    private void rebuild() {
        getChildren().setAll(commands.stream()
            .filter(Objects::nonNull)
            .map(command -> (Node) RibbonControlFactory.createQuickAccessButton(command, classLoader))
            .toList());
        boolean empty = commands.isEmpty();
        setManaged(!empty);
        setVisible(!empty);
    }

    private static ClassLoader resolveClassLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader == null ? QuickAccessToolbar.class.getClassLoader() : loader;
    }
}
