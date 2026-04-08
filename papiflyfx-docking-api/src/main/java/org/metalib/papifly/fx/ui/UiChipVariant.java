package org.metalib.papifly.fx.ui;

/**
 * Shared visual variants for pill and chip-like controls.
 */
public enum UiChipVariant {
    DEFAULT("pf-ui-chip-neutral"),
    NEUTRAL("pf-ui-chip-neutral"),
    ACCENT("pf-ui-chip-accent"),
    SUCCESS("pf-ui-chip-success"),
    WARNING("pf-ui-chip-warning"),
    DANGER("pf-ui-chip-danger"),
    MUTED("pf-ui-chip-muted");

    private final String styleClass;

    UiChipVariant(String styleClass) {
        this.styleClass = styleClass;
    }

    public String styleClass() {
        return styleClass;
    }
}
