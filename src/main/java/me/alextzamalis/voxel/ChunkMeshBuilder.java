package me.alextzamalis.voxel;

import me.alextzamalis.graphics.Mesh;
import me.alextzamalis.graphics.TextureAtlas;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds optimized meshes for chunks using face culling and texture atlas.
 * 
 * <p>This class generates meshes for chunks by only creating faces that are
 * visible (not adjacent to solid blocks). This significantly reduces the
 * number of triangles rendered.
 * 
 * <p>The mesh builder supports:
 * <ul>
 *   <li>Face culling - only visible faces are generated</li>
 *   <li>Texture atlas UV coordinates for each face</li>
 *   <li>Per-block texture selection based on block type</li>
 *   <li>Proper vertex ordering for back-face culling</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class ChunkMeshBuilder {
    
    /** Accumulated vertex positions. */
    private final List<Float> positions;
    
    /** Accumulated texture coordinates. */
    private final List<Float> texCoords;
    
    /** Accumulated normals. */
    private final List<Float> normals;
    
    /** Accumulated indices. */
    private final List<Integer> indices;
    
    /** Current vertex count for indexing. */
    private int vertexCount;
    
    /** The block registry for looking up block properties. */
    private final BlockRegistry blockRegistry;
    
    /** The texture atlas for UV lookups. */
    private TextureAtlas textureAtlas;
    
    /**
     * Creates a new chunk mesh builder.
     */
    public ChunkMeshBuilder() {
        this.positions = new ArrayList<>();
        this.texCoords = new ArrayList<>();
        this.normals = new ArrayList<>();
        this.indices = new ArrayList<>();
        this.vertexCount = 0;
        this.blockRegistry = BlockRegistry.getInstance();
    }
    
    /**
     * Sets the texture atlas for UV coordinate lookups.
     * 
     * @param atlas The texture atlas
     */
    public void setTextureAtlas(TextureAtlas atlas) {
        this.textureAtlas = atlas;
    }
    
    /**
     * Builds a mesh for the specified chunk.
     * 
     * @param chunk The chunk to build a mesh for
     * @return The generated mesh, or null if the chunk is empty
     */
    public Mesh buildMesh(Chunk chunk) {
        // Clear previous data
        positions.clear();
        texCoords.clear();
        normals.clear();
        indices.clear();
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
        if (positions.isEmpty()) {
            return null;
        }
        
        // Convert lists to arrays
        float[] posArray = toFloatArray(positions);
        float[] texArray = toFloatArray(texCoords);
        float[] normArray = toFloatArray(normals);
        int[] indArray = toIntArray(indices);
        
        return new Mesh(posArray, texArray, normArray, indArray);
    }
    
    /**
     * Adds visible faces for a block.
     */
    private void addVisibleFaces(Chunk chunk, int localX, int localY, int localZ,
                                  float worldX, float worldY, float worldZ, Block block) {
        
        // Check each face
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
                // Convert to world coordinates and check the neighbor block
                int worldNX = chunk.getWorldX(x) + face.getOffsetX();
                int worldNZ = chunk.getWorldZ(z) + face.getOffsetZ();
                int neighborBlockId = world.getBlock(worldNX, ny, worldNZ);
                
                // If neighbor is air or transparent, face is visible
                if (neighborBlockId == 0) {
                    return true;
                }
                
                Block neighborBlock = blockRegistry.getBlock(neighborBlockId);
                if (neighborBlock == null) {
                    return true;
                }
                
                return !neighborBlock.isSolid() || neighborBlock.isTransparent();
            }
            // No world reference - assume face is NOT visible (conservative)
            // This prevents rendering at unloaded chunk boundaries
            return false;
        }
        
        // Check the neighbor block within this chunk
        int neighborBlockId = chunk.getBlock(nx, ny, nz);
        if (neighborBlockId == 0) {
            return true; // Air, face is visible
        }
        
        Block neighborBlock = blockRegistry.getBlock(neighborBlockId);
        if (neighborBlock == null) {
            return true;
        }
        
        // Face is visible if neighbor is not solid or is transparent
        return !neighborBlock.isSolid() || neighborBlock.isTransparent();
    }
    
    /**
     * Adds a face to the mesh data with proper texture UVs.
     * 
     * <p>Vertices are ordered counter-clockwise when viewed from outside the block,
     * which is the standard for OpenGL front-facing triangles with default winding.
     */
    private void addFace(float x, float y, float z, BlockFace face, Block block) {
        // Get face vertices (ordered for correct winding)
        float[][] vertices = getFaceVertices(x, y, z, face);
        float[] normal = getFaceNormal(face);
        float[][] uvs = getFaceUVs(face, block);
        
        // Add vertices
        for (int i = 0; i < 4; i++) {
            positions.add(vertices[i][0]);
            positions.add(vertices[i][1]);
            positions.add(vertices[i][2]);
            
            texCoords.add(uvs[i][0]);
            texCoords.add(uvs[i][1]);
            
            normals.add(normal[0]);
            normals.add(normal[1]);
            normals.add(normal[2]);
        }
        
        // Add indices for two triangles (counter-clockwise winding)
        // Triangle 1: 0, 1, 2
        // Triangle 2: 0, 2, 3
        indices.add(vertexCount);
        indices.add(vertexCount + 1);
        indices.add(vertexCount + 2);
        indices.add(vertexCount);
        indices.add(vertexCount + 2);
        indices.add(vertexCount + 3);
        
        vertexCount += 4;
    }
    
    /**
     * Gets the vertices for a face.
     * 
     * <p>Vertices are ordered counter-clockwise when looking at the face from outside.
     * This ensures correct front-face determination for OpenGL culling.
     */
    private float[][] getFaceVertices(float x, float y, float z, BlockFace face) {
        return switch (face) {
            // TOP face (+Y) - looking down from above, counter-clockwise
            case TOP -> new float[][] {
                {x,     y + 1, z + 1},  // 0: back-left
                {x + 1, y + 1, z + 1},  // 1: back-right
                {x + 1, y + 1, z},      // 2: front-right
                {x,     y + 1, z}       // 3: front-left
            };
            // BOTTOM face (-Y) - looking up from below, counter-clockwise
            case BOTTOM -> new float[][] {
                {x,     y, z},          // 0: front-left
                {x + 1, y, z},          // 1: front-right
                {x + 1, y, z + 1},      // 2: back-right
                {x,     y, z + 1}       // 3: back-left
            };
            // NORTH face (-Z) - looking from -Z toward +Z, counter-clockwise
            case NORTH -> new float[][] {
                {x + 1, y,     z},      // 0: bottom-right (from outside)
                {x,     y,     z},      // 1: bottom-left
                {x,     y + 1, z},      // 2: top-left
                {x + 1, y + 1, z}       // 3: top-right
            };
            // SOUTH face (+Z) - looking from +Z toward -Z, counter-clockwise
            case SOUTH -> new float[][] {
                {x,     y,     z + 1},  // 0: bottom-left (from outside)
                {x + 1, y,     z + 1},  // 1: bottom-right
                {x + 1, y + 1, z + 1},  // 2: top-right
                {x,     y + 1, z + 1}   // 3: top-left
            };
            // EAST face (+X) - looking from +X toward -X, counter-clockwise
            case EAST -> new float[][] {
                {x + 1, y,     z + 1},  // 0: bottom-back (from outside)
                {x + 1, y,     z},      // 1: bottom-front
                {x + 1, y + 1, z},      // 2: top-front
                {x + 1, y + 1, z + 1}   // 3: top-back
            };
            // WEST face (-X) - looking from -X toward +X, counter-clockwise
            case WEST -> new float[][] {
                {x, y,     z},          // 0: bottom-front (from outside)
                {x, y,     z + 1},      // 1: bottom-back
                {x, y + 1, z + 1},      // 2: top-back
                {x, y + 1, z}           // 3: top-front
            };
        };
    }
    
    /**
     * Gets the normal vector for a face.
     */
    private float[] getFaceNormal(BlockFace face) {
        return switch (face) {
            case TOP -> new float[] {0, 1, 0};
            case BOTTOM -> new float[] {0, -1, 0};
            case NORTH -> new float[] {0, 0, -1};
            case SOUTH -> new float[] {0, 0, 1};
            case EAST -> new float[] {1, 0, 0};
            case WEST -> new float[] {-1, 0, 0};
        };
    }
    
    /**
     * Gets the UV coordinates for a face based on the block's texture.
     */
    private float[][] getFaceUVs(BlockFace face, Block block) {
        // Get the texture path for this face
        BlockTextures textures = block.getTextures();
        String texturePath = textures.getTexture(face);
        
        // If we have a texture atlas, use atlas UVs
        if (textureAtlas != null) {
            float[] atlasUVs = textureAtlas.getUVs(texturePath);
            if (atlasUVs != null) {
                // atlasUVs format: [u0, v0, u1, v1, u2, v2, u3, v3]
                // Corresponds to: bottom-left, bottom-right, top-right, top-left
                return new float[][] {
                    {atlasUVs[0], atlasUVs[1]},  // 0: bottom-left
                    {atlasUVs[2], atlasUVs[3]},  // 1: bottom-right
                    {atlasUVs[4], atlasUVs[5]},  // 2: top-right
                    {atlasUVs[6], atlasUVs[7]}   // 3: top-left
                };
            }
        }
        
        // Default UVs (full texture) - bottom-left, bottom-right, top-right, top-left
        return new float[][] {
            {0, 0},
            {1, 0},
            {1, 1},
            {0, 1}
        };
    }
    
    /**
     * Converts a Float list to a float array.
     */
    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
    
    /**
     * Converts an Integer list to an int array.
     */
    private int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
