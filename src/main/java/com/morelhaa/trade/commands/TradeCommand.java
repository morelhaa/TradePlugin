package com.morelhaa.trade.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import com.morelhaa.trade.TradePlugin;

public class TradeCommand extends Command {

    private final TradePlugin plugin;

    public TradeCommand(TradePlugin plugin) {
        super("trade", "Enviar solicitud de intercambio a otro jugador", "/trade <jugador>", new String[]{"intercambio", "comercio"});
        this.plugin = plugin;
        this.setPermission("trade.use");
        this.commandParameters.clear();
        this.commandParameters.put("default", new CommandParameter[]{
                new CommandParameter("jugador", CommandParamType.TARGET, false)
        });
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando solo puede ser ejecutado por jugadores.");
            return false;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("trade.use")) {
            player.sendMessage(plugin.getMessage("no-permission"));
            return false;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.getMessage("prefix") + " §eUso: §f/trade <jugador>");
            return false;
        }

        String targetName = args[0];
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            player.sendMessage(plugin.getMessage("player-not-found"));
            return false;
        }

        if (player.equals(target)) {
            player.sendMessage(plugin.getMessage("cannot-trade-yourself"));
            return false;
        }

        plugin.getTradeManager().sendRequest(player, target);

        return true;
    }
}