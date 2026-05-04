package de.boondocksulfur.customjukeboxbedrock.commands;

import de.boondocksulfur.customjukebox.api.CustomJukeboxAPI;
import de.boondocksulfur.customjukeboxbedrock.CustomJukeboxBedrockExtension;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command handler for /cjb-bedrock (alias /cjbb).
 * Subcommands: generate, reload, status, help
 */
public class BedrockCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("generate", "reload", "status", "help");

    private final CustomJukeboxBedrockExtension plugin;

    public BedrockCommand(CustomJukeboxBedrockExtension plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("customjukebox.bedrock.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender, label);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "generate" -> handleGenerate(sender);
            case "reload" -> handleReload(sender);
            case "status" -> handleStatus(sender);
            case "help" -> showHelp(sender, label);
            default -> {
                sender.sendMessage("§cUnknown subcommand: " + args[0]);
                sender.sendMessage("§7Use §e/" + label + " help §7for available commands.");
            }
        }

        return true;
    }

    private void handleGenerate(CommandSender sender) {
        sender.sendMessage("§7Generating Bedrock resource pack...");

        // Run async to avoid blocking main thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File result = plugin.getPackBuilder().generate();

            // Report back on main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (result != null) {
                    sender.sendMessage("§aResource pack generated successfully!");
                    sender.sendMessage("§7Output: §e" + result.getAbsolutePath());
                    sender.sendMessage("§7Copy to Geyser's §epacks/ §7folder and restart the server.");
                } else {
                    sender.sendMessage("§cFailed to generate resource pack. Check console for details.");
                }
            });
        });
    }

    private void handleReload(CommandSender sender) {
        plugin.getBedrockConfig().reload();
        sender.sendMessage("§aCustomJukebox-BedrockExtension config reloaded.");
    }

    private void handleStatus(CommandSender sender) {
        sender.sendMessage("§6═══ CustomJukebox-BedrockExtension Status ═══");
        sender.sendMessage("§7Version: §e" + plugin.getPluginMeta().getVersion());
        sender.sendMessage("§7Enabled: §e" + plugin.getBedrockConfig().isEnabled());
        sender.sendMessage("");

        // Dependencies
        sender.sendMessage("§7Geyser-Spigot: " + (plugin.isGeyserAvailable() ? "§aFOUND" : "§cNOT FOUND"));
        sender.sendMessage("§7Floodgate: " + (plugin.isFloodgateAvailable() ? "§aFOUND" : "§cNOT FOUND"));

        // CustomJukebox
        CustomJukeboxAPI api = CustomJukeboxAPI.getInstance();
        if (api != null) {
            sender.sendMessage("§7CustomJukebox: §aFOUND §7(v" + api.getVersion() + ")");
            sender.sendMessage("§7Custom Discs: §e" + api.getAllDiscs().size());
        } else {
            sender.sendMessage("§7CustomJukebox: §cNOT AVAILABLE");
        }

        // Registered items
        if (plugin.getItemRegistrar() != null) {
            sender.sendMessage("§7Bedrock Items Registered: §e" + plugin.getItemRegistrar().getRegisteredCount());
        }

        // Pack status
        File packFile = new File(plugin.getDataFolder(),
            plugin.getBedrockConfig().getPackOutputDirectory() + "/CustomJukebox-Bedrock.mcpack");
        if (packFile.exists()) {
            sender.sendMessage("§7Resource Pack: §aGENERATED §7(" +
                (packFile.length() / 1024) + " KB)");
        } else {
            sender.sendMessage("§7Resource Pack: §eNOT GENERATED §7(run /cjb-bedrock generate)");
        }

        sender.sendMessage("§6═══════════════════════════════════════════");
    }

    private void showHelp(CommandSender sender, String label) {
        sender.sendMessage("§6═══ CustomJukebox-BedrockExtension Help ═══");
        sender.sendMessage("§e/" + label + " generate §7- Generate Bedrock resource pack");
        sender.sendMessage("§e/" + label + " reload §7- Reload configuration");
        sender.sendMessage("§e/" + label + " status §7- Show plugin status");
        sender.sendMessage("§e/" + label + " help §7- Show this help");
        sender.sendMessage("§6═══════════════════════════════════════════");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("customjukebox.bedrock.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return SUBCOMMANDS.stream()
                .filter(s -> s.startsWith(partial))
                .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
