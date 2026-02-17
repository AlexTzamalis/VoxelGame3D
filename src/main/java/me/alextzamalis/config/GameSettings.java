package me.alextzamalis.config;

/**
 * Stores all game settings for performance and gameplay configuration.
 * 
 * <p>This class follows the architecture of performance mods like Sodium/Fabric,
 * providing granular control over rendering and update rates, view distance,
 * and other performance-critical settings.
 * 
 * <p>Settings are loaded from and saved to config.properties file.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class GameSettings {
    
    // ========== Performance Settings ==========
    
    /** Target frames per second (rendering rate). */
    private int targetFPS = 60;
    
    /** Target updates per second (game logic rate). */
    private int targetUPS = 20; // Lower UPS for better performance (like Sodium)
    
    /** Whether VSync is enabled. */
    private boolean vSync = true;
    
    /** Whether to use separate update thread (like Sodium/Fabric). */
    private boolean separateUpdateThread = true;
    
    /** Maximum frame time in milliseconds (prevents lag spikes). */
    private float maxFrameTime = 0.1f; // 100ms max frame time
    
    // ========== Rendering Settings ==========
    
    /** View distance in chunks (how far chunks are loaded/rendered). */
    private int viewDistance = 3;
    
    /** Whether to enable frustum culling. */
    private boolean frustumCulling = true;
    
    /** Whether to enable greedy meshing (reduces draw calls). */
    private boolean greedyMeshing = true;
    
    /** Whether to enable LOD (Level of Detail) system. */
    private boolean lodEnabled = true;
    
    /** Maximum chunks to generate per frame. */
    private int maxChunksPerFrame = 1;
    
    /** Maximum chunk meshes to build per frame. */
    private int maxMeshesPerFrame = 1;
    
    // ========== Graphics Settings ==========
    
    /** Render distance quality (affects chunk detail). */
    private RenderQuality renderQuality = RenderQuality.NORMAL;
    
    /** Whether to enable wireframe mode (for debugging). */
    private boolean wireframe = false;
    
    // ========== Gameplay Settings ==========
    
    /** Mouse sensitivity. */
    private float mouseSensitivity = 1.0f;
    
    /** Movement speed multiplier. */
    private float movementSpeed = 1.0f;
    
    /** Whether to show FPS counter. */
    private boolean showFPS = false;
    
    /** Whether to show debug info. */
    private boolean showDebug = false;
    
    /**
     * Render quality presets.
     */
    public enum RenderQuality {
        LOW(2, 1, 1),
        NORMAL(3, 1, 1),
        HIGH(4, 2, 2),
        ULTRA(6, 3, 2);
        
        public final int viewDistance;
        public final int maxChunksPerFrame;
        public final int maxMeshesPerFrame;
        
        RenderQuality(int viewDistance, int maxChunksPerFrame, int maxMeshesPerFrame) {
            this.viewDistance = viewDistance;
            this.maxChunksPerFrame = maxChunksPerFrame;
            this.maxMeshesPerFrame = maxMeshesPerFrame;
        }
    }
    
    // ========== Getters and Setters ==========
    
    public int getTargetFPS() {
        return targetFPS;
    }
    
    public void setTargetFPS(int targetFPS) {
        this.targetFPS = Math.max(30, Math.min(240, targetFPS)); // Clamp 30-240
    }
    
    public int getTargetUPS() {
        return targetUPS;
    }
    
    public void setTargetUPS(int targetUPS) {
        this.targetUPS = Math.max(10, Math.min(60, targetUPS)); // Clamp 10-60
    }
    
    public boolean isVSync() {
        return vSync;
    }
    
    public void setVSync(boolean vSync) {
        this.vSync = vSync;
    }
    
    public boolean isSeparateUpdateThread() {
        return separateUpdateThread;
    }
    
    public void setSeparateUpdateThread(boolean separateUpdateThread) {
        this.separateUpdateThread = separateUpdateThread;
    }
    
    public float getMaxFrameTime() {
        return maxFrameTime;
    }
    
    public void setMaxFrameTime(float maxFrameTime) {
        this.maxFrameTime = Math.max(0.016f, Math.min(0.5f, maxFrameTime)); // Clamp 16ms-500ms
    }
    
    public int getViewDistance() {
        return viewDistance;
    }
    
    public void setViewDistance(int viewDistance) {
        this.viewDistance = Math.max(2, Math.min(12, viewDistance)); // Clamp 2-12 chunks
    }
    
    public boolean isFrustumCulling() {
        return frustumCulling;
    }
    
    public void setFrustumCulling(boolean frustumCulling) {
        this.frustumCulling = frustumCulling;
    }
    
    public boolean isGreedyMeshing() {
        return greedyMeshing;
    }
    
    public void setGreedyMeshing(boolean greedyMeshing) {
        this.greedyMeshing = greedyMeshing;
    }
    
    public boolean isLodEnabled() {
        return lodEnabled;
    }
    
    public void setLodEnabled(boolean lodEnabled) {
        this.lodEnabled = lodEnabled;
    }
    
    public int getMaxChunksPerFrame() {
        return maxChunksPerFrame;
    }
    
    public void setMaxChunksPerFrame(int maxChunksPerFrame) {
        this.maxChunksPerFrame = Math.max(1, Math.min(10, maxChunksPerFrame));
    }
    
    public int getMaxMeshesPerFrame() {
        return maxMeshesPerFrame;
    }
    
    public void setMaxMeshesPerFrame(int maxMeshesPerFrame) {
        this.maxMeshesPerFrame = Math.max(1, Math.min(10, maxMeshesPerFrame));
    }
    
    public RenderQuality getRenderQuality() {
        return renderQuality;
    }
    
    public void setRenderQuality(RenderQuality renderQuality) {
        this.renderQuality = renderQuality;
        // Apply quality preset
        this.viewDistance = renderQuality.viewDistance;
        this.maxChunksPerFrame = renderQuality.maxChunksPerFrame;
        this.maxMeshesPerFrame = renderQuality.maxMeshesPerFrame;
    }
    
    public boolean isWireframe() {
        return wireframe;
    }
    
    public void setWireframe(boolean wireframe) {
        this.wireframe = wireframe;
    }
    
    public float getMouseSensitivity() {
        return mouseSensitivity;
    }
    
    public void setMouseSensitivity(float mouseSensitivity) {
        this.mouseSensitivity = Math.max(0.1f, Math.min(5.0f, mouseSensitivity));
    }
    
    public float getMovementSpeed() {
        return movementSpeed;
    }
    
    public void setMovementSpeed(float movementSpeed) {
        this.movementSpeed = Math.max(0.1f, Math.min(5.0f, movementSpeed));
    }
    
    public boolean isShowFPS() {
        return showFPS;
    }
    
    public void setShowFPS(boolean showFPS) {
        this.showFPS = showFPS;
    }
    
    public boolean isShowDebug() {
        return showDebug;
    }
    
    public void setShowDebug(boolean showDebug) {
        this.showDebug = showDebug;
    }
}

