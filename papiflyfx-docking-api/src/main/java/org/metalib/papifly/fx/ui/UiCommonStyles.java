package org.metalib.papifly.fx.ui;

import javafx.scene.Parent;

import java.net.URL;

/**
 * Stylesheet loader for shared lightweight UI controls.
 */
public final class UiCommonStyles {

    public static final String COMMON_STYLESHEET = "/org/metalib/papifly/fx/ui/ui-common.css";

    private UiCommonStyles() {
    }

    public static void ensureLoaded(Parent parent) {
        URL stylesheetUrl = UiCommonStyles.class.getResource(COMMON_STYLESHEET);
        if (stylesheetUrl == null) {
            return;
        }
        String stylesheet = stylesheetUrl.toExternalForm();
        if (!parent.getStylesheets().contains(stylesheet)) {
            parent.getStylesheets().add(stylesheet);
        }
    }
}
