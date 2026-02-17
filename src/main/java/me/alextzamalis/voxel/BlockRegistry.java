package me.alextzamalis.voxel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.alextzamalis.util.Logger;

/**
 * Central registry for all block types in the game.
 * 
 * <p>This registry manages block type registration and lookup. It uses a
 * modular, data-driven approach where blocks can be registered from code
 * or loaded from configuration files.
 * 
 * <p>Each block is assigned a numeric ID for efficient storage in chunks,
 * while also supporting string-based lookups for flexibility.
 * 
 * <p>The registry follows a singleton pattern and initializes with default
 * vanilla blocks. Additional blocks can be registered by mods or plugins.
 * 
 * <p>Usage example:
 * <pre>{@code
 * BlockRegistry registry = BlockRegistry.getInstance();
 * Block dirt = registry.getBlock("minecraft:dirt");
 * int dirtId = registry.getBlockId("minecraft:dirt");
 * }</pre>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class BlockRegistry {
    
    /** Singleton instance. */
    private static BlockRegistry instance;
    
    /** Base path for vanilla textures. */
    private static final String VANILLA_TEXTURE_PATH = "/assets/textures/VanillaPack/Blocks/";
    
    /** Map of block string IDs to Block objects. */
    private final Map<String, Block> blocksByStringId;
    
    /** Map of numeric IDs to Block objects. */
    private final Map<Integer, Block> blocksByNumericId;
    
    /** Map of string IDs to numeric IDs. */
    private final Map<String, Integer> stringToNumericId;
    
    /** Counter for assigning numeric IDs. */
    private int nextNumericId;
    
    /** List of all registered blocks in order. */
    private final List<Block> allBlocks;
    
    /**
     * Private constructor for singleton pattern.
     */
    private BlockRegistry() {
        this.blocksByStringId = new HashMap<>();
        this.blocksByNumericId = new HashMap<>();
        this.stringToNumericId = new HashMap<>();
        this.allBlocks = new ArrayList<>();
        this.nextNumericId = 0;
        
        // Register default blocks
        registerDefaultBlocks();
    }
    
    /**
     * Gets the singleton instance.
     * 
     * @return The BlockRegistry instance
     */
    public static BlockRegistry getInstance() {
        if (instance == null) {
            instance = new BlockRegistry();
        }
        return instance;
    }
    
    /**
     * Registers the default vanilla blocks.
     */
    private void registerDefaultBlocks() {
        Logger.info("Registering default blocks...");
        
        // Air (special block, ID 0) - uses dirt texture as placeholder (never rendered)
        register(new Block.Builder("minecraft:air")
            .displayName("Air")
            .texture(VANILLA_TEXTURE_PATH + "dirt.png") // Placeholder, air is never rendered
            .solid(false)
            .transparent(true)
            .build());
        
        // Stone
        register(new Block.Builder("minecraft:stone")
            .displayName("Stone")
            .texture(VANILLA_TEXTURE_PATH + "stone.png")
            .hardness(1.5f)
            .build());
        
        // Dirt
        register(new Block.Builder("minecraft:dirt")
            .displayName("Dirt")
            .texture(VANILLA_TEXTURE_PATH + "dirt.png")
            .hardness(0.5f)
            .build());
        
        // Grass Block (different textures on different faces, green tint on top and sides)
        // Top texture is grayscale and gets tinted green
        // Side texture has grass overlay that also gets tinted for proper grass color
        register(new Block.Builder("minecraft:grass_block")
            .displayName("Grass Block")
            .textures(new BlockTextures(
                VANILLA_TEXTURE_PATH + "grass_block_top.png",   // top (grayscale, will be tinted)
                VANILLA_TEXTURE_PATH + "dirt.png",               // bottom
                VANILLA_TEXTURE_PATH + "grass_block_side.png"    // sides (combined texture with grass)
            ))
            .tints(Block.GRASS_TINT, Block.DEFAULT_TINT, Block.GRASS_TINT) // Green top, normal bottom, green sides
            .hardness(0.6f)
            .build());
        
        // Cobblestone
        register(new Block.Builder("minecraft:cobblestone")
            .displayName("Cobblestone")
            .texture(VANILLA_TEXTURE_PATH + "cobblestone.png")
            .hardness(2.0f)
            .build());
        
        // Oak Planks
        register(new Block.Builder("minecraft:oak_planks")
            .displayName("Oak Planks")
            .texture(VANILLA_TEXTURE_PATH + "oak_planks.png")
            .hardness(2.0f)
            .build());
        
        // Oak Log (different top/bottom and sides)
        register(new Block.Builder("minecraft:oak_log")
            .displayName("Oak Log")
            .textures(new BlockTextures(
                VANILLA_TEXTURE_PATH + "oak_log.png",  // top (using same for now)
                VANILLA_TEXTURE_PATH + "oak_log.png",  // bottom
                VANILLA_TEXTURE_PATH + "oak_log.png"   // sides
            ))
            .hardness(2.0f)
            .build());
        
        Logger.info("Registered %d blocks", allBlocks.size());
    }
    
    /**
     * Registers a new block type.
     * 
     * @param block The block to register
     * @return The numeric ID assigned to the block
     * @throws IllegalArgumentException if a block with the same ID already exists
     */
    public int register(Block block) {
        String stringId = block.getId();
        
        if (blocksByStringId.containsKey(stringId)) {
            throw new IllegalArgumentException("Block already registered: " + stringId);
        }
        
        int numericId = nextNumericId++;
        
        blocksByStringId.put(stringId, block);
        blocksByNumericId.put(numericId, block);
        stringToNumericId.put(stringId, numericId);
        allBlocks.add(block);
        
        Logger.debug("Registered block: %s (ID: %d)", stringId, numericId);
        
        return numericId;
    }
    
    /**
     * Gets a block by its string ID.
     * 
     * @param stringId The block's string ID (e.g., "minecraft:dirt")
     * @return The block, or null if not found
     */
    public Block getBlock(String stringId) {
        return blocksByStringId.get(stringId);
    }
    
    /**
     * Gets a block by its numeric ID.
     * 
     * @param numericId The block's numeric ID
     * @return The block, or null if not found
     */
    public Block getBlock(int numericId) {
        return blocksByNumericId.get(numericId);
    }
    
    /**
     * Gets the numeric ID for a block.
     * 
     * @param stringId The block's string ID
     * @return The numeric ID, or -1 if not found
     */
    public int getBlockId(String stringId) {
        Integer id = stringToNumericId.get(stringId);
        return id != null ? id : -1;
    }
    
    /**
     * Gets the air block.
     * 
     * @return The air block
     */
    public Block getAir() {
        return getBlock("minecraft:air");
    }
    
    /**
     * Gets the air block's numeric ID.
     * 
     * @return The air block ID (always 0)
     */
    public int getAirId() {
        return 0;
    }
    
    /**
     * Checks if a block ID represents air.
     * 
     * @param numericId The numeric ID to check
     * @return true if the ID represents air
     */
    public boolean isAir(int numericId) {
        return numericId == 0;
    }
    
    /**
     * Gets all registered blocks.
     * 
     * @return Unmodifiable list of all blocks
     */
    public List<Block> getAllBlocks() {
        return Collections.unmodifiableList(allBlocks);
    }
    
    /**
     * Gets the total number of registered blocks.
     * 
     * @return The block count
     */
    public int getBlockCount() {
        return allBlocks.size();
    }
    
    /**
     * Checks if a block is registered.
     * 
     * @param stringId The block's string ID
     * @return true if the block is registered
     */
    public boolean isRegistered(String stringId) {
        return blocksByStringId.containsKey(stringId);
    }
}

