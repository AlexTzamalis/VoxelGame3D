package me.alextzamalis;

import me.alextzamalis.core.GameEngine;
import me.alextzamalis.core.IGameLogic;
import me.alextzamalis.util.Logger;

/**
 * Main entry point for the Voxel Game 3D application.
 * 
 * <p>This class initializes and starts the game engine with the VoxelGame logic.
 * It handles command-line arguments and sets up the initial window configuration.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class Main {
    
    /** Default window width. */
    private static final int DEFAULT_WIDTH = 1280;
    
    /** Default window height. */
    private static final int DEFAULT_HEIGHT = 720;
    
    /** Window title. */
    private static final String WINDOW_TITLE = "Voxel Game 3D";
    
    /**
     * Main entry point.
     * 
     * @param args Command line arguments (currently unused)
     */
    public static void main(String[] args) {
        try {
            Logger.info("Starting Voxel Game 3D...");
            
            // Parse command line arguments (for future use)
            int width = DEFAULT_WIDTH;
            int height = DEFAULT_HEIGHT;
            boolean vSync = true;
            
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--width" -> {
                        if (i + 1 < args.length) {
                            width = Integer.parseInt(args[++i]);
                        }
                    }
                    case "--height" -> {
                        if (i + 1 < args.length) {
                            height = Integer.parseInt(args[++i]);
                        }
                    }
                    case "--no-vsync" -> vSync = false;
                    case "--help", "-h" -> {
                        printHelp();
                        return;
                    }
                }
            }
            
            Logger.info("Window size: %dx%d", width, height);
            Logger.info("VSync: %s", vSync ? "Enabled" : "Disabled");
            
            // Create game logic
            IGameLogic gameLogic = new VoxelGame();
            
            // Create and start game engine
            GameEngine engine = new GameEngine(WINDOW_TITLE, width, height, vSync, gameLogic);
            engine.start();
            
        } catch (Exception e) {
            Logger.error("Fatal error: %s", e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Prints command line help.
     */
    private static void printHelp() {
        System.out.println("Voxel Game 3D");
        System.out.println("Usage: java -jar voxelgame.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --width <pixels>   Set window width (default: 1280)");
        System.out.println("  --height <pixels>  Set window height (default: 720)");
        System.out.println("  --no-vsync         Disable vertical sync");
        System.out.println("  --help, -h         Show this help message");
    }
}
