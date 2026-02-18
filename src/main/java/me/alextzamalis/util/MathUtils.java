package me.alextzamalis.util;

/**
 * Mathematical utility functions for the game engine.
 * 
 * <p>This class provides common mathematical operations used throughout
 * the engine, including clamping, interpolation, and noise functions.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class MathUtils {
    
    /** Pi constant. */
    public static final float PI = (float) Math.PI;
    
    /** Two times Pi. */
    public static final float TWO_PI = PI * 2;
    
    /** Half of Pi. */
    public static final float HALF_PI = PI / 2;
    
    /** Degrees to radians conversion factor. */
    public static final float DEG_TO_RAD = PI / 180.0f;
    
    /** Radians to degrees conversion factor. */
    public static final float RAD_TO_DEG = 180.0f / PI;
    
    /**
     * Private constructor to prevent instantiation.
     */
    private MathUtils() {
    }
    
    /**
     * Clamps a value between a minimum and maximum.
     * 
     * @param value The value to clamp
     * @param min The minimum value
     * @param max The maximum value
     * @return The clamped value
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Clamps an integer value between a minimum and maximum.
     * 
     * @param value The value to clamp
     * @param min The minimum value
     * @param max The maximum value
     * @return The clamped value
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Linear interpolation between two values.
     * 
     * @param a The start value
     * @param b The end value
     * @param t The interpolation factor (0-1)
     * @return The interpolated value
     */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
    
    /**
     * Smooth step interpolation (Hermite interpolation).
     * 
     * @param edge0 The lower edge
     * @param edge1 The upper edge
     * @param x The input value
     * @return The smoothly interpolated value
     */
    public static float smoothstep(float edge0, float edge1, float x) {
        float t = clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }
    
    /**
     * Smoother step interpolation (Ken Perlin's improved version).
     * 
     * @param edge0 The lower edge
     * @param edge1 The upper edge
     * @param x The input value
     * @return The smoothly interpolated value
     */
    public static float smootherstep(float edge0, float edge1, float x) {
        float t = clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f);
    }
    
    /**
     * Converts degrees to radians.
     * 
     * @param degrees The angle in degrees
     * @return The angle in radians
     */
    public static float toRadians(float degrees) {
        return degrees * DEG_TO_RAD;
    }
    
    /**
     * Converts radians to degrees.
     * 
     * @param radians The angle in radians
     * @return The angle in degrees
     */
    public static float toDegrees(float radians) {
        return radians * RAD_TO_DEG;
    }
    
    /**
     * Calculates the floor modulus (always positive).
     * 
     * @param x The dividend
     * @param y The divisor
     * @return The floor modulus
     */
    public static int floorMod(int x, int y) {
        return Math.floorMod(x, y);
    }
    
    /**
     * Calculates the floor division.
     * 
     * @param x The dividend
     * @param y The divisor
     * @return The floor division result
     */
    public static int floorDiv(int x, int y) {
        return Math.floorDiv(x, y);
    }
    
    /**
     * Checks if a value is within a range (inclusive).
     * 
     * @param value The value to check
     * @param min The minimum value
     * @param max The maximum value
     * @return true if the value is within the range
     */
    public static boolean inRange(float value, float min, float max) {
        return value >= min && value <= max;
    }
    
    /**
     * Maps a value from one range to another.
     * 
     * @param value The input value
     * @param inMin The input range minimum
     * @param inMax The input range maximum
     * @param outMin The output range minimum
     * @param outMax The output range maximum
     * @return The mapped value
     */
    public static float map(float value, float inMin, float inMax, float outMin, float outMax) {
        return outMin + (outMax - outMin) * ((value - inMin) / (inMax - inMin));
    }
    
    /**
     * Returns the sign of a value.
     * 
     * @param value The input value
     * @return -1, 0, or 1
     */
    public static int sign(float value) {
        if (value > 0) return 1;
        if (value < 0) return -1;
        return 0;
    }
    
    /**
     * Calculates the distance between two 2D points.
     * 
     * @param x1 First point X
     * @param y1 First point Y
     * @param x2 Second point X
     * @param y2 Second point Y
     * @return The distance
     */
    public static float distance2D(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Calculates the distance between two 3D points.
     * 
     * @param x1 First point X
     * @param y1 First point Y
     * @param z1 First point Z
     * @param x2 Second point X
     * @param y2 Second point Y
     * @param z2 Second point Z
     * @return The distance
     */
    public static float distance3D(float x1, float y1, float z1, float x2, float y2, float z2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * Calculates the squared distance between two 3D points (faster, no sqrt).
     * 
     * @param x1 First point X
     * @param y1 First point Y
     * @param z1 First point Z
     * @param x2 Second point X
     * @param y2 Second point Y
     * @param z2 Second point Z
     * @return The squared distance
     */
    public static float distanceSquared3D(float x1, float y1, float z1, float x2, float y2, float z2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        return dx * dx + dy * dy + dz * dz;
    }
}


