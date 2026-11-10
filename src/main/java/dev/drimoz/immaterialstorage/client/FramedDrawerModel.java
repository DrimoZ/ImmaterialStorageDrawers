/*
 * The UV remapping is adapted from Storage Drawers' SpriteReplacementModel
 * (https://github.com/jaquadro/StorageDrawers), MIT License, Copyright (c) 2014 Justin Aquadro.
 */
package dev.drimoz.immaterialstorage.client;

import com.jaquadro.minecraft.storagedrawers.api.framing.IFramedMaterials;
import dev.drimoz.immaterialstorage.StorageDrawersContract;
import dev.drimoz.immaterialstorage.drawer.DrawerBlockEntity;
import dev.drimoz.immaterialstorage.drawer.DrawerBlockEntity.FrameLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A drawer's block model in its frame: the same drawer model, each frame face retextured with the
 * block chosen at Storage Drawers' framing table. Unframed, it is the plain model.
 *
 * <p>Which face takes which material is marked in {@code drawer_body.json} by a tint index (see the
 * constants), so the geometry stays in one JSON and no Storage Drawers model code is involved. The
 * texture is the material block's particle texture, as in Storage Drawers.
 *
 * <p>{@link ForItem} does the same for the inventory icon, with the frame read from the item.
 */
public class FramedDrawerModel extends BakedModelWrapper<BakedModel> {

    /** Tint indices of drawer_body.json; no colour handler is registered for them, so unframed they draw untinted. */
    private static final int SIDE_FACE = 10;
    private static final int TRIM_FACE = 11;

    public FramedDrawerModel(BakedModel plain) {
        super(plain);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData data, @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        FrameLook look = data.get(DrawerBlockEntity.FRAME_LOOK);
        return look == null ? quads : framed(quads, look);
    }

    /** These quads in this frame: the faces marked by a tint index take the frame's materials. */
    static List<BakedQuad> framed(List<BakedQuad> quads, FrameLook look) {
        List<BakedQuad> framed = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            Block material = switch (quad.getTintIndex()) {
                case SIDE_FACE -> look.side();
                case TRIM_FACE -> look.trim();
                default -> null;
            };
            framed.add(material == null ? quad : retexture(quad, sprite(material)));
        }
        return framed;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        FrameLook look = data.get(DrawerBlockEntity.FRAME_LOOK);
        return look == null ? super.getParticleIcon(data) : sprite(look.side());
    }

    private static TextureAtlasSprite sprite(Block block) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(block.defaultBlockState()).getParticleIcon(ModelData.EMPTY);
    }

    /** The same quad on another sprite, at the same place within it; untinted. */
    private static BakedQuad retexture(BakedQuad quad, TextureAtlasSprite to) {
        TextureAtlasSprite from = quad.getSprite();
        int[] vertices = quad.getVertices().clone();
        for (int vertex = 0; vertex < 4; vertex++) {
            int u = vertex * IQuadTransformer.STRIDE + IQuadTransformer.UV0;
            float uFraction = (Float.intBitsToFloat(vertices[u]) - from.getU0()) / (from.getU1() - from.getU0());
            float vFraction = (Float.intBitsToFloat(vertices[u + 1]) - from.getV0()) / (from.getV1() - from.getV0());
            vertices[u] = Float.floatToRawIntBits(to.getU(uFraction));
            vertices[u + 1] = Float.floatToRawIntBits(to.getV(vFraction));
        }
        return new BakedQuad(vertices, -1, quad.getDirection(), to, quad.isShade(), quad.hasAmbientOcclusion());
    }

    /**
     * A drawer's inventory icon in its frame. An item has no model data, so the frame is read from the
     * item itself (Storage Drawers' frame component), and every render pass of the icon (the drawer,
     * and the energy cube or water drawn in it) is retextured; only the drawer's marked faces change.
     */
    public static final class ForItem extends BakedModelWrapper<BakedModel> {

        /** Retextured passes per look: an icon is drawn every frame, its quads are not rebuilt each time. */
        private final Map<FrameLook, List<BakedModel>> passes = new ConcurrentHashMap<>();

        public ForItem(BakedModel plain) {
            super(plain);
        }

        /**
         * Applies the item transform but stays the model drawn: delegated as is, the wrapped model
         * would hand back itself and the frame would never be reached.
         */
        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
            super.applyTransform(context, pose, leftHand);
            return this;
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            IFramedMaterials frame = StorageDrawersContract.newFrame();
            StorageDrawersContract.readFrame(frame, stack);
            FrameLook look = DrawerBlockEntity.lookOf(frame);
            if (look == null) {
                return super.getRenderPasses(stack, fabulous);
            }
            return passes.computeIfAbsent(look, key -> super.getRenderPasses(stack, fabulous).stream()
                    .map(pass -> (BakedModel) new Fixed(pass, key)).toList());
        }
    }

    /** One pass of an icon, always in the same frame. */
    private static final class Fixed extends BakedModelWrapper<BakedModel> {

        private final FrameLook look;
        private final Map<Direction, List<BakedQuad>> quads = new ConcurrentHashMap<>();
        private List<BakedQuad> unculled;

        Fixed(BakedModel pass, FrameLook look) {
            super(pass);
            this.look = look;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            if (side == null) {
                if (unculled == null) {
                    unculled = framed(super.getQuads(state, null, rand), look);
                }
                return unculled;
            }
            return quads.computeIfAbsent(side, key -> framed(super.getQuads(state, key, rand), look));
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                        ModelData data, @Nullable RenderType renderType) {
            return framed(super.getQuads(state, side, rand, data, renderType), look);
        }
    }
}
