package com.cognitivedynamics.noisegen.samples.caves;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Renders cave system slices to JavaFX canvases.
 *
 * <p>Supports multiple visualization modes:
 * <ul>
 *   <li>Material view - Shows rock, air, water, ore with distinct colors</li>
 *   <li>Density view - Grayscale density field</li>
 *   <li>Depth shading - Simulates lighting from above</li>
 * </ul>
 */
public class CaveRenderer {

    private final CaveGenerator caves;

    public CaveRenderer(CaveGenerator caves) {
        this.caves = caves;
    }

    /**
     * Render a horizontal slice (top-down view) at a given depth.
     */
    public void renderSlice(Canvas canvas, double startX, double startZ,
                            double scale, double depth, RenderMode mode) {
        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();

        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();

        for (int py = 0; py < height; py++) {
            double z = startZ + py * scale;
            for (int px = 0; px < width; px++) {
                double x = startX + px * scale;

                CaveGenerator.CaveData data = caves.sample(x, depth, z);
                Color color = mode.toColor(data, depth);
                writer.setColor(px, py, color);
            }
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.drawImage(image, 0, 0);
    }

    /**
     * Render a vertical cross-section (side view) along X axis.
     */
    public void renderCrossSection(Canvas canvas, double startX, double startY,
                                    double scale, double z, RenderMode mode) {
        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();

        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();

        for (int py = 0; py < height; py++) {
            // Y axis: top of canvas = surface (y=0), bottom = deep underground
            double y = -py * scale;  // Negative because underground
            for (int px = 0; px < width; px++) {
                double x = startX + px * scale;

                CaveGenerator.CaveData data = caves.sample(x, y, z);
                Color color = mode.toColor(data, y);
                writer.setColor(px, py, color);
            }
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.drawImage(image, 0, 0);
    }

    /**
     * Available render modes for cave visualization.
     */
    public enum RenderMode {
        /**
         * Shows materials with distinct colors.
         */
        MATERIAL {
            @Override
            Color toColor(CaveGenerator.CaveData data, double depth) {
                return switch (data.getMaterial()) {
                    case AIR -> {
                        // Dark caves - darker at greater depth
                        double brightness = Math.max(0.08, 0.25 + depth * 0.002);
                        yield Color.gray(brightness);
                    }
                    case WATER -> Color.rgb(40, 80, 160);
                    case ORE -> Color.rgb(220, 180, 50);  // Gold/yellow
                    case ROCK -> {
                        // Lighter rock colors for better contrast
                        double d = (data.density() + 1) * 0.5;
                        int base = 120 + (int)(d * 60);
                        base = Math.min(200, base);
                        yield Color.rgb(base, base - 15, base - 25);
                    }
                };
            }
        },

        /**
         * Grayscale density field.
         */
        DENSITY {
            @Override
            Color toColor(CaveGenerator.CaveData data, double depth) {
                double normalized = (data.density() + 1) * 0.5;
                normalized = Math.max(0, Math.min(1, normalized));
                return Color.gray(normalized);
            }
        },

        /**
         * Cave/rock binary with formation highlights.
         */
        FORMATIONS {
            @Override
            Color toColor(CaveGenerator.CaveData data, double depth) {
                if (data.isCave()) {
                    // Show formations in caves
                    double f = data.formationStrength();
                    if (f > 0.1) {
                        // Stalactites/stalagmites - lighter gray
                        return Color.gray(0.4 + f * 0.5);
                    }
                    return Color.gray(0.1);  // Dark cave
                } else {
                    return Color.rgb(100, 90, 80);  // Rock
                }
            }
        },

        /**
         * Heat map style showing cave probability.
         */
        HEAT_MAP {
            @Override
            Color toColor(CaveGenerator.CaveData data, double depth) {
                // More negative = more "cave-like"
                double caveiness = -data.density();
                caveiness = (caveiness + 0.5) / 1.0;  // Normalize roughly to 0-1
                caveiness = Math.max(0, Math.min(1, caveiness));

                // Black (rock) -> Blue -> Cyan -> Yellow -> White (cave)
                if (caveiness < 0.25) {
                    double t = caveiness / 0.25;
                    return Color.color(0, 0, t * 0.5);
                } else if (caveiness < 0.5) {
                    double t = (caveiness - 0.25) / 0.25;
                    return Color.color(0, t, 0.5 + t * 0.5);
                } else if (caveiness < 0.75) {
                    double t = (caveiness - 0.5) / 0.25;
                    return Color.color(t, 1, 1 - t);
                } else {
                    double t = (caveiness - 0.75) / 0.25;
                    return Color.color(1, 1, t);
                }
            }
        },

        /**
         * Minecraft-style view with varied rock types.
         */
        MINECRAFT {
            @Override
            Color toColor(CaveGenerator.CaveData data, double depth) {
                if (data.isWater()) {
                    return Color.rgb(30, 50, 180);  // Water blue
                }
                if (data.isCave()) {
                    return Color.rgb(20, 20, 25);  // Cave darkness
                }
                if (data.isOre()) {
                    // Different ore colors based on depth
                    if (depth < -80) {
                        return Color.rgb(50, 200, 200);  // Diamond (deep)
                    } else if (depth < -50) {
                        return Color.rgb(200, 50, 50);   // Redstone
                    } else {
                        return Color.rgb(180, 140, 50);  // Gold
                    }
                }
                // Rock varies by depth
                if (depth > -5) {
                    return Color.rgb(100, 80, 50);  // Dirt/surface
                } else if (depth > -30) {
                    return Color.rgb(120, 120, 120);  // Stone
                } else {
                    return Color.rgb(80, 80, 90);  // Deep stone
                }
            }
        };

        abstract Color toColor(CaveGenerator.CaveData data, double depth);
    }
}
