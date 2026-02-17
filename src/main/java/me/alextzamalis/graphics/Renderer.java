package me.alextzamalis.graphics;

import me.alextzamalis.core.Window;

import static org.lwjgl.opengl.GL11.*;

/**
 * Main rendering system for the game engine.
 * 
 * <p>This class manages the rendering pipeline and provides methods
 * for clearing the screen, setting up render states, and rendering
 * game objects.
 * 
 * <p>The renderer follows a deferred initialization pattern where
 * {@link #init()} must be called before any rendering operations.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class Renderer {
    
    /** Flag indicating if wireframe mode is enabled. */
    private boolean wireframe;
    
    /**
     * Creates a new renderer.
     */
    public Renderer() {
        this.wireframe = false;
    }
    
    /**
     * Initializes the renderer.
     * 
     * <p>This method sets up initial OpenGL states for rendering.
     */
    public void init() {
        // Enable depth testing
        glEnable(GL_DEPTH_TEST);
        
        // Enable back face culling
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        
        // Set polygon mode
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    }
    
    /**
     * Clears the screen buffers.
     * 
     * <p>Clears both the color buffer and depth buffer.
     */
    public void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }
    
    /**
     * Prepares the renderer for a new frame.
     * 
     * <p>This method should be called at the beginning of each frame
     * before rendering any objects.
     * 
     * @param window The window being rendered to
     */
    public void prepare(Window window) {
        clear();
        
        // Handle window resize
        if (window.isResized()) {
            glViewport(0, 0, window.getWidth(), window.getHeight());
            window.setResized(false);
        }
    }
    
    /**
     * Renders a mesh with the specified shader.
     * 
     * @param mesh The mesh to render
     * @param shader The shader program to use
     */
    public void render(Mesh mesh, ShaderProgram shader) {
        shader.bind();
        mesh.render();
        shader.unbind();
    }
    
    /**
     * Sets the clear color.
     * 
     * @param red Red component (0-1)
     * @param green Green component (0-1)
     * @param blue Blue component (0-1)
     * @param alpha Alpha component (0-1)
     */
    public void setClearColor(float red, float green, float blue, float alpha) {
        glClearColor(red, green, blue, alpha);
    }
    
    /**
     * Toggles wireframe rendering mode.
     */
    public void toggleWireframe() {
        wireframe = !wireframe;
        if (wireframe) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        } else {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }
    }
    
    /**
     * Sets wireframe rendering mode.
     * 
     * @param enabled true to enable wireframe mode
     */
    public void setWireframe(boolean enabled) {
        this.wireframe = enabled;
        if (wireframe) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        } else {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }
    }
    
    /**
     * Checks if wireframe mode is enabled.
     * 
     * @return true if wireframe mode is enabled
     */
    public boolean isWireframe() {
        return wireframe;
    }
    
    /**
     * Enables or disables depth testing.
     * 
     * @param enabled true to enable depth testing
     */
    public void setDepthTest(boolean enabled) {
        if (enabled) {
            glEnable(GL_DEPTH_TEST);
        } else {
            glDisable(GL_DEPTH_TEST);
        }
    }
    
    /**
     * Enables or disables back face culling.
     * 
     * @param enabled true to enable culling
     */
    public void setCulling(boolean enabled) {
        if (enabled) {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);
        } else {
            glDisable(GL_CULL_FACE);
        }
    }
    
    /**
     * Enables or disables blending for transparency.
     * 
     * @param enabled true to enable blending
     */
    public void setBlending(boolean enabled) {
        if (enabled) {
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        } else {
            glDisable(GL_BLEND);
        }
    }
    
    /**
     * Cleans up renderer resources.
     */
    public void cleanup() {
        // Currently no resources to clean up
        // Future: clean up any renderer-specific resources
    }
}

