package me.alextzamalis.world;

import me.alextzamalis.voxel.Chunk;
import me.alextzamalis.voxel.World;

/**
 * Interface for world generators.
 * 
 * <p>World generators are responsible for populating chunks with blocks
 * based on various algorithms. This interface allows for different
 * generation strategies to be plugged into the world system.
 * 
 * <p>Implementations might include:
 * <ul>
 *   <li>Flat world generator - simple flat terrain</li>
 *   <li>Perlin noise generator - realistic terrain with hills</li>
 *   <li>Biome-based generator - different biomes with unique features</li>
 *   <li>Structure generator - adds trees, caves, buildings, etc.</li>
 * </ul>
 * 
 * <p>Generators can be combined using decorator pattern for complex worlds.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public interface WorldGenerator {
    
    /**
     * Generates terrain for a chunk.
     * 
     * <p>This method should populate the chunk with blocks based on the
     * generator's algorithm. The world reference can be used to access
     * neighboring chunks or world properties like seed.
     * 
     * @param chunk The chunk to generate
     * @param world The world the chunk belongs to
     */
    void generateChunk(Chunk chunk, World world);
    
    /**
     * Gets the name of this generator.
     * 
     * @return The generator name
     */
    default String getName() {
        return getClass().getSimpleName();
    }
}


