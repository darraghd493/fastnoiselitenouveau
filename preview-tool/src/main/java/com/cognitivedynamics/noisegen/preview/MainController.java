package com.cognitivedynamics.noisegen.preview;

import com.cognitivedynamics.noisegen.preview.model.NoisePreviewModel;
import com.cognitivedynamics.noisegen.preview.util.NoiseRenderer;
import com.cognitivedynamics.noisegen.preview.view.ControlPanel;
import com.cognitivedynamics.noisegen.preview.view.NoiseCanvas;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * Main controller that wires together the model, views, and renderer.
 */
public class MainController {

    private final NoisePreviewModel model;
    private final NoiseRenderer renderer;
    private final Stage stage;

    private ControlPanel controlPanel;
    private NoiseCanvas noiseCanvas;
    private BorderPane root;

    public MainController(Stage stage) {
        this.stage = stage;
        this.model = new NoisePreviewModel();
        this.renderer = new NoiseRenderer();
    }

    /**
     * Build and return the main UI.
     */
    public BorderPane buildUI() {
        root = new BorderPane();
        root.getStyleClass().add("root");

        // Create views
        controlPanel = new ControlPanel(model);
        noiseCanvas = new NoiseCanvas(model, renderer);

        // Wire up settings changes to trigger renders
        controlPanel.setOnSettingsChanged(v -> noiseCanvas.requestRender());

        // Build menu bar
        MenuBar menuBar = buildMenuBar();

        // Layout
        root.setTop(menuBar);
        root.setLeft(controlPanel);
        root.setCenter(noiseCanvas);

        // Initial render
        noiseCanvas.requestRender();

        return root;
    }

    private MenuBar buildMenuBar() {
        MenuBar menuBar = new MenuBar();

        // File menu
        Menu fileMenu = new Menu("File");

        MenuItem exportPng = new MenuItem("Export PNG...");
        exportPng.setOnAction(e -> exportToPng());

        MenuItem copyCode = new MenuItem("Copy Code Snippet");
        copyCode.setOnAction(e -> copyCodeToClipboard());

        SeparatorMenuItem sep = new SeparatorMenuItem();

        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> stage.close());

        fileMenu.getItems().addAll(exportPng, copyCode, sep, exit);

        // View menu
        Menu viewMenu = new Menu("View");

        MenuItem resetView = new MenuItem("Reset View");
        resetView.setOnAction(e -> {
            model.viewOffsetXProperty().set(0);
            model.viewOffsetYProperty().set(0);
            model.zoomProperty().set(1.0);
            noiseCanvas.requestRender();
        });

        Menu canvasSizeMenu = new Menu("Canvas Size");
        MenuItem size256 = new MenuItem("256x256");
        size256.setOnAction(e -> setCanvasSize(256, 256));
        MenuItem size512 = new MenuItem("512x512");
        size512.setOnAction(e -> setCanvasSize(512, 512));
        MenuItem size1024 = new MenuItem("1024x1024");
        size1024.setOnAction(e -> setCanvasSize(1024, 1024));
        MenuItem sizeCustom = new MenuItem("Custom...");
        sizeCustom.setOnAction(e -> showCanvasSizeDialog());
        canvasSizeMenu.getItems().addAll(size256, size512, size1024, sizeCustom);

        viewMenu.getItems().addAll(resetView, canvasSizeMenu);

        // Presets menu
        Menu presetsMenu = new Menu("Presets");

        MenuItem presetTerrain = new MenuItem("Terrain");
        presetTerrain.setOnAction(e -> applyTerrainPreset());

        MenuItem presetClouds = new MenuItem("Clouds");
        presetClouds.setOnAction(e -> applyCloudsPreset());

        MenuItem presetMarble = new MenuItem("Marble");
        presetMarble.setOnAction(e -> applyMarblePreset());

        MenuItem presetWood = new MenuItem("Wood Grain");
        presetWood.setOnAction(e -> applyWoodPreset());

        MenuItem presetCells = new MenuItem("Cells/Voronoi");
        presetCells.setOnAction(e -> applyCellsPreset());

        presetsMenu.getItems().addAll(presetTerrain, presetClouds, presetMarble, presetWood, presetCells);

        // Help menu
        Menu helpMenu = new Menu("Help");

        MenuItem about = new MenuItem("About");
        about.setOnAction(e -> showAboutDialog());

        helpMenu.getItems().add(about);

