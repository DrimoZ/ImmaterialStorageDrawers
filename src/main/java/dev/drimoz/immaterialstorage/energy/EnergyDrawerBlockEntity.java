package dev.drimoz.immaterialstorage.energy;

import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.network.NetworkNodeBlockEntity;
import dev.drimoz.immaterialstorage.storage.BigEnergyStorage;
import dev.drimoz.immaterialstorage.upgrade.DrawerUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An Energy Drawer: a network node that holds Forge Energy, sized by Storage Drawers' upgrades.
 *
 * <p>Capacity = {@link #BASE_CAPACITY} x the drawer's storage multiplier, the same arithmetic
 * Storage Drawers applies to items: with its default config one netherite upgrade gives 2048 x,
 * seven give 14,336 x (about 7.2 billion FE, past the int API, hence the long storage).
 *
 * <p>Not yet: client sync and rendering.
 */
public class EnergyDrawerBlockEntity extends NetworkNodeBlockEntity {

    /** FE held with no storage upgrade. Immaterial Drawers' base size. */
    public static final long BASE_CAPACITY = 500_000;

    private static final String ENERGY_TAG = "Energy";
    private static final String UPGRADES_TAG = "Upgrades";

    private final DrawerUpgrades upgrades = new DrawerUpgrades();

    private final BigEnergyStorage energy = new BigEnergyStorage(BASE_CAPACITY) {
        @Override
        public boolean isDrawerVoid() {
            return upgrades.hasVoid();
        }

        @Override
        public boolean isDrawerCreative() {
            return upgrades.hasCreativeVending();
        }

        @Override
        public void onChange() {
            setChanged();
        }
    };

    public EnergyDrawerBlockEntity(BlockPos pos, BlockState state) {
        super(ImmaterialStorage.ENERGY_DRAWER_BLOCK_ENTITY.get(), pos, state);
    }

    /** Also what the drawer answers to {@code EnergyStorage.BLOCK}, on every side. */
    public BigEnergyStorage energy() {
        return energy;
    }

    public DrawerUpgrades upgrades() {
        return upgrades;
    }

    /** Nothing worth keeping in the dropped item: no energy (creative aside) and no upgrade. */
    public boolean isEmpty() {
        return energy.getStoredRaw() == 0 && upgrades.installed().isEmpty();
    }

    /** Installs one upgrade from the stack (the caller shrinks it). False if it does not fit. */
    public boolean installUpgrade(ItemStack stack) {
        if (!upgrades.add(stack)) {
            return false;
        }
        energy.setCapacity(capacityFor(upgrades));
        setChanged();
        return true;
    }

    /**
     * Takes out the last installed upgrade, or returns empty when there is none or when the energy
     * stored would no longer fit: removing an upgrade never destroys energy.
     */
    public ItemStack removeLastUpgrade() {
        if (upgrades.last().isEmpty() || capacityFor(upgrades.withoutLast()) < energy.getStoredRaw()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = upgrades.removeLast();
        energy.setCapacity(capacityFor(upgrades));
        setChanged();
        return removed;
    }

    static long capacityFor(DrawerUpgrades upgrades) {
        if (upgrades.hasCreativeStorage()) {
            return Long.MAX_VALUE;
        }
        long multiplier = upgrades.storageMultiplier();
        // Saturating: a pack can raise the multipliers far enough to overflow a long.
        return multiplier > Long.MAX_VALUE / BASE_CAPACITY ? Long.MAX_VALUE : BASE_CAPACITY * multiplier;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(ENERGY_TAG, energy.serializeNBT(registries));
        tag.put(UPGRADES_TAG, upgrades.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(ENERGY_TAG)) {
            energy.deserializeNBT(registries, tag.getCompound(ENERGY_TAG));
        }
        upgrades.load(registries, tag.getList(UPGRADES_TAG, Tag.TAG_COMPOUND));
        // Recomputed rather than trusted: the multipliers live in Storage Drawers' config, which can
        // change between sessions. A lowered config clamps the contents (BigEnergyStorage.setCapacity).
        energy.setCapacity(capacityFor(upgrades));
    }
}
