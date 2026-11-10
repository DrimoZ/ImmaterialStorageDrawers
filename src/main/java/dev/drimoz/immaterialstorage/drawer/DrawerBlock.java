package dev.drimoz.immaterialstorage.drawer;

import com.jaquadro.minecraft.storagedrawers.api.framing.FrameMaterial;
import com.jaquadro.minecraft.storagedrawers.api.framing.IFramedBlock;
import com.jaquadro.minecraft.storagedrawers.api.framing.IFramedBlockEntity;
import com.jaquadro.minecraft.storagedrawers.api.framing.IFramedSourceBlock;
import dev.drimoz.immaterialstorage.StorageDrawersContract;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelReader;
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
import net.minecraft.world.SimpleMenuProvider;
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
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What every drawer block of ours shares: facing, upgrades by hand, drops that keep the contents,
 * the item tooltip, and the sync ticker. Subclasses add what their content needs.
 *
 * <p>Right-click with a Storage Drawers upgrade installs it. Sneak + right-click with an empty hand
 * opens the upgrade screen, as on a Storage Drawers drawer.
 *
 * <p>Broken, a drawer keeps its contents and upgrades in the dropped item (vanilla's block entity
 * data component, applied back on placement), so breaking one never destroys anything. An empty
 * drawer drops a plain, stackable item.
 *
 * <p>Storage Drawers' framing table frames it like one of its own drawers, side and trim (no front: the window stays dark). The
 * frame is the same block in another look, not a separate framed block: it lives in Storage Drawers'
 * own frame component on the item, so their table also takes it off, and contents and upgrades
 * stay through both.
 */
public abstract class DrawerBlock extends NetworkNodeBlock implements IFramedSourceBlock, IFramedBlock {

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

    // Storage Drawers' redstone upgrades, wired as on their drawers (BlockDrawers): an analog signal
    // for comparators by default, or, with their "analogOutput" off, a signal the drawer emits
    // around it and strongly into the block above.

    @Override
    protected boolean isSignalSource(BlockState state) {
        return !StorageDrawersContract.analogRedstone();
    }

    @Override
    protected int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction side) {
        return isSignalSource(state) ? redstoneLevel(level, pos) : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction side) {
        return side == Direction.UP ? getSignal(state, level, pos, side) : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return redstoneLevel(level, pos);
    }

    private static int redstoneLevel(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof DrawerBlockEntity drawer ? drawer.redstoneLevel() : 0;
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
        ItemInteractionResult guarded = guard(stack, level, pos, player);
        if (guarded != null) {
            return guarded;
        }
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
        if (level.getBlockEntity(pos) instanceof DrawerBlockEntity protectedDrawer
                && !StorageDrawersContract.hasAccess(player, protectedDrawer)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof DrawerBlockEntity drawer)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, opener) -> new DrawerMenu(id, inventory, drawer), getName()), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Storage Drawers' Personal Key and its access rule, as on its own drawers: a drawer someone else
     * owns refuses every click (the admin "unlock" key frees it), and a Personal Key claims an
     * unowned drawer or frees one's own. Returns the click's result, or null when the click goes on.
     * Subclasses call it first.
     */
    @Nullable
    protected ItemInteractionResult guard(ItemStack stack, Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof DrawerBlockEntity drawer)) {
            return null;
        }
        java.util.Optional<String> key = StorageDrawersContract.personalKey(stack);
        boolean unlock = key.filter("unlock"::equals).isPresent();
        if (!StorageDrawersContract.hasAccess(player, drawer)) {
            if (unlock && !level.isClientSide) {
                drawer.setOwner(null);
                drawer.setSecurityProvider(null);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (key.isEmpty()) {
            return null;
        }
        if (!level.isClientSide) {
            if (unlock || (drawer.getOwner() != null && StorageDrawersContract.hasOwnership(player, drawer))) {
                drawer.setOwner(null);
                drawer.setSecurityProvider(null);
            } else if (drawer.getOwner() == null) {
                drawer.setOwner(player.getUUID());
                drawer.setSecurityProvider(StorageDrawersContract.securityProvider(key.get()));
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public ItemStack makeFramedItem(ItemStack source, ItemStack side, ItemStack trim, ItemStack front) {
        return StorageDrawersContract.makeFramedItem(this, source, side, trim, front);
    }

    @Override
    public IFramedBlockEntity getFramedBlockEntity(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof DrawerBlockEntity drawer ? drawer : null;
    }

    @Override
    public boolean supportsFrameMaterial(FrameMaterial material) {
        // No front: the window stays dark whatever the frame, so a front material would change nothing.
        return material != FrameMaterial.FRONT;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof DrawerBlockEntity drawer) {
            StorageDrawersContract.readFrame(drawer.material(), stack);
            drawer.setChanged();
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);
        if (level.getBlockEntity(pos) instanceof DrawerBlockEntity drawer) {
            StorageDrawersContract.writeFrame(drawer.material(), stack);
        }
        return stack;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof DrawerBlockEntity drawer) {
            for (ItemStack drop : drops) {
                if (drop.is(asItem())) {
                    if (!drawer.isEmpty()) {
                        BlockItem.setBlockEntityData(drop, drawer.getType(),
                                drawer.saveForDrop(params.getLevel().registryAccess()));
                    }
                    StorageDrawersContract.writeFrame(drawer.material(), drop);
                }
            }
        }
        return drops;
    }
}
