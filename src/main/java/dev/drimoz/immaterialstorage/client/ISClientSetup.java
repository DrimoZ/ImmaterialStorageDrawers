package dev.drimoz.immaterialstorage.client;

import dev.drimoz.immaterialstorage.ImmaterialStorage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Client-side registrations. Both events are mod-bus events, which NeoForge routes on its own. */
@EventBusSubscriber(modid = ImmaterialStorage.MOD_ID, value = Dist.CLIENT)
public final class ISClientSetup {

    private ISClientSetup() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ImmaterialStorage.ENERGY_DRAWER_BLOCK_ENTITY.get(),
                context -> new EnergyDrawerRenderer(context.getFont()));
    }

    /**
     * The cube's two halves. No block or item points at them, so nothing would load them otherwise,
     * and an unregistered model comes back as the missing model, not an error.
     */
    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(EnergyDrawerRenderer.CUBE_FRAME);
        event.register(EnergyDrawerRenderer.CUBE_CORE);
    }
}
