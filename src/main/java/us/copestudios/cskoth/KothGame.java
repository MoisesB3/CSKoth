package us.copestudios.cskoth;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import us.copestudios.copecore.util.TimeFormat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * A KOTH that is running right now. {@link #tick()} runs once a second.
 *
 * <h2>Rules (same as 1.x)</h2>
 * <ul>
 *   <li>One player alone in the zone captures: their time goes up by 1 every second.</li>
 *   <li>Two or more players: contested, the time stops (it isn't lost).</li>
 *   <li>The capturer leaves and someone else steps in alone: that player starts from zero.</li>
 *   <li>Nobody in the zone: the capture is lost (config.yml capture.reset-when-empty).</li>
 *   <li>Spectators and dead players don't count.</li>
 * </ul>
 */
public final class KothGame {

    /** Why it started (decides what happens after a capture). */
    public enum Reason { MANUAL, SCHEDULED, ALWAYS_ON }

    private final CSKoth plugin;
    private final Koth koth;
    private final Reason reason;
    private final long startedAt = System.currentTimeMillis();
    /** When it ends by itself (0 = only by capture or /cskoth stop). */
    private final long endsAt;

    private UUID captor;
    private int progress;
    private boolean contested;
    private final Set<UUID> contesting = new LinkedHashSet<>();
    private final List<String> inZoneNames = new ArrayList<>();
    private int sinceMessage;
    private BossBar bar;

    KothGame(CSKoth plugin, Koth koth, Reason reason, long endsAt) {
        this.plugin = plugin;
        this.koth = koth;
        this.reason = reason;
        this.endsAt = endsAt;
        if (plugin.cfg().getBoolean("bossbar.enabled", true)) {
            bar = Bukkit.createBossBar("", color(), style());
        }
    }

    private BarColor color() {
        try {
            return BarColor.valueOf(plugin.cfg().getString("bossbar.color", "YELLOW").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return BarColor.YELLOW;
        }
    }

    private BarStyle style() {
        try {
            return BarStyle.valueOf(plugin.cfg().getString("bossbar.style", "SEGMENTED_10").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return BarStyle.SEGMENTED_10;
        }
    }

    /**
     * One second passed.
     *
     * @return the player who just captured the KOTH, or null
     */
    Player tick() {
        List<Player> inside = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isDead() && player.getGameMode() != GameMode.SPECTATOR
                    && koth.area().contains(player.getLocation())) {
                inside.add(player);
            }
        }
        inZoneNames.clear();
        inside.forEach(player -> inZoneNames.add(player.getName()));
        sinceMessage++;
        int interval = Math.max(1, plugin.cfg().getInt("capture.progress-message-seconds", 15));
        Player winner = null;

        if (inside.isEmpty()) {
            if (progress > 0 && plugin.cfg().getBoolean("capture.reset-when-empty", true)) {
                plugin.koths().announce(koth, "capture.lost", "koth", koth.name());
                reset();
            }
            contested = false;
        } else if (inside.size() == 1) {
            Player player = inside.get(0);
            if (player.getUniqueId().equals(captor)) {
                if (contested) {
                    contested = false;
                    plugin.koths().announce(koth, "capture.resumed", "koth", koth.name(), "player", player.getName());
                }
                progress++;
                if (sinceMessage >= interval) {
                    plugin.koths().announce(koth, "capture.progress", "koth", koth.name(), "player", player.getName(),
                            "time", clock(remaining()));
                    sinceMessage = 0;
                }
                if (progress >= koth.captureTime()) {
                    winner = player;
                }
            } else {
                // Someone new (or the first one): start from zero.
                captor = player.getUniqueId();
                progress = 1;
                contested = false;
                contesting.clear();
                sinceMessage = 0;
                plugin.koths().announce(koth, "capture.started", "koth", koth.name(), "player", player.getName(),
                        "time", clock(remaining()));
                if (progress >= koth.captureTime()) {
                    winner = player;
                }
            }
        } else {
            if (!contested) {
                contested = true;
                sinceMessage = 0;
                plugin.koths().announce(koth, "capture.contested", "koth", koth.name());
                for (Player player : inside) {
                    if (contesting.add(player.getUniqueId())) {
                        plugin.stats().addContest(player.getUniqueId());
                    }
                }
            } else if (sinceMessage >= interval) {
                Player holder = captor == null ? null : Bukkit.getPlayer(captor);
                if (holder != null && inside.contains(holder)) {
                    plugin.koths().announce(koth, "capture.contested-progress", "koth", koth.name(),
                            "player", holder.getName(), "time", clock(remaining()));
                }
                sinceMessage = 0;
            }
        }
        updateBar();
        return winner;
    }

    private void reset() {
        captor = null;
        progress = 0;
        contested = false;
        contesting.clear();
    }

    // =====================================================================
    //  Boss bar
    // =====================================================================

    private void updateBar() {
        if (bar == null) {
            return;
        }
        String key;
        String[] pairs;
        Player holder = captor == null ? null : Bukkit.getPlayer(captor);
        if (contested) {
            key = "bossbar.contested";
            pairs = new String[]{"koth", koth.name(), "player", holder == null ? "-" : holder.getName(),
                    "time", clock(remaining())};
        } else if (holder != null && progress > 0) {
            key = "bossbar.capturing";
            pairs = new String[]{"koth", koth.name(), "player", holder.getName(), "time", clock(remaining())};
        } else {
            key = "bossbar.waiting";
            pairs = new String[]{"koth", koth.name(), "time", clock(koth.captureTime()),
                    "ends", endsAt == 0 ? "-" : clock((int) Math.max(0, (endsAt - System.currentTimeMillis()) / 1000))};
        }
        bar.setTitle(plugin.messages().legacy(key, pairs));
        bar.setProgress(Math.max(0.0, Math.min(1.0, progress / (double) koth.captureTime())));
        bar.setColor(contested ? BarColor.RED : color());

        // Show it to everyone the chat messages reach.
        Set<Player> shouldSee = new LinkedHashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.koths().inRange(koth, player)) {
                shouldSee.add(player);
            }
        }
        for (Player player : new ArrayList<>(bar.getPlayers())) {
            if (!shouldSee.contains(player)) {
                bar.removePlayer(player);
            }
        }
        for (Player player : shouldSee) {
            if (!bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
            }
        }
    }

    /** Stops showing the boss bar (the game ended). */
    void close() {
        if (bar != null) {
            bar.removeAll();
            bar = null;
        }
    }

    static String clock(int seconds) {
        return TimeFormat.clock(Math.max(0, seconds));
    }

    /** Seconds as "m:ss" (for menus). */
    public static String clockText(int seconds) {
        return clock(seconds);
    }

    // =====================================================================
    //  Read by placeholders, menus, commands
    // =====================================================================

    public Koth koth() {
        return koth;
    }

    public Reason reason() {
        return reason;
    }

    public UUID captor() {
        return captor;
    }

    public int progress() {
        return progress;
    }

    /** Seconds the current capturer still needs. */
    public int remaining() {
        return Math.max(0, koth.captureTime() - progress);
    }

    public boolean contested() {
        return contested;
    }

    /** Players who contested during the current capture. */
    public Set<UUID> contesting() {
        return contesting;
    }

    public List<String> playersInZone() {
        return inZoneNames;
    }

    public long startedAt() {
        return startedAt;
    }

    public long endsAt() {
        return endsAt;
    }

    /** Seconds until it ends by itself, or -1. */
    public int secondsLeft() {
        return endsAt == 0 ? -1 : (int) Math.max(0, (endsAt - System.currentTimeMillis()) / 1000);
    }

    Location center() {
        return koth.area().center();
    }
}
