/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.thruster;

import java.io.File;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Thruster plugin
 * @author ucchy
 */
public class Thruster extends JavaPlugin implements Listener {

    protected static final String DISPLAY_NAME = "thruster";
    private static HashMap<String, ThrusterTask> tasks;

    private ThrusterConfig config;

    /**
     * プラグインが有効化されたときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {

        // 各種初期化
        tasks = new HashMap<String, ThrusterTask>();

        // コンフィグファイルの読み込み
        config = new ThrusterConfig(this);

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

            config.reloadConfig();
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

        ItemStack item = new ItemStack(config.getThrusterMaterial());
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
        String name = player.getName();

        if ( event.isSneaking() ) {
            // スニークがオンになったら、タスクを作成して開始する
            ThrusterTask task = new ThrusterTask(player, this);
            tasks.put(name, task);

        } else {
            // スニークがオフになったら、スラスターを起動する
            if ( !tasks.containsKey(name) ) {
                return;
            }
            tasks.get(name).thruster();
            tasks.remove(name);
        }
    }

    /**
     * このプラグインのJarファイルを返す
     * @return jarファイル
     */
    protected File getJarFile() {
        return this.getFile();
    }

    protected ThrusterConfig getThrusterConfig() {
        return config;
    }
}
