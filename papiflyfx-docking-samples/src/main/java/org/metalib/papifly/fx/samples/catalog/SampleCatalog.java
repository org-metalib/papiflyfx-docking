package org.metalib.papifly.fx.samples.catalog;

import org.metalib.papifly.fx.samples.SampleScene;
import org.metalib.papifly.fx.samples.code.JavaEditorSample;
import org.metalib.papifly.fx.samples.code.JavaScriptEditorSample;
import org.metalib.papifly.fx.samples.code.JsonEditorSample;
import org.metalib.papifly.fx.samples.code.MarkdownEditorSample;
import org.metalib.papifly.fx.samples.docks.BasicSplitSample;
import org.metalib.papifly.fx.samples.docks.FloatingSample;
import org.metalib.papifly.fx.samples.docks.MinimizeSample;
import org.metalib.papifly.fx.samples.docks.NestedSplitSample;
import org.metalib.papifly.fx.samples.docks.PersistSample;
import org.metalib.papifly.fx.samples.docks.TabGroupSample;
import org.metalib.papifly.fx.samples.tree.TreeViewNodeInfoSample;
import org.metalib.papifly.fx.samples.tree.TreeViewSample;

import java.util.List;

/**
 * Static registry of all available samples in display order.
 */
public final class SampleCatalog {

    private SampleCatalog() {}

    /**
     * Returns all samples in catalog display order.
     * Categories: Docks (6 entries), Code (4 entries).
     *
     * @return ordered sample scene list
     */
    public static List<SampleScene> all() {
        return List.of(
            new BasicSplitSample(),
            new NestedSplitSample(),
            new TabGroupSample(),
            new FloatingSample(),
            new MinimizeSample(),
            new PersistSample(),
            new MarkdownEditorSample(),
            new JavaEditorSample(),
            new JavaScriptEditorSample(),
            new JsonEditorSample(),
            new TreeViewSample(),
            new TreeViewNodeInfoSample()
        );
    }
}
