/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.thruster;

import java.io.File;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

/**
 * Thruster plugin
 * @author ucchy
 */
public class Thruster extends JavaPlugin implements Listener {

    private static final String DISPLAY_NAME = "thruster";
    private static HashMap<String, Vector> prevVelocities;

    private double sidePower;
    private double upperPower;
    private Material thrusterMaterial;
    private int decreaseDurability;
    private boolean limitOnGround;

    /**
     * プラグインが有効化されたときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {

        // 各種初期化
        prevVelocities = new HashMap<String, Vector>();

        // コンフィグファイルの読み込み
        initConfig();

        // イベント登録
        getServer().getPluginManager().registerEvents(this, this);
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

            initConfig();
            sender.sendMessage(ChatColor.YELLOW + "Configuration reloaded.");
            return true;
        }

        return false;
    }

    /**
     * 対象のプレイヤーに、新しいスラスターをはかせる
     * @param player
     */
    private void setNewThruster(Player player) {

        ItemStack item = new ItemStack(thrusterMaterial);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(DISPLAY_NAME);
        item.setItemMeta(meta);

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

        Player player = event.getPlayer();

        // スニーク終了によるイベントの発生なら終了する
        // 1tick前の移動方向が記録されていないなら終了する
        if ( !event.isSneaking() ||
                !prevVelocities.containsKey(player.getName()) ) {
            return;
        }

        // ブーツがスラスターでなければ、終了する
        ItemStack boots = player.getInventory().getBoots();
        if ( boots == null || boots.getType() != thrusterMaterial ||
                !boots.getItemMeta().hasDisplayName() ||
                !boots.getItemMeta().getDisplayName().equals(DISPLAY_NAME) ) {
            return;
        }

        // コンフィグで非設置時が制限されているなら、
        // 設置していない場合は何もしないで終わる
        if ( limitOnGround &&
                player.getLocation().getBlock().
                    getRelative(BlockFace.DOWN).getType() == Material.AIR ) {
            return;
        }

        // 1tick前の移動方向を取得し、横方向、上昇方向の推進力を設定する。
        Vector vector = prevVelocities.get(player.getName());
        vector.normalize();
        vector.multiply(sidePower);
        vector.setY(upperPower);

        // 飛び出させる
        player.setVelocity(vector);
        player.setFallDistance(player.getFallDistance() - 2);

        // 音とエフェクトを発生させる
        player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, 10);
        player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 4);
        player.getWorld().playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 0);

        // 耐久値を減らす
        if ( decreaseDurability > 0 ) {
            short durability = boots.getDurability();
            durability += decreaseDurability;
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

    /**
     * プレイヤーの移動が検知されたときに呼び出されるメソッド
     * @param event
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {

        String name = event.getPlayer().getName();

        if ( !prevVelocities.containsKey(name) ) {
            prevVelocities.put(name, new Vector());
        }
        Vector vec = prevVelocities.get(name);
        vec.setX(event.getTo().getX() - event.getFrom().getX());
        vec.setZ(event.getTo().getZ() - event.getFrom().getZ());

        prevVelocities.put(name, vec);
    }

    /**
     * コンフィグを読み込む
     */
    private void initConfig() {

        if ( !getDataFolder().exists() ) {
            getDataFolder().mkdirs();
        }

        File file = new File(getDataFolder(), "config.yml");
        if ( !file.exists() ) {
            Utility.copyFileFromJar(getFile(), file, "config_ja.yml", false);
        }

        reloadConfig();
        FileConfiguration conf = getConfig();

        upperPower = conf.getDouble("upperPower", 0.5);
        sidePower = conf.getDouble("sidePower", 3.0);

        String temp = conf.getString("thrusterMaterial", "IRON_BOOTS");
        thrusterMaterial = Material.matchMaterial(temp);
        if ( thrusterMaterial == null ) {
            thrusterMaterial = Material.IRON_BOOTS;
        }

        decreaseDurability = conf.getInt("decreaseDurability", 1);
        if ( decreaseDurability < 0 ) {
            decreaseDurability = 0;
        }

        limitOnGround = conf.getBoolean("limitOnGround", true);
    }
}
