package me.alextzamalis.voxel;

import me.alextzamalis.graphics.Mesh;
import me.alextzamalis.graphics.TextureAtlas;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds optimized meshes for chunks using face culling and texture atlas.
 * 
 * <p>This class generates meshes for chunks by only creating faces that are
 * visible (not adjacent to solid blocks). Includes support for tint colors
 * for blocks like grass and leaves.
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
    
    /** Accumulated tint colors. */
    private final List<Float> tints;
    
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
        this.tints = new ArrayList<>();
        this.indices = new ArrayList<>();
        this.vertexCount = 0;
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
     */
    public Mesh buildMesh(Chunk chunk) {
        // Clear previous data
        positions.clear();
        texCoords.clear();
        normals.clear();
        tints.clear();
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
        float[] tintArray = toFloatArray(tints);
        int[] indArray = toIntArray(indices);
        
        return new Mesh(posArray, texArray, normArray, tintArray, indArray);
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
        float[][] vertices = getFaceVertices(x, y, z, face);
        float[] normal = getFaceNormal(face);
        float[][] uvs = getFaceUVs(face, block);
        float[] tint = block.getTint(face);
        
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
            
            // Add tint color for each vertex
            tints.add(tint[0]);
            tints.add(tint[1]);
            tints.add(tint[2]);
        }
        
        // Add indices for two triangles
        indices.add(vertexCount);
        indices.add(vertexCount + 1);
        indices.add(vertexCount + 2);
        indices.add(vertexCount);
        indices.add(vertexCount + 2);
        indices.add(vertexCount + 3);
        
        vertexCount += 4;
    }
    
    /**
     * Gets the vertices for a face (counter-clockwise winding).
     */
    private float[][] getFaceVertices(float x, float y, float z, BlockFace face) {
        return switch (face) {
            case TOP -> new float[][] {
                {x,     y + 1, z + 1},
                {x + 1, y + 1, z + 1},
                {x + 1, y + 1, z},
                {x,     y + 1, z}
            };
            case BOTTOM -> new float[][] {
                {x,     y, z},
                {x + 1, y, z},
                {x + 1, y, z + 1},
                {x,     y, z + 1}
            };
            case NORTH -> new float[][] {
                {x + 1, y,     z},
                {x,     y,     z},
                {x,     y + 1, z},
                {x + 1, y + 1, z}
            };
            case SOUTH -> new float[][] {
                {x,     y,     z + 1},
                {x + 1, y,     z + 1},
                {x + 1, y + 1, z + 1},
                {x,     y + 1, z + 1}
            };
            case EAST -> new float[][] {
                {x + 1, y,     z + 1},
                {x + 1, y,     z},
                {x + 1, y + 1, z},
                {x + 1, y + 1, z + 1}
            };
            case WEST -> new float[][] {
                {x, y,     z},
                {x, y,     z + 1},
                {x, y + 1, z + 1},
                {x, y + 1, z}
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
        BlockTextures textures = block.getTextures();
        String texturePath = textures.getTexture(face);
        
        if (textureAtlas != null) {
            float[] atlasUVs = textureAtlas.getUVs(texturePath);
            if (atlasUVs != null) {
                return new float[][] {
                    {atlasUVs[0], atlasUVs[1]},
                    {atlasUVs[2], atlasUVs[3]},
                    {atlasUVs[4], atlasUVs[5]},
                    {atlasUVs[6], atlasUVs[7]}
                };
            }
        }
        
        return new float[][] {
            {0, 0}, {1, 0}, {1, 1}, {0, 1}
        };
    }
    
    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
    
    private int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
