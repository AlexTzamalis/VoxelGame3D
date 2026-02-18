package me.alextzamalis.world;

import me.alextzamalis.voxel.BlockRegistry;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a reusable prefab structure that can be placed in the world.
 * 
 * <p>Prefabs are pre-defined structures (trees, buildings, caves, bridges, etc.)
 * that can be placed intelligently based on world patterns. This enables curated
 * proceduralism instead of pure random placement.
 * 
 * <p>Example prefabs:
 * <ul>
 *   <li>Oak Tree - 5x5x8 block structure</li>
 *   <li>Stone Bridge - spans gaps between shores</li>
 *   <li>Village House - 7x7x6 building</li>
 *   <li>Cave Entrance - natural cave opening</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class Prefab {
    
    /** The name/ID of this prefab. */
    private final String name;
    
    /** The size of this prefab (width, height, depth). */
    private final Vector3i size;
    
    /** Blocks in this prefab (relative positions + block IDs). */
    private final List<BlockEntry> blocks;
    
    /** Placement requirements (what patterns this prefab needs). */
    private final List<PlacementRequirement> requirements;
    
    /** Placement frequency (0.0 to 1.0, how often to place). */
    private float frequency;
    
    /**
     * Creates a new prefab.
     * 
     * @param name The prefab name/ID
     * @param width Width in blocks
     * @param height Height in blocks
     * @param depth Depth in blocks
     */
    public Prefab(String name, int width, int height, int depth) {
        this.name = name;
        this.size = new Vector3i(width, height, depth);
        this.blocks = new ArrayList<>();
        this.requirements = new ArrayList<>();
        this.frequency = 1.0f;
    }
    
    /**
     * Adds a block to this prefab at a relative position.
     * 
     * @param x Relative X (0 to width-1)
     * @param y Relative Y (0 to height-1)
     * @param z Relative Z (0 to depth-1)
     * @param blockId Block ID to place
     */
    public void addBlock(int x, int y, int z, int blockId) {
        if (x < 0 || x >= size.x || y < 0 || y >= size.y || z < 0 || z >= size.z) {
            throw new IllegalArgumentException("Block position out of bounds");
        }
        blocks.add(new BlockEntry(x, y, z, blockId));
    }
    
    /**
     * Adds a block by string ID.
     * 
     * @param x Relative X
     * @param y Relative Y
     * @param z Relative Z
     * @param blockStringId Block string ID (e.g., "minecraft:oak_log")
     */
    public void addBlock(int x, int y, int z, String blockStringId) {
        BlockRegistry registry = BlockRegistry.getInstance();
        int blockId = registry.getBlockId(blockStringId);
        addBlock(x, y, z, blockId);
    }
    
    /**
     * Adds a placement requirement.
     * 
     * @param requirement The requirement
     */
    public void addRequirement(PlacementRequirement requirement) {
        requirements.add(requirement);
    }
    
    /**
     * Sets the placement frequency.
     * 
     * @param frequency Frequency (0.0 = never, 1.0 = always when requirements met)
     */
    public void setFrequency(float frequency) {
        this.frequency = Math.max(0.0f, Math.min(1.0f, frequency));
    }
    
    /**
     * Gets the prefab name.
     * 
     * @return The name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the prefab size.
     * 
     * @return The size (width, height, depth)
     */
    public Vector3i getSize() {
        return size;
    }
    
    /**
     * Gets all blocks in this prefab.
     * 
     * @return List of block entries
     */
    public List<BlockEntry> getBlocks() {
        return blocks;
    }
    
    /**
     * Gets placement requirements.
     * 
     * @return List of requirements
     */
    public List<PlacementRequirement> getRequirements() {
        return requirements;
    }
    
    /**
     * Gets the placement frequency.
     * 
     * @return Frequency (0.0 to 1.0)
     */
    public float getFrequency() {
        return frequency;
    }
    
    /**
     * Represents a block entry in a prefab.
     */
    public static class BlockEntry {
        public final int x, y, z;
        public final int blockId;
        
        public BlockEntry(int x, int y, int z, int blockId) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockId = blockId;
        }
    }
    
    /**
     * Represents a placement requirement for a prefab.
     */
    public static class PlacementRequirement {
        public enum Type {
            FLAT_TERRAIN,      // Terrain must be flat
            NEAR_WATER,        // Must be near water
            ON_SOLID_GROUND,   // Must be on solid ground
            GAP_BETWEEN,       // Must span a gap (for bridges)
            CAVE_BELOW,        // Must have cave below
            HEIGHT_RANGE,      // Must be within height range
            SLOPE_MAX          // Maximum slope allowed
        }
        
        public final Type type;
        public final float value; // Additional parameter (e.g., max slope, height range)
        
        public PlacementRequirement(Type type, float value) {
            this.type = type;
            this.value = value;
        }
        
        public PlacementRequirement(Type type) {
            this(type, 0.0f);
        }
    }
}

