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

    // הגדרות בלוק וגודל
    private ItemStack customPinataBlock = new ItemStack(Material.GOLD_BLOCK);
    private int pinataSize = 1; // 1 = 1x1, 2 = 2x2, 3 = 3x3
    private final List<Location> spawnedBlocksLocations = new ArrayList<>();

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
        getLogger().info("LobbyPinata Custom Block & Size Plugin Enabled!");
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
                player.sendMessage(ChatColor.GREEN + "🎉 פיניאטה בגודל " + pinataSize + "x" + pinataSize + " נוצרה בהצלחה!");
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

    // --- GUI ניהול ---
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

        // 2. משבצת לשים את הבלוק מהאינונטורי שלך!
        ItemStack blockDisplay = customPinataBlock.clone();
        ItemMeta blockMeta = blockDisplay.getItemMeta();
        if (blockMeta != null) {
            blockMeta.setDisplayName(ChatColor.GOLD + "🧊 הבלוק של הפיניאטה");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "גרור לכאן בלוק מהאינונטורי שלך!");
            lore.add(ChatColor.GRAY + "הפיניאטה תיבנה מהבלוק הזה.");
            blockMeta.setLore(lore);
            blockDisplay.setItemMeta(blockMeta);
        }
        gui.setItem(4, blockDisplay);

        // 3. כפתור שינוי גודל הפיניאטה (Size)
        ItemStack sizeItem = new ItemStack(Material.OAK_LOG);
        ItemMeta sizeMeta = sizeItem.getItemMeta();
        if (sizeMeta != null) {
            sizeMeta.setDisplayName(ChatColor.AQUA + "📐 גודל פיזי: " + ChatColor.YELLOW + pinataSize + "x" + pinataSize);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "לחץ כדי להחליף גודל:");
            lore.add(ChatColor.GRAY + "• 1x1 (רגיל)");
            lore.add(ChatColor.GRAY + "• 2x2 (בינוני)");
            lore.add(ChatColor.GRAY + "• 3x3 (ענק)");
            sizeMeta.setLore(lore);
            sizeItem.setItemMeta(sizeMeta);
        }
        gui.setItem(6, sizeItem);

        // 4. הסבר פרסים
        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "📦 הגדרת חפצים ופרסים");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "שים חפצים/קוסמטיקס במשבצות למטה.");
            lore.add(ChatColor.GREEN + "כמות החפץ (Amount) = כמה עפים מהפיניאטה!");
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

        // שינוי גודל פיניאטה
        if (slot == 6) {
            event.setCancelled(true);
            pinataSize = (pinataSize % 3) + 1; // עובר בין 1, 2, 3
            openAdminGUI((Player) event.getWhoClicked());
            return;
        }

        // נעילת כפתורי ההסבר
        if (slot == 0) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onGUIClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;

        Inventory inv = event.getInventory();

        // שמירת הבלוק שנבחר מ-slot 4
        ItemStack placedBlock = inv.getItem(4);
        if (placedBlock != null && placedBlock.getType().isBlock() && placedBlock.getType() != Material.AIR) {
            customPinataBlock = placedBlock.clone();
            customPinataBlock.setAmount(1);
        }

        // שמירת הפרסים
        pinataRewards.clear();
        for (int i = 18; i < 54; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                pinataRewards.add(item.clone());
            }
        }

        event.getPlayer().sendMessage(ChatColor.GREEN + "✅ הבלוק, הגודל (x" + pinataSize + ") והפרסים נשמרו בהצלחה!");
    }

    // --- מנגנון הפיניאטה ---
    private void spawnPinata(Location loc) {
        removePinata();

        pinataLocation = loc.getBlock().getLocation();
        currentHits = maxHits;
        isSpawned = true;

        Material blockType = customPinataBlock.getType();

        // יצירת מראה פיזי לפי הגודל שנבחר (1x1, 2x2, 3x3)
        spawnedBlocksLocations.clear();
        for (int x = 0; x < pinataSize; x++) {
            for (int y = 0; y < pinataSize; y++) {
                for (int z = 0; z < pinataSize; z++) {
                    Location bLoc = pinataLocation.clone().add(x, y, z);
                    bLoc.getBlock().setType(blockType);
                    spawnedBlocksLocations.add(bLoc);
                }
            }
        }

        // מיקום ההולוגרמה מעל מרכז הפיניאטה
        double offset = (double) pinataSize / 2.0;
        Location hologramLoc = pinataLocation.clone().add(offset, pinataSize + 0.5, offset);
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
        if (!isSpawned || spawnedBlocksLocations.isEmpty()) return;

        Block clicked = event.getClickedBlock();
        if (clicked != null && spawnedBlocksLocations.contains(clicked.getLocation())) {
            if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);

                currentHits--;

                Player player = event.getPlayer();
                Location loc = clicked.getLocation().add(0.5, 0.5, 0.5);

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
        if (isSpawned && spawnedBlocksLocations.contains(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    private void breakPinata(Player lastHitPlayer) {
        double offset = (double) pinataSize / 2.0;
        Location loc = pinataLocation.clone().add(offset, pinataSize / 2.0, offset);

        loc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, loc, 2);
        loc.getWorld().spawnParticle(Particle.TOTEM, loc, 120, 0.8, 0.8, 0.8, 0.2);
        loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        Random random = new Random();
        if (!pinataRewards.isEmpty()) {
            for (ItemStack reward : pinataRewards) {
                int amountToDrop = reward.getAmount();
                for (int i = 0; i < amountToDrop; i++) {
                    ItemStack singleItem = reward.clone();
                    singleItem.setAmount(1);
                    Item droppedItem = loc.getWorld().dropItem(loc, singleItem);

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
        for (Location bLoc : spawnedBlocksLocations) {
            bLoc.getBlock().setType(Material.AIR);
        }
        spawnedBlocksLocations.clear();

        if (nameTag != null) {
            nameTag.remove();
            nameTag = null;
        }
    }
}
