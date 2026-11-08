package dev.drimoz.immaterialstorage.client;

import dev.drimoz.immaterialstorage.StorageDrawersContract;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.drimoz.immaterialstorage.fluid.FluidDrawerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws what a Fluid Drawer holds: the fluid standing in the tank, as high as the drawer is full,
 * and the amount below it.
 *
 * <p>A fluid has a surface, so its level is the glance; the exact figure is the text. The fluid
 * glows at its own light level (lava lights the tank, water does not).
 */
public class FluidDrawerRenderer implements BlockEntityRenderer<FluidDrawerBlockEntity> {

    /**
     * The tank's inside, in the local frame of {@link DrawerRendering#faceFront}: Functional Storage's
     * fluid drawer tank (models/block/drawer_body.json), 1.5 to 14.5 px across and high, from just
     * behind the front frame to 14.5 px deep.
     */
    private static final float HALF_WIDTH = 6.5f / 16f;
    private static final float BOTTOM = -6.5f / 16f;
    private static final float HEIGHT = 13f / 16f;
    private static final float FRONT = -0.5f + 1f / 16f;
    private static final float BACK = -0.5f + 14.5f / 16f - 0.001f;

    private final Font font;

    public FluidDrawerRenderer(Font font) {
        this.font = font;
    }

    @Override
    public void render(FluidDrawerBlockEntity drawer, float partialTicks, PoseStack pose, MultiBufferSource buffers,
                       int combinedLight, int combinedOverlay) {
        if (drawer.getLevel() == null) {
            return;
        }
        int light = DrawerRendering.frontLight(drawer);
        FluidStack fluid = drawer.tank().getFluid();
        int capacity = drawer.tank().getCapacity();

        pose.pushPose();
        DrawerRendering.faceFront(pose, drawer);
        if (!fluid.isEmpty() && capacity > 0) {
            renderFluid(pose, buffers, fluid, Math.min(1f, fluid.getAmount() / (float) capacity), light);
        }
        DrawerRendering.drawAmount(font, pose, buffers,
                StorageDrawersContract.formatCount(fluid.getAmount()) + " / " + StorageDrawersContract.formatCount(capacity) + " mB");
        pose.popPose();
    }

    private static void renderFluid(PoseStack pose, MultiBufferSource buffers, FluidStack fluid, float progress, int light) {
        IClientFluidTypeExtensions client = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(client.getStillTexture(fluid));
        int color = client.getTintColor(fluid);
        int glow = fluid.getFluid().getFluidType().getLightLevel(fluid);
        if (glow > 0) {
            light = LightTexture.pack(Math.max(LightTexture.block(light), glow), LightTexture.sky(light));
        }
        // No culling: the quads are seen from one side only, and a wrong winding would hide them.
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        PoseStack.Pose last = pose.last();

        float top = BOTTOM + HEIGHT * Math.max(progress, 1f / 16f);
        float u0 = sprite.getU0();
        float u1 = sprite.getU(13f / 16f);
        float v0 = sprite.getV0();
        float vFront = sprite.getV(Math.min(1f, (top - BOTTOM)));
        float vTop = sprite.getV(13.5f / 16f);

        // Front face, towards -z.
        vertex(buffer, last, HALF_WIDTH, BOTTOM, FRONT, u0, vFront, color, light, 0, 0, -1);
        vertex(buffer, last, -HALF_WIDTH, BOTTOM, FRONT, u1, vFront, color, light, 0, 0, -1);
        vertex(buffer, last, -HALF_WIDTH, top, FRONT, u1, v0, color, light, 0, 0, -1);
        vertex(buffer, last, HALF_WIDTH, top, FRONT, u0, v0, color, light, 0, 0, -1);

        // Surface, towards +y.
        vertex(buffer, last, -HALF_WIDTH, top, FRONT, u0, v0, color, light, 0, 1, 0);
        vertex(buffer, last, -HALF_WIDTH, top, BACK, u0, vTop, color, light, 0, 1, 0);
        vertex(buffer, last, HALF_WIDTH, top, BACK, u1, vTop, color, light, 0, 1, 0);
        vertex(buffer, last, HALF_WIDTH, top, FRONT, u1, v0, color, light, 0, 1, 0);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int color, int light, float nx, float ny, float nz) {
        buffer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
