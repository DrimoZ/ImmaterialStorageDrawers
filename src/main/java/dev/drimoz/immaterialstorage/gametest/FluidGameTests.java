package dev.drimoz.immaterialstorage.gametest;

import static dev.drimoz.immaterialstorage.gametest.TestSupport.*;

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

    private static final int BASE = FluidDrawerBlockEntity.BASE_CAPACITY;

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
        helper.assertValueEqual(fluids.fill(water(BASE), FluidAction.EXECUTE), BASE - 1_000, "water accepted when filling up");
        helper.assertValueEqual(fluids.drain(BASE * 2, FluidAction.EXECUTE).getAmount(), BASE, "water drained when emptying");
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
        helper.assertValueEqual(drawer.tank().getCapacity(), BASE * ironMult, "capacity with one iron upgrade");

        drawer.installUpgrade(sd("void_upgrade"));
        drawer.tank().fill(water(BASE * ironMult), FluidAction.EXECUTE);
        helper.assertValueEqual(drawer.tank().fill(water(1_000), FluidAction.EXECUTE), 1_000, "water reported accepted by a full void drawer");
        helper.assertValueEqual(drawer.tank().fill(lava(1_000), FluidAction.EXECUTE), 0, "lava accepted by a void water drawer");
        helper.assertValueEqual(drawer.tank().getFluidAmount(), BASE * ironMult, "mB in a full void drawer");
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
        helper.assertTrue(reloaded.tank().getFluid().is(Fluids.LAVA), "the dropped drawer lost its fluid");
        helper.assertValueEqual(reloaded.tank().getFluidAmount(), 7_000, "mB in the dropped drawer");
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

}
