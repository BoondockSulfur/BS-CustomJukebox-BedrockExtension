package de.boondocksulfur.customjukeboxbedrock.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Checks for plugin updates via Modrinth API.
 */
public class UpdateChecker {

    private final JavaPlugin plugin;
    private final String projectId;
    private volatile String latestVersion = null;
    private volatile String downloadUrl = null;
    private volatile boolean updateAvailable = false;

    public UpdateChecker(JavaPlugin plugin, String projectId) {
        this.plugin = plugin;
        this.projectId = projectId;
    }

    /**
     * Checks for updates asynchronously.
     */
    public void checkForUpdates() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                String currentVersion = plugin.getPluginMeta().getVersion();

                // Get server's Minecraft version for filtering
                String gameVersion = plugin.getServer().getMinecraftVersion();

                // Modrinth API endpoint with game version and loader filter
                String apiUrl = "https://api.modrinth.com/v2/project/" + projectId + "/version"
                        + "?game_versions=" + URLEncoder.encode("[\"" + gameVersion + "\"]", "UTF-8")
                        + "&loaders=" + URLEncoder.encode("[\"paper\"]", "UTF-8");

                URL url = java.net.URI.create(apiUrl).toURL();
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "CustomJukebox-BedrockExtension/" + currentVersion);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    plugin.getLogger().warning("Failed to check for updates: HTTP " + responseCode);
                    return;
                }

                java.io.InputStream inputStream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                // Parse JSON response
                JsonArray versions = JsonParser.parseString(response.toString()).getAsJsonArray();

                if (versions.isEmpty()) {
                    return;
                }

                // Get the latest version (first in array)
                JsonObject latestVersionObj = versions.get(0).getAsJsonObject();
                latestVersion = latestVersionObj.get("version_number").getAsString();

                // Get download URL
                JsonArray files = latestVersionObj.getAsJsonArray("files");
                if (files != null && !files.isEmpty()) {
                    JsonObject primaryFile = files.get(0).getAsJsonObject();
                    downloadUrl = primaryFile.get("url").getAsString();
                }

                // Compare versions
                int comparison = compareVersions(currentVersion, latestVersion);

                if (comparison < 0) {
                    updateAvailable = true;
                    plugin.getLogger().info("====================================");
                    plugin.getLogger().info("UPDATE AVAILABLE!");
                    plugin.getLogger().info("Current version: " + currentVersion);
                    plugin.getLogger().info("Latest version: " + latestVersion);
                    plugin.getLogger().info("Download: https://modrinth.com/plugin/bs-customjukebox-bedrock-extension");
                    plugin.getLogger().info("====================================");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
            } finally {
                try {
                    if (reader != null) reader.close();
                } catch (Exception ignored) {}
                if (connection != null) connection.disconnect();
            }
        });
    }

    public boolean isUpdateAvailable() { return updateAvailable; }
    public String getLatestVersion() { return latestVersion; }
    public String getDownloadUrl() { return downloadUrl; }
    public String getCurrentVersion() { return plugin.getPluginMeta().getVersion(); }

    private int compareVersions(String version1, String version2) {
        version1 = version1.replaceAll("^[^0-9]+", "");
        version2 = version2.replaceAll("^[^0-9]+", "");

        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            if (num1 != num2) return Integer.compare(num1, num2);
        }
        return 0;
    }

    private int parseVersionPart(String part) {
        try {
            String numericPart = part.split("[^0-9]")[0];
            return numericPart.isEmpty() ? 0 : Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
