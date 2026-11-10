package dev.drimoz.immaterialstorage.drawer;

import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerAttributesModifiable;
import com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute;
import net.minecraft.nbt.CompoundTag;

/**
 * What Storage Drawers' keys change on a drawer of ours, through its public
 * {@link IDrawerAttributesModifiable}: the keys ask the block for Storage Drawers'
 * {@code drawer_attributes} capability and act on this, as they do on their own drawers.
 *
 * <ul>
 *   <li><b>Drawer Key</b> (lock): each compartment keeps what it holds when emptied and takes
 *       nothing else. Only drawers with compartments can lock (not energy, not Source).</li>
 *   <li><b>Quantify Key</b>: shows or hides the amount on the front. Shown by default, unlike Storage
 *       Drawers' item drawers, because a tank's level says less than a stack does.</li>
 *   <li><b>Shroud Key</b>: hides the contents on the front.</li>
 *   <li><b>Priority Key</b>: controllers fill higher priorities first.</li>
 *   <li><b>Suspend Key</b>: controllers stop filling the drawer; it can still be emptied.</li>
 * </ul>
 *
 * <p>Not here: who may use the drawer (the Personal Key), which is {@code IProtectable} on the block
 * entity. Every change calls the owner back, which saves and syncs the drawer.
 */
public class DrawerAttributes implements IDrawerAttributesModifiable {

    private final boolean lockable;
    private final Runnable onChange;
    private boolean locked;
    private boolean concealed;
    private boolean showingQuantity = true;
    private int priority;
    private boolean suspended;

    public DrawerAttributes(boolean lockable, Runnable onChange) {
        this.lockable = lockable;
        this.onChange = onChange;
    }

    @Override
    public boolean canItemLock(LockAttribute attr) {
        return lockable;
    }

    @Override
    public boolean isItemLocked(LockAttribute attr) {
        return locked;
    }

    /** One lock for both of Storage Drawers' lock attributes: its Drawer Key always sets them together. */
    @Override
    public boolean setItemLocked(LockAttribute attr, boolean isLocked) {
        if (!lockable) {
            return false;
        }
        if (locked != isLocked) {
            locked = isLocked;
            onChange.run();
        }
        return true;
    }

    public boolean isLocked() {
        return locked;
    }

    @Override
    public boolean isConcealed() {
        return concealed;
    }

    @Override
    public boolean setIsConcealed(boolean state) {
        concealed = state;
        onChange.run();
        return true;
    }

    @Override
    public boolean isShowingQuantity() {
        return showingQuantity;
    }

    @Override
    public boolean setIsShowingQuantity(boolean state) {
        showingQuantity = state;
        onChange.run();
        return true;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public boolean setPriority(int priority) {
        this.priority = priority;
        onChange.run();
        return true;
    }

    @Override
    public boolean isSuspended() {
        return suspended;
    }

    @Override
    public boolean setIsSuspended(boolean state) {
        suspended = state;
        onChange.run();
        return true;
    }

    /** Nothing a key changed: a dropped drawer without contents then stays a plain, stackable item. */
    public boolean isDefault() {
        return !locked && !concealed && showingQuantity && priority == 0 && !suspended;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Locked", locked);
        tag.putBoolean("Concealed", concealed);
        tag.putBoolean("Quantity", showingQuantity);
        tag.putInt("Priority", priority);
        tag.putBoolean("Suspended", suspended);
        return tag;
    }

    /** No callback: loading is not a change to save or sync. */
    public void load(CompoundTag tag) {
        locked = tag.getBoolean("Locked");
        concealed = tag.getBoolean("Concealed");
        showingQuantity = !tag.contains("Quantity") || tag.getBoolean("Quantity");
        priority = tag.getInt("Priority");
        suspended = tag.getBoolean("Suspended");
    }
}
