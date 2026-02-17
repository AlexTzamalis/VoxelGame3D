package me.alextzamalis.config;

import me.alextzamalis.util.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Manages loading and saving game settings to/from config.properties file.
 * 
 * <p>This class follows the pattern of performance mods like Sodium/Fabric,
 * storing settings in a simple properties file that can be edited manually
 * or through the in-game settings menu.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class SettingsManager {
    
    /** Configuration file name. */
    private static final String CONFIG_FILE = "config.properties";
    
    /** Singleton instance. */
    private static SettingsManager instance;
    
    /** Current game settings. */
    private final GameSettings settings;
    
    /** Path to config file. */
    private final Path configPath;
    
    /**
     * Private constructor for singleton.
     */
    private SettingsManager() {
        this.settings = new GameSettings();
        this.configPath = Paths.get(CONFIG_FILE);
        loadSettings();
    }
    
    /**
     * Gets the singleton instance.
     * 
     * @return The SettingsManager instance
     */
    public static synchronized SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }
        return instance;
    }
    
    /**
     * Gets the current game settings.
     * 
     * @return The game settings
     */
    public GameSettings getSettings() {
        return settings;
    }
    
    /**
     * Loads settings from config.properties file.
     * Creates default config if file doesn't exist.
     */
    public void loadSettings() {
        if (!Files.exists(configPath)) {
            Logger.info("Config file not found, creating default config.properties");
            saveSettings(); // Create default config
            return;
        }
        
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            props.load(in);
            
            // Performance settings
            settings.setTargetFPS(getInt(props, "targetFPS", 60));
            settings.setTargetUPS(getInt(props, "targetUPS", 20));
            settings.setVSync(getBoolean(props, "vSync", true));
            settings.setSeparateUpdateThread(getBoolean(props, "separateUpdateThread", true));
            settings.setMaxFrameTime(getFloat(props, "maxFrameTime", 0.1f));
            
            // Rendering settings
            settings.setViewDistance(getInt(props, "viewDistance", 3));
            settings.setFrustumCulling(getBoolean(props, "frustumCulling", true));
            settings.setGreedyMeshing(getBoolean(props, "greedyMeshing", true));
            settings.setLodEnabled(getBoolean(props, "lodEnabled", true));
            settings.setMaxChunksPerFrame(getInt(props, "maxChunksPerFrame", 1));
            settings.setMaxMeshesPerFrame(getInt(props, "maxMeshesPerFrame", 1));
            
            // Graphics settings
            String qualityStr = props.getProperty("renderQuality", "NORMAL");
            try {
                settings.setRenderQuality(GameSettings.RenderQuality.valueOf(qualityStr));
            } catch (IllegalArgumentException e) {
                settings.setRenderQuality(GameSettings.RenderQuality.NORMAL);
            }
            settings.setWireframe(getBoolean(props, "wireframe", false));
            
            // Gameplay settings
            settings.setMouseSensitivity(getFloat(props, "mouseSensitivity", 1.0f));
            settings.setMovementSpeed(getFloat(props, "movementSpeed", 1.0f));
            settings.setShowFPS(getBoolean(props, "showFPS", false));
            settings.setShowDebug(getBoolean(props, "showDebug", false));
            
            Logger.info("Loaded settings from config.properties");
        } catch (IOException e) {
            Logger.error("Failed to load config.properties: %s", e.getMessage());
            Logger.info("Using default settings");
        }
    }
    
    /**
     * Saves settings to config.properties file.
     */
    public void saveSettings() {
        Properties props = new Properties();
        
        // Performance settings
        props.setProperty("targetFPS", String.valueOf(settings.getTargetFPS()));
        props.setProperty("targetUPS", String.valueOf(settings.getTargetUPS()));
        props.setProperty("vSync", String.valueOf(settings.isVSync()));
        props.setProperty("separateUpdateThread", String.valueOf(settings.isSeparateUpdateThread()));
        props.setProperty("maxFrameTime", String.valueOf(settings.getMaxFrameTime()));
        
        // Rendering settings
        props.setProperty("viewDistance", String.valueOf(settings.getViewDistance()));
        props.setProperty("frustumCulling", String.valueOf(settings.isFrustumCulling()));
        props.setProperty("greedyMeshing", String.valueOf(settings.isGreedyMeshing()));
        props.setProperty("lodEnabled", String.valueOf(settings.isLodEnabled()));
        props.setProperty("maxChunksPerFrame", String.valueOf(settings.getMaxChunksPerFrame()));
        props.setProperty("maxMeshesPerFrame", String.valueOf(settings.getMaxMeshesPerFrame()));
        
        // Graphics settings
        props.setProperty("renderQuality", settings.getRenderQuality().name());
        props.setProperty("wireframe", String.valueOf(settings.isWireframe()));
        
        // Gameplay settings
        props.setProperty("mouseSensitivity", String.valueOf(settings.getMouseSensitivity()));
        props.setProperty("movementSpeed", String.valueOf(settings.getMovementSpeed()));
        props.setProperty("showFPS", String.valueOf(settings.isShowFPS()));
        props.setProperty("showDebug", String.valueOf(settings.isShowDebug()));
        
        try (OutputStream out = Files.newOutputStream(configPath)) {
            props.store(out, "Voxel Game 3D Configuration\n" +
                           "Edit these settings to customize performance and gameplay.\n" +
                           "Changes take effect after restarting the game.");
            Logger.info("Saved settings to config.properties");
        } catch (IOException e) {
            Logger.error("Failed to save config.properties: %s", e.getMessage());
        }
    }
    
    /**
     * Helper method to get integer property with default.
     */
    private int getInt(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Helper method to get float property with default.
     */
    private float getFloat(Properties props, String key, float defaultValue) {
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Helper method to get boolean property with default.
     */
    private boolean getBoolean(Properties props, String key, boolean defaultValue) {
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value);
    }
}

