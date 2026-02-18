package me.alextzamalis.voxel;

import me.alextzamalis.graphics.GlobalBufferManager;
import me.alextzamalis.graphics.TextureAtlas;
import me.alextzamalis.graphics.VertexPacker;
import me.alextzamalis.util.Logger;

/**
 * Builds meshes for SimplifiedChunk using the global buffer (MDI rendering).
 * 
 * <p>This builder creates meshes for downsampled chunks, taking into account
 * the scale factor when generating vertices. Blocks are rendered at their
 * simplified scale (e.g., 4x4 blocks represented as 1 block).
 * 
 * <p>The mesh is built directly into the GlobalBufferManager for MDI rendering.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class SimplifiedChunkMeshBuilder {
    
    /** Initial capacity for vertex arrays. */
    private static final int INITIAL_VERTEX_CAPACITY = 10000;
    private static final int INITIAL_INDEX_CAPACITY = 15000;
    
    /** Position data (x, y, z per vertex). */
    private float[] positions;
    
    /** Texture coordinate data (u, v per vertex). */
    private float[] texCoords;
    
    /** Normal data (nx, ny, nz per vertex). */
    private float[] normals;
    
    /** Index data. */
    private int[] indices;
    
    /** Current position in arrays. */
    private int posIndex, texIndex, normIndex, indIndex;
    
    /** Current vertex count for indexing. */
    private int vertexCount;
    
    /** The block registry. */
    private final BlockRegistry blockRegistry;
    
    /** The texture atlas. */
    private TextureAtlas textureAtlas;
    
    /** The global buffer manager (for MDI rendering). */
    private GlobalBufferManager globalBufferManager;
    
    /** Cached normal vectors. */
    private static final float[][] FACE_NORMALS = {
        {0, 1, 0},   // TOP
        {0, -1, 0},  // BOTTOM
        {0, 0, -1},  // NORTH
        {0, 0, 1},   // SOUTH
        {1, 0, 0},   // EAST
        {-1, 0, 0}   // WEST
    };
    
    /**
     * Creates a new simplified chunk mesh builder.
     */
    public SimplifiedChunkMeshBuilder() {
        this.positions = new float[INITIAL_VERTEX_CAPACITY * 3];
        this.texCoords = new float[INITIAL_VERTEX_CAPACITY * 2];
        this.normals = new float[INITIAL_VERTEX_CAPACITY * 3];
        this.indices = new int[INITIAL_INDEX_CAPACITY];
        this.blockRegistry = BlockRegistry.getInstance();
    }
    
    /**
     * Sets the texture atlas.
     * 
     * @param atlas The texture atlas
     */
    public void setTextureAtlas(TextureAtlas atlas) {
        this.textureAtlas = atlas;
    }
    
    /**
     * Sets the global buffer manager for MDI rendering.
     * 
     * @param bufferManager The global buffer manager
     */
    public void setGlobalBufferManager(GlobalBufferManager bufferManager) {
        this.globalBufferManager = bufferManager;
    }
    
    /**
     * Builds a mesh for a SimplifiedChunk into the global buffer.
     * 
     * @param simplifiedChunk The simplified chunk
     * @param chunkKey Unique chunk identifier
     * @return true if mesh was successfully built
     */
    public boolean buildMesh(SimplifiedChunk simplifiedChunk, long chunkKey) {
        if (globalBufferManager == null) {
            Logger.warn("GlobalBufferManager not set, cannot build SimplifiedChunk mesh");
            return false;
        }
        
        // Reset indices
        posIndex = 0;
        texIndex = 0;
        normIndex = 0;
        indIndex = 0;
        vertexCount = 0;
        
        int scale = simplifiedChunk.getScale();
        int width = simplifiedChunk.getWidth();
        int depth = simplifiedChunk.getDepth();
        
        // Iterate through simplified blocks
        for (int y = 0; y < SimplifiedChunk.HEIGHT; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    short blockId = simplifiedChunk.getBlock(x, y, z);
                    
                    // Skip air blocks
                    if (blockId == 0) {
                        continue;
                    }
                    
                    Block block = blockRegistry.getBlock(blockId);
                    if (block == null || block.isAir()) {
                        continue;
                    }
                    
                    // Calculate world position (accounting for scale)
                    float worldX = simplifiedChunk.getWorldX(x);
                    float worldZ = simplifiedChunk.getWorldZ(z);
                    float blockSize = scale; // Blocks are scale x scale in size
                    
                    // Add faces for this simplified block (larger than normal blocks)
                    addSimplifiedBlockFaces(worldX, y, worldZ, blockSize, block);
                }
            }
        }
        
        // Return false if no faces were generated
        if (posIndex == 0) {
            return false;
        }
        
        // Calculate vertex count
        int vertexCount = posIndex / 3;
        int indexCount = indIndex;
        
        // Allocate or reuse space in global buffer
        GlobalBufferManager.ChunkAllocation existingAlloc = 
            globalBufferManager.getAllocations().get(chunkKey);
        
        GlobalBufferManager.ChunkAllocation alloc = existingAlloc;
        
        // If no existing allocation or size changed, allocate new space
        if (alloc == null || !alloc.valid || 
            alloc.vertexCount != vertexCount || alloc.indexCount != indexCount) {
            // Deallocate old allocation if it exists
            if (existingAlloc != null && existingAlloc.valid) {
                globalBufferManager.deallocateChunk(chunkKey);
            }
            
            // Allocate new space
            alloc = globalBufferManager.allocateChunk(chunkKey, vertexCount, indexCount);
            
            if (alloc == null) {
                Logger.warn("Failed to allocate buffer space for simplified chunk %d", chunkKey);
                return false;
            }
        }
        
        // Pack vertices into integers (4 ints per vertex)
        int[] packedVertices = new int[vertexCount * 4];
        int packedIndex = 0;
        
        for (int i = 0; i < vertexCount; i++) {
            int posIdx = i * 3;
            int texIdx = i * 2;
            int normIdx = i * 3;
            
            // Pack position
            int[] posPacked = VertexPacker.packPosition(
                positions[posIdx], positions[posIdx + 1], positions[posIdx + 2]);
            
            // Pack UV
            int uvPacked = VertexPacker.packUV(
                texCoords[texIdx], texCoords[texIdx + 1]);
            
            // Pack normal
            int normalPacked = VertexPacker.packNormal(
                normals[normIdx], normals[normIdx + 1], normals[normIdx + 2]);
            
            // Pack light (default values for now)
            int lightPacked = VertexPacker.packLight(15, 15);
            
            // Store packed data
            packedVertices[packedIndex++] = posPacked[0];
            packedVertices[packedIndex++] = uvPacked;
            packedVertices[packedIndex++] = normalPacked;
            packedVertices[packedIndex++] = lightPacked;
        }
        
        // Create trimmed index array
        int[] finalIndices = new int[indexCount];
        System.arraycopy(indices, 0, finalIndices, 0, indexCount);
        
        // Update global buffer
        globalBufferManager.updateChunk(chunkKey, packedVertices, finalIndices);
        
        return true;
    }
    
    /**
     * Adds faces for a simplified block (larger than normal blocks).
     * 
     * @param x World X position
     * @param y World Y position
     * @param z World Z position
     * @param blockSize Size of the block (scale factor)
     * @param block The block
     */
    private void addSimplifiedBlockFaces(float x, float y, float z, float blockSize, Block block) {
        // For simplified blocks, we render them at their full scale size
        // This means a 4x4 block is rendered as a single large block
        
        // Add all 6 faces (simplified chunks don't do face culling between blocks)
        // We could optimize this later, but for now we'll render all faces
        
        for (BlockFace face : BlockFace.values()) {
            addSimplifiedFace(x, y, z, blockSize, face, block);
        }
    }
    
    /**
     * Adds a face for a simplified block.
     * 
     * @param x World X position
     * @param y World Y position
     * @param z World Z position
     * @param blockSize Size of the block
     * @param face The face to add
     * @param block The block
     */
    private void addSimplifiedFace(float x, float y, float z, float blockSize, 
                                   BlockFace face, Block block) {
        // Ensure capacity
        if (posIndex + 12 > positions.length) {
            growArrays();
        }
        
        // Get face vertices (scaled by blockSize)
        float[] faceVertices = getSimplifiedFaceVertices(x, y, z, blockSize, face);
        float[] faceUVs = getFaceUVs(face, block);
        float[] normal = FACE_NORMALS[face.ordinal()];
        
        // Add 4 vertices
        for (int i = 0; i < 4; i++) {
            int vi = i * 3;
            int ti = i * 2;
            
            // Position
            positions[posIndex++] = faceVertices[vi];
            positions[posIndex++] = faceVertices[vi + 1];
            positions[posIndex++] = faceVertices[vi + 2];
            
            // Texture coordinates
            texCoords[texIndex++] = faceUVs[ti];
            texCoords[texIndex++] = faceUVs[ti + 1];
            
            // Normal
            normals[normIndex++] = normal[0];
            normals[normIndex++] = normal[1];
            normals[normIndex++] = normal[2];
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
     * Gets vertices for a simplified face (scaled by blockSize).
     */
    private float[] getSimplifiedFaceVertices(float x, float y, float z, float blockSize, BlockFace face) {
        float[] vertices = new float[12];
        
        switch (face) {
            case TOP -> {
                vertices[0] = x; vertices[1] = y + blockSize; vertices[2] = z + blockSize;
                vertices[3] = x + blockSize; vertices[4] = y + blockSize; vertices[5] = z + blockSize;
                vertices[6] = x + blockSize; vertices[7] = y + blockSize; vertices[8] = z;
                vertices[9] = x; vertices[10] = y + blockSize; vertices[11] = z;
            }
            case BOTTOM -> {
                vertices[0] = x; vertices[1] = y; vertices[2] = z;
                vertices[3] = x + blockSize; vertices[4] = y; vertices[5] = z;
                vertices[6] = x + blockSize; vertices[7] = y; vertices[8] = z + blockSize;
                vertices[9] = x; vertices[10] = y; vertices[11] = z + blockSize;
            }
            case NORTH -> {
                vertices[0] = x + blockSize; vertices[1] = y; vertices[2] = z;
                vertices[3] = x; vertices[4] = y; vertices[5] = z;
                vertices[6] = x; vertices[7] = y + blockSize; vertices[8] = z;
                vertices[9] = x + blockSize; vertices[10] = y + blockSize; vertices[11] = z;
            }
            case SOUTH -> {
                vertices[0] = x; vertices[1] = y; vertices[2] = z + blockSize;
                vertices[3] = x + blockSize; vertices[4] = y; vertices[5] = z + blockSize;
                vertices[6] = x + blockSize; vertices[7] = y + blockSize; vertices[8] = z + blockSize;
                vertices[9] = x; vertices[10] = y + blockSize; vertices[11] = z + blockSize;
            }
            case EAST -> {
                vertices[0] = x + blockSize; vertices[1] = y; vertices[2] = z + blockSize;
                vertices[3] = x + blockSize; vertices[4] = y; vertices[5] = z;
                vertices[6] = x + blockSize; vertices[7] = y + blockSize; vertices[8] = z;
                vertices[9] = x + blockSize; vertices[10] = y + blockSize; vertices[11] = z + blockSize;
            }
            case WEST -> {
                vertices[0] = x; vertices[1] = y; vertices[2] = z;
                vertices[3] = x; vertices[4] = y; vertices[5] = z + blockSize;
                vertices[6] = x; vertices[7] = y + blockSize; vertices[8] = z + blockSize;
                vertices[9] = x; vertices[10] = y + blockSize; vertices[11] = z;
            }
        }
        
        return vertices;
    }
    
    /**
     * Gets UV coordinates for a face.
     */
    private float[] getFaceUVs(BlockFace face, Block block) {
        BlockTextures textures = block.getTextures();
        String texturePath = textures.getTexture(face);
        
        if (textureAtlas != null) {
            float[] atlasUVs = textureAtlas.getUVs(texturePath);
            if (atlasUVs != null) {
                return atlasUVs;
            }
        }
        
        // Default UVs
        return new float[]{0, 0, 1, 0, 1, 1, 0, 1};
    }
    
    /**
     * Grows arrays when capacity is exceeded.
     */
    private void growArrays() {
        int newCapacity = positions.length * 2;
        
        float[] newPositions = new float[newCapacity];
        System.arraycopy(positions, 0, newPositions, 0, posIndex);
        positions = newPositions;
        
        float[] newNormals = new float[newCapacity];
        System.arraycopy(normals, 0, newNormals, 0, normIndex);
        normals = newNormals;
        
        if (texIndex + 8 > texCoords.length) {
            int newTexCapacity = texCoords.length * 2;
            float[] newTexCoords = new float[newTexCapacity];
            System.arraycopy(texCoords, 0, newTexCoords, 0, texIndex);
            texCoords = newTexCoords;
        }
        
        if (indIndex + 6 > indices.length) {
            int newIndexCapacity = indices.length * 2;
            int[] newIndices = new int[newIndexCapacity];
            System.arraycopy(indices, 0, newIndices, 0, indIndex);
            indices = newIndices;
        }
    }
}

