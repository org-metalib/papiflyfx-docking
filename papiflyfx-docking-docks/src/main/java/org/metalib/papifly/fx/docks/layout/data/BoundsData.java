package org.metalib.papifly.fx.docks.layout.data;

/**
 * DTO representing window bounds for serialization.
 *
 * @param x screen x position
 * @param y screen y position
 * @param width window width
 * @param height window height
 */
public record BoundsData(
    double x,
    double y,
    double width,
    double height
) {
    /**
     * Creates BoundsData with the given parameters.
     *
     * @param x screen x position
     * @param y screen y position
     * @param width window width
     * @param height window height
     * @return bounds data instance
     */
    public static BoundsData of(double x, double y, double width, double height) {
        return new BoundsData(x, y, width, height);
    }
}
