# 6b6tmap mod

Client mod for 6b6t that maps chunks around you and sends the block data to the live map.

Everything runs off the main thread so you can fly around without getting lag spikes or stuttering.

## Features

- Scans chunks in background threads so to not affect much performance.
- Compresses payloads with gzip before uploading
- Batches chunks together to save bandwidth
- Shows a small on-screen counter for upload status and queue size
- Dynamic version support (works across 1.18 through 1.21+)

## Commands

All commands start with `/6b6tmap`:

- `/6b6tmap toggle` - Turn scanning on/off
- `/6b6tmap status` - Show upload count, queue size, and error info
- `/6b6tmap server <url>` - Set the map server URL
- `/6b6tmap player <name>` - Override your player name if needed
- `/6b6tmap token <token>` - Set auth token for the backend
- `/6b6tmap area spawn [radius]` - Restrict mapping to spawn (defaults to 5k blocks)
- `/6b6tmap area world` - Record anywhere in the world
- `/6b6tmap resetcache` - Clear the session cache so it rescans visited chunks

## Compiling

Requires JDK 21.

Build both jars:
```bash
./gradlew jars
```

Build a specific version:
```bash
./gradlew :mc1204:remapJar
./gradlew :mc12111:remapJar
```

Built jars go into `versions/<version>/build/libs/`.

## Config

Saved to `.minecraft/config/6b6tmap.json`:

```json
{
  "enabled": true,
  "serverUrl": "http://map.6b6t.store/api/chunks/submit",
  "submitToken": "your-token",
  "playerOverride": "",
  "spawnRadius": 5000,
  "recordWorld": false,
  "scanIntervalTicks": 2
}
```

## License

MIT
