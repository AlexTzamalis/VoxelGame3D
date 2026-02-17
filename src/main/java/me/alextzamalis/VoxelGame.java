package me.alextzamalis;

import me.alextzamalis.core.IGameLogic;
import me.alextzamalis.core.Window;
import me.alextzamalis.entity.PlayerController;
import me.alextzamalis.graphics.*;
import me.alextzamalis.gui.GameState;
import me.alextzamalis.gui.ScreenManager;
import me.alextzamalis.gui.hud.GameHUD;
import me.alextzamalis.gui.screens.*;
import me.alextzamalis.input.InputManager;
import me.alextzamalis.util.Logger;
import me.alextzamalis.util.ResourceLoader;
import me.alextzamalis.physics.BlockInteraction;
import me.alextzamalis.voxel.Block;
import me.alextzamalis.voxel.BlockFace;
import me.alextzamalis.voxel.BlockRegistry;
import me.alextzamalis.voxel.Chunk;
import me.alextzamalis.voxel.World;
import me.alextzamalis.world.HeightmapGenerator;
import me.alextzamalis.world.PlayerData;
import me.alextzamalis.world.WorldMetadata;
import me.alextzamalis.world.WorldSaveManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Main voxel game implementation using the modular engine systems.
 * 
 * <p>The game starts in the main menu and transitions through states:
 * MAIN_MENU -> WORLD_SELECT -> WORLD_CREATE -> LOADING -> PLAYING
 * 
 * <p>Controls (when playing):
 * <ul>
 *   <li>Click to grab mouse for camera look</li>
 *   <li>WASD - Move</li>
 *   <li>Space - Jump (survival) / Fly up (creative)</li>
 *   <li>Shift - Fly down (creative)</li>
 *   <li>Left Ctrl - Sprint</li>
 *   <li>Left Click - Break block</li>
 *   <li>Right Click - Place block</li>
 *   <li>1-7 - Select block type</li>
 *   <li>Escape - Pause menu / Release mouse</li>
 *   <li>F1 - Toggle wireframe mode</li>
 *   <li>F3 - Toggle game mode (creative/survival)</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class VoxelGame implements IGameLogic {
    
    /** View distance in chunks. */
    private static final int VIEW_DISTANCE = 3;
    
    /** Unload distance multiplier (chunks beyond VIEW_DISTANCE * this are unloaded). */
    private static final float UNLOAD_DISTANCE_MULTIPLIER = 1.5f;
    
    /** Maximum chunks to generate per frame (prevents frame drops). */
    private static final int MAX_CHUNKS_PER_FRAME = 1;
    
    /** Maximum chunk meshes to build per frame. */
    private static final int MAX_MESHES_PER_FRAME = 1;
    
    /** Spiral index for chunk loading (tracks where we left off). */
    private int chunkLoadSpiralIndex = 0;
    
    /** Last player chunk position (for detecting movement). */
    private int lastPlayerChunkX = Integer.MIN_VALUE;
    private int lastPlayerChunkZ = Integer.MIN_VALUE;
    
    /** Block texture tile size in pixels. */
    private static final int TEXTURE_TILE_SIZE = 16;
    
    /** Light direction (sun position). */
    private static final Vector3f LIGHT_DIRECTION = new Vector3f(0.5f, 1.0f, 0.3f).normalize();
    
    /** Ambient light color/intensity. */
    private static final Vector3f AMBIENT_COLOR = new Vector3f(0.4f, 0.4f, 0.4f);
    
    // ==================== Core Systems ====================
    
    /** The renderer. */
    private Renderer renderer;
    
    /** The shader program. */
    private ShaderProgram shaderProgram;
    
    /** The texture atlas for all block textures. */
    private TextureAtlas textureAtlas;
    
    /** Transformation utility. */
    private Transformation transformation;
    
    // ==================== GUI System ====================
    
    /** Screen manager for menus. */
    private ScreenManager screenManager;
    
    /** Main menu screen. */
    private MainMenuScreen mainMenuScreen;
    
    /** World select screen. */
    private WorldSelectScreen worldSelectScreen;
    
    /** World create screen. */
    private WorldCreateScreen worldCreateScreen;
    
    /** Loading screen. */
    private LoadingScreen loadingScreen;
    
    /** Settings screen. */
    private SettingsScreen settingsScreen;
    
    // ==================== Game World (initialized on world load) ====================
    
    /** The player controller. */
    private PlayerController playerController;
    
    /** The voxel world. */
    private World world;
    
    /** Frustum culler for visibility testing. */
    private FrustumCuller frustumCuller;
    
    /** Block interaction handler. */
    private BlockInteraction blockInteraction;
    
    /** Block highlight renderer. */
    private BlockHighlight blockHighlight;
    
    /** In-game HUD. */
    private GameHUD gameHUD;
    
    // ==================== State ====================
    
    /** F1 key state for toggle. */
    private boolean f1WasPressed;
    
    /** Escape key state for toggle. */
    private boolean escapeWasPressed;
    
    /** Frame counter for chunk loading rate limiting. */
    private int chunksProcessedThisFrame;
    
    /** World being loaded (for async loading). */
    private String pendingWorldName;
    private long pendingWorldSeed;
    
    /** Loading progress tracker. */
    private int loadingChunksTotal;
    private int loadingChunksCompleted;
    
    /** Reference to window for closing. */
    private Window windowRef;
    
    /** Reference to input manager for GUI. */
    private InputManager inputManagerRef;
    
    /**
     * Creates a new voxel game.
     */
    public VoxelGame() {
        this.f1WasPressed = false;
        this.escapeWasPressed = false;
        this.chunksProcessedThisFrame = 0;
    }
    
    @Override
    public void init(Window window) throws Exception {
        Logger.info("Initializing Voxel Game...");
        this.windowRef = window;
        
        // Initialize renderer
        renderer = new Renderer();
        renderer.init();
        renderer.setClearColor(0.1f, 0.1f, 0.15f, 1.0f); // Dark menu background
        
        // Build texture atlas from all block textures (do this early for fast world loading later)
        Logger.info("Building texture atlas...");
        textureAtlas = buildTextureAtlas();
        
        // Load shaders
        Logger.info("Loading shaders...");
        shaderProgram = new ShaderProgram();
        String vertexShader = ResourceLoader.loadShader("simple_vertex.glsl");
        String fragmentShader = ResourceLoader.loadShader("simple_fragment.glsl");
        shaderProgram.createVertexShader(vertexShader);
        shaderProgram.createFragmentShader(fragmentShader);
        shaderProgram.link();
        
        // Create uniforms
        shaderProgram.createUniform("projectionMatrix");
        shaderProgram.createUniform("viewMatrix");
        shaderProgram.createUniform("modelMatrix");
        shaderProgram.createUniform("useTexture");
        shaderProgram.createUniform("textureSampler");
        shaderProgram.createUniform("lightDirection");
        shaderProgram.createUniform("ambientColor");
        
        // Initialize transformation
        transformation = new Transformation();
        
        // Initialize GUI system
        initializeMenus(window);
        
        // Start in main menu
        screenManager.setState(GameState.MAIN_MENU);
        
        // Make sure cursor is visible in menus
        glfwSetInputMode(window.getWindowHandle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        
        Logger.info("Voxel Game initialized! Starting in main menu.");
    }
    
    /**
     * Initializes the menu system.
     */
    private void initializeMenus(Window window) throws Exception {
        screenManager = new ScreenManager();
        screenManager.init(window.getWidth(), window.getHeight());
        
        // Create main menu
        mainMenuScreen = new MainMenuScreen(screenManager);
        mainMenuScreen.setOnQuit(() -> window.setWindowShouldClose(true));
        screenManager.registerScreen(GameState.MAIN_MENU, mainMenuScreen);
        
        // Create world select screen
        worldSelectScreen = new WorldSelectScreen(screenManager);
        worldSelectScreen.setOnWorldSelected(this::startLoadingWorld);
        screenManager.registerScreen(GameState.WORLD_SELECT, worldSelectScreen);
        
        // Create world create screen
        worldCreateScreen = new WorldCreateScreen(screenManager);
        worldCreateScreen.setOnWorldCreate(this::startLoadingWorld);
        screenManager.registerScreen(GameState.WORLD_CREATE, worldCreateScreen);
        
        // Create loading screen
        loadingScreen = new LoadingScreen();
        loadingScreen.setOnLoadingComplete(this::onWorldLoadComplete);
        screenManager.registerScreen(GameState.LOADING, loadingScreen);
        
        // Create settings screen
        settingsScreen = new SettingsScreen(screenManager);
        screenManager.registerScreen(GameState.SETTINGS, settingsScreen);
        
        // Listen for state changes
        screenManager.setStateChangeListener((oldState, newState) -> {
            if (newState == GameState.PLAYING) {
                // Lock cursor when playing
                glfwSetInputMode(window.getWindowHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                renderer.setClearColor(0.5f, 0.7f, 1.0f, 1.0f); // Sky blue
            } else {
                // Show cursor in menus
                glfwSetInputMode(window.getWindowHandle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                if (newState != GameState.LOADING) {
                    renderer.setClearColor(0.1f, 0.1f, 0.15f, 1.0f); // Dark menu
                }
            }
        });
        
        Logger.info("Menu system initialized");
    }
    
    /**
     * Starts loading a world.
     */
    private void startLoadingWorld(String worldName, long seed) {
        this.pendingWorldName = worldName;
        this.pendingWorldSeed = seed;
        
        loadingScreen.setProgress(0);
        loadingScreen.setStatusMessage("Preparing world...");
        
        // Calculate total chunks to load
        int diameter = VIEW_DISTANCE * 2 + 1;
        loadingChunksTotal = diameter * diameter;
        loadingChunksCompleted = 0;
        
        screenManager.setState(GameState.LOADING);
        
        Logger.info("Starting to load world: %s (seed: %d)", worldName, seed);
    }
    
    /**
     * Called when world loading is complete.
     */
    private void onWorldLoadComplete() {
        screenManager.setState(GameState.PLAYING);
        Logger.info("World loading complete! Now playing.");
        Logger.info("Controls: WASD to move, Space = jump/fly up, Shift = fly down");
        Logger.info("Left Click = break block, Right Click = place block, 1-7 = select block");
        Logger.info("F1 = wireframe, F3 = toggle game mode, Escape = menu");
    }
    
    /**
     * Initializes the world for playing.
     */
    private void initializeWorld() throws Exception {
        Logger.info("Initializing world: %s", pendingWorldName);
        
        // Don't call cleanupWorld() here as it would try to save a null world
        // Just ensure old resources are cleaned up
        if (gameHUD != null) { gameHUD.cleanup(); gameHUD = null; }
        if (blockHighlight != null) { blockHighlight.cleanup(); blockHighlight = null; }
        if (world != null) { world.cleanup(); world = null; }
        
        WorldSaveManager saveManager = WorldSaveManager.getInstance();
        
        // Load player data if exists
        PlayerData playerData = saveManager.loadPlayerData(pendingWorldName);
        Vector3f spawnPos;
        if (playerData != null) {
            spawnPos = new Vector3f((float)playerData.getX(), (float)playerData.getY(), (float)playerData.getZ());
            Logger.info("Loaded player position: (%.1f, %.1f, %.1f)", spawnPos.x, spawnPos.y, spawnPos.z);
        } else {
            spawnPos = new Vector3f(0, 70, 0); // Default spawn
            Logger.info("No saved player data, using default spawn");
        }
        
        // Initialize player controller
        playerController = new PlayerController();
        playerController.init(windowRef, spawnPos);
        playerController.setMovementSpeed(10.0f);
        
        // Restore player rotation if saved
        if (playerData != null) {
            playerController.getCamera().setPitch(playerData.getPitch());
            playerController.getCamera().setYaw(playerData.getYaw());
            // Set game mode
            if (playerData.isCreative()) {
                playerController.setCreativeMode();
            } else {
                playerController.setSurvivalMode();
            }
        }
        
        // Initialize frustum culler
        frustumCuller = new FrustumCuller();
        
        // Initialize world with heightmap generator
        world = new World(pendingWorldName, pendingWorldSeed);
        world.setGenerator(new HeightmapGenerator(world.getSeed(), 60, 20, 0.015f));
        world.setTextureAtlas(textureAtlas);
        
        // Initialize block interaction
        blockInteraction = new BlockInteraction(world);
        
        // Initialize block highlight
        blockHighlight = new BlockHighlight();
        blockHighlight.init();
        
        // Set world reference for player physics
        playerController.setWorld(world);
        
        // Initialize HUD
        gameHUD = new GameHUD();
        gameHUD.init(windowRef.getWidth(), windowRef.getHeight());
        
        // Restore hotbar selection
        if (playerData != null) {
            gameHUD.setSelectedSlot(playerData.getSelectedSlot());
            blockInteraction.setSelectedBlockId(playerData.getSelectedSlot() + 1);
        }
    }
    
    /**
     * Cleans up the current world, saving data first.
     */
    private void cleanupWorld() {
        // Save world data before cleanup
        saveWorldData();
        
        if (gameHUD != null) {
            gameHUD.cleanup();
            gameHUD = null;
        }
        if (blockHighlight != null) {
            blockHighlight.cleanup();
            blockHighlight = null;
        }
        if (world != null) {
            world.cleanup();
            world = null;
        }
        blockInteraction = null;
        playerController = null;
        frustumCuller = null;
    }
    
    /**
     * Saves world and player data to disk.
     */
    private void saveWorldData() {
        if (world == null || playerController == null) {
            return;
        }
        
        WorldSaveManager saveManager = WorldSaveManager.getInstance();
        String worldName = world.getName();
        
        try {
            // Save player data
            PlayerData playerData = new PlayerData();
            Vector3f pos = playerController.getPosition();
            playerData.setPosition(pos.x, pos.y, pos.z);
            playerData.setPitch(playerController.getCamera().getPitch());
            playerData.setYaw(playerController.getCamera().getYaw());
            playerData.setGameMode(playerController.isCreative() ? 
                                   WorldMetadata.GAME_MODE_CREATIVE : 
                                   WorldMetadata.GAME_MODE_SURVIVAL);
            if (gameHUD != null) {
                playerData.setSelectedSlot(gameHUD.getSelectedSlot());
            }
            
            saveManager.savePlayerData(worldName, playerData);
            
            // Update world metadata (last played time)
            WorldMetadata metadata = saveManager.loadWorldMetadata(worldName);
            if (metadata != null) {
                metadata.updateLastPlayed();
                saveManager.saveWorldMetadata(metadata);
            }
            
            // Save modified chunks
            saveManager.saveWorld(world);
            
            Logger.info("World saved: %s", worldName);
            
        } catch (Exception e) {
            Logger.error("Failed to save world data: %s", e.getMessage());
        }
    }
    
    /**
     * Builds a texture atlas from all registered block textures.
     */
    private TextureAtlas buildTextureAtlas() {
        TextureAtlas atlas = new TextureAtlas(TEXTURE_TILE_SIZE);
        BlockRegistry registry = BlockRegistry.getInstance();
        
        // Add all unique textures from all blocks
        for (Block block : registry.getAllBlocks()) {
            if (block.isAir()) continue;
            
            for (BlockFace face : BlockFace.values()) {
                String texturePath = block.getTextures().getTexture(face);
                atlas.addTexture(texturePath);
            }
        }
        
        atlas.build();
        Logger.info("Texture atlas built with %d unique textures", atlas.getTilesPerRow() * atlas.getTilesPerRow());
        return atlas;
    }
    
    @Override
    public void input(Window window, InputManager inputManager) {
        this.inputManagerRef = inputManager;
        
        GameState currentState = screenManager.getCurrentState();
        
        if (currentState == GameState.PLAYING) {
            // Handle escape to pause
            boolean escapePressed = window.isKeyPressed(GLFW_KEY_ESCAPE);
            if (escapePressed && !escapeWasPressed) {
                // For now, just release cursor. Later: open pause menu
                glfwSetInputMode(window.getWindowHandle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                screenManager.setState(GameState.MAIN_MENU);
                cleanupWorld();
            }
            escapeWasPressed = escapePressed;
            
            // Process player input
            if (playerController != null) {
                playerController.processInput(window, inputManager);
            }
            
            // Toggle wireframe with F1
            boolean f1Pressed = window.isKeyPressed(GLFW_KEY_F1);
            if (f1Pressed && !f1WasPressed) {
                renderer.toggleWireframe();
                Logger.info("Wireframe: %s", renderer.isWireframe() ? "ON" : "OFF");
            }
            f1WasPressed = f1Pressed;
            
            // Hotbar selection with number keys 1-9
            if (gameHUD != null) {
                for (int i = 0; i < 9; i++) {
                    if (window.isKeyPressed(GLFW_KEY_1 + i)) {
                        gameHUD.setSelectedSlot(i);
                        blockInteraction.setSelectedBlockId(i + 1); // Block IDs start at 1
                    }
                }
                
                // Scroll wheel for hotbar
                double scrollY = inputManager.getScrollOffset();
                if (scrollY != 0) {
                    gameHUD.scrollSlot((int) -scrollY);
                    blockInteraction.setSelectedBlockId(gameHUD.getSelectedSlot() + 1);
                }
            }
        } else {
            // Menu input
            screenManager.input(window, inputManager);
        }
    }
    
    @Override
    public void update(float deltaTime, InputManager inputManager) {
        GameState currentState = screenManager.getCurrentState();
        
        switch (currentState) {
            case MAIN_MENU:
                mainMenuScreen.update(deltaTime, inputManager);
                break;
                
            case WORLD_SELECT:
                worldSelectScreen.update(deltaTime, inputManager);
                break;
                
            case WORLD_CREATE:
                worldCreateScreen.update(deltaTime, inputManager);
                break;
                
            case SETTINGS:
                settingsScreen.update(deltaTime, inputManager);
                break;
                
            case LOADING:
                updateLoading(deltaTime);
                break;
                
            case PLAYING:
                updatePlaying(deltaTime, inputManager);
                break;
                
            default:
                screenManager.update(deltaTime);
                break;
        }
    }
    
    /**
     * Updates the loading state.
     */
    private void updateLoading(float deltaTime) {
        loadingScreen.update(deltaTime);
        
        // Initialize world on first frame of loading
        if (world == null && pendingWorldName != null) {
            try {
                initializeWorld();
                loadingScreen.setStatusMessage("Generating terrain...");
            } catch (Exception e) {
                Logger.error("Failed to initialize world: %s", e.getMessage());
                e.printStackTrace();
                screenManager.setState(GameState.MAIN_MENU);
                return;
            }
        }
        
        // Generate chunks incrementally - ONLY 1-2 per frame to stay responsive!
        if (world != null && loadingChunksCompleted < loadingChunksTotal) {
            Vector3f playerPos = playerController.getPosition();
            int playerChunkX = Chunk.worldToChunkX((int) playerPos.x);
            int playerChunkZ = Chunk.worldToChunkZ((int) playerPos.z);
            
            // Process only 1-2 chunks per frame to keep UI responsive
            int chunksThisFrame = Math.min(2, loadingChunksTotal - loadingChunksCompleted);
            
            for (int i = 0; i < chunksThisFrame; i++) {
                // Calculate chunk position in spiral
                int index = loadingChunksCompleted;
                int[] offset = getSpiralOffset(index);
                int chunkX = playerChunkX + offset[0];
                int chunkZ = playerChunkZ + offset[1];
                
                Chunk chunk = world.getOrCreateChunk(chunkX, chunkZ);
                
                if (!chunk.isGenerated()) {
                    world.getGenerator().generateChunk(chunk, world);
                    chunk.setGenerated(true);
                }
                
                if (chunk.isDirty()) {
                    world.buildChunkMesh(chunk);
                }
                
                loadingChunksCompleted++;
            }
            
            // Update progress
            float progress = (float) loadingChunksCompleted / loadingChunksTotal;
            loadingScreen.setProgress(progress);
            loadingScreen.setStatusMessage(String.format("Generating terrain... %d%%", (int)(progress * 100)));
            
            // Check if loading is complete
            if (loadingChunksCompleted >= loadingChunksTotal) {
                loadingScreen.complete();
            }
        }
    }
    
    /**
     * Gets the spiral offset for a given index.
     */
    private int[] getSpiralOffset(int index) {
        if (index == 0) return new int[]{0, 0};
        
        int x = 0, z = 0;
        int dx = 0, dz = -1;
        int segmentLength = 1;
        int segmentPassed = 0;
        int direction = 0;
        
        for (int i = 0; i < index; i++) {
            if (segmentPassed == segmentLength) {
                segmentPassed = 0;
                direction = (direction + 1) % 4;
                if (direction == 0 || direction == 2) {
                    segmentLength++;
                }
            }
            
            switch (direction) {
                case 0: dx = 1; dz = 0; break;
                case 1: dx = 0; dz = 1; break;
                case 2: dx = -1; dz = 0; break;
                case 3: dx = 0; dz = -1; break;
            }
            
            x += dx;
            z += dz;
            segmentPassed++;
        }
        
        return new int[]{x, z};
    }
    
    /**
     * Updates the playing state.
     */
    private void updatePlaying(float deltaTime, InputManager inputManager) {
        if (playerController == null || world == null) return;
        
        playerController.update(deltaTime, inputManager);
        
        // Update block highlight (raycasting)
        blockHighlight.update(playerController.getCamera(), world);
        
        // Update block interaction (breaking/placing)
        blockInteraction.update(playerController.getCamera(), inputManager, deltaTime);
        
        // Load chunks around player (rate-limited)
        Vector3f playerPos = playerController.getPosition();
        int playerChunkX = Chunk.worldToChunkX((int) playerPos.x);
        int playerChunkZ = Chunk.worldToChunkZ((int) playerPos.z);
        
        chunksProcessedThisFrame = 0;
        loadChunksAroundPlayer(playerChunkX, playerChunkZ);
        
        // Unload distant chunks
        int unloadDistance = (int) (VIEW_DISTANCE * UNLOAD_DISTANCE_MULTIPLIER);
        world.unloadDistantChunks(playerChunkX, playerChunkZ, unloadDistance);
    }
    
    /**
     * Loads chunks around the player with rate limiting.
     * Uses spiral pattern to load chunks incrementally without expensive nested loops.
     */
    private void loadChunksAroundPlayer(int centerChunkX, int centerChunkZ) {
        // Reset spiral if player moved to a new chunk
        if (centerChunkX != lastPlayerChunkX || centerChunkZ != lastPlayerChunkZ) {
            chunkLoadSpiralIndex = 0;
            lastPlayerChunkX = centerChunkX;
            lastPlayerChunkZ = centerChunkZ;
        }
        
        int chunksGenerated = 0;
        int chunksMeshed = 0;
        int maxChunks = (VIEW_DISTANCE * 2 + 1) * (VIEW_DISTANCE * 2 + 1);
        
        // Process chunks in spiral pattern (much faster than nested loops)
        while (chunkLoadSpiralIndex < maxChunks && 
               (chunksGenerated < MAX_CHUNKS_PER_FRAME || chunksMeshed < MAX_MESHES_PER_FRAME)) {
            
            int[] offset = getSpiralOffset(chunkLoadSpiralIndex);
            int chunkX = centerChunkX + offset[0];
            int chunkZ = centerChunkZ + offset[1];
            
            // Check if within view distance
            if (Math.abs(offset[0]) > VIEW_DISTANCE || Math.abs(offset[1]) > VIEW_DISTANCE) {
                chunkLoadSpiralIndex++;
                continue;
            }
            
            // Try to generate chunk if needed
            if (chunksGenerated < MAX_CHUNKS_PER_FRAME) {
                Chunk chunk = world.getOrCreateChunk(chunkX, chunkZ);
                if (!chunk.isGenerated()) {
                    world.getGenerator().generateChunk(chunk, world);
                    chunk.setGenerated(true);
                    chunksGenerated++;
                }
            }
            
            // Try to mesh chunk if needed
            if (chunksMeshed < MAX_MESHES_PER_FRAME) {
                Chunk chunk = world.getChunk(chunkX, chunkZ);
                if (chunk != null && chunk.isGenerated() && chunk.isDirty()) {
                    world.buildChunkMesh(chunk);
                    chunksMeshed++;
                }
            }
            
            chunkLoadSpiralIndex++;
        }
        
        // Reset spiral index if we've completed a full cycle
        if (chunkLoadSpiralIndex >= maxChunks) {
            chunkLoadSpiralIndex = 0; // Start over, checking for new/dirty chunks
        }
        
        chunksProcessedThisFrame = chunksGenerated + chunksMeshed;
    }
    
    @Override
    public void render(Window window) {
        renderer.prepare(window);
        
        GameState currentState = screenManager.getCurrentState();
        
        if (currentState == GameState.PLAYING && world != null && playerController != null) {
            renderWorld(window);
        }
        
        // Render GUI on top
        screenManager.render();
    }
    
    /**
     * Renders the voxel world.
     */
    private void renderWorld(Window window) {
        // Update projection if window resized
        if (window.isResized()) {
            playerController.updateProjection(window);
            screenManager.resize(window.getWidth(), window.getHeight());
        }
        
        // Update view matrix
        Camera camera = playerController.getCamera();
        camera.updateViewMatrix();
        
        // Bind shader and set uniforms
        shaderProgram.bind();
        shaderProgram.setUniform("projectionMatrix", camera.getProjectionMatrix());
        shaderProgram.setUniform("viewMatrix", camera.getViewMatrix());
        shaderProgram.setUniform("textureSampler", 0);
        shaderProgram.setUniform("useTexture", true);
        shaderProgram.setUniform("lightDirection", LIGHT_DIRECTION);
        shaderProgram.setUniform("ambientColor", AMBIENT_COLOR);
        
        // Bind texture atlas
        textureAtlas.bind(0);
        
        // Identity model matrix for chunks
        Matrix4f modelMatrix = new Matrix4f().identity();
        shaderProgram.setUniform("modelMatrix", modelMatrix);
        
        // Update frustum culler
        frustumCuller.update(camera.getProjectionMatrix(), camera.getViewMatrix());
        
        // Render visible chunks
        for (Chunk chunk : world.getChunks()) {
            if (chunk.hasMesh()) {
                if (frustumCuller.isChunkInFrustum(
                        chunk.getChunkX(), chunk.getChunkZ(),
                        Chunk.WIDTH, Chunk.HEIGHT, Chunk.DEPTH)) {
                    chunk.getMesh().render();
                }
            }
        }
        
        // Unbind main shader
        textureAtlas.unbind();
        shaderProgram.unbind();
        
        // Render block highlight
        blockHighlight.render(camera);
        
        // Render HUD
        if (gameHUD != null) {
            screenManager.getGuiRenderer().begin();
            gameHUD.render(screenManager.getGuiRenderer());
            screenManager.getGuiRenderer().end();
        }
    }
    
    @Override
    public void cleanup() {
        Logger.info("Cleaning up Voxel Game...");
        
        cleanupWorld();
        
        if (screenManager != null) screenManager.cleanup();
        if (shaderProgram != null) shaderProgram.cleanup();
        if (textureAtlas != null) textureAtlas.cleanup();
        if (renderer != null) renderer.cleanup();
        
        Logger.info("Voxel Game cleanup complete.");
    }
}
