package dev.drimoz.immaterialstorage.fluid;

import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.drawer.DrawerBlock;
import dev.drimoz.immaterialstorage.drawer.DrawerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Fluid Drawer block. Right-click with a bucket or any fluid container fills or empties the
 * drawer, through NeoForge's own bucket logic; everything else a drawer does by hand is in
 * {@link DrawerBlock}.
 */
public class FluidDrawerBlock extends DrawerBlock {

    public FluidDrawerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidDrawerBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends DrawerBlockEntity> blockEntityType() {
        return ImmaterialStorage.FLUID_DRAWER_BLOCK_ENTITY.get();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getCapability(Capabilities.FluidHandler.ITEM) != null) {
            return FluidUtil.interactWithFluidHandler(player, hand, level, pos, null)
                    ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                    : ItemInteractionResult.CONSUME;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected void describeContent(CompoundTag saved, @Nullable HolderLookup.Provider registries, List<Component> tooltip) {
        Component fluid = FluidDrawerBlockEntity.describeStored(saved, registries);
        if (fluid != null) {
            tooltip.add(fluid);
        }
    }
}
