# Changelog

## v2.1.0

- Unified release: one jar now supports Paper 1.21.4 through 26.x (matches CustomJukebox 3.1.0)
- Built against Paper API 1.21.4 with `api-version: '1.21'` and Java 21 bytecode — loads on 1.21.4+ servers (Java 21) and 26.x servers (Java 25) alike
- Requires CustomJukebox 3.1.0+

## v2.0.0

- Updated to Paper 26.1.2 (new versioning system)
- Updated to Java 25
- Updated Geyser API to 2.10.0-SNAPSHOT
- Updated Floodgate API to 2.2.5-SNAPSHOT
- Updated Gradle to 9.5.0

## v1.1.0

- Added bStats metrics integration
- UpdateChecker now filters by game version and loader via Modrinth API — users only see updates compatible with their Minecraft version
- Fixed thread-safety issue in UpdateChecker (volatile fields)

## v1.0.0 — Initial Release

- Bedrock sound bridging via Geyser — custom disc sounds are played to Bedrock players
- Bedrock resource pack generator (`/cjb-bedrock generate`) - creates `.mcpack` with manifest, sound definitions, and sound files
- Custom item registration via Geyser Custom Items API v2
- Bedrock player detection via Floodgate API
- Graceful no-op when Geyser or Floodgate is not installed
- JSON-based configuration
- Commands: `/cjb-bedrock generate`, `reload`, `status`, `help`
- Modrinth update checker with join notifications
