package com.sondplay.paradise.transform;

import net.minecraft.launchwrapper.IClassTransformer;

/**
 * Routes class transformations to the appropriate sub-transformer.
 * Checks class names against configured targets and dispatches accordingly.
 */
public class TransformerDispatcher implements IClassTransformer {

    private final WorldgenOffsetTransformer worldgenOffset = new WorldgenOffsetTransformer();
    private final WorldgenCenterTransformer worldgenCenter = new WorldgenCenterTransformer();
    private final NEIRepairTransformer neiRepair = new NEIRepairTransformer();
    private final SpawnCheckTransformer spawnCheck = new SpawnCheckTransformer();

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return null;

        // NEI RepairRecipeHandler fix
        if (transformedName.equals("codechicken.nei.recipe.RepairRecipeHandler")) {
            return neiRepair.transform(bytes);
        }

        // OreSpawn spawn check cache
        if (transformedName.startsWith("danger.orespawn.")) {
            bytes = spawnCheck.transform(transformedName, bytes);
        }

        // Worldgen cascade fixes
        if (isWorldgenOffsetTarget(transformedName)) {
            bytes = worldgenOffset.transform(transformedName, bytes);
        }
        if (isWorldgenCenterTarget(transformedName)) {
            bytes = worldgenCenter.transform(transformedName, bytes);
        }

        return bytes;
    }

    private boolean isWorldgenOffsetTarget(String className) {
        return WorldgenOffsetTransformer.isTarget(className);
    }

    private boolean isWorldgenCenterTarget(String className) {
        return WorldgenCenterTransformer.isTarget(className);
    }
}
