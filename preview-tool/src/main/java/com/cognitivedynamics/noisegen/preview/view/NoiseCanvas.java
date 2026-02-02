package com.cognitivedynamics.noisegen.preview.view;

import com.cognitivedynamics.noisegen.preview.model.NoisePreviewModel;
import com.cognitivedynamics.noisegen.preview.model.NoisePreviewModel.*;
import com.cognitivedynamics.noisegen.preview.util.NoiseRenderer;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * Canvas component for rendering noise preview with pan/zoom support.
 */
public class NoiseCanvas extends VBox {

    private final NoisePreviewModel model;
    private final NoiseRenderer renderer;
    private final Canvas canvas;
    private final GraphicsContext gc;

    // Mode tabs
    private final ToggleGroup modeGroup = new ToggleGroup();
    private final ToggleButton mode2D;
    private final ToggleButton mode3D;
    private final ToggleButton mode4D;

    // 3D/4D controls
    private final HBox sliceControls;
    private final Slider zSlider;
    private final Slider wSlider;
    private final Button playButton;
    private final Slider speedSlider;

    // Stats display
    private final Label statsLabel;

    // Pan state
    private double lastMouseX, lastMouseY;
    private boolean dragging = false;

    // Animation timer
    private AnimationTimer animationTimer;
    private long lastFrameTime;

    public NoiseCanvas(NoisePreviewModel model, NoiseRenderer renderer) {
        this.model = model;
        this.renderer = renderer;

        setSpacing(10);
        setPadding(new Insets(10));

        // Mode tabs
        mode2D = new ToggleButton("2D");
        mode3D = new ToggleButton("3D Slice");
        mode4D = new ToggleButton("4D Anim");

        mode2D.setToggleGroup(modeGroup);
        mode3D.setToggleGroup(modeGroup);
        mode4D.setToggleGroup(modeGroup);
        mode2D.setSelected(true);

        mode2D.getStyleClass().add("mode-button");
        mode3D.getStyleClass().add("mode-button");
        mode4D.getStyleClass().add("mode-button");

        HBox modeBar = new HBox(5, mode2D, mode3D, mode4D);
        modeBar.setAlignment(Pos.CENTER);

        // Canvas
        canvas = new Canvas(model.getCanvasWidth(), model.getCanvasHeight());
        gc = canvas.getGraphicsContext2D();

        // Initial fill
        gc.setFill(Color.rgb(30, 30, 30));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Canvas container with border
        StackPane canvasContainer = new StackPane(canvas);
        canvasContainer.setStyle("-fx-border-color: #555; -fx-border-width: 1;");
        canvasContainer.setMaxWidth(Region.USE_PREF_SIZE);
        canvasContainer.setMaxHeight(Region.USE_PREF_SIZE);

        // Slice controls
        Label zLabel = new Label("Z:");
        zSlider = new Slider(-10, 10, 0);
        zSlider.setShowTickLabels(true);
        zSlider.setShowTickMarks(true);
        zSlider.setMajorTickUnit(5);
        zSlider.setPrefWidth(200);

        Label wLabel = new Label("W/Time:");
        wSlider = new Slider(-10, 10, 0);
        wSlider.setShowTickLabels(true);
        wSlider.setShowTickMarks(true);
        wSlider.setMajorTickUnit(5);
        wSlider.setPrefWidth(200);

        playButton = new Button("\u25B6");
        playButton.getStyleClass().add("play-button");

        Label speedLabel = new Label("Speed:");
        speedSlider = new Slider(0.1, 5, 1);
        speedSlider.setPrefWidth(100);

        sliceControls = new HBox(10,
                zLabel, zSlider,
                new Separator(),
                wLabel, wSlider,
                playButton, speedLabel, speedSlider
        );
        sliceControls.setAlignment(Pos.CENTER);
        sliceControls.setVisible(false);
        sliceControls.setManaged(false);

        // Stats bar
        statsLabel = new Label("Render: -- ms | Min: -- Max: -- Avg: --");
        statsLabel.getStyleClass().add("stats-label");
        HBox statsBar = new HBox(statsLabel);
        statsBar.setAlignment(Pos.CENTER);
        statsBar.setPadding(new Insets(5, 0, 0, 0));

        // Layout
        getChildren().addAll(modeBar, canvasContainer, sliceControls, statsBar);
        setAlignment(Pos.CENTER);

        // Setup bindings and events
        setupBindings();
        setupMouseEvents();
        setupAnimation();
    }

