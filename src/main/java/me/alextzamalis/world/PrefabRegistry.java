package me.alextzamalis.world;

import me.alextzamalis.util.Logger;
import me.alextzamalis.voxel.BlockRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for managing prefabs.
 * 
 * <p>This registry stores all available prefabs and provides methods to
 * query and retrieve them based on placement requirements.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class PrefabRegistry {
    
    /** Singleton instance. */
    private static PrefabRegistry instance;
    
    /** All registered prefabs by name. */
    private final Map<String, Prefab> prefabs;
    
    /** Prefabs by requirement type (for fast lookup). */
    private final Map<Prefab.PlacementRequirement.Type, List<Prefab>> prefabsByRequirement;
    
    /**
     * Gets the singleton instance.
     * 
     * @return The registry instance
     */
    public static PrefabRegistry getInstance() {
        if (instance == null) {
            instance = new PrefabRegistry();
            instance.registerDefaultPrefabs();
        }
        return instance;
    }
    
    /**
     * Creates a new prefab registry.
     */
    private PrefabRegistry() {
        this.prefabs = new HashMap<>();
        this.prefabsByRequirement = new HashMap<>();
    }
    
    /**
     * Registers a prefab.
     * 
     * @param prefab The prefab to register
     */
    public void register(Prefab prefab) {
        prefabs.put(prefab.getName(), prefab);
        
        // Index by requirements
        for (Prefab.PlacementRequirement req : prefab.getRequirements()) {
            prefabsByRequirement.computeIfAbsent(req.type, k -> new ArrayList<>()).add(prefab);
        }
        
        Logger.debug("Registered prefab: %s", prefab.getName());
    }
    
    /**
     * Gets a prefab by name.
     * 
     * @param name The prefab name
     * @return The prefab, or null if not found
     */
    public Prefab getPrefab(String name) {
        return prefabs.get(name);
    }
    
    /**
     * Gets all prefabs that have a specific requirement type.
     * 
     * @param requirementType The requirement type
     * @return List of matching prefabs
     */
    public List<Prefab> getPrefabsByRequirement(Prefab.PlacementRequirement.Type requirementType) {
        return prefabsByRequirement.getOrDefault(requirementType, new ArrayList<>());
    }
    
    /**
     * Gets all registered prefabs.
     * 
     * @return List of all prefabs
     */
    public List<Prefab> getAllPrefabs() {
        return new ArrayList<>(prefabs.values());
    }
    
    /**
     * Registers default prefabs (trees, structures, etc.).
     */
    private void registerDefaultPrefabs() {
        BlockRegistry blockRegistry = BlockRegistry.getInstance();
        
        // Oak Tree
        Prefab oakTree = new Prefab("oak_tree", 5, 8, 5);
        int logId = blockRegistry.getBlockId("minecraft:oak_log");
        int leavesId = blockRegistry.getBlockId("minecraft:oak_leaves");
        
        // Trunk (center, 1x1x8)
        for (int y = 0; y < 6; y++) {
            oakTree.addBlock(2, y, 2, logId);
        }
        
        // Leaves (simple sphere)
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                for (int y = 4; y < 8; y++) {
                    if ((x - 2) * (x - 2) + (z - 2) * (z - 2) + (y - 6) * (y - 6) <= 4) {
                        if (x != 2 || z != 2 || y < 4) { // Don't place leaves where trunk is
                            oakTree.addBlock(x, y, z, leavesId);
                        }
                    }
                }
            }
        }
        oakTree.addRequirement(new Prefab.PlacementRequirement(Prefab.PlacementRequirement.Type.ON_SOLID_GROUND));
        oakTree.setFrequency(0.3f); // 30% chance when requirements met
        register(oakTree);
        
        // Stone Bridge (spans gaps)
        Prefab bridge = new Prefab("stone_bridge", 3, 1, 5);
        int stoneId = blockRegistry.getBlockId("minecraft:stone");
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 5; z++) {
                bridge.addBlock(x, 0, z, stoneId);
            }
        }
        bridge.addRequirement(new Prefab.PlacementRequirement(Prefab.PlacementRequirement.Type.GAP_BETWEEN, 5.0f));
        bridge.setFrequency(0.8f); // 80% chance when gap detected
        register(bridge);
        
        // Simple Cave Entrance
        Prefab cave = new Prefab("cave_entrance", 3, 3, 3);
        int airId = blockRegistry.getBlockId("minecraft:air");
        // Create a 3x3x3 hollow cube (cave entrance)
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    // Only hollow out the center
                    if (x == 1 && y == 1 && z == 1) {
                        cave.addBlock(x, y, z, airId);
                    }
                }
            }
        }
        cave.addRequirement(new Prefab.PlacementRequirement(Prefab.PlacementRequirement.Type.ON_SOLID_GROUND));
        cave.setFrequency(0.1f); // 10% chance
        register(cave);
        
        Logger.info("Registered %d default prefabs", prefabs.size());
    }
}

