package dev.drimoz.immaterialstorage.gametest;

import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.StorageDrawersContract;
import dev.drimoz.immaterialstorage.energy.EnergyDrawerBlockEntity;
import dev.drimoz.immaterialstorage.upgrade.DrawerUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Storage Drawers' own upgrade items, installed in an Energy Drawer.
 *
 * <p>Multipliers are read from Storage Drawers' config at run time, never hard-coded here, so a
 * changed default on their side does not break these tests; only a changed meaning does.
 */
@GameTestHolder(ImmaterialStorage.MOD_ID)
@PrefixGameTestTemplate(false)
public final class UpgradeGameTests {

    private static final String PLATFORM = "drawer_wall";
    private static final BlockPos DRAWER = new BlockPos(1, 1, 1);
    private static final long BASE = EnergyDrawerBlockEntity.BASE_CAPACITY;

    @GameTest(template = PLATFORM)
    public static void storageUpgradesAddTheirMultipliers(GameTestHelper helper) {
        EnergyDrawerBlockEntity drawer = placeDrawer(helper);
        ItemStack iron = sd("iron_storage_upgrade");
        ItemStack gold = sd("gold_storage_upgrade");
        long ironMult = StorageDrawersContract.storageMultiplier(iron);
        long goldMult = StorageDrawersContract.storageMultiplier(gold);
        helper.assertTrue(ironMult > 0 && goldMult > 0, "Storage Drawers' storage upgrades report no multiplier");

        helper.assertTrue(drawer.installUpgrade(iron), "iron upgrade refused");
        helper.assertValueEqual(drawer.energy().getCapacityLong(), BASE * ironMult, "capacity with one iron upgrade");
        helper.assertTrue(drawer.installUpgrade(iron), "second iron upgrade refused: storage upgrades stack");
        helper.assertTrue(drawer.installUpgrade(gold), "gold upgrade refused");
        helper.assertValueEqual(drawer.energy().getCapacityLong(), BASE * (2 * ironMult + goldMult), "capacity with iron, iron, gold");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void sevenSlotsAndNoMore(GameTestHelper helper) {
        EnergyDrawerBlockEntity drawer = placeDrawer(helper);
        ItemStack obsidian = sd("obsidian_storage_upgrade");
        for (int i = 0; i < DrawerUpgrades.SLOTS; i++) {
            helper.assertTrue(drawer.installUpgrade(obsidian), "upgrade " + (i + 1) + " refused");
        }
        helper.assertFalse(drawer.installUpgrade(obsidian), "an eighth upgrade was accepted");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void anUpgradeThatWouldSpillCannotBeRemoved(GameTestHelper helper) {
        EnergyDrawerBlockEntity drawer = placeDrawer(helper);
        drawer.installUpgrade(sd("iron_storage_upgrade"));
        drawer.energy().receiveEnergy((int) (BASE + 1), false);

        helper.assertTrue(drawer.removeLastUpgrade().isEmpty(), "the upgrade came out although the energy no longer fits");
        helper.assertValueEqual(drawer.energy().getStoredLong(), BASE + 1, "FE after the refused removal");

        drawer.energy().extractEnergy(1, false);
        helper.assertFalse(drawer.removeLastUpgrade().isEmpty(), "the upgrade stayed in although the energy fits at base size");
        helper.assertValueEqual(drawer.energy().getCapacityLong(), BASE, "capacity after removal");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theVoidUpgradeSwallowsTheOverflowAndFitsOnce(GameTestHelper helper) {
        EnergyDrawerBlockEntity drawer = placeDrawer(helper);
        ItemStack voidUpgrade = sd("void_upgrade");
        helper.assertTrue(drawer.installUpgrade(voidUpgrade), "void upgrade refused");
        helper.assertFalse(drawer.installUpgrade(voidUpgrade), "a second void upgrade was accepted");

        drawer.energy().receiveEnergy((int) BASE, false);
        helper.assertValueEqual(drawer.energy().receiveEnergy(1_000, false), 1_000, "FE reported accepted by a full void drawer");
        helper.assertValueEqual(drawer.energy().getStoredLong(), BASE, "FE stored by a full void drawer");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void creativeUpgrades(GameTestHelper helper) {
        EnergyDrawerBlockEntity drawer = placeDrawer(helper);
        drawer.installUpgrade(sd("creative_storage_upgrade"));
        helper.assertValueEqual(drawer.energy().getCapacityLong(), Long.MAX_VALUE, "capacity with creative storage");

        drawer.installUpgrade(sd("creative_vending_upgrade"));
        helper.assertValueEqual(drawer.energy().extractEnergy(1_000, false), 1_000, "FE handed out by an empty vending drawer");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void upgradesSurviveSaveAndLoad(GameTestHelper helper) {
        EnergyDrawerBlockEntity drawer = placeDrawer(helper);
        drawer.installUpgrade(sd("diamond_storage_upgrade"));
        drawer.energy().receiveEnergy(Integer.MAX_VALUE, false);
        long capacity = drawer.energy().getCapacityLong();
        long stored = drawer.energy().getStoredLong();

        var registries = helper.getLevel().registryAccess();
        CompoundTag saved = drawer.saveWithoutMetadata(registries);
        // A detached block entity, not setBlock(AIR): that would drop the upgrade on the platform.
        EnergyDrawerBlockEntity reloaded = new EnergyDrawerBlockEntity(drawer.getBlockPos(), drawer.getBlockState());
        reloaded.loadWithComponents(saved, registries);

        helper.assertValueEqual(reloaded.energy().getCapacityLong(), capacity, "capacity after reload");
        helper.assertValueEqual(reloaded.energy().getStoredLong(), stored, "FE after reload");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aPlayerInstallsAndRemovesAnUpgradeByHand(GameTestHelper helper) {
        EnergyDrawerBlockEntity drawer = placeDrawer(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, sd("iron_storage_upgrade").copyWithCount(3));

        use(helper, player);
        helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "upgrades left in hand after installing one");
        helper.assertTrue(drawer.energy().getCapacityLong() > BASE, "capacity did not grow");

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        use(helper, player);
        helper.assertValueEqual(drawer.energy().getCapacityLong(), BASE, "capacity after taking the upgrade out");
        helper.assertTrue(player.getInventory().contains(sd("iron_storage_upgrade")), "the removed upgrade is not in the inventory");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aBrokenDrawerKeepsItsEnergyAndUpgradesThroughReplacing(GameTestHelper helper) {
        EnergyDrawerBlockEntity drawer = placeDrawer(helper);
        drawer.installUpgrade(sd("gold_storage_upgrade"));
        long capacity = drawer.energy().getCapacityLong();
        int stored = (int) (BASE * 2);
        drawer.energy().receiveEnergy(stored, false);

        ItemStack dropped = breakAndPickUp(helper);
        helper.assertTrue(dropped.has(DataComponents.BLOCK_ENTITY_DATA), "the dropped drawer carries nothing");
        List<Component> tooltip = new ArrayList<>();
        ImmaterialStorage.ENERGY_DRAWER.get().appendHoverText(dropped, Item.TooltipContext.EMPTY, tooltip, TooltipFlag.NORMAL);
        helper.assertValueEqual(tooltip.size(), 2, "tooltip lines (energy, upgrades) on the dropped drawer");

        // Placed back the way a player does it, on top of a solid block two blocks away (the
        // platform surface is at y = 1, so the drawer lands in the air at y = 2).
        BlockPos floor = DRAWER.east(2);
        helper.setBlock(floor, Blocks.STONE);
        helper.setBlock(floor.above(), Blocks.AIR);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, dropped);
        BlockPos absFloor = helper.absolutePos(floor);
        var placed = dropped.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absFloor).add(0, 0.5, 0), Direction.UP, absFloor, false)));

        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(floor.above())) instanceof EnergyDrawerBlockEntity replaced)) {
            throw new GameTestAssertException("the drawer was not placed back: " + placed
                    + ", floor=" + helper.getBlockState(floor) + ", above=" + helper.getBlockState(floor.above()));
        }
        helper.assertValueEqual(replaced.energy().getCapacityLong(), capacity, "capacity after replacing");
        helper.assertValueEqual(replaced.energy().getStoredLong(), (long) stored, "FE after replacing");
        helper.assertItemEntityNotPresent(sd("gold_storage_upgrade").getItem(), DRAWER, 3.0);
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void anEmptyDrawerDropsAPlainItem(GameTestHelper helper) {
        placeDrawer(helper);
        ItemStack dropped = breakAndPickUp(helper);
        helper.assertFalse(dropped.has(DataComponents.BLOCK_ENTITY_DATA), "an empty drawer dropped with data: it would not stack");
        helper.succeed();
    }

    /** Breaks the drawer with drops and returns the drawer item it dropped. */
    private static ItemStack breakAndPickUp(GameTestHelper helper) {
        helper.getLevel().destroyBlock(helper.absolutePos(DRAWER), true);
        AABB around = new AABB(helper.absolutePos(DRAWER)).inflate(2);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).stream()
                .map(ItemEntity::getItem)
                .filter(stack -> stack.is(ImmaterialStorage.ENERGY_DRAWER_ITEM.get()))
                .findFirst()
                .orElseThrow(() -> new GameTestAssertException("breaking the drawer dropped no drawer"));
    }

    @GameTest(template = PLATFORM)
    public static void otherItemsAreNotUpgrades(GameTestHelper helper) {
        EnergyDrawerBlockEntity drawer = placeDrawer(helper);
        helper.assertFalse(drawer.installUpgrade(new ItemStack(Items.DIAMOND)), "a diamond was installed as an upgrade");
        // An item-only Storage Drawers upgrade means nothing for energy.
        helper.assertFalse(drawer.installUpgrade(sd("one_stack_upgrade")), "the one-stack downgrade was accepted");
        helper.succeed();
    }

    /** What a right-click does: the item path first, the empty-hand path when it passes. */
    private static void use(GameTestHelper helper, Player player) {
        BlockPos abs = helper.absolutePos(DRAWER);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.NORTH, abs, false);
        BlockState state = helper.getLevel().getBlockState(abs);
        ItemInteractionResult result = state.useItemOn(player.getMainHandItem(), helper.getLevel(), player,
                InteractionHand.MAIN_HAND, hit);
        if (result == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
            state.useWithoutItem(helper.getLevel(), player, hit);
        }
    }

    private static EnergyDrawerBlockEntity placeDrawer(GameTestHelper helper) {
        helper.setBlock(DRAWER, ImmaterialStorage.ENERGY_DRAWER.get());
        if (!(helper.getBlockEntity(DRAWER) instanceof EnergyDrawerBlockEntity drawer)) {
            throw new GameTestAssertException("no energy drawer at " + DRAWER);
        }
        return drawer;
    }

    private static ItemStack sd(String name) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("storagedrawers", name));
        if (item == Items.AIR) {
            throw new GameTestAssertException("Storage Drawers has no item " + name);
        }
        return new ItemStack(item);
    }
}
