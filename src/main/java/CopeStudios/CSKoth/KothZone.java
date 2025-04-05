package CopeStudios.CSKoth;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class KothZone {
    private final String name;
    private Location corner1;
    private Location corner2;
    private int captureTime;
    private int capturePoints;

    // Scheduling properties
    private boolean scheduled = false;
    private int startHour = -1;
    private int startMinute = -1;
    private int durationMinutes = 60;
    private boolean alwaysOn = false;
    private int respawnDelay = 30; // Minutes before respawning when always on

    // Rewards
    private List<ItemStack> physicalRewards = new ArrayList<>();
    private List<String> commandRewards = new ArrayList<>();

    public KothZone(String name, Location corner1, Location corner2, int captureTime, int capturePoints) {
        this.name = name;
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.captureTime = captureTime;
        this.capturePoints = capturePoints;
    }

    public String getName() {
        return name;
    }

    public Location getCorner1() {
        return corner1;
    }

    public Location getCorner2() {
        return corner2;
    }

    public void setCorners(Location corner1, Location corner2) {
        this.corner1 = corner1;
        this.corner2 = corner2;
    }

    public int getCaptureTime() {
        return captureTime;
    }

    public void setCaptureTime(int captureTime) {
        this.captureTime = captureTime;
    }

    public int getCapturePoints() {
        return capturePoints;
    }

    public void setCapturePoints(int capturePoints) {
        this.capturePoints = capturePoints;
    }

    public boolean isScheduled() {
        return scheduled;
    }

    public void setScheduled(boolean scheduled) {
        this.scheduled = scheduled;
    }

    public int getStartHour() {
        return startHour;
    }

    public void setStartHour(int startHour) {
        this.startHour = startHour;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public void setStartMinute(int startMinute) {
        this.startMinute = startMinute;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public boolean isAlwaysOn() {
        return alwaysOn;
    }

    public void setAlwaysOn(boolean alwaysOn) {
        this.alwaysOn = alwaysOn;
    }

    public int getRespawnDelay() {
        return respawnDelay;
    }

    public void setRespawnDelay(int respawnDelay) {
        this.respawnDelay = respawnDelay;
    }

    public List<ItemStack> getPhysicalRewards() {
        return physicalRewards;
    }

    public void setPhysicalRewards(List<ItemStack> physicalRewards) {
        this.physicalRewards = physicalRewards;
    }

    public List<String> getCommandRewards() {
        return commandRewards;
    }

    public void setCommandRewards(List<String> commandRewards) {
        this.commandRewards = commandRewards;
    }

    public boolean isInside(Location location) {
        if (location.getWorld() != corner1.getWorld()) {
            return false;
        }

        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

        return location.getBlockX() >= minX && location.getBlockX() <= maxX &&
                location.getBlockY() >= minY && location.getBlockY() <= maxY &&
                location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
    }
}