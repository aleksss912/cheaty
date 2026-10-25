package com.example.dripclient.modules;

import com.example.dripclient.BoxDraw;
import com.example.dripclient.Category;
import com.example.dripclient.Module;
import com.example.dripclient.Projection;
import com.example.dripclient.Setting;
import com.example.dripclient.UiConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Podswietla przedmioty lezace na ziemi (przez sciany) z nazwa i iloscia. */
public class ItemEspModule extends Module {
    private final Setting.Bool names = add(new Setting.Bool("Nazwy", true));
    private final Setting.Bool glow = add(new Setting.Bool("Glow", true));
    private final Setting.Num range = add(new Setting.Num("Zasięg", 48, 8, 128, 8));

    public ItemEspModule() {
        super("ItemESP", Category.RENDER);
    }

    @Override
    public void onHud(DrawContext ctx, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        double r2 = range.value * range.value;
        List<ItemEntity> list = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof ItemEntity ie && ie.squaredDistanceTo(mc.player) <= r2) list.add(ie);
        }
        list.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(mc.player)));

        int color = UiConfig.secondary();
        int n = 0;
        for (ItemEntity ie : list) {
            if (n++ >= 40) break;
            Vec3d pos = ie.getLerpedPos(delta);
            Box b = ie.getDimensions(ie.getPose()).getBoxAt(pos);
            BoxDraw.draw(mc, ctx, b, color, 0.8f, glow.value);

            if (names.value) {
                Projection.P sp = Projection.project(mc, ctx, pos.add(0, 0.6, 0));
                if (sp.behind) continue;
                int count = ie.getStack().getCount();
                String s = ie.getStack().getName().getString() + (count > 1 ? " x" + count : "");
                MatrixStack m = ctx.getMatrices();
                m.push();
                m.translate((float) sp.x, (float) sp.y, 0f);
                m.scale(0.75f, 0.75f, 1f);
                ctx.drawTextWithShadow(mc.textRenderer, s, -mc.textRenderer.getWidth(s) / 2, 0, 0xFFFFFFFF);
                m.pop();
            }
        }
    }
}
