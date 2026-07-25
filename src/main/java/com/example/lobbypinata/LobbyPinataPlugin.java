package com.example.lobbypinata;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class LobbyPinataPlugin extends JavaPlugin implements CommandExecutor, Listener {

    private Location pinataLocation;
    private ArmorStand nameTag;
    private final int maxHits = 50;
    private int currentHits = 50;
    private boolean isSpawned = false;

    @Override
    public void onEnable() {
        if (getCommand("pinata") != null) {
            getCommand("pinata").setExecutor(this);
        }
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("========================================");
        getLogger().info("LobbyPinata Block-Plugin Enabled!");
        getLogger().info("Created by: BadPanda14");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        removePinata();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!player.hasPermission("pinata.admin")) {
            player.sendMessage(ChatColor.RED + "אין לך הרשאה להשתמש בפקודה זו!");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("spawn")) {
            // הופך את הבלוק שהשחקן עומד עליו לבלוק פיניאטה
            Block targetBlock = player.getLocation().getBlock();
            spawnPinata(targetBlock.getLocation());
            player.sendMessage(ChatColor.GREEN + "🎉 בלוק הפיניאטה נוצר בהצלחה במיקום שלך!");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("remove")) {
            removePinata();
            player.sendMessage(ChatColor.RED + "🗑️ הפיניאטה הוסרה.");
            return true;
        }

        player.sendMessage(ChatColor.YELLOW + "שימוש: /pinata spawn  או  /pinata remove");
        return true;
    }

    private void spawnPinata(Location loc) {
        removePinata();

        pinataLocation = loc.getBlock().getLocation();
        currentHits = maxHits;
        isSpawned = true;

        // שינוי הבלוק לבלוק פיניאטה מיוחד (למשל Gold Block)
        pinataLocation.getBlock().setType(Material.GOLD_BLOCK);

        // יצירת הולוגרמה מעל הבלוק
        Location hologramLoc = pinataLocation.clone().add(0.5, 1.2, 0.5);
        nameTag = (ArmorStand) loc.getWorld().spawnEntity(hologramLoc, EntityType.ARMOR_STAND);
        nameTag.setGravity(false);
        nameTag.setVisible(false);
        nameTag.setCustomNameVisible(true);
        nameTag.setMarker(true);

        updateHologram();
    }

    private void updateHologram() {
        if (nameTag != null && isSpawned) {
            String progressBar = getProgressBar(currentHits, maxHits);
            nameTag.setCustomName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "🎈 פיניאטת הלובי 🎈 " 
                    + ChatColor.WHITE + "[" + progressBar + ChatColor.WHITE + "] " 
                    + ChatColor.YELLOW + currentHits + "/" + maxHits);
        }
    }

    private String getProgressBar(int current, int max) {
        int totalBars = 10;
        int activeBars = (int) (((double) current / max) * totalBars);
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < totalBars; i++) {
            if (i < activeBars) {
                builder.append(ChatColor.GREEN).append("█");
            } else {
                builder.append(ChatColor.GRAY).append("█");
            }
        }
        return builder.toString();
    }

    @EventHandler
    public void onBlockHit(PlayerInteractEvent event) {
        if (!isSpawned || pinataLocation == null) return;

        // בדיקה אם השחקן לחץ/הרביץ לבלוק הפיניאטה
        if (event.getClickedBlock() != null && event.getClickedBlock().getLocation().equals(pinataLocation)) {
            if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);

                currentHits--;

                Player player = event.getPlayer();
                Location loc = pinataLocation.clone().add(0.5, 1.0, 0.5);

                // אפקטים וצלילים
                loc.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, loc, 15, 0.3, 0.3, 0.3, 0.05);
                loc.getWorld().playSound(loc, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.2f);

                updateHologram();

                if (currentHits <= 0) {
                    breakPinata(player);
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // מניעת שבירת הבלוק באופן רגיל
        if (isSpawned && pinataLocation != null && event.getBlock().getLocation().equals(pinataLocation)) {
            event.setCancelled(true);
        }
    }

    private void breakPinata(Player lastHitPlayer) {
        Location loc = pinataLocation.clone().add(0.5, 1.0, 0.5);

        // אפקטי פיצוץ
        loc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, loc, 1);
        loc.getWorld().spawnParticle(Particle.TOTEM, loc, 100, 0.5, 0.5, 0.5, 0.2);
        loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // פיזור פרסים
        Random random = new Random();
        Material[] rewards = {Material.DIAMOND, Material.GOLD_INGOT, Material.IRON_INGOT, Material.EMERALD};

        for (int i = 0; i < 20; i++) {
            Material reward = rewards[random.nextInt(rewards.length)];
            ItemStack item = new ItemStack(reward, random.nextInt(3) + 1);
            loc.getWorld().dropItem(loc, item);
        }

        // הודעה בצ'אט
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "========================================");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "🎉 " + ChatColor.BOLD + "פיניאטת הלובי נשברה על ידי " + ChatColor.GREEN + lastHitPlayer.getName() + ChatColor.YELLOW + "!");
        Bukkit.broadcastMessage(ChatColor.AQUA + "💎 הפרסים פוזרו ברחבי הלובי, קחו אותם מהר!");
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "========================================");

        removePinata();
    }

    private void removePinata() {
        isSpawned = false;
        if (pinataLocation != null) {
            pinataLocation.getBlock().setType(Material.AIR);
            pinataLocation = null;
        }
        if (nameTag != null) {
            nameTag.remove();
            nameTag = null;
        }
    }
}
