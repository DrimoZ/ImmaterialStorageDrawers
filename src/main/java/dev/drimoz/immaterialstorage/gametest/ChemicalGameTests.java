package dev.drimoz.immaterialstorage.gametest;

import static dev.drimoz.immaterialstorage.gametest.TestSupport.*;

import dev.drimoz.immaterialstorage.ISConfig;
import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.StorageDrawersContract;
import dev.drimoz.immaterialstorage.chemical.ChemicalCapabilities;
import dev.drimoz.immaterialstorage.chemical.ChemicalContent;
import dev.drimoz.immaterialstorage.chemical.ChemicalDrawerBlockEntity;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Chemical Drawers, with Mekanism installed: its own capability, the compartments' routing and
 * sizes, and the controller.
 *
 * <p><b>Mekanism only.</b> Registered by {@code ChemicalContent} when Mekanism is there, not found by
 * annotation scanning, so a run without Mekanism never loads this class.
 */
@PrefixGameTestTemplate(false)
public final class ChemicalGameTests {

    private static final String NS = ImmaterialStorage.MOD_ID;

    private static Chemical chemical(String name) {
        return MekanismAPI.CHEMICAL_REGISTRY.get(ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, name));
    }

    private static ChemicalStack stack(String name, long amount) {
        return new ChemicalStack(chemical(name), amount);
    }

    private static IChemicalHandler chemicalsAt(GameTestHelper helper, BlockPos pos) {
        IChemicalHandler handler = helper.getLevel().getCapability(ChemicalCapabilities.BLOCK, helper.absolutePos(pos), null);
        if (handler == null) {
            throw new GameTestAssertException("no chemical capability at " + pos);
        }
        return handler;
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void weSpeakMekanismsOwnCapability(GameTestHelper helper) {
        helper.assertTrue(ChemicalCapabilities.BLOCK == mekanism.common.capabilities.Capabilities.CHEMICAL.block(),
                "our chemical BlockCapability is not Mekanism's instance");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void aDrawerHoldsOneChemicalPerCompartment(GameTestHelper helper) {
        helper.setBlock(DRAWER, ChemicalContent.CHEMICAL_DRAWER_2.get());
        IChemicalHandler chemicals = chemicalsAt(helper, DRAWER);
        helper.assertValueEqual(chemicals.getChemicalTanks(), 2, "tanks of a two-compartment drawer");
        helper.assertValueEqual(chemicals.getChemicalTankCapacity(0), ISConfig.chemicalBaseCapacity() / 2,
                "mB per compartment of two");

        chemicals.insertChemical(stack("hydrogen", 1_000), Action.EXECUTE);
        chemicals.insertChemical(stack("oxygen", 1_000), Action.EXECUTE);
        chemicals.insertChemical(stack("hydrogen", 500), Action.EXECUTE);
        helper.assertValueEqual(chemicals.getChemicalInTank(0).getAmount(), 1_500L, "hydrogen, all in the first compartment");
        helper.assertTrue(chemicals.getChemicalInTank(1).is(chemical("oxygen")), "oxygen did not take the empty compartment");
        helper.assertValueEqual(chemicals.insertChemical(stack("chlorine", 100), Action.EXECUTE).getAmount(), 100L,
                "a third chemical accepted by a full two-compartment drawer");

        ChemicalStack out = chemicals.extractChemical(stack("hydrogen", 400), Action.EXECUTE);
        helper.assertValueEqual(out.getAmount(), 400L, "hydrogen extracted");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void storageUpgradesMultiplyAndVoidSwallows(GameTestHelper helper) {
        helper.setBlock(DRAWER, ChemicalContent.CHEMICAL_DRAWER.get());
        ChemicalDrawerBlockEntity drawer = at(helper, DRAWER, ChemicalDrawerBlockEntity.class);
        int ironMult = StorageDrawersContract.storageMultiplier(sd("iron_storage_upgrade"));
        long full = ISConfig.chemicalBaseCapacity() * ironMult;

        drawer.installUpgrade(sd("iron_storage_upgrade"));
        helper.assertValueEqual(drawer.tanks().get(0).getCapacity(), full, "capacity with one iron upgrade");
        drawer.installUpgrade(sd("void_upgrade"));
        drawer.chemicals().insertChemical(stack("hydrogen", full), Action.EXECUTE);
        helper.assertTrue(drawer.chemicals().insertChemical(stack("hydrogen", 1_000), Action.EXECUTE).isEmpty(),
                "hydrogen not reported accepted by a full void drawer");
        helper.assertValueEqual(drawer.tanks().get(0).getAmount(), full, "mB in a full void drawer");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void theControllerSeesEveryChemicalCompartment(GameTestHelper helper) {
        placeController(helper);
        helper.setBlock(CONTROLLER.east(), ChemicalContent.CHEMICAL_DRAWER_4.get());
        helper.setBlock(CONTROLLER.west(), ChemicalContent.CHEMICAL_DRAWER.get());
        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    IChemicalHandler controller = chemicalsAt(helper, CONTROLLER);
                    helper.assertValueEqual(controller.getChemicalTanks(), 5, "tanks of a controller linking a 2x2 and a 1x1");
                    helper.assertTrue(controller.insertChemical(stack("oxygen", 2_000), Action.EXECUTE).isEmpty(),
                            "oxygen refused by the controller");
                })
                .thenSucceed();
    }
}
