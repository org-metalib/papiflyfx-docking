package org.metalib.papifly.fx.docks.ribbon;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.stage.WindowEvent;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonControlSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonIconHandle;
import org.metalib.papifly.fx.api.ribbon.RibbonMenuSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonSplitButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonToggleSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonToggleCommand;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.ui.UiStyleSupport;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Internal node factory for ribbon command descriptors.
 */
final class RibbonControlFactory {

    private static final double LARGE_ICON_SIZE = 32.0;
    private static final double MEDIUM_ICON_SIZE = 16.0;
    private static final double SMALL_ICON_SIZE = 16.0;
    private static final double LARGE_CONTROL_WIDTH = 92.0;
    private static final double MEDIUM_CONTROL_WIDTH = 72.0;
    private static final double SMALL_CONTROL_WIDTH = 36.0;
    private static final double COLLAPSED_GROUP_BUTTON_WIDTH = 56.0;
    private static final double CONTROL_GAP = 8.0;
    private static final String RIBBON_MENU_POPUP_CONFIGURED = "pf-ribbon-menu-popup-configured";

    private RibbonControlFactory() {
    }

    static Node createGroupControl(
        RibbonControlSpec spec,
        ClassLoader classLoader,
        RibbonGroupSizeMode mode,
        ObservableValue<Theme> theme
    ) {
        return RibbonControlStrategies.createGroupControl(spec, classLoader, mode, theme)
            .orElseGet(() -> new Label());
    }

    static Node createGroupControl(RibbonControlSpec spec, ClassLoader classLoader, RibbonGroupSizeMode mode) {
        return createGroupControl(spec, classLoader, mode, null);
    }

    static Button createQuickAccessButton(RibbonCommand command, ClassLoader classLoader) {
        Button button = new Button();
        button.getStyleClass().add("pf-ribbon-qat-button");
        configureCommand(button, command, true, false, classLoader);
        button.setOnAction(event -> command.execute());
        return button;
    }

    static Button createSideToolbarCommandButton(RibbonCommand command, ClassLoader classLoader) {
        Button button = new Button();
        button.getStyleClass().addAll("pf-ribbon-side-toolbar-button", "pf-ribbon-side-toolbar-command");
        configureCommand(button, command, true, true, classLoader);
        if (button.getTooltip() == null) {
            button.setTooltip(new Tooltip(command.label()));
        }
        button.setOnAction(event -> command.execute());
        return button;
    }

    static Node createSideToolbarGlyph(String label) {
        return createFallbackGlyph(label, SMALL_ICON_SIZE);
    }

    static void dispose(Node node) {
        JavaFxCommandBindings.dispose(node);
    }

