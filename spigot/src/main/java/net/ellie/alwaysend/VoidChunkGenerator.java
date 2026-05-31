package net.ellie.alwaysend;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

public class VoidChunkGenerator extends ChunkGenerator {

    @Override
    public boolean canSpawn(World world, int x, int z) {
        return true;
    }
}
