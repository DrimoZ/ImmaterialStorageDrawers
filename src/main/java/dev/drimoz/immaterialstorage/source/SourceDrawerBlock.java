package dev.drimoz.immaterialstorage.source;

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

/**
 * The Source Drawer block. <b>Ars Nouveau only: see {@code compat.Mods}.</b> Filled and emptied by
 * Ars itself (relays, sourcelinks, the apparatus), like a Source Jar; everything a drawer does by
 * hand is in {@link DrawerBlock}.
 */
public class SourceDrawerBlock extends DrawerBlock {

    public SourceDrawerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SourceDrawerBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends DrawerBlockEntity> blockEntityType() {
        return SourceContent.BLOCK_ENTITY.get();
    }

    @Override
    protected void describeContent(CompoundTag saved, @Nullable HolderLookup.Provider registries, List<Component> tooltip) {
        Component source = registries == null ? null : SourceDrawerBlockEntity.describeStored(saved, registries);
        if (source != null) {
            tooltip.add(source);
        }
    }
}
