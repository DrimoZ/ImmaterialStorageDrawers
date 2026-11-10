package dev.drimoz.immaterialstorage.chemical;

import com.jaquadro.minecraft.storagedrawers.api.storage.IControlGroup;
import dev.drimoz.immaterialstorage.network.ControllerNodes;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * A Storage Drawers controller's chemicals: one tank per compartment of every Chemical Drawer it
 * links, through the same routing as a single drawer ({@link DrawerChemicalTanks}). Also what its
 * Controller Slaves expose. <b>Mekanism only: see {@code compat.Mods}.</b>
 *
 * <p>Always present (then with 0 tanks), for the same reason as {@code ControllerEnergyStorage}.
 */
public final class ControllerChemicalHandler implements IChemicalHandler {

    /** One handler per controller; weak keys, server thread only. */
    private static final Map<IControlGroup, ControllerChemicalHandler> BY_CONTROLLER = new WeakHashMap<>();

    private final IControlGroup controller;

    private ControllerChemicalHandler(IControlGroup controller) {
        this.controller = controller;
    }

    public static ControllerChemicalHandler of(IControlGroup controller) {
        return BY_CONTROLLER.computeIfAbsent(controller, ControllerChemicalHandler::new);
    }

    private DrawerChemicalTanks network() {
        return new DrawerChemicalTanks(ControllerNodes.ofNetwork(controller, ChemicalDrawerBlockEntity.class).stream()
                .flatMap(drawer -> drawer.tanks().stream()).toList());
    }

    /** Where filling goes: every drawer but those Storage Drawers' Suspend Key holds back. */
    private DrawerChemicalTanks fillable() {
        return new DrawerChemicalTanks(ControllerNodes.ofNetwork(controller, ChemicalDrawerBlockEntity.class).stream()
                .filter(drawer -> !drawer.attributes().isSuspended())
                .flatMap(drawer -> drawer.tanks().stream()).toList());
    }

    @Override
    public int getChemicalTanks() {
        return network().getChemicalTanks();
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank) {
        return network().getChemicalInTank(tank);
    }

    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack) {
        network().setChemicalInTank(tank, stack);
    }

    @Override
    public long getChemicalTankCapacity(int tank) {
        return network().getChemicalTankCapacity(tank);
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack) {
        return network().isValid(tank, stack);
    }

    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
        return network().insertChemical(tank, stack, action);
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action) {
        return network().extractChemical(tank, amount, action);
    }

    @Override
    public ChemicalStack insertChemical(ChemicalStack stack, Action action) {
        return fillable().insertChemical(stack, action);
    }

    @Override
    public ChemicalStack extractChemical(ChemicalStack stack, Action action) {
        return network().extractChemical(stack, action);
    }

    @Override
    public ChemicalStack extractChemical(long amount, Action action) {
        return network().extractChemical(amount, action);
    }
}
