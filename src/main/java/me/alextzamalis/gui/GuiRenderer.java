package me.alextzamalis.gui;

import me.alextzamalis.graphics.ShaderProgram;
import me.alextzamalis.graphics.Texture;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renders 2D GUI elements using orthographic projection.
 * 
 * <p>This renderer handles all 2D UI rendering including:
 * <ul>
 *   <li>Textured quads (buttons, panels, icons)</li>
 *   <li>Colored rectangles</li>
 *   <li>Text rendering (future)</li>
 * </ul>
 * 
 * <p>Coordinates are in screen space where (0,0) is top-left
 * and positive Y goes downward (standard UI convention).
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class GuiRenderer {
    
    /** VAO for quad rendering. */
    private int quadVao;
    
    /** VBO for quad vertices. */
    private int quadVbo;
    
    /** Shader for GUI rendering. */
    private ShaderProgram shader;
    
    /** Orthographic projection matrix. */
    private final Matrix4f projectionMatrix;
    
    /** Model matrix for positioning. */
    private final Matrix4f modelMatrix;
    
    /** Current screen width. */
    private int screenWidth;
    
    /** Current screen height. */
    private int screenHeight;
    
    /** Font renderer for text. */
    private FontRenderer fontRenderer;
    
    /**
     * Creates a new GUI renderer.
     */
    public GuiRenderer() {
        this.projectionMatrix = new Matrix4f();
        this.modelMatrix = new Matrix4f();
    }
    
    /**
     * Initializes the GUI renderer.
     * 
     * @param screenWidth Initial screen width
     * @param screenHeight Initial screen height
     * @throws Exception if initialization fails
     */
    public void init(int screenWidth, int screenHeight) throws Exception {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        
        // Create quad VAO/VBO
        createQuad();
        
        // Create shader
        createShader();
        
        // Update projection
        updateProjection();
        
        // Create font renderer
        fontRenderer = new FontRenderer();
        fontRenderer.init(this);
    }
    
    /**
     * Creates the quad mesh for rendering.
     */
    private void createQuad() {
        // Quad vertices: position (x, y) and texCoord (u, v)
        // Unit quad from (0,0) to (1,1)
        float[] vertices = {
            // Position    // TexCoord
            0.0f, 0.0f,    0.0f, 0.0f,  // Top-left
            1.0f, 0.0f,    1.0f, 0.0f,  // Top-right
            1.0f, 1.0f,    1.0f, 1.0f,  // Bottom-right
            
            0.0f, 0.0f,    0.0f, 0.0f,  // Top-left
            1.0f, 1.0f,    1.0f, 1.0f,  // Bottom-right
            0.0f, 1.0f,    0.0f, 1.0f   // Bottom-left
        };
        
        quadVao = glGenVertexArrays();
        glBindVertexArray(quadVao);
        
        quadVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        
        // Position attribute
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        // TexCoord attribute
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }
    
    /**
     * Creates the GUI shader.
     */
    private void createShader() throws Exception {
        String vertexShader = 
            "#version 330 core\n" +
            "layout (location = 0) in vec2 aPosition;\n" +
            "layout (location = 1) in vec2 aTexCoord;\n" +
            "\n" +
            "out vec2 texCoord;\n" +
            "\n" +
            "uniform mat4 projection;\n" +
            "uniform mat4 model;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_Position = projection * model * vec4(aPosition, 0.0, 1.0);\n" +
            "    texCoord = aTexCoord;\n" +
            "}\n";
        
        String fragmentShader = 
            "#version 330 core\n" +
            "in vec2 texCoord;\n" +
            "out vec4 fragColor;\n" +
            "\n" +
            "uniform sampler2D guiTexture;\n" +
            "uniform vec4 color;\n" +
            "uniform int useTexture;\n" +
            "\n" +
            "void main() {\n" +
            "    if (useTexture == 1) {\n" +
            "        vec4 texColor = texture(guiTexture, texCoord);\n" +
            "        fragColor = texColor * color;\n" +
            "    } else {\n" +
            "        fragColor = color;\n" +
            "    }\n" +
            "}\n";
        
        shader = new ShaderProgram();
        shader.createVertexShader(vertexShader);
        shader.createFragmentShader(fragmentShader);
        shader.link();
        
        shader.createUniform("projection");
        shader.createUniform("model");
        shader.createUniform("guiTexture");
        shader.createUniform("color");
        shader.createUniform("useTexture");
    }
    
    /**
     * Updates the orthographic projection matrix.
     */
    private void updateProjection() {
        // Orthographic projection: (0,0) at top-left, (width, height) at bottom-right
        projectionMatrix.identity().ortho(0, screenWidth, screenHeight, 0, -1, 1);
    }
    
    /**
     * Called when screen is resized.
     * 
     * @param width New width
     * @param height New height
     */
    public void resize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        updateProjection();
    }
    
    /**
     * Begins GUI rendering. Call before drawing any GUI elements.
     */
    public void begin() {
        // Disable depth testing for 2D
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        
        // Enable blending for transparency
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        shader.bind();
        shader.setUniform("projection", projectionMatrix);
        shader.setUniform("guiTexture", 0);
    }
    
    /**
     * Ends GUI rendering. Call after drawing all GUI elements.
     */
    public void end() {
        shader.unbind();
        
        // Restore depth testing and culling
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glDisable(GL_BLEND);
    }
    
    /**
     * Draws a textured quad.
     * 
     * @param texture The texture to draw
     * @param x X position (screen coordinates)
     * @param y Y position (screen coordinates)
     * @param width Width in pixels
     * @param height Height in pixels
     */
    public void drawTexture(Texture texture, float x, float y, float width, float height) {
        drawTexture(texture, x, y, width, height, 1, 1, 1, 1);
    }
    
    /**
     * Draws a textured quad with color tint.
     * 
     * @param texture The texture to draw
     * @param x X position
     * @param y Y position
     * @param width Width
     * @param height Height
     * @param r Red tint (0-1)
     * @param g Green tint (0-1)
     * @param b Blue tint (0-1)
     * @param a Alpha (0-1)
     */
    public void drawTexture(Texture texture, float x, float y, float width, float height,
                           float r, float g, float b, float a) {
        modelMatrix.identity()
            .translate(x, y, 0)
            .scale(width, height, 1);
        
        shader.setUniform("model", modelMatrix);
        shader.setUniform("color", r, g, b, a);
        shader.setUniform("useTexture", 1);
        
        texture.bind(0);
        
        glBindVertexArray(quadVao);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
        
        texture.unbind();
    }
    
    /**
     * Draws a colored rectangle (no texture).
     * 
     * @param x X position
     * @param y Y position
     * @param width Width
     * @param height Height
     * @param r Red (0-1)
     * @param g Green (0-1)
     * @param b Blue (0-1)
     * @param a Alpha (0-1)
     */
    public void drawRect(float x, float y, float width, float height,
                        float r, float g, float b, float a) {
        modelMatrix.identity()
            .translate(x, y, 0)
            .scale(width, height, 1);
        
        shader.setUniform("model", modelMatrix);
        shader.setUniform("color", r, g, b, a);
        shader.setUniform("useTexture", 0);
        
        glBindVertexArray(quadVao);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
    }
    
    /**
     * Gets the current screen width.
     */
    public int getScreenWidth() {
        return screenWidth;
    }
    
    /**
     * Gets the current screen height.
     */
    public int getScreenHeight() {
        return screenHeight;
    }
    
    /**
     * Draws text at the specified position.
     * 
     * @param text The text to draw
     * @param x X position
     * @param y Y position
     * @param r Red (0-1)
     * @param g Green (0-1)
     * @param b Blue (0-1)
     */
    public void drawText(String text, float x, float y, float r, float g, float b) {
        if (fontRenderer != null) {
            fontRenderer.drawStringWithShadow(text, x, y, r, g, b);
        }
    }
    
    /**
     * Draws centered text.
     * 
     * @param text The text to draw
     * @param centerX Center X position
     * @param y Y position
     * @param r Red (0-1)
     * @param g Green (0-1)
     * @param b Blue (0-1)
     */
    public void drawTextCentered(String text, float centerX, float y, float r, float g, float b) {
        if (fontRenderer != null) {
            fontRenderer.drawStringCenteredWithShadow(text, centerX, y, r, g, b);
        }
    }
    
    /**
     * Gets the width of a text string.
     * 
     * @param text The text
     * @return Width in pixels
     */
    public float getTextWidth(String text) {
        return fontRenderer != null ? fontRenderer.getStringWidth(text) : 0;
    }
    
    /**
     * Gets the height of text.
     * 
     * @return Height in pixels
     */
    public float getTextHeight() {
        return fontRenderer != null ? fontRenderer.getStringHeight() : 16;
    }
    
    /**
     * Sets the font scale.
     * 
     * @param scale Scale factor
     */
    public void setFontScale(float scale) {
        if (fontRenderer != null) {
            fontRenderer.setScale(scale);
        }
    }
    
    /**
     * Cleans up resources.
     */
    public void cleanup() {
        // Unbind everything first
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        // Delete VBO
        if (quadVbo != 0) {
            glDeleteBuffers(quadVbo);
            quadVbo = 0;
        }
        
        // Delete VAO
        if (quadVao != 0) {
            glDeleteVertexArrays(quadVao);
            quadVao = 0;
        }
        
        // Cleanup font renderer
        if (fontRenderer != null) {
            fontRenderer.cleanup();
            fontRenderer = null;
        }
        
        // Cleanup shader last
        if (shader != null) {
            shader.cleanup();
            shader = null;
        }
    }
}

