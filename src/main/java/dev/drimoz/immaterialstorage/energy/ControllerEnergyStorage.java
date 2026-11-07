package dev.drimoz.immaterialstorage.energy;

import com.jaquadro.minecraft.storagedrawers.api.storage.IControlGroup;
import dev.drimoz.immaterialstorage.network.ControllerNodes;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A Storage Drawers controller's energy: the sum of the Energy Drawers it links. Also what its
 * Controller Slaves expose.
 *
 * <p>Registered on Storage Drawers' controller and Controller Slave block entity types; NeoForge
 * lets any mod do that.
 * The controller has never heard of energy; it only tells each drawer it is linked, and
 * {@link ControllerNodes} turns that into this list.
 *
 * <p>Always present, even with no Energy Drawer linked (then 0 / 0): a capability that appears and
 * disappears would need NeoForge's capability cache invalidated on every network change.
 */
public final class ControllerEnergyStorage implements IEnergyStorage {

    /**
     * One storage per controller: NeoForge's capability cache holds on to what a provider returns.
     * Weak keys so this never keeps an unloaded controller alive. Server thread only.
     */
    private static final Map<IControlGroup, ControllerEnergyStorage> BY_CONTROLLER = new WeakHashMap<>();

    private final IControlGroup controller;

    private ControllerEnergyStorage(IControlGroup controller) {
        this.controller = controller;
    }

    public static ControllerEnergyStorage of(IControlGroup controller) {
        return BY_CONTROLLER.computeIfAbsent(controller, ControllerEnergyStorage::new);
    }

    private List<EnergyDrawerBlockEntity> drawers() {
        return ControllerNodes.ofNetwork(controller, EnergyDrawerBlockEntity.class);
    }

    /** Fills drawers in the order they were linked: nearest first, as the controller searches. */
    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        // A void drawer reports more accepted than it stored: that is the void contract (never stall
        // the machine upstream), not an accounting bug.
        int accepted = 0;
        for (EnergyDrawerBlockEntity drawer : drawers()) {
            accepted += drawer.energy().receiveEnergy(toReceive - accepted, simulate);
            if (accepted >= toReceive) {
                break;
            }
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        int extracted = 0;
        for (EnergyDrawerBlockEntity drawer : drawers()) {
            extracted += drawer.energy().extractEnergy(toExtract - extracted, simulate);
            if (extracted >= toExtract) {
                break;
            }
        }
        return extracted;
    }

    /** Summed on the long values, saturating once: summing clamped ints would cap each drawer first. */
    public long getStoredLong() {
        long total = 0;
        for (EnergyDrawerBlockEntity drawer : drawers()) {
            total = saturatedAdd(total, drawer.energy().getStoredLong());
        }
        return total;
    }

    public long getCapacityLong() {
        long total = 0;
        for (EnergyDrawerBlockEntity drawer : drawers()) {
            total = saturatedAdd(total, drawer.energy().getCapacityLong());
        }
        return total;
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, getStoredLong());
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, getCapacityLong());
    }

    /** Creative drawers report Long.MAX_VALUE each; a plain sum would wrap negative. */
    private static long saturatedAdd(long running, long addition) {
        long sum = running + addition;
        return sum < running ? Long.MAX_VALUE : sum;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
