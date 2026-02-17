package me.alextzamalis.voxel;

/**
 * Represents a block type in the voxel world.
 * 
 * <p>This class defines the properties of a block type, including its
 * identifier, display name, textures, tint colors, and physical properties.
 * Blocks are registered in the {@link BlockRegistry} and referenced by their ID.
 * 
 * <p>Tint colors allow grayscale textures to be colorized (like Minecraft's
 * grass and leaves which are tinted based on biome).
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class Block {
    
    /** Default tint color (white - no tinting). */
    public static final float[] DEFAULT_TINT = {1.0f, 1.0f, 1.0f};
    
    /** Grass green tint color. */
    public static final float[] GRASS_TINT = {0.56f, 0.74f, 0.35f};
    
    /** The unique identifier for this block type. */
    private final String id;
    
    /** The display name of this block. */
    private final String displayName;
    
    /** The textures for each face of this block. */
    private final BlockTextures textures;
    
    /** The tint colors for each face (top, bottom, sides). */
    private final BlockTints tints;
    
    /** Whether this block is solid (blocks movement and light). */
    private final boolean solid;
    
    /** Whether this block is transparent (allows light through). */
    private final boolean transparent;
    
    /** Whether this block emits light. */
    private final boolean lightSource;
    
    /** The light level emitted by this block (0-15). */
    private final int lightLevel;
    
    /** The hardness of this block (affects break time). */
    private final float hardness;
    
    /**
     * Creates a new block type using a builder.
     */
    private Block(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.textures = builder.textures;
        this.tints = builder.tints != null ? builder.tints : new BlockTints(DEFAULT_TINT);
        this.solid = builder.solid;
        this.transparent = builder.transparent;
        this.lightSource = builder.lightSource;
        this.lightLevel = builder.lightLevel;
        this.hardness = builder.hardness;
    }
    
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public BlockTextures getTextures() { return textures; }
    public BlockTints getTints() { return tints; }
    public boolean isSolid() { return solid; }
    public boolean isTransparent() { return transparent; }
    public boolean isLightSource() { return lightSource; }
    public int getLightLevel() { return lightLevel; }
    public float getHardness() { return hardness; }
    
    /**
     * Gets the tint color for a specific face.
     * 
     * @param face The block face
     * @return RGB tint color array
     */
    public float[] getTint(BlockFace face) {
        return tints.getTint(face);
    }
    
    /**
     * Checks if this is the air block.
     */
    public boolean isAir() {
        return "minecraft:air".equals(id);
    }
    
    @Override
    public String toString() {
        return "Block{" + id + "}";
    }
    
    /**
     * Builder class for creating Block instances.
     */
    public static class Builder {
        private final String id;
        private String displayName;
        private BlockTextures textures;
        private BlockTints tints;
        private boolean solid = true;
        private boolean transparent = false;
        private boolean lightSource = false;
        private int lightLevel = 0;
        private float hardness = 1.0f;
        
        public Builder(String id) {
            this.id = id;
            this.displayName = id;
        }
        
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }
        
        public Builder texture(String texturePath) {
            this.textures = new BlockTextures(texturePath);
            return this;
        }
        
        public Builder textures(BlockTextures textures) {
            this.textures = textures;
            return this;
        }
        
        /**
         * Sets the tint color for all faces.
         */
        public Builder tint(float[] tint) {
            this.tints = new BlockTints(tint);
            return this;
        }
        
        /**
         * Sets different tint colors for top, bottom, and sides.
         */
        public Builder tints(float[] topTint, float[] bottomTint, float[] sideTint) {
            this.tints = new BlockTints(topTint, bottomTint, sideTint);
            return this;
        }
        
        /**
         * Sets the tints object directly.
         */
        public Builder tints(BlockTints tints) {
            this.tints = tints;
            return this;
        }
        
        public Builder solid(boolean solid) {
            this.solid = solid;
            return this;
        }
        
        public Builder transparent(boolean transparent) {
            this.transparent = transparent;
            return this;
        }
        
        public Builder lightSource(int lightLevel) {
            this.lightSource = lightLevel > 0;
            this.lightLevel = Math.min(15, Math.max(0, lightLevel));
            return this;
        }
        
        public Builder hardness(float hardness) {
            this.hardness = hardness;
            return this;
        }
        
        public Block build() {
            if (textures == null) {
                throw new IllegalStateException("Block must have textures defined");
            }
            return new Block(this);
        }
    }
}
