package dev.drimoz.immaterialstorage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.drimoz.immaterialstorage.StorageDrawersContract;
import dev.drimoz.immaterialstorage.chemical.ChemicalDrawerBlockEntity;
import dev.drimoz.immaterialstorage.drawer.Compartments;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Draws what a Chemical Drawer holds: in each compartment, the chemical standing in its tank as high
 * as the compartment is full, and its amount on the frame. <b>Mekanism only: see {@code compat.Mods}.</b>
 *
 * <p>The fluid drawer's tank, with one thing from Immaterial Drawers' chemical drawer: a gas reads as
 * a gas. It is drawn translucent and at full brightness, like the energy cube's core, so a filled
 * drawer reads in an unlit room; the heavy chemicals (slurries, pigments, infuse types) are drawn
 * like a fluid, opaque and lit by the block.
 */
public class ChemicalDrawerRenderer implements BlockEntityRenderer<ChemicalDrawerBlockEntity> {

    /** A gas is seen through, but not so much that a pale one vanishes against the tank walls. */
    private static final int GAS_ALPHA = 0xCC;

    private final Font font;

    public ChemicalDrawerRenderer(Font font) {
        this.font = font;
    }

    @Override
    public void render(ChemicalDrawerBlockEntity drawer, float partialTicks, PoseStack pose, MultiBufferSource buffers,
                       int combinedLight, int combinedOverlay) {
        if (drawer.getLevel() == null) {
            return;
        }
        int light = DrawerRendering.frontLight(drawer);
        Compartments compartments = drawer.compartments();

        pose.pushPose();
        DrawerRendering.faceFront(pose, drawer);
        DrawerRendering.drawIndicators(pose, buffers, drawer);
        for (int i = 0; i < compartments.count(); i++) {
            ChemicalStack chemical = drawer.tanks().get(i).getChemical();
            long capacity = drawer.tanks().get(i).getCapacity();
            ChemicalStack pin = drawer.tanks().get(i).getFilter();
            boolean ghost = chemical.isEmpty() && !pin.isEmpty();
            if (ghost) {
                chemical = pin;
            }
            if (!drawer.attributes().isConcealed() && !chemical.isEmpty() && capacity > 0) {
                boolean gas = chemical.getChemical().isGaseous();
                TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                        .apply(chemical.getChemical().getIcon());
                int alpha = ghost ? FluidDrawerRenderer.GHOST_ALPHA : gas ? GAS_ALPHA : 0xFF;
                int color = alpha << 24 | chemical.getChemicalTint() & 0xFFFFFF;
                float progress = ghost ? 1f : (float) Math.min(1d, chemical.getAmount() / (double) capacity);
                DrawerRendering.drawTank(pose, buffers, sprite, color, gas ? LightTexture.FULL_BRIGHT : light,
                        progress, compartments, i);
            }
            if (!drawer.attributes().isShowingQuantity()) {
                continue;
            }
            String amount = StorageDrawersContract.formatCount(ghost ? 0 : chemical.getAmount());
            DrawerRendering.drawAmount(font, pose, buffers, compartments.count() == 1
                    ? amount + " / " + StorageDrawersContract.formatCount(capacity) + " mB" : amount + " mB", compartments, i);
        }
        pose.popPose();
    }
}
