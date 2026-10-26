package us.copestudios.cskoth;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import us.copestudios.copecore.config.ConfigFile;
import us.copestudios.copecore.region.Region;
import us.copestudios.copecore.scheduler.Tasks;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * All KOTHs (plugins/CSKoth/koths.yml), the running ones, the daily schedule and the rewards.
 *
 * <h2>When a KOTH runs</h2>
 * <ul>
 *   <li>{@code /cskoth start <koth> [minutes]} or the menu. It ends after the first capture
 *       (config.yml capture.end-manual-on-capture), after the minutes if given, or with /cskoth stop.</li>
 *   <li>Scheduled: every day at its start time (config.yml timezone). It ends at the first capture or after
 *       durationMinutes.</li>
 *   <li>Always on: runs all the time. After each capture it pauses for respawnDelay minutes, then starts again.
 *       (In 1.x the pause never happened: always-on KOTHs were never stopped, so the delay did nothing.)</li>
 * </ul>
 *
 * <p>One task ticks every running KOTH once a second, and checks the schedule.</p>
 */
public final class KothManager {

    private final CSKoth plugin;
    private final ConfigFile file;
    /** lowercase name -> KOTH */
    private final Map<String, Koth> koths = new LinkedHashMap<>();
    /** lowercase name -> running game */
    private final Map<String, KothGame> games = new LinkedHashMap<>();
    /** lowercase name -> "yyyy-mm-dd hh:mm" it was last started by the schedule (so it starts once per minute) */
    private final Map<String, String> startedFor = new HashMap<>();
    private BukkitTask ticker;
    private int sinceSave;

    KothManager(CSKoth plugin) {
        this.plugin = plugin;
        this.file = new ConfigFile(plugin, "koths.yml", false);
        load();
    }

    // =====================================================================
    //  koths.yml
    // =====================================================================

    private void load() {
        koths.clear();
        ConfigurationSection root = file.get().getConfigurationSection("koths");
        if (root == null) {
            return;
        }
        int defaultTime = plugin.cfg().getInt("default-capture-time", 300);
        int defaultPoints = plugin.cfg().getInt("default-capture-points", 1);
        boolean oldLayout = false;
        for (String name : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(name);
            if (section == null) {
                continue;
            }
            Koth koth = Koth.load(name, section, defaultTime, defaultPoints);
            if (koth == null) {
                plugin.getLogger().warning("KOTH " + name + " in koths.yml has no zone - skipped.");
                continue;
            }
            oldLayout |= section.contains("x1");
            koths.put(name.toLowerCase(Locale.ROOT), koth);
        }
        if (oldLayout) {
            saveAll();
            plugin.getLogger().info("Converted koths.yml from the CSKoth 1.x layout.");
        }
    }

    /** Writes every KOTH to koths.yml. */
    public void saveAll() {
        YamlConfiguration yaml = file.get();
        yaml.set("koths", null);
        ConfigurationSection root = yaml.createSection("koths");
        for (Koth koth : koths.values()) {
            koth.save(root.createSection(koth.name()));
        }
        file.save();
    }

    /**
     * /cskoth reload. Running KOTHs keep running with the settings they had (edits to them in koths.yml apply
     * after they end); KOTHs removed from the file are stopped.
     */
    void reload() {
        file.reload();
        load();
        for (Map.Entry<String, KothGame> entry : new ArrayList<>(games.entrySet())) {
            if (koths.containsKey(entry.getKey())) {
                koths.put(entry.getKey(), entry.getValue().koth());
            } else {
                stop(entry.getKey(), true);
            }
        }
    }

    // =====================================================================
    //  KOTHs
    // =====================================================================

