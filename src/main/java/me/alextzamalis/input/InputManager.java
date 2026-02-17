package me.alextzamalis.input;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LAST;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LAST;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.glfwSetCursorEnterCallback;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import me.alextzamalis.core.Window;

/**
 * Manages all input from keyboard and mouse.
 * 
 * <p>This class provides a centralized input handling system that tracks
 * the state of keyboard keys and mouse buttons, as well as mouse position
 * and scroll wheel movement.
 * 
 * <p>The input manager uses callbacks to capture input events from GLFW
 * and provides methods to query the current input state.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class InputManager {
    
    /** Array tracking the state of all keyboard keys. */
    private final boolean[] keys;
    
    /** Array tracking the state of all mouse buttons. */
    private final boolean[] mouseButtons;
    
    /** Current mouse X position. */
    private double mouseX;
    
    /** Current mouse Y position. */
    private double mouseY;
    
    /** Previous mouse X position. */
    private double previousMouseX;
    
    /** Previous mouse Y position. */
    private double previousMouseY;
    
    /** Mouse X displacement since last frame. */
    private double deltaX;
    
    /** Mouse Y displacement since last frame. */
    private double deltaY;
    
    /** Scroll wheel offset. */
    private double scrollOffset;
    
    /** Flag indicating if the mouse is inside the window. */
    private boolean inWindow;
    
    /** Flag indicating if the cursor is grabbed (locked to window). */
    private boolean cursorGrabbed;
    
    /** The window handle for input callbacks. */
    private long windowHandle;
    
    /** GLFW key callback. */
    private GLFWKeyCallback keyCallback;
    
    /** GLFW cursor position callback. */
    private GLFWCursorPosCallback cursorPosCallback;
    
    /** GLFW mouse button callback. */
    private GLFWMouseButtonCallback mouseButtonCallback;
    
    /** GLFW scroll callback. */
    private GLFWScrollCallback scrollCallback;
    
    /**
     * Creates a new input manager.
     */
    public InputManager() {
        this.keys = new boolean[GLFW_KEY_LAST + 1];
        this.mouseButtons = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];
        this.mouseX = 0;
        this.mouseY = 0;
        this.previousMouseX = 0;
        this.previousMouseY = 0;
        this.deltaX = 0;
        this.deltaY = 0;
        this.scrollOffset = 0;
        this.inWindow = false;
        this.cursorGrabbed = false;
    }
    
    /**
     * Initializes the input manager with the specified window.
     * 
     * <p>This method sets up all GLFW input callbacks for the window.
     * 
     * @param window The window to capture input from
     */
    public void init(Window window) {
        this.windowHandle = window.getWindowHandle();
        
        // Key callback
        keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key >= 0 && key < keys.length) {
                    keys[key] = (action != GLFW_RELEASE);
                }
            }
        };
        glfwSetKeyCallback(windowHandle, keyCallback);
        
        // Cursor position callback
        cursorPosCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                mouseX = xpos;
                mouseY = ypos;
            }
        };
        glfwSetCursorPosCallback(windowHandle, cursorPosCallback);
        
        // Mouse button callback
        mouseButtonCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (button >= 0 && button < mouseButtons.length) {
                    mouseButtons[button] = (action != GLFW_RELEASE);
                }
            }
        };
        glfwSetMouseButtonCallback(windowHandle, mouseButtonCallback);
        
        // Scroll callback
        scrollCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double xoffset, double yoffset) {
                scrollOffset = yoffset;
            }
        };
        glfwSetScrollCallback(windowHandle, scrollCallback);
        
        // Cursor enter callback
        glfwSetCursorEnterCallback(windowHandle, (window1, entered) -> {
            inWindow = entered;
        });
    }
    
    /**
     * Updates the input state.
     * 
     * <p>This method calculates mouse deltas and should be called
     * once per frame before processing input.
     * 
     * @param window The window (needed for cursor centering when grabbed)
     */
    public void update(Window window) {
        if (cursorGrabbed && window != null) {
            // When cursor is grabbed, calculate delta from previous position first
            // This captures the movement before we reset the cursor
            deltaX = mouseX - previousMouseX;
            deltaY = mouseY - previousMouseY;
            
            // Then reset cursor to center for next frame
            double centerX = window.getWidth() / 2.0;
            double centerY = window.getHeight() / 2.0;
            glfwSetCursorPos(windowHandle, centerX, centerY);
            
            // Update positions for next frame
            // The callback will update mouseX/mouseY to center after this
            previousMouseX = centerX;
            previousMouseY = centerY;
            // Note: mouseX/mouseY will be updated by the callback, but we've already used them for delta
        } else {
            // Normal mode - calculate delta from previous position
            deltaX = mouseX - previousMouseX;
            deltaY = mouseY - previousMouseY;
            previousMouseX = mouseX;
            previousMouseY = mouseY;
        }
    }
    
    /**
     * Cleans up input resources.
     */
    public void cleanup() {
        if (keyCallback != null) {
            keyCallback.free();
        }
        if (cursorPosCallback != null) {
            cursorPosCallback.free();
        }
        if (mouseButtonCallback != null) {
            mouseButtonCallback.free();
        }
        if (scrollCallback != null) {
            scrollCallback.free();
        }
    }
    
    /**
     * Checks if a key is currently pressed.
     * 
     * @param keyCode The GLFW key code
     * @return true if the key is pressed
     */
    public boolean isKeyPressed(int keyCode) {
        if (keyCode >= 0 && keyCode < keys.length) {
            return keys[keyCode];
        }
        return false;
    }
    
    /**
     * Checks if a mouse button is currently pressed.
     * 
     * @param button The GLFW mouse button code
     * @return true if the button is pressed
     */
    public boolean isMouseButtonPressed(int button) {
        if (button >= 0 && button < mouseButtons.length) {
            return mouseButtons[button];
        }
        return false;
    }
    
    /**
     * Gets the current mouse X position.
     * 
     * @return The mouse X position in pixels
     */
    public double getMouseX() {
        return mouseX;
    }
    
    /**
     * Gets the current mouse Y position.
     * 
     * @return The mouse Y position in pixels
     */
    public double getMouseY() {
        return mouseY;
    }
    
    /**
     * Gets the mouse X displacement since last frame.
     * 
     * @return The mouse X delta
     */
    public double getDeltaX() {
        return deltaX;
    }
    
    /**
     * Gets the mouse Y displacement since last frame.
     * 
     * @return The mouse Y delta
     */
    public double getDeltaY() {
        return deltaY;
    }
    
    /**
     * Gets the scroll wheel offset and resets it.
     * 
     * @return The scroll offset
     */
    public double getScrollOffset() {
        double offset = scrollOffset;
        scrollOffset = 0;
        return offset;
    }
    
    /**
     * Checks if the mouse is inside the window.
     * 
     * @return true if the mouse is in the window
     */
    public boolean isInWindow() {
        return inWindow;
    }
    
    /**
     * Checks if the cursor is grabbed (locked to window center).
     * 
     * @return true if the cursor is grabbed
     */
    public boolean isCursorGrabbed() {
        return cursorGrabbed;
    }
    
    /**
     * Sets whether the cursor should be grabbed.
     * 
     * <p>When grabbed, the cursor is hidden and locked to the window,
     * useful for first-person camera controls.
     * 
     * @param grabbed true to grab the cursor
     */
    public void setCursorGrabbed(boolean grabbed) {
        this.cursorGrabbed = grabbed;
        if (grabbed) {
            glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        } else {
            glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }
    }
    
    /**
     * Centers the mouse cursor in the window.
     * 
     * @param window The window to center the cursor in
     */
    public void centerCursor(Window window) {
        glfwSetCursorPos(windowHandle, window.getWidth() / 2.0, window.getHeight() / 2.0);
        previousMouseX = window.getWidth() / 2.0;
        previousMouseY = window.getHeight() / 2.0;
        mouseX = previousMouseX;
        mouseY = previousMouseY;
    }
}

