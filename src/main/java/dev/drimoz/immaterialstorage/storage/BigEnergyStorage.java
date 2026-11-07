package dev.drimoz.immaterialstorage.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The energy one drawer holds.
 *
 * <p>Taken from Immaterial Drawers, itself adapted from Functional Storage's
 * {@code fluid/BigFluidHandler}. Copyright (c) 2021 Buuz135, Rid — MIT. See NOTICE.
 *
 * <p><b>Long inside, int at the boundary.</b> {@link IEnergyStorage} is an int API, so the standard
 * capability cannot express more than {@link Integer#MAX_VALUE}. Amounts are kept in a {@code long}
 * and clamped on the way out, as Powah does. A cable or a meter sees at most 2.1B;
 * {@link #getStoredLong()} and {@link #getCapacityLong()} are the truth.
 *
 * <p><b>Void and creative</b> are hooks for Storage Drawers' own upgrades, overridden by the block
 * entity once upgrades exist:
 * <ul>
 *   <li><b>Void</b> reports every FE offered as accepted and drops what does not fit, so the
 *       machine feeding it never stalls.</li>
 *   <li><b>Creative</b> is bottomless and infinite: extraction never depletes anything.</li>
 * </ul>
 */
public class BigEnergyStorage implements IEnergyStorage, INBTSerializable<CompoundTag> {

    private long capacity;
    private long energy;

    public BigEnergyStorage(long capacity) {
        this.capacity = capacity;
    }

    public boolean isDrawerVoid() {
        return false;
    }

    public boolean isDrawerCreative() {
        return false;
    }

    /** Called whenever the stored amount changes, so the owner can save and sync. */
    public void onChange() {
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        if (toReceive <= 0) {
            return 0;
        }
        if (isDrawerCreative()) {
            return toReceive;
        }
        // max(0, ..): capacity can sit below energy for a moment if a pack lowers the curve, and
        // receiveEnergy may never return a negative.
        long accepted = Math.max(0, Math.min(capacity - energy, toReceive));
        if (!simulate && accepted > 0) {
            energy += accepted;
            onChange();
        }
        return isDrawerVoid() ? toReceive : (int) accepted;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        if (toExtract <= 0) {
            return 0;
        }
        if (isDrawerCreative()) {
            return toExtract;
        }
        long extracted = Math.min(energy, toExtract);
        if (!simulate && extracted > 0) {
            energy -= extracted;
            onChange();
        }
        return (int) extracted;
    }

    /** Clamped to int, knowingly wrong above 2.1B: see the class comment. */
    @Override
    public int getEnergyStored() {
        return clampToInt(getStoredLong());
    }

    @Override
    public int getMaxEnergyStored() {
        return clampToInt(getCapacityLong());
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    /** What is stored, in full, creative included. */
    public long getStoredLong() {
        return isDrawerCreative() ? Long.MAX_VALUE : energy;
    }

    /** What fits, in full, creative included. */
    public long getCapacityLong() {
        return isDrawerCreative() ? Long.MAX_VALUE : capacity;
    }

    /** What is really stored, ignoring creative: for "is this drawer empty?" decisions. */
    public long getStoredRaw() {
        return energy;
    }

    /**
     * Resizes the drawer. Shrinking below the contents clamps them: callers that can refuse (an
     * upgrade being pulled out) must refuse first; this only covers a config lowered between sessions.
     */
    public void setCapacity(long capacity) {
        this.capacity = capacity;
        if (energy > capacity) {
            energy = capacity;
            onChange();
        }
    }

    private static int clampToInt(long value) {
        return (int) Math.min(Integer.MAX_VALUE, value);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Energy", energy);
        tag.putLong("Capacity", capacity);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.energy = tag.getLong("Energy");
        if (tag.contains("Capacity")) {
            this.capacity = tag.getLong("Capacity");
        }
    }
}
