package me.alextzamalis.gui.widgets;

import me.alextzamalis.graphics.Texture;
import me.alextzamalis.graphics.TextureManager;
import me.alextzamalis.gui.GuiRenderer;
import me.alextzamalis.input.InputManager;

/**
 * A clickable button widget with Minecraft-style textures.
 * 
 * <p>The button has three visual states:
 * <ul>
 *   <li>Normal: Default appearance</li>
 *   <li>Hovered: When mouse is over the button</li>
 *   <li>Disabled: Grayed out, not interactive</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class Button {
    
    /** Button texture paths. */
    private static final String TEXTURE_NORMAL = "/assets/textures/VanillaPack/gui/util-widget/button.png";
    private static final String TEXTURE_HOVER = "/assets/textures/VanillaPack/gui/util-widget/button_highlighted.png";
    private static final String TEXTURE_DISABLED = "/assets/textures/VanillaPack/gui/util-widget/button_disabled.png";
    
    /** Button position and size. */
    private float x, y, width, height;
    
    /** Button text (for future text rendering). */
    private String text;
    
    /** Whether the button is enabled. */
    private boolean enabled;
    
    /** Whether the mouse is hovering over the button. */
    private boolean hovered;
    
    /** Whether the button was just clicked. */
    private boolean clicked;
    
    /** Click callback. */
    private Runnable onClick;
    
    /** Textures. */
    private Texture normalTexture;
    private Texture hoverTexture;
    private Texture disabledTexture;
    
    /** Previous mouse button state for click detection. */
    private boolean wasMousePressed;
    
    /**
     * Creates a new button.
     * 
     * @param x X position (center)
     * @param y Y position (center)
     * @param width Width
     * @param height Height
     * @param text Button text
     */
    public Button(float x, float y, float width, float height, String text) {
        this.x = x - width / 2; // Convert center to top-left
        this.y = y - height / 2;
        this.width = width;
        this.height = height;
        this.text = text;
        this.enabled = true;
        this.hovered = false;
        this.clicked = false;
        this.wasMousePressed = false;
    }
    
    /**
     * Initializes button textures.
     */
    public void init() {
        TextureManager texManager = TextureManager.getInstance();
        normalTexture = texManager.getTexture(TEXTURE_NORMAL);
        hoverTexture = texManager.getTexture(TEXTURE_HOVER);
        disabledTexture = texManager.getTexture(TEXTURE_DISABLED);
    }
    
    /**
     * Sets the click callback.
     * 
     * @param onClick Callback to run when clicked
     * @return this for chaining
     */
    public Button setOnClick(Runnable onClick) {
        this.onClick = onClick;
        return this;
    }
    
    /**
     * Updates the button state based on mouse position and clicks.
     * 
     * @param inputManager The input manager
     */
    public void update(InputManager inputManager) {
        clicked = false;
        
        if (!enabled) {
            hovered = false;
            return;
        }
        
        // Check if mouse is over button
        double mouseX = inputManager.getMouseX();
        double mouseY = inputManager.getMouseY();
        
        hovered = mouseX >= x && mouseX <= x + width &&
                  mouseY >= y && mouseY <= y + height;
        
        // Check for click (mouse released while hovering)
        boolean mousePressed = inputManager.isMouseButtonPressed(0); // Left click
        
        if (hovered && wasMousePressed && !mousePressed) {
            clicked = true;
            if (onClick != null) {
                onClick.run();
            }
        }
        
        wasMousePressed = mousePressed;
    }
    
    /**
     * Renders the button.
     * 
     * @param renderer The GUI renderer
     */
    public void render(GuiRenderer renderer) {
        Texture texture;
        
        if (!enabled) {
            texture = disabledTexture;
        } else if (hovered) {
            texture = hoverTexture;
        } else {
            texture = normalTexture;
        }
        
        if (texture != null) {
            renderer.drawTexture(texture, x, y, width, height);
        } else {
            // Fallback: draw colored rectangle
            if (!enabled) {
                renderer.drawRect(x, y, width, height, 0.3f, 0.3f, 0.3f, 1.0f);
            } else if (hovered) {
                renderer.drawRect(x, y, width, height, 0.6f, 0.6f, 0.8f, 1.0f);
            } else {
                renderer.drawRect(x, y, width, height, 0.4f, 0.4f, 0.4f, 1.0f);
            }
        }
        
        // TODO: Render text when text rendering is implemented
    }
    
    /**
     * Sets the button position (center coordinates).
     * 
     * @param x Center X
     * @param y Center Y
     */
    public void setPosition(float x, float y) {
        this.x = x - width / 2;
        this.y = y - height / 2;
    }
    
    /**
     * Sets whether the button is enabled.
     * 
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Checks if the button is enabled.
     * 
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Checks if the button was just clicked this frame.
     * 
     * @return true if clicked
     */
    public boolean isClicked() {
        return clicked;
    }
    
    /**
     * Checks if the mouse is hovering over the button.
     * 
     * @return true if hovered
     */
    public boolean isHovered() {
        return hovered;
    }
    
    /**
     * Gets the button text.
     * 
     * @return The text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Sets the button text.
     * 
     * @param text The new text
     */
    public void setText(String text) {
        this.text = text;
    }
}

