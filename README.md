# $ond'Play Paradise

All-in-one **runtime** performance mod for Minecraft 1.7.10 modpacks with OreSpawn, SuperHeroes Unlimited, and heavy worldgen mods.

Applies ASM bytecode transformations and Mixin patches **at class-load time** — no need to edit or replace other mod jars. Drop it in `mods/` and everything is fixed automatically.

---

## The Problem

Heavy 1.7.10 modpacks suffer from multiple compounding performance issues:

- **Cascading worldgen** — Mods generate features at `chunkX*16 + random(0-15)`, which can land in neighboring chunks, triggering their generation, which triggers more neighbors... Chain reaction. A single chunk load can cascade into 40,000+ chunk generations.
- **OreSpawn entity overhead** — Hundreds of OreSpawn mobs running full AI every tick, even when no player is anywhere near them. 22% of server tick spent on mobs nobody can see.
- **SuperHeroes Unlimited event spam** — ~250 event handlers fire on every `LivingUpdateEvent` for every entity, checking if it is wearing a superhero suit. 99.9% of entities are not players and can never wear suits.
- **NEI RepairRecipe freeze** — `RepairRecipeHandler.buildCache()` does an O(n^2) scan over all items (~15k x ~15k), allocating ~23 million objects. Freezes the game for 10+ seconds.
- **Morph + Hodgepodge crash** — Morph casts NBT internal map to `HashMap`, but Hodgepodge replaces it with fastutil `Object2ObjectOpenHashMap`. Instant `ClassCastException`.
- **Item drops in water** — Thousands of `EntityItem` instances accumulate in oceans/rivers, each ticking every frame. 4% of server tick.

## The Solution

Paradise fixes all of the above **without modifying any other mod jar file**. It intercepts classes as they load and patches them in memory.

---

## Features

### ASM Transformers (bytecode rewriting at class-load)

#### 1. Cascading Worldgen Fix — WorldgenOffsetTransformer
Detects the pattern `chunkX * 16` (or `chunkX << 4`) in worldgen methods and adds `+ 8`, centering generation within the chunk so it never crosses into neighbors.

**Mods patched:** Legends Core, Superheroes Unlimited, Advent of Ascension, Mystic Mods (Stones + Ores), Chisel, IC2, Forestry, ExtrabiomesXL, MineFactory Reloaded

#### 2. Cascading Worldgen Fix — WorldgenCenterTransformer
Detects `Random.nextInt(16)` in worldgen and rewrites it to `nextInt(1)` (always returns 0), anchoring features at chunk center instead of random positions that can spill over.

**Mods patched:** Same as above + Forestry, ExtrabiomesXL

#### 3. NEI Freeze Fix — NEIRepairTransformer
Replaces the body of `RepairRecipeHandler.buildCache()` with a no-op (`cachedRecipes = new ArrayList(); return;`). The repair recipe handler is useless in modpacks with thousands of items.

**Mod patched:** NotEnoughItems-2.8.105-GTNH

#### 4. Spawn Check Cache — SpawnCheckTransformer
Wraps `getCanSpawnHere()` in OreSpawn entity classes with a 100ms TTL cache. OreSpawn spawn checks are expensive (biome lookups, light checks, collision scans) and get called repeatedly for the same entity.

**Mod patched:** All `danger.orespawn.*` entity classes (excluding bosses)

---

### Mixins (via UniMixins — surgical method injection)

#### 5. OreSpawn AI Throttle — MixinOreSpawnAI
Throttles `updateAITasks()` for non-boss OreSpawn mobs based on distance to nearest player:
- **< 32 blocks:** Full AI every tick (no change)
- **32-64 blocks:** AI runs 1 of every 3 ticks
- **> 64 blocks:** AI runs 1 of every 5 ticks

Boss mobs (Godzilla, TheKing, Mobzilla, Kraken, etc.) always run at full speed.

#### 6. OreSpawn OnUpdate Throttle — MixinOreSpawnOnUpdate
Skips `onUpdate()` for stationary/ambient OreSpawn entities (RockBase, insects, worms, butterflies) 3 out of 4 ticks when no player is within 32 blocks. These mobs do expensive random calculations every tick but do not actually need physics or AI.

#### 7. Aquatic Item Despawn — MixinAquaticItemDespawn
Items sitting in water with no owner despawn in 30 seconds instead of 5 minutes.

**Protected items (normal 5min despawn):**
- Items dropped by players
- Enchanted items
- Items with custom names
- Unstackable items (tools, armor, weapons)
- ALL OreSpawn items (scales, bones, crafting materials)

**Fast despawn:** Only generic junk (bones, rotten flesh, feathers, leather, etc.)

