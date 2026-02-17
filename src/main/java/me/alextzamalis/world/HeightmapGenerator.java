package me.alextzamalis.world;

import me.alextzamalis.voxel.BlockRegistry;
import me.alextzamalis.voxel.Chunk;
import me.alextzamalis.voxel.World;
import me.alextzamalis.world.noise.PerlinNoise;

/**
 * Generates terrain using heightmap-based generation with Perlin noise.
 * 
 * <p>This generator creates varied terrain with hills, valleys, and mountains
 * by using Perlin noise to generate a heightmap. The terrain is then filled
 * with appropriate block layers.
 * 
 * <p>Configuration options:
 * <ul>
 *   <li>Base height - minimum terrain height</li>
 *   <li>Height variation - maximum additional height from noise</li>
 *   <li>Scale - noise frequency (larger = smoother terrain)</li>
 *   <li>Octaves - detail levels in the noise</li>
 * </ul>
 * 
 * <p>Future enhancements:
 * <ul>
 *   <li>Multiple noise layers for different features</li>
 *   <li>Biome-based block selection</li>
 *   <li>Cave generation</li>
 *   <li>Ore distribution</li>
 *   <li>Structure placement (trees, buildings)</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class HeightmapGenerator implements WorldGenerator {
    
    /** The base height of the terrain. */
    private final int baseHeight;
    
    /** The maximum height variation. */
    private final int heightVariation;
    
    /** The noise scale (frequency). */
    private final float scale;
    
    /** The Perlin noise generator. */
    private final PerlinNoise noise;
    
    /** Block registry. */
    private final BlockRegistry blockRegistry;
    
    /** Block IDs. */
    private final int stoneId;
    private final int dirtId;
    private final int grassId;
    
    /**
     * Creates a heightmap generator with default settings.
     * 
     * @param seed The world seed
     */
    public HeightmapGenerator(long seed) {
        this(seed, 64, 32, 0.02f);
    }
    
    /**
     * Creates a heightmap generator with custom settings.
     * 
     * @param seed The world seed
     * @param baseHeight The base terrain height
     * @param heightVariation The maximum height variation
     * @param scale The noise scale (smaller = more variation)
     */
    public HeightmapGenerator(long seed, int baseHeight, int heightVariation, float scale) {
        this.baseHeight = baseHeight;
        this.heightVariation = heightVariation;
        this.scale = scale;
        this.noise = new PerlinNoise(seed);
        this.blockRegistry = BlockRegistry.getInstance();
        
        this.stoneId = blockRegistry.getBlockId("minecraft:stone");
        this.dirtId = blockRegistry.getBlockId("minecraft:dirt");
        this.grassId = blockRegistry.getBlockId("minecraft:grass_block");
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void generateChunk(Chunk chunk, World world) {
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                // Calculate world coordinates
                int worldX = chunk.getWorldX(x);
                int worldZ = chunk.getWorldZ(z);
                
                // Generate height using Perlin noise
                int height = getHeight(worldX, worldZ);
                
                // Generate the column
                generateColumn(chunk, x, z, height);
            }
        }
    }
    
    /**
     * Calculates the terrain height at a position.
     * 
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @return The terrain height
     */
    public int getHeight(int worldX, int worldZ) {
        // Get noise value (0 to 1)
        float noiseValue = noise.noise(worldX * scale, worldZ * scale);
        
        // Convert to height
        return baseHeight + (int) (noiseValue * heightVariation);
    }
    
    /**
     * Generates a column of blocks.
     * 
     * @param chunk The chunk
     * @param x Local X coordinate
     * @param z Local Z coordinate
     * @param height The surface height
     */
    private void generateColumn(Chunk chunk, int x, int z, int height) {
        // Stone layer (bedrock to height - 4)
        for (int y = 0; y < height - 3; y++) {
            chunk.setBlock(x, y, z, stoneId);
        }
        
        // Dirt layer (height - 3 to height - 1)
        for (int y = Math.max(0, height - 3); y < height; y++) {
            chunk.setBlock(x, y, z, dirtId);
        }
        
        // Grass on top
        if (height >= 0 && height < Chunk.HEIGHT) {
            chunk.setBlock(x, height, z, grassId);
        }
    }
    
    /**
     * Gets the base height.
     * 
     * @return The base height
     */
    public int getBaseHeight() {
        return baseHeight;
    }
    
    /**
     * Gets the height variation.
     * 
     * @return The height variation
     */
    public int getHeightVariation() {
        return heightVariation;
    }
    
    /**
     * Gets the noise scale.
     * 
     * @return The scale
     */
    public float getScale() {
        return scale;
    }
    
    @Override
    public String getName() {
        return "Heightmap Generator (base=" + baseHeight + ", var=" + heightVariation + ")";
    }
}

