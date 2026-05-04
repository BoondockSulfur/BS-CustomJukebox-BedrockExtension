package de.boondocksulfur.customjukeboxbedrock.listeners;

import de.boondocksulfur.customjukeboxbedrock.CustomJukeboxBedrockExtension;
import de.boondocksulfur.customjukebox.api.events.DiscRegisteredEvent;
import de.boondocksulfur.customjukebox.api.events.DiscRemovedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Listens for disc registration/removal events.
 * Notifies admins that a server restart is required for Geyser custom item changes.
 */
public class DiscRegistrationListener implements Listener {

    private final CustomJukeboxBedrockExtension plugin;

    public DiscRegistrationListener(CustomJukeboxBedrockExtension plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDiscRegistered(DiscRegisteredEvent event) {
        if (plugin.getBedrockConfig().isDebug()) {
            plugin.getLogger().info("[Bedrock] New disc registered: " + event.getDisc().getId());
        }

        plugin.getLogger().info("New disc '" + event.getDisc().getId() + "' registered. " +
            "Run '/cjb-bedrock generate' and restart the server to update Bedrock items.");
    }

    @EventHandler
    public void onDiscRemoved(DiscRemovedEvent event) {
        if (plugin.getBedrockConfig().isDebug()) {
            plugin.getLogger().info("[Bedrock] Disc removed: " + event.getDiscId());
        }

        plugin.getLogger().info("Disc '" + event.getDiscId() + "' removed. " +
            "Run '/cjb-bedrock generate' and restart the server to update Bedrock items.");
    }
}
