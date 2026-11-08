package dev.drimoz.immaterialstorage.gametest;

import static dev.drimoz.immaterialstorage.gametest.TestSupport.*;

import com.hollingsworth.arsnouveau.api.source.ISourceCap;
import dev.drimoz.immaterialstorage.ISConfig;
import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.StorageDrawersContract;
import dev.drimoz.immaterialstorage.source.SourceCapabilities;
import dev.drimoz.immaterialstorage.source.SourceContent;
import dev.drimoz.immaterialstorage.source.SourceDrawerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Source Drawer, with Ars Nouveau installed: its own capability, sizes and upgrades, and the
 * controller. <b>Ars Nouveau only.</b> Registered by {@code SourceContent}, not found by annotation
 * scanning, so a run without Ars never loads this class.
 */
@PrefixGameTestTemplate(false)
public final class SourceGameTests {

    private static final String NS = ImmaterialStorage.MOD_ID;

    private static ISourceCap sourceAt(GameTestHelper helper, BlockPos pos) {
        ISourceCap source = helper.getLevel().getCapability(SourceCapabilities.BLOCK, helper.absolutePos(pos), null);
        if (source == null) {
            throw new GameTestAssertException("no Source capability at " + pos);
        }
        return source;
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void weSpeakArsOwnCapability(GameTestHelper helper) {
        helper.assertTrue(SourceCapabilities.BLOCK == com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.SOURCE_CAPABILITY,
                "our Source BlockCapability is not Ars Nouveau's instance");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void aDrawerHoldsSourceAndUpgradesMultiplyIt(GameTestHelper helper) {
        helper.setBlock(DRAWER, SourceContent.SOURCE_DRAWER.get());
        ISourceCap source = sourceAt(helper, DRAWER);
        int base = ISConfig.sourceBaseCapacity();
        helper.assertValueEqual(source.getSourceCapacity(), base, "Source capacity without upgrades");
        helper.assertValueEqual(source.receiveSource(1_000, false), 1_000, "Source accepted");
        helper.assertValueEqual(source.extractSource(400, false), 400, "Source extracted");

        SourceDrawerBlockEntity drawer = at(helper, DRAWER, SourceDrawerBlockEntity.class);
        int ironMult = StorageDrawersContract.storageMultiplier(sd("iron_storage_upgrade"));
        drawer.installUpgrade(sd("iron_storage_upgrade"));
        helper.assertValueEqual(source.getSourceCapacity(), base * ironMult, "Source capacity with one iron upgrade");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = PLATFORM)
    public static void theControllerSumsItsSourceDrawers(GameTestHelper helper) {
        placeController(helper);
        helper.setBlock(CONTROLLER.east(), SourceContent.SOURCE_DRAWER.get());
        helper.setBlock(CONTROLLER.west(), SourceContent.SOURCE_DRAWER.get());
        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    ISourceCap controller = sourceAt(helper, CONTROLLER);
                    helper.assertValueEqual(controller.getSourceCapacity(), 2 * ISConfig.sourceBaseCapacity(),
                            "Source capacity of a controller linking two drawers");
                    int over = ISConfig.sourceBaseCapacity() + 500;
                    helper.assertValueEqual(controller.receiveSource(over, false), over, "Source spilling into the second drawer");
                })
                .thenSucceed();
    }
}
