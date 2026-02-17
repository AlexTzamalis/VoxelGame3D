package me.alextzamalis.graphics;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL30.*;

/**
 * Represents a 3D mesh with vertices, texture coordinates, normals, tints, and indices.
 * 
 * <p>This class manages OpenGL Vertex Array Objects (VAO) and Vertex Buffer Objects (VBO)
 * for efficient rendering. It supports positions, texture coordinates, normals, tint colors,
 * and indexed rendering using Element Buffer Objects (EBO).
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
    
    /** The tint color VBO ID. */
    private final int tintVboId;
    
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
        this(positions, texCoords, normals, null, indices);
    }
    
    /**
     * Creates a new mesh with the specified vertex data including tint colors.
     * 
     * @param positions The vertex positions (x, y, z for each vertex)
     * @param texCoords The texture coordinates (u, v for each vertex), can be null
     * @param normals The vertex normals (x, y, z for each vertex), can be null
     * @param tints The tint colors (r, g, b for each vertex), can be null
     * @param indices The vertex indices for indexed rendering
     */
    public Mesh(float[] positions, float[] texCoords, float[] normals, float[] tints, int[] indices) {
        FloatBuffer posBuffer = null;
        FloatBuffer texBuffer = null;
        FloatBuffer normBuffer = null;
        FloatBuffer tintBuffer = null;
        IntBuffer indicesBuffer = null;
        
        try {
            vertexCount = indices.length;
            
            // Create and bind VAO
            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);
            
            // Position VBO (location 0)
            posVboId = glGenBuffers();
            posBuffer = MemoryUtil.memAllocFloat(positions.length);
            posBuffer.put(positions).flip();
            glBindBuffer(GL_ARRAY_BUFFER, posVboId);
            glBufferData(GL_ARRAY_BUFFER, posBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            
            // Texture coordinate VBO (location 1)
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
            
            // Normal VBO (location 2)
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
            
            // Tint color VBO (location 3)
            if (tints != null && tints.length > 0) {
                tintVboId = glGenBuffers();
                tintBuffer = MemoryUtil.memAllocFloat(tints.length);
                tintBuffer.put(tints).flip();
                glBindBuffer(GL_ARRAY_BUFFER, tintVboId);
                glBufferData(GL_ARRAY_BUFFER, tintBuffer, GL_STATIC_DRAW);
                glEnableVertexAttribArray(3);
                glVertexAttribPointer(3, 3, GL_FLOAT, false, 0, 0);
            } else {
                tintVboId = 0;
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
            if (tintBuffer != null) {
                MemoryUtil.memFree(tintBuffer);
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
     */
    public void render() {
        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }
    
    /**
     * Cleans up mesh resources.
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
    }
}
