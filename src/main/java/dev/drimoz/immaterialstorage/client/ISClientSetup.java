package dev.drimoz.immaterialstorage.client;

import dev.drimoz.immaterialstorage.ImmaterialStorage;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.List;

/** Client-side registrations. All mod-bus events, which NeoForge routes on its own. */
@EventBusSubscriber(modid = ImmaterialStorage.MOD_ID, value = Dist.CLIENT)
public final class ISClientSetup {

    /** Vanilla's water colour in the default biome: the Fluid Drawer icon's water is tinted with it. */
    private static final int WATER_COLOR = 0xFF3F76E4;

    private ISClientSetup() {
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        ImmaterialStorage.FLUID_DRAWER_ITEMS.forEach(item ->
                event.register((stack, tintIndex) -> tintIndex == 0 ? WATER_COLOR : -1, item.get()));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ImmaterialStorage.ENERGY_DRAWER_BLOCK_ENTITY.get(),
                context -> new EnergyDrawerRenderer(context.getFont()));
        event.registerBlockEntityRenderer(ImmaterialStorage.FLUID_DRAWER_BLOCK_ENTITY.get(),
                context -> new FluidDrawerRenderer(context.getFont()));
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ImmaterialStorage.DRAWER_MENU.get(), DrawerScreen::new);
    }

    /**
     * The cube's two halves. No block or item points at them, so nothing would load them otherwise,
     * and an unregistered model comes back as the missing model, not an error.
     */
    /** Every state of our drawers wears its frame, when it has one. */
    @SubscribeEvent
    public static void wrapFramedModels(ModelEvent.ModifyBakingResult event) {
        List<Block> drawers = new java.util.ArrayList<>(List.of(ImmaterialStorage.ENERGY_DRAWER.get()));
        ImmaterialStorage.FLUID_DRAWERS.forEach(drawer -> drawers.add(drawer.get()));
        for (Block block : drawers) {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(state),
                        (location, plain) -> new FramedDrawerModel(plain));
            }
        }
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(EnergyDrawerRenderer.CUBE_FRAME);
        event.register(EnergyDrawerRenderer.CUBE_CORE);
    }
}
