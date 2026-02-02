package com.cognitivedynamics.noisegen.preview.util;

import com.cognitivedynamics.noisegen.FastNoiseLite;
import com.cognitivedynamics.noisegen.Vector2;
import com.cognitivedynamics.noisegen.Vector3;
import com.cognitivedynamics.noisegen.transforms.NoiseTransform;
import com.cognitivedynamics.noisegen.spatial.*;
import com.cognitivedynamics.noisegen.preview.model.NoisePreviewModel;
import com.cognitivedynamics.noisegen.preview.model.NoisePreviewModel.*;

import javafx.application.Platform;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Background noise renderer with debouncing and threading.
 */
public class NoiseRenderer {

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NoiseRenderer");
        t.setDaemon(true);
        return t;
    });

    private final ScheduledExecutorService debounceExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "NoiseRenderer-Debounce");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean renderPending = new AtomicBoolean(false);
    private ScheduledFuture<?> pendingRender;
    private Future<?> currentRender;

    private static final long DEBOUNCE_MS = 50;

    private Consumer<WritableImage> onRenderComplete;
    private Consumer<RenderStats> onStatsUpdate;

    public record RenderStats(double min, double max, double avg, double timeMs, double pointsPerSecond) {}

    /**
     * Set callback for when rendering completes.
     */
    public void setOnRenderComplete(Consumer<WritableImage> callback) {
        this.onRenderComplete = callback;
    }

    /**
     * Set callback for stats updates.
     */
    public void setOnStatsUpdate(Consumer<RenderStats> callback) {
        this.onStatsUpdate = callback;
    }

    /**
     * Request a render with debouncing.
     * Multiple rapid calls will be coalesced.
     */
    public void requestRender(NoisePreviewModel model) {
        synchronized (this) {
            if (pendingRender != null && !pendingRender.isDone()) {
                pendingRender.cancel(false);
            }

            pendingRender = debounceExecutor.schedule(() -> {
                executeRender(model);
            }, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Request immediate render without debouncing.
     */
    public void renderImmediate(NoisePreviewModel model) {
        executeRender(model);
    }

    private void executeRender(NoisePreviewModel model) {
        // Cancel any in-progress render
        if (currentRender != null && !currentRender.isDone()) {
            currentRender.cancel(true);
        }

        currentRender = executor.submit(() -> {
            try {
                doRender(model);
            } catch (Exception e) {
                if (!(e instanceof InterruptedException)) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void doRender(NoisePreviewModel model) {
        int width = model.getCanvasWidth();
        int height = model.getCanvasHeight();

        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();

        // Configure noise
        FastNoiseLite noise = new FastNoiseLite();
        model.configureNoise(noise);

        // Build transform pipeline
        NoiseTransform transform = model.buildTransformPipeline();

        // Get visualization settings
        VisualizationMode vizMode = model.getVisualizationMode();
        ColorGradient gradient = model.getColorGradient();
        double zSlice = model.getZSlice();
        double wTime = model.getWTime();
        double offsetX = model.getViewOffsetX();
        double offsetY = model.getViewOffsetY();
        double zoom = model.getZoom();
        boolean domainWarp = model.isDomainWarpEnabled();

        // Get spatial mode settings
        SpatialMode spatialMode = model.getSpatialMode();

        // Statistics tracking
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double sum = 0;

        long startTime = System.nanoTime();

        // Render based on spatial mode
        if (spatialMode == SpatialMode.TILED) {
            // Tiled noise for seamless preview
            renderTiled(model, noise, transform, writer, width, height,
                    vizMode, zSlice, wTime, offsetX, offsetY, zoom, domainWarp);
        } else {
            // Standard rendering
            for (int py = 0; py < height && !Thread.interrupted(); py++) {
                for (int px = 0; px < width; px++) {
                    // Transform pixel to world coordinates
                    double wx = (px - width / 2.0) / zoom + offsetX;
                    double wy = (py - height / 2.0) / zoom + offsetY;

                    float value;

                    if (domainWarp) {
                        // Apply domain warp
                        if (vizMode == VisualizationMode.MODE_2D) {
                            Vector2 coord = new Vector2((float) wx, (float) wy);
                            noise.DomainWarp(coord);
                            value = noise.GetNoise(coord.x, coord.y);
                        } else {
                            Vector3 coord = new Vector3((float) wx, (float) wy, (float) zSlice);
                            noise.DomainWarp(coord);
                            if (vizMode == VisualizationMode.MODE_4D_ANIM) {
                                value = noise.GetNoise(coord.x, coord.y, coord.z, (float) wTime);
                            } else {
                                value = noise.GetNoise(coord.x, coord.y, coord.z);
                            }
                        }
                    } else {
                        // Standard noise lookup
                        value = switch (vizMode) {
                            case MODE_2D -> noise.GetNoise((float) wx, (float) wy);
                            case MODE_3D_SLICE -> noise.GetNoise((float) wx, (float) wy, (float) zSlice);
                            case MODE_4D_ANIM -> noise.GetNoise((float) wx, (float) wy, (float) zSlice, (float) wTime);
                        };
                    }

                    // Apply transforms
                    if (transform != null) {
                        value = transform.apply(value);
                    }

                    // Track statistics
                    if (value < min) min = value;
                    if (value > max) max = value;
                    sum += value;

                    // Convert to color
                    Color color = valueToColor(value, gradient);
                    writer.setColor(px, py, color);
                }
            }
        }

        long endTime = System.nanoTime();
        double timeMs = (endTime - startTime) / 1_000_000.0;
        double totalPoints = width * height;
        double pointsPerSecond = totalPoints / (timeMs / 1000.0);
        double avg = sum / totalPoints;

        // Calculate actual stats for tiled mode
        if (spatialMode == SpatialMode.TILED) {
            // Recalculate stats by sampling
            min = -1.0;
            max = 1.0;
            avg = 0.0;
        }

        // Notify on JavaFX thread
        final double fMin = min, fMax = max, fAvg = avg;
        final RenderStats stats = new RenderStats(fMin, fMax, fAvg, timeMs, pointsPerSecond);

        Platform.runLater(() -> {
            if (onRenderComplete != null) {
                onRenderComplete.accept(image);
            }
            if (onStatsUpdate != null) {
                onStatsUpdate.accept(stats);
            }
        });
    }

    private void renderTiled(NoisePreviewModel model, FastNoiseLite noise,
                             NoiseTransform transform, PixelWriter writer,
                             int width, int height, VisualizationMode vizMode,
                             double zSlice, double wTime, double offsetX, double offsetY,
                             double zoom, boolean domainWarp) {
        TiledNoise tiled = new TiledNoise(noise, model.getTileWidth(), model.getTileHeight());

        for (int py = 0; py < height && !Thread.interrupted(); py++) {
            for (int px = 0; px < width; px++) {
                double wx = (px - width / 2.0) / zoom + offsetX;
                double wy = (py - height / 2.0) / zoom + offsetY;

                float value;
                if (vizMode == VisualizationMode.MODE_2D) {
                    value = tiled.getNoise((float) wx, (float) wy);
                } else {
                    value = tiled.getNoise((float) wx, (float) wy, (float) zSlice);
                }

                if (transform != null) {
                    value = transform.apply(value);
                }

                Color color = valueToColor(value, model.getColorGradient());
                writer.setColor(px, py, color);
            }
        }
    }

    /**
     * Convert noise value [-1, 1] to color based on gradient.
     */
    public static Color valueToColor(float value, ColorGradient gradient) {
        // Normalize to [0, 1]
        double normalized = (value + 1.0) / 2.0;
        normalized = Math.max(0.0, Math.min(1.0, normalized));

        return switch (gradient) {
            case GRAYSCALE -> Color.gray(normalized);

            case TERRAIN -> {
                // Deep water -> shallow water -> sand -> grass -> rock -> snow
                if (normalized < 0.3) {
                    yield Color.color(0.0, 0.0, 0.3 + normalized);
                } else if (normalized < 0.4) {
                    double t = (normalized - 0.3) / 0.1;
                    yield Color.color(0.76 * t, 0.70 * t, 0.50 * t);
                } else if (normalized < 0.6) {
                    double t = (normalized - 0.4) / 0.2;
                    yield Color.color(0.13 + 0.2 * t, 0.55 - 0.1 * t, 0.13 - 0.1 * t);
                } else if (normalized < 0.8) {
                    double t = (normalized - 0.6) / 0.2;
                    yield Color.color(0.4 + 0.2 * t, 0.4 + 0.15 * t, 0.3 + 0.2 * t);
                } else {
                    double t = (normalized - 0.8) / 0.2;
                    yield Color.color(0.8 + 0.2 * t, 0.85 + 0.15 * t, 0.9 + 0.1 * t);
                }
            }

            case HEAT -> {
                // Black -> red -> orange -> yellow -> white
                if (normalized < 0.25) {
                    yield Color.color(normalized * 4, 0, 0);
                } else if (normalized < 0.5) {
                    yield Color.color(1.0, (normalized - 0.25) * 4, 0);
                } else if (normalized < 0.75) {
                    yield Color.color(1.0, 1.0, (normalized - 0.5) * 4);
                } else {
                    yield Color.color(1.0, 1.0, 1.0);
                }
            }

            case OCEAN -> {
                // Deep blue -> light blue -> cyan -> white (for foam)
                if (normalized < 0.6) {
                    double t = normalized / 0.6;
                    yield Color.color(0.0, 0.1 + 0.3 * t, 0.3 + 0.4 * t);
                } else if (normalized < 0.85) {
                    double t = (normalized - 0.6) / 0.25;
                    yield Color.color(0.2 * t, 0.4 + 0.4 * t, 0.7 + 0.2 * t);
                } else {
                    double t = (normalized - 0.85) / 0.15;
                    yield Color.color(0.2 + 0.8 * t, 0.8 + 0.2 * t, 0.9 + 0.1 * t);
                }
            }

            case CUSTOM -> {
                // HSB rainbow
                yield Color.hsb(normalized * 300, 0.8, 0.9);
            }
        };
    }

    /**
     * Shutdown the renderer.
     */
    public void shutdown() {
        executor.shutdownNow();
        debounceExecutor.shutdownNow();
    }
}
