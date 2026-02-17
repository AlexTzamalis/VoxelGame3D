package me.alextzamalis.world.noise;

import java.util.Random;

/**
 * Perlin noise implementation for terrain generation.
 * 
 * <p>This class implements Ken Perlin's improved noise algorithm for
 * generating smooth, natural-looking random values. It's commonly used
 * for terrain heightmaps, textures, and other procedural content.
 * 
 * <p>Features:
 * <ul>
 *   <li>Deterministic - same seed produces same results</li>
 *   <li>Smooth interpolation between values</li>
 *   <li>Tileable with proper configuration</li>
 *   <li>Support for 2D and 3D noise</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class PerlinNoise {
    
    /** Permutation table. */
    private final int[] permutation;
    
    /** Doubled permutation table for overflow handling. */
    private final int[] p;
    
    /**
     * Creates a Perlin noise generator with the specified seed.
     * 
     * @param seed The random seed
     */
    public PerlinNoise(long seed) {
        permutation = new int[256];
        p = new int[512];
        
        // Initialize permutation array
        for (int i = 0; i < 256; i++) {
            permutation[i] = i;
        }
        
        // Shuffle using seed
        Random random = new Random(seed);
        for (int i = 255; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
        }
        
        // Duplicate for overflow
        for (int i = 0; i < 512; i++) {
            p[i] = permutation[i & 255];
        }
    }
    
    /**
     * Generates 2D Perlin noise.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @return Noise value between 0 and 1
     */
    public float noise(float x, float y) {
        // Find unit square containing point
        int X = fastFloor(x) & 255;
        int Y = fastFloor(y) & 255;
        
        // Find relative x, y in square
        x -= fastFloor(x);
        y -= fastFloor(y);
        
        // Compute fade curves
        float u = fade(x);
        float v = fade(y);
        
        // Hash coordinates of square corners
        int A = p[X] + Y;
        int B = p[X + 1] + Y;
        
        // Blend results from corners
        float result = lerp(v,
            lerp(u, grad(p[A], x, y), grad(p[B], x - 1, y)),
            lerp(u, grad(p[A + 1], x, y - 1), grad(p[B + 1], x - 1, y - 1))
        );
        
        // Normalize to 0-1 range
        return (result + 1) / 2;
    }
    
    /**
     * Generates 3D Perlin noise.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return Noise value between 0 and 1
     */
    public float noise(float x, float y, float z) {
        // Find unit cube containing point
        int X = fastFloor(x) & 255;
        int Y = fastFloor(y) & 255;
        int Z = fastFloor(z) & 255;
        
        // Find relative x, y, z in cube
        x -= fastFloor(x);
        y -= fastFloor(y);
        z -= fastFloor(z);
        
        // Compute fade curves
        float u = fade(x);
        float v = fade(y);
        float w = fade(z);
        
        // Hash coordinates of cube corners
        int A = p[X] + Y;
        int AA = p[A] + Z;
        int AB = p[A + 1] + Z;
        int B = p[X + 1] + Y;
        int BA = p[B] + Z;
        int BB = p[B + 1] + Z;
        
        // Blend results from corners
        float result = lerp(w,
            lerp(v,
                lerp(u, grad(p[AA], x, y, z), grad(p[BA], x - 1, y, z)),
                lerp(u, grad(p[AB], x, y - 1, z), grad(p[BB], x - 1, y - 1, z))
            ),
            lerp(v,
                lerp(u, grad(p[AA + 1], x, y, z - 1), grad(p[BA + 1], x - 1, y, z - 1)),
                lerp(u, grad(p[AB + 1], x, y - 1, z - 1), grad(p[BB + 1], x - 1, y - 1, z - 1))
            )
        );
        
        // Normalize to 0-1 range
        return (result + 1) / 2;
    }
    
    /**
     * Generates octave noise (fractal Brownian motion).
     * 
     * <p>Combines multiple noise samples at different frequencies
     * for more detailed, natural-looking results.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param octaves Number of octaves
     * @param persistence Amplitude multiplier per octave
     * @return Noise value between 0 and 1
     */
    public float octaveNoise(float x, float y, int octaves, float persistence) {
        float total = 0;
        float frequency = 1;
        float amplitude = 1;
        float maxValue = 0;
        
        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, y * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }
        
        return total / maxValue;
    }
    
    /**
     * Fade function for smooth interpolation.
     * 
     * @param t Input value
     * @return Smoothed value
     */
    private float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }
    
    /**
     * Linear interpolation.
     * 
     * @param t Interpolation factor
     * @param a Start value
     * @param b End value
     * @return Interpolated value
     */
    private float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }
    
    /**
     * Gradient function for 2D noise.
     * 
     * @param hash Hash value
     * @param x X distance
     * @param y Y distance
     * @return Gradient value
     */
    private float grad(int hash, float x, float y) {
        int h = hash & 3;
        float u = h < 2 ? x : y;
        float v = h < 2 ? y : x;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
    
    /**
     * Gradient function for 3D noise.
     * 
     * @param hash Hash value
     * @param x X distance
     * @param y Y distance
     * @param z Z distance
     * @return Gradient value
     */
    private float grad(int hash, float x, float y, float z) {
        int h = hash & 15;
        float u = h < 8 ? x : y;
        float v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
    
    /**
     * Fast floor function.
     * 
     * @param x Input value
     * @return Floor value
     */
    private int fastFloor(float x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }
}


