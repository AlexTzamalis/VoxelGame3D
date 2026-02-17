package me.alextzamalis.graphics;

import me.alextzamalis.util.Logger;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * Manages a global VBO/IBO buffer for all chunk data, enabling Multi-Draw Indirect (MDI).
 * 
 * <p>This class replaces individual VBOs per chunk with a single large buffer that
 * stores all chunk vertex and index data. Chunks are allocated sub-sections of this
 * buffer, tracked by offsets and sizes.
 * 
 * <p>Benefits:
 * <ul>
 *   <li>Single buffer reduces GPU memory fragmentation</li>
 *   <li>Enables Multi-Draw Indirect (one draw call for all chunks)</li>
 *   <li>Better GPU cache utilization</li>
 *   <li>Reduced driver overhead</li>
 * </ul>
 * 
 * <p>Buffer layout:
 * <ul>
 *   <li>Vertex Buffer: Packed vertex data (position, UV, normal, light) as integers</li>
 *   <li>Index Buffer: All chunk indices in one buffer</li>
 *   <li>Indirect Draw Buffer: Draw commands for glMultiDrawElementsIndirect</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class GlobalBufferManager {
    
    /** Initial vertex buffer size (in integers, ~4MB for 1M vertices). */
    private static final int INITIAL_VERTEX_BUFFER_SIZE = 1_000_000 * 2; // 2 ints per vertex
    
    /** Initial index buffer size (in integers, ~16MB for 4M indices). */
    private static final int INITIAL_INDEX_BUFFER_SIZE = 4_000_000;
    
    /** Growth factor when buffer needs to expand. */
    private static final float GROWTH_FACTOR = 1.5f;
    
    /** The global vertex buffer ID (stores packed vertex data). */
    private int vertexBufferId;
    
    /** The global index buffer ID. */
    private int indexBufferId;
    
    /** The indirect draw command buffer ID. */
    private int indirectBufferId;
    
    /** Current vertex buffer capacity (in integers). */
    private int vertexBufferCapacity;
    
    /** Current index buffer capacity (in integers). */
    private int indexBufferCapacity;
    
    /** Current vertex buffer usage (in integers). */
    private int vertexBufferUsed;
    
    /** Current index buffer usage (in integers). */
    private int indexBufferUsed;
    
    /** Map of chunk keys to their buffer allocations. */
    private final Map<Long, ChunkAllocation> allocations;
    
    /** The VAO for rendering. */
    private int vaoId;
    
    /**
     * Represents a chunk's allocation within the global buffer.
     */
    public static class ChunkAllocation {
        /** Vertex offset in the vertex buffer (in integers). */
        public final int vertexOffset;
        
        /** Number of vertices. */
        public final int vertexCount;
        
        /** Index offset in the index buffer (in integers). */
        public final int indexOffset;
        
        /** Number of indices. */
        public final int indexCount;
        
        /** Whether this allocation is valid. */
        public boolean valid;
        
        public ChunkAllocation(int vertexOffset, int vertexCount, int indexOffset, int indexCount) {
            this.vertexOffset = vertexOffset;
            this.vertexCount = vertexCount;
            this.indexOffset = indexOffset;
            this.indexCount = indexCount;
            this.valid = true;
        }
    }
    
    /**
     * Creates a new global buffer manager.
     */
    public GlobalBufferManager() {
        this.allocations = new HashMap<>();
        this.vertexBufferCapacity = INITIAL_VERTEX_BUFFER_SIZE;
        this.indexBufferCapacity = INITIAL_INDEX_BUFFER_SIZE;
        this.vertexBufferUsed = 0;
        this.indexBufferUsed = 0;
        
        initializeBuffers();
    }
    
    /**
     * Initializes OpenGL buffers.
     */
    private void initializeBuffers() {
        // Create VAO
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        
        // Create vertex buffer (stores packed vertex data as integers)
        vertexBufferId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferId);
        glBufferData(GL_ARRAY_BUFFER, (long) vertexBufferCapacity * Integer.BYTES, GL_DYNAMIC_DRAW);
        
        // Set up vertex attributes for packed data
        // Attribute 0: Position (packed into 2 ints, but we'll use 1 for now)
        glEnableVertexAttribArray(0);
        glVertexAttribIPointer(0, 1, GL_INT, 0, 0); // Position packed as int
        
        // Attribute 1: UV (packed as int)
        glEnableVertexAttribArray(1);
        glVertexAttribIPointer(1, 1, GL_INT, 0, 4); // Offset by 4 bytes (1 int)
        
        // Attribute 2: Normal (packed as int)
        glEnableVertexAttribArray(2);
        glVertexAttribIPointer(2, 1, GL_INT, 0, 8); // Offset by 8 bytes (2 ints)
        
        // Attribute 3: Light (packed as int, but only uses 1 byte)
        glEnableVertexAttribArray(3);
        glVertexAttribIPointer(3, 1, GL_INT, 0, 12); // Offset by 12 bytes (3 ints)
        
        // Create index buffer
        indexBufferId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, (long) indexBufferCapacity * Integer.BYTES, GL_DYNAMIC_DRAW);
        
        // Create indirect draw command buffer
        indirectBufferId = glGenBuffers();
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectBufferId);
        
        // Unbind
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        
        Logger.info("GlobalBufferManager initialized: VAO=%d, VBO=%d, IBO=%d, Indirect=%d",
                    vaoId, vertexBufferId, indexBufferId, indirectBufferId);
    }
    
    /**
     * Allocates buffer space for a chunk's mesh data.
     * 
     * @param chunkKey Unique chunk identifier
     * @param vertexCount Number of vertices (will be packed into 2 ints each)
     * @param indexCount Number of indices
     * @return Allocation info, or null if allocation failed
     */
    public ChunkAllocation allocateChunk(long chunkKey, int vertexCount, int indexCount) {
        // Calculate required space
        int requiredVertexSpace = vertexCount * 2; // 2 ints per vertex (position + UV/normal/light)
        int requiredIndexSpace = indexCount;
        
        // Check if we need to expand buffers
        if (vertexBufferUsed + requiredVertexSpace > vertexBufferCapacity) {
            expandVertexBuffer(vertexBufferUsed + requiredVertexSpace);
        }
        if (indexBufferUsed + requiredIndexSpace > indexBufferCapacity) {
            expandIndexBuffer(indexBufferUsed + requiredIndexSpace);
        }
        
        // Allocate space
        int vertexOffset = vertexBufferUsed;
        int indexOffset = indexBufferUsed;
        
        vertexBufferUsed += requiredVertexSpace;
        indexBufferUsed += requiredIndexSpace;
        
        ChunkAllocation allocation = new ChunkAllocation(vertexOffset, vertexCount, indexOffset, indexCount);
        allocations.put(chunkKey, allocation);
        
        return allocation;
    }
    
    /**
     * Updates chunk data in the global buffer.
     * 
     * @param chunkKey Unique chunk identifier
     * @param packedVertices Packed vertex data (2 ints per vertex)
     * @param indices Index data
     */
    public void updateChunk(long chunkKey, int[] packedVertices, int[] indices) {
        ChunkAllocation alloc = allocations.get(chunkKey);
        if (alloc == null || !alloc.valid) {
            Logger.warn("Attempted to update non-existent chunk allocation: %d", chunkKey);
            return;
        }
        
        // Update vertex data
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferId);
        IntBuffer vertexBuffer = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            vertexBuffer = stack.mallocInt(packedVertices.length);
            vertexBuffer.put(packedVertices);
            vertexBuffer.flip();
            glBufferSubData(GL_ARRAY_BUFFER, (long) alloc.vertexOffset * Integer.BYTES, vertexBuffer);
        }
        
        // Update index data (need to adjust indices for global buffer offset)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
        int[] adjustedIndices = new int[indices.length];
        int baseVertex = alloc.vertexOffset / 2; // Convert from packed ints to vertex count
        for (int i = 0; i < indices.length; i++) {
            adjustedIndices[i] = indices[i] + baseVertex;
        }
        
        IntBuffer indexBuffer = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            indexBuffer = stack.mallocInt(adjustedIndices.length);
            indexBuffer.put(adjustedIndices);
            indexBuffer.flip();
            glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, (long) alloc.indexOffset * Integer.BYTES, indexBuffer);
        }
        
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }
    
    /**
     * Deallocates a chunk's buffer space.
     * 
     * @param chunkKey Unique chunk identifier
     */
    public void deallocateChunk(long chunkKey) {
        ChunkAllocation alloc = allocations.remove(chunkKey);
        if (alloc != null) {
            alloc.valid = false;
            // Note: We don't compact the buffer immediately - that would be expensive
            // Instead, we mark it as invalid and reuse the space later
        }
    }
    
    /**
     * Expands the vertex buffer if needed.
     */
    private void expandVertexBuffer(int requiredSize) {
        int newCapacity = (int) (requiredSize * GROWTH_FACTOR);
        Logger.info("Expanding vertex buffer: %d -> %d integers", vertexBufferCapacity, newCapacity);
        
        // Create new buffer
        int newBufferId = glGenBuffers();
        glBindBuffer(GL_COPY_READ_BUFFER, vertexBufferId);
        glBindBuffer(GL_COPY_WRITE_BUFFER, newBufferId);
        glBufferData(GL_COPY_WRITE_BUFFER, (long) newCapacity * Integer.BYTES, GL_DYNAMIC_DRAW);
        
        // Copy old data
        glCopyBufferSubData(GL_COPY_READ_BUFFER, GL_COPY_WRITE_BUFFER, 0, 0, (long) vertexBufferUsed * Integer.BYTES);
        
        // Delete old buffer and update
        glDeleteBuffers(vertexBufferId);
        vertexBufferId = newBufferId;
        vertexBufferCapacity = newCapacity;
        
        // Rebind to VAO
        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferId);
        glVertexAttribIPointer(0, 1, GL_INT, 0, 0);
        glVertexAttribIPointer(1, 1, GL_INT, 0, 4);
        glVertexAttribIPointer(2, 1, GL_INT, 0, 8);
        glVertexAttribIPointer(3, 1, GL_INT, 0, 12);
        glBindVertexArray(0);
        
        glBindBuffer(GL_COPY_READ_BUFFER, 0);
        glBindBuffer(GL_COPY_WRITE_BUFFER, 0);
    }
    
    /**
     * Expands the index buffer if needed.
     */
    private void expandIndexBuffer(int requiredSize) {
        int newCapacity = (int) (requiredSize * GROWTH_FACTOR);
        Logger.info("Expanding index buffer: %d -> %d integers", indexBufferCapacity, newCapacity);
        
        // Create new buffer
        int newBufferId = glGenBuffers();
        glBindBuffer(GL_COPY_READ_BUFFER, indexBufferId);
        glBindBuffer(GL_COPY_WRITE_BUFFER, newBufferId);
        glBufferData(GL_COPY_WRITE_BUFFER, (long) newCapacity * Integer.BYTES, GL_DYNAMIC_DRAW);
        
        // Copy old data
        glCopyBufferSubData(GL_COPY_READ_BUFFER, GL_COPY_WRITE_BUFFER, 0, 0, (long) indexBufferUsed * Integer.BYTES);
        
        // Delete old buffer and update
        glDeleteBuffers(indexBufferId);
        indexBufferId = newBufferId;
        indexBufferCapacity = newCapacity;
        
        // Rebind to VAO
        glBindVertexArray(vaoId);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
        glBindVertexArray(0);
        
        glBindBuffer(GL_COPY_READ_BUFFER, 0);
        glBindBuffer(GL_COPY_WRITE_BUFFER, 0);
    }
    
    /**
     * Gets the VAO ID for rendering.
     * 
     * @return VAO ID
     */
    public int getVaoId() {
        return vaoId;
    }
    
    /**
     * Gets the indirect draw buffer ID.
     * 
     * @return Indirect buffer ID
     */
    public int getIndirectBufferId() {
        return indirectBufferId;
    }
    
    /**
     * Gets all chunk allocations for building indirect draw commands.
     * 
     * @return Map of chunk keys to allocations
     */
    public Map<Long, ChunkAllocation> getAllocations() {
        return allocations;
    }
    
    /**
     * Cleans up OpenGL resources.
     */
    public void cleanup() {
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
        
        if (vertexBufferId != 0) glDeleteBuffers(vertexBufferId);
        if (indexBufferId != 0) glDeleteBuffers(indexBufferId);
        if (indirectBufferId != 0) glDeleteBuffers(indirectBufferId);
        if (vaoId != 0) glDeleteVertexArrays(vaoId);
        
        vertexBufferId = 0;
        indexBufferId = 0;
        indirectBufferId = 0;
        vaoId = 0;
        
        allocations.clear();
    }
}

