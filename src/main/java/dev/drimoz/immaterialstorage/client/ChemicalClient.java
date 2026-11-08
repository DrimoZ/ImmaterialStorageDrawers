package dev.drimoz.immaterialstorage.client;

import dev.drimoz.immaterialstorage.StorageDrawersContract;
import dev.drimoz.immaterialstorage.chemical.ChemicalContent;
import dev.drimoz.immaterialstorage.chemical.ChemicalDrawerBlockEntity;
import dev.drimoz.immaterialstorage.chemical.DrawerChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * The Chemical Drawers' client side: their renderer, their upgrade screen view, and their frame. <b>Mekanism only: reached from {@code ChemicalContent}.</b>
 */
public final class ChemicalClient {

    private ChemicalClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(
                ChemicalContent.BLOCK_ENTITY.get(), context -> new ChemicalDrawerRenderer(context.getFont())));
        modBus.addListener((ModelEvent.ModifyBakingResult event) -> ISClientSetup.wrapFramed(event,
                ChemicalContent.DRAWERS.stream().map(drawer -> (Block) drawer.get()).toList()));
        DrawerScreen.registerView(ChemicalDrawerBlockEntity.class, new DrawerScreen.ContentView<>() {
            @Override
            public void drawIcon(GuiGraphics graphics, ChemicalDrawerBlockEntity drawer, int compartment, int x, int y, float partialTick) {
                ChemicalStack chemical = drawer.tanks().get(compartment).getChemical();
                if (!chemical.isEmpty()) {
                    DrawerScreen.drawSprite(graphics, chemical.getChemical().getIcon(), 0xFF000000 | chemical.getChemicalTint(), x, y);
                }
            }

            @Override
            public Component describe(ChemicalDrawerBlockEntity drawer, int compartment) {
                DrawerChemicalTank tank = drawer.tanks().get(compartment);
                return ChemicalDrawerBlockEntity.describe(tank.getChemical(), tank.getCapacity());
            }

            @Override
            public String capacity(ChemicalDrawerBlockEntity drawer) {
                return StorageDrawersContract.formatCount(drawer.tanks().get(0).getCapacity()) + " mB";
            }
        });
    }
}
