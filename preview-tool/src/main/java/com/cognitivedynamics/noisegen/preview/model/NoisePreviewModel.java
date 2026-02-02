package com.cognitivedynamics.noisegen.preview.model;

import com.cognitivedynamics.noisegen.FastNoiseLite;
import com.cognitivedynamics.noisegen.transforms.*;
import com.cognitivedynamics.noisegen.spatial.*;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;

/**
 * Observable model for the noise preview tool.
 * Contains all configurable parameters with JavaFX properties for binding.
 */
public class NoisePreviewModel {

    // ==================== General Settings ====================

    private final ObjectProperty<FastNoiseLite.NoiseType> noiseType =
            new SimpleObjectProperty<>(FastNoiseLite.NoiseType.OpenSimplex2);

    private final IntegerProperty seed = new SimpleIntegerProperty(1337);

    private final DoubleProperty frequency = new SimpleDoubleProperty(0.01);

    // ==================== Fractal Settings ====================

    private final ObjectProperty<FastNoiseLite.FractalType> fractalType =
            new SimpleObjectProperty<>(FastNoiseLite.FractalType.None);

    private final IntegerProperty octaves = new SimpleIntegerProperty(3);

    private final DoubleProperty lacunarity = new SimpleDoubleProperty(2.0);

    private final DoubleProperty gain = new SimpleDoubleProperty(0.5);

    private final DoubleProperty weightedStrength = new SimpleDoubleProperty(0.0);

    private final DoubleProperty pingPongStrength = new SimpleDoubleProperty(2.0);

    // ==================== Cellular Settings ====================

    private final ObjectProperty<FastNoiseLite.CellularDistanceFunction> cellularDistance =
            new SimpleObjectProperty<>(FastNoiseLite.CellularDistanceFunction.EuclideanSq);

    private final ObjectProperty<FastNoiseLite.CellularReturnType> cellularReturn =
            new SimpleObjectProperty<>(FastNoiseLite.CellularReturnType.Distance);

    private final DoubleProperty cellularJitter = new SimpleDoubleProperty(1.0);

    // ==================== Domain Warp Settings ====================

    private final ObjectProperty<FastNoiseLite.DomainWarpType> domainWarpType =
            new SimpleObjectProperty<>(FastNoiseLite.DomainWarpType.OpenSimplex2);

    private final DoubleProperty domainWarpAmp = new SimpleDoubleProperty(1.0);

    private final BooleanProperty domainWarpEnabled = new SimpleBooleanProperty(false);

    // ==================== 3D Rotation ====================

    private final ObjectProperty<FastNoiseLite.RotationType3D> rotationType3D =
            new SimpleObjectProperty<>(FastNoiseLite.RotationType3D.None);

    // ==================== Visualization Settings ====================

    public enum VisualizationMode {
        MODE_2D("2D"),
        MODE_3D_SLICE("3D Slice"),
        MODE_4D_ANIM("4D Animation");

        private final String displayName;

        VisualizationMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum ColorGradient {
        GRAYSCALE("Grayscale"),
        TERRAIN("Terrain"),
        HEAT("Heat Map"),
        OCEAN("Ocean Depth"),
        CUSTOM("Custom");

        private final String displayName;

        ColorGradient(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final ObjectProperty<VisualizationMode> visualizationMode =
            new SimpleObjectProperty<>(VisualizationMode.MODE_2D);

    private final ObjectProperty<ColorGradient> colorGradient =
            new SimpleObjectProperty<>(ColorGradient.GRAYSCALE);

    private final DoubleProperty zSlice = new SimpleDoubleProperty(0.0);

    private final DoubleProperty wTime = new SimpleDoubleProperty(0.0);

    private final BooleanProperty animating = new SimpleBooleanProperty(false);

    private final DoubleProperty animationSpeed = new SimpleDoubleProperty(1.0);

    // ==================== Canvas Settings ====================

    private final IntegerProperty canvasWidth = new SimpleIntegerProperty(512);

    private final IntegerProperty canvasHeight = new SimpleIntegerProperty(512);

    private final DoubleProperty viewOffsetX = new SimpleDoubleProperty(0.0);

    private final DoubleProperty viewOffsetY = new SimpleDoubleProperty(0.0);

    private final DoubleProperty zoom = new SimpleDoubleProperty(1.0);

    // ==================== Transform Pipeline ====================

    public static class TransformEntry {
        private final ObjectProperty<TransformType> type;
        private final DoubleProperty param1;
        private final DoubleProperty param2;
        private final IntegerProperty intParam;

        public enum TransformType {
            RANGE("Range", "Min", "Max"),
            POWER("Power", "Exponent", null),
            RIDGE("Ridge", "Exponent", null),
            TURBULENCE("Turbulence", "Power", null),
            CLAMP("Clamp", "Min", "Max"),
            INVERT("Invert", null, null),
            TERRACE("Terrace", null, null),
            QUANTIZE("Quantize", null, null);

            private final String displayName;
            private final String param1Name;
            private final String param2Name;

            TransformType(String displayName, String param1Name, String param2Name) {
                this.displayName = displayName;
                this.param1Name = param1Name;
                this.param2Name = param2Name;
            }

            public String getDisplayName() { return displayName; }
            public String getParam1Name() { return param1Name; }
            public String getParam2Name() { return param2Name; }
            public boolean hasParam1() { return param1Name != null; }
            public boolean hasParam2() { return param2Name != null; }
            public boolean hasIntParam() { return this == TERRACE || this == QUANTIZE; }

            @Override
            public String toString() { return displayName; }
        }

        public TransformEntry(TransformType type) {
            this.type = new SimpleObjectProperty<>(type);
            this.param1 = new SimpleDoubleProperty(getDefaultParam1(type));
            this.param2 = new SimpleDoubleProperty(getDefaultParam2(type));
            this.intParam = new SimpleIntegerProperty(getDefaultIntParam(type));
        }

        private double getDefaultParam1(TransformType type) {
            return switch (type) {
                case RANGE -> 0.0;
                case POWER, RIDGE -> 1.0;
                case TURBULENCE -> 2.0;
                case CLAMP -> -1.0;
                default -> 0.0;
            };
        }

        private double getDefaultParam2(TransformType type) {
            return switch (type) {
                case RANGE, CLAMP -> 1.0;
                default -> 0.0;
            };
        }

        private int getDefaultIntParam(TransformType type) {
            return switch (type) {
                case TERRACE -> 8;
                case QUANTIZE -> 16;
                default -> 1;
            };
        }

        public ObjectProperty<TransformType> typeProperty() { return type; }
        public DoubleProperty param1Property() { return param1; }
        public DoubleProperty param2Property() { return param2; }
        public IntegerProperty intParamProperty() { return intParam; }

        public TransformType getType() { return type.get(); }
        public double getParam1() { return param1.get(); }
        public double getParam2() { return param2.get(); }
        public int getIntParam() { return intParam.get(); }

        public NoiseTransform createTransform() {
            return switch (type.get()) {
                case RANGE -> new RangeTransform(-1f, 1f, (float) param1.get(), (float) param2.get());
                case POWER -> new PowerTransform((float) param1.get());
                case RIDGE -> new RidgeTransform(false, (float) param1.get());
                case TURBULENCE -> new TurbulenceTransform((float) param1.get(), 0f);
                case CLAMP -> new ClampTransform((float) param1.get(), (float) param2.get());
                case INVERT -> new InvertTransform();
                case TERRACE -> new TerraceTransform(intParam.get());
                case QUANTIZE -> new QuantizeTransform(intParam.get());
            };
        }
    }

    private final ObservableList<TransformEntry> transforms = FXCollections.observableArrayList();

    // ==================== Spatial Utilities ====================

    public enum SpatialMode {
        NONE("None"),
        CHUNKED("Chunked"),
        LOD("LOD"),
        TILED("Tiled"),
        DOUBLE_PRECISION("Double Precision"),
        HIERARCHICAL("Hierarchical"),
        TURBULENCE("Turbulence/Curl");

        private final String displayName;

        SpatialMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final ObjectProperty<SpatialMode> spatialMode =
            new SimpleObjectProperty<>(SpatialMode.NONE);

    // Chunked noise params
    private final DoubleProperty chunkSize = new SimpleDoubleProperty(1000.0);

    // LOD params
    private final IntegerProperty lodLevels = new SimpleIntegerProperty(4);
    private final DoubleProperty lodDistance = new SimpleDoubleProperty(100.0);

    // Tiled params
    private final IntegerProperty tileWidth = new SimpleIntegerProperty(256);
    private final IntegerProperty tileHeight = new SimpleIntegerProperty(256);

    // Hierarchical params
    private final IntegerProperty hierarchyLevels = new SimpleIntegerProperty(3);

    // ==================== Statistics (read-only) ====================

    private final DoubleProperty minValue = new SimpleDoubleProperty(0.0);
    private final DoubleProperty maxValue = new SimpleDoubleProperty(0.0);
    private final DoubleProperty avgValue = new SimpleDoubleProperty(0.0);
    private final DoubleProperty renderTimeMs = new SimpleDoubleProperty(0.0);
    private final DoubleProperty pointsPerSecond = new SimpleDoubleProperty(0.0);

    // ==================== Property Accessors ====================

    // General
    public ObjectProperty<FastNoiseLite.NoiseType> noiseTypeProperty() { return noiseType; }
    public IntegerProperty seedProperty() { return seed; }
    public DoubleProperty frequencyProperty() { return frequency; }

    // Fractal
    public ObjectProperty<FastNoiseLite.FractalType> fractalTypeProperty() { return fractalType; }
    public IntegerProperty octavesProperty() { return octaves; }
    public DoubleProperty lacunarityProperty() { return lacunarity; }
    public DoubleProperty gainProperty() { return gain; }
    public DoubleProperty weightedStrengthProperty() { return weightedStrength; }
    public DoubleProperty pingPongStrengthProperty() { return pingPongStrength; }

    // Cellular
    public ObjectProperty<FastNoiseLite.CellularDistanceFunction> cellularDistanceProperty() { return cellularDistance; }
    public ObjectProperty<FastNoiseLite.CellularReturnType> cellularReturnProperty() { return cellularReturn; }
    public DoubleProperty cellularJitterProperty() { return cellularJitter; }

    // Domain Warp
    public ObjectProperty<FastNoiseLite.DomainWarpType> domainWarpTypeProperty() { return domainWarpType; }
    public DoubleProperty domainWarpAmpProperty() { return domainWarpAmp; }
    public BooleanProperty domainWarpEnabledProperty() { return domainWarpEnabled; }

    // 3D Rotation
    public ObjectProperty<FastNoiseLite.RotationType3D> rotationType3DProperty() { return rotationType3D; }

    // Visualization
    public ObjectProperty<VisualizationMode> visualizationModeProperty() { return visualizationMode; }
    public ObjectProperty<ColorGradient> colorGradientProperty() { return colorGradient; }
    public DoubleProperty zSliceProperty() { return zSlice; }
    public DoubleProperty wTimeProperty() { return wTime; }
    public BooleanProperty animatingProperty() { return animating; }
    public DoubleProperty animationSpeedProperty() { return animationSpeed; }

    // Canvas
    public IntegerProperty canvasWidthProperty() { return canvasWidth; }
    public IntegerProperty canvasHeightProperty() { return canvasHeight; }
    public DoubleProperty viewOffsetXProperty() { return viewOffsetX; }
    public DoubleProperty viewOffsetYProperty() { return viewOffsetY; }
    public DoubleProperty zoomProperty() { return zoom; }

    // Transforms
    public ObservableList<TransformEntry> getTransforms() { return transforms; }

    // Spatial
    public ObjectProperty<SpatialMode> spatialModeProperty() { return spatialMode; }
    public DoubleProperty chunkSizeProperty() { return chunkSize; }
    public IntegerProperty lodLevelsProperty() { return lodLevels; }
    public DoubleProperty lodDistanceProperty() { return lodDistance; }
    public IntegerProperty tileWidthProperty() { return tileWidth; }
    public IntegerProperty tileHeightProperty() { return tileHeight; }
    public IntegerProperty hierarchyLevelsProperty() { return hierarchyLevels; }

    // Statistics
    public DoubleProperty minValueProperty() { return minValue; }
    public DoubleProperty maxValueProperty() { return maxValue; }
    public DoubleProperty avgValueProperty() { return avgValue; }
    public DoubleProperty renderTimeMsProperty() { return renderTimeMs; }
    public DoubleProperty pointsPerSecondProperty() { return pointsPerSecond; }

    // ==================== Convenience Getters ====================

    public FastNoiseLite.NoiseType getNoiseType() { return noiseType.get(); }
    public int getSeed() { return seed.get(); }
    public double getFrequency() { return frequency.get(); }
    public FastNoiseLite.FractalType getFractalType() { return fractalType.get(); }
    public int getOctaves() { return octaves.get(); }
    public double getLacunarity() { return lacunarity.get(); }
    public double getGain() { return gain.get(); }
    public double getWeightedStrength() { return weightedStrength.get(); }
    public double getPingPongStrength() { return pingPongStrength.get(); }
    public FastNoiseLite.CellularDistanceFunction getCellularDistance() { return cellularDistance.get(); }
    public FastNoiseLite.CellularReturnType getCellularReturn() { return cellularReturn.get(); }
    public double getCellularJitter() { return cellularJitter.get(); }
    public FastNoiseLite.DomainWarpType getDomainWarpType() { return domainWarpType.get(); }
    public double getDomainWarpAmp() { return domainWarpAmp.get(); }
    public boolean isDomainWarpEnabled() { return domainWarpEnabled.get(); }
    public FastNoiseLite.RotationType3D getRotationType3D() { return rotationType3D.get(); }
    public VisualizationMode getVisualizationMode() { return visualizationMode.get(); }
    public ColorGradient getColorGradient() { return colorGradient.get(); }
    public double getZSlice() { return zSlice.get(); }
    public double getWTime() { return wTime.get(); }
    public boolean isAnimating() { return animating.get(); }
    public double getAnimationSpeed() { return animationSpeed.get(); }
    public int getCanvasWidth() { return canvasWidth.get(); }
    public int getCanvasHeight() { return canvasHeight.get(); }
    public double getViewOffsetX() { return viewOffsetX.get(); }
    public double getViewOffsetY() { return viewOffsetY.get(); }
    public double getZoom() { return zoom.get(); }
    public SpatialMode getSpatialMode() { return spatialMode.get(); }
    public double getChunkSize() { return chunkSize.get(); }
    public int getLodLevels() { return lodLevels.get(); }
    public double getLodDistance() { return lodDistance.get(); }
    public int getTileWidth() { return tileWidth.get(); }
    public int getTileHeight() { return tileHeight.get(); }
    public int getHierarchyLevels() { return hierarchyLevels.get(); }

    // ==================== FastNoiseLite Configuration ====================

    /**
     * Configure a FastNoiseLite instance with current model settings.
     */
    public void configureNoise(FastNoiseLite noise) {
        noise.SetSeed(seed.get());
        noise.SetFrequency((float) frequency.get());
        noise.SetNoiseType(noiseType.get());
        noise.SetFractalType(fractalType.get());
        noise.SetFractalOctaves(octaves.get());
        noise.SetFractalLacunarity((float) lacunarity.get());
        noise.SetFractalGain((float) gain.get());
        noise.SetFractalWeightedStrength((float) weightedStrength.get());
        noise.SetFractalPingPongStrength((float) pingPongStrength.get());
        noise.SetCellularDistanceFunction(cellularDistance.get());
        noise.SetCellularReturnType(cellularReturn.get());
        noise.SetCellularJitter((float) cellularJitter.get());
        noise.SetDomainWarpType(domainWarpType.get());
        noise.SetDomainWarpAmp((float) domainWarpAmp.get());
        noise.SetRotationType3D(rotationType3D.get());
    }

    /**
     * Build a chained transform from the current transform pipeline.
     */
    public NoiseTransform buildTransformPipeline() {
        if (transforms.isEmpty()) {
            return null;
        }
        if (transforms.size() == 1) {
            return transforms.get(0).createTransform();
        }
        List<NoiseTransform> list = new ArrayList<>();
        for (TransformEntry entry : transforms) {
            list.add(entry.createTransform());
        }
        return new ChainedTransform(list.toArray(new NoiseTransform[0]));
    }

    /**
     * Generate Java code snippet for current configuration.
     */
    public String generateCodeSnippet() {
        StringBuilder sb = new StringBuilder();
        sb.append("FastNoiseLite noise = new FastNoiseLite(").append(seed.get()).append(");\n");
        sb.append("noise.SetNoiseType(FastNoiseLite.NoiseType.").append(noiseType.get().name()).append(");\n");
        sb.append("noise.SetFrequency(").append(frequency.get()).append("f);\n");

        if (fractalType.get() != FastNoiseLite.FractalType.None) {
            sb.append("noise.SetFractalType(FastNoiseLite.FractalType.").append(fractalType.get().name()).append(");\n");
            sb.append("noise.SetFractalOctaves(").append(octaves.get()).append(");\n");
            sb.append("noise.SetFractalLacunarity(").append(lacunarity.get()).append("f);\n");
            sb.append("noise.SetFractalGain(").append(gain.get()).append("f);\n");
            if (weightedStrength.get() != 0.0) {
                sb.append("noise.SetFractalWeightedStrength(").append(weightedStrength.get()).append("f);\n");
            }
            if (fractalType.get() == FastNoiseLite.FractalType.PingPong) {
                sb.append("noise.SetFractalPingPongStrength(").append(pingPongStrength.get()).append("f);\n");
            }
        }

        if (noiseType.get() == FastNoiseLite.NoiseType.Cellular) {
            sb.append("noise.SetCellularDistanceFunction(FastNoiseLite.CellularDistanceFunction.").append(cellularDistance.get().name()).append(");\n");
            sb.append("noise.SetCellularReturnType(FastNoiseLite.CellularReturnType.").append(cellularReturn.get().name()).append(");\n");
            if (cellularJitter.get() != 1.0) {
                sb.append("noise.SetCellularJitter(").append(cellularJitter.get()).append("f);\n");
            }
        }

        if (rotationType3D.get() != FastNoiseLite.RotationType3D.None) {
            sb.append("noise.SetRotationType3D(FastNoiseLite.RotationType3D.").append(rotationType3D.get().name()).append(");\n");
        }

        if (domainWarpEnabled.get()) {
            sb.append("\n// Domain Warp\n");
            sb.append("noise.SetDomainWarpType(FastNoiseLite.DomainWarpType.").append(domainWarpType.get().name()).append(");\n");
            sb.append("noise.SetDomainWarpAmp(").append(domainWarpAmp.get()).append("f);\n");
        }

        if (!transforms.isEmpty()) {
            sb.append("\n// Transform Pipeline\n");
            for (int i = 0; i < transforms.size(); i++) {
                TransformEntry entry = transforms.get(i);
                String varName = "transform" + (i + 1);
                sb.append("NoiseTransform ").append(varName).append(" = ");
                sb.append(switch (entry.getType()) {
                    case RANGE -> String.format("new RangeTransform(-1f, 1f, %sf, %sf)", entry.getParam1(), entry.getParam2());
                    case POWER -> String.format("new PowerTransform(%sf)", entry.getParam1());
                    case RIDGE -> String.format("new RidgeTransform(false, %sf)", entry.getParam1());
                    case TURBULENCE -> String.format("new TurbulenceTransform(%sf, 0f)", entry.getParam1());
                    case CLAMP -> String.format("new ClampTransform(%sf, %sf)", entry.getParam1(), entry.getParam2());
                    case INVERT -> "new InvertTransform()";
                    case TERRACE -> String.format("new TerraceTransform(%d)", entry.getIntParam());
                    case QUANTIZE -> String.format("new QuantizeTransform(%d)", entry.getIntParam());
                });
                sb.append(";\n");
            }
        }

        sb.append("\n// Generate noise\n");
        sb.append("float value = noise.GetNoise(x, y);\n");

        if (!transforms.isEmpty()) {
            sb.append("value = transform1.apply(value);\n");
        }

        return sb.toString();
    }

    /**
     * Update statistics after rendering.
     */
    public void updateStats(double min, double max, double avg, double timeMs, double pps) {
        minValue.set(min);
        maxValue.set(max);
        avgValue.set(avg);
        renderTimeMs.set(timeMs);
        pointsPerSecond.set(pps);
    }
}
