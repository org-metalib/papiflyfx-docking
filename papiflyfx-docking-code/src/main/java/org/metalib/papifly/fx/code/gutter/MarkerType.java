package org.metalib.papifly.fx.code.gutter;

/**
 * Types of markers that can appear in the gutter marker lane.
 */
public enum MarkerType {
    ERROR(0),
    WARNING(1),
    INFO(2),
    BREAKPOINT(3),
    BOOKMARK(4);

    private final int priority;

    MarkerType(int priority) {
        this.priority = priority;
    }

    /**
     * Lower values mean higher display priority.
     */
    public int priority() {
        return priority;
    }
}
