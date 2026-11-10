package dev.drimoz.immaterialstorage.chemical;

import dev.drimoz.immaterialstorage.drawer.Compartments;
import dev.drimoz.immaterialstorage.drawer.DrawerBlock;
import dev.drimoz.immaterialstorage.drawer.DrawerBlockEntity;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A Chemical Drawer block, with one, two or four compartments. <b>Mekanism only: see
 * {@code compat.Mods}.</b>
 *
 * <p>Right-click with a chemical container (a Mekanism chemical tank, most often) empties it into the
 * compartment it is used on, or, when it is empty, fills it from that compartment: a bucket's
 * behaviour, for chemicals. Everything else a drawer does by hand is in {@link DrawerBlock}.
 */
public class ChemicalDrawerBlock extends DrawerBlock {

    private final Compartments compartments;

    public ChemicalDrawerBlock(Properties properties, Compartments compartments) {
        super(properties);
        this.compartments = compartments;
    }

    public Compartments compartments() {
        return compartments;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChemicalDrawerBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends DrawerBlockEntity> blockEntityType() {
        return ChemicalContent.BLOCK_ENTITY.get();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        ItemInteractionResult guarded = guard(stack, level, pos, player);
        if (guarded != null) {
            return guarded;
        }
        IChemicalHandler item = stack.getCapability(ChemicalCapabilities.ITEM);
        if (item == null || !(level.getBlockEntity(pos) instanceof ChemicalDrawerBlockEntity drawer)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        int compartment = compartments.at(pos, state.getValue(FACING), hit.getLocation());
        for (int i = 0; i < item.getChemicalTanks(); i++) {
            if (!item.getChemicalInTank(i).isEmpty()) {
                drawer.tanks().get(compartment).assign(item.getChemicalInTank(i));
                break;
            }
        }
        IChemicalHandler tank = new DrawerChemicalTanks(List.of(drawer.tanks().get(compartment)));
        boolean moved = emptyInto(item, tank) || fillFrom(tank, item);
        return moved ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
    }

    /** What the item holds, into the compartment, as much as fits. */
    private static boolean emptyInto(IChemicalHandler item, IChemicalHandler tank) {
        boolean moved = false;
        for (int i = 0; i < item.getChemicalTanks(); i++) {
            ChemicalStack held = item.getChemicalInTank(i);
            if (held.isEmpty()) {
                continue;
            }
            long fits = held.getAmount() - tank.insertChemical(held, Action.SIMULATE).getAmount();
            if (fits > 0) {
                tank.insertChemical(item.extractChemical(i, fits, Action.EXECUTE), Action.EXECUTE);
                moved = true;
            }
        }
        return moved;
    }

    /** The compartment's chemical, into the item, as much as it takes. */
    private static boolean fillFrom(IChemicalHandler tank, IChemicalHandler item) {
        ChemicalStack available = tank.extractChemical(Long.MAX_VALUE, Action.SIMULATE);
        if (available.isEmpty()) {
            return false;
        }
        long fits = available.getAmount() - item.insertChemical(available, Action.SIMULATE).getAmount();
        if (fits <= 0) {
            return false;
        }
        item.insertChemical(tank.extractChemical(fits, Action.EXECUTE), Action.EXECUTE);
        return true;
    }

    @Override
    protected void describeContent(CompoundTag saved, @Nullable HolderLookup.Provider registries, List<Component> tooltip) {
        tooltip.addAll(ChemicalDrawerBlockEntity.describeStored(saved, registries));
    }
}
