package com.cognitivedynamics.noisegen.preview.view;

import com.cognitivedynamics.noisegen.FastNoiseLite;
import com.cognitivedynamics.noisegen.preview.model.NoisePreviewModel;
import com.cognitivedynamics.noisegen.preview.model.NoisePreviewModel.*;
import com.cognitivedynamics.noisegen.preview.util.NoiseRenderer;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.util.function.Consumer;

/**
 * Left sidebar control panel with all noise settings.
 */
public class ControlPanel extends ScrollPane {

    private final NoisePreviewModel model;
    private final VBox content;
    private Consumer<Void> onSettingsChanged;

    public ControlPanel(NoisePreviewModel model) {
        this.model = model;

        content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getStyleClass().add("control-panel");

        setContent(content);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        setPrefWidth(300);
        setMinWidth(280);

        buildUI();
    }

    public void setOnSettingsChanged(Consumer<Void> callback) {
        this.onSettingsChanged = callback;
    }

    private void notifyChanged() {
        if (onSettingsChanged != null) {
            onSettingsChanged.accept(null);
        }
    }

    private void buildUI() {
        content.getChildren().addAll(
                createGeneralSection(),
                createFractalSection(),
                createCellularSection(),
                createDomainWarpSection(),
                createRotationSection(),
                createTransformsSection(),
                createSpatialSection(),
                createColorSection(),
                createVersionLabel()
        );
    }

    // ==================== General Section ====================

    private TitledPane createGeneralSection() {
        GridPane grid = createGrid();
        int row = 0;

        // Noise Type
        Label typeLabel = new Label("Noise Type:");
        ComboBox<FastNoiseLite.NoiseType> typeCombo = new ComboBox<>(
                FXCollections.observableArrayList(FastNoiseLite.NoiseType.values())
        );
        typeCombo.valueProperty().bindBidirectional(model.noiseTypeProperty());
        typeCombo.valueProperty().addListener((obs, old, val) -> notifyChanged());
        grid.addRow(row++, typeLabel, typeCombo);

        // Seed
        Label seedLabel = new Label("Seed:");
        Spinner<Integer> seedSpinner = new Spinner<>(-999999999, 999999999, model.getSeed());
        seedSpinner.setEditable(true);
        seedSpinner.getValueFactory().valueProperty().addListener((obs, old, val) -> {
            model.seedProperty().set(val);
            notifyChanged();
        });
        Button randomSeed = new Button("\uD83C\uDFB2");
        randomSeed.setTooltip(new Tooltip("Random seed"));
        randomSeed.setOnAction(e -> {
            int newSeed = (int) (Math.random() * Integer.MAX_VALUE);
            seedSpinner.getValueFactory().setValue(newSeed);
        });
        HBox seedBox = new HBox(5, seedSpinner, randomSeed);
        grid.addRow(row++, seedLabel, seedBox);

        // Frequency
        Label freqLabel = new Label("Frequency:");
        Slider freqSlider = createLogSlider(0.001, 1.0, model.getFrequency());
        Label freqValue = new Label();
        freqValue.textProperty().bind(Bindings.format("%.4f", freqSlider.valueProperty()));
        freqSlider.valueProperty().bindBidirectional(model.frequencyProperty());
        freqSlider.valueProperty().addListener((obs, old, val) -> notifyChanged());
        HBox freqBox = new HBox(5, freqSlider, freqValue);
        HBox.setHgrow(freqSlider, Priority.ALWAYS);
        grid.addRow(row++, freqLabel, freqBox);

        TitledPane pane = new TitledPane("General", grid);
        pane.setExpanded(true);
        return pane;
    }

    // ==================== Fractal Section ====================

