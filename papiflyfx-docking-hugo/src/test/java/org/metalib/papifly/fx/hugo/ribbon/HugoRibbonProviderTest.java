package org.metalib.papifly.fx.hugo.ribbon;

import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.api.ribbon.PapiflyCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonContext;
import org.metalib.papifly.fx.api.ribbon.RibbonContextAttributes;
import org.metalib.papifly.fx.api.ribbon.RibbonControlSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonMenuSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonSplitButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonToggleSpec;
import org.metalib.papifly.fx.hugo.api.HugoPreviewFactory;
import org.metalib.papifly.fx.hugo.api.HugoRibbonActions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HugoRibbonProviderTest {

    @Test
    void mapsHugoCommandsAndRoutesToActiveActions() {
        StubActions actions = new StubActions();
        RibbonContext context = new RibbonContext(
            "dock-hugo",
            "hugo:docs",
            HugoPreviewFactory.FACTORY_ID,
            Map.of(
                RibbonContextAttributes.ACTIVE_CONTENT_NODE, actions,
                RibbonContextAttributes.DOCK_TITLE, "content/posts/welcome.md",
                RibbonContextAttributes.CONTENT_FACTORY_ID, HugoPreviewFactory.FACTORY_ID
            )
        );
        HugoRibbonProvider provider = new HugoRibbonProvider();

        List<RibbonTabSpec> tabs = visibleTabs(provider, context);
        assertEquals(2, tabs.size());
        RibbonTabSpec hugoTab = tabs.stream().filter(tab -> tab.id().equals("hugo")).findFirst().orElseThrow();
        RibbonTabSpec editorTab = tabs.stream().filter(tab -> tab.id().equals("hugo-editor")).findFirst().orElseThrow();

        command(hugoTab, "hugo.ribbon.development.server").execute();
        command(hugoTab, "hugo.ribbon.new-content.post").execute();
        command(hugoTab, "hugo.ribbon.build.site").execute();
        command(hugoTab, "hugo.ribbon.modules.tidy").execute();
        command(hugoTab, "hugo.ribbon.environment.env").execute();
        command(editorTab, "hugo.ribbon.editor.front-matter").execute();
        command(editorTab, "hugo.ribbon.editor.shortcode.youtube").execute();

        assertEquals(1, actions.toggleCount);
        assertEquals(1, actions.newContentCount);
        assertEquals(1, actions.buildCount);
        assertEquals(1, actions.modCount);
        assertEquals(1, actions.envCount);
        assertEquals(1, actions.frontMatterCount);
        assertEquals(1, actions.shortcodeCount);
    }

    @Test
    void contextualEditorTabAppearsForMarkdownContextAndHidesOtherwise() {
        HugoRibbonProvider provider = new HugoRibbonProvider();

        RibbonContext markdownContext = new RibbonContext(
            "dock-md",
            "content/post.md",
            "sample.markdown",
            Map.of(RibbonContextAttributes.DOCK_TITLE, "content/post.md")
        );
        List<RibbonTabSpec> markdownTabs = visibleTabs(provider, markdownContext);
        assertTrue(markdownTabs.stream().anyMatch(tab -> tab.id().equals("hugo-editor")));
        RibbonTabSpec editorTab = markdownTabs.stream().filter(tab -> tab.id().equals("hugo-editor")).findFirst().orElseThrow();
        assertFalse(command(editorTab, "hugo.ribbon.editor.front-matter").enabledProperty().get());

        RibbonContext javaContext = new RibbonContext(
            "dock-java",
            "src/App.java",
            "sample.code",
            Map.of(RibbonContextAttributes.DOCK_TITLE, "App.java")
        );
        List<RibbonTabSpec> javaTabs = visibleTabs(provider, javaContext);
        assertFalse(javaTabs.stream().anyMatch(tab -> tab.id().equals("hugo-editor")));
    }

    private static List<RibbonTabSpec> visibleTabs(HugoRibbonProvider provider, RibbonContext context) {
        return provider.getTabs(context).stream()
            .filter(tab -> tab.isVisible(context))
            .toList();
    }

    private static PapiflyCommand command(RibbonTabSpec tab, String id) {
        return commands(tab).stream()
            .filter(command -> command.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing command: " + id));
    }

    private static List<PapiflyCommand> commands(RibbonTabSpec tab) {
        List<PapiflyCommand> commands = new ArrayList<>();
        for (RibbonGroupSpec group : tab.groups()) {
            for (RibbonControlSpec control : group.controls()) {
                switch (control) {
                    case RibbonButtonSpec button -> commands.add(button.command());
                    case RibbonToggleSpec toggle -> commands.add(toggle.command());
                    case RibbonSplitButtonSpec splitButton -> {
                        commands.add(splitButton.primaryCommand());
                        commands.addAll(splitButton.secondaryCommands());
                    }
                    case RibbonMenuSpec menu -> commands.addAll(menu.items());
                }
            }
        }
        return commands;
    }

    private static final class StubActions extends StackPane implements HugoRibbonActions {

        private int toggleCount;
        private int newContentCount;
        private int buildCount;
        private int modCount;
        private int envCount;
        private int frontMatterCount;
        private int shortcodeCount;

        @Override
        public boolean isServerRunning() {
            return false;
        }

        @Override
        public boolean canRunHugoCommands() {
            return true;
        }

        @Override
        public void toggleServer() {
            toggleCount++;
        }

        @Override
        public void newContent(String relativePath) {
            newContentCount++;
        }

        @Override
        public void build() {
            buildCount++;
        }

        @Override
        public void mod(String subCommand) {
            modCount++;
        }

        @Override
        public void env() {
            envCount++;
        }

        @Override
        public void frontMatterTemplate() {
            frontMatterCount++;
        }

        @Override
        public void insertShortcode(String shortcodeName) {
            shortcodeCount++;
        }
    }
}
