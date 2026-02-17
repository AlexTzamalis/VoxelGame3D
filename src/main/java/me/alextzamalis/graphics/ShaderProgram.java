package me.alextzamalis.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

/**
 * Manages OpenGL shader programs.
 * 
 * <p>This class handles the loading, compilation, and linking of vertex
 * and fragment shaders. It also provides methods for setting uniform
 * variables in the shader program.
 * 
 * <p>Shader files should be placed in the resources/assets/shaders directory
 * and loaded using the resource path (e.g., "/assets/shaders/vertex.glsl").
 * 
 * <p>Usage example:
 * <pre>{@code
 * ShaderProgram shader = new ShaderProgram();
 * shader.createVertexShader(vertexCode);
 * shader.createFragmentShader(fragmentCode);
 * shader.link();
 * 
 * shader.bind();
 * shader.setUniform("projectionMatrix", projectionMatrix);
 * // render...
 * shader.unbind();
 * }</pre>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class ShaderProgram {
    
    /** The OpenGL program ID. */
    private final int programId;
    
    /** The vertex shader ID. */
    private int vertexShaderId;
    
    /** The fragment shader ID. */
    private int fragmentShaderId;
    
    /** Cache of uniform locations for performance. */
    private final Map<String, Integer> uniforms;
    
    /**
     * Creates a new shader program.
     * 
     * @throws RuntimeException if the program cannot be created
     */
    public ShaderProgram() {
        programId = glCreateProgram();
        if (programId == 0) {
            throw new RuntimeException("Could not create Shader Program");
        }
        uniforms = new HashMap<>();
    }
    
    /**
     * Creates and compiles a vertex shader from source code.
     * 
     * @param shaderCode The GLSL source code for the vertex shader
     * @throws RuntimeException if compilation fails
     */
    public void createVertexShader(String shaderCode) {
        vertexShaderId = createShader(shaderCode, GL_VERTEX_SHADER);
    }
    
    /**
     * Creates and compiles a fragment shader from source code.
     * 
     * @param shaderCode The GLSL source code for the fragment shader
     * @throws RuntimeException if compilation fails
     */
    public void createFragmentShader(String shaderCode) {
        fragmentShaderId = createShader(shaderCode, GL_FRAGMENT_SHADER);
    }
    
    /**
     * Creates and compiles a shader of the specified type.
     * 
     * @param shaderCode The GLSL source code
     * @param shaderType The shader type (GL_VERTEX_SHADER or GL_FRAGMENT_SHADER)
     * @return The shader ID
     * @throws RuntimeException if compilation fails
     */
    private int createShader(String shaderCode, int shaderType) {
        int shaderId = glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new RuntimeException("Error creating shader. Type: " + shaderType);
        }
        
        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);
        
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            String errorLog = glGetShaderInfoLog(shaderId, 1024);
            throw new RuntimeException("Error compiling Shader code: " + errorLog);
        }
        
        glAttachShader(programId, shaderId);
        
        return shaderId;
    }
    
    /**
     * Links the shader program.
     * 
     * <p>This method must be called after creating all shaders and before
     * using the program for rendering.
     * 
     * @throws RuntimeException if linking fails
     */
    public void link() {
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            String errorLog = glGetProgramInfoLog(programId, 1024);
            throw new RuntimeException("Error linking Shader code: " + errorLog);
        }
        
        // Detach shaders after linking (they're no longer needed)
        if (vertexShaderId != 0) {
            glDetachShader(programId, vertexShaderId);
        }
        if (fragmentShaderId != 0) {
            glDetachShader(programId, fragmentShaderId);
        }
        
        // Validate program (optional, useful for debugging)
        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            System.err.println("Warning validating Shader code: " + glGetProgramInfoLog(programId, 1024));
        }
    }
    
    /**
     * Binds this shader program for use.
     */
    public void bind() {
        glUseProgram(programId);
    }
    
    /**
     * Unbinds the current shader program.
     */
    public void unbind() {
        glUseProgram(0);
    }
    
    /**
     * Cleans up shader resources.
     */
    public void cleanup() {
        unbind();
        if (programId != 0) {
            glDeleteProgram(programId);
        }
    }
    
    /**
     * Creates a uniform variable in the shader.
     * 
     * @param uniformName The name of the uniform variable
     * @throws RuntimeException if the uniform cannot be found
     */
    public void createUniform(String uniformName) {
        int uniformLocation = glGetUniformLocation(programId, uniformName);
        if (uniformLocation < 0) {
            throw new RuntimeException("Could not find uniform: " + uniformName);
        }
        uniforms.put(uniformName, uniformLocation);
    }
    
    /**
     * Gets the location of a uniform variable.
     * 
     * @param uniformName The name of the uniform
     * @return The uniform location
     */
    private int getUniformLocation(String uniformName) {
        Integer location = uniforms.get(uniformName);
        if (location == null) {
            location = glGetUniformLocation(programId, uniformName);
            uniforms.put(uniformName, location);
        }
        return location;
    }
    
    /**
     * Sets a 4x4 matrix uniform.
     * 
     * @param uniformName The name of the uniform
     * @param value The matrix value
     */
    public void setUniform(String uniformName, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            value.get(fb);
            glUniformMatrix4fv(getUniformLocation(uniformName), false, fb);
        }
    }
    
    /**
     * Sets an integer uniform.
     * 
     * @param uniformName The name of the uniform
     * @param value The integer value
     */
    public void setUniform(String uniformName, int value) {
        glUniform1i(getUniformLocation(uniformName), value);
    }
    
    /**
     * Sets a float uniform.
     * 
     * @param uniformName The name of the uniform
     * @param value The float value
     */
    public void setUniform(String uniformName, float value) {
        glUniform1f(getUniformLocation(uniformName), value);
    }
    
    /**
     * Sets a 3-component vector uniform.
     * 
     * @param uniformName The name of the uniform
     * @param value The vector value
     */
    public void setUniform(String uniformName, Vector3f value) {
        glUniform3f(getUniformLocation(uniformName), value.x, value.y, value.z);
    }
    
    /**
     * Sets a 3-component vector uniform from individual floats.
     * 
     * @param uniformName The name of the uniform
     * @param x The X component
     * @param y The Y component
     * @param z The Z component
     */
    public void setUniform(String uniformName, float x, float y, float z) {
        glUniform3f(getUniformLocation(uniformName), x, y, z);
    }
    
    /**
     * Sets a 4-component vector uniform.
     * 
     * @param uniformName The name of the uniform
     * @param value The vector value
     */
    public void setUniform(String uniformName, Vector4f value) {
        glUniform4f(getUniformLocation(uniformName), value.x, value.y, value.z, value.w);
    }
    
    /**
     * Sets a 4-component vector uniform from individual floats.
     * 
     * @param uniformName The name of the uniform
     * @param x The X component
     * @param y The Y component
     * @param z The Z component
     * @param w The W component
     */
    public void setUniform(String uniformName, float x, float y, float z, float w) {
        glUniform4f(getUniformLocation(uniformName), x, y, z, w);
    }
    
    /**
     * Sets a boolean uniform (as integer 0 or 1).
     * 
     * @param uniformName The name of the uniform
     * @param value The boolean value
     */
    public void setUniform(String uniformName, boolean value) {
        glUniform1i(getUniformLocation(uniformName), value ? 1 : 0);
    }
    
    /**
     * Gets the program ID.
     * 
     * @return The OpenGL program ID
     */
    public int getProgramId() {
        return programId;
    }
    
    /**
     * Loads shader code from a resource file.
     * 
     * @param resourcePath The path to the shader file (e.g., "/assets/shaders/vertex.glsl")
     * @return The shader source code as a string
     * @throws RuntimeException if the file cannot be read
     */
    public static String loadShaderSource(String resourcePath) {
        StringBuilder shaderSource = new StringBuilder();
        
        try (InputStream inputStream = ShaderProgram.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new RuntimeException("Shader file not found: " + resourcePath);
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    shaderSource.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading shader file: " + resourcePath, e);
        }
        
        return shaderSource.toString();
    }
}

