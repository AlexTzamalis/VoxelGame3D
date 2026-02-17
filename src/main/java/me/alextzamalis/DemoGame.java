package me.alextzamalis;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

import me.alextzamalis.core.IGameLogic;
import me.alextzamalis.core.Window;
import me.alextzamalis.graphics.Camera;
import me.alextzamalis.graphics.Mesh;
import me.alextzamalis.graphics.Renderer;
import me.alextzamalis.graphics.ShaderProgram;
import me.alextzamalis.graphics.Texture;
import me.alextzamalis.graphics.TextureManager;
import me.alextzamalis.graphics.Transformation;
import me.alextzamalis.input.InputManager;
import me.alextzamalis.util.Logger;
import me.alextzamalis.util.ResourceLoader;
import me.alextzamalis.voxel.BlockRegistry;

/**
 * Demo game implementation showcasing the engine's capabilities.
 * 
 * <p>This class demonstrates basic 3D rendering with textured blocks,
 * first-person camera controls, and the block registry system. It serves as a
 * starting point for building the voxel game.
 * 
 * <p>Controls:
 * <ul>
 *   <li>Click to grab mouse for camera look</li>
 *   <li>WASD - Move camera</li>
 *   <li>Space/Shift - Move up/down</li>
 *   <li>Escape - Release mouse / Close game</li>
 *   <li>F1 - Toggle wireframe mode</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class DemoGame implements IGameLogic {
    
    /** Movement speed in units per second. */
    private static final float MOVEMENT_SPEED = 5.0f;
    
    /** Mouse sensitivity for camera rotation. */
    private static final float MOUSE_SENSITIVITY = 0.1f;
    
    /** The renderer instance. */
    private Renderer renderer;
    
    /** The shader program. */
    private ShaderProgram shaderProgram;
    
    /** The demo cube mesh. */
    private Mesh cubeMesh;
    
    /** The camera. */
    private Camera camera;
    
    /** Transformation utility. */
    private Transformation transformation;
    
    /** Texture manager. */
    private TextureManager textureManager;
    
    /** Block registry. */
    private BlockRegistry blockRegistry;
    
    /** Loaded textures for blocks. */
    private Texture dirtTexture;
    private Texture grassTopTexture;
    private Texture stoneTexture;
    private Texture cobblestoneTexture;
    private Texture oakPlanksTexture;
    
    /** Cube position. */
    private final Vector3f cubePosition;
    
    /** Cube rotation. */
    private final Vector3f cubeRotation;
    
    /** Cube scale. */
    private float cubeScale;
    
    /** Flag to track if left mouse was pressed last frame. */
    private boolean leftMouseWasPressed = false;
    
    /** Flag to track if F1 was pressed last frame. */
    private boolean f1WasPressed = false;
    
    /** Flag to track if Escape was pressed last frame. */
    private boolean escapeWasPressed = false;
    
    /**
     * Creates a new demo game.
     */
    public DemoGame() {
        this.cubePosition = new Vector3f(0, 0, -5);
        this.cubeRotation = new Vector3f(0, 0, 0);
        this.cubeScale = 1.0f;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Window window) throws Exception {
        Logger.info("Initializing Demo Game...");
        
        // Initialize renderer
        renderer = new Renderer();
        renderer.init();
        renderer.setClearColor(0.4f, 0.6f, 0.9f, 1.0f); // Sky blue
        
        // Initialize block registry
        Logger.info("Initializing block registry...");
        blockRegistry = BlockRegistry.getInstance();
        
        // Initialize texture manager and load textures
        Logger.info("Loading textures...");
        textureManager = TextureManager.getInstance();
        loadBlockTextures();
        
        // Load and compile shaders
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
        
        // Create cube mesh
        Logger.info("Creating cube mesh...");
        cubeMesh = createCubeMesh();
        
        // Initialize camera
        camera = new Camera();
        camera.setPosition(0, 2, 8);
        camera.updateProjection(70.0f, window.getAspectRatio(), 0.01f, 1000.0f);
        
        // Initialize transformation
        transformation = new Transformation();
        
        Logger.info("Demo Game initialized successfully!");
        Logger.info("Controls: Click to look around, WASD to move, Space/Shift for up/down, Escape to release mouse, F1 for wireframe");
    }
    
    /**
     * Loads all block textures from the registry.
     */
    private void loadBlockTextures() {
        // Load textures for the blocks we'll use
        dirtTexture = textureManager.getTexture("/assets/textures/VanillaPack/Blocks/dirt.png");
        grassTopTexture = textureManager.getTexture("/assets/textures/VanillaPack/Blocks/grass_block_top.png");
        stoneTexture = textureManager.getTexture("/assets/textures/VanillaPack/Blocks/stone.png");
        cobblestoneTexture = textureManager.getTexture("/assets/textures/VanillaPack/Blocks/cobblestone.png");
        oakPlanksTexture = textureManager.getTexture("/assets/textures/VanillaPack/Blocks/oak_planks.png");
        
        Logger.info("Loaded %d textures", textureManager.getCacheSize());
    }
    
    /**
     * Creates a cube mesh with positions and texture coordinates.
     * 
     * @return The cube mesh
     */
    private Mesh createCubeMesh() {
        // Cube vertices (position)
        float[] positions = {
            // Front face (South, +Z)
            -0.5f, -0.5f,  0.5f,
             0.5f, -0.5f,  0.5f,
             0.5f,  0.5f,  0.5f,
            -0.5f,  0.5f,  0.5f,
            // Back face (North, -Z)
            -0.5f, -0.5f, -0.5f,
             0.5f, -0.5f, -0.5f,
             0.5f,  0.5f, -0.5f,
            -0.5f,  0.5f, -0.5f,
            // Top face (+Y)
            -0.5f,  0.5f, -0.5f,
             0.5f,  0.5f, -0.5f,
             0.5f,  0.5f,  0.5f,
            -0.5f,  0.5f,  0.5f,
            // Bottom face (-Y)
            -0.5f, -0.5f, -0.5f,
             0.5f, -0.5f, -0.5f,
             0.5f, -0.5f,  0.5f,
            -0.5f, -0.5f,  0.5f,
            // Right face (East, +X)
             0.5f, -0.5f, -0.5f,
             0.5f,  0.5f, -0.5f,
             0.5f,  0.5f,  0.5f,
             0.5f, -0.5f,  0.5f,
            // Left face (West, -X)
            -0.5f, -0.5f, -0.5f,
            -0.5f,  0.5f, -0.5f,
            -0.5f,  0.5f,  0.5f,
            -0.5f, -0.5f,  0.5f
        };
        
        // Texture coordinates
        float[] texCoords = {
            // Front face
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f,
            // Back face
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            // Top face
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f,
            // Bottom face
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            // Right face
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f,
            0.0f, 1.0f,
            // Left face
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
        };
        
        // Indices
        int[] indices = {
            // Front face
            0, 1, 2, 2, 3, 0,
            // Back face
            4, 7, 6, 6, 5, 4,
            // Top face
            8, 9, 10, 10, 11, 8,
            // Bottom face
            12, 15, 14, 14, 13, 12,
            // Right face
            16, 19, 18, 18, 17, 16,
            // Left face
            20, 21, 22, 22, 23, 20
        };
        
        return new Mesh(positions, texCoords, null, indices);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void input(Window window, InputManager inputManager) {
        // Toggle cursor grab with left mouse click (like Minecraft)
        boolean leftMousePressed = inputManager.isMouseButtonPressed(GLFW_MOUSE_BUTTON_LEFT);
        if (leftMousePressed && !leftMouseWasPressed && !inputManager.isCursorGrabbed()) {
            inputManager.setCursorGrabbed(true);
            inputManager.centerCursor(window);
        }
        leftMouseWasPressed = leftMousePressed;
        
        // Escape key handling: release cursor first, then close window
        boolean escapePressed = window.isKeyPressed(GLFW_KEY_ESCAPE);
        if (escapePressed && !escapeWasPressed) {
            if (inputManager.isCursorGrabbed()) {
                // First press: release cursor
                inputManager.setCursorGrabbed(false);
            } else {
                // Second press (cursor already released): close window
                window.setWindowShouldClose(true);
            }
        }
        escapeWasPressed = escapePressed;
        
        // Toggle wireframe with F1 (with proper toggle detection)
        boolean f1Pressed = window.isKeyPressed(GLFW_KEY_F1);
        if (f1Pressed && !f1WasPressed) {
            renderer.toggleWireframe();
        }
        f1WasPressed = f1Pressed;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void update(float deltaTime, InputManager inputManager) {
        // Rotate the floating cube
        cubeRotation.y += 30.0f * deltaTime;
        if (cubeRotation.y >= 360) {
            cubeRotation.y -= 360;
        }
        
        // Camera movement
        float moveSpeed = MOVEMENT_SPEED * deltaTime;
        
        if (inputManager.isKeyPressed(GLFW_KEY_W)) {
            camera.move(0, 0, -moveSpeed);
        }
        if (inputManager.isKeyPressed(GLFW_KEY_S)) {
            camera.move(0, 0, moveSpeed);
        }
        if (inputManager.isKeyPressed(GLFW_KEY_A)) {
            camera.move(-moveSpeed, 0, 0);
        }
        if (inputManager.isKeyPressed(GLFW_KEY_D)) {
            camera.move(moveSpeed, 0, 0);
        }
        if (inputManager.isKeyPressed(GLFW_KEY_SPACE)) {
            camera.move(0, moveSpeed, 0);
        }
        if (inputManager.isKeyPressed(GLFW_KEY_LEFT_SHIFT)) {
            camera.move(0, -moveSpeed, 0);
        }
        
        // Camera rotation with mouse (when cursor is grabbed)
        if (inputManager.isCursorGrabbed()) {
            float rotX = (float) inputManager.getDeltaY() * MOUSE_SENSITIVITY;
            float rotY = (float) inputManager.getDeltaX() * MOUSE_SENSITIVITY;
            camera.rotate(rotX, rotY, 0);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void render(Window window) {
        renderer.prepare(window);
        
        // Update projection matrix if window was resized
        if (window.isResized()) {
            camera.updateProjection(70.0f, window.getAspectRatio(), 0.01f, 1000.0f);
        }
        
        // Update view matrix
        camera.updateViewMatrix();
        
        // Bind shader
        shaderProgram.bind();
        
        // Set projection and view matrices
        shaderProgram.setUniform("projectionMatrix", camera.getProjectionMatrix());
        shaderProgram.setUniform("viewMatrix", camera.getViewMatrix());
        shaderProgram.setUniform("textureSampler", 0);
        
        // Render textured floating cube (grass block)
        shaderProgram.setUniform("useTexture", true);
        shaderProgram.setUniform("color", new Vector3f(1.0f, 1.0f, 1.0f));
        grassTopTexture.bind(0);
        Matrix4f modelMatrix = transformation.getModelMatrix(cubePosition, cubeRotation, cubeScale);
        shaderProgram.setUniform("modelMatrix", modelMatrix);
        cubeMesh.render();
        
        // Render a floor of grass blocks
        Vector3f noRotation = new Vector3f(0, 0, 0);
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                Vector3f floorPos = new Vector3f(x, -1, z);
                Matrix4f floorModel = transformation.getModelMatrix(floorPos, noRotation, 1.0f);
                shaderProgram.setUniform("modelMatrix", floorModel);
                
                // Use grass for top layer
                grassTopTexture.bind(0);
                cubeMesh.render();
            }
        }
        
        // Render dirt layer below
        dirtTexture.bind(0);
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                for (int y = -2; y >= -3; y--) {
                    Vector3f dirtPos = new Vector3f(x, y, z);
                    Matrix4f dirtModel = transformation.getModelMatrix(dirtPos, noRotation, 1.0f);
                    shaderProgram.setUniform("modelMatrix", dirtModel);
                    cubeMesh.render();
                }
            }
        }
        
        // Render some stone underground
        stoneTexture.bind(0);
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                for (int y = -4; y >= -5; y--) {
                    Vector3f stonePos = new Vector3f(x, y, z);
                    Matrix4f stoneModel = transformation.getModelMatrix(stonePos, noRotation, 1.0f);
                    shaderProgram.setUniform("modelMatrix", stoneModel);
                    cubeMesh.render();
                }
            }
        }
        
        // Render some decorative blocks
        // Oak planks structure
        oakPlanksTexture.bind(0);
        for (int y = 0; y <= 3; y++) {
            Vector3f pillarPos = new Vector3f(5, y, -5);
            Matrix4f pillarModel = transformation.getModelMatrix(pillarPos, noRotation, 1.0f);
            shaderProgram.setUniform("modelMatrix", pillarModel);
            cubeMesh.render();
        }
        
        // Cobblestone pile
        cobblestoneTexture.bind(0);
        Vector3f cobblePos1 = new Vector3f(-5, 0, -3);
        shaderProgram.setUniform("modelMatrix", transformation.getModelMatrix(cobblePos1, noRotation, 1.0f));
        cubeMesh.render();
        
        Vector3f cobblePos2 = new Vector3f(-4, 0, -3);
        shaderProgram.setUniform("modelMatrix", transformation.getModelMatrix(cobblePos2, noRotation, 1.0f));
        cubeMesh.render();
        
        Vector3f cobblePos3 = new Vector3f(-5, 1, -3);
        shaderProgram.setUniform("modelMatrix", transformation.getModelMatrix(cobblePos3, noRotation, 1.0f));
        cubeMesh.render();
        
        // Unbind texture and shader
        grassTopTexture.unbind();
        shaderProgram.unbind();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanup() {
        Logger.info("Cleaning up Demo Game...");
        
        if (cubeMesh != null) {
            cubeMesh.cleanup();
        }
        if (shaderProgram != null) {
            shaderProgram.cleanup();
        }
        if (textureManager != null) {
            textureManager.cleanup();
        }
        if (renderer != null) {
            renderer.cleanup();
        }
        
        Logger.info("Demo Game cleanup complete.");
    }
}
