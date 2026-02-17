package me.alextzamalis.graphics;

import me.alextzamalis.util.Logger;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;
import org.lwjgl.system.MemoryStack;

/**
 * A texture atlas that combines multiple textures into a single OpenGL texture.
 * 
 * <p>This class creates a grid-based texture atlas where each cell contains
 * a block texture. The atlas is used for efficient rendering by minimizing
 * texture binds - all block faces can be rendered with a single texture bound.
 * 
 * <p>Each texture in the atlas is assigned an index, and UV coordinates can
 * be calculated to select the correct sub-texture for rendering.
 * 
 * <p>Usage example:
 * <pre>{@code
 * TextureAtlas atlas = new TextureAtlas(16, 16);  // 16x16 pixel textures
 * int dirtIndex = atlas.addTexture("/assets/textures/dirt.png");
 * atlas.build();
 * 
 * // Get UV coordinates for a texture
 * float[] uvs = atlas.getUVs(dirtIndex);
 * }</pre>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class TextureAtlas {
    
    /** The size of each texture tile in pixels. */
    private final int tileSize;
    
    /** The number of tiles per row in the atlas. */
    private int tilesPerRow;
    
    /** The total number of tiles in the atlas. */
    private int totalTiles;
    
    /** The OpenGL texture ID. */
    private int textureId;
    
    /** Map of texture paths to their atlas indices. */
    private final Map<String, Integer> textureIndices;
    
    /** List of texture paths to include in the atlas. */
    private final java.util.List<String> texturePaths;
    
    /** Whether the atlas has been built. */
    private boolean built;
    
    /** The atlas width in pixels. */
    private int atlasWidth;
    
    /** The atlas height in pixels. */
    private int atlasHeight;
    
    /**
     * Creates a new texture atlas.
     * 
     * @param tileSize The size of each texture tile in pixels (e.g., 16 for 16x16 textures)
     */
    public TextureAtlas(int tileSize) {
        this.tileSize = tileSize;
        this.textureIndices = new HashMap<>();
        this.texturePaths = new java.util.ArrayList<>();
        this.built = false;
    }
    
    /**
     * Adds a texture to the atlas.
     * 
     * @param resourcePath The path to the texture resource
     * @return The index of the texture in the atlas
     */
    public int addTexture(String resourcePath) {
        if (built) {
            throw new IllegalStateException("Cannot add textures after atlas is built");
        }
        
        if (textureIndices.containsKey(resourcePath)) {
            return textureIndices.get(resourcePath);
        }
        
        int index = texturePaths.size();
        texturePaths.add(resourcePath);
        textureIndices.put(resourcePath, index);
        return index;
    }
    
    /**
     * Gets the index of a texture in the atlas.
     * 
     * @param resourcePath The texture resource path
     * @return The texture index, or -1 if not found
     */
    public int getTextureIndex(String resourcePath) {
        Integer index = textureIndices.get(resourcePath);
        return index != null ? index : -1;
    }
    
    /**
     * Builds the texture atlas from all added textures.
     * 
     * @throws RuntimeException if the atlas cannot be built
     */
    public void build() {
        if (built) {
            return;
        }
        
        int numTextures = texturePaths.size();
        if (numTextures == 0) {
            Logger.warn("Building empty texture atlas");
            return;
        }
        
        // Calculate atlas dimensions (square, power of 2)
        tilesPerRow = (int) Math.ceil(Math.sqrt(numTextures));
        totalTiles = tilesPerRow * tilesPerRow;
        atlasWidth = tilesPerRow * tileSize;
        atlasHeight = tilesPerRow * tileSize;
        
        Logger.info("Building texture atlas: %d textures, %dx%d tiles, %dx%d pixels",
            numTextures, tilesPerRow, tilesPerRow, atlasWidth, atlasHeight);
        
        // Create the atlas buffer
        ByteBuffer atlasBuffer = BufferUtils.createByteBuffer(atlasWidth * atlasHeight * 4);
        
        // Initialize with transparent pixels
        for (int i = 0; i < atlasWidth * atlasHeight * 4; i++) {
            atlasBuffer.put((byte) 0);
        }
        atlasBuffer.flip();
        
        // Load each texture and copy to atlas
        for (int i = 0; i < texturePaths.size(); i++) {
            String path = texturePaths.get(i);
            copyTextureToAtlas(path, i, atlasBuffer);
        }
        
        // Create OpenGL texture
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        
        // Set texture parameters for pixel art
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        
        // Upload texture data
        atlasBuffer.rewind();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, atlasWidth, atlasHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, atlasBuffer);
        
        // Generate mipmaps
        glGenerateMipmap(GL_TEXTURE_2D);
        
        glBindTexture(GL_TEXTURE_2D, 0);
        
        built = true;
        Logger.info("Texture atlas built successfully (ID: %d)", textureId);
    }
    
    /**
     * Copies a texture to the atlas buffer.
     * 
     * @param resourcePath The texture resource path
     * @param index The atlas index
     * @param atlasBuffer The atlas buffer
     */
    private void copyTextureToAtlas(String resourcePath, int index, ByteBuffer atlasBuffer) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            
            // Load image from classpath
            ByteBuffer rawData = loadResourceToBuffer(resourcePath);
            
            // Decode image (don't flip - we handle orientation manually)
            stbi_set_flip_vertically_on_load(false);
            ByteBuffer imageBuffer = stbi_load_from_memory(rawData, w, h, channels, 4);
            
            if (imageBuffer == null) {
                Logger.error("Failed to load texture for atlas: %s - %s", resourcePath, stbi_failure_reason());
                return;
            }
            
            int imgWidth = w.get(0);
            int imgHeight = h.get(0);
            
            // Calculate position in atlas
            int tileX = index % tilesPerRow;
            int tileY = index / tilesPerRow;
            int startX = tileX * tileSize;
            int startY = tileY * tileSize;
            
            // Copy pixels to atlas (with vertical flip for OpenGL)
            for (int y = 0; y < Math.min(imgHeight, tileSize); y++) {
                for (int x = 0; x < Math.min(imgWidth, tileSize); x++) {
                    // Source pixel (flip Y for OpenGL)
                    int srcY = imgHeight - 1 - y;
                    int srcIndex = (srcY * imgWidth + x) * 4;
                    
                    // Destination pixel in atlas
                    int dstX = startX + x;
                    int dstY = startY + y;
                    int dstIndex = (dstY * atlasWidth + dstX) * 4;
                    
                    // Copy RGBA
                    atlasBuffer.put(dstIndex, imageBuffer.get(srcIndex));
                    atlasBuffer.put(dstIndex + 1, imageBuffer.get(srcIndex + 1));
                    atlasBuffer.put(dstIndex + 2, imageBuffer.get(srcIndex + 2));
                    atlasBuffer.put(dstIndex + 3, imageBuffer.get(srcIndex + 3));
                }
            }
            
            stbi_image_free(imageBuffer);
            Logger.debug("Added texture to atlas: %s at (%d, %d)", resourcePath, tileX, tileY);
            
        } catch (Exception e) {
            Logger.error("Failed to copy texture to atlas: %s", resourcePath);
        }
    }
    
    /**
     * Loads a resource file into a ByteBuffer.
     */
    private ByteBuffer loadResourceToBuffer(String resourcePath) {
        try (InputStream inputStream = TextureAtlas.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new RuntimeException("Resource not found: " + resourcePath);
            }
            
            byte[] bytes = inputStream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource: " + resourcePath, e);
        }
    }
    
    /**
     * Gets the UV coordinates for a texture in the atlas.
     * 
     * <p>Returns an array of 4 UV coordinate pairs (8 floats total):
     * [u0, v0, u1, v1, u2, v2, u3, v3] for the 4 corners of the texture.
     * 
     * @param index The texture index
     * @return The UV coordinates
     */
    public float[] getUVs(int index) {
        if (!built) {
            throw new IllegalStateException("Atlas not built");
        }
        
        int tileX = index % tilesPerRow;
        int tileY = index / tilesPerRow;
        
        float u0 = (float) tileX / tilesPerRow;
        float v0 = (float) tileY / tilesPerRow;
        float u1 = (float) (tileX + 1) / tilesPerRow;
        float v1 = (float) (tileY + 1) / tilesPerRow;
        
        // Return corners: bottom-left, bottom-right, top-right, top-left
        return new float[] {
            u0, v1,  // bottom-left
            u1, v1,  // bottom-right
            u1, v0,  // top-right
            u0, v0   // top-left
        };
    }
    
    /**
     * Gets the UV coordinates for a texture in the atlas.
     * 
     * @param resourcePath The texture resource path
     * @return The UV coordinates, or null if not found
     */
    public float[] getUVs(String resourcePath) {
        Integer index = textureIndices.get(resourcePath);
        if (index == null) {
            return null;
        }
        return getUVs(index);
    }
    
    /**
     * Binds the atlas texture.
     */
    public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureId);
    }
    
    /**
     * Binds the atlas texture to a specific unit.
     * 
     * @param unit The texture unit
     */
    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }
    
    /**
     * Unbinds the texture.
     */
    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    
    /**
     * Cleans up resources.
     */
    public void cleanup() {
        if (textureId != 0) {
            glDeleteTextures(textureId);
        }
    }
    
    /**
     * Gets the tile size.
     * 
     * @return The tile size in pixels
     */
    public int getTileSize() {
        return tileSize;
    }
    
    /**
     * Gets the number of tiles per row.
     * 
     * @return Tiles per row
     */
    public int getTilesPerRow() {
        return tilesPerRow;
    }
    
    /**
     * Gets the texture ID.
     * 
     * @return The OpenGL texture ID
     */
    public int getTextureId() {
        return textureId;
    }
}

