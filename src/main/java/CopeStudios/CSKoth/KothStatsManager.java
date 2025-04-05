package CopeStudios.CSKoth;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class KothStatsManager {

    private final CSKoth plugin;
    private File statsFile;
    private FileConfiguration statsConfig;
    private final Map<UUID, KothPlayerStats> playerStats = new HashMap<>();

    public KothStatsManager(CSKoth plugin) {
        this.plugin = plugin;
        loadStats();
    }

    private void loadStats() {
        // Create stats file if it doesn't exist
        statsFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!statsFile.exists()) {
            try {
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create stats.yml file");
            }
        }

        statsConfig = YamlConfiguration.loadConfiguration(statsFile);

        // Load player stats
        if (statsConfig.contains("players")) {
            for (String uuidStr : statsConfig.getConfigurationSection("players").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                int captures = statsConfig.getInt("players." + uuidStr + ".captures", 0);
                int contests = statsConfig.getInt("players." + uuidStr + ".contests", 0);

                KothPlayerStats stats = new KothPlayerStats(uuid);
                stats.setCaptures(captures);
                stats.setContests(contests);
                playerStats.put(uuid, stats);
            }
        }
    }

    public void saveStats() {
        // Save all player stats
        for (Map.Entry<UUID, KothPlayerStats> entry : playerStats.entrySet()) {
            UUID uuid = entry.getKey();
            KothPlayerStats stats = entry.getValue();

            statsConfig.set("players." + uuid.toString() + ".captures", stats.getCaptures());
            statsConfig.set("players." + uuid.toString() + ".contests", stats.getContests());
        }

        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save stats.yml file");
        }
    }

    public KothPlayerStats getPlayerStats(UUID uuid) {
        if (!playerStats.containsKey(uuid)) {
            playerStats.put(uuid, new KothPlayerStats(uuid));
        }
        return playerStats.get(uuid);
    }

    public List<KothPlayerStats> getTopCaptures(int limit) {
        return playerStats.values().stream()
                .sorted(Comparator.comparing(KothPlayerStats::getCaptures).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<KothPlayerStats> getTopContests(int limit) {
        return playerStats.values().stream()
                .sorted(Comparator.comparing(KothPlayerStats::getContests).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public int getPlayerRank(UUID uuid) {
        List<KothPlayerStats> allStats = playerStats.values().stream()
                .sorted(Comparator.comparing(KothPlayerStats::getCaptures).reversed())
                .collect(Collectors.toList());

        for (int i = 0; i < allStats.size(); i++) {
            if (allStats.get(i).getUUID().equals(uuid)) {
                return i + 1;
            }
        }

        return -1; // Not found
    }

    public String getFormattedTop10() {
        List<KothPlayerStats> top10 = getTopCaptures(10);
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.GOLD).append("Top 10 KOTH Captures:").append("\n");

        for (int i = 0; i < top10.size(); i++) {
            KothPlayerStats stats = top10.get(i);
            OfflinePlayer player = Bukkit.getOfflinePlayer(stats.getUUID());
            String name = player.getName() != null ? player.getName() : "Unknown";

            sb.append(ChatColor.YELLOW).append(i+1).append(". ")
                    .append(ChatColor.WHITE).append(name).append(": ")
                    .append(ChatColor.GREEN).append(stats.getCaptures())
                    .append("\n");
        }

        return sb.toString();
    }

    // Placeholder methods for external plugins
    public String getPlaceholder(Player player, String identifier) {
        UUID uuid = player.getUniqueId();
        KothPlayerStats stats = getPlayerStats(uuid);

        switch (identifier.toLowerCase()) {
            case "wins":
            case "captures":
                return String.valueOf(stats.getCaptures());
            case "contests":
                return String.valueOf(stats.getContests());
            case "rank":
                int rank = getPlayerRank(uuid);
                return rank > 0 ? String.valueOf(rank) : "N/A";
            case "topwins":
                return getFormattedTop10();
            default:
                return "0";
        }
    }

    // Inner class to hold player stats
    public static class KothPlayerStats {
        private final UUID uuid;
        private int captures;
        private int contests;

        public KothPlayerStats(UUID uuid) {
            this.uuid = uuid;
            this.captures = 0;
            this.contests = 0;
        }

        public UUID getUUID() {
            return uuid;
        }

        public int getCaptures() {
            return captures;
        }

        public void setCaptures(int captures) {
            this.captures = captures;
        }

        public void incrementCaptures() {
            this.captures++;
        }

        public int getContests() {
            return contests;
        }

        public void setContests(int contests) {
            this.contests = contests;
        }

        public void incrementContests() {
            this.contests++;
        }
    }
}