package dev.drimoz.immaterialstorage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.drimoz.immaterialstorage.drawer.DrawerBlock;
import dev.drimoz.immaterialstorage.drawer.DrawerBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;

/**
 * What every drawer renderer of ours shares: the frame they draw in, the light they use, and the
 * amount written on the front. One place, so every drawer in a wall reads the same.
 */
final class DrawerRendering {

    /** Text on the front plane, over the lower part of the niche. */
    private static final float TEXT_Y = -5.5f / 16f;
    private static final float TEXT_SCALE = 0.006f;

    private DrawerRendering() {
    }

    /**
     * Moves the pose to the block centre, turned so that local -z is the drawer's front. Local
     * coordinates then run from -0.5 to 0.5; the front plane is z = -0.5.
     */
    static void faceFront(PoseStack pose, DrawerBlockEntity drawer) {
        Direction facing = drawer.getBlockState().getValue(DrawerBlock.FACING);
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()));
    }

    /** The block is opaque, so its own light is 0: the niche is lit as the block in front is. */
    static int frontLight(DrawerBlockEntity drawer) {
        Direction facing = drawer.getBlockState().getValue(DrawerBlock.FACING);
        return LevelRenderer.getLightColor(drawer.getLevel(), drawer.getBlockPos().relative(facing));
    }

    /** Writes the text centred under the niche's contents, facing whoever looks at the front. */
    static void drawAmount(Font font, PoseStack pose, MultiBufferSource buffers, int light, String text) {
        pose.pushPose();
        pose.translate(0, TEXT_Y, -0.5f - 0.002f);
        pose.mulPose(Axis.YP.rotationDegrees(180f));
        pose.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        font.drawInBatch(text, -font.width(text) / 2f, 0, 0xFFFFFFFF, false, pose.last().pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0, light);
        pose.popPose();
    }
}
