package me.alextzamalis.graphics;

import me.alextzamalis.physics.BlockRaycast;
import me.alextzamalis.voxel.World;
import org.joml.Matrix4f;
import org.joml.Vector3i;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renders a wireframe highlight around the block the player is looking at.
 * 
 * <p>This class creates and manages a wireframe cube mesh that is rendered
 * around the currently targeted block. The highlight helps players see
 * exactly which block they will interact with.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class BlockHighlight {
    
    /** Slightly larger than 1.0 to prevent z-fighting. */
    private static final float SIZE = 1.002f;
    
    /** Offset to center the highlight. */
    private static final float OFFSET = -0.001f;
    
    /** Line width for the wireframe. */
    private static final float LINE_WIDTH = 2.0f;
    
    /** VAO for the wireframe cube. */
    private int vaoId;
    
    /** VBO for vertex positions. */
    private int vboId;
    
    /** Number of vertices. */
    private int vertexCount;
    
    /** Shader for rendering the highlight. */
    private ShaderProgram shader;
    
    /** Model matrix for positioning. */
    private final Matrix4f modelMatrix;
    
    /** Currently targeted block position, or null if none. */
    private Vector3i targetBlock;
    
    /**
     * Creates a new block highlight renderer.
     */
    public BlockHighlight() {
        this.modelMatrix = new Matrix4f();
        this.targetBlock = null;
    }
    
    /**
     * Initializes the highlight renderer.
     * 
     * @throws Exception if shader loading fails
     */
    public void init() throws Exception {
        // Create wireframe cube vertices (lines)
        float[] vertices = createWireframeCube();
        vertexCount = vertices.length / 3;
        
        // Create VAO
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        
        // Create VBO
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        
        // Position attribute
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);
        
        // Unbind
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        
        // Create simple shader for the highlight
        createShader();
    }
    
    /**
     * Creates the wireframe cube vertices (12 edges = 24 vertices).
     */
    private float[] createWireframeCube() {
        float min = OFFSET;
        float max = SIZE + OFFSET;
        
        return new float[] {
            // Bottom face edges
            min, min, min,  max, min, min,  // Edge 1
            max, min, min,  max, min, max,  // Edge 2
            max, min, max,  min, min, max,  // Edge 3
            min, min, max,  min, min, min,  // Edge 4
            
            // Top face edges
            min, max, min,  max, max, min,  // Edge 5
            max, max, min,  max, max, max,  // Edge 6
            max, max, max,  min, max, max,  // Edge 7
            min, max, max,  min, max, min,  // Edge 8
            
            // Vertical edges
            min, min, min,  min, max, min,  // Edge 9
            max, min, min,  max, max, min,  // Edge 10
            max, min, max,  max, max, max,  // Edge 11
            min, min, max,  min, max, max   // Edge 12
        };
    }
    
    /**
     * Creates the highlight shader.
     */
    private void createShader() throws Exception {
        String vertexShader = """
            #version 330 core
            layout (location = 0) in vec3 aPosition;
            uniform mat4 projectionMatrix;
            uniform mat4 viewMatrix;
            uniform mat4 modelMatrix;
            void main() {
                gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(aPosition, 1.0);
            }
            """;
        
        String fragmentShader = """
            #version 330 core
            out vec4 fragColor;
            uniform vec3 highlightColor;
            void main() {
                fragColor = vec4(highlightColor, 1.0);
            }
            """;
        
        shader = new ShaderProgram();
        shader.createVertexShader(vertexShader);
        shader.createFragmentShader(fragmentShader);
        shader.link();
        
        shader.createUniform("projectionMatrix");
        shader.createUniform("viewMatrix");
        shader.createUniform("modelMatrix");
        shader.createUniform("highlightColor");
    }
    
    /** Maximum raycast distance. */
    private static final float MAX_REACH = 8.0f;
    
    /**
     * Updates the target block based on raycasting.
     * 
     * @param camera The player camera
     * @param world The world
     */
    public void update(Camera camera, World world) {
        BlockRaycast.RaycastResult result = BlockRaycast.cast(
            world, 
            camera.getPosition(), 
            camera.getForward(), 
            MAX_REACH
        );
        
        if (result.hit && result.blockPos != null) {
            targetBlock = result.blockPos;
        } else {
            targetBlock = null;
        }
    }
    
    /**
     * Renders the block highlight if a block is targeted.
     * 
     * @param camera The player camera
     */
    public void render(Camera camera) {
        if (targetBlock == null) {
            return;
        }
        
        // Save OpenGL state
        boolean depthTestEnabled = glIsEnabled(GL_DEPTH_TEST);
        float[] previousLineWidth = new float[1];
        glGetFloatv(GL_LINE_WIDTH, previousLineWidth);
        
        // Set up for wireframe rendering
        glLineWidth(LINE_WIDTH);
        glEnable(GL_DEPTH_TEST);
        
        // Position the highlight at the target block
        modelMatrix.identity().translate(targetBlock.x, targetBlock.y, targetBlock.z);
        
        // Render
        shader.bind();
        shader.setUniform("projectionMatrix", camera.getProjectionMatrix());
        shader.setUniform("viewMatrix", camera.getViewMatrix());
        shader.setUniform("modelMatrix", modelMatrix);
        shader.setUniform("highlightColor", 0.0f, 0.0f, 0.0f); // Black outline
        
        glBindVertexArray(vaoId);
        glDrawArrays(GL_LINES, 0, vertexCount);
        glBindVertexArray(0);
        
        shader.unbind();
        
        // Restore OpenGL state
        glLineWidth(previousLineWidth[0]);
        if (!depthTestEnabled) {
            glDisable(GL_DEPTH_TEST);
        }
    }
    
    /**
     * Gets the currently targeted block position.
     * 
     * @return The target block position, or null if none
     */
    public Vector3i getTargetBlock() {
        return targetBlock;
    }
    
    /**
     * Checks if a block is currently targeted.
     * 
     * @return true if a block is targeted
     */
    public boolean hasTarget() {
        return targetBlock != null;
    }
    
    /**
     * Cleans up resources.
     */
    public void cleanup() {
        if (shader != null) {
            shader.cleanup();
        }
        
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        if (vboId != 0) {
            glDeleteBuffers(vboId);
        }
        if (vaoId != 0) {
            glDeleteVertexArrays(vaoId);
        }
    }
}

