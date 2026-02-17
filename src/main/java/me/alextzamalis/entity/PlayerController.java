package me.alextzamalis.entity;

import me.alextzamalis.core.Window;
import me.alextzamalis.graphics.Camera;
import me.alextzamalis.input.InputManager;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Controls player movement and camera in the game world.
 * 
 * <p>This class handles all player input processing including movement (WASD),
 * vertical movement (Space/Shift), and camera rotation via mouse. It is designed
 * to be reusable across different game modes and world types.
 * 
 * <p>The controller manages:
 * <ul>
 *   <li>First-person camera movement</li>
 *   <li>Mouse look controls</li>
 *   <li>Cursor grab/release</li>
 *   <li>Movement speed and sensitivity settings</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class PlayerController {
    
    /** Default movement speed in units per second. */
    public static final float DEFAULT_MOVEMENT_SPEED = 5.0f;
    
    /** Default mouse sensitivity. */
    public static final float DEFAULT_MOUSE_SENSITIVITY = 0.1f;
    
    /** Sprint speed multiplier. */
    public static final float SPRINT_MULTIPLIER = 1.5f;
    
    /** The camera controlled by this player. */
    private final Camera camera;
    
    /** Movement speed in units per second. */
    private float movementSpeed;
    
    /** Mouse sensitivity for camera rotation. */
    private float mouseSensitivity;
    
    /** Whether the player is currently sprinting. */
    private boolean sprinting;
    
    /** Flag to track if left mouse was pressed last frame. */
    private boolean leftMouseWasPressed;
    
    /** Flag to track if Escape was pressed last frame. */
    private boolean escapeWasPressed;
    
    /**
     * Creates a new player controller with default settings.
     */
    public PlayerController() {
        this.camera = new Camera();
        this.movementSpeed = DEFAULT_MOVEMENT_SPEED;
        this.mouseSensitivity = DEFAULT_MOUSE_SENSITIVITY;
        this.sprinting = false;
        this.leftMouseWasPressed = false;
        this.escapeWasPressed = false;
    }
    
    /**
     * Creates a new player controller with an existing camera.
     * 
     * @param camera The camera to control
     */
    public PlayerController(Camera camera) {
        this.camera = camera;
        this.movementSpeed = DEFAULT_MOVEMENT_SPEED;
        this.mouseSensitivity = DEFAULT_MOUSE_SENSITIVITY;
        this.sprinting = false;
        this.leftMouseWasPressed = false;
        this.escapeWasPressed = false;
    }
    
    /**
     * Initializes the player controller.
     * 
     * @param window The game window
     * @param startPosition The starting position
     */
    public void init(Window window, Vector3f startPosition) {
        camera.setPosition(startPosition);
        camera.updateProjection(70.0f, window.getAspectRatio(), 0.01f, 1000.0f);
    }
    
    /**
     * Processes input for cursor grab/release and escape handling.
     * 
     * @param window The game window
     * @param inputManager The input manager
     * @return true if the game should close
     */
    public boolean processInput(Window window, InputManager inputManager) {
        // Toggle cursor grab with left mouse click
        boolean leftMousePressed = inputManager.isMouseButtonPressed(GLFW_MOUSE_BUTTON_LEFT);
        if (leftMousePressed && !leftMouseWasPressed && !inputManager.isCursorGrabbed()) {
            inputManager.setCursorGrabbed(true);
            inputManager.centerCursor(window);
        }
        leftMouseWasPressed = leftMousePressed;
        
        // Escape key handling: release cursor first, then signal close
        boolean escapePressed = window.isKeyPressed(GLFW_KEY_ESCAPE);
        if (escapePressed && !escapeWasPressed) {
            if (inputManager.isCursorGrabbed()) {
                inputManager.setCursorGrabbed(false);
            } else {
                return true; // Signal to close
            }
        }
        escapeWasPressed = escapePressed;
        
        return false;
    }
    
    /**
     * Updates player movement and camera rotation.
     * 
     * @param deltaTime Time since last update in seconds
     * @param inputManager The input manager
     */
    public void update(float deltaTime, InputManager inputManager) {
        // Check sprint
        sprinting = inputManager.isKeyPressed(GLFW_KEY_LEFT_CONTROL);
        float speed = movementSpeed * deltaTime * (sprinting ? SPRINT_MULTIPLIER : 1.0f);
        
        // Movement
        if (inputManager.isKeyPressed(GLFW_KEY_W)) {
            camera.move(0, 0, -speed);
        }
        if (inputManager.isKeyPressed(GLFW_KEY_S)) {
            camera.move(0, 0, speed);
        }
        if (inputManager.isKeyPressed(GLFW_KEY_A)) {
            camera.move(-speed, 0, 0);
        }
        if (inputManager.isKeyPressed(GLFW_KEY_D)) {
            camera.move(speed, 0, 0);
        }
        if (inputManager.isKeyPressed(GLFW_KEY_SPACE)) {
            camera.move(0, speed, 0);
        }
        if (inputManager.isKeyPressed(GLFW_KEY_LEFT_SHIFT)) {
            camera.move(0, -speed, 0);
        }
        
        // Camera rotation with mouse (when cursor is grabbed)
        if (inputManager.isCursorGrabbed()) {
            float rotX = (float) inputManager.getDeltaY() * mouseSensitivity;
            float rotY = (float) inputManager.getDeltaX() * mouseSensitivity;
            camera.rotate(rotX, rotY, 0);
        }
    }
    
    /**
     * Updates the camera projection matrix (call on window resize).
     * 
     * @param window The game window
     */
    public void updateProjection(Window window) {
        camera.updateProjection(70.0f, window.getAspectRatio(), 0.01f, 1000.0f);
    }
    
    /**
     * Gets the camera.
     * 
     * @return The camera
     */
    public Camera getCamera() {
        return camera;
    }
    
    /**
     * Gets the player position.
     * 
     * @return The position
     */
    public Vector3f getPosition() {
        return camera.getPosition();
    }
    
    /**
     * Sets the player position.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    public void setPosition(float x, float y, float z) {
        camera.setPosition(x, y, z);
    }
    
    /**
     * Gets the movement speed.
     * 
     * @return The movement speed
     */
    public float getMovementSpeed() {
        return movementSpeed;
    }
    
    /**
     * Sets the movement speed.
     * 
     * @param movementSpeed The new movement speed
     */
    public void setMovementSpeed(float movementSpeed) {
        this.movementSpeed = movementSpeed;
    }
    
    /**
     * Gets the mouse sensitivity.
     * 
     * @return The mouse sensitivity
     */
    public float getMouseSensitivity() {
        return mouseSensitivity;
    }
    
    /**
     * Sets the mouse sensitivity.
     * 
     * @param mouseSensitivity The new mouse sensitivity
     */
    public void setMouseSensitivity(float mouseSensitivity) {
        this.mouseSensitivity = mouseSensitivity;
    }
    
    /**
     * Checks if the player is sprinting.
     * 
     * @return true if sprinting
     */
    public boolean isSprinting() {
        return sprinting;
    }
}

