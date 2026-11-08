/*
 * The UV remapping is adapted from Storage Drawers' SpriteReplacementModel
 * (https://github.com/jaquadro/StorageDrawers), MIT License, Copyright (c) 2014 Justin Aquadro.
 */
package dev.drimoz.immaterialstorage.client;

import dev.drimoz.immaterialstorage.drawer.DrawerBlockEntity;
import dev.drimoz.immaterialstorage.drawer.DrawerBlockEntity.FrameLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A drawer's block model in its frame: the same drawer model, each frame face retextured with the
 * block chosen at Storage Drawers' framing table. Unframed, it is the plain model.
 *
 * <p>Which face takes which material is marked in {@code drawer_body.json} by a tint index (see the
 * constants), so the geometry stays in one JSON and no Storage Drawers model code is involved. The
 * texture is the material block's particle texture, as in Storage Drawers.
 *
 * <p>Not this class's job: the item. A framed drawer's inventory icon is the plain one.
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
        if (look == null) {
            return quads;
        }
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
}
