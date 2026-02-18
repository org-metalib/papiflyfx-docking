package org.metalib.papifly.fx.docks.core;

import javafx.scene.control.Label;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.docking.api.DisposableContent;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockLeafTest {

    @Test
    void disposeCallsDisposableContent() {
        var disposable = new DisposableLabel();
        DockLeaf leaf = new DockLeaf();
        leaf.content(disposable);

        leaf.dispose();

        assertTrue(disposable.disposed, "DisposableContent.dispose() should have been called");
        assertNull(leaf.getContent(), "Content should be null after dispose");
    }

    @Test
    void disposeHandlesNonDisposableContent() {
        DockLeaf leaf = new DockLeaf();
        leaf.content(new Label("plain"));
        leaf.dispose();
        assertNull(leaf.getContent());
    }

    @Test
    void disposeHandlesNullContent() {
        DockLeaf leaf = new DockLeaf();
        leaf.dispose(); // should not throw
        assertNull(leaf.getContent());
    }

    private static class DisposableLabel extends Label implements DisposableContent {
        boolean disposed;

        @Override
        public void dispose() {
            disposed = true;
        }
    }
}
