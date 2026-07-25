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
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LobbyPinataPlugin extends JavaPlugin implements CommandExecutor, Listener {

    private Location pinataLocation;
    private ArmorStand nameTag;
    private int maxHits = 50;
    private int currentHits = 50;
    private boolean isSpawned = false;

    // סוג הבלוק שנבחר
    private Material pinataBlockType = Material.GOLD_BLOCK;
    private final Material[] availableBlocks = {Material.GOLD_BLOCK, Material.DIAMOND_BLOCK, Material.NETHERITE_BLOCK, Material.SPONGE, Material.BEACON, Material.HAY_BLOCK};
    private int blockTypeIndex = 0;

    // רשימת הפרסים
    private final List<ItemStack> pinataRewards = new ArrayList<>();
    private final String GUI_TITLE = ChatColor.DARK_PURPLE + "⚙️ ניהול פיניאטה מתקדם";

    @Override
    public void onEnable() {
        if (getCommand("pinata") != null) {
            getCommand("pinata").setExecutor(this);
        }
        getServer().getPluginManager().registerEvents(this, this);

        // פרסים ברירת מחדל
        pinataRewards.add(new ItemStack(Material.DIAMOND, 5));
        pinataRewards.add(new ItemStack(Material.GOLDEN_APPLE, 2));
        pinataRewards.add(new ItemStack(Material.EMERALD, 10));

        getLogger().info("========================================");
        getLogger().info("LobbyPinata Advanced Block GUI Enabled!");
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

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("spawn")) {
                Block targetBlock = player.getLocation().getBlock();
                spawnPinata(targetBlock.getLocation());
                player.sendMessage(ChatColor.GREEN + "🎉 בלוק הפיניאטה נוצר בהצלחה!");
                return true;
            }

            if (args[0].equalsIgnoreCase("remove")) {
                removePinata();
                player.sendMessage(ChatColor.RED + "🗑️ הפיניאטה הוסרה.");
                return true;
            }

            if (args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("menu")) {
                openAdminGUI(player);
                return true;
            }
        }

        player.sendMessage(ChatColor.YELLOW + "שימוש: /pinata spawn | /pinata remove | /pinata gui");
        return true;
    }

    // --- GUI ניהול מתקדם ---
    private void openAdminGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);

        // 1. כפתור שינוי חיים (Hits)
        ItemStack hitsItem = new ItemStack(Material.REDSTONE);
        ItemMeta hitsMeta = hitsItem.getItemMeta();
        if (hitsMeta != null) {
            hitsMeta.setDisplayName(ChatColor.RED + "❤️ חיים: " + ChatColor.YELLOW + maxHits);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "קליק שמאלי: +10");
            lore.add(ChatColor.GRAY + "קליק ימני: -10");
            hitsMeta.setLore(lore);
            hitsItem.setItemMeta(hitsMeta);
        }
        gui.setItem(2, hitsItem);

        // 2. כפתור בחירת מראה בלוק הפיניאטה
        ItemStack blockItem = new ItemStack(pinataBlockType);
        ItemMeta blockMeta = blockItem.getItemMeta();
        if (blockMeta != null) {
            blockMeta.setDisplayName(ChatColor.GOLD + "🧊 סוג/סגנון הבלוק: " + ChatColor.AQUA + pinataBlockType.name());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "לחץ כדי להחליף את עיצוב הבלוק של הפיניאטה!");
            blockMeta.setLore(lore);
            blockItem.setItemMeta(blockMeta);
        }
        gui.setItem(6, blockItem);

        // 3. כפתור הסבר על הפרסים והכמויות
        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "📦 הגדרת חפצים וכמויות");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "שים חפצים/קוסמטיקס במשבצות למטה.");
            lore.add(ChatColor.GREEN + "כמות החפץ בבלוק (Amount) = כמה יצאו מהפיניאטה!");
            infoMeta.setLore(lore);
            infoItem.setItemMeta(infoMeta);
        }
        gui.setItem(0, infoItem);

        // טעינת הפרסים
        int slot = 18;
        for (ItemStack reward : pinataRewards) {
            if (slot < 54) {
                gui.setItem(slot++, reward.clone());
            }
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onGUIClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;

        int slot = event.getRawSlot();

        // שינוי חיים
        if (slot == 2) {
            event.setCancelled(true);
            if (event.isLeftClick()) {
                maxHits += 10;
            } else if (event.isRightClick() && maxHits > 10) {
                maxHits -= 10;
            }
            openAdminGUI((Player) event.getWhoClicked());
            return;
        }

        // שינוי סוג הבלוק
        if (slot == 6) {
            event.setCancelled(true);
            blockTypeIndex = (blockTypeIndex + 1) % availableBlocks.length;
            pinataBlockType = availableBlocks[blockTypeIndex];
            if (isSpawned && pinataLocation != null) {
                pinataLocation.getBlock().setType(pinataBlockType);
            }
            openAdminGUI((Player) event.getWhoClicked());
            return;
        }

        // נעילת השורה העליונה
        if (slot < 18) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onGUIClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;

        pinataRewards.clear();
        Inventory inv = event.getInventory();
        for (int i = 18; i < 54; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                pinataRewards.add(item.clone());
            }
        }
        event.getPlayer().sendMessage(ChatColor.GREEN + "✅ הגדרות הפיניאטה, העיצוב והכמויות נשמרו!");
    }

    // --- מנגנון הפיניאטה ---
    private void spawnPinata(Location loc) {
        removePinata();

        pinataLocation = loc.getBlock().getLocation();
        currentHits = maxHits;
        isSpawned = true;

        pinataLocation.getBlock().setType(pinataBlockType);

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

        if (event.getClickedBlock() != null && event.getClickedBlock().getLocation().equals(pinataLocation)) {
            if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);

                currentHits--;

                Player player = event.getPlayer();
                Location loc = pinataLocation.clone().add(0.5, 1.0, 0.5);

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
        if (isSpawned && pinataLocation != null && event.getBlock().getLocation().equals(pinataLocation)) {
            event.setCancelled(true);
        }
    }

    private void breakPinata(Player lastHitPlayer) {
        Location loc = pinataLocation.clone().add(0.5, 1.0, 0.5);

        loc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, loc, 1);
        loc.getWorld().spawnParticle(Particle.TOTEM, loc, 100, 0.5, 0.5, 0.5, 0.2);
        loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        Random random = new Random();
        if (!pinataRewards.isEmpty()) {
            for (ItemStack reward : pinataRewards) {
                // פיזור כמות החפצים המדויקת שנקבעה ב-GUI לכל חפץ!
                int amountToDrop = reward.getAmount();
                for (int i = 0; i < amountToDrop; i++) {
                    ItemStack singleItem = reward.clone();
                    singleItem.setAmount(1);
                    Item droppedItem = loc.getWorld().dropItem(loc, singleItem);

                    // זריקה ברדיוס של 10 בלוקים בקשת
                    double angle = random.nextDouble() * 2 * Math.PI;
                    double speed = 0.5 + random.nextDouble() * 0.8;
                    double x = Math.cos(angle) * speed;
                    double z = Math.sin(angle) * speed;
                    double y = 0.4 + random.nextDouble() * 0.3;

                    droppedItem.setVelocity(new Vector(x, y, z));
                }
            }
        }

        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "========================================");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "🎉 " + ChatColor.BOLD + "פיניאטת הלובי נשברה על ידי " + ChatColor.GREEN + lastHitPlayer.getName() + ChatColor.YELLOW + "!");
        Bukkit.broadcastMessage(ChatColor.AQUA + "💎 הפרסים והקוסמטיקס פוזרו ברדיוס 10 בלוקים!");
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
