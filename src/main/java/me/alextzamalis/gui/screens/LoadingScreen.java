package me.alextzamalis.gui.screens;

import me.alextzamalis.core.Window;
import me.alextzamalis.gui.GuiRenderer;
import me.alextzamalis.gui.Screen;
import me.alextzamalis.input.InputManager;
import me.alextzamalis.util.Logger;

/**
 * Loading screen shown while generating/loading a world.
 * 
 * <p>Features:
 * <ul>
 *   <li>Progress bar</li>
 *   <li>Loading status text</li>
 *   <li>Animated loading indicator</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class LoadingScreen implements Screen {
    
    /** Current loading progress (0.0 to 1.0). */
    private float progress;
    
    /** Current loading status message. */
    private String statusMessage;
    
    /** Animation time for loading indicator. */
    private float animationTime;
    
    /** Callback when loading is complete. */
    private Runnable onLoadingComplete;
    
    private int screenWidth;
    private int screenHeight;
    
    /**
     * Creates a new loading screen.
     */
    public LoadingScreen() {
        this.progress = 0;
        this.statusMessage = "Loading...";
        this.animationTime = 0;
    }
    
    /**
     * Sets the loading complete callback.
     * 
     * @param callback The callback
     */
    public void setOnLoadingComplete(Runnable callback) {
        this.onLoadingComplete = callback;
    }
    
    /**
     * Sets the current progress.
     * 
     * @param progress Progress from 0.0 to 1.0
     */
    public void setProgress(float progress) {
        this.progress = Math.max(0, Math.min(1, progress));
    }
    
    /**
     * Gets the current progress.
     * 
     * @return Progress from 0.0 to 1.0
     */
    public float getProgress() {
        return progress;
    }
    
    /**
     * Sets the status message.
     * 
     * @param message The message to display
     */
    public void setStatusMessage(String message) {
        this.statusMessage = message;
        Logger.debug("Loading: %s", message);
    }
    
    /**
     * Marks loading as complete.
     */
    public void complete() {
        progress = 1.0f;
        statusMessage = "Done!";
        
        if (onLoadingComplete != null) {
            onLoadingComplete.run();
        }
    }
    
    @Override
    public void init(GuiRenderer guiRenderer) throws Exception {
        screenWidth = guiRenderer.getScreenWidth();
        screenHeight = guiRenderer.getScreenHeight();
        
        Logger.info("LoadingScreen initialized");
    }
    
    @Override
    public void onShow() {
        progress = 0;
        statusMessage = "Loading...";
        animationTime = 0;
        Logger.debug("LoadingScreen shown");
    }
    
    @Override
    public void onHide() {
        Logger.debug("LoadingScreen hidden");
    }
    
    @Override
    public void input(Window window, InputManager inputManager) {
        // No input on loading screen
    }
    
    @Override
    public void update(float deltaTime) {
        animationTime += deltaTime;
    }
    
    @Override
    public void render(GuiRenderer guiRenderer) {
        // Draw dark background
        guiRenderer.drawRect(0, 0, screenWidth, screenHeight, 0.05f, 0.05f, 0.08f, 1.0f);
        
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;
        
        // Draw loading title area
        guiRenderer.drawRect(centerX - 200, centerY - 100, 400, 50, 0.15f, 0.15f, 0.2f, 0.8f);
        
        // Progress bar background
        float barWidth = 400;
        float barHeight = 20;
        float barX = centerX - barWidth / 2;
        float barY = centerY;
        
        guiRenderer.drawRect(barX - 2, barY - 2, barWidth + 4, barHeight + 4, 0.3f, 0.3f, 0.3f, 1.0f);
        guiRenderer.drawRect(barX, barY, barWidth, barHeight, 0.1f, 0.1f, 0.1f, 1.0f);
        
        // Progress bar fill
        float fillWidth = barWidth * progress;
        if (fillWidth > 0) {
            // Gradient-like effect with multiple layers
            guiRenderer.drawRect(barX, barY, fillWidth, barHeight, 0.2f, 0.6f, 0.2f, 1.0f);
            guiRenderer.drawRect(barX, barY, fillWidth, barHeight / 2, 0.3f, 0.7f, 0.3f, 0.5f);
        }
        
        // Animated loading dots
        int numDots = 3;
        float dotPhase = animationTime * 2;
        for (int i = 0; i < numDots; i++) {
            float dotAlpha = (float) (0.3f + 0.7f * Math.max(0, Math.sin(dotPhase - i * 0.5f)));
            float dotY = centerY + 50 + (float) Math.sin(dotPhase - i * 0.5f) * 5;
            guiRenderer.drawRect(centerX - 30 + i * 30, dotY, 15, 15, 0.5f, 0.5f, 0.5f, dotAlpha);
        }
        
        // Status message area
        guiRenderer.drawRect(centerX - 150, centerY + 80, 300, 25, 0.2f, 0.2f, 0.2f, 0.5f);
        
        // Progress percentage
        guiRenderer.drawRect(centerX - 30, barY + barHeight + 10, 60, 20, 0.2f, 0.2f, 0.2f, 0.5f);
    }
    
    @Override
    public void resize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }
    
    @Override
    public void cleanup() {
        // Nothing to clean up
    }
}

