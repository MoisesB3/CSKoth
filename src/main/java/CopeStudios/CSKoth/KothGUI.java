package CopeStudios.CSKoth;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KothGUI implements Listener {
    private final CSKoth plugin;
    private final Map<UUID, String> editingPhysicalRewards = new HashMap<>();

    public KothGUI(CSKoth plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Main KOTH Menu
        if (title.equals(ChatColor.GOLD + "CSKoth Menu")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Create KOTH")) {
                player.closeInventory();
                player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Enter the name for your new KOTH in chat:");
                // Store that player is creating a KOTH
                plugin.getCreatingKoth().put(player.getUniqueId(), "naming");
            } else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.AQUA + "List KOTHs")) {
                player.closeInventory();
                openKothListMenu(player);
            } else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.RED + "Close")) {
                player.closeInventory();
            }
            return;
        }

        // KOTH List Menu
        if (title.equals(ChatColor.GOLD + "KOTH List")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            // Back button
            if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.RED + "Back")) {
                player.closeInventory();
                openMainMenu(player);
                return;
            }

            // Check if clicked on a KOTH
            String kothName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            if (plugin.getKothZones().containsKey(kothName)) {
                player.closeInventory();
                openKothManageMenu(player, kothName);
            }
            return;
        }

        // KOTH Manage Menu
        if (title.startsWith(ChatColor.GOLD + "Manage KOTH: ")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            String kothName = title.substring((ChatColor.GOLD + "Manage KOTH: ").length());

            if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Start KOTH")) {
                player.closeInventory();
                plugin.startKoth(player, kothName);
            } else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.RED + "Stop KOTH")) {
                player.closeInventory();
                plugin.stopKoth(player, kothName);
            } else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.YELLOW + "Delete KOTH")) {
                player.closeInventory();
                plugin.deleteKoth(player, kothName);
            } else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.AQUA + "Edit Settings")) {
                player.closeInventory();
                openKothSettingsMenu(player, kothName);
            } else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Rewards")) {
                player.closeInventory();
                openRewardsMenu(player, kothName);
            } else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.LIGHT_PURPLE + "Schedule")) {
                player.closeInventory();
                openScheduleMenu(player, kothName);
            } else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.RED + "Back")) {
                player.closeInventory();
                openKothListMenu(player);
            }
            return;
        }

        // KOTH Settings Menu
        if (title.startsWith(ChatColor.GOLD + "KOTH Settings: ")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            String kothName = title.substring((ChatColor.GOLD + "KOTH Settings: ").length());

            if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.YELLOW + "Change Capture Time")) {
                player.closeInventory();
                player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Enter the new capture time in seconds:");
                plugin.getCreatingKoth().put(player.getUniqueId(), "capture_time:" + kothName);
            } else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.YELLOW + "Change Points")) {
                player.closeInventory();
                player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Enter the new points value:");
                plugin.getCreatingKoth().put(player.getUniqueId(), "points:" + kothName);
            } else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.RED + "Back")) {
                player.closeInventory();
                openKothManageMenu(player, kothName);
            }
            return;
        }

        // KOTH Rewards Menu
        if (title.startsWith(ChatColor.GOLD + "KOTH Rewards: ")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            String kothName = title.substring((ChatColor.GOLD + "KOTH Rewards: ").length());

            if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.YELLOW + "Physical Rewards")) {
                player.closeInventory();
                openPhysicalRewardsMenu(player, kothName);
            } else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.YELLOW + "Command Rewards")) {
                player.closeInventory();
                openCommandRewardsMenu(player, kothName);
            } else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.RED + "Back")) {
                player.closeInventory();
                openKothManageMenu(player, kothName);
            }
            return;
        }

        // Physical Rewards Menu
        if (title.startsWith(ChatColor.GOLD + "Physical Rewards: ")) {
            // Allow inventory interaction for setting rewards
            String kothName = title.substring((ChatColor.GOLD + "Physical Rewards: ").length());

            // Only block clicks on the bottom row (save/back buttons)
            if (event.getSlot() >= 45) {
                event.setCancelled(true);

                if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                    return;
                }

                if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Save Rewards")) {
                    // Save the rewards
                    List<ItemStack> rewards = new ArrayList<>();
                    for (int i = 0; i < 45; i++) {
                        ItemStack item = event.getInventory().getItem(i);
                        if (item != null && item.getType() != Material.AIR) {
                            rewards.add(item);
                        }
                    }

                    plugin.savePhysicalRewards(kothName, rewards);
                    player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Physical rewards saved!");

                    player.closeInventory();
                    openRewardsMenu(player, kothName);
                } else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.RED + "Back")) {
                    player.closeInventory();
                    openRewardsMenu(player, kothName);
                }
            }

            // Track the player's editing session
            editingPhysicalRewards.put(player.getUniqueId(), kothName);
            return;
        }

        // Command Rewards Menu
        if (title.startsWith(ChatColor.GOLD + "Command Rewards: ")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            String kothName = title.substring((ChatColor.GOLD + "Command Rewards: ").length());

            if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Add Command")) {
                player.closeInventory();
                player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Enter a command (without the / prefix):");
                plugin.getCreatingKoth().put(player.getUniqueId(), "add_command:" + kothName);
            } else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.RED + "Back")) {
                player.closeInventory();
                openRewardsMenu(player, kothName);
            } else if (event.getCurrentItem().getType() == Material.WRITTEN_BOOK) {
                // Remove the command if right-clicked
                if (event.isRightClick()) {
                    String displayName = event.getCurrentItem().getItemMeta().getDisplayName();
                    if (displayName.startsWith(ChatColor.YELLOW + "Command ")) {
                        String numStr = displayName.substring((ChatColor.YELLOW + "Command ").length());
                        try {
                            int index = Integer.parseInt(numStr) - 1;
                            plugin.removeCommandReward(kothName, index);
                            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Command removed!");
                            openCommandRewardsMenu(player, kothName);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
            return;
        }

        // Schedule Menu
        if (title.startsWith(ChatColor.GOLD + "KOTH Schedule: ")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            String kothName = title.substring((ChatColor.GOLD + "KOTH Schedule: ").length());
            KothZone kothZone = plugin.getKothZones().get(kothName);

            if (event.getCurrentItem().getItemMeta().getDisplayName().startsWith(ChatColor.YELLOW + "Scheduled:")) {
                plugin.toggleScheduled(kothName);
                openScheduleMenu(player, kothName);
            } else if (event.getCurrentItem().getItemMeta().getDisplayName().startsWith(ChatColor.YELLOW + "Always On:")) {
                plugin.toggleAlwaysOn(kothName);
                openScheduleMenu(player, kothName);
            } else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.YELLOW + "Start Time")) {
                player.closeInventory();
                player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Enter the start hour (0-23):");
                plugin.getCreatingKoth().put(player.getUniqueId(), "start_hour:" + kothName);
            } else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.YELLOW + "Duration (minutes)")) {
                player.closeInventory();
                player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Enter the duration in minutes:");
                plugin.getCreatingKoth().put(player.getUniqueId(), "duration:" + kothName);
            } else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.YELLOW + "Respawn Delay (minutes)")) {
                player.closeInventory();
                player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Enter the respawn delay in minutes:");
                plugin.getCreatingKoth().put(player.getUniqueId(), "respawn_delay:" + kothName);
            } else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.RED + "Back")) {
                player.closeInventory();
                openKothManageMenu(player, kothName);
            }
            return;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (editingPhysicalRewards.containsKey(player.getUniqueId())) {
            String kothName = editingPhysicalRewards.get(player.getUniqueId());
            editingPhysicalRewards.remove(player.getUniqueId());

            // Safety check to prevent processing when opening another menu
            if (!event.getView().getTitle().startsWith(ChatColor.GOLD + "Physical Rewards: ")) {
                return;
            }

            // Auto-save the rewards on close
            List<ItemStack> rewards = new ArrayList<>();
            for (int i = 0; i < 45; i++) {
                ItemStack item = event.getInventory().getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    rewards.add(item);
                }
            }

            plugin.savePhysicalRewards(kothName, rewards);
            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Physical rewards saved on close!");
        }
    }

    public void openMainMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, ChatColor.GOLD + "CSKoth Menu");

        // Create KOTH button
        ItemStack createItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta createMeta = createItem.getItemMeta();
        createMeta.setDisplayName(ChatColor.GREEN + "Create KOTH");
        List<String> createLore = new ArrayList<>();
        createLore.add(ChatColor.GRAY + "Create a new King of the Hill zone");
        createMeta.setLore(createLore);
        createItem.setItemMeta(createMeta);
        menu.setItem(11, createItem);

        // List KOTHs button
        ItemStack listItem = new ItemStack(Material.BOOK);
        ItemMeta listMeta = listItem.getItemMeta();
        listMeta.setDisplayName(ChatColor.AQUA + "List KOTHs");
        List<String> listLore = new ArrayList<>();
        listLore.add(ChatColor.GRAY + "View and manage your KOTH zones");
        listMeta.setLore(listLore);
        listItem.setItemMeta(listMeta);
        menu.setItem(13, listItem);

        // Close button
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Close");
        closeItem.setItemMeta(closeMeta);
        menu.setItem(15, closeItem);

        // Filler
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < 27; i++) {
            if (menu.getItem(i) == null) {
                menu.setItem(i, filler);
            }
        }

        player.openInventory(menu);
    }

    public void openKothListMenu(Player player) {
        int size = (int) Math.ceil(plugin.getKothZones().size() / 9.0) * 9 + 9;
        size = Math.max(27, Math.min(54, size)); // Min 27, max 54

        Inventory menu = Bukkit.createInventory(null, size, ChatColor.GOLD + "KOTH List");

        int slot = 0;
        for (String kothName : plugin.getKothZones().keySet()) {
            if (slot >= size - 9) break; // Leave last row for navigation

            ItemStack kothItem;
            if (plugin.getActiveKoths().containsKey(kothName)) {
                kothItem = new ItemStack(Material.EMERALD_BLOCK);
            } else {
                kothItem = new ItemStack(Material.REDSTONE_BLOCK);
            }

            ItemMeta kothMeta = kothItem.getItemMeta();
            kothMeta.setDisplayName(ChatColor.GOLD + kothName);
            List<String> kothLore = new ArrayList<>();
            kothLore.add(ChatColor.GRAY + "Status: " +
                    (plugin.getActiveKoths().containsKey(kothName) ? ChatColor.GREEN + "Active" : ChatColor.RED + "Inactive"));
            kothLore.add(ChatColor.GRAY + "Capture Time: " + ChatColor.YELLOW + plugin.getKothZones().get(kothName).getCaptureTime() + "s");
            kothLore.add(ChatColor.GRAY + "Points: " + ChatColor.YELLOW + plugin.getKothZones().get(kothName).getCapturePoints());
            kothLore.add("");
            kothLore.add(ChatColor.YELLOW + "Click to manage");
            kothMeta.setLore(kothLore);
            kothItem.setItemMeta(kothMeta);

            menu.setItem(slot, kothItem);
            slot++;
        }

        // Back button in the middle of the last row
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back");
        backItem.setItemMeta(backMeta);
        menu.setItem(size - 5, backItem);

        // Filler
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < size; i++) {
            if (menu.getItem(i) == null) {
                menu.setItem(i, filler);
            }
        }

        player.openInventory(menu);
    }

    public void openKothManageMenu(Player player, String kothName) {
        Inventory menu = Bukkit.createInventory(null, 36, ChatColor.GOLD + "Manage KOTH: " + kothName);

        KothZone kothZone = plugin.getKothZones().get(kothName);
        boolean isActive = plugin.getActiveKoths().containsKey(kothName);

        // Start/Stop button
        ItemStack toggleItem;
        if (isActive) {
            toggleItem = new ItemStack(Material.REDSTONE_BLOCK);
            ItemMeta toggleMeta = toggleItem.getItemMeta();
            toggleMeta.setDisplayName(ChatColor.RED + "Stop KOTH");
            List<String> toggleLore = new ArrayList<>();
            toggleLore.add(ChatColor.GRAY + "Stop this KOTH event");
            toggleMeta.setLore(toggleLore);
            toggleItem.setItemMeta(toggleMeta);
        } else {
            toggleItem = new ItemStack(Material.EMERALD_BLOCK);
            ItemMeta toggleMeta = toggleItem.getItemMeta();
            toggleMeta.setDisplayName(ChatColor.GREEN + "Start KOTH");
            List<String> toggleLore = new ArrayList<>();
            toggleLore.add(ChatColor.GRAY + "Start this KOTH event");
            toggleMeta.setLore(toggleLore);
            toggleItem.setItemMeta(toggleMeta);
        }
        menu.setItem(10, toggleItem);

        // Edit Settings button
        ItemStack settingsItem = new ItemStack(Material.COMPARATOR);
        ItemMeta settingsMeta = settingsItem.getItemMeta();
        settingsMeta.setDisplayName(ChatColor.AQUA + "Edit Settings");
        List<String> settingsLore = new ArrayList<>();
        settingsLore.add(ChatColor.GRAY + "Change capture time, points, etc.");
        settingsMeta.setLore(settingsLore);
        settingsItem.setItemMeta(settingsMeta);
        menu.setItem(12, settingsItem);

        // Rewards button
        ItemStack rewardsItem = new ItemStack(Material.CHEST);
        ItemMeta rewardsMeta = rewardsItem.getItemMeta();
        rewardsMeta.setDisplayName(ChatColor.GOLD + "Rewards");
        List<String> rewardsLore = new ArrayList<>();
        rewardsLore.add(ChatColor.GRAY + "Set rewards for capturing");
        rewardsMeta.setLore(rewardsLore);
        rewardsItem.setItemMeta(rewardsMeta);
        menu.setItem(14, rewardsItem);

        // Schedule button
        ItemStack scheduleItem = new ItemStack(Material.CLOCK);
        ItemMeta scheduleMeta = scheduleItem.getItemMeta();
        scheduleMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Schedule");
        List<String> scheduleLore = new ArrayList<>();
        scheduleLore.add(ChatColor.GRAY + "Set automatic scheduling");
        scheduleMeta.setLore(scheduleLore);
        scheduleItem.setItemMeta(scheduleMeta);
        menu.setItem(16, scheduleItem);

        // Delete button
        ItemStack deleteItem = new ItemStack(Material.TNT);
        ItemMeta deleteMeta = deleteItem.getItemMeta();
        deleteMeta.setDisplayName(ChatColor.YELLOW + "Delete KOTH");
        List<String> deleteLore = new ArrayList<>();
        deleteLore.add(ChatColor.GRAY + "Remove this KOTH zone");
        deleteMeta.setLore(deleteLore);
        deleteItem.setItemMeta(deleteMeta);
        menu.setItem(22, deleteItem);

        // Back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back");
        backItem.setItemMeta(backMeta);
        menu.setItem(31, backItem);

        // Filler
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < 36; i++) {
            if (menu.getItem(i) == null) {
                menu.setItem(i, filler);
            }
        }

        player.openInventory(menu);
    }

    public void openKothSettingsMenu(Player player, String kothName) {
        Inventory menu = Bukkit.createInventory(null, 27, ChatColor.GOLD + "KOTH Settings: " + kothName);

        KothZone kothZone = plugin.getKothZones().get(kothName);

        // Capture Time button
        ItemStack timeItem = new ItemStack(Material.CLOCK);
        ItemMeta timeMeta = timeItem.getItemMeta();
        timeMeta.setDisplayName(ChatColor.YELLOW + "Change Capture Time");
        List<String> timeLore = new ArrayList<>();
        timeLore.add(ChatColor.GRAY + "Current: " + ChatColor.GOLD + kothZone.getCaptureTime() + " seconds");
        timeLore.add(ChatColor.GRAY + "Click to change");
        timeMeta.setLore(timeLore);
        timeItem.setItemMeta(timeMeta);
        menu.setItem(11, timeItem);

        // Points button
        ItemStack pointsItem = new ItemStack(Material.DIAMOND);
        ItemMeta pointsMeta = pointsItem.getItemMeta();
        pointsMeta.setDisplayName(ChatColor.YELLOW + "Change Points");
        List<String> pointsLore = new ArrayList<>();
        pointsLore.add(ChatColor.GRAY + "Current: " + ChatColor.GOLD + kothZone.getCapturePoints() + " points");
        pointsLore.add(ChatColor.GRAY + "Click to change");
        pointsMeta.setLore(pointsLore);
        pointsItem.setItemMeta(pointsMeta);
        menu.setItem(15, pointsItem);

        // Back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back");
        backItem.setItemMeta(backMeta);
        menu.setItem(22, backItem);

        // Filler
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < 27; i++) {
            if (menu.getItem(i) == null) {
                menu.setItem(i, filler);
            }
        }

        player.openInventory(menu);
    }

    public void openRewardsMenu(Player player, String kothName) {
        Inventory menu = Bukkit.createInventory(null, 27, ChatColor.GOLD + "KOTH Rewards: " + kothName);

        // Physical rewards button
        ItemStack physicalItem = new ItemStack(Material.CHEST);
        ItemMeta physicalMeta = physicalItem.getItemMeta();
        physicalMeta.setDisplayName(ChatColor.YELLOW + "Physical Rewards");
        List<String> physicalLore = new ArrayList<>();
        physicalLore.add(ChatColor.GRAY + "Set items to give to the winner");
        physicalMeta.setLore(physicalLore);
        physicalItem.setItemMeta(physicalMeta);
        menu.setItem(11, physicalItem);

        // Command rewards button
        ItemStack commandItem = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta commandMeta = commandItem.getItemMeta();
        commandMeta.setDisplayName(ChatColor.YELLOW + "Command Rewards");
        List<String> commandLore = new ArrayList<>();
        commandLore.add(ChatColor.GRAY + "Set commands to execute when captured");
        commandMeta.setLore(commandLore);
        commandItem.setItemMeta(commandMeta);
        menu.setItem(15, commandItem);

        // Back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back");
        backItem.setItemMeta(backMeta);
        menu.setItem(22, backItem);

        // Filler
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < 27; i++) {
            if (menu.getItem(i) == null) {
                menu.setItem(i, filler);
            }
        }

        player.openInventory(menu);
    }

    public void openPhysicalRewardsMenu(Player player, String kothName) {
        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.GOLD + "Physical Rewards: " + kothName);

        // Load existing rewards
        KothZone kothZone = plugin.getKothZones().get(kothName);
        List<ItemStack> rewards = kothZone.getPhysicalRewards();

        // Add existing rewards to inventory
        if (rewards != null) {
            for (int i = 0; i < Math.min(rewards.size(), 45); i++) {
                menu.setItem(i, rewards.get(i));
            }
        }

        // Save button
        ItemStack saveItem = new ItemStack(Material.EMERALD);
        ItemMeta saveMeta = saveItem.getItemMeta();
        saveMeta.setDisplayName(ChatColor.GREEN + "Save Rewards");
        saveItem.setItemMeta(saveMeta);
        menu.setItem(49, saveItem);

        // Back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back");
        backItem.setItemMeta(backMeta);
        menu.setItem(45, backItem);

        player.openInventory(menu);
    }

    public void openCommandRewardsMenu(Player player, String kothName) {
        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.GOLD + "Command Rewards: " + kothName);

        // Load existing command rewards
        KothZone kothZone = plugin.getKothZones().get(kothName);
        List<String> commands = kothZone.getCommandRewards();

        // Add commands as book items
        if (commands != null) {
            for (int i = 0; i < Math.min(commands.size(), 45); i++) {
                String cmd = commands.get(i);
                ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
                BookMeta meta = (BookMeta) book.getItemMeta();
                meta.setTitle("Command " + (i + 1));
                meta.setAuthor("CSKoth");
                meta.addPage(cmd);
                meta.setDisplayName(ChatColor.YELLOW + "Command " + (i + 1));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + cmd);
                lore.add(ChatColor.RED + "Right-click to remove");
                meta.setLore(lore);
                book.setItemMeta(meta);
                menu.setItem(i, book);
            }
        }

        // Add command button
        ItemStack addItem = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta addMeta = addItem.getItemMeta();
        addMeta.setDisplayName(ChatColor.GREEN + "Add Command");
        addItem.setItemMeta(addMeta);
        menu.setItem(49, addItem);

        // Back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back");
        backItem.setItemMeta(backMeta);
        menu.setItem(45, backItem);

        player.openInventory(menu);
    }

    public void openScheduleMenu(Player player, String kothName) {
        Inventory menu = Bukkit.createInventory(null, 36, ChatColor.GOLD + "KOTH Schedule: " + kothName);

        KothZone kothZone = plugin.getKothZones().get(kothName);

        // Scheduled toggle
        ItemStack scheduledItem = new ItemStack(kothZone.isScheduled() ? Material.LIME_WOOL : Material.RED_WOOL);
        ItemMeta scheduledMeta = scheduledItem.getItemMeta();
        scheduledMeta.setDisplayName(ChatColor.YELLOW + "Scheduled: " +
                (kothZone.isScheduled() ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
        scheduledItem.setItemMeta(scheduledMeta);
        menu.setItem(10, scheduledItem);

        // Start time
        ItemStack timeItem = new ItemStack(Material.CLOCK);
        ItemMeta timeMeta = timeItem.getItemMeta();
        timeMeta.setDisplayName(ChatColor.YELLOW + "Start Time");
        List<String> timeLore = new ArrayList<>();
        if (kothZone.getStartHour() >= 0 && kothZone.getStartMinute() >= 0) {
            String formattedHour = String.format("%02d", kothZone.getStartHour());
            String formattedMinute = String.format("%02d", kothZone.getStartMinute());
            timeLore.add(ChatColor.GRAY + "Current: " + ChatColor.GOLD + formattedHour + ":" + formattedMinute);
        } else {
            timeLore.add(ChatColor.GRAY + "Current: Not set");
        }
        timeLore.add(ChatColor.GRAY + "Click to change");
        timeMeta.setLore(timeLore);
        timeItem.setItemMeta(timeMeta);
        menu.setItem(12, timeItem);

        // Duration
        ItemStack durationItem = new ItemStack(Material.CLOCK);
        ItemMeta durationMeta = durationItem.getItemMeta();
        durationMeta.setDisplayName(ChatColor.YELLOW + "Duration (minutes)");
        List<String> durationLore = new ArrayList<>();
        durationLore.add(ChatColor.GRAY + "Current: " + ChatColor.GOLD + kothZone.getDurationMinutes() + " minutes");
        durationLore.add(ChatColor.GRAY + "Click to change");
        durationMeta.setLore(durationLore);
        durationItem.setItemMeta(durationMeta);
        menu.setItem(14, durationItem);

        // Always on toggle
        ItemStack alwaysOnItem = new ItemStack(kothZone.isAlwaysOn() ? Material.LIME_WOOL : Material.RED_WOOL);
        ItemMeta alwaysOnMeta = alwaysOnItem.getItemMeta();
        alwaysOnMeta.setDisplayName(ChatColor.YELLOW + "Always On: " +
                (kothZone.isAlwaysOn() ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
        alwaysOnItem.setItemMeta(alwaysOnMeta);
        menu.setItem(16, alwaysOnItem);

        // Respawn delay
        ItemStack delayItem = new ItemStack(Material.REPEATER);
        ItemMeta delayMeta = delayItem.getItemMeta();
        delayMeta.setDisplayName(ChatColor.YELLOW + "Respawn Delay (minutes)");
        List<String> delayLore = new ArrayList<>();
        delayLore.add(ChatColor.GRAY + "Current: " + ChatColor.GOLD + kothZone.getRespawnDelay() + " minutes");
        delayLore.add(ChatColor.GRAY + "Time between captures when Always On");
        delayLore.add(ChatColor.GRAY + "Click to change");
        delayMeta.setLore(delayLore);
        delayItem.setItemMeta(delayMeta);
        menu.setItem(22, delayItem);

        // Back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back");
        backItem.setItemMeta(backMeta);
        menu.setItem(31, backItem);

        // Filler
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < 36; i++) {
            if (menu.getItem(i) == null) {
                menu.setItem(i, filler);
            }
        }

        player.openInventory(menu);
    }
}