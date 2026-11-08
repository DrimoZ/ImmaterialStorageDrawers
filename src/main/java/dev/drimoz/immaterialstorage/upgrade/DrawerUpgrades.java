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
 * <p>Same rules as their drawers, applied by their own code (see
 * {@code StorageDrawersContract.fitsAlongside}): 7 slots, storage upgrades stack their multipliers
 * additively, any other kind fits once per group. Only the kinds that mean something for a non-item
 * drawer are accepted: storage, void, creative storage, creative vending.
 *
 * <p>Not this class's job: what a multiplier means in FE or mB (the owning block entity decides),
 * or whether removing an upgrade would spill contents (it asks, see {@link #without(int)}).
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

    public ItemStack get(int slot) {
        return slots.get(slot);
    }

    /** Whether the stack may go into this slot: an accepted kind, an empty slot, and Storage Drawers' group rule. */
    public boolean canPlace(int slot, ItemStack stack) {
        return isAccepted(stack) && slots.get(slot).isEmpty() && StorageDrawersContract.fitsAlongside(stack, slots);
    }

    /** The first slot the stack may go into, or -1. */
    public int slotFor(ItemStack stack) {
        for (int i = 0; i < SLOTS; i++) {
            if (canPlace(i, stack)) {
                return i;
            }
        }
        return -1;
    }

    /** Puts one item of the stack in the slot, or empties it. No rule checked: the owner checks. */
    public void set(int slot, ItemStack stack) {
        slots.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    /** A copy with this slot emptied: lets the owner check a removal before doing it. */
    public DrawerUpgrades without(int slot) {
        DrawerUpgrades copy = new DrawerUpgrades();
        for (int i = 0; i < SLOTS; i++) {
            copy.slots.set(i, i == slot ? ItemStack.EMPTY : slots.get(i));
        }
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