    public Koth get(String name) {
        return name == null ? null : koths.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<Koth> all() {
        return koths.values();
    }

    public List<String> names() {
        List<String> names = new ArrayList<>();
        koths.values().forEach(koth -> names.add(koth.name()));
        return names;
    }

    public Koth create(String name, Region area) {
        Koth koth = new Koth(name, area.withName(name), plugin.cfg().getInt("default-capture-time", 300),
                plugin.cfg().getInt("default-capture-points", 1));
        koths.put(name.toLowerCase(Locale.ROOT), koth);
        saveAll();
        return koth;
    }

    public void delete(Koth koth) {
        stop(koth.name(), false);
        koths.remove(koth.name().toLowerCase(Locale.ROOT));
        plugin.stats().forgetKoth(koth.name());
        saveAll();
    }

    // =====================================================================
    //  Running
    // =====================================================================

    public boolean isRunning(String name) {
        return name != null && games.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public KothGame game(String name) {
        return name == null ? null : games.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<KothGame> running() {
        return games.values();
    }

    /**
     * Starts a KOTH.
     *
     * @param minutes ends by itself after this many minutes (0 = no limit)
     * @return false if it was already running
     */
    public boolean start(Koth koth, KothGame.Reason reason, int minutes) {
        String key = koth.name().toLowerCase(Locale.ROOT);
        if (games.containsKey(key)) {
            return false;
        }
        long endsAt = minutes > 0 ? System.currentTimeMillis() + minutes * 60_000L : 0L;
        games.put(key, new KothGame(plugin, koth, reason, endsAt));
        Location center = koth.area().center();
        plugin.messages().broadcast(reason == KothGame.Reason.MANUAL ? "koth.started" : "koth.auto-started",
                "koth", koth.name(), "time", KothGame.clock(koth.captureTime()),
                "x", center == null ? "?" : String.valueOf(center.getBlockX()),
                "y", center == null ? "?" : String.valueOf(center.getBlockY()),
                "z", center == null ? "?" : String.valueOf(center.getBlockZ()),
                "world", koth.area().world());
        return true;
    }

    /**
     * Stops a running KOTH.
     *
     * @param announce broadcast "koth.ended"
     * @return false if it wasn't running
     */
    public boolean stop(String name, boolean announce) {
        KothGame game = games.remove(name.toLowerCase(Locale.ROOT));
        if (game == null) {
            return false;
        }
        game.close();
        if (announce) {
            plugin.messages().broadcast("koth.ended", "koth", game.koth().name());
        }
        return true;
    }

    void startTicking() {
        ticker = Tasks.timer(plugin, 20L, 20L, this::tick);
    }

    void stopAll() {
        if (ticker != null) {
            ticker.cancel();
        }
        for (String key : new ArrayList<>(games.keySet())) {
            stop(key, false);
        }
    }

    private void tick() {
        // 1) Running KOTHs.
        for (KothGame game : new ArrayList<>(games.values())) {
            Player winner;
            try {
                winner = game.tick();
            } catch (RuntimeException error) {
                plugin.getLogger().warning("Error in KOTH " + game.koth().name() + ": " + error);
                continue;
            }
            if (winner != null) {
                captured(game, winner);
            } else if (game.endsAt() > 0 && System.currentTimeMillis() >= game.endsAt()) {
                plugin.messages().broadcast("koth.time-up", "koth", game.koth().name());
                stop(game.koth().name(), false);
            }
        }
        // 2) Schedule and always-on.
        LocalDateTime now = LocalDateTime.now(zone());
        String minuteKey = now.toLocalDate() + " " + now.getHour() + ":" + now.getMinute();
        for (Koth koth : koths.values()) {
            String key = koth.name().toLowerCase(Locale.ROOT);
            if (games.containsKey(key)) {
                continue;
            }
            if (koth.alwaysOn()) {
                long wait = koth.respawnDelay() * 60_000L;
                if (koth.lastCapture() == 0 || System.currentTimeMillis() - koth.lastCapture() >= wait) {
                    start(koth, KothGame.Reason.ALWAYS_ON, 0);
                }
                continue;
            }
            if (koth.scheduled() && koth.hasStartTime() && koth.startHour() == now.getHour()
                    && koth.startMinute() == now.getMinute() && !minuteKey.equals(startedFor.get(key))) {
                startedFor.put(key, minuteKey);
                start(koth, KothGame.Reason.SCHEDULED, koth.durationMinutes());
            }
        }
        // 3) Save stats now and then.
        if (++sinceSave >= 30) {
            sinceSave = 0;
            plugin.stats().save();
        }
    }

    /** config.yml timezone ("" = the server's). */
    ZoneId zone() {
        String id = plugin.cfg().getString("timezone", "");
        try {
            return id == null || id.isBlank() ? ZoneId.systemDefault() : ZoneId.of(id);
        } catch (RuntimeException unknown) {
            return ZoneId.systemDefault();
        }
    }

    // =====================================================================
    //  Capturing
    // =====================================================================

    private void captured(KothGame game, Player winner) {
        Koth koth = game.koth();
        koth.captured(winner.getUniqueId().toString(), System.currentTimeMillis());
        plugin.stats().addCapture(winner.getUniqueId(), koth.name(), koth.capturePoints());
        plugin.stats().save();
        plugin.messages().broadcast("capture.captured", "koth", koth.name(), "player", winner.getName(),
                "points", String.valueOf(koth.capturePoints()));
        giveRewards(koth, winner);

        boolean end = switch (game.reason()) {
            case ALWAYS_ON, SCHEDULED -> true;
            case MANUAL -> plugin.cfg().getBoolean("capture.end-manual-on-capture", true);
        };
        if (end) {
            stop(koth.name(), false);
            if (koth.alwaysOn() && koth.respawnDelay() > 0) {
                plugin.messages().broadcast("koth.respawn-in", "koth", koth.name(),
                        "minutes", String.valueOf(koth.respawnDelay()));
            } else {
                plugin.messages().broadcast("koth.ended", "koth", koth.name());
            }
        } else {
            // Keeps running: next capture starts from zero.
            games.put(koth.name().toLowerCase(Locale.ROOT), new KothGame(plugin, koth, game.reason(), game.endsAt()));
            game.close();
        }
        saveAll();
    }

    private void giveRewards(Koth koth, Player winner) {
        List<String> commands = new ArrayList<>(koth.commandRewards());
        commands.addAll(plugin.cfg().getStringList("rewards")); // the global ones from config.yml (same as 1.x)
        for (String command : commands) {
            String line = command.replace("%player%", winner.getName()).replace("{player}", winner.getName())
                    .replace("%koth%", koth.name()).replace("{koth}", koth.name());
            if (line.startsWith("/")) {
                line = line.substring(1);
            }
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line);
            } catch (RuntimeException error) {
                plugin.getLogger().warning("Reward command failed: " + line + " (" + error.getMessage() + ")");
            }
        }
        if (!koth.itemRewards().isEmpty()) {
            for (ItemStack item : koth.itemRewards()) {
                // Full inventory: drop the rest at their feet instead of losing it (1.x lost it).
                winner.getInventory().addItem(item.clone()).values()
                        .forEach(left -> winner.getWorld().dropItemNaturally(winner.getLocation(), left));
            }
            plugin.messages().send(winner, "capture.item-rewards", "koth", koth.name());
        }
    }

    // =====================================================================
    //  Messages near a KOTH
    // =====================================================================

    /** In the KOTH's world and within config.yml broadcast-radius (0 = whole server). */
    public boolean inRange(Koth koth, Player player) {
        int radius = plugin.cfg().getInt("broadcast-radius", 3000);
        if (radius <= 0) {
            return true;
        }
        Location center = koth.area().center();
        return center != null && player.getWorld().equals(center.getWorld())
                && player.getLocation().distanceSquared(center) <= (double) radius * radius;
    }

    /** Sends a message to everyone in range of the KOTH. */
    public void announce(Koth koth, String key, String... pairs) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (inRange(koth, player)) {
                plugin.messages().send(player, key, pairs);
            }
        }
    }

    // =====================================================================
    //  Next scheduled KOTH
    // =====================================================================

    /** The scheduled KOTH that starts next, or null. */
    public Koth next() {
        Koth best = null;
        long bestWait = Long.MAX_VALUE;
        for (Koth koth : koths.values()) {
            long wait = secondsUntilStart(koth);
            if (wait >= 0 && wait < bestWait) {
                bestWait = wait;
                best = koth;
            }
        }
        return best;
    }

    /** Seconds until the KOTH's next scheduled start, or -1 if it has no schedule. */
    public long secondsUntilStart(Koth koth) {
        if (!koth.scheduled() || !koth.hasStartTime()) {
            return -1;
        }
        LocalDateTime now = LocalDateTime.now(zone());
        LocalDateTime start = now.withHour(koth.startHour()).withMinute(koth.startMinute()).withSecond(0).withNano(0);
        if (!start.isAfter(now)) {
            start = start.plusDays(1);
        }
        return java.time.Duration.between(now, start).getSeconds();
    }

    /** Shows the next KOTH (/cskoth next). */
    void showNext(CommandSender to) {
        Koth next = next();
        if (next == null) {
            plugin.messages().send(to, "next.none");
            return;
        }
        long seconds = secondsUntilStart(next);
        plugin.messages().send(to, "next.koth", "koth", next.name(), "time", next.startTimeText(),
                "countdown", us.copestudios.copecore.util.TimeFormat.duration(seconds * 1000));
    }
}
