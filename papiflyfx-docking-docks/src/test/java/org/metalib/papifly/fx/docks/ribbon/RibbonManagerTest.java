package org.metalib.papifly.fx.docks.ribbon;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.api.ribbon.MutableRibbonBooleanState;
import org.metalib.papifly.fx.api.ribbon.RibbonBooleanState;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonAttributeKey;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonContext;
import org.metalib.papifly.fx.api.ribbon.RibbonContextAttributes;
import org.metalib.papifly.fx.api.ribbon.RibbonControlKind;
import org.metalib.papifly.fx.api.ribbon.RibbonControlSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonMenuSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonProvider;
import org.metalib.papifly.fx.api.ribbon.RibbonSplitButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonToggleCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonToggleSpec;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.metalib.papifly.fx.docks.ribbon.RibbonTestSupport.provider;
import static org.metalib.papifly.fx.docks.ribbon.RibbonTestSupport.simpleButtonTab;

class RibbonManagerTest {

    @Test
    void ribbonContext_typedAttributesShareLegacyRawStringStorage() {
        RibbonAttributeKey<String> ownerKey = RibbonAttributeKey.of("test.ribbon.owner", String.class);
        RibbonContext context = new RibbonContext(
            "dock",
            "content",
            "type",
            Map.of(RibbonContextAttributes.DOCK_TITLE, "Draft.md")
        )
            .withAttribute(RibbonContextAttributes.CONTENT_KIND_KEY, "markdown")
            .withAttribute(ownerKey, "feature");

        assertEquals("Draft.md", context.attribute(RibbonContextAttributes.DOCK_TITLE_KEY).orElseThrow());
        assertEquals("markdown", context.attribute(RibbonContextAttributes.CONTENT_KIND, String.class).orElseThrow());
        assertEquals("feature", context.attribute(ownerKey.id(), String.class).orElseThrow());

        RibbonContext removed = context.withAttribute(RibbonContextAttributes.CONTENT_KIND_KEY, null);

        assertFalse(removed.attribute(RibbonContextAttributes.CONTENT_KIND_KEY).isPresent());
        assertFalse(removed.attribute(RibbonContextAttributes.CONTENT_KIND).isPresent());
    }

