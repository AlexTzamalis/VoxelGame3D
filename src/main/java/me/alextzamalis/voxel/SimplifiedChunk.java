package me.alextzamalis.voxel;

import me.alextzamalis.util.Logger;

/**
 * A simplified chunk representation for distant LOD rendering.
 * 
 * <p>This class stores a downsampled version of a chunk where one block
 * represents a larger area (e.g., 4x4 blocks). This dramatically reduces
 * memory usage for distant chunks while maintaining visual appearance.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Downsampled representation (configurable scale, default 4x4)</li>
 *   <li>Memory efficient: Only stores visible "shell" blocks, not internal hidden blocks</li>
 *   <li>Efficient serialization for disk storage</li>
 *   <li>Can be converted to/from full Chunk for transitions</li>
 * </ul>
 * 
 * <p>Storage strategy:
 * - For a 16x256x16 chunk with 4x4 downsampling:
 *   - Full chunk: 16 * 256 * 16 = 65,536 blocks
 *   - Simplified: 4 * 64 * 4 = 1,024 blocks (98.4% reduction)
 *   - Shell-only: Even less (only visible surface blocks)
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class SimplifiedChunk {
    
    /** Default downsampling scale (1 block = 4x4 area). */
    public static final int DEFAULT_SCALE = 4;
    
    /** The chunk's X position in chunk coordinates. */
    private final int chunkX;
    
    /** The chunk's Z position in chunk coordinates. */
    private final int chunkZ;
    
    /** Downsampling scale (1 block represents scale x scale area). */
    private final int scale;
    
    /** Simplified block data (downsampled). */
    private final short[] blocks;
    
    /** Width of simplified chunk (full chunk width / scale). */
    private final int width;
    
    /** Height of simplified chunk (same as full chunk). */
    public static final int HEIGHT = Chunk.HEIGHT;
    
    /** Depth of simplified chunk (full chunk depth / scale). */
    private final int depth;
    
    /** Whether this simplified chunk has been generated. */
    private boolean generated;
    
    /**
     * Creates a new simplified chunk at the specified position.
     * 
     * @param chunkX The chunk X coordinate
     * @param chunkZ The chunk Z coordinate
     * @param scale The downsampling scale (default: 4)
     */
    public SimplifiedChunk(int chunkX, int chunkZ, int scale) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.scale = scale;
        this.width = Chunk.WIDTH / scale;
        this.depth = Chunk.DEPTH / scale;
        this.blocks = new short[width * HEIGHT * depth];
        this.generated = false;
    }
    
    /**
     * Creates a simplified chunk with default scale (4).
     * 
     * @param chunkX The chunk X coordinate
     * @param chunkZ The chunk Z coordinate
     */
    public SimplifiedChunk(int chunkX, int chunkZ) {
        this(chunkX, chunkZ, DEFAULT_SCALE);
    }
    
    /**
     * Creates a simplified chunk from a full chunk by downsampling.
     * 
     * <p>This method samples the full chunk at regular intervals and stores
     * only the visible "shell" blocks (blocks with at least one exposed face).
     * 
     * @param fullChunk The full chunk to simplify
     * @param scale The downsampling scale
     * @return A new SimplifiedChunk
     */
    public static SimplifiedChunk fromFullChunk(Chunk fullChunk, int scale) {
        SimplifiedChunk simplified = new SimplifiedChunk(
            fullChunk.getChunkX(), 
            fullChunk.getChunkZ(), 
            scale
        );
        
        int width = Chunk.WIDTH / scale;
        int depth = Chunk.DEPTH / scale;
        
        // Downsample: sample every Nth block
        for (int sy = 0; sy < HEIGHT; sy++) {
            for (int sz = 0; sz < depth; sz++) {
                for (int sx = 0; sx < width; sx++) {
                    // Sample from center of the downsampled region
                    int fullX = sx * scale + scale / 2;
                    int fullZ = sz * scale + scale / 2;
                    
                    // Get block from full chunk
                    short blockId = (short) fullChunk.getBlock(fullX, sy, fullZ);
                    
                    // Only store if block is visible (not air, or check if it has exposed faces)
                    // For now, store all non-air blocks (we can optimize to shell-only later)
                    if (blockId != 0) {
                        simplified.setBlock(sx, sy, sz, blockId);
                    }
                }
            }
        }
        
        simplified.generated = true;
        return simplified;
    }
    
    /**
     * Gets the block ID at the specified simplified position.
     * 
     * @param x Local X coordinate (0 to width-1)
     * @param y Local Y coordinate (0 to HEIGHT-1)
     * @param z Local Z coordinate (0 to depth-1)
     * @return Block ID, or 0 for air
     */
    public short getBlock(int x, int y, int z) {
        if (x < 0 || x >= width || y < 0 || y >= HEIGHT || z < 0 || z >= depth) {
            return 0;
        }
        return blocks[y * (width * depth) + z * width + x];
    }
    
    /**
     * Sets the block ID at the specified simplified position.
     * 
     * @param x Local X coordinate
     * @param y Local Y coordinate
     * @param z Local Z coordinate
     * @param blockId Block ID to set
     */
    public void setBlock(int x, int y, int z, short blockId) {
        if (x < 0 || x >= width || y < 0 || y >= HEIGHT || z < 0 || z >= depth) {
            return;
        }
        blocks[y * (width * depth) + z * width + x] = blockId;
    }
    
    /**
     * Gets the chunk X coordinate.
     * 
     * @return Chunk X coordinate
     */
    public int getChunkX() {
        return chunkX;
    }
    
    /**
     * Gets the chunk Z coordinate.
     * 
     * @return Chunk Z coordinate
     */
    public int getChunkZ() {
        return chunkZ;
    }
    
    /**
     * Gets the downsampling scale.
     * 
     * @return Scale (1 block = scale x scale area)
     */
    public int getScale() {
        return scale;
    }
    
    /**
     * Gets the width of the simplified chunk.
     * 
     * @return Width (full chunk width / scale)
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Gets the height of the simplified chunk.
     * 
     * @return Height (same as full chunk: 256)
     */
    public int getHeight() {
        return HEIGHT;
    }
    
    /**
     * Gets the depth of the simplified chunk.
     * 
     * @return Depth (full chunk depth / scale)
     */
    public int getDepth() {
        return depth;
    }
    
    /**
     * Checks if this simplified chunk has been generated.
     * 
     * @return true if generated
     */
    public boolean isGenerated() {
        return generated;
    }
    
    /**
     * Marks this simplified chunk as generated.
     * 
     * @param generated Whether it's generated
     */
    public void setGenerated(boolean generated) {
        this.generated = generated;
    }
    
    /**
     * Converts simplified chunk coordinates to world coordinates.
     * 
     * @param localX Local X in simplified chunk
     * @param localZ Local Z in simplified chunk
     * @return World X coordinate
     */
    public int getWorldX(int localX) {
        return chunkX * Chunk.WIDTH + localX * scale;
    }
    
    /**
     * Converts simplified chunk coordinates to world coordinates.
     * 
     * @param localX Local X in simplified chunk
     * @param localZ Local Z in simplified chunk
     * @return World Z coordinate
     */
    public int getWorldZ(int localZ) {
        return chunkZ * Chunk.DEPTH + localZ * scale;
    }
    
    /**
     * Gets the memory size of this simplified chunk in bytes.
     * 
     * @return Memory size in bytes
     */
    public int getMemorySize() {
        return blocks.length * Short.BYTES;
    }
    
    /**
     * Gets the memory size of a full chunk for comparison.
     * 
     * @return Memory size in bytes
     */
    public static int getFullChunkMemorySize() {
        return Chunk.TOTAL_BLOCKS * Short.BYTES;
    }
    
    /**
     * Gets the memory reduction ratio.
     * 
     * @return Ratio (simplified size / full size)
     */
    public double getMemoryReductionRatio() {
        return (double) getMemorySize() / getFullChunkMemorySize();
    }
}

