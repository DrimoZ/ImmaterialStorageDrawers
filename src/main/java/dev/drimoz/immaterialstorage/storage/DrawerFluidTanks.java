package dev.drimoz.immaterialstorage.storage;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;

/**
 * Several drawer tanks seen as one fluid handler: a drawer's compartments, or every compartment of a
 * controller's network.
 *
 * <p>Filling tries the tanks already holding that fluid first, then empty ones, as Storage Drawers
 * does with items: a second tank of water is never started while one has room. Draining takes from
 * every tank holding the fluid.
 */
public final class DrawerFluidTanks implements IFluidHandler {

    private final List<DrawerFluidTank> tanks;

    public DrawerFluidTanks(List<DrawerFluidTank> tanks) {
        this.tanks = tanks;
    }

    @Override
    public int getTanks() {
        return tanks.size();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank >= 0 && tank < tanks.size() ? tanks.get(tank).getFluid() : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank >= 0 && tank < tanks.size() ? tanks.get(tank).getCapacity() : 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank >= 0 && tank < tanks.size() && tanks.get(tank).isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }
        int filled = 0;
        for (boolean wantEmpty : new boolean[]{false, true}) {
            for (DrawerFluidTank tank : tanks) {
                if (filled >= resource.getAmount()) {
                    return filled;
                }
                boolean candidate = wantEmpty
                        ? tank.isEmpty()
                        : FluidStack.isSameFluidSameComponents(tank.getFluid(), resource);
                if (candidate) {
                    filled += tank.fill(resource.copyWithAmount(resource.getAmount() - filled), action);
                }
            }
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        int drained = 0;
        for (DrawerFluidTank tank : tanks) {
            if (drained >= resource.getAmount()) {
                break;
            }
            if (FluidStack.isSameFluidSameComponents(tank.getFluid(), resource)) {
                drained += tank.drain(resource.getAmount() - drained, action).getAmount();
            }
        }
        return drained == 0 ? FluidStack.EMPTY : resource.copyWithAmount(drained);
    }

    /** Drains whatever fluid the first non-empty tank holds, from every tank holding it. */
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        for (DrawerFluidTank tank : tanks) {
            if (!tank.isEmpty()) {
                return drain(tank.getFluid().copyWithAmount(maxDrain), action);
            }
        }
        return FluidStack.EMPTY;
    }
}
