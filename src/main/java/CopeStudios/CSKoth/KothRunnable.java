package CopeStudios.CSKoth;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KothRunnable extends BukkitRunnable {
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

        // Initialize capture points map for this koth
        if (!plugin.getCapturePoints().containsKey(kothName)) {
            plugin.getCapturePoints().put(kothName, new HashMap<>());
        }
    }

    @Override
    public void run() {
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
                captureProgress = 0;
                currentCaptor = null;
                contestedPlayers.clear();
                contested = false;
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
                    captureProgress = 0;
                    messageTimer = 0;

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
                // We don't reset progress or current captor - we'll keep track of who was capturing first

                // Track contested players (for stats)
                for (Player player : playersInZone) {
                    KothStatsManager.KothPlayerStats stats = plugin.getStatsManager().getPlayerStats(player.getUniqueId());
                    stats.incrementContests();
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

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return minutes + ":" + (remainingSeconds < 10 ? "0" : "") + remainingSeconds;
    }

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
}