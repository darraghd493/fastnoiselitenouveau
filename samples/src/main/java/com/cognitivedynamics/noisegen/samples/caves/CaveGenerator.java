package com.cognitivedynamics.noisegen.samples.caves;

import com.cognitivedynamics.noisegen.graph.NoiseGraph;
import com.cognitivedynamics.noisegen.graph.NoiseNode;
import com.cognitivedynamics.noisegen.NoiseTypes.CellularDistanceFunction;
import com.cognitivedynamics.noisegen.NoiseTypes.CellularReturnType;

/**
 * 3D Cave System Generator using layered noise.
 *
 * <p>Creates realistic underground cave networks with:
 * <ul>
 *   <li>Large caverns using low-frequency cellular noise</li>
 *   <li>Connecting tunnels using medium-frequency ridged noise</li>
 *   <li>Small passages and details using high-frequency FBm</li>
 *   <li>Stalactites/stalagmites using vertical bias</li>
 *   <li>Underground water pools at low elevations</li>
 * </ul>
 *
 * <p>The cave system uses a density field approach:
 * <ul>
 *   <li>Negative values = cave (air)</li>
 *   <li>Positive values = rock (solid)</li>
 *   <li>Zero = cave surface</li>
 * </ul>
 */
public class CaveGenerator {

    private final int seed;
    private final NoiseGraph graph;

    // Noise nodes for different cave features
    private final NoiseNode cavernNode;      // Large open spaces
    private final NoiseNode tunnelNode;      // Connecting passages
    private final NoiseNode detailNode;      // Small features
    private final NoiseNode formationNode;   // Stalactites/stalagmites

    // Configuration
    private final double cavernFrequency;
    private final double tunnelFrequency;
    private final double detailFrequency;
    private final double caveThreshold;      // Below this = cave
    private final double tunnelThreshold;    // Secondary threshold for tunnels

    /**
     * Create a cave generator with default settings.
     */
    public CaveGenerator(int seed) {
        // Default threshold of 0.0: negative density = cave, positive = rock
        this(seed, 0.02, 0.03, 0.1, 0.0, 0.2);
    }

    /**
     * Create a cave generator with custom settings.
     *
     * @param seed             Random seed
     * @param cavernFrequency  Frequency for large caverns (lower = bigger caves)
     * @param tunnelFrequency  Frequency for tunnel networks
     * @param detailFrequency  Frequency for small details
     * @param caveThreshold    Threshold for main caves (lower = more cave space)
     * @param tunnelThreshold  Threshold for tunnel connections
     */
    public CaveGenerator(int seed, double cavernFrequency, double tunnelFrequency,
                         double detailFrequency, double caveThreshold, double tunnelThreshold) {
        this.seed = seed;
        this.cavernFrequency = cavernFrequency;
        this.tunnelFrequency = tunnelFrequency;
        this.detailFrequency = detailFrequency;
        this.caveThreshold = caveThreshold;
        this.tunnelThreshold = tunnelThreshold;

        this.graph = NoiseGraph.create(seed);

        // Build cave components
        this.cavernNode = buildCaverns();
        this.tunnelNode = buildTunnels();
        this.detailNode = buildDetails();
        this.formationNode = buildFormations();
    }

    /**
     * Build large cavern spaces using cellular noise.
     */
    private NoiseNode buildCaverns() {
        // Cellular noise creates bubble-like cavern shapes
        // Distance2Sub returns 0 to ~0.7 (positive), so we invert and offset
        // to make cell centers negative (caves) and edges positive (rock)
        NoiseNode cells = graph.cellular(
            CellularDistanceFunction.EuclideanSq,
            CellularReturnType.Distance2Sub,  // Creates interesting cave-like shapes
            0.8  // Jitter
        ).frequency(cavernFrequency)
         .invert()      // Now: -0.7 to 0 (cell centers are most negative)
         .add(0.3);     // Now: -0.4 to 0.3 (cell centers still negative = caves)

        // Add some FBm variation
        NoiseNode variation = graph.fbm(
            graph.simplex().frequency(cavernFrequency * 0.5),
            3, 2.0, 0.5
        ).multiply(0.2);

        // Combine
        return cells.add(variation);
    }

    /**
     * Build tunnel networks using ridged noise.
     */
    private NoiseNode buildTunnels() {
        // Ridged noise creates narrow passage-like features
        // Values are 0 to 1, with ridges (edges) at high values
        NoiseNode ridges = graph.ridged(
            graph.simplex().frequency(tunnelFrequency),
            4, 2.0, 0.5
        );

        // Invert and offset so ridges become tunnels (negative = cave)
        // After invert: -1 to 0, then add offset to center around 0
        NoiseNode tunnels = ridges.invert().add(0.5);  // Now -0.5 to 0.5

        // Warp for organic paths
        NoiseNode warpSource = graph.simplex().frequency(tunnelFrequency * 0.3);
        return tunnels.warp(warpSource, 20.0);
    }

    /**
     * Build small detail features.
     */
    private NoiseNode buildDetails() {
        return graph.fbm(
            graph.simplex().frequency(detailFrequency),
            3, 2.0, 0.5
        ).multiply(0.15);  // Subtle detail
    }

    /**
     * Build stalactite/stalagmite formations.
     * These have vertical bias - more likely at top and bottom of caves.
     */
    private NoiseNode buildFormations() {
        // High frequency noise for spiky formations
        NoiseNode spikes = graph.ridged(
            graph.simplex().frequency(detailFrequency * 2),
            3, 2.5, 0.4
        ).multiply(0.2);

        return spikes;
    }

