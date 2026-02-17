package me.alextzamalis.world;

import me.alextzamalis.util.Logger;
import me.alextzamalis.voxel.BlockRegistry;
import me.alextzamalis.voxel.Chunk;
import me.alextzamalis.voxel.World;
import me.alextzamalis.world.noise.PerlinNoise;

import java.util.ArrayList;
import java.util.List;

/**
 * Zone-based generator that creates distinct zones with different generation parameters.
 * 
 * <p>This generator avoids the "infinite ocean" and "monotonous biome" problems by
 * creating distinct zones where:
 * <ul>
 *   <li>Noise parameters are different (hills, plains, mountains)</li>
 *   <li>Block palettes are different (grass, sand, stone)</li>
 *   <li>Prefab frequencies are different (trees, structures)</li>
 * </ul>
 * 
 * <p>Zones transition smoothly to avoid jarring boundaries.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class ZoneGenerator implements WorldGenerator {
    
    /** Zone definition. */
    public static class Zone {
        /** Zone name. */
        public final String name;
        
        /** Zone center (world coordinates). */
        public final int centerX, centerZ;
        
        /** Zone radius. */
        public final int radius;
        
        /** Base height for this zone. */
        public final int baseHeight;
        
        /** Height variation. */
        public final int heightVariation;
        
        /** Noise scale. */
        public final float noiseScale;
        
        /** Block palette (surface, subsurface, deep). */
        public final int surfaceBlockId, subsurfaceBlockId, deepBlockId;
        
        /** Prefab frequency multiplier. */
        public final float prefabFrequency;
        
        public Zone(String name, int centerX, int centerZ, int radius,
                   int baseHeight, int heightVariation, float noiseScale,
                   int surfaceBlockId, int subsurfaceBlockId, int deepBlockId,
                   float prefabFrequency) {
            this.name = name;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.radius = radius;
            this.baseHeight = baseHeight;
            this.heightVariation = heightVariation;
            this.noiseScale = noiseScale;
            this.surfaceBlockId = surfaceBlockId;
            this.subsurfaceBlockId = subsurfaceBlockId;
            this.deepBlockId = deepBlockId;
            this.prefabFrequency = prefabFrequency;
        }
    }
    
    /** World seed. */
    private final long seed;
    
    /** Perlin noise for terrain. */
    private final PerlinNoise terrainNoise;
    
    /** Zone noise (for zone boundaries). */
    private final PerlinNoise zoneNoise;
    
    /** Defined zones. */
    private final List<Zone> zones;
    
    /** Block registry. */
    private final BlockRegistry blockRegistry;
    
    /** Heuristic placement engine. */
    private HeuristicPlacementEngine placementEngine;
    
    /**
     * Creates a new zone generator.
     * 
     * @param seed World seed
     */
    public ZoneGenerator(long seed) {
        this.seed = seed;
        this.terrainNoise = new PerlinNoise(seed);
        this.zoneNoise = new PerlinNoise(seed + 1000); // Different seed for zones
        this.zones = new ArrayList<>();
        this.blockRegistry = BlockRegistry.getInstance();
        
        // Define default zones
        defineDefaultZones();
    }
    
    /**
     * Defines default zones for world generation.
     */
    private void defineDefaultZones() {
        int grassId = blockRegistry.getBlockId("minecraft:grass_block");
        int dirtId = blockRegistry.getBlockId("minecraft:dirt");
        int stoneId = blockRegistry.getBlockId("minecraft:stone");
        
        // Plains zone (center, flat, grass)
        zones.add(new Zone("plains", 0, 0, 200,
            64, 8, 0.015f,
            grassId, dirtId, stoneId,
            0.3f)); // 30% prefab frequency
        
        // Hills zone (north, hilly, grass)
        zones.add(new Zone("hills", 0, 300, 150,
            80, 25, 0.02f,
            grassId, dirtId, stoneId,
            0.2f)); // 20% prefab frequency
        
        // Mountains zone (east, very high, stone)
        zones.add(new Zone("mountains", 400, 0, 200,
            100, 40, 0.025f,
            stoneId, stoneId, stoneId,
            0.1f)); // 10% prefab frequency (sparse)
        
        Logger.info("Defined %d zones for world generation", zones.size());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void generateChunk(Chunk chunk, World world) {
        // Initialize placement engine if needed
        if (placementEngine == null) {
            placementEngine = new HeuristicPlacementEngine(world, seed);
        }
        
        // Generate terrain using zone-based heightmap
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int worldX = chunk.getWorldX(x);
                int worldZ = chunk.getWorldZ(z);
                
                // Determine which zone(s) affect this position
                Zone primaryZone = getPrimaryZone(worldX, worldZ);
                Zone secondaryZone = getSecondaryZone(worldX, worldZ);
                
                // Blend zones for smooth transitions
                float blendFactor = getZoneBlendFactor(worldX, worldZ, primaryZone, secondaryZone);
                
                // Calculate height using blended zone parameters
                int height = calculateHeight(worldX, worldZ, primaryZone, secondaryZone, blendFactor);
                
                // Generate column with zone-specific blocks
                generateColumn(chunk, x, z, height, primaryZone, secondaryZone, blendFactor);
            }
        }
        
        // Place prefabs using heuristic engine
        placementEngine.placePrefabs(chunk);
    }
    
    /**
     * Gets the primary zone for a position.
     * 
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @return Primary zone, or null if none
     */
    private Zone getPrimaryZone(int worldX, int worldZ) {
        Zone closest = null;
        float closestDist = Float.MAX_VALUE;
        
        for (Zone zone : zones) {
            float dist = (float) Math.sqrt(
                (worldX - zone.centerX) * (worldX - zone.centerX) +
                (worldZ - zone.centerZ) * (worldZ - zone.centerZ)
            );
            
            if (dist < zone.radius && dist < closestDist) {
                closest = zone;
                closestDist = dist;
            }
        }
        
        return closest;
    }
    
    /**
     * Gets the secondary zone for blending.
     * 
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @return Secondary zone, or null
     */
    private Zone getSecondaryZone(int worldX, int worldZ) {
        // Find second closest zone for blending
        Zone primary = getPrimaryZone(worldX, worldZ);
        if (primary == null) {
            return null;
        }
        
        Zone secondary = null;
        float secondaryDist = Float.MAX_VALUE;
        
        for (Zone zone : zones) {
            if (zone == primary) {
                continue;
            }
            
            float dist = (float) Math.sqrt(
                (worldX - zone.centerX) * (worldX - zone.centerX) +
                (worldZ - zone.centerZ) * (worldZ - zone.centerZ)
            );
            
            if (dist < zone.radius && dist < secondaryDist) {
                secondary = zone;
                secondaryDist = dist;
            }
        }
        
        return secondary;
    }
    
    /**
     * Gets the blend factor between two zones.
     * 
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @param primary Primary zone
     * @param secondary Secondary zone
     * @return Blend factor (0.0 = primary, 1.0 = secondary)
     */
    private float getZoneBlendFactor(int worldX, int worldZ, Zone primary, Zone secondary) {
        if (secondary == null) {
            return 0.0f;
        }
        
        // Calculate distance to zone edges
        float distToPrimary = (float) Math.sqrt(
            (worldX - primary.centerX) * (worldX - primary.centerX) +
            (worldZ - primary.centerZ) * (worldZ - primary.centerZ)
        );
        
        float distToSecondary = (float) Math.sqrt(
            (worldX - secondary.centerX) * (worldX - secondary.centerX) +
            (worldZ - secondary.centerZ) * (worldZ - secondary.centerZ)
        );
        
        // Blend based on proximity to zone boundaries
        float transitionWidth = 50.0f; // Smooth transition over 50 blocks
        float blend = Math.max(0.0f, Math.min(1.0f,
            (primary.radius - distToPrimary) / transitionWidth));
        
        return 1.0f - blend; // Invert: closer to primary = less blend
    }
    
    /**
     * Calculates terrain height using blended zone parameters.
     * 
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @param primary Primary zone
     * @param secondary Secondary zone
     * @param blendFactor Blend factor
     * @return Terrain height
     */
    private int calculateHeight(int worldX, int worldZ, Zone primary, Zone secondary, float blendFactor) {
        if (primary == null) {
            return 64; // Default height
        }
        
        // Get noise value
        float noiseValue = terrainNoise.noise(worldX * primary.noiseScale, worldZ * primary.noiseScale);
        
        // Calculate height from primary zone
        int primaryHeight = primary.baseHeight + (int) (noiseValue * primary.heightVariation);
        
        if (secondary == null || blendFactor < 0.01f) {
            return primaryHeight;
        }
        
        // Blend with secondary zone
        float secondaryNoise = terrainNoise.noise(worldX * secondary.noiseScale, worldZ * secondary.noiseScale);
        int secondaryHeight = secondary.baseHeight + (int) (secondaryNoise * secondary.heightVariation);
        
        return (int) (primaryHeight * (1.0f - blendFactor) + secondaryHeight * blendFactor);
    }
    
    /**
     * Generates a column of blocks.
     * 
     * @param chunk The chunk
     * @param x Local X coordinate
     * @param z Local Z coordinate
     * @param height Surface height
     * @param primary Primary zone
     * @param secondary Secondary zone
     * @param blendFactor Blend factor
     */
    private void generateColumn(Chunk chunk, int x, int z, int height,
                                Zone primary, Zone secondary, float blendFactor) {
        if (primary == null) {
            return;
        }
        
        // Determine block IDs (blend if secondary exists)
        int surfaceId = primary.surfaceBlockId;
        int subsurfaceId = primary.subsurfaceBlockId;
        int deepId = primary.deepBlockId;
        
        if (secondary != null && blendFactor > 0.3f) {
            // Blend block types in transition zones
            surfaceId = random.nextFloat() < blendFactor ? secondary.surfaceBlockId : primary.surfaceBlockId;
        }
        
        // Generate column
        for (int y = 0; y < height - 3 && y < Chunk.HEIGHT; y++) {
            chunk.setBlock(x, y, z, deepId);
        }
        
        for (int y = Math.max(0, height - 3); y < height && y < Chunk.HEIGHT; y++) {
            chunk.setBlock(x, y, z, subsurfaceId);
        }
        
        if (height >= 0 && height < Chunk.HEIGHT) {
            chunk.setBlock(x, height, z, surfaceId);
        }
    }
    
    /** Random for block blending. */
    private final java.util.Random random = new java.util.Random();
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "ZoneGenerator";
    }
}

