package dev.drimoz.immaterialstorage.fluid;

import com.jaquadro.minecraft.storagedrawers.api.storage.IControlGroup;
import dev.drimoz.immaterialstorage.network.ControllerNodes;
import net.neoforged.neoforge.fluids.FluidStack;
import dev.drimoz.immaterialstorage.storage.DrawerFluidTanks;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A Storage Drawers controller's fluids: one tank per compartment of every Fluid Drawer it links.
 * Also what its Controller Slaves expose.
 *
 * <p>The same routing as a single drawer ({@link DrawerFluidTanks}), over the whole network: tanks
 * already holding that fluid first, then empty ones.
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

    private DrawerFluidTanks network() {
        return new DrawerFluidTanks(ControllerNodes.ofNetwork(controller, FluidDrawerBlockEntity.class).stream()
                .flatMap(drawer -> drawer.tanks().stream()).toList());
    }

    /** Where filling goes: every drawer but those Storage Drawers' Suspend Key holds back. */
    private DrawerFluidTanks fillable() {
        return new DrawerFluidTanks(ControllerNodes.ofNetwork(controller, FluidDrawerBlockEntity.class).stream()
                .filter(drawer -> !drawer.attributes().isSuspended())
                .flatMap(drawer -> drawer.tanks().stream()).toList());
    }

    @Override
    public int getTanks() {
        return network().getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return network().getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return network().getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return network().isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return fillable().fill(resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return network().drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return network().drain(maxDrain, action);
    }
}
