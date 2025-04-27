package CopeStudios.CSKoth;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CSPlaceholders extends PlaceholderExpansion {
    private final CSKoth plugin;
    private File guideFile;
    private Map<String, String> lastWinners = new HashMap<>();

    public CSPlaceholders(CSKoth plugin) {
        this.plugin = plugin;
        createPlaceholderGuide();
    }

    public void recordWinner(String kothName, String playerName) {
        lastWinners.put(kothName, playerName);
    }

    public void testPlaceholders(Player player) {
        // Player Statistics
        player.sendMessage("§6Placeholder Test Results:");
        player.sendMessage("§e%cskoth_captures%: §7" + onRequest(player, "captures"));
        player.sendMessage("§e%cskoth_contests%: §7" + onRequest(player, "contests"));
        player.sendMessage("§e%cskoth_rank%: §7" + onRequest(player, "rank"));
        player.sendMessage("§e%cskoth_topwins%: §7" + onRequest(player, "topwins"));

        // Current KOTH Status
        player.sendMessage("§e%cskoth_active%: §7" + onRequest(player, "active"));
        player.sendMessage("§e%cskoth_next%: §7" + onRequest(player, "next"));
        player.sendMessage("§e%cskoth_next_time%: §7" + onRequest(player, "next_time"));
        player.sendMessage("§e%cskoth_next_countdown%: §7" + onRequest(player, "next_countdown"));
        player.sendMessage("§e%cskoth_total_active%: §7" + onRequest(player, "total_active"));
        player.sendMessage("§e%cskoth_total_koths%: §7" + onRequest(player, "total_koths"));

        // Player specific
        player.sendMessage("§e%cskoth_player_in_koth%: §7" + onRequest(player, "player_in_koth"));

        // Attempt to test KOTHs with placeholders (will use first KOTH if exists)
        Map<String, KothZone> kothZones = plugin.getKothZones();
        if (!kothZones.isEmpty()) {
            String firstKoth = kothZones.keySet().iterator().next();

            // KOTH Zone Information
            player.sendMessage("§6Testing Placeholders for KOTH: §e" + firstKoth);
            player.sendMessage("§e%cskoth_points_" + firstKoth + "%: §7" + onRequest(player, "points_" + firstKoth));
            player.sendMessage("§e%cskoth_captime_" + firstKoth + "%: §7" + onRequest(player, "captime_" + firstKoth));
            player.sendMessage("§e%cskoth_status_" + firstKoth + "%: §7" + onRequest(player, "status_" + firstKoth));
            player.sendMessage("§e%cskoth_player_distance_" + firstKoth + "%: §7" + onRequest(player, "player_distance_" + firstKoth));
            player.sendMessage("§e%cskoth_last_winner_" + firstKoth + "%: §7" + onRequest(player, "last_winner_" + firstKoth));

            // KOTH Capture Progress
            player.sendMessage("§e%cskoth_capturer_" + firstKoth + "%: §7" + onRequest(player, "capturer_" + firstKoth));
            player.sendMessage("§e%cskoth_progress_" + firstKoth + "%: §7" + onRequest(player, "progress_" + firstKoth));
            player.sendMessage("§e%cskoth_remaining_" + firstKoth + "%: §7" + onRequest(player, "remaining_" + firstKoth));
            player.sendMessage("§e%cskoth_capture_time_left_" + firstKoth + "%: §7" + onRequest(player, "capture_time_left_" + firstKoth));
            player.sendMessage("§e%cskoth_duration_left_" + firstKoth + "%: §7" + onRequest(player, "duration_left_" + firstKoth));

            // Detailed Tracking
            player.sendMessage("§e%cskoth_contested_" + firstKoth + "%: §7" + onRequest(player, "contested_" + firstKoth));
            player.sendMessage("§e%cskoth_contested_players_" + firstKoth + "%: §7" + onRequest(player, "contested_players_" + firstKoth));
            player.sendMessage("§e%cskoth_coordinates_" + firstKoth + "%: §7" + onRequest(player, "coordinates_" + firstKoth));
        } else {
            player.sendMessage("§cNo KOTHs found to test detailed placeholders.");
        }
    }

    private void createPlaceholderGuide() {
        guideFile = new File(plugin.getDataFolder(), "placeholders_guide.yml");
        if (!guideFile.exists()) {
            try {
                if (!plugin.getDataFolder().exists()) {
                    plugin.getDataFolder().mkdirs();
                }

                guideFile.createNewFile();
                YamlConfiguration guide = new YamlConfiguration();

                // Player Statistics
                guide.set("Player Statistics.%cskoth_captures%", "Number of KOTHs the player has captured");
                guide.set("Player Statistics.%cskoth_wins%", "Alias for captures");
                guide.set("Player Statistics.%cskoth_contests%", "Number of times the player has contested a KOTH");
                guide.set("Player Statistics.%cskoth_rank%", "Player's rank based on number of captures");
                guide.set("Player Statistics.%cskoth_topwins%", "Formatted string of top 10 players by captures");
                guide.set("Player Statistics.%cskoth_player_in_koth%", "Whether player is currently in any KOTH zone (true/false)");

                // Current KOTH Status
                guide.set("Current KOTH Status.%cskoth_active%", "List of currently active KOTHs");
                guide.set("Current KOTH Status.%cskoth_next%", "Name of the next scheduled KOTH");
                guide.set("Current KOTH Status.%cskoth_next_time%", "Start time of the next scheduled KOTH");
                guide.set("Current KOTH Status.%cskoth_next_countdown%", "Time remaining until the next KOTH starts");
                guide.set("Current KOTH Status.%cskoth_total_active%", "Total number of active KOTHs");
                guide.set("Current KOTH Status.%cskoth_total_koths%", "Total number of configured KOTHs");

                // KOTH Zone Information
                guide.set("KOTH Zone Information.%cskoth_points_[name]%", "Points value for capturing the specified KOTH");
                guide.set("KOTH Zone Information.%cskoth_captime_[name]%", "Capture time in seconds for the specified KOTH");
                guide.set("KOTH Zone Information.%cskoth_status_[name]%", "Status of the specified KOTH (Active/Inactive)");
                guide.set("KOTH Zone Information.%cskoth_player_distance_[name]%", "Distance of player to the specified KOTH");
                guide.set("KOTH Zone Information.%cskoth_last_winner_[name]%", "Last player to capture the specified KOTH");

                // KOTH Capture Progress
                guide.set("KOTH Capture Progress.%cskoth_capturer_[name]%", "Name of the player currently capturing the KOTH");
                guide.set("KOTH Capture Progress.%cskoth_progress_[name]%", "Capture progress for the KOTH (in seconds)");
                guide.set("KOTH Capture Progress.%cskoth_remaining_[name]%", "Time remaining to capture the KOTH (in seconds)");
                guide.set("KOTH Capture Progress.%cskoth_capture_time_left_[name]%", "Time left in seconds for the current KOTH capture");
                guide.set("KOTH Capture Progress.%cskoth_duration_left_[name]%", "Remaining duration of an active KOTH event");

                // Detailed Tracking
                guide.set("Detailed Tracking.%cskoth_contested_[name]%", "Whether the KOTH is currently contested (true/false)");
                guide.set("Detailed Tracking.%cskoth_contested_players_[name]%", "Names of players contesting the KOTH");
                guide.set("Detailed Tracking.%cskoth_coordinates_[name]%", "Coordinates of the KOTH center");

                guide.save(guideFile);
                plugin.getLogger().info("Created placeholders_guide.yml");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create placeholders_guide.yml: " + e.getMessage());
            }
        }
    }

    @Override
    public String getIdentifier() {
        return "cskoth";
    }

    @Override
    public String getAuthor() {
        return "CopeStudios"; // Replace with your name
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean register() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return false;
        }
        return super.register();
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        // Player Statistics
        if (params.equalsIgnoreCase("captures") || params.equalsIgnoreCase("wins")) {
            return String.valueOf(getPlayerStatistic(player.getUniqueId(), "captures"));
        }
        if (params.equalsIgnoreCase("contests")) {
            return String.valueOf(getPlayerStatistic(player.getUniqueId(), "contests"));
        }
        if (params.equalsIgnoreCase("rank")) {
            int rank = plugin.getStatsManager().getPlayerRank(player.getUniqueId());
            return rank > 0 ? String.valueOf(rank) : "N/A";
        }
        if (params.equalsIgnoreCase("topwins")) {
            return plugin.getStatsManager().getFormattedTop10();
        }
        if (params.equalsIgnoreCase("player_in_koth")) {
            return isPlayerInAnyKoth(player) ? "true" : "false";
        }

        // Current KOTH Status
        if (params.equalsIgnoreCase("active")) {
            return getActiveKoths();
        }
        if (params.equalsIgnoreCase("next")) {
            return getNextScheduledKoth();
        }
        if (params.equalsIgnoreCase("next_time")) {
            return getNextKothStartTime();
        }
        if (params.equalsIgnoreCase("next_countdown")) {
            return getNextKothCountdown();
        }
        if (params.equalsIgnoreCase("total_active")) {
            return String.valueOf(plugin.getActiveKoths().size());
        }
        if (params.equalsIgnoreCase("total_koths")) {
            return String.valueOf(plugin.getKothZones().size());
        }

        // KOTH Zone Information
        if (params.startsWith("points_")) {
            String kothName = params.substring("points_".length());
            return getKothPoints(kothName);
        }
        if (params.startsWith("captime_")) {
            String kothName = params.substring("captime_".length());
            return getKothCaptureTime(kothName);
        }
        if (params.startsWith("status_")) {
            String kothName = params.substring("status_".length());
            return getKothStatus(kothName);
        }
        if (params.startsWith("player_distance_")) {
            String kothName = params.substring("player_distance_".length());
            return getPlayerDistance(player, kothName);
        }
        if (params.startsWith("last_winner_")) {
            String kothName = params.substring("last_winner_".length());
            return getLastWinner(kothName);
        }

        // KOTH Capture Progress
        if (params.startsWith("capturer_")) {
            String kothName = params.substring("capturer_".length());
            return getCurrentCapturer(kothName);
        }
        if (params.startsWith("progress_")) {
            String kothName = params.substring("progress_".length());
            return getCurrentCaptureProgress(kothName);
        }
        if (params.startsWith("remaining_")) {
            String kothName = params.substring("remaining_".length());
            return getRemainingCaptureTime(kothName);
        }
        if (params.startsWith("capture_time_left_")) {
            String kothName = params.substring("capture_time_left_".length());
            return getCaptureTimeLeft(kothName);
        }
        if (params.startsWith("duration_left_")) {
            String kothName = params.substring("duration_left_".length());
            return getDurationLeft(kothName);
        }

        // Detailed tracking
        if (params.startsWith("contested_")) {
            String kothName = params.substring("contested_".length());
            return String.valueOf(KothRunnable.isKothContested(kothName));
        }
        if (params.startsWith("contested_players_")) {
            String kothName = params.substring("contested_players_".length());
            return KothRunnable.getContestedPlayersNames(kothName);
        }
        if (params.startsWith("coordinates_")) {
            String kothName = params.substring("coordinates_".length());
            return getKothCoordinates(kothName);
        }

        return null;
    }

    private int getPlayerStatistic(UUID uuid, String type) {
        KothStatsManager.KothPlayerStats stats = plugin.getStatsManager().getPlayerStats(uuid);
        return type.equals("captures") ? stats.getCaptures() : stats.getContests();
    }

    private boolean isPlayerInAnyKoth(OfflinePlayer offlinePlayer) {
        if (!offlinePlayer.isOnline()) return false;

        Player player = offlinePlayer.getPlayer();
        if (player == null) return false;

        for (KothZone kothZone : plugin.getKothZones().values()) {
            if (kothZone.isInside(player.getLocation())) {
                return true;
            }
        }
        return false;
    }

    private String getActiveKoths() {
        Set<String> activeKoths = plugin.getActiveKoths().keySet();
        return activeKoths.isEmpty() ? "None" : String.join(", ", activeKoths);
    }

    private String getNextScheduledKoth() {
        Map<String, KothZone> kothZones = plugin.getKothZones();
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);
        int currentTimeInMinutes = currentHour * 60 + currentMinute;

        String nextKothName = null;
        int nextKothTime = Integer.MAX_VALUE;

        for (Map.Entry<String, KothZone> entry : kothZones.entrySet()) {
            KothZone kothZone = entry.getValue();

            if (kothZone.isScheduled() && kothZone.getStartHour() >= 0 && kothZone.getStartMinute() >= 0) {
                int kothTimeInMinutes = kothZone.getStartHour() * 60 + kothZone.getStartMinute();

                int timeUntilKoth = kothTimeInMinutes > currentTimeInMinutes
                        ? kothTimeInMinutes - currentTimeInMinutes
                        : (24 * 60) - currentTimeInMinutes + kothTimeInMinutes;

                if (timeUntilKoth < nextKothTime) {
                    nextKothTime = timeUntilKoth;
                    nextKothName = entry.getKey();
                }
            }
        }

        return nextKothName != null ? nextKothName : "None";
    }

    private String getNextKothStartTime() {
        Map<String, KothZone> kothZones = plugin.getKothZones();
        String nextKoth = getNextScheduledKoth();
        if (nextKoth.equals("None")) return "N/A";

        KothZone kothZone = kothZones.get(nextKoth);
        return String.format("%02d:%02d", kothZone.getStartHour(), kothZone.getStartMinute());
    }

    private String getNextKothCountdown() {
        Map<String, KothZone> kothZones = plugin.getKothZones();
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);
        int currentTimeInMinutes = currentHour * 60 + currentMinute;

        String nextKothName = getNextScheduledKoth();
        if (nextKothName.equals("None")) return "N/A";

        KothZone kothZone = kothZones.get(nextKothName);
        int kothTimeInMinutes = kothZone.getStartHour() * 60 + kothZone.getStartMinute();

        int timeUntilKoth = kothTimeInMinutes > currentTimeInMinutes
                ? kothTimeInMinutes - currentTimeInMinutes
                : (24 * 60) - currentTimeInMinutes + kothTimeInMinutes;

        int hours = timeUntilKoth / 60;
        int minutes = timeUntilKoth % 60;

        return hours > 0 ? hours + "h " + minutes + "m" : minutes + "m";
    }

    private String getKothPoints(String kothName) {
        KothZone kothZone = plugin.getKothZones().get(kothName);
        return kothZone != null ? String.valueOf(kothZone.getCapturePoints()) : "N/A";
    }

    private String getKothCaptureTime(String kothName) {
        KothZone kothZone = plugin.getKothZones().get(kothName);
        return kothZone != null ? String.valueOf(kothZone.getCaptureTime()) : "N/A";
    }

    private String getKothStatus(String kothName) {
        return plugin.getActiveKoths().containsKey(kothName) ? "Active" : "Inactive";
    }

    private String getPlayerDistance(OfflinePlayer offlinePlayer, String kothName) {
        if (!offlinePlayer.isOnline()) return "N/A";

        Player player = offlinePlayer.getPlayer();
        if (player == null) return "N/A";

        KothZone kothZone = plugin.getKothZones().get(kothName);
        if (kothZone == null) return "N/A";

        // Calculate center of KOTH
        Location corner1 = kothZone.getCorner1();
        Location corner2 = kothZone.getCorner2();

        // Ensure same world
        if (!player.getWorld().equals(corner1.getWorld())) return "Different World";

        Location center = new Location(
                corner1.getWorld(),
                (corner1.getX() + corner2.getX()) / 2,
                (corner1.getY() + corner2.getY()) / 2,
                (corner1.getZ() + corner2.getZ()) / 2
        );

        double distance = player.getLocation().distance(center);
        return String.format("%.1f", distance);
    }

    private String getLastWinner(String kothName) {
        return lastWinners.getOrDefault(kothName, "None");
    }

    private String getCurrentCapturer(String kothName) {
        return KothRunnable.getCurrentCapturerName(kothName);
    }

    private String getCurrentCaptureProgress(String kothName) {
        int progress = KothRunnable.getCurrentCaptureProgress(kothName);
        return progress > 0 ? String.valueOf(progress) : "Not in progress";
    }

    private String getRemainingCaptureTime(String kothName) {
        int remaining = KothRunnable.getRemainingCaptureTime(kothName);
        return remaining > 0 ? String.valueOf(remaining) : "N/A";
    }

    private String getCaptureTimeLeft(String kothName) {
        int progress = KothRunnable.getCurrentCaptureProgress(kothName);
        KothZone kothZone = plugin.getKothZones().get(kothName);

        if (kothZone == null || progress <= 0) return "N/A";

        int timeLeft = kothZone.getCaptureTime() - progress;
        return timeLeft > 0 ? String.valueOf(timeLeft) : "0";
    }

    private String getDurationLeft(String kothName) {
        if (!plugin.getActiveKoths().containsKey(kothName)) return "N/A";

        KothZone kothZone = plugin.getKothZones().get(kothName);
        if (kothZone == null) return "N/A";

        // This would need some additional tracking in your KothRunnable class
        // to store when a KOTH started and calculate time left
        // For now, return a placeholder value
        return "Needs implementation";
    }

    private String getKothCoordinates(String kothName) {
        KothZone kothZone = plugin.getKothZones().get(kothName);
        if (kothZone == null) return "N/A";

        Location corner1 = kothZone.getCorner1();
        Location corner2 = kothZone.getCorner2();

        int x = (corner1.getBlockX() + corner2.getBlockX()) / 2;
        int y = (corner1.getBlockY() + corner2.getBlockY()) / 2;
        int z = (corner1.getBlockZ() + corner2.getBlockZ()) / 2;

        return x + ", " + y + ", " + z;
    }
}