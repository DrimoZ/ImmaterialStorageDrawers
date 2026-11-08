package dev.drimoz.immaterialstorage.energy;

import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.drawer.DrawerBlockEntity;
import dev.drimoz.immaterialstorage.storage.BigEnergyStorage;
import dev.drimoz.immaterialstorage.upgrade.DrawerUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * An Energy Drawer: a drawer that holds Forge Energy, sized by Storage Drawers' upgrades.
 *
 * <p>Capacity = {@link #BASE_CAPACITY} x the drawer's storage multiplier, the same arithmetic
 * Storage Drawers applies to items: with its default config one netherite upgrade gives 2048 x,
 * seven give 14,336 x (about 7.2 billion FE, past the int API, hence the long storage).
 */
public class EnergyDrawerBlockEntity extends DrawerBlockEntity {

    /** FE held with no storage upgrade. Immaterial Drawers' base size. */
    public static final long BASE_CAPACITY = 500_000;

    private static final String ENERGY_TAG = "Energy";

    private final BigEnergyStorage energy = new BigEnergyStorage(BASE_CAPACITY) {
        @Override
        public boolean isDrawerVoid() {
            return upgrades().hasVoid();
        }

        @Override
        public boolean isDrawerCreative() {
            return upgrades().hasCreativeVending();
        }

        @Override
        public void onChange() {
            contentChanged();
        }
    };

    public EnergyDrawerBlockEntity(BlockPos pos, BlockState state) {
        super(ImmaterialStorage.ENERGY_DRAWER_BLOCK_ENTITY.get(), pos, state);
    }

    /** Also what the drawer answers to {@code EnergyStorage.BLOCK}, on every side. */
    public BigEnergyStorage energy() {
        return energy;
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
    protected void applyUpgrades() {
        energy.setCapacity(capacityFor(upgrades()));
    }

    @Override
    protected boolean contentFits(DrawerUpgrades upgrades) {
        return capacityFor(upgrades) >= energy.getStoredRaw();
    }

    @Override
    public boolean isContentEmpty() {
        return energy.getStoredRaw() == 0;
    }

    @Override
    protected void saveContent(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(ENERGY_TAG, energy.serializeNBT(registries));
    }

    @Override
    protected void loadContent(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains(ENERGY_TAG)) {
            energy.deserializeNBT(registries, tag.getCompound(ENERGY_TAG));
        }
    }

    /** The tooltip line for a dropped drawer's saved data, or null when it holds no energy. */
    @Nullable
    public static Component describeStored(CompoundTag saved) {
        CompoundTag energy = saved.getCompound(ENERGY_TAG);
        long stored = energy.getLong("Energy");
        if (stored <= 0) {
            return null;
        }
        return describe(stored, energy.getLong("Capacity"));
    }

    /** "stored / capacity FE", exact: the item tooltip and the upgrade screen both say it this way. */
    public static Component describe(long stored, long capacity) {
        return Component.translatable("tooltip.immaterialstorage_sd.energy",
                String.format("%,d", stored), String.format("%,d", capacity));
    }
}
