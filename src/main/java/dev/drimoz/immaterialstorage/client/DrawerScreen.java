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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A drawer's upgrade screen, drawn on Storage Drawers' own screen for that many drawers (1, 2 or 4)
 * so it is the screen a player already knows.
 *
 * <p>It reads like theirs: what each compartment holds sits in its slot as an item would (a fluid is
 * its still texture; energy is the drawer's own energy cube, turning), the field beside them gives
 * a compartment's capacity as theirs gives a drawer's, and hovering a slot gives its exact figures.
 * How each kind of drawer is shown is its {@link ContentView}; optional modules register theirs.
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

    /**
     * What the screen shows of one kind of drawer: each compartment's icon, its exact figures on
     * hover, and a compartment's capacity for the field.
     */
    public interface ContentView<T extends DrawerBlockEntity> {
        void drawIcon(GuiGraphics graphics, T drawer, int compartment, int x, int y, float partialTick);

        Component describe(T drawer, int compartment);

        String capacity(T drawer);
    }

    private static final Map<Class<?>, ContentView<?>> VIEWS = new LinkedHashMap<>();

    /** Registers how the screen shows a kind of drawer; called once per kind, at client setup. */
    public static <T extends DrawerBlockEntity> void registerView(Class<T> kind, ContentView<T> view) {
        VIEWS.put(kind, view);
    }

    @SuppressWarnings("unchecked")
    private static ContentView<DrawerBlockEntity> viewOf(DrawerBlockEntity drawer) {
        return (ContentView<DrawerBlockEntity>) VIEWS.entrySet().stream()
                .filter(entry -> entry.getKey().isInstance(drawer)).findFirst()
                .map(Map.Entry::getValue).orElseThrow(() -> new IllegalStateException("no screen view for " + drawer));
    }

    static {
        registerView(EnergyDrawerBlockEntity.class, new ContentView<>() {
            @Override
            public void drawIcon(GuiGraphics graphics, EnergyDrawerBlockEntity drawer, int compartment, int x, int y, float partialTick) {
                long capacity = drawer.energy().getCapacityLong();
                float progress = capacity <= 0 ? 0 : (float) Math.min(1d, drawer.energy().getStoredLong() / (double) capacity);
                cube(graphics, x, y, progress, partialTick);
            }

            @Override
            public Component describe(EnergyDrawerBlockEntity drawer, int compartment) {
                return EnergyDrawerBlockEntity.describe(drawer.energy().getStoredLong(), drawer.energy().getCapacityLong());
            }

            @Override
            public String capacity(EnergyDrawerBlockEntity drawer) {
                return StorageDrawersContract.formatCount(drawer.energy().getCapacityLong()) + " FE";
            }
        });
        registerView(FluidDrawerBlockEntity.class, new ContentView<>() {
            @Override
            public void drawIcon(GuiGraphics graphics, FluidDrawerBlockEntity drawer, int compartment, int x, int y, float partialTick) {
                FluidStack fluid = drawer.tanks().get(compartment).getFluid();
                if (!fluid.isEmpty()) {
                    IClientFluidTypeExtensions client = IClientFluidTypeExtensions.of(fluid.getFluid());
                    drawSprite(graphics, client.getStillTexture(fluid), client.getTintColor(fluid), x, y);
                }
            }

            @Override
            public Component describe(FluidDrawerBlockEntity drawer, int compartment) {
                DrawerFluidTank tank = drawer.tanks().get(compartment);
                return FluidDrawerBlockEntity.describe(tank.getFluid(), tank.getCapacity());
            }

            @Override
            public String capacity(FluidDrawerBlockEntity drawer) {
                return StorageDrawersContract.formatCount(drawer.tanks().get(0).getCapacity()) + " mB";
            }
        });
    }

    private final Compartments compartments;

    public DrawerScreen(DrawerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.compartments = menu.drawer().compartments();
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
                graphics.renderTooltip(font, viewOf(menu.drawer()).describe(menu.drawer(), i), mouseX, mouseY);
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
        ContentView<DrawerBlockEntity> view = viewOf(drawer);
        for (int i = 0; i < compartments.count(); i++) {
            view.drawIcon(graphics, drawer, i, leftPos + compartments.screenSlotX(i), topPos + compartments.screenSlotY(i), partialTick);
        }
        field(graphics, view.capacity(drawer));
    }

    /** A block-atlas sprite filling a slot, as an item icon would, tinted. */
    public static void drawSprite(GuiGraphics graphics, ResourceLocation texture, int color, int x, int y) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        graphics.blit(x, y, 0, SLOT_SIZE, SLOT_SIZE, sprite,
                (color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f, (color >>> 24) / 255f);
    }

    /** The energy cube in the slot, as an item icon: lit for items, turning like the one in the drawer. */
    private static void cube(GuiGraphics graphics, int x, int y, float progress, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
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
}
