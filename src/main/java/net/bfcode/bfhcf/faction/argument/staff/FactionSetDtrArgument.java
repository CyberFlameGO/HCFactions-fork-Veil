package net.bfcode.bfhcf.faction.argument.staff;

import org.bukkit.Bukkit;
import java.util.ArrayList;
import java.util.Collections;
import org.bukkit.entity.Player;
import java.util.List;

import net.bfcode.bfbase.util.command.CommandArgument;
import net.bfcode.bfhcf.HCFaction;
import net.bfcode.bfhcf.faction.type.Faction;
import net.bfcode.bfhcf.faction.type.PlayerFaction;
import net.bfcode.bfhcf.utils.JavaUtil;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class FactionSetDtrArgument extends CommandArgument
{
    private HCFaction plugin;
    
    public FactionSetDtrArgument(HCFaction plugin) {
        super("setdtr", "Sets the DTR of a faction.");
        this.plugin = plugin;
        this.permission = "hcf.command.faction.argument." + this.getName();
    }
    
    public String getUsage(String label) {
        return "/" + label + ' ' + this.getName() + " <playerName|factionName> <newDtr>";
    }
    
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Incorrect usage!" + ChatColor.YELLOW + " Use like this: " + ChatColor.AQUA + this.getUsage(label));
            return true;
        }
        Double newDTR = JavaUtil.tryParseDouble(args[2]);
        if (newDTR == null) {
            String s = args[2];
            switch (s) {
                case "-i": {
                    Faction factionincrease = this.plugin.getFactionManager().getContainingFaction(args[1]);
                    PlayerFaction playerFactionincrease = (PlayerFaction)factionincrease;
                    double previousDtr = playerFactionincrease.getDeathsUntilRaidable();
                    playerFactionincrease.setDeathsUntilRaidable(previousDtr + 1.0);
                    sender.sendMessage(ChatColor.YELLOW + "You have increased the DTR of " + playerFactionincrease.getName() + " by 1.");
                    return true;
                }
                case "-d": {
                    Faction factiondecrearse = this.plugin.getFactionManager().getContainingFaction(args[1]);
                    PlayerFaction playerFactiondecrearse = (PlayerFaction)factiondecrearse;
                    double previousDtrdecrearse = playerFactiondecrearse.getDeathsUntilRaidable();
                    playerFactiondecrearse.setDeathsUntilRaidable(previousDtrdecrearse - 1.0);
                    sender.sendMessage(ChatColor.YELLOW + "You have decreased the DTR of " + playerFactiondecrearse.getName() + " by 1.");
                    return true;
                }
                default: {
                    sender.sendMessage(ChatColor.RED + "'" + args[2] + "' is not a valid number.");
                    return true;
                }
            }
        }
        else {
            if (args[1].equalsIgnoreCase("all")) {
                for (Faction faction : this.plugin.getFactionManager().getFactions()) {
                    if (!(faction instanceof PlayerFaction)) {
                        continue;
                    }
                    ((PlayerFaction)faction).setDeathsUntilRaidable(newDTR);
                }
                Command.broadcastCommandMessage(sender, ChatColor.YELLOW + "Set DTR of all factions to " + newDTR + '.');
                return true;
            }
            Faction faction2 = this.plugin.getFactionManager().getContainingFaction(args[1]);
            if (faction2 == null) {
                sender.sendMessage(ChatColor.RED + "Faction named or containing member with IGN or UUID " + args[1] + " not found.");
                return true;
            }
            if (!(faction2 instanceof PlayerFaction)) {
                sender.sendMessage(ChatColor.RED + "You can only set DTR of player factions.");
                return true;
            }
            PlayerFaction playerFaction = (PlayerFaction)faction2;
            double previousDtr2 = playerFaction.getDeathsUntilRaidable();
            newDTR = playerFaction.setDeathsUntilRaidable(newDTR);
            Command.broadcastCommandMessage(sender, ChatColor.YELLOW + "Set DTR of " + faction2.getName() + " from " + previousDtr2 + " to " + newDTR + '.');
            return true;
        }
    }
    
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 2 || !(sender instanceof Player)) {
            return Collections.emptyList();
        }
        if (args[1].isEmpty()) {
            return null;
        }
        Player player = (Player)sender;
        ArrayList<String> results = new ArrayList<String>(this.plugin.getFactionManager().getFactionNameMap().keySet());
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (player.canSee(target)) {
                if (results.contains(target.getName())) {
                    continue;
                }
                results.add(target.getName());
            }
        }
        return results;
    }
}
