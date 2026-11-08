package dev.drimoz.immaterialstorage.gametest;

import static dev.drimoz.immaterialstorage.gametest.TestSupport.*;

import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityController;
import com.jaquadro.minecraft.storagedrawers.capabilities.Capabilities;
import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.StorageDrawersContract;
import dev.drimoz.immaterialstorage.network.NetworkNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * The blocking spike (SPIKE.md), as tests: does the Storage Drawers controller link a
 * block of ours, with no mixin?
 *
 * <p>These tests may name Storage Drawers' internal classes (the controller, {@code Capabilities}):
 * a test that breaks on a Storage Drawers release is the point. The mod itself may not.
 *
 * <p>The controller rebuilds its network on a scheduled tick every 100 ticks. The tests call
 * {@code updateCache()} directly instead of waiting for it.
 *
 * <p>Run headlessly with {@code ./gradlew runGameTestServer}.
 */
@GameTestHolder(ImmaterialStorage.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpikeGameTests {

    private static final int WALL_NODES = 50;

    @GameTest(template = PLATFORM)
    public static void ourCapabilityIsTheOneStorageDrawersQueries(GameTestHelper helper) {
        BlockPos node = CONTROLLER.east();
        helper.setBlock(node, ImmaterialStorage.ENERGY_DRAWER.get());

        // Asked through Storage Drawers' own capability object, answered by our registration.
        IDrawerGroup group = Capabilities.DRAWER_GROUP.getCapability(helper.getLevel(), helper.absolutePos(node));
        helper.assertTrue(group == nodeAt(helper, node).node(),
                "Storage Drawers' DRAWER_GROUP did not return our node: the capability by name is not the same one");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aNodeNextToAControllerIsLinked(GameTestHelper helper) {
        BlockEntityController controller = placeController(helper);
        BlockPos node = CONTROLLER.east();
        helper.setBlock(node, ImmaterialStorage.ENERGY_DRAWER.get());

        controller.updateCache();

        helper.assertTrue(controllerOf(nodeAt(helper, node)) == controller, "the node was not soft-bound by the controller");
        helper.assertValueEqual(controller.getDrawerCount(), 0, "item slots the controller counts for a node");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aNodeOnlyReachableThroughAnotherNodeIsLinked(GameTestHelper helper) {
        BlockEntityController controller = placeController(helper);
        helper.setBlock(CONTROLLER.east(), ImmaterialStorage.ENERGY_DRAWER.get());
        BlockPos far = CONTROLLER.east(2);
        helper.setBlock(far, ImmaterialStorage.ENERGY_DRAWER.get());

        controller.updateCache();

        helper.assertTrue(controllerOf(nodeAt(helper, far)) == controller,
                "the controller's search did not walk through our node");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void breakingTheControllerUnlinksTheNode(GameTestHelper helper) {
        BlockEntityController controller = placeController(helper);
        BlockPos node = CONTROLLER.east();
        helper.setBlock(node, ImmaterialStorage.ENERGY_DRAWER.get());
        controller.updateCache();
        helper.assertTrue(controllerOf(nodeAt(helper, node)) == controller, "precondition: node linked");

        helper.setBlock(CONTROLLER, Blocks.AIR);

        helper.assertTrue(controllerOf(nodeAt(helper, node)) == null,
                "the node still answers a controller that no longer exists");
        helper.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void aWallOfNodesIsLinkedAndDoesNotRebuildTheControllerEveryTick(GameTestHelper helper) {
        BlockEntityController controller = placeController(helper);

        List<BlockPos> placed = new ArrayList<>();
        for (int y = 1; y <= 3 && placed.size() < WALL_NODES; y++) {
            for (int x = 0; x < 11 && placed.size() < WALL_NODES; x++) {
                for (int z = 0; z < 11 && placed.size() < WALL_NODES; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!pos.equals(CONTROLLER)) {
                        helper.setBlock(pos, ImmaterialStorage.ENERGY_DRAWER.get());
                        placed.add(pos);
                    }
                }
            }
        }

        // No updateCache() here: a freshly placed controller schedules its own rebuild one tick later
        // (BlockEntityController.onEntityLoad), then every 100 ticks. Waiting for it tests the path
        // the game actually takes. Written once settled, read after idling; an array, not a field,
        // because game tests run concurrently.
        long[] rebuiltAt = new long[1];

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    rebuiltAt[0] = lastUpdateTime(controller);
                    for (BlockPos pos : placed) {
                        helper.assertTrue(controllerOf(nodeAt(helper, pos)) == controller, "node not linked at " + pos);
                    }
                    helper.assertValueEqual(controller.getDrawerCount(), 0,
                            "item slots counted for " + WALL_NODES + " nodes");
                })
                // 60 ticks: shorter than the controller's own 100-tick schedule, so any rebuild in
                // this window is one we caused.
                .thenIdle(60)
                .thenExecute(() -> {
                    helper.assertValueEqual(lastUpdateTime(controller), rebuiltAt[0], "controller rebuild time after idling");
                    for (BlockPos pos : placed) {
                        helper.assertTrue(controllerOf(nodeAt(helper, pos)) == controller,
                                "node unlinked after idling at " + pos);
                    }
                })
                .thenSucceed();
    }

    /** Private in Storage Drawers; read by reflection, in tests only. */
    private static long lastUpdateTime(BlockEntityController controller) {
        try {
            Field field = BlockEntityController.class.getDeclaredField("lastUpdateTime");
            field.setAccessible(true);
            return field.getLong(controller);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("BlockEntityController.lastUpdateTime is gone", e);
        }
    }
}
