package me.alextzamalis.voxel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import me.alextzamalis.util.Logger;
import me.alextzamalis.world.WorldGenerator;

/**
 * Manages asynchronous chunk generation and mesh building.
 * 
 * <p>This class handles chunk loading in background threads to prevent
 * frame drops when exploring new areas. The workflow is:
 * <ol>
 *   <li>Main thread identifies chunks that need loading</li>
 *   <li>Generation tasks are submitted to thread pool</li>
 *   <li>Worker threads generate chunk terrain data</li>
 *   <li>Main thread builds meshes from generated data (OpenGL requirement)</li>
 * </ol>
 * 
 * <p>Note: Mesh building must happen on the main thread due to OpenGL context
 * requirements. Only terrain generation is truly async.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class AsyncChunkManager {
    
    /** Number of worker threads for chunk generation. Uses all available cores for maximum performance. */
    private static final int WORKER_THREADS = Math.max(4, Runtime.getRuntime().availableProcessors());
    
    /** Maximum chunks to generate per frame. Increased for faster world loading. */
    private static final int MAX_GENERATIONS_PER_FRAME = 4;
    
    /** Maximum chunks to mesh per frame. Increased for faster world loading. */
    private static final int MAX_MESHES_PER_FRAME = 2;
    
    /** Frame counter for mesh building throttling. */
    private int meshBuildFrameCounter = 0;
    
    /** Build meshes every N frames to prevent render thread blocking. */
    private static final int MESH_BUILD_INTERVAL = 5; // Build meshes every 5 frames (more conservative)
    
    /** Maximum chunks to queue for meshing. Prevents unbounded queue growth. */
    private static final int MAX_CHUNKS_TO_MESH_QUEUE = 50; // Increased to prevent dropping chunks
    
    /** Maximum pending generation tasks. Reduced to prevent mesh pool exhaustion. */
    private static final int MAX_PENDING_TASKS = 32;
    
    /** The world being managed. */
    private final World world;
    
    /** Thread pool for async generation. */
    private final ExecutorService executorService;
    
    /** Chunks waiting to be generated. */
    private final Set<Long> pendingGeneration;
    
    /** Chunks currently being generated. */
    private final Map<Long, Future<Chunk>> generatingChunks;
    
    /** Chunks that have been generated and need meshing. */
    private final Queue<Chunk> chunksToMesh;
    
    /** Chunks that need their mesh rebuilt. */
    private final Set<Long> dirtyChunks;
    
    /** Current view distance in chunks. */
    private int viewDistance;
    
    /** Unload distance (chunks beyond this are unloaded). Increased to prevent premature unloading. */
    private int unloadDistance;
    
    /** Statistics. */
    private final AtomicInteger chunksGenerated = new AtomicInteger(0);
    private final AtomicInteger chunksMeshed = new AtomicInteger(0);
    
    /** Whether the manager is running. */
    private volatile boolean running;
    
    /**
     * Creates a new async chunk manager.
     * 
     * @param world The world to manage
     * @param viewDistance View distance in chunks
     */
    public AsyncChunkManager(World world, int viewDistance) {
        this.world = world;
        this.viewDistance = viewDistance;
        // Increase unload distance to 2.5x view distance to prevent chunks from being unloaded too close
        // This prevents chunks from disappearing when breaking blocks near the edge
        this.unloadDistance = (int) (viewDistance * 2.5f);
        
        this.executorService = Executors.newFixedThreadPool(WORKER_THREADS, r -> {
            Thread t = new Thread(r, "ChunkGenerator");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
        
        this.pendingGeneration = ConcurrentHashMap.newKeySet();
        this.generatingChunks = new ConcurrentHashMap<>();
        this.chunksToMesh = new ConcurrentLinkedQueue<>();
        this.dirtyChunks = ConcurrentHashMap.newKeySet();
        
        this.running = true;
        
        Logger.info("AsyncChunkManager initialized with %d worker threads", WORKER_THREADS);
    }
    
    /**
     * Updates chunk generation (can be called from any thread).
     * This handles chunk generation on worker threads.
     * 
     * @param playerChunkX Player's chunk X coordinate
     * @param playerChunkZ Player's chunk Z coordinate
     */
    public void updateGeneration(int playerChunkX, int playerChunkZ) {
        if (!running) return;
        
        // 1. Unload distant chunks
        unloadDistantChunks(playerChunkX, playerChunkZ);
        
        // 2. Identify chunks that need loading
        identifyChunksToLoad(playerChunkX, playerChunkZ);
        
        // 3. Check for completed generation tasks
        processCompletedGenerations();
    }
    
    /**
     * Updates mesh building (MUST be called from main thread with OpenGL context).
     * This builds meshes for generated chunks.
     * 
     * <p>Throttled to prevent blocking the render thread.
     */
    public void updateMeshes() {
        if (!running) return;
        
        // During loading, build meshes every frame (no throttling)
        // During gameplay, throttle to prevent render thread blocking
        boolean isDuringLoading = chunksToMesh.size() > 20; // If queue is large, we're likely loading
        
        if (!isDuringLoading) {
            // Throttle mesh building during gameplay
            meshBuildFrameCounter++;
            if (meshBuildFrameCounter < MESH_BUILD_INTERVAL) {
                return; // Skip this frame
            }
            meshBuildFrameCounter = 0;
        }
        
        // Build meshes for generated chunks (main thread only - requires OpenGL)
        buildPendingMeshes();
        
        // Rebuild dirty chunk meshes (main thread only - requires OpenGL)
        // Only rebuild one dirty chunk per cycle to prevent blocking
        rebuildDirtyMeshes();
    }
    
    /**
     * Updates the chunk manager. Call once per frame.
     * NOTE: This method calls updateMeshes() which requires OpenGL context.
     * Use updateGeneration() + updateMeshes() separately if you need to split threads.
     * 
     * @param playerChunkX Player's chunk X coordinate
     * @param playerChunkZ Player's chunk Z coordinate
     */
    public void update(int playerChunkX, int playerChunkZ) {
        updateGeneration(playerChunkX, playerChunkZ);
        updateMeshes(); // This requires OpenGL context (main thread)
    }
    
    /**
     * Unloads chunks that are too far from the player.
     */
    private void unloadDistantChunks(int playerChunkX, int playerChunkZ) {
        List<Chunk> toUnload = new ArrayList<>();
        
        for (Chunk chunk : world.getChunks()) {
            int dx = chunk.getChunkX() - playerChunkX;
            int dz = chunk.getChunkZ() - playerChunkZ;
            int distSq = dx * dx + dz * dz;
            
            // Only unload if:
            // 1. Chunk is beyond unload distance
            // 2. Chunk is not dirty (not being modified)
            // 3. Chunk is not in the mesh queue (not being processed)
            long key = getChunkKey(chunk.getChunkX(), chunk.getChunkZ());
            boolean isDirty = dirtyChunks.contains(key);
            boolean isInMeshQueue = chunksToMesh.contains(chunk);
            
            if (distSq > unloadDistance * unloadDistance && !isDirty && !isInMeshQueue) {
                toUnload.add(chunk);
            }
        }
        
        for (Chunk chunk : toUnload) {
            long key = getChunkKey(chunk.getChunkX(), chunk.getChunkZ());
            
            // Cancel any pending generation
            pendingGeneration.remove(key);
            Future<Chunk> future = generatingChunks.remove(key);
            if (future != null) {
                future.cancel(false);
            }
            dirtyChunks.remove(key);
            
            // Unload the chunk (this will release its mesh back to the pool)
            world.unloadChunk(chunk.getChunkX(), chunk.getChunkZ());
        }
    }
    
    /**
     * Identifies chunks that need to be loaded based on player position.
     */
    private void identifyChunksToLoad(int playerChunkX, int playerChunkZ) {
        // Use spiral pattern for loading (closest chunks first)
        int maxIndex = (viewDistance * 2 + 1) * (viewDistance * 2 + 1);
        int submitted = 0;
        
        for (int i = 0; i < maxIndex && submitted < MAX_GENERATIONS_PER_FRAME; i++) {
            int[] offset = getSpiralOffset(i);
            int chunkX = playerChunkX + offset[0];
            int chunkZ = playerChunkZ + offset[1];
            
            // Check if within view distance
            if (Math.abs(offset[0]) > viewDistance || Math.abs(offset[1]) > viewDistance) {
                continue;
            }
            
            long key = getChunkKey(chunkX, chunkZ);
            
            // Skip if already loaded, pending, or generating
            Chunk existing = world.getChunk(chunkX, chunkZ);
            if (existing != null && existing.isGenerated()) {
                continue;
            }
            if (pendingGeneration.contains(key) || generatingChunks.containsKey(key)) {
                continue;
            }
            
            // Check if we have room for more tasks
            if (pendingGeneration.size() + generatingChunks.size() >= MAX_PENDING_TASKS) {
                break;
            }
            
            // Submit generation task
            submitGenerationTask(chunkX, chunkZ);
            submitted++;
        }
    }
    
    /**
     * Submits a chunk generation task to the thread pool.
     */
    private void submitGenerationTask(int chunkX, int chunkZ) {
        long key = getChunkKey(chunkX, chunkZ);
        pendingGeneration.add(key);
        
        WorldGenerator generator = world.getGenerator();
        
        Future<Chunk> future = executorService.submit(() -> {
            try {
                // Create chunk
                Chunk chunk = new Chunk(chunkX, chunkZ);
                chunk.setWorld(world);
                
                // Generate terrain (this is the expensive part)
                if (generator != null) {
                    generator.generateChunk(chunk, world);
                }
                chunk.setGenerated(true);
                
                chunksGenerated.incrementAndGet();
                return chunk;
            } catch (Exception e) {
                Logger.error("Error generating chunk (%d, %d): %s", chunkX, chunkZ, e.getMessage());
                return null;
            } finally {
                pendingGeneration.remove(key);
            }
        });
        
        generatingChunks.put(key, future);
    }
    
    /**
     * Processes completed generation tasks.
     */
    private void processCompletedGenerations() {
        Iterator<Map.Entry<Long, Future<Chunk>>> it = generatingChunks.entrySet().iterator();
        
        while (it.hasNext()) {
            Map.Entry<Long, Future<Chunk>> entry = it.next();
            Future<Chunk> future = entry.getValue();
            
            if (future.isDone()) {
                it.remove();
                
                try {
                    Chunk chunk = future.get();
                    if (chunk != null) {
                        // Add to world
                        world.getChunks(); // Ensure we can access
                        // The chunk is already created, add it to the world's map
                        addChunkToWorld(chunk);
                        
                        // Queue for meshing
                        chunksToMesh.offer(chunk);
                    }
                } catch (Exception e) {
                    Logger.error("Error retrieving generated chunk: %s", e.getMessage());
                }
            }
        }
    }
    
    /**
     * Adds a chunk to the world's chunk map.
     */
    private void addChunkToWorld(Chunk chunk) {
        // Use reflection or a package-private method to add the chunk
        // For now, we'll use getOrCreateChunk which will return our chunk if we set it up right
        Chunk existing = world.getOrCreateChunk(chunk.getChunkX(), chunk.getChunkZ());
        
        // Copy data if needed (the getOrCreateChunk creates a new one)
        // This is a limitation - we need to modify World to accept pre-generated chunks
        // For now, regenerate in the existing chunk
        if (!existing.isGenerated() && world.getGenerator() != null) {
            world.getGenerator().generateChunk(existing, world);
            existing.setGenerated(true);
            chunksToMesh.offer(existing);
        }
    }
    
    /**
     * Builds meshes for generated chunks.
     * Very conservative to prevent render thread blocking.
     */
    private void buildPendingMeshes() {
        // Limit queue size to prevent unbounded growth
        while (chunksToMesh.size() > MAX_CHUNKS_TO_MESH_QUEUE) {
            Chunk chunk = chunksToMesh.poll();
            if (chunk != null) {
                Logger.debug("Dropping chunk from mesh queue (queue too large): (%d, %d)", 
                           chunk.getChunkX(), chunk.getChunkZ());
            }
        }
        
        // During loading (large queue), build more meshes per frame
        boolean isDuringLoading = chunksToMesh.size() > 20;
        int maxToBuild = isDuringLoading ? 10 : MAX_MESHES_PER_FRAME; // Build 10 per frame during loading
        
        int built = 0;
        
        // Build multiple meshes per call during loading to speed up world loading
        while (!chunksToMesh.isEmpty() && built < maxToBuild) {
            Chunk chunk = chunksToMesh.poll();
            if (chunk != null && chunk.isGenerated()) {
                try {
                    // Build mesh using pooled mesh system
                    world.buildChunkMeshPooled(chunk);
                    chunksMeshed.incrementAndGet();
                    built++;
                } catch (Exception e) {
                    // If mesh building fails (e.g., pool exhausted), re-queue the chunk
                    Logger.warn("Failed to build mesh for chunk (%d, %d): %s. Will retry later.", 
                               chunk.getChunkX(), chunk.getChunkZ(), e.getMessage());
                    // Don't re-queue if queue is too large
                    if (chunksToMesh.size() < MAX_CHUNKS_TO_MESH_QUEUE) {
                        chunksToMesh.offer(chunk);
                    }
                    break; // Stop trying this frame
                }
            }
        }
    }
    
    /**
     * Rebuilds meshes for dirty chunks.
     * Only rebuilds one chunk per call to prevent render thread blocking.
     */
    private void rebuildDirtyMeshes() {
        if (dirtyChunks.isEmpty()) {
            return;
        }
        
        // Only rebuild ONE dirty chunk per cycle to prevent blocking
        Iterator<Long> it = dirtyChunks.iterator();
        if (it.hasNext()) {
            Long key = it.next();
            it.remove();
            
            // Find the chunk (limit search to prevent blocking)
            int checked = 0;
            int maxChecks = 50; // Don't check more than 50 chunks
            
            for (Chunk chunk : world.getChunks()) {
                if (checked++ >= maxChecks) {
                    break; // Prevent blocking on large chunk collections
                }
                
                if (getChunkKey(chunk.getChunkX(), chunk.getChunkZ()) == key) {
                    if (chunk.isDirty()) {
                        try {
                            world.buildChunkMeshPooled(chunk);
                        } catch (Exception e) {
                            Logger.warn("Failed to rebuild mesh for dirty chunk (%d, %d): %s", 
                                       chunk.getChunkX(), chunk.getChunkZ(), e.getMessage());
                            // Re-add to dirty set for retry
                            dirtyChunks.add(key);
                        }
                    }
                    break;
                }
            }
        }
    }
    
    /**
     * Marks a chunk as dirty (needs mesh rebuild).
     * 
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     */
    public void markChunkDirty(int chunkX, int chunkZ) {
        dirtyChunks.add(getChunkKey(chunkX, chunkZ));
    }
    
    /**
     * Gets a spiral offset for the given index.
     */
    private int[] getSpiralOffset(int index) {
        if (index == 0) return new int[]{0, 0};
        
        int x = 0, z = 0;
        int dx = 0, dz = -1;
        int segmentLength = 1;
        int segmentPassed = 0;
        int direction = 0;
        
        for (int i = 0; i < index; i++) {
            if (segmentPassed == segmentLength) {
                segmentPassed = 0;
                direction = (direction + 1) % 4;
                if (direction == 0 || direction == 2) {
                    segmentLength++;
                }
            }
            
            switch (direction) {
                case 0: dx = 1; dz = 0; break;
                case 1: dx = 0; dz = 1; break;
                case 2: dx = -1; dz = 0; break;
                case 3: dx = 0; dz = -1; break;
            }
            
            x += dx;
            z += dz;
            segmentPassed++;
        }
        
        return new int[]{x, z};
    }
    
    /**
     * Generates a unique key for chunk coordinates.
     */
    private long getChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
    
    /**
     * Sets the view distance.
     * 
     * @param viewDistance New view distance in chunks
     */
    public void setViewDistance(int viewDistance) {
        this.viewDistance = viewDistance;
        // Increase unload distance to 2.5x view distance to prevent chunks from being unloaded too close
        // This prevents chunks from disappearing when breaking blocks near the edge
        this.unloadDistance = (int) (viewDistance * 2.5f);
    }
    
    /**
     * Gets statistics about chunk loading.
     * 
     * @return Statistics string
     */
    public String getStats() {
        return String.format("Chunks: gen=%d, mesh=%d, pending=%d, generating=%d, toMesh=%d, dirty=%d",
                            chunksGenerated.get(), chunksMeshed.get(),
                            pendingGeneration.size(), generatingChunks.size(),
                            chunksToMesh.size(), dirtyChunks.size());
    }
    
    /**
     * Gets the number of chunks waiting to be meshed.
     * 
     * @return Pending mesh count
     */
    public int getPendingMeshCount() {
        return chunksToMesh.size();
    }
    
    /**
     * Gets the number of chunks being generated.
     * 
     * @return Generating count
     */
    public int getGeneratingCount() {
        return generatingChunks.size() + pendingGeneration.size();
    }
    
    /**
     * Gets the total number of chunks generated so far.
     * 
     * @return Generated chunk count
     */
    public int getGeneratedChunkCount() {
        return chunksGenerated.get();
    }
    
    /**
     * Gets the total number of chunks meshed so far.
     * 
     * @return Meshed chunk count
     */
    public int getMeshedChunkCount() {
        return chunksMeshed.get();
    }
    
    /**
     * Checks if initial load is complete (all chunks in view distance are generated and meshed).
     * 
     * @return true if initial load is complete
     */
    public boolean isInitialLoadComplete() {
        // Check if there are no pending generations and no chunks waiting to be meshed
        // This is a simple heuristic - in practice, we'd track expected vs actual
        return pendingGeneration.isEmpty() && chunksToMesh.isEmpty() && generatingChunks.isEmpty();
    }
    
    /**
     * Shuts down the chunk manager.
     */
    public void shutdown() {
        running = false;
        
        // Cancel all pending tasks
        for (Future<Chunk> future : generatingChunks.values()) {
            future.cancel(true);
        }
        generatingChunks.clear();
        pendingGeneration.clear();
        chunksToMesh.clear();
        dirtyChunks.clear();
        
        // Shutdown executor
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        
        Logger.info("AsyncChunkManager shutdown. Stats: generated=%d, meshed=%d",
                   chunksGenerated.get(), chunksMeshed.get());
    }
}