#### 8. SuperHeroes Event Filter — MixinEventHandler
Intercepts `ASMEventHandler.invoke()` for SuperHeroes Unlimited handlers. For `LivingUpdateEvent`:
- If entity is not a player: cancel immediately
- If entity is a player: check once per second if they have any SHS items equipped. If not: cancel.

Eliminates ~250 handler invocations per tick per non-player entity.

#### 9. SuperHeroes EventBus Filter — MixinEventBus
The heavy win optimization. Redirects `ListenerList.getListeners()` inside `EventBus.post()` to return a filtered array that excludes all SuperHeroes handlers entirely for non-player `LivingUpdateEvent`. The dispatch loop never even iterates over them.

#### 10. Morph Crash Fix — MixinMorphState
Overwrites `MorphState.parseTag()` to use the `java.util.Map` interface via reflection instead of casting to `HashMap`. Works with both vanilla HashMap and Hodgepodge fastutil replacement.

---

### Event Handlers (runtime logic)

#### 11. OreSpawn Memory Manager — OreSpawnMemoryManager
Monitors OreSpawn mob population every second:
- **Soft cap (200):** Starts culling OreSpawn mobs above this count
- **Hard cap (300):** Immediate culling
- **Heap pressure (>75%):** Emergency cull 15% of OreSpawn mobs regardless of count

Only kills OreSpawn mobs (`danger.orespawn.*`). Never touches vanilla or other mod entities.

#### 12. Spawn Check Cache — SpawnCheckCache
WeakHashMap-based cache with 100ms TTL backing the SpawnCheckTransformer. Entities that fail spawn checks do not get re-checked for 100ms (2 ticks).

---

## Performance Results

Measured on the Modpack Edredom (200+ mods, OreSpawn + SuperHeroes + AoA + heavy worldgen):

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Cascading worldgen events | 40,735 | 1,461 | **-96%** |
| TPS (view distance 32) | 6.95 | 14.39 | **+107%** |
| Server idle time (Thread.sleep) | 0.08% | 2.44% | Server has headroom |
| Entity tick share | 22% | ~7% | **-68%** |

---

## Requirements

- Minecraft 1.7.10 + Forge (10.13.4.1614+)
- [UniMixins](https://github.com/LegacyModdingMC/UniMixins) 0.3.1+ (must be in mods/ with `+` prefix)

## Installation

1. Drop `SondPlayParadise-1.0.0.jar` into `.minecraft/mods/`
2. **Remove** if present: `suspatch-1.0.jar`, `orespawntweaks-v5.3.jar`
3. **Restore original jars** of any mods you previously patched manually (Legends, SHS, AoA, Mystic, Chisel, IC2, Forestry, ExtrabiomesXL, MFR, NEI, Morph)
4. Launch — check the log for `[Paradise]` messages confirming patches applied

## Configuration

Configurable options in `config/sondplayparadise.cfg`:

```properties
# Enable/disable mob population cap and heap pressure culling
enableMobCap=true

# Mob cap thresholds
softCap=200
hardCap=300
```

> **Note (v1.0):** ASM and Mixin patches are always active. Individual toggles for disabling specific patches will be added in a future version via early config loading.

## Building from source

```
build.cmd
```

Output: `build/SondPlayParadise-1.0.0.jar`

Dependencies (auto-resolved from local PolyMC installation):
- ASM 5.0.3
- Forge 1.7.10-10.13.4.1614 universal
- LaunchWrapper 1.12
- Log4j 2.0-beta9
- UniMixins 0.3.1

## How it works (technical)

Paradise registers as an FML CorePlugin (`IFMLLoadingPlugin`) which gives it access to the class-loading pipeline. Before any mod class is available to the game, Paradise can inspect and rewrite its bytecode.

1. **ASM Transformers** fire via `IClassTransformer` — they pattern-match bytecode sequences and inject/replace instructions
2. **Mixins** fire via UniMixins/SpongePowered Mixin — they inject methods, redirect calls, or overwrite methods in target classes
3. **Event Handlers** register normally via Forge event bus in the `@Mod` init phase

The combination means: worldgen classes get patched before they ever generate a single block, NEI handler gets neutered before it ever tries to cache, and OreSpawn entities get throttled from their very first tick.

## Replaces

This single jar replaces all of the following:
- `suspatch-1.0.jar` (5 mixins for OreSpawn/SHS)
- `orespawntweaks-v5.3.jar` (mob cap + spawn cache)
- Manual bytecode patches applied to 12 mod jars via PatchWorldgenOffset.java and PatchCenterFeature.java
- Manual NEI jar edit via PatchNEIRepair.java
- Manual Morph jar edit via PatchMorphState.java

## License

MIT
