package us.copestudios.cskoth;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/**
 * Keeps the CSKoth 1.x placeholder names ({@code %cskoth_captures%} ...) working.
 *
 * <p>This is the only class that touches PlaceholderAPI, and it's only loaded when PlaceholderAPI is installed.
 * 1.x always loaded its PlaceholderAPI class, so the plugin crashed on servers without PlaceholderAPI.</p>
 */
final class LegacyPapi extends PlaceholderExpansion {

    private final CSKoth plugin;
    private final Placeholders placeholders;

    private LegacyPapi(CSKoth plugin, Placeholders placeholders) {
        this.plugin = plugin;
        this.placeholders = placeholders;
    }

    static void register(CSKoth plugin, Placeholders placeholders) {
        new LegacyPapi(plugin, placeholders).register();
    }

    @Override
    public String getIdentifier() {
        return "cskoth";
    }

    @Override
    public String getAuthor() {
        return "Cope Studios";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /** Stay registered across /papi reload. */
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (!plugin.isEnabled()) {
            return null;
        }
        return placeholders.resolve(player, params);
    }
}
