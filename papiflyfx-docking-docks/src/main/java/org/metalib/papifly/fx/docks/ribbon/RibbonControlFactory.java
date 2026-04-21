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
import javafx.scene.shape.SVGPath;
import org.metalib.papifly.fx.api.ribbon.PapiflyCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonControlSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
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

    private static final double LARGE_ICON_SIZE = 32.0;
    private static final double MEDIUM_ICON_SIZE = 16.0;
    private static final double SMALL_ICON_SIZE = 16.0;
    private static final double LARGE_CONTROL_WIDTH = 92.0;
    private static final double MEDIUM_CONTROL_WIDTH = 72.0;
    private static final double SMALL_CONTROL_WIDTH = 36.0;
    private static final double COLLAPSED_GROUP_BUTTON_WIDTH = 56.0;
    private static final double CONTROL_GAP = 8.0;
    private static final String OCTICON_PREFIX = "octicon:";

    private RibbonControlFactory() {
    }

    static Node createGroupControl(RibbonControlSpec spec, ClassLoader classLoader, RibbonGroupSizeMode mode) {
        return switch (spec) {
            case RibbonButtonSpec button -> createButton(button.command(), classLoader, mode);
            case RibbonToggleSpec toggle -> createToggleButton(toggle.command(), classLoader, mode);
            case RibbonSplitButtonSpec splitButton -> createSplitButton(splitButton, classLoader, mode);
            case RibbonMenuSpec menu -> createMenuButton(menu, classLoader, mode);
        };
    }

    static Button createQuickAccessButton(PapiflyCommand command, ClassLoader classLoader) {
        Button button = new Button();
        button.getStyleClass().add("pf-ribbon-qat-button");
        configureCommand(button, command, true, false, classLoader);
        button.setOnAction(event -> command.execute());
        return button;
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

    private static Button createButton(PapiflyCommand command, ClassLoader classLoader, RibbonGroupSizeMode mode) {
        Button button = new Button();
        button.getStyleClass().add("pf-ribbon-command-button");
        configureGroupCommand(button, command, classLoader, mode);
        button.setOnAction(event -> command.execute());
        return button;
    }

    private static ToggleButton createToggleButton(PapiflyCommand command, ClassLoader classLoader, RibbonGroupSizeMode mode) {
        ToggleButton toggle = new ToggleButton();
        toggle.getStyleClass().add("pf-ribbon-toggle-button");
        configureGroupCommand(toggle, command, classLoader, mode);
        toggle.selectedProperty().bindBidirectional(JavaFxCommandBindings.bidirectional(command.selected()));
        toggle.setOnAction(event -> command.execute());
        return toggle;
    }

    private static SplitMenuButton createSplitButton(RibbonSplitButtonSpec spec, ClassLoader classLoader, RibbonGroupSizeMode mode) {
        SplitMenuButton splitButton = new SplitMenuButton();
        splitButton.getStyleClass().add("pf-ribbon-split-button");
        configureGroupCommand(splitButton, spec.primaryCommand(), classLoader, mode);
        splitButton.getItems().setAll(createMenuItems(spec.secondaryCommands(), classLoader));
        splitButton.setOnAction(event -> spec.primaryCommand().execute());
        return splitButton;
    }

    private static MenuButton createMenuButton(RibbonMenuSpec spec, ClassLoader classLoader, RibbonGroupSizeMode mode) {
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
        item.disableProperty().bind(JavaFxCommandBindings.readOnly(command.enabled()).not());
        item.setOnAction(event -> command.execute());
        return item;
    }

    private static void configureGroupCommand(
        Labeled labeled,
        PapiflyCommand command,
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
            buttonBase.disableProperty().bind(JavaFxCommandBindings.readOnly(command.enabled()).not());
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

        if (labeled instanceof Control control) {
            String resolvedTooltip = tooltip == null || tooltip.isBlank() ? label : tooltip;
            control.setTooltip(new Tooltip(resolvedTooltip));
        }
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
            buttonBase.disableProperty().bind(JavaFxCommandBindings.readOnly(command.enabled()).not());
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
            return createFallbackGlyph(label, LARGE_ICON_SIZE);
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
        double iconSize = switch (mode) {
            case LARGE, COLLAPSED -> LARGE_ICON_SIZE;
            case MEDIUM, SMALL -> MEDIUM_ICON_SIZE;
        };

        if (preferred != null) {
            Node icon = loadGraphic(preferred, iconSize, classLoader);
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
        return switch (control) {
            case RibbonButtonSpec button -> presentationFor(button.command());
            case RibbonToggleSpec toggle -> presentationFor(toggle.command());
            case RibbonSplitButtonSpec splitButton -> presentationFor(splitButton.primaryCommand());
            case RibbonMenuSpec menu -> new RibbonPresentation(menu.label(), menu.tooltip(), menu.smallIcon(), menu.largeIcon());
        };
    }

    private static RibbonPresentation presentationFor(PapiflyCommand command) {
        return new RibbonPresentation(command.label(), command.tooltip(), command.smallIcon(), command.largeIcon());
    }

    private static Node loadGraphic(RibbonIconHandle iconHandle, double size, ClassLoader classLoader) {
        String resourcePath = iconHandle.resourcePath();
        Node octicon = createOcticonGraphic(resourcePath, size);
        if (octicon != null) {
            return octicon;
        }
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

    private static Node createOcticonGraphic(String resourcePath, double size) {
        if (resourcePath == null || !resourcePath.startsWith(OCTICON_PREFIX)) {
            return null;
        }
        String name = resourcePath.substring(OCTICON_PREFIX.length()).trim().toLowerCase(Locale.ROOT);
        String pathData = octiconPath(name);
        if (pathData == null) {
            return null;
        }
        SVGPath svgPath = new SVGPath();
        svgPath.setContent(pathData);
        svgPath.getStyleClass().add("pf-ribbon-octicon");
        svgPath.setStyle("-fx-fill: -pf-ui-text-primary;");
        double scale = size / 16.0;
        svgPath.setScaleX(scale);
        svgPath.setScaleY(scale);
        StackPane wrapper = new StackPane(svgPath);
        wrapper.getStyleClass().add("pf-ribbon-icon-octicon-wrap");
        wrapper.setMinSize(size, size);
        wrapper.setPrefSize(size, size);
        wrapper.setMaxSize(size, size);
        return wrapper;
    }

    private static String octiconPath(String name) {
        return switch (name) {
            case "sync" -> "M8,2 A6,6 0 0 1 14,8 L12,6 M14,8 L10,8 M8,14 A6,6 0 0 1 2,8 L4,10 M2,8 L6,8";
            case "upload" -> "M8,2 L12,6 L10,6 L10,10 L6,10 L6,6 L4,6 Z M3,12 L13,12 L13,14 L3,14 Z";
            case "download", "repo-pull" -> "M8,14 L12,10 L10,10 L10,6 L6,6 L6,10 L4,10 Z M3,2 L13,2 L13,4 L3,4 Z";
            case "git-branch" -> "M5,3 A2,2 0 1 0 5,7 A2,2 0 1 0 5,3 Z M11,3 A2,2 0 1 0 11,7 A2,2 0 1 0 11,3 Z M11,9 A2,2 0 1 0 11,13 A2,2 0 1 0 11,9 Z M5,7 L5,11 L9,11 M5,9 C5,7 7,6 9,6";
            case "git-merge" -> "M5,3 A2,2 0 1 0 5,7 A2,2 0 1 0 5,3 Z M11,3 A2,2 0 1 0 11,7 A2,2 0 1 0 11,3 Z M8,13 A2,2 0 1 0 8,17 A2,2 0 1 0 8,13 Z M5,7 C5,9 7,9 8,11 M11,7 C11,9 9,9 8,11";
            case "git-pull-request" -> "M5,3 A2,2 0 1 0 5,7 A2,2 0 1 0 5,3 Z M5,7 L5,12 M11,3 A2,2 0 1 0 11,7 A2,2 0 1 0 11,3 Z M5,10 C5,7 8,7 10,7 M10,7 L8,5 M10,7 L8,9";
            case "issue-opened" -> "M8,2 A6,6 0 1 0 8,14 A6,6 0 1 0 8,2 Z M8,5 L8,9 M8,11 L8,11.2";
            case "git-commit" -> "M8,2 A6,6 0 1 0 8,14 A6,6 0 1 0 8,2 Z";
            case "diff" -> "M4,3 A1.5,1.5 0 1 0 4,6 A1.5,1.5 0 1 0 4,3 Z M12,10 A1.5,1.5 0 1 0 12,13 A1.5,1.5 0 1 0 12,10 Z M4,6 L4,12 M4,12 L10.5,12 M12,3 L12,10";
            case "trash" -> "M5,5 L11,5 L10.5,14 L5.5,14 Z M4,5 L12,5 M6.5,5 L6.5,3 L9.5,3 L9.5,5 M7,7 L7,12 M9,7 L9,12";
            case "play" -> "M4,3 L13,8 L4,13 Z";
            case "stop" -> "M4,4 L12,4 L12,12 L4,12 Z";
            case "file-add" -> "M4,2 L10,2 L13,5 L13,14 L4,14 Z M10,2 L10,5 L13,5 M8.5,8 L8.5,12 M6.5,10 L10.5,10";
            case "file" -> "M4,2 L10,2 L13,5 L13,14 L4,14 Z M10,2 L10,5 L13,5";
            case "pencil" -> "M3,12 L4,15 L7,14 L13,8 L10,5 Z M9,6 L12,9";
            case "package" -> "M3,5 L8,2 L13,5 L13,11 L8,14 L3,11 Z M3,5 L8,8 L13,5 M8,8 L8,14";
            case "package-dependencies", "archive" -> "M3,4 L13,4 L13,13 L3,13 Z M3,7 L13,7 M6,9 L10,9";
            case "terminal" -> "M2,3 L14,3 L14,13 L2,13 Z M4,6 L6.5,8 L4,10 M8,10 L11,10";
            case "code", "code-square" -> "M3,3 L13,3 L13,13 L3,13 Z M7,6 L5,8 L7,10 M9,6 L11,8 L9,10";
            case "video" -> "M3,4 L11,4 L11,12 L3,12 Z M11,7 L14,5.5 L14,10.5 L11,9 Z M6,6 L9,8 L6,10 Z";
            case "image" -> "M3,3 L13,3 L13,13 L3,13 Z M5,6 A1.2,1.2 0 1 0 5,8.4 A1.2,1.2 0 1 0 5,6 Z M4,12 L7.5,8.5 L9.5,10.5 L11,9 L12.5,12 Z";
            default -> null;
        };
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
