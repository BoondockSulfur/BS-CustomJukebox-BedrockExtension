package de.boondocksulfur.customjukeboxbedrock.geyser;

import de.boondocksulfur.customjukebox.api.CustomJukeboxAPI;
import de.boondocksulfur.customjukebox.model.CustomDisc;
import de.boondocksulfur.customjukeboxbedrock.CustomJukeboxBedrockExtension;
import de.boondocksulfur.customjukeboxbedrock.pack.TextureMapper;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;

import java.util.Collection;

/**
 * Registers custom Bedrock items for each CustomJukebox disc via Geyser's Custom Items API.
 * This allows Bedrock players to see unique disc items instead of generic music discs.
 */
public class CustomItemRegistrar implements EventRegistrar {

    private final CustomJukeboxBedrockExtension plugin;
    private int registeredCount;

    public CustomItemRegistrar(CustomJukeboxBedrockExtension plugin) {
        this.plugin = plugin;
        this.registeredCount = 0;
    }

    /**
     * Called by Geyser during initialization to register custom items.
     * This event fires once during server startup — changes require a restart.
     */
    @Subscribe
    public void onDefineCustomItems(GeyserDefineCustomItemsEvent event) {
        if (!plugin.getBedrockConfig().isAutoRegisterItems()) {
            plugin.getLogger().info("Auto-registration of Bedrock items is disabled in config.");
            return;
        }

        CustomJukeboxAPI api = CustomJukeboxAPI.getInstance();
        if (api == null) {
            plugin.getLogger().warning("CustomJukebox API not available during Geyser item registration!");
            return;
        }

        Collection<CustomDisc> discs = api.getAllDiscs();
        if (discs.isEmpty()) {
            plugin.getLogger().info("No custom discs to register as Bedrock items.");
            return;
        }

        plugin.getLogger().info("Registering " + discs.size() + " custom disc(s) as Bedrock items...");

        for (CustomDisc disc : discs) {
            try {
                registerDiscItem(event, disc);
                registeredCount++;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to register Bedrock item for disc '" +
                    disc.getId() + "': " + e.getMessage());
                if (plugin.getBedrockConfig().isDebug()) {
                    e.printStackTrace();
                }
            }
        }

        plugin.getLogger().info("Successfully registered " + registeredCount + " Bedrock custom item(s).");
    }

    /**
     * Registers a single disc as a Bedrock custom item.
     */
    private void registerDiscItem(GeyserDefineCustomItemsEvent event, CustomDisc disc) {
        // Map Java material name to Geyser identifier
        String baseItem = materialToBedrockId(disc.getDiscType().name());

        // Build the custom item definition
        org.geysermc.geyser.api.util.Identifier itemId =
            org.geysermc.geyser.api.util.Identifier.of("customjukebox:" + disc.getId());
        org.geysermc.geyser.api.util.Identifier modelId =
            org.geysermc.geyser.api.util.Identifier.of("customjukebox:" + disc.getId());

        CustomItemDefinition.Builder builder = CustomItemDefinition.builder(itemId, modelId);

        // Set Bedrock options (bedrockOptions takes a Builder, not the built object)
        CustomItemBedrockOptions.Builder bedrockOptions = CustomItemBedrockOptions.builder();
        bedrockOptions.icon(TextureMapper.getBedrockTextureName(disc.getId()));

        builder.bedrockOptions(bedrockOptions);

        // Register with the base vanilla item
        org.geysermc.geyser.api.util.Identifier baseIdentifier =
            org.geysermc.geyser.api.util.Identifier.of(baseItem);
        event.register(baseIdentifier, builder.build());

        if (plugin.getBedrockConfig().isDebug()) {
            plugin.getLogger().info("  Registered: " + disc.getId() +
                " (base: " + baseItem + ", cmd: " + disc.getCustomModelData() + ")");
        }
    }

    /**
     * Converts a Bukkit Material name to a Bedrock item identifier.
     * e.g., "MUSIC_DISC_13" -> "minecraft:music_disc_13"
     */
    private String materialToBedrockId(String materialName) {
        return "minecraft:" + materialName.toLowerCase();
    }

    /**
     * Gets the number of successfully registered items.
     * @return Count of registered Bedrock items
     */
    public int getRegisteredCount() {
        return registeredCount;
    }
}
