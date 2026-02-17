package me.alextzamalis.world;

import me.alextzamalis.voxel.Chunk;
import me.alextzamalis.voxel.World;

/**
 * Scans the world for patterns that can be used for intelligent prefab placement.
 * 
 * <p>This class analyzes the terrain to detect patterns like:
 * <ul>
 *   <li>Flat terrain areas (good for villages)</li>
 *   <li>Gaps between shores (good for bridges)</li>
 *   <li>Caves below surface (affects tree placement)</li>
 *   <li>Water proximity (affects structure placement)</li>
 * </ul>
 * 
 * <p>Pattern matching enables context-aware generation instead of pure randomness.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class PatternMatcher {
    
    /** The world to analyze. */
    private final World world;
    
    /**
     * Creates a new pattern matcher.
     * 
     * @param world The world to analyze
     */
    public PatternMatcher(World world) {
        this.world = world;
    }
    
    /**
     * Checks if terrain is flat at the specified position.
     * 
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @param radius Radius to check (in blocks)
     * @param maxHeightDiff Maximum height difference allowed
     * @return true if terrain is flat
     */
    public boolean isFlatTerrain(int worldX, int worldZ, int radius, int maxHeightDiff) {
        int centerHeight = world.getHighestBlock(worldX, worldZ);
        if (centerHeight < 0) {
            return false;
        }
        
        // Check surrounding area
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = worldX + dx;
                int z = worldZ + dz;
                int height = world.getHighestBlock(x, z);
                
                if (height < 0 || Math.abs(height - centerHeight) > maxHeightDiff) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Checks if there's a gap between two shores (for bridge placement).
     * 
     * @param worldX World X coordinate (center of gap)
     * @param worldZ World Z coordinate (center of gap)
     * @param direction Direction to check (0 = X, 1 = Z)
     * @param maxGapWidth Maximum gap width
     * @return Gap width if found, or -1 if no gap
     */
    public int findGapBetweenShores(int worldX, int worldZ, int direction, int maxGapWidth) {
        // Check for water/air gap with solid ground on both sides
        int gapStart = -1;
        int gapEnd = -1;
        
        // Scan in the specified direction
        for (int offset = -maxGapWidth; offset <= maxGapWidth; offset++) {
            int x = direction == 0 ? worldX + offset : worldX;
            int z = direction == 1 ? worldZ + offset : worldZ;
            
            int height = world.getHighestBlock(x, z);
            int block = world.getBlock(x, height, z);
            
            // Check if this is water or air (gap)
            boolean isGap = block == 0 || block == world.getBlockRegistry().getBlockId("minecraft:water");
            
            if (isGap && gapStart == -1) {
                gapStart = offset;
            } else if (!isGap && gapStart != -1) {
                gapEnd = offset;
                break;
            }
        }
        
        if (gapStart != -1 && gapEnd != -1) {
            return gapEnd - gapStart;
        }
        
        return -1;
    }
    
    /**
     * Checks if there's a cave below the specified position.
     * 
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @param surfaceHeight Surface height
     * @param minCaveDepth Minimum cave depth
     * @return true if cave detected
     */
    public boolean hasCaveBelow(int worldX, int worldZ, int surfaceHeight, int minCaveDepth) {
        // Check for air pockets below surface
        int airCount = 0;
        for (int y = surfaceHeight - minCaveDepth; y < surfaceHeight; y++) {
            int block = world.getBlock(worldX, y, worldZ);
            if (block == 0) {
                airCount++;
            }
        }
        
        // If significant air below, likely a cave
        return airCount >= minCaveDepth / 2;
    }
    
    /**
     * Checks if position is near water.
     * 
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @param radius Search radius
     * @return true if water found nearby
     */
    public boolean isNearWater(int worldX, int worldZ, int radius) {
        int waterId = world.getBlockRegistry().getBlockId("minecraft:water");
        
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = worldX + dx;
                int z = worldZ + dz;
                int height = world.getHighestBlock(x, z);
                
                if (height >= 0) {
                    int block = world.getBlock(x, height, z);
                    if (block == waterId) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Gets the terrain slope at a position.
     * 
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @return Slope in degrees (0 = flat, 90 = vertical)
     */
    public float getTerrainSlope(int worldX, int worldZ) {
        int centerHeight = world.getHighestBlock(worldX, worldZ);
        if (centerHeight < 0) {
            return 0;
        }
        
        // Check neighbors
        int northHeight = world.getHighestBlock(worldX, worldZ - 1);
        int southHeight = world.getHighestBlock(worldX, worldZ + 1);
        int eastHeight = world.getHighestBlock(worldX + 1, worldZ);
        int westHeight = world.getHighestBlock(worldX - 1, worldZ);
        
        // Calculate max height difference
        int maxDiff = 0;
        if (northHeight >= 0) maxDiff = Math.max(maxDiff, Math.abs(centerHeight - northHeight));
        if (southHeight >= 0) maxDiff = Math.max(maxDiff, Math.abs(centerHeight - southHeight));
        if (eastHeight >= 0) maxDiff = Math.max(maxDiff, Math.abs(centerHeight - eastHeight));
        if (westHeight >= 0) maxDiff = Math.max(maxDiff, Math.abs(centerHeight - westHeight));
        
        // Convert to approximate slope (rough calculation)
        return (float) Math.toDegrees(Math.atan(maxDiff / 1.0));
    }
}

