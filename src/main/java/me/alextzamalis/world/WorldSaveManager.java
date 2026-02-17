package me.alextzamalis.world;

import me.alextzamalis.util.Logger;
import me.alextzamalis.voxel.Chunk;
import me.alextzamalis.voxel.World;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Manages saving and loading world data to/from disk.
 * 
 * <p>World data is stored in the following structure:
 * <pre>
 * worlds/
 *   world_name/
 *     world.dat          - World metadata (name, seed, creation time)
 *     player.dat         - Player data (position, inventory, game mode)
 *     region/
 *       r.X.Z.dat        - Region files containing chunk data
 * </pre>
 * 
 * <p>Region files store multiple chunks (32x32 chunks per region) for efficiency.
 * Chunk data is compressed using GZIP for storage optimization.
 * 
 * @author AlexTzamalis
 * @since 1.0
 */
public class WorldSaveManager {
    
    /** Base directory for all worlds. */
    private static final String WORLDS_DIR = "worlds";
    
    /** World metadata filename. */
    private static final String WORLD_DATA_FILE = "world.dat";
    
    /** Player data filename. */
    private static final String PLAYER_DATA_FILE = "player.dat";
    
    /** Region folder name. */
    private static final String REGION_DIR = "region";
    
    /** Chunks per region (32x32). */
    private static final int REGION_SIZE = 32;
    
    /** Magic number for world files. */
    private static final int WORLD_MAGIC = 0x564F5845; // "VOXE"
    
    /** Current file format version. */
    private static final int FORMAT_VERSION = 1;
    
    /** Singleton instance. */
    private static WorldSaveManager instance;
    
    /** Path to the worlds directory. */
    private final Path worldsPath;
    
    /**
     * Private constructor for singleton.
     */
    private WorldSaveManager() {
        this.worldsPath = Paths.get(WORLDS_DIR);
        ensureWorldsDirectory();
    }
    
    /**
     * Gets the singleton instance.
     * 
     * @return The WorldSaveManager instance
     */
    public static synchronized WorldSaveManager getInstance() {
        if (instance == null) {
            instance = new WorldSaveManager();
        }
        return instance;
    }
    
    /**
     * Ensures the worlds directory exists.
     */
    private void ensureWorldsDirectory() {
        try {
            Files.createDirectories(worldsPath);
            Logger.info("Worlds directory: %s", worldsPath.toAbsolutePath());
        } catch (IOException e) {
            Logger.error("Failed to create worlds directory: %s", e.getMessage());
        }
    }
    
    /**
     * Gets the path to a specific world's directory.
     * 
     * @param worldName The world name
     * @return Path to the world directory
     */
    public Path getWorldPath(String worldName) {
        return worldsPath.resolve(sanitizeWorldName(worldName));
    }
    
