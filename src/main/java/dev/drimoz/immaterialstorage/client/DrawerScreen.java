package dev.drimoz.immaterialstorage.client;

import dev.drimoz.immaterialstorage.StorageDrawersContract;
import dev.drimoz.immaterialstorage.drawer.DrawerBlockEntity;
import dev.drimoz.immaterialstorage.drawer.DrawerMenu;
import dev.drimoz.immaterialstorage.energy.EnergyDrawerBlockEntity;
import dev.drimoz.immaterialstorage.fluid.FluidDrawerBlockEntity;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A drawer's upgrade screen, drawn on Storage Drawers' own single-drawer screen texture so it is
 * the same screen a player already knows.
 *
 * <p>It shows the content the way theirs shows an item: as an icon in the face's slot, the amount in
 * the field beside it, the exact figures on hover. A fluid is its still texture; energy is the
 * drawer's own energy cube, turning, its core glowing with the charge.
 */
public class DrawerScreen extends AbstractContainerScreen<DrawerMenu> {

    // Measured on Storage Drawers' drawers_1.png.
    private static final int SLOT_X = 80;
    private static final int SLOT_Y = 36;
    private static final int SLOT_SIZE = 16;
    private static final int FIELD_X = 128;
    private static final int FIELD_Y = 36;
    private static final int FIELD_WIDTH = 34;
    private static final int FIELD_HEIGHT = 16;

    /** The cube fills the slot as an item would, tilted to show its top. */
    private static final float CUBE_PIXELS = 15f;
    private static final float CUBE_TILT = 25f;

    public DrawerScreen(DrawerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 199;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(SLOT_X, SLOT_Y, SLOT_SIZE, SLOT_SIZE, mouseX, mouseY)
                || isHovering(FIELD_X, FIELD_Y, FIELD_WIDTH, FIELD_HEIGHT, mouseX, mouseY)) {
            graphics.renderTooltip(font, exactAmount(menu.drawer()), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(StorageDrawersContract.DRAWER_SCREEN_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        DrawerBlockEntity drawer = menu.drawer();
        int x = leftPos + SLOT_X;
        int y = topPos + SLOT_Y;
        if (drawer instanceof EnergyDrawerBlockEntity energy) {
            long capacity = energy.energy().getCapacityLong();
            float progress = capacity <= 0 ? 0 : (float) Math.min(1d, energy.energy().getStoredLong() / (double) capacity);
            cube(graphics, x, y, progress, partialTick);
            field(graphics, StorageDrawersContract.formatCount(energy.energy().getStoredLong()) + " FE");
        } else if (drawer instanceof FluidDrawerBlockEntity fluidDrawer) {
            FluidStack fluid = fluidDrawer.tank().getFluid();
            if (!fluid.isEmpty()) {
                IClientFluidTypeExtensions client = IClientFluidTypeExtensions.of(fluid.getFluid());
                int color = client.getTintColor(fluid);
                graphics.blit(x, y, 0, SLOT_SIZE, SLOT_SIZE, sprite(client.getStillTexture(fluid)),
                        (color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f, (color >>> 24) / 255f);
            }
            field(graphics, StorageDrawersContract.formatCount(fluid.getAmount()) + " mB");
        }
    }

    /** The energy cube in the slot, as an item icon: lit for items, turning like the one in the drawer. */
    private void cube(GuiGraphics graphics, int x, int y, float progress, float partialTick) {
        float time = minecraft.level == null ? 0 : minecraft.level.getGameTime() % 72000L + partialTick;
        float scale = CUBE_PIXELS / (9f / 16f);
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + SLOT_SIZE / 2f, y + SLOT_SIZE / 2f, 150);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(CUBE_TILT));
        Lighting.setupFor3DItems();
        EnergyDrawerRenderer.drawCube(pose, graphics.bufferSource(), time, 0, progress,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        graphics.flush();
        Lighting.setupForFlatItems();
        pose.popPose();
    }

    /** The short amount, centred in the field, shrunk when it would not fit. */
    private void field(GuiGraphics graphics, String text) {
        float scale = Math.min(1f, (FIELD_WIDTH - 2) / (float) font.width(text));
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(leftPos + FIELD_X + FIELD_WIDTH / 2f, topPos + FIELD_Y + (FIELD_HEIGHT - font.lineHeight * scale) / 2f + 1, 0);
        pose.scale(scale, scale, 1f);
        graphics.drawString(font, text, -font.width(text) / 2, 0, 0x404040, false);
        pose.popPose();
    }

    private static Component exactAmount(DrawerBlockEntity drawer) {
        if (drawer instanceof EnergyDrawerBlockEntity energy) {
            return EnergyDrawerBlockEntity.describe(energy.energy().getStoredLong(), energy.energy().getCapacityLong());
        }
        if (drawer instanceof FluidDrawerBlockEntity fluidDrawer) {
            return FluidDrawerBlockEntity.describe(fluidDrawer.tank().getFluid(), fluidDrawer.tank().getCapacity());
        }
        return Component.empty();
    }

    private static TextureAtlasSprite sprite(ResourceLocation texture) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
    }
}
