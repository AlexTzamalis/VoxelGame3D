package me.alextzamalis.core;

import me.alextzamalis.graphics.Renderer;
import me.alextzamalis.input.InputManager;

/**
 * The main game engine class that manages the game lifecycle.
 * 
 * <p>This class is responsible for initializing all engine subsystems,
 * running the main game loop, and cleaning up resources on shutdown.
 * It follows a modular architecture where each subsystem (window, input,
 * graphics, etc.) is managed independently.
 * 
 * <p>Usage example:
 * <pre>{@code
 * GameEngine engine = new GameEngine("My Game", 1280, 720);
 * engine.start();
 * }</pre>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class GameEngine implements Runnable {
    
    /** The target frames per second for the game loop. */
    public static final int TARGET_FPS = 60;
    
    /** The target updates per second for the game logic. */
    public static final int TARGET_UPS = 60;
    
    /** The window instance for rendering. */
    private final Window window;
    
    /** The game loop timer. */
    private final Timer timer;
    
    /** The input manager for handling keyboard and mouse input. */
    private final InputManager inputManager;
    
    /** The renderer for OpenGL rendering. */
    private final Renderer renderer;
    
    /** The game logic interface. */
    private final IGameLogic gameLogic;
    
    /** Flag indicating if the engine is running. */
    private volatile boolean running;
    
    /**
     * Creates a new game engine instance with the specified window parameters.
     * 
     * @param title The window title
     * @param width The window width in pixels
     * @param height The window height in pixels
     * @param vSync Whether to enable vertical sync
     * @param gameLogic The game logic implementation
     * @throws IllegalArgumentException if width or height is less than 1
     * @throws NullPointerException if title or gameLogic is null
     */
    public GameEngine(String title, int width, int height, boolean vSync, IGameLogic gameLogic) {
        if (title == null) {
            throw new NullPointerException("Title cannot be null");
        }
        if (gameLogic == null) {
            throw new NullPointerException("GameLogic cannot be null");
        }
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Width and height must be at least 1");
        }
        
        this.window = new Window(title, width, height, vSync);
        this.timer = new Timer();
        this.inputManager = new InputManager();
        this.renderer = new Renderer();
        this.gameLogic = gameLogic;
        this.running = false;
    }
    
    /**
     * Starts the game engine on the current thread.
     * 
     * <p>This method initializes all subsystems and begins the game loop.
     * The method blocks until the game is terminated.
     */
    public void start() {
        running = true;
        run();
    }
    
    /**
     * Stops the game engine.
     * 
     * <p>This method signals the game loop to terminate. The actual
     * cleanup happens after the current frame completes.
     */
    public void stop() {
        running = false;
    }
    
    /**
     * The main game loop entry point.
     * 
     * <p>This method is called when the engine starts and manages
     * the initialization, game loop, and cleanup phases.
     */
    @Override
    public void run() {
        try {
            init();
            gameLoop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }
    
    /**
     * Initializes all engine subsystems.
     * 
     * @throws Exception if initialization fails
     */
    private void init() throws Exception {
        // Initialize window and OpenGL context
        window.init();
        
        // Initialize timer
        timer.init();
        
        // Initialize input manager
        inputManager.init(window);
        
        // Initialize renderer
        renderer.init();
        
        // Initialize game logic
        gameLogic.init(window);
    }
    
    /**
     * The main game loop using a fixed timestep for updates.
     * 
     * <p>This loop separates update logic (fixed timestep) from rendering
     * (variable timestep) to ensure consistent physics and game logic
     * regardless of frame rate.
     */
    private void gameLoop() {
        float elapsedTime;
        float accumulator = 0f;
        float interval = 1f / TARGET_UPS;
        
        while (running && !window.windowShouldClose()) {
            elapsedTime = timer.getElapsedTime();
            accumulator += elapsedTime;
            
            // Process input
            input();
            
            // Fixed timestep updates
            while (accumulator >= interval) {
                update(interval);
                accumulator -= interval;
            }
            
            // Render
            render();
            
            // Sync if not using VSync
            if (!window.isVSync()) {
                sync();
            }
        }
    }
    
    /**
     * Synchronizes the game loop to the target FPS.
     * 
     * <p>This method sleeps the thread to maintain a consistent
     * frame rate when VSync is disabled.
     */
    private void sync() {
        float loopSlot = 1f / TARGET_FPS;
        double endTime = timer.getLastLoopTime() + loopSlot;
        while (timer.getTime() < endTime) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Processes input events.
     */
    private void input() {
        inputManager.update();
        gameLogic.input(window, inputManager);
    }
    
    /**
     * Updates game logic with a fixed timestep.
     * 
     * @param deltaTime The time step in seconds
     */
    private void update(float deltaTime) {
        gameLogic.update(deltaTime, inputManager);
    }
    
    /**
     * Renders the current frame.
     */
    private void render() {
        gameLogic.render(window);
        window.update();
    }
    
    /**
     * Cleans up all engine resources.
     * 
     * <p>This method is called when the engine shuts down and
     * releases all allocated resources. The cleanup order is important:
     * game logic first (releases OpenGL resources), then input, then window
     * (which destroys the OpenGL context).
     */
    private void cleanup() {
        // Clean up in reverse order of initialization
        // Game logic first (while OpenGL context is still valid)
        try {
            gameLogic.cleanup();
        } catch (Exception e) {
            System.err.println("Error cleaning up game logic: " + e.getMessage());
        }
        
        // Renderer next (also uses OpenGL)
        try {
            renderer.cleanup();
        } catch (Exception e) {
            System.err.println("Error cleaning up renderer: " + e.getMessage());
        }
        
        // Input manager (frees GLFW callbacks)
        try {
            inputManager.cleanup();
        } catch (Exception e) {
            System.err.println("Error cleaning up input manager: " + e.getMessage());
        }
        
        // Window last (destroys OpenGL context and terminates GLFW)
        try {
            window.cleanup();
        } catch (Exception e) {
            System.err.println("Error cleaning up window: " + e.getMessage());
        }
    }
    
    /**
     * Gets the window instance.
     * 
     * @return The window instance
     */
    public Window getWindow() {
        return window;
    }
    
    /**
     * Gets the input manager.
     * 
     * @return The input manager
     */
    public InputManager getInputManager() {
        return inputManager;
    }
    
    /**
     * Gets the renderer.
     * 
     * @return The renderer
     */
    public Renderer getRenderer() {
        return renderer;
    }
}

