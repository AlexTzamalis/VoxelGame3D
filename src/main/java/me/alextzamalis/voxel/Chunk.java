package me.alextzamalis.voxel;

import me.alextzamalis.graphics.Mesh;
import me.alextzamalis.graphics.MeshPool;
import me.alextzamalis.graphics.PooledMesh;

/**
 * Represents a chunk of blocks in the voxel world.
 * 
 * <p>A chunk is a 16x256x16 section of the world that stores block data
 * and manages its own mesh for rendering. Chunks are the fundamental
 * unit of world storage and rendering optimization.
 * 
 * <p>Block data is stored as a flat array of block IDs (shorts) for
 * memory efficiency. The array is indexed as: y * (WIDTH * DEPTH) + z * WIDTH + x
 * 
 * <p>Chunks track their "dirty" state to know when the mesh needs rebuilding.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class Chunk {
    
    /** Chunk width (X axis). */
    public static final int WIDTH = 16;
    
    /** Chunk height (Y axis). */
    public static final int HEIGHT = 256;
    
    /** Chunk depth (Z axis). */
    public static final int DEPTH = 16;
    
    /** Total number of blocks in a chunk. */
    public static final int TOTAL_BLOCKS = WIDTH * HEIGHT * DEPTH;
    
    /** The chunk's X position in chunk coordinates. */
    private final int chunkX;
    
    /** The chunk's Z position in chunk coordinates. */
    private final int chunkZ;
    
    /** Block data stored as numeric IDs. */
    private final short[] blocks;
    
    /** The rendered mesh for this chunk (traditional, non-pooled). */
    private Mesh mesh;
    
    /** The pooled mesh for this chunk (memory-efficient). */
    private PooledMesh pooledMesh;
    
    /** Whether the chunk data has changed and needs mesh rebuild. */
    private boolean dirty;
    
    /** Whether this chunk has been generated. */
    private boolean generated;
    
    /** Reference to the world this chunk belongs to. */
    private World world;
    
    /**
     * Creates a new empty chunk at the specified position.
     * 
     * @param chunkX The chunk X coordinate
     * @param chunkZ The chunk Z coordinate
     */
    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.blocks = new short[TOTAL_BLOCKS];
        this.mesh = null;
        this.dirty = true;
        this.generated = false;
        this.world = null;
    }
    
    /**
     * Gets the block ID at the specified local position.
     * 
     * @param x Local X coordinate (0-15)
     * @param y Local Y coordinate (0-255)
     * @param z Local Z coordinate (0-15)
     * @return The block ID, or 0 (air) if out of bounds
     */
    public int getBlock(int x, int y, int z) {
        if (!isInBounds(x, y, z)) {
            return 0; // Air
        }
        return blocks[getIndex(x, y, z)] & 0xFFFF;
    }
    
    /**
     * Sets the block at the specified local position.
     * 
     * @param x Local X coordinate (0-15)
     * @param y Local Y coordinate (0-255)
     * @param z Local Z coordinate (0-15)
     * @param blockId The block ID to set
     */
    public void setBlock(int x, int y, int z, int blockId) {
        if (!isInBounds(x, y, z)) {
            return;
        }
        blocks[getIndex(x, y, z)] = (short) blockId;
        dirty = true;
    }
    
    /**
     * Calculates the array index for a block position.
     * 
     * @param x Local X coordinate
     * @param y Local Y coordinate
     * @param z Local Z coordinate
     * @return The array index
     */
    private int getIndex(int x, int y, int z) {
        return y * (WIDTH * DEPTH) + z * WIDTH + x;
    }
    
    /**
     * Checks if a position is within chunk bounds.
     * 
     * @param x Local X coordinate
     * @param y Local Y coordinate
     * @param z Local Z coordinate
     * @return true if in bounds
     */
    public static boolean isInBounds(int x, int y, int z) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT && z >= 0 && z < DEPTH;
    }
    
    /**
     * Converts world coordinates to chunk coordinates.
     * 
     * @param worldX World X coordinate
     * @return Chunk X coordinate
     */
    public static int worldToChunkX(int worldX) {
        return Math.floorDiv(worldX, WIDTH);
    }
    
    /**
     * Converts world coordinates to chunk coordinates.
     * 
     * @param worldZ World Z coordinate
     * @return Chunk Z coordinate
     */
    public static int worldToChunkZ(int worldZ) {
        return Math.floorDiv(worldZ, DEPTH);
    }
    
    /**
     * Converts world coordinates to local chunk coordinates.
     * 
     * @param worldX World X coordinate
     * @return Local X coordinate (0-15)
     */
    public static int worldToLocalX(int worldX) {
        return Math.floorMod(worldX, WIDTH);
    }
    
    /**
     * Converts world coordinates to local chunk coordinates.
     * 
     * @param worldZ World Z coordinate
     * @return Local Z coordinate (0-15)
     */
    public static int worldToLocalZ(int worldZ) {
        return Math.floorMod(worldZ, DEPTH);
    }
    
    /**
     * Gets the world X coordinate of a local position.
     * 
     * @param localX Local X coordinate
     * @return World X coordinate
     */
    public int getWorldX(int localX) {
        return chunkX * WIDTH + localX;
    }
    
    /**
     * Gets the world Z coordinate of a local position.
     * 
     * @param localZ Local Z coordinate
     * @return World Z coordinate
     */
    public int getWorldZ(int localZ) {
        return chunkZ * DEPTH + localZ;
    }
    
    /**
     * Gets the chunk X coordinate.
     * 
     * @return The chunk X coordinate
     */
    public int getChunkX() {
        return chunkX;
    }
    
    /**
     * Gets the chunk Z coordinate.
     * 
     * @return The chunk Z coordinate
     */
    public int getChunkZ() {
        return chunkZ;
    }
    
    /**
     * Gets the mesh for this chunk.
     * 
     * @return The mesh, or null if not built
     */
    public Mesh getMesh() {
        return mesh;
    }
    
    /**
     * Sets the mesh for this chunk.
     * 
     * @param mesh The new mesh
     */
    public void setMesh(Mesh mesh) {
        // Clean up old mesh
        if (this.mesh != null) {
            this.mesh.cleanup();
        }
        this.mesh = mesh;
        this.dirty = false;
    }
    
    /**
     * Gets the pooled mesh for this chunk.
     * 
     * @return The pooled mesh, or null if not set
     */
    public PooledMesh getPooledMesh() {
        return pooledMesh;
    }
    
    /**
     * Sets the pooled mesh for this chunk.
     * 
     * @param pooledMesh The pooled mesh to use
     */
    public void setPooledMesh(PooledMesh pooledMesh) {
        // Release old pooled mesh back to pool
        if (this.pooledMesh != null) {
            MeshPool.getInstance().release(this.pooledMesh);
        }
        this.pooledMesh = pooledMesh;
        this.dirty = false;
    }
    
    /**
     * Checks if the chunk has a pooled mesh.
     * 
     * @return true if has pooled mesh with data
     */
    public boolean hasPooledMesh() {
        return pooledMesh != null && pooledMesh.hasData();
    }
    
    /**
     * Checks if the chunk is dirty (needs mesh rebuild).
     * 
     * @return true if dirty
     */
    public boolean isDirty() {
        return dirty;
    }
    
    /**
     * Marks the chunk as dirty.
     */
    public void markDirty() {
        this.dirty = true;
    }
    
    /**
     * Marks the chunk as clean (mesh is up to date).
     */
    public void markClean() {
        this.dirty = false;
    }
    
    /**
     * Checks if the chunk has been generated.
     * 
     * @return true if generated
     */
    public boolean isGenerated() {
        return generated;
    }
    
    /**
     * Marks the chunk as generated.
     */
    public void setGenerated(boolean generated) {
        this.generated = generated;
    }
    
    /**
     * Gets the world this chunk belongs to.
     * 
     * @return The world
     */
    public World getWorld() {
        return world;
    }
    
    /**
     * Sets the world this chunk belongs to.
     * 
     * @param world The world
     */
    public void setWorld(World world) {
        this.world = world;
    }
    
    /**
     * Checks if the chunk has a mesh.
     * 
     * @return true if the chunk has a mesh
     */
    public boolean hasMesh() {
        return mesh != null;
    }
    
    /**
     * Cleans up chunk resources.
     */
    public void cleanup() {
        if (mesh != null) {
            mesh.cleanup();
            mesh = null;
        }
        if (pooledMesh != null) {
            MeshPool.getInstance().release(pooledMesh);
            pooledMesh = null;
        }
    }
    
    /**
     * Fills the chunk with a single block type.
     * 
     * @param blockId The block ID to fill with
     */
    public void fill(int blockId) {
        short id = (short) blockId;
        for (int i = 0; i < TOTAL_BLOCKS; i++) {
            blocks[i] = id;
        }
        dirty = true;
    }
    
    /**
     * Fills a layer of the chunk with a block type.
     * 
     * @param y The Y level to fill
     * @param blockId The block ID
     */
    public void fillLayer(int y, int blockId) {
        if (y < 0 || y >= HEIGHT) return;
        short id = (short) blockId;
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                blocks[getIndex(x, y, z)] = id;
            }
        }
        dirty = true;
    }
    
    /**
     * Gets the highest non-air block at the specified position.
     * 
     * @param x Local X coordinate
     * @param z Local Z coordinate
     * @return The Y coordinate of the highest block, or -1 if all air
     */
    public int getHighestBlock(int x, int z) {
        for (int y = HEIGHT - 1; y >= 0; y--) {
            if (getBlock(x, y, z) != 0) {
                return y;
            }
        }
        return -1;
    }
}