    /**
     * Sanitizes a world name for use as a directory name.
     * 
     * @param name The world name
     * @return Sanitized name
     */
    private String sanitizeWorldName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-\\s]", "")
                   .trim()
                   .replaceAll("\\s+", "_")
                   .toLowerCase();
    }
    
    /**
     * Checks if a world exists.
     * 
     * @param worldName The world name
     * @return true if the world exists
     */
    public boolean worldExists(String worldName) {
        Path worldPath = getWorldPath(worldName);
        return Files.exists(worldPath.resolve(WORLD_DATA_FILE));
    }
    
    /**
     * Creates a new world with the given metadata.
     * 
     * @param metadata The world metadata
     * @return true if creation was successful
     */
    public boolean createWorld(WorldMetadata metadata) {
        Path worldPath = getWorldPath(metadata.getName());
        
        try {
            // Create world directory structure
            Files.createDirectories(worldPath);
            Files.createDirectories(worldPath.resolve(REGION_DIR));
            
            // Save world metadata
            saveWorldMetadata(metadata);
            
            Logger.info("Created new world: %s (seed: %d)", metadata.getName(), metadata.getSeed());
            return true;
        } catch (IOException e) {
            Logger.error("Failed to create world '%s': %s", metadata.getName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Saves world metadata to disk.
     * 
     * @param metadata The metadata to save
     * @throws IOException if save fails
     */
    public void saveWorldMetadata(WorldMetadata metadata) throws IOException {
        Path worldPath = getWorldPath(metadata.getName());
        Path metaFile = worldPath.resolve(WORLD_DATA_FILE);
        
        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(metaFile)))) {
            
            // Write header
            dos.writeInt(WORLD_MAGIC);
            dos.writeInt(FORMAT_VERSION);
            
            // Write metadata
            dos.writeUTF(metadata.getName());
            dos.writeLong(metadata.getSeed());
            dos.writeLong(metadata.getCreationTime());
            dos.writeLong(metadata.getLastPlayed());
            dos.writeInt(metadata.getGameMode());
            dos.writeLong(metadata.getPlayTime());
            
            Logger.debug("Saved world metadata: %s", metadata.getName());
        }
    }
    
    /**
     * Loads world metadata from disk.
     * 
     * @param worldName The world name
     * @return The metadata, or null if not found
     */
    public WorldMetadata loadWorldMetadata(String worldName) {
        Path worldPath = getWorldPath(worldName);
        Path metaFile = worldPath.resolve(WORLD_DATA_FILE);
        
        if (!Files.exists(metaFile)) {
            return null;
        }
        
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(metaFile)))) {
            
            // Read and verify header
            int magic = dis.readInt();
            if (magic != WORLD_MAGIC) {
                Logger.error("Invalid world file magic number");
                return null;
            }
            
            int version = dis.readInt();
            if (version > FORMAT_VERSION) {
                Logger.error("World file version %d is newer than supported %d", version, FORMAT_VERSION);
                return null;
            }
            
            // Read metadata
            String name = dis.readUTF();
            long seed = dis.readLong();
            long creationTime = dis.readLong();
            long lastPlayed = dis.readLong();
            int gameMode = dis.readInt();
            long playTime = dis.readLong();
            
            WorldMetadata metadata = new WorldMetadata(name, seed);
            metadata.setCreationTime(creationTime);
            metadata.setLastPlayed(lastPlayed);
            metadata.setGameMode(gameMode);
            metadata.setPlayTime(playTime);
            
            Logger.debug("Loaded world metadata: %s", name);
            return metadata;
            
        } catch (IOException e) {
            Logger.error("Failed to load world metadata: %s", e.getMessage());
            return null;
        }
    }
    
    /**
     * Saves player data for a world.
     * 
     * @param worldName The world name
     * @param playerData The player data
     * @throws IOException if save fails
     */
    public void savePlayerData(String worldName, PlayerData playerData) throws IOException {
        Path worldPath = getWorldPath(worldName);
        Path playerFile = worldPath.resolve(PLAYER_DATA_FILE);
        
        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(playerFile)))) {
            
            dos.writeInt(FORMAT_VERSION);
            
            // Position
            dos.writeDouble(playerData.getX());
            dos.writeDouble(playerData.getY());
            dos.writeDouble(playerData.getZ());
            
            // Rotation
            dos.writeFloat(playerData.getPitch());
            dos.writeFloat(playerData.getYaw());
            
            // Game mode
            dos.writeInt(playerData.getGameMode());
            
            // Health and hunger (for future)
            dos.writeFloat(playerData.getHealth());
            dos.writeFloat(playerData.getHunger());
            
            // Hotbar selection
            dos.writeInt(playerData.getSelectedSlot());
            
            Logger.debug("Saved player data for world: %s", worldName);
        }
    }
    
    /**
     * Loads player data for a world.
     * 
     * @param worldName The world name
     * @return The player data, or null if not found
     */
    public PlayerData loadPlayerData(String worldName) {
        Path worldPath = getWorldPath(worldName);
        Path playerFile = worldPath.resolve(PLAYER_DATA_FILE);
        
        if (!Files.exists(playerFile)) {
            return null;
        }
        
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(playerFile)))) {
            
            int version = dis.readInt();
            
            PlayerData data = new PlayerData();
            
            // Position
            data.setX(dis.readDouble());
            data.setY(dis.readDouble());
            data.setZ(dis.readDouble());
            
            // Rotation
            data.setPitch(dis.readFloat());
            data.setYaw(dis.readFloat());
            
            // Game mode
            data.setGameMode(dis.readInt());
            
            // Health and hunger
            data.setHealth(dis.readFloat());
            data.setHunger(dis.readFloat());
            
            // Hotbar selection
            data.setSelectedSlot(dis.readInt());
            
            Logger.debug("Loaded player data for world: %s", worldName);
            return data;
            
        } catch (IOException e) {
            Logger.error("Failed to load player data: %s", e.getMessage());
            return null;
        }
    }
    
    /**
     * Saves a chunk to disk.
     * 
     * @param worldName The world name
     * @param chunk The chunk to save
     * @throws IOException if save fails
     */
    public void saveChunk(String worldName, Chunk chunk) throws IOException {
        Path regionPath = getRegionPath(worldName, chunk.getChunkX(), chunk.getChunkZ());
        Files.createDirectories(regionPath.getParent());
        
        // For simplicity, we'll save each chunk as its own file
        // A more advanced system would use region files (like Minecraft's .mca)
        String chunkFileName = String.format("c.%d.%d.dat", chunk.getChunkX(), chunk.getChunkZ());
        Path chunkFile = regionPath.getParent().resolve(chunkFileName);
        
        try (DataOutputStream dos = new DataOutputStream(
                new GZIPOutputStream(
                        new BufferedOutputStream(Files.newOutputStream(chunkFile))))) {
            
            dos.writeInt(FORMAT_VERSION);
            dos.writeInt(chunk.getChunkX());
            dos.writeInt(chunk.getChunkZ());
            dos.writeBoolean(chunk.isGenerated());
            
            // Write block data
            // Use RLE compression for consecutive same blocks
            writeBlockDataRLE(dos, chunk);
            
            Logger.debug("Saved chunk (%d, %d)", chunk.getChunkX(), chunk.getChunkZ());
        }
    }
    
    /**
     * Loads a chunk from disk.
     * 
     * @param worldName The world name
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @return The loaded chunk, or null if not found
     */
    public Chunk loadChunk(String worldName, int chunkX, int chunkZ) {
        Path worldPath = getWorldPath(worldName);
        String chunkFileName = String.format("c.%d.%d.dat", chunkX, chunkZ);
        Path chunkFile = worldPath.resolve(REGION_DIR).resolve(chunkFileName);
        
        if (!Files.exists(chunkFile)) {
            return null;
        }
        
        try (DataInputStream dis = new DataInputStream(
                new GZIPInputStream(
                        new BufferedInputStream(Files.newInputStream(chunkFile))))) {
            
            int version = dis.readInt();
            int loadedChunkX = dis.readInt();
            int loadedChunkZ = dis.readInt();
            boolean generated = dis.readBoolean();
            
            Chunk chunk = new Chunk(loadedChunkX, loadedChunkZ);
            chunk.setGenerated(generated);
            
            // Read block data
            readBlockDataRLE(dis, chunk);
            
            Logger.debug("Loaded chunk (%d, %d)", chunkX, chunkZ);
            return chunk;
            
        } catch (IOException e) {
            Logger.error("Failed to load chunk (%d, %d): %s", chunkX, chunkZ, e.getMessage());
            return null;
        }
    }
    
    /**
     * Writes block data using Run-Length Encoding for compression.
     */
    private void writeBlockDataRLE(DataOutputStream dos, Chunk chunk) throws IOException {
        int currentBlock = chunk.getBlock(0, 0, 0);
        int count = 0;
        
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int x = 0; x < Chunk.WIDTH; x++) {
                    int block = chunk.getBlock(x, y, z);
                    
                    if (block == currentBlock && count < 65535) {
                        count++;
                    } else {
                        // Write the run
                        dos.writeShort(currentBlock);
                        dos.writeShort(count);
                        
                        currentBlock = block;
                        count = 1;
                    }
                }
            }
        }
        
        // Write final run
        dos.writeShort(currentBlock);
        dos.writeShort(count);
    }
    
    /**
     * Reads block data using Run-Length Encoding.
     */
    private void readBlockDataRLE(DataInputStream dis, Chunk chunk) throws IOException {
        int x = 0, y = 0, z = 0;
        
        while (y < Chunk.HEIGHT) {
            int blockId = dis.readShort() & 0xFFFF;
            int count = dis.readShort() & 0xFFFF;
            
            for (int i = 0; i < count && y < Chunk.HEIGHT; i++) {
                chunk.setBlock(x, y, z, blockId);
                
                x++;
                if (x >= Chunk.WIDTH) {
                    x = 0;
                    z++;
                    if (z >= Chunk.DEPTH) {
                        z = 0;
                        y++;
                    }
                }
            }
        }
        
        // Clear dirty flag since we just loaded
        // The chunk will be marked dirty when mesh is built
    }
    
    /**
     * Gets the region file path for a chunk.
     */
    private Path getRegionPath(String worldName, int chunkX, int chunkZ) {
        Path worldPath = getWorldPath(worldName);
        int regionX = Math.floorDiv(chunkX, REGION_SIZE);
        int regionZ = Math.floorDiv(chunkZ, REGION_SIZE);
        return worldPath.resolve(REGION_DIR).resolve(String.format("r.%d.%d.dat", regionX, regionZ));
    }
    
    /**
     * Gets a list of all saved worlds.
     * 
     * @return List of world metadata for all saved worlds
     */
    public List<WorldMetadata> listWorlds() {
        List<WorldMetadata> worlds = new ArrayList<>();
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(worldsPath)) {
            for (Path worldDir : stream) {
                if (Files.isDirectory(worldDir)) {
                    String worldName = worldDir.getFileName().toString();
                    WorldMetadata metadata = loadWorldMetadata(worldName);
                    if (metadata != null) {
                        worlds.add(metadata);
                    }
                }
            }
        } catch (IOException e) {
            Logger.error("Failed to list worlds: %s", e.getMessage());
        }
        
        // Sort by last played (most recent first)
        worlds.sort((a, b) -> Long.compare(b.getLastPlayed(), a.getLastPlayed()));
        
        return worlds;
    }
    
    /**
     * Deletes a world and all its data.
     * 
     * @param worldName The world name
     * @return true if deletion was successful
     */
    public boolean deleteWorld(String worldName) {
        Path worldPath = getWorldPath(worldName);
        
        if (!Files.exists(worldPath)) {
            return false;
        }
        
        try {
            // Recursively delete all files
            Files.walk(worldPath)
                 .sorted(Comparator.reverseOrder())
                 .forEach(path -> {
                     try {
                         Files.delete(path);
                     } catch (IOException e) {
                         Logger.error("Failed to delete: %s", path);
                     }
                 });
            
            Logger.info("Deleted world: %s", worldName);
            return true;
        } catch (IOException e) {
            Logger.error("Failed to delete world '%s': %s", worldName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Saves all modified chunks for a world.
     * 
     * @param world The world to save
     */
    public void saveWorld(World world) {
        String worldName = world.getName();
        int savedChunks = 0;
        
        for (Chunk chunk : world.getChunks()) {
            if (chunk.isGenerated()) {
                try {
                    saveChunk(worldName, chunk);
                    savedChunks++;
                } catch (IOException e) {
                    Logger.error("Failed to save chunk (%d, %d): %s", 
                                chunk.getChunkX(), chunk.getChunkZ(), e.getMessage());
                }
            }
        }
        
        Logger.info("Saved %d chunks for world '%s'", savedChunks, worldName);
    }
}

