package com.example.dripclient.modules;

import com.example.dripclient.Category;
import com.example.dripclient.Module;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Xray: rysowane sa tylko rudy (reszta swiata jest niewidoczna). Dziala przez mixin w Block.shouldDrawSide,
 * a przy wlaczaniu przebudowuje chunki i wylacza odrzucanie chunkow za sciana.
 */
public class XrayModule extends Module {
    public static volatile boolean active = false;

    private static final Map<Block, Boolean> CACHE = new ConcurrentHashMap<>();
    private boolean savedCulling = true;

    public XrayModule() {
        super("Xray", Category.RENDER);
    }

    public static boolean isTarget(BlockState state) {
        Block b = state.getBlock();
        Boolean cached = CACHE.get(b);
        if (cached != null) return cached;
        String path = Registries.BLOCK.getId(b).getPath();
        boolean result = path.endsWith("_ore") || path.equals("ancient_debris") || path.equals("gilded_blackstone");
        CACHE.put(b, result);
        return result;
    }

    @Override
    public void onLoad() {
        active = enabled;
    }

    @Override
    public void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        active = true;
        savedCulling = mc.chunkCullingEnabled;
        mc.chunkCullingEnabled = false;
        if (mc.worldRenderer != null && mc.world != null) mc.worldRenderer.reload();
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        active = false;
        mc.chunkCullingEnabled = savedCulling;
        if (mc.worldRenderer != null && mc.world != null) mc.worldRenderer.reload();
    }

    @Override
    public void onTick(MinecraftClient mc) {
        // po wczytaniu z pliku ustawien nie ma onEnable - upewnij sie, ze culling jest wylaczony
        if (mc.chunkCullingEnabled) {
            savedCulling = true;
            mc.chunkCullingEnabled = false;
        }
    }
}
