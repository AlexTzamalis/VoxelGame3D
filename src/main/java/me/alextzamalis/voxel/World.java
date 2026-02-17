package me.alextzamalis.voxel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.alextzamalis.graphics.Mesh;
import me.alextzamalis.graphics.PooledMesh;
import me.alextzamalis.graphics.TextureAtlas;
import me.alextzamalis.util.Logger;
import me.alextzamalis.world.WorldGenerator;

/**
 * Manages the voxel world including chunks, generation, and block access.
 * 
 * <p>The World class is the central manager for all world data. It handles:
 * <ul>
 *   <li>Chunk storage and retrieval</li>
 *   <li>Block access across chunk boundaries</li>
 *   <li>World generation via pluggable generators</li>
 *   <li>Chunk mesh building and updates</li>
 * </ul>
 * 
 * <p>Chunks are stored in a hash map keyed by their coordinates, allowing
 * efficient lookup and supporting infinite world generation.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class World {
    
    /** The world name. */
    private final String name;
    
    /** The world seed for generation. */
    private final long seed;
    
    /** Map of chunks keyed by their coordinate hash. */
    private final Map<Long, Chunk> chunks;
    
    /** The world generator. */
    private WorldGenerator generator;
    
    /** The chunk mesh builder. */
    private final ChunkMeshBuilder meshBuilder;
    
    /** The block registry. */
    private final BlockRegistry blockRegistry;
    
    /** The texture atlas for block textures. */
    private TextureAtlas textureAtlas;
    
    /** Chunk height constant for external access. */
    public static final int CHUNK_HEIGHT = Chunk.HEIGHT;
    
    /**
     * Creates a new world with the specified name and seed.
     * 
     * @param name The world name
     * @param seed The world seed
     */
    public World(String name, long seed) {
        this.name = name;
        this.seed = seed;
        this.chunks = new ConcurrentHashMap<>(); // Thread-safe for async loading
        this.meshBuilder = new ChunkMeshBuilder();
        this.blockRegistry = BlockRegistry.getInstance();
        
        Logger.info("Created world '%s' with seed %d", name, seed);
    }
    
    /**
     * Creates a new world with a random seed.
     * 
     * @param name The world name
     */
    public World(String name) {
        this(name, System.currentTimeMillis());
    }
    
    /**
     * Sets the world generator.
     * 
     * @param generator The world generator
     */
    public void setGenerator(WorldGenerator generator) {
        this.generator = generator;
        Logger.info("Set world generator: %s", generator.getClass().getSimpleName());
    }
    
    /**
     * Gets the world generator.
     * 
     * @return The world generator
     */
    public WorldGenerator getGenerator() {
        return generator;
    }
    
    /**
     * Sets the texture atlas for chunk mesh building.
     * 
     * @param atlas The texture atlas
     */
    public void setTextureAtlas(TextureAtlas atlas) {
        this.textureAtlas = atlas;
        meshBuilder.setTextureAtlas(atlas);
        Logger.info("Set texture atlas for world");
    }
    
    /**
     * Gets the texture atlas.
     * 
     * @return The texture atlas
     */
    public TextureAtlas getTextureAtlas() {
        return textureAtlas;
    }
    
    /**
     * Gets a block at the specified world coordinates.
     * 
     * @param x World X coordinate
     * @param y World Y coordinate
     * @param z World Z coordinate
     * @return The block ID, or 0 (air) if not loaded
     */
    public int getBlock(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return 0;
        }
        
        int chunkX = Chunk.worldToChunkX(x);
        int chunkZ = Chunk.worldToChunkZ(z);
        
        Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return 0;
        }
        
        int localX = Chunk.worldToLocalX(x);
        int localZ = Chunk.worldToLocalZ(z);
        
        return chunk.getBlock(localX, y, localZ);
    }
    
    /**
     * Sets a block at the specified world coordinates.
     * 
     * @param x World X coordinate
     * @param y World Y coordinate
     * @param z World Z coordinate
     * @param blockId The block ID to set
     */
    public void setBlock(int x, int y, int z, int blockId) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return;
        }
        
        int chunkX = Chunk.worldToChunkX(x);
        int chunkZ = Chunk.worldToChunkZ(z);
        
        Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return;
        }
        
        int localX = Chunk.worldToLocalX(x);
        int localZ = Chunk.worldToLocalZ(z);
        
        chunk.setBlock(localX, y, localZ, blockId);
        
        // Mark adjacent chunks as dirty if block is on edge
        if (localX == 0) markChunkDirty(chunkX - 1, chunkZ);
        if (localX == Chunk.WIDTH - 1) markChunkDirty(chunkX + 1, chunkZ);
        if (localZ == 0) markChunkDirty(chunkX, chunkZ - 1);
        if (localZ == Chunk.DEPTH - 1) markChunkDirty(chunkX, chunkZ + 1);
    }
    
    /**
     * Gets a chunk at the specified chunk coordinates.
     * 
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @return The chunk, or null if not loaded
     */
    public Chunk getChunk(int chunkX, int chunkZ) {
        return chunks.get(getChunkKey(chunkX, chunkZ));
    }
    
    /**
     * Gets or creates a chunk at the specified coordinates.
     * Thread-safe for async chunk loading.
     * 
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @return The chunk (created if necessary)
     */
    public Chunk getOrCreateChunk(int chunkX, int chunkZ) {
        long key = getChunkKey(chunkX, chunkZ);
        
        // Use computeIfAbsent for thread-safe creation
        return chunks.computeIfAbsent(key, k -> {
            Chunk newChunk = new Chunk(chunkX, chunkZ);
            newChunk.setWorld(this);
            return newChunk;
        });
    }
    
    /**
     * Loads chunks around a center position.
     * 
     * @param centerX Center X coordinate (world)
     * @param centerZ Center Z coordinate (world)
     * @param radius Radius in chunks
     */
    public void loadChunksAround(int centerX, int centerZ, int radius) {
        int centerChunkX = Chunk.worldToChunkX(centerX);
        int centerChunkZ = Chunk.worldToChunkZ(centerZ);
        
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int chunkX = centerChunkX + dx;
                int chunkZ = centerChunkZ + dz;
                
                Chunk chunk = getOrCreateChunk(chunkX, chunkZ);
                
                // Generate if not yet generated
                if (!chunk.isGenerated() && generator != null) {
                    generator.generateChunk(chunk, this);
                    chunk.setGenerated(true);
                }
                
                // Build mesh if dirty
                if (chunk.isDirty()) {
                    buildChunkMesh(chunk);
                }
            }
        }
    }
    
    /**
     * Builds the mesh for a chunk using traditional Mesh.
     * 
     * @param chunk The chunk
     */
    public void buildChunkMesh(Chunk chunk) {
        Mesh mesh = meshBuilder.buildMesh(chunk);
        chunk.setMesh(mesh);
    }
    
    /**
     * Builds the mesh for a chunk using a pooled mesh for efficiency.
     * 
     * @param chunk The chunk
     */
    public void buildChunkMeshPooled(Chunk chunk) {
        PooledMesh pooledMesh = meshBuilder.buildPooledMesh(chunk, chunk.getPooledMesh());
        chunk.setPooledMesh(pooledMesh);
    }
    
    /**
     * Marks a chunk as dirty (needs mesh rebuild).
     * 
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     */
    public void markChunkDirty(int chunkX, int chunkZ) {
        Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk != null) {
            chunk.markDirty();
        }
    }
    
    /**
     * Gets all loaded chunks.
     * 
     * @return Collection of all chunks
     */
    public Collection<Chunk> getChunks() {
        return chunks.values();
    }
    
    /**
     * Gets the number of loaded chunks.
     * 
     * @return Chunk count
     */
    public int getChunkCount() {
        return chunks.size();
    }
    
    /**
     * Generates a unique key for chunk coordinates.
     * 
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @return Unique long key
     */
    private long getChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
    
    /**
     * Gets the world name.
     * 
     * @return The world name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the world seed.
     * 
     * @return The world seed
     */
    public long getSeed() {
        return seed;
    }
    
    /**
     * Gets the block registry.
     * 
     * @return The block registry
     */
    public BlockRegistry getBlockRegistry() {
        return blockRegistry;
    }
    
    /**
     * Gets the highest non-air block at the specified position.
     * 
     * @param x World X coordinate
     * @param z World Z coordinate
     * @return The Y coordinate of the highest block, or -1 if all air
     */
    public int getHighestBlock(int x, int z) {
        int chunkX = Chunk.worldToChunkX(x);
        int chunkZ = Chunk.worldToChunkZ(z);
        Chunk chunk = getChunk(chunkX, chunkZ);
        
        if (chunk == null) {
            return -1;
        }
        
        int localX = Chunk.worldToLocalX(x);
        int localZ = Chunk.worldToLocalZ(z);
        
        return chunk.getHighestBlock(localX, localZ);
    }
    
    /**
     * Unloads a chunk by its key.
     * 
     * @param key The chunk key
     * @return true if the chunk was unloaded
     */
    public boolean unloadChunk(long key) {
        Chunk chunk = chunks.remove(key);
        if (chunk != null) {
            chunk.cleanup();
            return true;
        }
        return false;
    }
    
    /**
     * Unloads a chunk at the specified coordinates.
     * 
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @return true if the chunk was unloaded
     */
    public boolean unloadChunk(int chunkX, int chunkZ) {
        return unloadChunk(getChunkKey(chunkX, chunkZ));
    }
    
    /**
     * Unloads chunks that are beyond the specified distance from a center point.
     * 
     * @param centerChunkX Center chunk X
     * @param centerChunkZ Center chunk Z
     * @param maxDistance Maximum distance in chunks
     * @return Number of chunks unloaded
     */
    public int unloadDistantChunks(int centerChunkX, int centerChunkZ, int maxDistance) {
        int maxDistSq = maxDistance * maxDistance;
        List<Long> toUnload = new ArrayList<>();
        
        for (Chunk chunk : chunks.values()) {
            int dx = chunk.getChunkX() - centerChunkX;
            int dz = chunk.getChunkZ() - centerChunkZ;
            int distSq = dx * dx + dz * dz;
            
            if (distSq > maxDistSq) {
                toUnload.add(getChunkKey(chunk.getChunkX(), chunk.getChunkZ()));
            }
        }
        
        int unloaded = 0;
        for (Long key : toUnload) {
            if (unloadChunk(key)) {
                unloaded++;
            }
        }
        
        if (unloaded > 0) {
            Logger.debug("Unloaded %d distant chunks", unloaded);
        }
        
        return unloaded;
    }
    
    /**
     * Cleans up world resources.
     */
    public void cleanup() {
        Logger.info("Cleaning up world '%s' (%d chunks)...", name, chunks.size());
        for (Chunk chunk : chunks.values()) {
            chunk.cleanup();
        }
        chunks.clear();
    }
}

