package CopeStudios.CSKoth;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

            case "help":
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "CSKoth Commands:");
        sender.sendMessage(ChatColor.GOLD + "/cskoth" + ChatColor.GRAY + " - Open the KOTH menu");
        sender.sendMessage(ChatColor.GOLD + "/cskoth menu" + ChatColor.GRAY + " - Open the KOTH menu");
        if (sender.hasPermission("cskoth.admin")) {
            sender.sendMessage(ChatColor.GOLD + "/cskoth create <name>" + ChatColor.GRAY + " - Create a new KOTH at your location");
            sender.sendMessage(ChatColor.GOLD + "/cskoth start <name>" + ChatColor.GRAY + " - Start a KOTH event");
            sender.sendMessage(ChatColor.GOLD + "/cskoth stop <name>" + ChatColor.GRAY + " - Stop a KOTH event");
            sender.sendMessage(ChatColor.GOLD + "/cskoth list" + ChatColor.GRAY + " - List all KOTHs");
        }
        sender.sendMessage(ChatColor.GOLD + "/cskoth points [name]" + ChatColor.GRAY + " - Show KOTH capture points");
    }
}