        menuBar.getMenus().addAll(fileMenu, viewMenu, presetsMenu, helpMenu);
        return menuBar;
    }

    private void exportToPng() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Noise as PNG");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Images", "*.png")
        );
        chooser.setInitialFileName("noise.png");

        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            try {
                WritableImage image = noiseCanvas.captureImage();
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Complete");
                alert.setHeaderText(null);
                alert.setContentText("Image saved to: " + file.getAbsolutePath());
                alert.showAndWait();
            } catch (IOException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Failed");
                alert.setHeaderText(null);
                alert.setContentText("Failed to save image: " + ex.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void copyCodeToClipboard() {
        String code = model.generateCodeSnippet();

        ClipboardContent content = new ClipboardContent();
        content.putString(code);
        Clipboard.getSystemClipboard().setContent(content);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Code Copied");
        alert.setHeaderText(null);
        alert.setContentText("Java code snippet copied to clipboard.");
        alert.showAndWait();
    }

    private void setCanvasSize(int width, int height) {
        model.canvasWidthProperty().set(width);
        model.canvasHeightProperty().set(height);
    }

    private void showCanvasSizeDialog() {
        Dialog<int[]> dialog = new Dialog<>();
        dialog.setTitle("Custom Canvas Size");
        dialog.setHeaderText("Enter canvas dimensions:");

        // Set the button types
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        // Create fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Spinner<Integer> widthSpinner = new Spinner<>(64, 4096, model.getCanvasWidth(), 64);
        widthSpinner.setEditable(true);
        Spinner<Integer> heightSpinner = new Spinner<>(64, 4096, model.getCanvasHeight(), 64);
        heightSpinner.setEditable(true);

        grid.add(new Label("Width:"), 0, 0);
        grid.add(widthSpinner, 1, 0);
        grid.add(new Label("Height:"), 0, 1);
        grid.add(heightSpinner, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button == okButton) {
                return new int[]{widthSpinner.getValue(), heightSpinner.getValue()};
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> setCanvasSize(result[0], result[1]));
    }

    // ==================== Presets ====================

    private void applyTerrainPreset() {
        model.noiseTypeProperty().set(com.cognitivedynamics.noisegen.FastNoiseLite.NoiseType.OpenSimplex2);
        model.fractalTypeProperty().set(com.cognitivedynamics.noisegen.FastNoiseLite.FractalType.FBm);
        model.octavesProperty().set(6);
        model.frequencyProperty().set(0.005);
        model.lacunarityProperty().set(2.0);
        model.gainProperty().set(0.5);
        model.colorGradientProperty().set(NoisePreviewModel.ColorGradient.TERRAIN);
        model.getTransforms().clear();
        noiseCanvas.requestRender();
    }

    private void applyCloudsPreset() {
        model.noiseTypeProperty().set(com.cognitivedynamics.noisegen.FastNoiseLite.NoiseType.OpenSimplex2);
        model.fractalTypeProperty().set(com.cognitivedynamics.noisegen.FastNoiseLite.FractalType.Billow);
        model.octavesProperty().set(5);
        model.frequencyProperty().set(0.008);
        model.lacunarityProperty().set(2.0);
        model.gainProperty().set(0.6);
        model.colorGradientProperty().set(NoisePreviewModel.ColorGradient.GRAYSCALE);
        model.getTransforms().clear();
        noiseCanvas.requestRender();
    }

    private void applyMarblePreset() {
        model.noiseTypeProperty().set(com.cognitivedynamics.noisegen.FastNoiseLite.NoiseType.OpenSimplex2);
        model.fractalTypeProperty().set(com.cognitivedynamics.noisegen.FastNoiseLite.FractalType.Ridged);
        model.octavesProperty().set(4);
        model.frequencyProperty().set(0.01);
        model.domainWarpEnabledProperty().set(true);
        model.domainWarpAmpProperty().set(30.0);
        model.colorGradientProperty().set(NoisePreviewModel.ColorGradient.GRAYSCALE);
        model.getTransforms().clear();
        noiseCanvas.requestRender();
    }

    private void applyWoodPreset() {
        model.noiseTypeProperty().set(com.cognitivedynamics.noisegen.FastNoiseLite.NoiseType.OpenSimplex2);
        model.fractalTypeProperty().set(com.cognitivedynamics.noisegen.FastNoiseLite.FractalType.None);
        model.frequencyProperty().set(0.02);
        model.domainWarpEnabledProperty().set(false);
        model.colorGradientProperty().set(NoisePreviewModel.ColorGradient.TERRAIN);
        model.getTransforms().clear();
        model.getTransforms().add(new NoisePreviewModel.TransformEntry(
                NoisePreviewModel.TransformEntry.TransformType.TERRACE));
        model.getTransforms().get(0).intParamProperty().set(12);
        noiseCanvas.requestRender();
    }

    private void applyCellsPreset() {
        model.noiseTypeProperty().set(com.cognitivedynamics.noisegen.FastNoiseLite.NoiseType.Cellular);
        model.fractalTypeProperty().set(com.cognitivedynamics.noisegen.FastNoiseLite.FractalType.None);
        model.frequencyProperty().set(0.03);
        model.cellularDistanceProperty().set(com.cognitivedynamics.noisegen.FastNoiseLite.CellularDistanceFunction.EuclideanSq);
        model.cellularReturnProperty().set(com.cognitivedynamics.noisegen.FastNoiseLite.CellularReturnType.Distance);
        model.cellularJitterProperty().set(1.0);
        model.domainWarpEnabledProperty().set(false);
        model.colorGradientProperty().set(NoisePreviewModel.ColorGradient.CUSTOM);
        model.getTransforms().clear();
        noiseCanvas.requestRender();
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("FastNoiseLite Nouveau Preview Tool");
        alert.setContentText("""
                Version 1.1.1

                A modular Java refactoring of FastNoiseLite with extensions
                for procedural content generation.

                Original FastNoiseLite by Jordan Peck
                https://github.com/Auburn/FastNoiseLite

                Licensed under MIT License
                """);
        alert.showAndWait();
    }

    /**
     * Cleanup resources.
     */
    public void shutdown() {
        renderer.shutdown();
    }
}
