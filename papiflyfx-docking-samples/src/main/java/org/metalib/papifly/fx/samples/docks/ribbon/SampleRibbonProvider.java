package org.metalib.papifly.fx.samples.docks.ribbon;

import javafx.beans.property.SimpleBooleanProperty;
import org.metalib.papifly.fx.api.ribbon.PapiflyCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonContext;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonMenuSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonProvider;
import org.metalib.papifly.fx.api.ribbon.RibbonSplitButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonToggleSpec;

import java.util.List;

/**
 * Sample-local ribbon provider used to exercise ribbon provider discovery in
 * the samples app.
 */
public final class SampleRibbonProvider implements RibbonProvider {

    @Override
    public String id() {
        return "samples.ribbon";
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
                List.of(
                    new RibbonGroupSpec(
                        "clipboard",
                        "Clipboard",
                        0,
                        10,
                        command("clipboard-settings", "Clipboard settings"),
                        List.of(
                            new RibbonButtonSpec(command("paste", "Paste")),
                            new RibbonButtonSpec(command("copy", "Copy")),
                            new RibbonButtonSpec(command("duplicate", "Duplicate"))
                        )
                    ),
                    new RibbonGroupSpec(
                        "layout",
                        "Layout",
                        10,
                        20,
                        null,
                        List.of(
                            new RibbonToggleSpec(toggleCommand("pin-preview", "Pin Preview", true)),
                            new RibbonMenuSpec(
                                "layout-presets",
                                "Presets",
                                "Apply a sample layout preset",
                                null,
                                null,
                                List.of(
                                    command("preset-two-column", "Two Column"),
                                    command("preset-wide-preview", "Wide Preview")
                                )
                            )
                        )
                    )
                )
            ),
            new RibbonTabSpec(
                "view",
                "View",
                20,
                false,
                ribbonContext -> true,
                List.of(
                    new RibbonGroupSpec(
                        "window",
                        "Window",
                        0,
                        0,
                        null,
                        List.of(
                            new RibbonButtonSpec(command("float", "Float")),
                            new RibbonButtonSpec(command("focus", "Focus")),
                            new RibbonSplitButtonSpec(
                                command("restore", "Restore"),
                                List.of(
                                    command("restore-left", "Restore Left"),
                                    command("restore-right", "Restore Right")
                                )
                            )
                        )
                    )
                )
            ),
            new RibbonTabSpec(
                "markdown",
                "Markdown",
                100,
                true,
                ribbonContext -> ribbonContext.activeContentTypeKeyOptional()
                    .map("sample.markdown"::equals)
                    .orElse(false),
                List.of(
                    new RibbonGroupSpec(
                        "authoring",
                        "Authoring",
                        0,
                        0,
                        command("front-matter-settings", "Front matter options"),
                        List.of(
                            new RibbonButtonSpec(command("preview-markdown", "Preview")),
                            new RibbonButtonSpec(command("insert-front-matter", "Front Matter")),
                            new RibbonMenuSpec(
                                "shortcodes",
                                "Shortcodes",
                                "Insert common shortcodes",
                                null,
                                null,
                                List.of(
                                    command("insert-youtube", "YouTube"),
                                    command("insert-gallery", "Gallery")
                                )
                            )
                        )
                    )
                )
            )
        );
    }

    private static PapiflyCommand command(String id, String label) {
        return PapiflyCommand.of("samples.ribbon." + id, label, () -> {});
    }

    private static PapiflyCommand toggleCommand(String id, String label, boolean selected) {
        return new PapiflyCommand(
            "samples.ribbon." + id,
            label,
            label,
            null,
            null,
            null,
            new SimpleBooleanProperty(selected),
            () -> {}
        );
    }
}
