/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.thruster;

import java.io.File;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Thruster plugin
 * @author ucchy
 */
public class Thruster extends JavaPlugin implements Listener {

    private static final String NAME = "thruster";
    protected static final String DISPLAY_NAME = NAME;

    private ThrusterConfig config;

    /**
     * プラグインが有効化されたときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {

        // コンフィグファイルの読み込み
        config = new ThrusterConfig(this);

        // イベント登録
        getServer().getPluginManager().registerEvents(this, this);

        // ColorTeaming のロード
        Plugin colorteaming = null;
        if ( getServer().getPluginManager().isPluginEnabled("ColorTeaming") ) {
            colorteaming = getServer().getPluginManager().getPlugin("ColorTeaming");
            String ctversion = colorteaming.getDescription().getVersion();
            if ( Utility.isUpperVersion(ctversion, "2.2.5") ) {
                getLogger().info("ColorTeaming was loaded. "
                        + getDescription().getName() + " is in cooperation with ColorTeaming.");
                ItemStack item = makeNewThrusterItem();
                ColorTeamingBridge bridge = new ColorTeamingBridge(colorteaming);
                bridge.registerItem(item, NAME, DISPLAY_NAME);
            } else {
                getLogger().warning("ColorTeaming was too old. The cooperation feature will be disabled.");
                getLogger().warning("NOTE: Please use ColorTeaming v2.2.5 or later version.");
            }
        }
    }

    /**
     * プラグインのコマンドが実行されたときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if ( args.length >= 1 && args[0].equalsIgnoreCase("get") ) {

            if ( !sender.hasPermission("thruster.get") ) {
                sender.sendMessage(ChatColor.RED + "You don't have permission : thruster.get");
                return true;
            }

            if ( !(sender instanceof Player) ) {
                sender.sendMessage(ChatColor.RED + "You have to run this command in game.");
                return true;
            }

            setNewThruster((Player)sender);
            sender.sendMessage(ChatColor.YELLOW + "You got new thruster.");
            return true;

        } else if ( args.length >= 2 && args[0].equalsIgnoreCase("give") ) {

            if ( !sender.hasPermission("thruster.give") ) {
                sender.sendMessage(ChatColor.RED + "You don't have permission : thruster.give");
                return true;
            }

            Player target = Utility.getPlayer(args[1]);

            if ( target == null ) {
                sender.sendMessage(ChatColor.RED + "Player " + args[1] + " is not found.");
                return true;
            }

            setNewThruster(target);
            sender.sendMessage(ChatColor.YELLOW + "Player " + args[1] + " got new thruster.");
            return true;

        } else if ( args.length >= 1 && args[0].equalsIgnoreCase("reload") ) {

            if ( !sender.hasPermission("thruster.reload") ) {
                sender.sendMessage(ChatColor.RED + "You don't have permission : thruster.reload");
                return true;
            }

            config.reloadConfig();
            sender.sendMessage(ChatColor.YELLOW + "Configuration reloaded.");
            return true;
        }

        return false;
    }

    /**
     * スラスターのアイテムを作成して返す
     * @return スラスター
     */
    private ItemStack makeNewThrusterItem() {

        ItemStack item = new ItemStack(config.getThrusterMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(DISPLAY_NAME);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 対象のプレイヤーに、新しいスラスターをはかせる
     * @param player
     */
    private void setNewThruster(Player player) {

        ItemStack item = makeNewThrusterItem();
        ItemStack temp = player.getInventory().getBoots();
        player.getInventory().setBoots(item);
        if ( temp != null && temp.getType() != Material.AIR ) {
            player.getInventory().addItem(temp);
        }
        Utility.updateInventory(player);
    }

    /**
     * スニークの切り替えが行われたときに呼び出されるメソッド
     * @param event
     */
    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {

        // スニークがオフになったときだけ、スラスターを起動する
        if ( event.isSneaking() ) {
            return;
        }

        // 現在の位置を記録
        final Player player = event.getPlayer();
        final Location prevLoc = player.getLocation();

        // ブーツがスラスターでなければ、終了する
        final ItemStack boots = player.getInventory().getBoots();
        if ( boots == null || boots.getType() != config.getThrusterMaterial() ||
                !boots.getItemMeta().hasDisplayName() ||
                !boots.getItemMeta().getDisplayName().equals(Thruster.DISPLAY_NAME) ) {
            return;
        }

        // 権限が無ければ終了する
        if ( !player.hasPermission("thruster.use") ) {
            return;
        }

        // コンフィグで非接地時が制限されているなら、
        // 接地していない場合は何もしないで終わる
        if ( config.isLimitOnGround() &&
                player.getLocation().getBlock().
                    getRelative(BlockFace.DOWN).getType() == Material.AIR ) {
            return;
        }

        // 1tick後に移動の差分をとり、移動方向に飛び出させる
        new BukkitRunnable() {

            @Override
            public void run() {

                double x = player.getLocation().getX() - prevLoc.getX();
                double z = player.getLocation().getZ() - prevLoc.getZ();

                // 1tick前の移動方向を取得し、横方向、上昇方向の推進力を設定する。
                Vector vector = new Vector(x, 0, z);
                if ( vector.length() > 0 ) {
                    vector.normalize();
                    vector.multiply(config.getSidePower());
                    vector.setY(config.getUpperPower());
                } else {
                    vector.setY(config.getUpperPower() * 2);
                }

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

        }.runTaskLater(this, 1);

    }

    /**
     * このプラグインのJarファイルを返す
     * @return jarファイル
     */
    protected File getJarFile() {
        return this.getFile();
    }
}
