package de.boondocksulfur.customjukeboxbedrock.geyser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import de.boondocksulfur.customjukebox.api.CustomJukeboxAPI;
import de.boondocksulfur.customjukebox.model.CustomDisc;
import de.boondocksulfur.customjukeboxbedrock.CustomJukeboxBedrockExtension;
import de.boondocksulfur.customjukeboxbedrock.config.BedrockConfig;
import de.boondocksulfur.customjukeboxbedrock.pack.ManifestGenerator;
import de.boondocksulfur.customjukeboxbedrock.pack.SoundDefinitionsBuilder;
import de.boondocksulfur.customjukeboxbedrock.pack.TextureMapper;

import java.io.*;
import java.nio.file.Files;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Orchestrates Bedrock resource pack generation.
 * Combines manifest, sound definitions, sound files, and textures into a .mcpack file.
 */
public class ResourcePackBuilder {

    private final CustomJukeboxBedrockExtension plugin;
    private final Gson gson;

    public ResourcePackBuilder(CustomJukeboxBedrockExtension plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    /**
     * Generates the complete Bedrock resource pack.
     * @return The generated .mcpack file, or null on failure
     */
    public File generate() {
        BedrockConfig config = plugin.getBedrockConfig();

        CustomJukeboxAPI api = CustomJukeboxAPI.getInstance();
        if (api == null) {
            plugin.getLogger().severe("CustomJukebox API not available!");
            return null;
        }

        Collection<CustomDisc> discs = api.getAllDiscs();
        if (discs.isEmpty()) {
            plugin.getLogger().warning("No custom discs found! Nothing to generate.");
            return null;
        }

        // Output directory
        File outputDir = new File(plugin.getDataFolder(), config.getPackOutputDirectory());
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Temp directory for building the pack
        File tempDir = new File(plugin.getDataFolder(), "temp-pack-build");
        if (tempDir.exists()) {
            deleteDirectory(tempDir);
        }
        tempDir.mkdirs();

        try {
            int soundCount = 0;
            int textureCount = 0;

            // 1. Generate manifest.json
            ManifestGenerator manifestGen = new ManifestGenerator(
                config.getPackName(),
                config.getPackDescription(),
                "1.0.0"
            );
            JsonObject manifest = manifestGen.generate();
            writeJsonFile(new File(tempDir, "manifest.json"), manifest);
            plugin.getLogger().info("Generated manifest.json");

            // 2. Generate sound_definitions.json
            SoundDefinitionsBuilder soundDefBuilder = new SoundDefinitionsBuilder(config.getSoundPrefix());
            JsonObject soundDefs = soundDefBuilder.build(discs);

            File soundsDir = new File(tempDir, "sounds");
            soundsDir.mkdirs();
            writeJsonFile(new File(soundsDir, "sound_definitions.json"), soundDefs);
            plugin.getLogger().info("Generated sound_definitions.json");

            // 3. Copy sound files (.ogg)
            File mainPluginDataFolder = api.getPluginDataFolder();
            File soundSourceDir = new File(mainPluginDataFolder, "sounds");

            if (soundSourceDir.exists() && soundSourceDir.isDirectory()) {
                for (CustomDisc disc : discs) {
                    if (!disc.hasCustomSound()) continue;

                    String oggFilename = soundDefBuilder.getExpectedOggFilename(disc);
                    File oggSource = findOggFile(soundSourceDir, oggFilename, disc.getId());

                    if (oggSource != null) {
                        // Create the directory structure matching sound_definitions.json paths
                        String soundFilePath = soundDefBuilder.getSoundFilePath(disc);
                        File destFile = new File(tempDir, soundFilePath + ".ogg");
                        destFile.getParentFile().mkdirs();

                        Files.copy(oggSource.toPath(), destFile.toPath());
                        soundCount++;

                        if (config.isDebug()) {
                            plugin.getLogger().info("  Copied sound: " + oggSource.getName() + " -> " + soundFilePath + ".ogg");
                        }
                    } else {
                        plugin.getLogger().warning("  Sound file not found for disc '" + disc.getId() +
                            "' (expected: " + oggFilename + " in " + soundSourceDir.getAbsolutePath() + ")");
                    }
                }
            } else {
                plugin.getLogger().warning("Sound source directory not found: " + soundSourceDir.getAbsolutePath());
                plugin.getLogger().warning("Create a 'sounds' folder in the CustomJukebox data folder with your .ogg files.");
            }

            // 4. Copy textures
            File textureSourceDir = new File(mainPluginDataFolder, "textures");
            TextureMapper textureMapper = new TextureMapper(plugin.getLogger(), textureSourceDir);
            JsonObject itemTexture = textureMapper.buildTextures(discs, tempDir);

            if (itemTexture != null) {
                File texturesDir = new File(tempDir, "textures");
                texturesDir.mkdirs();
                writeJsonFile(new File(texturesDir, "item_texture.json"), itemTexture);
                textureCount = (int) discs.stream()
                    .filter(d -> new File(textureSourceDir, d.getId() + ".png").exists() ||
                                 new File(textureSourceDir, "disc_" + d.getId() + ".png").exists())
                    .count();
            }

            // 5. Pack everything as .mcpack (ZIP)
            File mcpackFile = new File(outputDir, "CustomJukebox-Bedrock.mcpack");
            zipDirectory(tempDir, mcpackFile);

            plugin.getLogger().info("═══════════════════════════════════════════════════════════");
            plugin.getLogger().info("Bedrock Resource Pack generated successfully!");
            plugin.getLogger().info("  Output: " + mcpackFile.getAbsolutePath());
            plugin.getLogger().info("  Discs: " + discs.size());
            plugin.getLogger().info("  Sounds copied: " + soundCount);
            plugin.getLogger().info("  Textures copied: " + textureCount);
            plugin.getLogger().info("═══════════════════════════════════════════════════════════");
            plugin.getLogger().info("Next steps:");
            plugin.getLogger().info("  1. Copy the .mcpack to Geyser's 'packs/' folder");
            plugin.getLogger().info("  2. Restart the server");
            plugin.getLogger().info("  3. Bedrock players will receive the pack automatically");
            plugin.getLogger().info("═══════════════════════════════════════════════════════════");

            return mcpackFile;

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to generate Bedrock resource pack: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            // Cleanup temp directory
            deleteDirectory(tempDir);
        }
    }

    /**
     * Finds an OGG file by filename or disc ID in the source directory.
     * Searches recursively.
     */
    private File findOggFile(File sourceDir, String expectedFilename, String discId) {
        // Try exact filename
        File exact = new File(sourceDir, expectedFilename);
        if (exact.exists()) return exact;

        // Try disc ID as filename
        File byId = new File(sourceDir, discId + ".ogg");
        if (byId.exists()) return byId;

        // Search subdirectories
        File[] subdirs = sourceDir.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File subdir : subdirs) {
                File found = findOggFile(subdir, expectedFilename, discId);
                if (found != null) return found;
            }
        }

        return null;
    }

    /**
     * Writes a JsonObject to a file.
     */
    private void writeJsonFile(File file, JsonObject json) throws IOException {
        file.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(json, writer);
        }
    }

    /**
     * Creates a ZIP file from a directory.
     */
    private void zipDirectory(File sourceDir, File outputFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {
            zipDirectoryRecursive(sourceDir, sourceDir, zos);
        }
    }

    private void zipDirectoryRecursive(File rootDir, File currentDir, ZipOutputStream zos) throws IOException {
        File[] files = currentDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            String relativePath = rootDir.toPath().relativize(file.toPath()).toString()
                .replace('\\', '/'); // Ensure forward slashes in ZIP

            if (file.isDirectory()) {
                zos.putNextEntry(new ZipEntry(relativePath + "/"));
                zos.closeEntry();
                zipDirectoryRecursive(rootDir, file, zos);
            } else {
                zos.putNextEntry(new ZipEntry(relativePath));
                Files.copy(file.toPath(), zos);
                zos.closeEntry();
            }
        }
    }

    /**
     * Recursively deletes a directory.
     */
    private void deleteDirectory(File dir) {
        if (!dir.exists()) return;

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
}
