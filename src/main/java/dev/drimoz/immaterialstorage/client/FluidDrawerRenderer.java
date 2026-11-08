package dev.drimoz.immaterialstorage.client;

import dev.drimoz.immaterialstorage.StorageDrawersContract;
import dev.drimoz.immaterialstorage.drawer.Compartments;
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
 * Draws what a Fluid Drawer holds: in each compartment, the fluid standing in its tank as high as
 * the compartment is full, and its amount on the frame.
 *
 * <p>A fluid has a surface, so its level is the glance; the exact figure is the text. The fluid
 * glows at its own light level (lava lights the tank, water does not).
 */
public class FluidDrawerRenderer implements BlockEntityRenderer<FluidDrawerBlockEntity> {

    /**
     * Depth of every tank, in the local frame of {@link DrawerRendering#faceFront}: Functional
     * Storage's fluid drawer tanks run from just behind the front frame to 14.5 px deep. Their sides
     * come from {@link Compartments}.
     */
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
        Compartments compartments = drawer.compartments();

        pose.pushPose();
        DrawerRendering.faceFront(pose, drawer);
        for (int i = 0; i < compartments.count(); i++) {
            FluidStack fluid = drawer.tanks().get(i).getFluid();
            int capacity = drawer.tanks().get(i).getCapacity();
            if (!fluid.isEmpty() && capacity > 0) {
                renderFluid(pose, buffers, fluid, Math.min(1f, fluid.getAmount() / (float) capacity), light, compartments, i);
            }
            // One compartment says stored / capacity; smaller ones have room for the amount only.
            String amount = StorageDrawersContract.formatCount(fluid.getAmount());
            DrawerRendering.drawAmount(font, pose, buffers, compartments.count() == 1
                    ? amount + " / " + StorageDrawersContract.formatCount(capacity) + " mB" : amount + " mB", compartments, i);
        }
        pose.popPose();
    }

    private static void renderFluid(PoseStack pose, MultiBufferSource buffers, FluidStack fluid, float progress, int light,
                                    Compartments compartments, int compartment) {
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

        float left = DrawerRendering.localX(compartments.tankLeft(compartment));
        float right = DrawerRendering.localX(compartments.tankRight(compartment));
        float bottom = DrawerRendering.localY(compartments.tankBottom(compartment));
        float height = (compartments.tankTop(compartment) - compartments.tankBottom(compartment)) / 16f;
        float top = bottom + height * Math.max(progress, 1f / 16f);
        float u0 = sprite.getU0();
        float u1 = sprite.getU(left - right);
        float v0 = sprite.getV0();
        float vFront = sprite.getV(Math.min(1f, top - bottom));
        float vTop = sprite.getV(13.5f / 16f);

        // Front face, towards -z.
        vertex(buffer, last, left, bottom, FRONT, u0, vFront, color, light, 0, 0, -1);
        vertex(buffer, last, right, bottom, FRONT, u1, vFront, color, light, 0, 0, -1);
        vertex(buffer, last, right, top, FRONT, u1, v0, color, light, 0, 0, -1);
        vertex(buffer, last, left, top, FRONT, u0, v0, color, light, 0, 0, -1);

        // Surface, towards +y.
        vertex(buffer, last, right, top, FRONT, u0, v0, color, light, 0, 1, 0);
        vertex(buffer, last, right, top, BACK, u0, vTop, color, light, 0, 1, 0);
        vertex(buffer, last, left, top, BACK, u1, vTop, color, light, 0, 1, 0);
        vertex(buffer, last, left, top, FRONT, u1, v0, color, light, 0, 1, 0);
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
