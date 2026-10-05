package buildcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/** Draws textured boxes for block entity renderers. Coordinates are in pixels (0-16 for a full block). */
public final class BoxRenderer {
    /** The texture for one face of a box. UVs are in pixels of the sprite (0-16). */
    public record Face(TextureAtlasSprite sprite, float u0, float v0, float u1, float v1) {
        public static Face full(TextureAtlasSprite sprite) {
            return new Face(sprite, 0, 0, 16, 16);
        }
    }

    private BoxRenderer() {}

    /** Draws a box. faces is indexed by {@link Direction#get3DDataValue()}; null entries are skipped. */
    public static void box(PoseStack.Pose pose, VertexConsumer buf, float x0, float y0, float z0, float x1, float y1, float z1,
        Face[] faces, int light, int color) {
        x0 /= 16;
        y0 /= 16;
        z0 /= 16;
        x1 /= 16;
        y1 /= 16;
        z1 /= 16;
        for (Direction dir : Direction.values()) {
            Face face = faces[dir.get3DDataValue()];
            if (face == null) continue;
            float[][] v = switch (dir) {
                case DOWN -> new float[][] { { x0, y0, z1 }, { x0, y0, z0 }, { x1, y0, z0 }, { x1, y0, z1 } };
                case UP -> new float[][] { { x0, y1, z0 }, { x0, y1, z1 }, { x1, y1, z1 }, { x1, y1, z0 } };
                case NORTH -> new float[][] { { x1, y1, z0 }, { x1, y0, z0 }, { x0, y0, z0 }, { x0, y1, z0 } };
                case SOUTH -> new float[][] { { x0, y1, z1 }, { x0, y0, z1 }, { x1, y0, z1 }, { x1, y1, z1 } };
                case WEST -> new float[][] { { x0, y1, z0 }, { x0, y0, z0 }, { x0, y0, z1 }, { x0, y1, z1 } };
                case EAST -> new float[][] { { x1, y1, z1 }, { x1, y0, z1 }, { x1, y0, z0 }, { x1, y1, z0 } };
            };
            TextureAtlasSprite s = face.sprite();
            float u0 = s.getU(face.u0() / 16), u1 = s.getU(face.u1() / 16);
            float v0 = s.getV(face.v0() / 16), v1 = s.getV(face.v1() / 16);
            float[][] uv = { { u0, v0 }, { u0, v1 }, { u1, v1 }, { u1, v0 } };
            int nx = dir.getStepX(), ny = dir.getStepY(), nz = dir.getStepZ();
            for (int i = 0; i < 4; i++) {
                buf.addVertex(pose, v[i][0], v[i][1], v[i][2])
                    .setColor(color)
                    .setUv(uv[i][0], uv[i][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, nx, ny, nz);
            }
        }
    }
}
