package me.alextzamalis.entity;

import me.alextzamalis.core.Window;
import me.alextzamalis.graphics.Camera;
import me.alextzamalis.input.InputManager;
import me.alextzamalis.physics.PlayerPhysics;
import me.alextzamalis.voxel.World;
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
 *   <li>Physics (gravity, collision) in survival mode</li>
 *   <li>Game mode switching (creative/survival)</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class PlayerController {
    
    /**
     * Game modes that affect player physics and abilities.
     */
    public enum GameMode {
        /** Survival mode with gravity and collision. */
        SURVIVAL,
        /** Creative mode with flying and no collision. */
        CREATIVE
    }
    
    /** Default movement speed in units per second. */
    public static final float DEFAULT_MOVEMENT_SPEED = 5.0f;
    
    /** Creative mode flying speed. */
    public static final float CREATIVE_FLY_SPEED = 10.0f;
    
    /** Default mouse sensitivity. */
    public static final float DEFAULT_MOUSE_SENSITIVITY = 0.1f;
    
    /** Sprint speed multiplier. */
    public static final float SPRINT_MULTIPLIER = 1.5f;
    
    /** The camera controlled by this player. */
    private final Camera camera;
    
    /** Player physics handler. */
    private final PlayerPhysics physics;
    
    /** Current game mode. */
    private GameMode gameMode;
    
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
    
    /** Flag to track if F3 was pressed last frame (game mode toggle). */
    private boolean f3WasPressed;
    
    /** Flag to track if Space was pressed last frame (for jump). */
    private boolean spaceWasPressed;
    
    /**
     * Creates a new player controller with default settings.
     */
    public PlayerController() {
        this.camera = new Camera();
        this.physics = new PlayerPhysics();
        this.gameMode = GameMode.CREATIVE; // Start in creative mode for easier testing
        this.movementSpeed = DEFAULT_MOVEMENT_SPEED;
        this.mouseSensitivity = DEFAULT_MOUSE_SENSITIVITY;
        this.sprinting = false;
        this.leftMouseWasPressed = false;
        this.escapeWasPressed = false;
        this.f3WasPressed = false;
        this.spaceWasPressed = false;
        
        updatePhysicsForGameMode();
    }
    
    /**
     * Creates a new player controller with an existing camera.
     * 
     * @param camera The camera to control
     */
    public PlayerController(Camera camera) {
        this.camera = camera;
        this.physics = new PlayerPhysics();
        this.gameMode = GameMode.CREATIVE;
        this.movementSpeed = DEFAULT_MOVEMENT_SPEED;
        this.mouseSensitivity = DEFAULT_MOUSE_SENSITIVITY;
        this.sprinting = false;
        this.leftMouseWasPressed = false;
        this.escapeWasPressed = false;
        this.f3WasPressed = false;
        this.spaceWasPressed = false;
        
        updatePhysicsForGameMode();
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
     * Sets the world for physics collision detection.
     * 
     * @param world The world
     */
    public void setWorld(World world) {
        physics.setWorld(world);
    }
    
    /**
     * Processes input for cursor grab/release, escape handling, and game mode toggle.
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
        
        // F3 - Toggle game mode (creative/survival)
        boolean f3Pressed = window.isKeyPressed(GLFW_KEY_F3);
        if (f3Pressed && !f3WasPressed) {
            toggleGameMode();
        }
        f3WasPressed = f3Pressed;
        
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
        float currentSpeed = (gameMode == GameMode.CREATIVE ? CREATIVE_FLY_SPEED : movementSpeed);
        float speed = currentSpeed * deltaTime * (sprinting ? SPRINT_MULTIPLIER : 1.0f);
        
        // Calculate movement direction
        float dx = 0, dz = 0;
        
        if (inputManager.isKeyPressed(GLFW_KEY_W)) {
            Vector3f forward = camera.getForward();
            dx -= forward.x * speed;
            dz -= forward.z * speed;
        }
        if (inputManager.isKeyPressed(GLFW_KEY_S)) {
            Vector3f forward = camera.getForward();
            dx += forward.x * speed;
            dz += forward.z * speed;
        }
        if (inputManager.isKeyPressed(GLFW_KEY_A)) {
            Vector3f right = camera.getRight();
            dx -= right.x * speed;
            dz -= right.z * speed;
        }
        if (inputManager.isKeyPressed(GLFW_KEY_D)) {
            Vector3f right = camera.getRight();
            dx += right.x * speed;
            dz += right.z * speed;
        }
        
        // Apply horizontal movement with collision detection
        if (gameMode == GameMode.SURVIVAL) {
            physics.moveWithCollision(camera.getPosition(), dx, dz);
        } else {
            camera.getPosition().x += dx;
            camera.getPosition().z += dz;
        }
        
        // Vertical movement
        if (gameMode == GameMode.CREATIVE) {
            // Creative mode - free vertical movement
            if (inputManager.isKeyPressed(GLFW_KEY_SPACE)) {
                camera.move(0, speed, 0);
            }
            if (inputManager.isKeyPressed(GLFW_KEY_LEFT_SHIFT)) {
                camera.move(0, -speed, 0);
            }
        } else {
            // Survival mode - jumping only when on ground and key just pressed
            boolean spacePressed = inputManager.isKeyPressed(GLFW_KEY_SPACE);
            if (spacePressed && !spaceWasPressed && physics.isOnGround()) {
                physics.jump();
            }
            spaceWasPressed = spacePressed;
        }
        
        // Apply physics (gravity, collision)
        physics.update(camera.getPosition(), deltaTime);
        
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
    
    /**
     * Gets the current game mode.
     * 
     * @return The game mode
     */
    public GameMode getGameMode() {
        return gameMode;
    }
    
    /**
     * Sets the game mode.
     * 
     * @param mode The new game mode
     */
    public void setGameMode(GameMode mode) {
        this.gameMode = mode;
        updatePhysicsForGameMode();
        me.alextzamalis.util.Logger.info("Game mode changed to: %s", mode);
    }
    
    /**
     * Toggles between creative and survival game modes.
     */
    public void toggleGameMode() {
        setGameMode(gameMode == GameMode.CREATIVE ? GameMode.SURVIVAL : GameMode.CREATIVE);
    }
    
    /**
     * Checks if in creative mode.
     * 
     * @return true if creative mode
     */
    public boolean isCreative() {
        return gameMode == GameMode.CREATIVE;
    }
    
    /**
     * Checks if in survival mode.
     * 
     * @return true if survival mode
     */
    public boolean isSurvival() {
        return gameMode == GameMode.SURVIVAL;
    }
    
    /**
     * Sets to creative mode.
     */
    public void setCreativeMode() {
        setGameMode(GameMode.CREATIVE);
    }
    
    /**
     * Sets to survival mode.
     */
    public void setSurvivalMode() {
        setGameMode(GameMode.SURVIVAL);
    }
    
    /**
     * Updates physics settings based on game mode.
     */
    private void updatePhysicsForGameMode() {
        physics.setEnabled(gameMode == GameMode.SURVIVAL);
        physics.reset();
    }
    
    /**
     * Gets the player physics handler.
     * 
     * @return The physics handler
     */
    public PlayerPhysics getPhysics() {
        return physics;
    }
    
    /**
     * Checks if the player is on the ground.
     * 
     * @return true if on ground
     */
    public boolean isOnGround() {
        return physics.isOnGround();
    }
}

