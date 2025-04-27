package CopeStudios.CSKoth;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KothRunnable extends BukkitRunnable {
    // Static map to track KOTH-specific information
    private static final Map<String, KothTrackingInfo> KOTH_TRACKING = new ConcurrentHashMap<>();

    private final CSKoth plugin;
    private final String kothName;
    private final KothZone kothZone;
    private final int captureTime;
    private UUID currentCaptor = null;
    private int captureProgress = 0;
    private int messageTimer = 0;
    private final Map<UUID, Integer> contestedPlayers = new HashMap<>();
    private boolean contested = false;

    public KothRunnable(CSKoth plugin, String kothName, KothZone kothZone) {
        this.plugin = plugin;
        this.kothName = kothName;
        this.kothZone = kothZone;
        this.captureTime = kothZone.getCaptureTime();

        // Initialize tracking info for this KOTH
        KothTrackingInfo trackingInfo = new KothTrackingInfo();
        trackingInfo.setKothCenter(calculateKothCenter());
        KOTH_TRACKING.put(kothName, trackingInfo);

        // Initialize capture points map for this koth
        if (!plugin.getCapturePoints().containsKey(kothName)) {
            plugin.getCapturePoints().put(kothName, new HashMap<>());
        }
    }

    private Location calculateKothCenter() {
        Location corner1 = kothZone.getCorner1();
        Location corner2 = kothZone.getCorner2();
        return new Location(
                corner1.getWorld(),
                (corner1.getX() + corner2.getX()) / 2,
                (corner1.getY() + corner2.getY()) / 2,
                (corner1.getZ() + corner2.getZ()) / 2
        );
    }

    @Override
    public void run() {
        // Tracking info for this specific KOTH
        KothTrackingInfo trackingInfo = KOTH_TRACKING.get(kothName);

        // Get all players in the zone
        List<Player> playersInZone = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (kothZone.isInside(player.getLocation())) {
                playersInZone.add(player);
            }
        }

        // If no players in zone, reset capture progress
        if (playersInZone.isEmpty()) {
            if (captureProgress > 0) {
                broadcastToNearbyPlayers(plugin.getPrefix() + ChatColor.YELLOW + "KOTH " + ChatColor.GOLD + kothName +
                        ChatColor.YELLOW + " is no longer being captured!");
                resetCaptureProgress(trackingInfo);
            }
            return;
        }
// Update message timer
        messageTimer++;

        // If only one player in zone
        if (playersInZone.size() == 1) {
            Player player = playersInZone.get(0);
            UUID playerUUID = player.getUniqueId();

            // If this player is already capturing or was capturing before contested
            if (currentCaptor != null && currentCaptor.equals(playerUUID)) {
                // If it was contested, resume capturing
                if (contested) {
                    contested = false;
                    broadcastToNearbyPlayers(plugin.getPrefix() + ChatColor.GREEN + player.getName() + ChatColor.YELLOW +
                            " has resumed capturing KOTH " + ChatColor.GOLD + kothName + ChatColor.YELLOW + "!");
                }

                captureProgress++;
                trackingInfo.setCurrentCapturer(playerUUID);
                trackingInfo.setCurrentCaptureProgress(captureProgress);
                trackingInfo.setRemainingCaptureTime(captureTime - captureProgress);
                trackingInfo.setContested(false);

                // Send progress message every 15 seconds
                if (messageTimer >= 15 || captureProgress == 1) {
                    String timeLeft = formatTime(captureTime - captureProgress);
                    broadcastToNearbyPlayers(plugin.getPrefix() + ChatColor.GREEN + player.getName() + ChatColor.YELLOW +
                            " is capturing KOTH: " + ChatColor.GOLD + kothName + ChatColor.YELLOW +
                            " - Time left: " + ChatColor.GREEN + timeLeft);
                    messageTimer = 0;
                }

                // Check if player has captured the koth
                if (captureProgress >= captureTime) {
                    // Award points
                    Map<UUID, Integer> kothPoints = plugin.getCapturePoints().get(kothName);
                    int currentPoints = kothPoints.getOrDefault(playerUUID, 0);
                    kothPoints.put(playerUUID, currentPoints + kothZone.getCapturePoints());

                    // Update player stats
                    KothStatsManager.KothPlayerStats stats = plugin.getStatsManager().getPlayerStats(playerUUID);
                    stats.incrementCaptures();

                    // Broadcast capture
                    Bukkit.broadcastMessage(plugin.getPrefix() + ChatColor.GREEN + player.getName() + ChatColor.YELLOW +
                            " has captured KOTH " + ChatColor.GOLD + kothName + ChatColor.YELLOW +
                            " and earned " + ChatColor.GREEN + kothZone.getCapturePoints() +
                            ChatColor.YELLOW + " points!");

                    // Reset capture progress
                    resetCaptureProgress(trackingInfo);

                    // Execute rewards
                    giveRewards(player);

                    // Record the capture time
                    plugin.recordCapture(kothName);

                    // If always-on is false and it's a scheduled KOTH, stop it
                    if (!kothZone.isAlwaysOn() && kothZone.isScheduled()) {
                        if (plugin.getActiveKoths().containsKey(kothName)) {
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                if (plugin.getActiveKoths().containsKey(kothName)) {
                                    plugin.getActiveKoths().get(kothName).cancel();
                                    plugin.getActiveKoths().remove(kothName);
                                    Bukkit.broadcastMessage(plugin.getPrefix() + ChatColor.YELLOW + "KOTH " + ChatColor.GOLD + kothName +
                                            ChatColor.YELLOW + " has ended.");
                                }
                            }, 20); // 1 second delay to avoid issues
                        }
                    }
                }
            }
            // New capturer
            else {
                currentCaptor = playerUUID;
                captureProgress = 1;
                contestedPlayers.clear();
                contested = false;
                messageTimer = 0;

                // Update tracking info
                trackingInfo.setCurrentCapturer(playerUUID);
                trackingInfo.setCurrentCaptureProgress(captureProgress);
                trackingInfo.setRemainingCaptureTime(captureTime - captureProgress);
                trackingInfo.setContested(false);

                broadcastToNearbyPlayers(plugin.getPrefix() + ChatColor.GREEN + player.getName() + ChatColor.YELLOW +
                        " is capturing KOTH " + ChatColor.GOLD + kothName + ChatColor.YELLOW + "!");
            }
        }
        // Multiple players in zone - contested
        else {
            // Only announce if not already contested
            if (!contested) {
                broadcastToNearbyPlayers(plugin.getPrefix() + ChatColor.YELLOW + "KOTH " + ChatColor.GOLD + kothName +
                        ChatColor.YELLOW + " is contested!");
                contested = true;
                trackingInfo.setContested(true);

                // Track contested players (for stats and placeholders)
                for (Player player : playersInZone) {
                    KothStatsManager.KothPlayerStats stats = plugin.getStatsManager().getPlayerStats(player.getUniqueId());
                    stats.incrementContests();
                    trackingInfo.addContestedPlayer(player.getUniqueId());
                }
            }

            // If we've been contested for a while, send periodic updates
            if (contested && messageTimer >= 15) {
                if (currentCaptor != null) {
                    Player captorPlayer = Bukkit.getPlayer(currentCaptor);
                    if (captorPlayer != null && playersInZone.contains(captorPlayer)) {
                        String timeLeft = formatTime(captureTime - captureProgress);
                        broadcastToNearbyPlayers(plugin.getPrefix() + ChatColor.YELLOW + "KOTH " + ChatColor.GOLD + kothName +
                                ChatColor.YELLOW + " is contested! " + captorPlayer.getName() + " has " +
                                ChatColor.GREEN + timeLeft + ChatColor.YELLOW + " capture time remaining.");
                    }
                }
                messageTimer = 0;
            }
        }
    }
    // Static method to get tracking info for a specific KOTH
    public static KothTrackingInfo getKothTrackingInfo(String kothName) {
        return KOTH_TRACKING.getOrDefault(kothName, new KothTrackingInfo());
    }

    // Static method to get current capturer name
    public static String getCurrentCapturerName(String kothName) {
        KothTrackingInfo info = getKothTrackingInfo(kothName);
        UUID capturerUUID = info.getCurrentCapturer();
        if (capturerUUID == null) return "None";

        Player player = Bukkit.getPlayer(capturerUUID);
        return player != null ? player.getName() : "Unknown";
    }

    // Static method to get current capture progress
    public static int getCurrentCaptureProgress(String kothName) {
        KothTrackingInfo info = getKothTrackingInfo(kothName);
        return info.getCurrentCaptureProgress();
    }

    // Static method to get remaining capture time
    public static int getRemainingCaptureTime(String kothName) {
        KothTrackingInfo info = getKothTrackingInfo(kothName);
        return info.getRemainingCaptureTime();
    }

    // Static method to get contested players names
    public static String getContestedPlayersNames(String kothName) {
        KothTrackingInfo info = getKothTrackingInfo(kothName);
        List<String> playerNames = new ArrayList<>();

        for (UUID uuid : info.getContestedPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                playerNames.add(player.getName());
            }
        }

        return playerNames.isEmpty() ? "None" : String.join(", ", playerNames);
    }

    // Static method to get KOTH center coordinates
    public static String getKothCoordinates(String kothName) {
        KothTrackingInfo info = getKothTrackingInfo(kothName);
        Location center = info.getKothCenter();

        return center != null
                ? String.format("%d, %d, %d", center.getBlockX(), center.getBlockY(), center.getBlockZ())
                : "N/A";
    }

    // Static method to check if a KOTH is contested
    public static boolean isKothContested(String kothName) {
        KothTrackingInfo info = getKothTrackingInfo(kothName);
        return info.isContested();
    }

    // Method to reset capture progress
    private void resetCaptureProgress(KothTrackingInfo trackingInfo) {
        captureProgress = 0;
        currentCaptor = null;
        contestedPlayers.clear();
        contested = false;

        // Update tracking info
        trackingInfo.setCurrentCapturer(null);
        trackingInfo.setCurrentCaptureProgress(0);
        trackingInfo.setRemainingCaptureTime(0);
        trackingInfo.setContested(false);
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return minutes + ":" + (remainingSeconds < 10 ? "0" : "") + remainingSeconds;
    }

    // Broadcast method
    private void broadcastToNearbyPlayers(String message) {
        // Center of the KOTH
        Location center = new Location(
                kothZone.getCorner1().getWorld(),
                (kothZone.getCorner1().getX() + kothZone.getCorner2().getX()) / 2,
                (kothZone.getCorner1().getY() + kothZone.getCorner2().getY()) / 2,
                (kothZone.getCorner1().getZ() + kothZone.getCorner2().getZ()) / 2
        );
        int radius = 3000; // 3000 blocks radius

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(center.getWorld()) &&
                    player.getLocation().distance(center) <= radius) {
                player.sendMessage(message);
            }
        }
    }
    private void giveRewards(Player player) {
        // Execute command rewards
        List<String> commands = kothZone.getCommandRewards();
        for (String command : commands) {
            command = command.replace("%player%", player.getName())
                    .replace("%koth%", kothName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }

        // Give physical rewards
        List<ItemStack> physicalRewards = kothZone.getPhysicalRewards();
        if (physicalRewards != null && !physicalRewards.isEmpty()) {
            for (ItemStack item : physicalRewards) {
                if (item != null) {
                    // Clone to prevent side effects
                    player.getInventory().addItem(item.clone());
                }
            }
            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "You received physical rewards for capturing the KOTH!");
        }

        // Also execute config-defined rewards for backward compatibility
        List<String> configCommands = plugin.getPluginConfig().getStringList("rewards");
        for (String command : configCommands) {
            command = command.replace("%player%", player.getName())
                    .replace("%koth%", kothName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    // Inner class for tracking KOTH information
    public static class KothTrackingInfo {
        private UUID currentCapturer;
        private int currentCaptureProgress;
        private int remainingCaptureTime;
        private boolean isContested;
        private List<UUID> contestedPlayers;
        private Location kothCenter;

        public KothTrackingInfo() {
            this.currentCapturer = null;
            this.currentCaptureProgress = 0;
            this.remainingCaptureTime = 0;
            this.isContested = false;
            this.contestedPlayers = new ArrayList<>();
            this.kothCenter = null;
        }

        // Getters and Setters
        public UUID getCurrentCapturer() {
            return currentCapturer;
        }

        public void setCurrentCapturer(UUID currentCapturer) {
            this.currentCapturer = currentCapturer;
        }

        public int getCurrentCaptureProgress() {
            return currentCaptureProgress;
        }

        public void setCurrentCaptureProgress(int currentCaptureProgress) {
            this.currentCaptureProgress = currentCaptureProgress;
        }

        public int getRemainingCaptureTime() {
            return remainingCaptureTime;
        }

        public void setRemainingCaptureTime(int remainingCaptureTime) {
            this.remainingCaptureTime = remainingCaptureTime;
        }

        public boolean isContested() {
            return isContested;
        }

        public void setContested(boolean contested) {
            isContested = contested;
        }

        public List<UUID> getContestedPlayers() {
            return contestedPlayers;
        }

        public void addContestedPlayer(UUID player) {
            if (!contestedPlayers.contains(player)) {
                contestedPlayers.add(player);
            }
        }

        public Location getKothCenter() {
            return kothCenter;
        }

        public void setKothCenter(Location kothCenter) {
            this.kothCenter = kothCenter;
        }
    }
}