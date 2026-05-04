package de.boondocksulfur.customjukeboxbedrock.listeners;

import de.boondocksulfur.customjukeboxbedrock.CustomJukeboxBedrockExtension;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Notifies players with permission about available updates.
 */
public class UpdateNotifyListener implements Listener {

    private final CustomJukeboxBedrockExtension plugin;

    public UpdateNotifyListener(CustomJukeboxBedrockExtension plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPermission("customjukebox.bedrock.admin")) {
            return;
        }

        if (plugin.getUpdateChecker() != null && plugin.getUpdateChecker().isUpdateAvailable()) {
            // Delay message by 2 seconds so it doesn't get lost in join spam
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage("§6§l[CJB-Bedrock] §eUpdate available!");
                player.sendMessage("§7Current: §e" + plugin.getUpdateChecker().getCurrentVersion());
                player.sendMessage("§7Latest: §a" + plugin.getUpdateChecker().getLatestVersion());
                player.sendMessage("§7Download: §bhttps://modrinth.com/plugin/bs-customjukebox-bedrock-extension");
            }, 40L);
        }
    }
}
