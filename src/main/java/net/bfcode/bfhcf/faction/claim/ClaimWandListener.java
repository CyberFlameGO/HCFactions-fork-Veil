package net.bfcode.bfhcf.faction.claim;

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.entity.Item;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.block.Block;

import java.util.UUID;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.ArrayList;

import org.bukkit.Location;

import com.google.common.base.Predicate;

import net.bfcode.bfhcf.HCFaction;
import net.bfcode.bfhcf.faction.type.PlayerFaction;
import net.bfcode.bfhcf.visualise.VisualBlock;
import net.bfcode.bfhcf.visualise.VisualType;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.Listener;

public class ClaimWandListener implements Listener
{
    private HCFaction plugin;
    
    public ClaimWandListener(HCFaction plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action == Action.PHYSICAL || !event.hasItem() || !this.isClaimingWand(event.getItem())) {
            return;
        }
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (action == Action.RIGHT_CLICK_AIR) {
            this.plugin.getClaimHandler().clearClaimSelection(player);
            player.setItemInHand(new ItemStack(Material.AIR, 1));
            player.sendMessage(ChatColor.RED + "You have cleared your claim selection.");
            return;
        }
        PlayerFaction playerFaction = this.plugin.getFactionManager().getPlayerFaction(uuid);
        if ((action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) || !player.isSneaking()) {
            if (action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK) {
                Block block = event.getClickedBlock();
                Location blockLocation = block.getLocation();
                if (action == Action.RIGHT_CLICK_BLOCK) {
                    event.setCancelled(true);
                }
                if (this.plugin.getClaimHandler().canClaimHere(player, blockLocation)) {
                    ClaimSelection revert = new ClaimSelection(blockLocation.getWorld());
                    ClaimSelection claimSelection = (ClaimSelection) this.plugin.getClaimHandler().claimSelectionMap.putIfAbsent(uuid, revert);
                    if (claimSelection == null) {
                        claimSelection = revert;
                    }
                    Location oldPosition = null;
                    Location opposite = null;
                    int selectionId = 0;
                    switch (action) {
                        case LEFT_CLICK_BLOCK: {
                            oldPosition = claimSelection.getPos1();
                            opposite = claimSelection.getPos2();
                            selectionId = 1;
                            break;
                        }
                        case RIGHT_CLICK_BLOCK: {
                            oldPosition = claimSelection.getPos2();
                            opposite = claimSelection.getPos1();
                            selectionId = 2;
                            break;
                        }
                        default: {
                            return;
                        }
                    }
                    int blockX = blockLocation.getBlockX();
                    int blockZ = blockLocation.getBlockZ();
                    if (oldPosition != null && blockX == oldPosition.getBlockX() && blockZ == oldPosition.getBlockZ()) {
                        return;
                    }
                    if (System.currentTimeMillis() - claimSelection.getLastUpdateMillis() <= 200L) {
                        return;
                    }
                    if (opposite != null) {
                        int xDiff = Math.abs(opposite.getBlockX() - blockX) + 1;
                        int zDiff = Math.abs(opposite.getBlockZ() - blockZ) + 1;
                        if (xDiff < 5 || zDiff < 5) {
                            player.sendMessage(ChatColor.RED + "Claim selections must be at least " + 5 + 'x' + 5 + " blocks.");
                            return;
                        }
                    }
                    if (oldPosition != null) {
                        Location finalOldPosition = oldPosition;
                        this.plugin.getVisualiseHandler().clearVisualBlocks(player, VisualType.CREATE_CLAIM_SELECTION, (Predicate<VisualBlock>)new Predicate<VisualBlock>() {
                            public boolean apply(VisualBlock visualBlock) {
                                Location location = visualBlock.getLocation();
                                return location.getBlockX() == finalOldPosition.getBlockX() && location.getBlockZ() == finalOldPosition.getBlockZ();
                            }
                        });
                    }
                    if (selectionId == 1) {
                        claimSelection.setPos1(blockLocation);
                    }
                    if (selectionId == 2) {
                        claimSelection.setPos2(blockLocation);
                    }
                    player.sendMessage(ChatColor.GREEN + "Set the location of claim selection " + ChatColor.YELLOW + selectionId + ChatColor.GREEN + " to: " + ChatColor.GOLD + '(' + ChatColor.YELLOW + blockX + ", " + blockZ + ChatColor.GOLD + ')');
                    if (claimSelection.hasBothPositionsSet()) {
                        Claim claim = claimSelection.toClaim(playerFaction);
                        int selectionPrice;
                        player.sendMessage(ChatColor.AQUA + "Claim selection cost: " + (((selectionPrice = claimSelection.getPrice(playerFaction, false)) > playerFaction.getBalance()) ? ChatColor.RED : ChatColor.GREEN) + '$' + selectionPrice + ChatColor.AQUA + ". Current size: (" + ChatColor.WHITE + claim.getWidth() + ", " + claim.getLength() + ChatColor.AQUA + "), " + ChatColor.WHITE + claim.getArea() + ChatColor.AQUA + " blocks.");
                    }
                    int blockY = block.getY();
                    int maxHeight = player.getWorld().getMaxHeight();
                    ArrayList<Location> locations = new ArrayList<Location>(maxHeight);
                    for (int i = blockY; i < maxHeight; ++i) {
                        Location other = blockLocation.clone();
                        other.setY((double)i);
                        locations.add(other);
                    }
                    new BukkitRunnable() {
                        public void run() {
                            ClaimWandListener.this.plugin.getVisualiseHandler().generate(player, locations, VisualType.CREATE_CLAIM_SELECTION, true);
                        }
                    }.runTask((Plugin)this.plugin);
                }
            }
            return;
        }
        ClaimSelection claimSelection2 = (ClaimSelection) this.plugin.getClaimHandler().claimSelectionMap.get(uuid);
        if (claimSelection2 == null || !claimSelection2.hasBothPositionsSet()) {
            player.sendMessage(ChatColor.RED + "You have not set both positions of this claim selection.");
            return;
        }
        if (this.plugin.getClaimHandler().tryPurchasing(player, claimSelection2.toClaim(playerFaction))) {
            this.plugin.getClaimHandler().clearClaimSelection(player);
            player.setItemInHand(new ItemStack(Material.AIR, 1));
        }
    }
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        if (this.isClaimingWand(event.getPlayer().getItemInHand())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.NORMAL)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Player player;
        if (event.getDamager() instanceof Player && this.isClaimingWand((player = (Player)event.getDamager()).getItemInHand())) {
            player.setItemInHand(new ItemStack(Material.AIR, 1));
            this.plugin.getClaimHandler().clearClaimSelection(player);
        }
    }
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.NORMAL)
    public void onPlayerKick(PlayerKickEvent event) {
        event.getPlayer().getInventory().remove(ClaimHandler.CLAIM_WAND);
    }
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.getPlayer().getInventory().remove(ClaimHandler.CLAIM_WAND);
    }
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.NORMAL)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        Item item = event.getItemDrop();
        if (this.isClaimingWand(item.getItemStack())) {
            item.remove();
            this.plugin.getClaimHandler().clearClaimSelection(event.getPlayer());
        }
    }
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.NORMAL)
    public void onPlayerPickup(PlayerPickupItemEvent event) {
        Item item = event.getItem();
        if (this.isClaimingWand(item.getItemStack())) {
            item.remove();
            this.plugin.getClaimHandler().clearClaimSelection(event.getPlayer());
        }
    }
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getDrops().remove(ClaimHandler.CLAIM_WAND)) {
            this.plugin.getClaimHandler().clearClaimSelection(event.getEntity());
        }
    }
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.NORMAL)
    public void onInventoryOpen(InventoryOpenEvent event) {
        HumanEntity humanEntity = event.getPlayer();
        if (humanEntity instanceof Player) {
            Player player = (Player)humanEntity;
            player.getInventory().remove(ClaimHandler.CLAIM_WAND);
            this.plugin.getClaimHandler().clearClaimSelection(player);
        }
    }
    
    public boolean isClaimingWand(ItemStack stack) {
        return stack != null && stack.isSimilar(ClaimHandler.CLAIM_WAND);
    }
}
