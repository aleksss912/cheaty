package com.example.dripclient.modules;

import com.example.dripclient.Category;
import com.example.dripclient.Module;
import com.example.dripclient.ModuleManager;
import com.example.dripclient.OwnWater;
import com.example.dripclient.PlaceUtil;
import com.example.dripclient.Setting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Klikasz pajeczyna (albo obsydianem) w dzwignie: mod wylewa wode w miejsce dzwigni (zmywa ja),
 * od razu odbiera wode wiadrem i stawia pajeczyne / obsydian w tym miejscu.
 * Odbieranie wody chroni pajeczyne - plynaca woda by ja zniszczyla.
 * Uwaga: na 2 ticki obraca kamere w strone sciany za dzwignia, bo wiadro dziala tam, gdzie patrzysz.
 */
public class ErrorKillerModule extends Module {
    private final Setting.Bool web = add(new Setting.Bool("Pajęczyna", true));
    private final Setting.Bool obsidian = add(new Setting.Bool("Obsydian", true));
    private final Setting.Bool pickUp = add(new Setting.Bool("Odbierz wodę", true));
    private final Setting.Bool notify = add(new Setting.Bool("Komunikaty", true));

    private enum Phase { IDLE, ACT, WATCH }

    private Phase phase = Phase.IDLE;
    private BlockPos target;
    private int bucketSlot, origSlot, timer, msgCooldown;
    private float oldYaw, oldPitch;

    public ErrorKillerModule() {
        super("ErrorKiller", Category.WORLD);
    }

    @Override
    public void onDisable() {
        phase = Phase.IDLE;
        target = null;
    }

    private void say(MinecraftClient mc, String msg) {
        if (notify.value && mc.player != null && msgCooldown == 0) {
            mc.player.sendMessage(Text.literal("Veltrix ErrorKiller: " + msg), true);
            msgCooldown = 20;
        }
    }

    /** Wywolywane z UseBlockCallback: prawy klik na dzwigni z pajeczyna / obsydianem w rece. */
    public static ActionResult onUse(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (!world.isClient || hand != Hand.MAIN_HAND) return ActionResult.PASS;
        ErrorKillerModule m = ModuleManager.get(ErrorKillerModule.class);
        if (m == null || !m.enabled || m.phase != Phase.IDLE || player.isSneaking()) return ActionResult.PASS;

        BlockPos pos = hit.getBlockPos();
        BlockState st = world.getBlockState(pos);
        if (!st.isOf(Blocks.LEVER)) return ActionResult.PASS;

        ItemStack held = player.getStackInHand(hand);
        boolean ok = (m.web.value && held.isOf(Items.COBWEB)) || (m.obsidian.value && held.isOf(Items.OBSIDIAN));
        if (!ok) return ActionResult.PASS;

        return m.begin(MinecraftClient.getInstance(), pos, st) ? ActionResult.SUCCESS : ActionResult.PASS;
    }

    private boolean begin(MinecraftClient mc, BlockPos pos, BlockState st) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return false;

        int bucket = PlaceUtil.findSlot(p, s -> s.isOf(Items.WATER_BUCKET));
        if (bucket == PlaceUtil.NONE) {
            say(mc, "brak wiadra z wodą w hotbarze");
            return false;
        }

        // punkt na scianie, do ktorej przylega dzwignia (obok dzwigni, zeby promien jej nie zlapal):
        // wiadro wyleje wtedy wode dokladnie w komorke dzwigni
        BlockFace face = st.get(Properties.BLOCK_FACE);
        Direction normal;
        if (face == BlockFace.FLOOR) normal = Direction.UP;
        else if (face == BlockFace.CEILING) normal = Direction.DOWN;
        else normal = st.get(Properties.HORIZONTAL_FACING);
        BlockPos attached = pos.offset(normal.getOpposite());
        Vec3d tangent = normal.getAxis().isVertical()
                ? new Vec3d(1, 0, 0)
                : new Vec3d(-normal.getOffsetZ(), 0, normal.getOffsetX());
        Vec3d aim = Vec3d.ofCenter(attached)
                .add(normal.getOffsetX() * 0.5, normal.getOffsetY() * 0.5, normal.getOffsetZ() * 0.5)
                .add(tangent.multiply(0.38));

        Vec3d eye = p.getEyePos();
        double dx = aim.x - eye.x, dy = aim.y - eye.y, dz = aim.z - eye.z;
        double hd = Math.sqrt(dx * dx + dz * dz);

        target = pos;
        bucketSlot = bucket;
        origSlot = p.getInventory().selectedSlot;
        oldYaw = p.getYaw();
        oldPitch = p.getPitch();
        p.setYaw((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        p.setPitch((float) -Math.toDegrees(Math.atan2(dy, hd)));
        p.getInventory().selectedSlot = bucket;
        OwnWater.mark(mc.world, pos, 60);
        phase = Phase.ACT;
        return true;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null || mc.interactionManager == null) return;
        if (msgCooldown > 0) msgCooldown--;
        ClientPlayerEntity p = mc.player;

        if (phase == Phase.ACT) {
            // serwer ma juz nowy obrot i wybrane wiadro: wylej wode, odbierz ja i postaw blok
            mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
            if (pickUp.value) mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
            OwnWater.mark(mc.world, target, 60);

            PlaceUtil.placeAt(mc, target, origSlot);

            p.getInventory().selectedSlot = origSlot;
            p.setYaw(oldYaw);
            p.setPitch(oldPitch);
            phase = Phase.WATCH;
            timer = 0;
        } else if (phase == Phase.WATCH) {
            BlockState st = mc.world.getBlockState(target);
            if (st.isOf(Blocks.COBWEB) || st.isOf(Blocks.OBSIDIAN)) {
                phase = Phase.IDLE;
            } else if (!st.isOf(Blocks.LEVER) && timer < 6) {
                // dzwignia znikla, ale blok jeszcze nie stoi - ponow
                int slot = origSlot;
                PlaceUtil.placeAt(mc, target, slot);
                timer++;
            } else if (++timer > 8) {
                say(mc, "nie udało się (sprawdź czy wiadro jest pełne i blok w ręce)");
                phase = Phase.IDLE;
            }
        }
    }
}
