package org.metalib.papifly.fx.settings.categories;

import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.docking.api.ThemeColors;
import org.metalib.papifly.fx.docking.api.ThemeDimensions;
import org.metalib.papifly.fx.docking.api.ThemeFonts;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingType;
import org.metalib.papifly.fx.settings.api.SettingsCategory;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.settings.ui.controls.ColorSettingControl;
import org.metalib.papifly.fx.settings.ui.controls.EnumSettingControl;
import org.metalib.papifly.fx.settings.ui.controls.NumberSettingControl;

import java.util.List;

public class AppearanceCategory implements SettingsCategory {

    private enum ThemeMode {
        DARK,
        LIGHT
    }

    private enum Density {
        COMPACT,
        COMFORTABLE
    }

    private static final SettingDefinition<ThemeMode> THEME_DEFINITION = SettingDefinition
        .of("appearance.theme", "Theme", SettingType.ENUM, ThemeMode.DARK)
        .withDescription("Choose the base theme preset.");
    private static final SettingDefinition<Integer> FONT_SIZE_DEFINITION = SettingDefinition
        .of("appearance.font.size", "Font Size", SettingType.INTEGER, 12)
        .withDescription("Sets the base content font size.");
    private static final SettingDefinition<Density> DENSITY_DEFINITION = SettingDefinition
        .of("appearance.density", "Density", SettingType.ENUM, Density.COMFORTABLE)
        .withDescription("Controls header and tab spacing.");
    private static final SettingDefinition<String> ACCENT_DEFINITION = SettingDefinition
        .of("appearance.accent", "Accent Color", SettingType.COLOR, "#007acc")
        .withDescription("Applied to focus and active elements.");
    private static final SettingDefinition<String> BACKGROUND_DEFINITION = SettingDefinition
        .of("appearance.background", "Background Color", SettingType.COLOR, "#1e1e1e")
        .withDescription("Main panel background override.");
    private static final SettingDefinition<String> BORDER_DEFINITION = SettingDefinition
        .of("appearance.border", "Border Color", SettingType.COLOR, "#3c3c3c")
        .withDescription("Border color override.");

    private EnumSettingControl<ThemeMode> themeControl;
    private NumberSettingControl<Integer> fontSizeControl;
    private EnumSettingControl<Density> densityControl;
    private ColorSettingControl accentColorControl;
    private ColorSettingControl backgroundColorControl;
    private ColorSettingControl borderColorControl;
    private VBox pane;
    private boolean dirty;

    @Override
    public String id() {
        return "appearance";
    }

