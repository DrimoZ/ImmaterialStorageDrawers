package dev.drimoz.immaterialstorage.source;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import net.minecraft.core.BlockPos;

/**
 * A Source Drawer, as Ars Nouveau's {@code SourceManager} sees it. <b>Ars Nouveau only: see
 * {@code compat.Mods}.</b> From Immaterial Drawers.
 *
 * <p>The half of the integration the capability cannot do. What in Ars consumes or makes Source
 * nearby (the enchanting apparatus, imbuement, the sourcelinks) finds it in two ways: by
 * {@code instanceof} on Ars's own class, which we can never pass, and through the providers
 * registered with {@code SourceManager.INSTANCE}, which anyone can join: its public API.
 *
 * <p>Ars cleans that registry itself, dropping every 60 ticks the providers no longer
 * {@link #isValid() valid}; so a provider answers truthfully for a drawer broken or unloaded, and the
 * drawer registers a new one each time it loads.
 */
public record SourceDrawerProvider(SourceDrawerBlockEntity drawer) implements ISpecialSourceProvider {

    @Override
    public ISourceTile getSource() {
        return drawer.source().asTile();
    }

    /** Still the block entity at its position, in a loaded chunk: also retires a drawer replaced in place. */
    @Override
    public boolean isValid() {
        var level = drawer.getLevel();
        BlockPos pos = drawer.getBlockPos();
        return !drawer.isRemoved() && level != null && level.isLoaded(pos) && level.getBlockEntity(pos) == drawer;
    }

    @Override
    public BlockPos getCurrentPos() {
        return drawer.getBlockPos();
    }
}
