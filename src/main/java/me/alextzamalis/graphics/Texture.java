package me.alextzamalis.graphics;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_NEAREST_MIPMAP_NEAREST;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.stbi_failure_reason;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;
import static org.lwjgl.stb.STBImage.stbi_set_flip_vertically_on_load;
import org.lwjgl.system.MemoryStack;

import me.alextzamalis.util.Logger;

/**
 * Represents an OpenGL texture loaded from an image file.
 * 
 * <p>This class handles loading textures from various image formats (PNG, JPG, etc.)
 * using the STB image library. It supports mipmapping and various texture filtering
 * options for optimal rendering quality.
 * 
 * <p>Textures are loaded from the classpath, typically from the resources folder.
 * The path should be relative to the resources folder (e.g., "/assets/textures/block.png").
 * 
 * <p>Usage example:
 * <pre>{@code
 * Texture texture = new Texture("/assets/textures/dirt.png");
 * texture.bind();
 * // render textured geometry
 * texture.unbind();
 * texture.cleanup();
 * }</pre>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class Texture {
    
    /** The OpenGL texture ID. */
    private final int textureId;
    
    /** The texture width in pixels. */
    private final int width;
    
    /** The texture height in pixels. */
    private final int height;
    
    /** The resource path this texture was loaded from. */
    private final String resourcePath;
    
    /**
     * Creates a new texture from an image file.
     * 
     * @param resourcePath The path to the image file (e.g., "/assets/textures/dirt.png")
     * @throws RuntimeException if the texture cannot be loaded
     */
    public Texture(String resourcePath) {
        this.resourcePath = resourcePath;
        
        // Load image data
        ByteBuffer imageBuffer;
        int imgWidth, imgHeight;
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            
            // Load image from classpath
            ByteBuffer rawData = loadResourceToBuffer(resourcePath);
            
            // Decode image
            stbi_set_flip_vertically_on_load(true);
            imageBuffer = stbi_load_from_memory(rawData, w, h, channels, 4);
            
            if (imageBuffer == null) {
                throw new RuntimeException("Failed to load texture: " + resourcePath + 
                    " - " + stbi_failure_reason());
            }
            
            imgWidth = w.get(0);
            imgHeight = h.get(0);
        }
        
        this.width = imgWidth;
        this.height = imgHeight;
        
        // Create OpenGL texture
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        
        // Set texture parameters for pixel art (no blurring)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        
        // Upload texture data
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageBuffer);
        
        // Generate mipmaps
        glGenerateMipmap(GL_TEXTURE_2D);
        
        // Free image data
        stbi_image_free(imageBuffer);
        
        // Unbind texture
        glBindTexture(GL_TEXTURE_2D, 0);
        
        Logger.debug("Loaded texture: %s (%dx%d)", resourcePath, width, height);
    }
    
    /**
     * Creates a texture from raw pixel data.
     * 
     * @param width The texture width
     * @param height The texture height
     * @param data The pixel data (RGBA format)
     */
    public Texture(int width, int height, ByteBuffer data) {
        this.resourcePath = "generated";
        this.width = width;
        this.height = height;
        
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
        glGenerateMipmap(GL_TEXTURE_2D);
        
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    
    /**
     * Loads a resource file into a ByteBuffer.
     * 
     * @param resourcePath The path to the resource
     * @return A ByteBuffer containing the file data
     * @throws RuntimeException if the resource cannot be loaded
     */
    private ByteBuffer loadResourceToBuffer(String resourcePath) {
        try (InputStream inputStream = Texture.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new RuntimeException("Resource not found: " + resourcePath);
            }
            
            byte[] bytes = inputStream.readAllBytes();
            ByteBuffer buffer = org.lwjgl.BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource: " + resourcePath, e);
        }
    }
    
    /**
     * Binds this texture for rendering.
     */
    public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureId);
    }
    
    /**
     * Binds this texture to a specific texture unit.
     * 
     * @param unit The texture unit (0-31)
     */
    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }
    
    /**
     * Unbinds the current texture.
     */
    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    
    /**
     * Cleans up texture resources.
     */
    public void cleanup() {
        glDeleteTextures(textureId);
    }
    
    /**
     * Gets the texture ID.
     * 
     * @return The OpenGL texture ID
     */
    public int getTextureId() {
        return textureId;
    }
    
    /**
     * Gets the texture width.
     * 
     * @return The width in pixels
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Gets the texture height.
     * 
     * @return The height in pixels
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Gets the resource path.
     * 
     * @return The path this texture was loaded from
     */
    public String getResourcePath() {
        return resourcePath;
    }
}

