package com.morelhaa.trade.utils;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import com.morelhaa.trade.TradePlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemBlacklist {

    private final TradePlugin plugin;
    private final Set<String> blacklistedItems;
    private boolean enabled;

    public ItemBlacklist(TradePlugin plugin) {
        this.plugin = plugin;
        this.blacklistedItems = new HashSet<>();
        this.enabled = true;
    }

    public void loadBlacklist() {
        blacklistedItems.clear();

        enabled = plugin.getConfig().getBoolean("blacklist.enabled", true);

        if (!enabled) {
            plugin.getLogger().info("§eBlacklist de items desactivada.");
            return;
        }

        List<String> items = plugin.getConfig().getStringList("blacklist.items");

        if (items != null && !items.isEmpty()) {
            for (String itemString : items) {
                if (itemString != null && !itemString.trim().isEmpty()) {
                    blacklistedItems.add(itemString.trim());
                }
            }
            plugin.getLogger().info("§aCargados " + blacklistedItems.size() + " items en la blacklist.");
        } else {
            plugin.getLogger().warning("§cNo se encontraron items en la blacklist.");
        }
    }

    public boolean isBlacklisted(Item item) {
        if (!enabled || item == null || item.getId() == 0) {
            return false;
        }

        String itemId = item.getId() + ":0";
        String itemIdMeta = item.getId() + ":" + item.getDamage(); 

        return blacklistedItems.contains(itemId) || blacklistedItems.contains(itemIdMeta);
    }

    public boolean canBypass(Player player) {
        return player != null && player.hasPermission("trade.bypass.blacklist");
    }

    public boolean canTrade(Item item, Player player) {
        if (!enabled) {
            return true;
        }

        if (!isBlacklisted(item)) {
            return true;
        }

        return canBypass(player);
    }

    public boolean addToBlacklist(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            return false;
        }

        String formatted = itemId.trim();
        if (!formatted.contains(":")) {
            formatted += ":0";
        }

        boolean added = blacklistedItems.add(formatted);

        if (added) {
            saveBlacklist();
        }

        return added;
    }

    public boolean removeFromBlacklist(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            return false;
        }

        String formatted = itemId.trim();
        if (!formatted.contains(":")) {
            formatted += ":0";
        }

        boolean removed = blacklistedItems.remove(formatted);

        if (removed) {
            saveBlacklist();
        }

        return removed;
    }

    private void saveBlacklist() {
        List<String> items = new ArrayList<>(blacklistedItems);
        plugin.getConfig().set("blacklist.items", items);
        plugin.getConfig().save();
    }

    public Item findBlacklistedItem(List<Item> items, Player player) {
        if (!enabled || items == null || items.isEmpty()) {
            return null;
        }

        for (Item item : items) {
            if (item != null && item.getId() != 0) {
                if (!canTrade(item, player)) {
                    return item;
                }
            }
        }

        return null;
    }

    public Set<String> getBlacklistedItems() {
        return new HashSet<>(blacklistedItems);
    }

    public int getBlacklistSize() {
        return blacklistedItems.size();
    }
turn true si está activa
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        plugin.getConfig().set("blacklist.enabled", enabled);
        plugin.getConfig().save();
    }

    public void clearBlacklist() {
        blacklistedItems.clear();
        saveBlacklist();
    }

    public static String itemToBlacklistFormat(Item item) {
        if (item == null || item.getId() == 0) {
            return null;
        }
        return item.getId() + ":" + item.getDamage();
    }

    public String getBlacklistInfo() {
        StringBuilder info = new StringBuilder();
        info.append("§6=== Blacklist de Items ===\n");
        info.append("§eEstado: ").append(enabled ? "§aActivada" : "§cDesactivada").append("\n");
        info.append("§eTotal de items: §f").append(blacklistedItems.size()).append("\n");

        if (!blacklistedItems.isEmpty()) {
            info.append("§eItems prohibidos:\n");
            for (String itemId : blacklistedItems) {
                info.append("§7- §f").append(itemId).append("\n");
            }
        }

        return info.toString();
    }
}
