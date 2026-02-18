package me.alextzamalis.graphics;

import me.alextzamalis.util.Logger;
import me.alextzamalis.voxel.Chunk;
import me.alextzamalis.voxel.LODManager;
import me.alextzamalis.voxel.SimplifiedChunk;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * Multi-Draw Indirect (MDI) renderer for efficient chunk rendering.
 * 
 * <p>This renderer uses OpenGL 4.3+ Multi-Draw Indirect to render all chunks
 * in a single draw call, dramatically reducing CPU overhead and improving
 * GPU utilization.
 * 
 * <p>Instead of:
 * <pre>{@code
 * for (Chunk chunk : chunks) {
 *     chunk.getMesh().render(); // 500 draw calls
 * }
 * }</pre>
 * 
 * <p>We do:
 * <pre>{@code
 * glMultiDrawElementsIndirect(...); // 1 draw call for all chunks
 * }</pre>
 * 
 * <p>The indirect draw commands are built from chunk allocations in the
 * GlobalBufferManager and updated each frame based on visible chunks.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class MDIRenderer {
    
    /** The global buffer manager. */
    private final GlobalBufferManager bufferManager;
    
    /** Current list of visible chunks to render. */
    private final List<Chunk> visibleChunks;
    
    /** Current list of visible simplified chunks to render. */
    private final List<SimplifiedChunk> visibleSimplifiedChunks;
    
    /** LOD manager for accessing simplified chunks. */
    private LODManager lodManager;
    
    /**
     * Creates a new MDI renderer.
     * 
     * @param bufferManager The global buffer manager
     */
    public MDIRenderer(GlobalBufferManager bufferManager) {
        this.bufferManager = bufferManager;
        this.visibleChunks = new ArrayList<>();
        this.visibleSimplifiedChunks = new ArrayList<>();
    }
    
    /**
     * Sets the LOD manager for simplified chunk rendering.
     * 
     * @param lodManager The LOD manager
     */
    public void setLODManager(LODManager lodManager) {
        this.lodManager = lodManager;
    }
    
    /**
     * Prepares the renderer for a new frame by collecting visible chunks.
     * 
     * @param chunks All chunks in the world
     * @param frustumCuller Frustum culler to test chunk visibility
     */
    public void prepareFrame(Iterable<Chunk> chunks, FrustumCuller frustumCuller) {
        visibleChunks.clear();
        visibleSimplifiedChunks.clear();
        
        // Collect full chunks
        for (Chunk chunk : chunks) {
            // Check if chunk is in global buffer (MDI rendering)
            long chunkKey = getChunkKey(chunk.getChunkX(), chunk.getChunkZ());
            GlobalBufferManager.ChunkAllocation alloc = bufferManager.getAllocations().get(chunkKey);
            
            // Only include chunks that are in the global buffer and valid
            if (alloc != null && alloc.valid && alloc.indexCount > 0) {
                if (frustumCuller.isChunkInFrustum(
                        chunk.getChunkX(), chunk.getChunkZ(),
                        Chunk.WIDTH, Chunk.HEIGHT, Chunk.DEPTH)) {
                    visibleChunks.add(chunk);
                }
            }
        }
        
        // Collect simplified chunks if LOD is enabled
        if (lodManager != null && lodManager.isEnabled()) {
            // Get all simplified chunks from LOD manager
            // We need to iterate through all possible chunk positions in view distance
            // For now, we'll check simplified chunks that exist in the LOD manager
            // This could be optimized by having LODManager provide an iterator
            
            // Check simplified chunks in the global buffer
            for (var entry : bufferManager.getAllocations().entrySet()) {
                long chunkKey = entry.getKey();
                GlobalBufferManager.ChunkAllocation alloc = entry.getValue();
                
                // If allocation exists and is valid, check if it's a simplified chunk
                if (alloc != null && alloc.valid && alloc.indexCount > 0) {
                    int chunkX = (int) (chunkKey >> 32);
                    int chunkZ = (int) chunkKey;
                    
                    SimplifiedChunk simplified = lodManager.getSimplifiedChunk(chunkX, chunkZ);
                    if (simplified != null && simplified.isGenerated()) {
                        // Check frustum (use simplified chunk dimensions)
                        int width = simplified.getWidth() * simplified.getScale();
                        int depth = simplified.getDepth() * simplified.getScale();
                        
                        if (frustumCuller.isChunkInFrustum(
                                chunkX, chunkZ,
                                width, SimplifiedChunk.HEIGHT, depth)) {
                            visibleSimplifiedChunks.add(simplified);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Builds indirect draw commands for all visible chunks (full and simplified).
     * 
     * <p>Each draw command contains:
     * <ul>
     *   <li>count: Number of indices to draw</li>
     *   <li>instanceCount: Number of instances (always 1 for chunks)</li>
     *   <li>firstIndex: Offset into index buffer</li>
     *   <li>baseVertex: Offset into vertex buffer (0, handled by index adjustment)</li>
     *   <li>baseInstance: Instance ID (0 for full chunks, 1 for simplified chunks for LOD transitions)</li>
     * </ul>
     * 
     * @return Number of draw commands created
     */
    public int buildIndirectCommands() {
        int totalCommands = visibleChunks.size() + visibleSimplifiedChunks.size();
        if (totalCommands == 0) {
            return 0;
        }
        
        // DrawElementsIndirectCommand structure (5 ints):
        // - count (4 bytes): Number of indices
        // - instanceCount (4 bytes): Number of instances (1)
        // - firstIndex (4 bytes): Offset into index buffer
        // - baseVertex (4 bytes): Vertex offset (0, indices are already adjusted)
        // - baseInstance (4 bytes): Instance ID (0 for full, 1 for simplified for shader LOD)
        
        int commandSize = 5; // 5 ints per command
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer commandBuffer = stack.mallocInt(totalCommands * commandSize);
            
            int commandIndex = 0;
            
            // Add full chunks first
            for (Chunk chunk : visibleChunks) {
                // Get chunk key
                long chunkKey = getChunkKey(chunk.getChunkX(), chunk.getChunkZ());
                GlobalBufferManager.ChunkAllocation alloc = bufferManager.getAllocations().get(chunkKey);
                
                if (alloc == null || !alloc.valid) {
                    // Chunk not in global buffer, skip
                    continue;
                }
                
                // Build draw command (baseInstance = 0 for full chunks)
                int offset = commandIndex * commandSize;
                commandBuffer.put(offset + 0, alloc.indexCount);      // count
                commandBuffer.put(offset + 1, 1);                    // instanceCount
                commandBuffer.put(offset + 2, alloc.indexOffset);   // firstIndex
                commandBuffer.put(offset + 3, 0);                    // baseVertex (indices already adjusted)
                commandBuffer.put(offset + 4, 0);                    // baseInstance (0 = full chunk)
                
                commandIndex++;
            }
            
            // Add simplified chunks (baseInstance = 1 for LOD shader transitions)
            for (SimplifiedChunk simplified : visibleSimplifiedChunks) {
                // Get chunk key
                long chunkKey = getChunkKey(simplified.getChunkX(), simplified.getChunkZ());
                GlobalBufferManager.ChunkAllocation alloc = bufferManager.getAllocations().get(chunkKey);
                
                if (alloc == null || !alloc.valid) {
                    // Simplified chunk not in global buffer, skip
                    continue;
                }
                
                // Build draw command (baseInstance = 1 for simplified chunks)
                int offset = commandIndex * commandSize;
                commandBuffer.put(offset + 0, alloc.indexCount);      // count
                commandBuffer.put(offset + 1, 1);                 // instanceCount
                commandBuffer.put(offset + 2, alloc.indexOffset);   // firstIndex
                commandBuffer.put(offset + 3, 0);                    // baseVertex (indices already adjusted)
                commandBuffer.put(offset + 4, 1);                    // baseInstance (1 = simplified chunk for LOD)
                
                commandIndex++;
            }
            
            // Update actual command count (may be less if some chunks weren't in buffer)
            int actualCommandCount = commandIndex;
            
            // Upload to GPU
            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, bufferManager.getIndirectBufferId());
            glBufferData(GL_DRAW_INDIRECT_BUFFER, commandBuffer, GL_DYNAMIC_DRAW);
            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
            
            return actualCommandCount;
        }
    }
    
    /**
     * Renders all visible chunks using Multi-Draw Indirect.
     * 
     * <p>This performs a single draw call for all chunks, dramatically
     * reducing CPU overhead compared to per-chunk draw calls.
     * 
     * @param commandCount Number of draw commands to execute
     */
    public void render(int commandCount) {
        if (commandCount == 0) {
            return;
        }
        
        // Bind VAO and buffers
        glBindVertexArray(bufferManager.getVaoId());
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, bufferManager.getIndirectBufferId());
        
        // Perform multi-draw indirect
        // This single call draws all chunks!
        glMultiDrawElementsIndirect(
                GL_TRIANGLES,           // Primitive type
                GL_UNSIGNED_INT,        // Index type
                0,                      // Offset into indirect buffer
                commandCount,           // Number of draw commands
                0                       // Stride (0 = tightly packed)
        );
        
        // Unbind
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
        glBindVertexArray(0);
    }
    
    /**
     * Gets the chunk key for a chunk coordinate.
     * 
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @return Chunk key
     */
    private long getChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
    
    /**
     * Gets the number of visible chunks.
     * 
     * @return Visible chunk count
     */
    public int getVisibleChunkCount() {
        return visibleChunks.size();
    }
}

