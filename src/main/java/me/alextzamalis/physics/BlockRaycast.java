package me.alextzamalis.physics;

import me.alextzamalis.voxel.BlockFace;
import me.alextzamalis.voxel.World;
import org.joml.Vector3f;
import org.joml.Vector3i;

/**
 * Performs raycasting to find blocks in the world.
 * 
 * <p>This class uses a voxel traversal algorithm (DDA - Digital Differential Analyzer)
 * to efficiently find the first solid block along a ray. It's used for:
 * <ul>
 *   <li>Block selection (highlighting the block player is looking at)</li>
 *   <li>Block breaking (left click)</li>
 *   <li>Block placing (right click on adjacent face)</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class BlockRaycast {
    
    /** The result of a raycast operation. */
    public static class RaycastResult {
        /** Whether a block was hit. */
        public final boolean hit;
        
        /** The position of the hit block. */
        public final Vector3i blockPos;
        
        /** The position for placing a new block (adjacent to hit face). */
        public final Vector3i placePos;
        
        /** The face that was hit. */
        public final BlockFace hitFace;
        
        /** The exact hit point in world coordinates. */
        public final Vector3f hitPoint;
        
        /** The distance to the hit point. */
        public final float distance;
        
        /**
         * Creates a miss result.
         */
        public RaycastResult() {
            this.hit = false;
            this.blockPos = null;
            this.placePos = null;
            this.hitFace = null;
            this.hitPoint = null;
            this.distance = Float.MAX_VALUE;
        }
        
        /**
         * Creates a hit result.
         */
        public RaycastResult(Vector3i blockPos, Vector3i placePos, BlockFace hitFace, 
                            Vector3f hitPoint, float distance) {
            this.hit = true;
            this.blockPos = blockPos;
            this.placePos = placePos;
            this.hitFace = hitFace;
            this.hitPoint = hitPoint;
            this.distance = distance;
        }
    }
    
    /**
     * Casts a ray from origin in the given direction and returns the first solid block hit.
     * 
     * <p>Uses DDA (Digital Differential Analyzer) algorithm for efficient voxel traversal.
     * 
     * @param world The world to raycast in
     * @param origin The ray origin (usually camera position)
     * @param direction The ray direction (normalized)
     * @param maxDistance Maximum distance to check
     * @return The raycast result
     */
    public static RaycastResult cast(World world, Vector3f origin, Vector3f direction, float maxDistance) {
        // Normalize direction
        Vector3f dir = new Vector3f(direction).normalize();
        
        // Current voxel position
        int x = (int) Math.floor(origin.x);
        int y = (int) Math.floor(origin.y);
        int z = (int) Math.floor(origin.z);
        
        // Direction signs
        int stepX = dir.x >= 0 ? 1 : -1;
        int stepY = dir.y >= 0 ? 1 : -1;
        int stepZ = dir.z >= 0 ? 1 : -1;
        
        // Distance to next voxel boundary
        float tMaxX = intBound(origin.x, dir.x);
        float tMaxY = intBound(origin.y, dir.y);
        float tMaxZ = intBound(origin.z, dir.z);
        
        // Distance along ray for one voxel step in each direction
        float tDeltaX = stepX / dir.x;
        float tDeltaY = stepY / dir.y;
        float tDeltaZ = stepZ / dir.z;
        
        // Handle infinite values
        if (Float.isInfinite(tDeltaX)) tDeltaX = Float.MAX_VALUE;
        if (Float.isInfinite(tDeltaY)) tDeltaY = Float.MAX_VALUE;
        if (Float.isInfinite(tDeltaZ)) tDeltaZ = Float.MAX_VALUE;
        
        // Track which face we entered through
        BlockFace hitFace = null;
        float t = 0;
        
        // Previous position for calculating place position
        int prevX = x, prevY = y, prevZ = z;
        
        // Traverse voxels
        while (t < maxDistance) {
            // Check current voxel
            int blockId = world.getBlock(x, y, z);
            if (blockId != 0) {
                // Hit a solid block
                Vector3f hitPoint = new Vector3f(
                    origin.x + dir.x * t,
                    origin.y + dir.y * t,
                    origin.z + dir.z * t
                );
                
                return new RaycastResult(
                    new Vector3i(x, y, z),
                    new Vector3i(prevX, prevY, prevZ),
                    hitFace,
                    hitPoint,
                    t
                );
            }
            
            // Save previous position for place position
            prevX = x;
            prevY = y;
            prevZ = z;
            
            // Step to next voxel
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    t = tMaxX;
                    x += stepX;
                    tMaxX += tDeltaX;
                    hitFace = stepX > 0 ? BlockFace.WEST : BlockFace.EAST;
                } else {
                    t = tMaxZ;
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                    hitFace = stepZ > 0 ? BlockFace.NORTH : BlockFace.SOUTH;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    t = tMaxY;
                    y += stepY;
                    tMaxY += tDeltaY;
                    hitFace = stepY > 0 ? BlockFace.BOTTOM : BlockFace.TOP;
                } else {
                    t = tMaxZ;
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                    hitFace = stepZ > 0 ? BlockFace.NORTH : BlockFace.SOUTH;
                }
            }
        }
        
        // No hit within max distance
        return new RaycastResult();
    }
    
    /**
     * Calculates the distance to the next integer boundary along an axis.
     */
    private static float intBound(float s, float ds) {
        if (ds < 0) {
            return intBound(-s, -ds);
        }
        
        // Find the next integer boundary
        float sFloor = (float) Math.floor(s);
        return (sFloor + 1 - s) / ds;
    }
}


