package dev.drimoz.immaterialstorage.upgrade;

import dev.drimoz.immaterialstorage.StorageDrawersContract;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The upgrade slots of one of our drawers, holding Storage Drawers' own upgrade items.
 *
 * <p>Same rules as their drawers: 7 slots, storage upgrades stack their multipliers additively,
 * every other kind fits once. Only the kinds that mean something for a non-item drawer are
 * accepted: storage, void, creative storage, creative vending.
 *
 * <p>Not this class's job: what a multiplier means in FE or mB (the owning block entity decides),
 * or whether removing an upgrade would spill contents (it asks, see {@link #withoutLast()}).
 */
public final class DrawerUpgrades {

    public static final int SLOTS = 7;

    private final NonNullList<ItemStack> slots = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public static boolean isAccepted(ItemStack stack) {
        return StorageDrawersContract.storageMultiplier(stack) > 0
                || StorageDrawersContract.isVoidUpgrade(stack)
                || StorageDrawersContract.isCreativeStorageUpgrade(stack)
                || StorageDrawersContract.isCreativeVendingUpgrade(stack);
    }

    public boolean canAdd(ItemStack stack) {
        if (!isAccepted(stack) || freeSlot() < 0) {
            return false;
        }
        if (StorageDrawersContract.storageMultiplier(stack) > 0) {
            return true;
        }
        return slots.stream().noneMatch(installed -> installed.is(stack.getItem()));
    }

    /** Installs one item of the stack (the caller shrinks it). False if {@link #canAdd} says no. */
    public boolean add(ItemStack stack) {
        if (!canAdd(stack)) {
            return false;
        }
        slots.set(freeSlot(), stack.copyWithCount(1));
        return true;
    }

    /** The upgrade {@link #removeLast()} would take out, or empty. */
    public ItemStack last() {
        for (int i = SLOTS - 1; i >= 0; i--) {
            if (!slots.get(i).isEmpty()) {
                return slots.get(i);
            }
        }
        return ItemStack.EMPTY;
    }

    public ItemStack removeLast() {
        for (int i = SLOTS - 1; i >= 0; i--) {
            if (!slots.get(i).isEmpty()) {
                ItemStack removed = slots.get(i);
                slots.set(i, ItemStack.EMPTY);
                return removed;
            }
        }
        return ItemStack.EMPTY;
    }

    /** A copy without the last upgrade: lets the owner check a removal before doing it. */
    public DrawerUpgrades withoutLast() {
        DrawerUpgrades copy = new DrawerUpgrades();
        for (int i = 0; i < SLOTS; i++) {
            copy.slots.set(i, slots.get(i));
        }
        copy.removeLast();
        return copy;
    }

    /** Sum of the storage upgrades' multipliers, or Storage Drawers' base multiplier when none. */
    public int storageMultiplier() {
        int total = 0;
        for (ItemStack stack : slots) {
            total += StorageDrawersContract.storageMultiplier(stack);
        }
        return total > 0 ? total : StorageDrawersContract.baseMultiplier();
    }

    public boolean hasVoid() {
        return slots.stream().anyMatch(StorageDrawersContract::isVoidUpgrade);
    }

    public boolean hasCreativeStorage() {
        return slots.stream().anyMatch(StorageDrawersContract::isCreativeStorageUpgrade);
    }

    public boolean hasCreativeVending() {
        return slots.stream().anyMatch(StorageDrawersContract::isCreativeVendingUpgrade);
    }

    /** Installed upgrades, for dropping them when the block breaks. */
    public List<ItemStack> installed() {
        return slots.stream().filter(stack -> !stack.isEmpty()).toList();
    }

    private int freeSlot() {
        for (int i = 0; i < SLOTS; i++) {
            if (slots.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public ListTag save(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (int i = 0; i < SLOTS; i++) {
            if (!slots.get(i).isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                list.add(slots.get(i).save(registries, entry));
            }
        }
        return list;
    }

    public void load(HolderLookup.Provider registries, ListTag list) {
        slots.replaceAll(stack -> ItemStack.EMPTY);
        for (Tag tag : list) {
            CompoundTag entry = (CompoundTag) tag;
            int slot = entry.getByte("Slot");
            if (slot >= 0 && slot < SLOTS) {
                slots.set(slot, ItemStack.parseOptional(registries, entry));
            }
        }
    }
}
