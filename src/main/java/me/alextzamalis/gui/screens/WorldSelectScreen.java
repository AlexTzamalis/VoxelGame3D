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
 * Screen for selecting or creating worlds.
 * 
 * <p>Features:
 * <ul>
 *   <li>List of existing worlds (future: load from disk)</li>
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
    
    /** Callback when a world is selected to play. */
    private WorldSelectedCallback onWorldSelected;
    
    /** Menu buttons. */
    private final List<Button> buttons;
    
    /** World list entries (placeholder for now). */
    private final List<WorldEntry> worldEntries;
    
    /** Currently selected world index. */
    private int selectedWorldIndex = -1;
    
    private int screenWidth;
    private int screenHeight;
    
    /**
     * Callback interface for world selection.
     */
    public interface WorldSelectedCallback {
        void onWorldSelected(String worldName, long seed);
    }
    
    /**
     * Represents a world entry in the list.
     */
    public static class WorldEntry {
        public final String name;
        public final long seed;
        public final String lastPlayed;
        
        public WorldEntry(String name, long seed, String lastPlayed) {
            this.name = name;
            this.seed = seed;
            this.lastPlayed = lastPlayed;
        }
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
        
        // Add some placeholder worlds
        worldEntries.add(new WorldEntry("Test World", 12345L, "Today"));
        worldEntries.add(new WorldEntry("Survival World", 98765L, "Yesterday"));
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
                WorldEntry entry = worldEntries.get(selectedWorldIndex);
                Logger.info("Playing world: %s (seed: %d)", entry.name, entry.seed);
                if (onWorldSelected != null) {
                    onWorldSelected.onWorldSelected(entry.name, entry.seed);
                }
            }
        });
        buttons.add(playBtn);
        
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
    
    @Override
    public void onShow() {
        selectedWorldIndex = -1;
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
        
        if (inputManager.isMouseButtonPressed(0)) {
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
                    break;
                }
            }
        }
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
        guiRenderer.drawRect(screenWidth / 2f - 150, 30, 300, 40, 0.2f, 0.2f, 0.3f, 0.8f);
        
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
        for (int i = 0; i < worldEntries.size(); i++) {
            WorldEntry entry = worldEntries.get(i);
            float entryY = listStartY + i * (entryHeight + 5);
            
            // Entry background (highlight if selected)
            if (i == selectedWorldIndex) {
                guiRenderer.drawRect(listX, entryY, listWidth, entryHeight, 0.3f, 0.3f, 0.5f, 1.0f);
            } else {
                guiRenderer.drawRect(listX, entryY, listWidth, entryHeight, 0.2f, 0.2f, 0.25f, 1.0f);
            }
            
            // World icon placeholder
            guiRenderer.drawRect(listX + 5, entryY + 5, 40, 40, 0.4f, 0.6f, 0.4f, 1.0f);
            
            // Text placeholders (will be replaced with actual text rendering)
            guiRenderer.drawRect(listX + 55, entryY + 10, 200, 15, 0.5f, 0.5f, 0.5f, 0.5f);
            guiRenderer.drawRect(listX + 55, entryY + 30, 100, 10, 0.4f, 0.4f, 0.4f, 0.5f);
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
    
    /**
     * Adds a world entry to the list.
     * 
     * @param name World name
     * @param seed World seed
     */
    public void addWorldEntry(String name, long seed) {
        worldEntries.add(new WorldEntry(name, seed, "New"));
    }
}

