package de.boondocksulfur.customjukeboxbedrock;

import de.boondocksulfur.customjukeboxbedrock.commands.BedrockCommand;
import de.boondocksulfur.customjukeboxbedrock.config.BedrockConfig;
import de.boondocksulfur.customjukeboxbedrock.geyser.BedrockSoundBridge;
import de.boondocksulfur.customjukeboxbedrock.geyser.CustomItemRegistrar;
import de.boondocksulfur.customjukeboxbedrock.geyser.ResourcePackBuilder;
import de.boondocksulfur.customjukeboxbedrock.listeners.DiscPlaybackListener;
import de.boondocksulfur.customjukeboxbedrock.listeners.DiscRegistrationListener;
import de.boondocksulfur.customjukeboxbedrock.listeners.UpdateNotifyListener;
import de.boondocksulfur.customjukeboxbedrock.utils.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;

/**
 * Main class for the CustomJukebox Bedrock Extension.
 * Bridges custom disc playback and textures to Bedrock Edition via Geyser/Floodgate.
 */
public class CustomJukeboxBedrockExtension extends JavaPlugin implements EventRegistrar {

    private static CustomJukeboxBedrockExtension instance;

    private BedrockConfig bedrockConfig;
    private BedrockSoundBridge soundBridge;
    private CustomItemRegistrar itemRegistrar;
    private ResourcePackBuilder packBuilder;
    private UpdateChecker updateChecker;

    private boolean geyserAvailable;
    private boolean floodgateAvailable;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("Starting CustomJukebox-BedrockExtension initialization...");

        // Load configuration
        bedrockConfig = new BedrockConfig(getDataFolder(), getLogger());
        bedrockConfig.load();

        if (!bedrockConfig.isEnabled()) {
            getLogger().info("Plugin is disabled in config.json");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Check for Geyser
        geyserAvailable = getServer().getPluginManager().getPlugin("Geyser-Spigot") != null;
        if (!geyserAvailable) {
            getLogger().warning("═══════════════════════════════════════════════════════════");
            getLogger().warning("Geyser-Spigot not found!");
            getLogger().warning("The Bedrock extension requires Geyser to function.");
            getLogger().warning("Bedrock sound bridging and custom items will be DISABLED.");
            getLogger().warning("═══════════════════════════════════════════════════════════");
        }

        // Check for Floodgate
        floodgateAvailable = getServer().getPluginManager().getPlugin("floodgate") != null;
        if (!floodgateAvailable) {
            getLogger().warning("Floodgate not found! Bedrock player detection will be limited.");
        }

        // Initialize components
        packBuilder = new ResourcePackBuilder(this);

        if (geyserAvailable) {
            soundBridge = new BedrockSoundBridge(this);
            itemRegistrar = new CustomItemRegistrar(this);

            // Register Geyser event listeners
            try {
                GeyserApi api = GeyserApi.api();
                if (api != null) {
                    api.eventBus().register(this, itemRegistrar);
                    getLogger().info("Registered Geyser event listeners");
                } else {
                    getLogger().warning("GeyserApi not yet available - item registration will happen on Geyser init");
                }
            } catch (Exception e) {
                getLogger().warning("Failed to register Geyser events: " + e.getMessage());
            }
        }

        // Register Bukkit event listeners
        getServer().getPluginManager().registerEvents(new DiscPlaybackListener(this), this);
        getServer().getPluginManager().registerEvents(new DiscRegistrationListener(this), this);
        getServer().getPluginManager().registerEvents(new UpdateNotifyListener(this), this);

        // Register commands
        BedrockCommand command = new BedrockCommand(this);
        getCommand("cjb-bedrock").setExecutor(command);
        getCommand("cjb-bedrock").setTabCompleter(command);

        // bStats metrics
        new Metrics(this, 31102);

        // Check for updates
        updateChecker = new UpdateChecker(this, "bs-customjukebox-bedrock-extension");
        updateChecker.checkForUpdates();

        // Status
        getLogger().info("CustomJukebox-BedrockExtension enabled!");
        getLogger().info("  Geyser: " + (geyserAvailable ? "FOUND" : "NOT FOUND"));
        getLogger().info("  Floodgate: " + (floodgateAvailable ? "FOUND" : "NOT FOUND"));
    }

    @Override
    public void onDisable() {
        getLogger().info("CustomJukebox-BedrockExtension disabled!");
    }

    public static CustomJukeboxBedrockExtension getInstance() {
        return instance;
    }

    public BedrockConfig getBedrockConfig() {
        return bedrockConfig;
    }

    public BedrockSoundBridge getSoundBridge() {
        return soundBridge;
    }

    public CustomItemRegistrar getItemRegistrar() {
        return itemRegistrar;
    }

    public ResourcePackBuilder getPackBuilder() {
        return packBuilder;
    }

    public boolean isGeyserAvailable() {
        return geyserAvailable;
    }

    public boolean isFloodgateAvailable() {
        return floodgateAvailable;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
}
