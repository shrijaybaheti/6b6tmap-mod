# 6b6t Spawn Map Mod

A lightweight client-side Fabric mod for scanning surface blocks around 6b6t spawn and submitting them to the live map server.

> **Disclaimer:**
> This mod is currently in **Beta**. It was built as a baseline community tool to populate the map. It might have rough edges or quirks. The community is heavily encouraged to fork this repo, improve chunk batching, clean up the architecture, rewrite parts, add features, or release their own better versions. PRs and forks are welcome!

---

- Scans all non-air blocks and fluids across all chunk sections
- Automatically streams chunk data to the configured web map backend
- Built-in retry queue with exponential backoff and deduplication
- On-screen HUD showing scan status, queue size, and upload statistics
- Multi-version Gradle setup supporting **Minecraft 1.20.4** and **Minecraft 1.21.11**

---

## Commands

All commands start with `/6b6tmap`:

- `/6b6tmap toggle` &mdash; Enable or disable chunk scanning
- `/6b6tmap status` &mdash; Print current network stats, queue size, and connection info
- `/6b6tmap server <url>` &mdash; Set the map upload server URL (e.g. `http://map.6b6t.store/api/chunks/submit`)
- `/6b6tmap player <name>` &mdash; Set the player name attached to uploaded chunks
- `/6b6tmap token <token>` &mdash; Set the authentication token for uploading chunks
- `/6b6tmap area spawn [radius]` &mdash; Restrict scanning within spawn radius (default: 5000 blocks)
- `/6b6tmap area world` &mdash; Allow scanning anywhere across the world
- `/6b6tmap resetcache` &mdash; Clear session cache of already-uploaded chunks

---

## Building from Source

### Requirements
- JDK 21 or higher
- Git

### Build Commands

Clone the repository:
```bash
git clone https://github.com/<your-username>/6b6tmap-mod.git
cd 6b6tmap-mod
```

Build mod jars for all supported versions:
```bash
./gradlew jars
```

Or build a specific version:
```bash
# For Minecraft 1.20.4
./gradlew :mc1204:remapJar

# For Minecraft 1.21.11
./gradlew :mc12111:remapJar
```

The compiled jars will be located in:
- `versions/1.20.4/build/libs/`
- `versions/1.21.11/build/libs/`

---

## Configuration

Configuration is saved in `.minecraft/config/6b6tmap.json`:

```json
{
  "enabled": true,
  "serverUrl": "http://map.6b6t.store/api/chunks/submit",
  "submitToken": "your-token-here",
  "playerOverride": "",
  "spawnRadius": 5000,
  "recordWorld": false,
  "scanIntervalTicks": 2
}
```

---

## License

This project is licensed under the [MIT License](LICENSE).
