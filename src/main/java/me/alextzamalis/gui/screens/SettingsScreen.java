package me.alextzamalis.gui.screens;

import me.alextzamalis.config.GameSettings;
import me.alextzamalis.config.SettingsManager;
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
    private static final float BUTTON_HEIGHT = 35;
    private static final float BUTTON_SPACING = 8;
    private static final float LABEL_WIDTH = 200;
    
    private final ScreenManager screenManager;
    private final SettingsManager settingsManager;
    private final List<Button> buttons;
    
    private int screenWidth;
    private int screenHeight;
    
    // Settings references
    private GameSettings settings;
    
    // Setting value buttons (for cycling/incrementing)
    private Button vSyncButton;
    private Button fpsButton;
    private Button upsButton;
    private Button viewDistanceButton;
    private Button renderQualityButton;
    private Button mouseSensitivityButton;
    private Button movementSpeedButton;
    private Button showFPSButton;
    private Button showDebugButton;
    
    /**
     * Creates a new settings screen.
     * 
     * @param screenManager The screen manager
     */
    public SettingsScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.settingsManager = SettingsManager.getInstance();
        this.settings = settingsManager.getSettings();
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
        
        if (settings == null) {
            Logger.error("SettingsScreen: Settings not loaded!");
            return;
        }
        
        float leftX = screenWidth / 2f - 250;
        float rightX = screenWidth / 2f + 50;
        float startY = 150;
        float y = startY;
        
        // ========== PERFORMANCE SETTINGS ==========
        // VSync toggle
        vSyncButton = createToggleButton(rightX, y, "VSync: " + (settings.isVSync() ? "ON" : "OFF"), 
                                        () -> {
                                            settings.setVSync(!settings.isVSync());
                                            updateButtonTexts();
                                        });
        buttons.add(vSyncButton);
        y += BUTTON_HEIGHT + BUTTON_SPACING;
        
        // FPS setting (30, 60, 120, 144, 240, Unlimited)
        fpsButton = createValueButton(rightX, y, "FPS: " + (settings.getTargetFPS() == 0 ? "Unlimited" : String.valueOf(settings.getTargetFPS())),
                                     new int[]{30, 60, 120, 144, 240, 0},
                                     () -> settings.getTargetFPS(),
                                     (value) -> settings.setTargetFPS(value));
        buttons.add(fpsButton);
        y += BUTTON_HEIGHT + BUTTON_SPACING;
        
        // UPS setting (10, 20, 30, 60)
        upsButton = createValueButton(rightX, y, "UPS: " + settings.getTargetUPS(),
                                     new int[]{10, 20, 30, 60},
                                     () -> settings.getTargetUPS(),
                                     (value) -> settings.setTargetUPS(value));
        buttons.add(upsButton);
        y += BUTTON_HEIGHT + BUTTON_SPACING;
        
        // ========== RENDERING SETTINGS ==========
        y += 20; // Spacing
        
        // View Distance (2-12 chunks)
        viewDistanceButton = createValueButton(rightX, y, "View Distance: " + settings.getViewDistance() + " chunks",
                                              new int[]{2, 3, 4, 6, 8, 10, 12},
                                              () -> settings.getViewDistance(),
                                              (value) -> settings.setViewDistance(value));
        buttons.add(viewDistanceButton);
        y += BUTTON_HEIGHT + BUTTON_SPACING;
        
        // Render Quality
        renderQualityButton = createQualityButton(rightX, y);
        buttons.add(renderQualityButton);
        y += BUTTON_HEIGHT + BUTTON_SPACING;
        
        // ========== GAMEPLAY SETTINGS ==========
        y += 20; // Spacing
        
        // Mouse Sensitivity (0.1 - 5.0)
        mouseSensitivityButton = createFloatButton(rightX, y, "Mouse Sensitivity: " + String.format("%.1f", settings.getMouseSensitivity()),
                                                 0.1f, 5.0f, 0.1f,
                                                 () -> settings.getMouseSensitivity(),
                                                 (value) -> settings.setMouseSensitivity((float) value));
        buttons.add(mouseSensitivityButton);
        y += BUTTON_HEIGHT + BUTTON_SPACING;
        
        // Movement Speed (0.1 - 5.0)
        movementSpeedButton = createFloatButton(rightX, y, "Movement Speed: " + String.format("%.1f", settings.getMovementSpeed()),
                                              0.1f, 5.0f, 0.1f,
                                              () -> settings.getMovementSpeed(),
                                              (value) -> settings.setMovementSpeed((float) value));
        buttons.add(movementSpeedButton);
        y += BUTTON_HEIGHT + BUTTON_SPACING;
        
        // ========== DISPLAY SETTINGS ==========
        y += 20; // Spacing
        
        // Show FPS toggle
        showFPSButton = createToggleButton(rightX, y, "Show FPS: " + (settings.isShowFPS() ? "ON" : "OFF"),
                                          () -> {
                                              settings.setShowFPS(!settings.isShowFPS());
                                              updateButtonTexts();
                                          });
        buttons.add(showFPSButton);
        y += BUTTON_HEIGHT + BUTTON_SPACING;
        
        // Show Debug toggle
        showDebugButton = createToggleButton(rightX, y, "Show Debug: " + (settings.isShowDebug() ? "ON" : "OFF"),
                                            () -> {
                                                settings.setShowDebug(!settings.isShowDebug());
                                                updateButtonTexts();
                                            });
        buttons.add(showDebugButton);
        y += BUTTON_HEIGHT + BUTTON_SPACING;
        
        // ========== ACTION BUTTONS ==========
        y += 30; // Spacing
        
        // Save button
        Button saveBtn = new Button(screenWidth / 2f - 100, y, 180, BUTTON_HEIGHT, "Save Settings");
        saveBtn.init();
        saveBtn.setOnClick(() -> {
            settingsManager.saveSettings();
            Logger.info("Settings saved!");
            screenManager.setState(GameState.MAIN_MENU);
        });
        buttons.add(saveBtn);
        
        // Cancel button
        Button cancelBtn = new Button(screenWidth / 2f + 100, y, 180, BUTTON_HEIGHT, "Cancel");
        cancelBtn.init();
        cancelBtn.setOnClick(() -> {
            // Reload settings to discard changes
            settingsManager.loadSettings();
            updateButtonTexts();
            Logger.info("Settings changes discarded");
            screenManager.setState(GameState.MAIN_MENU);
        });
        buttons.add(cancelBtn);
    }
    
    /**
     * Creates a toggle button for boolean settings.
     */
    private Button createToggleButton(float x, float y, String text, Runnable onToggle) {
        Button btn = new Button(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, text);
        btn.init();
        btn.setOnClick(onToggle);
        return btn;
    }
    
    /**
     * Creates a button that cycles through integer values.
     */
    private Button createValueButton(float x, float y, String text, int[] values,
                                    java.util.function.IntSupplier getCurrentValue,
                                    java.util.function.IntConsumer onValueChange) {
        Button btn = new Button(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, text);
        btn.init();
        btn.setOnClick(() -> {
            // Get current value and find its index
            int currentValue = getCurrentValue.getAsInt();
            int currentIndex = 0;
            for (int i = 0; i < values.length; i++) {
                if (values[i] == currentValue) {
                    currentIndex = i;
                    break;
                }
            }
            int nextIndex = (currentIndex + 1) % values.length;
            onValueChange.accept(values[nextIndex]);
            updateButtonTexts();
        });
        return btn;
    }
    
    /**
     * Creates a button for float values (increments by step).
     */
    private Button createFloatButton(float x, float y, String text, float min, float max, float step,
                                    java.util.function.Supplier<Float> getCurrentValue,
                                    java.util.function.DoubleConsumer onValueChange) {
        Button btn = new Button(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, text);
        btn.init();
        btn.setOnClick(() -> {
            float currentValue = getCurrentValue.get();
            float newValue = currentValue + step;
            if (newValue > max) {
                newValue = min; // Wrap around
            }
            onValueChange.accept((double) newValue);
            updateButtonTexts();
        });
        return btn;
    }
    
    /**
     * Creates render quality button.
     */
    private Button createQualityButton(float x, float y) {
        GameSettings.RenderQuality[] qualities = GameSettings.RenderQuality.values();
        
        Button btn = new Button(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, 
                               "Render Quality: " + settings.getRenderQuality().name());
        btn.init();
        btn.setOnClick(() -> {
            // Get current quality and find its index
            GameSettings.RenderQuality current = settings.getRenderQuality();
            int currentIndex = 0;
            for (int i = 0; i < qualities.length; i++) {
                if (qualities[i] == current) {
                    currentIndex = i;
                    break;
                }
            }
            int nextIndex = (currentIndex + 1) % qualities.length;
            settings.setRenderQuality(qualities[nextIndex]);
            updateButtonTexts();
        });
        return btn;
    }
    
    /**
     * Updates all button texts to reflect current settings.
     */
    private void updateButtonTexts() {
        if (settings == null) return;
        
        if (vSyncButton != null) {
            vSyncButton.setText("VSync: " + (settings.isVSync() ? "ON" : "OFF"));
        }
        if (fpsButton != null) {
            fpsButton.setText("FPS: " + (settings.getTargetFPS() == 0 ? "Unlimited" : String.valueOf(settings.getTargetFPS())));
        }
        if (upsButton != null) {
            upsButton.setText("UPS: " + settings.getTargetUPS());
        }
        if (viewDistanceButton != null) {
            viewDistanceButton.setText("View Distance: " + settings.getViewDistance() + " chunks");
        }
        if (renderQualityButton != null) {
            renderQualityButton.setText("Render Quality: " + settings.getRenderQuality().name());
        }
        if (mouseSensitivityButton != null) {
            mouseSensitivityButton.setText("Mouse Sensitivity: " + String.format("%.1f", settings.getMouseSensitivity()));
        }
        if (movementSpeedButton != null) {
            movementSpeedButton.setText("Movement Speed: " + String.format("%.1f", settings.getMovementSpeed()));
        }
        if (showFPSButton != null) {
            showFPSButton.setText("Show FPS: " + (settings.isShowFPS() ? "ON" : "OFF"));
        }
        if (showDebugButton != null) {
            showDebugButton.setText("Show Debug: " + (settings.isShowDebug() ? "ON" : "OFF"));
        }
    }
    
    @Override
    public void onShow() {
        // Reload settings to get current values
        settings = settingsManager.getSettings();
        updateButtonTexts();
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
            guiRenderer.drawTextCentered("SETTINGS", screenWidth / 2f, 60, 1.0f, 1.0f, 1.0f);
            
            // Draw section labels
            float leftX = screenWidth / 2f - 250;
            float startY = 150;
            float y = startY;
            
            guiRenderer.setFontScale(2.0f);
            guiRenderer.drawText("PERFORMANCE", leftX, y - 5, 0.7f, 0.7f, 0.9f);
            y += (BUTTON_HEIGHT + BUTTON_SPACING) * 3 + 20;
            guiRenderer.drawText("RENDERING", leftX, y - 5, 0.7f, 0.7f, 0.9f);
            y += (BUTTON_HEIGHT + BUTTON_SPACING) * 2 + 20;
            guiRenderer.drawText("GAMEPLAY", leftX, y - 5, 0.7f, 0.7f, 0.9f);
            y += (BUTTON_HEIGHT + BUTTON_SPACING) * 2 + 20;
            guiRenderer.drawText("DISPLAY", leftX, y - 5, 0.7f, 0.7f, 0.9f);
            
            // Draw buttons (they render their own text)
            guiRenderer.setFontScale(1.8f);
            if (buttons != null) {
                for (Button button : buttons) {
                    if (button != null) {
                        button.render(guiRenderer);
                    }
                }
            }
            
            // Draw hint
            guiRenderer.setFontScale(1.3f);
            guiRenderer.drawTextCentered("Click buttons to change values. Changes take effect after restart.", 
                                        screenWidth / 2f, screenHeight - 30, 0.5f, 0.5f, 0.5f);
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

