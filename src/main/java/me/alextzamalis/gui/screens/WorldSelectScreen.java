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

/**
 * Screen for selecting or creating worlds.
 * 
 * <p>Features:
 * <ul>
 *   <li>List of existing worlds loaded from disk</li>
 *   <li>Create New World button</li>
 *   <li>Play Selected World button</li>
 *   <li>Delete World button</li>
 *   <li>Back button</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class WorldSelectScreen implements Screen {
    
    private static final float BUTTON_WIDTH = 200;
    private static final float BUTTON_HEIGHT = 40;
    
    private final ScreenManager screenManager;
    private final WorldSaveManager saveManager;
    
    /** Callback when a world is selected to play. */
    private WorldSelectedCallback onWorldSelected;
    
    /** Menu buttons. */
    private final List<Button> buttons;
    
    /** World list entries loaded from disk. */
    private List<WorldMetadata> worldEntries;
    
    /** Currently selected world index. */
    private int selectedWorldIndex = -1;
    
    /** Track mouse press state for click detection. */
    private boolean wasMousePressed = false;
    
    private int screenWidth;
    private int screenHeight;
    
    /**
     * Callback interface for world selection.
     */
    public interface WorldSelectedCallback {
        void onWorldSelected(String worldName, long seed);
    }
    
    /**
     * Creates a new world select screen.
     * 
     * @param screenManager The screen manager
     */
    public WorldSelectScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.buttons = new ArrayList<>();
        this.worldEntries = new ArrayList<>();
        this.saveManager = WorldSaveManager.getInstance();
    }
    
    /**
     * Sets the world selected callback.
     * 
     * @param callback The callback
     */
    public void setOnWorldSelected(WorldSelectedCallback callback) {
        this.onWorldSelected = callback;
    }
    
    @Override
    public void init(GuiRenderer guiRenderer) throws Exception {
        screenWidth = guiRenderer.getScreenWidth();
        screenHeight = guiRenderer.getScreenHeight();
        
        createButtons();
        
        Logger.info("WorldSelectScreen initialized");
    }
    
    private void createButtons() {
        buttons.clear();
        
        float bottomY = screenHeight - 60;
        float buttonSpacing = 10;
        
        // Create New World button
        Button createBtn = new Button(screenWidth / 2f - BUTTON_WIDTH - buttonSpacing, bottomY,
                                      BUTTON_WIDTH, BUTTON_HEIGHT, "Create New World");
        createBtn.init();
        createBtn.setOnClick(() -> {
            Logger.info("Create New World clicked");
            screenManager.setState(GameState.WORLD_CREATE);
        });
        buttons.add(createBtn);
        
        // Play Selected button
        Button playBtn = new Button(screenWidth / 2f, bottomY, BUTTON_WIDTH, BUTTON_HEIGHT, "Play Selected");
        playBtn.init();
        playBtn.setOnClick(() -> {
            if (selectedWorldIndex >= 0 && selectedWorldIndex < worldEntries.size()) {
                WorldMetadata entry = worldEntries.get(selectedWorldIndex);
                Logger.info("Playing world: %s (seed: %d)", entry.getName(), entry.getSeed());
                if (onWorldSelected != null) {
                    onWorldSelected.onWorldSelected(entry.getName(), entry.getSeed());
                }
            } else {
                Logger.warn("No world selected");
            }
        });
        buttons.add(playBtn);
        
        // Delete button
        Button deleteBtn = new Button(screenWidth / 2f + BUTTON_WIDTH + buttonSpacing, bottomY - BUTTON_HEIGHT - 10,
                                     BUTTON_WIDTH, BUTTON_HEIGHT, "Delete World");
        deleteBtn.init();
        deleteBtn.setOnClick(() -> {
            if (selectedWorldIndex >= 0 && selectedWorldIndex < worldEntries.size()) {
                WorldMetadata entry = worldEntries.get(selectedWorldIndex);
                Logger.info("Deleting world: %s", entry.getName());
                if (saveManager.deleteWorld(entry.getName())) {
                    refreshWorldList();
                    selectedWorldIndex = -1;
                }
            }
        });
        buttons.add(deleteBtn);
        
        // Back button
        Button backBtn = new Button(screenWidth / 2f + BUTTON_WIDTH + buttonSpacing, bottomY,
                                    BUTTON_WIDTH, BUTTON_HEIGHT, "Cancel");
        backBtn.init();
        backBtn.setOnClick(() -> {
            Logger.info("Back to main menu");
            screenManager.setState(GameState.MAIN_MENU);
        });
        buttons.add(backBtn);
    }
    
    /**
     * Refreshes the world list from disk.
     */
    public void refreshWorldList() {
        worldEntries = saveManager.listWorlds();
        Logger.info("Found %d saved worlds", worldEntries.size());
    }
    
    @Override
    public void onShow() {
        selectedWorldIndex = -1;
        refreshWorldList();
        Logger.debug("WorldSelectScreen shown");
    }
    
    @Override
    public void onHide() {
        Logger.debug("WorldSelectScreen hidden");
    }
    
    @Override
    public void input(Window window, InputManager inputManager) {
        // Check for world entry clicks
        double mouseX = inputManager.getMouseX();
        double mouseY = inputManager.getMouseY();
        boolean mousePressed = inputManager.isMouseButtonPressed(0);
        
        // Detect click (mouse release)
        if (wasMousePressed && !mousePressed) {
            // Check if clicking on a world entry
            float listStartY = 100;
            float entryHeight = 50;
            float listX = screenWidth / 2f - 250;
            float listWidth = 500;
            
            for (int i = 0; i < worldEntries.size(); i++) {
                float entryY = listStartY + i * (entryHeight + 5);
                if (mouseX >= listX && mouseX <= listX + listWidth &&
                    mouseY >= entryY && mouseY <= entryY + entryHeight) {
                    selectedWorldIndex = i;
                    Logger.debug("Selected world index: %d", i);
                    break;
                }
            }
        }
        
        wasMousePressed = mousePressed;
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
        guiRenderer.setFontScale(3.0f);
        guiRenderer.drawTextCentered("SELECT WORLD", screenWidth / 2f, 40, 1.0f, 1.0f, 1.0f);
        
        // Draw world list area
        float listStartY = 100;
        float entryHeight = 50;
        float listX = screenWidth / 2f - 250;
        float listWidth = 500;
        
        // List background
        float listHeight = Math.max(200, worldEntries.size() * (entryHeight + 5) + 20);
        guiRenderer.drawRect(listX - 10, listStartY - 10, listWidth + 20, listHeight, 
                            0.15f, 0.15f, 0.2f, 0.9f);
        
        // Draw world entries
        guiRenderer.setFontScale(2.0f);
        for (int i = 0; i < worldEntries.size(); i++) {
            WorldMetadata entry = worldEntries.get(i);
            float entryY = listStartY + i * (entryHeight + 5);
            
            // Entry background (highlight if selected)
            if (i == selectedWorldIndex) {
                guiRenderer.drawRect(listX, entryY, listWidth, entryHeight, 0.3f, 0.3f, 0.5f, 1.0f);
            } else {
                guiRenderer.drawRect(listX, entryY, listWidth, entryHeight, 0.2f, 0.2f, 0.25f, 1.0f);
            }
            
            // World icon placeholder (green for creative, brown for survival)
            if (entry.getGameMode() == WorldMetadata.GAME_MODE_CREATIVE) {
                guiRenderer.drawRect(listX + 5, entryY + 5, 40, 40, 0.4f, 0.6f, 0.4f, 1.0f);
            } else {
                guiRenderer.drawRect(listX + 5, entryY + 5, 40, 40, 0.6f, 0.4f, 0.3f, 1.0f);
            }
            
            // World name
            guiRenderer.drawText(entry.getName().toUpperCase(), listX + 55, entryY + 8, 1.0f, 1.0f, 1.0f);
            
            // World info
            guiRenderer.setFontScale(1.5f);
            String info = entry.getGameModeString() + " - " + entry.getLastPlayedString();
            guiRenderer.drawText(info.toUpperCase(), listX + 55, entryY + 30, 0.6f, 0.6f, 0.6f);
            guiRenderer.setFontScale(2.0f);
        }
        
        // Draw "no worlds" message if empty
        if (worldEntries.isEmpty()) {
            guiRenderer.drawTextCentered("NO WORLDS FOUND", screenWidth / 2f, listStartY + 50, 0.5f, 0.5f, 0.5f);
            guiRenderer.setFontScale(1.5f);
            guiRenderer.drawTextCentered("CLICK CREATE NEW WORLD TO START", screenWidth / 2f, listStartY + 80, 0.4f, 0.4f, 0.4f);
            guiRenderer.setFontScale(2.0f);
        }
        
        // Draw buttons (they render their own text)
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
