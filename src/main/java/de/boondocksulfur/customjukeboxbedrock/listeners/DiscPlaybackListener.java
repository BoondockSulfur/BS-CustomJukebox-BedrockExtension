package de.boondocksulfur.customjukeboxbedrock.listeners;

import de.boondocksulfur.customjukeboxbedrock.CustomJukeboxBedrockExtension;
import de.boondocksulfur.customjukebox.api.events.DiscPlaybackStartEvent;
import de.boondocksulfur.customjukebox.api.events.DiscPlaybackStopEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listens for disc playback events from the main CustomJukebox plugin.
 * Intercepts Bedrock players and routes their sound through the Geyser bridge.
 */
public class DiscPlaybackListener implements Listener {

    private final CustomJukeboxBedrockExtension plugin;

    public DiscPlaybackListener(CustomJukeboxBedrockExtension plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDiscPlaybackStart(DiscPlaybackStartEvent event) {
        if (!plugin.isGeyserAvailable() || plugin.getSoundBridge() == null) {
            return;
        }

        plugin.getSoundBridge().handlePlaybackStart(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDiscPlaybackStop(DiscPlaybackStopEvent event) {
        if (!plugin.isGeyserAvailable() || plugin.getSoundBridge() == null) {
            return;
        }

        plugin.getSoundBridge().handlePlaybackStop(event);
    }
}
