package com.morelhaa.trade.session;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntityChest;
import cn.nukkit.inventory.DoubleChestInventory;
import cn.nukkit.item.Item;
import cn.nukkit.level.Sound;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.IntTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.nbt.tag.StringTag;
import com.morelhaa.trade.TradePlugin;
import com.morelhaa.trade.utils.TradeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradeSession {

    private final TradePlugin plugin;
    private final Player player1;
    private final Player player2;
    private DoubleChestInventory inventory;
    private BlockEntityChest chest1;
    private BlockEntityChest chest2;
    private boolean player1Ready;
    private boolean player2Ready;
    private final Map<Integer, Item> player1Items;
    private final Map<Integer, Item> player2Items;
    private boolean completed;
    private boolean cancelled;
    private int completionTaskId = -1;

    public TradeSession(TradePlugin plugin, Player player1, Player player2) {
        this.plugin = plugin;
        this.player1 = player1;
        this.player2 = player2;
        this.player1Ready = false;
        this.player2Ready = false;
        this.player1Items = new HashMap<>();
        this.player2Items = new HashMap<>();
        this.completed = false;
        this.cancelled = false;
    }

    public boolean start() {
        if (!TradeUtils.arePlayersValid(player1, player2)) {
            return false;
        }
        if (!createTradeInventory()) {
            return false;
        }
        setupSeparators();

        player1.addWindow(inventory);
        player2.addWindow(inventory);

        if (plugin.areSoundsEnabled()) {
            TradeUtils.playSoundToPlayers(player1, player2, Sound.RANDOM_ORB);
        }

        player1.sendMessage(plugin.getMessage("trade-started", "{player}", player2.getName()));
        player2.sendMessage(plugin.getMessage("trade-started", "{player}", player1.getName()));

        return true;
    }

    private boolean createTradeInventory() {
        try {
            Vector3 pos1 = player1.getPosition().add(0, -5, 0);
            Vector3 pos2 = pos1.add(1, 0, 0);

            CompoundTag nbt1 = getChestNBT(pos1, "§8Trade con " + player2.getName());
            CompoundTag nbt2 = getChestNBT(pos2, "§8Trade con " + player1.getName());

            chest1 = new BlockEntityChest(player1.getLevel().getChunk((int) pos1.x >> 4, (int) pos1.z >> 4), nbt1);
            chest2 = new BlockEntityChest(player1.getLevel().getChunk((int) pos2.x >> 4, (int) pos2.z >> 4), nbt2);

            inventory = new DoubleChestInventory(chest1, chest2);

            return true;
        } catch (Exception e) {
            plugin.getLogger().error("Error creando inventario de trade: " + e.getMessage());
            return false;
        }
    }

    private CompoundTag getChestNBT(Vector3 pos, String customName) {
        CompoundTag nbt = new CompoundTag()
                .putString("id", BlockEntity.CHEST)
                .putInt("x", (int) pos.x)
                .putInt("y", (int) pos.y)
                .putInt("z", (int) pos.z);

        if (customName != null) {
            nbt.putString("CustomName", customName);
        }

        return nbt;
    }

    private void setupSeparators() {
        Item glass = Item.get(95, 7);
        glass.setCustomName(plugin.getConfig().getString("messages.separator-name", "§7═══════════════"));

        List<Integer> separatorSlots = plugin.getConfig().getIntegerList("gui.separator-slots");
        for (int slot : separatorSlots) {
            inventory.setItem(slot, glass.clone());
        }

        int readySlot = plugin.getReadyButtonSlot();
        updateReadyButton();
    }

    private void updateReadyButton() {
        int readySlot = plugin.getReadyButtonSlot();

        Item button;
        if (!player1Ready && !player2Ready) {
            button = Item.get(95, 14);
            button.setCustomName("§cNinguno listo");
        } else if (player1Ready && player2Ready) {
            button = Item.get(95, 5);
            button.setCustomName("§a¡Ambos listos!");
        } else {
            button = Item.get(95, 4);
            String readyPlayer = player1Ready ? player1.getName() : player2.getName();
            button.setCustomName("§e-" + readyPlayer + " está listo");
        }

        inventory.setItem(readySlot, button);
    }

    public void handleClick(Player clicker, int slot, Item clickedItem) {
        if (!clicker.equals(player1) && !clicker.equals(player2)) {
            return;
        }

        if (TradeUtils.isSeparatorSlot(slot)) {
            return;
        }

        if (slot == plugin.getReadyButtonSlot()) {
            toggleReady(clicker);
            return;
        }

        boolean isPlayer1 = clicker.equals(player1);
        boolean isLeftSide = TradeUtils.isLeftSide(slot);
        boolean isRightSide = TradeUtils.isRightSide(slot);

        if ((isPlayer1 && !isLeftSide) || (!isPlayer1 && !isRightSide)) {
            clicker.sendMessage(plugin.getMessage("trade-cancelled"));
            return;
        }

        if (clickedItem != null && clickedItem.getId() != 0) {
            if (!plugin.getItemBlacklist().canTrade(clickedItem, clicker)) {
                clicker.sendMessage(plugin.getMessage("blacklist-item"));
                if (plugin.areSoundsEnabled()) {
                    TradeUtils.playSound(clicker, Sound.NOTE_BASS);
                }
                return;
            }
        }

        if (isPlayer1 && player1Ready) {
            player1Ready = false;
            player1.sendMessage(plugin.getMessage("trade-not-ready"));
            player2.sendMessage(plugin.getMessage("partner-not-ready", "{player}", player1.getName()));
        } else if (!isPlayer1 && player2Ready) {
            player2Ready = false;
            player2.sendMessage(plugin.getMessage("trade-not-ready"));
            player1.sendMessage(plugin.getMessage("partner-not-ready", "{player}", player2.getName()));
        }

        if (isPlayer1) {
            if (clickedItem == null || clickedItem.getId() == 0) {
                player1Items.remove(slot);
            } else {
                player1Items.put(slot, clickedItem.clone());
            }
        } else {
            if (clickedItem == null || clickedItem.getId() == 0) {
                player2Items.remove(slot);
            } else {
                player2Items.put(slot, clickedItem.clone());
            }
        }

        updateReadyButton();
    }

    private void toggleReady(Player player) {
        boolean isPlayer1 = player.equals(player1);

        if (isPlayer1) {
            player1Ready = !player1Ready;

            if (player1Ready) {
                player.sendMessage(plugin.getMessage("trade-ready"));
                player2.sendMessage(plugin.getMessage("partner-ready", "{player}", player1.getName()));
                if (plugin.areSoundsEnabled()) {
                    TradeUtils.playSound(player, Sound.RANDOM_LEVELUP);
                }
            } else {
                player.sendMessage(plugin.getMessage("trade-not-ready"));
                player2.sendMessage(plugin.getMessage("partner-not-ready", "{player}", player1.getName()));
            }
        } else {
            player2Ready = !player2Ready;

            if (player2Ready) {
                player.sendMessage(plugin.getMessage("trade-ready"));
                player1.sendMessage(plugin.getMessage("partner-ready", "{player}", player2.getName()));
                if (plugin.areSoundsEnabled()) {
                    TradeUtils.playSound(player, Sound.RANDOM_LEVELUP);
                }
            } else {
                player.sendMessage(plugin.getMessage("trade-not-ready"));
                player1.sendMessage(plugin.getMessage("partner-not-ready", "{player}", player2.getName()));
            }
        }

        updateReadyButton();

        if (player1Ready && player2Ready) {
            bothReady();
        } else {
            if (completionTaskId != -1) {
                plugin.getServer().getScheduler().cancelTask(completionTaskId);
                completionTaskId = -1;
            }
        }
    }

    private void bothReady() {
        player1.sendMessage(plugin.getMessage("both-ready"));
        player2.sendMessage(plugin.getMessage("both-ready"));

        if (plugin.areSoundsEnabled()) {
            TradeUtils.playSoundToPlayers(player1, player2, Sound.RANDOM_LEVELUP);
        }

        int delay = plugin.getCompletionDelay();
        completionTaskId = plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> {
            completeTrade();
        }, delay).getTaskId();
    }

    private void completeTrade() {
        if (completed || cancelled) {
            return;
        }

        if (!TradeUtils.arePlayersValid(player1, player2)) {
            cancel();
            return;
        }

        List<Item> items1 = new ArrayList<>(player1Items.values());
        List<Item> items2 = new ArrayList<>(player2Items.values());

        if (!TradeUtils.hasInventorySpace(player1, items2)) {
            player1.sendMessage(plugin.getMessage("inventory-full"));
            player2.sendMessage(plugin.getMessage("partner-inventory-full", "{player}", player1.getName()));
            cancel();
            return;
        }

        if (!TradeUtils.hasInventorySpace(player2, items1)) {
            player2.sendMessage(plugin.getMessage("inventory-full"));
            player1.sendMessage(plugin.getMessage("partner-inventory-full", "{player}", player2.getName()));
            cancel();
            return;
        }

        for (Item item : items1) {
            player1.getInventory().removeItem(item);
        }
        for (Item item : items2) {
            player2.getInventory().removeItem(item);
        }

        TradeUtils.giveItemsSafely(player1, items2);
        TradeUtils.giveItemsSafely(player2, items1);
        player1.sendMessage(plugin.getMessage("trade-completed"));
        player2.sendMessage(plugin.getMessage("trade-completed"));

        if (plugin.areSoundsEnabled()) {
            TradeUtils.playSoundToPlayers(player1, player2, Sound.RANDOM_LEVELUP);
        }

        completed = true;

        player1.removeWindow(inventory);
        player2.removeWindow(inventory);

        plugin.getTradeManager().removeSession(this);
    }

    public void cancel() {
        if (completed || cancelled) {
            return;
        }

        cancelled = true;

        for (Map.Entry<Integer, Item> entry : player1Items.entrySet()) {
            TradeUtils.giveItemsSafely(player1, List.of(entry.getValue()));
        }
        for (Map.Entry<Integer, Item> entry : player2Items.entrySet()) {
            TradeUtils.giveItemsSafely(player2, List.of(entry.getValue()));
        }

        if (player1 != null && player1.isOnline()) {
            player1.removeWindow(inventory);
            player1.sendMessage(plugin.getMessage("trade-cancelled"));
        }
        if (player2 != null && player2.isOnline()) {
            player2.removeWindow(inventory);
            player2.sendMessage(plugin.getMessage("trade-cancelled"));
        }

        if (completionTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(completionTaskId);
        }

        plugin.getTradeManager().removeSession(this);
    }

    public Player getPlayer1() {
        return player1;
    }
    public Player getPlayer2() {
        return player2;
    }
    public Player getOtherPlayer(Player player) {
        return player.equals(player1) ? player2 : player1;
    }
    public boolean involves(Player player) {
        return player.equals(player1) || player.equals(player2);
    }
    public DoubleChestInventory getInventory() {
        return inventory;
    }
    public boolean isCompleted() {
        return completed;
    }
    public boolean isCancelled() {
        return cancelled;
    }
}
