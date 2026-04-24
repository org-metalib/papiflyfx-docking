package org.metalib.papifly.fx.docks.ribbon;

import javafx.scene.Node;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonControlKind;
import org.metalib.papifly.fx.api.ribbon.RibbonControlSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonIconHandle;
import org.metalib.papifly.fx.api.ribbon.RibbonMenuSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonSplitButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonToggleCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonToggleSpec;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Package-private strategy registry for built-in ribbon controls.
 */
final class RibbonControlStrategies {

    private RibbonControlStrategies() {
    }

    static Optional<RibbonControlRenderPlan> renderPlan(RibbonControlSpec spec) {
        if (spec == null) {
            return Optional.empty();
        }
        return switch (spec.kind()) {
            case BUTTON -> spec instanceof RibbonButtonSpec button
                ? Optional.of(new RibbonControlRenderPlan(
                    button.id(),
                    RibbonControlKind.BUTTON,
                    button.command(),
                    List.of(),
                    presentation(button.command())
                ))
                : Optional.empty();
            case TOGGLE -> spec instanceof RibbonToggleSpec toggle
                ? Optional.of(new RibbonControlRenderPlan(
                    toggle.id(),
                    RibbonControlKind.TOGGLE,
                    toggle.command(),
                    List.of(),
                    presentation(toggle.command())
                ))
                : Optional.empty();
            case SPLIT_BUTTON -> spec instanceof RibbonSplitButtonSpec split
                ? Optional.of(new RibbonControlRenderPlan(
                    split.id(),
                    RibbonControlKind.SPLIT_BUTTON,
                    split.primaryCommand(),
                    split.secondaryCommands(),
                    presentation(split.primaryCommand())
                ))
                : Optional.empty();
            case MENU -> spec instanceof RibbonMenuSpec menu
                ? Optional.of(new RibbonControlRenderPlan(
                    menu.id(),
                    RibbonControlKind.MENU,
                    null,
                    menu.items(),
                    new RibbonPresentation(menu.label(), menu.tooltip(), menu.smallIcon(), menu.largeIcon())
                ))
                : Optional.empty();
            case UNKNOWN -> Optional.empty();
        };
    }

    static Optional<RibbonControlSpec> canonicalize(
        RibbonControlSpec spec,
        Function<RibbonCommand, RibbonCommand> commandCanonicalizer,
        Function<RibbonToggleCommand, RibbonToggleCommand> toggleCanonicalizer
    ) {
        Objects.requireNonNull(commandCanonicalizer, "commandCanonicalizer");
        Objects.requireNonNull(toggleCanonicalizer, "toggleCanonicalizer");
        if (spec == null) {
            return Optional.empty();
        }
        return switch (spec.kind()) {
            case BUTTON -> spec instanceof RibbonButtonSpec button
                ? Optional.of(new RibbonButtonSpec(commandCanonicalizer.apply(button.command())))
                : Optional.empty();
            case TOGGLE -> spec instanceof RibbonToggleSpec toggle
                ? Optional.of(new RibbonToggleSpec(toggleCanonicalizer.apply(toggle.command())))
                : Optional.empty();
            case SPLIT_BUTTON -> spec instanceof RibbonSplitButtonSpec split
                ? Optional.of(new RibbonSplitButtonSpec(
                    commandCanonicalizer.apply(split.primaryCommand()),
                    split.secondaryCommands().stream()
                        .filter(Objects::nonNull)
                        .map(commandCanonicalizer)
                        .toList()
                ))
                : Optional.empty();
            case MENU -> spec instanceof RibbonMenuSpec menu
                ? Optional.of(new RibbonMenuSpec(
                    menu.id(),
                    menu.label(),
                    menu.tooltip(),
                    menu.smallIcon(),
                    menu.largeIcon(),
                    menu.items().stream()
                        .filter(Objects::nonNull)
                        .map(commandCanonicalizer)
                        .toList()
                ))
                : Optional.empty();
            case UNKNOWN -> Optional.empty();
        };
    }

    static Optional<Node> createGroupControl(RibbonControlSpec spec, ClassLoader classLoader, RibbonGroupSizeMode mode) {
        return switch (spec.kind()) {
            case BUTTON -> spec instanceof RibbonButtonSpec button
                ? Optional.of(RibbonControlFactory.createButton(button.command(), classLoader, mode))
                : Optional.empty();
            case TOGGLE -> spec instanceof RibbonToggleSpec toggle
                ? Optional.of(RibbonControlFactory.createToggleButton(toggle.command(), classLoader, mode))
                : Optional.empty();
            case SPLIT_BUTTON -> spec instanceof RibbonSplitButtonSpec splitButton
                ? Optional.of(RibbonControlFactory.createSplitButton(splitButton, classLoader, mode))
                : Optional.empty();
            case MENU -> spec instanceof RibbonMenuSpec menu
                ? Optional.of(RibbonControlFactory.createMenuButton(menu, classLoader, mode))
                : Optional.empty();
            case UNKNOWN -> Optional.empty();
        };
    }

    private static RibbonPresentation presentation(RibbonCommand command) {
        return new RibbonPresentation(command.label(), command.tooltip(), command.smallIcon(), command.largeIcon());
    }

    record RibbonControlRenderPlan(
        String id,
        RibbonControlKind kind,
        RibbonCommand primaryCommand,
        List<RibbonCommand> itemCommands,
        RibbonPresentation presentation
    ) {
    }

    record RibbonPresentation(
        String label,
        String tooltip,
        RibbonIconHandle smallIcon,
        RibbonIconHandle largeIcon
    ) {
    }
}
