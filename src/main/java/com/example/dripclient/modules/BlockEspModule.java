package com.example.dripclient.modules;

import com.example.dripclient.BoxDraw;
import com.example.dripclient.Category;
import com.example.dripclient.Module;
import com.example.dripclient.Setting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Podswietla wybrane bloki w poblizu (skrzynie, spawnery, cenne rudy). Skan co sekunde. */
public class BlockEspModule extends Module {
    private final Setting.Bool chests = add(new Setting.Bool("Skrzynie i beczki", true));
    private final Setting.Bool spawners = add(new Setting.Bool("Spawnery", true));
    private final Setting.Bool ores = add(new Setting.Bool("Cenne rudy", true));
    private final Setting.Bool glow = add(new Setting.Bool("Glow", true));
    private final Setting.Num radius = add(new Setting.Num("Zasięg", 16, 8, 24, 4));

    private record Found(BlockPos pos, int color) {
    }

    private final List<Found> found = new ArrayList<>();
    private int cooldown = 0;

    public BlockEspModule() {
        super("BlockESP", Category.RENDER);
    }

    @Override
    public void onDisable() {
        found.clear();
    }

    private int classify(BlockState st) {
        Block b = st.getBlock();
        if (chests.value && (b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST || b == Blocks.ENDER_CHEST
                || b == Blocks.BARREL || b instanceof ShulkerBoxBlock)) return 0xFFFFC107;
        if (spawners.value && b == Blocks.SPAWNER) return 0xFFFF4D4D;
        if (ores.value && (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE
                || b == Blocks.ANCIENT_DEBRIS || b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE)) {
            return 0xFF40E0FF;
        }
        return 0;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return;
        if (++cooldown < 20) return;
        cooldown = 0;

        int r = (int) radius.value;
        BlockPos c = mc.player.getBlockPos();
        List<Found> tmp = new ArrayList<>();
        for (BlockPos pos : BlockPos.iterate(c.add(-r, -r, -r), c.add(r, r, r))) {
            int color = classify(mc.world.getBlockState(pos));
            if (color != 0) tmp.add(new Found(pos.toImmutable(), color));
            if (tmp.size() > 300) break;
        }
        Vec3d me = mc.player.getPos();
        tmp.sort(Comparator.comparingDouble(f -> Vec3d.ofCenter(f.pos()).squaredDistanceTo(me)));
        found.clear();
        found.addAll(tmp);
    }

    @Override
    public void onHud(DrawContext ctx, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        int n = 0;
        for (Found f : found) {
            if (n++ >= 40) break;
            BoxDraw.draw(mc, ctx, new Box(f.pos()), f.color(), 0.8f, glow.value);
        }
    }
}
