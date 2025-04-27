package CopeStudios.CSKoth;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Calendar;
import java.util.Map;

public class KothCommand implements CommandExecutor {
    private final CSKoth plugin;

    public KothCommand(CSKoth plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                plugin.getGui().openMainMenu((Player) sender);
            } else {
                sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "This command can only be used by players.");
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "placeholdertest":
                if (!sender.hasPermission("cskoth.admin")) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }

                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "This command can only be used by players.");
                    return true;
                }

                plugin.getPlaceholders().testPlaceholders((Player) sender);
                return true;

            case "menu":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "This command can only be used by players.");
                    return true;
                }
                plugin.getGui().openMainMenu((Player) sender);
                return true;

            case "create":
                if (!sender.hasPermission("cskoth.admin")) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /cskoth create <name>");
                    return true;
                }

                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "This command can only be used by players.");
                    return true;
                }

                plugin.createKoth((Player) sender, args[1]);
                return true;

            case "start":
                if (!sender.hasPermission("cskoth.admin")) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /cskoth start <name>");
                    return true;
                }

                String startKothName = args[1];

                if (sender instanceof Player) {
                    plugin.startKoth((Player) sender, startKothName);
                } else {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "This command can only be used by players.");
                }
                return true;

            case "stop":
                if (!sender.hasPermission("cskoth.admin")) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /cskoth stop <name>");
                    return true;
                }

                String stopKothName = args[1];

                if (sender instanceof Player) {
                    plugin.stopKoth((Player) sender, stopKothName);
                } else {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "This command can only be used by players.");
                }
                return true;

            case "list":
                if (!sender.hasPermission("cskoth.admin")) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }

                sender.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Available KOTHs:");
                for (String kothZoneName : plugin.getKothZones().keySet()) {
                    boolean isActive = plugin.getActiveKoths().containsKey(kothZoneName);
                    sender.sendMessage(ChatColor.GOLD + "- " + kothZoneName +
                            (isActive ? ChatColor.GREEN + " (Active)" : ChatColor.RED + " (Inactive)"));
                }
                return true;

            case "points":
                String targetKoth = null;

                if (args.length >= 2) {
                    targetKoth = args[1];
                    if (!plugin.getKothZones().containsKey(targetKoth)) {
                        sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "KOTH not found: " + targetKoth);
                        return true;
                    }
                }

                plugin.showKothPoints(sender, targetKoth);
                return true;

            case "next":
                showNextKoth(sender);
                return true;

            case "help":
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void showNextKoth(CommandSender sender) {
        Map<String, KothZone> kothZones = plugin.getKothZones();
        if (kothZones.isEmpty()) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "There are no KOTHs configured.");
            return;
        }

        // Get current time
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);

        // Convert to minutes for easier comparison
        int currentTimeInMinutes = currentHour * 60 + currentMinute;

        String nextKothName = null;
        int nextKothTime = Integer.MAX_VALUE;

        // Find the next scheduled KOTH
        for (Map.Entry<String, KothZone> entry : kothZones.entrySet()) {
            KothZone kothZone = entry.getValue();

            if (kothZone.isScheduled() && kothZone.getStartHour() >= 0 && kothZone.getStartMinute() >= 0) {
                int kothTimeInMinutes = kothZone.getStartHour() * 60 + kothZone.getStartMinute();

                // Calculate time until next KOTH (in minutes)
                int timeUntilKoth;
                if (kothTimeInMinutes > currentTimeInMinutes) {
                    // Later today
                    timeUntilKoth = kothTimeInMinutes - currentTimeInMinutes;
                } else {
                    // Tomorrow
                    timeUntilKoth = (24 * 60) - currentTimeInMinutes + kothTimeInMinutes;
                }

                if (timeUntilKoth < nextKothTime) {
                    nextKothTime = timeUntilKoth;
                    nextKothName = entry.getKey();
                }
            }
        }

        if (nextKothName == null) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "There are no scheduled KOTHs.");
        } else {
            int hours = nextKothTime / 60;
            int minutes = nextKothTime % 60;

            KothZone nextKoth = kothZones.get(nextKothName);
            String startTime = String.format("%02d:%02d", nextKoth.getStartHour(), nextKoth.getStartMinute());

            sender.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Next KOTH: " + ChatColor.GOLD + nextKothName);
            sender.sendMessage(ChatColor.YELLOW + "Starting at: " + ChatColor.GREEN + startTime);
            sender.sendMessage(ChatColor.YELLOW + "Time until start: " + ChatColor.GREEN +
                    (hours > 0 ? hours + " hours " : "") + minutes + " minutes");
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "CSKoth Commands:");
        sender.sendMessage(ChatColor.GOLD + "/cskoth" + ChatColor.GRAY + " - Open the KOTH menu");
        sender.sendMessage(ChatColor.GOLD + "/cskoth menu" + ChatColor.GRAY + " - Open the KOTH menu");
        sender.sendMessage(ChatColor.GOLD + "/cskoth next" + ChatColor.GRAY + " - Show the next scheduled KOTH");
        if (sender.hasPermission("cskoth.admin")) {
            sender.sendMessage(ChatColor.GOLD + "/cskoth create <name>" + ChatColor.GRAY + " - Create a new KOTH at your location");
            sender.sendMessage(ChatColor.GOLD + "/cskoth start <name>" + ChatColor.GRAY + " - Start a KOTH event");
            sender.sendMessage(ChatColor.GOLD + "/cskoth stop <name>" + ChatColor.GRAY + " - Stop a KOTH event");
            sender.sendMessage(ChatColor.GOLD + "/cskoth list" + ChatColor.GRAY + " - List all KOTHs");
            sender.sendMessage(ChatColor.GOLD + "/cskoth placeholdertest" + ChatColor.GRAY + " - Test CSKoth placeholders");
        }
        sender.sendMessage(ChatColor.GOLD + "/cskoth points [name]" + ChatColor.GRAY + " - Show KOTH capture points");
    }
}