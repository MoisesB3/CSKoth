package us.copestudios.cskoth;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import us.copestudios.copecore.CopeCore;
import us.copestudios.copecore.util.TimeFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Placeholders. Every one works with both names:
 * <ul>
 *   <li>{@code %cskoth_<key>%} - the 1.x names (see {@link LegacyPapi}), so scoreboards keep working</li>
 *   <li>{@code %cope_koth_<key>%} - through CopeCore, like the other Cope plugins</li>
 * </ul>
 *
 * <pre>
 * Player:   captures (= wins), contests, points, rank, topwins, player_in_koth
 * Server:   active, total_active, total_koths, next, next_time, next_countdown
 * Leaderboard (new): top_&lt;1-10&gt;_name, top_&lt;1-10&gt;_captures
 * Per KOTH (replace &lt;koth&gt; with its name):
 *   points_&lt;koth&gt;  captime_&lt;koth&gt;  status_&lt;koth&gt;  last_winner_&lt;koth&gt;  player_distance_&lt;koth&gt;
 *   capturer_&lt;koth&gt;  progress_&lt;koth&gt;  remaining_&lt;koth&gt;  capture_time_left_&lt;koth&gt;  duration_left_&lt;koth&gt;
 *   contested_&lt;koth&gt;  contested_players_&lt;koth&gt;  coordinates_&lt;koth&gt;
 * </pre>
 */
final class Placeholders {

    private final CSKoth plugin;

    Placeholders(CSKoth plugin) {
        this.plugin = plugin;
    }

    void register() {
        CopeCore.placeholders().register(plugin, "koth", this::resolve);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                LegacyPapi.register(plugin, this);
            } catch (Throwable error) {
                plugin.getLogger().warning("Could not register the %cskoth_...% placeholders: " + error);
            }
        }
    }

    /**
     * Value of a placeholder (null = unknown). Scoreboard plugins may ask from another thread, so any error from
     * a KOTH changing at that moment just gives "" instead of an error in the console.
     */
    String resolve(OfflinePlayer player, String raw) {
        try {
            return resolveNow(player, raw);
        } catch (RuntimeException busy) {
            return "";
        }
    }

    private String resolveNow(OfflinePlayer player, String raw) {
        String key = raw.toLowerCase(Locale.ROOT);
        KothManager koths = plugin.koths();
        Stats stats = plugin.stats();

        switch (key) {
            case "active": {
                List<String> names = new ArrayList<>();
                koths.running().forEach(game -> names.add(game.koth().name()));
                return names.isEmpty() ? text("none") : String.join(", ", names);
            }
            case "total_active":
                return String.valueOf(koths.running().size());
            case "total_koths":
                return String.valueOf(koths.all().size());
            case "next": {
                Koth next = koths.next();
                return next == null ? text("none") : next.name();
            }
            case "next_time": {
                Koth next = koths.next();
                return next == null ? "-" : next.startTimeText();
            }
            case "next_countdown": {
                Koth next = koths.next();
                return next == null ? "-" : TimeFormat.duration(koths.secondsUntilStart(next) * 1000);
            }
            case "topwins": {
                StringBuilder out = new StringBuilder();
                int rank = 1;
                for (Stats.Entry entry : stats.top(10)) {
                    out.append(rank++).append(". ").append(nameOf(entry.player())).append(": ")
                            .append(entry.captures()).append('\n');
                }
                return out.toString().trim();
            }
            default:
                break;
        }

        if (key.startsWith("top_")) {
            String[] parts = key.split("_");
            if (parts.length == 3) {
                try {
                    int place = Integer.parseInt(parts[1]);
                    List<Stats.Entry> top = stats.top(place);
                    if (place < 1 || top.size() < place) {
                        return parts[2].equals("name") ? "-" : "0";
                    }
                    Stats.Entry entry = top.get(place - 1);
                    return parts[2].equals("name") ? nameOf(entry.player()) : String.valueOf(entry.captures());
                } catch (NumberFormatException notANumber) {
                    return null;
                }
            }
        }

        // ----- per KOTH (longest prefixes first) -----
        String[] prefixes = {"contested_players_", "capture_time_left_", "duration_left_", "player_distance_",
                "last_winner_", "coordinates_", "contested_", "remaining_", "progress_", "capturer_", "captime_",
                "status_", "points_"};
        for (String prefix : prefixes) {
            if (key.startsWith(prefix)) {
                Koth koth = koths.get(raw.substring(prefix.length()));
                return koth == null ? "" : perKoth(prefix, koth, player);
            }
        }

        // ----- player -----
        if (player == null) {
            return "";
        }
        Stats.Entry own = stats.of(player.getUniqueId());
        switch (key) {
            case "captures":
            case "wins":
                return String.valueOf(own.captures());
            case "contests":
                return String.valueOf(own.contests());
            case "points":
                return String.valueOf(own.points());
            case "rank": {
                int rank = stats.rank(player.getUniqueId());
                return rank > 0 ? String.valueOf(rank) : "N/A";
            }
            case "player_in_koth": {
                Player online = player.getPlayer();
                if (online == null) {
                    return "false";
                }
                for (KothGame game : koths.running()) {
                    if (game.koth().area().contains(online.getLocation())) {
                        return "true";
                    }
                }
                return "false";
            }
            default:
                return null;
        }
    }

    private String perKoth(String prefix, Koth koth, OfflinePlayer player) {
        KothGame game = plugin.koths().game(koth.name());
        switch (prefix) {
            case "points_":
                return String.valueOf(koth.capturePoints());
            case "captime_":
                return String.valueOf(koth.captureTime());
            case "status_":
                return game != null ? text("active") : text("inactive");
            case "last_winner_":
                return koth.lastWinner() == null ? text("none") : nameOf(UUID.fromString(koth.lastWinner()));
            case "coordinates_": {
                Location center = koth.area().center();
                return center == null ? "N/A"
                        : center.getBlockX() + ", " + center.getBlockY() + ", " + center.getBlockZ();
            }
            case "player_distance_": {
                Player online = player == null ? null : player.getPlayer();
                Location center = koth.area().center();
                if (online == null || center == null || !online.getWorld().equals(center.getWorld())) {
                    return "-";
                }
                return String.valueOf((int) online.getLocation().distance(center));
            }
            case "capturer_":
                return game == null || game.captor() == null ? text("none") : nameOf(game.captor());
            case "progress_":
                return game == null ? "0" : String.valueOf(game.progress());
            case "remaining_":
            case "capture_time_left_":
                return game == null || game.captor() == null ? "0" : String.valueOf(game.remaining());
            case "duration_left_":
                return game == null || game.secondsLeft() < 0 ? "-" : TimeFormat.clock(game.secondsLeft());
            case "contested_":
                return String.valueOf(game != null && game.contested());
            case "contested_players_": {
                if (game == null || !game.contested()) {
                    return text("none");
                }
                return String.join(", ", game.playersInZone());
            }
            default:
                return "";
        }
    }

    private String text(String key) {
        return us.copestudios.copecore.text.Text.plain(plugin.messages().format("placeholder." + key));
    }

    private static String nameOf(UUID id) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(id);
        return player.getName() == null ? "?" : player.getName();
    }
}
