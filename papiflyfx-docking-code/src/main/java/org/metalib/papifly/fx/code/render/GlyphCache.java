package org.metalib.papifly.fx.code.render;

import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Caches monospace font measurements to avoid repeated Text node layout.
 */
public class GlyphCache {

    private static final Font DEFAULT_FONT = Font.font("monospace", 14);

    private final Text measureNode = new Text();
    private Font font;
    private double lineHeight;
    private double charWidth;

    /**
     * Creates a cache with the default monospace font.
     */
    public GlyphCache() {
        setFont(DEFAULT_FONT);
    }

    /**
     * Returns the cached line height.
     */
    public double getLineHeight() {
        return lineHeight;
    }

    /**
     * Returns the cached character width (monospace assumption).
     */
    public double getCharWidth() {
        return charWidth;
    }

    /**
     * Returns the current font.
     */
    public Font getFont() {
        return font;
    }

    /**
     * Sets the font and recalculates cached metrics.
     */
    public void setFont(Font font) {
        this.font = font != null ? font : DEFAULT_FONT;
        measureNode.setFont(this.font);

        // Measure character width using a single 'M' character
        measureNode.setText("M");
        charWidth = measureNode.getLayoutBounds().getWidth();

        // Measure line height
        lineHeight = measureNode.getLayoutBounds().getHeight();
    }
}
