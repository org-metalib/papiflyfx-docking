package org.metalib.papifly.fx.settings.categories;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.docking.api.ThemeColors;
import org.metalib.papifly.fx.docking.api.ThemeDimensions;
import org.metalib.papifly.fx.docking.api.ThemeFonts;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.SettingType;
import org.metalib.papifly.fx.settings.api.SettingsCategory;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.settings.ui.DefinitionFormBinder;

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

    private DefinitionFormBinder binder;

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
        if (binder == null) {
            binder = new DefinitionFormBinder(definitions());
        }
        binder.load(context);
        return binder.pane();
    }

    @Override
    public void apply(SettingsContext context) {
        binder.save(context);
        context.themeProperty().set(buildTheme());
        context.storage().save();
    }

    @Override
    public void reset(SettingsContext context) {
        binder.load(context);
    }

    @Override
    public boolean isDirty() {
        return binder != null && binder.isDirty();
    }

    @Override
    public ReadOnlyBooleanProperty dirtyProperty() {
        return binder == null ? null : binder.dirtyProperty();
    }

    private Theme buildTheme() {
        ThemeMode mode = binder.<ThemeMode>control(THEME_DEFINITION.key()).getValue();
        int fontSize = binder.<Integer>control(FONT_SIZE_DEFINITION.key()).getValue().intValue();
        Density density = binder.<Density>control(DENSITY_DEFINITION.key()).getValue();
        String accent = binder.<String>control(ACCENT_DEFINITION.key()).getValue();
        String background = binder.<String>control(BACKGROUND_DEFINITION.key()).getValue();
        String border = binder.<String>control(BORDER_DEFINITION.key()).getValue();

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
}
