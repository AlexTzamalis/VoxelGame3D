package me.alextzamalis.gui.screens;

import me.alextzamalis.core.Window;
import me.alextzamalis.gui.GameState;
import me.alextzamalis.gui.GuiRenderer;
import me.alextzamalis.gui.Screen;
import me.alextzamalis.gui.ScreenManager;
import me.alextzamalis.gui.widgets.Button;
import me.alextzamalis.input.InputManager;
import me.alextzamalis.util.Logger;
import me.alextzamalis.world.WorldMetadata;
import me.alextzamalis.world.WorldSaveManager;

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
 *   <li>Game mode selection</li>
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
    private final WorldSaveManager saveManager;
    
    /** Callback when world creation is confirmed. */
    private WorldCreateCallback onWorldCreate;
    
    /** Menu buttons. */
    private final List<Button> buttons;
    
    /** Flag to rebuild buttons next frame (avoids concurrent modification). */
    private boolean needsButtonRebuild = false;
    
    /** Current world name. */
    private String worldName = "New World";
    
    /** Current seed (0 = random). */
    private long worldSeed = 0;
    
    /** Whether to use a random seed. */
    private boolean useRandomSeed = true;
    
    /** Selected game mode. */
    private int gameMode = WorldMetadata.GAME_MODE_CREATIVE;
    
    /** World counter for unique names. */
    private static int worldCounter = 1;
    
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
        this.saveManager = WorldSaveManager.getInstance();
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
        
        // Game mode toggle button
        Button gameModeBtn = new Button(centerX, centerY - 20, BUTTON_WIDTH * 1.5f, BUTTON_HEIGHT,
                                       gameMode == WorldMetadata.GAME_MODE_CREATIVE ? "Game Mode: Creative" : "Game Mode: Survival");
        gameModeBtn.init();
        gameModeBtn.setOnClick(() -> {
            gameMode = (gameMode == WorldMetadata.GAME_MODE_CREATIVE) 
                      ? WorldMetadata.GAME_MODE_SURVIVAL 
                      : WorldMetadata.GAME_MODE_CREATIVE;
            needsButtonRebuild = true; // Schedule rebuild for next frame
        });
        buttons.add(gameModeBtn);
        
        // Random seed toggle button
        Button randomSeedBtn = new Button(centerX, centerY + 40, BUTTON_WIDTH * 1.5f, BUTTON_HEIGHT,
                                         useRandomSeed ? "Seed: Random" : "Seed: Custom");
        randomSeedBtn.init();
        randomSeedBtn.setOnClick(() -> {
            useRandomSeed = !useRandomSeed;
            if (useRandomSeed) {
                worldSeed = 0;
            } else {
                // Generate a seed to show
                worldSeed = random.nextLong() & 0xFFFFFFFFL; // Positive seed
            }
            needsButtonRebuild = true; // Schedule rebuild for next frame
        });
        buttons.add(randomSeedBtn);
        
        // Create World button
        Button createBtn = new Button(centerX - BUTTON_WIDTH / 2 - 10, screenHeight - 60,
                                      BUTTON_WIDTH, BUTTON_HEIGHT, "Create World");
        createBtn.init();
        createBtn.setOnClick(() -> {
            // Generate random seed if needed
            long finalSeed = useRandomSeed ? random.nextLong() : worldSeed;
            
            // Create world metadata
            WorldMetadata metadata = new WorldMetadata(worldName, finalSeed);
            metadata.setGameMode(gameMode);
            
            // Save world to disk
            if (saveManager.createWorld(metadata)) {
                Logger.info("Created world: %s (seed: %d, mode: %s)", 
                           worldName, finalSeed, metadata.getGameModeString());
                
                if (onWorldCreate != null) {
                    onWorldCreate.onWorldCreate(worldName, finalSeed);
                }
            } else {
                Logger.error("Failed to create world: %s", worldName);
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
        // Generate unique world name
        worldName = "New World " + worldCounter++;
        
        // Check if name already exists and increment
        while (saveManager.worldExists(worldName)) {
            worldName = "New World " + worldCounter++;
        }
        
        worldSeed = 0;
        useRandomSeed = true;
        gameMode = WorldMetadata.GAME_MODE_CREATIVE;
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
        // Rebuild buttons if needed (deferred to avoid concurrent modification)
        if (needsButtonRebuild) {
            needsButtonRebuild = false;
            createButtons();
            return; // Skip update this frame to avoid issues
        }
        
        for (Button button : buttons) {
            button.update(inputManager);
        }
    }
    
    @Override
    public void render(GuiRenderer guiRenderer) {
        // Draw background
        guiRenderer.drawRect(0, 0, screenWidth, screenHeight, 0.1f, 0.1f, 0.15f, 1.0f);
        
        // Draw title
        guiRenderer.setFontScale(3.0f);
        guiRenderer.drawTextCentered("CREATE NEW WORLD", screenWidth / 2f, 50, 1.0f, 1.0f, 1.0f);
        
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;
        
        // World name label
        guiRenderer.setFontScale(2.0f);
        guiRenderer.drawText("WORLD NAME:", centerX - 200, centerY - 130, 0.8f, 0.8f, 0.8f);
        
        // World name input area (placeholder - shows current name)
        guiRenderer.drawRect(centerX - 200, centerY - 100, 400, 50, 0.15f, 0.15f, 0.2f, 1.0f);
        guiRenderer.drawRect(centerX - 195, centerY - 95, 390, 40, 0.25f, 0.25f, 0.3f, 1.0f);
        
        // Display world name
        guiRenderer.drawText(worldName.toUpperCase(), centerX - 185, centerY - 85, 1.0f, 1.0f, 1.0f);
        
        // Seed display area (when custom)
        if (!useRandomSeed) {
            guiRenderer.drawText("CUSTOM SEED:", centerX - 200, centerY + 80, 0.8f, 0.8f, 0.8f);
            guiRenderer.drawRect(centerX - 200, centerY + 100, 400, 50, 0.15f, 0.15f, 0.2f, 1.0f);
            guiRenderer.drawRect(centerX - 195, centerY + 105, 390, 40, 0.25f, 0.25f, 0.3f, 1.0f);
            guiRenderer.drawText(String.valueOf(worldSeed), centerX - 185, centerY + 115, 1.0f, 1.0f, 1.0f);
        }
        
        // Draw buttons (they render their own text)
        for (Button button : buttons) {
            button.render(guiRenderer);
        }
        
        // Draw hint text at bottom
        guiRenderer.setFontScale(1.5f);
        guiRenderer.drawTextCentered("TIP: RANDOM SEEDS CREATE UNIQUE WORLDS", screenWidth / 2f, screenHeight - 100, 0.5f, 0.5f, 0.5f);
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