    /**
     * Sample the cave density at a 3D point.
     *
     * @param x X coordinate
     * @param y Y coordinate (vertical - 0 is surface, negative is deeper)
     * @param z Z coordinate
     * @return CaveData with density and material information
     */
    public CaveData sample(double x, double y, double z) {
        // Sample each layer
        double caverns = cavernNode.evaluate3D(seed, x, y, z);
        double tunnels = tunnelNode.evaluate3D(seed + 1000, x, y, z);
        double details = detailNode.evaluate3D(seed + 2000, x, y, z);
        double formations = formationNode.evaluate3D(seed + 3000, x, y, z);

        // Combine layers - use minimum so either caverns OR tunnels can create caves
        // Both now output negative values for cave spaces
        double caveContrib = Math.min(caverns, tunnels);

        // Add detail variation (small perturbations)
        double density = caveContrib + details;

        // Depth factor - caves more common at certain depths
        double depthFactor = calculateDepthFactor(y);
        density = density + depthFactor;

        // Determine if this is cave or rock
        // Negative density = cave, positive = rock
        boolean isCave = density < caveThreshold;

        // Check for water (caves below a certain depth can have water)
        boolean isWater = isCave && y < -70 && density < caveThreshold - 0.15;

        // Check for ore veins (rare, in rock only)
        boolean isOre = !isCave && isOreVein(x, y, z);

        // Formations are strongest near cave surfaces
        double formationStrength = 0;
        if (isCave && Math.abs(density - caveThreshold) < 0.15) {
            formationStrength = formations;
        }

        return new CaveData(density, isCave, isWater, isOre, formationStrength);
    }

    /**
     * Calculate depth influence on cave density.
     */
    private double calculateDepthFactor(double y) {
        // y is typically negative (underground)
        // Near surface (y > -10): fewer caves
        // Mid depth (-10 to -100): most caves
        // Deep (-100+): fewer but can still have large caverns

        if (y > -3) {
            // Very near surface - solid rock layer (no caves)
            return 0.5;
        } else if (y > -10) {
            // Shallow zone - some caves possible
            return 0.1;
        } else if (y > -120) {
            // Prime cave zone - most caves here
            return 0.0;
        } else {
            // Very deep zone - slightly fewer caves
            return 0.05;
        }
    }

    /**
     * Check if position has an ore vein.
     */
    private boolean isOreVein(double x, double y, double z) {
        // Use separate noise for ore
        double ore = graph.cellular(
            CellularDistanceFunction.Euclidean,
            CellularReturnType.CellValue,
            1.0
        ).frequency(0.1).evaluate3D(seed + 5000, x, y, z);

        // Ore is rare - only in small pockets
        return ore > 0.85;
    }

    /**
     * Generate a 2D slice of the cave system at a given depth.
     *
     * @param width   Width in samples
     * @param height  Height in samples (this is Z axis, not depth)
     * @param startX  Starting X coordinate
     * @param startZ  Starting Z coordinate
     * @param depth   Y coordinate (depth into ground, typically negative)
     * @param step    Distance between samples
     * @return 2D array of CaveData [z][x]
     */
    public CaveData[][] generateSlice(int width, int height,
                                       double startX, double startZ,
                                       double depth, double step) {
        CaveData[][] slice = new CaveData[height][width];

        for (int iz = 0; iz < height; iz++) {
            double z = startZ + iz * step;
            for (int ix = 0; ix < width; ix++) {
                double x = startX + ix * step;
                slice[iz][ix] = sample(x, depth, z);
            }
        }

        return slice;
    }

    /**
     * Data class holding cave sample information.
     */
    public record CaveData(
        double density,          // Raw density value
        boolean isCave,          // True if this is open space
        boolean isWater,         // True if underwater cave
        boolean isOre,           // True if ore vein
        double formationStrength // Stalactite/stalagmite intensity
    ) {
        /**
         * Get a simple cave/rock boolean.
         */
        public boolean isOpen() {
            return isCave;
        }

        /**
         * Get material type for rendering.
         */
        public Material getMaterial() {
            if (isWater) return Material.WATER;
            if (isCave) return Material.AIR;
            if (isOre) return Material.ORE;
            return Material.ROCK;
        }
    }

    /**
     * Material types for cave rendering.
     */
    public enum Material {
        AIR,    // Open cave space
        ROCK,   // Solid rock
        WATER,  // Underground water
        ORE     // Ore vein
    }

    // Getters
    public int getSeed() { return seed; }
    public double getCavernFrequency() { return cavernFrequency; }
    public double getTunnelFrequency() { return tunnelFrequency; }
    public double getDetailFrequency() { return detailFrequency; }
    public double getCaveThreshold() { return caveThreshold; }
    public double getTunnelThreshold() { return tunnelThreshold; }

    /**
     * Test entry point - prints cave statistics.
     */
    public static void main(String[] args) {
        CaveGenerator caves = new CaveGenerator(1337);

        int caveCount = 0;
        int rockCount = 0;
        int total = 0;

        // Sample at depth -50 (prime cave zone)
        double depth = -50;
        System.out.printf("Sampling at depth %.0f...%n", depth);
        for (int x = 0; x < 100; x++) {
            for (int z = 0; z < 100; z++) {
                CaveData data = caves.sample(x, depth, z);
                total++;
                if (data.isCave()) {
                    caveCount++;
                } else {
                    rockCount++;
                }
            }
        }

        double cavePercent = (caveCount * 100.0) / total;
        System.out.printf("Caves: %d (%.1f%%), Rock: %d (%.1f%%), Total: %d%n",
            caveCount, cavePercent, rockCount, 100 - cavePercent, total);

        if (cavePercent > 10 && cavePercent < 60) {
            System.out.println("SUCCESS: Cave percentage is within expected range (10-60%)");
        } else {
            System.out.println("WARNING: Cave percentage may be outside expected range");
        }
    }
}
