package dev.drimoz.immaterialstorage.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import dev.drimoz.immaterialstorage.StorageDrawersContract;
import dev.drimoz.immaterialstorage.drawer.Compartments;
import dev.drimoz.immaterialstorage.drawer.DrawerBlockEntity;
import dev.drimoz.immaterialstorage.drawer.DrawerMenu;
import dev.drimoz.immaterialstorage.energy.EnergyDrawerBlockEntity;
import dev.drimoz.immaterialstorage.fluid.FluidDrawerBlockEntity;
import dev.drimoz.immaterialstorage.storage.DrawerFluidTank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A drawer's upgrade screen, drawn on Storage Drawers' own screen for that many drawers (1, 2 or 4)
 * so it is the screen a player already knows.
 *
 * <p>It reads like theirs: what each compartment holds sits in its slot as an item would (a fluid is
 * its still texture; energy is the drawer's own energy cube, turning), the field beside them gives
 * a compartment's capacity as theirs gives a drawer's, and hovering a slot gives its exact figures.
 */
public class DrawerScreen extends AbstractContainerScreen<DrawerMenu> {

    // Measured on Storage Drawers' drawers_*.png; the slots are in Compartments.
    private static final int SLOT_SIZE = 16;
    private static final int FIELD_X = 128;
    private static final int FIELD_Y = 36;
    private static final int FIELD_WIDTH = 34;
    private static final int FIELD_HEIGHT = 16;
    /** Storage Drawers' DrawerScreen writes this label here. */
    private static final int UPGRADES_LABEL_Y = 75;
    private static final int LABEL_COLOR = 0x404040;

    /** The cube fills the slot as an item would, tilted to show its top. */
    private static final float CUBE_PIXELS = 15f;
    private static final float CUBE_TILT = 25f;

    private final Compartments compartments;

    public DrawerScreen(DrawerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.compartments = menu.drawer() instanceof FluidDrawerBlockEntity fluids ? fluids.compartments() : Compartments.ONE;
        imageWidth = 176;
        imageHeight = 199;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        for (int i = 0; i < compartments.count(); i++) {
            if (isHovering(compartments.screenSlotX(i), compartments.screenSlotY(i), SLOT_SIZE, SLOT_SIZE, mouseX, mouseY)) {
                graphics.renderTooltip(font, exactAmount(menu.drawer(), i), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("container.storagedrawers.upgrades"), 8, UPGRADES_LABEL_Y, LABEL_COLOR, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(compartments.screenTexture(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
        DrawerBlockEntity drawer = menu.drawer();
        if (drawer instanceof EnergyDrawerBlockEntity energy) {
            long capacity = energy.energy().getCapacityLong();
            float progress = capacity <= 0 ? 0 : (float) Math.min(1d, energy.energy().getStoredLong() / (double) capacity);
            cube(graphics, leftPos + compartments.screenSlotX(0), topPos + compartments.screenSlotY(0), progress, partialTick);
            field(graphics, StorageDrawersContract.formatCount(capacity) + " FE");
        } else if (drawer instanceof FluidDrawerBlockEntity fluids) {
            for (int i = 0; i < compartments.count(); i++) {
                fluidIcon(graphics, fluids.tanks().get(i).getFluid(),
                        leftPos + compartments.screenSlotX(i), topPos + compartments.screenSlotY(i));
            }
            field(graphics, StorageDrawersContract.formatCount(fluids.tanks().get(0).getCapacity()) + " mB");
        }
    }

    private static void fluidIcon(GuiGraphics graphics, FluidStack fluid, int x, int y) {
        if (fluid.isEmpty()) {
            return;
        }
        IClientFluidTypeExtensions client = IClientFluidTypeExtensions.of(fluid.getFluid());
        int color = client.getTintColor(fluid);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(client.getStillTexture(fluid));
        graphics.blit(x, y, 0, SLOT_SIZE, SLOT_SIZE, sprite,
                (color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f, (color >>> 24) / 255f);
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

    /** The capacity, centred in the field, shrunk when it would not fit. */
    private void field(GuiGraphics graphics, String text) {
        float scale = Math.min(1f, (FIELD_WIDTH - 2) / (float) font.width(text));
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(leftPos + FIELD_X + FIELD_WIDTH / 2f, topPos + FIELD_Y + (FIELD_HEIGHT - font.lineHeight * scale) / 2f + 1, 0);
        pose.scale(scale, scale, 1f);
        graphics.drawString(font, text, -font.width(text) / 2, 0, LABEL_COLOR, false);
        pose.popPose();
    }

    private static Component exactAmount(DrawerBlockEntity drawer, int compartment) {
        if (drawer instanceof EnergyDrawerBlockEntity energy) {
            return EnergyDrawerBlockEntity.describe(energy.energy().getStoredLong(), energy.energy().getCapacityLong());
        }
        if (drawer instanceof FluidDrawerBlockEntity fluids) {
            DrawerFluidTank tank = fluids.tanks().get(compartment);
            return FluidDrawerBlockEntity.describe(tank.getFluid(), tank.getCapacity());
        }
        return Component.empty();
    }
}