    private void setupBindings() {
        // Mode selection
        mode2D.selectedProperty().addListener((obs, old, selected) -> {
            if (selected) {
                model.visualizationModeProperty().set(VisualizationMode.MODE_2D);
                sliceControls.setVisible(false);
                sliceControls.setManaged(false);
            }
        });

        mode3D.selectedProperty().addListener((obs, old, selected) -> {
            if (selected) {
                model.visualizationModeProperty().set(VisualizationMode.MODE_3D_SLICE);
                sliceControls.setVisible(true);
                sliceControls.setManaged(true);
                wSlider.setDisable(true);
                playButton.setDisable(true);
                speedSlider.setDisable(true);
            }
        });

        mode4D.selectedProperty().addListener((obs, old, selected) -> {
            if (selected) {
                model.visualizationModeProperty().set(VisualizationMode.MODE_4D_ANIM);
                sliceControls.setVisible(true);
                sliceControls.setManaged(true);
                wSlider.setDisable(false);
                playButton.setDisable(false);
                speedSlider.setDisable(false);
            }
        });

        // Bind sliders to model
        zSlider.valueProperty().bindBidirectional(model.zSliceProperty());
        wSlider.valueProperty().bindBidirectional(model.wTimeProperty());
        speedSlider.valueProperty().bindBidirectional(model.animationSpeedProperty());

        // Slider changes trigger render
        zSlider.valueProperty().addListener((obs, old, val) -> {
            if (!model.isAnimating()) {
                renderer.requestRender(model);
            }
        });

        wSlider.valueProperty().addListener((obs, old, val) -> {
            if (!model.isAnimating()) {
                renderer.requestRender(model);
            }
        });

        // Play button
        playButton.setOnAction(e -> {
            model.animatingProperty().set(!model.isAnimating());
            playButton.setText(model.isAnimating() ? "\u23F8" : "\u25B6");
        });

        // Renderer callbacks
        renderer.setOnRenderComplete(this::updateCanvas);
        renderer.setOnStatsUpdate(stats -> {
            model.updateStats(stats.min(), stats.max(), stats.avg(),
                    stats.timeMs(), stats.pointsPerSecond());
            updateStatsLabel(stats);
        });

        // Canvas size binding
        model.canvasWidthProperty().addListener((obs, old, val) -> {
            canvas.setWidth(val.doubleValue());
            renderer.requestRender(model);
        });
        model.canvasHeightProperty().addListener((obs, old, val) -> {
            canvas.setHeight(val.doubleValue());
            renderer.requestRender(model);
        });
    }

    private void setupMouseEvents() {
        canvas.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                dragging = true;
                canvas.setCursor(javafx.scene.Cursor.CLOSED_HAND);
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (dragging) {
                double dx = e.getX() - lastMouseX;
                double dy = e.getY() - lastMouseY;

                // Update view offset (inverted for natural feel)
                double zoom = model.getZoom();
                model.viewOffsetXProperty().set(model.getViewOffsetX() - dx / zoom);
                model.viewOffsetYProperty().set(model.getViewOffsetY() - dy / zoom);

                lastMouseX = e.getX();
                lastMouseY = e.getY();

                renderer.requestRender(model);
            }
        });

        canvas.setOnMouseReleased(e -> {
            dragging = false;
            canvas.setCursor(javafx.scene.Cursor.DEFAULT);
        });

        canvas.setOnScroll((ScrollEvent e) -> {
            double zoomFactor = e.getDeltaY() > 0 ? 1.1 : 0.9;
            double newZoom = model.getZoom() * zoomFactor;

            // Clamp zoom
            newZoom = Math.max(0.1, Math.min(100, newZoom));
            model.zoomProperty().set(newZoom);

            renderer.requestRender(model);
        });

        // Reset view on double-click
        canvas.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                model.viewOffsetXProperty().set(0);
                model.viewOffsetYProperty().set(0);
                model.zoomProperty().set(1.0);
                renderer.requestRender(model);
            }
        });
    }

    private void setupAnimation() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!model.isAnimating()) {
                    return;
                }

                if (lastFrameTime == 0) {
                    lastFrameTime = now;
                    return;
                }

                double deltaSeconds = (now - lastFrameTime) / 1_000_000_000.0;
                lastFrameTime = now;

                // Update W/time value
                double newW = model.getWTime() + deltaSeconds * model.getAnimationSpeed();

                // Wrap around for looping
                if (newW > 10) newW = -10;
                model.wTimeProperty().set(newW);

                renderer.renderImmediate(model);
            }
        };

        // Start/stop based on animating property
        model.animatingProperty().addListener((obs, old, animating) -> {
            if (animating) {
                lastFrameTime = 0;
                animationTimer.start();
            } else {
                animationTimer.stop();
            }
        });
    }

    private void updateCanvas(WritableImage image) {
        gc.drawImage(image, 0, 0);
    }

    private void updateStatsLabel(NoiseRenderer.RenderStats stats) {
        String text = String.format(
                "Render: %.1f ms (%.1fM pts/s) | Min: %.2f Max: %.2f Avg: %.2f",
                stats.timeMs(),
                stats.pointsPerSecond() / 1_000_000.0,
                stats.min(),
                stats.max(),
                stats.avg()
        );
        statsLabel.setText(text);
    }

    /**
     * Request a render with current settings.
     */
    public void requestRender() {
        renderer.requestRender(model);
    }

    /**
     * Get the current canvas content as an image.
     */
    public WritableImage captureImage() {
        WritableImage image = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        canvas.snapshot(null, image);
        return image;
    }
}
