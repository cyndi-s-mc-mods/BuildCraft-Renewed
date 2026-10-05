package buildcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import org.joml.Vector3f;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;

/** Draws laser beams: long square tubes with a strip of a sprite repeated along their length. */
public final class LaserRenderer {
    private LaserRenderer() {}

    /** Draws a beam between two points (relative to the current pose).
     * @param width The width of the beam, in blocks.
     * @param v0 The top of the strip of the sprite to use, in pixels (0-16).
     * @param v1 The bottom of the strip. */
    public static void laser(PoseStack.Pose pose, VertexConsumer buf, Vec3 from, Vec3 to, float width, TextureAtlasSprite sprite,
        float v0, float v1, int light, int colour) {
        Vec3 dir = to.subtract(from);
        double length = dir.length();
        if (length < 1e-4) return;
        Vec3 forward = dir.scale(1 / length);
        // Any vector not parallel to the beam gives the two sides
        Vec3 helper = Math.abs(forward.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side = forward.cross(helper).normalize().scale(width / 2);
        Vec3 up = forward.cross(side).normalize().scale(width / 2);
        Vec3[] corners = { side.add(up), side.subtract(up), side.scale(-1).subtract(up), side.scale(-1).add(up) };

        float sv0 = sprite.getV(v0 / 16), sv1 = sprite.getV(v1 / 16);
        int segments = (int) Math.ceil(length);
        for (int s = 0; s < segments; s++) {
            double t0 = s, t1 = Math.min(length, s + 1);
            Vec3 a = from.add(forward.scale(t0));
            Vec3 b = from.add(forward.scale(t1));
            float u0 = sprite.getU(0), u1 = sprite.getU((float) (t1 - t0));
            for (int i = 0; i < 4; i++) {
                Vec3 c0 = corners[i], c1 = corners[(i + 1) % 4];
                Vec3 normal = c0.add(c1).normalize();
                vertex(pose, buf, a.add(c0), u0, sv0, normal, light, colour);
                vertex(pose, buf, b.add(c0), u1, sv0, normal, light, colour);
                vertex(pose, buf, b.add(c1), u1, sv1, normal, light, colour);
                vertex(pose, buf, a.add(c1), u0, sv1, normal, light, colour);
            }
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buf, Vec3 pos, float u, float v, Vec3 normal, int light, int colour) {
        Vector3f n = new Vector3f((float) normal.x, (float) normal.y, (float) normal.z);
        buf.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
            .setColor(colour)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, n.x, n.y, n.z);
    }
}
