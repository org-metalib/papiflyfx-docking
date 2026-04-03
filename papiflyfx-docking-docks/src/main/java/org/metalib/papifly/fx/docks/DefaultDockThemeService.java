package org.metalib.papifly.fx.docks;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.docking.api.Theme;

final class DefaultDockThemeService implements DockThemeService {

    private final ObjectProperty<Theme> themeProperty;
    private final StackPane dockingLayer;

    DefaultDockThemeService(Theme initialTheme, StackPane dockingLayer) {
        this.themeProperty = new SimpleObjectProperty<>(initialTheme);
        this.dockingLayer = dockingLayer;
        applyTheme(initialTheme);
        this.themeProperty.addListener((obs, oldTheme, newTheme) -> applyTheme(newTheme));
    }

    @Override
    public ObjectProperty<Theme> themeProperty() {
        return themeProperty;
    }

    @Override
    public void dispose() {
        if (themeProperty.isBound()) {
            themeProperty.unbind();
        }
    }

    private void applyTheme(Theme theme) {
        if (theme == null) {
            return;
        }
        dockingLayer.setStyle("-fx-background-color: " + toHexString(theme.background()) + ";");
    }

    private String toHexString(Paint paint) {
        if (paint instanceof Color color) {
            return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
        }
        return "#1E1E1E";
    }
}
