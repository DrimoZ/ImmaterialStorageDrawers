package dev.drimoz.immaterialstorage.fluid;

import com.jaquadro.minecraft.storagedrawers.api.storage.IControlGroup;
import dev.drimoz.immaterialstorage.network.ControllerNodes;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A Storage Drawers controller's fluids: one tank per Fluid Drawer it links. Also what its
 * Controller Slaves expose.
 *
 * <p>Filling tries the drawers already holding that fluid first, then empty ones, as Storage
 * Drawers does with items: a network never starts a second drawer of water while one has room.
 *
 * <p>Always present (then with 0 tanks), for the same reason as {@code ControllerEnergyStorage}.
 */
public final class ControllerFluidHandler implements IFluidHandler {

    /** One handler per controller; weak keys, server thread only. */
    private static final Map<IControlGroup, ControllerFluidHandler> BY_CONTROLLER = new WeakHashMap<>();

    private final IControlGroup controller;

    private ControllerFluidHandler(IControlGroup controller) {
        this.controller = controller;
    }

    public static ControllerFluidHandler of(IControlGroup controller) {
        return BY_CONTROLLER.computeIfAbsent(controller, ControllerFluidHandler::new);
    }

    private List<FluidDrawerBlockEntity> drawers() {
        return ControllerNodes.ofNetwork(controller, FluidDrawerBlockEntity.class);
    }

    @Override
    public int getTanks() {
        return drawers().size();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        List<FluidDrawerBlockEntity> drawers = drawers();
        return tank >= 0 && tank < drawers.size() ? drawers.get(tank).tank().getFluid() : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        List<FluidDrawerBlockEntity> drawers = drawers();
        return tank >= 0 && tank < drawers.size() ? drawers.get(tank).tank().getCapacity() : 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }
        List<FluidDrawerBlockEntity> drawers = drawers();
        int filled = 0;
        // Two passes: drawers that already hold this fluid, then empty ones.
        for (boolean wantEmpty : new boolean[]{false, true}) {
            for (FluidDrawerBlockEntity drawer : drawers) {
                if (filled >= resource.getAmount()) {
                    return filled;
                }
                boolean candidate = wantEmpty
                        ? drawer.tank().isEmpty()
                        : FluidStack.isSameFluidSameComponents(drawer.tank().getFluid(), resource);
                if (candidate) {
                    filled += drawer.tank().fill(resource.copyWithAmount(resource.getAmount() - filled), action);
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
        for (FluidDrawerBlockEntity drawer : drawers()) {
            if (drained >= resource.getAmount()) {
                break;
            }
            if (FluidStack.isSameFluidSameComponents(drawer.tank().getFluid(), resource)) {
                drained += drawer.tank().drain(resource.getAmount() - drained, action).getAmount();
            }
        }
        return drained == 0 ? FluidStack.EMPTY : resource.copyWithAmount(drained);
    }

    /** Drains whatever fluid the first non-empty drawer holds, from every drawer holding it. */
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        for (FluidDrawerBlockEntity drawer : drawers()) {
            if (!drawer.tank().isEmpty()) {
                return drain(drawer.tank().getFluid().copyWithAmount(maxDrain), action);
            }
        }
        return FluidStack.EMPTY;
    }
}
