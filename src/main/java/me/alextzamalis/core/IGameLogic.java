package me.alextzamalis.core;

import me.alextzamalis.input.InputManager;

/**
 * Interface for implementing game-specific logic.
 * 
 * <p>This interface defines the contract between the game engine and
 * the game implementation. Classes implementing this interface provide
 * the actual game behavior while the engine handles the low-level
 * systems like window management, input, and rendering.
 * 
 * <p>The lifecycle of a game follows these phases:
 * <ol>
 *   <li>{@link #init(Window)} - Called once at startup</li>
 *   <li>{@link #input(Window, InputManager)} - Called every frame for input processing</li>
 *   <li>{@link #update(float, InputManager)} - Called at fixed intervals for game logic</li>
 *   <li>{@link #render(Window)} - Called every frame for rendering</li>
 *   <li>{@link #cleanup()} - Called once at shutdown</li>
 * </ol>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public interface IGameLogic {
    
    /**
     * Initializes the game.
     * 
     * <p>This method is called once when the game starts. Use this
     * to load resources, create game objects, and set up the initial
     * game state.
     * 
     * @param window The game window
     * @throws Exception if initialization fails
     */
    void init(Window window) throws Exception;
    
    /**
     * Processes input events.
     * 
     * <p>This method is called every frame before the update method.
     * Use this to check for input and respond accordingly.
     * 
     * @param window The game window
     * @param inputManager The input manager for checking input state
     */
    void input(Window window, InputManager inputManager);
    
    /**
     * Updates game logic.
     * 
     * <p>This method is called at a fixed rate (typically 60 times per second).
     * Use this for game logic, physics, AI, and other time-dependent updates.
     * 
     * @param deltaTime The time step in seconds (typically 1/60)
     * @param inputManager The input manager for checking input state
     */
    void update(float deltaTime, InputManager inputManager);
    
    /**
     * Renders the game.
     * 
     * <p>This method is called every frame after the update method.
     * Use this to render game objects, UI, and other visual elements.
     * 
     * @param window The game window
     */
    void render(Window window);
    
    /**
     * Cleans up game resources.
     * 
     * <p>This method is called once when the game shuts down. Use this
     * to release any resources that were allocated during the game's
     * lifecycle.
     */
    void cleanup();
}


