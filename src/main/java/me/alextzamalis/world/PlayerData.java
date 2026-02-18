package me.alextzamalis.world;

/**
 * Contains player state data that is persisted with the world.
 * 
 * <p>This includes position, rotation, game mode, health, hunger,
 * and inventory data.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class PlayerData {
    
    /** Default spawn height above terrain. */
    public static final double DEFAULT_SPAWN_Y = 100.0;
    
    // Position
    private double x;
    private double y;
    private double z;
    
    // Rotation
    private float pitch;
    private float yaw;
    
    // Game state
    private int gameMode;
    private float health;
    private float hunger;
    private int selectedSlot;
    
    // Inventory (simplified for now - just hotbar block IDs)
    private int[] hotbarItems;
    
    /**
     * Creates new player data with default values.
     */
    public PlayerData() {
        this.x = 0;
        this.y = DEFAULT_SPAWN_Y;
        this.z = 0;
        this.pitch = 0;
        this.yaw = 0;
        this.gameMode = WorldMetadata.GAME_MODE_CREATIVE;
        this.health = 20.0f;
        this.hunger = 20.0f;
        this.selectedSlot = 0;
        this.hotbarItems = new int[9];
        
        // Default hotbar items (block IDs)
        hotbarItems[0] = 1; // Stone
        hotbarItems[1] = 2; // Dirt
        hotbarItems[2] = 3; // Grass
        hotbarItems[3] = 4; // Cobblestone
        hotbarItems[4] = 5; // Oak Planks
        hotbarItems[5] = 6; // Oak Log
    }
    
    /**
     * Creates player data at a specific spawn position.
     * 
     * @param x Spawn X
     * @param y Spawn Y
     * @param z Spawn Z
     */
    public PlayerData(double x, double y, double z) {
        this();
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    // Position getters/setters
    
    public double getX() {
        return x;
    }
    
    public void setX(double x) {
        this.x = x;
    }
    
    public double getY() {
        return y;
    }
    
    public void setY(double y) {
        this.y = y;
    }
    
    public double getZ() {
        return z;
    }
    
    public void setZ(double z) {
        this.z = z;
    }
    
    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    // Rotation getters/setters
    
    public float getPitch() {
        return pitch;
    }
    
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
    
    public float getYaw() {
        return yaw;
    }
    
    public void setYaw(float yaw) {
        this.yaw = yaw;
    }
    
    public void setRotation(float pitch, float yaw) {
        this.pitch = pitch;
        this.yaw = yaw;
    }
    
    // Game state getters/setters
    
    public int getGameMode() {
        return gameMode;
    }
    
    public void setGameMode(int gameMode) {
        this.gameMode = gameMode;
    }
    
    public boolean isCreative() {
        return gameMode == WorldMetadata.GAME_MODE_CREATIVE;
    }
    
    public boolean isSurvival() {
        return gameMode == WorldMetadata.GAME_MODE_SURVIVAL;
    }
    
    public float getHealth() {
        return health;
    }
    
    public void setHealth(float health) {
        this.health = Math.max(0, Math.min(20, health));
    }
    
    public float getHunger() {
        return hunger;
    }
    
    public void setHunger(float hunger) {
        this.hunger = Math.max(0, Math.min(20, hunger));
    }
    
    public int getSelectedSlot() {
        return selectedSlot;
    }
    
    public void setSelectedSlot(int selectedSlot) {
        this.selectedSlot = Math.max(0, Math.min(8, selectedSlot));
    }
    
    // Hotbar methods
    
    public int[] getHotbarItems() {
        return hotbarItems;
    }
    
    public void setHotbarItems(int[] items) {
        if (items != null && items.length == 9) {
            this.hotbarItems = items;
        }
    }
    
    public int getHotbarItem(int slot) {
        if (slot >= 0 && slot < 9) {
            return hotbarItems[slot];
        }
        return 0;
    }
    
    public void setHotbarItem(int slot, int blockId) {
        if (slot >= 0 && slot < 9) {
            hotbarItems[slot] = blockId;
        }
    }
    
    public int getSelectedBlockId() {
        return getHotbarItem(selectedSlot);
    }
    
    @Override
    public String toString() {
        return String.format("PlayerData[pos=(%.1f, %.1f, %.1f), mode=%d, health=%.1f]",
                            x, y, z, gameMode, health);
    }
}


