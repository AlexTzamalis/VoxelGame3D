package me.alextzamalis.gui;

import me.alextzamalis.core.Window;
import me.alextzamalis.input.InputManager;

/**
 * Interface for game screens (menus, HUD, etc.).
 * 
 * <p>Each screen represents a distinct UI state like main menu,
 * world selection, settings, or in-game HUD. Screens handle their
 * own input, updates, and rendering.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public interface Screen {
    
    /**
     * Initializes the screen.
     * 
     * @param guiRenderer The GUI renderer
     * @throws Exception if initialization fails
     */
    void init(GuiRenderer guiRenderer) throws Exception;
    
    /**
     * Called when this screen becomes active.
     */
    void onShow();
    
    /**
     * Called when this screen is hidden (another screen becomes active).
     */
    void onHide();
    
    /**
     * Handles input for this screen.
     * 
     * @param window The window
     * @param inputManager The input manager
     */
    void input(Window window, InputManager inputManager);
    
    /**
     * Updates the screen state.
     * 
     * @param deltaTime Time since last update
     */
    void update(float deltaTime);
    
    /**
     * Renders the screen.
     * 
     * @param guiRenderer The GUI renderer
     */
    void render(GuiRenderer guiRenderer);
    
    /**
     * Called when the window is resized.
     * 
     * @param width New width
     * @param height New height
     */
    void resize(int width, int height);
    
    /**
     * Cleans up screen resources.
     */
    void cleanup();
}