    @Test
    void mergesSharedTabsAndContextualVisibility() {
        RibbonProvider homeProvider = provider("home-provider", 0, context -> List.of(
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
                    List.of(new RibbonButtonSpec(RibbonCommand.of("copy", "Copy", () -> {})))
                ))
            )
        ));

        RibbonProvider viewProvider = provider("view-provider", 10, context -> List.of(
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
                    List.of(new RibbonButtonSpec(RibbonCommand.of("float", "Float", () -> {})))
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
                    List.of(new RibbonButtonSpec(RibbonCommand.of("preview", "Preview", () -> {})))
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
        RibbonCommand save = RibbonCommand.of("save", "Save", () -> {
        });
        RibbonCommand preview = RibbonCommand.of("preview", "Preview", () -> {
        });
        RibbonCommand publish = RibbonCommand.of("publish", "Publish", () -> {
        });
        RibbonCommand legacy = RibbonCommand.of("legacy", "Legacy", () -> {
        });

        RibbonProvider provider = provider("provider", context -> List.of(
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
        List<String> resolved = manager.resolveCommandsById(ids).stream().map(RibbonCommand::id).toList();

        assertEquals(List.of("preview", "legacy", "publish"), resolved);
    }

    @Test
    void commandRegistry_canonicalizesAcrossRefreshCycles() {
        RibbonProvider provider = provider("provider", context -> List.of(
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
                    List.of(new RibbonButtonSpec(RibbonCommand.of("save", "Save", () -> {
                    })))
                ))
            )
        ));

        RibbonManager manager = new RibbonManager(List.of(provider));
        RibbonCommand firstResolved = manager.getCommandRegistry().find("save").orElseThrow();
        RibbonCommand firstRendered = extractFirstButtonCommand(manager);
        assertSame(firstResolved, firstRendered);

        // Force a refresh via context change — provider will emit a brand-new
        // RibbonCommand instance, but the registry must canonicalize it.
        manager.setContext(new RibbonContext("dock", "content", "key", Map.of()));

        RibbonCommand secondResolved = manager.getCommandRegistry().find("save").orElseThrow();
        RibbonCommand secondRendered = extractFirstButtonCommand(manager);
        assertSame(firstResolved, secondResolved);
        assertSame(firstResolved, secondRendered);
    }

    @Test
    void commandRegistry_refreshesProviderComputedCommandStateAcrossContexts() {
        RibbonProvider provider = provider("provider", context -> {
            boolean enabled = context.activeContentTypeKeyOptional()
                .map("ready"::equals)
                .orElse(false);
            return List.of(new RibbonTabSpec(
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
                    List.of(new RibbonButtonSpec(RibbonCommand.of(
                        "refresh-state",
                        "Refresh State",
                        "Refresh State",
                        null,
                        null,
                        RibbonBooleanState.mutable(enabled),
                        () -> {
                        }
                    )))
                ))
            ));
        });

        RibbonManager manager = new RibbonManager(List.of(provider));
        RibbonCommand command = manager.getCommandRegistry().find("refresh-state").orElseThrow();
        assertFalse(command.enabled().get());

        manager.setContext(new RibbonContext(null, null, "ready", Map.of()));

        assertSame(command, manager.getCommandRegistry().find("refresh-state").orElseThrow());
        assertTrue(command.enabled().get());
    }

    @Test
    void commandRegistry_prunesCommandsNoLongerReachable() {
        RibbonProvider provider = provider("provider", context -> {
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
                        List.of(new RibbonButtonSpec(RibbonCommand.of("save", "Save", () -> {
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
                        List.of(new RibbonButtonSpec(RibbonCommand.of("save", "Save", () -> {
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
                        List.of(new RibbonButtonSpec(RibbonCommand.of("publish", "Publish", () -> {
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
        RibbonCommand save = RibbonCommand.of("save", "Save", () -> {
        });
        RibbonCommand preview = RibbonCommand.of("preview", "Preview", () -> {
        });

        RibbonProvider provider = provider("provider", context -> List.of(
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
            manager.getQuickAccessCommands().stream().map(RibbonCommand::id).toList());
    }

    @Test
    void mergesSingleContributionWithoutDoubleCountingControls() {
        RibbonProvider provider = provider("provider", context -> List.of(
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
                    List.of(new RibbonButtonSpec(RibbonCommand.of("save", "Save", () -> {
                    })))
                ))
            )
        ));

        RibbonManager manager = new RibbonManager(List.of(provider));

        assertEquals(1, manager.getTabs().size());
        assertEquals(1, manager.getTabs().getFirst().groups().size());
        assertEquals(1, manager.getTabs().getFirst().groups().getFirst().controls().size());
    }

    @Test
    void providerFailureEmitsTelemetryAndKeepsHealthyProviderTabs() {
        RibbonProvider broken = provider("broken-provider", context -> {
            throw new IllegalStateException("boom");
        });
        RibbonProvider healthy = provider("healthy-provider", 10, context -> List.of(simpleButtonTab(
            "home",
            "Home",
            "save",
            "Save"
        )));
        RibbonLayoutTelemetryRecorder telemetry = new RibbonLayoutTelemetryRecorder();
        RibbonManager manager = new RibbonManager(List.of());
        manager.setLayoutTelemetry(telemetry);

        manager.getProviders().setAll(broken, healthy);

        assertEquals(List.of("home"), manager.getTabs().stream().map(RibbonTabSpec::id).toList());
        assertEquals(1, telemetry.providerFailures().size());
        assertEquals("broken-provider", telemetry.providerFailures().getFirst().providerId());
        assertTrue(telemetry.providerFailures().getFirst().exception() instanceof IllegalStateException);
    }

    @Test
    void duplicateTabIdsEmitFirstWinsTelemetry() {
        RibbonProvider first = provider("first", context -> List.of(simpleButtonTab(
            "home",
            "Home",
            "save",
            "Save"
        )));
        RibbonProvider second = provider("second", 10, context -> List.of(simpleButtonTab(
            "home",
            "Start",
            "open",
            "Open"
        )));
        RibbonLayoutTelemetryRecorder telemetry = new RibbonLayoutTelemetryRecorder();
        RibbonManager manager = new RibbonManager(List.of());
        manager.setLayoutTelemetry(telemetry);

        manager.getProviders().setAll(first, second);

        assertEquals("Home", manager.getTabs().getFirst().label());
        assertEquals(1, telemetry.tabIdCollisions().size());
        RibbonLayoutTelemetryRecorder.TabIdCollisionEvent collision = telemetry.tabIdCollisions().getFirst();
        assertEquals("home", collision.tabId());
        assertEquals("Home", collision.retainedLabel());
        assertEquals("Start", collision.ignoredLabel());
    }

    @Test
    void duplicateCommandIdsEmitFirstWinsTelemetry() {
        RibbonProvider provider = provider("provider", context -> List.of(new RibbonTabSpec(
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
                    new RibbonButtonSpec(RibbonCommand.of("duplicate", "First", () -> {
                    })),
                    new RibbonButtonSpec(RibbonCommand.of("duplicate", "Second", () -> {
                    }))
                )
            ))
        )));
        RibbonLayoutTelemetryRecorder telemetry = new RibbonLayoutTelemetryRecorder();
        RibbonManager manager = new RibbonManager(List.of());
        manager.setLayoutTelemetry(telemetry);

        manager.getProviders().setAll(provider);

        assertEquals("First", manager.getCommandRegistry().find("duplicate").orElseThrow().label());
        assertEquals(1, telemetry.commandIdCollisions().size());
        RibbonLayoutTelemetryRecorder.CommandIdCollisionEvent collision = telemetry.commandIdCollisions().getFirst();
        assertEquals("duplicate", collision.commandId());
        assertEquals("First", collision.retainedLabel());
        assertEquals("Second", collision.ignoredLabel());
    }

    @Test
    void incompatibleCommandKindsAreDiagnosedAndSkipped() {
        RibbonProvider provider = provider("mixed-kinds", 0, context -> List.of(new RibbonTabSpec(
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
                    new RibbonButtonSpec(RibbonCommand.of("same", "Action", () -> {})),
                    new RibbonToggleSpec(RibbonToggleCommand.of("same", "Toggle", RibbonBooleanState.mutable(false), () -> {}))
                )
            ))
        )));
        RibbonLayoutTelemetryRecorder telemetry = new RibbonLayoutTelemetryRecorder();
        RibbonManager manager = new RibbonManager(List.of());
        manager.setLayoutTelemetry(telemetry);

        manager.getProviders().setAll(provider);

        assertEquals(1, manager.getTabs().getFirst().groups().getFirst().controls().size());
        assertEquals(1, telemetry.incompatibleCommandKinds().size());
        assertEquals("same", telemetry.incompatibleCommandKinds().getFirst().commandId());
    }

    @Test
    void unknownControlKindsAreDiagnosedAndSkipped() {
        RibbonControlSpec unknown = new RibbonControlSpec() {
            @Override
            public String id() {
                return "unknown-control";
            }

            @Override
            public RibbonControlKind kind() {
                return RibbonControlKind.UNKNOWN;
            }
        };
        RibbonProvider provider = provider("unknown-control-provider", 0, context -> List.of(new RibbonTabSpec(
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
                List.of(unknown)
            ))
        )));
        RibbonLayoutTelemetryRecorder telemetry = new RibbonLayoutTelemetryRecorder();
        RibbonManager manager = new RibbonManager(List.of());
        manager.setLayoutTelemetry(telemetry);

        manager.getProviders().setAll(provider);

        assertTrue(manager.getTabs().isEmpty());
        assertEquals(1, telemetry.unknownControlKinds().size());
        assertEquals("unknown-control", telemetry.unknownControlKinds().getFirst().controlId());
    }

    private static RibbonCommand extractFirstButtonCommand(RibbonManager manager) {
        RibbonControlSpec control = manager.getTabs().getFirst().groups().getFirst().controls().getFirst();
        return ((RibbonButtonSpec) control).command();
    }

}
