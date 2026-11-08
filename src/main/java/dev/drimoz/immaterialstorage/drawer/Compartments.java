package dev.drimoz.immaterialstorage.drawer;

import dev.drimoz.immaterialstorage.StorageDrawersContract;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * How a drawer's front is divided: one, two (stacked) or four (2 x 2) compartments, each with its
 * own tank. Everything that depends on the layout reads it here, so the click, the model, the
 * renderer and the screen cannot disagree:
 * <ul>
 *   <li>the tank of each compartment and where its amount is written, in block pixels, on
 *       Functional Storage's fluid drawer models (models/block/drawer_body*.json);</li>
 *   <li>which compartment a click on the front lands in;</li>
 *   <li>Storage Drawers' screen for that many drawers, and its slot positions.</li>
 * </ul>
 *
 * <p>Compartments are numbered as Storage Drawers numbers its drawers: left to right, top to bottom.
 * Horizontal positions are measured from the viewer's left.
 */
public enum Compartments {

    ONE(new float[][]{{1.5f, 14.5f, 1.5f, 14.5f}}, new float[][]{{8, 2}}, 0.4f, new int[][]{{80, 36}}),
    TWO(new float[][]{{1.5f, 14.5f, 9, 14.5f}, {1.5f, 14.5f, 1.5f, 7}},
            new float[][]{{8, 9.5f}, {8, 1.5f}}, 0.4f, new int[][]{{80, 23}, {80, 49}}),
    FOUR(new float[][]{{1.5f, 7, 9, 14.5f}, {9, 14.5f, 9, 14.5f}, {1.5f, 7, 1.5f, 7}, {9, 14.5f, 1.5f, 7}},
            new float[][]{{4, 9.5f}, {12, 9.5f}, {4, 1.5f}, {12, 1.5f}}, 0.2f,
            new int[][]{{67, 23}, {93, 23}, {67, 49}, {93, 49}});

    /** Per compartment: left, right, bottom, top of its tank, in pixels. */
    private final float[][] tanks;
    /** Per compartment: centre of its amount, from the left and from the bottom, in pixels. */
    private final float[][] labels;
    private final float labelMaxWidth;
    private final int[][] screenSlots;

    Compartments(float[][] tanks, float[][] labels, float labelMaxWidth, int[][] screenSlots) {
        this.tanks = tanks;
        this.labels = labels;
        this.labelMaxWidth = labelMaxWidth;
        this.screenSlots = screenSlots;
    }

    public int count() {
        return tanks.length;
    }

    /** The compartment under a point of the front, from the viewer's left (0..1) and from the bottom (0..1). */
    public int at(double fromLeft, double fromBottom) {
        int row = count() == 1 || fromBottom >= 0.5 ? 0 : 1;
        int column = count() == 4 && fromLeft >= 0.5 ? 1 : 0;
        return count() == 4 ? row * 2 + column : row;
    }

    /** The compartment a click on a drawer facing {@code facing} lands in. */
    public int at(BlockPos pos, Direction facing, Vec3 hit) {
        Vec3 local = hit.subtract(Vec3.atLowerCornerOf(pos));
        Direction left = facing.getClockWise();
        double along = left.getAxis().choose(local.x, local.y, local.z);
        double towardsLeft = left.getAxisDirection() == Direction.AxisDirection.POSITIVE ? along : 1 - along;
        return at(1 - towardsLeft, local.y);
    }

    public float tankLeft(int compartment) {
        return tanks[compartment][0];
    }

    public float tankRight(int compartment) {
        return tanks[compartment][1];
    }

    public float tankBottom(int compartment) {
        return tanks[compartment][2];
    }

    public float tankTop(int compartment) {
        return tanks[compartment][3];
    }

    public float labelFromLeft(int compartment) {
        return labels[compartment][0];
    }

    public float labelFromBottom(int compartment) {
        return labels[compartment][1];
    }

    /** Widest an amount may be written, in blocks, as Functional Storage limits it. */
    public float labelMaxWidth() {
        return labelMaxWidth;
    }

    public ResourceLocation screenTexture() {
        return StorageDrawersContract.drawerScreenTexture(count());
    }

    public int screenSlotX(int compartment) {
        return screenSlots[compartment][0];
    }

    public int screenSlotY(int compartment) {
        return screenSlots[compartment][1];
    }

    /** The block name suffix of this layout: none for one, then {@code _2} and {@code _4}. */
    public String suffix() {
        return count() == 1 ? "" : "_" + count();
    }
}
