package org.metalib.papifly.fx.docks.ribbon;

import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.api.ribbon.PapiflyCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonProvider;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;
import org.metalib.papifly.fx.docks.testutil.FxTestUtil;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class RibbonAdaptiveLayoutFxTest {

    private final AtomicInteger alphaExecutions = new AtomicInteger();

    private StackPane host;
    private Ribbon ribbon;

    @Start
    private void start(Stage stage) {
        RibbonManager manager = new RibbonManager(List.of(new TestRibbonProvider()));
        ribbon = new Ribbon(manager);
        host = new StackPane(ribbon);
        host.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        host.setPrefSize(1200, 280);
        host.setMinSize(0, 0);
        ribbon.setMaxWidth(Double.MAX_VALUE);
        stage.setScene(new Scene(host, 1200, 280));
        stage.show();
        settleFx();
    }

    @Test
    void collapsedGroupPopupKeepsCommandsReachable(FxRobot robot) {
        shrinkUntil(() -> group("alpha").getSizeMode() == RibbonGroupSizeMode.COLLAPSED, 560.0, 520.0, 480.0, 440.0, 400.0, 360.0, 320.0, 280.0);

        assertEquals(RibbonGroupSizeMode.COLLAPSED, group("alpha").getSizeMode());
        assertPriorityOrder();

        robot.clickOn("#pf-ribbon-group-collapsed-alpha");
        settleFx();

        assertFalse(robot.lookup(".pf-ribbon-collapsed-popup").queryAll().isEmpty());

        ButtonBase popupButton = robot.lookup(node ->
            node instanceof ButtonBase button
                && "Alpha One".equals(button.getText())
                && button.isVisible()
        ).queryAs(ButtonBase.class);

        robot.clickOn(popupButton);
        settleFx();

        assertEquals(1, alphaExecutions.get());
    }

    private void assertPriorityOrder() {
        RibbonGroup alpha = group("alpha");
        RibbonGroup beta = group("beta");
        RibbonGroup gamma = group("gamma");

        assertTrue(alpha.getSizeMode().compareTo(beta.getSizeMode()) >= 0);
        assertTrue(beta.getSizeMode().compareTo(gamma.getSizeMode()) >= 0);
    }

    private RibbonGroup group(String id) {
        return FxTestUtil.callFx(() -> ribbon.getRenderedGroups().stream()
            .filter(group -> group.getSpec().id().equals(id))
            .findFirst()
            .orElseThrow());
    }

    private void resizeTo(double width) {
        FxTestUtil.runFx(() -> {
            host.setPrefWidth(width);
            host.setMaxWidth(width);
            ribbon.setPrefWidth(width);
            ribbon.setMaxWidth(width);
            host.applyCss();
            host.layout();
        });
        settleFx();
        FxTestUtil.runFx(() -> {
            host.applyCss();
            host.layout();
        });
        settleFx();
    }

    private void settleFx() {
        FxTestUtil.waitForFxEvents();
        FxTestUtil.waitForFxEvents();
    }

    private void shrinkUntil(BooleanSupplier condition, double... widths) {
        for (double width : widths) {
            resizeTo(width);
            if (condition.getAsBoolean()) {
                return;
            }
        }
        assertTrue(condition.getAsBoolean(), "Ribbon did not reach the expected adaptive state");
    }

    private final class TestRibbonProvider implements RibbonProvider {

        @Override
        public String id() {
            return "test-ribbon-provider";
        }

        @Override
        public List<RibbonTabSpec> getTabs(org.metalib.papifly.fx.api.ribbon.RibbonContext context) {
            return List.of(new RibbonTabSpec(
                "home",
                "Home",
                0,
                false,
                ribbonContext -> true,
                List.of(
                    new RibbonGroupSpec(
                        "alpha",
                        "Alpha",
                        0,
                        0,
                        null,
                        List.of(
                            new RibbonButtonSpec(new PapiflyCommand("alpha-1", "Alpha One", "Alpha One", null, null, null, null, alphaExecutions::incrementAndGet)),
                            new RibbonButtonSpec(PapiflyCommand.of("alpha-2", "Alpha Two", () -> {})),
                            new RibbonButtonSpec(PapiflyCommand.of("alpha-3", "Alpha Three", () -> {}))
                        )
                    ),
                    new RibbonGroupSpec(
                        "beta",
                        "Beta",
                        10,
                        10,
                        null,
                        List.of(
                            new RibbonButtonSpec(PapiflyCommand.of("beta-1", "Beta One", () -> {})),
                            new RibbonButtonSpec(PapiflyCommand.of("beta-2", "Beta Two", () -> {})),
                            new RibbonButtonSpec(PapiflyCommand.of("beta-3", "Beta Three", () -> {}))
                        )
                    ),
                    new RibbonGroupSpec(
                        "gamma",
                        "Gamma",
                        20,
                        20,
                        null,
                        List.of(
                            new RibbonButtonSpec(PapiflyCommand.of("gamma-1", "Gamma One", () -> {})),
                            new RibbonButtonSpec(PapiflyCommand.of("gamma-2", "Gamma Two", () -> {})),
                            new RibbonButtonSpec(PapiflyCommand.of("gamma-3", "Gamma Three", () -> {}))
                        )
                    )
                )
            ));
        }
    }
}
