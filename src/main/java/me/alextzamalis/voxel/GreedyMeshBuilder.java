package me.alextzamalis.voxel;

import me.alextzamalis.graphics.MeshPool;
import me.alextzamalis.graphics.PooledMesh;
import me.alextzamalis.graphics.TextureAtlas;

import java.util.Arrays;

/**
 * Implements greedy meshing algorithm for optimal chunk mesh generation.
 * 
 * <p>Greedy meshing combines adjacent faces of the same block type into larger
 * quads, significantly reducing vertex count. This is particularly effective
 * for areas with uniform blocks (underground stone, large flat surfaces).
 * 
 * <p>Algorithm overview:
 * <ol>
 *   <li>For each face direction, create a 2D slice of the chunk</li>
 *   <li>Mark which faces are visible and their block type</li>
 *   <li>Greedily expand rectangles from each unprocessed face</li>
 *   <li>Generate a single quad for each rectangle</li>
 * </ol>
 * 
 * <p>Performance benefits:
 * <ul>
 *   <li>Can reduce vertex count by 50-90% in typical terrain</li>
 *   <li>Especially effective for underground areas</li>
 *   <li>Reduces draw calls and GPU memory usage</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class GreedyMeshBuilder {
    
    /** Initial capacity for vertex arrays. */
    private static final int INITIAL_VERTEX_CAPACITY = 40000;
    private static final int INITIAL_INDEX_CAPACITY = 60000;
    
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
    
    /** Mask for greedy meshing (stores block ID or 0 for no face). */
    private final int[] mask;
    
    /** Tint mask (stores tint data for each face). */
    private final float[][] tintMask;
    
    /** Cached normal vectors. */
    private static final float[][] FACE_NORMALS = {
        {0, 1, 0},   // TOP (+Y)
        {0, -1, 0},  // BOTTOM (-Y)
        {0, 0, -1},  // NORTH (-Z)
        {0, 0, 1},   // SOUTH (+Z)
        {1, 0, 0},   // EAST (+X)
        {-1, 0, 0}   // WEST (-X)
    };
    
    /**
     * Creates a new greedy mesh builder.
     */
    public GreedyMeshBuilder() {
        this.positions = new float[INITIAL_VERTEX_CAPACITY * 3];
        this.texCoords = new float[INITIAL_VERTEX_CAPACITY * 2];
        this.normals = new float[INITIAL_VERTEX_CAPACITY * 3];
        this.tints = new float[INITIAL_VERTEX_CAPACITY * 3];
        this.indices = new int[INITIAL_INDEX_CAPACITY];
        this.blockRegistry = BlockRegistry.getInstance();
        
        // Mask size: max of any 2D slice (WIDTH * HEIGHT or WIDTH * DEPTH or HEIGHT * DEPTH)
        int maxSliceSize = Math.max(Chunk.WIDTH * Chunk.HEIGHT, 
                          Math.max(Chunk.WIDTH * Chunk.DEPTH, Chunk.HEIGHT * Chunk.DEPTH));
        this.mask = new int[maxSliceSize];
        this.tintMask = new float[maxSliceSize][3];
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
     * Builds a greedy mesh for the specified chunk.
     * 
     * @param chunk The chunk to build
     * @param existingMesh Existing pooled mesh to reuse (or null)
     * @return The built mesh, or null if chunk has no visible faces
     */
    public PooledMesh buildGreedyMesh(Chunk chunk, PooledMesh existingMesh) {
        // Reset indices
        posIndex = 0;
        texIndex = 0;
        normIndex = 0;
        tintIndex = 0;
        indIndex = 0;
        vertexCount = 0;
        
        // Process each face direction
        for (BlockFace face : BlockFace.values()) {
            processDirection(chunk, face);
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
     * Processes all faces in a given direction using greedy meshing.
     */
    private void processDirection(Chunk chunk, BlockFace face) {
        // Determine the axis perpendicular to this face
        int axis = getAxis(face);
        boolean backface = isBackface(face);
        
        // Get dimensions for the 2D slice
        int[] dims = getSliceDimensions(axis);
        int uSize = dims[0];
        int vSize = dims[1];
        int dSize = dims[2]; // Depth (perpendicular to slice)
        
        // Process each slice perpendicular to the face direction
        for (int d = 0; d < dSize; d++) {
            // Build the mask for this slice
            buildMask(chunk, face, axis, d, uSize, vSize);
            
            // Greedy mesh the mask
            greedyMesh(chunk, face, axis, d, uSize, vSize, backface);
        }
    }
    
    /**
     * Gets the axis index for a face (0=X, 1=Y, 2=Z).
     */
    private int getAxis(BlockFace face) {
        return switch (face) {
            case EAST, WEST -> 0;   // X axis
            case TOP, BOTTOM -> 1;  // Y axis
            case NORTH, SOUTH -> 2; // Z axis
        };
    }
    
    /**
     * Checks if this is a backface (negative direction).
     */
    private boolean isBackface(BlockFace face) {
        return face == BlockFace.WEST || face == BlockFace.BOTTOM || face == BlockFace.NORTH;
    }
    
    /**
     * Gets the dimensions for a 2D slice perpendicular to the given axis.
     * Returns [uSize, vSize, dSize] where d is the depth direction.
     */
    private int[] getSliceDimensions(int axis) {
        return switch (axis) {
            case 0 -> new int[]{Chunk.DEPTH, Chunk.HEIGHT, Chunk.WIDTH};  // X axis: slice is ZY
            case 1 -> new int[]{Chunk.WIDTH, Chunk.DEPTH, Chunk.HEIGHT};  // Y axis: slice is XZ
            case 2 -> new int[]{Chunk.WIDTH, Chunk.HEIGHT, Chunk.DEPTH};  // Z axis: slice is XY
            default -> new int[]{Chunk.WIDTH, Chunk.HEIGHT, Chunk.DEPTH};
        };
    }
    
    /**
     * Builds the visibility mask for a 2D slice.
     */
    private void buildMask(Chunk chunk, BlockFace face, int axis, int d, int uSize, int vSize) {
        Arrays.fill(mask, 0);
        
        for (int v = 0; v < vSize; v++) {
            for (int u = 0; u < uSize; u++) {
                // Convert (u, v, d) to (x, y, z) based on axis
                int[] xyz = uvdToXYZ(u, v, d, axis);
                int x = xyz[0], y = xyz[1], z = xyz[2];
                
                int blockId = chunk.getBlock(x, y, z);
                if (blockId == 0) continue;
                
                Block block = blockRegistry.getBlock(blockId);
                if (block == null || block.isAir()) continue;
                
                // Check if this face is visible
                if (isFaceVisible(chunk, x, y, z, face)) {
                    int maskIndex = v * uSize + u;
                    mask[maskIndex] = blockId;
                    
                    // Store tint
                    float[] tint = block.getTint(face);
                    tintMask[maskIndex][0] = tint[0];
                    tintMask[maskIndex][1] = tint[1];
                    tintMask[maskIndex][2] = tint[2];
                }
            }
        }
    }
    
    /**
     * Converts (u, v, d) coordinates to (x, y, z) based on axis.
     */
    private int[] uvdToXYZ(int u, int v, int d, int axis) {
        return switch (axis) {
            case 0 -> new int[]{d, v, u};      // X axis: d=x, v=y, u=z
            case 1 -> new int[]{u, d, v};      // Y axis: u=x, d=y, v=z
            case 2 -> new int[]{u, v, d};      // Z axis: u=x, v=y, d=z
            default -> new int[]{u, v, d};
        };
    }
    
    /**
     * Performs greedy meshing on the mask.
     */
    private void greedyMesh(Chunk chunk, BlockFace face, int axis, int d, 
                           int uSize, int vSize, boolean backface) {
        for (int v = 0; v < vSize; v++) {
            for (int u = 0; u < uSize; ) {
                int maskIndex = v * uSize + u;
                int blockId = mask[maskIndex];
                
                if (blockId == 0) {
                    u++;
                    continue;
                }
                
                // Get tint for comparison
                float[] tint = tintMask[maskIndex];
                
                // Find width (how far we can extend in u direction)
                int width = 1;
                while (u + width < uSize) {
                    int nextIndex = v * uSize + (u + width);
                    if (mask[nextIndex] != blockId || 
                        !tintEquals(tintMask[nextIndex], tint)) {
                        break;
                    }
                    width++;
                }
                
                // Find height (how far we can extend in v direction)
                int height = 1;
                boolean done = false;
                while (v + height < vSize && !done) {
                    // Check entire row
                    for (int w = 0; w < width; w++) {
                        int checkIndex = (v + height) * uSize + (u + w);
                        if (mask[checkIndex] != blockId ||
                            !tintEquals(tintMask[checkIndex], tint)) {
                            done = true;
                            break;
                        }
                    }
                    if (!done) height++;
                }
                
                // Generate quad for this rectangle
                Block block = blockRegistry.getBlock(blockId);
                int[] xyz = uvdToXYZ(u, v, d, axis);
                float worldX = chunk.getWorldX(xyz[0]);
                float worldY = xyz[1];
                float worldZ = chunk.getWorldZ(xyz[2]);
                
                addGreedyQuad(worldX, worldY, worldZ, face, axis, 
                             width, height, block, tint, backface);
                
                // Clear the mask for processed area
                for (int dv = 0; dv < height; dv++) {
                    for (int du = 0; du < width; du++) {
                        mask[(v + dv) * uSize + (u + du)] = 0;
                    }
                }
                
                u += width;
            }
        }
    }
    
    /**
     * Checks if two tint arrays are equal.
     */
    private boolean tintEquals(float[] a, float[] b) {
        return Math.abs(a[0] - b[0]) < 0.001f &&
               Math.abs(a[1] - b[1]) < 0.001f &&
               Math.abs(a[2] - b[2]) < 0.001f;
    }
    
    /**
     * Adds a greedy quad to the mesh.
     */
    private void addGreedyQuad(float x, float y, float z, BlockFace face, int axis,
                               int width, int height, Block block, float[] tint, boolean backface) {
        ensureCapacity();
        
        // Get texture UVs
        String texturePath = block.getTextures().getTexture(face);
        float[] uvs = textureAtlas != null ? textureAtlas.getUVs(texturePath) : new float[]{0, 0, 1, 1};
        
        // Get face normal
        float[] normal = FACE_NORMALS[face.ordinal()];
        
        // Calculate quad vertices based on face and dimensions
        float[][] vertices = getGreedyQuadVertices(x, y, z, face, axis, width, height);
        
        // Scale UVs based on quad size for texture tiling
        float[][] uvCoords = getGreedyQuadUVs(uvs, width, height);
        
        // Reverse winding order for backfaces
        int[] order = backface ? new int[]{0, 1, 2, 3} : new int[]{3, 2, 1, 0};
        
        // Add 4 vertices
        for (int i = 0; i < 4; i++) {
            int idx = order[i];
            
            // Position
            positions[posIndex++] = vertices[idx][0];
            positions[posIndex++] = vertices[idx][1];
            positions[posIndex++] = vertices[idx][2];
            
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
     * Gets vertex positions for a greedy quad.
     */
    private float[][] getGreedyQuadVertices(float x, float y, float z, BlockFace face, 
                                            int axis, int width, int height) {
        float[][] vertices = new float[4][3];
        
        // Width and height map to different dimensions based on axis
        float w, h;
        
        switch (face) {
            case TOP:
                w = width; h = height;
                vertices[0] = new float[]{x, y + 1, z};
                vertices[1] = new float[]{x + w, y + 1, z};
                vertices[2] = new float[]{x + w, y + 1, z + h};
                vertices[3] = new float[]{x, y + 1, z + h};
                break;
            case BOTTOM:
                w = width; h = height;
                vertices[0] = new float[]{x, y, z + h};
                vertices[1] = new float[]{x + w, y, z + h};
                vertices[2] = new float[]{x + w, y, z};
                vertices[3] = new float[]{x, y, z};
                break;
            case NORTH:
                w = width; h = height;
                vertices[0] = new float[]{x + w, y, z};
                vertices[1] = new float[]{x, y, z};
                vertices[2] = new float[]{x, y + h, z};
                vertices[3] = new float[]{x + w, y + h, z};
                break;
            case SOUTH:
                w = width; h = height;
                vertices[0] = new float[]{x, y, z + 1};
                vertices[1] = new float[]{x + w, y, z + 1};
                vertices[2] = new float[]{x + w, y + h, z + 1};
                vertices[3] = new float[]{x, y + h, z + 1};
                break;
            case EAST:
                w = height; h = width;  // Swapped for this axis
                vertices[0] = new float[]{x + 1, y, z + w};
                vertices[1] = new float[]{x + 1, y, z};
                vertices[2] = new float[]{x + 1, y + h, z};
                vertices[3] = new float[]{x + 1, y + h, z + w};
                break;
            case WEST:
                w = height; h = width;  // Swapped for this axis
                vertices[0] = new float[]{x, y, z};
                vertices[1] = new float[]{x, y, z + w};
                vertices[2] = new float[]{x, y + h, z + w};
                vertices[3] = new float[]{x, y + h, z};
                break;
            default:
                // Default to TOP
                vertices[0] = new float[]{x, y + 1, z};
                vertices[1] = new float[]{x + width, y + 1, z};
                vertices[2] = new float[]{x + width, y + 1, z + height};
                vertices[3] = new float[]{x, y + 1, z + height};
        }
        
        return vertices;
    }
    
    /**
     * Gets UV coordinates for a greedy quad with tiling.
     */
    private float[][] getGreedyQuadUVs(float[] uvs, int width, int height) {
        float u1 = uvs[0], v1 = uvs[1], u2 = uvs[2], v2 = uvs[3];
        float uSize = u2 - u1;
        float vSize = v2 - v1;
        
        // Tile the texture across the quad
        return new float[][] {
            {u1, v1 + vSize * height},
            {u1 + uSize * width, v1 + vSize * height},
            {u1 + uSize * width, v1},
            {u1, v1}
        };
    }
    
    /**
     * Checks if a face is visible.
     */
    private boolean isFaceVisible(Chunk chunk, int x, int y, int z, BlockFace face) {
        int nx = x + face.getOffsetX();
        int ny = y + face.getOffsetY();
        int nz = z + face.getOffsetZ();
        
        if (ny < 0) return false;
        if (ny >= Chunk.HEIGHT) return true;
        
        if (nx < 0 || nx >= Chunk.WIDTH || nz < 0 || nz >= Chunk.DEPTH) {
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


