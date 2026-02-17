package me.alextzamalis.graphics;

import me.alextzamalis.util.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Object pool for PooledMesh instances to reduce GPU memory allocation overhead.
 * 
 * <p>This pool maintains a collection of pre-allocated PooledMesh objects that can
 * be acquired and released. When a mesh is needed, it's taken from the pool.
 * When no longer needed, it's returned to the pool for reuse.
 * 
 * <p>Benefits:
 * <ul>
 *   <li>Reduces OpenGL buffer allocations/deallocations</li>
 *   <li>Prevents GPU memory fragmentation</li>
 *   <li>Faster chunk mesh updates</li>
 *   <li>Predictable memory usage</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class MeshPool {
    
    /** Default initial pool size. */
    private static final int DEFAULT_INITIAL_SIZE = 64;
    
    /** Maximum pool size to prevent unbounded growth. Increased for better performance. */
    private static final int MAX_POOL_SIZE = 1024;
    
    /** Default vertex capacity for pooled meshes. */
    private static final int DEFAULT_MESH_CAPACITY = 8192;
    
    /** Singleton instance. */
    private static MeshPool instance;
    
    /** Available meshes ready for use. */
    private final Deque<PooledMesh> availableMeshes;
    
    /** All meshes created by this pool (for cleanup). */
    private final List<PooledMesh> allMeshes;
    
    /** Initial vertex capacity for new meshes. */
    private final int meshCapacity;
    
    /** Statistics. */
    private int totalAcquired;
    private int totalReleased;
    private int totalCreated;
    
    /**
     * Private constructor for singleton.
     */
    private MeshPool() {
        this(DEFAULT_INITIAL_SIZE, DEFAULT_MESH_CAPACITY);
    }
    
    /**
     * Creates a mesh pool with the specified parameters.
     * 
     * @param initialSize Initial number of meshes to pre-allocate
     * @param meshCapacity Initial vertex capacity for each mesh
     */
    private MeshPool(int initialSize, int meshCapacity) {
        this.availableMeshes = new ArrayDeque<>();
        this.allMeshes = new ArrayList<>();
        this.meshCapacity = meshCapacity;
        this.totalAcquired = 0;
        this.totalReleased = 0;
        this.totalCreated = 0;
        
        // Pre-allocate meshes
        for (int i = 0; i < initialSize; i++) {
            PooledMesh mesh = createNewMesh();
            availableMeshes.push(mesh);
        }
        
        Logger.info("MeshPool initialized with %d meshes (capacity: %d vertices each)", 
                   initialSize, meshCapacity);
    }
    
    /**
     * Gets the singleton instance, creating it if necessary.
     * 
     * @return The MeshPool instance
     */
    public static synchronized MeshPool getInstance() {
        if (instance == null) {
            instance = new MeshPool();
        }
        return instance;
    }
    
    /**
     * Initializes the pool with custom parameters.
     * Must be called before getInstance() if custom parameters are needed.
     * 
     * @param initialSize Initial number of meshes
     * @param meshCapacity Initial vertex capacity per mesh
     */
    public static synchronized void initialize(int initialSize, int meshCapacity) {
        if (instance != null) {
            Logger.warn("MeshPool already initialized, ignoring initialize call");
            return;
        }
        instance = new MeshPool(initialSize, meshCapacity);
    }
    
    /**
     * Acquires a mesh from the pool.
     * If no meshes are available, creates a new one.
     * 
     * @return A PooledMesh ready for use
     */
    public synchronized PooledMesh acquire() {
        PooledMesh mesh;
        
        if (availableMeshes.isEmpty()) {
            if (allMeshes.size() < MAX_POOL_SIZE) {
                mesh = createNewMesh();
                Logger.debug("MeshPool: Created new mesh (total: %d)", allMeshes.size());
            } else {
                // Pool is at max size - don't create more, wait briefly and try again
                // This prevents unbounded growth and GPU memory exhaustion
                Logger.warn("MeshPool at max capacity (%d), cannot create more meshes. Available: %d, In use: %d", 
                           MAX_POOL_SIZE, availableMeshes.size(), allMeshes.size() - availableMeshes.size());
                // Return null to indicate failure - caller should retry later
                return null;
            }
        } else {
            mesh = availableMeshes.pop();
        }
        
        mesh.acquire();
        totalAcquired++;
        return mesh;
    }
    
    /**
     * Releases a mesh back to the pool.
     * 
     * @param mesh The mesh to release
     */
    public synchronized void release(PooledMesh mesh) {
        if (mesh == null) {
            return;
        }
        
        mesh.release();
        
        // Only add back to available if under max size
        if (availableMeshes.size() < MAX_POOL_SIZE) {
            availableMeshes.push(mesh);
        } else {
            // Pool is full, destroy the mesh
            mesh.cleanup();
            allMeshes.remove(mesh);
        }
        
        totalReleased++;
    }
    
    /**
     * Creates a new mesh for the pool.
     */
    private PooledMesh createNewMesh() {
        PooledMesh mesh = new PooledMesh(meshCapacity);
        allMeshes.add(mesh);
        totalCreated++;
        return mesh;
    }
    
    /**
     * Gets the number of available meshes in the pool.
     * 
     * @return Available mesh count
     */
    public int getAvailableCount() {
        return availableMeshes.size();
    }
    
    /**
     * Gets the total number of meshes managed by the pool.
     * 
     * @return Total mesh count
     */
    public int getTotalCount() {
        return allMeshes.size();
    }
    
    /**
     * Gets pool statistics.
     * 
     * @return String with pool statistics
     */
    public String getStats() {
        int inUse = allMeshes.size() - availableMeshes.size();
        return String.format("MeshPool: %d total, %d in use, %d available (acquired: %d, released: %d, created: %d)",
                            allMeshes.size(), inUse, availableMeshes.size(),
                            totalAcquired, totalReleased, totalCreated);
    }
    
    /**
     * Cleans up all pool resources.
     */
    public synchronized void cleanup() {
        Logger.info("Cleaning up MeshPool (%d meshes)...", allMeshes.size());
        
        for (PooledMesh mesh : allMeshes) {
            mesh.cleanup();
        }
        
        allMeshes.clear();
        availableMeshes.clear();
        
        instance = null;
        
        Logger.info("MeshPool cleanup complete. Final stats: acquired=%d, released=%d, created=%d",
                   totalAcquired, totalReleased, totalCreated);
    }
}

