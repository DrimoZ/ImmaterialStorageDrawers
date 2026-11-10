package dev.drimoz.immaterialstorage.drawer;

import com.jaquadro.minecraft.storagedrawers.api.framing.FrameMaterial;
import com.jaquadro.minecraft.storagedrawers.api.framing.IFramedBlockEntity;
import com.jaquadro.minecraft.storagedrawers.api.framing.IFramedMaterials;
import com.jaquadro.minecraft.storagedrawers.api.security.ISecurityProvider;
import com.jaquadro.minecraft.storagedrawers.api.storage.attribute.IProtectable;
import dev.drimoz.immaterialstorage.StorageDrawersContract;
import dev.drimoz.immaterialstorage.network.NetworkNodeBlockEntity;
import dev.drimoz.immaterialstorage.upgrade.DrawerUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * What every drawer of ours shares, whatever it stores: Storage Drawers' upgrades, its frame (from
 * Storage Drawers' framing table), what its keys set ({@link DrawerAttributes}, and the owner of the
 * Personal Key), saving, and the client sync.
 *
 * <p>Subclasses own the content (energy, fluid) and say how upgrades size it. Not this class's job:
 * any unit, any capability.
 */
public abstract class DrawerBlockEntity extends NetworkNodeBlockEntity implements IFramedBlockEntity, IProtectable {

    private static final String UPGRADES_TAG = "Upgrades";
    private static final String FRAME_TAG = "Frame";
    private static final String ATTRIBUTES_TAG = "Attributes";
    private static final String OWNER_TAG = "Owner";
    private static final String SECURITY_TAG = "Security";

    /** The frame's look, for the block model; absent when the drawer is not framed. */
    public static final ModelProperty<FrameLook> FRAME_LOOK = new ModelProperty<>();

    /**
     * The blocks whose textures a framed drawer wears, the trim falling back to the side as
     * in Storage Drawers. Blocks, not stacks, so two looks compare by identity.
     */
    public record FrameLook(Block side, Block trim) {
    }

    /**
     * Ticks between two client syncs of the content. A pipe can change it every tick; one packet per
     * drawer per tick for a number on a face is not worth it.
     */
    private static final int SYNC_INTERVAL = 10;

    private final DrawerUpgrades upgrades = new DrawerUpgrades();
    private final IFramedMaterials frame = StorageDrawersContract.newFrame();
    private final DrawerAttributes attributes = new DrawerAttributes(supportsLock(), this::attributesChanged);
    private boolean wasLocked;
    @Nullable
    private UUID owner;
    @Nullable
    private ISecurityProvider securityProvider;
    private boolean syncPending;

    protected DrawerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The frame, in Storage Drawers' own holder; empty when the drawer is not framed. */
    @Override
    public IFramedMaterials material() {
        return frame;
    }

    @Nullable
    public FrameLook frameLook() {
        return lookOf(frame);
    }

    /** The look of any frame, a placed drawer's or one read from an item; null when unframed. */
    @Nullable
    public static FrameLook lookOf(IFramedMaterials frame) {
        Block side = blockOf(frame.getMaterial(FrameMaterial.SIDE));
        if (side == null) {
            return null;
        }
        Block trim = blockOf(frame.getMaterial(FrameMaterial.TRIM));
        return new FrameLook(side, trim != null ? trim : side);
    }

    @Nullable
    private static Block blockOf(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item ? item.getBlock() : null;
    }

    @Override
    public ModelData getModelData() {
        FrameLook look = frameLook();
        return look == null ? ModelData.EMPTY : ModelData.builder().with(FRAME_LOOK, look).build();
    }

    /**
     * The block entity data a dropped drawer keeps, without the frame: the frame travels in Storage
     * Drawers' own item component instead, so that their framing table can take it off.
     */
    public CompoundTag saveForDrop(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        tag.remove(FRAME_TAG);
        return tag;
    }

    /** What Storage Drawers' keys change; also what the drawer answers to their attributes capability. */
    public DrawerAttributes attributes() {
        return attributes;
    }

    /** Whether the Drawer Key locks this drawer: drawers with compartments, whose contents can be pinned. */
    protected boolean supportsLock() {
        return false;
    }

    /** The lock was set or lifted: compartments pin what they hold, or forget it. */
    protected void onLockChanged(boolean locked) {
    }

    private void attributesChanged() {
        if (attributes.isLocked() != wasLocked) {
            wasLocked = attributes.isLocked();
            onLockChanged(wasLocked);
        }
        setChanged();
        syncNow();
    }

    // Storage Drawers' Personal Key: IProtectable, checked through its own SecurityManager.

    @Override
    @Nullable
    public UUID getOwner() {
        return owner;
    }

    @Override
    public boolean setOwner(@Nullable UUID owner) {
        this.owner = owner;
        setChanged();
        syncNow();
        return true;
    }

    @Override
    @Nullable
    public ISecurityProvider getSecurityProvider() {
        return securityProvider;
    }

    @Override
    public boolean setSecurityProvider(@Nullable ISecurityProvider provider) {
        this.securityProvider = provider;
        setChanged();
        return true;
    }

    /** How the front is divided; one compartment unless a drawer says otherwise. */
    public Compartments compartments() {
        return Compartments.ONE;
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
        return isContentEmpty() && upgrades.installed().isEmpty() && attributes.isDefault() && owner == null;
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
        tag.put(ATTRIBUTES_TAG, attributes.save());
        if (owner != null) {
            tag.putUUID(OWNER_TAG, owner);
            if (securityProvider != null) {
                tag.putString(SECURITY_TAG, securityProvider.getProviderID());
            }
        }
        if (StorageDrawersContract.isFramed(frame)) {
            tag.put(FRAME_TAG, StorageDrawersContract.saveFrame(frame, registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadContent(tag, registries);
        upgrades.load(registries, tag.getList(UPGRADES_TAG, Tag.TAG_COMPOUND));
        attributes.load(tag.getCompound(ATTRIBUTES_TAG));
        wasLocked = attributes.isLocked();
        owner = tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
        securityProvider = StorageDrawersContract.securityProvider(tag.getString(SECURITY_TAG));
        // Recomputed rather than trusted: the multipliers live in Storage Drawers' config, which can
        // change between sessions.
        applyUpgrades();

        FrameLook before = frameLook();
        StorageDrawersContract.loadFrame(frame, registries, tag.getCompound(FRAME_TAG));
        if (level != null && level.isClientSide && !Objects.equals(before, frameLook())) {
            // Only on a new look: content syncs arrive every 10 ticks and must not re-mesh the chunk.
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }
}
