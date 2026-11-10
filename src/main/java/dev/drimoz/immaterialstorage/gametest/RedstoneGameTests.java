package dev.drimoz.immaterialstorage.gametest;

import static dev.drimoz.immaterialstorage.gametest.TestSupport.*;

import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.fluid.FluidDrawerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Storage Drawers' redstone and illumination upgrades on our drawers. Redstone is read as a
 * comparator reads it: combined, max and min over the compartments, on Storage Drawers' own scale
 * (15 only when full).
 */
@GameTestHolder(ImmaterialStorage.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RedstoneGameTests {

    private static int comparator(GameTestHelper helper, BlockPos pos) {
        BlockPos abs = helper.absolutePos(pos);
        return helper.getLevel().getBlockState(abs).getAnalogOutputSignal(helper.getLevel(), abs);
    }

    private static void swapRedstone(FluidDrawerBlockEntity drawer, String upgrade) {
        for (int slot = 0; slot < 7; slot++) {
            drawer.removeUpgrade(slot);
        }
        drawer.installUpgrade(sd(upgrade));
    }

    @GameTest(template = PLATFORM)
    public static void redstoneUpgradesReadTheCompartmentsAsStorageDrawersDoes(GameTestHelper helper) {
        helper.setBlock(DRAWER, ImmaterialStorage.FLUID_DRAWER_2.get());
        FluidDrawerBlockEntity drawer = fluidDrawerAt(helper, DRAWER);

        // One compartment full, the other empty.
        int capacity = drawer.tanks().get(0).getCapacity();
        fluidsAt(helper, DRAWER).fill(new FluidStack(Fluids.WATER, capacity), FluidAction.EXECUTE);
        helper.assertValueEqual(comparator(helper, DRAWER), 0, "signal without a redstone upgrade");

        swapRedstone(drawer, "redstone_upgrade");
        helper.assertValueEqual(comparator(helper, DRAWER), 7, "combined signal, one of two compartments full");
        swapRedstone(drawer, "max_redstone_upgrade");
        helper.assertValueEqual(comparator(helper, DRAWER), 15, "max signal, one compartment full");
        swapRedstone(drawer, "min_redstone_upgrade");
        helper.assertValueEqual(comparator(helper, DRAWER), 0, "min signal, one compartment empty");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void illuminationAndRedstoneFitOnceEach(GameTestHelper helper) {
        helper.setBlock(DRAWER, ImmaterialStorage.FLUID_DRAWER.get());
        FluidDrawerBlockEntity drawer = fluidDrawerAt(helper, DRAWER);
        helper.assertTrue(drawer.installUpgrade(sd("illumination_upgrade")), "illumination upgrade refused");
        helper.assertTrue(drawer.upgrades().hasIllumination(), "illumination upgrade not seen");
        helper.assertFalse(drawer.installUpgrade(sd("illumination_upgrade")), "a second illumination upgrade fitted");
        helper.assertTrue(drawer.installUpgrade(sd("min_redstone_upgrade")), "redstone upgrade refused");
        helper.assertFalse(drawer.installUpgrade(sd("max_redstone_upgrade")), "two redstone upgrades fitted");
        helper.succeed();
    }
}