    @Override
    public String displayName() {
        return "Appearance";
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public List<SettingDefinition<?>> definitions() {
        return List.of(
            THEME_DEFINITION,
            FONT_SIZE_DEFINITION,
            DENSITY_DEFINITION,
            ACCENT_DEFINITION,
            BACKGROUND_DEFINITION,
            BORDER_DEFINITION
        );
    }

    @Override
    public Node buildSettingsPane(SettingsContext context) {
        if (pane == null) {
            pane = new VBox(12);
            pane.setFillWidth(true);

            themeControl = new EnumSettingControl<>(THEME_DEFINITION);
            fontSizeControl = new NumberSettingControl(FONT_SIZE_DEFINITION);
            densityControl = new EnumSettingControl<>(DENSITY_DEFINITION);
            accentColorControl = new ColorSettingControl(ACCENT_DEFINITION);
            backgroundColorControl = new ColorSettingControl(BACKGROUND_DEFINITION);
            borderColorControl = new ColorSettingControl(BORDER_DEFINITION);

            themeControl.setOnChange(this::markDirty);
            fontSizeControl.setOnChange(this::markDirty);
            densityControl.setOnChange(this::markDirty);
            accentColorControl.setOnChange(this::markDirty);
            backgroundColorControl.setOnChange(this::markDirty);
            borderColorControl.setOnChange(this::markDirty);

            pane.getChildren().addAll(
                themeControl,
                fontSizeControl,
                densityControl,
                accentColorControl,
                backgroundColorControl,
                borderColorControl
            );
        }
        reset(context);
        return pane;
    }

    @Override
    public void apply(SettingsContext context) {
        ThemeMode mode = themeControl.getValue();
        int fontSize = fontSizeControl.getValue().intValue();
        Density density = densityControl.getValue();
        String accent = accentColorControl.getValue();
        String background = backgroundColorControl.getValue();
        String border = borderColorControl.getValue();

        context.storage().putString(SettingScope.APPLICATION, THEME_DEFINITION.key(), mode.name().toLowerCase());
        context.storage().putInt(SettingScope.APPLICATION, FONT_SIZE_DEFINITION.key(), fontSize);
        context.storage().putString(SettingScope.APPLICATION, DENSITY_DEFINITION.key(), density.name().toLowerCase());
        context.storage().putString(SettingScope.APPLICATION, ACCENT_DEFINITION.key(), accent);
        context.storage().putString(SettingScope.APPLICATION, BACKGROUND_DEFINITION.key(), background);
        context.storage().putString(SettingScope.APPLICATION, BORDER_DEFINITION.key(), border);
        context.themeProperty().set(buildTheme(mode, fontSize, density, accent, background, border));
        context.storage().save();
        dirty = false;
    }

    @Override
    public void reset(SettingsContext context) {
        ThemeMode mode = "light".equalsIgnoreCase(context.getString(THEME_DEFINITION.key(), "dark"))
            ? ThemeMode.LIGHT
            : ThemeMode.DARK;
        int fontSize = context.getInt(FONT_SIZE_DEFINITION.key(), FONT_SIZE_DEFINITION.defaultValue().intValue());
        Density density = "compact".equalsIgnoreCase(context.getString(DENSITY_DEFINITION.key(), "comfortable"))
            ? Density.COMPACT
            : Density.COMFORTABLE;
        themeControl.setValue(mode);
        fontSizeControl.setValue(fontSize);
        densityControl.setValue(density);
        accentColorControl.setValue(context.getString(ACCENT_DEFINITION.key(), ACCENT_DEFINITION.defaultValue()));
        backgroundColorControl.setValue(context.getString(BACKGROUND_DEFINITION.key(), defaultBackground(mode)));
        borderColorControl.setValue(context.getString(BORDER_DEFINITION.key(), defaultBorder(mode)));
        dirty = false;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    private void markDirty() {
        dirty = true;
    }

    private Theme buildTheme(
        ThemeMode mode,
        int fontSize,
        Density density,
        String accent,
        String background,
        String border
    ) {
        Theme base = mode == ThemeMode.LIGHT ? Theme.light() : Theme.dark();
        double densityScale = density == Density.COMPACT ? 0.9 : 1.0;
        return Theme.of(
            new ThemeColors(
                Color.web(background),
                base.colors().headerBackground(),
                base.colors().headerBackgroundActive(),
                Color.web(accent),
                base.colors().textColor(),
                base.colors().textColorActive(),
                Color.web(border),
                base.colors().dividerColor(),
                base.colors().dropHintColor(),
                base.colors().buttonHoverBackground(),
                base.colors().buttonPressedBackground(),
                base.colors().minimizedBarBackground()
            ),
            new ThemeFonts(
                javafx.scene.text.Font.font(base.fonts().headerFont().getFamily(), base.fonts().headerFont().getSize() * densityScale),
                javafx.scene.text.Font.font(base.fonts().contentFont().getFamily(), fontSize)
            ),
            new ThemeDimensions(
                base.dimensions().cornerRadius(),
                base.dimensions().borderWidth(),
                base.dimensions().headerHeight() * densityScale,
                base.dimensions().tabHeight() * densityScale,
                base.dimensions().contentPadding(),
                base.dimensions().buttonSpacing(),
                base.dimensions().minimizedBarHeight() * densityScale
            )
        );
    }

    private String defaultBackground(ThemeMode mode) {
        return mode == ThemeMode.LIGHT ? "#f0f0f0" : "#1e1e1e";
    }

    private String defaultBorder(ThemeMode mode) {
        return mode == ThemeMode.LIGHT ? "#b4b4b4" : "#3c3c3c";
    }
}
