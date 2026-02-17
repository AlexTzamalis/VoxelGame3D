package me.alextzamalis.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Utility class for creating transformation matrices.
 * 
 * <p>This class provides methods for creating model, view, and projection
 * matrices used in 3D rendering. It follows OpenGL conventions for
 * matrix operations.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class Transformation {
    
    /** Reusable model matrix to avoid allocations. */
    private final Matrix4f modelMatrix;
    
    /** Reusable model-view matrix to avoid allocations. */
    private final Matrix4f modelViewMatrix;
    
    /**
     * Creates a new transformation utility.
     */
    public Transformation() {
        this.modelMatrix = new Matrix4f();
        this.modelViewMatrix = new Matrix4f();
    }
    
    /**
     * Creates a model matrix from position, rotation, and scale.
     * 
     * @param position The position in world space
     * @param rotation The rotation in degrees (x, y, z)
     * @param scale The scale factor
     * @return The model matrix
     */
    public Matrix4f getModelMatrix(Vector3f position, Vector3f rotation, float scale) {
        modelMatrix.identity();
        modelMatrix.translate(position);
        modelMatrix.rotateX((float) Math.toRadians(rotation.x));
        modelMatrix.rotateY((float) Math.toRadians(rotation.y));
        modelMatrix.rotateZ((float) Math.toRadians(rotation.z));
        modelMatrix.scale(scale);
        return modelMatrix;
    }
    
    /**
     * Creates a model matrix from position, rotation, and non-uniform scale.
     * 
     * @param position The position in world space
     * @param rotation The rotation in degrees (x, y, z)
     * @param scale The scale factors (x, y, z)
     * @return The model matrix
     */
    public Matrix4f getModelMatrix(Vector3f position, Vector3f rotation, Vector3f scale) {
        modelMatrix.identity();
        modelMatrix.translate(position);
        modelMatrix.rotateX((float) Math.toRadians(rotation.x));
        modelMatrix.rotateY((float) Math.toRadians(rotation.y));
        modelMatrix.rotateZ((float) Math.toRadians(rotation.z));
        modelMatrix.scale(scale);
        return modelMatrix;
    }
    
    /**
     * Creates a model-view matrix by combining model and view matrices.
     * 
     * @param modelMatrix The model matrix
     * @param viewMatrix The view matrix from the camera
     * @return The combined model-view matrix
     */
    public Matrix4f getModelViewMatrix(Matrix4f modelMatrix, Matrix4f viewMatrix) {
        return viewMatrix.mul(modelMatrix, modelViewMatrix);
    }
    
    /**
     * Creates a view matrix for a camera.
     * 
     * @param position The camera position
     * @param rotation The camera rotation (pitch, yaw, roll)
     * @param dest The destination matrix
     * @return The view matrix
     */
    public static Matrix4f getViewMatrix(Vector3f position, Vector3f rotation, Matrix4f dest) {
        dest.identity();
        dest.rotate((float) Math.toRadians(rotation.x), new Vector3f(1, 0, 0));
        dest.rotate((float) Math.toRadians(rotation.y), new Vector3f(0, 1, 0));
        dest.rotate((float) Math.toRadians(rotation.z), new Vector3f(0, 0, 1));
        dest.translate(-position.x, -position.y, -position.z);
        return dest;
    }
    
    /**
     * Creates a perspective projection matrix.
     * 
     * @param fov Field of view in degrees
     * @param aspectRatio Aspect ratio (width / height)
     * @param nearPlane Near clipping plane
     * @param farPlane Far clipping plane
     * @param dest The destination matrix
     * @return The projection matrix
     */
    public static Matrix4f getProjectionMatrix(float fov, float aspectRatio, float nearPlane, float farPlane, Matrix4f dest) {
        dest.identity();
        dest.perspective((float) Math.toRadians(fov), aspectRatio, nearPlane, farPlane);
        return dest;
    }
    
    /**
     * Creates an orthographic projection matrix.
     * 
     * @param left Left bound
     * @param right Right bound
     * @param bottom Bottom bound
     * @param top Top bound
     * @param nearPlane Near clipping plane
     * @param farPlane Far clipping plane
     * @param dest The destination matrix
     * @return The orthographic projection matrix
     */
    public static Matrix4f getOrthoProjectionMatrix(float left, float right, float bottom, float top, 
                                                      float nearPlane, float farPlane, Matrix4f dest) {
        dest.identity();
        dest.ortho(left, right, bottom, top, nearPlane, farPlane);
        return dest;
    }
}

