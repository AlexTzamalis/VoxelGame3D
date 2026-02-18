package me.alextzamalis.world;

import me.alextzamalis.util.Logger;
import me.alextzamalis.voxel.Chunk;
import me.alextzamalis.voxel.World;

import java.util.List;
import java.util.Random;

/**
 * Heuristic placement engine for context-aware prefab placement.
 * 
 * <p>This engine analyzes the world before placing prefabs, using pattern
 * matching to make intelligent placement decisions instead of pure randomness.
 * 
 * <p>Examples:
 * <ul>
 *   <li>If two shores are 5 blocks apart → place a bridge prefab</li>
 *   <li>If there is a cave below → place a specific tree type above</li>
 *   <li>If flat terrain near water → place a village prefab</li>
 * </ul>
 * 
 * <p>This creates more interesting, curated worlds than pure noise-based generation.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class HeuristicPlacementEngine {
    
    /** The world to place prefabs in. */
    private final World world;
    
    /** Pattern matcher for analyzing terrain. */
    private final PatternMatcher patternMatcher;
    
    /** Prefab registry. */
    private final PrefabRegistry prefabRegistry;
    
    /** Random number generator (seeded). */
    private final Random random;
    
    /**
     * Creates a new heuristic placement engine.
     * 
     * @param world The world
     * @param seed Random seed
     */
    public HeuristicPlacementEngine(World world, long seed) {
        this.world = world;
        this.patternMatcher = new PatternMatcher(world);
        this.prefabRegistry = PrefabRegistry.getInstance();
        this.random = new Random(seed);
    }
    
    /**
     * Places prefabs in a chunk based on detected patterns.
     * 
     * <p>This method:
     * <ol>
     *   <li>Scans the chunk for patterns</li>
     *   <li>Matches patterns to prefab requirements</li>
     *   <li>Places prefabs intelligently based on context</li>
     * </ol>
     * 
     * @param chunk The chunk to decorate
     */
    public void placePrefabs(Chunk chunk) {
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();
        
        // Sample positions in the chunk (not every block, for performance)
        int sampleRate = 4; // Check every 4th block
        
        for (int x = 0; x < Chunk.WIDTH; x += sampleRate) {
            for (int z = 0; z < Chunk.DEPTH; z += sampleRate) {
                int worldX = chunk.getWorldX(x);
                int worldZ = chunk.getWorldZ(z);
                int surfaceHeight = world.getHighestBlock(worldX, worldZ);
                
                if (surfaceHeight < 0) {
                    continue; // No surface found
                }
                
                // Try to place a prefab at this position
                placePrefabAt(worldX, surfaceHeight, worldZ, chunk);
            }
        }
    }
    
    /**
     * Attempts to place a prefab at the specified position.
     * 
     * @param worldX World X coordinate
     * @param surfaceY Surface Y coordinate
     * @param worldZ World Z coordinate
     * @param chunk The chunk
     */
    private void placePrefabAt(int worldX, int surfaceY, int worldZ, Chunk chunk) {
        // Check for gap (bridge placement)
        int gapWidth = patternMatcher.findGapBetweenShores(worldX, worldZ, 0, 10);
        if (gapWidth > 0 && gapWidth <= 8) {
            Prefab bridge = prefabRegistry.getPrefab("stone_bridge");
            if (bridge != null && random.nextFloat() < bridge.getFrequency()) {
                placePrefab(bridge, worldX, surfaceY, worldZ, chunk);
                return;
            }
        }
        
        // Check for flat terrain (good for structures)
        if (patternMatcher.isFlatTerrain(worldX, worldZ, 3, 2)) {
            // Could place village, but for now skip (no village prefab yet)
        }
        
        // Check for cave below (affects tree placement)
        boolean hasCave = patternMatcher.hasCaveBelow(worldX, worldZ, surfaceY, 5);
        
        // Place trees on solid ground (not on caves)
        if (!hasCave && patternMatcher.getTerrainSlope(worldX, worldZ) < 30.0f) {
            Prefab tree = prefabRegistry.getPrefab("oak_tree");
            if (tree != null && random.nextFloat() < tree.getFrequency()) {
                placePrefab(tree, worldX, surfaceY + 1, worldZ, chunk);
                return;
            }
        }
        
        // Place cave entrances occasionally
        if (hasCave && random.nextFloat() < 0.05f) {
            Prefab cave = prefabRegistry.getPrefab("cave_entrance");
            if (cave != null) {
                placePrefab(cave, worldX, surfaceY, worldZ, chunk);
            }
        }
    }
    
    /**
     * Places a prefab at the specified world position.
     * 
     * @param prefab The prefab to place
     * @param worldX World X coordinate (bottom-left corner)
     * @param worldY World Y coordinate (bottom)
     * @param worldZ World Z coordinate (bottom-left corner)
     * @param chunk The chunk (for local coordinate conversion)
     */
    private void placePrefab(Prefab prefab, int worldX, int worldY, int worldZ, Chunk chunk) {
        // Check if prefab fits in bounds
        if (worldY + prefab.getSize().y >= Chunk.HEIGHT) {
            return; // Prefab too tall
        }
        
        // Place all blocks in the prefab
        for (Prefab.BlockEntry entry : prefab.getBlocks()) {
            int blockWorldX = worldX + entry.x;
            int blockWorldY = worldY + entry.y;
            int blockWorldZ = worldZ + entry.z;
            
            // Check if block is in this chunk or neighboring chunks
            int blockChunkX = Chunk.worldToChunkX(blockWorldX);
            int blockChunkZ = Chunk.worldToChunkZ(blockWorldZ);
            
            Chunk targetChunk = world.getChunk(blockChunkX, blockChunkZ);
            if (targetChunk == null) {
                continue; // Chunk not loaded, skip
            }
            
            int localX = Chunk.worldToLocalX(blockWorldX);
            int localZ = Chunk.worldToLocalZ(blockWorldZ);
            
            // Only place if target position is air or replaceable
            int existingBlock = targetChunk.getBlock(localX, blockWorldY, localZ);
            if (existingBlock == 0 || isReplaceable(existingBlock)) {
                targetChunk.setBlock(localX, blockWorldY, localZ, entry.blockId);
            }
        }
        
        Logger.debug("Placed prefab '%s' at (%d, %d, %d)", prefab.getName(), worldX, worldY, worldZ);
    }
    
    /**
     * Checks if a block is replaceable (can be overwritten by prefabs).
     * 
     * @param blockId Block ID
     * @return true if replaceable
     */
    private boolean isReplaceable(int blockId) {
        // Air, grass, flowers, etc. can be replaced
        // Stone, dirt, etc. should not be replaced (prefabs should respect terrain)
        return blockId == 0; // For now, only air is replaceable
    }
}

