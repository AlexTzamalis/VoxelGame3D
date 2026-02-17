package me.alextzamalis.voxel;

/**
 * Represents a block type in the voxel world.
 * 
 * <p>This class defines the properties of a block type, including its
 * identifier, display name, textures, and physical properties. Blocks
 * are registered in the {@link BlockRegistry} and referenced by their ID.
 * 
 * <p>The block system is designed to be modular and data-driven, allowing
 * new blocks to be added easily without modifying core engine code.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class Block {
    
    /** The unique identifier for this block type. */
    private final String id;
    
    /** The display name of this block. */
    private final String displayName;
    
    /** The textures for each face of this block. */
    private final BlockTextures textures;
    
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
     * 
     * @param builder The block builder
     */
    private Block(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.textures = builder.textures;
        this.solid = builder.solid;
        this.transparent = builder.transparent;
        this.lightSource = builder.lightSource;
        this.lightLevel = builder.lightLevel;
        this.hardness = builder.hardness;
    }
    
    /**
     * Gets the block identifier.
     * 
     * @return The block ID (e.g., "minecraft:dirt")
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the display name.
     * 
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the block textures.
     * 
     * @return The textures for each face
     */
    public BlockTextures getTextures() {
        return textures;
    }
    
    /**
     * Checks if this block is solid.
     * 
     * @return true if solid
     */
    public boolean isSolid() {
        return solid;
    }
    
    /**
     * Checks if this block is transparent.
     * 
     * @return true if transparent
     */
    public boolean isTransparent() {
        return transparent;
    }
    
    /**
     * Checks if this block is a light source.
     * 
     * @return true if it emits light
     */
    public boolean isLightSource() {
        return lightSource;
    }
    
    /**
     * Gets the light level emitted.
     * 
     * @return The light level (0-15)
     */
    public int getLightLevel() {
        return lightLevel;
    }
    
    /**
     * Gets the hardness.
     * 
     * @return The hardness value
     */
    public float getHardness() {
        return hardness;
    }
    
    /**
     * Checks if this is the air block.
     * 
     * @return true if this is air
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
        private boolean solid = true;
        private boolean transparent = false;
        private boolean lightSource = false;
        private int lightLevel = 0;
        private float hardness = 1.0f;
        
        /**
         * Creates a new block builder.
         * 
         * @param id The unique block identifier
         */
        public Builder(String id) {
            this.id = id;
            this.displayName = id;
        }
        
        /**
         * Sets the display name.
         * 
         * @param displayName The display name
         * @return This builder
         */
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }
        
        /**
         * Sets the textures using a single texture for all faces.
         * 
         * @param texturePath The texture path
         * @return This builder
         */
        public Builder texture(String texturePath) {
            this.textures = new BlockTextures(texturePath);
            return this;
        }
        
        /**
         * Sets the textures.
         * 
         * @param textures The block textures
         * @return This builder
         */
        public Builder textures(BlockTextures textures) {
            this.textures = textures;
            return this;
        }
        
        /**
         * Sets whether the block is solid.
         * 
         * @param solid true if solid
         * @return This builder
         */
        public Builder solid(boolean solid) {
            this.solid = solid;
            return this;
        }
        
        /**
         * Sets whether the block is transparent.
         * 
         * @param transparent true if transparent
         * @return This builder
         */
        public Builder transparent(boolean transparent) {
            this.transparent = transparent;
            return this;
        }
        
        /**
         * Sets the block as a light source.
         * 
         * @param lightLevel The light level (0-15)
         * @return This builder
         */
        public Builder lightSource(int lightLevel) {
            this.lightSource = lightLevel > 0;
            this.lightLevel = Math.min(15, Math.max(0, lightLevel));
            return this;
        }
        
        /**
         * Sets the hardness.
         * 
         * @param hardness The hardness value
         * @return This builder
         */
        public Builder hardness(float hardness) {
            this.hardness = hardness;
            return this;
        }
        
        /**
         * Builds the block.
         * 
         * @return The new Block instance
         */
        public Block build() {
            if (textures == null) {
                throw new IllegalStateException("Block must have textures defined");
            }
            return new Block(this);
        }
    }
}

