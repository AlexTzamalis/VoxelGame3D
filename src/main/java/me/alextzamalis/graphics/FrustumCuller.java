package me.alextzamalis.graphics;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

/**
 * Performs frustum culling to determine which objects are visible.
 * 
 * <p>Frustum culling is an optimization technique that determines which
 * objects are within the camera's view frustum (the visible area). Objects
 * outside the frustum can be skipped during rendering, significantly
 * improving performance.
 * 
 * <p>This class uses JOML's FrustumIntersection class for efficient
 * frustum-AABB intersection tests.
 * 
 * <p>Usage example:
 * <pre>{@code
 * FrustumCuller culler = new FrustumCuller();
 * culler.update(projectionMatrix, viewMatrix);
 * 
 * if (culler.isBoxInFrustum(minX, minY, minZ, maxX, maxY, maxZ)) {
 *     // Object is visible, render it
 * }
 * }</pre>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class FrustumCuller {
    
    /** The frustum intersection tester. */
    private final FrustumIntersection frustumIntersection;
    
    /** Combined projection-view matrix. */
    private final Matrix4f projViewMatrix;
    
    /**
     * Creates a new frustum culler.
     */
    public FrustumCuller() {
        this.frustumIntersection = new FrustumIntersection();
        this.projViewMatrix = new Matrix4f();
    }
    
    /**
     * Updates the frustum with the current projection and view matrices.
     * 
     * <p>This method should be called once per frame before performing
     * any visibility tests.
     * 
     * @param projectionMatrix The projection matrix
     * @param viewMatrix The view matrix
     */
    public void update(Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        // Combine projection and view matrices
        projectionMatrix.mul(viewMatrix, projViewMatrix);
        // Update frustum planes
        frustumIntersection.set(projViewMatrix);
    }
    
    /**
     * Tests if an axis-aligned bounding box is inside or intersects the frustum.
     * 
     * @param minX Minimum X coordinate
     * @param minY Minimum Y coordinate
     * @param minZ Minimum Z coordinate
     * @param maxX Maximum X coordinate
     * @param maxY Maximum Y coordinate
     * @param maxZ Maximum Z coordinate
     * @return true if the box is at least partially visible
     */
    public boolean isBoxInFrustum(float minX, float minY, float minZ, 
                                   float maxX, float maxY, float maxZ) {
        return frustumIntersection.testAab(minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    /**
     * Tests if a chunk is visible in the frustum.
     * 
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @param chunkWidth Width of the chunk in blocks
     * @param chunkHeight Height of the chunk in blocks
     * @param chunkDepth Depth of the chunk in blocks
     * @return true if the chunk is at least partially visible
     */
    public boolean isChunkInFrustum(int chunkX, int chunkZ, 
                                     int chunkWidth, int chunkHeight, int chunkDepth) {
        float minX = chunkX * chunkWidth;
        float minY = 0;
        float minZ = chunkZ * chunkDepth;
        float maxX = minX + chunkWidth;
        float maxY = chunkHeight;
        float maxZ = minZ + chunkDepth;
        
        return isBoxInFrustum(minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    /**
     * Tests if a sphere is inside or intersects the frustum.
     * 
     * @param x Center X coordinate
     * @param y Center Y coordinate
     * @param z Center Z coordinate
     * @param radius Sphere radius
     * @return true if the sphere is at least partially visible
     */
    public boolean isSphereInFrustum(float x, float y, float z, float radius) {
        return frustumIntersection.testSphere(x, y, z, radius);
    }
    
    /**
     * Tests if a point is inside the frustum.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return true if the point is visible
     */
    public boolean isPointInFrustum(float x, float y, float z) {
        return frustumIntersection.testPoint(x, y, z);
    }
}


