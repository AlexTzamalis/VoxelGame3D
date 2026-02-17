package me.alextzamalis.physics;

import me.alextzamalis.graphics.Camera;
import me.alextzamalis.input.InputManager;
import me.alextzamalis.util.Logger;
import me.alextzamalis.voxel.BlockRegistry;
import me.alextzamalis.voxel.World;
import org.joml.Vector3f;
import org.joml.Vector3i;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Handles player interaction with blocks (breaking and placing).
 * 
 * <p>This class manages:
 * <ul>
 *   <li>Raycasting to find the block the player is looking at</li>
 *   <li>Block breaking on left click</li>
 *   <li>Block placing on right click</li>
 *   <li>Selected block type for placing</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class BlockInteraction {
    
    /** Maximum reach distance for block interaction. */
    private static final float REACH_DISTANCE = 5.0f;
    
    /** Cooldown between interactions (seconds). */
    private static final float INTERACTION_COOLDOWN = 0.2f;
    
    /** The world to interact with. */
    private final World world;
    
    /** The block registry. */
    private final BlockRegistry blockRegistry;
    
    /** Currently selected block type for placing. */
    private int selectedBlockId;
    
    /** Current raycast result. */
    private BlockRaycast.RaycastResult currentTarget;
    
    /** Time since last interaction. */
    private float interactionCooldown;
    
    /** Previous mouse button states. */
    private boolean leftMouseWasPressed;
    private boolean rightMouseWasPressed;
    
    /**
     * Creates a new block interaction handler.
     * 
     * @param world The world to interact with
     */
    public BlockInteraction(World world) {
        this.world = world;
        this.blockRegistry = BlockRegistry.getInstance();
        this.selectedBlockId = blockRegistry.getBlockId("minecraft:dirt"); // Default to dirt
        this.currentTarget = null;
        this.interactionCooldown = 0;
        this.leftMouseWasPressed = false;
        this.rightMouseWasPressed = false;
    }
    
    /**
     * Updates the block interaction system.
     * 
     * @param camera The player camera
     * @param inputManager The input manager
     * @param deltaTime Time since last frame
     */
    public void update(Camera camera, InputManager inputManager, float deltaTime) {
        // Update cooldown
        if (interactionCooldown > 0) {
            interactionCooldown -= deltaTime;
        }
        
        // Perform raycast to find target block
        Vector3f origin = camera.getPosition();
        Vector3f direction = camera.getForward();
        currentTarget = BlockRaycast.cast(world, origin, direction, REACH_DISTANCE);
        
        // Only process clicks if cursor is grabbed (in game mode)
        if (!inputManager.isCursorGrabbed()) {
            return;
        }
        
        // Handle left click (break block)
        boolean leftMousePressed = inputManager.isMouseButtonPressed(GLFW_MOUSE_BUTTON_LEFT);
        if (leftMousePressed && !leftMouseWasPressed && interactionCooldown <= 0) {
            if (currentTarget.hit) {
                breakBlock(currentTarget.blockPos);
                interactionCooldown = INTERACTION_COOLDOWN;
            }
        }
        leftMouseWasPressed = leftMousePressed;
        
        // Handle right click (place block)
        boolean rightMousePressed = inputManager.isMouseButtonPressed(GLFW_MOUSE_BUTTON_RIGHT);
        if (rightMousePressed && !rightMouseWasPressed && interactionCooldown <= 0) {
            if (currentTarget.hit && currentTarget.placePos != null) {
                placeBlock(currentTarget.placePos);
                interactionCooldown = INTERACTION_COOLDOWN;
            }
        }
        rightMouseWasPressed = rightMousePressed;
        
        // Handle number keys for block selection (1-7)
        for (int i = 0; i < 7; i++) {
            if (inputManager.isKeyPressed(GLFW_KEY_1 + i)) {
                int newBlockId = i + 1; // Skip air (ID 0)
                if (blockRegistry.getBlock(newBlockId) != null) {
                    selectedBlockId = newBlockId;
                }
            }
        }
    }
    
    /**
     * Breaks a block at the specified position.
     * 
     * @param pos The block position
     */
    private void breakBlock(Vector3i pos) {
        int oldBlockId = world.getBlock(pos.x, pos.y, pos.z);
        if (oldBlockId != 0) {
            world.setBlock(pos.x, pos.y, pos.z, 0); // Set to air
            Logger.debug("Broke block at (%d, %d, %d)", pos.x, pos.y, pos.z);
        }
    }
    
    /**
     * Places a block at the specified position.
     * 
     * @param pos The position to place at
     */
    private void placeBlock(Vector3i pos) {
        // Check if position is valid (not inside player, etc.)
        int existingBlock = world.getBlock(pos.x, pos.y, pos.z);
        if (existingBlock == 0) {
            world.setBlock(pos.x, pos.y, pos.z, selectedBlockId);
            Logger.debug("Placed block %d at (%d, %d, %d)", selectedBlockId, pos.x, pos.y, pos.z);
        }
    }
    
    /**
     * Gets the current raycast target.
     * 
     * @return The current target, or null if no block is targeted
     */
    public BlockRaycast.RaycastResult getCurrentTarget() {
        return currentTarget;
    }
    
    /**
     * Checks if a block is currently targeted.
     * 
     * @return true if a block is targeted
     */
    public boolean hasTarget() {
        return currentTarget != null && currentTarget.hit;
    }
    
    /**
     * Gets the currently selected block ID for placing.
     * 
     * @return The selected block ID
     */
    public int getSelectedBlockId() {
        return selectedBlockId;
    }
    
    /**
     * Sets the selected block ID for placing.
     * 
     * @param blockId The block ID
     */
    public void setSelectedBlockId(int blockId) {
        this.selectedBlockId = blockId;
    }
    
    /**
     * Gets the reach distance.
     * 
     * @return The maximum reach distance
     */
    public float getReachDistance() {
        return REACH_DISTANCE;
    }
}

