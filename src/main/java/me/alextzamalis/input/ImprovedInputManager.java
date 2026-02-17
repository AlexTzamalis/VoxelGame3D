package me.alextzamalis.input;

import me.alextzamalis.core.Window;
import me.alextzamalis.util.Logger;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Improved input manager with smooth, responsive mouse/keyboard handling.
 * 
 * <p>This is a modern FPS-style input system that:
 * <ul>
 *   <li>Provides smooth, immediate camera rotation</li>
 *   <li>Handles cursor grabbing automatically when entering game</li>
 *   <li>Prevents cursor drift and jitter</li>
 *   <li>Supports raw mouse input for better precision</li>
 * </ul>
 * 
 * <p>Unlike Minecraft's system, this uses continuous cursor locking
 * (like Hytale, CS:GO, etc.) where the cursor is always locked when in-game.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class ImprovedInputManager {
    
    /** Array tracking the state of all keyboard keys. */
    private final boolean[] keys;
    
    /** Array tracking the state of all mouse buttons. */
    private final boolean[] mouseButtons;
    
    /** Previous frame key states (for key press detection). */
    private final boolean[] previousKeys;
    
    /** Previous frame mouse button states. */
    private final boolean[] previousMouseButtons;
    
    /** Current mouse X position. */
    private double mouseX;
    
    /** Current mouse Y position. */
    private double mouseY;
    
    /** Mouse X displacement since last frame (raw, unfiltered). */
    private double deltaX;
    
    /** Mouse Y displacement since last frame (raw, unfiltered). */
    private double deltaY;
    
    /** Scroll wheel offset. */
    private double scrollOffset;
    
    /** Flag indicating if the mouse is inside the window. */
    private boolean inWindow;
    
    /** Flag indicating if the cursor is grabbed (locked to window). */
    private boolean cursorGrabbed;
    
    /** The window handle for input callbacks. */
    private long windowHandle;
    
    /** Window center X (cached for performance). */
    private double centerX;
    
    /** Window center Y (cached for performance). */
    private double centerY;
    
    /** Raw input enabled flag. */
    private boolean rawInputEnabled = false;
    
    /**
     * Creates a new improved input manager.
     */
    public ImprovedInputManager() {
        this.keys = new boolean[GLFW_KEY_LAST + 1];
        this.mouseButtons = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];
        this.previousKeys = new boolean[GLFW_KEY_LAST + 1];
        this.previousMouseButtons = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];
        this.mouseX = 0;
        this.mouseY = 0;
        this.deltaX = 0;
        this.deltaY = 0;
        this.scrollOffset = 0;
        this.inWindow = false;
        this.cursorGrabbed = false;
    }
    
    /**
     * Initializes the input manager with the specified window.
     * 
     * @param window The window to capture input from
     */
    public void init(Window window) {
        this.windowHandle = window.getWindowHandle();
        this.centerX = window.getWidth() / 2.0;
        this.centerY = window.getHeight() / 2.0;
        
        // Try to enable raw mouse input (better precision, no acceleration)
        if (glfwRawMouseMotionSupported()) {
            rawInputEnabled = true;
            Logger.info("Raw mouse input supported and enabled");
        } else {
            Logger.warn("Raw mouse input not supported, using standard input");
        }
        
        // Key callback
        glfwSetKeyCallback(windowHandle, (window1, key, scancode, action, mods) -> {
            if (key >= 0 && key < keys.length) {
                keys[key] = (action != GLFW_RELEASE);
            }
        });
        
        // Cursor position callback - only used when cursor is NOT grabbed
        glfwSetCursorPosCallback(windowHandle, (window1, xpos, ypos) -> {
            if (!cursorGrabbed) {
                // Only update position when cursor is not grabbed
                double oldX = mouseX;
                double oldY = mouseY;
                mouseX = xpos;
                mouseY = ypos;
                
                // Calculate delta for non-grabbed mode (for UI interactions)
                deltaX = mouseX - oldX;
                deltaY = mouseY - oldY;
            }
        });
        
        // Mouse button callback
        glfwSetMouseButtonCallback(windowHandle, (window1, button, action, mods) -> {
            if (button >= 0 && button < mouseButtons.length) {
                mouseButtons[button] = (action != GLFW_RELEASE);
            }
        });
        
        // Scroll callback
        glfwSetScrollCallback(windowHandle, (window1, xoffset, yoffset) -> {
            scrollOffset = yoffset;
        });
        
        // Cursor enter callback
        glfwSetCursorEnterCallback(windowHandle, (window1, entered) -> {
            inWindow = entered;
            if (!entered && cursorGrabbed) {
                // If cursor leaves window while grabbed, release it
                setCursorGrabbed(false);
            }
        });
    }
    
    /**
     * Updates the input state.
     * 
     * <p>This method should be called once per frame before processing input.
     * It calculates mouse deltas and updates previous states.
     * 
     * @param window The window (needed for cursor centering when grabbed)
     */
    public void update(Window window) {
        // Update window center (in case window was resized)
        centerX = window.getWidth() / 2.0;
        centerY = window.getHeight() / 2.0;
        
        if (cursorGrabbed) {
            // When cursor is grabbed, use raw input or calculate from position
            if (rawInputEnabled) {
                // Raw input provides delta directly (no need to calculate)
                // But we still need to get it from GLFW
                double[] x = new double[1];
                double[] y = new double[1];
                glfwGetCursorPos(windowHandle, x, y);
                
                // Calculate delta from center
                deltaX = x[0] - centerX;
                deltaY = y[0] - centerY;
                
                // Reset cursor to center immediately
                glfwSetCursorPos(windowHandle, centerX, centerY);
            } else {
                // Standard input: calculate delta and reset cursor
                double[] x = new double[1];
                double[] y = new double[1];
                glfwGetCursorPos(windowHandle, x, y);
                
                // Calculate delta from center
                deltaX = x[0] - centerX;
                deltaY = y[0] - centerY;
                
                // Reset cursor to center
                glfwSetCursorPos(windowHandle, centerX, centerY);
            }
        } else {
            // Not grabbed: delta already calculated in callback
            // Just ensure it's reset if needed
            if (deltaX != 0 || deltaY != 0) {
                // Keep delta for one frame, will be reset on next update
            }
        }
        
        // Update previous states for key/button press detection
        System.arraycopy(keys, 0, previousKeys, 0, keys.length);
        System.arraycopy(mouseButtons, 0, previousMouseButtons, 0, mouseButtons.length);
    }
    
    /**
     * Cleans up input resources.
     */
    public void cleanup() {
        // GLFW callbacks are automatically cleaned up when window is destroyed
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
     * Checks if a key was just pressed this frame (wasn't pressed last frame).
     * 
     * @param keyCode The GLFW key code
     * @return true if the key was just pressed
     */
    public boolean isKeyJustPressed(int keyCode) {
        if (keyCode >= 0 && keyCode < keys.length) {
            return keys[keyCode] && !previousKeys[keyCode];
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
     * Checks if a mouse button was just pressed this frame.
     * 
     * @param button The GLFW mouse button code
     * @return true if the button was just pressed
     */
    public boolean isMouseButtonJustPressed(int button) {
        if (button >= 0 && button < mouseButtons.length) {
            return mouseButtons[button] && !previousMouseButtons[button];
        }
        return false;
    }
    
    /**
     * Gets the mouse X displacement since last frame.
     * 
     * @return The mouse X delta (in pixels)
     */
    public double getDeltaX() {
        return deltaX;
    }
    
    /**
     * Gets the mouse Y displacement since last frame.
     * 
     * @return The mouse Y delta (in pixels)
     */
    public double getDeltaY() {
        return deltaY;
    }
    
    /**
     * Gets and resets the scroll wheel offset.
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
     * <p>When grabbed, the cursor is hidden and locked to the window center,
     * providing smooth FPS-style camera control.
     * 
     * @param grabbed true to grab the cursor
     */
    public void setCursorGrabbed(boolean grabbed) {
        this.cursorGrabbed = grabbed;
        if (grabbed) {
            // Disable cursor (hide and lock)
            glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            
            // Enable raw input if supported
            if (rawInputEnabled) {
                glfwSetInputMode(windowHandle, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
            }
            
            // Center cursor
            glfwSetCursorPos(windowHandle, centerX, centerY);
            mouseX = centerX;
            mouseY = centerY;
            deltaX = 0;
            deltaY = 0;
        } else {
            // Re-enable cursor
            if (rawInputEnabled) {
                glfwSetInputMode(windowHandle, GLFW_RAW_MOUSE_MOTION, GLFW_FALSE);
            }
            glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }
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
}

