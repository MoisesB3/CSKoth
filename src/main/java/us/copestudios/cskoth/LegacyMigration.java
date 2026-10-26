package us.copestudios.cskoth;

import org.bukkit.configuration.file.YamlConfiguration;
import us.copestudios.copecore.config.ConfigFile;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One-time upgrade from CSKoth 1.x. Nothing is deleted.
 *
 * <ul>
 *   <li>config.yml - same setting names, kept as it is (new settings are added by CopeCore).</li>
 *   <li>koths.yml, stats.yml - same layout, read as they are (KothManager / Stats).</li>
 *   <li>messages.yml - 1.x used codes like "E0004". It's renamed to messages.yml.old and every text you had
 *       changed is copied to its new key. (%player%, %koth%, %time% and %points% still work.)</li>
 *   <li>guide.yml, placeholders_guide.yml - not used any more (see README.md); you can delete them.</li>
 * </ul>
 */
final class LegacyMigration {

    /** 1.x message code -> new key. Codes that aren't here have no direct match any more. */
    private static final Map<String, String> CODES = new LinkedHashMap<>();

    static {
        CODES.put("A0001", "prefix");
        CODES.put("A0002", "generic.players-only");
        CODES.put("A0003", "generic.no-permission");
        CODES.put("B0008", "koth.not-found");
        CODES.put("B0009", "list.empty");
        CODES.put("B0014", "next.none");
        CODES.put("E0001", "capture.lost");
        CODES.put("E0002", "capture.resumed");
        CODES.put("E0003", "capture.progress");
        CODES.put("E0004", "capture.captured");
        CODES.put("E0005", "koth.ended");
        CODES.put("E0006", "capture.started");
        CODES.put("E0007", "capture.contested");
        CODES.put("E0008", "capture.contested-progress");
        CODES.put("E0009", "capture.item-rewards");
        CODES.put("F0001", "top.header");
    }

    private final CSKoth plugin;
    private boolean oldMessages;

    LegacyMigration(CSKoth plugin) {
        this.plugin = plugin;
    }

    /** Before messages.yml is opened: move the 1.x one away. */
    void prepareFiles() {
        File messages = new File(plugin.getDataFolder(), "messages.yml");
        if (messages.isFile() && YamlConfiguration.loadConfiguration(messages).contains("A0001")) {
            File target = new File(plugin.getDataFolder(), "messages.yml.old");
            if (target.exists()) {
                target = new File(plugin.getDataFolder(), "messages.yml.old-" + System.currentTimeMillis());
            }
            oldMessages = messages.renameTo(target);
            if (oldMessages) {
                plugin.getLogger().info("Found the CSKoth 1.x messages.yml - upgrading it (kept as " + target.getName() + ").");
            }
        }
    }

    /** After the new messages.yml exists: copy the texts that were changed in 1.x. */
    void importOldMessages() {
        if (!oldMessages) {
            return;
        }
        File[] candidates = plugin.getDataFolder().listFiles((dir, name) -> name.startsWith("messages.yml.old"));
        if (candidates == null || candidates.length == 0) {
            return;
        }
        File newest = candidates[0];
        for (File file : candidates) {
            if (file.lastModified() > newest.lastModified()) {
                newest = file;
            }
        }
        YamlConfiguration old = YamlConfiguration.loadConfiguration(newest);
        YamlConfiguration defaults = new YamlConfiguration();
        try (InputStream in = plugin.getResource("legacy/old-messages.yml")) {
            if (in != null) {
                defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
            // without the old defaults every old text counts as changed
        }

        ConfigFile file = plugin.messages().file();
        int copied = 0;
        for (Map.Entry<String, String> entry : CODES.entrySet()) {
            String value = old.getString(entry.getKey());
            if (value == null || value.equals(defaults.getString(entry.getKey()))) {
                continue;
            }
            file.get().set(entry.getValue(), value);
            copied++;
        }
        if (copied > 0) {
            file.save();
            plugin.messages().reload();
        }
        plugin.getLogger().info("messages.yml upgraded: " + copied + " changed text(s) kept.");
    }
}
