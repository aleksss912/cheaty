package com.example.dripclient.modules;

import com.example.dripclient.Category;
import com.example.dripclient.Module;
import com.example.dripclient.Setting;
import com.example.dripclient.mixin.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;

/** Szybkie stawianie blokow: brak przerwy miedzy kolejnymi blokami przy trzymaniu prawego przycisku. */
public class FastPlaceModule extends Module {
    private final Setting.Bool onlyBlocks = add(new Setting.Bool("Tylko bloki", true));

    public FastPlaceModule() {
        super("FastPlace", Category.PLAYER);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null) return;
        if (onlyBlocks.value && !(mc.player.getMainHandStack().getItem() instanceof BlockItem)) return;
        try {
            if ((Object) mc instanceof MinecraftClientAccessor acc) acc.dripclient$setItemUseCooldown(0);
        } catch (Throwable ignored) {
        }
    }
}
