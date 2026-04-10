package org.metalib.papifly.fx.ui;

import javafx.scene.paint.Paint;

import java.util.Objects;

/**
 * Shared semantic palette used by extracted lightweight UI controls.
 */
public record UiCommonPalette(
    Paint surfaceOverlay,
    Paint borderDefault,
    Paint textPrimary,
    Paint textMuted,
    Paint textDisabled,
    Paint surfaceControl,
    Paint surfaceControlHover,
    Paint surfaceControlPressed,
    Paint borderFocus,
    Paint accent,
    Paint success,
    Paint warning,
    Paint danger,
    Paint dropHint,
    Paint shadowOverlay
) {

    public UiCommonPalette {
        Objects.requireNonNull(surfaceOverlay, "surfaceOverlay");
        Objects.requireNonNull(borderDefault, "borderDefault");
        Objects.requireNonNull(textPrimary, "textPrimary");
        Objects.requireNonNull(textMuted, "textMuted");
        Objects.requireNonNull(textDisabled, "textDisabled");
        Objects.requireNonNull(surfaceControl, "surfaceControl");
        Objects.requireNonNull(surfaceControlHover, "surfaceControlHover");
        Objects.requireNonNull(surfaceControlPressed, "surfaceControlPressed");
        Objects.requireNonNull(borderFocus, "borderFocus");
        Objects.requireNonNull(accent, "accent");
        Objects.requireNonNull(success, "success");
        Objects.requireNonNull(warning, "warning");
        Objects.requireNonNull(danger, "danger");
        Objects.requireNonNull(dropHint, "dropHint");
        Objects.requireNonNull(shadowOverlay, "shadowOverlay");
    }
}
