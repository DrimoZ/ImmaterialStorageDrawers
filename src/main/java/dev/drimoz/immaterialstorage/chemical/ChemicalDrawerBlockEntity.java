package dev.drimoz.immaterialstorage.chemical;

import dev.drimoz.immaterialstorage.ISConfig;
import dev.drimoz.immaterialstorage.drawer.Compartments;
import dev.drimoz.immaterialstorage.drawer.DrawerBlockEntity;
import dev.drimoz.immaterialstorage.upgrade.DrawerUpgrades;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * A Chemical Drawer: one Mekanism chemical (gas, infuse type, pigment or slurry) per compartment,
 * sized by Storage Drawers' upgrades. <b>Mekanism only: see {@code compat.Mods}.</b>
 *
 * <p>Sized like the Fluid Drawer beside it: the configured base ({@link ISConfig}, 32,000 mB by
 * default) x the storage multiplier, divided by the number of compartments. Mekanism's API is long,
 * so a compartment has no int ceiling.
 */
public class ChemicalDrawerBlockEntity extends DrawerBlockEntity {

    private static final String TANKS_TAG = "Tanks";
    private static final String CAPACITY_TAG = "Capacity";

    private final Compartments compartments;
    private final List<DrawerChemicalTank> tanks;
    /** Also what the drawer answers to Mekanism's chemical capability, on every side. */
    private final DrawerChemicalTanks chemicals;

    public ChemicalDrawerBlockEntity(BlockPos pos, BlockState state) {
        super(ChemicalContent.BLOCK_ENTITY.get(), pos, state);
        this.compartments = state.getBlock() instanceof ChemicalDrawerBlock block ? block.compartments() : Compartments.ONE;
        this.tanks = IntStream.range(0, compartments.count()).mapToObj(i -> newTank()).toList();
        this.chemicals = new DrawerChemicalTanks(tanks);
    }

    private DrawerChemicalTank newTank() {
        return new DrawerChemicalTank(capacityFor(upgrades(), compartments)) {
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
            protected void onChange() {
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
    public List<DrawerChemicalTank> tanks() {
        return tanks;
    }

    public DrawerChemicalTanks chemicals() {
        return chemicals;
    }

    static long capacityFor(DrawerUpgrades upgrades, Compartments compartments) {
        if (upgrades.hasCreativeStorage()) {
            return Long.MAX_VALUE;
        }
        long base = ISConfig.chemicalBaseCapacity();
        long multiplier = upgrades.storageMultiplier();
        long total = multiplier > Long.MAX_VALUE / base ? Long.MAX_VALUE : base * multiplier;
        return Math.max(1, total / compartments.count());
    }

    @Override
    protected void applyUpgrades() {
        long capacity = capacityFor(upgrades(), compartments);
        for (DrawerChemicalTank tank : tanks) {
            // Never below what it holds: a pack lowering the base size must not delete chemicals on load.
            tank.resize(Math.max(capacity, tank.getAmount()));
        }
    }

    @Override
    protected boolean contentFits(DrawerUpgrades upgrades) {
        long capacity = capacityFor(upgrades, compartments);
        return tanks.stream().allMatch(tank -> tank.getAmount() <= capacity);
    }

    @Override
    public boolean isContentEmpty() {
        return tanks.stream().allMatch(DrawerChemicalTank::isEmpty);
    }

    @Override
    protected void saveContent(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (DrawerChemicalTank tank : tanks) {
            CompoundTag saved = tank.save(registries);
            saved.putLong(CAPACITY_TAG, tank.getCapacity());
            list.add(saved);
        }
        tag.put(TANKS_TAG, list);
    }

    @Override
    protected void loadContent(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = tag.getList(TANKS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < tanks.size(); i++) {
            tanks.get(i).load(registries, list.getCompound(i));
        }
    }

    /** The tooltip lines for a dropped drawer's saved data: one per compartment holding a chemical. */
    public static List<Component> describeStored(CompoundTag saved, @Nullable HolderLookup.Provider registries) {
        List<Component> lines = new ArrayList<>();
        if (registries == null) {
            return lines;
        }
        for (Tag entry : saved.getList(TANKS_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag tank = (CompoundTag) entry;
            ChemicalStack chemical = DrawerChemicalTank.read(registries, tank);
            if (!chemical.isEmpty()) {
                lines.add(describe(chemical, tank.getLong(CAPACITY_TAG)));
            }
        }
        return lines;
    }

    /** "Chemical: stored / capacity mB", exact: the item tooltip and the upgrade screen both say it this way. */
    public static Component describe(ChemicalStack chemical, long capacity) {
        Component name = chemical.isEmpty() ? Component.translatable("tooltip.immaterialstorage_sd.empty") : chemical.getTextComponent();
        return Component.translatable("tooltip.immaterialstorage_sd.chemical", name,
                String.format("%,d", chemical.getAmount()), String.format("%,d", capacity));
    }
}
