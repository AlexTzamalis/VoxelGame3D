package me.alextzamalis.world;

import me.alextzamalis.voxel.BlockRegistry;
import me.alextzamalis.voxel.Chunk;
import me.alextzamalis.voxel.World;

/**
 * Generates a flat world with configurable layers.
 * 
 * <p>This generator creates a simple flat world with distinct layers
 * of different block types. It's useful for testing and as a base
 * for creative building.
 * 
 * <p>Default configuration:
 * <ul>
 *   <li>Y 0: Bedrock (stone for now)</li>
 *   <li>Y 1-3: Stone</li>
 *   <li>Y 4-6: Dirt</li>
 *   <li>Y 7: Grass</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class FlatWorldGenerator implements WorldGenerator {
    
    /** The height of the flat terrain. */
    private final int terrainHeight;
    
    /** Block registry for looking up block IDs. */
    private final BlockRegistry blockRegistry;
    
    /** Block IDs for each layer. */
    private int bedrockId;
    private int stoneId;
    private int dirtId;
    private int grassId;
    
    /**
     * Creates a flat world generator with default height (64).
     */
    public FlatWorldGenerator() {
        this(64);
    }
    
    /**
     * Creates a flat world generator with the specified terrain height.
     * 
     * @param terrainHeight The height of the terrain surface
     */
    public FlatWorldGenerator(int terrainHeight) {
        this.terrainHeight = terrainHeight;
        this.blockRegistry = BlockRegistry.getInstance();
        
        // Get block IDs
        this.bedrockId = blockRegistry.getBlockId("minecraft:stone"); // Using stone as bedrock for now
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
                generateColumn(chunk, x, z);
            }
        }
    }
    
    /**
     * Generates a single column of blocks.
     * 
     * @param chunk The chunk
     * @param x Local X coordinate
     * @param z Local Z coordinate
     */
    private void generateColumn(Chunk chunk, int x, int z) {
        // Bedrock layer (Y = 0)
        chunk.setBlock(x, 0, z, bedrockId);
        
        // Stone layers (Y = 1 to terrainHeight - 5)
        for (int y = 1; y < terrainHeight - 4; y++) {
            chunk.setBlock(x, y, z, stoneId);
        }
        
        // Dirt layers (Y = terrainHeight - 4 to terrainHeight - 1)
        for (int y = terrainHeight - 4; y < terrainHeight; y++) {
            chunk.setBlock(x, y, z, dirtId);
        }
        
        // Grass on top (Y = terrainHeight)
        chunk.setBlock(x, terrainHeight, z, grassId);
    }
    
    /**
     * Gets the terrain height.
     * 
     * @return The terrain height
     */
    public int getTerrainHeight() {
        return terrainHeight;
    }
    
    @Override
    public String getName() {
        return "Flat World Generator (height=" + terrainHeight + ")";
    }
}


