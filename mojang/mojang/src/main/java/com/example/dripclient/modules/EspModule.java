package com.example.dripclient.modules;

import com.example.dripclient.BoxDraw;
import com.example.dripclient.Category;
import com.example.dripclient.Module;
import com.example.dripclient.ModuleManager;
import com.example.dripclient.Setting;
import com.example.dripclient.UiConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Obrys boxa wokol graczy (i opcjonalnie wrogich mobow) przez sciany, ze swieceniem w kolorach motywu. */
public class EspModule extends Module {
    private final Setting.Bool players = add(new Setting.Bool("Gracze", true));
    private final Setting.Bool mobs = add(new Setting.Bool("Moby", false));
    private final Setting.Bool glow = add(new Setting.Bool("Glow", true));
    private final Setting.Bool friendsGreen = add(new Setting.Bool("Znajomi zielono", true));
    private final Setting.Num thickness = add(new Setting.Num("Grubość", 0.8, 0.4, 3.0, 0.1));
    private final Setting.Num range = add(new Setting.Num("Zasięg", 96, 16, 256, 16));

    public EspModule() {
        super("ESP", Category.RENDER);
    }

    @Override
    public void onHud(DrawContext ctx, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        List<Entity> list = new ArrayList<>();
        double r2 = range.value * range.value;
        if (players.value) {
            for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
                if (p != mc.player && p.squaredDistanceTo(mc.player) <= r2) list.add(p);
            }
        }
        if (mobs.value) {
            for (Entity e : mc.world.getEntities()) {
                if (e instanceof HostileEntity && e.squaredDistanceTo(mc.player) <= r2) list.add(e);
            }
        }
        list.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(mc.player)));

        int pri = UiConfig.primary(), sec = UiConfig.secondary();
        int n = 0;
        for (Entity e : list) {
            if (n++ >= 24) break;
            boolean friend = e instanceof PlayerEntity pe && ModuleManager.isFriend(pe.getName().getString());
            int color = (friend && friendsGreen.value) ? 0xFF55FF55 : (e instanceof PlayerEntity ? pri : sec);
            Box b = e.getDimensions(e.getPose()).getBoxAt(e.getLerpedPos(delta));
            BoxDraw.draw(mc, ctx, b, color, (float) thickness.value, glow.value);
        }
    }
}
