package dev.drimoz.immaterialstorage.drawer;

import dev.drimoz.immaterialstorage.network.NetworkNodeBlock;
import dev.drimoz.immaterialstorage.upgrade.DrawerUpgrades;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What every drawer block of ours shares: facing, upgrades by hand, drops that keep the contents,
 * the item tooltip, and the sync ticker. Subclasses add what their content needs.
 *
 * <p>Right-click with a Storage Drawers upgrade installs it. Sneak + right-click with an empty hand
 * takes the last one out: a stopgap until drawers have a screen with upgrade slots.
 *
 * <p>Broken, a drawer keeps its contents and upgrades in the dropped item (vanilla's block entity
 * data component, applied back on placement), so breaking one never destroys anything. An empty
 * drawer drops a plain, stackable item.
 */
public abstract class DrawerBlock extends NetworkNodeBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    protected DrawerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** The block entity type of this drawer, for the ticker. */
    protected abstract BlockEntityType<? extends DrawerBlockEntity> blockEntityType();

    /** Tooltip lines for the content in a dropped drawer's saved data; nothing when it holds none. */
    protected abstract void describeContent(CompoundTag saved, @Nullable HolderLookup.Provider registries,
                                            List<Component> tooltip);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != blockEntityType()
                ? null
                : (BlockEntityTicker<T>) (BlockEntityTicker<DrawerBlockEntity>) DrawerBlockEntity::serverTick;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) {
            return;
        }
        CompoundTag saved = data.copyTag();
        List<Component> lines = new java.util.ArrayList<>();
        describeContent(saved, context.registries(), lines);
        int upgrades = DrawerBlockEntity.upgradeCount(saved);
        if (upgrades > 0) {
            lines.add(Component.translatable("tooltip.immaterialstorage_sd.upgrades", upgrades));
        }
        lines.forEach(line -> tooltip.add(line.copy().withStyle(ChatFormatting.GRAY)));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!DrawerUpgrades.isAccepted(stack) || !(level.getBlockEntity(pos) instanceof DrawerBlockEntity drawer)) {
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
        if (!player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof DrawerBlockEntity drawer)) {
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
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof DrawerBlockEntity drawer
                && !drawer.isEmpty()) {
            for (ItemStack drop : drops) {
                if (drop.is(asItem())) {
                    BlockItem.setBlockEntityData(drop, drawer.getType(),
                            drawer.saveCustomOnly(params.getLevel().registryAccess()));
                }
            }
        }
        return drops;
    }
}
