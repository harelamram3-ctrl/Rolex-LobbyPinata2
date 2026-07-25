package com.example.lobbypinata;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class LobbyPinataPlugin extends JavaPlugin implements CommandExecutor, Listener {

    private Llama pinataEntity;
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
        getLogger().info("LobbyPinata Plugin Enabled!");
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
            spawnPinata(player.getLocation());
            player.sendMessage(ChatColor.GREEN + "🎉 הפיניאטה שוגרה בהצלחה במיקום שלך!");
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

        currentHits = maxHits;
        isSpawned = true;

        pinataEntity = (Llama) loc.getWorld().spawnEntity(loc, EntityType.LLAMA);
        pinataEntity.setCustomNameVisible(false);
        pinataEntity.setAI(false);
        pinataEntity.setInvulnerable(false);
        pinataEntity.setColor(Llama.Color.CREAMY);

        Location hologramLoc = loc.clone().add(0, 2.2, 0);
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
    public void onPinataHit(EntityDamageByEntityEvent event) {
        if (!isSpawned || pinataEntity == null) return;

        if (event.getEntity().getUniqueId().equals(pinataEntity.getUniqueId())) {
            event.setCancelled(true);

            if (event.getDamager() instanceof Player damager) {
                currentHits--;

                Location loc = pinataEntity.getLocation().add(0, 1, 0);
                loc.getWorld().spawnParticle(Particle.FIREWORK_EXPLOSION, loc, 15, 0.3, 0.3, 0.3, 0.05);
                loc.getWorld().playSound(loc, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.2f);

                updateHologram();

                if (currentHits <= 0) {
                    breakPinata(damager);
                }
            }
        }
    }

    @EventHandler
    public void onPinataDamage(EntityDamageEvent event) {
        if (isSpawned && pinataEntity != null && event.getEntity().getUniqueId().equals(pinataEntity.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void breakPinata(Player lastHitPlayer) {
        Location loc = pinataEntity.getLocation().add(0, 1, 0);

        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 100, 0.5, 0.5, 0.5, 0.2);
        loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        Random random = new Random();
        Material[] rewards = {Material.DIAMOND, Material.GOLD_INGOT, Material.IRON_INGOT, Material.EMERALD};

        for (int i = 0; i < 20; i++) {
            Material reward = rewards[random.nextInt(rewards.length)];
            ItemStack item = new ItemStack(reward, random.nextInt(3) + 1);
            loc.getWorld().dropItem(loc, item);
        }

        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "========================================");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "🎉 " + ChatColor.BOLD + "פיניאטת הלובי נשברה על ידי " + ChatColor.GREEN + lastHitPlayer.getName() + ChatColor.YELLOW + "!");
        Bukkit.broadcastMessage(ChatColor.AQUA + "💎 הפרסים פוזרו ברחבי הלובי, קחו אותם מהר!");
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "========================================");

        removePinata();
    }

    private void removePinata() {
        isSpawned = false;
        if (pinataEntity != null) {
            pinataEntity.remove();
            pinataEntity = null;
        }
        if (nameTag != null) {
            nameTag.remove();
            nameTag = null;
        }
    }
}
