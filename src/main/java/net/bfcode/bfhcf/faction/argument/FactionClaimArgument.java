package net.bfcode.bfhcf.faction.argument;

import org.bukkit.inventory.PlayerInventory;

import net.bfcode.bfbase.util.command.CommandArgument;
import net.bfcode.bfhcf.HCFaction;
import net.bfcode.bfhcf.faction.claim.ClaimHandler;
import net.bfcode.bfhcf.faction.type.PlayerFaction;

import java.util.UUID;
import org.bukkit.inventory.ItemStack;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class FactionClaimArgument extends CommandArgument
{
    private HCFaction plugin;
    
    public FactionClaimArgument(HCFaction plugin) {
        super("claim", "Claim land in the Wilderness.", new String[] { "claimland" });
        this.plugin = plugin;
    }
    
    public String getUsage(String label) {
        return "/" + label + ' ' + this.getName();
    }
    
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command is only executable by players.");
            return true;
        }
        Player player = (Player)sender;
        UUID uuid = player.getUniqueId();
        PlayerFaction playerFaction = this.plugin.getFactionManager().getPlayerFaction(uuid);
        if (playerFaction == null) {
            sender.sendMessage(ChatColor.RED + "You are not in a faction.");
            return true;
        }
        if (playerFaction.isRaidable()) {
            sender.sendMessage(ChatColor.RED + "You cannot claim land for your faction while raidable.");
            return true;
        }
        PlayerInventory inventory = player.getInventory();
        if (inventory.contains(ClaimHandler.CLAIM_WAND)) {
            sender.sendMessage(ChatColor.RED + "You already have a claiming wand in your inventory.");
            return true;
        }
        if (!inventory.addItem(new ItemStack[] { ClaimHandler.CLAIM_WAND }).isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Your inventory is full.");
            return true;
        }
        sender.sendMessage(ChatColor.YELLOW + "You have recieved a" + ChatColor.LIGHT_PURPLE + " claiming wand" + ChatColor.YELLOW + ". Read the item to understand how to claim. You can also" + ChatColor.YELLOW + " use " + ChatColor.RED + '/' + label + " claimchunk" + ChatColor.YELLOW + '.');
        return true;
    }
}
