package org.metalib.papifly.fx.searchui;

import javafx.scene.layout.VBox;

public abstract class SearchOverlayBase extends VBox {

    protected SearchOverlayBase() {
        setManaged(false);
        setVisible(false);
        setFocusTraversable(false);
    }

    protected final void showOverlay() {
        setManaged(true);
        setVisible(true);
    }

    protected final void hideOverlay() {
        setManaged(false);
        setVisible(false);
    }

    public abstract void open(String initialQuery);

    public abstract void close();
}
