package dev.drimoz.immaterialstorage.drawer;

import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.upgrade.DrawerUpgrades;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The upgrade screen of any drawer of ours: 7 upgrade slots and the player's inventory, laid out
 * exactly as Storage Drawers lays out its own drawer screen, whose texture the screen draws.
 *
 * <p>Both sides wrap the drawer block entity, the client one being kept in sync by the drawer
 * itself; vanilla's slot sync corrects any click the server refused.
 */
public class DrawerMenu extends AbstractContainerMenu {

    // Storage Drawers' ContainerDrawers positions, so their texture lines up.
    private static final int UPGRADE_X = 26;
    private static final int UPGRADE_Y = 86;
    private static final int INVENTORY_X = 8;
    private static final int INVENTORY_Y = 117;
    private static final int HOTBAR_Y = 175;

    private final UpgradeContainer upgrades;

    public DrawerMenu(int id, Inventory inventory, DrawerBlockEntity drawer) {
        super(ImmaterialStorage.DRAWER_MENU.get(), id);
        this.upgrades = new UpgradeContainer(drawer);

        for (int i = 0; i < DrawerUpgrades.SLOTS; i++) {
            addSlot(new UpgradeSlot(upgrades, i, UPGRADE_X + i * 18, UPGRADE_Y));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, INVENTORY_X + column * 18, HOTBAR_Y));
        }
    }

    /** Client side: the drawer is found from the position the server sent. */
    public static DrawerMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        var pos = buffer.readBlockPos();
        if (!(inventory.player.level().getBlockEntity(pos) instanceof DrawerBlockEntity drawer)) {
            throw new IllegalStateException("no drawer at " + pos + " for its upgrade screen");
        }
        return new DrawerMenu(id, inventory, drawer);
    }

    public DrawerBlockEntity drawer() {
        return upgrades.drawer();
    }

    @Override
    public boolean stillValid(Player player) {
        return upgrades.stillValid(player);
    }

    /** Shift-click: an upgrade goes to the player, anything from the player goes in if it fits. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < DrawerUpgrades.SLOTS) {
            if (!slot.mayPickup(player) || !moveItemStackTo(stack.copy(), DrawerUpgrades.SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.remove(1);
            return original;
        }
        int target = drawer().upgrades().slotFor(stack);
        if (target < 0) {
            return ItemStack.EMPTY;
        }
        drawer().setUpgrade(target, stack);
        stack.shrink(1);
        slot.setChanged();
        return ItemStack.EMPTY;
    }

    private static final class UpgradeSlot extends Slot {

        private final UpgradeContainer upgrades;

        UpgradeSlot(UpgradeContainer upgrades, int index, int x, int y) {
            super(upgrades, index, x, y);
            this.upgrades = upgrades;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return upgrades.drawer().canPlaceUpgrade(getContainerSlot(), stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return upgrades.drawer().canRemoveUpgrade(getContainerSlot());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
