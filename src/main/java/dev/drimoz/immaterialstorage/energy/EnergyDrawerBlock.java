package dev.drimoz.immaterialstorage.energy;

import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.drawer.DrawerBlock;
import dev.drimoz.immaterialstorage.drawer.DrawerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The Energy Drawer block. Everything a drawer does by hand is in {@link DrawerBlock}. */
public class EnergyDrawerBlock extends DrawerBlock {

    public EnergyDrawerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyDrawerBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends DrawerBlockEntity> blockEntityType() {
        return ImmaterialStorage.ENERGY_DRAWER_BLOCK_ENTITY.get();
    }

    @Override
    protected void describeContent(CompoundTag saved, @Nullable HolderLookup.Provider registries, List<Component> tooltip) {
        Component energy = EnergyDrawerBlockEntity.describeStored(saved);
        if (energy != null) {
            tooltip.add(energy);
        }
    }
}
