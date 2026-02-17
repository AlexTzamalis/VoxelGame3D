package me.alextzamalis.core;

/**
 * High-precision timer for game loop timing.
 * 
 * <p>This class provides accurate time measurement using the system's
 * high-resolution timer. It tracks elapsed time between frames and
 * provides methods for calculating delta time.
 * 
 * <p>The timer uses nanosecond precision and converts to seconds
 * for ease of use in game calculations.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class Timer {
    
    /** The time of the last loop iteration in seconds. */
    private double lastLoopTime;
    
    /**
     * Creates a new timer.
     * 
     * <p>The timer is not initialized until {@link #init()} is called.
     */
    public Timer() {
        this.lastLoopTime = 0;
    }
    
    /**
     * Initializes the timer.
     * 
     * <p>This method sets the initial time reference. Should be called
     * once before the game loop starts.
     */
    public void init() {
        lastLoopTime = getTime();
    }
    
    /**
     * Gets the current time in seconds.
     * 
     * <p>Uses the system's high-resolution timer for accurate timing.
     * 
     * @return The current time in seconds
     */
    public double getTime() {
        return System.nanoTime() / 1_000_000_000.0;
    }
    
    /**
     * Gets the elapsed time since the last call to this method.
     * 
     * <p>This method calculates the time delta and updates the
     * last loop time. Should be called once per frame.
     * 
     * @return The elapsed time in seconds since the last call
     */
    public float getElapsedTime() {
        double time = getTime();
        float elapsedTime = (float) (time - lastLoopTime);
        lastLoopTime = time;
        return elapsedTime;
    }
    
    /**
     * Gets the time of the last loop iteration.
     * 
     * @return The last loop time in seconds
     */
    public double getLastLoopTime() {
        return lastLoopTime;
    }
}

