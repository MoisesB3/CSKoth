package CopeStudios.CSKoth;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageManager {
    private final JavaPlugin plugin;
    private final File messagesFile;
    private FileConfiguration messagesConfig;
    private final Map<String, String> messages = new HashMap<>();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        loadMessages();
    }

    private void loadMessages() {
        if (!messagesFile.exists()) {
            try {
                // Create directory if it doesn't exist
                if (!plugin.getDataFolder().exists()) {
                    plugin.getDataFolder().mkdirs();
                }

                // Create empty file
                messagesFile.createNewFile();
                messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

                // Populate with default messages
                saveDefaultMessages();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create messages.yml: " + e.getMessage());
                // Create empty config to prevent NPE
                messagesConfig = new YamlConfiguration();
            }
        } else {
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        }

        messages.clear();
        for (String key : messagesConfig.getKeys(true)) {
            if (messagesConfig.isString(key)) {
                messages.put(key, messagesConfig.getString(key));
            }
        }
    }

    public void reloadMessages() {
        loadMessages();
    }

    public String getMessage(String id) {
        String message = messages.getOrDefault(id, "Message not found: " + id);
        return formatMessage(message);
    }

    public String formatMessage(String message) {
        // Convert hex colors (&#RRGGBB)
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hexCode = matcher.group(1);
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + hexCode).toString());
        }
        matcher.appendTail(buffer);

        // Convert standard color codes
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public void saveDefaultMessages() {
        // General messages
        setDefaultMessage("A0001", "&7[&6CSKoth&7] ");
        setDefaultMessage("A0002", "&cThis command can only be used by players.");
        setDefaultMessage("A0003", "&cYou don't have permission to use this command.");

        // KothCommand messages
        setDefaultMessage("B0001", "&cUsage: /cskoth create <name>");
        setDefaultMessage("B0002", "&cUsage: /cskoth start <name>");
        setDefaultMessage("B0003", "&cUsage: /cskoth stop <name>");
        setDefaultMessage("B0004", "&eAvailable KOTHs:");
        setDefaultMessage("B0005", "&6- %koth% %status%");
        setDefaultMessage("B0006", "&a(Active)");
        setDefaultMessage("B0007", "&c(Inactive)");
        setDefaultMessage("B0008", "&cKOTH not found: %koth%");
        setDefaultMessage("B0009", "&cThere are no KOTHs configured.");
        setDefaultMessage("B0010", "&eNext KOTH: &6%koth%");
        setDefaultMessage("B0011", "&eStarting at: &a%time%");
        setDefaultMessage("B0012", "&eTime until start: &a%hours% hours %minutes% minutes");
        setDefaultMessage("B0013", "&eTime until start: &a%minutes% minutes");
        setDefaultMessage("B0014", "&eThere are no scheduled KOTHs.");
        setDefaultMessage("B0015", "&eCSKoth Commands:");
        setDefaultMessage("B0016", "&6/cskoth &7- Open the KOTH menu");
        setDefaultMessage("B0017", "&6/cskoth menu &7- Open the KOTH menu");
        setDefaultMessage("B0018", "&6/cskoth next &7- Show the next scheduled KOTH");
        setDefaultMessage("B0019", "&6/cskoth create <name> &7- Create a new KOTH at your location");
        setDefaultMessage("B0020", "&6/cskoth start <name> &7- Start a KOTH event");
        setDefaultMessage("B0021", "&6/cskoth stop <name> &7- Stop a KOTH event");
        setDefaultMessage("B0022", "&6/cskoth list &7- List all KOTHs");
        setDefaultMessage("B0023", "&6/cskoth points [name] &7- Show KOTH capture points");

        // KothGUI messages
        setDefaultMessage("C0001", "&6CSKoth Menu");
        setDefaultMessage("C0002", "&aCreate KOTH");
        setDefaultMessage("C0003", "&7Create a new King of the Hill zone");
        setDefaultMessage("C0004", "&bList KOTHs");
        setDefaultMessage("C0005", "&7View and manage your KOTH zones");
        setDefaultMessage("C0006", "&cClose");
        setDefaultMessage("C0007", "&6KOTH List");
        setDefaultMessage("C0008", "&cBack");
        setDefaultMessage("C0009", "&7Status: %status%");
        setDefaultMessage("C0010", "&7Capture Time: &e%time%s");
        setDefaultMessage("C0011", "&7Points: &e%points%");
        setDefaultMessage("C0012", "&eClick to manage");
        setDefaultMessage("C0013", "&6Manage KOTH: %koth%");
        setDefaultMessage("C0014", "&aStart KOTH");
        setDefaultMessage("C0015", "&7Start this KOTH event");
        setDefaultMessage("C0016", "&cStop KOTH");
        setDefaultMessage("C0017", "&7Stop this KOTH event");
        setDefaultMessage("C0018", "&bEdit Settings");
        setDefaultMessage("C0019", "&7Change capture time, points, etc.");
        setDefaultMessage("C0020", "&6Rewards");
        setDefaultMessage("C0021", "&7Set rewards for capturing");
        setDefaultMessage("C0022", "&dSchedule");
        setDefaultMessage("C0023", "&7Set automatic scheduling");
        setDefaultMessage("C0024", "&6Resize Zone");
        setDefaultMessage("C0025", "&7Change the size of the KOTH zone");
        setDefaultMessage("C0026", "&eDelete KOTH");
        setDefaultMessage("C0027", "&7Remove this KOTH zone");

        // More KothGUI messages (settings)
        setDefaultMessage("C0028", "&6KOTH Settings: %koth%");
        setDefaultMessage("C0029", "&eChange Capture Time");
        setDefaultMessage("C0030", "&7Current: &6%time% seconds");
        setDefaultMessage("C0031", "&7Click to change");
        setDefaultMessage("C0032", "&eChange Points");
        setDefaultMessage("C0033", "&7Current: &6%points% points");

        // Rewards messages
        setDefaultMessage("C0034", "&6KOTH Rewards: %koth%");
        setDefaultMessage("C0035", "&ePhysical Rewards");
        setDefaultMessage("C0036", "&7Set items to give to the winner");
        setDefaultMessage("C0037", "&eCommand Rewards");
        setDefaultMessage("C0038", "&7Set commands to execute when captured");
        setDefaultMessage("C0039", "&6Physical Rewards: %koth%");
        setDefaultMessage("C0040", "&aSave Rewards");
        setDefaultMessage("C0041", "&aPhysical rewards saved!");
        setDefaultMessage("C0042", "&aPhysical rewards saved on close!");

        // Command rewards
        setDefaultMessage("C0043", "&6Command Rewards: %koth%");
        setDefaultMessage("C0044", "&eCommand %number%");
        setDefaultMessage("C0045", "&cRight-click to remove");
        setDefaultMessage("C0046", "&aAdd Command");
        setDefaultMessage("C0047", "&aCommand removed!");

        // Schedule messages
        setDefaultMessage("C0048", "&6KOTH Schedule: %koth%");
        setDefaultMessage("C0049", "&eScheduled: %status%");
        setDefaultMessage("C0050", "&aON");
        setDefaultMessage("C0051", "&cOFF");
        setDefaultMessage("C0052", "&eAlways On: %status%");
        setDefaultMessage("C0053", "&eStart Time");
        setDefaultMessage("C0054", "&7Current: &6%time%");
        setDefaultMessage("C0055", "&7Current: Not set");
        setDefaultMessage("C0056", "&eDuration (minutes)");
        setDefaultMessage("C0057", "&7Current: &6%duration% minutes");
        setDefaultMessage("C0058", "&eRespawn Delay (minutes)");
        setDefaultMessage("C0059", "&7Current: &6%delay% minutes");
        setDefaultMessage("C0060", "&7Time between captures when Always On");

        // Resize menu
        setDefaultMessage("C0061", "&6Resize KOTH: %koth%");
        setDefaultMessage("C0062", "&aExpand KOTH Zone");
        setDefaultMessage("C0063", "&7Increase the size of the KOTH zone");
        setDefaultMessage("C0064", "&7by 1 block in all directions");
        setDefaultMessage("C0065", "&cShrink KOTH Zone");
        setDefaultMessage("C0066", "&7Decrease the size of the KOTH zone");
        setDefaultMessage("C0067", "&7by 1 block in all directions");
        setDefaultMessage("C0068", "&eManual Selection");
        setDefaultMessage("C0069", "&7Redefine the KOTH zone manually");
        setDefaultMessage("C0070", "&7by setting two corner positions");
        setDefaultMessage("C0071", "&bCurrent Zone Size");
        setDefaultMessage("C0072", "&7Width: &e%width%");
        setDefaultMessage("C0073", "&7Height: &e%height%");
        setDefaultMessage("C0074", "&7Length: &e%length%");
        setDefaultMessage("C0075", "&7Volume: &e%volume% blocks");
        setDefaultMessage("C0076", "&aKOTH zone expanded by 1 block in all directions.");
        setDefaultMessage("C0077", "&cKOTH zone is too small to shrink further.");
        setDefaultMessage("C0078", "&aKOTH zone shrunk by 1 block in all directions.");

        // Creation and management messages
        setDefaultMessage("D0001", "&eEnter the name for your new KOTH in chat:");
        setDefaultMessage("D0002", "&eEnter the new capture time in seconds:");
        setDefaultMessage("D0003", "&eEnter the new points value:");
        setDefaultMessage("D0004", "&eEnter a command (without the / prefix):");
        setDefaultMessage("D0005", "&eEnter the start hour (0-23):");
        setDefaultMessage("D0006", "&eEnter the duration in minutes:");
        setDefaultMessage("D0007", "&eEnter the respawn delay in minutes:");

        // Stats messages
        setDefaultMessage("F0001", "&6Top 10 KOTH Captures:");
        setDefaultMessage("F0002", "&e%rank%. &f%player%: &a%captures%");

        // Runtime messages
        setDefaultMessage("E0001", "&eKOTH %koth% is no longer being captured!");
        setDefaultMessage("E0002", "&a%player% &ehas resumed capturing KOTH &6%koth%&e!");
        setDefaultMessage("E0003", "&a%player% &eis capturing KOTH: &6%koth% &e- Time left: &a%time%");
        setDefaultMessage("E0004", "&a%player% &ehas captured KOTH &6%koth% &eand earned &a%points% &epoints!");
        setDefaultMessage("E0005", "&eKOTH &6%koth% &ehas ended.");
        setDefaultMessage("E0006", "&a%player% &eis capturing KOTH &6%koth%&e!");
        setDefaultMessage("E0007", "&eKOTH &6%koth% &eis contested!");
        setDefaultMessage("E0008", "&eKOTH &6%koth% &eis contested! %player% has &a%time% &ecapture time remaining.");
        setDefaultMessage("E0009", "&aYou received physical rewards for capturing the KOTH!");

        saveMessages();
    }

    private void setDefaultMessage(String id, String defaultMessage) {
        if (!messagesConfig.contains(id)) {
            messagesConfig.set(id, defaultMessage);
        }
    }

    private void saveMessages() {
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save messages.yml: " + e.getMessage());
        }
    }

    // Convenience method to get a message with replacements
    public String getMessage(String id, Map<String, String> replacements) {
        String message = getMessage(id);

        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }

        return message;
    }

    // Convenience method for simple replacements
    public String getMessage(String id, String placeholder, String value) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put(placeholder, value);
        return getMessage(id, replacements);
    }

    // Convenience method for two replacements
    public String getMessage(String id, String placeholder1, String value1, String placeholder2, String value2) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put(placeholder1, value1);
        replacements.put(placeholder2, value2);
        return getMessage(id, replacements);
    }
}