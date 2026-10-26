package us.copestudios.cskoth;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import us.copestudios.copecore.region.Region;

import java.util.ArrayList;
import java.util.List;

/**
 * One KOTH: its zone and its rules. What happens while it runs is in {@link KothGame}.
 *
 * <h2>koths.yml</h2>
 * The same keys as CSKoth 1.x, so old files load as they are. Only the corners changed: 1.x wrote
 * {@code world, x1, y1, z1, x2, y2, z2}; now the zone is a CopeCore region under {@code area}. The old keys are still
 * read.
 * <pre>
 * koths:
 *   desert:
 *     area: {world, min: {x,y,z}, max: {x,y,z}}
 *     captureTime: 300          # seconds a player must hold the zone alone
 *     capturePoints: 1          # points for capturing it
 *     scheduled: false          # start every day at startHour:startMinute
 *     startHour: 18
 *     startMinute: 0
 *     durationMinutes: 60       # scheduled KOTHs end after this (if nobody captured it)
 *     alwaysOn: false           # restart by itself respawnDelay minutes after each capture
 *     respawnDelay: 30
 *     commandRewards: ["give %player% diamond 5"]
 *     physicalRewards: {'0': item, ...}
 *     lastWinner: uuid          # (new) for %cskoth_last_winner_desert%
 *     lastCapture: 1700000000000  # (new) so always-on KOTHs keep their delay across restarts
 * </pre>
 */
public final class Koth {

    private final String name;
    private Region area;
    private int captureTime;
    private int capturePoints;
    private boolean scheduled;
    private int startHour = -1;
    private int startMinute = -1;
    private int durationMinutes = 60;
    private boolean alwaysOn;
    private int respawnDelay = 30;
    private List<ItemStack> itemRewards = new ArrayList<>();
    private List<String> commandRewards = new ArrayList<>();
    private String lastWinner;
    private long lastCapture;

    public Koth(String name, Region area, int captureTime, int capturePoints) {
        this.name = name;
        this.area = area;
        this.captureTime = Math.max(1, captureTime);
        this.capturePoints = Math.max(0, capturePoints);
    }

    // =====================================================================
    //  File
    // =====================================================================

    /** Reads a KOTH (new "area" layout, or the 1.x x1..z2 keys). Null if the zone is missing. */
    static Koth load(String name, ConfigurationSection section, int defaultTime, int defaultPoints) {
        Region area = Region.load(name, section.getConfigurationSection("area"));
        if (area == null && section.getString("world") != null && section.contains("x1")) {
            area = new Region(name, section.getString("world"),
                    section.getInt("x1"), section.getInt("y1"), section.getInt("z1"),
                    section.getInt("x2"), section.getInt("y2"), section.getInt("z2"));
        }
        if (area == null) {
            return null;
        }
        Koth koth = new Koth(name, area, section.getInt("captureTime", defaultTime),
                section.getInt("capturePoints", defaultPoints));
        koth.scheduled = section.getBoolean("scheduled", false);
        koth.startHour = section.getInt("startHour", -1);
        koth.startMinute = section.getInt("startMinute", -1);
        koth.durationMinutes = Math.max(1, section.getInt("durationMinutes", 60));
        koth.alwaysOn = section.getBoolean("alwaysOn", false);
        koth.respawnDelay = Math.max(0, section.getInt("respawnDelay", 30));
        koth.commandRewards = new ArrayList<>(section.getStringList("commandRewards"));
        koth.lastWinner = section.getString("lastWinner");
        koth.lastCapture = section.getLong("lastCapture", 0L);
        ConfigurationSection items = section.getConfigurationSection("physicalRewards");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ItemStack item = items.getItemStack(key);
                if (item != null && !item.getType().isAir()) {
                    koth.itemRewards.add(item);
                }
            }
        }
        return koth;
    }

    void save(ConfigurationSection section) {
        area.save(section.createSection("area"));
        section.set("captureTime", captureTime);
        section.set("capturePoints", capturePoints);
        section.set("scheduled", scheduled);
        section.set("startHour", startHour);
        section.set("startMinute", startMinute);
        section.set("durationMinutes", durationMinutes);
        section.set("alwaysOn", alwaysOn);
        section.set("respawnDelay", respawnDelay);
        section.set("commandRewards", commandRewards);
        ConfigurationSection items = section.createSection("physicalRewards");
        for (int i = 0; i < itemRewards.size(); i++) {
            items.set(String.valueOf(i), itemRewards.get(i));
        }
        section.set("lastWinner", lastWinner);
        section.set("lastCapture", lastCapture == 0 ? null : lastCapture);
    }

    // =====================================================================
    //  Getters / setters
    // =====================================================================

    public String name() {
        return name;
    }

    public Region area() {
        return area;
    }

    public void area(Region area) {
        this.area = area.withName(name);
    }

    public int captureTime() {
        return captureTime;
    }

    public void captureTime(int seconds) {
        this.captureTime = Math.max(1, seconds);
    }

    public int capturePoints() {
        return capturePoints;
    }

    public void capturePoints(int points) {
        this.capturePoints = Math.max(0, points);
    }

    public boolean scheduled() {
        return scheduled;
    }

    public void scheduled(boolean scheduled) {
        this.scheduled = scheduled;
    }

    /** True if a start time is set. */
    public boolean hasStartTime() {
        return startHour >= 0 && startHour <= 23 && startMinute >= 0 && startMinute <= 59;
    }

    public int startHour() {
        return startHour;
    }

    public int startMinute() {
        return startMinute;
    }

    public void startTime(int hour, int minute) {
        this.startHour = hour;
        this.startMinute = minute;
    }

    /** "18:05", or "-" when not set. */
    public String startTimeText() {
        return hasStartTime() ? String.format("%02d:%02d", startHour, startMinute) : "-";
    }

    public int durationMinutes() {
        return durationMinutes;
    }

    public void durationMinutes(int minutes) {
        this.durationMinutes = Math.max(1, minutes);
    }

    public boolean alwaysOn() {
        return alwaysOn;
    }

    public void alwaysOn(boolean alwaysOn) {
        this.alwaysOn = alwaysOn;
    }

    public int respawnDelay() {
        return respawnDelay;
    }

    public void respawnDelay(int minutes) {
        this.respawnDelay = Math.max(0, minutes);
    }

    public List<ItemStack> itemRewards() {
        return itemRewards;
    }

    public void itemRewards(List<ItemStack> items) {
        this.itemRewards = new ArrayList<>(items);
    }

    public List<String> commandRewards() {
        return commandRewards;
    }

    public String lastWinner() {
        return lastWinner;
    }

    public long lastCapture() {
        return lastCapture;
    }

    void captured(String winnerUuid, long when) {
        this.lastWinner = winnerUuid;
        this.lastCapture = when;
    }
}
