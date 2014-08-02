/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.thruster;

import java.io.File;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * スラスターのコンフィグクラス
 * @author ucchy
 */
public class ThrusterConfig {

    private Thruster parent;

    private double sidePower;
    private double upperPower;
    private Material thrusterMaterial;
    private int decreaseDurability;
    private boolean limitOnGround;

    public ThrusterConfig(Thruster parent) {
        this.parent = parent;
        reloadConfig();
    }

    /**
     * コンフィグを読み込む
     */
    protected void reloadConfig() {

        if ( !parent.getDataFolder().exists() ) {
            parent.getDataFolder().mkdirs();
        }

        File file = new File(parent.getDataFolder(), "config.yml");
        if ( !file.exists() ) {
            Utility.copyFileFromJar(
                    parent.getJarFile(), file, "config_ja.yml", false);
        }

        parent.reloadConfig();
        FileConfiguration conf = parent.getConfig();

        upperPower = conf.getDouble("upperPower", 0.5);
        sidePower = conf.getDouble("sidePower", 3.0);

        String temp = conf.getString("thrusterMaterial", "GOLD_BOOTS");
        thrusterMaterial = Material.matchMaterial(temp);
        if ( thrusterMaterial == null ) {
            thrusterMaterial = Material.IRON_BOOTS;
        }

        decreaseDurability = conf.getInt("decreaseDurability", 2);
        if ( decreaseDurability < 0 ) {
            decreaseDurability = 0;
        }

        limitOnGround = conf.getBoolean("limitOnGround", true);
    }

    /**
     * @return parent
     */
    public Thruster getParent() {
        return parent;
    }

    /**
     * @return sidePower
     */
    public double getSidePower() {
        return sidePower;
    }

    /**
     * @return upperPower
     */
    public double getUpperPower() {
        return upperPower;
    }

    /**
     * @return thrusterMaterial
     */
    public Material getThrusterMaterial() {
        return thrusterMaterial;
    }

    /**
     * @return decreaseDurability
     */
    public int getDecreaseDurability() {
        return decreaseDurability;
    }

    /**
     * @return limitOnGround
     */
    public boolean isLimitOnGround() {
        return limitOnGround;
    }
}
