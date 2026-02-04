# Node Graph System API Reference

Complete API reference for the FastNoiseLite Nouveau Node Graph System. This document provides all information needed to use the API effectively.

## Package and Imports

```java
// Core classes
import com.cognitivedynamics.noisegen.graph.NoiseGraph;
import com.cognitivedynamics.noisegen.graph.NoiseNode;
import com.cognitivedynamics.noisegen.graph.util.BulkEvaluator;

// For cellular noise configuration
import com.cognitivedynamics.noisegen.NoiseTypes.CellularDistanceFunction;
import com.cognitivedynamics.noisegen.NoiseTypes.CellularReturnType;

// For transforms (optional)
import com.cognitivedynamics.noisegen.transforms.*;
```

## Core Concepts

### Design Principles
- **Immutable nodes**: All fluent methods return new node instances
- **Thread-safe**: Same node can be evaluated from multiple threads
- **Double precision**: Coordinates use `double` for astronomical scales
- **Seed at evaluation**: Seed is passed to `evaluate*()` methods, not stored in nodes

### Output Range
- Most noise functions output values in range **[-1.0, 1.0]**
- Cellular noise may have different ranges depending on return type
- Use `.clamp(min, max)` to constrain output range

---

## NoiseGraph Factory

Create graphs with `NoiseGraph.create(seed)`:

```java
NoiseGraph graph = NoiseGraph.create(1337);  // With seed
NoiseGraph graph = NoiseGraph.create();       // Default seed 1337
int seed = graph.getSeed();                   // Get stored seed
```

---

## Source Nodes

Source nodes generate base noise values. All support `.frequency(double)` to scale coordinates.

### Available Sources

| Method | Description | Output Range |
|--------|-------------|--------------|
| `graph.simplex()` | OpenSimplex2 noise (recommended general-purpose) | [-1, 1] |
| `graph.simplexSmooth()` | OpenSimplex2S (smoother variant) | [-1, 1] |
| `graph.perlin()` | Classic Perlin noise | [-1, 1] |
| `graph.value()` | Value noise (interpolated random grid) | [-1, 1] |
| `graph.valueCubic()` | Value noise with cubic interpolation | [-1, 1] |
| `graph.cellular()` | Cellular/Voronoi noise (default settings) | varies |
| `graph.cellular(distFunc, returnType, jitter)` | Cellular with custom settings | varies |
| `graph.simplex4D()` | 4D Simplex (supports W dimension) | [-1, 1] |
| `graph.constant(value)` | Constant value | value |

### Frequency

Frequency controls feature size. **Lower = larger features, higher = smaller features**.

```java
graph.simplex().frequency(0.001)  // Continental scale
graph.simplex().frequency(0.01)   // Regional scale
graph.simplex().frequency(0.1)    // Local detail
graph.simplex().frequency(1.0)    // Fine texture
```

### Cellular Noise Options

```java
// CellularDistanceFunction options:
CellularDistanceFunction.Euclidean    // Standard distance
CellularDistanceFunction.EuclideanSq  // Squared (faster, similar look)
CellularDistanceFunction.Manhattan    // City-block distance
CellularDistanceFunction.Hybrid       // Mix of Euclidean and Manhattan

// CellularReturnType options:
CellularReturnType.CellValue     // Random value per cell
CellularReturnType.Distance      // Distance to nearest point
CellularReturnType.Distance2     // Distance to second nearest
CellularReturnType.Distance2Add  // Distance + Distance2
CellularReturnType.Distance2Sub  // Distance2 - Distance (good for caves/edges)
CellularReturnType.Distance2Mul  // Distance * Distance2
CellularReturnType.Distance2Div  // Distance / Distance2

// Example: Cave-like cellular noise
graph.cellular(
    CellularDistanceFunction.EuclideanSq,
    CellularReturnType.Distance2Sub,
    0.8  // Jitter: 0.0 = regular grid, 1.0 = maximum randomness
).frequency(0.02)
```

---

## Fractal Nodes

Fractal nodes combine multiple octaves of a source noise for natural-looking patterns.

### Available Fractals

| Method | Description | Best For |
|--------|-------------|----------|
| `graph.fbm(source, octaves)` | Fractional Brownian motion | Terrain, clouds, general |
| `graph.ridged(source, octaves)` | Ridged multifractal | Mountains, ridges, veins |
| `graph.billow(source, octaves)` | Absolute value (soft) | Puffy clouds, soft hills |
| `graph.hybridMulti(source, octaves)` | Erosion-like detail | Realistic terrain |

### Parameters

```java
// Simple (uses default lacunarity=2.0, gain=0.5)
graph.fbm(source, 4)

// Full control
graph.fbm(source, octaves, lacunarity, gain)
// - octaves: Number of layers (1-8 typical)
// - lacunarity: Frequency multiplier per octave (typically 2.0)
// - gain: Amplitude multiplier per octave (typically 0.5)
```

