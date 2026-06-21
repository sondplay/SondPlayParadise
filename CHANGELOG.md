# Changelog

## 1.0.0 (2026-06-21)

Initial release — consolidates suspatch-1.0.jar + orespawntweaks-v5.3.jar + manual jar patches into a single runtime mod.

### ASM Transformers
- **WorldgenOffsetTransformer**: Adds +8 offset to worldgen `chunkX*16` patterns (fixes cascading worldgen)
- **WorldgenCenterTransformer**: Converts `nextInt(16)` to `nextInt(1)` in worldgen feature placement
- **NEIRepairTransformer**: Neutralizes `RepairRecipeHandler.buildCache()` O(n²) freeze
- **SpawnCheckTransformer**: Caches `getCanSpawnHere()` with 100ms TTL for OreSpawn entities

### Mixins (via UniMixins)
- **MixinOreSpawnAI**: Distance-based AI throttle (<32: full, 32-64: 1/3, >64: 1/5)
- **MixinOreSpawnOnUpdate**: Cancels onUpdate 3/4 ticks for stationary/ambient OreSpawn mobs
- **MixinAquaticItemDespawn**: Generic items in water despawn in 30s (protects player drops, enchanted, unstackable, OreSpawn items)
- **MixinEventHandler**: Filters SuperHeroes Unlimited handlers for non-players
- **MixinEventBus**: Returns filtered listener array excluding SHS handlers for non-player LivingUpdateEvent
- **MixinMorphState**: Fixes Morph+Hodgepodge fastutil HashMap crash

### Handlers
- **OreSpawnMemoryManager**: Mob cap (soft 200/hard 300) + heap pressure culling
- **SpawnCheckCache**: WeakHashMap cache with 100ms TTL

### Config
- All features individually toggleable via `sondplayparadise.cfg`
- Worldgen targets configurable (add/remove mods to patch)
- Mob cap thresholds configurable
