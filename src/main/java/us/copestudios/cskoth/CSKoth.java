package us.copestudios.cskoth;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import us.copestudios.copecore.CopeCore;
import us.copestudios.copecore.config.ConfigFile;
import us.copestudios.copecore.region.Region;
import us.copestudios.copecore.region.Selection;
import us.copestudios.copecore.text.Messages;

import java.util.List;
import java.util.Locale;

/**
 * CSKoth: King of the Hill. Hold a zone alone long enough to capture it and win points and rewards.
 *
 * <h2>How it fits together</h2>
 * <ul>
 *   <li>{@link Koth} - one KOTH (zone + rules), saved in koths.yml</li>
 *   <li>{@link KothManager} - all KOTHs, starting/stopping, the daily schedule, rewards</li>
 *   <li>{@link KothGame} - a running KOTH: who is capturing, contested, boss bar</li>
 *   <li>{@link Stats} - captures/contests/points per player (stats.yml)</li>
 *   <li>{@link KothCommand} - /cskoth (/koth)</li>
 *   <li>{@link us.copestudios.cskoth.gui.KothMenus} - the admin menus</li>
 *   <li>{@link Placeholders}, {@link LegacyPapi} - %cskoth_...% and %cope_koth_...%</li>
 *   <li>{@link LegacyMigration} - one-time upgrade of the 1.x messages.yml</li>
 * </ul>
 *
 * <p>About 1.x: its last version never loaded koths.yml, never registered its command, events or schedule
 * (onEnable stopped at "Rest of your initialization code..."), and didn't compile. Everything it meant to do is
 * here, built on CopeCore (messages, commands, menus, chat questions on the main thread, regions, the wand).</p>
 */
public final class CSKoth extends JavaPlugin {

    private static final int REQUIRED_CORE_API = 2;
    public static final String ADMIN = "cskoth.admin";
    public static final String USE = "cskoth.use";

    private ConfigFile config;
    private Messages messages;
    private Stats stats;
    private KothManager koths;

    @Override
    public void onEnable() {
        if (!CopeCore.requireApi(this, REQUIRED_CORE_API)) {
            return;
        }
        LegacyMigration migration = new LegacyMigration(this);
        migration.prepareFiles();

        config = new ConfigFile(this, "config.yml");
        messages = CopeCore.messages(this);
        migration.importOldMessages();

        stats = new Stats(this);
        koths = new KothManager(this);
        koths.startTicking();
        new KothCommand(this).register();
        new Placeholders(this).register();

        getLogger().info("CSKoth enabled - " + koths.all().size() + " KOTH(s).");
    }

    @Override
    public void onDisable() {
        if (koths != null) {
            koths.stopAll();
            koths.saveAll();
        }
        if (stats != null) {
            stats.save();
        }
    }

    /** /cskoth reload */
    void reload() {
        config.reload();
        messages.reload();
        stats.reload();
        koths.reload();
    }

    // =====================================================================
    //  Creating and resizing zones (used by the command and the menus)
    // =====================================================================

    /** Letters, numbers, - and _ (max 32). */
    public static boolean validName(String name) {
        return name.matches("[A-Za-z0-9_-]{1,32}");
    }

    /**
     * Makes a KOTH: from the player's CopeCore wand selection if they have one, otherwise a box around them
     * (config.yml default-size, like 1.x: 11 x 5 x 11).
     *
     * @return the new KOTH, or null if the name is bad or taken (a message was sent)
     */
    public Koth create(Player player, String name) {
        if (!validName(name)) {
            messages.send(player, "koth.bad-name");
            return null;
        }
        if (koths.get(name) != null) {
            messages.send(player, "koth.exists", "koth", name);
            return null;
        }
        Region area = selection(player, name);
        boolean fromWand = area != null;
        if (area == null) {
            Location here = player.getLocation();
            int radius = Math.max(0, cfg().getInt("default-size.radius", 5));
            int below = Math.max(0, cfg().getInt("default-size.below", 1));
            int above = Math.max(0, cfg().getInt("default-size.above", 3));
            area = new Region(name, here.getWorld().getName(),
                    here.getBlockX() - radius, here.getBlockY() - below, here.getBlockZ() - radius,
                    here.getBlockX() + radius, here.getBlockY() + above, here.getBlockZ() + radius);
        }
        Koth koth = koths.create(name, area);
        CopeCore.regions().showOutline(player, koth.area());
        messages.send(player, fromWand ? "koth.created-wand" : "koth.created", "koth", koth.name(),
                "size", area.sizeX() + "x" + area.sizeY() + "x" + area.sizeZ());
        return koth;
    }

    /** The player's complete wand selection as a region, or null. */
    public Region selection(Player player, String name) {
        Selection selection = CopeCore.regions().selection(player);
        if (selection.isComplete() && !selection.isDifferentWorlds()) {
            return selection.toRegion(name);
        }
        return null;
    }

    /**
     * Sets a KOTH's zone: the wand selection right away, or two "here" questions in chat.
     *
     * @param after runs when it's done or cancelled (e.g. re-open a menu); may be null
     */
    public void askZone(Player player, Koth koth, Runnable after) {
        Runnable done = after == null ? () -> { } : after;
        Region wand = selection(player, koth.name());
        if (wand != null) {
            setZone(player, koth, wand);
            done.run();
            return;
        }
        CopeCore.prompts().here(player, messages.format("zone.corner", "number", "1", "koth", koth.name()), first ->
                CopeCore.prompts().here(player, messages.format("zone.corner", "number", "2", "koth", koth.name()), second -> {
                    if (second.getWorld() == null || !second.getWorld().equals(first.getWorld())) {
                        messages.send(player, "zone.same-world");
                    } else {
                        setZone(player, koth, Region.of(koth.name(), first, second));
                    }
                    done.run();
                }, done), done);
    }

    public void setZone(Player player, Koth koth, Region area) {
        koth.area(area);
        koths.saveAll();
        CopeCore.regions().showOutline(player, koth.area());
        messages.send(player, "zone.updated", "koth", koth.name(),
                "size", area.sizeX() + "x" + area.sizeY() + "x" + area.sizeZ());
    }

    /** Grows (blocks &gt; 0) or shrinks a zone on every side. @return false if it would become too small */
    public boolean resize(Koth koth, int blocks) {
        Region area = koth.area();
        if (blocks < 0 && (area.sizeX() + 2 * blocks < 1 || area.sizeY() + 2 * blocks < 1 || area.sizeZ() + 2 * blocks < 1)) {
            return false;
        }
        koth.area(area.expand(blocks));
        koths.saveAll();
        return true;
    }

    // ----- getters -----

    public YamlConfiguration cfg() {
        return config.get();
    }

    public Messages messages() {
        return messages;
    }

    public Stats stats() {
        return stats;
    }

    public KothManager koths() {
        return koths;
    }

    /** KOTH names for tab completion. */
    public List<String> names() {
        return koths.names();
    }

    static String lower(String text) {
        return text.toLowerCase(Locale.ROOT);
    }
}