    private TitledPane createFractalSection() {
        GridPane grid = createGrid();
        int row = 0;

        // Fractal Type
        Label typeLabel = new Label("Type:");
        ComboBox<FastNoiseLite.FractalType> typeCombo = new ComboBox<>(
                FXCollections.observableArrayList(FastNoiseLite.FractalType.values())
        );
        typeCombo.valueProperty().bindBidirectional(model.fractalTypeProperty());
        typeCombo.valueProperty().addListener((obs, old, val) -> notifyChanged());
        grid.addRow(row++, typeLabel, typeCombo);

        // Octaves
        Label octLabel = new Label("Octaves:");
        Slider octSlider = new Slider(1, 10, model.getOctaves());
        octSlider.setMajorTickUnit(1);
        octSlider.setMinorTickCount(0);
        octSlider.setSnapToTicks(true);
        octSlider.setShowTickMarks(true);
        Label octValue = new Label(String.valueOf(model.getOctaves()));
        octSlider.valueProperty().addListener((obs, old, val) -> {
            model.octavesProperty().set(val.intValue());
            octValue.setText(String.valueOf(val.intValue()));
            notifyChanged();
        });
        HBox octBox = new HBox(5, octSlider, octValue);
        HBox.setHgrow(octSlider, Priority.ALWAYS);
        grid.addRow(row++, octLabel, octBox);

        // Lacunarity
        Label lacLabel = new Label("Lacunarity:");
        Slider lacSlider = new Slider(1.0, 4.0, model.getLacunarity());
        Label lacValue = new Label();
        lacValue.textProperty().bind(Bindings.format("%.2f", lacSlider.valueProperty()));
        lacSlider.valueProperty().bindBidirectional(model.lacunarityProperty());
        lacSlider.valueProperty().addListener((obs, old, val) -> notifyChanged());
        HBox lacBox = new HBox(5, lacSlider, lacValue);
        HBox.setHgrow(lacSlider, Priority.ALWAYS);
        grid.addRow(row++, lacLabel, lacBox);

        // Gain
        Label gainLabel = new Label("Gain:");
        Slider gainSlider = new Slider(0.0, 1.0, model.getGain());
        Label gainValue = new Label();
        gainValue.textProperty().bind(Bindings.format("%.2f", gainSlider.valueProperty()));
        gainSlider.valueProperty().bindBidirectional(model.gainProperty());
        gainSlider.valueProperty().addListener((obs, old, val) -> notifyChanged());
        HBox gainBox = new HBox(5, gainSlider, gainValue);
        HBox.setHgrow(gainSlider, Priority.ALWAYS);
        grid.addRow(row++, gainLabel, gainBox);

        // Weighted Strength
        Label wsLabel = new Label("Weighted Str:");
        Slider wsSlider = new Slider(0.0, 1.0, model.getWeightedStrength());
        Label wsValue = new Label();
        wsValue.textProperty().bind(Bindings.format("%.2f", wsSlider.valueProperty()));
        wsSlider.valueProperty().bindBidirectional(model.weightedStrengthProperty());
        wsSlider.valueProperty().addListener((obs, old, val) -> notifyChanged());
        HBox wsBox = new HBox(5, wsSlider, wsValue);
        HBox.setHgrow(wsSlider, Priority.ALWAYS);
        grid.addRow(row++, wsLabel, wsBox);

        // PingPong Strength (only visible for PingPong fractal)
        Label ppLabel = new Label("PingPong Str:");
        Slider ppSlider = new Slider(0.0, 5.0, model.getPingPongStrength());
        Label ppValue = new Label();
        ppValue.textProperty().bind(Bindings.format("%.2f", ppSlider.valueProperty()));
        ppSlider.valueProperty().bindBidirectional(model.pingPongStrengthProperty());
        ppSlider.valueProperty().addListener((obs, old, val) -> notifyChanged());
        HBox ppBox = new HBox(5, ppSlider, ppValue);
        HBox.setHgrow(ppSlider, Priority.ALWAYS);
        grid.addRow(row++, ppLabel, ppBox);

        // Visibility based on fractal type
        model.fractalTypeProperty().addListener((obs, old, type) -> {
            boolean hasFractal = type != FastNoiseLite.FractalType.None;
            octSlider.setDisable(!hasFractal);
            lacSlider.setDisable(!hasFractal);
            gainSlider.setDisable(!hasFractal);
            wsSlider.setDisable(!hasFractal);
            ppSlider.setDisable(type != FastNoiseLite.FractalType.PingPong);
        });

        TitledPane pane = new TitledPane("Fractal", grid);
        pane.setExpanded(true);
        return pane;
    }

