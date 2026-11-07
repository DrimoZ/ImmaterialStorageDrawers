package dev.drimoz.immaterialstorage.energy;

import dev.drimoz.immaterialstorage.network.NetworkNodeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The Energy Drawer block. Storage and linking live in {@link EnergyDrawerBlockEntity}. */
public class EnergyDrawerBlock extends NetworkNodeBlock {

    public EnergyDrawerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyDrawerBlockEntity(pos, state);
    }
}
