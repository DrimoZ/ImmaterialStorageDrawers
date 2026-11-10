package dev.drimoz.immaterialstorage.gametest;

import static dev.drimoz.immaterialstorage.gametest.TestSupport.*;

import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.StorageDrawersContract;
import dev.drimoz.immaterialstorage.fluid.FluidDrawerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Storage Drawers' own keys on our drawers, used as a player uses them: the Drawer Key, Suspend and
 * Priority Keys through Storage Drawers' attributes capability, and the Personal Key on the block.
 */
@GameTestHolder(ImmaterialStorage.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KeyGameTests {

    /** What right-clicking the front with this Storage Drawers key does: the key's own code runs. */
    private static void useKey(GameTestHelper helper, Player player, BlockPos pos, String key) {
        BlockPos abs = helper.absolutePos(pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, sd(key));
        player.getCooldowns().removeCooldown(player.getMainHandItem().getItem());
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.NORTH, abs, false)));
    }

    @GameTest(template = PLATFORM)
    public static void theDrawerKeyPinsWhatACompartmentHolds(GameTestHelper helper) {
        helper.setBlock(DRAWER, ImmaterialStorage.FLUID_DRAWER.get());
        helper.assertTrue(helper.getLevel().getCapability(StorageDrawersContract.DRAWER_ATTRIBUTES, helper.absolutePos(DRAWER), null) != null,
                "no Storage Drawers attributes capability on a Fluid Drawer");
        IFluidHandler fluids = fluidsAt(helper, DRAWER);
        fluids.fill(new FluidStack(Fluids.WATER, 1_000), FluidAction.EXECUTE);

        useKey(helper, helper.makeMockPlayer(GameType.SURVIVAL), DRAWER, "drawer_key");
        fluids.drain(1_000, FluidAction.EXECUTE);
        helper.assertValueEqual(fluids.fill(new FluidStack(Fluids.LAVA, 1_000), FluidAction.EXECUTE), 0,
                "lava accepted by a drawer locked to water, emptied");
        helper.assertValueEqual(fluids.fill(new FluidStack(Fluids.WATER, 1_000), FluidAction.EXECUTE), 1_000,
                "water refused by a drawer locked to water");

        useKey(helper, helper.makeMockPlayer(GameType.SURVIVAL), DRAWER, "drawer_key");
        fluids.drain(1_000, FluidAction.EXECUTE);
        helper.assertValueEqual(fluids.fill(new FluidStack(Fluids.LAVA, 1_000), FluidAction.EXECUTE), 1_000,
                "lava refused once unlocked");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theControllerFollowsPriorityAndSkipsSuspendedDrawers(GameTestHelper helper) {
        placeController(helper);
        BlockPos first = CONTROLLER.east();
        BlockPos second = CONTROLLER.west();
        BlockPos suspended = CONTROLLER.north();
        helper.setBlock(first, ImmaterialStorage.FLUID_DRAWER.get());
        helper.setBlock(second, ImmaterialStorage.FLUID_DRAWER.get());
        helper.setBlock(suspended, ImmaterialStorage.FLUID_DRAWER.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        useKey(helper, player, second, "priority_key_p2");
        useKey(helper, player, suspended, "suspend_key");
        useKey(helper, player, suspended, "priority_key_p2");
        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    fluidsAt(helper, CONTROLLER).fill(new FluidStack(Fluids.WATER, 1_000), FluidAction.EXECUTE);
                    helper.assertValueEqual(fluidDrawerAt(helper, second).tanks().get(0).getFluidAmount(), 1_000,
                            "mB in the drawer with the higher priority");
                    helper.assertTrue(fluidDrawerAt(helper, first).tanks().get(0).isEmpty(), "the lower priority drawer was filled");
                    helper.assertTrue(fluidDrawerAt(helper, suspended).tanks().get(0).isEmpty(), "the suspended drawer was filled");
                })
                .thenSucceed();
    }

    @GameTest(template = PLATFORM)
    public static void thePersonalKeyKeepsOthersOut(GameTestHelper helper) {
        helper.setBlock(DRAWER, ImmaterialStorage.FLUID_DRAWER.get());
        FluidDrawerBlockEntity drawer = fluidDrawerAt(helper, DRAWER);
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);

        owner.setItemInHand(InteractionHand.MAIN_HAND, sd("personal_key"));
        use(helper, owner, DRAWER);
        helper.assertValueEqual(drawer.getOwner(), owner.getUUID(), "owner after the Personal Key");

        stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        use(helper, stranger, DRAWER);
        helper.assertTrue(drawer.tanks().get(0).isEmpty(), "someone else filled a drawer they do not own");

        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        use(helper, owner, DRAWER);
        helper.assertValueEqual(drawer.tanks().get(0).getFluidAmount(), 1_000, "mB the owner put in");
        helper.succeed();
    }
}
