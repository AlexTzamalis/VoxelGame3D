package me.alextzamalis.gui.screens;

import me.alextzamalis.core.Window;
import me.alextzamalis.gui.GameState;
import me.alextzamalis.gui.GuiRenderer;
import me.alextzamalis.gui.Screen;
import me.alextzamalis.gui.ScreenManager;
import me.alextzamalis.gui.widgets.Button;
import me.alextzamalis.input.InputManager;
import me.alextzamalis.util.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * The main menu screen shown when the game starts.
 * 
 * <p>Features:
 * <ul>
 *   <li>Game title</li>
 *   <li>Singleplayer button (opens world selection)</li>
 *   <li>Settings button</li>
 *   <li>Quit button</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class MainMenuScreen implements Screen {
    
    /** Button dimensions. */
    private static final float BUTTON_WIDTH = 400;
    private static final float BUTTON_HEIGHT = 40;
    private static final float BUTTON_SPACING = 10;
    
    /** Reference to screen manager for state changes. */
    private final ScreenManager screenManager;
    
    /** Callback for quit action. */
    private Runnable onQuit;
    
    /** Menu buttons. */
    private final List<Button> buttons;
    
    /** Screen dimensions. */
    private int screenWidth;
    private int screenHeight;
    
    /**
     * Creates a new main menu screen.
     * 
     * @param screenManager The screen manager
     */
    public MainMenuScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.buttons = new ArrayList<>();
    }
    
    /**
     * Sets the quit callback.
     * 
     * @param onQuit Callback when quit is clicked
     */
    public void setOnQuit(Runnable onQuit) {
        this.onQuit = onQuit;
    }
    
    @Override
    public void init(GuiRenderer guiRenderer) throws Exception {
        screenWidth = guiRenderer.getScreenWidth();
        screenHeight = guiRenderer.getScreenHeight();
        
        createButtons();
        
        Logger.info("MainMenuScreen initialized");
    }
    
    /**
     * Creates the menu buttons.
     */
    private void createButtons() {
        buttons.clear();
        
        float centerX = screenWidth / 2f;
        float startY = screenHeight / 2f - 20;
        
        // Singleplayer button
        Button singleplayerBtn = new Button(centerX, startY, BUTTON_WIDTH, BUTTON_HEIGHT, "Singleplayer");
        singleplayerBtn.init();
        singleplayerBtn.setOnClick(() -> {
            Logger.info("Singleplayer clicked");
            screenManager.setState(GameState.WORLD_SELECT);
        });
        buttons.add(singleplayerBtn);
        
        // Multiplayer button (disabled for now)
        Button multiplayerBtn = new Button(centerX, startY + BUTTON_HEIGHT + BUTTON_SPACING, 
                                           BUTTON_WIDTH, BUTTON_HEIGHT, "Multiplayer");
        multiplayerBtn.init();
        multiplayerBtn.setEnabled(false);
        buttons.add(multiplayerBtn);
        
        // Settings button
        Button settingsBtn = new Button(centerX, startY + (BUTTON_HEIGHT + BUTTON_SPACING) * 2,
                                        BUTTON_WIDTH, BUTTON_HEIGHT, "Settings");
        settingsBtn.init();
        settingsBtn.setOnClick(() -> {
            Logger.info("Settings clicked");
            screenManager.setState(GameState.SETTINGS);
        });
        buttons.add(settingsBtn);
        
        // Quit button
        Button quitBtn = new Button(centerX, startY + (BUTTON_HEIGHT + BUTTON_SPACING) * 3,
                                    BUTTON_WIDTH, BUTTON_HEIGHT, "Quit Game");
        quitBtn.init();
        quitBtn.setOnClick(() -> {
            Logger.info("Quit clicked");
            if (onQuit != null) {
                onQuit.run();
            }
        });
        buttons.add(quitBtn);
    }
    
    @Override
    public void onShow() {
        Logger.debug("MainMenuScreen shown");
    }
    
    @Override
    public void onHide() {
        Logger.debug("MainMenuScreen hidden");
    }
    
    @Override
    public void input(Window window, InputManager inputManager) {
        // Buttons handle their own input in update
    }
    
    @Override
    public void update(float deltaTime) {
        // Temporarily get input manager from somewhere
        // This is a design limitation we'll fix later
    }
    
    /**
     * Updates with input manager reference.
     * 
     * @param deltaTime Time since last update
     * @param inputManager The input manager
     */
    public void update(float deltaTime, InputManager inputManager) {
        for (Button button : buttons) {
            button.update(inputManager);
        }
    }
    
    @Override
    public void render(GuiRenderer guiRenderer) {
        // Draw dark background
        guiRenderer.drawRect(0, 0, screenWidth, screenHeight, 0.1f, 0.1f, 0.15f, 1.0f);
        
        // Draw title area (placeholder - will add logo later)
        float titleY = screenHeight / 4f;
        guiRenderer.drawRect(screenWidth / 2f - 200, titleY - 30, 400, 60, 0.2f, 0.2f, 0.3f, 0.8f);
        
        // Draw buttons
        for (Button button : buttons) {
            button.render(guiRenderer);
        }
        
        // Draw version info (bottom left)
        guiRenderer.drawRect(5, screenHeight - 25, 150, 20, 0.0f, 0.0f, 0.0f, 0.5f);
    }
    
    @Override
    public void resize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        createButtons(); // Recreate buttons with new positions
    }
    
    @Override
    public void cleanup() {
        buttons.clear();
    }
    
    /**
     * Gets the buttons for external input handling.
     * 
     * @return The list of buttons
     */
    public List<Button> getButtons() {
        return buttons;
    }
}

