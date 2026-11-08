package dev.drimoz.immaterialstorage.gametest;

import com.jaquadro.minecraft.storagedrawers.api.storage.IControlGroup;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityController;
import dev.drimoz.immaterialstorage.energy.EnergyDrawerBlockEntity;
import dev.drimoz.immaterialstorage.fluid.FluidDrawerBlockEntity;
import dev.drimoz.immaterialstorage.network.NetworkNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.Set;

/**
 * What every game test shares: the platform, where things go on it, and the ways of placing and
 * reading them. Also the names of Storage Drawers blocks that only the tests place, which have no
 * business in the mod itself.
 *
 * <p>Capabilities are always asked for through the level, as a cable or a pipe would.
 */
final class TestSupport {

    /** An 11 x 5 x 11 platform, copied from Immaterial Drawers; its surface is at y = 1. */
    static final String PLATFORM = "drawer_wall";
    static final BlockPos CONTROLLER = new BlockPos(5, 1, 5);
    static final BlockPos DRAWER = new BlockPos(1, 1, 1);
    /** Enough for a placed controller's own first rebuild, scheduled one tick after it appears. */
    static final int SETTLE_TICKS = 5;

    static final ResourceLocation CONTROLLER_BLOCK = ResourceLocation.fromNamespaceAndPath("storagedrawers", "controller");
    static final ResourceLocation CONTROLLER_IO_BLOCK = ResourceLocation.fromNamespaceAndPath("storagedrawers", "controller_io");

    private TestSupport() {
    }

    static BlockEntityController placeController(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, BuiltInRegistries.BLOCK.get(CONTROLLER_BLOCK));
        if (!(helper.getBlockEntity(CONTROLLER) instanceof BlockEntityController controller)) {
            throw new GameTestAssertException("no Storage Drawers controller at " + CONTROLLER
                    + " - is " + CONTROLLER_BLOCK + " still its name?");
        }
        return controller;
    }

    /** The block entity at a position, checked to be of the given type. */
    static <T> T at(GameTestHelper helper, BlockPos pos, Class<T> type) {
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        if (!type.isInstance(blockEntity)) {
            throw new GameTestAssertException("no " + type.getSimpleName() + " at " + pos);
        }
        return type.cast(blockEntity);
    }

    static EnergyDrawerBlockEntity energyDrawerAt(GameTestHelper helper, BlockPos pos) {
        return at(helper, pos, EnergyDrawerBlockEntity.class);
    }

    static FluidDrawerBlockEntity fluidDrawerAt(GameTestHelper helper, BlockPos pos) {
        return at(helper, pos, FluidDrawerBlockEntity.class);
    }

    static NetworkNodeBlockEntity nodeAt(GameTestHelper helper, BlockPos pos) {
        return at(helper, pos, NetworkNodeBlockEntity.class);
    }

    static IEnergyStorage energyAt(GameTestHelper helper, BlockPos pos) {
        IEnergyStorage energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), null);
        if (energy == null) {
            throw new GameTestAssertException("no energy capability at " + pos);
        }
        return energy;
    }

    static IFluidHandler fluidsAt(GameTestHelper helper, BlockPos pos) {
        IFluidHandler fluids = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), null);
        if (fluids == null) {
            throw new GameTestAssertException("no fluid capability at " + pos);
        }
        return fluids;
    }

    /** The controller that currently counts this node, or null. */
    static IControlGroup controllerOf(NetworkNodeBlockEntity node) {
        Set<IControlGroup> bound = node.node().getSoftBoundControlGroups();
        return bound.isEmpty() ? null : bound.iterator().next();
    }

    /** One item of Storage Drawers, by registry name. */
    static ItemStack sd(String name) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("storagedrawers", name));
        if (item == Items.AIR) {
            throw new GameTestAssertException("Storage Drawers has no item " + name);
        }
        return new ItemStack(item);
    }

    /** What a right-click on the middle of the front does: the item path first, the empty-hand path when it passes. */
    static void use(GameTestHelper helper, Player player, BlockPos pos) {
        useAt(helper, player, pos, 0.5, 0.5);
    }

    /**
     * A right-click on the front of a north-facing drawer, at a point measured from the block's
     * corner: {@code x} along east (the viewer's left, seen from the north), {@code y} up.
     */
    static void useAt(GameTestHelper helper, Player player, BlockPos pos, double x, double y) {
        BlockPos abs = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atLowerCornerOf(abs).add(x, y, 0), Direction.NORTH, abs, false);
        BlockState state = helper.getLevel().getBlockState(abs);
        ItemInteractionResult result = state.useItemOn(player.getMainHandItem(), helper.getLevel(), player,
                InteractionHand.MAIN_HAND, hit);
        if (result == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
            state.useWithoutItem(helper.getLevel(), player, hit);
        }
    }
}
