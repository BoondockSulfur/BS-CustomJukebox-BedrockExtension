package de.boondocksulfur.customjukeboxbedrock.pack;

import com.google.gson.JsonObject;
import de.boondocksulfur.customjukebox.model.CustomDisc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Maps Java CustomModelData textures to Bedrock texture paths.
 * Looks for PNG files in configurable texture directories and copies them
 * into the Bedrock resource pack structure.
 */
public class TextureMapper {

    private final Logger logger;
    private final File textureSourceDir;

    public TextureMapper(Logger logger, File textureSourceDir) {
        this.logger = logger;
        this.textureSourceDir = textureSourceDir;
    }

    /**
     * Copies texture files to the pack output directory and generates item_texture.json.
     * @param discs All custom discs
     * @param packDir The pack root directory (output)
     * @return item_texture.json content, or null if no textures found
     */
    public JsonObject buildTextures(Collection<CustomDisc> discs, File packDir) {
        File texturesDir = new File(packDir, "textures/items/customjukebox");

        if (!textureSourceDir.exists() || !textureSourceDir.isDirectory()) {
            logger.info("No texture source directory found at: " + textureSourceDir.getAbsolutePath());
            return null;
        }

        boolean hasTextures = false;
        JsonObject itemTexture = new JsonObject();
        itemTexture.addProperty("resource_pack_name", "customjukebox");

        JsonObject textureData = new JsonObject();

        for (CustomDisc disc : discs) {
            // Look for texture file matching disc ID
            File textureFile = findTextureFile(disc.getId());
            if (textureFile == null) {
                continue;
            }

            // Copy texture to pack
            if (!texturesDir.exists()) {
                texturesDir.mkdirs();
            }

            File destFile = new File(texturesDir, disc.getId() + ".png");
            try {
                Files.copy(textureFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                hasTextures = true;

                // Add to item_texture.json
                JsonObject texEntry = new JsonObject();
                texEntry.addProperty("textures", "textures/items/customjukebox/" + disc.getId());
                textureData.add("customjukebox_" + disc.getId(), texEntry);

                logger.info("  Texture mapped: " + disc.getId() + " -> " + destFile.getName());
            } catch (IOException e) {
                logger.warning("Failed to copy texture for disc '" + disc.getId() + "': " + e.getMessage());
            }
        }

        if (!hasTextures) {
            return null;
        }

        itemTexture.add("texture_data", textureData);
        return itemTexture;
    }

    /**
     * Finds a texture file for the given disc ID.
     * Looks for: {id}.png, disc_{id}.png
     * @param discId The disc ID
     * @return The texture file, or null if not found
     */
    private File findTextureFile(String discId) {
        // Try exact match first
        File exact = new File(textureSourceDir, discId + ".png");
        if (exact.exists()) {
            return exact;
        }

        // Try with disc_ prefix
        File prefixed = new File(textureSourceDir, "disc_" + discId + ".png");
        if (prefixed.exists()) {
            return prefixed;
        }

        return null;
    }

    /**
     * Gets the Bedrock texture name for a disc (for use in custom item registration).
     * @param discId The disc ID
     * @return Bedrock texture identifier
     */
    public static String getBedrockTextureName(String discId) {
        return "customjukebox_" + discId;
    }
}
