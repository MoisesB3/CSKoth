package us.copestudios.cskoth;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import us.copestudios.copecore.config.ConfigFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Player statistics in plugins/CSKoth/stats.yml (same file and keys as 1.x, plus points).
 *
 * <pre>
 * players:
 *   &lt;uuid&gt;: {captures: 4, contests: 9, points: 12}
 * koth-points:                    # (new) points per KOTH - 1.x kept these in memory only, so they reset on restart
 *   desert:
 *     &lt;uuid&gt;: 5
 * </pre>
 *
 * <p>Changes are written within a few seconds (and when the server stops), not on every capture.</p>
 */
public final class Stats {

    /** One player's numbers. */
    public static final class Entry {
        final UUID player;
        int captures;
        int contests;
        int points;

        Entry(UUID player) {
            this.player = player;
        }

        public UUID player() {
            return player;
        }

        public int captures() {
            return captures;
        }

        public int contests() {
            return contests;
        }

        public int points() {
            return points;
        }
    }

    private final ConfigFile file;
    private final Map<UUID, Entry> players = new HashMap<>();
    /** koth (lowercase) -> player -> points */
    private final Map<String, Map<UUID, Integer>> kothPoints = new HashMap<>();
    private boolean dirty;

    Stats(CSKoth plugin) {
        this.file = new ConfigFile(plugin, "stats.yml", false);
        load();
    }

    private void load() {
        players.clear();
        kothPoints.clear();
        YamlConfiguration yaml = file.get();
        ConfigurationSection root = yaml.getConfigurationSection("players");
        if (root != null) {
            for (String key : root.getKeys(false)) {
                try {
                    Entry entry = new Entry(UUID.fromString(key));
                    entry.captures = root.getInt(key + ".captures");
                    entry.contests = root.getInt(key + ".contests");
                    entry.points = root.getInt(key + ".points");
                    players.put(entry.player, entry);
                } catch (IllegalArgumentException notAUuid) {
                    // skip broken lines
                }
            }
        }
        ConfigurationSection perKoth = yaml.getConfigurationSection("koth-points");
        if (perKoth != null) {
            for (String koth : perKoth.getKeys(false)) {
                Map<UUID, Integer> map = new HashMap<>();
                ConfigurationSection section = perKoth.getConfigurationSection(koth);
                if (section == null) {
                    continue;
                }
                for (String key : section.getKeys(false)) {
                    try {
                        map.put(UUID.fromString(key), section.getInt(key));
                    } catch (IllegalArgumentException notAUuid) {
                        // skip
                    }
                }
                kothPoints.put(koth.toLowerCase(Locale.ROOT), map);
            }
        }
    }

    /** Writes stats.yml if something changed. */
    void save() {
        if (!dirty) {
            return;
        }
        YamlConfiguration yaml = file.get();
        yaml.set("players", null);
        for (Entry entry : players.values()) {
            String path = "players." + entry.player;
            yaml.set(path + ".captures", entry.captures);
            yaml.set(path + ".contests", entry.contests);
            yaml.set(path + ".points", entry.points);
        }
        yaml.set("koth-points", null);
        kothPoints.forEach((koth, map) -> map.forEach((player, points) ->
                yaml.set("koth-points." + koth + "." + player, points)));
        file.save();
        dirty = false;
    }

    public Entry of(UUID player) {
        return players.computeIfAbsent(player, Entry::new);
    }

    void addCapture(UUID player, String koth, int points) {
        Entry entry = of(player);
        entry.captures++;
        entry.points += points;
        kothPoints.computeIfAbsent(koth.toLowerCase(Locale.ROOT), k -> new HashMap<>()).merge(player, points, Integer::sum);
        dirty = true;
    }

    void addContest(UUID player) {
        of(player).contests++;
        dirty = true;
    }

    /** Players by captures, most first. */
    public List<Entry> top(int limit) {
        List<Entry> sorted = new ArrayList<>(players.values());
        sorted.sort(Comparator.comparingInt(Entry::captures).reversed().thenComparing(Entry::points, Comparator.reverseOrder()));
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }

    /** 1 = most captures; -1 = no captures yet. */
    public int rank(UUID player) {
        Entry own = players.get(player);
        if (own == null || own.captures == 0) {
            return -1;
        }
        int better = 0;
        for (Entry entry : players.values()) {
            if (entry.captures > own.captures) {
                better++;
            }
        }
        return better + 1;
    }

    /** Points per player for one KOTH, highest first. */
    public Map<UUID, Integer> pointsFor(String koth) {
        Map<UUID, Integer> map = kothPoints.getOrDefault(koth.toLowerCase(Locale.ROOT), Map.of());
        Map<UUID, Integer> sorted = new LinkedHashMap<>();
        map.entrySet().stream().sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    void forgetKoth(String koth) {
        if (kothPoints.remove(koth.toLowerCase(Locale.ROOT)) != null) {
            dirty = true;
        }
    }

    void reload() {
        save();
        file.reload();
        load();
    }
}
