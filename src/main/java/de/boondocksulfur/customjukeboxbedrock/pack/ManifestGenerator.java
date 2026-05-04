package de.boondocksulfur.customjukeboxbedrock.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Generates Bedrock resource pack manifest.json.
 * Uses format_version 3 and min_engine_version 1.21.0.
 */
public class ManifestGenerator {

    private final String packName;
    private final String packDescription;
    private final String packVersion;

    public ManifestGenerator(String packName, String packDescription, String packVersion) {
        this.packName = packName;
        this.packDescription = packDescription;
        this.packVersion = packVersion;
    }

    /**
     * Generates the manifest.json content as a JsonObject.
     * @return Complete manifest JsonObject
     */
    public JsonObject generate() {
        JsonObject manifest = new JsonObject();
        manifest.addProperty("format_version", 2);

        // Header
        JsonObject header = new JsonObject();
        header.addProperty("name", packName);
        header.addProperty("description", packDescription);
        header.addProperty("uuid", generateDeterministicUuid(packName + "-header").toString());
        header.add("version", parseVersionArray(packVersion));

        // min_engine_version as array
        JsonArray minEngine = new JsonArray();
        minEngine.add(1);
        minEngine.add(21);
        minEngine.add(0);
        header.add("min_engine_version", minEngine);

        manifest.add("header", header);

        // Modules
        JsonArray modules = new JsonArray();
        JsonObject resourceModule = new JsonObject();
        resourceModule.addProperty("type", "resources");
        resourceModule.addProperty("uuid", generateDeterministicUuid(packName + "-module").toString());
        resourceModule.add("version", parseVersionArray(packVersion));
        modules.add(resourceModule);

        manifest.add("modules", modules);

        return manifest;
    }

    /**
     * Generates a deterministic UUID based on a seed string.
     * This ensures the same pack always gets the same UUIDs.
     * @param seed Seed string
     * @return Deterministic UUID
     */
    /**
     * Parses a version string "1.0.0" into a JSON array [1, 0, 0].
     * Bedrock requires version fields as arrays, not strings.
     */
    private JsonArray parseVersionArray(String version) {
        JsonArray arr = new JsonArray();
        String[] parts = version.split("\\.");
        for (String part : parts) {
            try {
                arr.add(Integer.parseInt(part));
            } catch (NumberFormatException e) {
                arr.add(0);
            }
        }
        // Ensure at least 3 elements
        while (arr.size() < 3) {
            arr.add(0);
        }
        return arr;
    }

    private UUID generateDeterministicUuid(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
