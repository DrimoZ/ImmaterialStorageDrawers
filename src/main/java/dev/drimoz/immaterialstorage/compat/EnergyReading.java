package dev.drimoz.immaterialstorage.compat;

import com.jaquadro.minecraft.storagedrawers.api.storage.IControlGroup;
import dev.drimoz.immaterialstorage.energy.ControllerEnergyStorage;
import dev.drimoz.immaterialstorage.energy.EnergyDrawerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The energy a probe mod should show for a block, in full: the one place Jade and The One Probe
 * both read from, so they never disagree.
 *
 * <p>Long, not the int of the energy capability, which stops at 2.1 billion FE: one fully upgraded
 * drawer already passes it.
 *
 * @param stored   FE stored
 * @param capacity FE that fits
 */
public record EnergyReading(long stored, long capacity) {

    /**
     * For an Energy Drawer, or a controller or Controller Slave with Energy Drawers on its network;
     * null for anything else, including a controller with none (an empty energy bar on every
     * Storage Drawers controller would be noise).
     */
    @Nullable
    public static EnergyReading of(BlockEntity blockEntity) {
        if (blockEntity instanceof EnergyDrawerBlockEntity drawer) {
            return new EnergyReading(drawer.energy().getStoredLong(), drawer.energy().getCapacityLong());
        }
        if (blockEntity instanceof IControlGroup group) {
            ControllerEnergyStorage network = ControllerEnergyStorage.of(group);
            long capacity = network.getCapacityLong();
            return capacity > 0 ? new EnergyReading(network.getStoredLong(), capacity) : null;
        }
        return null;
    }
}
