package dev.drimoz.immaterialstorage.gametest;

import static dev.drimoz.immaterialstorage.gametest.TestSupport.*;

import dev.drimoz.immaterialstorage.ISConfig;
import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.StorageDrawersContract;
import dev.drimoz.immaterialstorage.fluid.FluidDrawerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Fluid Drawer alone, by hand with buckets, with upgrades, and summed by a controller.
 *
 * <p>Capabilities are asked for through the level, as a pipe would.
 */
@GameTestHolder(ImmaterialStorage.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FluidGameTests {

    private static int base() {
        return ISConfig.fluidBaseCapacity();
    }

    private static FluidStack water(int amount) {
        return new FluidStack(Fluids.WATER, amount);
    }

    private static FluidStack lava(int amount) {
        return new FluidStack(Fluids.LAVA, amount);
    }

    @GameTest(template = PLATFORM)
    public static void aDrawerHoldsOneFluid(GameTestHelper helper) {
        helper.setBlock(DRAWER, ImmaterialStorage.FLUID_DRAWER.get());
        IFluidHandler fluids = fluidsAt(helper, DRAWER);

        helper.assertValueEqual(fluids.fill(water(1_000), FluidAction.EXECUTE), 1_000, "water accepted");
        helper.assertValueEqual(fluids.fill(lava(1_000), FluidAction.EXECUTE), 0, "lava accepted into a water drawer");
        helper.assertValueEqual(fluids.fill(water(base()), FluidAction.EXECUTE), base() - 1_000, "water accepted when filling up");
        helper.assertValueEqual(fluids.drain(base() * 2, FluidAction.EXECUTE).getAmount(), base(), "water drained when emptying");
        helper.assertValueEqual(fluids.fill(lava(500), FluidAction.EXECUTE), 500, "lava accepted once empty");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void bucketsFillAndEmptyItByHand(GameTestHelper helper) {
        helper.setBlock(DRAWER, ImmaterialStorage.FLUID_DRAWER.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        use(helper, player, DRAWER);
        helper.assertValueEqual(fluidsAt(helper, DRAWER).getFluidInTank(0).getAmount(), 1_000, "mB after emptying a bucket in");
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "the bucket did not come back empty");

        use(helper, player, DRAWER);
        helper.assertValueEqual(fluidsAt(helper, DRAWER).getFluidInTank(0).getAmount(), 0, "mB after filling the bucket back");
        helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "the bucket did not come back full");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void storageUpgradesMultiplyAndVoidSwallows(GameTestHelper helper) {
        helper.setBlock(DRAWER, ImmaterialStorage.FLUID_DRAWER.get());
        FluidDrawerBlockEntity drawer = fluidDrawerAt(helper, DRAWER);
        ItemStack iron = sd("iron_storage_upgrade");
        int ironMult = StorageDrawersContract.storageMultiplier(iron);

        drawer.installUpgrade(iron);
        helper.assertValueEqual(drawer.tanks().get(0).getCapacity(), base() * ironMult, "capacity with one iron upgrade");

        drawer.installUpgrade(sd("void_upgrade"));
        drawer.tanks().get(0).fill(water(base() * ironMult), FluidAction.EXECUTE);
        helper.assertValueEqual(drawer.tanks().get(0).fill(water(1_000), FluidAction.EXECUTE), 1_000, "water reported accepted by a full void drawer");
        helper.assertValueEqual(drawer.tanks().get(0).fill(lava(1_000), FluidAction.EXECUTE), 0, "lava accepted by a void water drawer");
        helper.assertValueEqual(drawer.tanks().get(0).getFluidAmount(), base() * ironMult, "mB in a full void drawer");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aBrokenDrawerKeepsItsFluid(GameTestHelper helper) {
        helper.setBlock(DRAWER, ImmaterialStorage.FLUID_DRAWER.get());
        fluidsAt(helper, DRAWER).fill(lava(7_000), FluidAction.EXECUTE);

        helper.getLevel().destroyBlock(helper.absolutePos(DRAWER), true);
        ItemStack dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(DRAWER)).inflate(2))
                .stream().map(ItemEntity::getItem)
                .filter(stack -> stack.is(ImmaterialStorage.FLUID_DRAWER_ITEM.get()))
                .findFirst().orElseThrow(() -> new GameTestAssertException("breaking the drawer dropped no drawer"));
        CompoundTag saved = dropped.get(DataComponents.BLOCK_ENTITY_DATA).copyTag();

        FluidDrawerBlockEntity reloaded = new FluidDrawerBlockEntity(helper.absolutePos(DRAWER),
                ImmaterialStorage.FLUID_DRAWER.get().defaultBlockState());
        reloaded.loadCustomOnly(saved, helper.getLevel().registryAccess());
        helper.assertTrue(reloaded.tanks().get(0).getFluid().is(Fluids.LAVA), "the dropped drawer lost its fluid");
        helper.assertValueEqual(reloaded.tanks().get(0).getFluidAmount(), 7_000, "mB in the dropped drawer");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theControllerFillsMatchingDrawersFirst(GameTestHelper helper) {
        placeController(helper);
        BlockPos empty = CONTROLLER.east();
        BlockPos watered = CONTROLLER.west();
        helper.setBlock(empty, ImmaterialStorage.FLUID_DRAWER.get());
        helper.setBlock(watered, ImmaterialStorage.FLUID_DRAWER.get());

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    fluidsAt(helper, watered).fill(water(1_000), FluidAction.EXECUTE);
                    IFluidHandler controller = fluidsAt(helper, CONTROLLER);
                    helper.assertValueEqual(controller.getTanks(), 2, "tanks the controller exposes");

                    helper.assertValueEqual(controller.fill(water(2_000), FluidAction.EXECUTE), 2_000, "water accepted by the controller");
                    helper.assertValueEqual(fluidsAt(helper, watered).getFluidInTank(0).getAmount(), 3_000, "mB in the drawer already holding water");
                    helper.assertTrue(fluidsAt(helper, empty).getFluidInTank(0).isEmpty(), "water went to the empty drawer first");

                    helper.assertValueEqual(controller.fill(lava(500), FluidAction.EXECUTE), 500, "lava accepted by the controller");
                    helper.assertTrue(fluidsAt(helper, empty).getFluidInTank(0).is(Fluids.LAVA), "lava did not go to the empty drawer");

                    helper.assertValueEqual(controller.drain(water(10_000), FluidAction.EXECUTE).getAmount(), 3_000, "water drained by the controller");
                })
                .thenSucceed();
    }

    @GameTest(template = PLATFORM)
    public static void aTwoCompartmentDrawerHoldsTwoFluids(GameTestHelper helper) {
        helper.setBlock(DRAWER, ImmaterialStorage.FLUID_DRAWER_2.get());
        FluidDrawerBlockEntity drawer = fluidDrawerAt(helper, DRAWER);
        IFluidHandler fluids = fluidsAt(helper, DRAWER);
        helper.assertValueEqual(fluids.getTanks(), 2, "tanks of a two-compartment drawer");
        helper.assertValueEqual(fluids.getTankCapacity(0), base() / 2, "mB per compartment of two");

        fluids.fill(water(1_000), FluidAction.EXECUTE);
        fluids.fill(lava(1_000), FluidAction.EXECUTE);
        fluids.fill(water(500), FluidAction.EXECUTE);
        helper.assertValueEqual(drawer.tanks().get(0).getFluidAmount(), 1_500, "water, all in the first compartment");
        helper.assertTrue(drawer.tanks().get(1).getFluid().is(Fluids.LAVA), "lava did not take the empty compartment");
        helper.assertValueEqual(fluids.fill(new FluidStack(Fluids.FLOWING_WATER, 100), FluidAction.EXECUTE), 0,
                "a third fluid accepted by a full two-compartment drawer");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aBucketFillsTheCompartmentItIsUsedOn(GameTestHelper helper) {
        helper.setBlock(DRAWER, ImmaterialStorage.FLUID_DRAWER_4.get());
        FluidDrawerBlockEntity drawer = fluidDrawerAt(helper, DRAWER);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        // Bottom right, seen from the front: the viewer's left is east, so west of the middle.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        useAt(helper, player, DRAWER, 0.25, 0.25);
        helper.assertValueEqual(drawer.tanks().get(3).getFluidAmount(), 1_000, "mB in the bottom right compartment");
        // Top left.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
        useAt(helper, player, DRAWER, 0.75, 0.75);
        helper.assertTrue(drawer.tanks().get(0).getFluid().is(Fluids.LAVA), "lava is not in the top left compartment");
        helper.assertTrue(drawer.tanks().get(1).isEmpty() && drawer.tanks().get(2).isEmpty(), "a bucket reached another compartment");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theControllerSeesEveryCompartment(GameTestHelper helper) {
        placeController(helper);
        helper.setBlock(CONTROLLER.east(), ImmaterialStorage.FLUID_DRAWER_4.get());
        helper.setBlock(CONTROLLER.west(), ImmaterialStorage.FLUID_DRAWER.get());
        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> helper.assertValueEqual(fluidsAt(helper, CONTROLLER).getTanks(), 5,
                        "tanks of a controller linking a 2x2 and a 1x1 drawer"))
                .thenSucceed();
    }
}