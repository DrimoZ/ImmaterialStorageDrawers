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
        int glow = fluid.getFluid().getFluidType().getLightLevel(fluid);
        if (glow > 0) {
            light = LightTexture.pack(Math.max(LightTexture.block(light), glow), LightTexture.sky(light));
        }
        DrawerRendering.drawTank(pose, buffers, sprite, client.getTintColor(fluid), light, progress, compartments, compartment);
    }
}
