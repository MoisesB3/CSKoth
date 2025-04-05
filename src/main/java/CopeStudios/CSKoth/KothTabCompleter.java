package CopeStudios.CSKoth;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class KothTabCompleter implements TabCompleter {
    private final CSKoth plugin;

    public KothTabCompleter(CSKoth plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("menu");
            completions.add("points");
            completions.add("help");

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