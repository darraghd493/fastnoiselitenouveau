package com.cognitivedynamics.noisegen.samples.nebula;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Random;

/**
 * Animated nebula visualization using 4D noise and curl noise.
 *
 * <p>Features:
 * <ul>
 *   <li>Real-time animation using 4D noise</li>
 *   <li>Multiple color palette options</li>
 *   <li>Curl noise for fluid-like motion</li>
 *   <li>Pan and zoom navigation</li>
 * </ul>
 */
public class NebulaViewApp extends Application {

    private static final int CANVAS_WIDTH = 800;
    private static final int CANVAS_HEIGHT = 600;

    private Canvas canvas;
    private NebulaGenerator nebula;
    private NebulaRenderer renderer;

    // View state
    private double viewX = 0;
    private double viewY = 0;
    private double viewScale = 1.0;

    // Animation state
    private double time = 0;
    private double animationSpeed = 1.0;
    private boolean isAnimating = false;
    private AnimationTimer animationTimer;
    private long lastFrameTime;

    // Render mode
    private NebulaRenderer.ColorMode colorMode = NebulaRenderer.ColorMode.FULL_COLOR;

    // Controls
    private TextField seedField;
    private Slider baseFreqSlider;
    private Slider filamentFreqSlider;
    private Slider turbulenceSlider;
    private Slider speedSlider;
    private Label statusLabel;
    private Label fpsLabel;
    private Button playPauseBtn;

    // FPS tracking
    private int frameCount = 0;
    private long fpsStartTime;

    @Override
    public void start(Stage primaryStage) {
        // Initialize nebula
        nebula = new NebulaGenerator(1337);
        renderer = new NebulaRenderer(nebula);

        // Build UI
        BorderPane root = new BorderPane();
        root.setCenter(createCanvasPane());
        root.setRight(createControlPanel());
        root.setBottom(createStatusBar());

        Scene scene = new Scene(root);
        primaryStage.setTitle("Nebula Generator - 4D Noise Animation Demo");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Setup animation timer
        setupAnimation();

        // Initial render
        renderFrame();
    }

    private Pane createCanvasPane() {
        canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);

        // Mouse drag for panning
        final double[] dragStart = new double[2];
        final double[] viewStart = new double[2];

        canvas.setOnMousePressed(e -> {
            dragStart[0] = e.getX();
            dragStart[1] = e.getY();
            viewStart[0] = viewX;
            viewStart[1] = viewY;
        });

        canvas.setOnMouseDragged(e -> {
            double dx = e.getX() - dragStart[0];
            double dy = e.getY() - dragStart[1];
            viewX = viewStart[0] - dx * viewScale;
            viewY = viewStart[1] - dy * viewScale;
            if (!isAnimating) {
                renderFrame();
            }
        });

        // Scroll for zoom
        canvas.setOnScroll(e -> {
            double factor = e.getDeltaY() > 0 ? 0.9 : 1.1;

            double mouseX = e.getX();
            double mouseY = e.getY();
            double worldX = viewX + mouseX * viewScale;
            double worldY = viewY + mouseY * viewScale;

            viewScale *= factor;
            viewScale = Math.max(0.1, Math.min(10, viewScale));

            viewX = worldX - mouseX * viewScale;
            viewY = worldY - mouseY * viewScale;

            if (!isAnimating) {
                renderFrame();
            }
        });

