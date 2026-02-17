package me.alextzamalis.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple logging utility for the game engine.
 * 
 * <p>This class provides basic logging functionality with different log levels
 * (DEBUG, INFO, WARN, ERROR). Log messages are formatted with timestamps and
 * log levels for easy debugging.
 * 
 * <p>Usage example:
 * <pre>{@code
 * Logger.info("Game started");
 * Logger.debug("Player position: " + position);
 * Logger.error("Failed to load texture: " + filename);
 * }</pre>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class Logger {
    
    /** Log level enumeration. */
    public enum Level {
        /** Debug level for detailed debugging information. */
        DEBUG,
        /** Info level for general information. */
        INFO,
        /** Warning level for potential issues. */
        WARN,
        /** Error level for errors and exceptions. */
        ERROR
    }
    
    /** The current minimum log level. */
    private static Level currentLevel = Level.DEBUG;
    
    /** Date/time formatter for log timestamps. */
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    /** ANSI color codes for colored output. */
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN = "\u001B[32m";
    private static final String CYAN = "\u001B[36m";
    
    /** Flag to enable/disable colored output. */
    private static boolean colorEnabled = true;
    
    /**
     * Private constructor to prevent instantiation.
     */
    private Logger() {
    }
    
    /**
     * Sets the minimum log level.
     * 
     * <p>Messages below this level will not be logged.
     * 
     * @param level The minimum log level
     */
    public static void setLevel(Level level) {
        currentLevel = level;
    }
    
    /**
     * Gets the current log level.
     * 
     * @return The current minimum log level
     */
    public static Level getLevel() {
        return currentLevel;
    }
    
    /**
     * Enables or disables colored output.
     * 
     * @param enabled true to enable colors
     */
    public static void setColorEnabled(boolean enabled) {
        colorEnabled = enabled;
    }
    
    /**
     * Logs a debug message.
     * 
     * @param message The message to log
     */
    public static void debug(String message) {
        log(Level.DEBUG, message);
    }
    
    /**
     * Logs a debug message with format arguments.
     * 
     * @param format The format string
     * @param args The format arguments
     */
    public static void debug(String format, Object... args) {
        log(Level.DEBUG, String.format(format, args));
    }
    
    /**
     * Logs an info message.
     * 
     * @param message The message to log
     */
    public static void info(String message) {
        log(Level.INFO, message);
    }
    
    /**
     * Logs an info message with format arguments.
     * 
     * @param format The format string
     * @param args The format arguments
     */
    public static void info(String format, Object... args) {
        log(Level.INFO, String.format(format, args));
    }
    
    /**
     * Logs a warning message.
     * 
     * @param message The message to log
     */
    public static void warn(String message) {
        log(Level.WARN, message);
    }
    
    /**
     * Logs a warning message with format arguments.
     * 
     * @param format The format string
     * @param args The format arguments
     */
    public static void warn(String format, Object... args) {
        log(Level.WARN, String.format(format, args));
    }
    
    /**
     * Logs an error message.
     * 
     * @param message The message to log
     */
    public static void error(String message) {
        log(Level.ERROR, message);
    }
    
    /**
     * Logs an error message with format arguments.
     * 
     * @param format The format string
     * @param args The format arguments
     */
    public static void error(String format, Object... args) {
        log(Level.ERROR, String.format(format, args));
    }
    
    /**
     * Logs an error message with an exception.
     * 
     * @param message The message to log
     * @param throwable The exception to log
     */
    public static void error(String message, Throwable throwable) {
        log(Level.ERROR, message);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
    
    /**
     * Logs a message at the specified level.
     * 
     * @param level The log level
     * @param message The message to log
     */
    private static void log(Level level, String message) {
        if (level.ordinal() < currentLevel.ordinal()) {
            return;
        }
        
        String timestamp = LocalDateTime.now().format(formatter);
        String levelStr = String.format("%-5s", level.name());
        String color = getColor(level);
        
        String formattedMessage;
        if (colorEnabled) {
            formattedMessage = String.format("%s[%s] [%s%s%s] %s%s", 
                CYAN, timestamp, color, levelStr, CYAN, RESET, message);
        } else {
            formattedMessage = String.format("[%s] [%s] %s", timestamp, levelStr, message);
        }
        
        if (level == Level.ERROR) {
            System.err.println(formattedMessage);
        } else {
            System.out.println(formattedMessage);
        }
    }
    
    /**
     * Gets the ANSI color code for a log level.
     * 
     * @param level The log level
     * @return The ANSI color code
     */
    private static String getColor(Level level) {
        return switch (level) {
            case DEBUG -> CYAN;
            case INFO -> GREEN;
            case WARN -> YELLOW;
            case ERROR -> RED;
        };
    }
}


