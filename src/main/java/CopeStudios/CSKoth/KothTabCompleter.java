package CopeStudios.CSKoth;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KothTabCompleter implements TabCompleter {
    private final CSKoth plugin;

    public KothTabCompleter(CSKoth plugin) {
        this.plugin = plugin;
        createVariablesGuide();
    }

    private void createVariablesGuide() {
        File guideFile = new File(plugin.getDataFolder(), "variables_guide.yml");
        if (!guideFile.exists()) {
            YamlConfiguration guide = new YamlConfiguration();

            // Player Statistics
            guide.set("Player Statistics.cskoth_captures", "Number of KOTHs the player has captured");
            guide.set("Player Statistics.cskoth_wins", "Alias for captures");
            guide.set("Player Statistics.cskoth_contests", "Number of times the player has contested a KOTH");
            guide.set("Player Statistics.cskoth_rank", "Player's rank based on number of captures (1 = highest)");
            guide.set("Player Statistics.cskoth_topwins", "Formatted string of top 10 players by captures");

            // Current KOTH Status
            guide.set("Current KOTH Status.cskoth_active", "List of currently active KOTHs");
            guide.set("Current KOTH Status.cskoth_next", "Name of the next scheduled KOTH");
            guide.set("Current KOTH Status.cskoth_next_time", "Start time of the next scheduled KOTH");
            guide.set("Current KOTH Status.cskoth_next_countdown", "Time remaining until the next KOTH starts");

            // KOTH Zone Information
            guide.set("KOTH Zone Information.cskoth_points_name", "Points value for capturing the specified KOTH (replace 'name' with KOTH name)");
            guide.set("KOTH Zone Information.cskoth_captime_name", "Capture time in seconds for the specified KOTH (replace 'name' with KOTH name)");
            guide.set("KOTH Zone Information.cskoth_status_name", "Status of the specified KOTH (Active/Inactive) (replace 'name' with KOTH name)");

            // KOTH Capture Progress
            guide.set("KOTH Capture Progress.cskoth_capturer_name", "Name of the player currently capturing the specified KOTH (replace 'name' with KOTH name)");
            guide.set("KOTH Capture Progress.cskoth_progress_name", "Capture progress for the specified KOTH in seconds (replace 'name' with KOTH name)");
            guide.set("KOTH Capture Progress.cskoth_remaining_name", "Time remaining to capture the specified KOTH in seconds (replace 'name' with KOTH name)");

            try {
                guide.save(guideFile);
                plugin.getLogger().info("Created variables_guide.yml");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create variables_guide.yml: " + e.getMessage());
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("menu");
            completions.add("points");
            completions.add("help");
            completions.add("next");

            if (sender.hasPermission("cskoth.admin")) {
                completions.add("create");
                completions.add("start");
                completions.add("stop");
                completions.add("list");
            }

            return filterCompletions(completions, args[0]);
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if ((subCommand.equals("start") || subCommand.equals("stop") ||
                    subCommand.equals("points")) &&
                    sender.hasPermission("cskoth.admin")) {
                return filterCompletions(new ArrayList<>(plugin.getKothZones().keySet()), args[1]);
            }
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> completions, String prefix) {
        if (prefix.isEmpty()) {
            return completions;
        }

        prefix = prefix.toLowerCase();
        List<String> filteredCompletions = new ArrayList<>();

        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(prefix)) {
                filteredCompletions.add(completion);
            }
        }

        return filteredCompletions;
    }
}