package dev.drimoz.immaterialstorage.fluid;

import dev.drimoz.immaterialstorage.ISConfig;
import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.drawer.Compartments;
import dev.drimoz.immaterialstorage.drawer.DrawerBlockEntity;
import dev.drimoz.immaterialstorage.storage.DrawerFluidTank;
import dev.drimoz.immaterialstorage.storage.DrawerFluidTanks;
import dev.drimoz.immaterialstorage.upgrade.DrawerUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * A Fluid Drawer: one fluid per compartment (one, two or four, from its block), sized by Storage
 * Drawers' upgrades.
 *
 * <p>Capacity of each compartment = the configured base ({@link ISConfig}, 32,000 mB by default, 32
 * buckets as a full Storage Drawers drawer holds 32 stacks) x the storage multiplier, divided by the
 * number of compartments, as Storage Drawers divides a drawer's 32 stacks into 16 or 8 per drawer.
 * The fluid API is int, so a compartment stops at 2.1 billion mB.
 */
public class FluidDrawerBlockEntity extends DrawerBlockEntity {

    private static final String TANKS_TAG = "Tanks";
    private static final String FLUID_TAG = "Fluid";
    private static final String CAPACITY_TAG = "Capacity";
    private static final String FILTER_TAG = "Filter";

    private final Compartments compartments;
    private final List<DrawerFluidTank> tanks;
    /** Also what the drawer answers to {@code FluidHandler.BLOCK}, on every side. */
    private final DrawerFluidTanks fluids;

    public FluidDrawerBlockEntity(BlockPos pos, BlockState state) {
        super(ImmaterialStorage.FLUID_DRAWER_BLOCK_ENTITY.get(), pos, state);
        this.compartments = state.getBlock() instanceof FluidDrawerBlock block ? block.compartments() : Compartments.ONE;
        this.tanks = IntStream.range(0, compartments.count()).mapToObj(i -> newTank()).toList();
        this.fluids = new DrawerFluidTanks(tanks);
    }

    private DrawerFluidTank newTank() {
        return new DrawerFluidTank(capacityFor(upgrades(), compartments)) {
            @Override
            public boolean isDrawerVoid() {
                return upgrades().hasVoid();
            }

            @Override
            public boolean isDrawerVending() {
                return upgrades().hasCreativeVending();
            }

            @Override
            public boolean isDrawerLocked() {
                return attributes().isLocked();
            }

            @Override
            protected void onContentsChanged() {
                contentChanged();
            }
        };
    }

    @Override
    protected boolean supportsLock() {
        return true;
    }

    @Override
    protected void onLockChanged(boolean locked) {
        tanks.forEach(tank -> tank.lock(locked));
    }

    @Override
    public Compartments compartments() {
        return compartments;
    }

    /** One per compartment, in Storage Drawers' order: left to right, top to bottom. */
    public List<DrawerFluidTank> tanks() {
        return tanks;
    }

    public DrawerFluidTanks fluids() {
        return fluids;
    }

    static int capacityFor(DrawerUpgrades upgrades, Compartments compartments) {
        if (upgrades.hasCreativeStorage()) {
            return Integer.MAX_VALUE;
        }
        long total = (long) ISConfig.fluidBaseCapacity() * upgrades.storageMultiplier();
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1, total / compartments.count()));
    }

    @Override
    protected void applyUpgrades() {
        int capacity = capacityFor(upgrades(), compartments);
        for (DrawerFluidTank tank : tanks) {
            // Never below what it holds: a pack lowering the base size must not delete fluid on load.
            tank.resize(Math.max(capacity, tank.getFluidAmount()));
        }
    }

    @Override
    protected boolean contentFits(DrawerUpgrades upgrades) {
        int capacity = capacityFor(upgrades, compartments);
        return tanks.stream().allMatch(tank -> tank.getFluidAmount() <= capacity);
    }

    @Override
    public boolean isContentEmpty() {
        return tanks.stream().allMatch(DrawerFluidTank::isEmpty);
    }

    @Override
    protected float fillRatio(int compartment) {
        return ratio(tanks.get(compartment).getFluidAmount(), tanks.get(compartment).getCapacity());
    }

    @Override
    protected void saveContent(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (DrawerFluidTank tank : tanks) {
            CompoundTag saved = new CompoundTag();
            if (!tank.isEmpty()) {
                saved.put(FLUID_TAG, tank.getFluid().save(registries));
            }
            saved.putInt(CAPACITY_TAG, tank.getCapacity());
            if (!tank.getFilter().isEmpty()) {
                saved.put(FILTER_TAG, tank.getFilter().save(registries));
            }
            list.add(saved);
        }
        tag.put(TANKS_TAG, list);
    }

    @Override
    protected void loadContent(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = tag.getList(TANKS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < tanks.size(); i++) {
            tanks.get(i).setFluid(FluidStack.parseOptional(registries, list.getCompound(i).getCompound(FLUID_TAG)));
            tanks.get(i).setFilter(FluidStack.parseOptional(registries, list.getCompound(i).getCompound(FILTER_TAG)));
        }
    }

    /** The tooltip lines for a dropped drawer's saved data: one per compartment holding a fluid. */
    public static List<Component> describeStored(CompoundTag saved, @Nullable HolderLookup.Provider registries) {
        List<Component> lines = new ArrayList<>();
        if (registries == null) {
            return lines;
        }
        for (Tag entry : saved.getList(TANKS_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag tank = (CompoundTag) entry;
            FluidStack fluid = FluidStack.parseOptional(registries, tank.getCompound(FLUID_TAG));
            if (!fluid.isEmpty()) {
                lines.add(describe(fluid, tank.getInt(CAPACITY_TAG)));
            }
        }
        return lines;
    }

    /** "Fluid: stored / capacity mB", exact: the item tooltip and the upgrade screen both say it this way. */
    public static Component describe(FluidStack fluid, int capacity) {
        Component name = fluid.isEmpty() ? Component.translatable("tooltip.immaterialstorage_sd.empty") : fluid.getHoverName();
        return Component.translatable("tooltip.immaterialstorage_sd.fluid", name,
                String.format("%,d", fluid.getAmount()), String.format("%,d", capacity));
    }
}
