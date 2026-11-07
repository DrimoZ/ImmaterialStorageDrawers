package dev.drimoz.immaterialstorage.drawer;

import dev.drimoz.immaterialstorage.network.NetworkNodeBlockEntity;
import dev.drimoz.immaterialstorage.upgrade.DrawerUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What every drawer of ours shares, whatever it stores: Storage Drawers' upgrades, saving, and the
 * client sync.
 *
 * <p>Subclasses own the content (energy, fluid) and say how upgrades size it. Not this class's job:
 * any unit, any capability.
 */
public abstract class DrawerBlockEntity extends NetworkNodeBlockEntity {

    private static final String UPGRADES_TAG = "Upgrades";

    /**
     * Ticks between two client syncs of the content. A pipe can change it every tick; one packet per
     * drawer per tick for a number on a face is not worth it.
     */
    private static final int SYNC_INTERVAL = 10;

    private final DrawerUpgrades upgrades = new DrawerUpgrades();
    private boolean syncPending;

    protected DrawerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public DrawerUpgrades upgrades() {
        return upgrades;
    }

    /** Resizes the content to the current upgrades. Called after every change and after loading. */
    protected abstract void applyUpgrades();

    /** Whether the current content would still fit with these upgrades. */
    protected abstract boolean contentFits(DrawerUpgrades upgrades);

    public abstract boolean isContentEmpty();

    protected abstract void saveContent(CompoundTag tag, HolderLookup.Provider registries);

    protected abstract void loadContent(CompoundTag tag, HolderLookup.Provider registries);

    /** Subclasses call this when their content changes: saves, and schedules a client sync. */
    protected void contentChanged() {
        setChanged();
        syncPending = true;
    }

    /** Nothing worth keeping in the dropped item. */
    public boolean isEmpty() {
        return isContentEmpty() && upgrades.installed().isEmpty();
    }

    /** Installs one upgrade from the stack in the first slot that takes it (the caller shrinks it). */
    public boolean installUpgrade(ItemStack stack) {
        int slot = upgrades.slotFor(stack);
        if (slot < 0) {
            return false;
        }
        setUpgrade(slot, stack);
        return true;
    }

    public boolean canPlaceUpgrade(int slot, ItemStack stack) {
        return upgrades.canPlace(slot, stack);
    }

    /** Whether the upgrade in this slot can come out: there is one, and the content would still fit. */
    public boolean canRemoveUpgrade(int slot) {
        return !upgrades.get(slot).isEmpty() && contentFits(upgrades.without(slot));
    }

    /** Takes the upgrade out of the slot, or returns empty when {@link #canRemoveUpgrade} says no. */
    public ItemStack removeUpgrade(int slot) {
        if (!canRemoveUpgrade(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = upgrades.get(slot);
        setUpgrade(slot, ItemStack.EMPTY);
        return removed;
    }

    /**
     * Sets a slot, for the upgrade screen and for {@link #installUpgrade}. A change that would
     * destroy content is ignored rather than applied: removing an upgrade never destroys anything,
     * whatever path the request took.
     */
    public void setUpgrade(int slot, ItemStack stack) {
        boolean removing = !upgrades.get(slot).isEmpty() && !ItemStack.isSameItemSameComponents(upgrades.get(slot), stack);
        if (removing && !contentFits(upgrades.without(slot))) {
            return;
        }
        upgrades.set(slot, stack);
        applyUpgrades();
        setChanged();
        syncNow();
    }

    /** Server ticker: sends the content to clients, at most every {@link #SYNC_INTERVAL} ticks. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, DrawerBlockEntity drawer) {
        if (drawer.syncPending && level.getGameTime() % SYNC_INTERVAL == 0) {
            drawer.syncNow();
        }
    }

    private void syncNow() {
        syncPending = false;
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** What the client needs: the same data as a save. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** How many upgrades a dropped drawer's saved data holds. */
    public static int upgradeCount(CompoundTag saved) {
        return saved.getList(UPGRADES_TAG, Tag.TAG_COMPOUND).size();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveContent(tag, registries);
        tag.put(UPGRADES_TAG, upgrades.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadContent(tag, registries);
        upgrades.load(registries, tag.getList(UPGRADES_TAG, Tag.TAG_COMPOUND));
        // Recomputed rather than trusted: the multipliers live in Storage Drawers' config, which can
        // change between sessions. A lowered config clamps the contents.
        applyUpgrades();
    }
}
