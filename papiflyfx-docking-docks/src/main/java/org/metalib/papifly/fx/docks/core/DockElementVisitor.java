package org.metalib.papifly.fx.docks.core;

/**
 * Visitor for structural dock elements.
 *
 * @param <T> visitor result type
 */
public interface DockElementVisitor<T> {

    T visitTabGroup(DockTabGroup tabGroup);

    T visitSplitGroup(DockSplitGroup splitGroup);
}
