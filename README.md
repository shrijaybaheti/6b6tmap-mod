# 6b6tmap mod

Client mod for 6b6t that maps chunks around you and sends the block data to the live map.

## Commands

All commands start with `/6b6tmap`:

- `/6b6tmap toggle` - Turn scanning on/off
- `/6b6tmap status` - Show upload count, queue size, and error info
- `/6b6tmap server <url>` - Set the map server URL
- `/6b6tmap token <token>` - Set auth token for the backend
- `/6b6tmap area spawn [radius]` - Restrict mapping to spawn (defaults to 5k blocks)
- `/6b6tmap area world` - Record anywhere in the world
- `/6b6tmap resetcache` - Clear the session cache so it rescans visited chunks

## Compiling

Build every configured version or one target:

```bash
bash gradlew buildall
bash gradlew build1_21_11
```

Other targets use the same names: `build1_20`, `build1_20_4`, `build1_21_1`, `build1_21_4`, `build1_21_5`, `build26_1`, `build26_2`, and `build26_3`. `jars` and `buildAll` alias `buildall`. The version tasks run isolated builds sequentially, following BabelAddon's pattern. Outputs are in `build/versions/<version>/libs/`.

All versions build from this root project. Shared 1.20–1.21.11 code is in `src/main/java`; small API adapters live in `src/versionAdapters/`. The 26.x source set uses its own mappings and rendering APIs there. Use JDK 17 for 1.20.x, JDK 21 for 1.21.x, and JDK 25 for 26.x. There are no per-version Gradle projects.

## Config

Saved to `.minecraft/config/6b6tmap.json`:

```json
{
  "enabled": true,
  "serverUrl": "http://map.6b6t.store/api/chunks/submit",
  "submitToken": "your-token",
  "spawnRadius": 5000,
  "recordWorld": false,
  "scanIntervalTicks": 2
}
```