    // ==================== Cellular Section ====================

    private TitledPane createCellularSection() {
        GridPane grid = createGrid();
        int row = 0;

        // Distance Function
        Label distLabel = new Label("Distance:");
        ComboBox<FastNoiseLite.CellularDistanceFunction> distCombo = new ComboBox<>(
                FXCollections.observableArrayList(FastNoiseLite.CellularDistanceFunction.values())
        );
        distCombo.valueProperty().bindBidirectional(model.cellularDistanceProperty());
        distCombo.valueProperty().addListener((obs, old, val) -> notifyChanged());
        grid.addRow(row++, distLabel, distCombo);

        // Return Type
        Label retLabel = new Label("Return:");
        ComboBox<FastNoiseLite.CellularReturnType> retCombo = new ComboBox<>(
                FXCollections.observableArrayList(FastNoiseLite.CellularReturnType.values())
        );
        retCombo.valueProperty().bindBidirectional(model.cellularReturnProperty());
        retCombo.valueProperty().addListener((obs, old, val) -> notifyChanged());
        grid.addRow(row++, retLabel, retCombo);

        // Jitter
        Label jitLabel = new Label("Jitter:");
        Slider jitSlider = new Slider(0.0, 1.0, model.getCellularJitter());
        Label jitValue = new Label();
        jitValue.textProperty().bind(Bindings.format("%.2f", jitSlider.valueProperty()));
        jitSlider.valueProperty().bindBidirectional(model.cellularJitterProperty());
        jitSlider.valueProperty().addListener((obs, old, val) -> notifyChanged());
        HBox jitBox = new HBox(5, jitSlider, jitValue);
        HBox.setHgrow(jitSlider, Priority.ALWAYS);
        grid.addRow(row++, jitLabel, jitBox);

        TitledPane pane = new TitledPane("Cellular", grid);
        pane.setExpanded(false);

        // Show/hide based on noise type
        model.noiseTypeProperty().addListener((obs, old, type) -> {
            pane.setExpanded(type == FastNoiseLite.NoiseType.Cellular);
            pane.setDisable(type != FastNoiseLite.NoiseType.Cellular);
        });
        pane.setDisable(model.getNoiseType() != FastNoiseLite.NoiseType.Cellular);

        return pane;
    }

    // ==================== Domain Warp Section ====================

    private TitledPane createDomainWarpSection() {
        GridPane grid = createGrid();
        int row = 0;

        // Enable checkbox
        CheckBox enableCheck = new CheckBox("Enable Domain Warp");
        enableCheck.selectedProperty().bindBidirectional(model.domainWarpEnabledProperty());
        enableCheck.selectedProperty().addListener((obs, old, val) -> notifyChanged());
        grid.add(enableCheck, 0, row++, 2, 1);

        // Warp Type
        Label typeLabel = new Label("Type:");
        ComboBox<FastNoiseLite.DomainWarpType> typeCombo = new ComboBox<>(
                FXCollections.observableArrayList(FastNoiseLite.DomainWarpType.values())
        );
        typeCombo.valueProperty().bindBidirectional(model.domainWarpTypeProperty());
        typeCombo.valueProperty().addListener((obs, old, val) -> notifyChanged());
        typeCombo.disableProperty().bind(model.domainWarpEnabledProperty().not());
        grid.addRow(row++, typeLabel, typeCombo);

        // Amplitude
        Label ampLabel = new Label("Amplitude:");
        Slider ampSlider = new Slider(0.0, 100.0, model.getDomainWarpAmp());
        Label ampValue = new Label();
        ampValue.textProperty().bind(Bindings.format("%.1f", ampSlider.valueProperty()));
        ampSlider.valueProperty().bindBidirectional(model.domainWarpAmpProperty());
        ampSlider.valueProperty().addListener((obs, old, val) -> notifyChanged());
        ampSlider.disableProperty().bind(model.domainWarpEnabledProperty().not());
        HBox ampBox = new HBox(5, ampSlider, ampValue);
        HBox.setHgrow(ampSlider, Priority.ALWAYS);
        grid.addRow(row++, ampLabel, ampBox);

        TitledPane pane = new TitledPane("Domain Warp", grid);
        pane.setExpanded(false);
        return pane;
    }

