package dev.drimoz.immaterialstorage.client;

import dev.drimoz.immaterialstorage.StorageDrawersContract;
import dev.drimoz.immaterialstorage.compat.Mods;
import dev.drimoz.immaterialstorage.source.SourceContent;
import dev.drimoz.immaterialstorage.source.SourceDrawerBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.List;

/**
 * The Source Drawer's client side: its renderer, its upgrade screen view and its frame. <b>Ars
 * Nouveau only: reached from {@code SourceContent}.</b>
 */
public final class SourceClient {

    /** What fills a Source Jar, animated: a player who has filled one recognises it. */
    static final ResourceLocation SOURCE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Mods.ARS_NOUVEAU, "block/mana_still");

    private SourceClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(
                SourceContent.BLOCK_ENTITY.get(), context -> new SourceDrawerRenderer(context.getFont())));
        modBus.addListener((ModelEvent.ModifyBakingResult event) -> ISClientSetup.wrapFramed(event,
                List.of(SourceContent.SOURCE_DRAWER.get())));
        DrawerScreen.registerView(SourceDrawerBlockEntity.class, new DrawerScreen.ContentView<>() {
            @Override
            public void drawIcon(GuiGraphics graphics, SourceDrawerBlockEntity drawer, int compartment, int x, int y, float partialTick) {
                if (drawer.source().getStoredRaw() > 0) {
                    DrawerScreen.drawSprite(graphics, SOURCE_TEXTURE, 0xFFFFFFFF, x, y);
                }
            }

            @Override
            public Component describe(SourceDrawerBlockEntity drawer, int compartment) {
                return SourceDrawerBlockEntity.describe(drawer.source().getStoredRaw(), drawer.source().getCapacityRaw());
            }

            @Override
            public String capacity(SourceDrawerBlockEntity drawer) {
                return StorageDrawersContract.formatCount(drawer.source().getCapacityRaw());
            }
        });
    }
}
