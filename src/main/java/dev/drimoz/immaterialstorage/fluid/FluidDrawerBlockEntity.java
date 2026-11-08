package dev.drimoz.immaterialstorage.fluid;

import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.drawer.DrawerBlockEntity;
import dev.drimoz.immaterialstorage.storage.DrawerFluidTank;
import dev.drimoz.immaterialstorage.upgrade.DrawerUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * A Fluid Drawer: a drawer that holds one fluid, sized by Storage Drawers' upgrades.
 *
 * <p>Capacity = {@link #BASE_CAPACITY} mB x the storage multiplier, like the Energy Drawer. The fluid
 * API is int, so it stops at 2.1 billion mB; the default curve tops out at 458,752,000 mB.
 */
public class FluidDrawerBlockEntity extends DrawerBlockEntity {

    /** mB held with no storage upgrade: 32 buckets, as a full Storage Drawers drawer holds 32 stacks. */
    public static final int BASE_CAPACITY = 32_000;

    private static final String FLUID_TAG = "Fluid";
    private static final String CAPACITY_TAG = "Capacity";

    private final DrawerFluidTank tank = new DrawerFluidTank(BASE_CAPACITY) {
        @Override
        public boolean isDrawerVoid() {
            return upgrades().hasVoid();
        }

        @Override
        public boolean isDrawerVending() {
            return upgrades().hasCreativeVending();
        }

        @Override
        protected void onContentsChanged() {
            contentChanged();
        }
    };

    public FluidDrawerBlockEntity(BlockPos pos, BlockState state) {
        super(ImmaterialStorage.FLUID_DRAWER_BLOCK_ENTITY.get(), pos, state);
    }

    /** Also what the drawer answers to {@code FluidHandler.BLOCK}, on every side. */
    public DrawerFluidTank tank() {
        return tank;
    }

    static int capacityFor(DrawerUpgrades upgrades) {
        if (upgrades.hasCreativeStorage()) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.min(Integer.MAX_VALUE, (long) BASE_CAPACITY * upgrades.storageMultiplier());
    }

    @Override
    protected void applyUpgrades() {
        tank.resize(capacityFor(upgrades()));
    }

    @Override
    protected boolean contentFits(DrawerUpgrades upgrades) {
        return capacityFor(upgrades) >= tank.getFluidAmount();
    }

    @Override
    public boolean isContentEmpty() {
        return tank.isEmpty();
    }

    @Override
    protected void saveContent(CompoundTag tag, HolderLookup.Provider registries) {
        if (!tank.isEmpty()) {
            tag.put(FLUID_TAG, tank.getFluid().save(registries));
        }
        tag.putInt(CAPACITY_TAG, tank.getCapacity());
    }

    @Override
    protected void loadContent(CompoundTag tag, HolderLookup.Provider registries) {
        tank.setFluid(FluidStack.parseOptional(registries, tag.getCompound(FLUID_TAG)));
    }

    /** The tooltip line for a dropped drawer's saved data, or null when it holds no fluid. */
    @Nullable
    public static Component describeStored(CompoundTag saved, @Nullable HolderLookup.Provider registries) {
        if (registries == null || !saved.contains(FLUID_TAG)) {
            return null;
        }
        FluidStack fluid = FluidStack.parseOptional(registries, saved.getCompound(FLUID_TAG));
        if (fluid.isEmpty()) {
            return null;
        }
        return describe(fluid, saved.getInt(CAPACITY_TAG));
    }

    /** "Fluid: stored / capacity mB", exact: the item tooltip and the upgrade screen both say it this way. */
    public static Component describe(FluidStack fluid, int capacity) {
        Component name = fluid.isEmpty() ? Component.translatable("tooltip.immaterialstorage_sd.empty") : fluid.getHoverName();
        return Component.translatable("tooltip.immaterialstorage_sd.fluid", name,
                String.format("%,d", fluid.getAmount()), String.format("%,d", capacity));
    }
}
