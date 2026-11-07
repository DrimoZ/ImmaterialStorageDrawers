package dev.drimoz.immaterialstorage.gametest;

import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.StorageDrawersContract;
import dev.drimoz.immaterialstorage.energy.ControllerEnergyStorage;
import dev.drimoz.immaterialstorage.energy.EnergyDrawerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Energy Drawer alone, and the controller summing a network of them.
 *
 * <p>Every capability is asked for through the level, as a cable would, never by reaching into
 * the block entity: what is tested is what other mods see.
 */
@GameTestHolder(ImmaterialStorage.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EnergyGameTests {

    private static final String PLATFORM = "drawer_wall";
    private static final BlockPos CONTROLLER = new BlockPos(5, 1, 5);
    /** The controller's own first rebuild runs one tick after it is placed. */
    private static final int SETTLE_TICKS = 5;
    private static final long BASE = EnergyDrawerBlockEntity.BASE_CAPACITY;

    @GameTest(template = PLATFORM)
    public static void aDrawerStoresAndReturnsEnergy(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ImmaterialStorage.ENERGY_DRAWER.get());
        IEnergyStorage energy = energyAt(helper, pos);

        helper.assertValueEqual(energy.receiveEnergy(1_000, false), 1_000, "FE accepted");
        helper.assertValueEqual(energy.getEnergyStored(), 1_000, "FE stored");
        helper.assertValueEqual(energy.receiveEnergy(Integer.MAX_VALUE, false), (int) (BASE - 1_000), "FE accepted when filling up");
        helper.assertValueEqual(energy.extractEnergy(Integer.MAX_VALUE, false), (int) BASE, "FE extracted when emptying");
        helper.assertValueEqual(energy.getEnergyStored(), 0, "FE left");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aDrawerKeepsItsEnergyThroughSaveAndLoad(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ImmaterialStorage.ENERGY_DRAWER.get());
        energyAt(helper, pos).receiveEnergy(12_345, false);

        var registries = helper.getLevel().registryAccess();
        CompoundTag saved = drawerAt(helper, pos).saveWithoutMetadata(registries);
        helper.setBlock(pos, Blocks.AIR);
        helper.setBlock(pos, ImmaterialStorage.ENERGY_DRAWER.get());
        drawerAt(helper, pos).loadWithComponents(saved, registries);

        helper.assertValueEqual(energyAt(helper, pos).getEnergyStored(), 12_345, "FE after reload");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void clientsAreSentTheStoredEnergy(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ImmaterialStorage.ENERGY_DRAWER.get());
        energyAt(helper, pos).receiveEnergy(4_242, false);

        // What a client receives, loaded into a block entity that never saw the server one.
        var registries = helper.getLevel().registryAccess();
        CompoundTag update = drawerAt(helper, pos).getUpdateTag(registries);
        EnergyDrawerBlockEntity client = new EnergyDrawerBlockEntity(drawerAt(helper, pos).getBlockPos(), drawerAt(helper, pos).getBlockState());
        client.loadWithComponents(update, registries);
        helper.assertValueEqual(client.energy().getStoredLong(), 4_242L, "FE a client would see");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theControllerExposesTheSumOfItsDrawers(GameTestHelper helper) {
        placeController(helper);
        BlockPos[] drawers = {CONTROLLER.east(), CONTROLLER.east(2), CONTROLLER.east(3)};
        for (BlockPos pos : drawers) {
            helper.setBlock(pos, ImmaterialStorage.ENERGY_DRAWER.get());
        }

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    for (BlockPos pos : drawers) {
                        energyAt(helper, pos).receiveEnergy(1_000, false);
                    }
                    IEnergyStorage controller = energyAt(helper, CONTROLLER);
                    helper.assertValueEqual(controller.getEnergyStored(), 3_000, "FE the controller reports");
                    helper.assertValueEqual(controller.getMaxEnergyStored(), (int) (3 * BASE), "capacity the controller reports");

                    helper.assertValueEqual(controller.receiveEnergy(500, false), 500, "FE accepted through the controller");
                    helper.assertValueEqual(controller.extractEnergy(Integer.MAX_VALUE, false), 3_500, "FE extracted through the controller");
                    for (BlockPos pos : drawers) {
                        helper.assertValueEqual(energyAt(helper, pos).getEnergyStored(), 0, "FE left in drawer " + pos);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = PLATFORM)
    public static void aBrokenDrawerLeavesTheTotalAtOnce(GameTestHelper helper) {
        placeController(helper);
        BlockPos kept = CONTROLLER.east();
        BlockPos broken = CONTROLLER.west();
        helper.setBlock(kept, ImmaterialStorage.ENERGY_DRAWER.get());
        helper.setBlock(broken, ImmaterialStorage.ENERGY_DRAWER.get());

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    energyAt(helper, kept).receiveEnergy(100, false);
                    energyAt(helper, broken).receiveEnergy(900, false);
                    helper.assertValueEqual(energyAt(helper, CONTROLLER).getEnergyStored(), 1_000, "FE before breaking");

                    // No rebuild in between: the total must not wait up to 100 ticks for one.
                    helper.setBlock(broken, Blocks.AIR);
                    helper.assertValueEqual(energyAt(helper, CONTROLLER).getEnergyStored(), 100, "FE right after breaking");
                })
                .thenSucceed();
    }

    @GameTest(template = PLATFORM)
    public static void theControllerSumsInLongAndClampsOnlyAtTheEnd(GameTestHelper helper) {
        placeController(helper);
        BlockPos[] drawers = {CONTROLLER.east(), CONTROLLER.west()};
        long each = 2_000_000_000L;
        for (BlockPos pos : drawers) {
            helper.setBlock(pos, ImmaterialStorage.ENERGY_DRAWER.get());
        }

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    for (BlockPos pos : drawers) {
                        // Past the int API: only reachable through the long storage itself.
                        CompoundTag tag = new CompoundTag();
                        tag.putLong("Energy", each);
                        tag.putLong("Capacity", 3_000_000_000L);
                        drawerAt(helper, pos).energy().deserializeNBT(helper.getLevel().registryAccess(), tag);
                    }
                    var controller = (ControllerEnergyStorage) energyAt(helper, CONTROLLER);
                    helper.assertValueEqual(controller.getStoredLong(), 2 * each, "FE summed in long");
                    helper.assertValueEqual(controller.getEnergyStored(), Integer.MAX_VALUE, "FE clamped for the int API");
                })
                .thenSucceed();
    }

    private static void placeController(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, BuiltInRegistries.BLOCK.get(StorageDrawersContract.CONTROLLER_BLOCK));
    }

    private static IEnergyStorage energyAt(GameTestHelper helper, BlockPos pos) {
        IEnergyStorage energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), null);
        if (energy == null) {
            throw new GameTestAssertException("no energy capability at " + pos);
        }
        return energy;
    }

    private static EnergyDrawerBlockEntity drawerAt(GameTestHelper helper, BlockPos pos) {
        if (!(helper.getBlockEntity(pos) instanceof EnergyDrawerBlockEntity drawer)) {
            throw new GameTestAssertException("no energy drawer at " + pos);
        }
        return drawer;
    }
}
