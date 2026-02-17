package me.alextzamalis.gui;

import me.alextzamalis.core.Window;
import me.alextzamalis.input.InputManager;
import me.alextzamalis.util.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages game screens and transitions between them.
 * 
 * <p>The ScreenManager handles:
 * <ul>
 *   <li>Screen registration and lookup</li>
 *   <li>Screen transitions with proper show/hide callbacks</li>
 *   <li>Delegating input, update, and render to active screen</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class ScreenManager {
    
    /** Registered screens by state. */
    private final Map<GameState, Screen> screens;
    
    /** Current game state. */
    private GameState currentState;
    
    /** Currently active screen. */
    private Screen currentScreen;
    
    /** The GUI renderer. */
    private GuiRenderer guiRenderer;
    
    /** Callback for state changes. */
    private StateChangeListener stateChangeListener;
    
    /**
     * Listener for game state changes.
     */
    public interface StateChangeListener {
        void onStateChanged(GameState oldState, GameState newState);
    }
    
    /**
     * Creates a new screen manager.
     */
    public ScreenManager() {
        this.screens = new HashMap<>();
        this.currentState = null;
        this.currentScreen = null;
    }
    
    /**
     * Initializes the screen manager.
     * 
     * @param screenWidth Initial screen width
     * @param screenHeight Initial screen height
     * @throws Exception if initialization fails
     */
    public void init(int screenWidth, int screenHeight) throws Exception {
        guiRenderer = new GuiRenderer();
        guiRenderer.init(screenWidth, screenHeight);
        
        Logger.info("ScreenManager initialized");
    }
    
    /**
     * Registers a screen for a game state.
     * 
     * @param state The game state
     * @param screen The screen to register
     */
    public void registerScreen(GameState state, Screen screen) throws Exception {
        screens.put(state, screen);
        screen.init(guiRenderer);
        Logger.debug("Registered screen for state: %s", state);
    }
    
    /**
     * Sets the state change listener.
     * 
     * @param listener The listener
     */
    public void setStateChangeListener(StateChangeListener listener) {
        this.stateChangeListener = listener;
    }
    
    /**
     * Changes to a new game state.
     * 
     * @param newState The new state
     */
    public void setState(GameState newState) {
        if (newState == currentState) {
            return;
        }
        
        GameState oldState = currentState;
        
        // Hide current screen
        if (currentScreen != null) {
            currentScreen.onHide();
        }
        
        // Switch to new screen
        currentState = newState;
        currentScreen = screens.get(newState);
        
        if (currentScreen != null) {
            currentScreen.onShow();
        } else if (newState != GameState.PLAYING) {
            // PLAYING state doesn't need a screen (renders world directly)
            // Only warn for other states
            Logger.warn("No screen registered for state: %s", newState);
        }
        
        // Notify listener
        if (stateChangeListener != null) {
            stateChangeListener.onStateChanged(oldState, newState);
        }
        
        Logger.info("Game state changed: %s -> %s", oldState, newState);
    }
    
    /**
     * Gets the current game state.
     * 
     * @return The current state
     */
    public GameState getCurrentState() {
        return currentState;
    }
    
    /**
     * Gets the current screen.
     * 
     * @return The current screen, or null if none
     */
    public Screen getCurrentScreen() {
        return currentScreen;
    }
    
    /**
     * Gets the GUI renderer.
     * 
     * @return The GUI renderer
     */
    public GuiRenderer getGuiRenderer() {
        return guiRenderer;
    }
    
    /**
     * Handles input for the current screen.
     * 
     * @param window The window
     * @param inputManager The input manager
     */
    public void input(Window window, InputManager inputManager) {
        if (currentScreen != null) {
            currentScreen.input(window, inputManager);
        }
    }
    
    /**
     * Updates the current screen.
     * 
     * @param deltaTime Time since last update
     */
    public void update(float deltaTime) {
        if (currentScreen != null) {
            currentScreen.update(deltaTime);
        }
    }
    
    /**
     * Renders the current screen.
     */
    public void render() {
        if (guiRenderer != null) {
            guiRenderer.begin();
            
            // Debug: Always draw a test rectangle to verify rendering works
            // guiRenderer.drawRect(100, 100, 200, 200, 1.0f, 0.0f, 0.0f, 1.0f);
            
            if (currentScreen != null) {
                currentScreen.render(guiRenderer);
            }
            guiRenderer.end();
        }
    }
    
    /**
     * Called when window is resized.
     * 
     * @param width New width
     * @param height New height
     */
    public void resize(int width, int height) {
        if (guiRenderer != null) {
            guiRenderer.resize(width, height);
        }
        
        for (Screen screen : screens.values()) {
            screen.resize(width, height);
        }
    }
    
    /**
     * Cleans up all resources.
     */
    public void cleanup() {
        for (Screen screen : screens.values()) {
            screen.cleanup();
        }
        screens.clear();
        
        if (guiRenderer != null) {
            guiRenderer.cleanup();
        }
        
        Logger.info("ScreenManager cleaned up");
    }
}

