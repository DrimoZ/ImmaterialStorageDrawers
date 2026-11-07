package dev.drimoz.immaterialstorage.energy;

import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.network.NetworkNodeBlockEntity;
import dev.drimoz.immaterialstorage.storage.BigEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An Energy Drawer: a network node that holds Forge Energy.
 *
 * <p>Not yet: upgrades (capacity is fixed), client sync and rendering.
 */
public class EnergyDrawerBlockEntity extends NetworkNodeBlockEntity {

    /** Fixed until Storage Drawers' storage upgrades are wired in. Immaterial Drawers' base size. */
    public static final long BASE_CAPACITY = 500_000;

    private static final String ENERGY_TAG = "Energy";

    private final BigEnergyStorage energy = new BigEnergyStorage(BASE_CAPACITY) {
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(ENERGY_TAG, energy.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(ENERGY_TAG)) {
            energy.deserializeNBT(registries, tag.getCompound(ENERGY_TAG));
        }
    }
}
