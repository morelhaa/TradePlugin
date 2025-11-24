package com.morelhaa.trade;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import com.morelhaa.trade.commands.TradeCommand;
import com.morelhaa.trade.listeners.TradeListener;
import com.morelhaa.trade.manager.TradeManager;
import com.morelhaa.trade.utils.ItemBlacklist;

import java.io.File;

public class TradePlugin extends PluginBase {

    private static TradePlugin instance;
    private TradeManager tradeManager;
    private ItemBlacklist itemBlacklist;
    private Config config;

    @Override
    public void onEnable() {
        instance = this;

        loadConfiguration();

        itemBlacklist = new ItemBlacklist(this);
        itemBlacklist.loadBlacklist();

        tradeManager = new TradeManager(this);

        this.getServer().getCommandMap().register("trade", new TradeCommand(this));

        this.getServer().getPluginManager().registerEvents(new TradeListener(this), this);

        getLogger().info("§a  TradePlugin " + getDescription().getVersion());
        getLogger().info("§a  Sistema de intercambios cargado");
        getLogger().info("§a  Items en blacklist: " + itemBlacklist.getBlacklistSize());
    }

    @Override
    public void onDisable() {
        if (tradeManager != null) {
            // Cancelar todos los trades activos
            tradeManager.cancelAllTrades();
            getLogger().info("§eTodos los trades activos han sido cancelados.");
        }

        getLogger().info("§c  TradePlugin desactivado correctamente");
    }

    private void loadConfiguration() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }

        // Cargar configuración
        config = new Config(configFile, Config.YAML);

        getLogger().info("§aConfiguración cargada correctamente.");
    }

    public void reloadConfiguration() {
        config.reload();
        itemBlacklist.loadBlacklist();
        getLogger().info("§aConfiguración recargada.");
    }

    public String getMessage(String key) {
        String prefix = config.getString("messages.prefix", "§8[§6Trade§8]§r");
        String message = config.getString("messages." + key, "§cMensaje no encontrado: " + key);
        return prefix + " " + message;
    }

    public String getMessage(String key, String placeholder, String value) {
        String message = getMessage(key);
        return message.replace(placeholder, value);
    }

    public boolean areSoundsEnabled() {
        return config.getBoolean("sounds-enabled", true);
    }
    public int getRequestTimeout() {
        return config.getInt("request-timeout", 30);
    }
    public double getMaxTradeDistance() {
        return config.getDouble("advanced.max-distance", 15.0);
    }
    public boolean shouldCheckDistance() {
        return config.getBoolean("advanced.check-distance", true);
    }
    public boolean shouldCancelOnDistance() {
        return config.getBoolean("advanced.cancel-on-distance", true);
    }
    public int getCompletionDelay() {
        return config.getInt("advanced.completion-delay", 40);
    }
    public int getReadyButtonSlot() {
        return config.getInt("gui.ready-button-slot", 49);
    }
    public static TradePlugin getInstance() {
        return instance;
    }
    public TradeManager getTradeManager() {
        return tradeManager;
    }
    public ItemBlacklist getItemBlacklist() {
        return itemBlacklist;
    }
    @Override
    public Config getConfig() {
        return config;
    }
}