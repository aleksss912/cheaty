package com.example.dripclient.mixin;

import com.example.dripclient.modules.XrayModule;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class BlockMixin {

    /** Xray: rysuj tylko wybrane bloki (rudy), reszte pomijaj. */
    @Inject(method = "shouldDrawSide(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;Lnet/minecraft/util/math/BlockPos;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void dripclient$xray(BlockState state, BlockView world, BlockPos pos, Direction side,
                                        BlockPos otherPos, CallbackInfoReturnable<Boolean> cir) {
        if (XrayModule.active) cir.setReturnValue(XrayModule.isTarget(state));
    }
}
