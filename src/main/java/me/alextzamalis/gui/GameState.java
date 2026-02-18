package me.alextzamalis.gui;

/**
 * Represents the current state of the game.
 * 
 * <p>The game transitions between these states based on user actions:
 * <ul>
 *   <li>MAIN_MENU: Initial state, shows title and main options</li>
 *   <li>WORLD_SELECT: Shows list of existing worlds</li>
 *   <li>WORLD_CREATE: Creating a new world (name, seed input)</li>
 *   <li>SETTINGS: Game settings menu</li>
 *   <li>LOADING: Loading/generating a world</li>
 *   <li>PLAYING: In-game, world is active</li>
 *   <li>PAUSED: In-game pause menu</li>
 * </ul>
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public enum GameState {
    /** Main menu with title and options. */
    MAIN_MENU,
    
    /** World selection screen. */
    WORLD_SELECT,
    
    /** World creation screen. */
    WORLD_CREATE,
    
    /** Settings menu. */
    SETTINGS,
    
    /** Loading screen while world generates. */
    LOADING,
    
    /** Actively playing in a world. */
    PLAYING,
    
    /** Game is paused (in-game menu). */
    PAUSED
}


