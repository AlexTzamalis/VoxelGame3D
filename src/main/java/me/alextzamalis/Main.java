package me.alextzamalis;

import me.alextzamalis.core.GameEngine;
import me.alextzamalis.util.Logger;

/**
 * Main entry point for the Voxel Game 3D application.
 * 
 * <p>This class initializes and starts the game engine with the
 * demo game implementation. It sets up the window parameters and
 * handles any startup errors.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class Main {
    
    /** Default window width. */
    private static final int WINDOW_WIDTH = 1280;
    
    /** Default window height. */
    private static final int WINDOW_HEIGHT = 720;
    
    /** Window title. */
    private static final String WINDOW_TITLE = "Voxel Game 3D";
    
    /** Enable VSync by default. */
    private static final boolean VSYNC = true;
    
    /**
     * Main entry point.
     * 
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        Logger.info("Starting %s...", WINDOW_TITLE);
        Logger.info("Window size: %dx%d", WINDOW_WIDTH, WINDOW_HEIGHT);
        Logger.info("VSync: %s", VSYNC ? "Enabled" : "Disabled");
        
        try {
            // Create game logic - use VoxelGame for the full voxel world
            // or DemoGame for the simple textured block demo
            VoxelGame game = new VoxelGame();
            
            // Create and start the game engine
            GameEngine engine = new GameEngine(
                WINDOW_TITLE,
                WINDOW_WIDTH,
                WINDOW_HEIGHT,
                VSYNC,
                game
            );
            
            engine.start();
            
        } catch (Exception e) {
            Logger.error("Failed to start the game!", e);
            System.exit(1);
        }
        
        Logger.info("Game closed.");
    }
}
