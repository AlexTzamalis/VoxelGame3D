package me.alextzamalis.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * First-person camera for 3D rendering.
 * 
 * <p>This class manages the camera's position, rotation, and generates
 * the view and projection matrices needed for rendering. It supports
 * first-person style movement with mouse look.
 * 
 * <p>The camera uses a right-handed coordinate system where:
 * <ul>
 *   <li>+X is right</li>
 *   <li>+Y is up</li>
 *   <li>-Z is forward (into the screen)</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class Camera {
    
    /** The camera position in world space. */
    private final Vector3f position;
    
    /** The camera rotation (pitch, yaw, roll) in degrees. */
    private final Vector3f rotation;
    
    /** The view matrix (camera transformation). */
    private final Matrix4f viewMatrix;
    
    /** The projection matrix. */
    private final Matrix4f projectionMatrix;
    
    /** Field of view in degrees. */
    private float fov;
    
    /** Near clipping plane distance. */
    private float nearPlane;
    
    /** Far clipping plane distance. */
    private float farPlane;
    
    /** Aspect ratio (width / height). */
    private float aspectRatio;
    
    /**
     * Creates a new camera at the origin.
     */
    public Camera() {
        this.position = new Vector3f(0, 0, 0);
        this.rotation = new Vector3f(0, 0, 0);
        this.viewMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();
        this.fov = 70.0f;
        this.nearPlane = 0.01f;
        this.farPlane = 1000.0f;
        this.aspectRatio = 16.0f / 9.0f;
    }
    
    /**
     * Creates a new camera at the specified position.
     * 
     * @param position The initial camera position
     */
    public Camera(Vector3f position) {
        this();
        this.position.set(position);
    }
    
    /**
     * Creates a new camera with the specified position and rotation.
     * 
     * @param position The initial camera position
     * @param rotation The initial camera rotation (pitch, yaw, roll)
     */
    public Camera(Vector3f position, Vector3f rotation) {
        this(position);
        this.rotation.set(rotation);
    }
    
    /**
     * Updates the projection matrix with the specified parameters.
     * 
     * @param fov Field of view in degrees
     * @param aspectRatio Aspect ratio (width / height)
     * @param nearPlane Near clipping plane distance
     * @param farPlane Far clipping plane distance
     */
    public void updateProjection(float fov, float aspectRatio, float nearPlane, float farPlane) {
        this.fov = fov;
        this.aspectRatio = aspectRatio;
        this.nearPlane = nearPlane;
        this.farPlane = farPlane;
        
        projectionMatrix.identity();
        projectionMatrix.perspective((float) Math.toRadians(fov), aspectRatio, nearPlane, farPlane);
    }
    
    /**
     * Updates the view matrix based on current position and rotation.
     * 
     * @return The updated view matrix
     */
    public Matrix4f updateViewMatrix() {
        viewMatrix.identity();
        
        // Apply rotation (pitch, then yaw)
        viewMatrix.rotate((float) Math.toRadians(rotation.x), new Vector3f(1, 0, 0));
        viewMatrix.rotate((float) Math.toRadians(rotation.y), new Vector3f(0, 1, 0));
        viewMatrix.rotate((float) Math.toRadians(rotation.z), new Vector3f(0, 0, 1));
        
        // Apply translation (negative because we move the world, not the camera)
        viewMatrix.translate(-position.x, -position.y, -position.z);
        
        return viewMatrix;
    }
    
    /**
     * Moves the camera relative to its current rotation.
     * 
     * @param offsetX Movement along the camera's right axis
     * @param offsetY Movement along the camera's up axis
     * @param offsetZ Movement along the camera's forward axis
     */
    public void move(float offsetX, float offsetY, float offsetZ) {
        if (offsetZ != 0) {
            position.x += (float) Math.sin(Math.toRadians(rotation.y)) * -1.0f * offsetZ;
            position.z += (float) Math.cos(Math.toRadians(rotation.y)) * offsetZ;
        }
        if (offsetX != 0) {
            position.x += (float) Math.sin(Math.toRadians(rotation.y - 90)) * -1.0f * offsetX;
            position.z += (float) Math.cos(Math.toRadians(rotation.y - 90)) * offsetX;
        }
        position.y += offsetY;
    }
    
    /**
     * Rotates the camera.
     * 
     * @param offsetX Pitch offset in degrees (looking up/down)
     * @param offsetY Yaw offset in degrees (looking left/right)
     * @param offsetZ Roll offset in degrees (tilting)
     */
    public void rotate(float offsetX, float offsetY, float offsetZ) {
        rotation.x += offsetX;
        rotation.y += offsetY;
        rotation.z += offsetZ;
        
        // Clamp pitch to prevent flipping
        if (rotation.x > 90) {
            rotation.x = 90;
        } else if (rotation.x < -90) {
            rotation.x = -90;
        }
        
        // Normalize yaw to 0-360
        while (rotation.y >= 360) {
            rotation.y -= 360;
        }
        while (rotation.y < 0) {
            rotation.y += 360;
        }
    }
    
    /**
     * Sets the camera position.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }
    
    /**
     * Sets the camera position.
     * 
     * @param position The new position
     */
    public void setPosition(Vector3f position) {
        this.position.set(position);
    }
    
    /**
     * Sets the camera rotation.
     * 
     * @param pitch Pitch in degrees (looking up/down)
     * @param yaw Yaw in degrees (looking left/right)
     * @param roll Roll in degrees (tilting)
     */
    public void setRotation(float pitch, float yaw, float roll) {
        rotation.set(pitch, yaw, roll);
    }
    
    /**
     * Sets the camera rotation.
     * 
     * @param rotation The new rotation (pitch, yaw, roll)
     */
    public void setRotation(Vector3f rotation) {
        this.rotation.set(rotation);
    }
    
    /**
     * Gets the camera position.
     * 
     * @return The camera position
     */
    public Vector3f getPosition() {
        return position;
    }
    
    /**
     * Gets the camera rotation.
     * 
     * @return The camera rotation (pitch, yaw, roll)
     */
    public Vector3f getRotation() {
        return rotation;
    }
    
    /**
     * Gets the view matrix.
     * 
     * @return The view matrix
     */
    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }
    
    /**
     * Gets the projection matrix.
     * 
     * @return The projection matrix
     */
    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
    
    /**
     * Gets the field of view.
     * 
     * @return The field of view in degrees
     */
    public float getFov() {
        return fov;
    }
    
    /**
     * Sets the field of view.
     * 
     * @param fov The field of view in degrees
     */
    public void setFov(float fov) {
        this.fov = fov;
    }
    
    /**
     * Gets the forward direction vector.
     * 
     * @return The normalized forward direction
     */
    public Vector3f getForward() {
        float pitch = (float) Math.toRadians(rotation.x);
        float yaw = (float) Math.toRadians(rotation.y);
        
        float x = (float) (Math.sin(yaw) * Math.cos(pitch));
        float y = (float) -Math.sin(pitch);
        float z = (float) (-Math.cos(yaw) * Math.cos(pitch));
        
        return new Vector3f(x, y, z).normalize();
    }
    
    /**
     * Gets the right direction vector.
     * 
     * @return The normalized right direction
     */
    public Vector3f getRight() {
        float yaw = (float) Math.toRadians(rotation.y - 90);
        return new Vector3f((float) Math.sin(yaw), 0, (float) -Math.cos(yaw)).normalize();
    }
    
    /**
     * Gets the up direction vector.
     * 
     * @return The normalized up direction
     */
    public Vector3f getUp() {
        return getRight().cross(getForward()).normalize();
    }
}

