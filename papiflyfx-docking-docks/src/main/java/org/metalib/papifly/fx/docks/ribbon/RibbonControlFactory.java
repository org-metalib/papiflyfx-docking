package org.metalib.papifly.fx.docks.ribbon;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import org.metalib.papifly.fx.api.ribbon.PapiflyCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonControlSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonIconHandle;
import org.metalib.papifly.fx.api.ribbon.RibbonMenuSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonSplitButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonToggleSpec;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Locale;

/**
 * Internal node factory for ribbon command descriptors.
 */
final class RibbonControlFactory {

    private static final double LARGE_ICON_SIZE = 28.0;
    private static final double SMALL_ICON_SIZE = 16.0;

    private RibbonControlFactory() {
    }

    static Node createGroupControl(RibbonControlSpec spec, ClassLoader classLoader) {
        return switch (spec) {
            case RibbonButtonSpec button -> createButton(button.command(), classLoader);
            case RibbonToggleSpec toggle -> createToggleButton(toggle.command(), classLoader);
            case RibbonSplitButtonSpec splitButton -> createSplitButton(splitButton, classLoader);
            case RibbonMenuSpec menu -> createMenuButton(menu, classLoader);
        };
    }

    static Button createQuickAccessButton(PapiflyCommand command, ClassLoader classLoader) {
        Button button = new Button();
        button.getStyleClass().add("pf-ribbon-qat-button");
        configureCommand(button, command, true, false, classLoader);
        button.setOnAction(event -> command.execute());
        return button;
    }

    private static Button createButton(PapiflyCommand command, ClassLoader classLoader) {
        Button button = new Button();
        button.getStyleClass().add("pf-ribbon-command-button");
        configureCommand(button, command, false, true, classLoader);
        button.setOnAction(event -> command.execute());
        return button;
    }

    private static ToggleButton createToggleButton(PapiflyCommand command, ClassLoader classLoader) {
        ToggleButton toggle = new ToggleButton();
        toggle.getStyleClass().add("pf-ribbon-toggle-button");
        configureCommand(toggle, command, false, true, classLoader);
        toggle.selectedProperty().bindBidirectional(command.selectedProperty());
        toggle.setOnAction(event -> command.execute());
        return toggle;
    }

    private static SplitMenuButton createSplitButton(RibbonSplitButtonSpec spec, ClassLoader classLoader) {
        SplitMenuButton splitButton = new SplitMenuButton();
        splitButton.getStyleClass().add("pf-ribbon-split-button");
        configureCommand(splitButton, spec.primaryCommand(), false, true, classLoader);
        splitButton.getItems().setAll(createMenuItems(spec.secondaryCommands(), classLoader));
        splitButton.setOnAction(event -> spec.primaryCommand().execute());
        return splitButton;
    }

    private static MenuButton createMenuButton(RibbonMenuSpec spec, ClassLoader classLoader) {
        MenuButton menuButton = new MenuButton();
        menuButton.getStyleClass().add("pf-ribbon-menu-button");
        configureMetadata(menuButton, spec.label(), spec.tooltip(), spec.smallIcon(), spec.largeIcon(), false, true, classLoader);
        menuButton.getItems().setAll(createMenuItems(spec.items(), classLoader));
        return menuButton;
    }

    private static List<MenuItem> createMenuItems(List<PapiflyCommand> commands, ClassLoader classLoader) {
        return commands.stream()
            .map(command -> createMenuItem(command, classLoader))
            .toList();
    }

    private static MenuItem createMenuItem(PapiflyCommand command, ClassLoader classLoader) {
        MenuItem item = new MenuItem(command.label());
        Node graphic = createGraphic(command.label(), command.smallIcon(), command.largeIcon(), true, false, classLoader);
        item.setGraphic(graphic);
        item.disableProperty().bind(command.enabledProperty().not());
        item.setOnAction(event -> command.execute());
        return item;
    }

    private static void configureCommand(
        Labeled labeled,
        PapiflyCommand command,
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
            buttonBase.disableProperty().bind(command.enabledProperty().not());
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
        boolean hasIconHandle = smallIcon != null || largeIcon != null;
        labeled.setGraphic(graphic);
        labeled.setMnemonicParsing(false);

        if (compact) {
            labeled.setText(hasIconHandle ? "" : label);
            labeled.setContentDisplay(hasIconHandle ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
        } else {
            labeled.setText(label);
            labeled.setWrapText(true);
            labeled.setContentDisplay(graphic == null ? ContentDisplay.TEXT_ONLY : ContentDisplay.TOP);
        }

        if (tooltip != null && !tooltip.isBlank() && labeled instanceof Control control) {
            control.setTooltip(new Tooltip(tooltip));
        }
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

        if (preferred != null) {
            Node icon = loadGraphic(preferred, compact ? SMALL_ICON_SIZE : LARGE_ICON_SIZE, classLoader);
            if (icon != null) {
                return icon;
            }
        }

        if (allowFallbackGlyph && !compact) {
            Label fallback = new Label(initials(label));
            fallback.setAlignment(Pos.CENTER);
            fallback.getStyleClass().add("pf-ribbon-fallback-glyph");
            fallback.setMinSize(LARGE_ICON_SIZE + 8.0, LARGE_ICON_SIZE + 8.0);
            fallback.setPrefSize(LARGE_ICON_SIZE + 8.0, LARGE_ICON_SIZE + 8.0);
            fallback.setMaxSize(LARGE_ICON_SIZE + 8.0, LARGE_ICON_SIZE + 8.0);
            StackPane wrapper = new StackPane(fallback);
            wrapper.getStyleClass().add("pf-ribbon-fallback-glyph-wrap");
            return wrapper;
        }

        return null;
    }

    private static Node loadGraphic(RibbonIconHandle iconHandle, double size, ClassLoader classLoader) {
        String resourcePath = iconHandle.resourcePath();
        if (resourcePath == null || resourcePath.isBlank() || resourcePath.endsWith(".svg")) {
            return null;
        }
        try {
            URL resource = resolveResource(resourcePath, classLoader);
            if (resource == null) {
                return null;
            }
            try (InputStream inputStream = resource.openStream()) {
                Image image = new Image(inputStream, size, size, true, true);
                if (image.isError()) {
                    return null;
                }
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(size);
                imageView.setFitHeight(size);
                imageView.setPreserveRatio(true);
                imageView.getStyleClass().add("pf-ribbon-icon");
                return imageView;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static URL resolveResource(String resourcePath, ClassLoader classLoader) {
        if (resourcePath.startsWith("http://") || resourcePath.startsWith("https://") || resourcePath.startsWith("file:")) {
            try {
                return new URL(resourcePath);
            } catch (Exception ignored) {
                return null;
            }
        }
        URL resource = RibbonControlFactory.class.getResource(resourcePath);
        if (resource != null) {
            return resource;
        }
        String normalizedPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        ClassLoader resolvedLoader = classLoader == null ? RibbonControlFactory.class.getClassLoader() : classLoader;
        return resolvedLoader.getResource(normalizedPath);
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
}
