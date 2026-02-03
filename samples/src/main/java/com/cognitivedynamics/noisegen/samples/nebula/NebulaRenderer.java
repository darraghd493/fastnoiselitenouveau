package com.cognitivedynamics.noisegen.samples.nebula;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Renders nebula data to JavaFX canvases with various color schemes.
 *
 * <p>Supports multiple visualization modes:
 * <ul>
 *   <li>Full color - RGB emission mapping</li>
 *   <li>Hydrogen-alpha - Red/pink monochrome</li>
 *   <li>Oxygen III - Blue/teal monochrome</li>
 *   <li>Hubble palette - False color SHO mapping</li>
 * </ul>
 */
public class NebulaRenderer {

    private final NebulaGenerator nebula;

    public NebulaRenderer(NebulaGenerator nebula) {
        this.nebula = nebula;
    }

    /**
     * Render the nebula in full color mode.
     */
    public void renderFullColor(Canvas canvas, double startX, double startY,
                                 double scale, double time) {
        render(canvas, startX, startY, scale, time, ColorMode.FULL_COLOR);
    }

    /**
     * Render with specified color mode.
     */
    public void render(Canvas canvas, double startX, double startY,
                       double scale, double time, ColorMode mode) {
        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();

        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();

        for (int py = 0; py < height; py++) {
            double worldY = startY + py * scale;
            for (int px = 0; px < width; px++) {
                double worldX = startX + px * scale;

                NebulaGenerator.NebulaData data = nebula.sample(worldX, worldY, time);
                Color color = mode.toColor(data);
                writer.setColor(px, py, color);
            }
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.drawImage(image, 0, 0);
    }

    /**
     * Available color modes for nebula visualization.
     */
    public enum ColorMode {
        /**
         * Full RGB color based on emission channels.
         */
        FULL_COLOR {
            @Override
            Color toColor(NebulaGenerator.NebulaData data) {
                double r = gammaCorrect(data.red());
                double g = gammaCorrect(data.green());
                double b = gammaCorrect(data.blue());
                return Color.color(clamp(r), clamp(g), clamp(b));
            }
        },

        /**
         * Hydrogen-alpha emission (red/pink).
         */
        HYDROGEN_ALPHA {
            @Override
            Color toColor(NebulaGenerator.NebulaData data) {
                double intensity = data.brightness();
                double emission = Math.max(0, data.emission());
                double r = gammaCorrect(intensity * 0.9 + emission * 0.4);
                double g = gammaCorrect(intensity * 0.3 + emission * 0.1);
                double b = gammaCorrect(intensity * 0.4 + emission * 0.15);
                return Color.color(clamp(r), clamp(g), clamp(b));
            }
        },

        /**
         * Oxygen III emission (blue/teal).
         */
        OXYGEN_III {
            @Override
            Color toColor(NebulaGenerator.NebulaData data) {
                double intensity = data.brightness();
                double density = Math.max(0, data.density());
                double r = gammaCorrect(intensity * 0.2);
                double g = gammaCorrect(intensity * 0.7 + density * 0.2);
                double b = gammaCorrect(intensity * 0.95 + density * 0.3);
                return Color.color(clamp(r), clamp(g), clamp(b));
            }
        },

        /**
         * Hubble palette (SHO - Sulfur/Hydrogen/Oxygen mapped to RGB).
         */
        HUBBLE_PALETTE {
            @Override
            Color toColor(NebulaGenerator.NebulaData data) {
                // SHO: Sulfur→Red, Hydrogen→Green, Oxygen→Blue
                double sulfur = Math.max(0, data.filaments()) + data.brightness() * 0.3;
                double hydrogen = Math.max(0, data.emission()) + data.brightness() * 0.5;
                double oxygen = Math.max(0, data.density()) * 0.5 + data.brightness() * 0.4;

                double r = gammaCorrect(sulfur * 0.9);
                double g = gammaCorrect(hydrogen * 0.8);
                double b = gammaCorrect(oxygen * 0.95);
                return Color.color(clamp(r), clamp(g), clamp(b));
            }
        },

        /**
         * Monochrome density view.
         */
        DENSITY {
            @Override
            Color toColor(NebulaGenerator.NebulaData data) {
                double v = gammaCorrect(data.brightness());
                return Color.gray(clamp(v));
            }
        },

        /**
         * Filaments only view.
         */
        FILAMENTS {
            @Override
            Color toColor(NebulaGenerator.NebulaData data) {
                double v = gammaCorrect((data.filaments() + 1) * 0.5);
                return Color.color(clamp(v * 0.8), clamp(v * 0.9), clamp(v));
            }
        },

        /**
         * Heat map style.
         */
        HEAT_MAP {
            @Override
            Color toColor(NebulaGenerator.NebulaData data) {
                double v = data.brightness();
                // Black → Red → Orange → Yellow → White
                if (v < 0.25) {
                    return Color.color(v * 4, 0, 0);
                } else if (v < 0.5) {
                    return Color.color(1, (v - 0.25) * 4, 0);
                } else if (v < 0.75) {
                    return Color.color(1, 1, (v - 0.5) * 4);
                } else {
                    double w = (v - 0.75) * 4;
                    return Color.color(1, 1, 1);
                }
            }
        };

        abstract Color toColor(NebulaGenerator.NebulaData data);

        /**
         * Apply gamma correction for better visual appearance.
         */
        static double gammaCorrect(double value) {
            return Math.pow(Math.max(0, value), 0.7);  // Slight gamma boost
        }

        /**
         * Clamp value to 0-1 range.
         */
        static double clamp(double value) {
            return Math.max(0, Math.min(1, value));
        }
    }
}
