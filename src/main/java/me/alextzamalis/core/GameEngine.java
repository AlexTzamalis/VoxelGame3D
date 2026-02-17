package me.alextzamalis.core;

import me.alextzamalis.config.GameSettings;
import me.alextzamalis.config.SettingsManager;
import me.alextzamalis.graphics.Renderer;
import me.alextzamalis.input.InputManager;
import me.alextzamalis.util.Logger;

/**
 * The main game engine class that manages the game lifecycle.
 * 
 * <p>This class is responsible for initializing all engine subsystems,
 * running the main game loop, and cleaning up resources on shutdown.
 * It follows a modular architecture where each subsystem (window, input,
 * graphics, etc.) is managed independently.
 * 
 * <p>Supports separate update and render threads (like Sodium/Fabric) for
 * better performance, where game logic runs on a separate thread from rendering.
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
    
    /** Default target FPS (used if settings not loaded). */
    private static final int DEFAULT_TARGET_FPS = 60;
    
    /** Default target UPS (used if settings not loaded). */
    private static final int DEFAULT_TARGET_UPS = 20;
    
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
    
    /** Settings manager for configurable FPS/UPS and other settings. */
    private final SettingsManager settingsManager;
    
    /** Update thread (runs game logic separately from rendering). */
    private Thread updateThread;
    
    /** Synchronization object for update thread. */
    private final Object updateLock = new Object();
    
    /** Flag for update thread to run. */
    private volatile boolean updateThreadRunning;
    
    /** Accumulator for fixed timestep updates. */
    private float updateAccumulator = 0f;
    
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
        
        this.settingsManager = SettingsManager.getInstance();
        GameSettings settings = settingsManager.getSettings();
        
        // Use VSync from settings if available, otherwise use parameter
        boolean useVSync = settings != null ? settings.isVSync() : vSync;
        
        this.window = new Window(title, width, height, useVSync);
        this.timer = new Timer();
        this.inputManager = new InputManager();
        this.renderer = new Renderer();
        this.gameLogic = gameLogic;
        this.running = false;
        this.updateThreadRunning = false;
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
     * 
     * <p>Supports separate update thread (like Sodium/Fabric) where game logic
     * runs on a dedicated thread, allowing rendering to run at higher FPS
     * while updates run at a lower, fixed rate.
     */
    private void gameLoop() {
        GameSettings settings = settingsManager.getSettings();
        int targetFPS = settings != null ? settings.getTargetFPS() : DEFAULT_TARGET_FPS;
        int targetUPS = settings != null ? settings.getTargetUPS() : DEFAULT_TARGET_UPS;
        boolean separateThread = settings != null && settings.isSeparateUpdateThread();
        
        float updateInterval = 1f / targetUPS;
        
        // Start update thread if enabled
        if (separateThread) {
            startUpdateThread(targetUPS, updateInterval);
        }
        
        // Main render loop (runs on main thread)
        while (running && !window.windowShouldClose()) {
            float elapsedTime = timer.getElapsedTime();
            
            // Process input (always on main thread)
            input();
            
            // Update logic (on separate thread if enabled, otherwise here)
            if (!separateThread) {
                updateAccumulator += elapsedTime;
                while (updateAccumulator >= updateInterval) {
                    update(updateInterval);
                    updateAccumulator -= updateInterval;
                }
            }
            
            // Render (always on main thread - OpenGL requirement)
            render();
            
            // Sync if not using VSync
            if (!window.isVSync()) {
                sync(targetFPS);
            }
        }
        
        // Stop update thread
        if (separateThread) {
            stopUpdateThread();
        }
    }
    
    /**
     * Starts the update thread for separate update/render threading.
     * 
     * @param targetUPS Target updates per second
     * @param updateInterval Update interval in seconds
     */
    private void startUpdateThread(int targetUPS, float updateInterval) {
        updateThreadRunning = true;
        updateThread = new Thread(() -> {
            Logger.info("Update thread started (target UPS: %d)", targetUPS);
            
            long lastTime = System.nanoTime();
            double nsPerUpdate = 1_000_000_000.0 / targetUPS;
            
            while (updateThreadRunning && running) {
                long now = System.nanoTime();
                long elapsed = now - lastTime;
                lastTime = now;
                
                double deltaTime = elapsed / nsPerUpdate;
                
                // Cap delta time to prevent large jumps
                if (deltaTime > 1.0) {
                    deltaTime = 1.0;
                }
                
                synchronized (updateLock) {
                    update((float) (deltaTime * updateInterval));
                }
                
                // Sleep to maintain target UPS
                long sleepTime = (long) ((nsPerUpdate - elapsed) / 1_000_000);
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            Logger.info("Update thread stopped");
        }, "GameUpdateThread");
        updateThread.setDaemon(true);
        updateThread.start();
    }
    
    /**
     * Stops the update thread.
     */
    private void stopUpdateThread() {
        updateThreadRunning = false;
        if (updateThread != null) {
            try {
                updateThread.join(1000); // Wait up to 1 second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Synchronizes the game loop to the target FPS.
     * 
     * <p>This method sleeps the thread to maintain a consistent
     * frame rate when VSync is disabled.
     * 
     * @param targetFPS Target frames per second
     */
    private void sync(int targetFPS) {
        float loopSlot = 1f / targetFPS;
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
     * <p>This method is thread-safe and can be called from either
     * the main thread or the update thread.
     * 
     * @param deltaTime The time step in seconds
     */
    private void update(float deltaTime) {
        // Cap delta time to prevent large jumps (like Sodium does)
        GameSettings settings = settingsManager.getSettings();
        float maxDelta = settings != null ? settings.getMaxFrameTime() : 0.1f;
        if (deltaTime > maxDelta) {
            deltaTime = maxDelta;
        }
        
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
    
    /**
     * Gets the settings manager.
     * 
     * @return The settings manager
     */
    public SettingsManager getSettingsManager() {
        return settingsManager;
    }
    
    /**
     * Applies VSync setting from config (call after settings change).
     */
    public void applyVSyncSetting() {
        GameSettings settings = settingsManager.getSettings();
        if (settings != null) {
            window.setVSync(settings.isVSync());
        }
    }
}

