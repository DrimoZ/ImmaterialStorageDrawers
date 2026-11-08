package dev.drimoz.immaterialstorage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.drimoz.immaterialstorage.StorageDrawersContract;
import dev.drimoz.immaterialstorage.drawer.Compartments;
import dev.drimoz.immaterialstorage.source.SourceDrawerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Draws what a Source Drawer holds: Ars's Source standing in the tank as high as the drawer is full,
 * and the amount on the frame. <b>Ars Nouveau only: see {@code compat.Mods}.</b>
 *
 * <p>Ars's own texture, untinted (it already is the colour), translucent and at full brightness: Source
 * glows in every block Ars puts it in, as in Immaterial Drawers.
 */
public class SourceDrawerRenderer implements BlockEntityRenderer<SourceDrawerBlockEntity> {

    private static final int SOURCE_COLOR = 0xE6FFFFFF;

    private final Font font;

    public SourceDrawerRenderer(Font font) {
        this.font = font;
    }

    @Override
    public void render(SourceDrawerBlockEntity drawer, float partialTicks, PoseStack pose, MultiBufferSource buffers,
                       int combinedLight, int combinedOverlay) {
        if (drawer.getLevel() == null) {
            return;
        }
        int stored = drawer.source().getStoredRaw();
        int capacity = drawer.source().getCapacityRaw();

        pose.pushPose();
        DrawerRendering.faceFront(pose, drawer);
        if (stored > 0 && capacity > 0) {
            var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(SourceClient.SOURCE_TEXTURE);
            DrawerRendering.drawTank(pose, buffers, sprite, SOURCE_COLOR, LightTexture.FULL_BRIGHT,
                    Math.min(1f, stored / (float) capacity), Compartments.ONE, 0);
        }
        DrawerRendering.drawAmount(font, pose, buffers,
                StorageDrawersContract.formatCount(stored) + " / " + StorageDrawersContract.formatCount(capacity), Compartments.ONE, 0);
        pose.popPose();
    }
}
