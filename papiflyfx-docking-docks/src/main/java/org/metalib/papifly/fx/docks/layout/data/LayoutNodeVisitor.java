package org.metalib.papifly.fx.docks.layout.data;

/**
 * Visitor for layout DTO nodes.
 *
 * @param <T> visitor result type
 */
public interface LayoutNodeVisitor<T> {

    T visitLeaf(LeafData leaf);

    T visitSplit(SplitData split);

    T visitTabGroup(TabGroupData tabGroup);
}
