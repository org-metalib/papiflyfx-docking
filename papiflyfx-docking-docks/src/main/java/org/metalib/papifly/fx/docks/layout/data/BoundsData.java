package org.metalib.papifly.fx.docks.layout.data;

/**
 * DTO representing window bounds for serialization.
 */
public record BoundsData(
    double x,
    double y,
    double width,
    double height
) {
    /**
     * Creates BoundsData with the given parameters.
     */
    public static BoundsData of(double x, double y, double width, double height) {
        return new BoundsData(x, y, width, height);
    }
}
