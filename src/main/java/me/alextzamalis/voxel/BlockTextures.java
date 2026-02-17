package me.alextzamalis.voxel;

/**
 * Defines the texture paths for each face of a block.
 * 
 * <p>This class allows blocks to have different textures on different faces,
 * like grass blocks which have grass on top, dirt on bottom, and grass_side
 * on the sides.
 * 
 * <p>For blocks with the same texture on all faces, use the single-texture
 * constructor. For blocks with different textures, specify each face individually.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class BlockTextures {
    
    /** Texture path for the top face. */
    private final String top;
    
    /** Texture path for the bottom face. */
    private final String bottom;
    
    /** Texture path for the north face. */
    private final String north;
    
    /** Texture path for the south face. */
    private final String south;
    
    /** Texture path for the east face. */
    private final String east;
    
    /** Texture path for the west face. */
    private final String west;
    
    /**
     * Creates block textures with the same texture on all faces.
     * 
     * @param allFaces The texture path for all faces
     */
    public BlockTextures(String allFaces) {
        this.top = allFaces;
        this.bottom = allFaces;
        this.north = allFaces;
        this.south = allFaces;
        this.east = allFaces;
        this.west = allFaces;
    }
    
    /**
     * Creates block textures with top/bottom and sides.
     * 
     * @param top The texture path for the top face
     * @param bottom The texture path for the bottom face
     * @param sides The texture path for all side faces
     */
    public BlockTextures(String top, String bottom, String sides) {
        this.top = top;
        this.bottom = bottom;
        this.north = sides;
        this.south = sides;
        this.east = sides;
        this.west = sides;
    }
    
    /**
     * Creates block textures with individual textures for each face.
     * 
     * @param top The texture path for the top face
     * @param bottom The texture path for the bottom face
     * @param north The texture path for the north face
     * @param south The texture path for the south face
     * @param east The texture path for the east face
     * @param west The texture path for the west face
     */
    public BlockTextures(String top, String bottom, String north, String south, String east, String west) {
        this.top = top;
        this.bottom = bottom;
        this.north = north;
        this.south = south;
        this.east = east;
        this.west = west;
    }
    
    /**
     * Gets the texture path for a specific face.
     * 
     * @param face The block face
     * @return The texture path
     */
    public String getTexture(BlockFace face) {
        return switch (face) {
            case TOP -> top;
            case BOTTOM -> bottom;
            case NORTH -> north;
            case SOUTH -> south;
            case EAST -> east;
            case WEST -> west;
        };
    }
    
    /**
     * Gets the top texture path.
     * 
     * @return The top texture path
     */
    public String getTop() {
        return top;
    }
    
    /**
     * Gets the bottom texture path.
     * 
     * @return The bottom texture path
     */
    public String getBottom() {
        return bottom;
    }
    
    /**
     * Gets the north texture path.
     * 
     * @return The north texture path
     */
    public String getNorth() {
        return north;
    }
    
    /**
     * Gets the south texture path.
     * 
     * @return The south texture path
     */
    public String getSouth() {
        return south;
    }
    
    /**
     * Gets the east texture path.
     * 
     * @return The east texture path
     */
    public String getEast() {
        return east;
    }
    
    /**
     * Gets the west texture path.
     * 
     * @return The west texture path
     */
    public String getWest() {
        return west;
    }
    
    /**
     * Checks if all faces have the same texture.
     * 
     * @return true if all faces use the same texture
     */
    public boolean isUniform() {
        return top.equals(bottom) && top.equals(north) && 
               top.equals(south) && top.equals(east) && top.equals(west);
    }
}