### Examples

```java
// Standard terrain
NoiseNode terrain = graph.fbm(graph.simplex().frequency(0.01), 5);

// Sharp mountain ridges
NoiseNode mountains = graph.ridged(graph.simplex().frequency(0.01), 4);

// Soft clouds
NoiseNode clouds = graph.billow(graph.simplex().frequency(0.02), 4);

// Eroded terrain (detail in rough areas, smooth in flat areas)
NoiseNode eroded = graph.hybridMulti(graph.simplex().frequency(0.01), 5);
```

---

## Combiner Nodes

Combine two or more noise sources.

### Binary Combiners (Fluent)

```java
nodeA.add(nodeB)       // a + b
nodeA.subtract(nodeB)  // a - b
nodeA.multiply(nodeB)  // a * b
nodeA.min(nodeB)       // min(a, b)
nodeA.max(nodeB)       // max(a, b)
```

### Constant Operations

```java
node.add(0.5)          // Add constant: output + 0.5
node.multiply(0.5)     // Scale: output * 0.5
```

### Blend (Interpolation)

```java
// Interpolates between a and b based on control signal
// control=0 -> a, control=1 -> b
graph.blend(nodeA, nodeB, controlNode)
```

### Factory Methods

```java
graph.add(nodeA, nodeB)
graph.subtract(nodeA, nodeB)
graph.multiply(nodeA, nodeB)
graph.min(nodeA, nodeB)
graph.max(nodeA, nodeB)
graph.blend(nodeA, nodeB, controlNode)
```

---

## Modifier Nodes

Transform coordinates or output values.

### Domain Modifiers (affect input coordinates)

```java
node.scale(2.0)              // Multiply coordinates (higher = more detail)
node.offset(dx, dy, dz)      // Offset coordinates
```

### Value Modifiers (affect output)

```java
node.clamp(min, max)         // Clamp output to range
node.abs()                   // Absolute value: |output|
node.invert()                // Negate: -output
node.transform(transformer)  // Apply NoiseTransform
```

### Available Transforms

```java
import com.cognitivedynamics.noisegen.transforms.*;

node.transform(new RangeTransform(-1, 1, 0, 100))    // Remap range
node.transform(new PowerTransform(2.0))              // Power curve
node.transform(new RidgeTransform())                 // Ridge pattern
node.transform(new TerraceTransform(8))              // Stepped terraces
node.transform(new ClampTransform(-0.5f, 0.5f))      // Clamp
node.transform(new InvertTransform())                // Invert

// Chain multiple transforms
node.transform(new ChainedTransform(
    new RidgeTransform(),
    new PowerTransform(2.0),
    new RangeTransform(0, 1, 0, 255)
))
```

---

## Domain Warp

Distort coordinates using another noise source for organic patterns.

```java
// Warp terrain coordinates using simplex noise
NoiseNode warpSource = graph.simplex().frequency(0.005);
NoiseNode warped = terrain.warp(warpSource, amplitude);
// amplitude: How much to distort (50.0 is moderate)

// Factory method
graph.warp(sourceNode, warpSourceNode, amplitude)
```

---

## NoiseNode Interface

All nodes implement `NoiseNode` with these methods:

### Evaluation Methods

```java
double evaluate2D(int seed, double x, double y)
double evaluate3D(int seed, double x, double y, double z)
double evaluate4D(int seed, double x, double y, double z, double w)  // If supports4D()
boolean supports4D()  // Check if 4D is supported
String getNodeType()  // Node type name for debugging
```

### Fluent Methods (return new nodes)

```java
// Domain
NoiseNode scale(double factor)
NoiseNode offset(double dx, double dy, double dz)
NoiseNode warp(NoiseNode warpSource, double amplitude)

// Combiners
NoiseNode add(NoiseNode other)
NoiseNode add(double constant)
NoiseNode subtract(NoiseNode other)
NoiseNode multiply(NoiseNode other)
NoiseNode multiply(double constant)
NoiseNode min(NoiseNode other)
NoiseNode max(NoiseNode other)

// Value modifiers
NoiseNode clamp(double min, double max)
NoiseNode abs()
NoiseNode invert()
NoiseNode transform(NoiseTransform t)
```

---

## BulkEvaluator

Efficiently fill arrays with noise values.

```java
BulkEvaluator bulk = new BulkEvaluator(seed);
```

### 2D Methods

```java
// Fill 2D array with step size
double[][] map = bulk.fill2D(node, width, height, startX, startY, step);

// Fill 2D array with coordinate range
double[][] map = bulk.fill2DRange(node, width, height, minX, minY, maxX, maxY);

// Fill flat array (row-major: y * width + x)
double[] flat = bulk.fill2DFlat(node, width, height, startX, startY, step);

// Float array for graphics APIs
float[][] floatMap = bulk.fill2DFloat(node, width, height, startX, startY, step);

// Single line
double[] line = bulk.fillLine2D(node, length, startX, startY, stepX, stepY);
```

