package org.metalib.papifly.fx.docks.ribbon;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.api.ribbon.PapiflyCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonContext;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonMenuSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonProvider;
import org.metalib.papifly.fx.api.ribbon.RibbonSplitButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RibbonManagerTest {

    @Test
    void mergesSharedTabsAndContextualVisibility() {
        RibbonProvider homeProvider = new TestProvider("home-provider", 0, context -> List.of(
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
            )
        ));

        RibbonProvider viewProvider = new TestProvider("view-provider", 10, context -> List.of(
            new RibbonTabSpec(
                "home",
                "Home",
                0,
                false,
                ribbonContext -> true,
                List.of(new RibbonGroupSpec(
                    "window",
                    "Window",
                    10,
                    10,
                    null,
                    List.of(new RibbonButtonSpec(PapiflyCommand.of("float", "Float", () -> {})))
                ))
            ),
            new RibbonTabSpec(
                "markdown",
                "Markdown",
                50,
                true,
                ribbonContext -> ribbonContext.activeContentTypeKeyOptional()
                    .map("sample.markdown"::equals)
                    .orElse(false),
                List.of(new RibbonGroupSpec(
                    "authoring",
                    "Authoring",
                    0,
                    0,
                    null,
                    List.of(new RibbonButtonSpec(PapiflyCommand.of("preview", "Preview", () -> {})))
                ))
            )
        ));

        RibbonManager manager = new RibbonManager(List.of(viewProvider, homeProvider));

        assertEquals(1, manager.getTabs().size());
        assertEquals("home", manager.getTabs().getFirst().id());
        assertEquals(2, manager.getTabs().getFirst().groups().size());

        manager.setContext(new RibbonContext("dock-1", "post-1", "sample.markdown", Map.of()));

        assertEquals(2, manager.getTabs().size());
        assertEquals("markdown", manager.getTabs().get(1).id());
        assertTrue(manager.getTabs().get(1).contextual());
    }

    @Test
    void resolveCommandsById_collectsFromTabsAndSkipsMissingIds() {
        PapiflyCommand save = PapiflyCommand.of("save", "Save", () -> {
        });
        PapiflyCommand preview = PapiflyCommand.of("preview", "Preview", () -> {
        });
        PapiflyCommand publish = PapiflyCommand.of("publish", "Publish", () -> {
        });
        PapiflyCommand legacy = PapiflyCommand.of("legacy", "Legacy", () -> {
        });

        RibbonProvider provider = new TestProvider("provider", 0, context -> List.of(
            new RibbonTabSpec(
                "home",
                "Home",
                0,
                false,
                ribbonContext -> true,
                List.of(new RibbonGroupSpec(
                    "actions",
                    "Actions",
                    0,
                    0,
                    null,
                    List.of(
                        new RibbonButtonSpec(save),
                        new RibbonMenuSpec("preview-menu", "Preview Menu", "Preview Menu", null, null, List.of(preview)),
                        new RibbonSplitButtonSpec(preview, List.of(publish))
                    )
                ))
            )
        ));

        RibbonManager manager = new RibbonManager(List.of(provider));
        manager.getQuickAccessCommands().setAll(legacy);

        List<String> ids = List.of("preview", "legacy", "missing", "preview", "publish");
        List<String> resolved = manager.resolveCommandsById(ids).stream().map(PapiflyCommand::id).toList();

        assertEquals(List.of("preview", "legacy", "publish"), resolved);
    }

    private record TestProvider(
        String providerId,
        int providerOrder,
        Function<RibbonContext, List<RibbonTabSpec>> tabsFactory
    ) implements RibbonProvider {

        @Override
        public String id() {
            return providerId;
        }

        @Override
        public int order() {
            return providerOrder;
        }

        @Override
        public List<RibbonTabSpec> getTabs(RibbonContext context) {
            return tabsFactory.apply(context);
        }
    }
}
