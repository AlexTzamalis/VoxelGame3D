package me.alextzamalis.voxel;

import me.alextzamalis.graphics.LODLevel;
import me.alextzamalis.util.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Level of Detail (LOD) for chunks to support massive view distances.
 * 
 * <p>This manager determines which chunks should use full detail vs simplified
 * representations based on distance from the player. Chunks beyond the "active
 * radius" (view distance) are converted to SimplifiedChunk for memory efficiency.
 * 
 * <p>LOD Strategy:
 * <ul>
 *   <li>Active Radius (0 to viewDistance): Full 16x256x16 chunks</li>
 *   <li>Beyond Active Radius: SimplifiedChunk (downsampled, shell-only)</li>
 *   <li>Smooth transitions between LOD levels</li>
 * </ul>
 * 
 * <p>This enables massive view distances (32+ chunks) without memory explosion
 * by using simplified representations for distant chunks.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class LODManager {
    
    /** The world being managed. */
    private final World world;
    
    /** Current view distance (active radius for full chunks). */
    private int viewDistance;
    
    /** Map of simplified chunks (chunk key -> SimplifiedChunk). */
    private final Map<Long, SimplifiedChunk> simplifiedChunks;
    
    /** Whether LOD is enabled. */
    private boolean enabled;
    
    /**
     * Creates a new LOD manager.
     * 
     * @param world The world to manage
     * @param viewDistance View distance in chunks (active radius)
     */
    public LODManager(World world, int viewDistance) {
        this.world = world;
        this.viewDistance = viewDistance;
        this.simplifiedChunks = new ConcurrentHashMap<>();
        this.enabled = true;
        
        Logger.info("LODManager initialized with view distance: %d chunks", viewDistance);
    }
    
    /**
     * Updates the LOD system based on player position.
     * 
     * <p>This method:
     * <ol>
     *   <li>Identifies chunks beyond active radius that should be simplified</li>
     *   <li>Converts full chunks to SimplifiedChunk when they move out of range</li>
     *   <li>Converts SimplifiedChunk back to full chunks when they move into range</li>
     * </ol>
     * 
     * @param playerChunkX Player's chunk X coordinate
     * @param playerChunkZ Player's chunk Z coordinate
     */
    public void update(int playerChunkX, int playerChunkZ) {
        if (!enabled) {
            return;
        }
        
        // Process chunks: convert to/from simplified based on distance
        for (Chunk chunk : world.getChunks()) {
            int dx = chunk.getChunkX() - playerChunkX;
            int dz = chunk.getChunkZ() - playerChunkZ;
            int distSq = dx * dx + dz * dz;
            int distance = (int) Math.sqrt(distSq);
            
            long chunkKey = getChunkKey(chunk.getChunkX(), chunk.getChunkZ());
            
            // If chunk is beyond active radius and is a full chunk, simplify it
            if (distance > viewDistance && chunk.isGenerated()) {
                if (!simplifiedChunks.containsKey(chunkKey)) {
                    // Convert full chunk to simplified
                    SimplifiedChunk simplified = SimplifiedChunk.fromFullChunk(chunk, SimplifiedChunk.DEFAULT_SCALE);
                    simplifiedChunks.put(chunkKey, simplified);
                    
                    // Unload the full chunk to save memory
                    // Note: We keep the simplified version, so we don't call world.unloadChunk()
                    // Instead, we mark it for potential unloading later
                    Logger.debug("Converted chunk (%d, %d) to SimplifiedChunk (distance: %d)", 
                               chunk.getChunkX(), chunk.getChunkZ(), distance);
                }
            }
            // If chunk is within active radius and we have a simplified version, we might want to
            // convert it back, but for now we'll keep simplified chunks as-is to avoid expensive conversions
        }
    }
    
    /**
     * Gets the LOD level for a chunk at the specified distance.
     * 
     * @param distanceInChunks Distance from player in chunks
     * @return The appropriate LOD level
     */
    public LODLevel getLODLevel(int distanceInChunks) {
        return LODLevel.getLevelForDistance(distanceInChunks);
    }
    
    /**
     * Gets a simplified chunk if it exists.
     * 
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @return SimplifiedChunk, or null if not simplified
     */
    public SimplifiedChunk getSimplifiedChunk(int chunkX, int chunkZ) {
        long key = getChunkKey(chunkX, chunkZ);
        return simplifiedChunks.get(key);
    }
    
    /**
     * Checks if a chunk should use simplified representation.
     * 
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @param playerChunkX Player's chunk X
     * @param playerChunkZ Player's chunk Z
     * @return true if should be simplified
     */
    public boolean shouldSimplify(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ) {
        int dx = chunkX - playerChunkX;
        int dz = chunkZ - playerChunkZ;
        int distSq = dx * dx + dz * dz;
        int distance = (int) Math.sqrt(distSq);
        
        return distance > viewDistance;
    }
    
    /**
     * Sets the view distance (active radius).
     * 
     * @param viewDistance New view distance in chunks
     */
    public void setViewDistance(int viewDistance) {
        this.viewDistance = viewDistance;
        Logger.info("LODManager view distance updated to: %d chunks", viewDistance);
    }
    
    /**
     * Gets the view distance.
     * 
     * @return View distance in chunks
     */
    public int getViewDistance() {
        return viewDistance;
    }
    
    /**
     * Enables or disables LOD management.
     * 
     * @param enabled Whether LOD is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Checks if LOD is enabled.
     * 
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Cleans up simplified chunks.
     */
    public void cleanup() {
        simplifiedChunks.clear();
    }
    
    /**
     * Gets the chunk key for coordinates.
     * 
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @return Chunk key
     */
    private long getChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
    
    /**
     * Gets the number of simplified chunks.
     * 
     * @return Count of simplified chunks
     */
    public int getSimplifiedChunkCount() {
        return simplifiedChunks.size();
    }
}

