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
 * Settings screen for game options.
 * 
 * <p>Features (placeholder for now):
 * <ul>
 *   <li>Graphics settings</li>
 *   <li>Audio settings</li>
 *   <li>Controls</li>
 *   <li>Back button</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class SettingsScreen implements Screen {
    
    private static final float BUTTON_WIDTH = 300;
    private static final float BUTTON_HEIGHT = 40;
    private static final float BUTTON_SPACING = 10;
    
    private final ScreenManager screenManager;
    private final List<Button> buttons;
    
    private int screenWidth;
    private int screenHeight;
    
    /**
     * Creates a new settings screen.
     * 
     * @param screenManager The screen manager
     */
    public SettingsScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.buttons = new ArrayList<>();
    }
    
    @Override
    public void init(GuiRenderer guiRenderer) throws Exception {
        if (guiRenderer == null) {
            throw new IllegalArgumentException("GuiRenderer cannot be null");
        }
        
        screenWidth = guiRenderer.getScreenWidth();
        screenHeight = guiRenderer.getScreenHeight();
        
        // Safety check
        if (screenWidth <= 0 || screenHeight <= 0) {
            Logger.warn("SettingsScreen: Invalid dimensions from GuiRenderer, using defaults");
            screenWidth = 1280;
            screenHeight = 720;
        }
        
        createButtons();
        
        Logger.info("SettingsScreen initialized (size: %dx%d)", screenWidth, screenHeight);
    }
    
    private void createButtons() {
        buttons.clear();
        
        // Safety check for valid screen dimensions
        if (screenWidth <= 0 || screenHeight <= 0) {
            Logger.warn("SettingsScreen: Invalid screen dimensions, using defaults");
            screenWidth = 1280;
            screenHeight = 720;
        }
        
        float centerX = screenWidth / 2f;
        float startY = screenHeight / 2f - 80;
        
        // Graphics settings button (placeholder)
        Button graphicsBtn = new Button(centerX, startY, BUTTON_WIDTH, BUTTON_HEIGHT, "Graphics Settings");
        graphicsBtn.init();
        graphicsBtn.setEnabled(false); // Not implemented yet
        buttons.add(graphicsBtn);
        
        // Audio settings button (placeholder)
        Button audioBtn = new Button(centerX, startY + BUTTON_HEIGHT + BUTTON_SPACING, 
                                     BUTTON_WIDTH, BUTTON_HEIGHT, "Audio Settings");
        audioBtn.init();
        audioBtn.setEnabled(false); // Not implemented yet
        buttons.add(audioBtn);
        
        // Controls button (placeholder)
        Button controlsBtn = new Button(centerX, startY + (BUTTON_HEIGHT + BUTTON_SPACING) * 2,
                                        BUTTON_WIDTH, BUTTON_HEIGHT, "Controls");
        controlsBtn.init();
        controlsBtn.setEnabled(false); // Not implemented yet
        buttons.add(controlsBtn);
        
        // Done/Back button
        Button doneBtn = new Button(centerX, startY + (BUTTON_HEIGHT + BUTTON_SPACING) * 3 + 20,
                                    BUTTON_WIDTH, BUTTON_HEIGHT, "Done");
        doneBtn.init();
        doneBtn.setOnClick(() -> {
            Logger.info("Settings: Done clicked");
            screenManager.setState(GameState.MAIN_MENU);
        });
        buttons.add(doneBtn);
    }
    
    @Override
    public void onShow() {
        Logger.debug("SettingsScreen shown");
    }
    
    @Override
    public void onHide() {
        Logger.debug("SettingsScreen hidden");
    }
    
    @Override
    public void input(Window window, InputManager inputManager) {
        // Buttons handle their own input in update
    }
    
    @Override
    public void update(float deltaTime) {
        // Buttons need InputManager to update, which is provided by VoxelGame
        // This method is called by ScreenManager, but buttons are updated
        // in the overloaded method called by VoxelGame
    }
    
    /**
     * Updates with input manager (called by VoxelGame).
     * 
     * @param deltaTime Time since last update
     * @param inputManager The input manager
     */
    public void update(float deltaTime, InputManager inputManager) {
        if (inputManager != null) {
            for (Button button : buttons) {
                button.update(inputManager);
            }
        }
    }
    
    @Override
    public void render(GuiRenderer guiRenderer) {
        try {
            if (guiRenderer == null || screenWidth <= 0 || screenHeight <= 0) {
                Logger.warn("SettingsScreen: Cannot render - invalid state (guiRenderer=%s, size=%dx%d)", 
                           guiRenderer != null ? "ok" : "null", screenWidth, screenHeight);
                return; // Safety check
            }
            
            // Draw dark background
            guiRenderer.drawRect(0, 0, screenWidth, screenHeight, 0.1f, 0.1f, 0.15f, 1.0f);
            
            // Draw title
            guiRenderer.setFontScale(3.0f);
            guiRenderer.drawTextCentered("SETTINGS", screenWidth / 2f, 80, 1.0f, 1.0f, 1.0f);
            
            // Draw buttons (they render their own text)
            guiRenderer.setFontScale(2.0f);
            if (buttons != null) {
                for (Button button : buttons) {
                    if (button != null) {
                        button.render(guiRenderer);
                    }
                }
            }
            
            // Draw "coming soon" notice
            guiRenderer.setFontScale(1.5f);
            guiRenderer.drawTextCentered("MORE OPTIONS COMING SOON", screenWidth / 2f, screenHeight - 50, 0.5f, 0.5f, 0.5f);
        } catch (Exception e) {
            Logger.error("SettingsScreen render error: %s", e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void resize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        createButtons();
    }
    
    @Override
    public void cleanup() {
        buttons.clear();
    }
    
    public List<Button> getButtons() {
        return buttons;
    }
}

