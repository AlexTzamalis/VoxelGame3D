package me.alextzamalis.graphics;

import me.alextzamalis.util.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages texture loading and caching.
 * 
 * <p>This class provides a centralized system for loading and managing textures.
 * Textures are cached to avoid loading the same texture multiple times, improving
 * performance and reducing memory usage.
 * 
 * <p>Usage example:
 * <pre>{@code
 * TextureManager texManager = TextureManager.getInstance();
 * Texture dirt = texManager.getTexture("/assets/textures/VanillaPack/Blocks/dirt.png");
 * dirt.bind();
 * }</pre>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class TextureManager {
    
    /** Singleton instance. */
    private static TextureManager instance;
    
    /** Cache of loaded textures. */
    private final Map<String, Texture> textureCache;
    
    /**
     * Private constructor for singleton pattern.
     */
    private TextureManager() {
        this.textureCache = new HashMap<>();
    }
    
    /**
     * Gets the singleton instance.
     * 
     * @return The TextureManager instance
     */
    public static TextureManager getInstance() {
        if (instance == null) {
            instance = new TextureManager();
        }
        return instance;
    }
    
    /**
     * Gets a texture, loading it if not already cached.
     * 
     * @param resourcePath The path to the texture file
     * @return The loaded texture
     */
    public Texture getTexture(String resourcePath) {
        Texture texture = textureCache.get(resourcePath);
        if (texture == null) {
            texture = new Texture(resourcePath);
            textureCache.put(resourcePath, texture);
            Logger.debug("Cached texture: %s", resourcePath);
        }
        return texture;
    }
    
    /**
     * Checks if a texture is already loaded.
     * 
     * @param resourcePath The path to check
     * @return true if the texture is cached
     */
    public boolean isLoaded(String resourcePath) {
        return textureCache.containsKey(resourcePath);
    }
    
    /**
     * Removes a texture from the cache and cleans it up.
     * 
     * @param resourcePath The path of the texture to remove
     */
    public void removeTexture(String resourcePath) {
        Texture texture = textureCache.remove(resourcePath);
        if (texture != null) {
            texture.cleanup();
            Logger.debug("Removed texture: %s", resourcePath);
        }
    }
    
    /**
     * Cleans up all cached textures.
     */
    public void cleanup() {
        Logger.info("Cleaning up %d textures...", textureCache.size());
        for (Texture texture : textureCache.values()) {
            texture.cleanup();
        }
        textureCache.clear();
    }
    
    /**
     * Gets the number of cached textures.
     * 
     * @return The cache size
     */
    public int getCacheSize() {
        return textureCache.size();
    }
}

