package me.alextzamalis.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

import org.lwjgl.BufferUtils;

/**
 * Utility class for loading resources from the classpath.
 * 
 * <p>This class provides methods for loading various types of resources
 * including text files (shaders), images, and binary files. Resources
 * are loaded from the classpath, typically from the resources folder.
 * 
 * <p>Resource paths should start with "/" and be relative to the
 * resources folder (e.g., "/assets/shaders/vertex.glsl").
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class ResourceLoader {
    
    /**
     * Private constructor to prevent instantiation.
     */
    private ResourceLoader() {
    }
    
    /**
     * Loads a text file from the classpath.
     * 
     * @param resourcePath The path to the resource (e.g., "/assets/shaders/vertex.glsl")
     * @return The file contents as a string
     * @throws RuntimeException if the file cannot be read
     */
    public static String loadTextFile(String resourcePath) {
        StringBuilder result = new StringBuilder();
        
        try (InputStream inputStream = ResourceLoader.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new RuntimeException("Resource not found: " + resourcePath);
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading resource: " + resourcePath, e);
        }
        
        return result.toString();
    }
    
    /**
     * Loads a binary file from the classpath into a ByteBuffer.
     * 
     * @param resourcePath The path to the resource
     * @param bufferSize The initial buffer size
     * @return A ByteBuffer containing the file data
     * @throws RuntimeException if the file cannot be read
     */
    public static ByteBuffer loadBinaryFile(String resourcePath, int bufferSize) {
        ByteBuffer buffer;
        
        try (InputStream inputStream = ResourceLoader.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new RuntimeException("Resource not found: " + resourcePath);
            }
            
            try (ReadableByteChannel channel = Channels.newChannel(inputStream)) {
                buffer = BufferUtils.createByteBuffer(bufferSize);
                
                while (true) {
                    int bytes = channel.read(buffer);
                    if (bytes == -1) {
                        break;
                    }
                    if (buffer.remaining() == 0) {
                        // Resize buffer
                        ByteBuffer newBuffer = BufferUtils.createByteBuffer(buffer.capacity() * 2);
                        buffer.flip();
                        newBuffer.put(buffer);
                        buffer = newBuffer;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading resource: " + resourcePath, e);
        }
        
        buffer.flip();
        return buffer;
    }
    
    /**
     * Loads a shader file from the assets/shaders directory.
     * 
     * @param shaderName The shader filename (e.g., "vertex.glsl")
     * @return The shader source code
     */
    public static String loadShader(String shaderName) {
        return loadTextFile("/assets/shaders/" + shaderName);
    }
    
    /**
     * Checks if a resource exists.
     * 
     * @param resourcePath The path to the resource
     * @return true if the resource exists
     */
    public static boolean resourceExists(String resourcePath) {
        return ResourceLoader.class.getResource(resourcePath) != null;
    }
    
    /**
     * Gets an input stream for a resource.
     * 
     * @param resourcePath The path to the resource
     * @return The input stream, or null if not found
     */
    public static InputStream getResourceAsStream(String resourcePath) {
        return ResourceLoader.class.getResourceAsStream(resourcePath);
    }
}


