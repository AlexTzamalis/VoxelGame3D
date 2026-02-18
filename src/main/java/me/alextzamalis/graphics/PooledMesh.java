package me.alextzamalis.graphics;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL30.*;

/**
 * A reusable mesh that can be updated with new vertex data without reallocating GPU buffers.
 * 
 * <p>This class is designed for chunk meshes that frequently need to be rebuilt.
 * Instead of creating new OpenGL buffers each time, it reuses existing buffers
 * and only reallocates when the new data exceeds the current buffer capacity.
 * 
 * <p>Key optimizations:
 * <ul>
 *   <li>Buffer reuse - avoids GPU memory fragmentation</li>
 *   <li>Capacity-based growth - only reallocates when necessary</li>
 *   <li>Uses GL_DYNAMIC_DRAW for frequently updated data</li>
 *   <li>Poolable - can be returned to a MeshPool for reuse</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class PooledMesh {
    
    /** Default initial capacity (vertices). */
    private static final int DEFAULT_INITIAL_CAPACITY = 4096;
    
    /** Growth factor when reallocating. */
    private static final float GROWTH_FACTOR = 1.5f;
    
    /** The Vertex Array Object ID. */
    private int vaoId;
    
    /** The position VBO ID. */
    private int posVboId;
    
    /** The texture coordinate VBO ID. */
    private int texVboId;
    
    /** The normal VBO ID. */
    private int normVboId;
    
    /** The tint color VBO ID. */
    private int tintVboId;
    
    /** The Element Buffer Object ID. */
    private int eboId;
    
    /** Current buffer capacities (in elements, not bytes). */
    private int posCapacity;
    private int texCapacity;
    private int normCapacity;
    private int tintCapacity;
    private int indexCapacity;
    
    /** The number of indices to render. */
    private int indexCount;
    
    /** Whether this mesh is currently in use. */
    private boolean inUse;
    
    /** Whether this mesh has valid data. */
    private boolean hasData;
    
    /**
     * Creates a new pooled mesh with default capacity.
     */
    public PooledMesh() {
        this(DEFAULT_INITIAL_CAPACITY);
    }
    
    /**
     * Creates a new pooled mesh with the specified initial capacity.
     * 
     * @param initialVertexCapacity Initial capacity in vertices
     */
    public PooledMesh(int initialVertexCapacity) {
        this.inUse = false;
        this.hasData = false;
        this.indexCount = 0;
        
        // Create VAO
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        
        // Calculate capacities (positions = 3 floats per vertex, etc.)
        posCapacity = initialVertexCapacity * 3;
        texCapacity = initialVertexCapacity * 2;
        normCapacity = initialVertexCapacity * 3;
        tintCapacity = initialVertexCapacity * 3;
        indexCapacity = initialVertexCapacity * 6; // Assume quads (6 indices per quad, 4 vertices per quad)
        
        // Create position VBO (location 0)
        posVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, posVboId);
        glBufferData(GL_ARRAY_BUFFER, (long) posCapacity * Float.BYTES, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        
        // Create texture coordinate VBO (location 1)
        texVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, texVboId);
        glBufferData(GL_ARRAY_BUFFER, (long) texCapacity * Float.BYTES, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        
        // Create normal VBO (location 2)
        normVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, normVboId);
        glBufferData(GL_ARRAY_BUFFER, (long) normCapacity * Float.BYTES, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
        
        // Create tint color VBO (location 3)
        tintVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, tintVboId);
        glBufferData(GL_ARRAY_BUFFER, (long) tintCapacity * Float.BYTES, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(3);
        glVertexAttribPointer(3, 3, GL_FLOAT, false, 0, 0);
        
        // Create EBO
        eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, (long) indexCapacity * Integer.BYTES, GL_DYNAMIC_DRAW);
        
        // Unbind
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }
    
    /**
     * Updates the mesh with new vertex data.
     * 
     * @param positions Vertex positions (x, y, z per vertex)
     * @param texCoords Texture coordinates (u, v per vertex)
     * @param normals Vertex normals (x, y, z per vertex)
     * @param tints Tint colors (r, g, b per vertex)
     * @param indices Vertex indices
     */
    public void updateData(float[] positions, float[] texCoords, float[] normals, 
                          float[] tints, int[] indices) {
        if (positions == null || indices == null || indices.length == 0) {
            hasData = false;
            indexCount = 0;
            return;
        }
        
        glBindVertexArray(vaoId);
        
        // Update positions
        updateBuffer(posVboId, positions, posCapacity, 0);
        if (positions.length > posCapacity) {
            posCapacity = calculateNewCapacity(positions.length);
        }
        
        // Update texture coordinates
        if (texCoords != null && texCoords.length > 0) {
            updateBuffer(texVboId, texCoords, texCapacity, 1);
            if (texCoords.length > texCapacity) {
                texCapacity = calculateNewCapacity(texCoords.length);
            }
        }
        
        // Update normals
        if (normals != null && normals.length > 0) {
            updateBuffer(normVboId, normals, normCapacity, 2);
            if (normals.length > normCapacity) {
                normCapacity = calculateNewCapacity(normals.length);
            }
        }
        
        // Update tints
        if (tints != null && tints.length > 0) {
            updateBuffer(tintVboId, tints, tintCapacity, 3);
            if (tints.length > tintCapacity) {
                tintCapacity = calculateNewCapacity(tints.length);
            }
        }
        
        // Update indices
        updateIndexBuffer(indices);
        
        glBindVertexArray(0);
        
        indexCount = indices.length;
        hasData = true;
    }
    
    /**
     * Updates a float buffer, reallocating if necessary.
     */
    private void updateBuffer(int vboId, float[] data, int currentCapacity, int attribIndex) {
        FloatBuffer buffer = null;
        try {
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            
            if (data.length > currentCapacity) {
                // Need to reallocate
                int newCapacity = calculateNewCapacity(data.length);
                buffer = MemoryUtil.memAllocFloat(newCapacity);
                buffer.put(data);
                buffer.flip();
                glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);
            } else {
                // Can use subdata
                buffer = MemoryUtil.memAllocFloat(data.length);
                buffer.put(data);
                buffer.flip();
                glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
            }
        } finally {
            if (buffer != null) {
                MemoryUtil.memFree(buffer);
            }
        }
    }
    
    /**
     * Updates the index buffer, reallocating if necessary.
     */
    private void updateIndexBuffer(int[] indices) {
        IntBuffer buffer = null;
        try {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
            
            if (indices.length > indexCapacity) {
                // Need to reallocate
                indexCapacity = calculateNewCapacity(indices.length);
                buffer = MemoryUtil.memAllocInt(indexCapacity);
                buffer.put(indices);
                buffer.flip();
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);
            } else {
                // Can use subdata
                buffer = MemoryUtil.memAllocInt(indices.length);
                buffer.put(indices);
                buffer.flip();
                glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, buffer);
            }
        } finally {
            if (buffer != null) {
                MemoryUtil.memFree(buffer);
            }
        }
    }
    
    /**
     * Calculates a new capacity using growth factor.
     */
    private int calculateNewCapacity(int required) {
        return (int) (required * GROWTH_FACTOR);
    }
    
    /**
     * Renders the mesh.
     */
    public void render() {
        if (!hasData || indexCount == 0) {
            return;
        }
        
        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }
    
    /**
     * Checks if this mesh has valid data to render.
     * 
     * @return true if has data
     */
    public boolean hasData() {
        return hasData;
    }
    
    /**
     * Gets the index count.
     * 
     * @return Number of indices
     */
    public int getIndexCount() {
        return indexCount;
    }
    
    /**
     * Gets the vertex count (approximate, based on indices).
     * 
     * @return Vertex count
     */
    public int getVertexCount() {
        return indexCount;
    }
    
    /**
     * Marks this mesh as in use.
     */
    public void acquire() {
        inUse = true;
    }
    
    /**
     * Marks this mesh as available for reuse.
     */
    public void release() {
        inUse = false;
        hasData = false;
        indexCount = 0;
    }
    
    /**
     * Checks if this mesh is in use.
     * 
     * @return true if in use
     */
    public boolean isInUse() {
        return inUse;
    }
    
    /**
     * Cleans up OpenGL resources.
     */
    public void cleanup() {
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        
        if (posVboId != 0) glDeleteBuffers(posVboId);
        if (texVboId != 0) glDeleteBuffers(texVboId);
        if (normVboId != 0) glDeleteBuffers(normVboId);
        if (tintVboId != 0) glDeleteBuffers(tintVboId);
        if (eboId != 0) glDeleteBuffers(eboId);
        if (vaoId != 0) glDeleteVertexArrays(vaoId);
        
        vaoId = 0;
        posVboId = 0;
        texVboId = 0;
        normVboId = 0;
        tintVboId = 0;
        eboId = 0;
    }
}


