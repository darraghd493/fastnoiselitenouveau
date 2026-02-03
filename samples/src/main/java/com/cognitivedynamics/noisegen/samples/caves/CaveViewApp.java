package com.cognitivedynamics.noisegen.samples.caves;

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
 * 3D Cave System Visualizer using slice-based navigation.
 *
 * <p>Features:
 * <ul>
 *   <li>Top-down slice view at adjustable depth</li>
 *   <li>Cross-section side view</li>
 *   <li>Multiple visualization modes</li>
 *   <li>Pan and zoom navigation</li>
 * </ul>
 */
public class CaveViewApp extends Application {

    private static final int CANVAS_WIDTH = 700;
    private static final int CANVAS_HEIGHT = 600;

    private Canvas mainCanvas;
    private Canvas miniMapCanvas;
    private CaveGenerator caves;
    private CaveRenderer renderer;

    // View state
    private double viewX = 0;
    private double viewZ = 0;
    private double viewScale = 1.0;
    private double depth = -50;  // Current depth (negative = underground)

    // View mode
    private ViewMode viewMode = ViewMode.TOP_DOWN;
    private CaveRenderer.RenderMode renderMode = CaveRenderer.RenderMode.MATERIAL;

    // Controls
    private TextField seedField;
    private Slider cavernFreqSlider;
    private Slider tunnelFreqSlider;
    private Slider thresholdSlider;
    private Slider depthSlider;
    private Label depthLabel;
    private Label statusLabel;

    @Override
    public void start(Stage primaryStage) {
        // Initialize cave generator
        caves = new CaveGenerator(1337);
        renderer = new CaveRenderer(caves);

        // Build UI
        BorderPane root = new BorderPane();
        root.setCenter(createCanvasPane());
        root.setRight(createControlPanel());
        root.setBottom(createStatusBar());

        Scene scene = new Scene(root);
        primaryStage.setTitle("Cave System Generator - 3D Noise Demo");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Initial render
        renderView();
    }

    private Pane createCanvasPane() {
        mainCanvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);

        // Mouse drag for panning
        final double[] dragStart = new double[2];
        final double[] viewStart = new double[2];

        mainCanvas.setOnMousePressed(e -> {
            dragStart[0] = e.getX();
            dragStart[1] = e.getY();
            viewStart[0] = viewX;
            viewStart[1] = viewZ;
        });

        mainCanvas.setOnMouseDragged(e -> {
            double dx = e.getX() - dragStart[0];
            double dy = e.getY() - dragStart[1];
            viewX = viewStart[0] - dx * viewScale;
            viewZ = viewStart[1] - dy * viewScale;
            renderView();
        });

        // Scroll for zoom
        mainCanvas.setOnScroll(e -> {
            double factor = e.getDeltaY() > 0 ? 0.9 : 1.1;

            double mouseX = e.getX();
            double mouseY = e.getY();
            double worldX = viewX + mouseX * viewScale;
            double worldZ = viewZ + mouseY * viewScale;

            viewScale *= factor;
            viewScale = Math.max(0.1, Math.min(5, viewScale));

            viewX = worldX - mouseX * viewScale;
            viewZ = worldZ - mouseY * viewScale;

            renderView();
        });

