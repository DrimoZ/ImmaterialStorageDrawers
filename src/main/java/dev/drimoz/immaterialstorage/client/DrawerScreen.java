package dev.drimoz.immaterialstorage.client;

import dev.drimoz.immaterialstorage.ImmaterialStorage;
import dev.drimoz.immaterialstorage.StorageDrawersContract;
import dev.drimoz.immaterialstorage.drawer.DrawerBlockEntity;
import dev.drimoz.immaterialstorage.drawer.DrawerMenu;
import dev.drimoz.immaterialstorage.energy.EnergyDrawerBlockEntity;
import dev.drimoz.immaterialstorage.fluid.FluidDrawerBlockEntity;
import dev.drimoz.immaterialstorage.util.AmountFormat;
import net.minecraft.client.Minecraft;
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
 * <p>Where their screen shows the drawer's item, ours fills the slot with what the drawer holds
 * (the fluid, or the energy core), as high as the drawer is full, and writes the amount in the
 * field beside it; hovering either shows the exact figures.
 */
public class DrawerScreen extends AbstractContainerScreen<DrawerMenu> {

    // Measured on Storage Drawers' drawers_1.png.
    private static final int CONTENT_X = 80;
    private static final int CONTENT_Y = 36;
    private static final int CONTENT_SIZE = 16;
    private static final int FIELD_X = 128;
    private static final int FIELD_Y = 36;
    private static final int FIELD_WIDTH = 34;
    private static final int FIELD_HEIGHT = 16;

    private static final ResourceLocation ENERGY_CORE =
            ResourceLocation.fromNamespaceAndPath(ImmaterialStorage.MOD_ID, "block/energy_core");

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
        if (isHovering(CONTENT_X, CONTENT_Y, CONTENT_SIZE, CONTENT_SIZE, mouseX, mouseY)
                || isHovering(FIELD_X, FIELD_Y, FIELD_WIDTH, FIELD_HEIGHT, mouseX, mouseY)) {
            graphics.renderTooltip(font, exactAmount(menu.drawer()), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(StorageDrawersContract.DRAWER_SCREEN_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        DrawerBlockEntity drawer = menu.drawer();
        int x = leftPos + CONTENT_X;
        int y = topPos + CONTENT_Y;
        if (drawer instanceof EnergyDrawerBlockEntity energy) {
            long capacity = energy.energy().getCapacityLong();
            float progress = capacity <= 0 ? 0 : (float) Math.min(1d, energy.energy().getStoredLong() / (double) capacity);
            fill(graphics, sprite(ENERGY_CORE), 0xFFFFFFFF, x, y, progress);
            field(graphics, AmountFormat.format(energy.energy().getStoredLong()) + " FE");
        } else if (drawer instanceof FluidDrawerBlockEntity fluidDrawer) {
            FluidStack fluid = fluidDrawer.tank().getFluid();
            if (!fluid.isEmpty()) {
                IClientFluidTypeExtensions client = IClientFluidTypeExtensions.of(fluid.getFluid());
                float progress = Math.min(1f, fluid.getAmount() / (float) Math.max(1, fluidDrawer.tank().getCapacity()));
                fill(graphics, sprite(client.getStillTexture(fluid)), client.getTintColor(fluid), x, y, progress);
            }
            field(graphics, AmountFormat.format(fluid.getAmount()) + " mB");
        }
    }

    /** The slot, filled from the bottom as high as the drawer is full. */
    private static void fill(GuiGraphics graphics, TextureAtlasSprite sprite, int color, int x, int y, float progress) {
        int height = Math.round(CONTENT_SIZE * progress);
        if (height <= 0) {
            return;
        }
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8 & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = (color >>> 24) / 255f;
        graphics.blit(x, y + CONTENT_SIZE - height, 0, CONTENT_SIZE, height, sprite, r, g, b, a);
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
            return Component.translatable("tooltip.immaterialstorage_sd.energy",
                    String.format("%,d", energy.energy().getStoredLong()), String.format("%,d", energy.energy().getCapacityLong()));
        }
        if (drawer instanceof FluidDrawerBlockEntity fluidDrawer) {
            FluidStack fluid = fluidDrawer.tank().getFluid();
            return Component.translatable("tooltip.immaterialstorage_sd.fluid",
                    fluid.isEmpty() ? Component.translatable("tooltip.immaterialstorage_sd.empty") : fluid.getHoverName(),
                    String.format("%,d", fluid.getAmount()), String.format("%,d", fluidDrawer.tank().getCapacity()));
        }
        return Component.empty();
    }

    private static TextureAtlasSprite sprite(ResourceLocation texture) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
    }
}
