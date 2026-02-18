package me.alextzamalis.voxel;

/**
 * Defines the tint colors for each face of a block.
 * 
 * <p>Tint colors are used to colorize grayscale textures, similar to how
 * Minecraft colors grass and leaves based on biome. The tint is multiplied
 * with the texture color in the shader.
 * 
 * <p>A tint of (1, 1, 1) means no tinting (original texture colors).
 * A tint of (0.5, 0.8, 0.3) would give a green tint.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class BlockTints {
    
    /** Tint color for the top face (RGB). */
    private final float[] top;
    
    /** Tint color for the bottom face (RGB). */
    private final float[] bottom;
    
    /** Tint color for the side faces (RGB). */
    private final float[] sides;
    
    /**
     * Creates block tints with the same color for all faces.
     * 
     * @param allFaces The tint color for all faces (RGB array)
     */
    public BlockTints(float[] allFaces) {
        this.top = allFaces.clone();
        this.bottom = allFaces.clone();
        this.sides = allFaces.clone();
    }
    
    /**
     * Creates block tints with different colors for top, bottom, and sides.
     * 
     * @param top The tint color for the top face
     * @param bottom The tint color for the bottom face
     * @param sides The tint color for all side faces
     */
    public BlockTints(float[] top, float[] bottom, float[] sides) {
        this.top = top.clone();
        this.bottom = bottom.clone();
        this.sides = sides.clone();
    }
    
    /**
     * Gets the tint color for a specific face.
     * 
     * @param face The block face
     * @return The tint color (RGB array)
     */
    public float[] getTint(BlockFace face) {
        return switch (face) {
            case TOP -> top;
            case BOTTOM -> bottom;
            default -> sides;
        };
    }
    
    /**
     * Gets the top tint color.
     */
    public float[] getTop() {
        return top;
    }
    
    /**
     * Gets the bottom tint color.
     */
    public float[] getBottom() {
        return bottom;
    }
    
    /**
     * Gets the sides tint color.
     */
    public float[] getSides() {
        return sides;
    }
}