        StackPane pane = new StackPane(canvas);
        pane.setStyle("-fx-background-color: #000000;");
        return pane;
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(15));
        panel.setPrefWidth(280);
        panel.setStyle("-fx-background-color: #1a1a2e;");

        // Title
        Label title = new Label("Nebula Parameters");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e0e0ff;");

        // Seed control
        HBox seedBox = new HBox(10);
        seedBox.setAlignment(Pos.CENTER_LEFT);
        Label seedLabel = new Label("Seed:");
        seedLabel.setStyle("-fx-text-fill: #c0c0e0;");
        seedField = new TextField(String.valueOf(nebula.getSeed()));
        seedField.setPrefWidth(80);
        Button randomBtn = new Button("Random");
        randomBtn.setOnAction(e -> {
            seedField.setText(String.valueOf(new Random().nextInt(100000)));
            regenerateNebula();
        });
        seedBox.getChildren().addAll(seedLabel, seedField, randomBtn);

        // Sliders
        VBox baseFreqBox = createSlider("Base Frequency", 0.001, 0.01, nebula.getBaseFrequency(),
            s -> baseFreqSlider = s);
        VBox filamentFreqBox = createSlider("Filament Frequency", 0.002, 0.02, nebula.getFilamentFrequency(),
            s -> filamentFreqSlider = s);
        VBox turbulenceBox = createSlider("Turbulence", 0, 100, nebula.getTurbulenceStrength(),
            s -> turbulenceSlider = s);

        // Regenerate button
        Button regenerateBtn = new Button("Regenerate Nebula");
        regenerateBtn.setMaxWidth(Double.MAX_VALUE);
        regenerateBtn.setOnAction(e -> regenerateNebula());

        // Animation controls
        Label animLabel = new Label("Animation");
        animLabel.setStyle("-fx-text-fill: #e0e0ff; -fx-font-weight: bold;");

        HBox playBox = new HBox(10);
        playBox.setAlignment(Pos.CENTER_LEFT);
        playPauseBtn = new Button("▶ Play");
        playPauseBtn.setPrefWidth(80);
        playPauseBtn.setOnAction(e -> toggleAnimation());

        Button resetTimeBtn = new Button("Reset");
        resetTimeBtn.setOnAction(e -> {
            time = 0;
            if (!isAnimating) renderFrame();
        });
        playBox.getChildren().addAll(playPauseBtn, resetTimeBtn);

        VBox speedBox = createSlider("Speed", 0.1, 5.0, 1.0, s -> {
            speedSlider = s;
            s.valueProperty().addListener((obs, old, val) -> animationSpeed = val.doubleValue());
        });

        // Color mode
        Label colorLabel = new Label("Color Mode:");
        colorLabel.setStyle("-fx-text-fill: #c0c0e0;");

        ToggleGroup colorGroup = new ToggleGroup();
        VBox colorModes = new VBox(5);
        for (NebulaRenderer.ColorMode mode : NebulaRenderer.ColorMode.values()) {
            RadioButton rb = new RadioButton(formatModeName(mode.name()));
            rb.setToggleGroup(colorGroup);
            rb.setStyle("-fx-text-fill: #c0c0e0;");
            rb.setSelected(mode == colorMode);
            rb.setOnAction(e -> {
                colorMode = mode;
                if (!isAnimating) renderFrame();
            });
            colorModes.getChildren().add(rb);
        }

        // Reset view
        Button resetViewBtn = new Button("Reset View");
        resetViewBtn.setMaxWidth(Double.MAX_VALUE);
        resetViewBtn.setOnAction(e -> {
            viewX = 0;
            viewY = 0;
            viewScale = 1.0;
            if (!isAnimating) renderFrame();
        });

        // Instructions
        Label instructions = new Label(
            "Controls:\n" +
            "• Drag to pan\n" +
            "• Scroll to zoom\n" +
            "• Play for 4D animation"
        );
        instructions.setStyle("-fx-text-fill: #8080a0; -fx-font-size: 11px;");

        panel.getChildren().addAll(
            title,
            new Separator(),
            seedBox,
            baseFreqBox,
            filamentFreqBox,
            turbulenceBox,
            regenerateBtn,
            new Separator(),
            animLabel,
            playBox,
            speedBox,
            new Separator(),
            colorLabel,
            colorModes,
            new Separator(),
            resetViewBtn,
            instructions
        );

        return panel;
    }

    private VBox createSlider(String name, double min, double max, double value,
                               java.util.function.Consumer<Slider> consumer) {
        VBox box = new VBox(2);

        Label label = new Label(String.format("%s: %.4f", name, value));
        label.setStyle("-fx-text-fill: #c0c0e0; -fx-font-size: 11px;");

        Slider slider = new Slider(min, max, value);
        slider.setShowTickMarks(true);
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            label.setText(String.format("%s: %.4f", name, newVal.doubleValue()));
        });

        consumer.accept(slider);

        box.getChildren().addAll(label, slider);
        return box;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(20);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color: #0d0d1a;");

        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: #6060a0;");

        fpsLabel = new Label("FPS: --");
        fpsLabel.setStyle("-fx-text-fill: #6060a0;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timeLabel = new Label();
        timeLabel.setStyle("-fx-text-fill: #6060a0;");

        // Update time label during animation
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // This is set up later
            }
        };

        statusBar.getChildren().addAll(statusLabel, fpsLabel, spacer, timeLabel);

        // Store reference for time updates
        this.timeLabel = timeLabel;

        return statusBar;
    }

    private Label timeLabel;

    private void setupAnimation() {
        fpsStartTime = System.nanoTime();

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!isAnimating) return;

                // Calculate delta time
                double deltaSeconds = (now - lastFrameTime) / 1_000_000_000.0;
                lastFrameTime = now;

                // Update time
                time += deltaSeconds * animationSpeed;

                // Render
                renderFrame();

                // Update FPS
                frameCount++;
                double elapsed = (now - fpsStartTime) / 1_000_000_000.0;
                if (elapsed >= 1.0) {
                    fpsLabel.setText(String.format("FPS: %d", frameCount));
                    frameCount = 0;
                    fpsStartTime = now;
                }

                // Update time display
                timeLabel.setText(String.format("Time: %.1f", time));
            }
        };
    }

    private void toggleAnimation() {
        isAnimating = !isAnimating;
        if (isAnimating) {
            playPauseBtn.setText("⏸ Pause");
            lastFrameTime = System.nanoTime();
            fpsStartTime = System.nanoTime();
            frameCount = 0;
            animationTimer.start();
        } else {
            playPauseBtn.setText("▶ Play");
            animationTimer.stop();
            fpsLabel.setText("FPS: --");
        }
    }

    private void renderFrame() {
        long start = System.currentTimeMillis();
        renderer.render(canvas, viewX, viewY, viewScale, time, colorMode);
        long elapsed = System.currentTimeMillis() - start;

        if (!isAnimating) {
            statusLabel.setText(String.format("Rendered in %d ms | Scale: %.2f", elapsed, viewScale));
        }
    }

    private void regenerateNebula() {
        try {
            int seed = Integer.parseInt(seedField.getText());
            double baseFreq = baseFreqSlider.getValue();
            double filamentFreq = filamentFreqSlider.getValue();
            double turbulence = turbulenceSlider.getValue();

            nebula = new NebulaGenerator(seed, baseFreq, filamentFreq, turbulence);
            renderer = new NebulaRenderer(nebula);

            if (!isAnimating) {
                renderFrame();
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid seed value");
        }
    }

    private String formatModeName(String name) {
        return name.replace("_", " ").toLowerCase()
            .substring(0, 1).toUpperCase() +
            name.replace("_", " ").toLowerCase().substring(1);
    }

    @Override
    public void stop() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
