package CopeStudios.CSKoth;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CSKoth extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private FileConfiguration kothsConfig;
    private File kothsFile;

    private final Map<String, KothZone> kothZones = new HashMap<>();
    private final Map<String, BukkitTask> activeKoths = new HashMap<>();
    private final Map<String, Map<UUID, Integer>> capturePoints = new HashMap<>();
    private final Map<UUID, Location> pos1Selection = new HashMap<>();
    private final Map<UUID, Location> pos2Selection = new HashMap<>();
    private final Map<UUID, String> creatingKoth = new HashMap<>();
    private final Map<String, Long> lastCaptureTimes = new HashMap<>();
    private KothStatsManager statsManager;

    private final String prefix = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "CSKoth" + ChatColor.DARK_GRAY + "] " + ChatColor.RESET;
    private CSPlaceholders placeholders;

    public CSPlaceholders getPlaceholders() {
        return placeholders;
    }

    private KothGUI gui;

    @Override
    public void onEnable() {
        // Create data folder and files first
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        saveDefaultConfig();
        config = getConfig();

        // Create koths.yml file path
        kothsFile = new File(getDataFolder(), "koths.yml");
        if (!kothsFile.exists()) {
            try {
                kothsFile.createNewFile();
                kothsConfig = new YamlConfiguration();
                kothsConfig.save(kothsFile);
            } catch (IOException e) {
                getLogger().severe("Could not create koths.yml: " + e.getMessage());
            }
        }
        kothsConfig = YamlConfiguration.loadConfiguration(kothsFile);

        // Initialize message manager (before other components)
        messageManager = new MessageManager(this);

        // Initialize placeholders
        placeholders = new CSPlaceholders(this);
        if (placeholders.register()) {
            getLogger().info("CSKoth Placeholders registered successfully!");
        } else {
            getLogger().warning("Failed to register CSKoth Placeholders!");
        }

        // Initialize stats manager
        statsManager = new KothStatsManager(this);

        // Initialize GUI handler
        gui = new KothGUI(this);

        // Rest of your initialization code...
    }

    @Override
    public void onDisable() {
        // Cancel all running Koth tasks
        for (BukkitTask task : activeKoths.values()) {
            task.cancel();
        }
        activeKoths.clear();

        // Save all koths to config
        saveKoths();

        // Save stats
        statsManager.saveStats();

        getLogger().info("CSKoth has been disabled!");
    }

    private void checkScheduledKoths() {
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);

        for (Map.Entry<String, KothZone> entry : kothZones.entrySet()) {
            String kothName = entry.getKey();
            KothZone kothZone = entry.getValue();

            // Skip if already active
            if (activeKoths.containsKey(kothName)) {
                continue;
            }

            // Handle always on KOTHs
            if (kothZone.isAlwaysOn()) {
                // Check if it's been long enough since last capture
                Long lastCaptureTime = lastCaptureTimes.get(kothName);
                if (lastCaptureTime == null ||
                        (System.currentTimeMillis() - lastCaptureTime) >= kothZone.getRespawnDelay() * 60 * 1000) {
                    // Start this KOTH
                    KothRunnable kothRunnable = new KothRunnable(this, kothName, kothZone);
                    BukkitTask task = kothRunnable.runTaskTimer(this, 0, 20);
                    activeKoths.put(kothName, task);

                    Bukkit.broadcastMessage(prefix + ChatColor.YELLOW + "KOTH " + ChatColor.GOLD + kothName +
                            ChatColor.YELLOW + " has automatically started!");
                }
                continue;
            }

            // Handle scheduled KOTHs
            if (kothZone.isScheduled() &&
                    kothZone.getStartHour() == currentHour &&
                    kothZone.getStartMinute() == currentMinute) {

                // Start this KOTH
                KothRunnable kothRunnable = new KothRunnable(this, kothName, kothZone);
                BukkitTask task = kothRunnable.runTaskTimer(this, 0, 20);
                activeKoths.put(kothName, task);

                // Schedule auto-stop after duration
                final String finalKothName = kothName;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (activeKoths.containsKey(finalKothName)) {
                            activeKoths.get(finalKothName).cancel();
                            activeKoths.remove(finalKothName);
                            Bukkit.broadcastMessage(prefix + ChatColor.YELLOW + "KOTH " + ChatColor.GOLD + finalKothName +
                                    ChatColor.YELLOW + " has automatically ended.");
                        }
                    }
                }.runTaskLater(this, 20 * 60 * kothZone.getDurationMinutes());

                Bukkit.broadcastMessage(prefix + ChatColor.YELLOW + "KOTH " + ChatColor.GOLD + kothName +
                        ChatColor.YELLOW + " has automatically started!");
            }
        }
    }

    private void loadKoths() {
        if (kothsConfig.contains("koths")) {
            ConfigurationSection kothsSection = kothsConfig.getConfigurationSection("koths");
            if (kothsSection != null) {
                for (String kothName : kothsSection.getKeys(false)) {
                    ConfigurationSection kothSection = kothsSection.getConfigurationSection(kothName);
                    if (kothSection != null) {
                        String worldName = kothSection.getString("world");
                        World world = Bukkit.getWorld(worldName);
                        if (world != null) {
                            int x1 = kothSection.getInt("x1");
                            int y1 = kothSection.getInt("y1");
                            int z1 = kothSection.getInt("z1");
                            int x2 = kothSection.getInt("x2");
                            int y2 = kothSection.getInt("y2");
                            int z2 = kothSection.getInt("z2");
                            int captureTime = kothSection.getInt("captureTime", 300);
                            int capturePoints = kothSection.getInt("capturePoints", 1);

                            Location corner1 = new Location(world, x1, y1, z1);
                            Location corner2 = new Location(world, x2, y2, z2);

                            KothZone kothZone = new KothZone(kothName, corner1, corner2, captureTime, capturePoints);

                            // Load scheduling details
                            if (kothSection.contains("scheduled")) {
                                kothZone.setScheduled(kothSection.getBoolean("scheduled"));
                            }
                            if (kothSection.contains("startHour")) {
                                kothZone.setStartHour(kothSection.getInt("startHour"));
                            }
                            if (kothSection.contains("startMinute")) {
                                kothZone.setStartMinute(kothSection.getInt("startMinute"));
                            }
                            if (kothSection.contains("durationMinutes")) {
                                kothZone.setDurationMinutes(kothSection.getInt("durationMinutes"));
                            }
                            if (kothSection.contains("alwaysOn")) {
                                kothZone.setAlwaysOn(kothSection.getBoolean("alwaysOn"));
                            }
                            if (kothSection.contains("respawnDelay")) {
                                kothZone.setRespawnDelay(kothSection.getInt("respawnDelay"));
                            }
                            // Load command rewards
                            if (kothSection.contains("commandRewards")) {
                                List<String> commandRewards = kothSection.getStringList("commandRewards");
                                kothZone.setCommandRewards(commandRewards);
                            }

                            // Load physical rewards if they exist
                            if (kothSection.contains("physicalRewards")) {
                                ConfigurationSection rewardsSection = kothSection.getConfigurationSection("physicalRewards");
                                if (rewardsSection != null) {
                                    List<ItemStack> rewards = new ArrayList<>();
                                    for (String key : rewardsSection.getKeys(false)) {
                                        rewards.add(rewardsSection.getItemStack(key));
                                    }
                                    kothZone.setPhysicalRewards(rewards);
                                }
                            }

                            kothZones.put(kothName, kothZone);
                            getLogger().info("Loaded KOTH: " + kothName);
                        }
                    }
                }
            }
        }
    }

    public void saveKoths() {
        kothsConfig.set("koths", null);
        for (Map.Entry<String, KothZone> entry : kothZones.entrySet()) {
            String kothName = entry.getKey();
            KothZone kothZone = entry.getValue();

            kothsConfig.set("koths." + kothName + ".world", kothZone.getCorner1().getWorld().getName());
            kothsConfig.set("koths." + kothName + ".x1", kothZone.getCorner1().getBlockX());
            kothsConfig.set("koths." + kothName + ".y1", kothZone.getCorner1().getBlockY());
            kothsConfig.set("koths." + kothName + ".z1", kothZone.getCorner1().getBlockZ());
            kothsConfig.set("koths." + kothName + ".x2", kothZone.getCorner2().getBlockX());
            kothsConfig.set("koths." + kothName + ".y2", kothZone.getCorner2().getBlockY());
            kothsConfig.set("koths." + kothName + ".z2", kothZone.getCorner2().getBlockZ());
            kothsConfig.set("koths." + kothName + ".captureTime", kothZone.getCaptureTime());
            kothsConfig.set("koths." + kothName + ".capturePoints", kothZone.getCapturePoints());

            // Save scheduling details
            kothsConfig.set("koths." + kothName + ".scheduled", kothZone.isScheduled());
            kothsConfig.set("koths." + kothName + ".startHour", kothZone.getStartHour());
            kothsConfig.set("koths." + kothName + ".startMinute", kothZone.getStartMinute());
            kothsConfig.set("koths." + kothName + ".durationMinutes", kothZone.getDurationMinutes());
            kothsConfig.set("koths." + kothName + ".alwaysOn", kothZone.isAlwaysOn());
            kothsConfig.set("koths." + kothName + ".respawnDelay", kothZone.getRespawnDelay());

            // Save command rewards
            kothsConfig.set("koths." + kothName + ".commandRewards", kothZone.getCommandRewards());

            // Save physical rewards
            List<ItemStack> physicalRewards = kothZone.getPhysicalRewards();
            if (physicalRewards != null && !physicalRewards.isEmpty()) {
                for (int i = 0; i < physicalRewards.size(); i++) {
                    kothsConfig.set("koths." + kothName + ".physicalRewards." + i, physicalRewards.get(i));
                }
            }
        }

        try {
            kothsConfig.save(kothsFile);
        } catch (IOException e) {
            getLogger().severe("Could not save koths to koths.yml: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only process if it's a block movement, not just looking around
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        // Check if player is selecting a KOTH zone
        if (pos1Selection.containsKey(player.getUniqueId()) || pos2Selection.containsKey(player.getUniqueId())) {
            return; // Skip KOTH tracking during selection mode
        }

        // Check if player is in any active KOTH zone
        for (Map.Entry<String, KothZone> entry : kothZones.entrySet()) {
            String kothName = entry.getKey();
            KothZone kothZone = entry.getValue();

            // Only process for active KOTHs
            if (!activeKoths.containsKey(kothName)) {
                continue;
            }

            // If player is in this KOTH
            if (kothZone.isInside(event.getTo())) {
                // Initialize this KOTH's points map if needed
                if (!capturePoints.containsKey(kothName)) {
                    capturePoints.put(kothName, new HashMap<>());
                }

                // Make sure player is in the points tracking
                Map<UUID, Integer> kothPointsMap = capturePoints.get(kothName);
                if (!kothPointsMap.containsKey(player.getUniqueId())) {
                    kothPointsMap.put(player.getUniqueId(), 0);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Check if the player is setting up a KOTH
        if (creatingKoth.containsKey(playerUUID)) {
            event.setCancelled(true); // Cancel the message from being broadcast
            String action = creatingKoth.get(playerUUID);

            if (action.equals("naming")) {
                String kothName = event.getMessage();
                createKoth(player, kothName);
                creatingKoth.remove(playerUUID);
                player.sendMessage(prefix + ChatColor.GREEN + "KOTH " + ChatColor.GOLD + kothName +
                        ChatColor.GREEN + " has been created successfully!");
            }
            else if (action.startsWith("capture_time:")) {
                String kothName = action.substring("capture_time:".length());
                KothZone kothZone = kothZones.get(kothName);

                try {
                    int captureTime = Integer.parseInt(event.getMessage());
                    if (captureTime <= 0) {
                        player.sendMessage(prefix + ChatColor.RED + "Capture time must be greater than 0.");
                        return;
                    }

                    kothZone.setCaptureTime(captureTime);
                    saveKoths();
                    player.sendMessage(prefix + ChatColor.GREEN + "Capture time for " + ChatColor.GOLD + kothName +
                            ChatColor.GREEN + " has been set to " + ChatColor.YELLOW + captureTime + ChatColor.GREEN + " seconds.");

                    // Schedule reopening the settings menu on the main thread
                    Bukkit.getScheduler().runTask(this, () -> {
                        gui.openKothSettingsMenu(player, kothName);
                    });
                } catch (NumberFormatException e) {
                    player.sendMessage(prefix + ChatColor.RED + "Please enter a valid number.");
                }

                creatingKoth.remove(playerUUID);
            }
            else if (action.startsWith("points:")) {
                String kothName = action.substring("points:".length());
                KothZone kothZone = kothZones.get(kothName);

                try {
                    int points = Integer.parseInt(event.getMessage());
                    if (points <= 0) {
                        player.sendMessage(prefix + ChatColor.RED + "Points must be greater than 0.");
                        return;
                    }

                    kothZone.setCapturePoints(points);
                    saveKoths();
                    player.sendMessage(prefix + ChatColor.GREEN + "Points for " + ChatColor.GOLD + kothName +
                            ChatColor.GREEN + " has been set to " + ChatColor.YELLOW + points + ChatColor.GREEN + ".");

                    // Schedule reopening the settings menu on the main thread
                    Bukkit.getScheduler().runTask(this, () -> {
                        gui.openKothSettingsMenu(player, kothName);
                    });
                } catch (NumberFormatException e) {
                    player.sendMessage(prefix + ChatColor.RED + "Please enter a valid number.");
                }

                creatingKoth.remove(playerUUID);
            }
            else if (action.startsWith("start_hour:")) {
                String kothName = action.substring("start_hour:".length());
                KothZone kothZone = kothZones.get(kothName);

                try {
                    int hour = Integer.parseInt(event.getMessage());
                    if (hour < 0 || hour > 23) {
                        player.sendMessage(prefix + ChatColor.RED + "Hour must be between 0 and 23.");
                        return;
                    }

                    kothZone.setStartHour(hour);
                    saveKoths();
                    player.sendMessage(prefix + ChatColor.GREEN + "Start hour for " + ChatColor.GOLD + kothName +
                            ChatColor.GREEN + " has been set to " + ChatColor.YELLOW + hour + ChatColor.GREEN + ".");

                    // Ask for minutes now
                    player.sendMessage(prefix + ChatColor.YELLOW + "Now enter the start minute (0-59):");
                    creatingKoth.put(playerUUID, "start_minute:" + kothName);
                } catch (NumberFormatException e) {
                    player.sendMessage(prefix + ChatColor.RED + "Please enter a valid number.");
                    creatingKoth.remove(playerUUID);
                }
            }
            else if (action.startsWith("start_minute:")) {
                String kothName = action.substring("start_minute:".length());
                KothZone kothZone = kothZones.get(kothName);

                try {
                    int minute = Integer.parseInt(event.getMessage());
                    if (minute < 0 || minute > 59) {
                        player.sendMessage(prefix + ChatColor.RED + "Minute must be between 0 and 59.");
                        return;
                    }

                    kothZone.setStartMinute(minute);
                    saveKoths();

                    String formattedHour = String.format("%02d", kothZone.getStartHour());
                    String formattedMinute = String.format("%02d", minute);
                    player.sendMessage(prefix + ChatColor.GREEN + "Start time for " + ChatColor.GOLD + kothName +
                            ChatColor.GREEN + " has been set to " + ChatColor.YELLOW + formattedHour + ":" +
                            formattedMinute + ChatColor.GREEN + ".");

                    // Schedule reopening the schedule menu on the main thread
                    Bukkit.getScheduler().runTask(this, () -> {
                        gui.openScheduleMenu(player, kothName);
                    });
                } catch (NumberFormatException e) {
                    player.sendMessage(prefix + ChatColor.RED + "Please enter a valid number.");
                }

                creatingKoth.remove(playerUUID);
            }
            else if (action.startsWith("duration:")) {
                String kothName = action.substring("duration:".length());
                KothZone kothZone = kothZones.get(kothName);

                try {
                    int duration = Integer.parseInt(event.getMessage());
                    if (duration <= 0) {
                        player.sendMessage(prefix + ChatColor.RED + "Duration must be greater than 0.");
                        return;
                    }

                    kothZone.setDurationMinutes(duration);
                    saveKoths();
                    player.sendMessage(prefix + ChatColor.GREEN + "Duration for " + ChatColor.GOLD + kothName +
                            ChatColor.GREEN + " has been set to " + ChatColor.YELLOW + duration + ChatColor.GREEN + " minutes.");

                    // Schedule reopening the schedule menu on the main thread
                    Bukkit.getScheduler().runTask(this, () -> {
                        gui.openScheduleMenu(player, kothName);
                    });
                } catch (NumberFormatException e) {
                    player.sendMessage(prefix + ChatColor.RED + "Please enter a valid number.");
                }

                creatingKoth.remove(playerUUID);
            }
            else if (action.startsWith("respawn_delay:")) {
                String kothName = action.substring("respawn_delay:".length());
                KothZone kothZone = kothZones.get(kothName);

                try {
                    int delay = Integer.parseInt(event.getMessage());
                    if (delay <= 0) {
                        player.sendMessage(prefix + ChatColor.RED + "Delay must be greater than 0.");
                        return;
                    }

                    kothZone.setRespawnDelay(delay);
                    saveKoths();
                    player.sendMessage(prefix + ChatColor.GREEN + "Respawn delay for " + ChatColor.GOLD + kothName +
                            ChatColor.GREEN + " has been set to " + ChatColor.YELLOW + delay + ChatColor.GREEN + " minutes.");

                    // Schedule reopening the schedule menu on the main thread
                    Bukkit.getScheduler().runTask(this, () -> {
                        gui.openScheduleMenu(player, kothName);
                    });
                } catch (NumberFormatException e) {
                    player.sendMessage(prefix + ChatColor.RED + "Please enter a valid number.");
                }

                creatingKoth.remove(playerUUID);
            }
            else if (action.startsWith("add_command:")) {
                String kothName = action.substring("add_command:".length());
                KothZone kothZone = kothZones.get(kothName);

                String command = event.getMessage();
                List<String> commands = kothZone.getCommandRewards();
                commands.add(command);
                kothZone.setCommandRewards(commands);

                saveKoths();
                player.sendMessage(prefix + ChatColor.GREEN + "Command reward added: " + ChatColor.YELLOW + command);

                // Schedule reopening the commands menu on the main thread
                Bukkit.getScheduler().runTask(this, () -> {
                    gui.openCommandRewardsMenu(player, kothName);
                });

                creatingKoth.remove(playerUUID);
            }
            else if (action.startsWith("select_zone1:")) {
                String kothName = action.substring("select_zone1:".length());

                if (event.getMessage().equalsIgnoreCase("here")) {
                    Location loc = player.getLocation();
                    pos1Selection.put(player.getUniqueId(), loc);
                    player.sendMessage(prefix + ChatColor.GREEN + "Position 1 set to your location. Now type 'here' to set position 2.");
                    creatingKoth.put(playerUUID, "select_zone2:" + kothName);
                } else if (event.getMessage().equalsIgnoreCase("cancel")) {
                    player.sendMessage(prefix + ChatColor.RED + "Zone selection cancelled.");
                    creatingKoth.remove(playerUUID);
                } else {
                    player.sendMessage(prefix + ChatColor.RED + "Please type 'here' to set your current location, or 'cancel' to abort.");
                }
            }
            else if (action.startsWith("select_zone2:")) {
                String kothName = action.substring("select_zone2:".length());

                if (event.getMessage().equalsIgnoreCase("here")) {
                    Location loc = player.getLocation();
                    pos2Selection.put(player.getUniqueId(), loc);

                    // Now update the KOTH zone
                    Location pos1 = pos1Selection.get(player.getUniqueId());
                    Location pos2 = pos2Selection.get(player.getUniqueId());

                    KothZone kothZone = kothZones.get(kothName);
                    kothZone.setCorners(pos1, pos2);

                    pos1Selection.remove(player.getUniqueId());
                    pos2Selection.remove(player.getUniqueId());

                    saveKoths();
                    player.sendMessage(prefix + ChatColor.GREEN + "KOTH zone for " + ChatColor.GOLD + kothName +
                            ChatColor.GREEN + " has been updated!");

                    Bukkit.getScheduler().runTask(this, () -> {
                        gui.openKothManageMenu(player, kothName);
                    });
                } else if (event.getMessage().equalsIgnoreCase("cancel")) {
                    player.sendMessage(prefix + ChatColor.RED + "Zone selection cancelled.");
                    pos1Selection.remove(player.getUniqueId());
                } else {
                    player.sendMessage(prefix + ChatColor.RED + "Please type 'here' to set your current location, or 'cancel' to abort.");
                }

                creatingKoth.remove(playerUUID);
            }
        }
    }
    public void startKoth(Player player, String kothName) {
        if (!kothZones.containsKey(kothName)) {
            player.sendMessage(prefix + ChatColor.RED + "KOTH not found: " + kothName);
            return;
        }

        if (activeKoths.containsKey(kothName)) {
            player.sendMessage(prefix + ChatColor.RED + "This KOTH is already active.");
            return;
        }

        KothZone kothZone = kothZones.get(kothName);
        KothRunnable kothRunnable = new KothRunnable(this, kothName, kothZone);
        BukkitTask task = kothRunnable.runTaskTimer(this, 0, 20); // Run every second
        activeKoths.put(kothName, task);

        player.sendMessage(prefix + ChatColor.GREEN + "KOTH " + ChatColor.GOLD + kothName +
                ChatColor.GREEN + " has been started!");

        // Broadcast to all players
        Bukkit.broadcastMessage(prefix + ChatColor.YELLOW + "KOTH " + ChatColor.GOLD + kothName +
                ChatColor.YELLOW + " has started! Capture time: " + ChatColor.GREEN +
                kothZone.getCaptureTime() + "s");
    }

    public void stopKoth(Player player, String kothName) {
        if (!activeKoths.containsKey(kothName)) {
            player.sendMessage(prefix + ChatColor.RED + "This KOTH is not currently active.");
            return;
        }

        activeKoths.get(kothName).cancel();
        activeKoths.remove(kothName);

        player.sendMessage(prefix + ChatColor.YELLOW + "KOTH " + ChatColor.GOLD + kothName +
                ChatColor.YELLOW + " has been stopped.");

        // Broadcast to all players
        Bukkit.broadcastMessage(prefix + ChatColor.YELLOW + "KOTH " + ChatColor.GOLD + kothName +
                ChatColor.YELLOW + " has ended.");
    }
    public void deleteKoth(Player player, String kothName) {
        if (!kothZones.containsKey(kothName)) {
            player.sendMessage(prefix + ChatColor.RED + "KOTH not found: " + kothName);
            return;
        }

        // Stop if active
        if (activeKoths.containsKey(kothName)) {
            activeKoths.get(kothName).cancel();
            activeKoths.remove(kothName);
            Bukkit.broadcastMessage(prefix + ChatColor.YELLOW + "KOTH " + ChatColor.GOLD + kothName +
                    ChatColor.YELLOW + " has been stopped.");
        }

        kothZones.remove(kothName);
        capturePoints.remove(kothName);
        lastCaptureTimes.remove(kothName);

        player.sendMessage(prefix + ChatColor.GREEN + "KOTH " + ChatColor.GOLD + kothName +
                ChatColor.GREEN + " has been deleted.");

        saveKoths();
    }

    public void createKoth(Player player, String kothName) {
        if (kothZones.containsKey(kothName)) {
            player.sendMessage(prefix + ChatColor.RED + "A KOTH with that name already exists.");
            return;
        }

        // Create a new KOTH at player's location
        Location center = player.getLocation();
        Location corner1 = center.clone().add(-5, -1, -5);
        Location corner2 = center.clone().add(5, 3, 5);

        int captureTime = config.getInt("default-capture-time", 300);
        int capturePoints = config.getInt("default-capture-points", 1);

        KothZone kothZone = new KothZone(kothName, corner1, corner2, captureTime, capturePoints);
        kothZones.put(kothName, kothZone);

        player.sendMessage(prefix + ChatColor.GREEN + "KOTH " + ChatColor.GOLD + kothName +
                ChatColor.GREEN + " has been created at your location with default size!");

        player.sendMessage(prefix + ChatColor.YELLOW + "Tip: Use the menu to manage and start this KOTH.");

        saveKoths();
    }

    public void startZoneSelection(Player player, String kothName) {
        player.sendMessage(prefix + ChatColor.YELLOW + "KOTH zone selection started for " + ChatColor.GOLD + kothName);
        player.sendMessage(prefix + ChatColor.YELLOW + "Go to the first corner and type 'here' in chat.");
        creatingKoth.put(player.getUniqueId(), "select_zone1:" + kothName);
    }

    public void toggleScheduled(String kothName) {
        KothZone kothZone = kothZones.get(kothName);
        kothZone.setScheduled(!kothZone.isScheduled());
        saveKoths();
    }

    public void toggleAlwaysOn(String kothName) {
        KothZone kothZone = kothZones.get(kothName);
        kothZone.setAlwaysOn(!kothZone.isAlwaysOn());
        saveKoths();
    }

    public void recordCapture(String kothName) {
        lastCaptureTimes.put(kothName, System.currentTimeMillis());
    }

    // Method to display KOTH points
    public void showKothPoints(org.bukkit.command.CommandSender sender, String specificKoth) {
        sender.sendMessage(prefix + ChatColor.YELLOW + "KOTH Points:");
        boolean hasData = false;

        // Iterate through all KOTHs or just the specified one
        for (String kothId : kothZones.keySet()) {
            if (specificKoth != null && !kothId.equals(specificKoth)) {
                continue;
            }

            sender.sendMessage(ChatColor.GOLD + kothId + ":");
            Map<UUID, Integer> kothPointsMap = capturePoints.get(kothId);

            if (kothPointsMap == null || kothPointsMap.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "  No points recorded yet.");
            } else {
                hasData = true;
                for (Map.Entry<UUID, Integer> playerEntry : kothPointsMap.entrySet()) {
                    Player target = Bukkit.getPlayer(playerEntry.getKey());
                    String playerName = target != null ? target.getName() : "Unknown";
                    sender.sendMessage(ChatColor.GREEN + "  " + playerName + ": " + playerEntry.getValue());
                }
            }
        }

        if (!hasData && specificKoth == null) {
            sender.sendMessage(ChatColor.GRAY + "  No points recorded for any KOTH yet.");
        }
    }
    public void savePhysicalRewards(String kothName, List<ItemStack> rewards) {
        KothZone kothZone = kothZones.get(kothName);
        kothZone.setPhysicalRewards(rewards);
        saveKoths();
    }

    public void removeCommandReward(String kothName, int index) {
        KothZone kothZone = kothZones.get(kothName);
        List<String> commands = kothZone.getCommandRewards();
        if (index >= 0 && index < commands.size()) {
            commands.remove(index);
            kothZone.setCommandRewards(commands);
            saveKoths();
        }
    }

    // Broadcast a message to all players within a specific radius of a KOTH
    public void broadcastToNearbyPlayers(String kothName, String message, int radius) {
        KothZone kothZone = kothZones.get(kothName);
        if (kothZone == null) return;

        // Calculate center of KOTH
        Location corner1 = kothZone.getCorner1();
        Location corner2 = kothZone.getCorner2();
        Location center = new Location(
                corner1.getWorld(),
                (corner1.getX() + corner2.getX()) / 2,
                (corner1.getY() + corner2.getY()) / 2,
                (corner1.getZ() + corner2.getZ()) / 2
        );

        // Send to players within radius
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(center.getWorld()) &&
                    player.getLocation().distance(center) <= radius) {
                player.sendMessage(message);
            }
        }
    }

    // Getter methods
    public Map<String, KothZone> getKothZones() {
        return kothZones;
    }

    public Map<String, BukkitTask> getActiveKoths() {
        return activeKoths;
    }

    public Map<String, Map<UUID, Integer>> getCapturePoints() {
        return capturePoints;
    }

    public String getPrefix() {
        return prefix;
    }

    public FileConfiguration getPluginConfig() {
        return config;
    }

    public Map<UUID, String> getCreatingKoth() {
        return creatingKoth;
    }

    public KothGUI getGui() {
        return gui;
    }

    public Map<String, Long> getLastCaptureTimes() {
        return lastCaptureTimes;
    }

    public KothStatsManager getStatsManager() {
        return statsManager;
    }
}