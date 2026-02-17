package me.alextzamalis.graphics;

/**
 * Defines Level of Detail settings for chunk rendering.
 * 
 * <p>Each LOD level specifies:
 * <ul>
 *   <li>Distance range where this LOD applies</li>
 *   <li>Block sampling rate (higher = fewer blocks sampled)</li>
 *   <li>Whether to use simplified geometry</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public enum LODLevel {
    
    /**
     * Full detail - all blocks rendered.
     * Used for chunks closest to the player.
     */
    LOD_0(0, 4, 1, false),
    
    /**
     * High detail - every block rendered but simplified faces.
     * Used for nearby chunks.
     */
    LOD_1(4, 8, 1, true),
    
    /**
     * Medium detail - every other block sampled.
     * Used for medium-distance chunks.
     */
    LOD_2(8, 16, 2, true),
    
    /**
     * Low detail - every 4th block sampled.
     * Used for distant chunks.
     */
    LOD_3(16, 32, 4, true),
    
    /**
     * Very low detail - every 8th block sampled.
     * Used for very distant chunks (horizon).
     */
    LOD_4(32, Integer.MAX_VALUE, 8, true);
    
    /** Minimum distance in chunks for this LOD level. */
    private final int minDistance;
    
    /** Maximum distance in chunks for this LOD level. */
    private final int maxDistance;
    
    /** Block sampling rate (1 = every block, 2 = every other, etc.). */
    private final int sampleRate;
    
    /** Whether to use simplified geometry (skip internal faces). */
    private final boolean simplified;
    
    /**
     * Creates a LOD level.
     * 
     * @param minDistance Minimum distance in chunks
     * @param maxDistance Maximum distance in chunks
     * @param sampleRate Block sampling rate
     * @param simplified Whether to use simplified geometry
     */
    LODLevel(int minDistance, int maxDistance, int sampleRate, boolean simplified) {
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.sampleRate = sampleRate;
        this.simplified = simplified;
    }
    
    /**
     * Gets the minimum distance for this LOD level.
     * 
     * @return Minimum distance in chunks
     */
    public int getMinDistance() {
        return minDistance;
    }
    
    /**
     * Gets the maximum distance for this LOD level.
     * 
     * @return Maximum distance in chunks
     */
    public int getMaxDistance() {
        return maxDistance;
    }
    
    /**
     * Gets the block sampling rate.
     * 
     * @return Sample rate (1 = every block, 2 = every other, etc.)
     */
    public int getSampleRate() {
        return sampleRate;
    }
    
    /**
     * Checks if simplified geometry should be used.
     * 
     * @return true if simplified
     */
    public boolean isSimplified() {
        return simplified;
    }
    
    /**
     * Gets the appropriate LOD level for a given distance.
     * 
     * @param distanceInChunks Distance from player in chunks
     * @return The appropriate LOD level
     */
    public static LODLevel getLevelForDistance(int distanceInChunks) {
        for (LODLevel level : values()) {
            if (distanceInChunks >= level.minDistance && distanceInChunks < level.maxDistance) {
                return level;
            }
        }
        return LOD_4; // Default to lowest detail
    }
    
    /**
     * Gets the appropriate LOD level for a given squared distance.
     * More efficient than using sqrt for distance calculation.
     * 
     * @param distanceSquared Squared distance from player in chunks
     * @return The appropriate LOD level
     */
    public static LODLevel getLevelForDistanceSquared(int distanceSquared) {
        for (LODLevel level : values()) {
            int minSq = level.minDistance * level.minDistance;
            int maxSq = level.maxDistance == Integer.MAX_VALUE ? Integer.MAX_VALUE : level.maxDistance * level.maxDistance;
            if (distanceSquared >= minSq && distanceSquared < maxSq) {
                return level;
            }
        }
        return LOD_4;
    }
}