    // ==================== 3D Rotation Section ====================

    private TitledPane createRotationSection() {
        GridPane grid = createGrid();

        Label rotLabel = new Label("3D Rotation:");
        ComboBox<FastNoiseLite.RotationType3D> rotCombo = new ComboBox<>(
                FXCollections.observableArrayList(FastNoiseLite.RotationType3D.values())
        );
        rotCombo.valueProperty().bindBidirectional(model.rotationType3DProperty());
        rotCombo.valueProperty().addListener((obs, old, val) -> notifyChanged());
        grid.addRow(0, rotLabel, rotCombo);

        TitledPane pane = new TitledPane("3D Rotation", grid);
        pane.setExpanded(false);
        return pane;
    }

    // ==================== Transforms Section ====================

    private TitledPane createTransformsSection() {
        VBox box = new VBox(10);

        // Transform list
        ListView<TransformEntry> listView = new ListView<>(model.getTransforms());
        listView.setPrefHeight(150);
        listView.setCellFactory(lv -> new TransformListCell());

        // Add button
        ComboBox<TransformEntry.TransformType> addCombo = new ComboBox<>(
                FXCollections.observableArrayList(TransformEntry.TransformType.values())
        );
        addCombo.setPromptText("Add transform...");
        addCombo.setOnAction(e -> {
            TransformEntry.TransformType type = addCombo.getValue();
            if (type != null) {
                model.getTransforms().add(new TransformEntry(type));
                addCombo.setValue(null);
                notifyChanged();
            }
        });

        // Buttons
        Button removeBtn = new Button("Remove");
        removeBtn.setOnAction(e -> {
            TransformEntry selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                model.getTransforms().remove(selected);
                notifyChanged();
            }
        });
        removeBtn.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());

        Button clearBtn = new Button("Clear All");
        clearBtn.setOnAction(e -> {
            model.getTransforms().clear();
            notifyChanged();
        });

        HBox buttons = new HBox(5, addCombo, removeBtn, clearBtn);

        box.getChildren().addAll(listView, buttons);

        TitledPane pane = new TitledPane("Transforms", box);
        pane.setExpanded(false);
        return pane;
    }

    private class TransformListCell extends ListCell<TransformEntry> {
        @Override
        protected void updateItem(TransformEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                String text = item.getType().getDisplayName();
                if (item.getType().hasParam1()) {
                    text += String.format(" (%.2f", item.getParam1());
                    if (item.getType().hasParam2()) {
                        text += String.format(", %.2f", item.getParam2());
                    }
                    text += ")";
                } else if (item.getType().hasIntParam()) {
                    text += String.format(" (%d)", item.getIntParam());
                }
                setText(text);
            }
        }
    }

    // ==================== Spatial Utilities Section ====================

    private TitledPane createSpatialSection() {
        VBox box = new VBox(10);

        // Mode selector
        Label modeLabel = new Label("Mode:");
        ComboBox<SpatialMode> modeCombo = new ComboBox<>(
                FXCollections.observableArrayList(SpatialMode.values())
        );
        modeCombo.valueProperty().bindBidirectional(model.spatialModeProperty());
        modeCombo.valueProperty().addListener((obs, old, val) -> notifyChanged());

        HBox modeBox = new HBox(10, modeLabel, modeCombo);

        // Mode-specific settings
        GridPane paramsGrid = createGrid();

        // Chunked params
        Label chunkLabel = new Label("Chunk Size:");
        Spinner<Double> chunkSpinner = new Spinner<>(100, 100000, model.getChunkSize(), 100);
        chunkSpinner.setEditable(true);
        chunkSpinner.getValueFactory().valueProperty().addListener((obs, old, val) -> {
            model.chunkSizeProperty().set(val);
            notifyChanged();
        });
        HBox chunkRow = new HBox(10, chunkLabel, chunkSpinner);

        // Tiled params
        Label tileWLabel = new Label("Tile Width:");
        Spinner<Integer> tileWSpinner = new Spinner<>(16, 1024, model.getTileWidth(), 16);
        tileWSpinner.getValueFactory().valueProperty().addListener((obs, old, val) -> {
            model.tileWidthProperty().set(val);
            notifyChanged();
        });

        Label tileHLabel = new Label("Tile Height:");
        Spinner<Integer> tileHSpinner = new Spinner<>(16, 1024, model.getTileHeight(), 16);
        tileHSpinner.getValueFactory().valueProperty().addListener((obs, old, val) -> {
            model.tileHeightProperty().set(val);
            notifyChanged();
        });
        HBox tiledRow = new HBox(10, tileWLabel, tileWSpinner, tileHLabel, tileHSpinner);

        // LOD params
        Label lodLevelLabel = new Label("LOD Levels:");
        Spinner<Integer> lodLevelSpinner = new Spinner<>(1, 10, model.getLodLevels());
        lodLevelSpinner.getValueFactory().valueProperty().addListener((obs, old, val) -> {
            model.lodLevelsProperty().set(val);
            notifyChanged();
        });
        HBox lodRow = new HBox(10, lodLevelLabel, lodLevelSpinner);

        // Hierarchical params
        Label hierLabel = new Label("Hierarchy Levels:");
        Spinner<Integer> hierSpinner = new Spinner<>(1, 8, model.getHierarchyLevels());
        hierSpinner.getValueFactory().valueProperty().addListener((obs, old, val) -> {
            model.hierarchyLevelsProperty().set(val);
            notifyChanged();
        });
        HBox hierRow = new HBox(10, hierLabel, hierSpinner);

        // Show/hide based on mode
        VBox paramsBox = new VBox(5);
        model.spatialModeProperty().addListener((obs, old, mode) -> {
            paramsBox.getChildren().clear();
            switch (mode) {
                case CHUNKED -> paramsBox.getChildren().add(chunkRow);
                case TILED -> paramsBox.getChildren().add(tiledRow);
                case LOD -> paramsBox.getChildren().add(lodRow);
                case HIERARCHICAL -> paramsBox.getChildren().add(hierRow);
                default -> {}
            }
        });

        box.getChildren().addAll(modeBox, paramsBox);

        TitledPane pane = new TitledPane("Spatial Utilities", box);
        pane.setExpanded(false);
        return pane;
    }

    // ==================== Color Section ====================

    private TitledPane createColorSection() {
        GridPane grid = createGrid();

        Label gradLabel = new Label("Color Gradient:");
        ComboBox<ColorGradient> gradCombo = new ComboBox<>(
                FXCollections.observableArrayList(ColorGradient.values())
        );
        gradCombo.valueProperty().bindBidirectional(model.colorGradientProperty());
        gradCombo.valueProperty().addListener((obs, old, val) -> notifyChanged());
        grid.addRow(0, gradLabel, gradCombo);

        TitledPane pane = new TitledPane("Visualization", grid);
        pane.setExpanded(false);
        return pane;
    }

    // ==================== Version Label ====================

    private Label createVersionLabel() {
        Label label = new Label("FastNoiseLite Nouveau v1.1.1");
        label.getStyleClass().add("version-label");
        label.setStyle("-fx-text-fill: #666; -fx-font-size: 10px;");
        return label;
    }

    // ==================== Helpers ====================

    private GridPane createGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(80);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        return grid;
    }

    private Slider createLogSlider(double min, double max, double value) {
        // For frequency, use logarithmic scale
        double logMin = Math.log10(min);
        double logMax = Math.log10(max);
        double logValue = Math.log10(value);

        Slider slider = new Slider(logMin, logMax, logValue);

        // Convert between log and linear
        slider.setLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Double val) {
                return String.format("%.4f", Math.pow(10, val));
            }

            @Override
            public Double fromString(String s) {
                return Math.log10(Double.parseDouble(s));
            }
        });

        // Update model with linear value
        slider.valueProperty().addListener((obs, old, val) -> {
            model.frequencyProperty().set(Math.pow(10, val.doubleValue()));
        });

        return slider;
    }
}
