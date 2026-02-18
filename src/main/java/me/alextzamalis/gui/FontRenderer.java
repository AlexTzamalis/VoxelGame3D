package me.alextzamalis.gui;

import me.alextzamalis.graphics.Texture;
import me.alextzamalis.util.Logger;

/**
 * Simple bitmap font renderer for GUI text.
 * 
 * <p>Uses a font atlas PNG where characters are arranged in a grid.
 * The default font assumes ASCII characters starting from space (32)
 * arranged in a 16x16 grid (256 characters total).
 * 
 * <p>If no font texture is available, falls back to drawing colored
 * rectangles as placeholder "blocks" for each character.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class FontRenderer {
    
    /** Default character width in the atlas. */
    private static final int CHAR_WIDTH = 8;
    
    /** Default character height in the atlas. */
    private static final int CHAR_HEIGHT = 8;
    
    /** Characters per row in the atlas. */
    private static final int CHARS_PER_ROW = 16;
    
    /** First ASCII character in the atlas (space). */
    private static final int FIRST_CHAR = 32;
    
    /** Font texture (null if not loaded). */
    private Texture fontTexture;
    
    /** Whether we have a valid font texture. */
    private boolean hasFontTexture;
    
    /** Scale factor for rendering. */
    private float scale = 2.0f;
    
    /** Reference to GUI renderer for drawing. */
    private GuiRenderer guiRenderer;
    
    /**
     * Creates a new font renderer.
     */
    public FontRenderer() {
        this.hasFontTexture = false;
    }
    
    /**
     * Initializes the font renderer.
     * 
     * @param guiRenderer The GUI renderer to use
     */
    public void init(GuiRenderer guiRenderer) {
        this.guiRenderer = guiRenderer;
        
        // Try to load font texture
        try {
            // For now, we don't have a font texture, so we'll use fallback
            // fontTexture = TextureManager.getInstance().getTexture("/assets/textures/font.png");
            // hasFontTexture = (fontTexture != null);
            hasFontTexture = false;
        } catch (Exception e) {
            Logger.warn("Could not load font texture, using fallback rendering");
            hasFontTexture = false;
        }
        
        Logger.info("FontRenderer initialized (fallback mode: %s)", !hasFontTexture);
    }
    
    /**
     * Sets the rendering scale.
     * 
     * @param scale Scale factor (1.0 = original size)
     */
    public void setScale(float scale) {
        this.scale = scale;
    }
    
    /**
     * Gets the width of a string in pixels.
     * 
     * @param text The text
     * @return Width in pixels
     */
    public float getStringWidth(String text) {
        return text.length() * CHAR_WIDTH * scale;
    }
    
    /**
     * Gets the height of text in pixels.
     * 
     * @return Height in pixels
     */
    public float getStringHeight() {
        return CHAR_HEIGHT * scale;
    }
    
    /**
     * Draws text at the specified position.
     * 
     * @param text The text to draw
     * @param x X position
     * @param y Y position
     * @param r Red (0-1)
     * @param g Green (0-1)
     * @param b Blue (0-1)
     */
    public void drawString(String text, float x, float y, float r, float g, float b) {
        drawString(text, x, y, r, g, b, 1.0f);
    }
    
    /**
     * Draws text at the specified position with alpha.
     * 
     * @param text The text to draw
     * @param x X position
     * @param y Y position
     * @param r Red (0-1)
     * @param g Green (0-1)
     * @param b Blue (0-1)
     * @param a Alpha (0-1)
     */
    public void drawString(String text, float x, float y, float r, float g, float b, float a) {
        if (text == null || text.isEmpty()) return;
        
        float charW = CHAR_WIDTH * scale;
        float charH = CHAR_HEIGHT * scale;
        
        float currentX = x;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == ' ') {
                currentX += charW;
                continue;
            }
            
            // Draw character (fallback: simple rectangles)
            drawCharFallback(c, currentX, y, charW, charH, r, g, b, a);
            
            currentX += charW;
        }
    }
    
    /**
     * Draws centered text.
     * 
     * @param text The text to draw
     * @param centerX Center X position
     * @param y Y position
     * @param r Red (0-1)
     * @param g Green (0-1)
     * @param b Blue (0-1)
     */
    public void drawStringCentered(String text, float centerX, float y, float r, float g, float b) {
        float width = getStringWidth(text);
        drawString(text, centerX - width / 2, y, r, g, b);
    }
    
    /**
     * Draws text with a shadow effect.
     * 
     * @param text The text to draw
     * @param x X position
     * @param y Y position
     * @param r Red (0-1)
     * @param g Green (0-1)
     * @param b Blue (0-1)
     */
    public void drawStringWithShadow(String text, float x, float y, float r, float g, float b) {
        // Draw shadow (darker, offset)
        drawString(text, x + scale, y + scale, r * 0.25f, g * 0.25f, b * 0.25f, 0.8f);
        // Draw main text
        drawString(text, x, y, r, g, b, 1.0f);
    }
    
    /**
     * Draws centered text with shadow.
     * 
     * @param text The text to draw
     * @param centerX Center X position
     * @param y Y position
     * @param r Red (0-1)
     * @param g Green (0-1)
     * @param b Blue (0-1)
     */
    public void drawStringCenteredWithShadow(String text, float centerX, float y, float r, float g, float b) {
        float width = getStringWidth(text);
        drawStringWithShadow(text, centerX - width / 2, y, r, g, b);
    }
    
    /**
     * Fallback character rendering using simple shapes.
     * Creates a stylized block-letter effect.
     */
    private void drawCharFallback(char c, float x, float y, float w, float h, 
                                   float r, float g, float b, float a) {
        // Simple block-style letters using rectangles
        float px = w / 5; // Pixel size (5 pixels wide per char)
        float py = h / 7; // 7 pixels tall
        
        switch (Character.toUpperCase(c)) {
            case 'A':
                drawBlock(x, y + py, px, h - py, r, g, b, a); // Left
                drawBlock(x + 4*px, y + py, px, h - py, r, g, b, a); // Right
                drawBlock(x + px, y, 3*px, py, r, g, b, a); // Top
                drawBlock(x + px, y + 3*py, 3*px, py, r, g, b, a); // Middle
                break;
            case 'B':
                drawBlock(x, y, px, h, r, g, b, a); // Left
                drawBlock(x + px, y, 3*px, py, r, g, b, a); // Top
                drawBlock(x + px, y + 3*py, 3*px, py, r, g, b, a); // Middle
                drawBlock(x + px, y + 6*py, 3*px, py, r, g, b, a); // Bottom
                drawBlock(x + 4*px, y + py, px, 2*py, r, g, b, a); // Right top
                drawBlock(x + 4*px, y + 4*py, px, 2*py, r, g, b, a); // Right bottom
                break;
            case 'C':
                drawBlock(x, y + py, px, 5*py, r, g, b, a); // Left
                drawBlock(x + px, y, 4*px, py, r, g, b, a); // Top
                drawBlock(x + px, y + 6*py, 4*px, py, r, g, b, a); // Bottom
                break;
            case 'D':
                drawBlock(x, y, px, h, r, g, b, a); // Left
                drawBlock(x + px, y, 3*px, py, r, g, b, a); // Top
                drawBlock(x + px, y + 6*py, 3*px, py, r, g, b, a); // Bottom
                drawBlock(x + 4*px, y + py, px, 5*py, r, g, b, a); // Right
                break;
            case 'E':
                drawBlock(x, y, px, h, r, g, b, a); // Left
                drawBlock(x + px, y, 4*px, py, r, g, b, a); // Top
                drawBlock(x + px, y + 3*py, 3*px, py, r, g, b, a); // Middle
                drawBlock(x + px, y + 6*py, 4*px, py, r, g, b, a); // Bottom
                break;
            case 'F':
                drawBlock(x, y, px, h, r, g, b, a); // Left
                drawBlock(x + px, y, 4*px, py, r, g, b, a); // Top
                drawBlock(x + px, y + 3*py, 3*px, py, r, g, b, a); // Middle
                break;
            case 'G':
                drawBlock(x, y + py, px, 5*py, r, g, b, a); // Left
                drawBlock(x + px, y, 4*px, py, r, g, b, a); // Top
                drawBlock(x + px, y + 6*py, 4*px, py, r, g, b, a); // Bottom
                drawBlock(x + 4*px, y + 3*py, px, 3*py, r, g, b, a); // Right bottom
                drawBlock(x + 2*px, y + 3*py, 2*px, py, r, g, b, a); // Middle
                break;
            case 'H':
                drawBlock(x, y, px, h, r, g, b, a); // Left
                drawBlock(x + 4*px, y, px, h, r, g, b, a); // Right
                drawBlock(x + px, y + 3*py, 3*px, py, r, g, b, a); // Middle
                break;
            case 'I':
                drawBlock(x + px, y, 3*px, py, r, g, b, a); // Top
                drawBlock(x + 2*px, y + py, px, 5*py, r, g, b, a); // Middle
                drawBlock(x + px, y + 6*py, 3*px, py, r, g, b, a); // Bottom
                break;
            case 'J':
                drawBlock(x + px, y, 4*px, py, r, g, b, a); // Top
                drawBlock(x + 3*px, y + py, px, 5*py, r, g, b, a); // Right
                drawBlock(x, y + 5*py, px, 2*py, r, g, b, a); // Left bottom
                drawBlock(x + px, y + 6*py, 2*px, py, r, g, b, a); // Bottom
                break;
            case 'K':
                drawBlock(x, y, px, h, r, g, b, a); // Left
                drawBlock(x + 4*px, y, px, 3*py, r, g, b, a); // Right top
                drawBlock(x + 4*px, y + 4*py, px, 3*py, r, g, b, a); // Right bottom
                drawBlock(x + px, y + 3*py, 3*px, py, r, g, b, a); // Middle
                break;
            case 'L':
                drawBlock(x, y, px, h, r, g, b, a); // Left
                drawBlock(x + px, y + 6*py, 4*px, py, r, g, b, a); // Bottom
                break;
            case 'M':
                drawBlock(x, y, px, h, r, g, b, a); // Left
                drawBlock(x + 4*px, y, px, h, r, g, b, a); // Right
                drawBlock(x + px, y + py, px, py, r, g, b, a); // Left inner
                drawBlock(x + 3*px, y + py, px, py, r, g, b, a); // Right inner
                drawBlock(x + 2*px, y + 2*py, px, py, r, g, b, a); // Middle
                break;
            case 'N':
                drawBlock(x, y, px, h, r, g, b, a); // Left
                drawBlock(x + 4*px, y, px, h, r, g, b, a); // Right
                drawBlock(x + px, y + py, px, 2*py, r, g, b, a); // Diagonal 1
                drawBlock(x + 2*px, y + 3*py, px, py, r, g, b, a); // Diagonal 2
                drawBlock(x + 3*px, y + 4*py, px, 2*py, r, g, b, a); // Diagonal 3
                break;
            case 'O':
                drawBlock(x, y + py, px, 5*py, r, g, b, a); // Left
                drawBlock(x + 4*px, y + py, px, 5*py, r, g, b, a); // Right
                drawBlock(x + px, y, 3*px, py, r, g, b, a); // Top
                drawBlock(x + px, y + 6*py, 3*px, py, r, g, b, a); // Bottom
                break;
            case 'P':
                drawBlock(x, y, px, h, r, g, b, a); // Left
                drawBlock(x + px, y, 3*px, py, r, g, b, a); // Top
                drawBlock(x + px, y + 3*py, 3*px, py, r, g, b, a); // Middle
                drawBlock(x + 4*px, y + py, px, 2*py, r, g, b, a); // Right
                break;
            case 'Q':
                drawBlock(x, y + py, px, 5*py, r, g, b, a); // Left
                drawBlock(x + 4*px, y + py, px, 4*py, r, g, b, a); // Right
                drawBlock(x + px, y, 3*px, py, r, g, b, a); // Top
                drawBlock(x + px, y + 6*py, 3*px, py, r, g, b, a); // Bottom
                drawBlock(x + 3*px, y + 5*py, 2*px, 2*py, r, g, b, a); // Tail
                break;
            case 'R':
                drawBlock(x, y, px, h, r, g, b, a); // Left
                drawBlock(x + px, y, 3*px, py, r, g, b, a); // Top
                drawBlock(x + px, y + 3*py, 3*px, py, r, g, b, a); // Middle
                drawBlock(x + 4*px, y + py, px, 2*py, r, g, b, a); // Right top
                drawBlock(x + 4*px, y + 4*py, px, 3*py, r, g, b, a); // Right bottom
                break;
            case 'S':
                drawBlock(x + px, y, 4*px, py, r, g, b, a); // Top
                drawBlock(x, y + py, px, 2*py, r, g, b, a); // Left top
                drawBlock(x + px, y + 3*py, 3*px, py, r, g, b, a); // Middle
                drawBlock(x + 4*px, y + 4*py, px, 2*py, r, g, b, a); // Right bottom
                drawBlock(x, y + 6*py, 4*px, py, r, g, b, a); // Bottom
                break;
            case 'T':
                drawBlock(x, y, w, py, r, g, b, a); // Top
                drawBlock(x + 2*px, y + py, px, 6*py, r, g, b, a); // Middle
                break;
            case 'U':
                drawBlock(x, y, px, 6*py, r, g, b, a); // Left
                drawBlock(x + 4*px, y, px, 6*py, r, g, b, a); // Right
                drawBlock(x + px, y + 6*py, 3*px, py, r, g, b, a); // Bottom
                break;
            case 'V':
                drawBlock(x, y, px, 5*py, r, g, b, a); // Left
                drawBlock(x + 4*px, y, px, 5*py, r, g, b, a); // Right
                drawBlock(x + px, y + 5*py, px, py, r, g, b, a); // Left bottom
                drawBlock(x + 3*px, y + 5*py, px, py, r, g, b, a); // Right bottom
                drawBlock(x + 2*px, y + 6*py, px, py, r, g, b, a); // Point
                break;
            case 'W':
                drawBlock(x, y, px, h, r, g, b, a); // Left
                drawBlock(x + 4*px, y, px, h, r, g, b, a); // Right
                drawBlock(x + 2*px, y + 3*py, px, 4*py, r, g, b, a); // Middle
                drawBlock(x + px, y + 6*py, px, py, r, g, b, a); // Bottom left
                drawBlock(x + 3*px, y + 6*py, px, py, r, g, b, a); // Bottom right
                break;
            case 'X':
                drawBlock(x, y, px, 3*py, r, g, b, a); // Left top
                drawBlock(x + 4*px, y, px, 3*py, r, g, b, a); // Right top
                drawBlock(x + px, y + 3*py, 3*px, py, r, g, b, a); // Middle
                drawBlock(x, y + 4*py, px, 3*py, r, g, b, a); // Left bottom
                drawBlock(x + 4*px, y + 4*py, px, 3*py, r, g, b, a); // Right bottom
                break;
            case 'Y':
                drawBlock(x, y, px, 3*py, r, g, b, a); // Left top
                drawBlock(x + 4*px, y, px, 3*py, r, g, b, a); // Right top
                drawBlock(x + px, y + 3*py, 3*px, py, r, g, b, a); // Middle
                drawBlock(x + 2*px, y + 4*py, px, 3*py, r, g, b, a); // Bottom
                break;
            case 'Z':
                drawBlock(x, y, w, py, r, g, b, a); // Top
                drawBlock(x + 3*px, y + py, px, py, r, g, b, a);
                drawBlock(x + 2*px, y + 2*py, px, py, r, g, b, a);
                drawBlock(x + px, y + 3*py, px, 2*py, r, g, b, a);
                drawBlock(x, y + 5*py, px, py, r, g, b, a);
                drawBlock(x, y + 6*py, w, py, r, g, b, a); // Bottom
                break;
            case '0':
                drawBlock(x, y + py, px, 5*py, r, g, b, a); // Left
                drawBlock(x + 4*px, y + py, px, 5*py, r, g, b, a); // Right
                drawBlock(x + px, y, 3*px, py, r, g, b, a); // Top
                drawBlock(x + px, y + 6*py, 3*px, py, r, g, b, a); // Bottom
                break;
            case '1':
                drawBlock(x + 2*px, y, px, h, r, g, b, a); // Middle
                drawBlock(x + px, y + py, px, py, r, g, b, a); // Top left
                drawBlock(x + px, y + 6*py, 3*px, py, r, g, b, a); // Bottom
                break;
            case '2':
                drawBlock(x + px, y, 3*px, py, r, g, b, a); // Top
                drawBlock(x + 4*px, y + py, px, 2*py, r, g, b, a); // Right top
                drawBlock(x + px, y + 3*py, 3*px, py, r, g, b, a); // Middle
                drawBlock(x, y + 4*py, px, 2*py, r, g, b, a); // Left bottom
                drawBlock(x, y + 6*py, w, py, r, g, b, a); // Bottom
                break;
            case '3':
                drawBlock(x, y, 4*px, py, r, g, b, a); // Top
                drawBlock(x + 4*px, y + py, px, 2*py, r, g, b, a); // Right top
                drawBlock(x + px, y + 3*py, 3*px, py, r, g, b, a); // Middle
                drawBlock(x + 4*px, y + 4*py, px, 2*py, r, g, b, a); // Right bottom
                drawBlock(x, y + 6*py, 4*px, py, r, g, b, a); // Bottom
                break;
            case '4':
                drawBlock(x, y, px, 4*py, r, g, b, a); // Left
                drawBlock(x + 4*px, y, px, h, r, g, b, a); // Right
                drawBlock(x + px, y + 3*py, 3*px, py, r, g, b, a); // Middle
                break;
            case '5':
                drawBlock(x, y, w, py, r, g, b, a); // Top
                drawBlock(x, y + py, px, 2*py, r, g, b, a); // Left
                drawBlock(x, y + 3*py, 4*px, py, r, g, b, a); // Middle
                drawBlock(x + 4*px, y + 4*py, px, 2*py, r, g, b, a); // Right
                drawBlock(x, y + 6*py, 4*px, py, r, g, b, a); // Bottom
                break;
            case '6':
                drawBlock(x, y + py, px, 5*py, r, g, b, a); // Left
                drawBlock(x + px, y, 3*px, py, r, g, b, a); // Top
                drawBlock(x + px, y + 3*py, 3*px, py, r, g, b, a); // Middle
                drawBlock(x + 4*px, y + 4*py, px, 2*py, r, g, b, a); // Right
                drawBlock(x + px, y + 6*py, 3*px, py, r, g, b, a); // Bottom
                break;
            case '7':
                drawBlock(x, y, w, py, r, g, b, a); // Top
                drawBlock(x + 4*px, y + py, px, 6*py, r, g, b, a); // Right
                break;
            case '8':
                drawBlock(x, y + py, px, 2*py, r, g, b, a); // Left top
                drawBlock(x, y + 4*py, px, 2*py, r, g, b, a); // Left bottom
                drawBlock(x + 4*px, y + py, px, 2*py, r, g, b, a); // Right top
                drawBlock(x + 4*px, y + 4*py, px, 2*py, r, g, b, a); // Right bottom
                drawBlock(x + px, y, 3*px, py, r, g, b, a); // Top
                drawBlock(x + px, y + 3*py, 3*px, py, r, g, b, a); // Middle
                drawBlock(x + px, y + 6*py, 3*px, py, r, g, b, a); // Bottom
                break;
            case '9':
                drawBlock(x, y + py, px, 2*py, r, g, b, a); // Left
                drawBlock(x + 4*px, y + py, px, 5*py, r, g, b, a); // Right
                drawBlock(x + px, y, 3*px, py, r, g, b, a); // Top
                drawBlock(x + px, y + 3*py, 3*px, py, r, g, b, a); // Middle
                drawBlock(x + px, y + 6*py, 3*px, py, r, g, b, a); // Bottom
                break;
            case ':':
                drawBlock(x + 2*px, y + 2*py, px, py, r, g, b, a);
                drawBlock(x + 2*px, y + 5*py, px, py, r, g, b, a);
                break;
            case '.':
                drawBlock(x + 2*px, y + 6*py, px, py, r, g, b, a);
                break;
            case ',':
                drawBlock(x + 2*px, y + 5*py, px, 2*py, r, g, b, a);
                break;
            case '!':
                drawBlock(x + 2*px, y, px, 5*py, r, g, b, a);
                drawBlock(x + 2*px, y + 6*py, px, py, r, g, b, a);
                break;
            case '?':
                drawBlock(x + px, y, 3*px, py, r, g, b, a); // Top
                drawBlock(x + 4*px, y + py, px, 2*py, r, g, b, a); // Right
                drawBlock(x + 2*px, y + 3*py, 2*px, py, r, g, b, a); // Middle
                drawBlock(x + 2*px, y + 4*py, px, py, r, g, b, a);
                drawBlock(x + 2*px, y + 6*py, px, py, r, g, b, a); // Dot
                break;
            case '-':
                drawBlock(x + px, y + 3*py, 3*px, py, r, g, b, a);
                break;
            case '+':
                drawBlock(x + 2*px, y + py, px, 5*py, r, g, b, a); // Vertical
                drawBlock(x + px, y + 3*py, 3*px, py, r, g, b, a); // Horizontal
                break;
            case '/':
                drawBlock(x + 4*px, y, px, 2*py, r, g, b, a);
                drawBlock(x + 3*px, y + 2*py, px, py, r, g, b, a);
                drawBlock(x + 2*px, y + 3*py, px, py, r, g, b, a);
                drawBlock(x + px, y + 4*py, px, py, r, g, b, a);
                drawBlock(x, y + 5*py, px, 2*py, r, g, b, a);
                break;
            case '(':
                drawBlock(x + 3*px, y, px, py, r, g, b, a);
                drawBlock(x + 2*px, y + py, px, 5*py, r, g, b, a);
                drawBlock(x + 3*px, y + 6*py, px, py, r, g, b, a);
                break;
            case ')':
                drawBlock(x + px, y, px, py, r, g, b, a);
                drawBlock(x + 2*px, y + py, px, 5*py, r, g, b, a);
                drawBlock(x + px, y + 6*py, px, py, r, g, b, a);
                break;
            default:
                // Unknown character - draw a filled block
                drawBlock(x + px, y + py, 3*px, 5*py, r * 0.5f, g * 0.5f, b * 0.5f, a);
                break;
        }
    }
    
    /**
     * Draws a single block (pixel) of the character.
     */
    private void drawBlock(float x, float y, float w, float h, float r, float g, float b, float a) {
        if (guiRenderer != null) {
            guiRenderer.drawRect(x, y, w, h, r, g, b, a);
        }
    }
    
    /**
     * Cleans up resources.
     */
    public void cleanup() {
        // Font texture is managed by TextureManager
    }
}


