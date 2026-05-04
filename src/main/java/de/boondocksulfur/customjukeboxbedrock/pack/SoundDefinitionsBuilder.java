package de.boondocksulfur.customjukeboxbedrock.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.boondocksulfur.customjukebox.model.CustomDisc;

import java.util.Collection;

/**
 * Builds the Bedrock sound_definitions.json from custom disc data.
 * Converts Java sound keys (namespace:key) to Bedrock format (namespace.key).
 */
public class SoundDefinitionsBuilder {

    private final String soundPrefix;

    public SoundDefinitionsBuilder(String soundPrefix) {
        this.soundPrefix = soundPrefix;
    }

    /**
     * Builds sound_definitions.json content from a collection of custom discs.
     * @param discs All custom discs to include
     * @return Complete sound_definitions.json as JsonObject
     */
    public JsonObject build(Collection<CustomDisc> discs) {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.14.0");

        JsonObject soundDefinitions = new JsonObject();

        for (CustomDisc disc : discs) {
            if (!disc.hasCustomSound()) {
                continue;
            }

            String bedrockSoundName = javaKeyToBedrockName(disc.getSoundKey());
            String soundFilePath = getSoundFilePath(disc);

            JsonObject soundEntry = new JsonObject();
            soundEntry.addProperty("category", "record");

            JsonArray soundsArray = new JsonArray();
            JsonObject soundFile = new JsonObject();
            soundFile.addProperty("name", soundFilePath);
            soundFile.addProperty("volume", 1.0);
            soundFile.addProperty("stream", true);
            soundFile.addProperty("is3D", true);
            soundsArray.add(soundFile);

            soundEntry.add("sounds", soundsArray);
            soundDefinitions.add(bedrockSoundName, soundEntry);
        }

        root.add("sound_definitions", soundDefinitions);
        return root;
    }

    /**
     * Converts a Java sound key (e.g., "customjukebox:music_disc.epic_journey")
     * to a Bedrock sound name (e.g., "customjukebox.music_disc.epic_journey").
     * @param javaSoundKey Java format sound key
     * @return Bedrock format sound name
     */
    public String javaKeyToBedrockName(String javaSoundKey) {
        if (javaSoundKey == null || javaSoundKey.isEmpty()) {
            return "";
        }
        // Replace the namespace separator ':' with '.'
        return javaSoundKey.replace(':', '.');
    }

    /**
     * Gets the sound file path for use in sound_definitions.json.
     * Path is relative to the resource pack root, without file extension.
     * @param disc The custom disc
     * @return Sound file path (e.g., "sounds/music_disc/epic_journey")
     */
    public String getSoundFilePath(CustomDisc disc) {
        String soundKey = disc.getSoundKey();

        // Extract the sound name after the namespace prefix
        // e.g., "customjukebox:music_disc.epic_journey" -> "music_disc.epic_journey"
        String soundName;
        if (soundKey.contains(":")) {
            soundName = soundKey.substring(soundKey.indexOf(':') + 1);
        } else {
            soundName = soundKey;
        }

        // Convert dots to path separators for directory structure
        // e.g., "music_disc.epic_journey" -> "sounds/music_disc/epic_journey"
        return "sounds/" + soundName.replace('.', '/');
    }

    /**
     * Gets the expected OGG filename for a disc.
     * @param disc The custom disc
     * @return Expected filename (e.g., "epic_journey.ogg")
     */
    public String getExpectedOggFilename(CustomDisc disc) {
        String soundKey = disc.getSoundKey();

        // Get the last segment after the last dot or colon
        String name;
        if (soundKey.contains(".")) {
            name = soundKey.substring(soundKey.lastIndexOf('.') + 1);
        } else if (soundKey.contains(":")) {
            name = soundKey.substring(soundKey.indexOf(':') + 1);
        } else {
            name = soundKey;
        }

        return name + ".ogg";
    }
}
