/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.thruster;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ThrusterTask extends BukkitRunnable {

    private ThrusterConfig config;
    private Player player;
    private Location prevLoc;

    public ThrusterTask(Player player, Thruster parent) {
        this.player = player;
        this.config = parent.getThrusterConfig();
        runTaskTimer(parent, 0, 1);
    }

    @Override
    public void run() {
        prevLoc = player.getLocation();
    }

    protected void thruster() {

        cancel();

        if ( prevLoc == null ) {
            return;
        }

        double x = player.getLocation().getX() - prevLoc.getX();
        double z = player.getLocation().getZ() - prevLoc.getZ();

        // ブーツがスラスターでなければ、終了する
        ItemStack boots = player.getInventory().getBoots();
        if ( boots == null || boots.getType() != config.getThrusterMaterial() ||
                !boots.getItemMeta().hasDisplayName() ||
                !boots.getItemMeta().getDisplayName().equals(Thruster.DISPLAY_NAME) ) {
            return;
        }

        // コンフィグで非設置時が制限されているなら、
        // 設置していない場合は何もしないで終わる
        if ( config.isLimitOnGround() &&
                player.getLocation().getBlock().
                    getRelative(BlockFace.DOWN).getType() == Material.AIR ) {
            return;
        }

        // 1tick前の移動方向を取得し、横方向、上昇方向の推進力を設定する。
        Vector vector = new Vector(x, 0, z);
        vector.normalize();
        vector.multiply(config.getSidePower());
        vector.setY(config.getUpperPower());

        // 飛び出させる
        player.setVelocity(vector);
        player.setFallDistance(player.getFallDistance() - 2);

        // 音とエフェクトを発生させる
        player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, 10);
        player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 4);
        player.getWorld().playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 0);

        // 耐久値を減らす
        if ( config.getDecreaseDurability() > 0 ) {
            short durability = boots.getDurability();
            durability += config.getDecreaseDurability();
            if ( durability >= boots.getType().getMaxDurability() ) {
                player.getInventory().setBoots(null);
                Utility.updateInventory(player);
                player.getWorld().playSound(
                        player.getLocation(), Sound.ITEM_BREAK, 1, 1);
            } else {
                boots.setDurability(durability);
            }
        }
    }
}
