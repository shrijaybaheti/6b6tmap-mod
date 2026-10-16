# 6b6tmap mod

Client mod for 6b6t that maps chunks around you and submits block data to the live map.

## Requirements

- Minecraft Fabric Loader (>= 0.15.0)
- Fabric API
- Connected to 6b6t (`6b6t.org`, `6b6t.net`, `6b6t.co`, `6b6t.me`, `6b6t.cc`, etc.)
- World difficulty set to **Hard**

## Commands

All commands start with `/6b6tmap`:

- `/6b6tmap` or `/6b6tmap gui` - Open the configuration GUI screen
- `/6b6tmap toggle` - Toggle scanning on or off (`/6b6tmap toggle on` / `/6b6tmap toggle off`)
- `/6b6tmap status` - Show scanning status, queue metrics, tracked chunk count, and server info
- `/6b6tmap hud <status|map|void|nametag>` - Toggle specific HUD elements
- `/6b6tmap hud edit` - Open the visual HUD repositioning editor
- `/6b6tmap area <diameter>` - Set square recording boundary centered at (0, 0) with side length <diameter> (32 to 60,000,000 blocks)
- `/6b6tmap area world` - Record all visited chunks across the entire world
- `/6b6tmap server <url>` - Set the submission backend URL (default: `http://map.6b6t.store/api/chunks/submit`)
- `/6b6tmap token <token>` - Set the authorization token for chunk submissions
- `/6b6tmap resetcache` - Clear the session cache to rescan already visited chunks

## Supported Versions

Built across multiple Minecraft release targets:
- 1.20 / 1.20.4
- 1.21.1 / 1.21.4 / 1.21.5 / 1.21.11
- 26.1 / 26.2 / 26.3

## Compiling

Build all configured versions or a single target:

```bash
./gradlew buildall
./gradlew build1_21_11
```

Individual target tasks:
- `build1_20`
- `build1_20_4`
- `build1_21_1`
- `build1_21_4`
- `build1_21_5`
- `build1_21_11`
- `build26_1`
- `build26_2`
- `build26_3`

Outputs are placed in `build/versions/<version>/libs/`.

Use JDK 17 for 1.20.x, JDK 21 for 1.21.x, and JDK 25 for 26.x.

## Configuration

Config file is saved at `.minecraft/config/6b6tmap.json`:

```json
{
  "enabled": true,
  "serverUrl": "http://map.6b6t.store/api/chunks/submit",
  "submitToken": "your-token",
  "spawnRadius": 25000,
  "recordWorld": false,
  "showHud": true,
  "showMinimap": false,
  "showVoidHud": false,
  "showNametagHud": false,
  "scanIntervalTicks": 2
}
```