    static void configureCollapsedGroupButton(Button button, RibbonGroupSpec spec, ClassLoader classLoader) {
        RibbonPresentation presentation = groupPresentation(spec);
        button.getStyleClass().add("pf-ribbon-group-collapsed-button");
        button.setAccessibleText(spec.label());
        button.setMnemonicParsing(false);
        button.setText("");
        button.setWrapText(false);
        button.setGraphic(createGraphic(
            spec.label(),
            presentation.smallIcon(),
            presentation.largeIcon(),
            RibbonGroupSizeMode.COLLAPSED,
            true,
            classLoader
        ));
        button.setContentDisplay(button.getGraphic() == null ? ContentDisplay.TEXT_ONLY : ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(new Tooltip(spec.label()));
    }

    static double preferredControlWidth(RibbonGroupSizeMode mode) {
        return switch (mode) {
            case LARGE -> LARGE_CONTROL_WIDTH;
            case MEDIUM -> MEDIUM_CONTROL_WIDTH;
            case SMALL -> SMALL_CONTROL_WIDTH;
            case COLLAPSED -> COLLAPSED_GROUP_BUTTON_WIDTH;
        };
    }

    static double controlGap() {
        return CONTROL_GAP;
    }

    static Button createButton(RibbonCommand command, ClassLoader classLoader, RibbonGroupSizeMode mode) {
        Button button = new Button();
        button.getStyleClass().add("pf-ribbon-command-button");
        configureGroupCommand(button, command, classLoader, mode);
        button.setOnAction(event -> command.execute());
        return button;
    }

    static ToggleButton createToggleButton(RibbonToggleCommand command, ClassLoader classLoader, RibbonGroupSizeMode mode) {
        ToggleButton toggle = new ToggleButton();
        toggle.getStyleClass().add("pf-ribbon-toggle-button");
        configureGroupCommand(toggle, command, classLoader, mode);
        JavaFxCommandBindings.bindBidirectional(toggle, command.selected());
        toggle.setOnAction(event -> command.execute());
        return toggle;
    }

    static SplitMenuButton createSplitButton(
        RibbonSplitButtonSpec spec,
        ClassLoader classLoader,
        RibbonGroupSizeMode mode,
        ObservableValue<Theme> theme
    ) {
        SplitMenuButton splitButton = new SplitMenuButton();
        splitButton.getStyleClass().add("pf-ribbon-split-button");
        configureGroupCommand(splitButton, spec.primaryCommand(), classLoader, mode);
        splitButton.getItems().setAll(createMenuItems(spec.secondaryCommands(), classLoader, theme));
        splitButton.setOnAction(event -> spec.primaryCommand().execute());
        configureMenuPopup(splitButton, theme);
        return splitButton;
    }

    static MenuButton createMenuButton(
        RibbonMenuSpec spec,
        ClassLoader classLoader,
        RibbonGroupSizeMode mode,
        ObservableValue<Theme> theme
    ) {
        MenuButton menuButton = new MenuButton();
        menuButton.getStyleClass().add("pf-ribbon-menu-button");
        configureGroupMetadata(
            menuButton,
            spec.label(),
            spec.tooltip(),
            spec.smallIcon(),
            spec.largeIcon(),
            classLoader,
            mode
        );
        menuButton.getItems().setAll(createMenuItems(spec.items(), classLoader, theme));
        configureMenuPopup(menuButton, theme);
        return menuButton;
    }

    private static List<MenuItem> createMenuItems(
        List<RibbonCommand> commands,
        ClassLoader classLoader,
        ObservableValue<Theme> theme
    ) {
        return commands.stream()
            .map(command -> createMenuItem(command, classLoader, theme))
            .toList();
    }

    private static MenuItem createMenuItem(RibbonCommand command, ClassLoader classLoader, ObservableValue<Theme> theme) {
        MenuItem item = new MenuItem(command.label());
        item.getStyleClass().add("pf-ribbon-menu-item");
        Node graphic = createGraphic(command.label(), command.smallIcon(), command.largeIcon(), true, false, classLoader);
        item.setGraphic(graphic);
        JavaFxCommandBindings.bindDisabledToNot(item, command.enabled());
        item.setOnAction(event -> command.execute());
        item.parentPopupProperty().addListener((obs, oldPopup, newPopup) -> configureContextMenu(newPopup, theme));
        return item;
    }

    private static void configureMenuPopup(MenuButton menuButton, ObservableValue<Theme> theme) {
        menuButton.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (!isShowing) {
                return;
            }
            menuButton.getItems().forEach(item -> configureContextMenu(item.getParentPopup(), theme));
        });
    }

    private static void configureContextMenu(ContextMenu popup, ObservableValue<Theme> theme) {
        if (popup == null) {
            return;
        }
        if (!popup.getStyleClass().contains("pf-ribbon-menu-popup")) {
            popup.getStyleClass().add("pf-ribbon-menu-popup");
        }
        applyContextMenuTheme(popup, theme);
        if (Boolean.TRUE.equals(popup.getProperties().get(RIBBON_MENU_POPUP_CONFIGURED))) {
            return;
        }
        popup.getProperties().put(RIBBON_MENU_POPUP_CONFIGURED, Boolean.TRUE);
        popup.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> applyContextMenuTheme(popup, theme));
        if (theme != null) {
            ChangeListener<Theme> listener = (obs, oldTheme, newTheme) -> applyContextMenuTheme(popup, theme);
            theme.addListener(listener);
        }
    }

    private static void applyContextMenuTheme(ContextMenu popup, ObservableValue<Theme> theme) {
        Theme resolvedTheme = theme == null || theme.getValue() == null ? Theme.dark() : theme.getValue();
        popup.setStyle(RibbonThemeSupport.themeVariables(resolvedTheme));
        if (popup.getScene() != null && popup.getScene().getRoot() != null) {
            UiStyleSupport.ensureCommonStylesheetLoaded(popup.getScene().getRoot());
            UiStyleSupport.ensureStylesheetLoaded(popup.getScene().getRoot(), Ribbon.STYLESHEET);
            popup.getScene().getRoot().setStyle(RibbonThemeSupport.themeVariables(resolvedTheme));
        }
    }

    private static void configureGroupCommand(
        Labeled labeled,
        RibbonCommand command,
        ClassLoader classLoader,
        RibbonGroupSizeMode mode
    ) {
        configureGroupMetadata(
            labeled,
            command.label(),
            command.tooltip(),
            command.smallIcon(),
            command.largeIcon(),
            classLoader,
            mode
        );
        if (labeled instanceof ButtonBase buttonBase) {
            JavaFxCommandBindings.bindDisabledToNot(buttonBase, command.enabled());
        }
    }

    private static void configureGroupMetadata(
        Labeled labeled,
        String label,
        String tooltip,
        RibbonIconHandle smallIcon,
        RibbonIconHandle largeIcon,
        ClassLoader classLoader,
        RibbonGroupSizeMode mode
    ) {
        Node graphic = createGraphic(label, smallIcon, largeIcon, mode, true, classLoader);
        updateModeClass(labeled, mode);
        labeled.setGraphic(graphic);
        labeled.setMnemonicParsing(false);

        switch (mode) {
            case LARGE, MEDIUM -> {
                labeled.setText(label);
                labeled.setWrapText(true);
                labeled.setContentDisplay(graphic == null ? ContentDisplay.TEXT_ONLY : ContentDisplay.TOP);
            }
            case SMALL -> {
                labeled.setText(graphic == null ? label : "");
                labeled.setWrapText(false);
                labeled.setContentDisplay(graphic == null ? ContentDisplay.TEXT_ONLY : ContentDisplay.GRAPHIC_ONLY);
            }
            case COLLAPSED -> {
                labeled.setText("");
                labeled.setWrapText(false);
                labeled.setContentDisplay(graphic == null ? ContentDisplay.TEXT_ONLY : ContentDisplay.GRAPHIC_ONLY);
            }
        }
        updateAccessibleText(labeled, label);

        if (labeled instanceof Control control) {
            String resolvedTooltip = tooltip == null || tooltip.isBlank() ? label : tooltip;
            control.setTooltip(new Tooltip(resolvedTooltip));
        }
    }

    private static void configureCommand(
        Labeled labeled,
        RibbonCommand command,
        boolean compact,
        boolean allowFallbackGlyph,
        ClassLoader classLoader
    ) {
        configureMetadata(
            labeled,
            command.label(),
            command.tooltip(),
            command.smallIcon(),
            command.largeIcon(),
            compact,
            allowFallbackGlyph,
            classLoader
        );
        if (labeled instanceof ButtonBase buttonBase) {
            JavaFxCommandBindings.bindDisabledToNot(buttonBase, command.enabled());
        }
    }

    private static void configureMetadata(
        Labeled labeled,
        String label,
        String tooltip,
        RibbonIconHandle smallIcon,
        RibbonIconHandle largeIcon,
        boolean compact,
        boolean allowFallbackGlyph,
        ClassLoader classLoader
    ) {
        Node graphic = createGraphic(label, smallIcon, largeIcon, compact, allowFallbackGlyph, classLoader);
        labeled.setGraphic(graphic);
        labeled.setMnemonicParsing(false);

        if (compact) {
            labeled.setText(graphic == null ? label : "");
            labeled.setContentDisplay(graphic == null ? ContentDisplay.LEFT : ContentDisplay.GRAPHIC_ONLY);
        } else {
            labeled.setText(label);
            labeled.setWrapText(true);
            labeled.setContentDisplay(graphic == null ? ContentDisplay.TEXT_ONLY : ContentDisplay.TOP);
        }
        updateAccessibleText(labeled, label);

        if (tooltip != null && !tooltip.isBlank() && labeled instanceof Control control) {
            control.setTooltip(new Tooltip(tooltip));
        }
    }

    private static void updateAccessibleText(Labeled labeled, String label) {
        String text = labeled.getText();
        labeled.setAccessibleText(text == null || text.isBlank() ? label : null);
    }

    private static Node createGraphic(
        String label,
        RibbonIconHandle smallIcon,
        RibbonIconHandle largeIcon,
        boolean compact,
        boolean allowFallbackGlyph,
        ClassLoader classLoader
    ) {
        RibbonIconHandle preferred = compact
            ? (smallIcon != null ? smallIcon : largeIcon)
            : (largeIcon != null ? largeIcon : smallIcon);
        RibbonIconHandle alternate = compact
            ? (preferred == smallIcon ? largeIcon : smallIcon)
            : (preferred == largeIcon ? smallIcon : largeIcon);

        for (RibbonIconHandle iconHandle : orderedCandidates(preferred, alternate)) {
            Node icon = RibbonIconLoader.loadGraphic(iconHandle, compact ? SMALL_ICON_SIZE : LARGE_ICON_SIZE, classLoader);
            if (icon != null) {
                return icon;
            }
        }

        if (allowFallbackGlyph) {
            return createFallbackGlyph(label, compact ? SMALL_ICON_SIZE : LARGE_ICON_SIZE);
        }

        return null;
    }

    private static Node createGraphic(
        String label,
        RibbonIconHandle smallIcon,
        RibbonIconHandle largeIcon,
        RibbonGroupSizeMode mode,
        boolean allowFallbackGlyph,
        ClassLoader classLoader
    ) {
        RibbonIconHandle preferred = switch (mode) {
            case LARGE, COLLAPSED -> largeIcon != null ? largeIcon : smallIcon;
            case MEDIUM, SMALL -> smallIcon != null ? smallIcon : largeIcon;
        };
        RibbonIconHandle alternate = switch (mode) {
            case LARGE, COLLAPSED -> preferred == largeIcon ? smallIcon : largeIcon;
            case MEDIUM, SMALL -> preferred == smallIcon ? largeIcon : smallIcon;
        };
        double iconSize = switch (mode) {
            case LARGE, COLLAPSED -> LARGE_ICON_SIZE;
            case MEDIUM, SMALL -> MEDIUM_ICON_SIZE;
        };

        for (RibbonIconHandle iconHandle : orderedCandidates(preferred, alternate)) {
            Node icon = RibbonIconLoader.loadGraphic(iconHandle, iconSize, classLoader);
            if (icon != null) {
                return icon;
            }
        }

        if (allowFallbackGlyph) {
            return createFallbackGlyph(label, iconSize);
        }

        return null;
    }

    private static RibbonPresentation groupPresentation(RibbonGroupSpec spec) {
        if (spec == null || spec.controls().isEmpty()) {
            return new RibbonPresentation(spec == null ? null : spec.label(), spec == null ? null : spec.label(), null, null);
        }
        return presentationFor(spec.controls().getFirst());
    }

    private static RibbonPresentation presentationFor(RibbonControlSpec control) {
        return RibbonControlStrategies.renderPlan(control)
            .map(RibbonControlStrategies.RibbonControlRenderPlan::presentation)
            .map(presentation -> new RibbonPresentation(
                presentation.label(),
                presentation.tooltip(),
                presentation.smallIcon(),
                presentation.largeIcon()
            ))
            .orElseGet(() -> new RibbonPresentation(null, null, null, null));
    }

    private static RibbonPresentation presentationFor(RibbonCommand command) {
        return new RibbonPresentation(command.label(), command.tooltip(), command.smallIcon(), command.largeIcon());
    }

    private static List<RibbonIconHandle> orderedCandidates(RibbonIconHandle preferred, RibbonIconHandle alternate) {
        if (preferred == null && alternate == null) {
            return List.of();
        }
        if (preferred != null && alternate != null && preferred != alternate
            && !Objects.equals(preferred.resourcePath(), alternate.resourcePath())) {
            return List.of(preferred, alternate);
        }
        return preferred == null ? List.of(alternate) : List.of(preferred);
    }

    private static String initials(String label) {
        if (label == null || label.isBlank()) {
            return "R";
        }
        String[] parts = label.trim().split("\\s+");
        if (parts.length >= 2) {
            return (firstLetter(parts[0]) + firstLetter(parts[1])).toUpperCase(Locale.ROOT);
        }
        String compact = label.replaceAll("[^A-Za-z0-9]", "");
        if (compact.length() >= 2) {
            return compact.substring(0, 2).toUpperCase(Locale.ROOT);
        }
        return compact.isEmpty() ? "R" : compact.toUpperCase(Locale.ROOT);
    }

    private static String firstLetter(String value) {
        return value == null || value.isBlank() ? "" : value.substring(0, 1);
    }

    private static void updateModeClass(Node node, RibbonGroupSizeMode mode) {
        node.getStyleClass().removeAll(
            "pf-ribbon-control-large",
            "pf-ribbon-control-medium",
            "pf-ribbon-control-small",
            "pf-ribbon-control-collapsed"
        );
        node.getStyleClass().add(switch (mode) {
            case LARGE -> "pf-ribbon-control-large";
            case MEDIUM -> "pf-ribbon-control-medium";
            case SMALL -> "pf-ribbon-control-small";
            case COLLAPSED -> "pf-ribbon-control-collapsed";
        });
    }

    private static Node createFallbackGlyph(String label, double iconSize) {
        Label fallback = new Label(initials(label));
        fallback.setAlignment(Pos.CENTER);
        fallback.getStyleClass().add("pf-ribbon-fallback-glyph");
        double dimension = iconSize + 8.0;
        fallback.setMinSize(dimension, dimension);
        fallback.setPrefSize(dimension, dimension);
        fallback.setMaxSize(dimension, dimension);
        StackPane wrapper = new StackPane(fallback);
        wrapper.getStyleClass().add("pf-ribbon-fallback-glyph-wrap");
        return wrapper;
    }

    private record RibbonPresentation(
        String label,
        String tooltip,
        RibbonIconHandle smallIcon,
        RibbonIconHandle largeIcon
    ) {
    }
}
