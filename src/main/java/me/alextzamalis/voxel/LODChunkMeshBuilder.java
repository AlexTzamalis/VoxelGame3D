package me.alextzamalis.voxel;

import me.alextzamalis.graphics.LODLevel;
import me.alextzamalis.graphics.MeshPool;
import me.alextzamalis.graphics.PooledMesh;
import me.alextzamalis.graphics.TextureAtlas;

/**
 * Builds chunk meshes with Level of Detail (LOD) support.
 * 
 * <p>This builder generates simplified meshes for distant chunks by:
 * <ul>
 *   <li>Sampling blocks at lower resolution (every Nth block)</li>
 *   <li>Skipping internal faces for simplified mode</li>
 *   <li>Using larger block sizes for distant chunks</li>
 * </ul>
 * 
 * <p>This significantly reduces vertex count for distant chunks while
 * maintaining visual appearance at distance.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class LODChunkMeshBuilder {
    
    /** Initial capacity for vertex arrays. */
    private static final int INITIAL_VERTEX_CAPACITY = 20000;
    private static final int INITIAL_INDEX_CAPACITY = 30000;
    
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
    
    /** Current position in arrays. */
    private int posIndex, texIndex, normIndex, tintIndex, indIndex;
    
    /** Current vertex count for indexing. */
    private int vertexCount;
    
    /** The block registry. */
    private final BlockRegistry blockRegistry;
    
    /** The texture atlas. */
    private TextureAtlas textureAtlas;
    
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
     * Creates a new LOD mesh builder.
     */
    public LODChunkMeshBuilder() {
        this.positions = new float[INITIAL_VERTEX_CAPACITY * 3];
        this.texCoords = new float[INITIAL_VERTEX_CAPACITY * 2];
        this.normals = new float[INITIAL_VERTEX_CAPACITY * 3];
        this.tints = new float[INITIAL_VERTEX_CAPACITY * 3];
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
     * Builds a LOD mesh for the specified chunk.
     * 
     * @param chunk The chunk to build
     * @param lodLevel The LOD level to use
     * @param existingMesh Existing pooled mesh to reuse (or null)
     * @return The built mesh, or null if chunk has no visible faces
     */
    public PooledMesh buildLODMesh(Chunk chunk, LODLevel lodLevel, PooledMesh existingMesh) {
        // Reset indices
        posIndex = 0;
        texIndex = 0;
        normIndex = 0;
        tintIndex = 0;
        indIndex = 0;
        vertexCount = 0;
        
        int sampleRate = lodLevel.getSampleRate();
        boolean simplified = lodLevel.isSimplified();
        float blockScale = sampleRate; // Scale blocks for lower LOD
        
        // Iterate through blocks with sampling
        for (int y = 0; y < Chunk.HEIGHT; y += sampleRate) {
            for (int z = 0; z < Chunk.DEPTH; z += sampleRate) {
                for (int x = 0; x < Chunk.WIDTH; x += sampleRate) {
                    // Get the dominant block in this sample area
                    int blockId = getDominantBlock(chunk, x, y, z, sampleRate);
                    
                    if (blockId == 0) continue;
                    
                    Block block = blockRegistry.getBlock(blockId);
                    if (block == null || block.isAir()) continue;
                    
                    // Calculate world position
                    float worldX = chunk.getWorldX(x);
                    float worldZ = chunk.getWorldZ(z);
                    
                    // Add faces for this block
                    if (simplified) {
                        addSimplifiedFaces(chunk, x, y, z, worldX, y, worldZ, block, sampleRate, blockScale);
                    } else {
                        addDetailedFaces(chunk, x, y, z, worldX, y, worldZ, block, blockScale);
                    }
                }
            }
        }
        
        // Return null if no faces
        if (posIndex == 0) {
            if (existingMesh != null) {
                MeshPool.getInstance().release(existingMesh);
            }
            return null;
        }
        
        // Get or acquire mesh
        if (existingMesh == null) {
            existingMesh = MeshPool.getInstance().acquire();
        }
        
        // Create trimmed arrays
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
        
        existingMesh.updateData(finalPositions, finalTexCoords, finalNormals, finalTints, finalIndices);
        
        return existingMesh;
    }
    
    /**
     * Gets the dominant (most common) block in a sample area.
     */
    private int getDominantBlock(Chunk chunk, int startX, int startY, int startZ, int sampleRate) {
        // For efficiency, just return the first non-air block found
        // A more accurate method would count block types and return the most common
        for (int dy = 0; dy < sampleRate && startY + dy < Chunk.HEIGHT; dy++) {
            for (int dz = 0; dz < sampleRate && startZ + dz < Chunk.DEPTH; dz++) {
                for (int dx = 0; dx < sampleRate && startX + dx < Chunk.WIDTH; dx++) {
                    int blockId = chunk.getBlock(startX + dx, startY + dy, startZ + dz);
                    if (blockId != 0) {
                        return blockId;
                    }
                }
            }
        }
        return 0;
    }
    
    /**
     * Adds simplified faces (only outer faces of chunk).
     */
    private void addSimplifiedFaces(Chunk chunk, int localX, int localY, int localZ,
                                    float worldX, float worldY, float worldZ,
                                    Block block, int sampleRate, float blockScale) {
        // For simplified mode, only render faces on chunk edges or exposed to air
        for (BlockFace face : BlockFace.values()) {
            if (isEdgeFaceVisible(chunk, localX, localY, localZ, face, sampleRate)) {
                addFace(worldX, worldY, worldZ, face, block, blockScale);
            }
        }
    }
    
    /**
     * Adds detailed faces (all visible faces).
     */
    private void addDetailedFaces(Chunk chunk, int localX, int localY, int localZ,
                                  float worldX, float worldY, float worldZ,
                                  Block block, float blockScale) {
        for (BlockFace face : BlockFace.values()) {
            if (isFaceVisible(chunk, localX, localY, localZ, face)) {
                addFace(worldX, worldY, worldZ, face, block, blockScale);
            }
        }
    }
    
    /**
     * Checks if a face on an edge block is visible.
     */
    private boolean isEdgeFaceVisible(Chunk chunk, int x, int y, int z, BlockFace face, int sampleRate) {
        int nx = x + face.getOffsetX() * sampleRate;
        int ny = y + face.getOffsetY() * sampleRate;
        int nz = z + face.getOffsetZ() * sampleRate;
        
        // Face is visible if neighbor is outside chunk or is air
        if (nx < 0 || nx >= Chunk.WIDTH || ny < 0 || ny >= Chunk.HEIGHT || nz < 0 || nz >= Chunk.DEPTH) {
            return true;
        }
        
        // Check if the entire neighboring sample area is air
        return getDominantBlock(chunk, nx, ny, nz, sampleRate) == 0;
    }
    
    /**
     * Checks if a face is visible (standard check).
     */
    private boolean isFaceVisible(Chunk chunk, int x, int y, int z, BlockFace face) {
        int nx = x + face.getOffsetX();
        int ny = y + face.getOffsetY();
        int nz = z + face.getOffsetZ();
        
        if (ny < 0 || ny >= Chunk.HEIGHT) {
            return ny < 0 ? false : true;
        }
        
        if (nx < 0 || nx >= Chunk.WIDTH || nz < 0 || nz >= Chunk.DEPTH) {
            // Check adjacent chunk
            World world = chunk.getWorld();
            if (world != null) {
                int worldNx = chunk.getWorldX(x) + face.getOffsetX();
                int worldNz = chunk.getWorldZ(z) + face.getOffsetZ();
                int neighborBlock = world.getBlock(worldNx, ny, worldNz);
                Block neighbor = blockRegistry.getBlock(neighborBlock);
                return neighbor == null || !neighbor.isSolid();
            }
            return true;
        }
        
        int neighborBlock = chunk.getBlock(nx, ny, nz);
        Block neighbor = blockRegistry.getBlock(neighborBlock);
        return neighbor == null || !neighbor.isSolid();
    }
    
    /**
     * Adds a face to the mesh with the specified scale.
     */
    private void addFace(float x, float y, float z, BlockFace face, Block block, float scale) {
        ensureCapacity();
        
        // Get texture UVs
        String texturePath = block.getTextures().getTexture(face);
        float[] uvs = textureAtlas != null ? textureAtlas.getUVs(texturePath) : new float[]{0, 0, 1, 1};
        
        // Get tint color
        float[] tint = block.getTint(face);
        
        // Get face normal
        float[] normal = FACE_NORMALS[face.ordinal()];
        
        // Generate vertices based on face
        float[][] vertices = getFaceVertices(x, y, z, face, scale);
        float[][] uvCoords = getFaceUVs(uvs);
        
        // Add 4 vertices
        for (int i = 0; i < 4; i++) {
            // Position
            positions[posIndex++] = vertices[i][0];
            positions[posIndex++] = vertices[i][1];
            positions[posIndex++] = vertices[i][2];
            
            // Texture coordinate
            texCoords[texIndex++] = uvCoords[i][0];
            texCoords[texIndex++] = uvCoords[i][1];
            
            // Normal
            normals[normIndex++] = normal[0];
            normals[normIndex++] = normal[1];
            normals[normIndex++] = normal[2];
            
            // Tint
            tints[tintIndex++] = tint[0];
            tints[tintIndex++] = tint[1];
            tints[tintIndex++] = tint[2];
        }
        
        // Add indices (2 triangles)
        indices[indIndex++] = vertexCount;
        indices[indIndex++] = vertexCount + 1;
        indices[indIndex++] = vertexCount + 2;
        indices[indIndex++] = vertexCount + 2;
        indices[indIndex++] = vertexCount + 3;
        indices[indIndex++] = vertexCount;
        
        vertexCount += 4;
    }
    
    /**
     * Gets vertex positions for a face.
     */
    private float[][] getFaceVertices(float x, float y, float z, BlockFace face, float scale) {
        float[][] vertices = new float[4][3];
        
        switch (face) {
            case TOP:
                vertices[0] = new float[]{x, y + scale, z};
                vertices[1] = new float[]{x + scale, y + scale, z};
                vertices[2] = new float[]{x + scale, y + scale, z + scale};
                vertices[3] = new float[]{x, y + scale, z + scale};
                break;
            case BOTTOM:
                vertices[0] = new float[]{x, y, z + scale};
                vertices[1] = new float[]{x + scale, y, z + scale};
                vertices[2] = new float[]{x + scale, y, z};
                vertices[3] = new float[]{x, y, z};
                break;
            case NORTH:
                vertices[0] = new float[]{x + scale, y, z};
                vertices[1] = new float[]{x, y, z};
                vertices[2] = new float[]{x, y + scale, z};
                vertices[3] = new float[]{x + scale, y + scale, z};
                break;
            case SOUTH:
                vertices[0] = new float[]{x, y, z + scale};
                vertices[1] = new float[]{x + scale, y, z + scale};
                vertices[2] = new float[]{x + scale, y + scale, z + scale};
                vertices[3] = new float[]{x, y + scale, z + scale};
                break;
            case EAST:
                vertices[0] = new float[]{x + scale, y, z + scale};
                vertices[1] = new float[]{x + scale, y, z};
                vertices[2] = new float[]{x + scale, y + scale, z};
                vertices[3] = new float[]{x + scale, y + scale, z + scale};
                break;
            case WEST:
                vertices[0] = new float[]{x, y, z};
                vertices[1] = new float[]{x, y, z + scale};
                vertices[2] = new float[]{x, y + scale, z + scale};
                vertices[3] = new float[]{x, y + scale, z};
                break;
        }
        
        return vertices;
    }
    
    /**
     * Gets UV coordinates for a face.
     */
    private float[][] getFaceUVs(float[] uvs) {
        float u1 = uvs[0], v1 = uvs[1], u2 = uvs[2], v2 = uvs[3];
        return new float[][] {
            {u1, v2}, {u2, v2}, {u2, v1}, {u1, v1}
        };
    }
    
    /**
     * Ensures arrays have enough capacity.
     */
    private void ensureCapacity() {
        if (posIndex + 12 >= positions.length) {
            int newCapacity = positions.length * 2;
            positions = expandArray(positions, newCapacity);
        }
        if (texIndex + 8 >= texCoords.length) {
            texCoords = expandArray(texCoords, texCoords.length * 2);
        }
        if (normIndex + 12 >= normals.length) {
            normals = expandArray(normals, normals.length * 2);
        }
        if (tintIndex + 12 >= tints.length) {
            tints = expandArray(tints, tints.length * 2);
        }
        if (indIndex + 6 >= indices.length) {
            indices = expandIntArray(indices, indices.length * 2);
        }
    }
    
    private float[] expandArray(float[] array, int newSize) {
        float[] newArray = new float[newSize];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }
    
    private int[] expandIntArray(int[] array, int newSize) {
        int[] newArray = new int[newSize];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }
}

