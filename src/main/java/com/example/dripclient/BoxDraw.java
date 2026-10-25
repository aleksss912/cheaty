package com.example.dripclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Rysuje obrys boxa 3D na ekranie (przez sciany), opcjonalnie ze swieceniem (kilka warstw). */
public final class BoxDraw {
    private static final int[][] EDGES = {
            {0, 1}, {1, 3}, {3, 2}, {2, 0},
            {4, 5}, {5, 7}, {7, 6}, {6, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    private BoxDraw() {
    }

    public static void draw(MinecraftClient mc, DrawContext ctx, Box b, int color, float th, boolean glow) {
        Vec3d[] c = new Vec3d[8];
        int i = 0;
        for (int x = 0; x < 2; x++)
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 2; z++)
                    c[i++] = new Vec3d(x == 0 ? b.minX : b.maxX, y == 0 ? b.minY : b.maxY, z == 0 ? b.minZ : b.maxZ);

        Projection.P[] pr = new Projection.P[8];
        for (int k = 0; k < 8; k++) pr[k] = Projection.project(mc, ctx, c[k]);

        for (int[] e : EDGES) {
            Projection.P a = pr[e[0]], d = pr[e[1]];
            if (a.behind || d.behind) continue;
            if (glow) {
                Draw.line(ctx, a.x, a.y, d.x, d.y, th + 3.5f, Draw.withAlpha(color, 0.10f));
                Draw.line(ctx, a.x, a.y, d.x, d.y, th + 1.8f, Draw.withAlpha(color, 0.22f));
            }
            Draw.line(ctx, a.x, a.y, d.x, d.y, th, color);
        }
    }
}
