# $ond'Play Paradise

All-in-one runtime performance mod for Minecraft 1.7.10 modpacks with OreSpawn, SuperHeroes Unlimited, and heavy worldgen mods.

Applies ASM and Mixin patches **at runtime** — no need to modify other mod jars.

## What it fixes

| Problem | Solution | Impact |
|---------|----------|--------|
| Cascading worldgen (40k+ cascades) | +8 offset + center fix via ASM | -96% cascades |
| OreSpawn mob AI lag (22% tick) | Distance-based AI throttle | -70% entity tick |
| SuperHeroes event spam | Filtered listener arrays | -15% tick |
| NEI RepairRecipe O(n²) freeze | Neutralize buildCache() | No more RAM leak |
| Items in water (4% tick) | Fast despawn for junk items | -80% EntityItem |
| Morph + Hodgepodge crash | HashMap cast fix | No more crash |
| OreSpawn mob overpopulation | Soft/hard cap + heap culling | Stable RAM |

## Requirements

- Minecraft 1.7.10 + Forge
- [UniMixins](https://github.com/LegacyModdingMC/UniMixins) 0.3.1+

## Installation

1. Drop `SondPlayParadise-1.0.0.jar` into `.minecraft/mods/`
2. Remove: `suspatch-1.0.jar` and `orespawntweaks-v5.3.jar` (if present)
3. Restore original mod jars if previously patched manually
4. Launch — all patches apply automatically

## Configuration

## Configuration

Configurable options in `config/sondplayparadise.cfg`:

```properties
# Enable/disable mob population cap and heap pressure culling
enableMobCap=true

# Mob cap thresholds
softCap=200
hardCap=300
```

> **Note (v1.0):** ASM and Mixin patches (worldgen fix, NEI fix, AI throttle, item despawn, event filter, morph fix, spawn cache) are always active. Individual toggles for these will be added in a future version.

## Building from source

```
build.cmd
```

Output: `build/SondPlayParadise-1.0.0.jar`

## License

MIT
