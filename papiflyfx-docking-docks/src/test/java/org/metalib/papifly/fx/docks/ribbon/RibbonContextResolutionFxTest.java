package org.metalib.papifly.fx.docks.ribbon;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.api.ribbon.PapiflyCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonContext;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonProvider;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.metalib.papifly.fx.docks.ribbon.RibbonTestSupport.settleFx;

@ExtendWith(ApplicationExtension.class)
class RibbonContextResolutionFxTest {

    private static final String HUGO_PREVIEW_TYPE = "hugo-preview";

    private DockManager dockManager;
    private Ribbon ribbon;

    @Start
    void start(Stage stage) {
        dockManager = new DockManager();

        DockTabGroup editorGroup = dockManager.createTabGroup();
        DockLeaf editorLeaf = dockManager.createLeaf("Landing.java", content("editor-content", "Editor"));
        editorLeaf.setContentFactoryId("sample.code");
        editorLeaf.setContentData(LeafContentData.of("sample.code", "src/Landing.java", 1));
        editorGroup.addLeaf(editorLeaf);

        DockTabGroup hugoGroup = dockManager.createTabGroup();
        DockLeaf hugoLeaf = dockManager.createLeaf("content/post.md", content("hugo-content", "Hugo"));
        hugoLeaf.setContentFactoryId(HUGO_PREVIEW_TYPE);
        hugoLeaf.setContentData(LeafContentData.of(HUGO_PREVIEW_TYPE, "content/post.md", 1));
        hugoGroup.addLeaf(hugoLeaf);

        dockManager.setRoot(dockManager.createHorizontalSplit(editorGroup, hugoGroup, 0.55));

        RibbonManager ribbonManager = new RibbonManager(List.of(new ContextProvider()));
        ribbon = new Ribbon(ribbonManager);

        RibbonDockHost host = new RibbonDockHost(dockManager, ribbonManager, ribbon);
        stage.setScene(new Scene(host, 1120, 700));
        stage.show();
        settleFx();
    }

    @Test
    void contextualTabAppearsAndDisappearsWhenFocusMovesAcrossDockGroups(FxRobot robot) {
        assertFalse(hasTab(robot, "Hugo Editor"));

        robot.clickOn("#hugo-content");
        settleFx();
        assertEquals(HUGO_PREVIEW_TYPE, dockManager.getRibbonContext().activeContentTypeKey());
        assertTrue(hasTab(robot, "Hugo Editor"));

        robot.clickOn("#editor-content");
        settleFx();
        assertEquals("sample.code", dockManager.getRibbonContext().activeContentTypeKey());
        assertFalse(hasTab(robot, "Hugo Editor"));
    }

    private static StackPane content(String id, String text) {
        Label label = new Label(text);
        StackPane pane = new StackPane(label);
        pane.setId(id);
        pane.setMinSize(0, 0);
        pane.setPrefSize(420, 260);
        return pane;
    }

    private static boolean hasTab(FxRobot robot, String label) {
        return robot.lookup(node ->
            node instanceof ToggleButton toggleButton
                && label.equals(toggleButton.getText())
                && toggleButton.isVisible()
        ).tryQuery().isPresent();
    }

    private static final class ContextProvider implements RibbonProvider {

        @Override
        public String id() {
            return "context-provider";
        }

        @Override
        public List<RibbonTabSpec> getTabs(RibbonContext context) {
            return List.of(
                new RibbonTabSpec(
                    "home",
                    "Home",
                    0,
                    false,
                    ribbonContext -> true,
                    List.of(new RibbonGroupSpec(
                        "clipboard",
                        "Clipboard",
                        0,
                        0,
                        null,
                        List.of(new RibbonButtonSpec(PapiflyCommand.of("copy", "Copy", () -> {})))
                    ))
                ),
                new RibbonTabSpec(
                    "hugo-editor",
                    "Hugo Editor",
                    50,
                    true,
                    ribbonContext -> ribbonContext.activeContentTypeKeyOptional().map(HUGO_PREVIEW_TYPE::equals).orElse(false),
                    List.of(new RibbonGroupSpec(
                        "authoring",
                        "Authoring",
                        0,
                        0,
                        null,
                        List.of(new RibbonButtonSpec(PapiflyCommand.of("front-matter", "Front Matter", () -> {})))
                    ))
                )
            );
        }
    }
}
