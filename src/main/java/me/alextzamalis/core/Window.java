package me.alextzamalis.core;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Manages the game window and OpenGL context.
 * 
 * <p>This class handles window creation, OpenGL context initialization,
 * and window-related events using GLFW. It provides methods for querying
 * window state and handling resize events.
 * 
 * <p>The window must be initialized before use by calling {@link #init()}.
 * After use, resources should be released by calling {@link #cleanup()}.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class Window {
    
    /** The window title. */
    private final String title;
    
    /** The window width in pixels. */
    private int width;
    
    /** The window height in pixels. */
    private int height;
    
    /** The GLFW window handle. */
    private long windowHandle;
    
    /** Flag indicating if the window was resized. */
    private boolean resized;
    
    /** Flag indicating if VSync is enabled. */
    private boolean vSync;
    
    /**
     * Creates a new window with the specified parameters.
     * 
     * @param title The window title
     * @param width The window width in pixels
     * @param height The window height in pixels
     * @param vSync Whether to enable vertical sync
     */
    public Window(String title, int width, int height, boolean vSync) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.vSync = vSync;
        this.resized = false;
    }
    
    /**
     * Initializes the window and OpenGL context.
     * 
     * <p>This method creates the GLFW window, sets up callbacks,
     * and initializes the OpenGL context. Must be called before
     * any rendering operations.
     * 
     * @throws IllegalStateException if GLFW initialization fails
     * @throws RuntimeException if window creation fails
     */
    public void init() {
        // Setup error callback to print to System.err
        GLFWErrorCallback.createPrint(System.err).set();
        
        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        
        // OpenGL version hints (3.3 core profile)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        
        // Anti-aliasing
        glfwWindowHint(GLFW_SAMPLES, 4);
        
        // Create the window
        windowHandle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (windowHandle == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        // Setup resize callback
        glfwSetFramebufferSizeCallback(windowHandle, (window, w, h) -> {
            this.width = w;
            this.height = h;
            this.resized = true;
        });
        
        // Note: Escape key handling is done in game logic to allow cursor release first
        // The window will close when windowShouldClose is set to true by game logic
        
        // Center the window
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            
            glfwGetWindowSize(windowHandle, pWidth, pHeight);
            
            GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidMode != null) {
                glfwSetWindowPos(
                    windowHandle,
                    (vidMode.width() - pWidth.get(0)) / 2,
                    (vidMode.height() - pHeight.get(0)) / 2
                );
            }
        }
        
        // Make the OpenGL context current
        glfwMakeContextCurrent(windowHandle);
        
        // Enable VSync
        if (vSync) {
            glfwSwapInterval(1);
        }
        
        // Make the window visible
        glfwShowWindow(windowHandle);
        
        // Initialize OpenGL bindings
        GL.createCapabilities();
        
        // Set the clear color (dark blue-gray)
        glClearColor(0.1f, 0.1f, 0.15f, 1.0f);
        
        // Enable depth testing
        glEnable(GL_DEPTH_TEST);
        
        // Enable back face culling
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        
        // Enable multisampling
        glEnable(GL_MULTISAMPLE);
    }
    
    /**
     * Updates the window (swaps buffers and polls events).
     * 
     * <p>This method should be called at the end of each frame
     * to display the rendered content and process window events.
     */
    public void update() {
        glfwSwapBuffers(windowHandle);
        glfwPollEvents();
    }
    
    /**
     * Cleans up window resources.
     * 
     * <p>This method releases all GLFW resources associated with
     * the window. Should be called when the window is no longer needed.
     */
    public void cleanup() {
        // Free error callback first
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
        
        // Free window callbacks
        if (windowHandle != NULL) {
            glfwFreeCallbacks(windowHandle);
            glfwDestroyWindow(windowHandle);
        }
        
        // Terminate GLFW
        glfwTerminate();
    }
    
    /**
     * Checks if the window should close.
     * 
     * @return true if the window should close, false otherwise
     */
    public boolean windowShouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }
    
    /**
     * Sets whether the window should close.
     * 
     * @param shouldClose true to signal the window should close
     */
    public void setWindowShouldClose(boolean shouldClose) {
        glfwSetWindowShouldClose(windowHandle, shouldClose);
    }
    
    /**
     * Gets the window title.
     * 
     * @return The window title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Gets the window width.
     * 
     * @return The window width in pixels
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Gets the window height.
     * 
     * @return The window height in pixels
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Gets the window handle.
     * 
     * @return The GLFW window handle
     */
    public long getWindowHandle() {
        return windowHandle;
    }
    
    /**
     * Checks if the window was resized.
     * 
     * @return true if the window was resized since the last check
     */
    public boolean isResized() {
        return resized;
    }
    
    /**
     * Sets the resized flag.
     * 
     * @param resized The new resized state
     */
    public void setResized(boolean resized) {
        this.resized = resized;
    }
    
    /**
     * Checks if VSync is enabled.
     * 
     * @return true if VSync is enabled
     */
    public boolean isVSync() {
        return vSync;
    }
    
    /**
     * Sets VSync state.
     * 
     * @param vSync true to enable VSync
     */
    public void setVSync(boolean vSync) {
        this.vSync = vSync;
        glfwSwapInterval(vSync ? 1 : 0);
    }
    
    /**
     * Gets the aspect ratio of the window.
     * 
     * @return The aspect ratio (width / height)
     */
    public float getAspectRatio() {
        return (float) width / height;
    }
    
    /**
     * Checks if a key is pressed.
     * 
     * @param keyCode The GLFW key code
     * @return true if the key is pressed
     */
    public boolean isKeyPressed(int keyCode) {
        return glfwGetKey(windowHandle, keyCode) == GLFW_PRESS;
    }
}

