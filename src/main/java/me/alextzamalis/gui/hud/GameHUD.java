package me.alextzamalis.gui.hud;

import me.alextzamalis.graphics.Texture;
import me.alextzamalis.graphics.TextureManager;
import me.alextzamalis.gui.GuiRenderer;
import me.alextzamalis.util.Logger;

/**
 * In-game HUD (Heads-Up Display) showing player information.
 * 
 * <p>Components:
 * <ul>
 *   <li>Crosshair in center of screen</li>
 *   <li>Hotbar at bottom with block selection</li>
 *   <li>Health bar (future)</li>
 *   <li>Food bar (future)</li>
 *   <li>Experience bar (future)</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class GameHUD {
    
    /** Texture paths. */
    private static final String HOTBAR_TEXTURE = "/assets/textures/VanillaPack/gui/hud/hotbar.png";
    private static final String HOTBAR_SELECTION_TEXTURE = "/assets/textures/VanillaPack/gui/hud/hotbar_selection.png";
    
    /** Crosshair size. */
    private static final float CROSSHAIR_SIZE = 16;
    private static final float CROSSHAIR_THICKNESS = 2;
    
    /** Hotbar dimensions. */
    private static final float HOTBAR_SCALE = 2.0f;
    private static final int HOTBAR_SLOTS = 9;
    private static final float SLOT_SIZE = 20 * HOTBAR_SCALE;
    
    /** Textures. */
    private Texture hotbarTexture;
    private Texture hotbarSelectionTexture;
    
    /** Currently selected hotbar slot (0-8). */
    private int selectedSlot;
    
    /** Screen dimensions. */
    private int screenWidth;
    private int screenHeight;
    
    /** Whether HUD is visible. */
    private boolean visible;
    
    /**
     * Creates a new game HUD.
     */
    public GameHUD() {
        this.selectedSlot = 0;
        this.visible = true;
    }
    
    /**
     * Initializes the HUD.
     * 
     * @param screenWidth Screen width
     * @param screenHeight Screen height
     */
    public void init(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        
        // Load textures
        TextureManager texManager = TextureManager.getInstance();
        hotbarTexture = texManager.getTexture(HOTBAR_TEXTURE);
        hotbarSelectionTexture = texManager.getTexture(HOTBAR_SELECTION_TEXTURE);
        
        Logger.info("GameHUD initialized");
    }
    
    /**
     * Sets the selected hotbar slot.
     * 
     * @param slot Slot index (0-8)
     */
    public void setSelectedSlot(int slot) {
        this.selectedSlot = Math.max(0, Math.min(HOTBAR_SLOTS - 1, slot));
    }
    
    /**
     * Gets the selected hotbar slot.
     * 
     * @return Slot index (0-8)
     */
    public int getSelectedSlot() {
        return selectedSlot;
    }
    
    /**
     * Scrolls the hotbar selection.
     * 
     * @param direction Scroll direction (-1 or 1)
     */
    public void scrollSlot(int direction) {
        selectedSlot = (selectedSlot + direction + HOTBAR_SLOTS) % HOTBAR_SLOTS;
    }
    
    /**
     * Sets HUD visibility.
     * 
     * @param visible true to show HUD
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    /**
     * Checks if HUD is visible.
     * 
     * @return true if visible
     */
    public boolean isVisible() {
        return visible;
    }
    
    /**
     * Updates screen dimensions.
     * 
     * @param width New width
     * @param height New height
     */
    public void resize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }
    
    /**
     * Renders the HUD.
     * 
     * @param renderer The GUI renderer
     */
    public void render(GuiRenderer renderer) {
        if (!visible) return;
        
        renderCrosshair(renderer);
        renderHotbar(renderer);
    }
    
    /**
     * Renders the crosshair.
     */
    private void renderCrosshair(GuiRenderer renderer) {
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;
        
        // Horizontal line
        renderer.drawRect(
            centerX - CROSSHAIR_SIZE / 2,
            centerY - CROSSHAIR_THICKNESS / 2,
            CROSSHAIR_SIZE,
            CROSSHAIR_THICKNESS,
            1.0f, 1.0f, 1.0f, 0.8f
        );
        
        // Vertical line
        renderer.drawRect(
            centerX - CROSSHAIR_THICKNESS / 2,
            centerY - CROSSHAIR_SIZE / 2,
            CROSSHAIR_THICKNESS,
            CROSSHAIR_SIZE,
            1.0f, 1.0f, 1.0f, 0.8f
        );
    }
    
    /**
     * Renders the hotbar.
     */
    private void renderHotbar(GuiRenderer renderer) {
        // Hotbar texture is 182x22 pixels
        float hotbarWidth = 182 * HOTBAR_SCALE;
        float hotbarHeight = 22 * HOTBAR_SCALE;
        float hotbarX = (screenWidth - hotbarWidth) / 2f;
        float hotbarY = screenHeight - hotbarHeight - 5;
        
        // Draw hotbar background
        if (hotbarTexture != null) {
            renderer.drawTexture(hotbarTexture, hotbarX, hotbarY, hotbarWidth, hotbarHeight);
        } else {
            // Fallback: draw rectangles for slots
            for (int i = 0; i < HOTBAR_SLOTS; i++) {
                float slotX = hotbarX + 3 * HOTBAR_SCALE + i * SLOT_SIZE;
                float slotY = hotbarY + 3 * HOTBAR_SCALE;
                renderer.drawRect(slotX, slotY, SLOT_SIZE - 2, SLOT_SIZE - 2, 0.2f, 0.2f, 0.2f, 0.8f);
            }
        }
        
        // Draw selection highlight
        float selectionWidth = 24 * HOTBAR_SCALE;
        float selectionHeight = 24 * HOTBAR_SCALE;
        float selectionX = hotbarX + (selectedSlot * SLOT_SIZE) - 1 * HOTBAR_SCALE;
        float selectionY = hotbarY - 1 * HOTBAR_SCALE;
        
        if (hotbarSelectionTexture != null) {
            renderer.drawTexture(hotbarSelectionTexture, selectionX, selectionY, selectionWidth, selectionHeight);
        } else {
            // Fallback: draw highlight rectangle
            renderer.drawRect(selectionX, selectionY, selectionWidth, selectionHeight, 1.0f, 1.0f, 1.0f, 0.3f);
        }
    }
    
    /**
     * Cleans up resources.
     */
    public void cleanup() {
        // Textures are managed by TextureManager
    }
}


