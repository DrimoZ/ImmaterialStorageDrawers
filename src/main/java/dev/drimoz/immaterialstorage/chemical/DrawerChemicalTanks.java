package dev.drimoz.immaterialstorage.chemical;

import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

import java.util.List;

/**
 * Several chemical compartments seen as one Mekanism handler: a drawer's, or every compartment of a
 * controller's network. <b>Mekanism only: see {@code compat.Mods}.</b>
 *
 * <p>The fluid drawers' routing ({@code storage.DrawerFluidTanks}): filling tries the compartments
 * already holding that chemical first, then empty ones; draining takes from every compartment
 * holding it. Mekanism's default would take the first compartment that accepts at all, which splits
 * one chemical across empty ones while the one holding it has room.
 */
public final class DrawerChemicalTanks implements IChemicalHandler {

    private final List<DrawerChemicalTank> tanks;

    public DrawerChemicalTanks(List<DrawerChemicalTank> tanks) {
        this.tanks = tanks;
    }

    private boolean inRange(int tank) {
        return tank >= 0 && tank < tanks.size();
    }

    @Override
    public int getChemicalTanks() {
        return tanks.size();
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank) {
        return inRange(tank) ? tanks.get(tank).getChemical() : ChemicalStack.EMPTY;
    }

    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack) {
        if (inRange(tank)) {
            tanks.get(tank).setChemical(stack);
        }
    }

    @Override
    public long getChemicalTankCapacity(int tank) {
        return inRange(tank) ? tanks.get(tank).getCapacity() : 0;
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack) {
        return inRange(tank) && tanks.get(tank).isValid(stack);
    }

    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
        return inRange(tank) ? tanks.get(tank).insert(stack, action) : stack;
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action) {
        return inRange(tank) ? tanks.get(tank).extract(amount, action) : ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalStack insertChemical(ChemicalStack stack, Action action) {
        ChemicalStack left = stack;
        for (boolean wantEmpty : new boolean[]{false, true}) {
            for (DrawerChemicalTank tank : tanks) {
                if (left.isEmpty()) {
                    return ChemicalStack.EMPTY;
                }
                boolean candidate = wantEmpty ? tank.isEmpty() : ChemicalStack.isSameChemical(tank.getChemical(), left);
                if (candidate) {
                    left = tank.insert(left, action);
                }
            }
        }
        return left;
    }

    @Override
    public ChemicalStack extractChemical(ChemicalStack stack, Action action) {
        if (stack.isEmpty()) {
            return ChemicalStack.EMPTY;
        }
        long taken = 0;
        for (DrawerChemicalTank tank : tanks) {
            if (taken >= stack.getAmount()) {
                break;
            }
            if (ChemicalStack.isSameChemical(tank.getChemical(), stack)) {
                taken += tank.extract(stack.getAmount() - taken, action).getAmount();
            }
        }
        return taken == 0 ? ChemicalStack.EMPTY : stack.copyWithAmount(taken);
    }

    /** Whatever chemical the first non-empty compartment holds, from every compartment holding it. */
    @Override
    public ChemicalStack extractChemical(long amount, Action action) {
        for (DrawerChemicalTank tank : tanks) {
            if (!tank.isEmpty()) {
                return extractChemical(tank.getChemical().copyWithAmount(amount), action);
            }
        }
        return ChemicalStack.EMPTY;
    }
}