        StackPane pane = new StackPane(mainCanvas);
        pane.setStyle("-fx-background-color: #1a1a1a;");
        return pane;
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(15));
        panel.setPrefWidth(300);
        panel.setStyle("-fx-background-color: #2d2d30;");

        // Title
        Label title = new Label("Cave Parameters");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e0e0e0;");

        // Seed control
        HBox seedBox = new HBox(10);
        seedBox.setAlignment(Pos.CENTER_LEFT);
        Label seedLabel = new Label("Seed:");
        seedLabel.setStyle("-fx-text-fill: #c0c0c0;");
        seedField = new TextField(String.valueOf(caves.getSeed()));
        seedField.setPrefWidth(80);
        Button randomBtn = new Button("Random");
        randomBtn.setOnAction(e -> {
            seedField.setText(String.valueOf(new Random().nextInt(100000)));
            regenerateCaves();
        });
        seedBox.getChildren().addAll(seedLabel, seedField, randomBtn);

        // Frequency sliders
        VBox cavernFreqBox = createSlider("Cavern Size", 0.005, 0.05, caves.getCavernFrequency(),
            s -> cavernFreqSlider = s, true);
        VBox tunnelFreqBox = createSlider("Tunnel Frequency", 0.01, 0.1, caves.getTunnelFrequency(),
            s -> tunnelFreqSlider = s, false);
        VBox thresholdBox = createSlider("Cave Amount", -0.5, 0.3, caves.getCaveThreshold(),
            s -> thresholdSlider = s, false);

        // Regenerate button
        Button regenerateBtn = new Button("Regenerate Caves");
        regenerateBtn.setMaxWidth(Double.MAX_VALUE);
        regenerateBtn.setOnAction(e -> regenerateCaves());

        // Depth control
        Label depthTitle = new Label("Depth Navigation");
        depthTitle.setStyle("-fx-text-fill: #e0e0e0; -fx-font-weight: bold;");

        depthLabel = new Label(String.format("Depth: %.0f meters", -depth));
        depthLabel.setStyle("-fx-text-fill: #c0c0c0;");

        depthSlider = new Slider(-150, 0, depth);
        depthSlider.setShowTickMarks(true);
        depthSlider.setShowTickLabels(true);
        depthSlider.setMajorTickUnit(50);
        depthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            depth = newVal.doubleValue();
            depthLabel.setText(String.format("Depth: %.0f meters", -depth));
            renderView();
        });

        // Quick depth buttons
        HBox depthButtons = new HBox(5);
        depthButtons.setAlignment(Pos.CENTER);
        Button surfaceBtn = new Button("Surface");
        surfaceBtn.setOnAction(e -> { depthSlider.setValue(-5); });
        Button shallowBtn = new Button("Shallow");
        shallowBtn.setOnAction(e -> { depthSlider.setValue(-30); });
        Button deepBtn = new Button("Deep");
        deepBtn.setOnAction(e -> { depthSlider.setValue(-80); });
        Button abyssBtn = new Button("Abyss");
        abyssBtn.setOnAction(e -> { depthSlider.setValue(-130); });
        depthButtons.getChildren().addAll(surfaceBtn, shallowBtn, deepBtn, abyssBtn);

        // View mode
        Label viewLabel = new Label("View Mode:");
        viewLabel.setStyle("-fx-text-fill: #c0c0c0;");

        ToggleGroup viewGroup = new ToggleGroup();
        HBox viewModes = new HBox(10);
        for (ViewMode mode : ViewMode.values()) {
            RadioButton rb = new RadioButton(mode.displayName);
            rb.setToggleGroup(viewGroup);
            rb.setStyle("-fx-text-fill: #c0c0c0;");
            rb.setSelected(mode == viewMode);
            rb.setOnAction(e -> {
                viewMode = mode;
                renderView();
            });
            viewModes.getChildren().add(rb);
        }

        // Render mode
        Label renderLabel = new Label("Color Mode:");
        renderLabel.setStyle("-fx-text-fill: #c0c0c0;");

        ToggleGroup renderGroup = new ToggleGroup();
        VBox renderModes = new VBox(3);
        for (CaveRenderer.RenderMode mode : CaveRenderer.RenderMode.values()) {
            RadioButton rb = new RadioButton(formatName(mode.name()));
            rb.setToggleGroup(renderGroup);
            rb.setStyle("-fx-text-fill: #c0c0c0;");
            rb.setSelected(mode == renderMode);
            rb.setOnAction(e -> {
                renderMode = mode;
                renderView();
            });
            renderModes.getChildren().add(rb);
        }

        // Reset view button
        Button resetViewBtn = new Button("Reset View");
        resetViewBtn.setMaxWidth(Double.MAX_VALUE);
        resetViewBtn.setOnAction(e -> {
            viewX = 0;
            viewZ = 0;
            viewScale = 1.0;
            renderView();
        });

        // Legend
        VBox legend = createLegend();

        // Instructions
        Label instructions = new Label(
            "Controls:\n" +
            "• Drag to pan\n" +
            "• Scroll to zoom\n" +
            "• Depth slider to explore vertically"
        );
        instructions.setStyle("-fx-text-fill: #808080; -fx-font-size: 11px;");

        panel.getChildren().addAll(
            title,
            new Separator(),
            seedBox,
            cavernFreqBox,
            tunnelFreqBox,
            thresholdBox,
            regenerateBtn,
            new Separator(),
            depthTitle,
            depthLabel,
            depthSlider,
            depthButtons,
            new Separator(),
            viewLabel,
            viewModes,
            renderLabel,
            renderModes,
            new Separator(),
            resetViewBtn,
            legend,
            instructions
        );

        return panel;
    }

    private VBox createSlider(String name, double min, double max, double value,
                               java.util.function.Consumer<Slider> consumer, boolean inverted) {
        VBox box = new VBox(2);

        String displayValue = inverted ? String.format("%.4f (larger = smaller caves)", value)
                                       : String.format("%.4f", value);
        Label label = new Label(name + ": " + displayValue);
        label.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 11px;");

        Slider slider = new Slider(min, max, value);
        slider.setShowTickMarks(true);
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            String dv = inverted ? String.format("%.4f (larger = smaller caves)", newVal.doubleValue())
                                 : String.format("%.4f", newVal.doubleValue());
            label.setText(name + ": " + dv);
        });

        consumer.accept(slider);

        box.getChildren().addAll(label, slider);
        return box;
    }

    private VBox createLegend() {
        VBox legend = new VBox(3);
        Label legendTitle = new Label("Legend:");
        legendTitle.setStyle("-fx-text-fill: #c0c0c0; -fx-font-weight: bold;");

        legend.getChildren().add(legendTitle);

        addLegendItem(legend, Color.gray(0.15), "Cave (air)");
        addLegendItem(legend, Color.rgb(100, 100, 100), "Rock");
        addLegendItem(legend, Color.rgb(30, 60, 120), "Water");
        addLegendItem(legend, Color.rgb(180, 150, 50), "Ore");

        return legend;
    }

    private void addLegendItem(VBox legend, Color color, String label) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Region colorBox = new Region();
        colorBox.setPrefSize(14, 14);
        colorBox.setStyle(String.format(
            "-fx-background-color: rgb(%d,%d,%d); -fx-border-color: #555;",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255)
        ));

        Label textLabel = new Label(label);
        textLabel.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 10px;");

        row.getChildren().addAll(colorBox, textLabel);
        legend.getChildren().add(row);
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(15);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color: #1a1a1a;");

        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: #707070;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label coordsLabel = new Label();
        coordsLabel.setStyle("-fx-text-fill: #707070;");

        mainCanvas.setOnMouseMoved(e -> {
            double worldX = viewX + e.getX() * viewScale;
            double worldZ = viewZ + e.getY() * viewScale;
            CaveGenerator.CaveData data = caves.sample(worldX, depth, worldZ);
            coordsLabel.setText(String.format(
                "Pos: (%.0f, %.0f, %.0f) | %s | Density: %.2f",
                worldX, depth, worldZ,
                data.getMaterial().name(),
                data.density()
            ));
        });

        statusBar.getChildren().addAll(statusLabel, spacer, coordsLabel);
        return statusBar;
    }

    private void renderView() {
        long start = System.currentTimeMillis();

        if (viewMode == ViewMode.TOP_DOWN) {
            renderer.renderSlice(mainCanvas, viewX, viewZ, viewScale, depth, renderMode);
        } else {
            renderer.renderCrossSection(mainCanvas, viewX, depth, viewScale, viewZ, renderMode);
        }

        long elapsed = System.currentTimeMillis() - start;
        statusLabel.setText(String.format("Rendered in %d ms | Scale: %.2f | %s",
            elapsed, viewScale, viewMode.displayName));
    }

    private void regenerateCaves() {
        try {
            int seed = Integer.parseInt(seedField.getText());
            double cavernFreq = cavernFreqSlider.getValue();
            double tunnelFreq = tunnelFreqSlider.getValue();
            double threshold = thresholdSlider.getValue();

            caves = new CaveGenerator(seed, cavernFreq, tunnelFreq, 0.1, threshold, 0.2);
            renderer = new CaveRenderer(caves);
            renderView();
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid seed value");
        }
    }

    private String formatName(String name) {
        return name.substring(0, 1) + name.substring(1).toLowerCase().replace("_", " ");
    }

    /**
     * View modes for the cave visualization.
     */
    enum ViewMode {
        TOP_DOWN("Top-Down (Slice)"),
        CROSS_SECTION("Cross-Section");

        final String displayName;

        ViewMode(String displayName) {
            this.displayName = displayName;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
