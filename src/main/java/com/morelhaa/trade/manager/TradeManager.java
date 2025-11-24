package com.morelhaa.trade.manager;

import cn.nukkit.Player;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.level.Sound;
import com.morelhaa.trade.TradePlugin;
import com.morelhaa.trade.session.TradeSession;
import com.morelhaa.trade.utils.TradeUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TradeManager {

    private final TradePlugin plugin;
    private final Map<UUID, TradeSession> activeSessions;
    private final Map<UUID, TradeRequest> pendingRequests;

    public TradeManager(TradePlugin plugin) {
        this.plugin = plugin;
        this.activeSessions = new ConcurrentHashMap<>();
        this.pendingRequests = new ConcurrentHashMap<>();
    }

    public boolean sendRequest(Player sender, Player target) {
        if (sender.equals(target)) {
            sender.sendMessage(plugin.getMessage("cannot-trade-yourself"));
            return false;
        }

        if (!TradeUtils.arePlayersValid(sender, target)) {
            sender.sendMessage(plugin.getMessage("player-not-found"));
            return false;
        }

        if (!sender.hasPermission("trade.use")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return false;
        }

        if (isInTrade(sender)) {
            sender.sendMessage(plugin.getMessage("already-in-trade"));
            return false;
        }

        if (isInTrade(target)) {
            sender.sendMessage(plugin.getMessage("target-in-trade", "{player}", target.getName()));
            return false;
        }

        if (hasPendingRequest(target)) {
            sender.sendMessage(plugin.getMessage("already-sent-request"));
            return false;
        }

        if (hasPendingRequest(sender)) {
            sender.sendMessage(plugin.getMessage("pending-request"));
            return false;
        }

        double distance = TradeUtils.getDistance(sender, target);
        if (distance > plugin.getMaxTradeDistance()) {
            sender.sendMessage(plugin.getMessage("player-not-found"));
            return false;
        }

        TradeRequest request = new TradeRequest(sender, target);
        pendingRequests.put(target.getUniqueId(), request);

        int timeout = plugin.getRequestTimeout();
        int taskId = plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> {
            expireRequest(target.getUniqueId());
        }, timeout * 20).getTaskId();

        request.setExpirationTaskId(taskId);

        sendRequestForm(target, sender);

        sender.sendMessage(plugin.getMessage("request-sent", "{player}", target.getName()));
        target.sendMessage(plugin.getMessage("request-received", "{player}", sender.getName()));

        if (plugin.areSoundsEnabled()) {
            TradeUtils.playSound(target, Sound.RANDOM_ORB);
        }

        return true;
    }

    private void sendRequestForm(Player target, Player sender) {
        FormWindowSimple form = new FormWindowSimple(
                "§6Solicitud de Trade",
                "§e" + sender.getName() + "§r te ha enviado una solicitud de intercambio.\n\n" +
                        "§7¿Deseas aceptar?"
        );

        form.addButton(new ElementButton("§aAceptar"));
        form.addButton(new ElementButton("§cRechazar"));

        int formId = target.showFormWindow(form);
        TradeRequest request = pendingRequests.get(target.getUniqueId());
        if (request != null) {
            request.setFormId(formId);
        }
    }

    public void handleFormResponse(Player player, int formId, int response) {
        TradeRequest request = pendingRequests.get(player.getUniqueId());

        if (request == null || request.getFormId() != formId) {
            return;
        }

        Player sender = request.getSender();

        pendingRequests.remove(player.getUniqueId());

        if (request.getExpirationTaskId() != -1) {
            plugin.getServer().getScheduler().cancelTask(request.getExpirationTaskId());
        }

        if (response == 0) {
            acceptRequest(sender, player);
        } else {
            rejectRequest(sender, player);
        }
    }

    private void acceptRequest(Player sender, Player target) {
        if (!TradeUtils.arePlayersValid(sender, target)) {
            if (sender.isOnline()) {
                sender.sendMessage(plugin.getMessage("player-not-found"));
            }
            return;
        }

        if (isInTrade(sender) || isInTrade(target)) {
            sender.sendMessage(plugin.getMessage("already-in-trade"));
            target.sendMessage(plugin.getMessage("already-in-trade"));
            return;
        }

        double distance = TradeUtils.getDistance(sender, target);
        if (distance > plugin.getMaxTradeDistance()) {
            sender.sendMessage(plugin.getMessage("player-not-found"));
            target.sendMessage(plugin.getMessage("player-not-found"));
            return;
        }

        TradeSession session = new TradeSession(plugin, sender, target);

        if (session.start()) {
            activeSessions.put(sender.getUniqueId(), session);
            activeSessions.put(target.getUniqueId(), session);
        } else {
            sender.sendMessage(plugin.getMessage("trade-cancelled"));
            target.sendMessage(plugin.getMessage("trade-cancelled"));
        }
    }

    private void rejectRequest(Player sender, Player target) {
        if (sender.isOnline()) {
            sender.sendMessage(plugin.getMessage("request-cancelled"));
        }

        if (plugin.areSoundsEnabled()) {
            TradeUtils.playSound(target, Sound.RANDOM_BREAK);
        }
    }

    private void expireRequest(UUID targetUuid) {
        TradeRequest request = pendingRequests.remove(targetUuid);

        if (request != null) {
            Player sender = request.getSender();
            Player target = request.getTarget();

            if (sender != null && sender.isOnline()) {
                sender.sendMessage(plugin.getMessage("request-expired"));
            }

            if (target != null && target.isOnline()) {
                target.sendMessage(plugin.getMessage("request-expired"));
            }
        }
    }

    public void cancelRequest(Player player) {
        TradeRequest request = pendingRequests.remove(player.getUniqueId());

        if (request != null) {
            // Cancelar tarea de expiración
            if (request.getExpirationTaskId() != -1) {
                plugin.getServer().getScheduler().cancelTask(request.getExpirationTaskId());
            }

            Player sender = request.getSender();
            if (sender != null && sender.isOnline()) {
                sender.sendMessage(plugin.getMessage("request-cancelled"));
            }

            player.sendMessage(plugin.getMessage("request-cancelled"));
        }
    }

    public TradeSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    public void removeSession(TradeSession session) {
        if (session != null) {
            activeSessions.remove(session.getPlayer1().getUniqueId());
            activeSessions.remove(session.getPlayer2().getUniqueId());
        }
    }

    public void cancelTrade(Player player) {
        TradeSession session = getSession(player);
        if (session != null) {
            session.cancel();
        }
    }

    public void cancelAllTrades() {
        Set<TradeSession> sessions = new HashSet<>(activeSessions.values());
        for (TradeSession session : sessions) {
            session.cancel();
        }
        activeSessions.clear();
        for (TradeRequest request : pendingRequests.values()) {
            if (request.getExpirationTaskId() != -1) {
                plugin.getServer().getScheduler().cancelTask(request.getExpirationTaskId());
            }
        }
        pendingRequests.clear();
    }

    public boolean isInTrade(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }
    public boolean hasPendingRequest(Player player) {
        return pendingRequests.containsKey(player.getUniqueId());
    }
    public int getActiveTradesCount() {
        return activeSessions.size() / 2;
    }

    public int getPendingRequestsCount() {
        return pendingRequests.size();
    }
    private static class TradeRequest {
        private final Player sender;
        private final Player target;
        private final long timestamp;
        private int expirationTaskId = -1;
        private int formId = -1;

        public TradeRequest(Player sender, Player target) {
            this.sender = sender;
            this.target = target;
            this.timestamp = System.currentTimeMillis();
        }

        public Player getSender() {
            return sender;
        }

        public Player getTarget() {
            return target;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public int getExpirationTaskId() {
            return expirationTaskId;
        }

        public void setExpirationTaskId(int taskId) {
            this.expirationTaskId = taskId;
        }

        public int getFormId() {
            return formId;
        }

        public void setFormId(int formId) {
            this.formId = formId;
        }
    }
}