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
import java.util.Random;

/**
 * Screen for creating a new world.
 * 
 * <p>Features:
 * <ul>
 *   <li>World name input</li>
 *   <li>Seed input (or random)</li>
 *   <li>Game mode selection (future)</li>
 *   <li>Create button</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class WorldCreateScreen implements Screen {
    
    private static final float BUTTON_WIDTH = 200;
    private static final float BUTTON_HEIGHT = 40;
    
    private final ScreenManager screenManager;
    private final Random random;
    
    /** Callback when world creation is confirmed. */
    private WorldCreateCallback onWorldCreate;
    
    /** Menu buttons. */
    private final List<Button> buttons;
    
    /** Current world name (placeholder - will use text input later). */
    private String worldName = "New World";
    
    /** Current seed (0 = random). */
    private long worldSeed = 0;
    
    /** Whether to use a random seed. */
    private boolean useRandomSeed = true;
    
    private int screenWidth;
    private int screenHeight;
    
    /**
     * Callback interface for world creation.
     */
    public interface WorldCreateCallback {
        void onWorldCreate(String worldName, long seed);
    }
    
    /**
     * Creates a new world create screen.
     * 
     * @param screenManager The screen manager
     */
    public WorldCreateScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.buttons = new ArrayList<>();
        this.random = new Random();
    }
    
    /**
     * Sets the world create callback.
     * 
     * @param callback The callback
     */
    public void setOnWorldCreate(WorldCreateCallback callback) {
        this.onWorldCreate = callback;
    }
    
    @Override
    public void init(GuiRenderer guiRenderer) throws Exception {
        screenWidth = guiRenderer.getScreenWidth();
        screenHeight = guiRenderer.getScreenHeight();
        
        createButtons();
        
        Logger.info("WorldCreateScreen initialized");
    }
    
    private void createButtons() {
        buttons.clear();
        
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;
        
        // Random seed toggle button
        Button randomSeedBtn = new Button(centerX, centerY + 50, BUTTON_WIDTH * 1.5f, BUTTON_HEIGHT,
                                         useRandomSeed ? "Seed: Random" : "Seed: Custom");
        randomSeedBtn.init();
        randomSeedBtn.setOnClick(() -> {
            useRandomSeed = !useRandomSeed;
            if (useRandomSeed) {
                worldSeed = 0;
            }
            createButtons(); // Refresh button text
        });
        buttons.add(randomSeedBtn);
        
        // Create World button
        Button createBtn = new Button(centerX - BUTTON_WIDTH / 2 - 10, screenHeight - 60,
                                      BUTTON_WIDTH, BUTTON_HEIGHT, "Create World");
        createBtn.init();
        createBtn.setOnClick(() -> {
            // Generate random seed if needed
            long finalSeed = useRandomSeed ? random.nextLong() : worldSeed;
            
            Logger.info("Creating world: %s (seed: %d)", worldName, finalSeed);
            
            if (onWorldCreate != null) {
                onWorldCreate.onWorldCreate(worldName, finalSeed);
            }
        });
        buttons.add(createBtn);
        
        // Cancel button
        Button cancelBtn = new Button(centerX + BUTTON_WIDTH / 2 + 10, screenHeight - 60,
                                      BUTTON_WIDTH, BUTTON_HEIGHT, "Cancel");
        cancelBtn.init();
        cancelBtn.setOnClick(() -> {
            Logger.info("Cancelled world creation");
            screenManager.setState(GameState.WORLD_SELECT);
        });
        buttons.add(cancelBtn);
    }
    
    @Override
    public void onShow() {
        // Reset to defaults
        worldName = "New World " + (System.currentTimeMillis() % 1000);
        worldSeed = 0;
        useRandomSeed = true;
        createButtons();
        
        Logger.debug("WorldCreateScreen shown");
    }
    
    @Override
    public void onHide() {
        Logger.debug("WorldCreateScreen hidden");
    }
    
    @Override
    public void input(Window window, InputManager inputManager) {
        // TODO: Handle text input for world name and seed
    }
    
    @Override
    public void update(float deltaTime) {
        // Update handled in update with input manager
    }
    
    /**
     * Updates with input manager.
     */
    public void update(float deltaTime, InputManager inputManager) {
        for (Button button : buttons) {
            button.update(inputManager);
        }
    }
    
    @Override
    public void render(GuiRenderer guiRenderer) {
        // Draw background
        guiRenderer.drawRect(0, 0, screenWidth, screenHeight, 0.1f, 0.1f, 0.15f, 1.0f);
        
        // Draw title
        guiRenderer.drawRect(screenWidth / 2f - 150, 50, 300, 40, 0.2f, 0.2f, 0.3f, 0.8f);
        
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;
        
        // World name input area (placeholder)
        guiRenderer.drawRect(centerX - 200, centerY - 80, 400, 50, 0.15f, 0.15f, 0.2f, 1.0f);
        guiRenderer.drawRect(centerX - 195, centerY - 75, 390, 40, 0.25f, 0.25f, 0.3f, 1.0f);
        
        // Label placeholders
        guiRenderer.drawRect(centerX - 200, centerY - 110, 100, 20, 0.5f, 0.5f, 0.5f, 0.5f);
        
        // Seed display area (when custom)
        if (!useRandomSeed) {
            guiRenderer.drawRect(centerX - 200, centerY + 100, 400, 50, 0.15f, 0.15f, 0.2f, 1.0f);
            guiRenderer.drawRect(centerX - 195, centerY + 105, 390, 40, 0.25f, 0.25f, 0.3f, 1.0f);
        }
        
        // Draw buttons
        for (Button button : buttons) {
            button.render(guiRenderer);
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

