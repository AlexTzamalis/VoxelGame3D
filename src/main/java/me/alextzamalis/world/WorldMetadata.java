package me.alextzamalis.world;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Contains metadata about a saved world.
 * 
 * <p>This class stores information about a world that is persisted
 * to disk and displayed in the world selection screen.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class WorldMetadata {
    
    /** Game mode constants. */
    public static final int GAME_MODE_SURVIVAL = 0;
    public static final int GAME_MODE_CREATIVE = 1;
    
    /** The world name (display name). */
    private String name;
    
    /** The world seed for generation. */
    private long seed;
    
    /** Unix timestamp when the world was created. */
    private long creationTime;
    
    /** Unix timestamp when the world was last played. */
    private long lastPlayed;
    
    /** The game mode (0 = Survival, 1 = Creative). */
    private int gameMode;
    
    /** Total play time in seconds. */
    private long playTime;
    
    /**
     * Creates new world metadata.
     * 
     * @param name The world name
     * @param seed The world seed
     */
    public WorldMetadata(String name, long seed) {
        this.name = name;
        this.seed = seed;
        this.creationTime = System.currentTimeMillis();
        this.lastPlayed = this.creationTime;
        this.gameMode = GAME_MODE_CREATIVE; // Default to creative
        this.playTime = 0;
    }
    
    /**
     * Creates new world metadata with a random seed.
     * 
     * @param name The world name
     */
    public WorldMetadata(String name) {
        this(name, System.currentTimeMillis());
    }
    
    // Getters and Setters
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public long getSeed() {
        return seed;
    }
    
    public void setSeed(long seed) {
        this.seed = seed;
    }
    
    public long getCreationTime() {
        return creationTime;
    }
    
    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }
    
    public long getLastPlayed() {
        return lastPlayed;
    }
    
    public void setLastPlayed(long lastPlayed) {
        this.lastPlayed = lastPlayed;
    }
    
    public int getGameMode() {
        return gameMode;
    }
    
    public void setGameMode(int gameMode) {
        this.gameMode = gameMode;
    }
    
    public long getPlayTime() {
        return playTime;
    }
    
    public void setPlayTime(long playTime) {
        this.playTime = playTime;
    }
    
    /**
     * Adds to the play time.
     * 
     * @param seconds Seconds to add
     */
    public void addPlayTime(long seconds) {
        this.playTime += seconds;
    }
    
    /**
     * Updates the last played timestamp to now.
     */
    public void updateLastPlayed() {
        this.lastPlayed = System.currentTimeMillis();
    }
    
    /**
     * Gets the game mode as a string.
     * 
     * @return "Survival" or "Creative"
     */
    public String getGameModeString() {
        return gameMode == GAME_MODE_CREATIVE ? "Creative" : "Survival";
    }
    
    /**
     * Gets a formatted creation date string.
     * 
     * @return Formatted date string
     */
    public String getCreationDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
        return sdf.format(new Date(creationTime));
    }
    
    /**
     * Gets a formatted last played string.
     * 
     * @return Human-readable last played string
     */
    public String getLastPlayedString() {
        long now = System.currentTimeMillis();
        long diff = now - lastPlayed;
        
        // Convert to appropriate unit
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 30) {
            return new SimpleDateFormat("MMM dd, yyyy").format(new Date(lastPlayed));
        } else if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "Just now";
        }
    }
    
    /**
     * Gets formatted play time string.
     * 
     * @return Human-readable play time
     */
    public String getPlayTimeString() {
        long hours = playTime / 3600;
        long minutes = (playTime % 3600) / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm", minutes);
        } else {
            return "< 1m";
        }
    }
    
    @Override
    public String toString() {
        return String.format("World[name=%s, seed=%d, mode=%s, lastPlayed=%s]",
                            name, seed, getGameModeString(), getLastPlayedString());
    }
}


