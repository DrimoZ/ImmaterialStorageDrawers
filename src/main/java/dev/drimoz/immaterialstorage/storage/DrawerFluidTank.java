package dev.drimoz.immaterialstorage.storage;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * The fluid one drawer holds: one fluid at a time, like a 1x1 Storage Drawers drawer holds one item.
 *
 * <p>NeoForge's tank, plus the two upgrades that change its contract, hooks overridden by the owner:
 * <ul>
 *   <li><b>Void</b> reports all of a fluid it could hold as accepted and drops what does not fit,
 *       so the pump feeding it never stalls. A different fluid is still refused.</li>
 *   <li><b>Vending</b> hands out whatever is asked for without draining.</li>
 * </ul>
 */
public class DrawerFluidTank extends FluidTank {

    public DrawerFluidTank(int capacity) {
        super(capacity);
    }

    public boolean isDrawerVoid() {
        return false;
    }

    public boolean isDrawerVending() {
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        int filled = super.fill(resource, action);
        boolean holdable = !resource.isEmpty() && isFluidValid(resource)
                && (fluid.isEmpty() || FluidStack.isSameFluidSameComponents(fluid, resource));
        return isDrawerVoid() && holdable ? resource.getAmount() : filled;
    }

    /** The FluidStack variant of drain goes through this one too, after checking the fluid matches. */
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (isDrawerVending() && !fluid.isEmpty() && maxDrain > 0) {
            return fluid.copyWithAmount(maxDrain);
        }
        return super.drain(maxDrain, action);
    }

    /**
     * Resizes the tank. Shrinking below the contents clamps them: callers that can refuse (an
     * upgrade being pulled out) refuse first; this only covers a config lowered between sessions.
     */
    public void resize(int capacity) {
        setCapacity(capacity);
        if (fluid.getAmount() > capacity) {
            fluid.setAmount(capacity);
            onContentsChanged();
        }
    }
}
