package dev.drimoz.immaterialstorage.energy;

import dev.drimoz.immaterialstorage.network.NetworkNodeBlock;
import dev.drimoz.immaterialstorage.upgrade.DrawerUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Energy Drawer block: player interaction. Storage and linking live in
 * {@link EnergyDrawerBlockEntity}.
 *
 * <p>Right-click with a Storage Drawers upgrade installs it. Sneak + right-click with an empty hand
 * takes the last one out: a stopgap until the drawer has a screen with upgrade slots.
 */
public class EnergyDrawerBlock extends NetworkNodeBlock {

    public EnergyDrawerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyDrawerBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!DrawerUpgrades.isAccepted(stack) || !(level.getBlockEntity(pos) instanceof EnergyDrawerBlockEntity drawer)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!drawer.installUpgrade(stack)) {
            // Storage Drawers' own message, from its language files.
            player.displayClientMessage(Component.translatable("message.storagedrawers.cannot_add_upgrade"), true);
            return ItemInteractionResult.CONSUME;
        }
        stack.consume(1, player);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof EnergyDrawerBlockEntity drawer)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (drawer.upgrades().last().isEmpty()) {
            return InteractionResult.PASS;
        }
        ItemStack removed = drawer.removeLastUpgrade();
        if (removed.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.immaterialstorage_sd.upgrade_would_spill"), true);
        } else {
            player.getInventory().placeItemBackInInventory(removed);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof EnergyDrawerBlockEntity drawer) {
            drawer.upgrades().installed().forEach(stack -> popResource(level, pos, stack));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
