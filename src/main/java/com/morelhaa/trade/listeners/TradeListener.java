package com.morelhaa.trade.listeners;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.inventory.InventoryClickEvent;
import cn.nukkit.event.inventory.InventoryCloseEvent;
import cn.nukkit.event.inventory.InventoryTransactionEvent;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.event.player.PlayerTeleportEvent;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.inventory.transaction.InventoryTransaction;
import cn.nukkit.inventory.transaction.action.InventoryAction;
import com.morelhaa.trade.TradePlugin;
import com.morelhaa.trade.session.TradeSession;
import com.morelhaa.trade.utils.TradeUtils;

public class TradeListener implements Listener {

    private final TradePlugin plugin;

    public TradeListener(TradePlugin plugin) {
        this.plugin = plugin;
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryTransaction(InventoryTransactionEvent event) {
        Player player = event.getTransaction().getSource();
        TradeSession session = plugin.getTradeManager().getSession(player);

        if (session == null) {
            return;
        }

        InventoryTransaction transaction = event.getTransaction();

        for (InventoryAction action : transaction.getActions()) {
            if (action.getInventory().equals(session.getInventory())) {
                int slot = -1;
                if (action.getSlot() >= 0) {
                    slot = action.getSlot();
                }

                if (slot >= 0) {
                    session.handleClick(player, slot, action.getTargetItem());
                }

                if (TradeUtils.isSeparatorSlot(slot)) {
                    event.setCancelled(true);
                    return;
                }

                boolean isPlayer1 = player.equals(session.getPlayer1());
                boolean isLeftSide = TradeUtils.isLeftSide(slot);
                boolean isRightSide = TradeUtils.isRightSide(slot);
                if ((isPlayer1 && isRightSide) || (!isPlayer1 && isLeftSide)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = event.getPlayer();
        TradeSession session = plugin.getTradeManager().getSession(player);

        if (session == null) {
            return;
        }

        if (event.getInventory().equals(session.getInventory())) {
            int slot = event.getSlot();

            if (TradeUtils.isSeparatorSlot(slot)) {
                event.setCancelled(true);
                return;
            }

            if (slot == plugin.getReadyButtonSlot()) {
                event.setCancelled(true);
                session.handleClick(player, slot, null);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = event.getPlayer();
        TradeSession session = plugin.getTradeManager().getSession(player);

        if (session == null) {
            return;
        }

        // Verificar si es el inventario del trade
        if (event.getInventory().equals(session.getInventory())) {
            // Cancelar el trade después de un pequeño delay
            // para evitar problemas con el cierre automático
            plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> {
                if (plugin.getTradeManager().getSession(player) != null) {
                    Player other = session.getOtherPlayer(player);

                    session.cancel();

                    if (other != null && other.isOnline()) {
                        other.sendMessage(plugin.getMessage("partner-cancelled", "{player}", player.getName()));
                    }
                }
            }, 1);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onFormRespond(PlayerFormRespondedEvent event) {
        Player player = event.getPlayer();

        if (event.getResponse() instanceof FormResponseSimple) {
            FormResponseSimple response = (FormResponseSimple) event.getResponse();
            int formId = event.getFormID();
            int buttonId = response.getClickedButtonId();
            plugin.getTradeManager().handleFormResponse(player, formId, buttonId);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        TradeSession session = plugin.getTradeManager().getSession(player);
        if (session != null) {
            Player other = session.getOtherPlayer(player);
            session.cancel();

            if (other != null && other.isOnline()) {
                other.sendMessage(plugin.getMessage("partner-cancelled", "{player}", player.getName()));
            }
        }

        plugin.getTradeManager().cancelRequest(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        TradeSession session = plugin.getTradeManager().getSession(player);
        if (session != null) {
            Player other = session.getOtherPlayer(player);
            session.cancel();

            if (other != null && other.isOnline()) {
                other.sendMessage(plugin.getMessage("partner-cancelled", "{player}", player.getName()));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        TradeSession session = plugin.getTradeManager().getSession(player);

        if (session == null) {
            return;
        }

        if (!plugin.shouldCancelOnDistance()) {
            return;
        }

        Player other = session.getOtherPlayer(player);
        double newDistance = event.getTo().distance(other.getLocation());

        if (newDistance > plugin.getMaxTradeDistance()) {
            // Programar cancelación después del teleport
            plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> {
                TradeSession currentSession = plugin.getTradeManager().getSession(player);
                if (currentSession != null) {
                    currentSession.cancel();

                    player.sendMessage(plugin.getMessage("trade-cancelled"));
                    if (other.isOnline()) {
                        other.sendMessage(plugin.getMessage("partner-cancelled", "{player}", player.getName()));
                    }
                }
            }, 5);
        }
    }
}