### 3D Methods

```java
// Fill 3D array [z][y][x]
double[][][] volume = bulk.fill3D(node, width, height, depth, startX, startY, startZ, step);

// Fill flat array (z * height * width + y * width + x)
double[] flat = bulk.fill3DFlat(node, width, height, depth, startX, startY, startZ, step);
```

---

## Complete Examples

### Basic Terrain

```java
NoiseGraph g = NoiseGraph.create(1337);

NoiseNode terrain = g.fbm(g.simplex().frequency(0.01), 5)
    .clamp(-1, 1);

double height = terrain.evaluate2D(g.getSeed(), x, y);
```

### Layered Terrain with Domain Warp

```java
NoiseGraph g = NoiseGraph.create(1337);

// Base continental shapes
NoiseNode continental = g.fbm(g.simplex().frequency(0.002), 3)
    .warp(g.simplex().frequency(0.001), 50.0);

// Mountain ridges
NoiseNode mountains = g.ridged(g.simplex().frequency(0.008), 4)
    .multiply(0.5);

// Rolling hills
NoiseNode hills = g.fbm(g.simplex().frequency(0.02), 3)
    .multiply(0.3);

// Fine detail
NoiseNode detail = g.fbm(g.simplex().frequency(0.1), 2)
    .multiply(0.1);

// Combine all layers
NoiseNode terrain = continental
    .add(mountains)
    .add(hills)
    .add(detail)
    .clamp(-1, 1);
```

### Biome Blending

```java
NoiseGraph g = NoiseGraph.create(1337);

// Two different terrain types
NoiseNode plains = g.fbm(g.simplex().frequency(0.01), 3).multiply(0.3);
NoiseNode mountains = g.ridged(g.simplex().frequency(0.02), 5).multiply(1.5);

// Biome selector: remap to [0, 1]
NoiseNode biomeControl = g.simplex().frequency(0.001)
    .multiply(0.5)
    .add(0.5)
    .clamp(0, 1);

// Blend based on biome
NoiseNode terrain = g.blend(plains, mountains, biomeControl);
```

### Cave System (3D)

```java
NoiseGraph g = NoiseGraph.create(1337);

// Cellular caverns (Distance2Sub creates bubble-like shapes)
NoiseNode caverns = g.cellular(
        CellularDistanceFunction.EuclideanSq,
        CellularReturnType.Distance2Sub,
        0.8
    ).frequency(0.02)
     .invert()
     .add(0.3);

// Ridged tunnels
NoiseNode tunnels = g.ridged(g.simplex().frequency(0.03), 4)
    .invert()
    .add(0.5);

// Combine: either caverns OR tunnels create cave space
NoiseNode caves = caverns.min(tunnels);

// Sample: negative = cave, positive = rock
double density = caves.evaluate3D(seed, x, y, z);
boolean isCave = density < 0;
```

### Animated 4D Noise

```java
NoiseGraph g = NoiseGraph.create(1337);

NoiseNode animated = g.fbm(g.simplex4D().frequency(0.05), 4);

// Animate by varying W (time) coordinate
for (double time = 0; time < 10; time += 0.1) {
    double value = animated.evaluate4D(seed, x, y, z, time);
}
```

### Heightmap Generation

```java
NoiseGraph g = NoiseGraph.create(1337);
BulkEvaluator bulk = new BulkEvaluator(g.getSeed());

NoiseNode terrain = g.fbm(g.simplex().frequency(0.01), 5);

// Generate 512x512 heightmap
double[][] heightmap = bulk.fill2D(terrain, 512, 512, 0, 0, 1.0);

// Or with explicit coordinate range
double[][] heightmap = bulk.fill2DRange(terrain, 512, 512,
    -100, -100,  // min coordinates
    100, 100     // max coordinates
);
```

---

## Performance Tips

1. **Reuse nodes**: Create node graphs once, evaluate many times
2. **Use BulkEvaluator**: More efficient than individual point evaluation for grids
3. **Limit octaves**: Each octave roughly halves performance
4. **Use appropriate precision**: `fill2DFloat` for graphics APIs
5. **Cache results**: Noise evaluation is deterministic (same seed + coords = same result)

## Frequency Guidelines

| Scale | Frequency | Example Use |
|-------|-----------|-------------|
| 0.001-0.003 | Continental | Landmasses, ocean basins |
| 0.003-0.01 | Regional | Mountain ranges, biomes |
| 0.01-0.05 | Local | Hills, forests, terrain variation |
| 0.05-0.2 | Detail | Rocks, surface texture |
| 0.2-1.0 | Fine | Small-scale detail, noise texture |

## Thread Safety

- All `NoiseNode` instances are immutable and thread-safe
- `BulkEvaluator` instances are thread-safe
- Same graph can be evaluated from multiple threads simultaneously
- No synchronization needed for read-only operations
