package org.metalib.papifly.fx.docks.ribbon;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.api.ribbon.PapiflyCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonContext;
import org.metalib.papifly.fx.api.ribbon.RibbonControlSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonMenuSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonProvider;
import org.metalib.papifly.fx.api.ribbon.RibbonSplitButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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
        manager.addQuickAccessCommand(legacy);

        List<String> ids = List.of("preview", "legacy", "missing", "preview", "publish");
        List<String> resolved = manager.resolveCommandsById(ids).stream().map(PapiflyCommand::id).toList();

        assertEquals(List.of("preview", "legacy", "publish"), resolved);
    }

    @Test
    void commandRegistry_canonicalizesAcrossRefreshCycles() {
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
                    List.of(new RibbonButtonSpec(PapiflyCommand.of("save", "Save", () -> {
                    })))
                ))
            )
        ));

        RibbonManager manager = new RibbonManager(List.of(provider));
        PapiflyCommand firstResolved = manager.getCommandRegistry().find("save").orElseThrow();
        PapiflyCommand firstRendered = extractFirstButtonCommand(manager);
        assertSame(firstResolved, firstRendered);

        // Force a refresh via context change — provider will emit a brand-new
        // PapiflyCommand instance, but the registry must canonicalize it.
        manager.setContext(new RibbonContext("dock", "content", "key", Map.of()));

        PapiflyCommand secondResolved = manager.getCommandRegistry().find("save").orElseThrow();
        PapiflyCommand secondRendered = extractFirstButtonCommand(manager);
        assertSame(firstResolved, secondResolved);
        assertSame(firstResolved, secondRendered);
    }

    @Test
    void commandRegistry_prunesCommandsNoLongerReachable() {
        RibbonProvider provider = new TestProvider("provider", 0, context -> {
            boolean includeContextualTab = context.activeContentTypeKeyOptional()
                .map("markdown"::equals)
                .orElse(false);
            if (!includeContextualTab) {
                return List.of(new RibbonTabSpec(
                    "home",
                    "Home",
                    0,
                    false,
                    ribbonContext -> true,
                    List.of(new RibbonGroupSpec(
                        "home-actions",
                        "Home Actions",
                        0,
                        0,
                        null,
                        List.of(new RibbonButtonSpec(PapiflyCommand.of("save", "Save", () -> {
                        })))
                    ))
                ));
            }
            return List.of(
                new RibbonTabSpec(
                    "home",
                    "Home",
                    0,
                    false,
                    ribbonContext -> true,
                    List.of(new RibbonGroupSpec(
                        "home-actions",
                        "Home Actions",
                        0,
                        0,
                        null,
                        List.of(new RibbonButtonSpec(PapiflyCommand.of("save", "Save", () -> {
                        })))
                    ))
                ),
                new RibbonTabSpec(
                    "markdown",
                    "Markdown",
                    10,
                    true,
                    ribbonContext -> true,
                    List.of(new RibbonGroupSpec(
                        "markdown-actions",
                        "Markdown Actions",
                        0,
                        0,
                        null,
                        List.of(new RibbonButtonSpec(PapiflyCommand.of("publish", "Publish", () -> {
                        })))
                    ))
                )
            );
        });

        RibbonManager manager = new RibbonManager(List.of(provider));
        manager.setContext(new RibbonContext(null, null, "markdown", Map.of()));
        assertTrue(manager.getCommandRegistry().contains("publish"));

        // Pin the contextual command to QAT; it should survive a context change
        // that hides the owning tab.
        manager.getQuickAccessCommandIds().setAll("publish");
        manager.setContext(new RibbonContext(null, null, "code", Map.of()));
        assertTrue(manager.getCommandRegistry().contains("publish"));

        // After unpinning, the command becomes unreachable and is evicted by the
        // next refresh cycle.
        manager.getQuickAccessCommandIds().clear();
        manager.setContext(new RibbonContext(null, null, "code", Map.of()));
        manager.refresh();
        assertFalse(manager.getCommandRegistry().contains("publish"));
    }

    @Test
    void quickAccessCommands_isDerivedFromIdsAndToleratesMissingIds() {
        PapiflyCommand save = PapiflyCommand.of("save", "Save", () -> {
        });
        PapiflyCommand preview = PapiflyCommand.of("preview", "Preview", () -> {
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
                    List.of(new RibbonButtonSpec(save), new RibbonButtonSpec(preview))
                ))
            )
        ));

        RibbonManager manager = new RibbonManager(List.of(provider));
        manager.getQuickAccessCommandIds().setAll("save", "missing", "preview", "save");

        // Identifier list preserves duplicates; derived view deduplicates and
        // drops unresolved entries.
        assertEquals(List.of("save", "missing", "preview", "save"),
            List.copyOf(manager.getQuickAccessCommandIds()));
        assertEquals(List.of("save", "preview"),
            manager.getQuickAccessCommands().stream().map(PapiflyCommand::id).toList());
    }

    @Test
    void mergesSingleContributionWithoutDoubleCountingControls() {
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
                    List.of(new RibbonButtonSpec(PapiflyCommand.of("save", "Save", () -> {
                    })))
                ))
            )
        ));

        RibbonManager manager = new RibbonManager(List.of(provider));

        assertEquals(1, manager.getTabs().size());
        assertEquals(1, manager.getTabs().getFirst().groups().size());
        assertEquals(1, manager.getTabs().getFirst().groups().getFirst().controls().size());
    }

    private static PapiflyCommand extractFirstButtonCommand(RibbonManager manager) {
        RibbonControlSpec control = manager.getTabs().getFirst().groups().getFirst().controls().getFirst();
        return ((RibbonButtonSpec) control).command();
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
