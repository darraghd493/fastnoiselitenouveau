# FastNoiseLite Nouveau - Complete API Reference

Comprehensive API reference for all components of FastNoiseLite Nouveau. This document provides everything needed to use the library effectively.

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [FastNoiseLite (Main Facade)](#fastnoiselite-main-facade)
3. [Noise Types](#noise-types)
4. [Fractal Types](#fractal-types)
5. [Domain Warping](#domain-warping)
6. [4D Noise](#4d-noise)
7. [Transforms](#transforms)
8. [Spatial Utilities](#spatial-utilities)
9. [Noise Derivatives](#noise-derivatives)
10. [Node Graph System](#node-graph-system)
11. [Common Patterns](#common-patterns)

---

## Quick Start

```java
import com.cognitivedynamics.noisegen.FastNoiseLite;

// Create and configure
FastNoiseLite noise = new FastNoiseLite(1337);  // seed
noise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
noise.SetFractalType(FastNoiseLite.FractalType.FBm);
noise.SetFractalOctaves(5);
noise.SetFrequency(0.01f);

// Generate noise
float value2D = noise.GetNoise(x, y);           // 2D
float value3D = noise.GetNoise(x, y, z);        // 3D
float value4D = noise.GetNoise(x, y, z, w);     // 4D [EXT]
```

---

## FastNoiseLite (Main Facade)

The primary entry point for noise generation.

### Constructor

```java
FastNoiseLite noise = new FastNoiseLite();      // Default seed
FastNoiseLite noise = new FastNoiseLite(1337);  // With seed
```

### Core Methods

```java
// Noise generation
float GetNoise(float x, float y)                    // 2D noise
float GetNoise(float x, float y, float z)           // 3D noise
float GetNoise(float x, float y, float z, float w)  // 4D noise [EXT]

// Domain warping (modifies coordinates in place)
void DomainWarp(Vector2 coord)   // 2D warp
void DomainWarp(Vector3 coord)   // 3D warp
```

### Configuration Methods

```java
void SetSeed(int seed)
int GetSeed()
void SetFrequency(float frequency)      // Default: 0.01
void SetNoiseType(NoiseType type)
void SetRotationType3D(RotationType3D type)
```

### Fractal Configuration

```java
void SetFractalType(FractalType type)
void SetFractalOctaves(int octaves)             // Default: 3
void SetFractalLacunarity(float lacunarity)     // Default: 2.0
void SetFractalGain(float gain)                 // Default: 0.5
void SetFractalWeightedStrength(float strength) // Default: 0.0
void SetFractalPingPongStrength(float strength) // Default: 2.0
```

### Cellular Configuration

```java
void SetCellularDistanceFunction(CellularDistanceFunction func)
void SetCellularReturnType(CellularReturnType type)
void SetCellularJitter(float jitter)  // Default: 1.0
```

### Domain Warp Configuration

```java
void SetDomainWarpType(DomainWarpType type)
void SetDomainWarpAmp(float amplitude)  // Default: 1.0
```

---

## Noise Types

```java
FastNoiseLite.NoiseType.OpenSimplex2   // Recommended general-purpose
FastNoiseLite.NoiseType.OpenSimplex2S  // Smoother variant
FastNoiseLite.NoiseType.Perlin         // Classic Perlin
FastNoiseLite.NoiseType.Cellular       // Voronoi/Worley
FastNoiseLite.NoiseType.Value          // Simple value noise
FastNoiseLite.NoiseType.ValueCubic     // Value with cubic interpolation
```

### Noise Type Characteristics

| Type | Speed | Smoothness | Best For |
|------|-------|------------|----------|
| OpenSimplex2 | Fast | Good | General purpose, terrain |
| OpenSimplex2S | Medium | Very Good | Smooth gradients |
| Perlin | Fast | Good | Classic look |
| Cellular | Slow | N/A | Cells, cracks, stone |
| Value | Very Fast | Fair | Simple patterns |
| ValueCubic | Slow | Good | Smooth value noise |

### Cellular Distance Functions

```java
CellularDistanceFunction.Euclidean    // Standard (sqrt of squared)
CellularDistanceFunction.EuclideanSq  // Squared distance (faster)
CellularDistanceFunction.Manhattan    // City-block distance
CellularDistanceFunction.Hybrid       // Euclidean + Manhattan blend
```

### Cellular Return Types

```java
CellularReturnType.CellValue     // Random value per cell [-1, 1]
CellularReturnType.Distance      // Distance to nearest [0, ~1]
CellularReturnType.Distance2     // Distance to 2nd nearest
CellularReturnType.Distance2Add  // Distance + Distance2
CellularReturnType.Distance2Sub  // Distance2 - Distance (edges)
CellularReturnType.Distance2Mul  // Distance * Distance2
CellularReturnType.Distance2Div  // Distance / Distance2
```

---

## Fractal Types

Fractals combine multiple octaves of noise for natural-looking patterns.

```java
FastNoiseLite.FractalType.None        // No fractal (single octave)
FastNoiseLite.FractalType.FBm         // Fractional Brownian motion
FastNoiseLite.FractalType.Ridged      // Ridged multifractal
FastNoiseLite.FractalType.PingPong    // Ping-pong (banded)
FastNoiseLite.FractalType.Billow      // Billow (soft) [EXT]
FastNoiseLite.FractalType.HybridMulti // Hybrid multifractal [EXT]

// Domain warp fractals
FastNoiseLite.FractalType.DomainWarpProgressive
FastNoiseLite.FractalType.DomainWarpIndependent
```

### Fractal Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| Octaves | 3 | Number of noise layers (1-8 typical) |
| Lacunarity | 2.0 | Frequency multiplier per octave |
| Gain | 0.5 | Amplitude multiplier per octave |
| WeightedStrength | 0.0 | Octave weighting (0 = equal) |
| PingPongStrength | 2.0 | PingPong band sharpness |

### Fractal Characteristics

| Type | Output | Best For |
|------|--------|----------|
| FBm | [-1, 1] | Terrain, clouds, general |
| Ridged | [0, 1] | Mountains, ridges, veins |
| PingPong | [0, 1] | Terraced, banded patterns |
| Billow | [0, 1] | Puffy clouds, soft hills |
| HybridMulti | varies | Eroded terrain |

---

## Domain Warping

Distort coordinates before noise sampling for organic patterns.

```java
// Configure warp
noise.SetDomainWarpType(DomainWarpType.OpenSimplex2);
noise.SetDomainWarpAmp(30.0f);  // Distortion amount

// Optional: Fractal warp
noise.SetFractalType(FractalType.DomainWarpProgressive);
noise.SetFractalOctaves(4);

// Apply warp
Vector2 coord = new Vector2(x, y);
noise.DomainWarp(coord);
float value = noise.GetNoise(coord.x, coord.y);
```

### Warp Types

```java
DomainWarpType.OpenSimplex2        // Smooth warp
DomainWarpType.OpenSimplex2Reduced // Faster, less smooth
DomainWarpType.BasicGrid           // Simple grid-based
```

---

## 4D Noise

**[Extension]** Use 4D noise for time-based animations.

```java
// Animated 3D volume
float value = noise.GetNoise(x, y, z, time * 0.5f);

// Looping animation (circular path in 4D)
float angle = progress * 2.0f * (float)Math.PI;
float loopRadius = 10.0f;
float w = (float)Math.sin(angle) * loopRadius;
float extra = (float)Math.cos(angle) * loopRadius;
float looping = noise.GetNoise(x, y, z + extra, w);
```

---

## Transforms

**[Extension]** Post-processing transforms for noise values.

### Import

```java
import com.cognitivedynamics.noisegen.transforms.*;
```

### NoiseTransform Interface

```java
public interface NoiseTransform {
    float apply(float value);
}
```

### Available Transforms

#### RangeTransform
Remap values from one range to another.

```java
// Remap [-1, 1] to [0, 255]
NoiseTransform t = new RangeTransform(-1f, 1f, 0f, 255f);
float result = t.apply(noiseValue);
```

#### PowerTransform
Apply power curve for contrast adjustment.

```java
// Sharper contrast (exponent > 1)
NoiseTransform t = new PowerTransform(2.0f);

// Softer contrast (exponent < 1)
NoiseTransform t = new PowerTransform(0.5f);
```

#### RidgeTransform
Create ridge patterns from noise.

```java
// Basic ridge: 1 - |value|
NoiseTransform t = new RidgeTransform();

// With offset
NoiseTransform t = new RidgeTransform(0.5f);
```

#### ClampTransform
Clamp values to a range.

```java
NoiseTransform t = new ClampTransform(-0.5f, 0.5f);
```

#### InvertTransform
Negate values.

```java
NoiseTransform t = new InvertTransform();  // -value
```

#### TerraceTransform
Create stepped terraces.

```java
// 8 terrace levels
NoiseTransform t = TerraceTransform.contours(8);

// Custom levels
NoiseTransform t = new TerraceTransform(new float[]{-0.5f, 0f, 0.3f, 0.7f, 1.0f});
```

#### TurbulenceTransform
Multiply by absolute value for turbulence effect.

```java
NoiseTransform t = new TurbulenceTransform();  // value * |value|
```

#### QuantizeTransform
Quantize to discrete levels.

```java
// Posterize to 8 levels
NoiseTransform t = new QuantizeTransform(8);
```

#### ChainedTransform
Chain multiple transforms.

```java
NoiseTransform pipeline = new ChainedTransform(
    new RidgeTransform(),
    new PowerTransform(2.0f),
    new RangeTransform(0f, 1f, 0f, 255f)
);

float result = pipeline.apply(noiseValue);
```

---

## Spatial Utilities

**[Extension]** Utilities for large-scale or specialized coordinate systems.

### Import

```java
import com.cognitivedynamics.noisegen.spatial.*;
```

### ChunkedNoise
Handle infinite worlds without float precision loss.

```java
FastNoiseLite base = new FastNoiseLite(1337);
ChunkedNoise chunked = new ChunkedNoise(base, 1000.0);  // chunk size

// Works at huge coordinates without precision loss
float value = chunked.getNoise(1_000_000_000.0, 2_500_000_000.0);

// 3D
float value3D = chunked.getNoise3D(x, y, z);
```

### DoublePrecisionNoise
Full double precision for astronomical scales.

```java
FastNoiseLite base = new FastNoiseLite(1337);
DoublePrecisionNoise precise = new DoublePrecisionNoise(base);

// Precise to the decimal at trillion-scale coordinates
float value = precise.getNoise(1_000_000_000_000.5, 2_500_000_000_000.3);

// 3D
float value3D = precise.getNoise3D(x, y, z);
```

### TiledNoise
Create seamlessly tileable noise textures.

```java
FastNoiseLite base = new FastNoiseLite(1337);
TiledNoise tiled = new TiledNoise(base, 256, 256);  // tile dimensions

// Seamless at boundaries
float value = tiled.getNoise(x, y);  // x=0 == x=256

// Generate seamless image (grayscale)
byte[] grayscale = tiled.getSeamlessImage(256, 256);

// Generate seamless RGB image
byte[] rgb = tiled.getSeamlessImageRGB(256, 256, TiledNoise.TERRAIN_GRADIENT);
```

Available gradients:
- `TiledNoise.GRAYSCALE_GRADIENT`
- `TiledNoise.TERRAIN_GRADIENT`
- `TiledNoise.HEAT_GRADIENT`

### LODNoise
Distance-based level of detail (reduces octaves with distance).

```java
FastNoiseLite base = new FastNoiseLite(1337);
base.SetFractalOctaves(8);

LODNoise lod = new LODNoise(base, 8);  // max octaves

// Near = full detail, far = fewer octaves
float near = lod.getNoise(x, y, 0.0f);     // 8 octaves
float far = lod.getNoise(x, y, 500.0f);    // ~2 octaves

// Configure LOD distances
lod.setMaxDistance(1000f);
lod.setMinOctaves(1);
```

### TurbulenceNoise
Curl noise and turbulence for fluid-like motion.

```java
FastNoiseLite base = new FastNoiseLite(1337);
TurbulenceNoise turbulence = new TurbulenceNoise(base, 0.01f);  // frequency

// Classic Perlin turbulence (absolute value sum)
float turb = turbulence.perlinTurbulence(x, y, 4);  // 4 octaves

// 2D curl noise (returns velocity vector)
float[] curl2D = turbulence.curl2D(x, y);
// curl2D[0] = vx, curl2D[1] = vy

// 3D curl noise (returns 3D velocity)
float[] curl3D = turbulence.curl3D(x, y, z);
// curl3D[0] = vx, curl3D[1] = vy, curl3D[2] = vz

// 4D curl with time
float[] curl3DAnimated = turbulence.curl3D(x, y, z, time);

// Multi-octave curl (FBm-style)
float[] curlFBm = turbulence.curlFBm3D(x, y, z, 4);  // 4 octaves

// Warped turbulence
float warped = turbulence.warpedTurbulence(x, y, 4, 30f);  // octaves, warp amp
```

### HierarchicalNoise
Quadtree/octree-based adaptive sampling.

```java
FastNoiseLite base = new FastNoiseLite(1337);
HierarchicalNoise hier = new HierarchicalNoise(base, 8);  // levels

// Sample specific level (0 = coarsest, 7 = finest)
float coarse = hier.sampleLevel(x, y, 0);    // Continents
float medium = hier.sampleLevel(x, y, 4);    // Regions
float fine = hier.sampleLevel(x, y, 7);      // Details

// Adaptive based on view scale
float adaptive = hier.sampleAdaptive(x, y, viewScale);

// Cumulative (sum of levels)
float cumulative = hier.sampleCumulative(x, y, 4);  // levels 0-4
```

### SparseConvolutionNoise
Memory-efficient noise with constant memory usage.

```java
SparseConvolutionNoise sparse = new SparseConvolutionNoise(1337);

float value = sparse.getNoise(x, y);
float value3D = sparse.getNoise3D(x, y, z);

// Configure kernel
sparse.setKernelSize(3);
sparse.setFeatureDensity(1.0f);
```

---

## Noise Derivatives

**[Extension]** Analytical gradients for lighting and normal maps.

### Import

```java
import com.cognitivedynamics.noisegen.derivatives.NoiseDerivatives;
```

### Basic Usage

```java
FastNoiseLite noise = new FastNoiseLite(1337);
noise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);

NoiseDerivatives deriv = new NoiseDerivatives(noise);
```

### Get Noise with Gradient

```java
// 2D (most efficient - computes value and gradient together)
NoiseDerivatives.NoiseWithGradient2D result = deriv.getNoiseWithGradient2D(x, y);
float value = result.value;
float dNdx = result.dx;
float dNdy = result.dy;

// 3D
NoiseDerivatives.NoiseWithGradient3D result3D = deriv.getNoiseWithGradient3D(x, y, z);
```

### Compute Surface Normal

```java
// For terrain lighting (assumes z = noise(x,y) * heightScale)
float[] normal = deriv.computeNormal2D(x, y, heightScale);
// normal[0] = nx, normal[1] = ny, normal[2] = nz (normalized)

// Convert to RGB color (for normal maps)
int[] rgb = deriv.normalToRGB(normal);
// rgb[0] = R, rgb[1] = G, rgb[2] = B
```

### Generate Normal Map

```java
// Generate full normal map texture
byte[] normalMap = deriv.generateNormalMapRGB(
    width, height,     // Texture dimensions
    worldSize,         // World units covered by texture
    heightScale        // Height multiplier
);
```

### Configuration

```java
// Use analytical derivatives (faster, default)
deriv.setUseAnalytical(true);

// Use numerical derivatives (fallback, works with any noise)
deriv.setUseAnalytical(false);

// Set epsilon for numerical derivatives
deriv.setEpsilon(0.001f);
```

---

## Node Graph System

**[Extension]** FastNoise2-inspired composable noise DAG with fluent API.

See [NODE_GRAPH_API.md](NODE_GRAPH_API.md) for complete reference, or summary below.

### Quick Reference

```java
import com.cognitivedynamics.noisegen.graph.NoiseGraph;
import com.cognitivedynamics.noisegen.graph.NoiseNode;
import com.cognitivedynamics.noisegen.graph.util.BulkEvaluator;

NoiseGraph g = NoiseGraph.create(1337);

// Source nodes
g.simplex()              // OpenSimplex2
g.simplexSmooth()        // OpenSimplex2S
g.perlin()               // Perlin
g.value()                // Value
g.valueCubic()           // Value cubic
g.cellular()             // Cellular (default)
g.cellular(distFunc, returnType, jitter)
g.simplex4D()            // 4D Simplex
g.constant(value)        // Constant

// All sources support .frequency(double)
g.simplex().frequency(0.01)

// Fractal nodes
g.fbm(source, octaves)
g.fbm(source, octaves, lacunarity, gain)
g.ridged(source, octaves)
g.billow(source, octaves)
g.hybridMulti(source, octaves)

// Fluent combiners (on any node)
node.add(other)          // a + b
node.add(constant)       // a + c
node.subtract(other)     // a - b
node.multiply(other)     // a * b
node.multiply(constant)  // a * c
node.min(other)          // min(a, b)
node.max(other)          // max(a, b)

// Fluent modifiers
node.scale(factor)       // Scale coordinates
node.offset(dx, dy, dz)  // Offset coordinates
node.clamp(min, max)     // Clamp output
node.abs()               // |output|
node.invert()            // -output
node.transform(t)        // Apply NoiseTransform
node.warp(warpNode, amp) // Domain warp

// Blend
g.blend(a, b, control)   // lerp(a, b, control)

// Evaluation
double v = node.evaluate2D(seed, x, y);
double v = node.evaluate3D(seed, x, y, z);
double v = node.evaluate4D(seed, x, y, z, w);

// Bulk evaluation
BulkEvaluator bulk = new BulkEvaluator(seed);
double[][] map = bulk.fill2D(node, width, height, startX, startY, step);
double[][][] vol = bulk.fill3D(node, w, h, d, startX, startY, startZ, step);
```

---

## Common Patterns

### Basic Terrain

```java
FastNoiseLite noise = new FastNoiseLite(1337);
noise.SetNoiseType(NoiseType.OpenSimplex2);
noise.SetFractalType(FractalType.FBm);
noise.SetFractalOctaves(5);
noise.SetFrequency(0.005f);

float height = noise.GetNoise(x, y);  // [-1, 1]
```

### Mountain Terrain

```java
FastNoiseLite noise = new FastNoiseLite(1337);
noise.SetNoiseType(NoiseType.OpenSimplex2);
noise.SetFractalType(FractalType.Ridged);
noise.SetFractalOctaves(5);
noise.SetFrequency(0.01f);

float height = noise.GetNoise(x, y);  // [0, 1], sharp ridges
```

### Domain Warped Terrain

```java
FastNoiseLite noise = new FastNoiseLite(1337);
noise.SetNoiseType(NoiseType.OpenSimplex2);
noise.SetFractalType(FractalType.FBm);
noise.SetFractalOctaves(5);
noise.SetFrequency(0.005f);

noise.SetDomainWarpType(DomainWarpType.OpenSimplex2);
noise.SetDomainWarpAmp(50f);

Vector2 coord = new Vector2(x, y);
noise.DomainWarp(coord);
float height = noise.GetNoise(coord.x, coord.y);
```

### Caves (3D Cellular)

```java
FastNoiseLite noise = new FastNoiseLite(1337);
noise.SetNoiseType(NoiseType.Cellular);
noise.SetCellularDistanceFunction(CellularDistanceFunction.EuclideanSq);
noise.SetCellularReturnType(CellularReturnType.Distance2Sub);
noise.SetFrequency(0.05f);

float density = noise.GetNoise(x, y, z);
boolean isCave = density < 0.3f;
```

### Animated Volumetric

```java
FastNoiseLite noise = new FastNoiseLite(1337);
noise.SetNoiseType(NoiseType.OpenSimplex2);
noise.SetFractalType(FractalType.FBm);
noise.SetFractalOctaves(4);
noise.SetFrequency(0.02f);

// Animate with 4D
float time = frameCount * 0.05f;
float density = noise.GetNoise(x, y, z, time);
```

### Multi-Layer Terrain (Node Graph)

```java
NoiseGraph g = NoiseGraph.create(1337);

NoiseNode terrain = g.fbm(g.simplex().frequency(0.002), 3)
    .warp(g.simplex().frequency(0.001), 50)
    .add(g.ridged(g.simplex().frequency(0.008), 4).multiply(0.5))
    .add(g.fbm(g.simplex().frequency(0.02), 3).multiply(0.3))
    .clamp(-1, 1);

double height = terrain.evaluate2D(g.getSeed(), x, y);
```

### Nebula with Curl Noise

```java
FastNoiseLite base = new FastNoiseLite(1337);
base.SetNoiseType(NoiseType.OpenSimplex2);
TurbulenceNoise turb = new TurbulenceNoise(base, 0.003f);

// Get flow velocity
float[] velocity = turb.curlFBm3D(x, y, z, 4);

// Advect particles
particle.x += velocity[0] * dt;
particle.y += velocity[1] * dt;
particle.z += velocity[2] * dt;
```

---

## Frequency Guidelines

| Scale | Frequency | Example |
|-------|-----------|---------|
| 0.001-0.003 | Continental | Landmasses, ocean basins |
| 0.003-0.01 | Regional | Mountain ranges, biomes |
| 0.01-0.05 | Local | Hills, forests |
| 0.05-0.2 | Detail | Rocks, surface texture |
| 0.2-1.0 | Fine | Small details, noise texture |

---

## Performance Notes

- **Value noise** is fastest, **Cellular** is slowest
- Each fractal octave roughly halves performance
- Analytical derivatives are ~4x faster than numerical
- Node Graph has minimal overhead vs direct calls
- Use `BulkEvaluator` for grid generation

---

## Thread Safety

- `FastNoiseLite` instances are **NOT** thread-safe for configuration changes
- `FastNoiseLite.GetNoise()` IS thread-safe if configuration doesn't change
- All `NoiseNode` instances are immutable and fully thread-safe
- `BulkEvaluator` is thread-safe
- Spatial utilities are generally thread-safe for reads
