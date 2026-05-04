package de.boondocksulfur.customjukeboxbedrock.geyser;

import de.boondocksulfur.customjukebox.api.events.DiscPlaybackStartEvent;
import de.boondocksulfur.customjukebox.api.events.DiscPlaybackStopEvent;
import de.boondocksulfur.customjukebox.model.CustomDisc;
import de.boondocksulfur.customjukeboxbedrock.CustomJukeboxBedrockExtension;
import de.boondocksulfur.customjukeboxbedrock.pack.SoundDefinitionsBuilder;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges sound playback from the Java plugin to Bedrock clients.
 * Intercepts playback events, removes Bedrock players from Java sound delivery,
 * and triggers Bedrock-compatible sounds via Geyser connections.
 */
public class BedrockSoundBridge {

    private final CustomJukeboxBedrockExtension plugin;
    private final SoundDefinitionsBuilder soundDefBuilder;

    // Track active Bedrock playbacks for stop handling
    // Location key -> Set of Bedrock player UUIDs hearing sound at that location
    private final Map<String, Set<UUID>> activeBedrockPlaybacks;

    public BedrockSoundBridge(CustomJukeboxBedrockExtension plugin) {
        this.plugin = plugin;
        this.soundDefBuilder = new SoundDefinitionsBuilder(plugin.getBedrockConfig().getSoundPrefix());
        this.activeBedrockPlaybacks = new ConcurrentHashMap<>();
    }

    /**
     * Handles a disc playback start event.
     * Removes Bedrock players from the Java listener set and plays sound via Geyser.
     */
    public void handlePlaybackStart(DiscPlaybackStartEvent event) {
        CustomDisc disc = event.getDisc();
        if (!disc.hasCustomSound()) return;

        Location location = event.getLocation();
        String locationKey = getLocationKey(location);
        String bedrockSoundName = soundDefBuilder.javaKeyToBedrockName(disc.getSoundKey());

        Set<Player> bedrockPlayers = new HashSet<>();
        Set<UUID> bedrockUuids = ConcurrentHashMap.newKeySet();

        // Identify Bedrock players in the listener set
        Iterator<Player> iterator = event.getListeners().iterator();
        while (iterator.hasNext()) {
            Player player = iterator.next();
            if (isBedrockPlayer(player.getUniqueId())) {
                bedrockPlayers.add(player);
                iterator.remove(); // Remove from Java sound delivery
            }
        }

        if (bedrockPlayers.isEmpty()) return;

        // Play sound to Bedrock players via Geyser
        for (Player player : bedrockPlayers) {
            if (playBedrockSound(player.getUniqueId(), bedrockSoundName, location)) {
                bedrockUuids.add(player.getUniqueId());

                if (plugin.getBedrockConfig().isDebug()) {
                    plugin.getLogger().info("[BedrockSound] Playing '" + bedrockSoundName +
                        "' to " + player.getName());
                }
            }
        }

        // Track for stop handling
        if (!bedrockUuids.isEmpty()) {
            activeBedrockPlaybacks.put(locationKey, bedrockUuids);
        }
    }

    /**
     * Handles a disc playback stop event.
     * Stops sounds for tracked Bedrock players at this location.
     */
    public void handlePlaybackStop(DiscPlaybackStopEvent event) {
        CustomDisc disc = event.getDisc();
        if (!disc.hasCustomSound()) return;

        Location location = event.getLocation();
        String locationKey = getLocationKey(location);
        String bedrockSoundName = soundDefBuilder.javaKeyToBedrockName(disc.getSoundKey());

        Set<UUID> bedrockListeners = activeBedrockPlaybacks.remove(locationKey);
        if (bedrockListeners == null || bedrockListeners.isEmpty()) return;

        // Stop sound for all tracked Bedrock players
        for (UUID uuid : bedrockListeners) {
            stopBedrockSound(uuid, bedrockSoundName, location);

            if (plugin.getBedrockConfig().isDebug()) {
                plugin.getLogger().info("[BedrockSound] Stopping '" + bedrockSoundName +
                    "' for " + uuid);
            }
        }
    }

    /**
     * Checks if a player is a Bedrock player via Floodgate or Geyser API.
     */
    private boolean isBedrockPlayer(UUID uuid) {
        // Try Floodgate first (more reliable)
        if (plugin.isFloodgateAvailable()) {
            try {
                return org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(uuid);
            } catch (Exception e) {
                // Fall through to Geyser API
            }
        }

        // Fallback: Geyser API
        try {
            GeyserApi api = GeyserApi.api();
            if (api != null) {
                return api.isBedrockPlayer(uuid);
            }
        } catch (Exception e) {
            // Not available
        }

        return false;
    }

    /**
     * Plays a sound to a Bedrock player via Geyser.
     * Uses the Bedrock sound name from the resource pack's sound_definitions.json.
     */
    private boolean playBedrockSound(UUID playerUuid, String bedrockSoundName, Location location) {
        try {
            GeyserApi api = GeyserApi.api();
            if (api == null) return false;

            GeyserConnection connection = api.connectionByUuid(playerUuid);
            if (connection == null) return false;

            // Use the server-side playSound which Geyser translates to Bedrock
            // Since we have the resource pack loaded, the Bedrock sound name will work
            Player player = plugin.getServer().getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                // Play the Bedrock-mapped sound key directly
                // Geyser will forward this to the Bedrock client using the resource pack's sound
                player.playSound(location, bedrockSoundName,
                    org.bukkit.SoundCategory.RECORDS,
                    plugin.getBedrockConfig().isDebug() ? 1.0f :
                        (float) de.boondocksulfur.customjukebox.api.CustomJukeboxAPI.getInstance().getVolume(),
                    1.0f);
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[BedrockSound] Failed to play sound for " + playerUuid + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * Stops a sound for a Bedrock player.
     */
    private void stopBedrockSound(UUID playerUuid, String bedrockSoundName, Location location) {
        try {
            Player player = plugin.getServer().getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                player.stopSound(bedrockSoundName, org.bukkit.SoundCategory.RECORDS);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[BedrockSound] Failed to stop sound for " + playerUuid + ": " + e.getMessage());
        }
    }

    /**
     * Creates a location key for map storage.
     */
    private String getLocationKey(Location loc) {
        if (loc == null || loc.getWorld() == null) return "unknown";
        return loc.getWorld().getName() + ":" +
               loc.getBlockX() + ":" +
               loc.getBlockY() + ":" +
               loc.getBlockZ();
    }
}
