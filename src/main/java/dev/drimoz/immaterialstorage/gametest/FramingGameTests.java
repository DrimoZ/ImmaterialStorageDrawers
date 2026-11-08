package dev.drimoz.immaterialstorage.gametest;

import static dev.drimoz.immaterialstorage.gametest.TestSupport.*;

import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityFramingTable;
import com.jaquadro.minecraft.storagedrawers.components.item.FrameData;
import com.jaquadro.minecraft.storagedrawers.core.ModDataComponents;
import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.drawer.DrawerBlockEntity;
import dev.drimoz.immaterialstorage.energy.EnergyDrawerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Framing: Storage Drawers' own framing table frames our drawers and takes the frame off again, and
 * a placed framed drawer keeps its frame through saving and breaking.
 *
 * <p>The table is Storage Drawers' real block entity, driven through its inventory, so what is
 * tested is their code accepting our blocks, not a copy of it. How the frame looks is not tested
 * here: it is only drawn on the client.
 */
@GameTestHolder(ImmaterialStorage.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FramingGameTests {

    private static final ResourceLocation FRAMING_TABLE = ResourceLocation.fromNamespaceAndPath("storagedrawers", "framing_table");

    private static Container table(GameTestHelper helper) {
        BlockEntityFramingTable table = new BlockEntityFramingTable(helper.absolutePos(CONTROLLER),
                BuiltInRegistries.BLOCK.get(FRAMING_TABLE).defaultBlockState());
        table.setLevel(helper.getLevel());
        return table.inventory();
    }

    private static ItemStack framedWithOak() {
        return ImmaterialStorage.ENERGY_DRAWER.get().makeFramedItem(new ItemStack(ImmaterialStorage.ENERGY_DRAWER_ITEM.get()),
                new ItemStack(Items.OAK_PLANKS), ItemStack.EMPTY, ItemStack.EMPTY);
    }

    @GameTest(template = PLATFORM)
    public static void theFramingTableFramesAndUnframesADrawer(GameTestHelper helper) {
        Container table = table(helper);
        ItemStack drawer = new ItemStack(ImmaterialStorage.FLUID_DRAWER_ITEM.get());
        helper.assertTrue(table.canPlaceItem(BlockEntityFramingTable.SLOT_INPUT, drawer), "the framing table refuses a Fluid Drawer");

        table.setItem(BlockEntityFramingTable.SLOT_INPUT, drawer);
        table.setItem(BlockEntityFramingTable.SLOT_SIDE, new ItemStack(Items.OAK_PLANKS));
        ItemStack framed = table.getItem(BlockEntityFramingTable.SLOT_RESULT).copy();
        helper.assertTrue(framed.is(ImmaterialStorage.FLUID_DRAWER_ITEM.get()), "the table did not hand out a Fluid Drawer");
        helper.assertTrue(frameOf(framed).side().is(Items.OAK_PLANKS), "the framed drawer has no oak side");

        Container unframing = table(helper);
        unframing.setItem(BlockEntityFramingTable.SLOT_INPUT, framed);
        helper.assertTrue(unframing.getItem(BlockEntityFramingTable.SLOT_SIDE).is(Items.OAK_PLANKS), "unframing did not give the oak back");
        helper.assertFalse(unframing.getItem(BlockEntityFramingTable.SLOT_INPUT).has(ModDataComponents.FRAME_DATA.get()),
                "the unframed drawer still has a frame");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aPlacedFramedDrawerKeepsItsFrame(GameTestHelper helper) {
        BlockPos abs = helper.absolutePos(DRAWER);
        helper.setBlock(DRAWER, ImmaterialStorage.ENERGY_DRAWER.get());
        ImmaterialStorage.ENERGY_DRAWER.get().setPlacedBy(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), null, framedWithOak());
        EnergyDrawerBlockEntity drawer = energyDrawerAt(helper, DRAWER);
        helper.assertValueEqual(drawer.frameLook(),
                new DrawerBlockEntity.FrameLook(Blocks.OAK_PLANKS, Blocks.OAK_PLANKS), "frame after placing");

        CompoundTag saved = drawer.saveCustomOnly(helper.getLevel().registryAccess());
        EnergyDrawerBlockEntity reloaded = new EnergyDrawerBlockEntity(abs, drawer.getBlockState());
        reloaded.loadCustomOnly(saved, helper.getLevel().registryAccess());
        helper.assertValueEqual(reloaded.frameLook(), drawer.frameLook(), "frame after saving and loading");

        drawer.energy().receiveEnergy(1_000, false);
        helper.getLevel().destroyBlock(abs, true);
        ItemStack dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(2))
                .stream().map(ItemEntity::getItem)
                .filter(stack -> stack.is(ImmaterialStorage.ENERGY_DRAWER_ITEM.get()))
                .findFirst().orElseThrow(() -> new GameTestAssertException("breaking the drawer dropped no drawer"));
        helper.assertTrue(frameOf(dropped).side().is(Items.OAK_PLANKS), "the dropped drawer lost its frame");
        CompoundTag data = dropped.get(DataComponents.BLOCK_ENTITY_DATA).copyTag();
        helper.assertTrue(data.contains("Energy"), "the dropped drawer lost its energy");
        // Else the framing table would take the frame off the item and placing it would put it back.
        helper.assertFalse(data.contains("Frame"), "the frame is also in the dropped block entity data");
        helper.succeed();
    }

    private static FrameData frameOf(ItemStack stack) {
        FrameData frame = stack.get(ModDataComponents.FRAME_DATA.get());
        if (frame == null) {
            throw new GameTestAssertException("no frame on " + stack);
        }
        return frame;
    }
}
