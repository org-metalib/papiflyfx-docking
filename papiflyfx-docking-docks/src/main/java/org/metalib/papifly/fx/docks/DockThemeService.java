package org.metalib.papifly.fx.docks;

import javafx.beans.property.ObjectProperty;
import org.metalib.papifly.fx.docking.api.Theme;

public interface DockThemeService {

    ObjectProperty<Theme> themeProperty();

    default Theme getTheme() {
        return themeProperty().get();
    }

    default void setTheme(Theme theme) {
        themeProperty().set(theme);
    }

    void dispose();
}
