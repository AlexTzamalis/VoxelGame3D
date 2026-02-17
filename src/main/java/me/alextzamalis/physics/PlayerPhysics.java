package me.alextzamalis.physics;

import me.alextzamalis.voxel.Block;
import me.alextzamalis.voxel.BlockRegistry;
import me.alextzamalis.voxel.World;
import org.joml.Vector3f;

/**
 * Handles player physics including gravity, collision detection, and movement.
 * 
 * <p>This class provides physics simulation for the player character including:
 * <ul>
 *   <li>Gravity and falling</li>
 *   <li>Collision detection with solid blocks</li>
 *   <li>Jumping mechanics</li>
 *   <li>Ground detection</li>
 * </ul>
 * 
 * <p>The player is treated as an AABB (Axis-Aligned Bounding Box) for collision
 * purposes. The collision system uses a simple but effective approach of checking
 * block positions that the player's bounding box would intersect.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class PlayerPhysics {
    
    /** Gravity acceleration in blocks per second squared. */
    public static final float GRAVITY = 28.0f;
    
    /** Maximum falling speed (terminal velocity). */
    public static final float MAX_FALL_SPEED = 78.4f;
    
    /** Jump velocity in blocks per second. */
    public static final float JUMP_VELOCITY = 9.0f;
    
    /** Player width (X and Z). */
    public static final float PLAYER_WIDTH = 0.6f;
    
    /** Player height. */
    public static final float PLAYER_HEIGHT = 1.8f;
    
    /** Eye height offset from feet. */
    public static final float EYE_HEIGHT = 1.62f;
    
    /** Small epsilon for collision detection. */
    private static final float EPSILON = 0.001f;
    
    /** The world for collision checks. */
    private World world;
    
    /** Block registry for checking block properties. */
    private final BlockRegistry blockRegistry;
    
    /** Current vertical velocity. */
    private float velocityY;
    
    /** Whether the player is on the ground. */
    private boolean onGround;
    
    /** Whether physics is enabled (false for creative/spectator mode). */
    private boolean enabled;
    
    /**
     * Creates a new player physics handler.
     */
    public PlayerPhysics() {
        this.blockRegistry = BlockRegistry.getInstance();
        this.velocityY = 0;
        this.onGround = false;
        this.enabled = true;
    }
    
    /**
     * Sets the world for collision detection.
     * 
     * @param world The world
     */
    public void setWorld(World world) {
        this.world = world;
    }
    
    /**
     * Enables or disables physics (for creative mode flying).
     * 
     * @param enabled Whether physics should be enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            velocityY = 0;
        }
    }
    
    /**
     * Checks if physics is enabled.
     * 
     * @return true if physics is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Updates physics and applies gravity/collision.
     * 
     * @param position The player's current position (camera/eye position)
     * @param deltaTime Time since last update in seconds
     */
    public void update(Vector3f position, float deltaTime) {
        if (!enabled || world == null) {
            return;
        }
        
        // Convert eye position to feet position for physics
        float feetY = position.y - EYE_HEIGHT;
        
        // Apply gravity
        velocityY -= GRAVITY * deltaTime;
        velocityY = Math.max(velocityY, -MAX_FALL_SPEED);
        
        // Calculate new Y position
        float newFeetY = feetY + velocityY * deltaTime;
        
        // Check vertical collision
        if (velocityY < 0) {
            // Falling - check ground collision
            float groundY = getGroundLevel(position.x, newFeetY, position.z);
            if (newFeetY <= groundY) {
                newFeetY = groundY;
                velocityY = 0;
                onGround = true;
            } else {
                onGround = false;
            }
        } else if (velocityY > 0) {
            // Rising - check ceiling collision
            float ceilingY = getCeilingLevel(position.x, feetY, position.z);
            float headY = newFeetY + PLAYER_HEIGHT;
            if (headY >= ceilingY) {
                newFeetY = ceilingY - PLAYER_HEIGHT;
                velocityY = 0;
            }
            onGround = false;
        }
        
        // Update position (convert back to eye position)
        position.y = newFeetY + EYE_HEIGHT;
    }
    
    /**
     * Attempts to move the player horizontally with collision detection.
     * 
     * @param position Current position (eye position)
     * @param dx Delta X movement
     * @param dz Delta Z movement
     */
    public void moveWithCollision(Vector3f position, float dx, float dz) {
        if (world == null) {
            position.x += dx;
            position.z += dz;
            return;
        }
        
        float feetY = position.y - EYE_HEIGHT;
        
        // Try X movement
        if (dx != 0) {
            float newX = position.x + dx;
            if (!collidesHorizontal(newX, feetY, position.z)) {
                position.x = newX;
            }
        }
        
        // Try Z movement
        if (dz != 0) {
            float newZ = position.z + dz;
            if (!collidesHorizontal(position.x, feetY, newZ)) {
                position.z = newZ;
            }
        }
    }
    
    /**
     * Makes the player jump if on ground.
     */
    public void jump() {
        if (onGround && enabled) {
            velocityY = JUMP_VELOCITY;
            onGround = false;
        }
    }
    
    /**
     * Checks if the player is on the ground.
     * 
     * @return true if on ground
     */
    public boolean isOnGround() {
        return onGround;
    }
    
    /**
     * Gets the current vertical velocity.
     * 
     * @return Vertical velocity in blocks/second
     */
    public float getVelocityY() {
        return velocityY;
    }
    
    /**
     * Sets the vertical velocity (for external effects like knockback).
     * 
     * @param velocityY The new vertical velocity
     */
    public void setVelocityY(float velocityY) {
        this.velocityY = velocityY;
    }
    
    /**
     * Gets the ground level at a position (top of the highest solid block).
     * 
     * @param x X position
     * @param currentFeetY Current feet Y position (for checking blocks below)
     * @param z Z position
     * @return The Y level of the ground
     */
    private float getGroundLevel(float x, float currentFeetY, float z) {
        // Check blocks that the player's bounding box touches
        float halfWidth = PLAYER_WIDTH / 2;
        
        int minBlockX = (int) Math.floor(x - halfWidth);
        int maxBlockX = (int) Math.floor(x + halfWidth);
        int minBlockZ = (int) Math.floor(z - halfWidth);
        int maxBlockZ = (int) Math.floor(z + halfWidth);
        
        float highestGround = 0;
        
        // Check from current position down
        int checkY = (int) Math.floor(currentFeetY);
        
        for (int bx = minBlockX; bx <= maxBlockX; bx++) {
            for (int bz = minBlockZ; bz <= maxBlockZ; bz++) {
                // Check blocks below feet
                for (int by = checkY; by >= 0; by--) {
                    if (isSolidBlock(bx, by, bz)) {
                        float blockTop = by + 1;
                        highestGround = Math.max(highestGround, blockTop);
                        break;
                    }
                }
            }
        }
        
        return highestGround;
    }
    
    /**
     * Gets the ceiling level at a position (bottom of the lowest solid block above).
     * 
     * @param x X position
     * @param currentFeetY Current feet Y position
     * @param z Z position
     * @return The Y level of the ceiling
     */
    private float getCeilingLevel(float x, float currentFeetY, float z) {
        float halfWidth = PLAYER_WIDTH / 2;
        
        int minBlockX = (int) Math.floor(x - halfWidth);
        int maxBlockX = (int) Math.floor(x + halfWidth);
        int minBlockZ = (int) Math.floor(z - halfWidth);
        int maxBlockZ = (int) Math.floor(z + halfWidth);
        
        float lowestCeiling = 256;
        
        int headY = (int) Math.floor(currentFeetY + PLAYER_HEIGHT);
        
        for (int bx = minBlockX; bx <= maxBlockX; bx++) {
            for (int bz = minBlockZ; bz <= maxBlockZ; bz++) {
                // Check blocks above head
                for (int by = headY; by < 256; by++) {
                    if (isSolidBlock(bx, by, bz)) {
                        lowestCeiling = Math.min(lowestCeiling, by);
                        break;
                    }
                }
            }
        }
        
        return lowestCeiling;
    }
    
    /**
     * Checks if the player collides horizontally at the given position.
     * 
     * @param x X position (center)
     * @param feetY Feet Y position
     * @param z Z position (center)
     * @return true if collision would occur
     */
    private boolean collidesHorizontal(float x, float feetY, float z) {
        float halfWidth = PLAYER_WIDTH / 2 - EPSILON;
        
        int minBlockX = (int) Math.floor(x - halfWidth);
        int maxBlockX = (int) Math.floor(x + halfWidth);
        int minBlockY = (int) Math.floor(feetY + EPSILON);
        int maxBlockY = (int) Math.floor(feetY + PLAYER_HEIGHT - EPSILON);
        int minBlockZ = (int) Math.floor(z - halfWidth);
        int maxBlockZ = (int) Math.floor(z + halfWidth);
        
        for (int bx = minBlockX; bx <= maxBlockX; bx++) {
            for (int by = minBlockY; by <= maxBlockY; by++) {
                for (int bz = minBlockZ; bz <= maxBlockZ; bz++) {
                    if (isSolidBlock(bx, by, bz)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a block at the given position is solid.
     * 
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @return true if the block is solid
     */
    private boolean isSolidBlock(int x, int y, int z) {
        if (y < 0 || y >= 256) {
            return false;
        }
        
        int blockId = world.getBlock(x, y, z);
        if (blockId == 0) {
            return false;
        }
        
        Block block = blockRegistry.getBlock(blockId);
        return block != null && block.isSolid();
    }
    
    /**
     * Resets physics state (useful when teleporting).
     */
    public void reset() {
        velocityY = 0;
        onGround = false;
    }
}

