package me.alextzamalis.graphics;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL30.*;

/**
 * Represents a 3D mesh with vertices, texture coordinates, normals, and indices.
 * 
 * <p>This class manages OpenGL Vertex Array Objects (VAO) and Vertex Buffer Objects (VBO)
 * for efficient rendering. It supports positions, texture coordinates, normals, and
 * indexed rendering using Element Buffer Objects (EBO).
 * 
 * <p>Usage example:
 * <pre>{@code
 * float[] positions = { -0.5f, -0.5f, 0.0f, 0.5f, -0.5f, 0.0f, 0.0f, 0.5f, 0.0f };
 * float[] texCoords = { 0.0f, 1.0f, 1.0f, 1.0f, 0.5f, 0.0f };
 * int[] indices = { 0, 1, 2 };
 * 
 * Mesh mesh = new Mesh(positions, texCoords, null, indices);
 * mesh.render();
 * mesh.cleanup();
 * }</pre>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class Mesh {
    
    /** The Vertex Array Object ID. */
    private final int vaoId;
    
    /** The position VBO ID. */
    private final int posVboId;
    
    /** The texture coordinate VBO ID. */
    private final int texVboId;
    
    /** The normal VBO ID. */
    private final int normVboId;
    
    /** The Element Buffer Object ID. */
    private final int eboId;
    
    /** The number of vertices to render. */
    private final int vertexCount;
    
    /**
     * Creates a new mesh with the specified vertex data.
     * 
     * @param positions The vertex positions (x, y, z for each vertex)
     * @param texCoords The texture coordinates (u, v for each vertex), can be null
     * @param normals The vertex normals (x, y, z for each vertex), can be null
     * @param indices The vertex indices for indexed rendering
     */
    public Mesh(float[] positions, float[] texCoords, float[] normals, int[] indices) {
        FloatBuffer posBuffer = null;
        FloatBuffer texBuffer = null;
        FloatBuffer normBuffer = null;
        IntBuffer indicesBuffer = null;
        
        try {
            vertexCount = indices.length;
            
            // Create and bind VAO
            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);
            
            // Position VBO
            posVboId = glGenBuffers();
            posBuffer = MemoryUtil.memAllocFloat(positions.length);
            posBuffer.put(positions).flip();
            glBindBuffer(GL_ARRAY_BUFFER, posVboId);
            glBufferData(GL_ARRAY_BUFFER, posBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            
            // Texture coordinate VBO
            if (texCoords != null && texCoords.length > 0) {
                texVboId = glGenBuffers();
                texBuffer = MemoryUtil.memAllocFloat(texCoords.length);
                texBuffer.put(texCoords).flip();
                glBindBuffer(GL_ARRAY_BUFFER, texVboId);
                glBufferData(GL_ARRAY_BUFFER, texBuffer, GL_STATIC_DRAW);
                glEnableVertexAttribArray(1);
                glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
            } else {
                texVboId = 0;
            }
            
            // Normal VBO
            if (normals != null && normals.length > 0) {
                normVboId = glGenBuffers();
                normBuffer = MemoryUtil.memAllocFloat(normals.length);
                normBuffer.put(normals).flip();
                glBindBuffer(GL_ARRAY_BUFFER, normVboId);
                glBufferData(GL_ARRAY_BUFFER, normBuffer, GL_STATIC_DRAW);
                glEnableVertexAttribArray(2);
                glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
            } else {
                normVboId = 0;
            }
            
            // Element Buffer Object (indices)
            eboId = glGenBuffers();
            indicesBuffer = MemoryUtil.memAllocInt(indices.length);
            indicesBuffer.put(indices).flip();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
            
            // Unbind VAO
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
            
        } finally {
            // Free memory buffers
            if (posBuffer != null) {
                MemoryUtil.memFree(posBuffer);
            }
            if (texBuffer != null) {
                MemoryUtil.memFree(texBuffer);
            }
            if (normBuffer != null) {
                MemoryUtil.memFree(normBuffer);
            }
            if (indicesBuffer != null) {
                MemoryUtil.memFree(indicesBuffer);
            }
        }
    }
    
    /**
     * Gets the VAO ID.
     * 
     * @return The Vertex Array Object ID
     */
    public int getVaoId() {
        return vaoId;
    }
    
    /**
     * Gets the vertex count.
     * 
     * @return The number of vertices (indices) to render
     */
    public int getVertexCount() {
        return vertexCount;
    }
    
    /**
     * Renders the mesh.
     * 
     * <p>This method binds the VAO and issues a draw call using
     * indexed rendering.
     */
    public void render() {
        // Bind VAO
        glBindVertexArray(vaoId);
        
        // Draw the mesh
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        
        // Unbind VAO
        glBindVertexArray(0);
    }
    
    /**
     * Cleans up mesh resources.
     * 
     * <p>This method deletes all OpenGL buffers and the VAO.
     * Should be called when the mesh is no longer needed.
     */
    public void cleanup() {
        // Unbind everything first
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        
        // Delete VBOs
        if (posVboId != 0) {
            glDeleteBuffers(posVboId);
        }
        if (texVboId != 0) {
            glDeleteBuffers(texVboId);
        }
        if (normVboId != 0) {
            glDeleteBuffers(normVboId);
        }
        if (eboId != 0) {
            glDeleteBuffers(eboId);
        }
        
        // Delete VAO
        if (vaoId != 0) {
            glDeleteVertexArrays(vaoId);
        }
    }
}

