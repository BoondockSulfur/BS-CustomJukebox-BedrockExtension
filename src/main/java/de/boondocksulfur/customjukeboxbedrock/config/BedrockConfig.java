package de.boondocksulfur.customjukeboxbedrock.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JSON-based configuration manager for the Bedrock extension.
 */
public class BedrockConfig {

    private final File configFile;
    private final Logger logger;
    private final Gson gson;

    private boolean enabled;
    private boolean debug;
    private String packOutputDirectory;
    private boolean autoRegisterItems;
    private boolean autoDeliverPack;
    private String soundPrefix;
    private String packName;
    private String packDescription;

    public BedrockConfig(File dataFolder, Logger logger) {
        this.configFile = new File(dataFolder, "config.json");
        this.logger = logger;
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

        // Defaults
        this.enabled = true;
        this.debug = false;
        this.packOutputDirectory = "bedrock-pack";
        this.autoRegisterItems = true;
        this.autoDeliverPack = true;
        this.soundPrefix = "customjukebox";
        this.packName = "CustomJukebox Bedrock Sounds";
        this.packDescription = "Custom disc sounds for Bedrock Edition";
    }

    /**
     * Loads configuration from config.json.
     * Creates default file if it doesn't exist.
     */
    public void load() {
        if (!configFile.getParentFile().exists()) {
            configFile.getParentFile().mkdirs();
        }

        if (!configFile.exists()) {
            save();
            logger.info("Created default config.json");
            return;
        }

        try (Reader reader = new FileReader(configFile)) {
            JsonObject config = gson.fromJson(reader, JsonObject.class);

            if (config == null) {
                logger.warning("config.json is empty, using defaults");
                return;
            }

            this.enabled = getBool(config, "enabled", true);
            this.debug = getBool(config, "debug", false);
            this.packOutputDirectory = getString(config, "packOutputDirectory", "bedrock-pack");
            this.autoRegisterItems = getBool(config, "autoRegisterItems", true);
            this.autoDeliverPack = getBool(config, "autoDeliverPack", true);
            this.soundPrefix = getString(config, "soundPrefix", "customjukebox");
            this.packName = getString(config, "packName", "CustomJukebox Bedrock Sounds");
            this.packDescription = getString(config, "packDescription", "Custom disc sounds for Bedrock Edition");

            logger.info("Loaded config.json");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load config.json, using defaults", e);
        }
    }

    /**
     * Saves current configuration to config.json.
     */
    public void save() {
        try {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }

            JsonObject config = new JsonObject();
            config.addProperty("enabled", enabled);
            config.addProperty("debug", debug);
            config.addProperty("packOutputDirectory", packOutputDirectory);
            config.addProperty("autoRegisterItems", autoRegisterItems);
            config.addProperty("autoDeliverPack", autoDeliverPack);
            config.addProperty("soundPrefix", soundPrefix);
            config.addProperty("packName", packName);
            config.addProperty("packDescription", packDescription);

            try (Writer writer = new FileWriter(configFile)) {
                gson.toJson(config, writer);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save config.json", e);
        }
    }

    public void reload() {
        load();
    }

    // Getters

    public boolean isEnabled() { return enabled; }
    public boolean isDebug() { return debug; }
    public String getPackOutputDirectory() { return packOutputDirectory; }
    public boolean isAutoRegisterItems() { return autoRegisterItems; }
    public boolean isAutoDeliverPack() { return autoDeliverPack; }
    public String getSoundPrefix() { return soundPrefix; }
    public String getPackName() { return packName; }
    public String getPackDescription() { return packDescription; }

    // Helper methods

    private String getString(JsonObject obj, String key, String defaultValue) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return defaultValue;
    }

    private boolean getBool(JsonObject obj, String key, boolean defaultValue) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsBoolean();
        }
        return defaultValue;
    }
}
