package me.alextzamalis;

import me.alextzamalis.core.IGameLogic;
import me.alextzamalis.core.Window;
import me.alextzamalis.entity.PlayerController;
import me.alextzamalis.graphics.*;
import me.alextzamalis.input.InputManager;
import me.alextzamalis.util.Logger;
import me.alextzamalis.util.ResourceLoader;
import me.alextzamalis.voxel.Block;
import me.alextzamalis.voxel.BlockFace;
import me.alextzamalis.voxel.BlockRegistry;
import me.alextzamalis.voxel.Chunk;
import me.alextzamalis.voxel.World;
import me.alextzamalis.world.HeightmapGenerator;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Main voxel game implementation using the modular engine systems.
 * 
 * <p>This class demonstrates the proper use of the engine's modular architecture:
 * <ul>
 *   <li>{@link PlayerController} - handles all player input and camera</li>
 *   <li>{@link World} - manages chunks and block data</li>
 *   <li>{@link me.alextzamalis.world.WorldGenerator} - generates terrain</li>
 *   <li>{@link BlockRegistry} - provides block type information</li>
 *   <li>{@link TextureAtlas} - combines all block textures for efficient rendering</li>
 * </ul>
 * 
 * <p>Controls:
 * <ul>
 *   <li>Click to grab mouse for camera look</li>
 *   <li>WASD - Move</li>
 *   <li>Space/Shift - Move up/down</li>
 *   <li>Left Ctrl - Sprint</li>
 *   <li>Escape - Release mouse / Close game</li>
 *   <li>F1 - Toggle wireframe mode</li>
 *   <li>F3 - Toggle debug info</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class VoxelGame implements IGameLogic {
    
    /** View distance in chunks. */
    private static final int VIEW_DISTANCE = 4;
    
    /** Block texture tile size in pixels. */
    private static final int TEXTURE_TILE_SIZE = 16;
    
    /** The renderer. */
    private Renderer renderer;
    
    /** The shader program. */
    private ShaderProgram shaderProgram;
    
    /** The player controller. */
    private PlayerController playerController;
    
    /** The voxel world. */
    private World world;
    
    /** Transformation utility. */
    private Transformation transformation;
    
    /** The texture atlas for all block textures. */
    private TextureAtlas textureAtlas;
    
    /** Frustum culler for visibility testing. */
    private FrustumCuller frustumCuller;
    
    /** Debug display flag. */
    private boolean showDebug;
    
    /** F1 key state for toggle. */
    private boolean f1WasPressed;
    
    /** F3 key state for toggle. */
    private boolean f3WasPressed;
    
    /**
     * Creates a new voxel game.
     */
    public VoxelGame() {
        this.showDebug = false;
        this.f1WasPressed = false;
        this.f3WasPressed = false;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Window window) throws Exception {
        Logger.info("Initializing Voxel Game...");
        
        // Initialize renderer
        renderer = new Renderer();
        renderer.init();
        renderer.setClearColor(0.5f, 0.7f, 1.0f, 1.0f); // Sky blue
        
        // Build texture atlas from all block textures
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
        shaderProgram.createUniform("color");
        shaderProgram.createUniform("useTexture");
        shaderProgram.createUniform("textureSampler");
        
        // Initialize player controller
        Logger.info("Initializing player...");
        playerController = new PlayerController();
        playerController.init(window, new Vector3f(0, 70, 0));
        playerController.setMovementSpeed(10.0f); // Faster for testing
        
        // Initialize transformation
        transformation = new Transformation();
        
        // Initialize frustum culler
        frustumCuller = new FrustumCuller();
        
        // Initialize world with heightmap generator
        Logger.info("Creating world...");
        world = new World("TestWorld", 12345L);
        world.setGenerator(new HeightmapGenerator(world.getSeed(), 60, 20, 0.015f));
        world.setTextureAtlas(textureAtlas); // Set atlas for mesh building
        
        // Load initial chunks around player
        Logger.info("Generating initial chunks...");
        Vector3f playerPos = playerController.getPosition();
        world.loadChunksAround((int) playerPos.x, (int) playerPos.z, VIEW_DISTANCE);
        
        Logger.info("Voxel Game initialized! %d chunks loaded.", world.getChunkCount());
        Logger.info("Controls: Click to look, WASD to move, Space/Shift up/down, Ctrl to sprint");
        Logger.info("F1 = wireframe, F3 = debug info, Escape = release mouse/quit");
    }
    
    /**
     * Builds a texture atlas from all registered block textures.
     * 
     * @return The built texture atlas
     */
    private TextureAtlas buildTextureAtlas() {
        TextureAtlas atlas = new TextureAtlas(TEXTURE_TILE_SIZE);
        BlockRegistry registry = BlockRegistry.getInstance();
        
        // Add all unique textures from all blocks
        for (Block block : registry.getAllBlocks()) {
            if (block.isAir()) {
                continue;
            }
            
            // Add textures for each face
            for (BlockFace face : BlockFace.values()) {
                String texturePath = block.getTextures().getTexture(face);
                atlas.addTexture(texturePath);
            }
        }
        
        // Build the atlas
        atlas.build();
        
        Logger.info("Texture atlas built with %d unique textures", atlas.getTilesPerRow() * atlas.getTilesPerRow());
        return atlas;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void input(Window window, InputManager inputManager) {
        // Process player input (cursor grab, escape)
        if (playerController.processInput(window, inputManager)) {
            window.setWindowShouldClose(true);
        }
        
        // Toggle wireframe with F1
        boolean f1Pressed = window.isKeyPressed(GLFW_KEY_F1);
        if (f1Pressed && !f1WasPressed) {
            renderer.toggleWireframe();
            Logger.info("Wireframe: %s", renderer.isWireframe() ? "ON" : "OFF");
        }
        f1WasPressed = f1Pressed;
        
        // Toggle debug with F3
        boolean f3Pressed = window.isKeyPressed(GLFW_KEY_F3);
        if (f3Pressed && !f3WasPressed) {
            showDebug = !showDebug;
            Logger.info("Debug display: %s", showDebug ? "ON" : "OFF");
        }
        f3WasPressed = f3Pressed;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void update(float deltaTime, InputManager inputManager) {
        // Update player
        playerController.update(deltaTime, inputManager);
        
        // Load chunks around player
        Vector3f playerPos = playerController.getPosition();
        world.loadChunksAround((int) playerPos.x, (int) playerPos.z, VIEW_DISTANCE);
        
        // Debug output
        if (showDebug) {
            // Print debug info periodically (every ~60 frames)
            // In a real game, this would be rendered on screen
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void render(Window window) {
        renderer.prepare(window);
        
        // Update projection if window resized
        if (window.isResized()) {
            playerController.updateProjection(window);
        }
        
        // Update view matrix
        Camera camera = playerController.getCamera();
        camera.updateViewMatrix();
        
        // Bind shader
        shaderProgram.bind();
        shaderProgram.setUniform("projectionMatrix", camera.getProjectionMatrix());
        shaderProgram.setUniform("viewMatrix", camera.getViewMatrix());
        shaderProgram.setUniform("textureSampler", 0);
        shaderProgram.setUniform("useTexture", true);
        shaderProgram.setUniform("color", new Vector3f(1.0f, 1.0f, 1.0f));
        
        // Bind texture atlas
        textureAtlas.bind(0);
        
        // Identity model matrix for chunks (they use world coordinates)
        Matrix4f modelMatrix = new Matrix4f().identity();
        shaderProgram.setUniform("modelMatrix", modelMatrix);
        
        // Update frustum culler with current matrices
        frustumCuller.update(camera.getProjectionMatrix(), camera.getViewMatrix());
        
        // Render visible chunks (frustum culling)
        int renderedChunks = 0;
        int culledChunks = 0;
        for (Chunk chunk : world.getChunks()) {
            if (chunk.hasMesh()) {
                // Check if chunk is in view frustum
                if (frustumCuller.isChunkInFrustum(
                        chunk.getChunkX(), chunk.getChunkZ(),
                        Chunk.WIDTH, Chunk.HEIGHT, Chunk.DEPTH)) {
                    chunk.getMesh().render();
                    renderedChunks++;
                } else {
                    culledChunks++;
                }
            }
        }
        
        // Unbind
        textureAtlas.unbind();
        shaderProgram.unbind();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanup() {
        Logger.info("Cleaning up Voxel Game...");
        
        if (world != null) {
            world.cleanup();
        }
        if (shaderProgram != null) {
            shaderProgram.cleanup();
        }
        if (textureAtlas != null) {
            textureAtlas.cleanup();
        }
        if (renderer != null) {
            renderer.cleanup();
        }
        
        Logger.info("Voxel Game cleanup complete.");
    }
}
