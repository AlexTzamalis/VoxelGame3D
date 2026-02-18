package me.alextzamalis.voxel;

import org.joml.Vector3i;

/**
 * Represents the six faces of a block in a voxel world.
 * 
 * <p>Each face has an associated normal direction and offset vector
 * used for neighbor block lookups and face culling.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public enum BlockFace {
    
    /** The top face (+Y direction). */
    TOP(0, 1, 0),
    
    /** The bottom face (-Y direction). */
    BOTTOM(0, -1, 0),
    
    /** The north face (-Z direction). */
    NORTH(0, 0, -1),
    
    /** The south face (+Z direction). */
    SOUTH(0, 0, 1),
    
    /** The east face (+X direction). */
    EAST(1, 0, 0),
    
    /** The west face (-X direction). */
    WEST(-1, 0, 0);
    
    /** The normal direction of this face. */
    private final Vector3i normal;
    
    /**
     * Creates a block face with the specified normal direction.
     * 
     * @param x The X component of the normal
     * @param y The Y component of the normal
     * @param z The Z component of the normal
     */
    BlockFace(int x, int y, int z) {
        this.normal = new Vector3i(x, y, z);
    }
    
    /**
     * Gets the normal direction of this face.
     * 
     * @return The normal vector
     */
    public Vector3i getNormal() {
        return normal;
    }
    
    /**
     * Gets the X offset for neighbor lookup.
     * 
     * @return The X offset
     */
    public int getOffsetX() {
        return normal.x;
    }
    
    /**
     * Gets the Y offset for neighbor lookup.
     * 
     * @return The Y offset
     */
    public int getOffsetY() {
        return normal.y;
    }
    
    /**
     * Gets the Z offset for neighbor lookup.
     * 
     * @return The Z offset
     */
    public int getOffsetZ() {
        return normal.z;
    }
    
    /**
     * Gets the opposite face.
     * 
     * @return The opposite face
     */
    public BlockFace getOpposite() {
        return switch (this) {
            case TOP -> BOTTOM;
            case BOTTOM -> TOP;
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }
    
    /**
     * Gets all block faces as an array.
     * 
     * @return Array of all faces
     */
    public static BlockFace[] all() {
        return values();
    }
}


