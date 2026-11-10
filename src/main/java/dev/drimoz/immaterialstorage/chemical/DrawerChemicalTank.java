package dev.drimoz.immaterialstorage.chemical;

import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * The chemical one compartment holds: one chemical at a time, like a fluid compartment holds one
 * fluid. <b>Mekanism only: see {@code compat.Mods}.</b>
 *
 * <p>The fluid tank's rules, in Mekanism's long amounts (its API is long end to end, so there is no
 * int ceiling to clamp at), with the two upgrades that change its contract, hooks overridden by the
 * owner:
 * <ul>
 *   <li><b>Void</b> reports all of a chemical it could hold as accepted and drops what does not fit,
 *       so the machine feeding it never stalls. A different chemical is still refused.</li>
 *   <li><b>Vending</b> hands out whatever is asked for without draining.</li>
 * </ul>
 */
public class DrawerChemicalTank {

    private static final String CHEMICAL_TAG = "Chemical";
    private static final String FILTER_TAG = "Filter";

    private ChemicalStack stored = ChemicalStack.EMPTY;
    private long capacity;

    public DrawerChemicalTank(long capacity) {
        this.capacity = capacity;
    }

    public boolean isDrawerVoid() {
        return false;
    }

    public boolean isDrawerVending() {
        return false;
    }

    /** Whether the drawer's Drawer Key lock is on: only the pinned chemical goes in. */
    public boolean isDrawerLocked() {
        return false;
    }

    private ChemicalStack filter = ChemicalStack.EMPTY;

    public ChemicalStack getFilter() {
        return filter;
    }

    public void setFilter(ChemicalStack filter) {
        this.filter = filter.isEmpty() ? ChemicalStack.EMPTY : filter.copyWithAmount(1);
    }

    /** The lock was set or lifted: pin what is held, or forget it. */
    public void lock(boolean locked) {
        setFilter(locked ? stored : ChemicalStack.EMPTY);
    }

    /** A player fills a locked, empty, unpinned compartment by hand: it takes that chemical as its pin. */
    public void assign(ChemicalStack stack) {
        if (isDrawerLocked() && filter.isEmpty() && stored.isEmpty()) {
            setFilter(stack);
        }
    }

    /** Called whenever what is stored changes. */
    protected void onChange() {
    }

    public ChemicalStack getChemical() {
        return stored;
    }

    public long getAmount() {
        return stored.getAmount();
    }

    public long getCapacity() {
        return capacity;
    }

    public boolean isEmpty() {
        return stored.isEmpty();
    }

    public void setChemical(ChemicalStack stack) {
        stored = stack.isEmpty() ? ChemicalStack.EMPTY : stack.copy();
        onChange();
    }

    /** Whether this compartment may take this chemical: not refused by the pack, and the one it holds. */
    public boolean isValid(ChemicalStack stack) {
        if (isDrawerLocked() && (filter.isEmpty() || !ChemicalStack.isSameChemical(filter, stack))) {
            return false;
        }
        return !stack.isEmpty() && !stack.is(ChemicalCapabilities.DENYLIST)
                && (stored.isEmpty() || ChemicalStack.isSameChemical(stored, stack));
    }

    /** Takes what fits; returns what is left. */
    public ChemicalStack insert(ChemicalStack stack, Action action) {
        if (!isValid(stack)) {
            return stack;
        }
        long accepted = Math.max(0, Math.min(capacity - stored.getAmount(), stack.getAmount()));
        if (accepted > 0 && action.execute()) {
            if (stored.isEmpty()) {
                stored = stack.copyWithAmount(accepted);
            } else {
                stored.grow(accepted);
            }
            onChange();
        }
        return isDrawerVoid() || accepted == stack.getAmount() ? ChemicalStack.EMPTY : stack.copyWithAmount(stack.getAmount() - accepted);
    }

    public ChemicalStack extract(long amount, Action action) {
        if (stored.isEmpty() || amount <= 0) {
            return ChemicalStack.EMPTY;
        }
        if (isDrawerVending()) {
            return stored.copyWithAmount(amount);
        }
        long taken = Math.min(amount, stored.getAmount());
        ChemicalStack result = stored.copyWithAmount(taken);
        if (action.execute()) {
            stored.shrink(taken);
            if (stored.isEmpty()) {
                stored = ChemicalStack.EMPTY;
            }
            onChange();
        }
        return result;
    }

    /**
     * Resizes the compartment. Shrinking below the contents clamps them: callers that can refuse (an
     * upgrade being pulled out) refuse first; this only covers a config lowered between sessions.
     */
    public void resize(long capacity) {
        this.capacity = capacity;
        if (stored.getAmount() > capacity) {
            stored.setAmount(capacity);
            onChange();
        }
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (!stored.isEmpty()) {
            tag.put(CHEMICAL_TAG, stored.save(registries));
        }
        if (!filter.isEmpty()) {
            tag.put(FILTER_TAG, filter.save(registries));
        }
        return tag;
    }

    /** A chemical whose mod left the pack comes back empty rather than failing the drawer. */
    public void load(HolderLookup.Provider registries, CompoundTag tag) {
        stored = read(registries, tag);
        filter = tag.contains(FILTER_TAG, Tag.TAG_COMPOUND)
                ? ChemicalStack.parseOptional(registries, tag.getCompound(FILTER_TAG)) : ChemicalStack.EMPTY;
    }

    public static ChemicalStack read(HolderLookup.Provider registries, CompoundTag tag) {
        return tag.contains(CHEMICAL_TAG, Tag.TAG_COMPOUND)
                ? ChemicalStack.parseOptional(registries, tag.getCompound(CHEMICAL_TAG))
                : ChemicalStack.EMPTY;
    }
}
