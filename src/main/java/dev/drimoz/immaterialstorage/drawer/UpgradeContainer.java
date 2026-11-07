package dev.drimoz.immaterialstorage.drawer;

import dev.drimoz.immaterialstorage.upgrade.DrawerUpgrades;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A drawer's upgrade slots seen as a vanilla {@link Container}, for the upgrade screen.
 *
 * <p>Every write goes through the drawer, so the screen cannot bypass a rule: a removal that would
 * destroy content is refused there, whatever the click was.
 */
final class UpgradeContainer implements Container {

    private final DrawerBlockEntity drawer;

    UpgradeContainer(DrawerBlockEntity drawer) {
        this.drawer = drawer;
    }

    DrawerBlockEntity drawer() {
        return drawer;
    }

    @Override
    public int getContainerSize() {
        return DrawerUpgrades.SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return drawer.upgrades().installed().isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return drawer.upgrades().get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return amount <= 0 ? ItemStack.EMPTY : drawer.removeUpgrade(slot);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return drawer.removeUpgrade(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        drawer.setUpgrade(slot, stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
        drawer.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(drawer, player);
    }

    @Override
    public void clearContent() {
        // Never: upgrades leave one by one, each checked.
    }
}
