package com.morelhaa.trade.utils;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import com.morelhaa.trade.TradePlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gestiona la blacklist de items prohibidos en el trade
 * Permite configurar qué items no pueden ser intercambiados
 */
public class ItemBlacklist {

    private final TradePlugin plugin;
    private final Set<String> blacklistedItems;
    private boolean enabled;

    public ItemBlacklist(TradePlugin plugin) {
        this.plugin = plugin;
        this.blacklistedItems = new HashSet<>();
        this.enabled = true;
    }

    /**
     * Carga la blacklist desde la configuración
     */
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

    /**
     * Verifica si un item está en la blacklist
     * @param item Item a verificar
     * @return true si está prohibido
     */
    public boolean isBlacklisted(Item item) {
        if (!enabled || item == null || item.getId() == 0) {
            return false;
        }

        String itemId = item.getId() + ":0";  // Cualquier meta
        String itemIdMeta = item.getId() + ":" + item.getDamage();  // Meta específica

        return blacklistedItems.contains(itemId) || blacklistedItems.contains(itemIdMeta);
    }

    /**
     * Verifica si un jugador puede bypassear la blacklist
     * @param player Jugador a verificar
     * @return true si tiene permiso de bypass
     */
    public boolean canBypass(Player player) {
        return player != null && player.hasPermission("trade.bypass.blacklist");
    }

    /**
     * Verifica si un item puede ser comerciado por un jugador
     * Considera tanto la blacklist como los permisos de bypass
     * @param item Item a verificar
     * @param player Jugador que intenta comerciarlo
     * @return true si puede comerciarlo
     */
    public boolean canTrade(Item item, Player player) {
        if (!enabled) {
            return true;
        }

        if (!isBlacklisted(item)) {
            return true;
        }

        return canBypass(player);
    }

    /**
     * Añade un item a la blacklist
     * @param itemId ID del item en formato "id:meta"
     * @return true si se añadió correctamente
     */
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

    /**
     * Remueve un item de la blacklist
     * @param itemId ID del item en formato "id:meta"
     * @return true si se removió correctamente
     */
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

    /**
     * Guarda la blacklist en la configuración
     */
    private void saveBlacklist() {
        List<String> items = new ArrayList<>(blacklistedItems);
        plugin.getConfig().set("blacklist.items", items);
        plugin.getConfig().save();
    }

    /**
     * Verifica si hay items prohibidos en una lista
     * @param items Lista de items a verificar
     * @param player Jugador que intenta comerciarlos
     * @return Item prohibido encontrado, o null si todos están permitidos
     */
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

    /**
     * Obtiene la lista completa de items prohibidos
     * @return Set de items en blacklist
     */
    public Set<String> getBlacklistedItems() {
        return new HashSet<>(blacklistedItems);
    }

    /**
     * Obtiene el tamaño de la blacklist
     * @return Número de items prohibidos
     */
    public int getBlacklistSize() {
        return blacklistedItems.size();
    }

    /**
     * Verifica si la blacklist está habilitada
     * @return true si está activa
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Activa o desactiva la blacklist
     * @param enabled Estado deseado
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        plugin.getConfig().set("blacklist.enabled", enabled);
        plugin.getConfig().save();
    }

    /**
     * Limpia toda la blacklist
     */
    public void clearBlacklist() {
        blacklistedItems.clear();
        saveBlacklist();
    }

    /**
     * Convierte un Item a formato de blacklist
     * @param item Item a convertir
     * @return String en formato "id:meta"
     */
    public static String itemToBlacklistFormat(Item item) {
        if (item == null || item.getId() == 0) {
            return null;
        }
        return item.getId() + ":" + item.getDamage();
    }

    /**
     * Obtiene información detallada de la blacklist
     * @return String con información formateada
     */
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