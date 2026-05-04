# BS-CustomJukebox Bedrock Extension

Companion plugin for [BS-CustomJukebox](https://modrinth.com/plugin/bs-customjukebox) that adds **Bedrock Edition support** via Geyser/Floodgate.

## Features

- **Sound Bridging** — Custom disc sounds are automatically played to Bedrock players when a disc is inserted into a jukebox
- **Resource Pack Generator** — Generates a Bedrock-compatible `.mcpack` from your existing custom discs with a single command
- **Custom Item Registration** — Registers custom disc items via Geyser so Bedrock players see unique disc textures
- **Zero impact on Java players** — Java Edition playback is completely unchanged
- **Graceful degradation** — Plugin runs without Geyser/Floodgate installed (logs a warning, no crash)

## Requirements

- **BS-CustomJukebox** v2.2.1+ (required)
- **Geyser-Spigot** (required for Bedrock features)
- **Floodgate** (recommended for reliable Bedrock player detection)
- Paper 1.21+
- Java 21+

## Quick Setup

### 1. Install plugins

Place both JARs in your server's `plugins/` folder along with Geyser-Spigot and Floodgate.

### 2. Generate the Bedrock resource pack

```
/cjb-bedrock generate
```

This creates a `.mcpack` file at:
```
plugins/CustomJukebox-BedrockExtension/bedrock-pack/CustomJukebox-Bedrock.mcpack
```

### 3. Deploy the resource pack

Copy the generated `.mcpack` file to Geyser's `packs/` folder:
```
plugins/Geyser-Spigot/packs/CustomJukebox-Bedrock.mcpack
```

### 4. Restart the server

Restart the server so that:
- Geyser loads the new resource pack
- Custom items are registered for Bedrock clients

Bedrock players will automatically receive the resource pack when connecting.

### 5. Add sound files

Place your `.ogg` sound files in:
```
plugins/CustomJukebox/sounds/
```

The file names should match the sound key's last segment. For example, if your disc's sound key is `customjukebox:music_disc.epic_journey`, the file should be named `epic_journey.ogg`.

### 6. (Optional) Add disc textures

Place PNG textures in:
```
plugins/CustomJukebox/textures/
```

Name them `{disc_id}.png` or `disc_{disc_id}.png`.

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/cjb-bedrock generate` | `customjukebox.bedrock.admin` | Generate Bedrock resource pack |
| `/cjb-bedrock reload` | `customjukebox.bedrock.admin` | Reload configuration |
| `/cjb-bedrock status` | `customjukebox.bedrock.admin` | Show plugin status |
| `/cjb-bedrock help` | `customjukebox.bedrock.admin` | Show help |

Alias: `/cjbb`

## Configuration

`plugins/CustomJukebox-BedrockExtension/config.json`:

```json
{
  "enabled": true,
  "debug": false,
  "packOutputDirectory": "bedrock-pack",
  "autoRegisterItems": true,
  "autoDeliverPack": true,
  "soundPrefix": "customjukebox",
  "packName": "CustomJukebox Bedrock Sounds",
  "packDescription": "Custom disc sounds for Bedrock Edition"
}
```

| Field | Description |
|-------|-------------|
| `enabled` | Enable/disable the plugin |
| `debug` | Enable verbose logging |
| `packOutputDirectory` | Where to output generated packs |
| `autoRegisterItems` | Register custom items with Geyser on startup |
| `autoDeliverPack` | Auto-deliver pack to Bedrock players |
| `soundPrefix` | Namespace prefix for Bedrock sounds |
| `packName` | Display name of the Bedrock resource pack |
| `packDescription` | Description shown in Bedrock settings |

## Troubleshooting

### Bedrock players don't hear sounds
1. Check `/cjb-bedrock status` to verify Geyser/Floodgate are detected
2. Ensure you ran `/cjb-bedrock generate` after adding/changing discs
3. Verify the `.mcpack` is in Geyser's `packs/` folder
4. Restart the server (not just reload)
5. Check that `.ogg` files exist in `plugins/CustomJukebox/sounds/`

### Custom items don't show for Bedrock
- Custom items are registered once at server startup
- After adding new discs, regenerate the pack and restart
- Check console for registration errors

### Java players are not affected
This plugin only modifies behavior for Bedrock players. Java Edition playback is completely unchanged.
