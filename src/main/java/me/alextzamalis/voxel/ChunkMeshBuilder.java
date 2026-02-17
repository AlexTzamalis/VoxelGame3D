package me.alextzamalis.voxel;

import me.alextzamalis.graphics.Mesh;
import me.alextzamalis.graphics.MeshPool;
import me.alextzamalis.graphics.PooledMesh;
import me.alextzamalis.graphics.TextureAtlas;
import me.alextzamalis.util.Logger;

/**
 * Builds optimized meshes for chunks using face culling and texture atlas.
 * 
 * <p>This class generates meshes for chunks by only creating faces that are
 * visible (not adjacent to solid blocks). Includes support for tint colors
 * for blocks like grass and leaves.
 * 
 * <p>Performance optimizations:
 * <ul>
 *   <li>Uses primitive arrays instead of boxed Lists to avoid GC pressure</li>
 *   <li>Pre-allocates arrays based on worst-case estimates</li>
 *   <li>Reuses vertex/normal/UV arrays to minimize allocations</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class ChunkMeshBuilder {
    
    // Pre-allocated arrays for building mesh data
    // Worst case: every block has all 6 faces visible (surface only scenario)
    // Realistic estimate: ~10% of blocks are surface blocks with ~3 visible faces average
    // 16*256*16 = 65536 blocks, ~6500 surface blocks, ~20000 faces, 4 verts each = 80000 vertices
    private static final int INITIAL_VERTEX_CAPACITY = 80000;
    private static final int INITIAL_INDEX_CAPACITY = 120000; // 6 indices per face (2 triangles)
    
    /** Position data (x, y, z per vertex). */
    private float[] positions;
    
    /** Texture coordinate data (u, v per vertex). */
    private float[] texCoords;
    
    /** Normal data (nx, ny, nz per vertex). */
    private float[] normals;
    
    /** Tint color data (r, g, b per vertex). */
    private float[] tints;
    
    /** Index data. */
    private int[] indices;
    
    /** Current position in position array. */
    private int posIndex;
    
    /** Current position in texCoord array. */
    private int texIndex;
    
    /** Current position in normal array. */
    private int normIndex;
    
    /** Current position in tint array. */
    private int tintIndex;
    
    /** Current position in index array. */
    private int indIndex;
    
    /** Current vertex count for indexing. */
    private int vertexCount;
    
    /** The block registry for looking up block properties. */
    private final BlockRegistry blockRegistry;
    
    /** The texture atlas for UV lookups. */
    private TextureAtlas textureAtlas;
    
    // Pre-allocated temporary arrays to avoid allocations in hot path
    private final float[] tempVertices = new float[12]; // 4 vertices * 3 components
    private final float[] tempUVs = new float[8];       // 4 vertices * 2 components
    
    // Cached normal vectors (never change)
    private static final float[][] FACE_NORMALS = {
        {0, 1, 0},   // TOP
        {0, -1, 0},  // BOTTOM
        {0, 0, -1},  // NORTH
        {0, 0, 1},   // SOUTH
        {1, 0, 0},   // EAST
        {-1, 0, 0}   // WEST
    };
    
    /**
     * Creates a new chunk mesh builder with pre-allocated buffers.
     */
    public ChunkMeshBuilder() {
        this.positions = new float[INITIAL_VERTEX_CAPACITY * 3];
        this.texCoords = new float[INITIAL_VERTEX_CAPACITY * 2];
        this.normals = new float[INITIAL_VERTEX_CAPACITY * 3];
        this.tints = new float[INITIAL_VERTEX_CAPACITY * 3];
        this.indices = new int[INITIAL_INDEX_CAPACITY];
        this.blockRegistry = BlockRegistry.getInstance();
    }
    
    /**
     * Sets the texture atlas for UV coordinate lookups.
     */
    public void setTextureAtlas(TextureAtlas atlas) {
        this.textureAtlas = atlas;
    }
    
    /**
     * Builds a mesh for the specified chunk.
     * 
     * @param chunk The chunk to build a mesh for
     * @return The mesh, or null if the chunk has no visible faces
     */
    public Mesh buildMesh(Chunk chunk) {
        // Reset indices
        posIndex = 0;
        texIndex = 0;
        normIndex = 0;
        tintIndex = 0;
        indIndex = 0;
        vertexCount = 0;
        
        // Iterate through all blocks in the chunk
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int x = 0; x < Chunk.WIDTH; x++) {
                    int blockId = chunk.getBlock(x, y, z);
                    
                    // Skip air blocks
                    if (blockId == 0) {
                        continue;
                    }
                    
                    Block block = blockRegistry.getBlock(blockId);
                    if (block == null || block.isAir()) {
                        continue;
                    }
                    
                    // Calculate world position for this block
                    float worldX = chunk.getWorldX(x);
                    float worldZ = chunk.getWorldZ(z);
                    
                    // Check each face for visibility
                    addVisibleFaces(chunk, x, y, z, worldX, y, worldZ, block);
                }
            }
        }
        
        // Return null if no faces were generated
        if (posIndex == 0) {
            return null;
        }
        
        // Create trimmed arrays for the actual mesh
        float[] finalPositions = new float[posIndex];
        float[] finalTexCoords = new float[texIndex];
        float[] finalNormals = new float[normIndex];
        float[] finalTints = new float[tintIndex];
        int[] finalIndices = new int[indIndex];
        
        System.arraycopy(positions, 0, finalPositions, 0, posIndex);
        System.arraycopy(texCoords, 0, finalTexCoords, 0, texIndex);
        System.arraycopy(normals, 0, finalNormals, 0, normIndex);
        System.arraycopy(tints, 0, finalTints, 0, tintIndex);
        System.arraycopy(indices, 0, finalIndices, 0, indIndex);
        
        return new Mesh(finalPositions, finalTexCoords, finalNormals, finalTints, finalIndices);
    }
    
    /**
     * Builds mesh data into a PooledMesh for memory efficiency.
     * 
     * @param chunk The chunk to build a mesh for
     * @param pooledMesh The pooled mesh to update (or null to acquire from pool)
     * @return The updated pooled mesh, or null if chunk has no visible faces
     */
    public PooledMesh buildPooledMesh(Chunk chunk, PooledMesh pooledMesh) {
        // Reset indices
        posIndex = 0;
        texIndex = 0;
        normIndex = 0;
        tintIndex = 0;
        indIndex = 0;
        vertexCount = 0;
        
        // Iterate through all blocks in the chunk
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int x = 0; x < Chunk.WIDTH; x++) {
                    int blockId = chunk.getBlock(x, y, z);
                    
                    // Skip air blocks
                    if (blockId == 0) {
                        continue;
                    }
                    
                    Block block = blockRegistry.getBlock(blockId);
                    if (block == null || block.isAir()) {
                        continue;
                    }
                    
                    // Calculate world position for this block
                    float worldX = chunk.getWorldX(x);
                    float worldZ = chunk.getWorldZ(z);
                    
                    // Check each face for visibility
                    addVisibleFaces(chunk, x, y, z, worldX, y, worldZ, block);
                }
            }
        }
        
        // Return null if no faces were generated
        if (posIndex == 0) {
            if (pooledMesh != null) {
                MeshPool.getInstance().release(pooledMesh);
            }
            return null;
        }
        
        // Acquire a pooled mesh if needed
        if (pooledMesh == null) {
            pooledMesh = MeshPool.getInstance().acquire();
            if (pooledMesh == null) {
                // Pool is exhausted, cannot build mesh right now
                Logger.warn("Cannot build chunk mesh: MeshPool exhausted. Retry later.");
                return null;
            }
        }
        
        // Create trimmed arrays for the mesh data
        float[] finalPositions = new float[posIndex];
        float[] finalTexCoords = new float[texIndex];
        float[] finalNormals = new float[normIndex];
        float[] finalTints = new float[tintIndex];
        int[] finalIndices = new int[indIndex];
        
        System.arraycopy(positions, 0, finalPositions, 0, posIndex);
        System.arraycopy(texCoords, 0, finalTexCoords, 0, texIndex);
        System.arraycopy(normals, 0, finalNormals, 0, normIndex);
        System.arraycopy(tints, 0, finalTints, 0, tintIndex);
        System.arraycopy(indices, 0, finalIndices, 0, indIndex);
        
        // Update the pooled mesh with new data
        pooledMesh.updateData(finalPositions, finalTexCoords, finalNormals, finalTints, finalIndices);
        
        return pooledMesh;
    }
    
    /**
     * Adds visible faces for a block.
     */
    private void addVisibleFaces(Chunk chunk, int localX, int localY, int localZ,
                                  float worldX, float worldY, float worldZ, Block block) {
        for (BlockFace face : BlockFace.values()) {
            if (isFaceVisible(chunk, localX, localY, localZ, face)) {
                addFace(worldX, worldY, worldZ, face, block);
            }
        }
    }
    
    /**
     * Checks if a face is visible (not blocked by an adjacent solid block).
     */
    private boolean isFaceVisible(Chunk chunk, int x, int y, int z, BlockFace face) {
        int nx = x + face.getOffsetX();
        int ny = y + face.getOffsetY();
        int nz = z + face.getOffsetZ();
        
        // If neighbor is outside chunk bounds vertically
        if (ny < 0) {
            return false; // Bottom of world - don't render
        }
        if (ny >= Chunk.HEIGHT) {
            return true; // Top of world is visible
        }
        
        // If neighbor is outside chunk bounds horizontally, check adjacent chunk via World
        if (nx < 0 || nx >= Chunk.WIDTH || nz < 0 || nz >= Chunk.DEPTH) {
            World world = chunk.getWorld();
            if (world != null) {
                int worldNX = chunk.getWorldX(x) + face.getOffsetX();
                int worldNZ = chunk.getWorldZ(z) + face.getOffsetZ();
                int neighborBlockId = world.getBlock(worldNX, ny, worldNZ);
                
                if (neighborBlockId == 0) {
                    return true;
                }
                
                Block neighborBlock = blockRegistry.getBlock(neighborBlockId);
                if (neighborBlock == null) {
                    return true;
                }
                
                return !neighborBlock.isSolid() || neighborBlock.isTransparent();
            }
            // No world reference - don't render at unloaded chunk boundaries
            return false;
        }
        
        // Check the neighbor block within this chunk
        int neighborBlockId = chunk.getBlock(nx, ny, nz);
        if (neighborBlockId == 0) {
            return true;
        }
        
        Block neighborBlock = blockRegistry.getBlock(neighborBlockId);
        if (neighborBlock == null) {
            return true;
        }
        
        return !neighborBlock.isSolid() || neighborBlock.isTransparent();
    }
    
    /**
     * Adds a face to the mesh data with proper texture UVs and tint colors.
     */
    private void addFace(float x, float y, float z, BlockFace face, Block block) {
        // Ensure capacity
        ensureCapacity();
        
        // Get vertices directly into temp array
        getFaceVertices(x, y, z, face, tempVertices);
        
        // Get UVs
        getFaceUVs(face, block, tempUVs);
        
        // Get normal (cached, no allocation)
        float[] normal = FACE_NORMALS[face.ordinal()];
        
        // Get tint
        float[] tint = block.getTint(face);
        
        // Add 4 vertices
        for (int i = 0; i < 4; i++) {
            int vi = i * 3;
            int ti = i * 2;
            
            // Position
            positions[posIndex++] = tempVertices[vi];
            positions[posIndex++] = tempVertices[vi + 1];
            positions[posIndex++] = tempVertices[vi + 2];
            
            // Texture coordinates
            texCoords[texIndex++] = tempUVs[ti];
            texCoords[texIndex++] = tempUVs[ti + 1];
            
            // Normal
            normals[normIndex++] = normal[0];
            normals[normIndex++] = normal[1];
            normals[normIndex++] = normal[2];
            
            // Tint
            tints[tintIndex++] = tint[0];
            tints[tintIndex++] = tint[1];
            tints[tintIndex++] = tint[2];
        }
        
        // Add indices for two triangles
        indices[indIndex++] = vertexCount;
        indices[indIndex++] = vertexCount + 1;
        indices[indIndex++] = vertexCount + 2;
        indices[indIndex++] = vertexCount;
        indices[indIndex++] = vertexCount + 2;
        indices[indIndex++] = vertexCount + 3;
        
        vertexCount += 4;
    }
    
    /**
     * Ensures arrays have enough capacity for another face.
     */
    private void ensureCapacity() {
        // Check if we need to grow arrays (4 vertices per face, 6 indices per face)
        if (posIndex + 12 > positions.length) {
            int newCapacity = positions.length * 2;
            
            float[] newPositions = new float[newCapacity];
            System.arraycopy(positions, 0, newPositions, 0, posIndex);
            positions = newPositions;
            
            float[] newNormals = new float[newCapacity];
            System.arraycopy(normals, 0, newNormals, 0, normIndex);
            normals = newNormals;
            
            float[] newTints = new float[newCapacity];
            System.arraycopy(tints, 0, newTints, 0, tintIndex);
            tints = newTints;
        }
        
        if (texIndex + 8 > texCoords.length) {
            int newCapacity = texCoords.length * 2;
            float[] newTexCoords = new float[newCapacity];
            System.arraycopy(texCoords, 0, newTexCoords, 0, texIndex);
            texCoords = newTexCoords;
        }
        
        if (indIndex + 6 > indices.length) {
            int newCapacity = indices.length * 2;
            int[] newIndices = new int[newCapacity];
            System.arraycopy(indices, 0, newIndices, 0, indIndex);
            indices = newIndices;
        }
    }
    
    /**
     * Gets the vertices for a face directly into the output array.
     * 
     * @param x Block X position
     * @param y Block Y position
     * @param z Block Z position
     * @param face The face
     * @param out Output array (12 floats: 4 vertices * 3 components)
     */
    private void getFaceVertices(float x, float y, float z, BlockFace face, float[] out) {
        switch (face) {
            case TOP -> {
                out[0] = x;     out[1] = y + 1; out[2] = z + 1;
                out[3] = x + 1; out[4] = y + 1; out[5] = z + 1;
                out[6] = x + 1; out[7] = y + 1; out[8] = z;
                out[9] = x;     out[10] = y + 1; out[11] = z;
            }
            case BOTTOM -> {
                out[0] = x;     out[1] = y; out[2] = z;
                out[3] = x + 1; out[4] = y; out[5] = z;
                out[6] = x + 1; out[7] = y; out[8] = z + 1;
                out[9] = x;     out[10] = y; out[11] = z + 1;
            }
            case NORTH -> {
                out[0] = x + 1; out[1] = y;     out[2] = z;
                out[3] = x;     out[4] = y;     out[5] = z;
                out[6] = x;     out[7] = y + 1; out[8] = z;
                out[9] = x + 1; out[10] = y + 1; out[11] = z;
            }
            case SOUTH -> {
                out[0] = x;     out[1] = y;     out[2] = z + 1;
                out[3] = x + 1; out[4] = y;     out[5] = z + 1;
                out[6] = x + 1; out[7] = y + 1; out[8] = z + 1;
                out[9] = x;     out[10] = y + 1; out[11] = z + 1;
            }
            case EAST -> {
                out[0] = x + 1; out[1] = y;     out[2] = z + 1;
                out[3] = x + 1; out[4] = y;     out[5] = z;
                out[6] = x + 1; out[7] = y + 1; out[8] = z;
                out[9] = x + 1; out[10] = y + 1; out[11] = z + 1;
            }
            case WEST -> {
                out[0] = x; out[1] = y;     out[2] = z;
                out[3] = x; out[4] = y;     out[5] = z + 1;
                out[6] = x; out[7] = y + 1; out[8] = z + 1;
                out[9] = x; out[10] = y + 1; out[11] = z;
            }
        }
    }
    
    /**
     * Gets the UV coordinates for a face directly into the output array.
     * 
     * @param face The face
     * @param block The block
     * @param out Output array (8 floats: 4 vertices * 2 components)
     */
    private void getFaceUVs(BlockFace face, Block block, float[] out) {
        BlockTextures textures = block.getTextures();
        String texturePath = textures.getTexture(face);
        
        if (textureAtlas != null) {
            float[] atlasUVs = textureAtlas.getUVs(texturePath);
            if (atlasUVs != null) {
                System.arraycopy(atlasUVs, 0, out, 0, 8);
                return;
            }
        }
        
        // Default UVs
        out[0] = 0; out[1] = 0;
        out[2] = 1; out[3] = 0;
        out[4] = 1; out[5] = 1;
        out[6] = 0; out[7] = 1;
    }
}
