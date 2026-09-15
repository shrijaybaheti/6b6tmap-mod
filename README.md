# 6b6t Spawn Map Mod

Fabric mod that scans chunks as you walk or fly around 6b6t and uploads block data to the map server.

Designed to be lightweight and run in the background without dropping frames or lagging your game.

---

## What it does

- Scans chunks around you asynchronously using worker threads (never blocks the render tick).
- Pre-compresses payloads with gzip before queuing so uploads go out quickly.
- Uploads in batches with automatic retries and deduplication if a chunk hasn't changed.
- Compact in-game HUD showing upload progress, queue count, and network stats.
- Works on both Minecraft 1.20.4 and 1.21.1.

---

## In-game Commands

Base command: `/6b6tmap`

- `/6b6tmap player <name>` — Set your username if auto-detection doesn't catch it.
- `/6b6tmap toggle` — Turn scanning on/off.
- `/6b6tmap status` — View active stats (queue depth, total uploaded, errors).
- `/6b6tmap server <url>` — Point to your server (default: `http://map.6b6t.store/api/chunks/submit`).
- `/6b6tmap token <value>` — Set upload token if your backend requires it.
- `/6b6tmap area spawn [radius]` — Limit recording to a set distance from spawn (default: 5000).
- `/6b6tmap area world` — Scan anywhere in the world.
- `/6b6tmap resetcache` — Clear the local session cache to rescan already-visited chunks.

---

## Building

Requires Java 21+.

Build jars for both 1.20.4 and 1.21.1:
```bash
./gradlew jars
```

Or build a single version:
```bash
./gradlew :mc1204:remapJar
./gradlew :mc12111:remapJar
```

Jars will output to:
- `versions/1.20.4/build/libs/`
- `versions/1.21.11/build/libs/`

---

## Config

Settings live in `.minecraft/config/6b6tmap.json`:

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

---

## License

MIT
